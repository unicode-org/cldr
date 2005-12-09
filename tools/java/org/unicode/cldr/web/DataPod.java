//
//  DataPod.java
//  all the good names were .. ??
//
//  Created by Steven R. Loomis on 18/11/2005.
//  Copyright 2005 IBM. All rights reserved.
//

package org.unicode.cldr.web;
import org.unicode.cldr.util.*;
import org.unicode.cldr.test.*;
import java.util.*;

/** A data pod represents a group of related data that will be displayed to users in a list
 * such as, "all of the language display names contained in the en_US locale".
 * It is sortable, as well, and has some level of persistence.
 **/

public class DataPod {
    public String locale = null;
    public String xpathPrefix = null;

    DataPod(String loc, String pref) {
        locale = loc;
        xpathPrefix = pref;
    }
    
    /** The unit of data within the pod.  contains all data of the specified Type. */
    boolean valid = true;
    public boolean isValid(LocaleChangeRegistry lcr) {
        if(valid) { 
            if(!lcr.isKeyValid(locale, key)) {
                //lcr.unregister();
                valid=false;
            }
        }
        return valid;
    }
    public void register(LocaleChangeRegistry lcr) {
        lcr.register(locale, key, this);
    }
    private String key = LocaleChangeRegistry.newKey(); // key for this item
    
    public class Pea {
        public String type = null;
		public String xpathSuffix = null; // if null:  prefix+type is sufficient (simple list).  If non-null: mixed Pod, prefix+suffix is required and type is informative only.
        public String displayName = null;
        public String altType = null; // alt type (NOT to be confused with -proposedn)
        boolean hasTests = false;
        public class Item {
            public String altProposed = null; // proposed part of the name (or NULL for nondraft)
            public String value = null; // actual value
            public int id = -1; // id of CLDR_DATA table row
            public List tests = null;
            // anything else? userID? 
        }
        
        public Set items = new TreeSet(new Comparator() {
                    public int compare(Object o1, Object o2){
                        //com.ibm.icu.text.Collator myCollator = com.ibm.icu.text.Collator.getInstance();
                        Item p1 = (Item) o1;
                        Item p2 = (Item) o2;
                        if(p1==p2) { 
                            return 0;
                        }
                        if((p1.altProposed==null)&&(p2.altProposed==null)) return 0;
                        if(p1.altProposed == null) return -1;
                        if(p2.altProposed == null) return 1;
                       // return myCollator.compare(p1.type, p2.type);
                       return 0;
                    }
                });
        void addItem(String value, String altProposed, List tests) {
            Item pi = new Item();
            pi.value = value;
            pi.altProposed = altProposed;
            pi.tests = tests;
            items.add(pi);
//            System.out.println("  v: " + pi.value);
        }
    }
	
    Hashtable peasHash = new Hashtable();
    String oldSortMode = null;
    List peas = null;

    public List getList(String sortMode) {
        synchronized(peasHash) {
            if(!sortMode.equals(oldSortMode)) {
                peas = null; /* invalid */
            }
        
            if((peas == null) /* || sortMode != curSortMode... */ ) {
                Set newSet;
                if(sortMode.equals(SurveyMain.PREF_SORTMODE_CODE)) {
                    newSet = new TreeSet(new Comparator() {
                        com.ibm.icu.text.Collator myCollator = com.ibm.icu.text.Collator.getInstance();
                        public int compare(Object o1, Object o2){
                            Pea p1 = (Pea) o1;
                            Pea p2 = (Pea) o2;
                            if(p1==p2) { 
                                return 0;
                            }
                            return myCollator.compare(p1.type, p2.type);
                        }
                    });
                } else if (sortMode.equals(SurveyMain.PREF_SORTMODE_WARNING)) {
                    newSet = new TreeSet(new Comparator() {
                        com.ibm.icu.text.Collator myCollator = com.ibm.icu.text.Collator.getInstance();
                        public int compare(Object o1, Object o2){
                            Pea p1 = (Pea) o1;
                            Pea p2 = (Pea) o2;
                            if(p1==p2) { 
                                return 0;
                            }
                            if(!(p1.hasTests&&p2.hasTests)) {
                                if(p1.hasTests) {
                                    return -1;
                                }
                                if(p2.hasTests) {
                                    return 1;
                                }
                            }
                            if(p1.items.size() > p2.items.size()) {
                                return -1;
                            } else if(p1.items.size() < p2.items.size()) {
                                return 1;
                            }
                            return myCollator.compare(p1.type, p2.type);
                        }
                    });
                } else {
                    throw new InternalError("Unknown or unsupported sort mode: " + sortMode);
                }
                newSet.addAll(peasHash.values()); // sort it    
                
                peas = new ArrayList(); // list it (waste here??)
                peas.addAll(newSet);
            }
            oldSortMode = sortMode;
            return peas;
        }
    }
    
    /** Returns a list parallel to that of getList() but of Strings suitable for display. 
    (Alternate idea: just make toString() do so on Pea.. advantage here is we can adjust for sort mode.) **/
    public List getDisplayList(String sortMode) {
        final List myPeas = getList(sortMode);
        return new AbstractList() {
            private List p = myPeas;
            public Object get(int n) { return ((Pea)p.get(n)).type; }
            public int size() { return p.size(); }
        };
    }
    
	/**
	 * @param ctx context to use (contains CLDRDBSource, etc.)
	 * @param locale locale
	 * @param prefix XPATH prefix
	 * @param simple if true, means that data is simply xpath+type. If false, all xpaths under prefix.
	 */
	public static DataPod make(WebContext ctx, String locale,String prefix, boolean simple) {
		DataPod pod = new DataPod(locale, prefix);
		if(simple==true) {
            CLDRDBSource ourSrc = (CLDRDBSource)ctx.getByLocale(SurveyMain.USER_FILE + SurveyMain.CLDRDBSRC, locale);
            CheckCLDR checkCldr = (CheckCLDR)ctx.getByLocale(SurveyMain.USER_FILE + SurveyMain.CHECKCLDR);
            if(checkCldr == null) {
                throw new InternalError("checkCldr == null");
            }
            pod.populateFrom(ourSrc, checkCldr);
		} else {
			throw new InternalError("non-simple pods not supported");
		}
		return pod;
	}
    
    private void populateFrom(CLDRDBSource src, CheckCLDR checkCldr) {
        XPathParts xpp = new XPathParts(null,null);
        System.out.println("[] initting from pod  with pref " + xpathPrefix);
        CLDRFile aFile = new CLDRFile(src, false);
        XPathParts pathParts = new XPathParts(null, null);
        XPathParts fullPathParts = new XPathParts(null, null);
/*
                checkCldr.check(path, fullPath, value, pathParts, fullPathParts, checkCldrResult);
                for (Iterator it3 = checkCldrResult.iterator(); it3.hasNext();) {
                    CheckCLDR.CheckStatus status = (CheckCLDR.CheckStatus) it3.next();
                    if (!status.getType().equals(status.exampleType)) {
                        ctx.println(status.toString() + "\t" + value + "\t" + fullPath);
*/
        List checkCldrResult = new ArrayList();
        for(Iterator it = aFile.iterator(xpathPrefix);it.hasNext();) {
            String xpath = (String)it.next();
            String type = src.xpt.typeFromPathToTinyXpath(xpath, xpp);
            String value = aFile.getStringValue(xpath);
            String fullPath = aFile.getFullXPath(xpath);
//            System.out.println("* T=" + type + ", X= " + xpath);
            String alt = src.xpt.altFromPathToTinyXpath(xpath, xpp);
            String altProposed = alt; // TODO: 0 should be a PARSE
            Pea p = getPea(type);
            if(p == null) {
                p = new Pea();
                p.type = type;
                // p.altType, etc..
                addPea(p);
            }
            if(checkCldr != null) {
                checkCldr.check(xpath, fullPath, value, pathParts, fullPathParts, checkCldrResult);
            }
            if(checkCldrResult.isEmpty()) {
                p.addItem( value,  altProposed, null);
            } else {
                p.addItem( value, altProposed, checkCldrResult);
                p.hasTests = true;
                checkCldrResult = new ArrayList(); // can't reuse it if nonempty
            }
        }
        
//        aFile.close();
    }
    private Pea getPea(String type) {
        return (Pea)peasHash.get(type);
    }
    
    void addPea(Pea p) {
        peasHash.put(p.type, p);
    }
    
    public String toString() {
        return "{DataPod " + locale + ":" + xpathPrefix + " #" + key + "} ";
    }
}
