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
import org.mvel2.ParserConfiguration;
import org.mvel2.ParserContext;
import org.mvel2.compiler.Accessor;
import org.mvel2.debug.DebugTools;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.optimizers.AccessorOptimizer;
import org.mvel2.optimizers.OptimizationNotSupported;

import java.io.Serializable;

import static java.lang.Thread.currentThread;
import static org.mvel2.Operator.NOOP;
import static org.mvel2.PropertyAccessor.get;
import static org.mvel2.optimizers.OptimizerFactory.*;
import static org.mvel2.util.CompilerTools.getInjectedImports;
import static org.mvel2.util.ParseTools.*;

/**
 * 通用节点描述，当前节点也可以通过一些判定，判定为其它节点，以提供节点之间的转换操作
 */
@SuppressWarnings({"ManualArrayCopy", "CaughtExceptionImmediatelyRethrown"})
public class ASTNode implements Cloneable, Serializable {
  /** 该节点为文本节点 */
  public static final int LITERAL = 1;
  /** 是否是深度属性处理(如+=) */
  public static final int DEEP_PROPERTY = 1 << 1;
  /** 该节点为操作节点 */
  public static final int OPERATOR = 1 << 2;
  /** 变量节点 */
  public static final int IDENTIFIER = 1 << 3;
  /** 表示当前处于编译阶段,是一个中间状态值 */
  public static final int COMPILE_IMMEDIATE = 1 << 4;
  /** 数字节点 */
  public static final int NUMERIC = 1 << 5;

  /** 表示该节点是否应该被反转,当前已实际无作用 */
  @Deprecated
  public static final int INVERT = 1 << 6;

  /** 表示该节点是一个赋值节点 */
  public static final int ASSIGN = 1 << 7;

  /** 当前节点中存在集合处理(如变量名中存在[这种操作符） */
  public static final int COLLECTION = 1 << 8;
  /** 当前节点是一个this引用节点，实际上不再使用 */
  @Deprecated
  public static final int THISREF = 1 << 9;
  /** 内联的集合,即{12,3,4} 这种数据 */
  public static final int INLINE_COLLECTION = 1 << 10;

  //---------------------------- 以下的标记仅起到静态标记的作用，没有其它逻辑作用 start ------------------------------//

  /** 特定的if语句块标识 */
  public static final int BLOCK_IF = 1 << 11;
  /** 特定的foreach语句块标识 */
  public static final int BLOCK_FOREACH = 1 << 12;
  /** 特定的with语句块标识 */
  public static final int BLOCK_WITH = 1 << 13;
  /** 特定的until语句块标识 */
  public static final int BLOCK_UNTIL = 1 << 14;
  /** while语句块 */
  public static final int BLOCK_WHILE = 1 << 15;
  /** do语句块 */
  public static final int BLOCK_DO = 1 << 16;
  /** do{}until语句块 */
  public static final int BLOCK_DO_UNTIL = 1 << 17;
  /** for语句块 */
  public static final int BLOCK_FOR = 1 << 18;

  //---------------------------- 以下的标记仅起到静态标记的作用，没有其它逻辑作用 end ------------------------------//

  /** 特定的标识 表示当前操作数需要取反，但实际上也没有意义 */
  public static final int OPT_SUBTR = 1 << 19;

  /** 表示当前节点是一个静态全称访问，即通过全类型名来访问一个属性，通常指静态属性访问 */
  public static final int FQCN = 1 << 20;

  /** 表示当前节点为StackLang类型 */
  public static final int STACKLANG = 1 << 22;

  public static final int DEFERRED_TYPE_RES = 1 << 23;
  /** 当前处理是否是强类型处理 */
  public static final int STRONG_TYPING = 1 << 24;
  public static final int PCTX_STORED = 1 << 25;
  /** 表示当前节点是一个 数组类型的空常量,如[]，即用来标识数组 */
  public static final int ARRAY_TYPE_LITERAL = 1 << 26;

  /** 取消优化 */
  public static final int NOJIT = 1 << 27;
  /** 表示当前节点需要反优化，即之前的优化失败了 */
  public static final int DEOP = 1 << 28;

  /** 表示当前节点应该被废掉 */
  public static final int DISCARD = 1 << 29;


  // *** //

  protected int firstUnion;
  protected int endOfName;

  /** 描述当前节点的解析属性值 */
  public int fields = 0;

  /** 当前节点的处理类型 */
  protected Class egressType;
  /** 描述当前节点所引用的字符串 */
  protected char[] expr;
  /** 有效值字符串起始位 */
  protected int start;
  /** 有效值字符串有效位长度 */
  protected int offset;

  /** 当前节点缓存的名称值(即当前节点的一个name属性，如变量名等) */
  protected String nameCache;

  /** 用于描述当前节点的一个常量值，即如果当前节点为表示一个常量信息,这里可以是任意的值，不仅仅是字符串 */
  protected Object literal;

  /** 当前节点的值访问器(优化版的)，通常指asm访问 */
  protected transient volatile Accessor accessor;
  /**
   * 当前节点的安全访问器(未优化的)
   * 这里的是否优化版，均是指通过二次编译之后的访问，而不是指解释运行
   */
  protected volatile Accessor safeAccessor;

  protected int cursorPosition;
  /** 当前节点的下一步节点(顺序上的下一步) */
  public ASTNode nextASTNode;

  /** 当前解析上下文 */
  protected ParserContext pCtx;

  /** 获取相应的执行值，采用快速优化模式运行 */
  public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
    if (accessor != null) {
      try {
        return accessor.getValue(ctx, thisValue, factory);
      }
      catch (ClassCastException ce) {
        return deop(ctx, thisValue, factory, ce);
      }
    }
    else {
      return optimize(ctx, thisValue, factory);
    }
  }

  private Object deop(Object ctx, Object thisValue, VariableResolverFactory factory, RuntimeException e) {
    if ((fields & DEOP) == 0) {
      accessor = null;
      fields |= DEOP | NOJIT;

      synchronized (this) {
        return getReducedValueAccelerated(ctx, thisValue, factory);
      }
    }
    else {
      throw e;
    }
  }

  private Object optimize(Object ctx, Object thisValue, VariableResolverFactory factory) {
    if ((fields & DEOP) != 0) {
      fields ^= DEOP;
    }

    AccessorOptimizer optimizer;
    Object retVal = null;

    if ((fields & NOJIT) != 0 || factory != null && factory.isResolveable(nameCache)) {
      optimizer = getAccessorCompiler(SAFE_REFLECTIVE);
    }
    else {
      optimizer = getDefaultAccessorCompiler();
    }

    ParserContext pCtx;

    if ((fields & PCTX_STORED) != 0) {
      pCtx = (ParserContext) literal;
    }
    else {
      pCtx = new ParserContext(new ParserConfiguration(getInjectedImports(factory), null));
    }

    try {
      pCtx.optimizationNotify();
      setAccessor(optimizer.optimizeAccessor(pCtx, expr, start, offset, ctx, thisValue, factory, true, egressType));
    }
    catch (OptimizationNotSupported ne) {
      setAccessor((optimizer = getAccessorCompiler(SAFE_REFLECTIVE))
          .optimizeAccessor(pCtx, expr, start, offset, ctx, thisValue, factory, true, null));
    }

    if (accessor == null) {
      return get(expr, start, offset, ctx, factory, thisValue, pCtx);
    }

    if (retVal == null) {
      retVal = optimizer.getResultOptPass();
    }

    if (egressType == null) {
      egressType = optimizer.getEgressType();
    }

    return retVal;
  }


  /**
   * 获取相应的执行值，采用解释模式运行
   * 解释模式即最简单的方式，通过逐步读取信息，然后通过读取下一步的字符来判定下一步的走向
   * 在mvel中，解释模式会在编译时改写为优化方式，因此大部分的节点都不支持解释运行
   * 解释模式的运行主要应用于如字段的读取或者访问地调用，在这些地方如果优化模式不能调用，将最终退化为解释模式运行
   * <p>
   * 解释模式主要用于在MvelInterpretedRuntime(即通过MVEL.eval调用的)时候，完成基本的解释调用处理
   * 主要的功能由四则混合运算以及bean(数组)访问来完成,其中混合运行通过ExecutionStack来完成，而属性访问则
   * 通过PropertyAccessor来完成，在解释过程中，能够进入到这里的，肯定只会有属性解释,因此默认的执行即是通过调用属性访问来获取相应的结果
   */
  public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
    //如果是常量节点，则直接返回相应的常量信息
    if ((fields & (LITERAL)) != 0) {
      return literal;
    }
    else {
      //调用解释模式来获取相应的数据信息
      return get(expr, start, offset, ctx, factory, thisValue, pCtx);
    }
  }

  protected String getAbsoluteRootElement() {
    if ((fields & (DEEP_PROPERTY | COLLECTION)) != 0) {
      return new String(expr, start, getAbsoluteFirstPart());
    }
    return nameCache;
  }

  public Class getEgressType() {
    return egressType;
  }

  public void setEgressType(Class egressType) {
    this.egressType = egressType;
  }

  public char[] getNameAsArray() {
    return subArray(expr, start, start + offset);
  }

  private int getAbsoluteFirstPart() {
    if ((fields & COLLECTION) != 0) {
      if (firstUnion < 0 || endOfName < firstUnion) return endOfName;
      else return firstUnion;
    }
    else if ((fields & DEEP_PROPERTY) != 0) {
      return firstUnion;
    }
    else {
      return -1;
    }
  }

  public String getAbsoluteName() {
    if (firstUnion > start) {
      return new String(expr, start, getAbsoluteFirstPart() - start);
    }
    else {
      return getName();
    }
  }

  public String getName() {
    if (nameCache != null) {
      return nameCache;
    }
    else if (expr != null) {
      return nameCache = new String(expr, start, offset);
    }
    return "";
  }

  public Object getLiteralValue() {
    return literal;
  }

  public void storeInLiteralRegister(Object o) {
    this.literal = o;
  }

  public void setLiteralValue(Object literal) {
    this.literal = literal;
    this.fields |= LITERAL;
  }

  @SuppressWarnings({"SuspiciousMethodCalls"})
  protected void setName(char[] name) {
    //判断当前字符串是否是一个数字，如果是数字，则设置相应的标记
    if (isNumber(name, start, offset)) {
      egressType = (literal = handleNumericConversion(name, start, offset)).getClass();
      if (((fields |= NUMERIC | LITERAL | IDENTIFIER) & INVERT) != 0) {
        try {
          literal = ~((Integer) literal);
        }
        catch (ClassCastException e) {
          throw new CompileException("bitwise (~) operator can only be applied to integers", expr, start);
        }
      }
      return;
    }

    this.literal = new String(name, start, offset);

    int end = start + offset;

    Scan:
    for (int i = start; i < end; i++) {
      switch (name[i]) {
        case '.':
          if (firstUnion == 0) {
            firstUnion = i;
          }
          break;
        case '[':
        case '(':
          if (firstUnion == 0) {
            firstUnion = i;
          }
          if (endOfName == 0) {
            endOfName = i;
            if (i < name.length && name[i + 1] == ']') fields |= ARRAY_TYPE_LITERAL;
            break Scan;
          }
      }
    }

    if ((fields & INLINE_COLLECTION) != 0) {
      return;
    }

    if (firstUnion > start) {
      fields |= DEEP_PROPERTY | IDENTIFIER;
    }
    else {
      fields |= IDENTIFIER;
    }
  }

  public Accessor setAccessor(Accessor accessor) {
    return this.accessor = accessor;
  }

  /** 表示当前节点是否是变量节点 */
  public boolean isIdentifier() {
    return (fields & IDENTIFIER) != 0;
  }

  /** 表示当前节点是否是常量节点 */
  public boolean isLiteral() {
    return (fields & LITERAL) != 0;
  }

  public boolean isThisVal() {
    return (fields & THISREF) != 0;
  }

  /** 当前节点是否是操作符节点 */
  public boolean isOperator() {
    return (fields & OPERATOR) != 0;
  }

  /** 当前节点是操作节点并且是指定的运算符 */
  public boolean isOperator(Integer operator) {
    return (fields & OPERATOR) != 0 && operator.equals(literal);
  }

  /** 获取当前节点的操作符，默认情况下为 NOOP，表示不作任何操作 */
  public Integer getOperator() {
    return NOOP;
  }

  protected boolean isCollection() {
    return (fields & COLLECTION) != 0;
  }

  public boolean isAssignment() {
    return ((fields & ASSIGN) != 0);
  }

  public boolean isDeepProperty() {
    return ((fields & DEEP_PROPERTY) != 0);
  }

  public boolean isFQCN() {
    return ((fields & FQCN) != 0);
  }

  public void setAsLiteral() {
    fields |= LITERAL;
  }

  public void setAsFQCNReference() {
    fields |= FQCN;
  }

  public int getCursorPosition() {
    return cursorPosition;
  }

  public void setCursorPosition(int cursorPosition) {
    this.cursorPosition = cursorPosition;
  }

  public boolean isDiscard() {
    return fields != -1 && (fields & DISCARD) != 0;
  }

  public void discard() {
    this.fields |= DISCARD;
  }

  public void strongTyping() {
    this.fields |= STRONG_TYPING;
  }

  public void storePctx() {
    this.fields |= PCTX_STORED;
  }

  public boolean isDebuggingSymbol() {
    return this.fields == -1;
  }

  public int getFields() {
    return fields;
  }

  public Accessor getAccessor() {
    return accessor;
  }

  public boolean canSerializeAccessor() {
    return safeAccessor != null;
  }

  public int getStart() {
    return start;
  }

  public int getOffset() {
    return offset;
  }

  public char[] getExpr() {
    return expr;
  }

  protected ASTNode(ParserContext pCtx) {
    this.pCtx = pCtx;
  }

  public ASTNode(char[] expr, int start, int offset, int fields, ParserContext pCtx) {
    this(pCtx);
    this.fields = fields;
    this.expr = expr;
    this.start = start;
    this.offset = offset;

    setName(expr);
  }

  public String toString() {
    return isOperator() ? "<<" + DebugTools.getOperatorName(getOperator()) + ">>" :
        (PCTX_STORED & fields) != 0 ? nameCache : new String(expr, start, offset);
  }

  protected ClassLoader getClassLoader() {
    return pCtx != null ? pCtx.getClassLoader() : currentThread().getContextClassLoader();
  }
}


