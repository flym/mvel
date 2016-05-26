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

package org.mvel2.integration.impl;

import org.mvel2.UnresolveablePropertyException;
import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.VariableResolverFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 一个主要基于下标进行工作的变量工厂，即变量的存储均使用外部已经初始化好的数组来处理
 * 内部对于变量的处理均采用下标来进行，不再使用父类的map变量表处理
 * 此工厂实际上要求外部传入的变量表实际上已经存储了要操作的变量信息，否则后续地对不存在的变量操作将无效
 */
public class IndexedVariableResolverFactory extends BaseVariableResolverFactory {

  public IndexedVariableResolverFactory(String[] varNames, VariableResolver[] resolvers) {
    this.indexedVariableNames = varNames;
    this.indexedVariableResolvers = resolvers;
  }

  public IndexedVariableResolverFactory(String[] varNames, Object[] values) {
    this.indexedVariableNames = varNames;
    this.indexedVariableResolvers = createResolvers(values, varNames.length);
  }

  public IndexedVariableResolverFactory(String[] varNames, Object[] values, VariableResolverFactory nextFactory) {
    this.indexedVariableNames = varNames;
    this.nextFactory = new MapVariableResolverFactory();
    this.nextFactory.setNextFactory(nextFactory);
    this.indexedVariableResolvers = createResolvers(values, varNames.length);

  }

  private static VariableResolver[] createResolvers(Object[] values, int size) {
    VariableResolver[] vr = new VariableResolver[size];
    for (int i = 0; i < size; i++) {
      vr[i] = i >= values.length ? new SimpleValueResolver(null) : new IndexVariableResolver(i, values);
    }
    return vr;
  }

  public VariableResolver createIndexedVariable(int index, String name, Object value) {
    VariableResolver r = indexedVariableResolvers[index];
    r.setValue(value);
    return r;
  }

  public VariableResolver getIndexedVariableResolver(int index) {
    return indexedVariableResolvers[index];
  }

  /** 创建变量解析器，要求此变量必须之前已经在变量中存在了 */
  public VariableResolver createVariable(String name, Object value) {
    VariableResolver vr = getResolver(name);
    if (vr != null) {
      vr.setValue(value);
    }
    return vr;
  }

  public VariableResolver createVariable(String name, Object value, Class<?> type) {
    VariableResolver vr = getResolver(name);
    if (vr != null) {
      vr.setValue(value);
    }
    return vr;

//        if (nextFactory == null) nextFactory = new MapVariableResolverFactory(new HashMap());
//        return nextFactory.createVariable(name, value, type);
  }

  public VariableResolver getVariableResolver(String name) {
    VariableResolver vr = getResolver(name);
    if (vr != null) return vr;
    else if (nextFactory != null) {
      return nextFactory.getVariableResolver(name);
    }

    throw new UnresolveablePropertyException("unable to resolve variable '" + name + "'");
  }

  public boolean isResolveable(String name) {
    return isTarget(name) || (nextFactory != null && nextFactory.isResolveable(name));
  }

  protected VariableResolver addResolver(String name, VariableResolver vr) {
    variableResolvers.put(name, vr);
    return vr;
  }

  /** 通过下标来查找相应的变量解析器 */
  private VariableResolver getResolver(String name) {
    for (int i = 0; i < indexedVariableNames.length; i++) {
      if (indexedVariableNames[i].equals(name)) {
        return indexedVariableResolvers[i];
      }
    }
    return null;
  }

  public boolean isTarget(String name) {
    for (String indexedVariableName : indexedVariableNames) {
      if (indexedVariableName.equals(name)) {
        return true;
      }
    }
    return false;
  }

  public Set<String> getKnownVariables() {
    return new HashSet<String>(Arrays.asList(indexedVariableNames));
  }

  public void clear() {
    // variableResolvers.clear();

  }

  @Override
  public boolean isIndexedFactory() {
    return true;
  }
}