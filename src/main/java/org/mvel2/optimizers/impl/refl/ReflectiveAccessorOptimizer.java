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
package org.mvel2.optimizers.impl.refl;

import org.mvel2.CompileException;
import org.mvel2.MVEL;
import org.mvel2.OptimizationFailure;
import org.mvel2.ParserContext;
import org.mvel2.PropertyAccessException;
import org.mvel2.ast.FunctionInstance;
import org.mvel2.ast.TypeDescriptor;
import org.mvel2.compiler.Accessor;
import org.mvel2.compiler.AccessorNode;
import org.mvel2.compiler.ExecutableLiteral;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.compiler.PropertyVerifier;
import org.mvel2.integration.GlobalListenerFactory;
import org.mvel2.integration.PropertyHandler;
import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.optimizers.AbstractOptimizer;
import org.mvel2.optimizers.AccessorOptimizer;
import org.mvel2.optimizers.impl.refl.collection.ArrayCreator;
import org.mvel2.optimizers.impl.refl.collection.ExprValueAccessor;
import org.mvel2.optimizers.impl.refl.collection.ListCreator;
import org.mvel2.optimizers.impl.refl.collection.MapCreator;
import org.mvel2.optimizers.impl.refl.nodes.ArrayAccessor;
import org.mvel2.optimizers.impl.refl.nodes.ArrayAccessorNest;
import org.mvel2.optimizers.impl.refl.nodes.ArrayLength;
import org.mvel2.optimizers.impl.refl.nodes.ConstructorAccessor;
import org.mvel2.optimizers.impl.refl.nodes.DynamicFieldAccessor;
import org.mvel2.optimizers.impl.refl.nodes.DynamicFunctionAccessor;
import org.mvel2.optimizers.impl.refl.nodes.FieldAccessor;
import org.mvel2.optimizers.impl.refl.nodes.FieldAccessorNH;
import org.mvel2.optimizers.impl.refl.nodes.FunctionAccessor;
import org.mvel2.optimizers.impl.refl.nodes.GetterAccessor;
import org.mvel2.optimizers.impl.refl.nodes.GetterAccessorNH;
import org.mvel2.optimizers.impl.refl.nodes.IndexedCharSeqAccessor;
import org.mvel2.optimizers.impl.refl.nodes.IndexedCharSeqAccessorNest;
import org.mvel2.optimizers.impl.refl.nodes.IndexedVariableAccessor;
import org.mvel2.optimizers.impl.refl.nodes.ListAccessor;
import org.mvel2.optimizers.impl.refl.nodes.ListAccessorNest;
import org.mvel2.optimizers.impl.refl.nodes.MapAccessor;
import org.mvel2.optimizers.impl.refl.nodes.MapAccessorNest;
import org.mvel2.optimizers.impl.refl.nodes.MethodAccessor;
import org.mvel2.optimizers.impl.refl.nodes.MethodAccessorNH;
import org.mvel2.optimizers.impl.refl.nodes.Notify;
import org.mvel2.optimizers.impl.refl.nodes.NullSafe;
import org.mvel2.optimizers.impl.refl.nodes.PropertyHandlerAccessor;
import org.mvel2.optimizers.impl.refl.nodes.SetterAccessor;
import org.mvel2.optimizers.impl.refl.nodes.StaticReferenceAccessor;
import org.mvel2.optimizers.impl.refl.nodes.StaticVarAccessor;
import org.mvel2.optimizers.impl.refl.nodes.StaticVarAccessorNH;
import org.mvel2.optimizers.impl.refl.nodes.ThisValueAccessor;
import org.mvel2.optimizers.impl.refl.nodes.Union;
import org.mvel2.optimizers.impl.refl.nodes.VariableAccessor;
import org.mvel2.optimizers.impl.refl.nodes.WithAccessor;
import org.mvel2.util.ArrayTools;
import org.mvel2.util.ErrorUtil;
import org.mvel2.util.MethodStub;
import org.mvel2.util.NullType;
import org.mvel2.util.ParseTools;
import org.mvel2.util.PropertyTools;
import org.mvel2.util.StringAppender;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

import static java.lang.Integer.parseInt;
import static java.lang.Thread.currentThread;
import static java.lang.reflect.Array.getLength;
import static org.mvel2.DataConversion.canConvert;
import static org.mvel2.DataConversion.convert;
import static org.mvel2.MVEL.eval;
import static org.mvel2.ast.TypeDescriptor.getClassReference;
import static org.mvel2.integration.GlobalListenerFactory.*;
import static org.mvel2.integration.PropertyHandlerFactory.*;
import static org.mvel2.util.CompilerTools.expectType;
import static org.mvel2.util.ParseTools.*;
import static org.mvel2.util.PropertyTools.getFieldOrAccessor;
import static org.mvel2.util.PropertyTools.getFieldOrWriteAccessor;
import static org.mvel2.util.ReflectionUtil.toNonPrimitiveType;
import static org.mvel2.util.Varargs.normalizeArgsForVarArgs;
import static org.mvel2.util.Varargs.paramTypeVarArgsSafe;

public class ReflectiveAccessorOptimizer extends AbstractOptimizer implements AccessorOptimizer {
  /** 当前优化器所优化时的第一个节点(首节点) */
  private AccessorNode rootNode;
  /** 当前在进行优化处理时所对应的当前节点 */
  private AccessorNode currNode;

  /** 引用实际的上下文对象 */
  private Object ctx;
  /** 引用当前对象 */
  private Object thisRef;
  /** 当前处理结束临时存储的值 */
  private Object val;

  private VariableResolverFactory variableFactory;

  private static final int DONE = -1;

  private static final Object[] EMPTYARG = new Object[0];
  private static final Class[] EMPTYCLS = new Class[0];

  /** 表示当前处理属性刚开始(即处理位置在首位) */
  private boolean first = true;

  /** 当前处理对象的类型信息(入参类型) */
  private Class ingressType;
  /** 当前处理对象的返回类型 */
  private Class returnType;

  public ReflectiveAccessorOptimizer() {
  }

  public void init() {
  }

  private ReflectiveAccessorOptimizer(ParserContext pCtx, char[] property, int start, int offset, Object ctx,
                                      Object thisRef, VariableResolverFactory variableFactory) {
    super(pCtx);
    this.expr = property;
    this.start = start;
    this.length = property != null ? offset : start;
    this.end = start + length;
    this.ctx = ctx;
    this.variableFactory = variableFactory;
    this.thisRef = thisRef;
  }

  public Accessor optimizeAccessor(ParserContext pCtx, char[] property, int start, int offset, Object ctx, Object thisRef,
                                   VariableResolverFactory factory, boolean root, Class ingressType) {
    this.rootNode = this.currNode = null;
    this.expr = property;
    this.start = start;
    this.end = start + offset;
    this.length = end - start;


    this.first = true;
    this.ctx = ctx;
    this.thisRef = thisRef;
    this.variableFactory = factory;
    this.ingressType = ingressType;

    this.pCtx = pCtx;

    return compileGetChain();
  }

  public Accessor optimizeSetAccessor(ParserContext pCtx, char[] property, int start, int offset, Object ctx,
                                      Object thisRef, VariableResolverFactory factory, boolean rootThisRef,
                                      Object value, Class ingressType) {
    this.rootNode = this.currNode = null;
    this.expr = property;
    this.start = start;
    this.first = true;

    this.length = start + offset;
    this.ctx = ctx;
    this.thisRef = thisRef;
    this.variableFactory = factory;
    this.ingressType = ingressType;

    char[] root = null;

    int split = findLastUnion();

    PropertyVerifier verifier = new PropertyVerifier(property, this.pCtx = pCtx);

    if (split != -1) {
      root = subset(property, 0, split++);
      //todo: must use the property verifier.
      property = subset(property, split, property.length - split);
    }

    if (root != null) {
      this.length = end = (this.expr = root).length;

      compileGetChain();
      ctx = this.val;
    }

    if (ctx == null) {
      throw new PropertyAccessException("could not access property: " + new String(property, this.start, Math.min(length, property.length)) + "; parent is null: "
          + new String(expr), expr, this.start, pCtx);
    }

    try {
      this.length = end = (this.expr = property).length;
      int st;
      this.cursor = st = 0
      ;

      skipWhitespace();

      if (collection) {
        st = cursor;

        if (cursor == end)
          throw new PropertyAccessException("unterminated '['", expr, this.start, pCtx);

        if (scanTo(']'))
          throw new PropertyAccessException("unterminated '['", expr, this.start, pCtx);

        String ex = new String(property, st, cursor - st);

        if (ctx instanceof Map) {
          if (MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING && hasPropertyHandler(Map.class)) {
            propHandlerSet(ex, ctx, Map.class, value);
          }
          else {
            //noinspection unchecked
            ((Map) ctx).put(eval(ex, ctx, variableFactory), convert(value, returnType = verifier.analyze()));

            addAccessorNode(new MapAccessorNest(ex, returnType));
          }

          return rootNode;
        }
        else if (ctx instanceof List) {
          if (MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING && hasPropertyHandler(List.class)) {
            propHandlerSet(ex, ctx, List.class, value);
          }
          else {
            //noinspection unchecked
            ((List) ctx).set(eval(ex, ctx, variableFactory, Integer.class),
                convert(value, returnType = verifier.analyze()));

            addAccessorNode(new ListAccessorNest(ex, returnType));
          }

          return rootNode;
        }
        else if (MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING && hasPropertyHandler(ctx.getClass())) {
          propHandlerSet(ex, ctx, ctx.getClass(), value);
          return rootNode;
        }
        else if (ctx.getClass().isArray()) {
          if (MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING && hasPropertyHandler(Array.class)) {
            propHandlerSet(ex, ctx, Array.class, value);
          }
          else {
            //noinspection unchecked
            Array.set(ctx, eval(ex, ctx, variableFactory, Integer.class),
                convert(value, getBaseComponentType(ctx.getClass())));
            addAccessorNode(new ArrayAccessorNest(ex));
          }
          return rootNode;
        }
        else {
          throw new PropertyAccessException("cannot bind to collection property: " + new String(property) +
              ": not a recognized collection type: " + ctx.getClass(), expr, this.st, pCtx);
        }
      }
      else if (MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING && hasPropertyHandler(ctx.getClass())) {
        propHandlerSet(new String(property), ctx, ctx.getClass(), value);
        return rootNode;
      }

      String tk = new String(property, 0, length).trim();

      if (hasSetListeners()) {
        notifySetListeners(ctx, tk, variableFactory, value);
        addAccessorNode(new Notify(tk));
      }

      Member member = getFieldOrWriteAccessor(ctx.getClass(), tk, value == null ? null : ingressType);

      if (member instanceof Field) {
        Field fld = (Field) member;

        if (value != null && !fld.getType().isAssignableFrom(value.getClass())) {
          if (!canConvert(fld.getType(), value.getClass())) {
            throw new CompileException("cannot convert type: "
                + value.getClass() + ": to " + fld.getType(), this.expr, this.start);
          }

          fld.set(ctx, convert(value, fld.getType()));
          addAccessorNode(new DynamicFieldAccessor(fld));
        }
        else if (value == null && fld.getType().isPrimitive()) {
          fld.set(ctx, PropertyTools.getPrimitiveInitialValue(fld.getType()));
          addAccessorNode(new FieldAccessor(fld));
        }
        else {
          fld.set(ctx, value);
          addAccessorNode(new FieldAccessor(fld));
        }
      }
      else if (member != null) {
        Method meth = (Method) member;

        if (value != null && !meth.getParameterTypes()[0].isAssignableFrom(value.getClass())) {
          if (!canConvert(meth.getParameterTypes()[0], value.getClass())) {
            throw new CompileException("cannot convert type: "
                + value.getClass() + ": to " + meth.getParameterTypes()[0], this.expr, this.start);
          }

          meth.invoke(ctx, convert(value, meth.getParameterTypes()[0]));
        }
        else if (value == null && meth.getParameterTypes()[0].isPrimitive()) {
          meth.invoke(ctx, PropertyTools.getPrimitiveInitialValue(meth.getParameterTypes()[0]));
        }
        else {
          meth.invoke(ctx, value);
        }

        addAccessorNode(new SetterAccessor(meth));
      }
      else if (ctx instanceof Map) {
        //noinspection unchecked
        ((Map) ctx).put(tk, value);

        addAccessorNode(new MapAccessor(tk));
      }
      else {
        throw new PropertyAccessException("could not access property (" + tk + ") in: " + ingressType.getName()
            , this.expr, this.start, pCtx);
      }
    }
    catch (InvocationTargetException e) {
      throw new PropertyAccessException("could not access property: " + new String(property), this.expr, st, e, pCtx);
    }
    catch (IllegalAccessException e) {
      throw new PropertyAccessException("could not access property: " + new String(property), this.expr, st, e, pCtx);
    }
    catch (IllegalArgumentException e) {
      throw new PropertyAccessException("error binding property: " + new String(property) + " (value <<" + value + ">>::"
          + (value == null ? "null" : value.getClass().getCanonicalName()) + ")", this.expr, st, e, pCtx);
    }

    return rootNode;
  }

  /** 编译get访问处理链 */
  private Accessor compileGetChain() {
    Object curr = ctx;
    cursor = start;

    try {
      //第1个if调用
      if (!MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING) {
        while (cursor < end) {
          switch (nextSubToken()) {
            //属性访问
            case BEAN:
              curr = getBeanProperty(curr, capture());
              break;
            //方法调用
            case METH:
              curr = getMethod(curr, capture());
              break;
            //集合信息调用
            case COL:
              curr = getCollectionProperty(curr, capture());
              break;
            case WITH:
              curr = getWithProperty(curr);
              break;
            case DONE:
              break;
          }

          first = false;
          if (curr != null) returnType = curr.getClass();
          if (cursor < end) {
            if (nullSafe) {
              int os = expr[cursor] == '.' ? 1 : 0;
              addAccessorNode(new NullSafe(expr, cursor + os, length - cursor - os, pCtx));
              if (curr == null) break;
            }
            if (curr == null) throw new NullPointerException();
          }
          staticAccess = false;
        }

      }
      else {
        while (cursor < end) {
          switch (nextSubToken()) {
            case BEAN:
              curr = getBeanPropertyAO(curr, capture());
              break;
            case METH:
              curr = getMethod(curr, capture());
              break;
            case COL:
              curr = getCollectionPropertyAO(curr, capture());
              break;
            case WITH:
              curr = getWithProperty(curr);
              break;
            case DONE:
              break;
          }

          first = false;
          if (curr != null) returnType = curr.getClass();
          if (cursor < end) {
            if (nullSafe) {
              int os = expr[cursor] == '.' ? 1 : 0;
              addAccessorNode(new NullSafe(expr, cursor + os, length - cursor - os, pCtx));
              if (curr == null) break;
            }
            if (curr == null) throw new NullPointerException();
          }
          staticAccess = false;
        }
      }

      val = curr;
      return rootNode;
    }
    catch (InvocationTargetException e) {
      if (MVEL.INVOKED_METHOD_EXCEPTIONS_BUBBLE) {
        if (e.getTargetException() instanceof RuntimeException) {
          throw (RuntimeException) e.getTargetException();
        }
        else {
          throw new RuntimeException(e);
        }
      }

      throw new PropertyAccessException(new String(expr, start, length) + ": "
          + e.getTargetException().getMessage(), this.expr, this.st, e, pCtx);
    }
    catch (IllegalAccessException e) {
      throw new PropertyAccessException(new String(expr, start, length) + ": "
          + e.getMessage(), this.expr, this.st, e, pCtx);
    }
    catch (IndexOutOfBoundsException e) {
      throw new PropertyAccessException(new String(expr, start, length)
          + ": array index out of bounds.", this.expr, this.st, e, pCtx);
    }
    catch (CompileException e) {
      throw e;
    }
    catch (NullPointerException e) {
      throw new PropertyAccessException("null pointer: " + new String(expr, start, length), this.expr, this.st, e, pCtx);
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new CompileException(e.getMessage(), this.expr, st, e);
    }
  }

  /** 设置起相应的节点链 */
  private void addAccessorNode(AccessorNode an) {
    if (rootNode == null)
      rootNode = currNode = an;
    else {
      currNode = currNode.setNextNode(an);
    }
  }

  private Object getWithProperty(Object ctx) {
    currType = null;
    String root = start == cursor ? null : new String(expr, start, cursor - 1).trim();

    int st = cursor + 1;
    cursor = balancedCaptureWithLineAccounting(expr, cursor, end, '{', pCtx);

    WithAccessor wa = new WithAccessor(pCtx, root, expr, st, cursor++ - st, ingressType);
    addAccessorNode(wa);
    return wa.getValue(ctx, thisRef, variableFactory);
  }

  private Object getBeanPropertyAO(Object ctx, String property)
      throws Exception {

    if (GlobalListenerFactory.hasGetListeners()) {
      notifyGetListeners(ctx, property, variableFactory);
      addAccessorNode(new Notify(property));
    }

    if (ctx != null && hasPropertyHandler(ctx.getClass())) return propHandler(property, ctx, ctx.getClass());

    return getBeanProperty(ctx, property);
  }

  /** 读取属性值信息 */
  private Object getBeanProperty(Object ctx, String property) throws Exception {
    if ((pCtx == null ? currType : pCtx.getVarOrInputTypeOrNull(property)) == Object.class
        && !pCtx.isStrongTyping()) {
      currType = null;
    }

    //只有在初始处理时才需要处理this属性
    if (first) {
      //this属性处理
      if ("this".equals(property)) {
        addAccessorNode(new ThisValueAccessor());
        return this.thisRef;
      }
      //如果变量解析器能够解析此变量，则使用变量解析器，变量解析器敢只有在first时才能解析，
      //如a.b中，只有a才可能在变量工厂中使用,b是不能使用的
      else if (variableFactory != null && variableFactory.isResolveable(property)) {


        //2种处理方式，一种是基于下标处理，另一种是基于属性直接映射处理
        if (variableFactory.isIndexedFactory() && variableFactory.isTarget(property)) {
          int idx;
          addAccessorNode(new IndexedVariableAccessor(idx = variableFactory.variableIndexOf(property)));

          VariableResolver vr = variableFactory.getIndexedVariableResolver(idx);
          if (vr == null) {
            variableFactory.setIndexedVariableResolver(idx, variableFactory.getVariableResolver(property));
          }

          return variableFactory.getIndexedVariableResolver(idx).getValue();
        }
        else {
          addAccessorNode(new VariableAccessor(property));

          return variableFactory.getVariableResolver(property).getValue();
        }
      }
    }

    boolean classRef = false;//当前对象是否是类型引用

    Class<?> cls;
    //当前对象为class，并且是否支持T.class 这种处理
    if (ctx instanceof Class) {
      if (MVEL.COMPILER_OPT_SUPPORT_JAVA_STYLE_CLASS_LITERALS
          && "class".equals(property)) {
        return ctx;
      }

      cls = (Class<?>) ctx;

      classRef = true;
    }
    else if (ctx != null) {
      cls = ctx.getClass();
    }
    else {
      cls = currType;
    }

    //如果有属性解析器，则交由属性处理器来处理
    if (hasPropertyHandler(cls)) {
      PropertyHandlerAccessor acc = new PropertyHandlerAccessor(property, cls, getPropertyHandler(cls));
      addAccessorNode(acc);
      return acc.getValue(ctx, thisRef, variableFactory);
    }

    //通过成员信息来处理
    Member member = cls != null ? getFieldOrAccessor(cls, property) : null;

    //如果当前成员找到了，但当前处理对象为类类型，并且当前成员并不是静态成员，
    //则表示找到的成员不能满足要求，则设置为null，避免错误处理
    if (member != null && classRef && (member.getModifiers() & Modifier.STATIC) == 0) {
      member = null;
    }

    Object o;

    //处理getter方法
    if (member instanceof Method) {
      try {
        //正常情况下，采用无参方法调用处理
        o = ctx != null ? ((Method) member).invoke(ctx, EMPTYARG) : null;

        if (hasNullPropertyHandler()) {
          addAccessorNode(new GetterAccessorNH((Method) member, getNullPropertyHandler()));
          if (o == null) o = getNullPropertyHandler().getProperty(member.getName(), ctx, variableFactory);
        }
        else {
          addAccessorNode(new GetterAccessor((Method) member));
        }
      }
      catch (IllegalAccessException e) {
        //这里表示权限受限，则尝试使用不受限的方法来调用
        Method iFaceMeth = determineActualTargetMethod((Method) member);

        if (iFaceMeth == null)
          throw new PropertyAccessException("could not access field: "
              + cls.getName() + "." + property, this.expr, this.start, pCtx);

        o = iFaceMeth.invoke(ctx, EMPTYARG);

        if (hasNullPropertyHandler()) {
          addAccessorNode(new GetterAccessorNH((Method) member, getNullMethodHandler()));
          if (o == null) o = getNullMethodHandler().getProperty(member.getName(), ctx, variableFactory);
        }
        else {
          addAccessorNode(new GetterAccessor(iFaceMeth));
        }
      }
      catch (IllegalArgumentException e) {
        if (member.getDeclaringClass().equals(ctx)) {
          try {
            Class c = Class.forName(member.getDeclaringClass().getName() + "$" + property);

            throw new CompileException("name collision between innerclass: " + c.getCanonicalName()
                + "; and bean accessor: " + property + " (" + member.toString() + ")", expr, tkStart);
          }
          catch (ClassNotFoundException e2) {
            //fallthru
          }
        }
        throw e;
      }
      currType = toNonPrimitiveType(((Method) member).getReturnType());
      return o;
    }
    //剩下的成员肯定字段，因此有字段方式
    else if (member != null) {
      Field f = (Field) member;

      //静态成员
      if ((f.getModifiers() & Modifier.STATIC) != 0) {
        o = f.get(null);

        if (hasNullPropertyHandler()) {
          addAccessorNode(new StaticVarAccessorNH((Field) member, getNullMethodHandler()));
          if (o == null) o = getNullMethodHandler().getProperty(member.getName(), ctx, variableFactory);
        }
        else {
          addAccessorNode(new StaticVarAccessor((Field) member));
        }
      }
      //非静态成员
      else {
        o = ctx != null ? f.get(ctx) : null;
        if (hasNullPropertyHandler()) {
          addAccessorNode(new FieldAccessorNH((Field) member, getNullMethodHandler()));
          if (o == null) o = getNullMethodHandler().getProperty(member.getName(), ctx, variableFactory);
        }
        else {
          addAccessorNode(new FieldAccessor((Field) member));
        }
      }
      currType = toNonPrimitiveType(f.getType());
      return o;
    }
    //map属性获取的方式(前提是有此key或者是允许null安全)
    else if (ctx instanceof Map && (((Map) ctx).containsKey(property) || nullSafe)) {
      addAccessorNode(new MapAccessor(property));
      return ((Map) ctx).get(property);
    }
    //读取数组长度的方式
    else if (ctx != null && "length".equals(property) && ctx.getClass().isArray()) {
      //当前对象是数组，并且属性名为length，则访问数组长度
      addAccessorNode(new ArrayLength());
      return getLength(ctx);
    }
    //静态常量引用
    else if (LITERALS.containsKey(property)) {
      addAccessorNode(new StaticReferenceAccessor(ctx = LITERALS.get(property)));
      return ctx;
    }
    else {
      //尝试获取静态方法引用，如果该属性即是一个静态方法，则直接返回此,即类.方法名的形式
      Object tryStaticMethodRef = tryStaticAccess();
      staticAccess = true;
      if (tryStaticMethodRef != null) {
        if (tryStaticMethodRef instanceof Class) {
          addAccessorNode(new StaticReferenceAccessor(tryStaticMethodRef));
          return tryStaticMethodRef;
        }
        else if (tryStaticMethodRef instanceof Field) {
          addAccessorNode(new StaticVarAccessor((Field) tryStaticMethodRef));
          return ((Field) tryStaticMethodRef).get(null);
        }
        else {
          addAccessorNode(new StaticReferenceAccessor(tryStaticMethodRef));
          return tryStaticMethodRef;
        }
      }
      //这里与上面不同，上面是直接有类名，这里是只有方法名，如当前ctx为T 这里的属性名为 abc，则表示访问T.abc这个方法
      else if (ctx instanceof Class) {
        Class c = (Class) ctx;
        //处理静态方法伪引用，即直接通过方法名引用的方式达到调用方法的目的
        for (Method m : c.getMethods()) {
          if (property.equals(m.getName())) {
            if (pCtx != null && pCtx.getParserConfiguration() != null ? pCtx.getParserConfiguration().isAllowNakedMethCall() : MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL) {
              o = m.invoke(null, EMPTY_OBJ_ARR);
              if (hasNullMethodHandler()) {
                addAccessorNode(new MethodAccessorNH(m, new ExecutableStatement[0], getNullMethodHandler()));
                if (o == null)
                  o = getNullMethodHandler().getProperty(m.getName(), ctx, variableFactory);
              }
              else {
                addAccessorNode(new MethodAccessor(m, new ExecutableStatement[0]));
              }
              return o;
            }
            else {
              //不支持，则表示直接获取此静态方法引用
              addAccessorNode(new StaticReferenceAccessor(m));
              return m;
            }
          }
        }

        //这里最后认为是 T$abc这个内部类的引用，一般情况下这里不会到达，即这里只写一个abc，最后引用到T$abc这个内部类
        try {
          Class subClass = findClass(variableFactory, c.getName() + "$" + property, pCtx);
          addAccessorNode(new StaticReferenceAccessor(subClass));
          return subClass;
        }
        catch (ClassNotFoundException cnfe) {
          // fall through.
        }
      }
      else if (pCtx != null && pCtx.getParserConfiguration() != null ? pCtx.getParserConfiguration().isAllowNakedMethCall() : MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL) {
        //最后尝试直接执行此方法,虽然这里并不能走到这里,就像普通的方法一样执行(要求参数长度为0)
        //实际上并不能走到这里，因为上面在获取getter时已经获取了此方法(getter会根据名字拿到此方法)
        return getMethod(ctx, property);
      }

      //这里尝试一次从this引用上拿,虽然thisRef和ctx通常认为是一样的
      // if it is not already using this as context try to read the property value from this
      if (ctx != this.thisRef && this.thisRef != null) {
        addAccessorNode(new ThisValueAccessor());
        return getBeanProperty(this.thisRef, property);
      }

      //最后都不能访问到，因此直接报错
      if (ctx == null) {
        throw new PropertyAccessException("unresolvable property or identifier: " + property, expr, start, pCtx);
      }
      else {
        throw new PropertyAccessException("could not access: " + property + "; in class: "
            + ctx.getClass().getName(), expr, start, pCtx);
      }
    }
  }

  /**
   * 获取一个集合的值信息
   * Handle accessing a property embedded in a collections, map, or array
   *
   * @param ctx  -
   * @param prop -
   * @return -
   * @throws Exception -
   */
  private Object getCollectionProperty(Object ctx, String prop) throws Exception {
    //集合前的属性信息，如 a.bc[2]，先拿到a.bc信息
    if (prop.length() > 0) {
      ctx = getBeanProperty(ctx, prop);
    }

    currType = null;
    if (ctx == null) return null;

    int start = ++cursor;

    skipWhitespace();

    if (cursor == end)
      throw new CompileException("unterminated '['", this.expr, this.start);

    String item;

    if (scanTo(']'))
      throw new CompileException("unterminated '['", this.expr, this.start);

    item = new String(expr, start, cursor - start);

    //下标是否是表达式
    boolean itemSubExpr = true;

    Object idx = null;

    //先尝试直接解析下标，如果能够解析，则表示下标是数字，否则就是表达式
    try {
      idx = parseInt(item);
      itemSubExpr = false;
    }
    catch (Exception e) {
      // not a number;
    }

    //单元解析表达式
    ExecutableStatement itemStmt = null;
    if (itemSubExpr) {
      try {
        idx = (itemStmt = (ExecutableStatement) subCompileExpression(item.toCharArray(), pCtx))
            .getValue(ctx, thisRef, variableFactory);
      }
      catch (CompileException e) {
        e.setExpr(this.expr);
        e.setCursor(start);
        throw e;
      }
    }

    ++cursor;

    //处理map访问的形式,如a[b]
    if (ctx instanceof Map) {
      if (itemSubExpr) {
        addAccessorNode(new MapAccessorNest(itemStmt, null));
      }
      else {
        addAccessorNode(new MapAccessor(parseInt(item)));
      }

      return ((Map) ctx).get(idx);
    }
    //处理list访问
    else if (ctx instanceof List) {
      if (itemSubExpr) {
        addAccessorNode(new ListAccessorNest(itemStmt, null));
      }
      else {
        addAccessorNode(new ListAccessor(parseInt(item)));
      }

      return ((List) ctx).get((Integer) idx);
    }
    //处理数组访问的形式
    else if (ctx.getClass().isArray()) {
      if (itemSubExpr) {
        addAccessorNode(new ArrayAccessorNest(itemStmt));
      }
      else {
        addAccessorNode(new ArrayAccessor(parseInt(item)));
      }

      return Array.get(ctx, (Integer) idx);
    }
    //处理字符串访问的形式
    else if (ctx instanceof CharSequence) {
      if (itemSubExpr) {
        addAccessorNode(new IndexedCharSeqAccessorNest(itemStmt));
      }
      else {
        addAccessorNode(new IndexedCharSeqAccessor(parseInt(item)));
      }

      return ((CharSequence) ctx).charAt((Integer) idx);
    }
    else {
      //最后认为是一个数组类型描述符,当前数组内的内容忽略
      TypeDescriptor tDescr = new TypeDescriptor(expr, this.start, length, 0);
      if (tDescr.isArray()) {
        Class cls = getClassReference((Class) ctx, tDescr, variableFactory, pCtx);
        rootNode = new StaticReferenceAccessor(cls);
        return cls;
      }

      throw new CompileException("illegal use of []: unknown type: "
          + ctx.getClass().getName(), this.expr, this.start);
    }
  }


  private Object getCollectionPropertyAO(Object ctx, String prop) throws Exception {
    if (prop.length() > 0) {
      ctx = getBeanPropertyAO(ctx, prop);
    }

    currType = null;
    if (ctx == null) return null;

    int _start = ++cursor;

    skipWhitespace();

    if (cursor == end)
      throw new CompileException("unterminated '['", this.expr, this.start);

    String item;

    if (scanTo(']'))
      throw new CompileException("unterminated '['", this.expr, this.start);

    item = new String(expr, _start, cursor - _start);

    boolean itemSubExpr = true;

    Object idx = null;

    try {
      idx = parseInt(item);
      itemSubExpr = false;
    }
    catch (Exception e) {
      // not a number;
    }

    ExecutableStatement itemStmt = null;
    if (itemSubExpr) {
      idx = (itemStmt = (ExecutableStatement) subCompileExpression(item.toCharArray(), pCtx))
          .getValue(ctx, thisRef, variableFactory);
    }

    ++cursor;

    if (ctx instanceof Map) {
      if (hasPropertyHandler(Map.class)) {
        return propHandler(item, ctx, Map.class);
      }
      else {
        if (itemSubExpr) {
          addAccessorNode(new MapAccessorNest(itemStmt, null));
        }
        else {
          addAccessorNode(new MapAccessor(parseInt(item)));
        }

        return ((Map) ctx).get(idx);
      }
    }
    else if (ctx instanceof List) {
      if (hasPropertyHandler(List.class)) {
        return propHandler(item, ctx, List.class);
      }
      else {
        if (itemSubExpr) {
          addAccessorNode(new ListAccessorNest(itemStmt, null));
        }
        else {
          addAccessorNode(new ListAccessor(parseInt(item)));
        }

        return ((List) ctx).get((Integer) idx);
      }
    }
    else if (ctx.getClass().isArray()) {
      if (hasPropertyHandler(Array.class)) {
        return propHandler(item, ctx, Array.class);
      }
      else {
        if (itemSubExpr) {
          addAccessorNode(new ArrayAccessorNest(itemStmt));
        }
        else {
          addAccessorNode(new ArrayAccessor(parseInt(item)));
        }

        return Array.get(ctx, (Integer) idx);
      }
    }
    else if (ctx instanceof CharSequence) {
      if (hasPropertyHandler(CharSequence.class)) {
        return propHandler(item, ctx, CharSequence.class);
      }
      else {
        if (itemSubExpr) {
          addAccessorNode(new IndexedCharSeqAccessorNest(itemStmt));
        }
        else {
          addAccessorNode(new IndexedCharSeqAccessor(parseInt(item)));
        }

        return ((CharSequence) ctx).charAt((Integer) idx);
      }
    }
    else {
      TypeDescriptor tDescr = new TypeDescriptor(expr, this.start, end - this.start, 0);
      if (tDescr.isArray()) {
        Class cls = getClassReference((Class) ctx, tDescr, variableFactory, pCtx);
        rootNode = new StaticReferenceAccessor(cls);
        return cls;
      }

      throw new CompileException("illegal use of []: unknown type: "
          + ctx.getClass().getName(), this.expr, this.st);
    }
  }

  /**
   * Find an appropriate method, execute it, and return it's response.
   *
   * @param ctx  -
   * @param name -
   * @return -
   * @throws Exception -
   */
  @SuppressWarnings({"unchecked"})
  private Object getMethod(Object ctx, String name) throws Exception {
    int st = cursor;
    //sk表示捕获到()内的相应参数内容信息,如(a,b,c)就拿到a,b,c
    String tk = cursor != end
        && expr[cursor] == '(' && ((cursor = balancedCapture(expr, cursor, '(')) - st) > 1 ?
        new String(expr, st + 1, cursor - st - 1) : "";
    cursor++;

    Object[] args;
    Class[] argTypes;
    ExecutableStatement[] es;

    //空参数的情况
    if (tk.length() == 0) {
      args = ParseTools.EMPTY_OBJ_ARR;
      argTypes = ParseTools.EMPTY_CLS_ARR;
      es = null;
    }
    else {
      //参数分组处理
      List<char[]> subtokens = parseParameterList(tk.toCharArray(), 0, -1);
      es = new ExecutableStatement[subtokens.size()];
      args = new Object[subtokens.size()];
      argTypes = new Class[subtokens.size()];

      //每个参数段分别编译并执行
      for (int i = 0; i < subtokens.size(); i++) {
        try {
          args[i] = (es[i] = (ExecutableStatement) subCompileExpression(subtokens.get(i), pCtx))
              .getValue(this.thisRef, thisRef, variableFactory);
        }
        catch (CompileException e) {
          throw ErrorUtil.rewriteIfNeeded(e, this.expr, this.start);
        }

        //如果这个参数是类似(Abc) xxx的调用，则使用转型之后的出参信息
        if (es[i].isExplicitCast()) argTypes[i] = es[i].getKnownEgressType();
      }

      //设置参数类型信息
      if (pCtx.isStrictTypeEnforcement()) {
        for (int i = 0; i < args.length; i++) {
          argTypes[i] = es[i].getKnownEgressType();
          if (es[i] instanceof ExecutableLiteral && ((ExecutableLiteral) es[i]).getLiteral() == null) {
            argTypes[i] = NullType.class;
          }
        }
      }
      else {
        for (int i = 0; i < args.length; i++) {
          if (argTypes[i] != null) continue;

          if (es[i].getKnownEgressType() == Object.class) {
            argTypes[i] = args[i] == null ? null : args[i].getClass();
          }
          else {
            argTypes[i] = es[i].getKnownEgressType();
          }
        }
      }
    }

    return getMethod(ctx, name, args, argTypes, es);
  }

  /** 通过当前方法名(或其它名称),参数类型，参数信息，以及参数执行单元执行此方法 */
  @SuppressWarnings({"unchecked"})
  private Object getMethod(Object ctx, String name, Object[] args, Class[] argTypes, ExecutableStatement[] es) throws Exception {
    //如果是起始调用，并且变量能够解析，则表示此变量是一个方法句柄类信息，则通过此方法句柄进行调用
    if (first && variableFactory != null && variableFactory.isResolveable(name)) {
      Object ptr = variableFactory.getVariableResolver(name).getValue();
      //变量为一个方法
      if (ptr instanceof Method) {
        ctx = ((Method) ptr).getDeclaringClass();
        name = ((Method) ptr).getName();
      }
      //方法句柄
      else if (ptr instanceof MethodStub) {
        ctx = ((MethodStub) ptr).getClassReference();
        name = ((MethodStub) ptr).getMethodName();
      }
      //函数定义
      else if (ptr instanceof FunctionInstance) {
        FunctionInstance func = (FunctionInstance) ptr;
        //这里是说引用的名称与函数名不相同，如 在表达式中 function abc;var b = abc;
        // 这里引用b时，就会出现此种情况
        if (!name.equals(func.getFunction().getName())) {
          getBeanProperty(ctx, name);
          addAccessorNode(new DynamicFunctionAccessor(es));
        }
        else {
          //正常的函数调用
          addAccessorNode(new FunctionAccessor(func, es));
        }
        return func.call(ctx, thisRef, variableFactory, args);
      }
      else {
        throw new OptimizationFailure("attempt to optimize a method call for a reference that does not point to a method: "
            + name + " (reference is type: " + (ctx != null ? ctx.getClass().getName() : null) + ")");
      }

      first = false;
    }

    if (ctx == null && currType == null) {
      throw new PropertyAccessException("null pointer or function not found: " + name, this.expr, this.start, pCtx);
    }

    //当前上下文为静态类，即静态方法调用
    boolean classTarget = false;
    Class<?> cls = currType != null ? currType : ((classTarget = ctx instanceof Class) ? (Class<?>) ctx : ctx.getClass());
    currType = null;

    Method m;
    Class[] parameterTypes = null;

    /**
     * If we have not cached the method then we need to go ahead and try to resolve it.
     */

    /**
     * 重新尝试获取最匹配的方法，并且重置相应的参数类型
     * Try to find an instance method from the class target.
     */
    if ((m = getBestCandidate(argTypes, name, cls, cls.getMethods(), false, classTarget)) != null) {
      parameterTypes = m.getParameterTypes();
    }

    //静态方法，并且还没找到方法，尝试查找Class类上的方法,如getClass等
    if (m == null && classTarget) {
      /**
       * If we didn't find anything, maybe we're looking for the actual java.lang.Class methods.
       */
      if ((m = getBestCandidate(argTypes, name, cls, Class.class.getMethods(), false)) != null) {
        parameterTypes = m.getParameterTypes();
      }
    }

    // If we didn't find anything and the declared class is different from the actual one try also with the actual one
    if (m == null && ctx != null && cls != ctx.getClass() && !(ctx instanceof Class)) {
      cls = ctx.getClass();
      if ((m = getBestCandidate(argTypes, name, cls, cls.getMethods(), false, classTarget)) != null) {
        parameterTypes = m.getParameterTypes();
      }
    }

    //还没有找到方法，则尝试几个特殊的方法,或者直接报错
    if (m == null) {
      StringAppender errorBuild = new StringAppender();
      //数组的size方法
      if ("size".equals(name) && args.length == 0 && cls.isArray()) {
        addAccessorNode(new ArrayLength());
        return getLength(ctx);
      }

      // if it is not already using this as context try to access the method this
      if (ctx != this.thisRef && this.thisRef != null) {
        addAccessorNode(new ThisValueAccessor());
        return getMethod(this.thisRef, name, args, argTypes, es);
      }

      for (int i = 0; i < args.length; i++) {
        errorBuild.append(args[i] != null ? args[i].getClass().getName() : null);
        if (i < args.length - 1) errorBuild.append(", ");
      }

      throw new PropertyAccessException("unable to resolve method: " + cls.getName() + "."
          + name + "(" + errorBuild.toString() + ") [arglength=" + args.length + "]", this.expr, this.st, pCtx);
    }

    //如果有表达式，则通过表达式重新处理相应的参数信息，如类型转换等
    if (es != null) {
      ExecutableStatement cExpr;
      for (int i = 0; i < es.length; i++) {
        cExpr = es[i];
        //从参数签名上处理相应的类型兼容及转换问题
        if (cExpr.getKnownIngressType() == null) {
          cExpr.setKnownIngressType(paramTypeVarArgsSafe(parameterTypes, i, m.isVarArgs()));
          cExpr.computeTypeConversionRule();
        }
        //类型不兼容，则处理相应的类型转换，转换为正确的参数类型
        if (!cExpr.isConvertableIngressEgress()) {
          args[i] = convert(args[i], paramTypeVarArgsSafe(parameterTypes, i, m.isVarArgs()));
        }
      }
    }
    else {
      /**
       * 如果执行单元，则直接进行类型转换
       * Coerce any types if required.
       */
      for (int i = 0; i < args.length; i++)
        args[i] = convert(args[i], paramTypeVarArgsSafe(parameterTypes, i, m.isVarArgs()));
    }

    //获取方法并进行调用
    Method method = getWidenedTarget(cls, m);
    Object o = ctx != null ? method.invoke(ctx, normalizeArgsForVarArgs(parameterTypes, args, m.isVarArgs())) : null;

    if (hasNullMethodHandler()) {
      addAccessorNode(new MethodAccessorNH(method, (ExecutableStatement[]) es, getNullMethodHandler()));
      if (o == null) o = getNullMethodHandler().getProperty(m.getName(), ctx, variableFactory);
    }
    else {
      addAccessorNode(new MethodAccessor(method, (ExecutableStatement[]) es));
    }

    /**
     * return the response.
     */
    currType = toNonPrimitiveType(method.getReturnType());
    return o;
  }

  public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) throws Exception {
    return rootNode.getValue(ctx, elCtx, variableFactory);
  }

  private Accessor _getAccessor(Object o, Class type) {
    if (o instanceof List) {
      Accessor[] a = new Accessor[((List) o).size()];
      int i = 0;

      for (Object item : (List) o) {
        a[i++] = _getAccessor(item, type);
      }

      returnType = List.class;

      return new ListCreator(a);
    }
    else if (o instanceof Map) {
      Accessor[] k = new Accessor[((Map) o).size()];
      Accessor[] v = new Accessor[k.length];
      int i = 0;

      for (Object item : ((Map) o).keySet()) {
        k[i] = _getAccessor(item, type); // key
        v[i++] = _getAccessor(((Map) o).get(item), type); // value
      }

      returnType = Map.class;

      return new MapCreator(k, v);
    }
    else if (o instanceof Object[]) {
      Accessor[] a = new Accessor[((Object[]) o).length];
      int i = 0;
      int dim = 0;

      if (type != null) {
        String nm = type.getName();
        while (nm.charAt(dim) == '[') dim++;
      }
      else {
        type = Object[].class;
        dim = 1;
      }

      try {
        Class base = getBaseComponentType(type);
        Class cls = dim > 1 ? findClass(null, repeatChar('[', dim - 1) + "L" + base.getName() + ";", pCtx)
            : type;

        for (Object item : (Object[]) o) {
          expectType(pCtx, a[i++] = _getAccessor(item, cls), base, true);
        }

        return new ArrayCreator(a, getSubComponentType(type));
      }
      catch (ClassNotFoundException e) {
        throw new RuntimeException("this error should never throw:" + getBaseComponentType(type).getName(), e);
      }
    }
    else {
      if (returnType == null) returnType = Object.class;
      if (type.isArray()) {
        return new ExprValueAccessor((String) o, type, ctx, variableFactory, pCtx);
      }
      else {
        return new ExprValueAccessor((String) o, Object.class, ctx, variableFactory, pCtx);
      }
    }
  }


  public Accessor optimizeCollection(ParserContext pCtx, Object o, Class type, char[] property, int start, int offset,
                                     Object ctx, Object thisRef, VariableResolverFactory factory) {
    this.start = this.cursor = start;
    this.length = start + offset;
    this.returnType = type;
    this.ctx = ctx;
    this.variableFactory = factory;
    this.pCtx = pCtx;

    Accessor root = _getAccessor(o, returnType);

    if (property != null && length > start) {
      return new Union(pCtx, root, property, cursor, offset);
    }
    else {
      return root;
    }
  }


  public Accessor optimizeObjectCreation(ParserContext pCtx, char[] property, int start, int offset,
                                         Object ctx, Object thisRef, VariableResolverFactory factory) {
    this.length = start + offset;
    this.cursor = this.start = start;
    this.pCtx = pCtx;

    try {
      return compileConstructor(property, ctx, factory);
    }
    catch (CompileException e) {
      throw ErrorUtil.rewriteIfNeeded(e, property, this.start);
    }
    catch (ClassNotFoundException e) {
      throw new CompileException("could not resolve class: " + e.getMessage(), property, this.start, e);
    }
    catch (Exception e) {
      throw new CompileException("could not create constructor: " + e.getMessage(), property, this.start, e);
    }
  }


  private void setRootNode(AccessorNode rootNode) {
    this.rootNode = this.currNode = rootNode;
  }

  private AccessorNode getRootNode() {
    return rootNode;
  }

  public Object getResultOptPass() {
    return val;
  }

  @SuppressWarnings({"WeakerAccess"})
  private AccessorNode compileConstructor(char[] expression, Object ctx, VariableResolverFactory vars) throws
      InstantiationException, IllegalAccessException, InvocationTargetException,
      ClassNotFoundException, NoSuchMethodException {

    String[] cnsRes = captureContructorAndResidual(expression, start, length);
    List<char[]> constructorParms = parseMethodOrConstructor(cnsRes[0].toCharArray());

    if (constructorParms != null) {
      String s = new String(subset(expression, 0, ArrayTools.findFirst('(', start, length, expression)));
      Class cls = ParseTools.findClass(vars, s, pCtx);

      ExecutableStatement[] cStmts = new ExecutableStatement[constructorParms.size()];

      for (int i = 0; i < constructorParms.size(); i++) {
        cStmts[i] = (ExecutableStatement) subCompileExpression(constructorParms.get(i), pCtx);
      }

      Object[] parms = new Object[constructorParms.size()];
      for (int i = 0; i < constructorParms.size(); i++) {
        parms[i] = cStmts[i].getValue(ctx, vars);
      }

      Constructor cns = getBestConstructorCandidate(parms, cls, pCtx.isStrongTyping());

      if (cns == null) {
        StringBuilder error = new StringBuilder();
        for (int i = 0; i < parms.length; i++) {
          error.append(parms[i].getClass().getName());
          if (i + 1 < parms.length) error.append(", ");
        }

        throw new CompileException("unable to find constructor: " + cls.getName()
            + "(" + error.toString() + ")", this.expr, this.start);
      }
      for (int i = 0; i < parms.length; i++) {
        //noinspection unchecked
        parms[i] = convert(parms[i], paramTypeVarArgsSafe(cns.getParameterTypes(), i, cns.isVarArgs()));
      }
      parms = normalizeArgsForVarArgs(cns.getParameterTypes(), parms, cns.isVarArgs());

      AccessorNode ca = new ConstructorAccessor(cns, cStmts);

      if (cnsRes.length > 1) {
        ReflectiveAccessorOptimizer compiledOptimizer
            = new ReflectiveAccessorOptimizer(pCtx, cnsRes[1].toCharArray(), 0, cnsRes[1].length(),
            cns.newInstance(parms), ctx, vars);
        compiledOptimizer.ingressType = cns.getDeclaringClass();


        compiledOptimizer.setRootNode(ca);
        compiledOptimizer.compileGetChain();
        ca = compiledOptimizer.getRootNode();

        this.val = compiledOptimizer.getResultOptPass();
      }

      return ca;
    }
    else {
      ClassLoader classLoader = pCtx != null ? pCtx.getClassLoader() : currentThread().getContextClassLoader();
      Constructor<?> cns = Class.forName(new String(expression), true, classLoader).getConstructor(EMPTYCLS);
      AccessorNode ca = new ConstructorAccessor(cns, null);

      if (cnsRes.length > 1) {
        //noinspection NullArgumentToVariableArgMethod
        ReflectiveAccessorOptimizer compiledOptimizer
            = new ReflectiveAccessorOptimizer(pCtx, cnsRes[1].toCharArray(), 0, cnsRes[1].length(), cns.newInstance(null), ctx, vars);
        compiledOptimizer.setRootNode(ca);
        compiledOptimizer.compileGetChain();
        ca = compiledOptimizer.getRootNode();

        this.val = compiledOptimizer.getResultOptPass();
      }

      return ca;
    }
  }

  public Class getEgressType() {
    return returnType;
  }

  public boolean isLiteralOnly() {
    return false;
  }

  private Object propHandler(String property, Object ctx, Class handler) {
    PropertyHandler ph = getPropertyHandler(handler);
    addAccessorNode(new PropertyHandlerAccessor(property, handler, ph));
    return ph.getProperty(property, ctx, variableFactory);
  }

  public void propHandlerSet(String property, Object ctx, Class handler, Object value) {
    PropertyHandler ph = getPropertyHandler(handler);
    addAccessorNode(new PropertyHandlerAccessor(property, handler, ph));
    ph.setProperty(property, ctx, variableFactory, value);
  }
}
