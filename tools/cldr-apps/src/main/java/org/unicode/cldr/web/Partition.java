/**
 * Copyright (C) 2011-2012 IBM Corporation and Others. All Rights Reserved.
 *
 */
package org.unicode.cldr.web;

/**
 * @author srl
 *
 *         This would be Generic except generic arrays are UGLY.
 *         http://stackoverflow
 *         .com/questions/529085/java-how-to-generic-array-creation
 */
public class Partition {
    public static abstract class Membership {
        public String name;
        public String helptext;

        public String name() {
            return name;
        }

        public String helptext() {
            return helptext;
        }

        protected Membership(String name) {
            this.name = name;
            this.helptext = "";
        }

        protected Membership(String name, String helptext) {
            this.name = name;
            this.helptext = helptext;
        }

        public abstract boolean isMember(DataSection.DataRow p);
    };

    public Membership pm;

    public String name; // name of this partition
    public String helptext; // help text for this partition
    public int start; // first item
    public int limit; // after last item

    public Partition(String n, int s, int l) {
        name = n;
        start = s;
        limit = l;
    }

    public Partition(Membership pm) {
        this.pm = pm;
        name = pm.name();
        helptext = pm.helptext();
        start = -1;
        limit = -1;
    }

    public String toString() {
        return name + " - [" + start + ".." + limit + "]";
    }

};
