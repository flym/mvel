package org.mvel2.optimizers.impl.refl.nodes;

import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;

import java.lang.reflect.Array;

import static org.mvel2.DataConversion.convert;

/** 表示可以被调用访问的访问器，即通过参数进行访问的访问器，主要有构建函数和方法调用 */
public abstract class InvokableAccessor extends BaseAccessor {
  /** 当前方法声明的参数个数 */
  protected int length;
  /** 当前方法所有的参数列表 */
  protected ExecutableStatement[] parms;
  /** 每个参数的类型信息 */
  protected Class[] parameterTypes;
  /** 表示是否需要进行可变参数处理(默认值false，当失败时转换为true) */
  protected boolean coercionNeeded = false;

  /** 对指定的目标类型参数信息将其转换为正式可用的参数列表(同时对参数进行求值操作) */
  protected Object[] executeAndCoerce(Class[] target, Object elCtx, VariableResolverFactory vars, boolean isVarargs) {
    Object[] values = new Object[length];
    for (int i = 0; i < length && !(isVarargs && i >= length-1); i++) {
      //noinspection unchecked
      values[i] = convert(parms[i].getValue(elCtx, vars), target[i]);
    }
    if (isVarargs) {
      Class<?> componentType = target[length-1].getComponentType();
      Object vararg;
      if (parms == null) {
        vararg = Array.newInstance( componentType, 0 );
      } else {
        vararg = Array.newInstance(componentType, parms.length - length + 1);
        for (int i = length-1; i < parms.length; i++) {
          Array.set(vararg, i - length + 1, convert(parms[i].getValue(elCtx, vars), componentType));
        }
      }
      values[length-1] = vararg;
    }
    return values;
  }

  public Class[] getParameterTypes() {
    return parameterTypes;
  }
}
