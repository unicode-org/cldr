package org.unicode.cldr.draft;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class SetX<T> implements Set<T> {

    final private Set<T> source;
    final private Class<T> classType;

    public static <T> SetX<T> of(Set<T> source, Class<T> classType) {
        return new SetX<>(source, classType);
    }

    private SetX(Set<T> source, Class<T> classType) {
        this.source = source;
        this.classType = classType;
    }
    /**
     * @param action
     * @see java.lang.Iterable#forEach(java.util.function.Consumer)
     */
    public void forEach(Consumer<? super T> action) {
        source.forEach(action);
    }
    /**
     * @return
     * @see java.util.Set#size()
     */
    public int size() {
        return source.size();
    }
    /**
     * @return
     * @see java.util.Set#isEmpty()
     */
    public boolean isEmpty() {
        return source.isEmpty();
    }
    /**
     * Change to containsX for type safety
     * @param o
     * @return
     * @see java.util.Set#contains(java.lang.Object)
     * @deprecated
     */
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        verifyClass(o);
        return source.contains(o);
    }
    
    /**
     * Type-safe contains
     * @param o
     * @return
     * @see java.util.Set#contains(java.lang.Object)
     */
    public boolean containsX(T o) {
        return source.contains(o);
    }

    private void verifyClass(Object o) {
        if (classType.isInstance(o)) {
            throw new ClassCastException();
        }
    }
    /**
     * @return
     * @see java.util.Set#iterator()
     */
    public Iterator<T> iterator() {
        return source.iterator();
    }
    /**
     * @return
     * @see java.util.Set#toArray()
     */
    public Object[] toArray() {
        return source.toArray();
    }
    /**
     * @param a
     * @return
     * @see java.util.Set#toArray(java.lang.Object[])
     */
    public <U> U[] toArray(U[] a) {
        return source.toArray(a);
    }
    /**
     * @param e
     * @return
     * @see java.util.Set#add(java.lang.Object)
     */
    public boolean add(T e) {
        return source.add(e);
    }
    /**
     * Use type-safe removeX
     * @param o
     * @return
     * @see java.util.Set#remove(java.lang.Object)
     * @deprecated
     */
    public boolean remove(Object o) {
        verifyClass(o);
        return source.remove(o);
    }
    /**
     * Type-save remove
     * @param o
     * @return
     * @see java.util.Set#remove(java.lang.Object)
     */
    public boolean removeX(T o) {
        return source.remove(o);
    }

    /**
     * @param c
     * @return
     * @see java.util.Set#containsAll(java.util.Collection)
     */
    public boolean containsAll(Collection<?> c) {
        return source.containsAll(c);
    }
    /**
     * @param c
     * @return
     * @see java.util.Set#addAll(java.util.Collection)
     */
    public boolean addAll(Collection<? extends T> c) {
        return source.addAll(c);
    }
    /**
     * @param c
     * @return
     * @see java.util.Set#retainAll(java.util.Collection)
     */
    public boolean retainAll(Collection<?> c) {
        return source.retainAll(c);
    }
    /**
     * @param c
     * @return
     * @see java.util.Set#removeAll(java.util.Collection)
     */
    public boolean removeAll(Collection<?> c) {
        return source.removeAll(c);
    }
    /**
     * 
     * @see java.util.Set#clear()
     */
    public void clear() {
        source.clear();
    }
    /**
     * @param o
     * @return
     * @see java.util.Set#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        verifyClass(o);
        return source.equals(o);
    }
    /**
     * @return
     * @see java.util.Set#hashCode()
     */
    public int hashCode() {
        return source.hashCode();
    }
    /**
     * @return
     * @see java.util.Set#spliterator()
     */
    public Spliterator<T> spliterator() {
        return source.spliterator();
    }
    /**
     * @param filter
     * @return
     * @see java.util.Collection#removeIf(java.util.function.Predicate)
     */
    public boolean removeIf(Predicate<? super T> filter) {
        return source.removeIf(filter);
    }
    /**
     * @return
     * @see java.util.Collection#stream()
     */
    public Stream<T> stream() {
        return source.stream();
    }
    /**
     * @return
     * @see java.util.Collection#parallelStream()
     */
    public Stream<T> parallelStream() {
        return source.parallelStream();
    }

    public static void main(String[] args) {
        SetX<CharSequence> s = new SetX<>(new HashSet<CharSequence>(), CharSequence.class);
        s.contains("abc");
        s.contains(3); // fails
    }
}
