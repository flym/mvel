package org.mvel2.integration.impl;

import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.VariableResolverFactory;

import java.util.Set;

/**
 * 全程委托，但期望委托类在创建变量时由自己处理，而不是再继续委托给后续的工厂类
 *
 * @author Mike Brock
 */
public class StackDelimiterResolverFactory extends StackDemarcResolverFactory {
  public StackDelimiterResolverFactory(VariableResolverFactory delegate) {
    super(delegate);
  }

  public VariableResolver createVariable(String name, Object value) {
    VariableResolverFactory delegate = getDelegate();
    VariableResolverFactory nextFactory = delegate.getNextFactory();
    delegate.setNextFactory(null);
    VariableResolver resolver = delegate.createVariable(name, value);
    delegate.setNextFactory(nextFactory);
    return resolver;
  }
}
