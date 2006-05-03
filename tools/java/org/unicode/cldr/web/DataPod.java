//
//  DataPod.java
//
//  Created by Steven R. Loomis on 18/11/2005.
//  Copyright 2005-2006 IBM. All rights reserved.
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
import com.ibm.icu.text.RuleBasedCollator;

/** A data pod represents a group of related data that will be displayed to users in a list
 * such as, "all of the language display names contained in the en_US locale".
 * It is sortable, as well, and has some level of persistence.
 **/

public class DataPod {
    long touchTime = -1;
    public void touch() {
        touchTime = System.currentTimeMillis();
    }
    public long age() {
        return System.currentTimeMillis() - touchTime;
    }
    // UI strings
    boolean canName = true;
    public static final String DATAPOD_MISSING = "Inherited";
    public static final String DATAPOD_NORMAL = "Normal";
    public static final String DATAPOD_PRIORITY = "Priority";
    public static final String DATAPOD_PROPOSED = "Proposed";
    public static final String DATAPOD_VETPROB = "Vetting Issue";

    public String locale = null;
    public String xpathPrefix = null;
    
    private String fieldHash; // prefix string used for calculating html fields
    private SurveyMain sm;
    
    DataPod(SurveyMain sm, String loc, String prefix) {
        this.sm = sm;
        locale = loc;
        xpathPrefix = prefix;
        fieldHash =  CookieSession.cheapEncode(sm.xpt.getByXpath(prefix));
    }
    private static int n =0;
    protected static synchronized int getN() { return ++n; }
    // This class represents an Example box, so that it can be popped out.
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
    Hashtable exampleHash = new Hashtable();
    ExampleEntry addExampleEntry(ExampleEntry e) {
        synchronized(exampleHash) {
            exampleHash.put(e.hash,e);
        }
        return e; // for the hash.
    }
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
        public boolean confirmOnly = false; // if true: don't accept new data, this pea is something strange.
        public String type = null;
		public String xpathSuffix = null; // if null:  prefix+type is sufficient (simple list).  If non-null: mixed Pod, prefix+suffix is required and type is informative only.
        public String displayName = null;
        public String altType = null; // alt type (NOT to be confused with -proposedn)
        int base_xpath = -1;
        boolean hasTests = false;
        boolean hasProps = false;
        boolean hasInherited = false;
        public int voteType = 0; // bitmask of all voting types included
        public int reservedForSort = -1; // reserved to use in collator.
        String inheritFrom = null;
        public class Item {
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
        Item addItem(String value, String altProposed, List tests) {
            Item pi = new Item();
            pi.value = value;
            pi.altProposed = altProposed;
            pi.tests = tests;
            items.add(pi);
//            System.out.println("  v: " + pi.value);
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
            peas = myPeas;
            displayPeas = myDisplayPeas;
            this.sortMode = sortMode;
            
            // fetch partitions..
            Vector v = new Vector();
            if(sortMode.equals(SurveyMain.PREF_SORTMODE_WARNING)) {
                Partition testPartitions[] = createTestPartitions();
                // find the starts
                int lastGood = 0;
                Pea peasArray[] = (Pea[])peas.toArray(new Pea[0]);
                for(int i=0;i<peasArray.length;i++) {
                    Pea p = peasArray[i];
                    
///*srl*/                    if(p.base_xpath == 964) {
//                        System.err.println(p.base_xpath+": Props:"+new Boolean(p.hasProps)+", Tests:"+new Boolean(p.hasTests)+
//                            ", Inherit:"+new Boolean(p.hasInherited));
//                            
//                        for(int j=0;j<testPartitions.length;j++) {
//                            System.err.println(p.base_xpath+": " + testPartitions[j].name+": " + testPartitions[j].pm.isMember(p));
//                        }
//                    }
                    
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
 ///*srl*/                   System.err.println("P"+j+" - " + testPartitions[j]);
                    if(testPartitions[j].start != -1) {
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


    private Partition[] createTestPartitions() {
        Partition theTestPartitions[] = 
        {                 
                new Partition("Changes Proposed: Insufficient Votes", 
                    new PartitionMembership() { 
                        public boolean isMember(Pea p) {
                            return  (p.voteType == Vetting.RES_INSUFFICIENT) ||
                                (p.voteType == Vetting.RES_NO_VOTES);
                        }
                    }),
                new Partition("Changes Proposed: Disputed", 
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
    

    private Hashtable displayHash = new Hashtable();
    
    public DisplaySet getDisplaySet(String sortMode) {
        DisplaySet aDisplaySet = (DisplaySet)displayHash.get(sortMode);
        if(aDisplaySet == null)  {
            aDisplaySet = new DisplaySet(getList(sortMode), getDisplayList(sortMode), sortMode);
            aDisplaySet.canName = canName;
            displayHash.put(sortMode, aDisplaySet);
        }
        return aDisplaySet;
    }
    
    private Hashtable listHash = new Hashtable();  // hash of sortMode->pea
    
    /**
     * get a List of peas, in sorted order 
     */
    public List getList(String sortMode) {
        List aList = (List)listHash.get(sortMode);
        if(aList == null) {
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
                        for(int i=0;i<partitions.length;i++) {
                            if(partitions[i].pm.isMember(p)) {
                                rv = i;
                            }
                        }
                        if(rv==-1) {
///*srl*/                                System.err.println("Uncategorized pea: " + p.base_xpath);
                        }
                        return rv;
                    }
                    final Partition[] warningSort = createTestPartitions();
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
                            int crv;
                            String p1d = p1.displayName;
                            if(p1.displayName == null ) {
                                p1d = p1.type;
                            }
                            String p2d = p2.displayName;
                            if(p2.displayName == null ) {
                                p2d = p2.type;
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
            
            newSet.addAll(peasHash.values()); // sort it    
            
            aList = new ArrayList(); // list it (waste here??)
            aList.addAll(newSet);
        }
        return aList;
    }
    
    /** Returns a list parallel to that of getList() but of Strings suitable for display. 
    (Alternate idea: just make toString() do so on Pea.. advantage here is we can adjust for sort mode.) **/
    public List getDisplayList(String sortMode) {
        final List myPeas = getList(sortMode);
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
	public static DataPod make(WebContext ctx, String locale,String prefix, boolean simple) {
		DataPod pod = new DataPod(ctx.sm, locale, prefix);
		if(simple==true) {
//            pod.loadStandard(ctx.sm.getEnglishFile()); //load standardcodes + english        
            CLDRDBSource ourSrc = (CLDRDBSource)ctx.getByLocale(SurveyMain.USER_FILE + SurveyMain.CLDRDBSRC, locale);
            CheckCLDR checkCldr = (CheckCLDR)ctx.getByLocale(SurveyMain.USER_FILE + SurveyMain.CHECKCLDR+":"+ctx.defaultPtype());
            if(checkCldr == null) {
                throw new InternalError("checkCldr == null");
            }
            com.ibm.icu.dev.test.util.ElapsedTimer et;
            if(SHOW_TIME) {
                et= new com.ibm.icu.dev.test.util.ElapsedTimer();
                System.err.println("DP: Starting populate of " + locale + " // " + prefix+":"+ctx.defaultPtype());
            }
            pod.populateFrom(ourSrc, checkCldr, ctx.sm.getEnglishFile(),ctx.getOptionsMap());
            if(SHOW_TIME) {
                System.err.println("DP: Time taken to populate " + locale + " // " + prefix +":"+ctx.defaultPtype()+ " = " + et);
            }
		} else {
			throw new InternalError("non-simple pods not supported");
		}
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
                                              "^//ldml/dates/timeZoneNames/zone.*|"+
                                              "^//ldml/dates/calendar.*|"+
                                              "^//ldml/identity.*");
        // what to exclude under 'misc' and calendars
         excludeAlways = Pattern.compile("^//ldml/segmentations.*|"+
                                                "^//ldml/measurement.*|"+
                                                ".*week/minDays.*|"+
                                                ".*week/firstDay.*|"+
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
    
    private static final boolean SHOW_TIME=false;
    public static final String FAKE_FLEX_THING = "available date formats: add NEW";
    public static final String FAKE_FLEX_SUFFIX = "dateTimes/availableDateFormats/dateFormatItem[@id=\"NEW\"]";
    public static final String FAKE_FLEX_XPATH = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/availableFormats/dateFormatItem";
    
    private void populateFrom(CLDRDBSource src, CheckCLDR checkCldr, CLDRFile engFile, Map options) {
        init();
        XPathParts xpp = new XPathParts(null,null);
//        System.out.println("[] initting from pod " + locale + " with prefix " + xpathPrefix);
        CLDRFile aFile = new CLDRFile(src, true);
        XPathParts pathParts = new XPathParts(null, null);
        XPathParts fullPathParts = new XPathParts(null, null);
        List examplesResult = new ArrayList();
        /*srl*/
        boolean ndebug = false;
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
                useShorten = true;
            } else {
                removePrefix = "//ldml/numbers/currencies/currency";
                useShorten = true;
                hackCurrencyDisplay = true;
            }
        } else if(xpathPrefix.startsWith("//ldml/dates")) {
            useShorten = true;
            if(xpathPrefix.startsWith("//ldml/dates/timeZoneNames")) {
                removePrefix = "//ldml/dates/timeZoneNames/zone";
                excludeTimeZones = false;
            } else {
                removePrefix = "//ldml/dates/calendars/calendar";
                excludeTimeZones = true;
                if(xpathPrefix.indexOf("gregorian")==-1) {
                    excludeGrego = true; 
                    // nongreg
                } else {
                    removePrefix = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]";
                    
                    // Add the fake 'dateTimes/availableDateFormats/new'
                    Pea myp = getPea(FAKE_FLEX_THING);
                    String spiel = "<i>Use this item to add a new availableDateFormat</i>";
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
        for(Iterator it = aFile.iterator(xpathPrefix);it.hasNext();) {
            boolean confirmOnly = false;
            String xpath = (String)it.next();
            if(SHOW_TIME) {
                count++;
                if((count%250)==0) {
                    System.err.println("[] " + locale + ":"+xpathPrefix +" #"+count+", or "+
                        (((double)(System.currentTimeMillis()-countStart))/count)+"ms per.");
                }
            }
            
            if(doExcludeAlways && excludeAlways.matcher(xpath).matches()) {
 //if(ndebug)    System.err.println("ns1  "+(System.currentTimeMillis()-nextTime) + " " + xpath);
                continue;
            } else if(excludeMost && mostPattern.matcher(xpath).matches()) {
//if(ndebug)     System.err.println("ns1  "+(System.currentTimeMillis()-nextTime) + " " + xpath);
                continue;
            } else if(excludeCurrencies && (xpath.startsWith("//ldml/numbers/currencies/currency"))) {
//if(ndebug)     System.err.println("ns1  "+(System.currentTimeMillis()-nextTime) + " " + xpath);
                continue;
            } else if(excludeCalendars && (xpath.startsWith("//ldml/dates/calendars"))) {
//if(ndebug)     System.err.println("ns1  "+(System.currentTimeMillis()-nextTime) + " " + xpath);
                continue;
            } else if(excludeTimeZones && (xpath.startsWith("//ldml/dates/timeZoneNames/zone"))) {
//if(ndebug)     System.err.println("ns1  "+(System.currentTimeMillis()-nextTime) + " " + xpath);
                continue;
            } else if(!excludeCalendars && excludeGrego && (xpath.startsWith(SurveyMain.GREGO_XPATH))) {
//if(ndebug)     System.err.println("ns1  "+(System.currentTimeMillis()-nextTime) + " " + xpath);
                continue;
            }

            String fullPath = aFile.getFullXPath(xpath);
            //int xpath_id = src.xpt.getByXpath(fullPath);
            int base_xpath = src.xpt.xpathToBaseXpathId(xpath);
            String baseXpath = src.xpt.getById(base_xpath);
            
            
///*srl*/            System.err.println("X: "+xpath+"\nF: "+fullPath+"\nB: " + baseXpath);
            
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
///*srl*/System.err.println("LT = " + lastType);
            String displaySuffixXpath;
            String peaSuffixXpath = null; // if non null:  write to suffixXpath
            
            // these need to work on the base
            String fullSuffixXpath = baseXpath.substring(xpathPrefix.length(),baseXpath.length());
///*srl*/System.err.println("Fs:"+fullSuffixXpath);
            if((removePrefix == null)||!baseXpath.startsWith(removePrefix)) {  
                displaySuffixXpath = baseXpath;
///*srl*/                System.err.println("RP: " + removePrefix);
            } else {
                displaySuffixXpath = baseXpath.substring(removePrefix.length(),baseXpath.length());
            }
///*srl*/System.err.println("dX:"+displaySuffixXpath);
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

            if(xpath.indexOf("default[@type")!=-1) {
                peaSuffixXpath = displaySuffixXpath;
                int n = type.lastIndexOf('/');
                if(n==-1) {
                    type = "(default type)";
                } else {
                    type = type.substring(0,n); //   blahblah/default/foo   ->  blahblah/default   ('foo' is lastType and will show up as the value)
                }
                value = lastType;
                confirmOnly = true; // can't acccept new data for this.
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
            p.base_xpath = base_xpath;
            Pea superP = getPea(type);
            peaSuffixXpath = fullSuffixXpath; // for now...
            if(peaSuffixXpath!=null) {
                p.xpathSuffix = peaSuffixXpath;
                superP.xpathSuffix = peaSuffixXpath;
            }
            p.confirmOnly = superP.confirmOnly = confirmOnly;

//if(ndebug)     System.err.println("n05  "+(System.currentTimeMillis()-nextTime));
            if(altProposed == null) {
                // just work on the supers
                if(superP.displayName == null) {
                    if(xpathPrefix.startsWith("//ldml/localeDisplayNames/")||
                       xpathPrefix.startsWith("//ldml/dates/timeZoneNames/zone")||
                       (xpathPrefix.startsWith("//ldml/dates") && (-1==peaSuffixXpath.indexOf("/pattern"))
                                                               && (-1==peaSuffixXpath.indexOf("availableFormats")))) {
                        superP.displayName = engFile.getStringValue(xpath(superP)); // isn't this what it's for?
                        /*
                        if(mixedType == false) {
                            superP.displayName = engFile.getStringValue(xpathPrefix+"[@type=\""+type+"\"]");
                        } else {
                            superP.displayName = engFile.getStringValue(xpathPrefix);
                        }
                        */
                    }
                }
                if(superP.displayName == null) {
                    if(isReferences) {
                        String eUri = xpp.findAttributeValue(lelement,"uri");
                       if((eUri!=null)&&eUri.length()>0) {
                           if(eUri.startsWith("isbn:")) {
                                // linkbaton doesn't have ads, and lets you choose which provider to go to (including LOC).                
                                superP.displayName = /*type + " - "+*/ "<a href='http://my.linkbaton.com/isbn/"+
                                    eUri.substring(5,eUri.length())+"'>"+eUri+"</a>";
                            } else {
                                superP.displayName = /*type + " - "+*/ "<a href='"+eUri+"'>"+eUri+"</a>";
                            }
                        } else {
                            superP.displayName = null;
                        }
                    } else if(!xpath.startsWith("//ldml/characters") && !useShorten) {
                        superP.displayName = "'"+type+"'";
                    }
                }
                if((superP.displayName == null)&& hackCurrencyDisplay) {
                    int slashDex = type.indexOf('/');
                    String cType = type;
                    if(slashDex!=-1) {
                        cType = type.substring(0,slashDex);
                    }
                    superP.displayName = engFile.getStringValue(SurveyMain.CURRENCYTYPE+cType+"']/displayName");
                    if((superP.displayName != null)&&(type.indexOf("symbol")!=-1)) {
                        superP.displayName = superP.displayName + " (symbol)";
                    }
                }
                if(superP.displayName == null) {
                    canName = false; // disable 'view by name' if not all have names.
                }
            }
//    System.err.println("n06  "+(System.currentTimeMillis()-nextTime));
            
            // If it is draft and not proposed.. make it proposed-draft 
            if( ((eDraft!=null)&&(!eDraft.equals("false"))) &&
                (altProposed == null) ) {
                altProposed = SurveyMain.PROPOSED_DRAFT;
            }
            
            // Inherit display names.
            if((superP != p) && (p.displayName == null)) {
                p.displayName = superP.displayName;
            }
            String sourceLocale = aFile.getSourceLocaleID(xpath, null);

            boolean isInherited = !(sourceLocale.equals(locale));
            
//    System.err.println("n07  "+(System.currentTimeMillis()-nextTime));
    
            if(altProposed == null) {
                if(!isInherited) {
                    superP.hasInherited=false;
                    p.hasInherited=false;
                } else {
                    p.hasInherited = true;
                    p.inheritFrom = sourceLocale;
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
                   p.inheritFrom=sourceLocale;
                }
            }
            
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
                    } else if (!(isCodeFallback &&
                        (status.getCause() instanceof org.unicode.cldr.test.CheckForExemplars))) { 
                        // skip codefallback exemplar complaints (i.e. 'JPY' isn't in exemplars).. they'll show up in missing
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
            myItem.xpath = xpath;
            myItem.xpathId = src.xpt.getByXpath(xpath);
    
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
 //               myItem.examplesList = examplesResult;
   //             examplesResult = new ArrayList(); // getExamples will clear it.
            }

            myItem.inheritFrom=setInheritFrom;
            if((eRefs != null) && (!isInherited)) {
                myItem.references = eRefs;
            }

//if(ndebug)    System.err.println("n09  "+(System.currentTimeMillis()-nextTime));
            /*srl*/
    /*
            nextTime=System.currentTimeMillis();
            if(lastTime>0) {
                long thisTime = nextTime-lastTime;
                if(thisTime>longestTime) {
                    longestPath=xpath;
                    longestTime=thisTime;
                    System.err.println("Longest: " + longestTime+"ms, " +xpath);
                }
            }            
            if(lastTime>0) {
                t--;
     //if(ndebug)           if(t==0) {
                    return;
                }
            }
            lastTime=nextTime;
    */
            /*end srl*/
            

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
