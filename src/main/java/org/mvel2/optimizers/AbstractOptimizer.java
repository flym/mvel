/**
 * MVEL 2.0
 * Copyright (C) 2007 The Codehaus
 * Mike Brock, Dhanji Prasanna, John Graham, Mark Proctor
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mvel2.optimizers;

import org.mvel2.CompileException;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.compiler.AbstractParser;

import java.lang.reflect.Method;

import static java.lang.Thread.currentThread;
import static org.mvel2.util.ParseTools.*;

/**
 * @author Christopher Brock
 */
public class AbstractOptimizer extends AbstractParser {
  /** 表示属性访问 */
  protected static final int BEAN = 0;
  /** 表示方法访问 */
  protected static final int METH = 1;
  /** 表示集合访问 */
  protected static final int COL = 2;
  protected static final int WITH = 3;

  /** 当前处理是否是集合 */
  protected boolean collection = false;
  /** 当前处理是否是null安全的 */
  protected boolean nullSafe = false;
  /** 当前处理属性的类型 */
  protected Class currType = null;
  /** 当前是否是静态方法，即静态访问字段，类等 */
  protected boolean staticAccess = false;

  /** 当前处理的表达式在整个语句中的起始下标,为一个在处理过程中会变化的下标位,其可认为在start和end当中作于其它作用的临时变量 */
  protected int tkStart;

  protected AbstractOptimizer() {
  }

  protected AbstractOptimizer(ParserContext pCtx) {
    super(pCtx);
  }

  /**
   * 尝试静态访问此属性，此属性可能是字段，类或者对象本身
   * Try static access of the property, and return an instance of the Field, Method of Class if successful.
   *
   * @return - Field, Method or Class instance.
   */
  protected Object tryStaticAccess() {
    int begin = cursor;
    try {
      /**
       * Try to resolve this *smartly* as a static class reference.
       *
       * This starts at the end of the token and starts to step backwards to figure out whether
       * or not this may be a static class reference.  We search for method calls simply by
       * inspecting for ()'s.  The first union area we come to where no brackets are present is our
       * test-point for a class reference.  If we find a class, we pass the reference to the
       * property accessor along  with trailing methods (if any).
       *
       */
      boolean meth = false;
      // int end = start + length;
      int last = end;
      for (int i = end - 1; i > start; i--) {
        switch (expr[i]) {
          case '.':
            if (!meth) {
              ClassLoader classLoader = pCtx != null ? pCtx.getClassLoader() : currentThread().getContextClassLoader();
              String test = new String(expr, start, (cursor = last) - start);
              try {
                if (MVEL.COMPILER_OPT_SUPPORT_JAVA_STYLE_CLASS_LITERALS && test.endsWith(".class"))
                  test = test.substring(0, test.length() - 6);

                return Class.forName(test, true, classLoader);
              }
              catch (ClassNotFoundException cnfe) {
                try {
                  return findInnerClass(test, classLoader, cnfe);
                }
                catch (ClassNotFoundException e) { /* ignore */ }
                Class cls = forNameWithInner(new String(expr, start, i - start), classLoader);
                String name = new String(expr, i + 1, end - i - 1);
                try {
                  return cls.getField(name);
                }
                catch (NoSuchFieldException nfe) {
                  for (Method m : cls.getMethods()) {
                    if (name.equals(m.getName())) return m;
                  }
                  return null;
                }
              }
            }

            meth = false;
            last = i;
            break;

          case '}':
            i--;
            for (int d = 1; i > start && d != 0; i--) {
              switch (expr[i]) {
                case '}':
                  d++;
                  break;
                case '{':
                  d--;
                  break;
                case '"':
                case '\'':
                  char s = expr[i];
                  while (i > start && (expr[i] != s && expr[i - 1] != '\\')) i--;
              }
            }
            break;

          case ')':
            i--;

            for (int d = 1; i > start && d != 0; i--) {
              switch (expr[i]) {
                case ')':
                  d++;
                  break;
                case '(':
                  d--;
                  break;
                case '"':
                case '\'':
                  char s = expr[i];
                  while (i > start && (expr[i] != s && expr[i - 1] != '\\')) i--;
              }
            }

            meth = true;
            last = i++;
            break;


          case '\'':
            while (--i > start) {
              if (expr[i] == '\'' && expr[i - 1] != '\\') {
                break;
              }
            }
            break;

          case '"':
            while (--i > start) {
              if (expr[i] == '"' && expr[i - 1] != '\\') {
                break;
              }
            }
            break;
        }
      }
    }
    catch (Exception cnfe) {
      cursor = begin;
    }

    return null;
  }

  /**
   * 读取可能的操作属性,通过查找当前字符串中可能存在的特殊符号来进行定位.
   * <p/>
   * 操作属性的读取是通过读取最接近的操作来完成的,而并不是一步一步来完成的.如a.b[2]则会定义为集合访问，即最终为(a.b)[2]这种操作，然后先处理a.b，再处理[2]操作
   */
  protected int nextSubToken() {
    skipWhitespace();
    nullSafe = false;

    //先通过首字符来判定，可能是集合，属性或者其它调用
    switch (expr[tkStart = cursor]) {
      //集合调用
      case '[':
        return COL;
      //with调用
      case '{':
        if (expr[cursor - 1] == '.') {
          return WITH;
        }
        break;
      //属性调用,如果.后接一个?号，表示当前属性的值结果可能是null的
      case '.':
        if ((start + 1) != end) {
          switch (expr[cursor = ++tkStart]) {
            case '?':
              skipWhitespace();
              if ((cursor = ++tkStart) == end) {
                throw new CompileException("unexpected end of statement", expr, start);
              }
              nullSafe = true;

              fields = -1;
              break;
            //.后面接{,表示with调用
            case '{':
              return WITH;
            default:
              if (isWhitespace(expr[tkStart])) {
                skipWhitespace();
                tkStart = cursor;
              }
          }
        }
        else {
          throw new CompileException("unexpected end of statement", expr, start);
        }
        break;
      //这里直接在最前台加一个?，即表示访问这个属性，并且这个属性值可能为null
      case '?':
        if (start == cursor) {
          tkStart++;
          cursor++;
          nullSafe = true;
        }
    }

    //表示没有特殊字段,则是正常的字符,则继续找到下一个非字符处理
    //noinspection StatementWithEmptyBody
    while (++cursor < end && isIdentifierPart(expr[cursor])) ;

    //在跳过一堆字段之后，还没有到达末尾，表示中间有类似操作符存在，则通过第一个非字段点来进行判断
    skipWhitespace();
    if (cursor < end) {
      switch (expr[cursor]) {
        case '[':
          return COL;
        case '(':
          return METH;
        default:
          return BEAN;
      }
    }

    //默认为bean操作，即读取属性
    return 0;
  }

  /** 当前捕获的属性名(字符串),即在刚才的处理过程中处理的字符串 */
  protected String capture() {
    /**
     * Trim off any whitespace.
     */
    return new String(expr, tkStart = trimRight(tkStart), trimLeft(cursor) - tkStart);
  }

  /**
   * Skip to the next non-whitespace position.
   */
  protected void whiteSpaceSkip() {
    if (cursor < length)
      //noinspection StatementWithEmptyBody
      while (isWhitespace(expr[cursor]) && ++cursor != length) ;
  }

  /**
   * 查找指定的字符，直到找到为止
   *
   * @param c - character to scan to.
   * @return - returns true is end of statement is hit, false if the scan scar is countered.
   */
  protected boolean scanTo(char c) {
    for (; cursor < end; cursor++) {
      switch (expr[cursor]) {
        case '\'':
        case '"':
          cursor = captureStringLiteral(expr[cursor], expr, cursor, end);
        default:
          if (expr[cursor] == c) {
            return false;
          }
      }
    }
    return true;
  }

  protected int findLastUnion() {
    int split = -1;
    int depth = 0;

    int end = start + length;
    for (int i = end - 1; i != start; i--) {
      switch (expr[i]) {
        case '}':
        case ']':
          depth++;
          break;

        case '{':
        case '[':
          if (--depth == 0) {
            split = i;
            collection = true;
          }
          break;
        case '.':
          if (depth == 0) {
            split = i;
          }
          break;
      }
      if (split != -1) break;
    }

    return split;
  }
}
