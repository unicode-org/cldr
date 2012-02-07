// Copyright (C) 2008-2009 IBM Corporation and Others. All Rights Reserved.

package org.unicode.cldr.util;

import java.util.Hashtable;
import java.util.Iterator;

import com.ibm.icu.util.ULocale;

/**
 * This class implements a CLDR UTS#35 compliant locale.
 * It differs from ICU and Java locales in that it is singleton based, and that it is Comparable.
 * It uses LocaleIDParser to do the heavy lifting of parsing.
 * 
 * @author srl
 * @see LocaleIDParser
 * @see ULocale
 */
public final class CLDRLocale implements Comparable<CLDRLocale> {
	/**
	 * Reference to the parent CLDRLocale
	 */
    private CLDRLocale parent = null;
    /**
     * Cached ICU format locale
     */
    private ULocale ulocale;
    /**
     * base name, 'without parameters'. Currently same as fullname.
     */
    private String basename;
    /**
     * Full name
     */
    private String fullname;
    /**
     * The LocaleIDParser interprets the various parts (language, country, script, etc).
     */
    private LocaleIDParser parts = null;

    /**
     * Construct a CLDRLocale from an ICU ULocale.
     * Internal, called by the factory function.
     */
    private CLDRLocale(ULocale loc) {
        init(loc);
    }

    /**
     * Construct a CLDRLocale from a string with the full locale ID.
     * Internal, called by the factory function.
     * @param str
     */
    private CLDRLocale(String str) {
        init(str);
    }

    /**
     * Initialize a CLDRLocale from a ULocale
     * @param loc
     */
    private void init(ULocale loc) {
        ulocale = loc;
        init(loc.getBaseName());
    }
     
    /**
     * Initialize a CLDRLocale from a string.
     * @param str
     */
    private void init(String str) {
//        if(str.length()==0) {
//            str = "root";
//        }
        str = process(str);
        //System.err.println("bn: " + str);
        parts = new LocaleIDParser();
        parts.set(str);
        basename = fullname = parts.toString();
        if(ulocale == null) {
            ulocale = new ULocale(fullname);
        }
        String parentId = LocaleIDParser.getParent(str);
        if(parentId != null) {
            parent = CLDRLocale.getInstance(parentId);
        } else {
            parent = null; // probably, we are root or we are supplemental
        }
    }

    /**
     * Return the full locale name, in CLDR format.
     */
    public String toString() {
        return fullname;
    }
    
    /**
     * Return the base locale name, in CLDR format, without any @keywords
     * @return
     */
    public String getBaseName() {
        return basename;
    }

    /**
     * internal: process a string from ICU to CLDR form. For now, just collapse double underscores.
     * @param baseName
     * @return
     * @internal
     */
    private String process(String baseName) {
        return baseName.replaceAll("__", "_");
    }

    /**
     * Compare to another CLDRLocale. Uses string order of toString().
     */
    public int compareTo(CLDRLocale o) {
        if(o==this) return 0;
        return fullname.compareTo(o.fullname);
    }
    
    /**
     * Hashcode - is the hashcode of the full string
     */
    public int hashCode()
    {
        return fullname.hashCode();
    }
    
    /**
     * Convert to an ICU compatible ULocale. 
     * @return
     */
    public ULocale toULocale() {
        return ulocale;
    }
    
    /**
     * Allocate a CLDRLocale (could be a singleton).  If null is passed in, null will be returned.
     * @param s
     * @return
     */
    public static CLDRLocale getInstance(String s) {
        if(s==null) return null;
        CLDRLocale loc = stringToLoc.get(s);
        if(loc == null) {
            loc = new CLDRLocale(s);
            loc.register();
        }
        return loc;
    }
    
    /**
     * Public factory function.  Allocate a CLDRLocale (could be a singleton).  If null is passed in, null will be returned.
     * @param u
     * @return
     */
    public static CLDRLocale getInstance(ULocale u) {
    	if(u==null) return null;
        CLDRLocale loc = ulocToLoc.get(u);
        if(loc == null) {
            loc = new CLDRLocale(u);
            loc.register();
        }
        return loc;
    }
    
    /**
     * Register the singleton instance.
     */
    private void register() {
        stringToLoc.put(this.toString(), this);
        ulocToLoc.put(this.toULocale(), this);
    }
    
    private static Hashtable<String,CLDRLocale> stringToLoc= new Hashtable<String,CLDRLocale>();
    private static Hashtable<ULocale,CLDRLocale> ulocToLoc = new Hashtable<ULocale,CLDRLocale>();
    
   /**
     * Return the parent locale of this item. Null if no parent (root has no parent)
     * @return
     */
    public CLDRLocale getParent() {
        return parent;
    }

    /**
     * Return an iterator that will iterate over locale, parent, parent etc, finally reaching root.
     * @return
     */
    public Iterable<CLDRLocale> getParentIterator() {
        final CLDRLocale newThis = this;
        return new Iterable<CLDRLocale> () {
            public Iterator<CLDRLocale> iterator() {
                return new Iterator<CLDRLocale> () {
                    CLDRLocale what = newThis;
                    public boolean hasNext() {
                        // TODO Auto-generated method stub
                        return what.getParent()!=null;
                    }

                    public CLDRLocale next() {
                        // TODO Auto-generated method stub
                        CLDRLocale curr = what;
                        if(what != null) {
                            what = what.getParent();
                        }
                        return curr;
                    }

                    public void remove() {
                        throw new InternalError("unmodifiable iterator");
                    }
                    
                };
            }
        };
    }
   
    public String getDisplayName(ULocale displayLocale) {
        return toULocale().getDisplayName(displayLocale);
    }

    public String getLanguage() {
        return parts.getLanguage();
    }

    public String getScript() {
        return parts.getScript();
    }

    /**
     * Return the region
     * @return
     */
    public String getCountry() {
        return parts.getRegion();
    }

    /**
     * Return "the" variant.
     * @return
     */
    public String getVariant() {
        return toULocale().getVariant(); // TODO: replace with parts?
    }

    /**
     * Most objects should be singletons, and so equality/inequality comparison is done first.
     */
    public boolean equals(Object o) {
        if(o==this) return true;
        if(!(o instanceof CLDRLocale)) return false;
        return (0==compareTo((CLDRLocale)o));
    }
    
    /**
     * The root locale, a singleton.
     */
    public static final CLDRLocale ROOT = getInstance("root");
    
    /**
     * Testing.
     * @param args
     */
    public static void main(String args[]) {
        System.out.println("Tests for CLDRLocale:");
        String tests_str[] = { "", "root", "el__POLYTON", "el_POLYTON", "__UND"};
        for(String s:tests_str) {
            CLDRLocale loc = CLDRLocale.getInstance(s);
            System.out.println(s+":  tostring:"+loc.toString()+", uloc:"+loc.toULocale().toString()+", fromloc:"+new ULocale(s).toString());
        }
    }

 }

