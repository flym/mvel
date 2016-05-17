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

import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;

import java.lang.reflect.Constructor;

/** 表示对象访问器(创建对象)(由NewNode对应) */
public class ConstructorAccessor extends InvokableAccessor {
  /** 当前所引用的构建函数 */
  private Constructor constructor;

  public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
    try {
      //默认情况下不作可变参数处理
      if (!coercionNeeded) {
        try {
          if (nextNode != null) {
            return nextNode.getValue(constructor.newInstance(executeAll(elCtx, variableFactory)), elCtx, variableFactory);
          }
          else {
            return constructor.newInstance(executeAll(elCtx, variableFactory));
          }
        }
        catch (IllegalArgumentException e) {
          //默认处理报错，重新设置标记位处理
          coercionNeeded = true;
          return getValue(ctx, elCtx, variableFactory);
        }

      }
      else {
        if (nextNode != null) {
          return nextNode.getValue(constructor.newInstance(executeAndCoerce(parameterTypes, elCtx, variableFactory, constructor.isVarArgs())),
              elCtx, variableFactory);
        }
        else {
          return constructor.newInstance(executeAndCoerce(parameterTypes, elCtx, variableFactory, constructor.isVarArgs()));
        }
      }
    }
    catch (Exception e) {
      throw new RuntimeException("cannot construct object", e);
    }
  }

  public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
    return null;
  }

  /** 通过额外的参数上下文执行所有的参数节点信息,并返回相应的值信息 */
  private Object[] executeAll(Object ctx, VariableResolverFactory vars) {
    if (length == 0) return GetterAccessor.EMPTY;

    Object[] vals = new Object[length];
    for (int i = 0; i < length; i++) {
      vals[i] = parms[i].getValue( ctx, vars);
    }
    return vals;
  }

  public ConstructorAccessor(Constructor constructor, ExecutableStatement[] parms) {
    this.constructor = constructor;
    this.length = (this.parameterTypes = constructor.getParameterTypes()).length;
    this.parms = parms;
  }

  public Class getKnownEgressType() {
    return constructor.getClass();
  }

  public Constructor getConstructor() {
    return constructor;
  }

  public ExecutableStatement[] getParameters() {
    return parms;
  }
}
