package org.mvel2.ast;

import org.mvel2.ParserContext;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.util.CompilerTools;


/** 描述一个instanceof的操作节点,为一个优化节点 */
public class Instance extends ASTNode {
  /** 主对象表达式 */
  private ASTNode stmt;
  /** 类型的表达式 */
  private ASTNode clsStmt;

  public Instance(ASTNode stmt, ASTNode clsStmt, ParserContext pCtx) {
    super(pCtx);
    this.stmt = stmt;
    this.clsStmt = clsStmt;
    //要求后面的clsStmt返回值为class类型
    CompilerTools.expectType(pCtx, clsStmt, Class.class, true);
  }

  public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
    return ((Class) clsStmt.getReducedValueAccelerated(ctx, thisValue, factory)).isInstance(stmt.getReducedValueAccelerated(ctx, thisValue, factory));
  }

  public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
    try {
      Class i = (Class) clsStmt.getReducedValue(ctx, thisValue, factory);
      if (i == null) throw new ClassCastException();

      return i.isInstance(stmt.getReducedValue(ctx, thisValue, factory));
    }
    catch (ClassCastException e) {
      throw new RuntimeException("not a class reference: " + clsStmt.getName());
    }

  }

  public Class getEgressType() {
    return Boolean.class;
  }

  public ASTNode getStatement() {
    return stmt;
  }

  public ASTNode getClassStatement() {
    return clsStmt;
  }
}
