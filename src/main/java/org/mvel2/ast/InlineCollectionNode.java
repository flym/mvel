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

import org.mvel2.CompileException;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.optimizers.AccessorOptimizer;
import org.mvel2.optimizers.OptimizerFactory;
import org.mvel2.util.CollectionParser;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mvel2.util.ParseTools.*;

/**
 * 表示一个内部的集合表达式,使用[ 或 {均可以用来表示集合
 *
 * @author Christopher Brock
 */
public class InlineCollectionNode extends ASTNode {
  /** 用于描述数组内部的数据类型及值描述,可能是数组,集合,map的一种 */
  private Object collectionGraph;
  int trailingStart;
  int trailingOffset;

  /** 初始化,但未指定数据类型 */
  public InlineCollectionNode(char[] expr, int start, int end, int fields, ParserContext pctx) {
    super(expr, start, end, fields | INLINE_COLLECTION, pctx);

    if ((fields & COMPILE_IMMEDIATE) != 0) {
      parseGraph(true, null, pctx);
      try {
        AccessorOptimizer ao = OptimizerFactory.getThreadAccessorOptimizer();
        accessor = ao.optimizeCollection(pctx, collectionGraph, egressType, expr, trailingStart, trailingOffset, null, null, null);
        egressType = ao.getEgressType();
      }
      finally {
        OptimizerFactory.clearThreadAccessorOptimizer();
      }
    }
  }

  /** 初始化,并使用已知的内部类型进行解析 */
  public InlineCollectionNode(char[] expr, int start, int end, int fields, Class type, ParserContext pctx) {
    super(expr, start, end, fields | INLINE_COLLECTION, pctx);

    this.egressType = type;

    if ((fields & COMPILE_IMMEDIATE) != 0) {
      try {
        parseGraph(true, type, pctx);
        AccessorOptimizer ao = OptimizerFactory.getThreadAccessorOptimizer();
        accessor = ao.optimizeCollection(pctx, collectionGraph, egressType, expr, this.trailingStart, trailingOffset, null, null, null);
        egressType = ao.getEgressType();
      }
      finally {
        OptimizerFactory.clearThreadAccessorOptimizer();
      }
    }
  }

  public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
    if (accessor != null) {
      return accessor.getValue(ctx, thisValue, factory);
    }
    else {
      try {
        AccessorOptimizer ao = OptimizerFactory.getThreadAccessorOptimizer();
        if (collectionGraph == null) parseGraph(true, null, null);

        accessor = ao.optimizeCollection(pCtx, collectionGraph,
            egressType, expr, trailingStart, trailingOffset, ctx, thisValue, factory);
        egressType = ao.getEgressType();

        return accessor.getValue(ctx, thisValue, factory);
      }
      finally {
        OptimizerFactory.clearThreadAccessorOptimizer();
      }
    }

  }

  public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
    parseGraph(false, egressType, pCtx);

    return execGraph(collectionGraph, egressType, ctx, factory);
  }

  private void parseGraph(boolean compile, Class type, ParserContext pCtx) {
    CollectionParser parser = new CollectionParser();

    //以下因为是以内部集合的方式进行解析,即外层通过[或者{使用,即认为外层表示为集合
    //因此下层的解析返回值肯定为list,然后再单独返回内层对象
    if (type == null) {
      collectionGraph = ((List) parser.parseCollection(expr, start, offset, compile, pCtx)).get(0);
    }
    else {
      collectionGraph = ((List) parser.parseCollection(expr, start, offset, compile, type, pCtx)).get(0);
    }

    trailingStart = parser.getCursor() + 2;
    trailingOffset = offset - (trailingStart - start);

    if (this.egressType == null) this.egressType = collectionGraph.getClass();
  }

  private Object execGraph(Object o, Class type, Object ctx, VariableResolverFactory factory) {
    if (o instanceof List) {
      ArrayList list = new ArrayList(((List) o).size());

      for (Object item : (List) o) {
        list.add(execGraph(item, type, ctx, factory));
      }

      return list;
    }
    else if (o instanceof Map) {
      HashMap map = new HashMap();

      for (Object item : ((Map) o).keySet()) {
        map.put(execGraph(item, type, ctx, factory), execGraph(((Map) o).get(item), type, ctx, factory));
      }

      return map;
    }
    else if (o instanceof Object[]) {
      int dim = 0;

      if (type != null) {
        String nm = type.getName();
        while (nm.charAt(dim) == '[') dim++;
      }
      else {
        type = Object[].class;
        dim = 1;
      }

      Object newArray = Array.newInstance(getSubComponentType(type), ((Object[]) o).length);

      try {
        Class cls = dim > 1 ? findClass(null, repeatChar('[', dim - 1) + "L" + getBaseComponentType(type).getName() + ";", pCtx) : type;

        int c = 0;
        for (Object item : (Object[]) o) {
          Array.set(newArray, c++, execGraph(item, cls, ctx, factory));
        }

        return newArray;
      }
      catch (IllegalArgumentException e) {
        throw new CompileException("type mismatch in array", expr, start, e);
      }
      catch (ClassNotFoundException e) {
        throw new RuntimeException("this error should never throw:" + getBaseComponentType(type).getName(), e);
      }
    }
    else {
      if (type.isArray()) {
        return MVEL.eval((String) o, ctx, factory, getBaseComponentType(type));
      }
      else {
        return MVEL.eval((String) o, ctx, factory);
      }
    }
  }
}
