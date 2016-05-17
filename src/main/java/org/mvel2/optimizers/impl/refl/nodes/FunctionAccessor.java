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

import org.mvel2.ast.FunctionInstance;
import org.mvel2.compiler.Accessor;
import org.mvel2.integration.VariableResolverFactory;


/** 描述一个函数调用访问器 */
public class FunctionAccessor extends BaseAccessor {
  /** 函数定义实体对象 */
  private FunctionInstance function;
  /** 相应的参数信息 */
  private Accessor[] parameters;

  public FunctionAccessor(FunctionInstance function, Accessor[] parms) {
    this.function = function;
    this.parameters = parms;
  }

  public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
    Object[] parms = null;

    if (parameters != null && parameters.length != 0) {
      parms = new Object[parameters.length];
      for (int i = 0; i < parms.length; i++) {
        parms[i] = parameters[i].getValue(ctx, elCtx, variableFactory);
      }
    }

    if (nextNode != null) {
      return nextNode.getValue(function.call(ctx, elCtx, variableFactory, parms), elCtx, variableFactory);
    }
    else {
      return function.call(ctx, elCtx, variableFactory, parms);
    }
  }

  public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
    throw new RuntimeException("can't write to function");
  }

  public Class getKnownEgressType() {
    return Object.class;
  }
}
