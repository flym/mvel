package org.mvel2.util;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

/**
 * 行映射处理
 *
 * @author Mike Brock <cbrock@redhat.com>
 */
public class LineMapper {
  /** 整个表达式 */
  private char[] expr;

  /** 每一行的具体映射 */
  private ArrayList<Node> lineMapping;
  /** 有哪些行 */
  private Set<Integer> lines;

  public LineMapper(char[] expr) {
    this.expr = expr;
  }

  public LineLookup map() {
    lineMapping = new ArrayList<Node>();
    lines = new TreeSet<Integer>();

    int cursor = 0;
    int start = 0;
    int line = 1;

    for (; cursor < expr.length; cursor++) {
      switch (expr[cursor]) {
        case '\n':
          lines.add(line);
          lineMapping.add(new Node(start, cursor, line++));
          start = cursor + 1;
          break;
      }
    }

    if (cursor > start) {
      lines.add(line);
      lineMapping.add(new Node(start, cursor, line));
    }

    return new LineLookup() {
      public int getLineFromCursor(int cursor) {
        for (Node n : lineMapping) {
          if (n.isInRange(cursor)) {
            return n.getLine();
          }
        }
        return -1;
      }

      public boolean hasLine(int line) {
        return lines.contains(line);
      }
    };
  }

  public static interface LineLookup {
    public int getLineFromCursor(int cursor);

    public boolean hasLine(int line);
  }

  /** 描述每一行的位置信息 */
  private static class Node implements Comparable<Node> {
    /** 起始点 */
    private int cursorStart;
    /** 结束点 */
    private int cursorEnd;

    /** 当前第几行 */
    private int line;


    private Node(int cursorStart, int cursorEnd, int line) {
      this.cursorStart = cursorStart;
      this.cursorEnd = cursorEnd;
      this.line = line;
    }

    public int getLine() {
      return line;
    }

    public boolean isInRange(int cursor) {
      return cursor >= cursorStart && cursor <= cursorEnd;
    }

    public int compareTo(Node node) {
      if (node.cursorStart >= cursorEnd) {
        return 1;
      }
      else if (node.cursorEnd < cursorStart) {
        return -1;
      }
      else {
        return 0;
      }
    }
  }
}
