/**
 * 
 */
package org.unicode.cldr.util;

import java.util.Hashtable;
import java.util.Iterator;

import com.ibm.icu.util.ULocale;

/**
 * @author srl
 *
 */
public final class CLDRLocale implements Comparable<CLDRLocale> {
    private CLDRLocale parent = null;
    private ULocale ulocale;
    private String basename;
    private String fullname;
    private LocaleIDParser parts = null;

    /**
     * Construct a CLDRLocale from an ICU ULocale
     */
    private CLDRLocale(ULocale loc) {
        init(loc);
    }

    private CLDRLocale(String s) {
        init(s);
    }

    private void init(ULocale loc) {
        ulocale = loc;
        init(loc.getBaseName());
    }
        
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
    
    public static CLDRLocale getInstance(ULocale u) {
        CLDRLocale loc = ulocToLoc.get(u);
        if(loc == null) {
            loc = new CLDRLocale(u);
            loc.register();
        }
        return loc;
    }
    
    private void register() {
        stringToLoc.put(this.toString(), this);
        ulocToLoc.put(this.toULocale(), this);
    }
    
    private static Hashtable<String,CLDRLocale> stringToLoc= new Hashtable<String,CLDRLocale>();
    private static Hashtable<ULocale,CLDRLocale> ulocToLoc = new Hashtable<ULocale,CLDRLocale>();
    
    
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
        // TODO Auto-generated method stub
        return toULocale().getDisplayName(displayLocale);
    }

    public String getLanguage() {
        // TODO Auto-generated method stub
        return parts.getLanguage();
    }

    public String getScript() {
        // TODO Auto-generated method stub
        return parts.getScript();
    }

    public String getCountry() {
        // TODO Auto-generated method stub
        return parts.getRegion();
    }

    public String getVariant() {
        return toULocale().getVariant(); // TODO: replace with parts?
    }

    public boolean equals(Object o) {
        if(o==this) return true;
        if(!(o instanceof CLDRLocale)) return false;
        return (0==compareTo((CLDRLocale)o));
    }
    public static final CLDRLocale ROOT = getInstance("root");
}

