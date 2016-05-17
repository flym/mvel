package org.mvel2.ast;

import org.mvel2.ParserContext;

/** 表示一个使用两个操作符处理的运算节点(并不单指boolean操作) */
public abstract class BooleanNode extends ASTNode {
  protected ASTNode left;
  protected ASTNode right;

  protected BooleanNode(ParserContext pCtx) {
    super(pCtx);
  }

  public ASTNode getLeft() {
    return this.left;
  }

  public ASTNode getRight() {
    return this.right;
  }

  public void setLeft(ASTNode node) {
    this.left = node;
  }

  public void setRight(ASTNode node) {
    this.right = node;
  }

  public abstract void setRightMost(ASTNode right);

  public abstract ASTNode getRightMost();
}
