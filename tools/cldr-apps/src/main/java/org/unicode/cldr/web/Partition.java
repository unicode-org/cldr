/** Copyright (C) 2011-2012 IBM Corporation and Others. All Rights Reserved. */
package org.unicode.cldr.web;

/**
 * @author srl
 *     <p>This would be Generic except generic arrays are UGLY. http://stackoverflow
 *     .com/questions/529085/java-how-to-generic-array-creation
 */
public class Partition {

    public String name; // name of this partition
    public String helptext; // help text for this partition
    public int start; // first item
    public int limit; // after last item

    public Partition(String n, int s, int l) {
        name = n;
        start = s;
        limit = l;
    }

    @Override
    public String toString() {
        return name + " - [" + start + ".." + limit + "]";
    }
}
