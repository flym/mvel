package org.mvel2.optimizers.impl.refl.nodes;

import org.mvel2.compiler.AccessorNode;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.util.PropertyTools;

import java.lang.reflect.Method;

import static org.mvel2.DataConversion.convert;
import static org.mvel2.util.ParseTools.getBestCandidate;

/** 描述一个setter方法的访问器 */
public class SetterAccessor implements AccessorNode {
  private AccessorNode nextNode;
  /** 当前所对应的方法 */
  private final Method method;
  /** 目标参数类型 */
  private Class<?> targetType;
  /** 参数是否是基本类型的 */
  private boolean primitive;

  /** 是否需要可变参数转换 */
  private boolean coercionRequired = false;

  public static final Object[] EMPTY = new Object[0];

  public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
    try {
      if (coercionRequired) {
        return method.invoke(ctx, convert(value, targetType));
      }
      else {
        return method.invoke(ctx, value == null && primitive ? PropertyTools.getPrimitiveInitialValue(targetType) : value);
      }
    }
    catch (IllegalArgumentException e) {
      if (ctx != null && method.getDeclaringClass() != ctx.getClass()) {
        Method o = getBestCandidate(EMPTY, method.getName(), ctx.getClass(), ctx.getClass().getMethods(), true);
        if (o != null) {
          return executeOverrideTarget(o, ctx, value);
        }
      }

      if (!coercionRequired) {
        coercionRequired = true;
        return setValue(ctx, elCtx, variableFactory, value);
      }
      throw new RuntimeException("unable to bind property", e);
    }
    catch (Exception e) {
      throw new RuntimeException("error calling method: " + method.getDeclaringClass().getName() + "." + method.getName(), e);
    }
  }

  public Object getValue(Object ctx, Object elCtx, VariableResolverFactory vars) {
    return null;
  }

  public SetterAccessor(Method method) {
    this.method = method;
    assert method != null;
    primitive = (this.targetType = method.getParameterTypes()[0]).isPrimitive();
  }

  public Method getMethod() {
    return method;
  }

  public AccessorNode setNextNode(AccessorNode nextNode) {
    return this.nextNode = nextNode;
  }

  public AccessorNode getNextNode() {
    return nextNode;
  }

  public String toString() {
    return method.getDeclaringClass().getName() + "." + method.getName();
  }

  public Class getKnownEgressType() {
    return method.getReturnType();
  }

  private Object executeOverrideTarget(Method o, Object ctx, Object value) {
    try {
      return o.invoke(ctx, convert(value, targetType));
    }
    catch (Exception e2) {
      throw new RuntimeException("unable to invoke method", e2);
    }
  }
}
