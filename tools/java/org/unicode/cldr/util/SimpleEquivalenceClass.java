package org.unicode.cldr.util;



import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
public  class SimpleEquivalenceClass {
    
    public SimpleEquivalenceClass(Comparator c) {
        comparator = c;
        itemToSet = new TreeMap(c);
    }
    
    private Map itemToSet;
    
    private Comparator comparator;
    
    public void add(Object a, Object b) {
    	if(a == b) {
    		throw new InternalError("Err! a is b!");
    	}
        Set sa = (Set) itemToSet.get(a);
        Set sb = (Set) itemToSet.get(b);
        if (sa == null && sb == null) { // new set!
            Set s = new TreeSet(comparator);
            s.add(a);
            s.add(b);
            itemToSet.put(a, s);
            itemToSet.put(b, s);
        } else if (sa == null) {
            sb.add(a);
        } else if (sb == null) {
            sa.add(b);
        } else { // merge sets, dumping sb
            sa.addAll(sb);
            Iterator it = sb.iterator();
            while (it.hasNext()) {
                itemToSet.put(it.next(), sa);
            }
        }
    }
    
    private class MyIterator implements Iterator {
        private Iterator it;
        
        MyIterator(Comparator comp) {
            if (comp == null) it = itemToSet.values().iterator();
            else {
            TreeSet values = new TreeSet(comp);
            values.addAll(itemToSet.values());
            it = values.iterator();
            }
        }
        
        public boolean hasNext() {
            return it.hasNext();
        }
        
        public Object next() {
            return it.next();
        }
        
        public void remove() {
            throw new IllegalArgumentException("can't remove");
        }
    }
    
    public Iterator getSetIterator(Comparator comp) {
        return new MyIterator(comp);
    }
    
    public String toString() {
        return itemToSet.values().toString();
    }
}