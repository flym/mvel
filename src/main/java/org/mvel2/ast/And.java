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

import org.mvel2.ParserContext;
import org.mvel2.integration.VariableResolverFactory;

import static org.mvel2.util.CompilerTools.expectType;

/**
 * 用于表示 && 的节点表达式,此节点类型只会在二次编译期间才会产生
 * 即在finalizePayload中产生,由op节点转换而来
 */
public class And extends BooleanNode {

  public And(ASTNode left, ASTNode right, boolean strongTyping, ParserContext pCtx) {
    super(pCtx);
    expectType(pCtx, this.left = left, Boolean.class, strongTyping);
    expectType(pCtx, this.right = right, Boolean.class, strongTyping);
  }

  public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
    return (((Boolean) left.getReducedValueAccelerated(ctx, thisValue, factory))
        && ((Boolean) right.getReducedValueAccelerated(ctx, thisValue, factory)));
  }

  /** 不支持解释模式 */
  public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
    throw new RuntimeException("improper use of AST element");
  }

  public String toString() {
    return "(" + left.toString() + " && " + right.toString() + ")";
  }

  public void setRightMost(ASTNode right) {
    And n = this;
    while (n.right != null && n.right instanceof And) {
      n = (And) n.right;
    }
    n.right = right;
  }

  public ASTNode getRightMost() {
    And n = this;
    while (n.right != null && n.right instanceof And) {
      n = (And) n.right;
    }
    return n.right;
  }

  public Class getEgressType() {
    return Boolean.class;
  }
}


