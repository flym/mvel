/**
 * MVEL (The MVFLEX Expression Language)
 *
 * Copyright (C) 2007 Christopher Brock, MVFLEX/Valhalla Project and the Codehaus
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
 *
 */
package org.mvel2.optimizers.impl.refl.nodes;

import org.mvel2.compiler.AccessorNode;
import org.mvel2.integration.VariableResolverFactory;

/**
 * 对特定的属性this的一个表示
 * <p/>
 * this属性只能在一个表达式的起始点才能使用，在属性名中，如abc.this使用无效
 */
public class ThisValueAccessor implements AccessorNode {
  private AccessorNode nextNode;

  public Object getValue(Object ctx, Object elCtx, VariableResolverFactory vars) {
    if (nextNode != null) {
      return this.nextNode.getValue(elCtx, elCtx, vars);
    }
    else {
      return elCtx;
    }
  }

  public ThisValueAccessor() {
  }

  public AccessorNode getNextNode() {
    return nextNode;
  }

  public AccessorNode setNextNode(AccessorNode nextNode) {
    return this.nextNode = nextNode;
  }

  public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
    if (nextNode == null) throw new RuntimeException("assignment to reserved variable 'this' not permitted");
    return this.nextNode.setValue(elCtx, elCtx, variableFactory, value);

  }

  public Class getKnownEgressType() {
    return Object.class;
  }
}
