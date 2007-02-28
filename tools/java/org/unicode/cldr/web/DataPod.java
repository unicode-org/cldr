//
//  DataPod.java
//
//  Created by Steven R. Loomis on 18/11/2005.
//  Copyright 2005-2007 IBM. All rights reserved.
//

//  TODO: this class now has lots of knowledge about specific data types.. so does SurveyMain
//  Probably, it should be concentrated in one side or another- perhaps SurveyMain should call this
//  class to get a list of displayable items?

package org.unicode.cldr.web;
import org.unicode.cldr.util.*;
import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.test.*;
import java.util.*;
import java.util.regex.*;

import com.ibm.icu.text.Collator;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.text.RuleBasedCollator;

/** A data pod represents a group of related data that will be displayed to users in a list
 * such as, "all of the language display names contained in the en_US locale".
 * It is sortable, as well, and has some level of persistence.
 **/

public class DataPod extends Registerable {

/*
    Ballast.  [ used for memory testing ]
    
    int r[] = new int[1024000]; // red sand
    int g[] = new int[1024000]; // grn sand
    int b[] = new int[1024000]; // blu sand
*/
    
    long touchTime = -1; // when has this pod been hit?
    
    public void touch() {
        touchTime = System.currentTimeMillis();
    }
    public long age() {
        return System.currentTimeMillis() - touchTime;
    }
    // UI strings
    boolean canName = true;
    boolean simple = false; // is it a 'simple code list'?
    
    public static final String DATAPOD_MISSING = "Inherited";
    public static final String DATAPOD_NORMAL = "Normal";
    public static final String DATAPOD_PRIORITY = "Priority";
    public static final String DATAPOD_PROPOSED = "Proposed";
    public static final String DATAPOD_VETPROB = "Vetting Issue";

    public static final String EXEMPLAR_ONLY = "//ldml/dates/timeZoneNames/zone/*/exemplarCity";
    public static final String EXEMPLAR_PARENT = "//ldml/dates/timeZoneNames/zone";

    public String xpathPrefix = null;
    
//    public boolean exemplarCityOnly = false;
    
    private String fieldHash; // prefix string used for calculating html fields
    private SurveyMain sm;
    
    public boolean hasExamples = false;
    
    public ExampleGenerator exampleGenerator = null;

    public String intgroup; 
    DataPod(SurveyMain sm, String loc, String prefix) {
        super(sm.lcr,loc); // initialize call to LCR

        this.sm = sm;
/*        if(prefix.equals(EXEMPLAR_ONLY)) {
            prefix = "//ldml/dates/timeZoneNames/zone";
            exemplarCityOnly = true;
        }*/
        xpathPrefix = prefix;
        fieldHash =  CookieSession.cheapEncode(sm.xpt.getByXpath(prefix));
        intgroup = new ULocale(loc).getLanguage(); // calculate interest group
    }
    private static int n =0;
    protected static synchronized int getN() { return ++n; }
    
    /** 
     * This class represents an Example box, so that it can be stored and restored.
     */ 
    public class ExampleEntry {

        public String hash = null;

        public DataPod pod;
        public DataPod.Pea pea;
        public Pea.Item item;
        public CheckCLDR.CheckStatus status;
        
        public ExampleEntry(DataPod pod, Pea p, Pea.Item item, CheckCLDR.CheckStatus status) {
            this.pod = pod;
            this.pea = p;
            this.item = item;
            this.status = status;

            hash = CookieSession.cheapEncode(DataPod.getN()) +  // unique serial #- covers item, status..
                this.pod.fieldHash(p);   /* fieldHash ensures that we don't get the wrong field.. */
        }
    }
    Hashtable exampleHash = new Hashtable(); // hash of examples
    
    /**
     * Enregister an ExampleEntry
     */
    ExampleEntry addExampleEntry(ExampleEntry e) {
        synchronized(exampleHash) {
            exampleHash.put(e.hash,e);
        }
        return e; // for the hash.
    }
    
    /**
     * Given a hash, see addExampleEntry, retrieve the ExampleEntry which has pod, pea, item and status
     */
    ExampleEntry getExampleEntry(String hash) {
        synchronized(exampleHash) {
            return (DataPod.ExampleEntry)exampleHash.get(hash);
        }
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
//            if(p.xpathSuffix.startsWith("[")) {
                return xpathPrefix +  p.xpathSuffix;
//            } else {
//                return xpathPrefix+"/"+p.xpathSuffix;
//            }
        }
        
        return path;
    }
        
    static Collator getOurCollator() {
        RuleBasedCollator rbc = 
            ((RuleBasedCollator)Collator.getInstance());
        rbc.setNumericCollation(true);
        return rbc;
    }
    
    final Collator myCollator = getOurCollator();
    
    public class Pea {
        Pea superPea = this; // parent - defaults to self if it is a super pea (i.e. parent without any alt)
        public boolean confirmOnly = false; // if true: don't accept new data, this pea is something strange.
        public String type = null;
        public String xpathSuffix = null; // if null:  prefix+type is sufficient (simple list).  If non-null: mixed Pod, prefix+suffix is required and type is informative only.
        public String displayName = null;
        public String altType = null; // alt type (NOT to be confused with -proposedn)
        int base_xpath = -1;
        boolean hasTests = false;
        boolean hasErrors = false;
        boolean hasWarnings = false;
        boolean hasProps = false;
        boolean hasInherited = false;
        public int voteType = 0; // bitmask of all voting types included
        public int reservedForSort = -1; // reserved to use in collator.
//        String inheritFrom = null;
//        String pathWhereFound = null;
        public class Item {
            String pathWhereFound = null;
            String inheritFrom = null;
            public String altProposed = null; // proposed part of the name (or NULL for nondraft)
            public String value = null; // actual value
            public int id = -1; // id of CLDR_DATA table row
            public List tests = null;
            public Vector examples = null; 
            //public List examplesList = null;
            String references = null;
            String xpath = null;
            int xpathId = -1;
            
            public Set votes = null; // Set of Users who voted for this.
            
            public String example = "";
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
        public Item addItem(String value, String altProposed, List tests) {
            Item pi = new Item();
            pi.value = value;
            pi.altProposed = altProposed;
            pi.tests = tests;
            items.add(pi);
///*srl*/            if(type.indexOf("Chicago")>-1) {
//                System.out.println(type+"  v: " + pi.value);
//            }
            
            return pi;
        }
        
        String myFieldHash = null;
        public String fieldHash() { // deterministic. No need for sync.
            if(myFieldHash == null) {
                String ret = "";
                if(type != null) {
                    ret = ret + ":" + CookieSession.cheapEncode(type.hashCode());
                }
                if(xpathSuffix != null) {
                    ret = ret + ":" + CookieSession.cheapEncode(xpathSuffix.hashCode());
                }
                if(altType != null) {
                    ret = ret + ":" + CookieSession.cheapEncode(altType.hashCode());
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
                p.superPea = this;
                subPeas.put(altType, p);
            }
            return p;
        }
    }

    Hashtable peasHash = new Hashtable(); // hashtable of type->Pea
 
    /**
     * get all peas.. unsorted.
     */
    public Collection getAll() {
        return peasHash.values();
    }
    
    public abstract class PartitionMembership {
        public abstract boolean isMember(Pea p);
    };
    public class Partition {

        public PartitionMembership pm;

        public String name; // name of this partition
        public int start; // first item
        public int limit; // after last item

        public Partition(String n, int s, int l) {
            name = n;
            start = s;
            limit = l;
        }
        
        public Partition(String n, PartitionMembership pm) {
            name = n;
            this.pm = pm;
            start = -1;
            limit = -1;
        }
        
        public String toString() {
            return name + " - ["+start+".."+limit+"]";
        }

    };

    /** 
     * A class representing a list of peas, in sorted and divided order.
     */
    public class DisplaySet {
        public int size() {
            return peas.size();
        }
        String sortMode = null;
        public boolean canName = true; // can use the 'name' view?
        public List peas; // list of peas in sorted order
        public List displayPeas; // list of Strings suitable for display
        /**
         * Partitions divide up the peas into sets, such as 'proposed', 'normal', etc.
         * The 'limit' is one more than the index number of the last item.
         * In some cases, there is only one partition, and its name is null.
         */
        
        public Partition partitions[];  // display group partitions.  Might only contain one entry:  {null, 0, <end>}.  Otherwise, contains a list of entries to be named separately

        public DisplaySet(List myPeas, List myDisplayPeas, String sortMode) {
            this.sortMode = sortMode;
            
            peas = myPeas;
            displayPeas = myDisplayPeas;

            /*
            if(matcher != null) {
                List peas = new ArrayList();
                List displayPeas = new ArrayList();
                peas.addAll(myPeas);
                displayPeas.addAll(displayPeas);
                
                for(Object o : myPeas) {
                    Pea p = (Pea)o;
                    if(!matcher.matcher(p.type).matches()) {
                        peas.remove(o);
                    }
                }
                for(Object o : myDisplayPeas) {
                    Pea p = (Pea)o;
                    if(!matcher.matcher(p.type).matches()) {
                        displayPeas.remove(o);
                    }
                }
                System.err.println("now " +peas.size()+"/"+displayPeas.size()+" versus " + myPeas.size()+"/"+myDisplayPeas.size());
            }
            */
            
            // fetch partitions..
            Vector v = new Vector();
            if(sortMode.equals(SurveyMain.PREF_SORTMODE_WARNING)) {
                Partition testPartitions[] = SurveyMain.phaseSubmit?createSubmitPartitions():
                                                                           createVettingPartitions();
                // find the starts
                int lastGood = 0;
                Pea peasArray[] = null;
                peasArray = (Pea[])peas.toArray(new Pea[0]);
                for(int i=0;i<peasArray.length;i++) {
                    Pea p = peasArray[i];
                                        
                    for(int j=lastGood;j<testPartitions.length;j++) {
                        if(testPartitions[j].pm.isMember(p)) {
                            if(j>lastGood) {
                                lastGood = j;
                            }
                            if(testPartitions[j].start == -1) {
                                testPartitions[j].start = i;
                            }
                            break; // sit here until we fail membership
                        }
                        
                        if(testPartitions[j].start != -1) {
                            testPartitions[j].limit = i;
                        }
                    }
                }
                // catch the last item
                if((testPartitions[lastGood].start != -1) &&
                    (testPartitions[lastGood].limit == -1)) {
                    testPartitions[lastGood].limit = peas.size(); // limit = off the end.
                }
                    
                for(int j=0;j<testPartitions.length;j++) {
                    if(testPartitions[j].start != -1) {
						if(testPartitions[j].start!=0 && v.isEmpty()) {
//							v.add(new Partition("Other",0,testPartitions[j].start));
						}
                        v.add(testPartitions[j]);
                    }
                }
            } else {
                // default partition
                v.add(new Partition(null, 0, peas.size()));
            }
            partitions = (Partition[])v.toArray(new Partition[0]); // fold it up
        }

    }

	public static String CHANGES_DISPUTED = "Changes Proposed: Disputed";


    private Partition[] createVettingPartitions() {
        Partition theTestPartitions[] = 
        {                 
                new Partition("Changes Proposed: Insufficient Votes", 
                    new PartitionMembership() { 
                        public boolean isMember(Pea p) {
//		System.err.println("CPI: "+Vetting.typeToStr(p.voteType)+" - " + p.type);
                            return  (p.voteType == Vetting.RES_INSUFFICIENT) ||
                                (p.voteType == Vetting.RES_NO_VOTES);
                        }
                    }),
                new Partition(CHANGES_DISPUTED, 
                    new PartitionMembership() { 
                        public boolean isMember(Pea p) {
                            return ((p.voteType & Vetting.RES_DISPUTED)>0);
                        }
                    }),
                new Partition("Changes Proposed: Tentatively Approved", 
                    new PartitionMembership() { 
                        public boolean isMember(Pea p) {
                            return ((p.hasProps)&&
                                ((p.voteType & Vetting.RES_BAD_MASK)==0)&&
                                    (p.voteType>0)); // has proposed, and has a 'good' mark. Excludes by definition RES_NO_CHANGE
                        }
                    }),
/*                new Partition("Other "+DATAPOD_VETPROB + " [internal error]",  // should not appear?
                    new PartitionMembership() { 
                        public boolean isMember(Pea p) {
                            return ((p.voteType & Vetting.RES_BAD_MASK)>0);
                        }
                    }),
    */
                new Partition("No Changes Proposed: Questionable Values", 
                    new PartitionMembership() { 
                        public boolean isMember(Pea p) {
                            return (p.hasTests&&!p.hasProps) && ((p.voteType==0) || (p.voteType==Vetting.RES_NO_VOTES)
                                    || (p.voteType==Vetting.RES_NO_CHANGE));
                        }
                    }),
        /*
                new Partition("Changes Propsed: [internal error]", 
                    new PartitionMembership() { 
                        public boolean isMember(Pea p) {
                            return (p.hasProps);
                        }
                    }),
        */
                new Partition("No Changes Proposed: Status Quo", 
                    new PartitionMembership() { 
                        public boolean isMember(Pea p) {
                            return ((!p.hasInherited&&!p.hasProps) || // nothing to change.
                                    (p.voteType == Vetting.RES_NO_CHANGE));
                        }
                    }),
                new Partition("No Changes Proposed: Inherited", 
                    new PartitionMembership() { 
                        public boolean isMember(Pea p) {
                            return (p.hasInherited&&!p.hasProps);
                        }
                    }),
        };
        return theTestPartitions;
    }

    private Partition[] createSubmitPartitions() {
        Partition theTestPartitions[] = 
        {                 
                new Partition("Errors and Warnings", 
                    new PartitionMembership() { 
                        public boolean isMember(Pea p) {
                            return (p.hasErrors||p.hasWarnings);
                        }
                    }),
                new Partition("Unconfirmed", 
                    new PartitionMembership() { 
                        public boolean isMember(Pea p) {
                            // == insufficient votes
                            return  (p.voteType == Vetting.RES_INSUFFICIENT) ||
                                (p.voteType == Vetting.RES_NO_VOTES);
                        }
                    }),
                new Partition("Changes Proposed: Tentatively Approved", 
                    new PartitionMembership() { 
                        public boolean isMember(Pea p) {
                            return ((p.hasProps)&&
                                ((p.voteType & Vetting.RES_BAD_MASK)==0)&&
                                    (p.voteType>0)); // has proposed, and has a 'good' mark. Excludes by definition RES_NO_CHANGE
                        }
                    }),
                new Partition("Others", 
                    new PartitionMembership() { 
                        public boolean isMember(Pea p) {
                            return true;
                        }
                    }),
        };
        return theTestPartitions;
    }        
    

    private Hashtable displayHash = new Hashtable();
    
    public DisplaySet getDisplaySet(String sortMode, Pattern matcher) {
        return createDisplaySet(sortMode, matcher); // don't cache.
    }

    public DisplaySet getDisplaySet(String sortMode) {
        DisplaySet aDisplaySet = (DisplaySet)displayHash.get(sortMode);
        if(aDisplaySet == null)  {
            aDisplaySet = createDisplaySet(sortMode, null);
            displayHash.put(sortMode, aDisplaySet);
        }
        return aDisplaySet;
    }
    
    private DisplaySet createDisplaySet(String sortMode, Pattern matcher) {
        DisplaySet aDisplaySet = new DisplaySet(getList(sortMode, matcher), getDisplayList(sortMode, matcher), sortMode);
        aDisplaySet.canName = canName;
        return aDisplaySet;
    }
    
    private Hashtable listHash = new Hashtable();  // hash of sortMode->pea
    
    /**
     * get a List of peas, in sorted order 
     */
    public List getList(String sortMode) {
        List aList = (List)listHash.get(sortMode);
        if(aList == null) {
            aList = getList(sortMode, null);
        }
        listHash.put(sortMode, aList);
        return aList;
    }
        
    public List getList(String sortMode, Pattern matcher) {
    //        final boolean canName = canName;
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
            
                int categorizePea(Pea p, Partition partitions[]) {
                    int rv = -1;
                    for(int i=0;(rv==-1)&&(i<partitions.length);i++) {
                        if(partitions[i].pm.isMember(p)) {
                            rv = i;
                        }
                    }
                    if(rv==-1) {
                    }
                    return rv;
                }
                
                final Partition[] warningSort = SurveyMain.phaseSubmit?createSubmitPartitions():
                                                                       createVettingPartitions();
    //                        com.ibm.icu.text.Collator myCollator = rbc;
                public int compare(Object o1, Object o2){
                    Pea p1 = (Pea) o1;
                    Pea p2 = (Pea) o2;
                    
                    if(p1==p2) {
                        return 0;
                    }
                    
                    int rv = 0; // neg:  a < b.  pos: a> b
                    
                    if(p1.reservedForSort==-1) {
                        p1.reservedForSort = categorizePea(p1, warningSort);
                    }
                    if(p2.reservedForSort==-1) {
                        p2.reservedForSort = categorizePea(p2, warningSort);
                    }
                    
                    if(rv == 0) {
                        if(p1.reservedForSort < p2.reservedForSort) {
                            return -1;
                        } else if(p1.reservedForSort > p2.reservedForSort) {
                            return 1;
                        }
                    }

                   if(rv == 0) { // try to avoid a compare
                        String p1d  = null;
                        String p2d  = null;
                        if(canName) {
                          p1d = p1.displayName;
                          p2d = p2.displayName;
                        }
                        if(p1d == null ) {
                            p1d = p1.type;
                            if(p1d == null) {
                                p1d = "(null)";
                            }
                        }
                        if(p2d == null ) {
                            p2d = p2.type;
                            if(p2d == null) {
                                p2d = "(null)";
                            }
                        }
                        return myCollator.compare(p1d, p2d);
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
                    String p1d = p1.displayName;
                    if(p1.displayName == null ) {
                            p1d = p1.type;
    //                                throw new InternalError("item p1 w/ null display: " + p1.type);
                    }
                    String p2d = p2.displayName;
                    if(p2.displayName == null ) {
                            p2d = p2.type;
    //                                throw new InternalError("item p2 w/ null display: " + p2.type);
                    }
                    return myCollator.compare(p1d, p2d);
                }
            });
        } else {
            throw new InternalError("Unknown or unsupported sort mode: " + sortMode);
        }
        
        if(matcher == null) {
            newSet.addAll(peasHash.values()); // sort it    
        } else {
            for(Object o : peasHash.values()) {
                Pea p = (Pea)o;
                if(p.type.indexOf("Adak")!=-1) {
/*srl*/                    System.err.println("xp: "+p.xpathSuffix+":"+p.type+"- match: "+(matcher.matcher(p.type).matches()));
                }
                if(matcher.matcher(p.type).matches()) {
                    newSet.add(p);
                }
            }
        }
        
        ArrayList aList = new ArrayList(); // list it (waste here??)
        aList.addAll(newSet);

        return aList;
    }
    
    /** Returns a list parallel to that of getList() but of Strings suitable for display. 
    (Alternate idea: just make toString() do so on Pea.. advantage here is we can adjust for sort mode.) **/
    public List getDisplayList(String sortMode) {
        return getDisplayList(sortMode, getList(sortMode));
    }
    
    public List getDisplayList(String sortMode, Pattern matcher) {
        return getDisplayList(sortMode, getList(sortMode, matcher));
    }
    
    public List getDisplayList(String sortMode, List inPeas) {
        final List myPeas = inPeas;
        if(sortMode.equals(SurveyMain.PREF_SORTMODE_CODE)) {
            return new AbstractList() {
                private List ps = myPeas;
                public Object get(int n) {
                  return ((Pea)ps.get(n)).type; // always code
                }
                public int size() { return ps.size(); }
            };
        } else {
            return new AbstractList() {
                private List ps = myPeas;
                public Object get(int n) {
                  Pea p = (Pea)ps.get(n);
                  if(p.displayName != null) {
                    return p.displayName;
                  } else {
                    return p.type;
                  } 
                  //return ((Pea)ps.get(n)).type;
                }
                public int size() { return ps.size(); }
            };
        }
    }

	/**
	 * @param ctx context to use (contains CLDRDBSource, etc.)
	 * @param locale locale
	 * @param prefix XPATH prefix
	 * @param simple if true, means that data is simply xpath+type. If false, all xpaths under prefix.
	 */
	public static DataPod make(WebContext ctx, String locale, String prefix, boolean simple) {
		DataPod pod = new DataPod(ctx.sm, locale, prefix);
        pod.simple = simple;
        SurveyMain.UserLocaleStuff uf = ctx.sm.getUserFile(ctx, ctx.session.user, ctx.locale);
  
        CLDRDBSource ourSrc = uf.dbSource;
        CheckCLDR checkCldr = uf.getCheck(ctx);
        if(checkCldr == null) {
            throw new InternalError("checkCldr == null");
        }
        com.ibm.icu.dev.test.util.ElapsedTimer et;
        if(SHOW_TIME) {
            et= new com.ibm.icu.dev.test.util.ElapsedTimer();
            System.err.println("DP: Starting populate of " + locale + " // " + prefix+":"+ctx.defaultPtype());
        }
        pod.populateFrom(ourSrc, checkCldr, ctx.sm.getBaselineFile(),ctx.getOptionsMap());
        if(SHOW_TIME) {
            System.err.println("DP: Time taken to populate " + locale + " // " + prefix +":"+ctx.defaultPtype()+ " = " + et + " - Count: " + pod.getAll().size());
        }
        com.ibm.icu.dev.test.util.ElapsedTimer cet = new com.ibm.icu.dev.test.util.ElapsedTimer();
        pod.ensureComplete(ourSrc, checkCldr, ctx.sm.getBaselineFile(), ctx.getOptionsMap());
        if(SHOW_TIME) {
            System.err.println("DP: Time taken to complete " + locale + " // " + prefix +":"+ctx.defaultPtype()+ " = " + cet + " - Count: " + pod.getAll().size());
        }
        pod.exampleGenerator = new ExampleGenerator(new CLDRFile(ourSrc,true));
		return pod;
	}
    
    private static boolean isInitted = false;
    
    private static Pattern typeReplacementPattern;
    private static Pattern keyTypeSwapPattern;
    private static Pattern noisePattern;
    private static Pattern mostPattern;
    private static Pattern excludeAlways;
    private static Pattern needFullPathPattern; // items that need getFullXpath 
    
    private static final         String fromto[] = {   "^days/(.*)/(sun)$",  "days/1-$2/$1",
                              "^days/(.*)/(mon)$",  "days/2-$2/$1",
                              "^days/(.*)/(tue)$",  "days/3-$2/$1",
                              "^days/(.*)/(wed)$",  "days/4-$2/$1",
                              "^days/(.*)/(thu)$",  "days/5-$2/$1",
                              "^days/(.*)/(fri)$",  "days/6-$2/$1",
                              "^days/(.*)/(sat)$",  "days/7-$2/$1",
                              "^months/(.*)/month/([0-9]*)$", "months/$2/$1",
                              "^([^/]*)/months/(.*)/month/([0-9]*)$", "$1/months/$3/$2",
                              "^eras/(.*)/era/([0-9]*)$", "eras/$2/$1",
                              "^([^/]*)/eras/(.*)/era/([0-9]*)$", "$1/eras/$3/$2",
                              "^([ap]m)$","ampm/$1",
                              "^quarter/(.*)/quarter/([0-9]*)$", "quarter/$2/$1",
                              "^([^/]*)/([^/]*)/time$", "$1/time/$2",
                              "^([^/]*)/([^/]*)/date", "$1/date/$2",
                              "/alias$", "",
                              "dateTimes/date/availablesItem", "available date formats:",
                             /* "/date/availablesItem.*@_q=\"[0-9]*\"\\]\\[@id=\"([0-9]*)\"\\]","/availableDateFormats/$1" */
//                              "/date/availablesItem.*@_q=\"[0-9]*\"\\]","/availableDateFormats"
                            };
    private static Pattern fromto_p[] = new Pattern[fromto.length/2];
                            

    private static synchronized void init() {
        if(!isInitted) {
         typeReplacementPattern = Pattern.compile("\\[@(?:type|key)=['\"]([^'\"]*)['\"]\\]");
         keyTypeSwapPattern = Pattern.compile("([^/]*)/(.*)");
         noisePattern = Pattern.compile( // 'noise' to be removed
                                                    "^/|"+
                                                    "Formats/currencyFormatLength/currencyFormat|"+
                                                    "Formats/currencySpacing|"+
                                                    "Formats/percentFormatLength/percentFormat|"+
                                                    "Formats/decimalFormatLength/decimalFormat|"+
                                                    "Formats/scientificFormatLength/scientificFormat|"+
                                                    "dateTimes/dateTimeLength/|"+
                                                    "Formats/timeFormatLength|"+
                                                    "/timeFormats/timeFormatLength|"+
                                                    "/timeFormat|"+
                                                    "s/quarterContext|"+
                                                    "/dateFormats/dateFormatLength|"+
                                                    "/pattern|"+
                                                    "/monthContext|"+
                                                    "/monthWidth|"+
                                                    "/timeLength|"+
                                                    "/quarterWidth|"+
                                                    "/dayContext|"+
                                                    "/dayWidth|"+
                                                    "day/|"+
                                                    "date/|"+
                                                    "Format|"+
                                                    "s/field|"+
                                                    "\\[@draft=\"true\"\\]|"+ // ???
                                                    "\\[@alt=\"[^\"]*\"\\]|"+ // ???
                                                    "/displayName$|"+  // for currency
                                                    "/standard/standard$"     );
         mostPattern = Pattern.compile("^//ldml/localeDisplayNames.*|"+
                                              "^//ldml/characters/exemplarCharacters.*|"+
                                              "^//ldml/numbers.*|"+
                                              "^//ldml/references.*|"+
                                              "^//ldml/dates/timeZoneNames/zone.*|"+
                                              "^//ldml/dates/timeZoneNames/metazone.*|"+
                                              "^//ldml/dates/calendar.*|"+
                                              "^//ldml/identity.*");
        // what to exclude under 'misc' and calendars
         excludeAlways = Pattern.compile("^//ldml/segmentations.*|"+
                                                "^//ldml/measurement.*|"+
                                                ".*week/minDays.*|"+
                                                ".*week/firstDay.*|"+
                                                ".*/usesMetazone.*|"+
                                                ".*week/weekendEnd.*|"+
                                                ".*week/weekendStart.*|" +
//                                                "^//ldml/dates/.*localizedPatternChars.*|" +
                                                "^//ldml/posix/messages/.*expr$|" +
                                                "^//ldml/dates/timeZoneNames/.*/GMT.*exemplarCity$|" +
                                                "^//ldml/dates/.*default");// no defaults
                                                
        needFullPathPattern = Pattern.compile("^//ldml/layout/orientation$|" +
                                              ".*/alias");
        
            int pn;
            for(pn=0;pn<fromto.length/2;pn++) {
                fromto_p[pn]= Pattern.compile(fromto[pn*2]);
            }

        }
        isInitted = true;
    }
    
    private static final boolean SHOW_TIME=true;
    public static final String FAKE_FLEX_THING = "available date formats: add NEW item";
    public static final String FAKE_FLEX_SUFFIX = "dateTimes/availableDateFormats/dateFormatItem[@id=\"NEW\"]";
    public static final String FAKE_FLEX_XPATH = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/availableFormats/dateFormatItem";
    
    private void populateFrom(CLDRDBSource src, CheckCLDR checkCldr, CLDRFile baselineFile, Map options) {
        init();
        XPathParts xpp = new XPathParts(null,null);
//        System.out.println("[] initting from pod " + locale + " with prefix " + xpathPrefix);
        CLDRFile aFile = new CLDRFile(src, true);
        XPathParts pathParts = new XPathParts(null, null);
        XPathParts fullPathParts = new XPathParts(null, null);
        List examplesResult = new ArrayList();
        CLDRFile.Status sourceLocaleStatus = new CLDRFile.Status();
        final boolean ndebug = false;
        long lastTime = -1;
        long longestTime = -1;
        String longestPath = "NONE";
        long nextTime = -1;
        int count=0;
        long countStart = 0;
        if(SHOW_TIME) {
            countStart = System.currentTimeMillis();
        }
        // what to exclude under 'misc'
        int t = 10;
            
        int pn;
        String exclude = null;
        boolean excludeCurrencies = false;
        boolean excludeCalendars = false;
        boolean excludeLDN = false;
        boolean excludeGrego = false;
        boolean excludeTimeZones = false;
        boolean excludeMetaZones = false;
        boolean useShorten = false; // 'shorten' xpaths instead of extracting type
        boolean keyTypeSwap = false;
        boolean hackCurrencyDisplay = false;
        boolean excludeMost = false;
        boolean doExcludeAlways = true;
        boolean isReferences = false;
        String removePrefix = null;
        if(xpathPrefix.equals("//ldml")) {
            excludeMost = true;
            useShorten = true;
            removePrefix="//ldml/";
        }else if(xpathPrefix.startsWith("//ldml/numbers")) {
            if(-1 == xpathPrefix.indexOf("currencies")) {
                doExcludeAlways=false;
                excludeCurrencies=true; // = "//ldml/numbers/currencies";
                removePrefix = "//ldml/numbers/";
                canName=false;  // sort numbers by code
                useShorten = true;
            } else {
                removePrefix = "//ldml/numbers/currencies/currency";
                useShorten = true;
//                hackCurrencyDisplay = true;
            }
        } else if(xpathPrefix.startsWith("//ldml/dates")) {
            useShorten = true;
            if(xpathPrefix.startsWith("//ldml/dates/timeZoneNames/zone")) {
                removePrefix = "//ldml/dates/timeZoneNames/zone";
//        System.err.println("ZZ0");
                excludeTimeZones = false;
            } else if(xpathPrefix.startsWith("//ldml/dates/timeZoneNames/metazone")) {
                removePrefix = "//ldml/dates/timeZoneNames/metazone";
                excludeMetaZones = false;
//        System.err.println("ZZ1");
            } else {
                removePrefix = "//ldml/dates/calendars/calendar";
                excludeTimeZones = true;
                excludeMetaZones = true;
                if(xpathPrefix.indexOf("gregorian")==-1) {
                    excludeGrego = true; 
                    // nongreg
                } else {
                    removePrefix = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]";
                    
                    // Add the fake 'dateTimes/availableDateFormats/new'
                    Pea myp = getPea(FAKE_FLEX_THING);
                    String spiel = "<i>add</i>"; //Use this item to add a new availableDateFormat
                    myp.xpathSuffix = FAKE_FLEX_SUFFIX;
                    canName=false;
                    myp.displayName = spiel;
//                    myp.addItem(spiel, null, null);
                }
            }
        } else if(xpathPrefix.startsWith("//ldml/localeDisplayNames/types")) {
            useShorten = true;
            removePrefix = "//ldml/localeDisplayNames/types/type";
            keyTypeSwap = true; //these come in reverse order  (type/key) i.e. buddhist/celander, pinyin/collation.  Reverse this for sorting...
        } else if(xpathPrefix.equals("//ldml/references")) {
            isReferences = true;
            canName = false; // disable 'view by name'  for references
        }
        List checkCldrResult = new ArrayList();
        
        // iterate over everything in this prefix ..
        
        for(Iterator it = aFile.iterator(xpathPrefix);it.hasNext();) {
            boolean confirmOnly = false;
            String xpath = (String)it.next();

///*srl*/  if(xpath.indexOf("Adak")!=-1)
///*srl*/   {ndebug=true;System.err.println("p] "+xpath + " - xtz = "+excludeTimeZones+"..");}
                
            if(SHOW_TIME) {
                count++;
                if((count%250)==0) {
                    System.err.println("[] " + locale + ":"+xpathPrefix +" #"+count+", or "+
                        (((double)(System.currentTimeMillis()-countStart))/count)+"ms per.");
                }
            }

            if(doExcludeAlways && excludeAlways.matcher(xpath).matches()) {
// if(ndebug && (xpath.indexOf("Adak")!=-1))    System.err.println("ns1 1 "+(System.currentTimeMillis()-nextTime) + " " + xpath);
                continue;
            } else if(excludeMost && mostPattern.matcher(xpath).matches()) {
//if(ndebug)     System.err.println("ns1 2 "+(System.currentTimeMillis()-nextTime) + " " + xpath);
                continue;
            } else if(excludeCurrencies && (xpath.startsWith("//ldml/numbers/currencies/currency"))) {
//if(ndebug)     System.err.println("ns1 3 "+(System.currentTimeMillis()-nextTime) + " " + xpath);
                continue;
            } else if(excludeCalendars && (xpath.startsWith("//ldml/dates/calendars"))) {
//if(ndebug)     System.err.println("ns1 4 "+(System.currentTimeMillis()-nextTime) + " " + xpath);
                continue;
            } else if(excludeTimeZones && (xpath.startsWith("//ldml/dates/timeZoneNames/zone"))) {
//if(ndebug && (xpath.indexOf("Adak")!=-1))     System.err.println("ns1 5 "+(System.currentTimeMillis()-nextTime) + " " + xpath);
                continue;
/*            } else if(exemplarCityOnly && (xpath.indexOf("exemplarCity")==-1)) {
                continue;*/
            } else if(excludeMetaZones && (xpath.startsWith("//ldml/dates/timeZoneNames/metazone"))) {
//if(ndebug&& (xpath.indexOf("Adak")!=-1))     System.err.println("ns1 6 "+(System.currentTimeMillis()-nextTime) + " " + xpath);
                continue;
            } else if(!excludeCalendars && excludeGrego && (xpath.startsWith(SurveyMain.GREGO_XPATH))) {
//if(ndebug)     System.err.println("ns1 7 "+(System.currentTimeMillis()-nextTime) + " " + xpath);
                continue;
            }
            
            if(CheckCLDR.skipShowingInSurvey.matcher(xpath).matches()) {
                System.err.println("ns1 8 "+(System.currentTimeMillis()-nextTime) + " " + xpath);
                continue;
            }

            String fullPath = aFile.getFullXPath(xpath);
            //int xpath_id = src.xpt.getByXpath(fullPath);
            int base_xpath = src.xpt.xpathToBaseXpathId(xpath);
            String baseXpath = src.xpt.getById(base_xpath);
            
            
            if(fullPath == null) {
                System.err.println("DP:P Error: fullPath of " + xpath + " for locale " + locale + " returned null.");
                fullPath = xpath;
            }

            if(needFullPathPattern.matcher(xpath).matches()) {
                //  we are going to turn on shorten, in case a non-shortened xpath is added someday.
                useShorten = true;
            }           

//if(ndebug)    System.err.println("ns0  "+(System.currentTimeMillis()-nextTime));
            boolean mixedType = false;
            String type;
            String lastType = src.xpt.typeFromPathToTinyXpath(baseXpath, xpp);  // last type in the list
            String displaySuffixXpath;
            String peaSuffixXpath = null; // if non null:  write to suffixXpath
            
            // these need to work on the base
            String fullSuffixXpath = baseXpath.substring(xpathPrefix.length(),baseXpath.length());
            if((removePrefix == null)||!baseXpath.startsWith(removePrefix)) {  
                displaySuffixXpath = baseXpath;
            } else {
                displaySuffixXpath = baseXpath.substring(removePrefix.length(),baseXpath.length());
            }
            if(useShorten == false) {
                type = lastType;
                if(type == null) {
                    peaSuffixXpath = displaySuffixXpath; // Mixed pea
                    if(xpath.startsWith("//ldml/characters")) {
                        type = "standard";
                    } else {
                        type = displaySuffixXpath;
                        mixedType = true;
                    }
                }
            } else {
                // shorten
                peaSuffixXpath = displaySuffixXpath; // always mixed pea if we get here
                    
                Matcher m = typeReplacementPattern.matcher(displaySuffixXpath);
                type = m.replaceAll("/$1");
                Matcher n = noisePattern.matcher(type);
                type = n.replaceAll("");
                if(keyTypeSwap) { // see above
                    Matcher o = keyTypeSwapPattern.matcher(type);
                    type = o.replaceAll("$2/$1");
                }

                for(pn=0;pn<fromto.length/2;pn++) {
//                    String oldType = type;
                    type = fromto_p[pn].matcher(type).replaceAll(fromto[(pn*2)+1]);
                    // who caused the change?
//                    if((type.indexOf("ldmls/")>0)&&(oldType.indexOf("ldmls/")<0)) {
//                        System.err.println("ldmls @ #"+pn+", "+fromto[pn*2]+" -> " + fromto[(pn*2)+1]);
//                    }
                }

            }
            
 //if(ndebug)    System.err.println("n00  "+(System.currentTimeMillis()-nextTime));
            
            String value = aFile.getStringValue(xpath);

//if(ndebug)     System.err.println("n01  "+(System.currentTimeMillis()-nextTime));

            if( xpath.indexOf("default[@type")!=-1 ) {
                peaSuffixXpath = displaySuffixXpath;
                int n = type.lastIndexOf('/');
                if(n==-1) {
                    type = "(default type)";
                } else {
                    type = type.substring(0,n); //   blahblah/default/foo   ->  blahblah/default   ('foo' is lastType and will show up as the value)
                }
                value = lastType;
                confirmOnly = true; // can't acccept new data for this.
            } else if(xpath.indexOf("commonlyUsed[@used")!=-1) { // For now, don't allow input for commonlyUsed
                confirmOnly = true;
            }
            
            if(useShorten) {
                if((xpath.indexOf("/orientation")!=-1)||
                   (xpath.indexOf("/alias")!=-1)||
                   (xpath.indexOf("/inList")!=-1)) {
                    if((value !=null)&&(value.length()>0)) {
                        throw new InternalError("Shouldn't have a value for " + xpath + " but have '"+value+"'.");
                    }
                    peaSuffixXpath = displaySuffixXpath;
                    int n = type.indexOf('[');
                    if(n!=-1) {
                        value = type.substring(n,type.length());
                        type = type.substring(0,n); //   blahblah/default/foo   ->  blahblah/default   ('foo' is lastType and will show up as the value)                        
                        //value = lastType;
                        confirmOnly = true; // can't acccept new data for this.
                    }
                }
            }
            
            if(value == null) {
//                throw new InternalError("Value of " + xpath + " is null.");
                  System.err.println("Value of " + xpath + " is null.");
                 value = "(NOTHING)";
            }
            
//if(ndebug)    System.err.println("n02  "+(System.currentTimeMillis()-nextTime));
//            System.out.println("* T=" + type + ", X= " + xpath);
            String alt = src.xpt.altFromPathToTinyXpath(xpath, xpp);

//    System.err.println("n03  "+(System.currentTimeMillis()-nextTime));
    
            xpp.clear();
            /*
            xpp.initialize(xpath);
            String lelement = xpp.getElement(-1); */
            /* all of these are always at the end */
            
            /* FULL path processing (references.. alt proposed.. ) */
            xpp.clear();
            xpp.initialize(fullPath);
            String lelement = xpp.getElement(-1);
            String eAlt = xpp.findAttributeValue(lelement, LDMLConstants.ALT);
            String eRefs = xpp.findAttributeValue(lelement,  LDMLConstants.REFERENCES);
            String eDraft = xpp.findAttributeValue(lelement, LDMLConstants.DRAFT);
//if(ndebug) System.err.println("n04  "+(System.currentTimeMillis()-nextTime));
            
            
            String typeAndProposed[] = LDMLUtilities.parseAlt(alt);
            String altProposed = typeAndProposed[1];
            String altType = typeAndProposed[0];
            Pea p = getPea(type, altType);
            
///*srl*/ if(xpath.indexOf("Adak")!=-1) {System.err.println("MM ["+fullPath+"] item:"+type+" - " + altProposed + "  v="+value); }

            
///*srl*/   if(type.equals("HK")) { System.err.println("@@ alt: " + alt + " -> " + altProposed + " // " + altType + " - type = " + type); }
            p.base_xpath = base_xpath;
            Pea superP = getPea(type);
///*SRL*/System.err.println(locale+"T:{"+type+"}, xps: {"+peaSuffixXpath+"}");
            peaSuffixXpath = fullSuffixXpath; // for now...
            if(peaSuffixXpath!=null) {
                p.xpathSuffix = peaSuffixXpath;
                superP.xpathSuffix = XPathTable.removeAltFromStub(peaSuffixXpath);
///*srl*/         if(type.equals("HK")) { System.err.println("SuperP's xps = " + superP.xpathSuffix); }
            }
            p.confirmOnly = superP.confirmOnly = confirmOnly;

//if(ndebug)     System.err.println("n05  "+(System.currentTimeMillis()-nextTime));

            // make sure the superP has its display name
            if(isReferences) {
                String eUri = xpp.findAttributeValue(lelement,"uri");
               if((eUri!=null)&&eUri.length()>0) {
                   if(eUri.startsWith("isbn:")) {
                        // linkbaton doesn't have ads, and lets you choose which provider to go to (including LOC).  
                        // could also go to wikipedia's  ISBN special page.              
                        p.displayName = /*type + " - "+*/ "<a href='http://my.linkbaton.com/isbn/"+
                            eUri.substring(5,eUri.length())+"'>"+eUri+"</a>";
                    } else {
                        p.displayName = /*type + " - "+*/ "<a href='"+eUri+"'>"+eUri+"</a>";
                    }
                } else {
                    p.displayName = null;
                }
                if(superP.displayName == null) {
                    superP.displayName = p.displayName;
                }
            } else {
                if(superP.displayName == null) {
                    superP.displayName = baselineFile.getStringValue(xpath(superP)); // isn't this what it's for?
///*srl*/                  if(p.type.equals("HK") || p.type.equals("MO")) {
///*srl*/                        System.err.println("SP["+xpath(superP)+"] = " + superP.displayName);
///*srl*/                  }
                }
                if(p.displayName == null) {
                    p.displayName = baselineFile.getStringValue(baseXpath);
///*srl*/                    if(p.type.equals("HK") || p.type.equals("MO")) {
///*srl*/                        System.err.println("P["+baseXpath+"] = " + p.displayName);
///*srl*/                    }
                }
            }
    
            if((superP.displayName == null) ||
                (p.displayName == null)) {
                canName = false; // disable 'view by name' if not all have names.
            }
//    System.err.println("n06  "+(System.currentTimeMillis()-nextTime));
            
            // If it is draft and not proposed.. make it proposed-draft 
            if( ((eDraft!=null)&&(!eDraft.equals("false"))) &&
                (altProposed == null) ) {
                altProposed = SurveyMain.PROPOSED_DRAFT;
            }
            
            // Inherit display names.
            if((superP != p) && (p.displayName == null)) {
                p.displayName = baselineFile.getStringValue(baseXpath); 
                if(p.displayName == null) {
                    p.displayName = superP.displayName; // too: unscramble this a little bit
                }
            }
            String sourceLocale = aFile.getSourceLocaleID(xpath, sourceLocaleStatus);
            
            boolean isInherited = !(sourceLocale.equals(locale));
            
//    System.err.println("n07  "+(System.currentTimeMillis()-nextTime));
    
            // ?? simplify this.
            if(altProposed == null) {
                if(!isInherited) {
                    //superP.hasInherited=false;
                    //p.hasInherited=false;
                } else {
                    p.hasInherited = true;
                    superP.hasInherited=true;
                }
            } else {
                if(!isInherited) {
                    p.hasProps = true;
                    superP.hasProps = true;
                } else {
                    // inherited, proposed
                   // p.hasProps = true; // Don't mark as a proposal.
                   // superP.hasProps = true;
                   p.hasInherited=true;
                   superP.hasInherited=true;
                }
            }
            
///*SRL*/     if(xpath.indexOf("Chicago")>-1) {
//                System.err.println(locale + " - CHI - " + xpath + " V:"+value+" - I:"+isInherited);
//            }
            
            String setInheritFrom = (isInherited)?sourceLocale:null; // no inherit if it's current.
            boolean isCodeFallback = (setInheritFrom!=null)&&
                (setInheritFrom.equals(XMLSource.CODE_FALLBACK_ID)); // don't flag errors from code fallback.
            if((checkCldr != null)/*&&(altProposed == null)*/) {
                if(altProposed == null) {
                    checkCldr.check(xpath, fullPath, value, options, checkCldrResult);
                    checkCldr.getExamples(xpath, fullPath, value, options, examplesResult);
                } else {
                    // hose the Xpath off first
                    checkCldr.check(baseXpath, baseXpath, value, options, checkCldrResult);
                    checkCldr.getExamples(baseXpath, baseXpath, value, options, examplesResult);
                }
            }
            DataPod.Pea.Item myItem;
 //if(ndebug)   System.err.println("n08  "+(System.currentTimeMillis()-nextTime));
            if(checkCldrResult.isEmpty()) {
               myItem = p.addItem( value, altProposed, null);
            } else {
                myItem = p.addItem( value, altProposed, checkCldrResult);
                // only consider non-example tests as notable.
                boolean weHaveTests = false;
                int errorCount = 0;
                int warningCount =0 ;
                for (Iterator it3 = checkCldrResult.iterator(); it3.hasNext();) {
                    CheckCLDR.CheckStatus status = (CheckCLDR.CheckStatus) it3.next();
                    if(status.getType().equals(status.exampleType)) {
                        //throw new InternalError("Not supposed to be any examples here.");
                    /*
                        if(myItem.examples == null) {
                            myItem.examples = new Vector();
                        }
                        myItem.examples.add(addExampleEntry(new ExampleEntry(this,p,myItem,status)));
                        */
                    } else /* if (!(isCodeFallback &&
                        (status.getCause() instanceof org.unicode.cldr.test.CheckForExemplars))) */ { 
                        // skip codefallback exemplar complaints (i.e. 'JPY' isn't in exemplars).. they'll show up in missing
                        weHaveTests = true;
                        if(status.getType().equals(status.errorType)) {
                            errorCount++;
                        } else if(status.getType().equals(status.warningType)) {
                            warningCount++;
                        }
                    }
                }
                if(weHaveTests) {
                    p.hasTests = true;
                    p.hasErrors = (errorCount>0);
                    p.hasWarnings = (warningCount>0);
                    // propagate to parent
                    superP.hasTests = true;
                    if(p.hasErrors) {
                        superP.hasErrors = true;
                    }
                    if(p.hasWarnings) {
                        superP.hasWarnings = true;
                    }
                }
                // set the parent
                checkCldrResult = new ArrayList(); // can't reuse it if nonempty
            }
            myItem.xpath = xpath;
            myItem.xpathId = src.xpt.getByXpath(xpath);

            if(!sourceLocaleStatus.pathWhereFound.equals(xpath)) {
                myItem.pathWhereFound = sourceLocaleStatus.pathWhereFound;
            }
            myItem.inheritFrom = setInheritFrom;

            // store who voted for what. [ this could be loaded at displaytime..]
            myItem.votes = sm.vet.gatherVotes(locale, xpath);

            // bitwise OR in the voting types. Needed for sorting.
            if(p.voteType == 0) {
                int vtypes[] = new int[1];
                vtypes[0]=0;
                /* res = */ sm.vet.queryResult(locale, base_xpath, vtypes);
                p.voteType |= vtypes[0];
            }
            
            if(!examplesResult.isEmpty()) {
                // reuse the same ArrayList  unless it contains something                
                if(myItem.examples == null) {
                    myItem.examples = new Vector();
                }
                for (Iterator it3 = examplesResult.iterator(); it3.hasNext();) {
                    CheckCLDR.CheckStatus status = (CheckCLDR.CheckStatus) it3.next();                
                    myItem.examples.add(addExampleEntry(new ExampleEntry(this,p,myItem,status)));
                }
   //             myItem.examplesList = examplesResult;
   //             examplesResult = new ArrayList(); // getExamples will clear it.
            }

            if((eRefs != null) && (!isInherited)) {
                myItem.references = eRefs;
            }
            
        }
//        aFile.close();
    }

    /**
     * Makes sure this pod contains the peas we'd like to see.
     */
    private void ensureComplete(CLDRDBSource src, CheckCLDR checkCldr, CLDRFile baselineFile, Map options) {
        if(xpathPrefix.startsWith("//ldml/"+"dates/timeZoneNames")) {
            // work on zones
            boolean isMetazones = xpathPrefix.startsWith("//ldml/"+"dates/timeZoneNames/metazone");
            // Make sure the pod contains the peas we'd like to see.
            // regular zone
            
            Set mzones = sm.getMetazones();
            
            Iterator zoneIterator;
            
            if(isMetazones) {
                zoneIterator = sm.getMetazones().iterator();
            } else {
                zoneIterator = StandardCodes.make().getAvailableCodes("tzid").iterator();
            }
            
            final String tzsuffs[] = {  "/long/generic",
                                "/long/daylight",
                                "/long/standard",
                                "/short/generic",
                                "/short/daylight",
                                "/short/standard",
                                "/exemplarCity" };
            final String mzsuffs[] = {  "/long/generic",
                                "/long/daylight",
                                "/long/standard",
                                "/short/generic",
                                "/short/daylight",
                                "/short/standard",
                                "/commonlyUsed[@used=\"true\"]"
            };
            
            String suffs[];
            if(isMetazones) {
                suffs = mzsuffs;            
            } else {
                suffs = tzsuffs;
            }        

            String podBase = xpathPrefix;
            CLDRFile resolvedFile = new CLDRFile(src, true);
            XPathParts parts = new XPathParts(null,null);
//            TimezoneFormatter timezoneFormatter = new TimezoneFormatter(resolvedFile, true); // TODO: expensive here.

            for(;zoneIterator.hasNext();) {
                String zone = zoneIterator.next().toString();
//                System.err.println(">> " + zone);
                /** some compatibility **/
                String ourSuffix = "[@type=\""+zone+"\"]";
                String whichMZone = null;
                if(isMetazones) {
                    whichMZone = zone;
                }

                for(int i=0;i<suffs.length;i++) {
                    String suff = suffs[i];
                    
                    // synthesize a new pea..
                    DataPod.Pea myp = getPea(zone+suff);
                    
                    // set it up..
                    String base_xpath_string = podBase+ourSuffix+suff;
                    int base_xpath = sm.xpt.getByXpath(base_xpath_string);
                    myp.base_xpath = base_xpath;
                    
                    if(myp.xpathSuffix == null) {
                        myp.xpathSuffix = ourSuffix+suff;
                    }
        ///*srl*/            System.err.println("P: ["+zone+suff+"] - count: " + myp.items.size());

                    if(suff.indexOf("commonlyUsed[@used")!=-1) { // For now, don't allow input for commonlyUsed
                        myp.confirmOnly = true;
                    }


                    if(myp.items.isEmpty()) {
            /*
                        String formatted = CheckZones.exampleTextForXpath(parts, timezoneFormatter, 
                            podBase+ourSuffix+suff);
                        if (suff.indexOf("commonlyUsed")>-1) {
                            formatted = "true"; // set to true...
                        } else if(whichMZone != null) {
                            formatted = "???";
                        }
                        if(formatted != null) {
                            DataPod.Pea.Item item = myp.addItem(formatted, null, null); // <<<<<< THIS IS BAD. 
                            item.inheritFrom = XMLSource.CODE_FALLBACK_ID;
                        }
            */
                    }                  
                    myp.displayName = baselineFile.getStringValue(podBase+ourSuffix+suff); // isn't this what it's for?
                }
            }
        } // tz
    }
// ==

    public Pea getPea(String type) {
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
        return "{DataPod " + locale + ":" + xpathPrefix + " #" + super.toString() + ", " + getAll().size() +" items} ";
    }
    
    /** 
     * Given a (cleaned, etc) xpath, this returns the podBase, i.e. context.getPod(base), that would be used to show
     * that xpath.  
     * Keep this in sync with SurveyMain.showLocale() where there is the list of menu items.
     */
    public static String xpathToPodBase(String xpath) {
        int n;
        String base;
        
        // is it one of the prefixes we can check statically?
        String staticBases[] = { 
            // LOCALEDISPLAYNAMES
                "//ldml/"+SurveyMain.NUMBERSCURRENCIES,
                "//ldml/"+"dates/timeZoneNames/zone",
                "//ldml/"+"dates/timeZoneNames/metazone",
            // OTHERROOTS
                SurveyMain.GREGO_XPATH,
                SurveyMain.OTHER_CALENDARS_XPATH
        };
         
        // is it one of the static bases?
        for(n=0;n<staticBases.length;n++) {
            if(xpath.startsWith(staticBases[n])) {
                return staticBases[n];
            }
        }
            
        // dynamic LOCALEDISPLAYNAMES
        for(n =0 ; n < SurveyMain.LOCALEDISPLAYNAMES_ITEMS.length; n++) {   // is it a simple code list?
            base = SurveyMain.LOCALEDISPLAYNAMES+SurveyMain.LOCALEDISPLAYNAMES_ITEMS[n]+
                '/'+SurveyMain.typeToSubtype(SurveyMain.LOCALEDISPLAYNAMES_ITEMS[n]);  // see: SurveyMain.showLocaleCodeList()
            if(xpath.startsWith(base)) {
                return base;
            }
        }
        
        // OTHERROOTS
        for(n=0;n<SurveyMain.OTHERROOTS_ITEMS.length;n++) {
            base= "//ldml/"+SurveyMain.OTHERROOTS_ITEMS[n];
            if(xpath.startsWith(base)) {
                return base;
            }
        }
        
        return "//ldml"; // the "misc" pile.
    }
    
}
