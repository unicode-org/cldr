package org.unicode.cldr.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class DelegatingIterator<T> implements Iterator<T> {

  private Iterator<T>[] iterators;
  private int           item = 0;

  public DelegatingIterator(Iterator<T>... iterators) {
    this.iterators = iterators;
  }

  public boolean hasNext() {
    // TODO Auto-generated method stub
    while (item < iterators.length) {
      boolean result = iterators[item].hasNext();
      if (result) {
        return true;
      }
      ++item;
    }
    return false;
  }

  public T next() {
    while (item < iterators.length) {
      try {
        return iterators[item].next();
      } catch (NoSuchElementException e) {
        ++item;
      }
    }
    throw new NoSuchElementException();
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }

  public static <T> Iterable<T> iterable(Iterable<T>... s) {
    // TODO Auto-generated method stub
    return new MyIterable<T>(s);
  }
  
  public static <T> T[] array(T... s) {
    // TODO Auto-generated method stub
    return s;
  }

  private static class MyIterable<T> implements Iterable<T> {
    public Iterable<T>[] iterables;

    public MyIterable(Iterable<T>... s) {
      iterables = s;
    }

    @SuppressWarnings("unchecked")
    public Iterator<T> iterator() {
      Iterator<T>[] iterators = new Iterator[iterables.length];
      for (int i = 0; i < iterables.length; ++i) {
        iterators[i] = iterables[i].iterator();
      }
      return new DelegatingIterator<T>(iterators);
    }
  }
}
