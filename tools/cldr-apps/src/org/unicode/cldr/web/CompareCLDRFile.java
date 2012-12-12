// Copyright (C) 2012 IBM Corporation and Others. All Rights Reserved.

package org.unicode.cldr.web;

import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile;

public class CompareCLDRFile {

    private Set<Entry> entries = new TreeSet<Entry>();

    private Set<String> allXpaths = new TreeSet<String>();

    public class Entry implements Comparable<Entry> {
        private String title;
        private CLDRFile file;

        private Set<String> xpaths = new TreeSet<String>();

        private Entry(CLDRFile f, String t) {
            this.file = f;
            this.title = t;

            for (String s : f) {
                xpaths.add(s);
            }

            allXpaths.addAll(xpaths);
        }

        public String toString() {
            return title;
        }

        public CLDRFile getFile() {
            return file;
        }

        public Set<String> getXPaths() {
            return xpaths;
        }

        public Set<String> getUniqueXPaths() {
            Set<String> aSet = new TreeSet<String>(getAllXPaths());
            for (Entry e : getResults()) {
                if (e != this) {
                    aSet.removeAll(e.getXPaths());
                }
            }
            return aSet;
        }

        @Override
        public int compareTo(Entry o) {
            // TODO Auto-generated method stub
            return title.compareTo(o.title);
        }
    }

    public CompareCLDRFile() {
    }

    public Entry add(CLDRFile f, String t) {
        Entry e = new Entry(f, t);
        entries.add(e);
        return e;
    }

    public Set<String> getAllXPaths() {
        return allXpaths;
    }

    public Set<Entry> getResults() {
        return entries;
    }
};
