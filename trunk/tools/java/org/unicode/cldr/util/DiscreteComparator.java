package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.icu.impl.Utility;

/**
 * A comparator that can be built from a series of 'less than' relations.
 * 
 * <pre>
 * DiscreteComparator<String> comp = new Builder<String>(order).add("c", "d", "b", "a").add("m", "n", "d").get();
 * if (comp.compare("a", "m")) ...
 * </pre>
 * 
 * @author markdavis
 * @param <T>
 *          any class
 */
public class DiscreteComparator<T> implements Comparator<T> {
  /**
   * The builder can take three different orders: input order, natural order
   * (assumes T is Comparable<T>), and arbitrary.
   */
  public enum Ordering {
    CHRONOLOGICAL, NATURAL, ARBITRARY
  }

  private static final boolean DEBUG = false;
  private static int debugIndent;

  private Map<T, Integer> ordering;
  private Comparator<T> backupOrdering;

  private DiscreteComparator(Map<T, Integer> ordering, Comparator<T> backupOrdering) {
    this.backupOrdering = backupOrdering;
    this.ordering = ordering;
  }

  /**
   * Compare the items. If there was a backup comparator, it will be used for
   * items not specified explicitly. However, all explicit items will always
   * come before all others, to preserve transitivity.
   * 
   * @param o1
   *          first item
   * @param o2
   *          second item
   * @return integer representing the order
   * @exception MissingItemException
   *              thrown if there is no backup comparator, and at least one of
   *              the items is not explicit in the collator.
   */
  public int compare(T o1, T o2) {
    // TODO add option for back ordering
    Integer a = ordering.get(o1);
    Integer b = ordering.get(o2);
    if (a != null && b != null) {
    }
    if (a == null) {
      if (backupOrdering != null) {
        if (b == null) {
          return backupOrdering.compare(o1, o2);
        } else {
          return 1; // b is in, so less
        }
      }
      throw new MissingItemException("Item not in ordering:\t" + o1);
    } else if (b == null) {
      if (backupOrdering != null) {
        return -1; // a is in, so less
      }
      throw new MissingItemException("Item not in ordering:\t" + o2);
    }
    return a.compareTo(b);
  }

  /**
   * Get a list of the explicit items.
   * 
   * @return a list
   */
  public List<T> getOrdering() {
    return new ArrayList<T>(ordering.keySet());
  }

  @Override
  public String toString() {
    return ordering.keySet().toString();
  }

  /**
   * Builder for DiscreteComparator
   * 
   * @param <T>
   *          any class
   */
  public static class Builder<T> {

    // builds a topological sort from an input set of relations
    // with O(n) algorithm
    private Map<T, Node<T>> all;
    private Comparator<T> backupOrdering;
    private Ordering order;

    /**
     * Pass the order you want for the results.
     * 
     * @param order
     */
    public Builder(Ordering order) {
      this.order = order;
      all = order == Ordering.CHRONOLOGICAL ? new LinkedHashMap<T, Node<T>>()
              : order == Ordering.NATURAL ? new TreeMap<T, Node<T>>()
                      : new HashMap<T, Node<T>>();
    }

    /**
     * If there is a backup comparator, specify it here.
     * 
     * @param backupOrdering
     * @return this, for chaining
     */
    public Builder<T> setFallbackComparator(Comparator<T> backupOrdering) {
      this.backupOrdering = backupOrdering;
      return this;
    }

    /**
     * Add explicitly ordered items, from least to greatest
     * 
     * @param items
     * @return this, for chaining
     */
    public Builder<T> add(Collection<T> items) {
      if (items.size() < 2) {
        if (items.size() == 1) {
          T item = items.iterator().next();
          if (!all.containsKey(item)) {
            addNew(item);
          }
        }
        return this;
      }
      T last = null;
      boolean first = true;
      for (T item : items) {
        if (first) {
          first = false;
        } else {
          add(last, item);
        }
        last = item;
      }
      return this;
    }

    /**
     * Add explicitly ordered items, from least to greatest
     * 
     * @param items
     * @return this, for chaining
     */
    public Builder<T> add(T... items) {
      if (items.length < 2) {
        if (items.length == 1) {
          T item = items[0];
          if (!all.containsKey(item)) {
            addNew(item);
          }
        }
      }
      T last = null;
      boolean first = true;
      for (T item : items) {
        if (first) {
          first = false;
        } else {
          add(last, item);
        }
        last = item;
      }
      return this;
    }

    /**
     * Add explicitly ordered items
     * 
     * @param a
     *          lesser
     * @param b
     *          greater
     * @return this, for chaining
     */
    public Builder<T> add(T a, T b) {
      // if (a.equals(b)) {
      // throw new CycleException("Can't add equal items", a, a);
      // }
      Node<T> aNode = all.get(a);
      if (aNode == null) {
        aNode = addNew(a);
      }
      Node<T> bNode = all.get(b);
      if (bNode == null) {
        bNode = addNew(b);
      }
      addLink(aNode, bNode);
      if (DEBUG) {
        System.out.println(a + " + " + b + " => " + this);
      }
      return this;
    }

    /**
     * Get the comparator you've been building. After this call, the builder is
     * reset (if there is no error).
     * 
     * @return comparator
     * @exception CycleException
     *              throwing if there is (at least one) cycle, like a > b > c >
     *              a. If so, call getCycle to see the cycle that triggered the
     *              exception.
     */
    public DiscreteComparator<T> get() {
      if (DEBUG) {
        debugIndent = new Exception().getStackTrace().length;
      }
      Map<T, Integer> ordering = new LinkedHashMap<T, Integer>();
      for (Node<T> subNode : all.values()) {
        if (!subNode.visited) {
          subNode.visit(ordering);
        }
      }
      // clean up, so another call doesn't mess things up
      all.clear();
      return new DiscreteComparator<T>(ordering, backupOrdering);
    }

    /**
     * Call only after getting a CycleException
     * 
     * @return list of items that form a cycle, in order from least to greatest
     */
    public List<T> getCycle() {
      List<T> result = new LinkedList<T>();
      Collection<Node<T>> lesser = all.values();
      main: while (true) {
        for (Node<T> item : lesser) {
          if (item.chained) {
            if (result.contains(item.me)) {
              return result;
            }
            result.add(0, item.me);
            lesser = item.less;
            continue main;
          }
        }
        throw new IllegalArgumentException("Must only be called after a CycleException");
      }
    }

    @Override
    public String toString() {
      return order + ":\t" + all.values().toString();
    }

    private void addLink(Node<T> aNode, Node<T> bNode) {
      bNode.less.add(aNode);
    }

    private Node<T> addNew(T a) {
      Node<T> aNode = new Node<T>(a, order);
      all.put(a, aNode);
      return aNode;
    }
  }

  private static class Node<T> implements Comparable<Node<T>> {
    private Set<Node<T>> less;
    private T me;
    private boolean visited = false;
    private boolean chained = false;

    public Node(T a, Ordering order) {
      less = new LinkedHashSet<Node<T>>();
      less = order == Ordering.CHRONOLOGICAL ? new LinkedHashSet<Node<T>>()
              : order == Ordering.NATURAL ? new TreeSet<Node<T>>()
                      : new HashSet<Node<T>>();

              me = a;
    }

    private void visit(Map<T, Integer> resultOrdering) {
      Node<T> currentNode = this;
      if (DEBUG) {
        String gap = Utility.repeat(" ", new Exception().getStackTrace().length - debugIndent);
        System.out.println("Visiting:\t" + gap + currentNode + " => " + resultOrdering.keySet());
      }
      currentNode.visited = true;
      currentNode.chained = true;
      for (Node<T> subNode : currentNode.less) {
        if (subNode.chained) {
          throw new CycleException("Cycle in input data");
        }
        if (subNode.visited) {
          continue;
        }
        subNode.visit(resultOrdering);
      }
      currentNode.chained = false;
      resultOrdering.put(currentNode.me, resultOrdering.size());
    }

    public String toString() {
      StringBuilder result = new StringBuilder();
      result.append(me == null ? null : me.toString()).append(" >");
      for (Node<T> lesser : less) {
        result.append(" ").append(lesser.me);
      }
      return result.toString();
    }

    @SuppressWarnings("unchecked")
    public int compareTo(Node<T> o) {
      return ((Comparable) me).compareTo((Comparable) (o.me));
    }
  }

  /**
   * Exception for indicating that a cycle was found.
   */
  public static class CycleException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public <T> CycleException(String message) {
      super(message);
    }
  }

  /**
   * Exception indicating that there is no backup comparator, and at least one
   * of the items compared is not explicitly set.
   */
  public static class MissingItemException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public MissingItemException(String message) {
      super(message);
    }
  }
}
