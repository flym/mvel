/**
 * MVEL 2.0
 * Copyright (C) 2007 The Codehaus
 * Mike Brock, Dhanji Prasanna, John Graham, Mark Proctor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel2.compiler;

import org.mvel2.*;
import org.mvel2.ast.*;
import org.mvel2.util.*;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.mvel2.DataConversion.canConvert;
import static org.mvel2.DataConversion.convert;
import static org.mvel2.Operator.PTABLE;
import static org.mvel2.ast.ASTNode.COMPILE_IMMEDIATE;
import static org.mvel2.ast.ASTNode.OPT_SUBTR;
import static org.mvel2.util.CompilerTools.finalizePayload;
import static org.mvel2.util.CompilerTools.signNumber;
import static org.mvel2.util.ParseTools.subCompileExpression;
import static org.mvel2.util.ParseTools.unboxPrimitive;

/**
 * 描述相应的表达式编译器
 * This is the main MVEL compiler.
 */
public class ExpressionCompiler extends AbstractParser {
  /** 当前编译表达式返回的类型 */
  private Class returnType;

  /** 是否只是验证表达式是否合法 */
  private boolean verifyOnly = false;
  /** 表示当前是否正在验证中 */
  private boolean verifying = true;
  /** 是否需要二次优化(即去and操作) */
  private boolean secondPassOptimization = false;

  /** 主要的编译操作，返回编译表达式 */
  public CompiledExpression compile() {
    try {
      this.debugSymbols = pCtx.isDebugSymbols();
      return _compile();
    }
    finally {
      //如果有严重的编译错误，报相应的异常
      //当前很少会有严重错误，因此一般情况下都不会走到这里来，而是直接throw出相应的异常
      if (pCtx.isFatalError()) {
        StringAppender err = new StringAppender();

        Iterator<ErrorDetail> iter = pCtx.getErrorList().iterator();
        ErrorDetail e;
        while (iter.hasNext()) {
          e = iter.next();

          e = ErrorUtil.rewriteIfNeeded(e, expr, cursor);

          if (e.getExpr() != expr) {
            iter.remove();
          }
          else {
            err.append("\n - ").append("(").append(e.getLineNumber()).append(",").append(e.getColumn()).append(")")
                .append(" ").append(e.getMessage());
          }
        }

        //noinspection ThrowFromFinallyBlock
        throw new CompileException("Failed to compileShared: " + pCtx.getErrorList().size()
            + " compilation error(s): " + err.toString(), pCtx.getErrorList(), expr, cursor, pCtx);
      }
    }

  }

  /**
   * 进行实际的编译操作
   * Initiate an in-context compileShared.  This method should really only be called by the internal API.
   *
   * @return compiled expression object
   */
  public CompiledExpression _compile() {
    ASTNode tk;
    ASTNode tkOp;
    ASTNode tkOp2;
    ASTNode tkLA;
    ASTNode tkLA2;

    int op, lastOp = -1;
    cursor = start;

    ASTLinkedList astBuild = new ASTLinkedList();
    stk = new ExecutionStack();
    dStack = new ExecutionStack();
    //开启编译状态
    compileMode = true;

    boolean firstLA;

    try {
      if (verifying) {
        pCtx.initializeTables();
      }

      //首先表示当前为编译阶段，以便各个阶段进行信息编译，即状态值调整
      fields |= COMPILE_IMMEDIATE;

      while ((tk = nextToken()) != null) {
        /**
         * 调试节点？继续处理
         * If this is a debug symbol, just add it and continue.
         */
        if (tk.fields == -1) {
          astBuild.addTokenNode(tk);
          continue;
        }

        /**
         * Record the type of the current node..
         */
        returnType = tk.getEgressType();

        //单独处理子运行程序
        if (tk instanceof Substatement) {
          String key = new String(expr, tk.getStart(), tk.getOffset());
          Map<String, CompiledExpression> cec = pCtx.getCompiledExpressionCache();
          Map<String, Class> rtc = pCtx.getReturnTypeCache();
          CompiledExpression compiled = cec.get(key);
          Class rt = rtc.get(key);
          //子程序单独进行编译,以保证相应的程序都已经成功的进行了解析
          //但实际上没有什么特别的用处,因此subStatement的执行不是通过accessor来保证的,而是由statement处理
          if (compiled == null) {
            ExpressionCompiler subCompiler = new ExpressionCompiler(expr, tk.getStart(), tk.getOffset(), pCtx);
            compiled = subCompiler._compile();
            rt = subCompiler.getReturnType();
            cec.put(key, compiled);
            rtc.put(key, rt);
          }
          tk.setAccessor(compiled);
          returnType = rt;
        }

        /**
         * 这下面的动作在于进行编译期优化，以减少运行期的信息处理
         * 即当前节点是一个常量节点(可能是数字),如果下一个常量类似于 1 + 2 这种,则可以优化
         * This kludge of code is to handle compileShared-time literal reduction.  We need to avoid
         * reducing for certain literals like, 'this', ternary and ternary else.
         */
        if (!verifyOnly && tk.isLiteral()) {
          if (literalOnly == -1) literalOnly = 1;

          //下一个符号为操作符节点
          if ((tkOp = nextTokenSkipSymbols()) != null && tkOp.isOperator()
              && !tkOp.isOperator(Operator.TERNARY) && !tkOp.isOperator(Operator.TERNARY_ELSE)) {

            /**
             * 如果操作符之后的节点是常量节点 则表示可以编译期优化
             * If the next token is ALSO a literal, then we have a candidate for a compileShared-time literal
             * reduction.
             */
            if ((tkLA = nextTokenSkipSymbols()) != null && tkLA.isLiteral()
                && tkOp.getOperator() < 34 && ((lastOp == -1
                || (lastOp < PTABLE.length && PTABLE[lastOp] < PTABLE[tkOp.getOperator()])))) {
              //先将其推入栈中,以方便计算处理,采用后缀表示法,以方便进行栈式判断和处理
              stk.push(tk.getLiteralValue(), tkLA.getLiteralValue(), op = tkOp.getOperator());

              /**
               * 如果是算术运算,则表示是基于的数学处理,则需要根据当前栈以及节点的进一步信息进行处理
               * Reduce the token now.
               */
              if (isArithmeticOperator(op)) {
                if (!compileReduce(op, astBuild)) continue;
              }
              //这里表示不是基本运算,则进行其它处理,比如 1 < 2这种或者是 true || false这种操作
              else {
                reduce();
              }

              firstLA = true;

              /**
               * Now we need to check to see if this is a continuing reduction.
               */
              while ((tkOp2 = nextTokenSkipSymbols()) != null) {
                if (isBooleanOperator(tkOp2.getOperator())) {
                  astBuild.addTokenNode(new LiteralNode(stk.pop(), pCtx), verify(pCtx, tkOp2));
                  break;
                }
                else if ((tkLA2 = nextTokenSkipSymbols()) != null) {

                  if (tkLA2.isLiteral()) {
                    stk.push(tkLA2.getLiteralValue(), op = tkOp2.getOperator());

                    if (isArithmeticOperator(op)) {
                      compileReduce(op, astBuild);
                    }
                    else {
                      reduce();
                    }
                  }
                  else {
                    /**
                     * A reducable line of literals has ended.  We must now terminate here and
                     * leave the rest to be determined at runtime.
                     */
                    if (!stk.isEmpty()) {
                      astBuild.addTokenNode(new LiteralNode(getStackValueResult(), pCtx));
                    }

                    astBuild.addTokenNode(new OperatorNode(tkOp2.getOperator(), expr, st, pCtx), verify(pCtx, tkLA2));
                    break;
                  }

                  firstLA = false;
                  literalOnly = 0;
                }
                else {
                  if (firstLA) {
                    /**
                     * There are more tokens, but we can't reduce anymore.  So
                     * we create a reduced token for what we've got.
                     */
                    astBuild.addTokenNode(new LiteralNode(getStackValueResult(), pCtx));
                  }
                  else {
                    /**
                     * We have reduced additional tokens, but we can't reduce
                     * anymore.
                     */
                    astBuild.addTokenNode(new LiteralNode(getStackValueResult(), pCtx), tkOp2);

                    if (tkLA2 != null) astBuild.addTokenNode(verify(pCtx, tkLA2));
                  }

                  break;
                }
              }

              /**
               * If there are no more tokens left to parse, we check to see if
               * we've been doing any reducing, and if so we create the token
               * now.
               */
              if (!stk.isEmpty())
                astBuild.addTokenNode(new LiteralNode(getStackValueResult(), pCtx));

              continue;
            }
            else {
              astBuild.addTokenNode(verify(pCtx, tk), verify(pCtx, tkOp));
              if (tkLA != null) astBuild.addTokenNode(verify(pCtx, tkLA));
              continue;
            }
          }
          else if (tkOp != null && !tkOp.isOperator() && !(tk.getLiteralValue() instanceof Class)) {
            throw new CompileException("unexpected token: " + tkOp.getName(), expr, tkOp.getStart());
          }
          else {
            literalOnly = 0;
            astBuild.addTokenNode(verify(pCtx, tk));
            if (tkOp != null) astBuild.addTokenNode(verify(pCtx, tkOp));
            continue;
          }
        }
        else {
          if (tk.isOperator()) {
            lastOp = tk.getOperator();
          }
          else {
            literalOnly = 0;
          }
        }

        astBuild.addTokenNode(verify(pCtx, tk));
      }

      astBuild.finish();

      if (verifying && !verifyOnly) {
        pCtx.processTables();
      }

      if (!stk.isEmpty()) {
        throw new CompileException("COMPILE ERROR: non-empty stack after compileShared.", expr, cursor);
      }

      if (!verifyOnly) {
        return new CompiledExpression(finalizePayload(astBuild, secondPassOptimization, pCtx), pCtx.getSourceFile(), returnType, pCtx.getParserConfiguration(), literalOnly == 1);
      }
      else {
        try {
          returnType = CompilerTools.getReturnType(astBuild, pCtx.isStrongTyping());
        } catch (RuntimeException e) {
          throw new CompileException(e.getMessage(), expr, st, e);
        }
        return null;
      }
    }
    catch (NullPointerException e) {
      throw new CompileException("not a statement, or badly formed structure", expr, st, e);
    }
    catch (CompileException e) {
      throw ErrorUtil.rewriteIfNeeded(e, expr, st);
    }
    catch (Throwable e) {
      if (e instanceof RuntimeException) throw (RuntimeException) e;
      else {
        throw new CompileException(e.getMessage(), expr, st, e);
      }
    }
  }

  private Object getStackValueResult() {
    return (fields & OPT_SUBTR) == 0 ? stk.pop() : signNumber(stk.pop());
  }

  /** 根据当前操作码以及节点信息进行进一步处理 */
  private boolean compileReduce(int opCode, ASTLinkedList astBuild) {
    switch (arithmeticFunctionReduction(opCode)) {
      case -1:
        /**
         * The reduction failed because we encountered a non-literal,
         * so we must now back out and cleanup.
         */

        stk.xswap_op();

        astBuild.addTokenNode(new LiteralNode(stk.pop(), pCtx));
        astBuild.addTokenNode(
            (OperatorNode) splitAccumulator.pop(),
            verify(pCtx, (ASTNode) splitAccumulator.pop())
        );
        return false;
      case -2:
        /**
         * Back out completely, pull everything back off the stack and add the instructions
         * to the output payload as they are.
         */

        LiteralNode rightValue = new LiteralNode(stk.pop(), pCtx);
        OperatorNode operator = new OperatorNode((Integer) stk.pop(), expr, st, pCtx);

        astBuild.addTokenNode(new LiteralNode(stk.pop(), pCtx), operator);
        astBuild.addTokenNode(rightValue, (OperatorNode) splitAccumulator.pop());
        astBuild.addTokenNode(verify(pCtx, (ASTNode) splitAccumulator.pop()));
    }
    return true;
  }

  private static boolean isBooleanOperator(int operator) {
    return operator == Operator.AND || operator == Operator.OR || operator == Operator.TERNARY || operator == Operator.TERNARY_ELSE;
  }

  /** 验证节点并设置相应的信息，同时返回该节点 */
  protected ASTNode verify(ParserContext pCtx, ASTNode tk) {
    if (tk.isOperator() && (tk.getOperator().equals(Operator.AND) || tk.getOperator().equals(Operator.OR))) {
      secondPassOptimization = true;
    }
    if (tk.isDiscard() || tk.isOperator()) {
      return tk;
    }
    else if (tk.isLiteral()) {
      /**
       * 如果当前节点是常量节点(可能为ASTNode，则强制转换为LiteralNode节点，以避免再操作astNode)
       * Convert literal values from the default ASTNode to the more-efficient LiteralNode.
       */
      if ((fields & COMPILE_IMMEDIATE) != 0 && tk.getClass() == ASTNode.class) {
        return new LiteralNode(tk.getLiteralValue(), pCtx);
      }
      else {
        return tk;
      }
    }

    if (verifying) {
      if (tk.isIdentifier()) {
        PropertyVerifier propVerifier = new PropertyVerifier(expr, tk.getStart(), tk.getOffset(), pCtx);

        if (tk instanceof Union) {
          propVerifier.setCtx(((Union) tk).getLeftEgressType());
          tk.setEgressType(returnType = propVerifier.analyze());
        }
        else {
          tk.setEgressType(returnType = propVerifier.analyze());

          if (propVerifier.isFqcn()) {
            tk.setAsFQCNReference();
          }

          if (propVerifier.isClassLiteral()) {
            return new LiteralNode(returnType, pCtx);
          }
          if (propVerifier.isInput()) {
            pCtx.addInput(tk.getAbsoluteName(), propVerifier.isDeepProperty() ? Object.class : returnType);
          }

          if (!propVerifier.isMethodCall() && !returnType.isEnum() && !pCtx.isOptimizerNotified() &&
                  pCtx.isStrongTyping() && !pCtx.isVariableVisible(tk.getAbsoluteName()) && !tk.isFQCN()) {
            throw new CompileException("no such identifier: " + tk.getAbsoluteName(), expr, tk.getStart());
          }
        }
      }
      else if (tk.isAssignment()) {
        Assignment a = (Assignment) tk;

        if (a.getAssignmentVar() != null) {
          //    pCtx.makeVisible(a.getAssignmentVar());

          PropertyVerifier propVerifier = new PropertyVerifier(a.getAssignmentVar(), pCtx);
          tk.setEgressType(returnType = propVerifier.analyze( ));

          if (!a.isNewDeclaration() && propVerifier.isResolvedExternally()) {
            pCtx.addInput(tk.getAbsoluteName(), returnType);
          }

          ExecutableStatement c = (ExecutableStatement) subCompileExpression(expr, tk.getStart(),
              tk.getOffset(), pCtx);

          if (pCtx.isStrictTypeEnforcement()) {
            /**
             * If we're using strict type enforcement, we need to see if this coercion can be done now,
             * or fail epicly.
             */
            if (!returnType.isAssignableFrom(c.getKnownEgressType()) && c.isLiteralOnly()) {
              if (canConvert(c.getKnownEgressType(), returnType)) {
                /**
                 * We convert the literal to the proper type.
                 */
                try {
                  a.setValueStatement(new ExecutableLiteral(convert(c.getValue(null, null), returnType)));
                  return tk;
                }
                catch (Exception e) {
                  // fall through.
                }
              }
              else if (returnType.isPrimitive()
                  && unboxPrimitive(c.getKnownEgressType()).equals(returnType)) {
                /**
                 * We ignore boxed primitive cases, since MVEL does not recognize primitives.
                 */
                return tk;
              }

              throw new CompileException(
                  "cannot assign type " + c.getKnownEgressType().getName()
                      + " to " + returnType.getName(), expr, st);
            }
          }
        }
      }
      else if (tk instanceof NewObjectNode) {
        // this is a bit of a hack for now.
        NewObjectNode n = (NewObjectNode) tk;
        List<char[]> parms = ParseTools.parseMethodOrConstructor(tk.getNameAsArray());
        if (parms != null) {
          for (char[] p : parms) {
            MVEL.analyze(p, pCtx);
          }
        }
      }
      returnType = tk.getEgressType();
    }

    if (!tk.isLiteral() && tk.getClass() == ASTNode.class && (tk.getFields() & ASTNode.ARRAY_TYPE_LITERAL) == 0) {
      if (pCtx.isStrongTyping()) tk.strongTyping();
      tk.storePctx();
      tk.storeInLiteralRegister(pCtx);
    }

    return tk;
  }

  public ExpressionCompiler(String expression) {
    setExpression(expression);
  }

  public ExpressionCompiler(String expression, boolean verifying) {
    setExpression(expression);
    this.verifying = verifying;
  }

  public ExpressionCompiler(char[] expression) {
    setExpression(expression);
  }

  public ExpressionCompiler(String expression, ParserContext ctx) {
    setExpression(expression);
    this.pCtx = ctx;
  }

  public ExpressionCompiler(char[] expression, int start, int offset) {
    this.expr = expression;
    this.start = start;
    this.end = start + offset;
    this.end = trimLeft(this.end);
    this.length = this.end - start;
  }

  public ExpressionCompiler(String expression, int start, int offset, ParserContext ctx) {
    this.expr = expression.toCharArray();
    this.start = start;
    this.end = start + offset;
    this.end = trimLeft(this.end);
    this.length = this.end - start;
    this.pCtx = ctx;
  }

  public ExpressionCompiler(char[] expression, int start, int offset, ParserContext ctx) {
    this.expr = expression;
    this.start = start;
    this.end = start + offset;
    this.end = trimLeft(this.end);
    this.length = this.end - start;
    this.pCtx = ctx;
  }

  public ExpressionCompiler(char[] expression, ParserContext ctx) {
    setExpression(expression);
    this.pCtx = ctx;
  }

  public boolean isVerifying() {
    return verifying;
  }

  public void setVerifying(boolean verifying) {
    this.verifying = verifying;
  }

  public boolean isVerifyOnly() {
    return verifyOnly;
  }

  public void setVerifyOnly(boolean verifyOnly) {
    this.verifyOnly = verifyOnly;
  }

  public Class getReturnType() {
    return returnType;
  }

  public void setReturnType(Class returnType) {
    this.returnType = returnType;
  }

  public ParserContext getParserContextState() {
    return pCtx;
  }

  public boolean isLiteralOnly() {
    return literalOnly == 1;
  }
}
