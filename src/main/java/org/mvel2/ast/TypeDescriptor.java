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

package org.mvel2.ast;

import org.mvel2.CompileException;
import org.mvel2.ParserContext;
import org.mvel2.compiler.AbstractParser;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.util.ParseTools;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;

import static java.lang.Character.isDigit;
import static org.mvel2.ast.ASTNode.COMPILE_IMMEDIATE;
import static org.mvel2.util.ArrayTools.findFirst;
import static org.mvel2.util.ParseTools.*;
import static org.mvel2.util.ReflectionUtil.toPrimitiveArrayType;

/**
 * 类型描述，即描述一个有效的类型定义信息,如Date Date[2]这样的
 * 对于非数组，这里仅描述类名，对于数组，则要求描述相应的数组长度信息
 */
public class TypeDescriptor implements Serializable {
  /** 存储的类名 */
  private String className;
  /** 当前处理字符串引用 */
  private char[] expr;

  /** 起始点 */
  private int start;
  /** 跨度(即start+size中的size值) */
  private int offset;

  /** 用于描述数组长度的信息值 */
  private ArraySize[] arraySize;
  /** 用于描述长度信息中可能存在的计算表达式 */
  private ExecutableStatement[] compiledArraySize;
  int endRange;

  public TypeDescriptor(char[] name, int start, int offset, int fields) {
    updateClassName(this.expr = name, this.start = start, this.offset = offset, fields);
  }

  public void updateClassName(char[] name, int start, int offset, int fields) {
    this.expr = name;

    if (offset == 0 || !ParseTools.isIdentifierPart(name[start]) || isDigit(name[start])) return;

    if ((endRange = findFirst('(', start, offset, name)) == -1) {
      if ((endRange = findFirst('[', start, offset, name)) != -1) {
        className = new String(name, start, endRange - start).trim();
        int to;

        LinkedList<char[]> sizes = new LinkedList<char[]>();

        int end = start + offset;
        while (endRange < end) {
          while (endRange < end && isWhitespace(name[endRange])) endRange++;

          if (endRange == end || name[endRange] == '{') break;

          if (name[endRange] != '[') {
            throw new CompileException("unexpected token in constructor", name, endRange);
          }
          to = balancedCapture(name, endRange, start + offset, '[');
          sizes.add(subset(name, ++endRange, to - endRange));
          endRange = to + 1;
        }

        Iterator<char[]> iter = sizes.iterator();
        arraySize = new ArraySize[sizes.size()];

        for (int i = 0; i < arraySize.length; i++)
          arraySize[i] = new ArraySize(iter.next());

        //这里表示在编译期的话，需要再次处理[]里面表达式的值
        if ((fields & COMPILE_IMMEDIATE) != 0) {
          compiledArraySize = new ExecutableStatement[arraySize.length];
          for (int i = 0; i < compiledArraySize.length; i++)
            compiledArraySize[i] = (ExecutableStatement) subCompileExpression(arraySize[i].value);
        }

        return;
      }

      className = new String(name, start, offset).trim();
    }
    else {
      className = new String(name, start, endRange - start).trim();
    }
  }

  public boolean isArray() {
    return arraySize != null;
  }

  public int getArrayLength() {
    return arraySize.length;
  }

  public ArraySize[] getArraySize() {
    return arraySize;
  }

  public ExecutableStatement[] getCompiledArraySize() {
    return compiledArraySize;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public boolean isClass() {
    return className != null && className.length() != 0;
  }

  public int getEndRange() {
    return endRange;
  }

  public void setEndRange(int endRange) {
    this.endRange = endRange;
  }

  public Class<?> getClassReference() throws ClassNotFoundException {
    return getClassReference(null, this);
  }

  public Class<?> getClassReference(ParserContext ctx) throws ClassNotFoundException {
    return getClassReference(ctx, this);
  }

  public static Class getClassReference(Class baseType,
                                        TypeDescriptor tDescr,
                                        VariableResolverFactory factory, ParserContext ctx) throws ClassNotFoundException {
    return findClass(factory, repeatChar('[', tDescr.arraySize.length) + "L" + baseType.getName() + ";", ctx);
  }

  public static Class getClassReference(ParserContext ctx, Class cls, TypeDescriptor tDescr) throws ClassNotFoundException {
    if (tDescr.isArray()) {
      cls = cls.isPrimitive() ?
          toPrimitiveArrayType(cls) :
          findClass(null, repeatChar('[', tDescr.arraySize.length) + "L" + cls.getName() + ";", ctx);
    }
    return cls;
  }


  /** 从上下文中获取相应的类型信息，因为从原始的typeDescriptor中拿不到类型 */
  public static Class getClassReference(ParserContext ctx, TypeDescriptor tDescr) throws ClassNotFoundException {
    Class cls;
    //由上下文引用的
    if (ctx != null && ctx.hasImport(tDescr.className)) {
      cls = ctx.getImport(tDescr.className);
      if (tDescr.isArray()) {
        cls = cls.isPrimitive() ?
            toPrimitiveArrayType(cls) :
            findClass(null, repeatChar('[', tDescr.arraySize.length) + "L" + cls.getName() + ";", ctx);
      }
    }
    else if (ctx == null && hasContextFreeImport(tDescr.className)) {
      //由字面量常量引用的，如true,false等
      cls = getContextFreeImport(tDescr.className);
      if (tDescr.isArray()) {
        cls = cls.isPrimitive() ?
            toPrimitiveArrayType(cls) :
            findClass(null, repeatChar('[', tDescr.arraySize.length) + "L" + cls.getName() + ";", ctx);
      }
    }
    else {
      //默认处理，实际上到这里的也会报错
      cls = createClass(tDescr.getClassName(), ctx);
      if (tDescr.isArray()) {
        cls = cls.isPrimitive() ?
            toPrimitiveArrayType(cls) :
            findClass(null, repeatChar('[', tDescr.arraySize.length) + "L" + cls.getName() + ";", ctx);
      }
    }

    return cls;
  }

  public boolean isUndimensionedArray() {
    if (arraySize != null) {
      for (ArraySize anArraySize : arraySize) {
        if (anArraySize.value.length == 0) return true;
      }
    }

    return false;
  }

  public static boolean hasContextFreeImport(String name) {
    return AbstractParser.LITERALS.containsKey(name) && AbstractParser.LITERALS.get(name) instanceof Class;
  }

  public static Class getContextFreeImport(String name) {
    return (Class) AbstractParser.LITERALS.get(name);
  }

  public char[] getExpr() {
    return expr;
  }

  public int getStart() {
    return start;
  }

  public int getOffset() {
    return offset;
  }
}
