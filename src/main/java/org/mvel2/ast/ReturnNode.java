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

import org.mvel2.Operator;
import org.mvel2.ParserContext;
import org.mvel2.compiler.Accessor;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.StackDemarcResolverFactory;

import static org.mvel2.MVEL.eval;
import static org.mvel2.util.ParseTools.subCompileExpression;

/**
 * 描述一个简单的return x 节点 此节点的作用除对x求值外,还将在当前作用域中设定标记,以提前中止计算并返回
 * @author Christopher Brock
 */
public class ReturnNode extends ASTNode {

  public ReturnNode(char[] expr, int start, int offset, int fields, ParserContext pCtx) {
    super(pCtx);
    this.expr = expr;
    this.start = start;
    this.offset = offset;

    if ((fields & COMPILE_IMMEDIATE) != 0) {
      setAccessor((Accessor) subCompileExpression(expr, start, offset, pCtx));
    }
  }

  public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
    if (accessor == null) {
      setAccessor((Accessor) subCompileExpression(expr, start, offset, pCtx));
    }

    factory.setTiltFlag(true);

    return accessor.getValue(ctx, thisValue, new StackDemarcResolverFactory(factory));
  }

  public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
    factory.setTiltFlag(true);
    return eval(expr, start, offset, ctx, new StackDemarcResolverFactory(factory));
  }

  @Override
  public boolean isOperator() {
    return true;
  }

  @Override
  public Integer getOperator() {
    return Operator.RETURN;
  }

  @Override
  public boolean isOperator(Integer operator) {
    return Operator.RETURN == operator;
  }
}
