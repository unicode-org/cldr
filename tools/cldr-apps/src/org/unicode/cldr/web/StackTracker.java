package org.unicode.cldr.web;

import java.util.Hashtable;
import java.util.Map;


public class StackTracker {
    private Hashtable<Object,String> stacks = new Hashtable<Object,String>();
    
    /**
     * Add object
     * @param o
     */
    public void add(Object o) {
        String stack = currentStack();
        stacks.put(o, stack);
    }
    
    /**
     * remove obj
     * @param o
     */
    public void remove(Object o) {
        stacks.remove(o);
    }

    /**
     * internal - convert a stack to string
     * @param stackTrace
     * @param skip
     * @return
     */
    public static String stackToString(StackTraceElement[] stackTrace, int skip) {
        StringBuffer sb = new StringBuffer();
        for(int i=0;i<stackTrace.length;i++) {
            sb.append(stackTrace[i].toString()+"\n");
        }
        return sb.toString();
    }
    
    /**
     * get this tracker as a string
     */
    public String toString() {
        if(stacks.isEmpty()) {
            return "{Empty StackTracker}";
        }
        StringBuffer sb = new StringBuffer();
        
        sb.append("{");
        int n=0;
        for(Map.Entry<Object,String> e : stacks.entrySet()) {
            sb.append("\n");
            sb.append("#"+(++n)+"/"+stacks.size()+"\n");
            sb.append(e.getValue()+"\n\n");
        }
        sb.append("}");
        return sb.toString();
    }

    public void clear() {
        stacks.clear();
        
    }
    
    public static String currentStack() {
    	return stackToString(Thread.currentThread().getStackTrace(),2);
    }
}
