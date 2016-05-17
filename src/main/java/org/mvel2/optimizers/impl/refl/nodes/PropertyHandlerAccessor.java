package org.mvel2.optimizers.impl.refl.nodes;

import org.mvel2.MVEL;
import org.mvel2.integration.PropertyHandler;
import org.mvel2.integration.VariableResolverFactory;


/** 使用属性处理器来进行属性访问的访问器 */
public class PropertyHandlerAccessor extends BaseAccessor {
  /** 属性名 */
  private String propertyName;
  /** 对应的处理器 */
  private PropertyHandler propertyHandler;
  /** 当前所支持的类型 */
  private Class conversionType;

  public PropertyHandlerAccessor(String propertyName, Class conversionType, PropertyHandler propertyHandler) {
    this.propertyName = propertyName;
    this.conversionType = conversionType;
    this.propertyHandler = propertyHandler;
  }

  public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
    //如果类型与处理类型不一致，则使用原生的处理方式(不再由当前处理)
    if (!conversionType.isAssignableFrom(ctx.getClass())) {
      if (nextNode != null) {
        return nextNode.getValue(MVEL.getProperty(propertyName, ctx), elCtx, variableFactory);
      }
      else {
        return MVEL.getProperty(propertyName, ctx);
      }
    }
    //正常的处理流程
    try {
      if (nextNode != null) {
        return nextNode.getValue(propertyHandler.getProperty(propertyName, ctx, variableFactory), elCtx, variableFactory);
      }
      else {
        return propertyHandler.getProperty(propertyName, ctx, variableFactory);
      }
    }
    catch (Exception e) {
      throw new RuntimeException("unable to access field", e);
    }
  }

  public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
    if (nextNode != null) {
      return nextNode.setValue(propertyHandler.getProperty(propertyName, ctx, variableFactory), ctx, variableFactory, value);
    }
    else {
      return propertyHandler.setProperty(propertyName, ctx, variableFactory, value);
    }
  }

  public Class getKnownEgressType() {
    return Object.class;
  }
}
