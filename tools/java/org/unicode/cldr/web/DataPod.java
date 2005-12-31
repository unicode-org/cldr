//
//  DataPod.java
//  all the good names were .. ??
//
//  Created by Steven R. Loomis on 18/11/2005.
//  Copyright 2005 IBM. All rights reserved.
//

package org.unicode.cldr.web;
import org.unicode.cldr.util.*;
import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.test.*;
import java.util.*;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;

/** A data pod represents a group of related data that will be displayed to users in a list
 * such as, "all of the language display names contained in the en_US locale".
 * It is sortable, as well, and has some level of persistence.
 **/

public class DataPod {
    public String locale = null;
    public String xpathPrefix = null;
    
    private String fieldHash; // prefix string used for calculating html fields

    DataPod(SurveyMain sm, String loc, String pref) {
        locale = loc;
        xpathPrefix = pref;
        fieldHash =  CookieSession.cheapEncode(sm.xpt.getByXpath(pref));
    }
    
    /* get a short key for use in fields */
    public String fieldHash(Pea p) {
        return fieldHash + p.fieldHash();
    }

    public String xpath(Pea p) {
        String path = xpathPrefix;
        if(path == null) {
            throw new InternalError("Can't handle mixed peas with no prefix");
        }
        if(p.xpathSuffix == null) {
            if(p.type != null) {
                path = path + "[@type='" + p.type +"']";
            }
            if(p.altType != null) {
                path = path + "[@alt='" + p.altType +"']";
            }
        } else {
            throw new InternalError("Can't handle mixed peas with no suffix");
        }
        
        return path;
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

    static Collator getOurCollator() {
        RuleBasedCollator rbc = 
            ((RuleBasedCollator)Collator.getInstance());
        rbc.setNumericCollation(true);
        return rbc;
    }
    
    final Collator myCollator = getOurCollator();
    
    public class Pea {
        public String type = null;
		public String xpathSuffix = null; // if null:  prefix+type is sufficient (simple list).  If non-null: mixed Pod, prefix+suffix is required and type is informative only.
        public String displayName = null;
        public String altType = null; // alt type (NOT to be confused with -proposedn)
        boolean hasTests = false;
        boolean hasProps = false;
        boolean hasInherited = false;
        String inheritFrom = null;
        public class Item {
            public String altProposed = null; // proposed part of the name (or NULL for nondraft)
            public String value = null; // actual value
            public int id = -1; // id of CLDR_DATA table row
            public List tests = null;
            // anything else? userID? 
        }
        
        public Set items = new TreeSet(new Comparator() {
                    public int compare(Object o1, Object o2){
                        Item p1 = (Item) o1;
                        Item p2 = (Item) o2;
                        if(p1==p2) { 
                            return 0;
                        }
                        if((p1.altProposed==null)&&(p2.altProposed==null)) return 0;
                        if(p1.altProposed == null) return -1;
                        if(p2.altProposed == null) return 1;
                        return myCollator.compare(p1.altProposed, p2.altProposed);
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
        
        String myFieldHash = null;
        public synchronized String fieldHash() {
            if(myFieldHash == null) {
                String ret = "";
                if(type != null) {
                    ret = ret + ":" + type;
                }
                if(xpathSuffix != null) {
                    ret = ret + ":" + xpathSuffix;
                }
                if(altType != null) {
                    ret = ret + ":" + altType;
                }
                myFieldHash = ret;
            }
            return myFieldHash;
        }
        
        Hashtable subPeas = null;
        
        public Pea getSubPea(String altType) {
            if(altType == null) {
                return this;
            }
            if(subPeas == null) {
                subPeas = new Hashtable();
            }

            Pea p = (Pea)subPeas.get(altType);
            if(p==null) {
                p = new Pea();
                p.type = type;
                p.altType = altType;
                subPeas.put(altType, p);
            }
            return p;
        }
    }

    Hashtable peasHash = new Hashtable();
    String oldSortMode = null;
    List peas = null;
 
    /**
     * get all peas.. unsorted.
     */
    public Collection getAll() {
        synchronized(peasHash) {
            return peasHash.values();
        }
    }
    
    /**
     * get a List of peas, in sorted order 
     */
    public List getList(String sortMode) {
        synchronized(peasHash) {
            if(!sortMode.equals(oldSortMode)) {
                peas = null; /* throw out the old list - it's in the wrong order. */
            }
        
            if((peas == null) /* || sortMode != curSortMode... */ ) {
                Set newSet;
                
//                final com.ibm.icu.text.RuleBasedCollator rbc = 
//                    ((com.ibm.icu.text.RuleBasedCollator)com.ibm.icu.text.Collator.getInstance());
//                rbc.setNumericCollation(true);

                
                if(sortMode.equals(SurveyMain.PREF_SORTMODE_CODE)) {
                    newSet = new TreeSet(new Comparator() {
//                        com.ibm.icu.text.Collator myCollator = rbc;
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
//                        com.ibm.icu.text.Collator myCollator = rbc;
                        public int compare(Object o1, Object o2){
                            Pea p1 = (Pea) o1;
                            Pea p2 = (Pea) o2;
                            
                            if(p1==p2) {
                                return 0;
                            }
                            
                            int rv = 0; // neg:  a < b.  pos: a> b
                            
                            if(rv == 0) {
                                if(p1.hasTests) {
                                    rv -= 1000;
                                }
                                if(p2.hasTests) {
                                    rv += 1000;
                                }
                            }
                            
                            if(rv == 0) {
                                if(p1.hasProps) {
                                    rv -= 100; // 0 items last
                                }
                                if(p2.hasProps) {
                                    rv += 100; // 0 items last
                                }
                            }
                            
                            if(rv == 0) {
                                if(!p1.hasInherited) {
                                    rv -= 10; // 0 items last
                                }
                                if(!p2.hasInherited) {
                                    rv += 10; // 0 items last
                                }
                            }

                           if(rv == 0) { // try to avoid a compare
                                int crv = myCollator.compare(p1.type, p2.type);
                                if(crv < 0) {
                                    rv -= 1;
                                } else if(crv > 0) {
                                    rv += 1;
                                }
                            }
                            
                            if(rv < 0) {
                                return -1;
                            } else if(rv > 0) {
                                return 1;
                            } else {
                                return 0;
                            }
                        }
                    });
                } else if(sortMode.equals(SurveyMain.PREF_SORTMODE_NAME)) {
                    newSet = new TreeSet(new Comparator() {
//                        com.ibm.icu.text.Collator myCollator = rbc;
                        public int compare(Object o1, Object o2){
                            Pea p1 = (Pea) o1;
                            Pea p2 = (Pea) o2;
                            if(p1==p2) { 
                                return 0;
                            }
                            return myCollator.compare(p1.displayName, p2.displayName);
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
		DataPod pod = new DataPod(ctx.sm, locale, prefix);
		if(simple==true) {
//            pod.loadStandard(ctx.sm.getEnglishFile()); //load standardcodes + english        
            CLDRDBSource ourSrc = (CLDRDBSource)ctx.getByLocale(SurveyMain.USER_FILE + SurveyMain.CLDRDBSRC, locale);
            CheckCLDR checkCldr = (CheckCLDR)ctx.getByLocale(SurveyMain.USER_FILE + SurveyMain.CHECKCLDR);
            if(checkCldr == null) {
                throw new InternalError("checkCldr == null");
            }
            com.ibm.icu.dev.test.util.ElapsedTimer et = new com.ibm.icu.dev.test.util.ElapsedTimer();
            System.err.println("DP: Starting populate of " + locale + " // " + prefix);
            pod.populateFrom(ourSrc, checkCldr, ctx.sm.getEnglishFile());
            System.err.println("DP: Time to populate " + locale + " // " + prefix + " = " + et);
		} else {
			throw new InternalError("non-simple pods not supported");
		}
		return pod;
	}
    /*
    private void loadStandard(CLDRFile engf)
    {
        XPathParts xpp = new XPathParts(null,null);
        System.out.println("Loading with prefix: '" + xpathPrefix + "'");
        xpp.initialize(xpathPrefix);
        String subtype = xpp.getElement(-1);
        StandardCodes standardCodes = StandardCodes.make();
        Set defaultSet = standardCodes.getAvailableCodes(subtype);
        if(defaultSet != null) {
            for(Iterator i = defaultSet.iterator();i.hasNext();)
            {
                String code = (String)i.next();
                Pea p = getPea(code);
                if(p == null) {
                    p = new Pea();
                    p.type = code;
                    // p.altType, etc..
                    addPea(p);
                }
                p.hasInherited=true;
                p.displayName = engf.get
//                p.displayName = standardCodes.getData(subtype, code); // Hack - should use English. 
            }
        }
    }*/
    
    private void populateFrom(CLDRDBSource src, CheckCLDR checkCldr, CLDRFile engFile) {
        XPathParts xpp = new XPathParts(null,null);
        System.out.println("[] initting from pod " + locale + " with prefix " + xpathPrefix);
        CLDRFile aFile = new CLDRFile(src, true);
        Map options = new TreeMap();
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
            if(-1!=xpath.indexOf("001")) {
                System.err.println("001:: " + xpath + aFile.getSourceLocaleID(xpath));
//                if(aFile.getSourceLocaleID(xpath).equals("code-fallback")) {
//                    throw new InternalError("No err!");
//                }
            }
            String type = src.xpt.typeFromPathToTinyXpath(xpath, xpp);
            boolean mixedType = false;
            if(type == null) {
                type = xpath.substring(xpathPrefix.length(),xpath.length());
                mixedType = true;
            }
            String value = aFile.getStringValue(xpath);
            if(value == null) {
//                throw new InternalError("Value of " + xpath + " is null.");
                  System.err.println("Value of " + xpath + " is null.");
                 value = "(NOTHING)";
            }
            String fullPath = aFile.getFullXPath(xpath);
//            System.out.println("* T=" + type + ", X= " + xpath);
            String alt = src.xpt.altFromPathToTinyXpath(xpath, xpp);

            xpp.clear();
            xpp.initialize(xpath);
            String lelement = xpp.getElement(-1);
            /* all of these are always at the end */
            String eAlt = xpp.findAttributeValue(lelement,LDMLConstants.ALT);
            String eDraft = xpp.findAttributeValue(lelement,LDMLConstants.DRAFT);

            String typeAndProposed[] = LDMLUtilities.parseAlt(alt);
            String altProposed = typeAndProposed[1];
            String altType = typeAndProposed[0];
            Pea p = getPea(type, altType);
            Pea superP = getPea(type);
            
            if(altProposed == null) {
                // just work on the supers
                if(superP.displayName == null) {
                    if(xpathPrefix.startsWith("//ldml/localeDisplayNames/")) {
                        if(mixedType == false) {
                            superP.displayName = engFile.getStringValue(xpathPrefix+"[@type=\""+type+"\"]");
                        } else {
                            superP.displayName = engFile.getStringValue(xpathPrefix);
                        }
                    }
                }
                if(superP.displayName == null) {
                    superP.displayName = "'"+type+"'";
                }
            }
            
            // If it is draft and not proposed.. make it proposed-draft 
            if( ((eDraft!=null)&&(!eDraft.equals("false"))) &&
                (altProposed == null) ) {
                altProposed = SurveyMain.PROPOSED_DRAFT;
            }
            
/*srl*/            if(-1!=xpath.indexOf("ain")) {
                System.err.println("ain:: " + xpath + aFile.getSourceLocaleID(xpath) + " - AP:" + altProposed + " - AT: " + altType );
//                if(aFile.getSourceLocaleID(xpath).equals("code-fallback")) {
//                    throw new InternalError("No err!");
//                }
            }

            // Inherit display names.
            if((superP != p) && (p.displayName == null)) {
                p.displayName = superP.displayName;
            }
            String sourceLocale = aFile.getSourceLocaleID(xpath);
            boolean isInherited = !(sourceLocale.equals(locale));
            
            if(altProposed == null) {
                if(!isInherited) {
                    superP.hasInherited=false;
                    p.hasInherited=false;
                } else {
                    p.hasInherited = true;
                    p.inheritFrom = sourceLocale;
                }
            } else {
                p.hasProps = true;
                superP.hasProps = true;
            }

            if((checkCldr != null)&&(altProposed == null)) {
                checkCldr.check(xpath, fullPath, value, options, checkCldrResult);
            }
/*srl*/     if(-1!=xpath.indexOf("ain")) {
                System.err.println("ain+- was have " + p.items.size() + " Items ");
            }
            if(checkCldrResult.isEmpty()) {
                p.addItem( value, altProposed, null);
            } else {
                p.addItem( value, altProposed, checkCldrResult);
                // only consider non-example tests as notable.
                boolean weHaveTests = false;
                for (Iterator it3 = checkCldrResult.iterator(); it3.hasNext();) {
                    CheckCLDR.CheckStatus status = (CheckCLDR.CheckStatus) it3.next();
                    if(!status.getType().equals(status.exampleType)) {
                        weHaveTests = true;
                    }
                }
                if(weHaveTests) {
                    p.hasTests = true;
                    superP.hasTests = true;
                }
                // set the parent
                checkCldrResult = new ArrayList(); // can't reuse it if nonempty
            }
/*srl*/     if(-1!=xpath.indexOf("ain")) {
                System.err.println("ain+: now have " + p.items.size() + " Items ");
            }
        }
        
//        aFile.close();
    }
    private Pea getPea(String type) {
        if(type == null) {
            throw new InternalError("type is null");
        }
        if(peasHash == null) {
            throw new InternalError("peasHash is null");
        }
        Pea p = (Pea)peasHash.get(type);
        if(p == null) {
            p = new Pea();
            p.type = type;
            addPea(p);
        }
        return p;
    }
    
    private Pea getPea(String type, String altType) {
        if(altType == null) {
            return getPea(type);
        } else {
            Pea superPea = getPea(type);
            return superPea.getSubPea(altType);
        }
    }
    
    void addPea(Pea p) {
        peasHash.put(p.type, p);
    }
    
    public String toString() {
        return "{DataPod " + locale + ":" + xpathPrefix + " #" + key + "} ";
    }
}
