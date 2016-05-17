package org.mvel2.optimizers.impl.refl.nodes;

import org.mvel2.compiler.AccessorNode;
import org.mvel2.integration.GlobalListenerFactory;
import org.mvel2.integration.VariableResolverFactory;


/** 描述一下可以在调用时进行监听通知的访问器 */
public class Notify implements AccessorNode {
  /** 当前处理的属性名 */
  private String name;
  private AccessorNode nextNode;

  public Notify(String name) {
    this.name = name;
  }

  public Object getValue(Object ctx, Object elCtx, VariableResolverFactory vrf) {
    GlobalListenerFactory.notifyGetListeners(ctx, name, vrf);
    return nextNode.getValue(ctx, elCtx, vrf);
  }

  public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
    GlobalListenerFactory.notifySetListeners(ctx, name, variableFactory, value);
    return nextNode.setValue(ctx, elCtx, variableFactory, value);
  }

  public AccessorNode getNextNode() {
    return nextNode;
  }

  public AccessorNode setNextNode(AccessorNode nextNode) {
    return this.nextNode = nextNode;
  }

  public Class getKnownEgressType() {
    return Object.class;
  }
}
