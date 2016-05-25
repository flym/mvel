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

package org.mvel2.util;

import org.mvel2.ScriptRuntimeException;

import static java.lang.String.valueOf;
import static org.mvel2.math.MathProcessor.doOperations;

/**
 * 维护了一个栈式的计算结构，即通过数据入栈，操作数入栈，然后再通过op进行操作处理以模拟一个栈式的计算过程
 * 在处理过程中，通过size来维护相应的栈内数据，并且可以判定相应的栈是否已处理完毕
 * 操作数的栈的引用没有通过通常的java stack来表示，而是通过在入栈时使用element来进行引用，
 * 即下一个要入栈的元素将引用之前在栈内的数据，这样在整个计算栈中，只需要维护最上层的引用即可
 * 如 a + b，在栈内即表现为a b +，这种后缀表达式结构，然后再通过最上层的操作符来进行op操作，
 * 得到的结果c再重新入栈
 */
public class ExecutionStack {
  /** 当前最新值 */
  private StackElement element;
  /** 栈中操作数长度 */
  private int size = 0;

  /** 当前栈是否是空的，即没有操作数也没有操作符 */
  public boolean isEmpty() {
    return size == 0;
  }

  public void add(Object o) {
    size++;
    StackElement el = element;
    if (el != null) {
      while (el.next != null) {
        el = el.next;
      }
      el.next = new StackElement(null, o);
    }
    else {
      element = new StackElement(null, o);
    }
  }

  public void push(Object o) {
    size++;
    element = new StackElement(element, o);
    assert size == deepCount();

  }

  /**
   * 入栈2个对象,其中第二个对象为一个操作符对象，即插入的数据为 对象 + 操作符
   * 在后序的处理中，将直接通过此操作符将相应的节点以及之前插入的节点进行直接运算或者是编译处理
   */
  public void push(Object obj1, Object obj2) {
    size += 2;
    element = new StackElement(new StackElement(element, obj1), obj2);
    assert size == deepCount();

  }

  public void push(Object obj1, Object obj2, Object obj3) {
    size += 3;
    element = new StackElement(new StackElement(new StackElement(element, obj1), obj2), obj3);
    assert size == deepCount();

  }

  public Object peek() {
    if (size == 0) return null;
    else return element.value;
  }

  public void dup() {
    size++;
    element = new StackElement(element, element.value);
    assert size == deepCount();

  }

  public Boolean peekBoolean() {
    if (size == 0) return null;
    if (element.value instanceof Boolean) return (Boolean) element.value;
    throw new ScriptRuntimeException("expected Boolean; but found: " + (element.value == null ? "null" : element.value.getClass().getName()));
  }

  public void copy2(ExecutionStack es) {
    element = new StackElement(new StackElement(element, es.element.value), es.element.next.value);
    es.element = es.element.next.next;
    size += 2;
    es.size -= 2;
  }

  public void copyx2(ExecutionStack es) {
    element = new StackElement(new StackElement(element, es.element.next.value), es.element.value);
    es.element = es.element.next.next;
    size += 2;
    es.size -= 2;
  }

  public Object peek2() {
    return element.next.value;
  }

  /** 弹出当前操作数 */
  public Object pop() {
    if (size == 0) {
      return null;
    }
    try {
      size--;
      return element.value;
    }
    finally {
      element = element.next;
      assert size == deepCount();
    }
  }

  /** 弹出当前操作数，并期望是一个boolean值 */
  public Boolean popBoolean() {
    if (size-- == 0) {
      return null;
    }
    try {
      if (element.value instanceof Boolean) return (Boolean) element.value;
      throw new ScriptRuntimeException("expected Boolean; but found: " + (element.value == null ? "null" : element.value.getClass().getName()));
    }
    finally {
      element = element.next;
      assert size == deepCount();

    }
  }

  public Object pop2() {
    try {
      size -= 2;
      return element.value;
    }
    finally {
      element = element.next.next;
      assert size == deepCount();
    }
  }

  public void discard() {
    if (size != 0) {
      size--;
      element = element.next;
    }
  }

  public int size() {
    return size;
  }

  /** 判定当前操作数栈是否需要减少 */
  public boolean isReduceable() {
    return size > 1;
  }

  public void clear() {
    size = 0;
    element = null;
  }

  public void xswap_op() {
    element = new StackElement(element.next.next.next, doOperations(element.next.next.value, (Integer) element.next.value, element.value));
    size -= 2;
    assert size == deepCount();
  }

  /** 使用栈上的操作符对最近的2个操作数进行处理，处理的结果重新入栈 */
  public void op() {
    element = new StackElement(element.next.next.next, doOperations(element.next.next.value, (Integer) element.value, element.next.value));
    size -= 2;
    assert size == deepCount();
  }

  public void op(int operator) {
    element = new StackElement(element.next.next, doOperations(element.next.value, operator, element.value));
    size--;
    assert size == deepCount();
  }

  public void xswap() {
    StackElement e = element.next;
    StackElement relink = e.next;
    e.next = element;
    (element = e).next.next = relink;
  }

  public void xswap2() {
    StackElement node2 = element.next;
    StackElement node3 = node2.next;

    (node2.next = element).next = node3.next;
    element = node3;
    element.next = node2;
  }

  public int deepCount() {
    int count = 0;

    if (element == null) {
      return 0;
    }
    else {
      count++;
    }

    StackElement element = this.element;
    while ((element = element.next) != null) {
      count++;
    }
    return count;
  }

  public String toString() {
    StackElement el = element;

    if (element == null) return "<EMPTY>";

    StringBuilder appender = new StringBuilder().append("[");
    do {
      appender.append(valueOf(el.value));
      if (el.next != null) appender.append(", ");
    }
    while ((el = el.next) != null);

    appender.append("]");

    return appender.toString();
  }
}
