//
//  DataSection.java
//
//  Created by Steven R. Loomis on 18/11/2005.
//  Copyright 2005-2009 IBM. All rights reserved.
//

//  TODO: this class now has lots of knowledge about specific data types.. so does SurveyMain
//  Probably, it should be concentrated in one side or another- perhaps SurveyMain should call this
//  class to get a list of displayable items?

package org.unicode.cldr.web;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.LDMLUtilities;
import org.unicode.cldr.util.PathUtilities;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.web.UserRegistry.User;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;

/** A data section represents a group of related data that will be displayed to users in a list
 * such as, "all of the language display names contained in the en_US locale".
 * It is sortable, as well, and has some level of persistence.
 * This class was formerly, and unfortunately, named DataPod
 **/

public class DataSection extends Registerable {

    /**
     * Trace in detail time taken to populate?
     */
    private static final boolean TRACE_TIME=false;
    
    /**
     * Show time taken to populate?
     */
    private static final boolean SHOW_TIME= false || TRACE_TIME;

    /**
     * Warn user why these messages are showing up.
     */
    static {
        if(TRACE_TIME==true) {
            System.err.println("DataSection: Note, TRACE_TIME is TRUE");
        }
    }

    long touchTime = -1; // when has this pod been hit?
    
    public void touch() {
        touchTime = System.currentTimeMillis();
    }
    public long age() {
        return System.currentTimeMillis() - touchTime;
    }
    // UI strings
    boolean canName = true; // can the Display Name be used for sorting?
//    boolean simple = false; // is it a 'simple code list'?
    
    public static final String DATASECTION_MISSING = "Inherited";
    public static final String DATASECTION_NORMAL = "Normal";
    public static final String DATASECTION_PRIORITY = "Priority";
    public static final String DATASECTION_PROPOSED = "Proposed";
    public static final String DATASECTION_VETPROB = "Vetting Issue";

    public static final String EXEMPLAR_ONLY = "//ldml/dates/timeZoneNames/zone/*/exemplarCity";
    public static final String EXEMPLAR_EXCLUDE = "!exemplarCity";
    public static final String EXEMPLAR_PARENT = "//ldml/dates/timeZoneNames/zone";
    
    public String[] LAYOUT_INTEXT_VALUES = { "titlecase-words", "titlecase-firstword", "lowercase-words", "mixed" }; // layout/inText/* - from UTS35
    public String[] LAYOUT_INLIST_VALUES = { "titlecase-words", "titlecase-firstword", "lowercase-words", "mixed"}; // layout/inList/* - from UTS35
    public String[] METAZONE_COMMONLYUSED_VALUES = { "true","false" }; // layout/inText/* - from UTS35

    static final Pattern NAME_TYPE_PATTERN = Pattern.compile("[a-zA-Z0-9]+|.*exemplarCity.*");

    public String xpathPrefix = null;
    
//    public boolean exemplarCityOnly = false;
    
    private String fieldHash; // prefix string used for calculating html fields
    private SurveyMain sm;
    
    public boolean hasExamples = false;
    
    public String intgroup; 
    DataSection(SurveyMain sm, CLDRLocale loc, String prefix) {
        super(sm.lcr,loc); // initialize call to LCR

        this.sm = sm;
        xpathPrefix = prefix;
        fieldHash =  CookieSession.cheapEncode(sm.xpt.getByXpath(prefix));
        intgroup = loc.getLanguage(); // calculate interest group
    }
    private static int n =0;
    protected static synchronized int getN() { return ++n; } // serial number
        
    /** 
     * This class represents an Example box, so that it can be stored and restored.
     */ 
    public class ExampleEntry {

        public String hash = null;

        public DataSection section;
        public DataSection.DataRow dataRow;
        public DataRow.CandidateItem item;
        public CheckCLDR.CheckStatus status;
        
        public ExampleEntry(DataSection section, DataRow p, DataRow.CandidateItem item, CheckCLDR.CheckStatus status) {
            this.section = section;
            this.dataRow = p;
            this.item = item;
            this.status = status;

            hash = CookieSession.cheapEncode(DataSection.getN()) +  // unique serial #- covers item, status..
                this.section.fieldHash(p);   /* fieldHash ensures that we don't get the wrong field.. */
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
     * Given a hash, see addExampleEntry, retrieve the ExampleEntry which has section, row, candidateitem and status
     */
    ExampleEntry getExampleEntry(String hash) {
        synchronized(exampleHash) {
            return (DataSection.ExampleEntry)exampleHash.get(hash);
        }
    }
    
    /* get a short key for use in fields */
    public String fieldHash(DataRow p) {
        return fieldHash + p.fieldHash();
    }

    /**
     * @deprecated use p.xpath()
     * @param p
     */
    public String xpath(DataRow p) {
    	return p.xpath();
    }
        
    static Collator getOurCollator() {
        RuleBasedCollator rbc = 
            ((RuleBasedCollator)Collator.getInstance());
        rbc.setNumericCollation(true);
        return rbc;
    }
    
    final Collator myCollator = getOurCollator();
    
    /**
     * This class represents a "row" of data - a single distinguishing xpath
     * This class was formerly (and unfortunately) named "Pea"
     * @author srl
     *
     */
    public class DataRow {
        DataRow parentRow = this; // parent - defaults to self if it is a "super" (i.e. parent without any alternate)
        
        // what kind of row is this?
        public boolean confirmOnly = false; // if true: don't accept new data, this row is something that might be confusing to input.
        public boolean zoomOnly = false; // if true - don't show any editing in the zoomout view, they must zoom in.
//        public DataRow toggleWith = null;    // obsolete: pea is a TOGGLE ( true / false ) with another pea.   Special rules apply.
//        public boolean toggleValue = false;  // obsolete: 
        String[] valuesList = null; // if non null - list of acceptable values.  If null, freeform input
//        public AttributeChoice attributeChoice = null; // obsolete: is an attributed list of items
        
        public String type = null;
        public String uri = null; // URI for the type
        
        public String xpathSuffix = null; // if null:  prefix+type is sufficient (simple list).  If non-null: mixed Pod, prefix+suffix is required and type is informative only.
        public String displayName = null;
        public String altType = null; // alt type (NOT to be confused with -proposedn)
        int base_xpath = -1;

	public String getXpath() {
		return sm.xpt.getById(base_xpath);
	}
	public int getXpathId() {
		return base_xpath;
	}
	public String getPrettyPath() {
		return sm.xpt.getPrettyPath(base_xpath);
	}
        
        // true even if only the non-winning subitems have tests.
        boolean hasTests = false;

        // the xpath id of the winner. If no winner or n/a, -1. 
        int winningXpathId = -1;
        
        // these apply to the 'winning' item, if applicable
        boolean hasErrors = false;
        boolean hasWarnings = false;
        
        Vetting.Status confirmStatus = Vetting.Status.INDETERMINATE;
        
        // do any items have warnings or errs?
        boolean anyItemHasWarnings = false;
        boolean anyItemHasErrors = false;
        
        // Do some items alias to a different base xpath or locale?
        String aliasFromLocale  = null; // locale, if different
        int aliasFromXpath      = -1;   // xpath, if different
        
        boolean hasProps = false; // true if has some proposed items
        boolean hasMultipleProposals = false; // true if more than 1 proposal is available
        boolean hasInherited = false; // True if has inherited value
        public int allVoteType = 0; // bitmask of all voting types included
        public int voteType = 0; // status of THIS item
        public int reservedForSort = -1; // ordering for use in collator.
        
//        String inheritFrom = null;
//        String pathWhereFound = null;
        /**
         * The Item is a particular alternate which could be chosen 
         * It was previously named "Item"
         */
        public class CandidateItem implements java.lang.Comparable {
            String pathWhereFound = null;
            String inheritFrom = null;
            boolean isParentFallback = false; // true if it is not actually part of this locale,but is just the parent fallback ( .inheritedValue );
            public String altProposed = null; // proposed part of the name (or NULL for nondraft)
            public int submitter = -1; // if this was submitted via ST, record user id. ( NOT from XML - in other words, we won't be parsing 'proposed-uXX' items. ) 
            public String value = null; // actual value
            public int id = -1; // id of CLDR_DATA table row
            public List tests = null;
            boolean itemErrors = false;
            public Vector examples = null; 
            //public List examplesList = null;
            String references = null;
            String xpath = null; // xpath of actual item
            int xpathId = -1; // xpid of actual item
            boolean isFallback = false; // item is from the parent locale - don't consider it a win.
            
            public Set<UserRegistry.User> votes = null; // Set of Users who voted for this.
            boolean checkedVotes = false;
            
            public Set<UserRegistry.User> getVotes() {
                if(!checkedVotes) {
                    if(!isFallback) {
                        votes = sm.vet.gatherVotes(locale, xpath);
                    }
                    checkedVotes = true;
                }
                return votes;
            }
            
            public String example = "";
            
            public String toString() { 
                return "{Item v='"+value+"', altProposed='"+altProposed+"', inheritFrom='"+inheritFrom+"'}";
            }
			
			public int compareTo(Object other) {
                if(other == this) {
                    return 0;
                }
				CandidateItem i = (CandidateItem) other;
				int rv = value.compareTo(i.value);
				if(rv == 0) {
					rv = xpath.compareTo(i.xpath);
				}
				return rv;
			}
			
			/**
			 * Is this a winning (non fallback) item?
			 */
			public boolean isWinner() {
				String resultXpath = getResultXpath();
				return ((resultXpath!=null)&&
			            (this.xpath!=null)&&
			            (this.xpath.equals(resultXpath))&& // todo: replace string.equals with comparison to item.xpathId . .
			            !this.isFallback);			
			}
          
            /* return true if any valid tests were found */
            public boolean setTests(List testList) {
                tests = testList;
                // only consider non-example tests as notable.
                boolean weHaveTests = false;
                int errorCount = 0;
                int warningCount =0 ;
                for (Iterator it3 = tests.iterator(); it3.hasNext();) {
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
                    /* row */ hasTests = true;
                    parentRow.hasTests = true;
                                        
                    if(((winningXpathId==-1)&&(xpathId==base_xpath)) || (xpathId == winningXpathId)) {
                        if(errorCount>0) /* row */ hasErrors = true;
                        if(warningCount>0) /* row */ hasWarnings = true;
                        // propagate to parent
                        if(errorCount>0) /* row */ parentRow.hasErrors = true;
                        if(warningCount>0) /* row */ parentRow.hasWarnings = true;
                    }
                   
                    if(errorCount>0) /* row */ { itemErrors=true;  anyItemHasErrors = true;  parentRow.anyItemHasErrors = true; }
                    if(warningCount>0) /* row */ anyItemHasWarnings = true;
                    // propagate to parent
                    if(warningCount>0) /* row */ parentRow.anyItemHasWarnings = true;
                }
                return weHaveTests;
            }

            /**
             * Did any voters in my org vote for it?
             * @param me
             */
            public boolean votesByMyOrg(User me) {
                if(me==null || getVotes()==null) return false;
                for(UserRegistry.User u : getVotes()) {
                    if(u.org.equals(me.org) && u.id!=me.id) {
                        return true;
                    }
                }
                return false;
            }
        }
        
        CandidateItem previousItem = null;
        CandidateItem inheritedValue = null; // vetted value inherited from parent
        
        public String toString() {
            return "{DataRow t='"+type+"', n='"+displayName+"', x='"+xpathSuffix+"', item#='"+items.size()+"'}";
        }
        
        public String getDisplayName() {
            return displayName;
        }

        public Set<CandidateItem> items = new TreeSet<CandidateItem>(new Comparator<CandidateItem>() {
                    public int compare(CandidateItem o1, CandidateItem o2){
                        CandidateItem p1 = (CandidateItem) o1;
                        CandidateItem p2 = (CandidateItem) o2;
                        if(p1==p2) { 
                            return 0;
                        }
                        if((p1.altProposed==null)&&(p2.altProposed==null)) return 0;
                        if(p1.altProposed == null) return -1;
                        if(p2.altProposed == null) return 1;
                        return myCollator.compare(p1.altProposed, p2.altProposed);
                    }
                });
        public CandidateItem addItem(String value, String altProposed, List tests) {
            CandidateItem pi = new CandidateItem();
            pi.value = value;
            pi.altProposed = altProposed;
            pi.tests = tests;
            items.add(pi);
///*srl*/            if(type.indexOf("Chicago")>-1) {
//               System.out.println("@@ "+type+"  v: " + pi.value);
//            }
            
            return pi;
        }
        
        String myFieldHash = null; /** Cache of field hash **/
        
        /**
         * Calculate the hash used for HTML forms for this row
         */
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
        
        public String fullFieldHash() {
        	return /*section.*/fieldHash + fieldHash();
        }
        
        Hashtable<String,DataRow> subRows = null;

        public DataRow getSubDataRow(String altType) {
            if(altType == null) {
                return this;
            }
            if(subRows == null) {
                subRows = new Hashtable<String,DataRow>();
            }

            DataRow p = subRows.get(altType);
            if(p==null) {
                p = new DataRow();
                p.type = type;
                p.altType = altType;
                p.parentRow = this;
                subRows.put(altType, p);
            }
            return p;
        }
        
        /**
         * Calculate the item from the vetted parent locale, without any tests
         * @param vettedParent CLDRFile for the parent locale, resolved with vetting on
         */
        void updateInheritedValue(CLDRFile vettedParent) {
            updateInheritedValue(vettedParent,null, null);
        }
        
        /**
         * Calculate the item from the vetted parent locale, possibly including tests
         * @param vettedParent CLDRFile for the parent locale, resolved with vetting on
         * @param checkCldr The tests to use
         * @param options Test options
         */
        void updateInheritedValue(CLDRFile vettedParent, CheckCLDR checkCldr, Map options) {
            long lastTime = System.currentTimeMillis();
            if(vettedParent == null) {
                return;
            }
            
            if(base_xpath == -1) {
                return;
            }
            

            String xpath = sm.xpt.getById(base_xpath);
            if(TRACE_TIME) System.err.println("@@0:"+(System.currentTimeMillis()-lastTime));
            if(xpath == null) {
                return;
            }
            
            if((vettedParent != null) && (inheritedValue == null)) {
                String value = vettedParent.getStringValue(xpath);
                if(TRACE_TIME) System.err.println("@@1:"+(System.currentTimeMillis()-lastTime));
                
                if(value != null) {
                    inheritedValue = new CandidateItem();
                    if(TRACE_TIME) System.err.println("@@2:"+(System.currentTimeMillis()-lastTime));
                    inheritedValue.isParentFallback=true;

                    CLDRFile.Status sourceLocaleStatus = new CLDRFile.Status();
                    if(TRACE_TIME) System.err.println("@@3:"+(System.currentTimeMillis()-lastTime));
                    String sourceLocale = vettedParent.getSourceLocaleID(xpath, sourceLocaleStatus);
                    if(TRACE_TIME) System.err.println("@@4:"+(System.currentTimeMillis()-lastTime));

                    inheritedValue.inheritFrom = sourceLocale;
                    
                    if(sourceLocaleStatus!=null && sourceLocaleStatus.pathWhereFound!=null && !sourceLocaleStatus.pathWhereFound.equals(xpath)) {
                        inheritedValue.pathWhereFound = sourceLocaleStatus.pathWhereFound;
                        if(TRACE_TIME) System.err.println("@@5:"+(System.currentTimeMillis()-lastTime));
                        
                        // set up Pod alias-ness
                        aliasFromLocale = sourceLocale;
                        aliasFromXpath = sm.xpt.xpathToBaseXpathId(sourceLocaleStatus.pathWhereFound);
                        if(TRACE_TIME) System.err.println("@@6:"+(System.currentTimeMillis()-lastTime));
                    }
                    
                    inheritedValue.value = value;
                    inheritedValue.xpath = xpath;
                    inheritedValue.xpathId = base_xpath;
                    inheritedValue.isFallback = true;                    
                } else {
                 //   throw new InternalError("could not get inherited value: " + xpath);
                }
            }
            
            if((checkCldr != null) && (inheritedValue != null) && (inheritedValue.tests == null)) {
                if(TRACE_TIME) System.err.println("@@7:"+(System.currentTimeMillis()-lastTime));
                List iTests = new ArrayList();
                checkCldr.check(xpath, xpath, inheritedValue.value, options, iTests);
                if(TRACE_TIME) System.err.println("@@8:"+(System.currentTimeMillis()-lastTime));
             //   checkCldr.getExamples(xpath, fullPath, value, ctx.getOptionsMap(), examplesResult);
                if(!iTests.isEmpty()) {
                    inheritedValue.setTests(iTests);
                    if(TRACE_TIME) System.err.println("@@9:"+(System.currentTimeMillis()-lastTime));
                }
            }
            if(TRACE_TIME) System.err.println("@@10:"+(System.currentTimeMillis()-lastTime));
        }
 
        /**
         * A Shim is a candidate item which does not correspond to actual XML data, but is synthesized. 
         * 
         * @param base_xpath
         * @param base_xpath_string
         * @param checkCldr
         * @param options
         */
        void setShimTests(int base_xpath,String base_xpath_string,CheckCLDR checkCldr,Map options) {
            CandidateItem shimItem = inheritedValue;
            
            if(shimItem == null) {
                shimItem = new CandidateItem();

                shimItem.value = null;
                shimItem.xpath = base_xpath_string;
                shimItem.xpathId = base_xpath;
                shimItem.isFallback = false;
        
                List iTests = new ArrayList();
                checkCldr.check(base_xpath_string, base_xpath_string, null, options, iTests);
                if(!iTests.isEmpty()) {
                    // Got a bite.
                    if(shimItem.setTests(iTests)) {
                        // had valid tests
                        inheritedValue = shimItem;
                        inheritedValue.isParentFallback = true;
                    }
                }
            } else {
//                if(SurveyMain.isUnofficial) System.err.println("already have inherited @ " + base_xpath_string);
            }
        }
       
        /**
         * Utility function
         * @param str String to modify
         * @param oldEnd old suffix
         * @param newEnd new suffix
         * @return the modified string
         */
        private String replaceEndWith(String str, String oldEnd, String newEnd) {
            if(!str.endsWith(oldEnd)) {
                throw new InternalError("expected " + str + " to end with " + oldEnd);
            }
            return str.substring(0,str.length()-oldEnd.length())+newEnd;
        }
        
//        void updateToggle(String path, String attribute) {
//            if(true == true) {
//                confirmOnly = true;
//                return; /// Disable toggles - for now.
//            }
//            
//            
//            
//            XPathParts parts = new XPathParts(null,null);
//            parts.initialize(path);
//            String lelement = parts.getElement(-1);
//            String eAtt = parts.findAttributeValue(lelement, attribute);
//            if(eAtt == null) {
//                System.err.println(this + " - no attribute " + attribute + " in " + path);
//            }
//            toggleValue = eAtt.equals("true");
//            
//            //System.err.println("DataRow: " + type + " , toggle of val: " + myValue + " at xpath " + path);
//            String myValueSuffix = "[@"+attribute+"=\""+toggleValue+"\"]";
//            String notMyValueSuffix = "[@"+attribute+"=\""+!toggleValue+"\"]";
//            
//            if(!type.endsWith(myValueSuffix)) {
//                throw new InternalError("toggle: expected "+ type + " to end with " + myValueSuffix);
//            }
//            
//            String typeNoValue =  type.substring(0,type.length()-myValueSuffix.length());
//            String notMyType = typeNoValue+notMyValueSuffix;
//            
//            
//            DataRow notMyDataRow = getDataRow(notMyType);
//            if(notMyDataRow.toggleWith == null) {
//                notMyDataRow.toggleValue = !toggleValue;
//                notMyDataRow.toggleWith = this;
//
//                String my_base_xpath_string = sm.xpt.getById(base_xpath);
//                String not_my_base_xpath_string = replaceEndWith(my_base_xpath_string, myValueSuffix, notMyValueSuffix);
//                notMyDataRow.base_xpath = sm.xpt.getByXpath(not_my_base_xpath_string);
//
//                notMyDataRow.xpathSuffix = replaceEndWith(xpathSuffix,myValueSuffix,notMyValueSuffix);
//
//                //System.err.println("notMyRow.xpath = " + xpath(notMyRow));
//            }
//            
//            toggleWith = notMyDataRow;
//            
//        }

        public boolean isName() {
          return NAME_TYPE_PATTERN.matcher(type).matches();
        }

		public CLDRLocale getLocale() {
			return locale;
		}

		public String getIntgroup() {
			return intgroup;
		}

		public String xpath() {
	        String path = xpathPrefix;
	        if(path == null) {
	            throw new InternalError("Can't handle mixed rows with no prefix");
	        }
	        if(xpathSuffix == null) {
	            if(type != null) {
	                path = path + "[@type='" + type +"']";
	            }
	            if(altType != null) {
	                path = path + "[@alt='" + altType +"']";
	            }
	        } else {
//		            if(p.xpathSuffix.startsWith("[")) {
	                return xpathPrefix +  xpathSuffix;
//		            } else {
//		                return xpathPrefix+"/"+p.xpathSuffix;
//		            }
	        }
	        
	        return path;
		}

		private String resultXpath = null;
		private int resultType[] = new int[1];
		private int resultXpath_id = -1;
		private boolean errorNoOutcome  = false;
		
		public String getResultXpath() {
			if(resultXpath == null) {
				if(base_xpath==-1) return xpath(); /* pseudo element. no real path */
				int resultType[] = new int[1];
				resultXpath_id =  sm.vet.queryResult(locale, base_xpath, resultType);
				if(resultXpath_id != -1) {
					resultXpath = sm.xpt.getById(resultXpath_id); 
				} else {
					// noWinner = true;
				}
				errorNoOutcome = (resultType[0]==Vetting.RES_ERROR)&&(resultXpath_id==-1); // error item - NO result.
			}
			return resultXpath;
		}
		
		public int getResultType() {
			getResultXpath();
			return resultType[0];
		}
		
		public int getResultXpathId() {
			getResultXpath();
			return resultXpath_id;
		}
		
		public boolean getErrorNoOutcome() {
			getResultXpath();
			return errorNoOutcome;
		}
		
		public String getWinningValue() {
			for(CandidateItem i : items) {
				if(i.isWinner()) {
					return i.value;
				}
			}

			List<CandidateItem> topItems = getCurrentItems();
			if(topItems==null || topItems.size()<1) {
				return null;
			}
			return topItems.get(0).value;
		}
		
		
		/**
		 * Get a list of current CandidateItems for this Row.
		 * The top item will be the winning value, if any. 
		 */
		public synchronized List<CandidateItem> getCurrentItems() {
			if(this.currentItems==null) {

				String resultXpath = getResultXpath();

				List<DataSection.DataRow.CandidateItem> currentItems = new ArrayList<DataSection.DataRow.CandidateItem>();
				List<DataSection.DataRow.CandidateItem> proposedItems = new ArrayList<DataSection.DataRow.CandidateItem>();

				for(Iterator j = items.iterator();j.hasNext();) {
					DataSection.DataRow.CandidateItem item = (DataSection.DataRow.CandidateItem)j.next();
					if(
							(  (item.xpathId == resultXpath_id) ||
									(resultXpath_id==-1 && item.xpathId==this.base_xpath
											&& !errorNoOutcome)  ) &&  // do NOT add as current, if vetting said 'no' to current item.
											!(item.isFallback || (item.inheritFrom != null))) { 
						currentItems.add(item); 
					} else {
						proposedItems.add(item);
					}
				}
				// if there is an inherited value available - see if we need to show it.
				if((inheritedValue != null) &&
						(inheritedValue.value != null)  // and it isn't a shim
						/* && p.inheritedValue.pathWhereFound == null */ 
				/* && !p.inheritedValue.isParentFallback */ ) { // or an alias
					if(currentItems.isEmpty()) {  // no other current items.. 
						currentItems.add(inheritedValue); 
					} else {
						boolean found = false;
						for( DataSection.DataRow.CandidateItem i : proposedItems ) {
							if(inheritedValue.value.equals(i.value)) {
								found = true;
							}
						}
						if (!found) for( DataSection.DataRow.CandidateItem i : currentItems ) {
							if(inheritedValue.value.equals(i.value)) {
								found = true;
							}
						}
						if(!found) {
							proposedItems.add(inheritedValue);
						}
					}
				}
				this.currentItems=currentItems;
				this.proposedItems=proposedItems;
			}
			return this.currentItems;
		}
		
		
		
		List<CandidateItem> currentItems = null, proposedItems=null;

		/**
		 * Get a list of proposed items, if any.
		 */
		public List<CandidateItem> getProposedItems() {
			// TODO Auto-generated method stub
			if(currentItems==null) {
				getCurrentItems();
			}
			return proposedItems;
		}
        
    }

    Hashtable<String, DataRow> rowsHash = new Hashtable<String, DataRow>(); // hashtable of type->Row
 
    /**
     * get all rows.. unsorted.
     */
    public Collection<DataRow> getAll() {
        return rowsHash.values();
    }
    
    public abstract class PartitionMembership {
        public abstract boolean isMember(DataRow p);
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
     * A class representing a list of rows, in sorted and divided order.
     */
    public class DisplaySet {
        public int size() {
            return rows.size();
        }
        String sortMode = null;
        public boolean canName = true; // can use the 'name' view?
        public List<DataRow> rows; // list of peas in sorted order
        public List<DataRow> displayRows; // list of Strings suitable for display
        /**
         * Partitions divide up the peas into sets, such as 'proposed', 'normal', etc.
         * The 'limit' is one more than the index number of the last item.
         * In some cases, there is only one partition, and its name is null.
         */
        
        public Partition partitions[];  // display group partitions.  Might only contain one entry:  {null, 0, <end>}.  Otherwise, contains a list of entries to be named separately

        public DisplaySet(List<DataRow> myRows, List<DataRow> myDisplayRows, String sortMode) {
            this.sortMode = sortMode;
            
            rows = myRows;
            displayRows = myDisplayRows;

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
            Vector<Partition> v = new Vector<Partition>();
            if(sortMode.equals(SurveyMain.PREF_SORTMODE_WARNING)) { // priority
                Partition testPartitions[] = (SurveyMain.isPhaseSubmit()||SurveyMain.isPhaseVetting())?createSubmitPartitions():
                                                                           createVettingPartitions();
                // find the starts
                int lastGood = 0;
                DataRow peasArray[] = null;
                peasArray = (DataRow[])rows.toArray(new DataRow[0]);
                for(int i=0;i<peasArray.length;i++) {
                    DataRow p = peasArray[i];
                                        
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
                    testPartitions[lastGood].limit = rows.size(); // limit = off the end.
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
                v.add(new Partition(null, 0, rows.size()));
            }
            partitions = (Partition[])v.toArray(new Partition[0]); // fold it up
        }

    }

	public static String PARTITION_ERRORS = "Error Values";
	public static String CHANGES_DISPUTED = "Disputed";
	public static String PARTITION_UNCONFIRMED = "Unconfirmed";
	public static String TENTATIVELY_APPROVED = "Tentatively Approved";
	public static String STATUS_QUO = "Status Quo";
    
    public static final String VETTING_PROBLEMS_LIST[] = { 
        PARTITION_ERRORS,
        CHANGES_DISPUTED,
        PARTITION_UNCONFIRMED };


    private Partition[] createVettingPartitions() {
        return createSubmitPartitions(); // added disputed into the Submit partitions
    }

    private Partition[] createSubmitPartitions() {
      Partition theTestPartitions[] = 
      {                 
              new Partition("Errors", 
                      new PartitionMembership() { 
                public boolean isMember(DataRow p) {
                  return (p.hasErrors);
                }
              }),
              new Partition("Disputed", 
                      new PartitionMembership() { 
                public boolean isMember(DataRow p) {
                  return ((p.allVoteType & Vetting.RES_DISPUTED)>0) ; // not sure why "allVoteType" is needed
                }
              }),
              new Partition("Warnings", 
                      new PartitionMembership() { 
                public boolean isMember(DataRow p) {
                  return (p.hasWarnings);
                }
              }),
//            Later, we might want more groups.
//            INDETERMINATE (-1),
//            APPROVED (0),
//            CONTRIBUTED (1),
//            PROVISIONAL (2),
//            UNCONFIRMED (3);
              new Partition("Not (minimally) Approved", 
                      new PartitionMembership() { 
                public boolean isMember(DataRow p) {
                  return p.winningXpathId != -1 
                  && p.confirmStatus != Vetting.Status.APPROVED
                  && p.confirmStatus != Vetting.Status.CONTRIBUTED;
                  // || p.winningXpathId == -1 && p.hasMultipleProposals;
                }
              }),
              new Partition("Approved", 
                      new PartitionMembership() { 
                public boolean isMember(DataRow p) {
                  return p.winningXpathId != -1; // will be APPROVED
                }
              }),
              new Partition("Missing", 
                      new PartitionMembership() { 
                public boolean isMember(DataRow p) {
                  //return "root".equals(p.aliasFromLocale) || XMLSource.CODE_FALLBACK_ID.equals(p.aliasFromLocale);
                  return p.inheritedValue!=null && // found inherited item (extrapaths and some special paths may not have an inherited item)
                  ( "root".equals(p.inheritedValue.inheritFrom) 
                          || XMLSource.CODE_FALLBACK_ID.equals(p.inheritedValue.inheritFrom) );
                  /*
       p.winningXpathId==-1 &&    // no winning item
       p.inheritedValue!=null && // found inherited item (extrapaths and some special paths may not have an inherited item)
           ( "root".equals(p.inheritedValue.inheritFrom) ||XMLSource.CODE_FALLBACK_ID,equals(p.inheritedValue.inheritFrom) )
                   */
                }
              }),
              new Partition("Inherited", 
                      new PartitionMembership() { 
                public boolean isMember(DataRow p) {
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
    
    private Hashtable<String, List<DataRow>> listHash = new Hashtable<String, List<DataRow>>();  // hash of sortMode->pea
    
    /**
     * get a List of peas, in sorted order 
     */
    public List<DataRow> getList(String sortMode) {
        List<DataRow> aList = (List<DataRow>)listHash.get(sortMode);
        if(aList == null) {
            aList = getList(sortMode, null);
        }
        listHash.put(sortMode, aList);
        return aList;
    }
        
    public List getList(String sortMode, Pattern matcher) {
    //        final boolean canName = canName;
        Set<DataRow> newSet;
        
    //                final com.ibm.icu.text.RuleBasedCollator rbc = 
    //                    ((com.ibm.icu.text.RuleBasedCollator)com.ibm.icu.text.Collator.getInstance());
    //                rbc.setNumericCollation(true);

        
        if(sortMode.equals(SurveyMain.PREF_SORTMODE_CODE)) {
            newSet = new TreeSet<DataRow>(COMPARE_CODE);
        } else if (sortMode.equals(SurveyMain.PREF_SORTMODE_WARNING)) {
            newSet = new TreeSet<DataRow>(COMPARE_PRIORITY);
        } else if(sortMode.equals(SurveyMain.PREF_SORTMODE_NAME)) {
            newSet = new TreeSet<DataRow>(COMPARE_NAME);
        } else {
            throw new InternalError("Unknown or unsupported sort mode: " + sortMode);
        }
        
        if(matcher == null) {
            newSet.addAll(rowsHash.values()); // sort it    
        } else {
            for(Object o : rowsHash.values()) {
                DataRow p = (DataRow)o;
                                
///*srl*/         /*if(p.type.indexOf("Australia")!=-1)*/ {  System.err.println("xp: "+p.xpathSuffix+":"+p.type+"- match: "+(matcher.matcher(p.type).matches())); }

                if(matcher.matcher(p.type).matches()) {
                    newSet.add(p);
                }
            }
        }
        
        ArrayList aList = new ArrayList(); // list it (waste here??)
        aList.addAll(newSet);
        if(matcher != null) {
///*srl*/ System.err.println("Pruned match of " + aList.size() + " items from " + peasHash.size());
        }

        return aList;
    }
    
    /**
     * Comparator that just compares codes
     */
    final Comparator<DataRow> COMPARE_CODE = new Comparator<DataRow>() {
      //                        com.ibm.icu.text.Collator myCollator = rbc;
      public int compare(DataRow p1, DataRow p2){
        if(p1==p2) { 
          return 0;
        }
        return myCollator.compare(p1.type, p2.type);
      }
    };
    
    /**
     * Comparator that compares priorities, then codes (used to be priorities, then names, then codes)
     */
    final Comparator<DataRow> COMPARE_PRIORITY = new Comparator<DataRow>() {

      int categorizeDataRow(DataRow p, Partition partitions[]) {
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

      final Partition[] warningSort = (SurveyMain.isPhaseVetting()||SurveyMain.isPhaseSubmit())?createSubmitPartitions():
        createVettingPartitions();
//    com.ibm.icu.text.Collator myCollator = rbc;
      public int compare(DataRow p1, DataRow p2){
        if(p1==p2) {
          return 0;
        }

        int rv = 0; // neg:  a < b.  pos: a> b

        if(p1.reservedForSort==-1) {
          p1.reservedForSort = categorizeDataRow(p1, warningSort);
        }
        if(p2.reservedForSort==-1) {
          p2.reservedForSort = categorizeDataRow(p2, warningSort);
        }

        if(rv == 0) {
          if(p1.reservedForSort < p2.reservedForSort) {
            return -1;
          } else if(p1.reservedForSort > p2.reservedForSort) {
            return 1;
          }
        }
        final boolean p1IsName = p1.isName();
        final boolean p2IsName = p2.isName();
        if (p1IsName != p2IsName) { // do this for transitivity, so that names sort first if there are mixtures
          return p1IsName ? -1 : 1;
        } else if (p1IsName) {
          return COMPARE_NAME.compare(p1,p2);
        }
        return COMPARE_CODE.compare(p1,p2);

//        if(rv == 0) { // try to avoid a compare
//          String p1d  = null;
//          String p2d  = null;
//          if(canName) {
//            p1d = p1.displayName;
//            p2d = p2.displayName;
//          }
//          if(p1d == null ) {
//            p1d = p1.type;
//            if(p1d == null) {
//              p1d = "(null)";
//            }
//          }
//          if(p2d == null ) {
//            p2d = p2.type;
//            if(p2d == null) {
//              p2d = "(null)";
//            }
//          }
//          rv = myCollator.compare(p1d, p2d);
//        }
//
//        if(rv == 0) {
//          // Question for Steven. It doesn't appear that the null checks would be needed, since they aren't in COMPARE_BY_CODE
//          String p1d  = p1.type;
//          String p2d  = p2.type;
//          if(p1d == null ) {
//            p1d = "(null)";
//          }
//          if(p2d == null ) {
//            p2d = "(null)";
//          }
//          rv = myCollator.compare(p1d, p2d);
//        }
//
//        if(rv < 0) {
//          return -1;
//        } else if(rv > 0) {
//          return 1;
//        } else {
//          return 0;
//        }
      }
    };

    /**
     * Comparator that compares names, then codes
     */
    final Comparator<DataRow> COMPARE_NAME = new Comparator<DataRow>() {
      //                        com.ibm.icu.text.Collator myCollator = rbc;
      public int compare(DataRow p1, DataRow p2){
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
        int rv = myCollator.compare(p1d, p2d);
        if(rv == 0) {
          p1d  = p1.type;
          p2d  = p2.type;
          if(p1d == null ) {
            p1d = "(null)";
          }
          if(p2d == null ) {
            p2d = "(null)";
          }
          rv = myCollator.compare(p1d, p2d);
        }
        return rv;
      }
    };

    
    /** Returns a list parallel to that of getList() but of Strings suitable for display. 
    (Alternate idea: just make toString() do so on Row.. advantage here is we can adjust for sort mode.) **/
    public List getDisplayList(String sortMode) {
        return getDisplayList(sortMode, getList(sortMode));
    }
    /**
     * Returns a list parallel to that of getList, but of Strings suitable for display
     * @param sortMode the mode such as SurveyMain.PREF_SORTMODE_CODE
     * @param matcher regex to determine matching rows
     * @return the new list
     */
    public List getDisplayList(String sortMode, Pattern matcher) {
        return getDisplayList(sortMode, getList(sortMode, matcher));
    }
    
    public List getDisplayList(String sortMode, List inRows) {
        final List myPeas = inRows;
        if(sortMode.equals(SurveyMain.PREF_SORTMODE_CODE)) {
            return new AbstractList() {
                private List ps = myPeas;
                public Object get(int n) {
                  return ((DataRow)ps.get(n)).type; // always code
                }
                public int size() { return ps.size(); }
            };
        } else {
            return new AbstractList() {
                private List ps = myPeas;
                public Object get(int n) {
                  DataRow p = (DataRow)ps.get(n);
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
	 * Create, populate, and complete a DataSection given the specified locale and prefix
	 * @param ctx context to use (contains CLDRDBSource, etc.)
	 * @param locale locale
	 * @param prefix XPATH prefix
	 * @param simple if true, means that data is simply xpath+type. If false, all xpaths under prefix.
	 */
	public static DataSection make(WebContext ctx, CLDRLocale locale, String prefix, boolean simple) {
		DataSection section = new DataSection(ctx.sm, locale, prefix);
//        section.simple = simple;
        SurveyMain.UserLocaleStuff uf = ctx.getUserFile();
  
        XMLSource ourSrc = uf.dbSource;
        
        synchronized(ourSrc) {
            CheckCLDR checkCldr = uf.getCheck(ctx);
            if(checkCldr == null) {
                throw new InternalError("checkCldr == null");
            }

            

            
            
            com.ibm.icu.dev.test.util.ElapsedTimer cet;
            if(SHOW_TIME) {
                cet= new com.ibm.icu.dev.test.util.ElapsedTimer();
                System.err.println("Begin populate of " + locale + " // " + prefix+":"+ctx.defaultPtype() + " - is:" + ourSrc.getClass().getName());
            }
            CLDRFile baselineFile = ctx.sm.getBaselineFile();
            section.populateFrom(ourSrc, checkCldr, baselineFile,ctx.getOptionsMap());
			int popCount = section.getAll().size();
/*            if(SHOW_TIME) {
                System.err.println("DP: Time taken to populate " + locale + " // " + prefix +":"+ctx.defaultPtype()+ " = " + et + " - Count: " + pod.getAll().size());
            }*/
            section.ensureComplete(ourSrc, checkCldr, baselineFile, ctx.getOptionsMap());
            if(SHOW_TIME) {
				int allCount = section.getAll().size();
                System.err.println("Populate+complete " + locale + " // " + prefix +":"+ctx.defaultPtype()+ " = " + cet + " - Count: " + popCount+"+"+(allCount-popCount)+"="+allCount);
            }
        }
		return section;
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
                              "/displayName\\[@count=\"([^\"]*)\"\\]$", "/count=$1",
                              "\\[@count=\"([^\"]*)\"\\]$", "/count=$1",
                              "^unit/([^/]*)/unit([^/]*)/", "$1/$2/",
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
//                                                    "day/|"+
//                                                    "date/|"+
                                                    "Format|"+
                                                    "s/field|"+
                                                    "\\[@draft=\"true\"\\]|"+ // ???
                                                    "\\[@alt=\"[^\"]*\"\\]|"+ // ???
                                                    "/displayName$|"+  // for currency
                                                    "/standard/standard$"     );
         mostPattern = Pattern.compile("^//ldml/localeDisplayNames.*|"+  // these are excluded when 'misc' is chosen.
                                              "^//ldml/characters/exemplarCharacters.*|"+
                                              "^//ldml/numbers.*|"+
                                              "^//ldml/units.*|"+
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
    
    public static final String FAKE_FLEX_THING = "available date formats: add NEW item";
    public static final String FAKE_FLEX_SUFFIX = "dateTimes/availableDateFormats/dateFormatItem[@id=\"NEW\"]";
    public static final String FAKE_FLEX_XPATH = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/availableFormats/dateFormatItem";
    
    private void populateFrom(XMLSource ourSrc, CheckCLDR checkCldr, CLDRFile baselineFile, Map<String,String> options) {
        init();
        XPathParts xpp = new XPathParts(null,null);
//        System.out.println("[] initting from pod " + locale + " with prefix " + xpathPrefix);
        CLDRFile aFile = new CLDRFile(ourSrc, true);
        List examplesResult = new ArrayList();
        SupplementalDataInfo sdi = sm.getSupplementalDataInfo();
        long lastTime = -1;
        String workPrefix = xpathPrefix;
        long nextTime = -1;
        int count=0;
        long countStart = 0;
        if(SHOW_TIME) {
            lastTime = countStart = System.currentTimeMillis();
        }
        
        String workingCoverageLevel = options.get("CheckCoverage.requiredLevel");
        if ( workingCoverageLevel.equals("default")) {
            org.unicode.cldr.test.CoverageLevel.Level itsLevel = 
            StandardCodes.make().getLocaleCoverageLevel(WebContext.getEffectiveLocaleType(options.get("CheckCoverage.requiredLocaleType")), locale.toString()) ;
            workingCoverageLevel = itsLevel.toString();
        }
        int workingCoverageValue = SupplementalDataInfo.CoverageLevelInfo.strToCoverageValue(workingCoverageLevel);
        // what to exclude under 'misc'
        
        CLDRFile vettedParent = null;
        CLDRLocale parentLoc = locale.getParent();
        if(parentLoc != null) {
            XMLSource vettedParentSource = sm.makeDBSource(parentLoc, true /*finalData*/);
            vettedParent = new CLDRFile(vettedParentSource,true);
        }
            
        int pn;
        boolean excludeCurrencies = false;
        boolean excludeCalendars = false;
        boolean excludeGrego = false;
        boolean excludeTimeZones = false;
        boolean excludeMetaZones = false;
        boolean useShorten = false; // 'shorten' xpaths instead of extracting type
        boolean keyTypeSwap = false;
        boolean excludeMost = false;
        boolean doExcludeAlways = true;
        boolean isReferences = false;
        String removePrefix = null;
        String continent = null;
        if(xpathPrefix.equals("//ldml")) {
            excludeMost = true;
            useShorten = true;
            removePrefix="//ldml/";
        }else if(xpathPrefix.startsWith("//ldml/units")) {
            canName=false;
            excludeMost=false;
            doExcludeAlways=false;
            removePrefix = "//ldml/units/";
            useShorten=true;
        }else if(xpathPrefix.startsWith("//ldml/numbers")) {
            if(-1 == xpathPrefix.indexOf("currencies")) {
                doExcludeAlways=false;
                excludeCurrencies=true; // = "//ldml/numbers/currencies";
                removePrefix = "//ldml/numbers/";
                canName = false;  // sort numbers by code
                useShorten = true;
            } else {
                removePrefix = "//ldml/numbers/currencies/currency";
                useShorten = true;
                canName = false; // because symbols are included
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
                int continentStart = xpathPrefix.indexOf("_");
                if(continentStart>0) {
		    continent = xpathPrefix.substring(xpathPrefix.indexOf("_")+1);
                }
		if(false) System.err.println(xpathPrefix+": -> continent " + continent);
                workPrefix = "//ldml/dates/timeZoneNames/metazone";
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
                    DataRow myp = getDataRow(FAKE_FLEX_THING);
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
            keyTypeSwap = true; //these come in reverse order  (type/key) i.e. buddhist/calendar, pinyin/collation.  Reverse this for sorting...
        } else if(xpathPrefix.equals("//ldml/references")) {
            isReferences = true;
            canName = false; // disable 'view by name'  for references
        }
        List checkCldrResult = new ArrayList();
        
        // iterate over everything in this prefix ..
        Set<String> baseXpaths = new HashSet<String>();
        for(Iterator<String> it = aFile.iterator(workPrefix);it.hasNext();) {
            String xpath = (String)it.next();
            
            baseXpaths.add(xpath);
        }
        Set<String> allXpaths = new HashSet<String>();
        Set<String> extraXpaths = new HashSet<String>();

        allXpaths.addAll(baseXpaths);
        aFile.getExtraPaths(workPrefix,extraXpaths);
        extraXpaths.removeAll(baseXpaths);
        allXpaths.addAll(extraXpaths);

//        // Process extra paths.
//        System.err.println("@@X@ base: " + baseXpaths.size() + ", extra: " + extraXpaths.size());
//        addExtraPaths(aFile, src, checkCldr, baselineFile, options, extraXpaths);
        
        for(String xpath : allXpaths) {
            boolean confirmOnly = false;
            String isToggleFor= null;
            if(xpath.equals(null)) {
            	throw new InternalError("null xpath in allXpaths");
            }
            if(!xpath.startsWith(workPrefix)) {
                if(false && SurveyMain.isUnofficial) System.err.println("@@ BAD XPATH " + xpath);
                continue;
            } else if(aFile.isPathExcludedForSurvey(xpath)) {
                if(false && SurveyMain.isUnofficial) System.err.println("@@ excluded:" + xpath);
                continue;
            } else if(false) {
		        System.err.println("allPath: " + xpath);
	        }

            boolean isExtraPath = extraXpaths.contains(xpath); // 'extra' paths get shim treatment
///*srl*/  if(xpath.indexOf("Adak")!=-1)
///*srl*/   {ndebug=true;System.err.println("p] "+xpath + " - xtz = "+excludeTimeZones+"..");}
                
            
            if(SHOW_TIME) {
                count++;
				nextTime = System.currentTimeMillis();
				if((nextTime - lastTime) > 10000) {
					lastTime = nextTime;
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
            } else if( continent != null && !continent.equals(sm.getMetazoneContinent(xpath))) {
//		if(false) System.err.println("Wanted " + continent +" but got " + sm.getMetazoneContinent(xpath) +" for " + xpath);
                continue;
            } 
//            else if(false && continent != null) {
//		System.err.println("Got " + continent +" for " + xpath);
//	    }
            
            if(CheckCLDR.skipShowingInSurvey.matcher(xpath).matches()) {
//if(TRACE_TIME)                System.err.println("ns1 8 "+(System.currentTimeMillis()-nextTime) + " " + xpath);
//		if(false) System.err.println("CheckCLDR.skipShowingInSurvey match for "+xpath);
                continue;
            }
            // Filter out data that is higher than the desired coverage level
            int coverageValue = sdi.getCoverageValue(xpath,locale.toULocale());
            if ( coverageValue > workingCoverageValue ) {
                if ( coverageValue <= 100 ) {
                    // TODO: KEEP COUNT OF FILTERED ITEMS
                }
                continue;
            }

            String fullPath = aFile.getFullXPath(xpath);
            //int xpath_id = src.xpt.getByXpath(fullPath);
            int base_xpath = sm.xpt.xpathToBaseXpathId(xpath);
            String baseXpath = sm.xpt.getById(base_xpath);

            if(fullPath == null) { 
                if(isExtraPath) {
                    fullPath=xpath; // (this is normal for 'extra' paths)
                } else {
                    //System.err.println("Extrapath: " +isExtraPath + ", base:"+baseXpath);
                    
                    System.err.println("DP:P Error: fullPath of " + xpath + " for locale " + locale + " returned null.");
                    //continue;
                    fullPath = xpath;
                }
            }

            if(needFullPathPattern.matcher(xpath).matches()) {
                //  we are going to turn on shorten, in case a non-shortened xpath is added someday.
                useShorten = true;
            }           

            if(TRACE_TIME)    System.err.println("ns0  "+(System.currentTimeMillis()-nextTime));
            boolean mixedType = false;
            String type;
            String lastType = sm.xpt.typeFromPathToTinyXpath(baseXpath, xpp);  // last type in the list
            String displaySuffixXpath;
            String peaSuffixXpath = null; // if non null:  write to suffixXpath
            
            // these need to work on the base
            String fullSuffixXpath = baseXpath.substring(workPrefix.length(),baseXpath.length());
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
            
            if(TRACE_TIME)    System.err.println("n00  "+(System.currentTimeMillis()-nextTime));
            
            String value = isExtraPath?null:aFile.getStringValue(xpath);

//if(ndebug)     System.err.println("n01  "+(System.currentTimeMillis()-nextTime));

            if( xpath.indexOf("default[@type")!=-1 ) {
                peaSuffixXpath = displaySuffixXpath;
                int n = type.lastIndexOf('/');
                if(n==-1) {
                    type = "(default type)";
                } else {
                    type = type.substring(0,n); //   blahblah/default/foo   ->  blahblah/default   ('foo' is lastType and will show up as the value)
                }
//                if(isExtraPath && SurveyMain.isUnofficial) System.err.println("About to replace ["+value+"] value: " + xpath);
                value = lastType;
                confirmOnly = true; // can't acccept new data for this.
            }
            
            if(useShorten) {
                if((xpath.indexOf("/orientation")!=-1)||
                   (xpath.indexOf("/alias")!=-1)) {
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
                value = "(NOTHING)";  /* This is set to prevent crashes.. */
            }
            
            // determine 'alt' param
            String alt = sm.xpt.altFromPathToTinyXpath(xpath, xpp);

//    System.err.println("n03  "+(System.currentTimeMillis()-nextTime));
    
            /* FULL path processing (references.. alt proposed.. ) */
            xpp.clear();
            xpp.initialize(fullPath);
            String lelement = xpp.getElement(-1);
            String eAlt = xpp.findAttributeValue(lelement, LDMLConstants.ALT);
            String eRefs = xpp.findAttributeValue(lelement,  LDMLConstants.REFERENCES);
            String eDraft = xpp.findAttributeValue(lelement, LDMLConstants.DRAFT);
            if(TRACE_TIME) System.err.println("n04  "+(System.currentTimeMillis()-nextTime));
            
            String typeAndProposed[] = LDMLUtilities.parseAlt(alt);
            String altProposed = typeAndProposed[1];
            String altType = typeAndProposed[0];
            
            // Now we are ready to add the data
            
            // Load the 'data row' which represents one user visible row of options 
            // (may be nested in the case of alt types)
            DataRow p = getDataRow(type, altType);
            p.base_xpath = base_xpath;
            p.winningXpathId = sm.dbsrcfac.getWinningPathId(base_xpath, locale, false);

            DataRow superP = getDataRow(type);  // the 'parent' row (sans alt) - may be the same object
            
            peaSuffixXpath = fullSuffixXpath; // for now...
            
            if(peaSuffixXpath!=null) {
                p.xpathSuffix = peaSuffixXpath;
                superP.xpathSuffix = XPathTable.removeAltFromStub(peaSuffixXpath); // initialize parent row without alt
            }

            if(CheckCLDR.FORCE_ZOOMED_EDIT.matcher(xpath).matches()) {
                p.zoomOnly = superP.zoomOnly = true;
            }
            p.confirmOnly = superP.confirmOnly = confirmOnly;

            if(isExtraPath) {
                // Set up 'shim' tests, to display coverage
                p.setShimTests(base_xpath,this.sm.xpt.getById(base_xpath),checkCldr,options);
         //       System.err.println("Shimmed! "+xpath);
            } else if(!isReferences) {
                if(p.inheritedValue == null) {
                    p.updateInheritedValue(vettedParent);
                }
                if(superP.inheritedValue == null) {
                    superP.updateInheritedValue(vettedParent);
                }
            }
            
            // voting 
            // bitwise OR in the voting types. Needed for sorting.
            if(p.voteType == 0) {
                int vtypes[] = new int[1];
                vtypes[0]=0;
                /* res = */ sm.vet.queryResult(locale, base_xpath, vtypes);
                p.confirmStatus = sm.vet.queryResultStatus(locale, base_xpath);
                p.allVoteType |= vtypes[0];
                superP.allVoteType |= p.allVoteType;
                p.voteType = vtypes[0]; // no mask
            }
//            System.out.println("@@V "+type+"  v: " + value + " - base"+base_xpath+" , win: " + p.voteType);

//            // Is this a toggle pair with another item?
//            if(isToggleFor != null) {
//                if(superP.toggleWith == null) {
//                    superP.updateToggle(fullPath, isToggleFor);
//                }
//                if(p.toggleWith == null) {
//                    p.updateToggle(fullPath, isToggleFor);
//                }
//            }
            
            // Is it an attribute choice? (obsolete)
/*            if(attributeChoice != null) {
                p.attributeChoice = attributeChoice;
                p.valuesList = p.attributeChoice.valuesList;

                if(superP.attributeChoice == null) {
                    superP.attributeChoice = p.attributeChoice;
                    superP.valuesList = p.valuesList;
                }
            }*/
            
            // Some special cases.. a popup menu of values
            if(p.type.startsWith("layout/inText")) {
                p.valuesList = LAYOUT_INTEXT_VALUES;
                superP.valuesList = p.valuesList;
            } else if(p.type.startsWith("defaultNumberingSystem")) { 
                // Not all available numbering systems are good candidates for default numbering system.
                // Things like "roman" shouldn't really be an option.  So, in the interest of simplicity,
                // we are hard-coding the choices here.
                String [] values = { "latn", "arab", "arabext", "armn", "beng", "deva", "ethi", "geor", "gujr", "guru", "hans", "hant", "hebr", "jpan", "khmr", "knda", "laoo", "mlym", "mong", "orya", "taml", "telu", "thai", "tibt" };
                p.valuesList = values;
                superP.valuesList = p.valuesList;
            } else if(p.type.indexOf("commonlyUsed")!=-1) { 
                p.valuesList = METAZONE_COMMONLYUSED_VALUES;
                superP.valuesList = p.valuesList;
            } else if(p.type.startsWith("layout/inList")) {
                p.valuesList = LAYOUT_INLIST_VALUES;
                superP.valuesList = p.valuesList;
            }
            

            if(TRACE_TIME) System.err.println("n05  "+(System.currentTimeMillis()-nextTime));

            // make sure the superP has its display name
            if(isReferences) {
                String eUri = xpp.findAttributeValue(lelement,"uri");
               if((eUri!=null)&&eUri.length()>0) {
                   if(eUri.startsWith("isbn:")) {
                        // linkbaton doesn't have ads, and lets you choose which provider to go to (including LOC).  
                        // could also go to wikipedia's  ISBN special page.              
						p.uri = "http://my.linkbaton.com/isbn/"+
                            eUri.substring(5,eUri.length());
                        p.displayName = eUri;
                    } else {
						p.uri = eUri;
						p.displayName = eUri.replaceAll("(/|&)","\u200b$0");  //  put zwsp before "/" or "&"
                        //p.displayName = /*type + " - "+*/ "<a href='"+eUri+"'>"+eUri+"</a>";
                    }
                } else {
                    p.displayName = null;
                }
                if(superP.displayName == null) {
                    superP.displayName = p.displayName;
                }
            } else {
                if(superP.displayName == null) {
                    superP.displayName = baselineFile.getStringValue(xpath(superP)); 
                }
                if(p.displayName == null) {
                    p.displayName = baselineFile.getStringValue(baseXpath);
                }
            }
    
            if((superP.displayName == null) ||
                (p.displayName == null)) {
                canName = false; // disable 'view by name' if not all have names.
            }
            if(TRACE_TIME) System.err.println("n06  "+(System.currentTimeMillis()-nextTime));
            
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
            if(TRACE_TIME) System.err.println("n06a  "+(System.currentTimeMillis()-nextTime));

            CLDRFile.Status sourceLocaleStatus = new CLDRFile.Status();
            if(TRACE_TIME) System.err.println("n06b  "+(System.currentTimeMillis()-nextTime));
            String sourceLocale = aFile.getSourceLocaleID(xpath, sourceLocaleStatus);
            if(TRACE_TIME) System.err.println("n06c  "+(System.currentTimeMillis()-nextTime));
            boolean isInherited = !(sourceLocale.equals(locale.toString()));
            if(TRACE_TIME) System.err.println("n06d  "+(System.currentTimeMillis()-nextTime));

            // with xpath munging, attributeChoice items show up as code fallback. Correct it.
/*            if(attributeChoice!=null && isInherited) {
                if(sourceLocale.equals(XMLSource.CODE_FALLBACK_ID)) {
                    isInherited = false;
                    sourceLocale = locale;
                }
            }*/
            // ** IF it is inherited, do NOT add any Items.   
            if(isInherited) {
                if(TRACE_TIME) System.err.println("n06da  [src:"+sourceLocale+" vs " + locale+ ", sttus:"+sourceLocaleStatus+"] "+(System.currentTimeMillis()-nextTime));
                if(!isReferences) {
                    if(TRACE_TIME) System.err.println("n06db  "+(System.currentTimeMillis()-nextTime));
                    p.updateInheritedValue(vettedParent, checkCldr, options); // update the tests
                    if(TRACE_TIME) System.err.println("n06dc  "+(System.currentTimeMillis()-nextTime));
                }
                continue;
            }
            if(TRACE_TIME) System.err.println("n06e  "+(System.currentTimeMillis()-nextTime));
            
            
            if(TRACE_TIME) System.err.println("n07  "+(System.currentTimeMillis()-nextTime));
    
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
                    if(altProposed != SurveyMain.PROPOSED_DRAFT) {  // 'draft=true'
                        p.hasMultipleProposals = true; 
                    }
                    superP.hasProps = true;
                } else {
                    // inherited, proposed
                   // p.hasProps = true; // Don't mark as a proposal.
                   // superP.hasProps = true;
                   p.hasInherited=true;
                   superP.hasInherited=true;
                }
            }
            
            
            String setInheritFrom = (isInherited)?sourceLocale:null; // no inherit if it's current.
//            boolean isCodeFallback = (setInheritFrom!=null)&&
//                (setInheritFrom.equals(XMLSource.CODE_FALLBACK_ID)); // don't flag errors from code fallback.
            
            if(isExtraPath) { // No real data items if it's an extra path.
              //  System.err.println("ExtraPath: "+xpath);
                continue; 
            }
            
            // ***** Set up Candidate Items *****
            // These are the items users may choose between
            //
            if((checkCldr != null)/*&&(altProposed == null)*/) {
                checkCldr.check(xpath, fullPath, isExtraPath?null:value, options, checkCldrResult);
                checkCldr.getExamples(xpath, fullPath, isExtraPath?null:value, options, examplesResult);
            }
            DataSection.DataRow.CandidateItem myItem = null;
            
/*            if(p.attributeChoice != null) {
                String newValue = p.attributeChoice.valueOfXpath(fullPath);
//       System.err.println("ac:"+fullPath+" -> " + newValue);
                value = newValue;
            }*/
            if(TRACE_TIME) System.err.println("n08  "+(System.currentTimeMillis()-nextTime));
            myItem = p.addItem( value, altProposed, null);
//if("gsw".equals(type)) System.err.println(myItem + " - # " + p.items.size());
            
            myItem.xpath = xpath;
            myItem.xpathId = sm.xpt.getByXpath(xpath);
            if(myItem.xpathId == base_xpath) {
                p.previousItem = myItem; // did have a previous item.
            }

            if(!checkCldrResult.isEmpty()) {
                myItem.setTests(checkCldrResult);
                // set the parent
                checkCldrResult = new ArrayList(); // can't reuse it if nonempty
            }
            /*
                Was this item submitted via SurveyTool? Let's find out.
            */
            myItem.submitter = sm.dbsrcfac.getSubmitterId(locale, myItem.xpathId);
            if(myItem.submitter != -1) {
///*srl*/                System.err.println("submitter set: " + myItem.submitter + " @ " + locale + ":"+ xpath);
            }

            if(sourceLocaleStatus!=null && sourceLocaleStatus.pathWhereFound!=null && !sourceLocaleStatus.pathWhereFound.equals(xpath)) {
//System.err.println("PWF diff: " + xpath + " vs " + sourceLocaleStatus.pathWhereFound);
                myItem.pathWhereFound = sourceLocaleStatus.pathWhereFound;
                // set up Pod alias-ness
                p.aliasFromLocale = sourceLocale;
                p.aliasFromXpath = sm.xpt.xpathToBaseXpathId(sourceLocaleStatus.pathWhereFound);
            }
            myItem.inheritFrom = setInheritFrom;
            // store who voted for what. [ this could be loaded at displaytime..]
           //myItem.votes = sm.vet.gatherVotes(locale, xpath);
            
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

//    /**
//     * Create a 'shim' row for each of the named paths
//     * @param extraXpaths
//     */
//    private void addExtraPaths(CLDRFile aFile, CLDRDBSource src, CheckCLDR checkCldr, CLDRFile baselineFile, Map options, Set<String> extraXpaths) {
//        
//        for(String xpath : extraXpaths) {
//            DataSection.DataRow myp = getDataRow(xpath);
//            int base_xpath = sm.xpt.getByXpath(xpath);
//            myp.base_xpath = base_xpath;
//            
//            if(myp.xpathSuffix == null) {
//                myp.xpathSuffix = xpath.substring(xpathPrefix.length());
//                
//                // set up the pea
//                if(CheckCLDR.FORCE_ZOOMED_EDIT.matcher(xpath).matches()) {
//                    myp.zoomOnly = true;
//                }
//
//                // set up tests
//                System.err.println("@@@shimmy - " + xpath);
//                myp.setShimTests(base_xpath,xpath,checkCldr,options);
//            }
//        }
//    }
    /**
     * Makes sure this pod contains the peas we'd like to see.
     */
    private void ensureComplete(XMLSource ourSrc, CheckCLDR checkCldr, CLDRFile baselineFile, Map<String,String> options) {
        SupplementalDataInfo sdi = sm.getSupplementalDataInfo();
        String workingCoverageLevel = options.get("CheckCoverage.requiredLevel");
        if ( workingCoverageLevel.equals("default")) {
            org.unicode.cldr.test.CoverageLevel.Level itsLevel = 
            StandardCodes.make().getLocaleCoverageLevel(WebContext.getEffectiveLocaleType(options.get("CheckCoverage.requiredLocaleType")), locale.toString()) ;
            workingCoverageLevel = itsLevel.toString();
        }
        int workingCoverageValue = SupplementalDataInfo.CoverageLevelInfo.strToCoverageValue(workingCoverageLevel);
        if(xpathPrefix.startsWith("//ldml/"+"dates/timeZoneNames")) {
            // work on zones
            boolean isMetazones = xpathPrefix.startsWith("//ldml/"+"dates/timeZoneNames/metazone");
            // Make sure the pod contains the peas we'd like to see.
            // regular zone
            
            Iterator zoneIterator;
            
            if(isMetazones) {
                if ( xpathPrefix.indexOf('_') > 0 ) {
                    String [] pieces = xpathPrefix.split("_",2);
                    xpathPrefix = pieces[0];
                    zoneIterator = sm.getMetazones(pieces[1]).iterator();
                } else {
                    zoneIterator = sm.getMetazones().iterator();
                }
            } else {
                zoneIterator = StandardCodes.make().getGoodAvailableCodes("tzid").iterator();
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
                                "/commonlyUsed"
            };
            
            String suffs[];
            if(isMetazones) {
                suffs = mzsuffs;
            } else {
                suffs = tzsuffs;
            }        

            String podBase = xpathPrefix;
            CLDRFile resolvedFile = new CLDRFile(ourSrc, true);
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
                    String rowXpath = zone+suff;
                    String base_xpath_string = podBase+ourSuffix+suff;
                    if(resolvedFile.isPathExcludedForSurvey(base_xpath_string)) {
//                       if(SurveyMain.isUnofficial) System.err.println("@@ synthesized+excluded:" + base_xpath_string);
                       continue;
                    }

                    // Only display metazone data for which an English value exists
                    if (isMetazones && suff != "/commonlyUsed") {
                        String engValue = baselineFile.getStringValue(base_xpath_string);
                        if ( engValue == null || engValue.length() == 0 ) {
                            continue;
                        }
                    }
                    // Filter out data that is higher than the desired coverage level
                    int coverageValue = sdi.getCoverageValue(base_xpath_string,locale.toULocale());
                    if ( coverageValue > workingCoverageValue ) {
                        if ( coverageValue <= 100 ) {
                            // KEEP COUNT OF FILTERED ITEMS
                        }
                        continue;
                    }
                    
                    DataSection.DataRow myp = getDataRow(rowXpath);
                    
                    // set it up..
                    int base_xpath = sm.xpt.getByXpath(base_xpath_string);
                    myp.base_xpath = base_xpath;
                    
                    if(myp.xpathSuffix == null) {
                        myp.xpathSuffix = ourSuffix+suff;
                        
                        // set up the pea
                        if(CheckCLDR.FORCE_ZOOMED_EDIT.matcher(base_xpath_string).matches()) {
                            myp.zoomOnly = true;
                        }

                        // set up tests
                        myp.setShimTests(base_xpath,base_xpath_string,checkCldr,options);
                    }
                    
                    ///*srl*/            System.err.println("P: ["+zone+suff+"] - count: " + myp.items.size());
                    if(isMetazones) {
                        if(suff.equals("/commonlyUsed")) {
                            myp.valuesList = METAZONE_COMMONLYUSED_VALUES;
                        }
                    }
                    
                    myp.displayName = baselineFile.getStringValue(podBase+ourSuffix+suff); // use the baseline (English) data for display name.
                    
                }
            }
        } // tz
    }
// ==

    public DataRow getDataRow(String type) {
        if(type == null) {
            throw new InternalError("type is null");
        }
        if(rowsHash == null) {
            throw new InternalError("peasHash is null");
        }
        DataRow p = (DataRow)rowsHash.get(type);
        if(p == null) {
            p = new DataRow();
            p.type = type;
            addDataRow(p);
        }
        return p;
    }
    
    private DataRow getDataRow(String type, String altType) {
        if(altType == null) {
            return getDataRow(type);
        } else {
            DataRow superDataRow = getDataRow(type);
            return superDataRow.getSubDataRow(altType);
        }
    }
    
    /**
     * Linear search for matching item.
     * @param xpath
     * @return the matching DatRow
     */
    public DataRow getDataRow(int xpath) {
    	// TODO: replace with better sort.
    	for(DataRow dr : rowsHash.values()) {
    		if(dr.base_xpath == xpath) {
    			return dr;
    		}
    		// search subrows
    		if(dr.subRows!=null) {
    			for(DataRow subRow : dr.subRows.values()) {
    				if(subRow.base_xpath == xpath) {
    					return subRow;
    				}
    			}
    		}
    	}
    	// look for sub-row
    	
    	return null;
    }
    
    void addDataRow(DataRow p) {
        rowsHash.put(p.type, p);
    }
    
    public String toString() {
        return "{DataPod " + locale + ":" + xpathPrefix + " #" + super.toString() + ", " + getAll().size() +" items} ";
    }
    
    /** 
     * Given a (cleaned, etc) xpath, this returns the podBase, i.e. context.getPod(base), that would be used to show
     * that xpath.  
     * Keep this in sync with SurveyMain.showLocale() where there is the list of menu items.
     */
    public static String xpathToSectionBase(String xpath) {
        int n;
        String base;
        
        // is it one of the prefixes we can check statically?
        String staticBases[] = { 
            // LOCALEDISPLAYNAMES
                "//ldml/"+PathUtilities.NUMBERSCURRENCIES,
                "//ldml/"+"dates/timeZoneNames/zone",
                "//ldml/"+"dates/timeZoneNames/metazone",
            // OTHERROOTS
                SurveyMain.GREGO_XPATH,
                PathUtilities.LOCALEDISPLAYPATTERN_XPATH,
                PathUtilities.OTHER_CALENDARS_XPATH,
                "//ldml/units"
        };
         
        // is it one of the static bases?
        for(n=0;n<staticBases.length;n++) {
            if(xpath.startsWith(staticBases[n])) {
                return staticBases[n];
            }
        }
            
        // dynamic LOCALEDISPLAYNAMES
        for(n =0 ; n < PathUtilities.LOCALEDISPLAYNAMES_ITEMS.length; n++) {   // is it a simple code list?
            base = PathUtilities.LOCALEDISPLAYNAMES+PathUtilities.LOCALEDISPLAYNAMES_ITEMS[n]+
                '/'+SurveyMain.typeToSubtype(PathUtilities.LOCALEDISPLAYNAMES_ITEMS[n]);  // see: SurveyMain.showLocaleCodeList()
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
    /**
     * 'shim' 
     * @param ourSrc
     * @param cf
     * @param locale
     * @param fullPathMinusAlt
     * @param altType
     * @param altPrefix
     * @param id
     * @deprecated SHIM- please rewrite caller to use CLDRFile api
     * @return the altProposed string for the new slot, or null on failure
     */
    public static String addDataToNextSlot(XMLSource ourSrc, CLDRFile cf, CLDRLocale locale,
            String fullPathMinusAlt, String altType, String altPrefix, int id, String value,
            String refs) {

        XPathParts xpp = new XPathParts(null,null);
        // prepare for slot check
        for(int slot=1;slot<100;slot++) {
            String altProposed = null;
            String alt = null;
            String altTag = null;
            if(altPrefix != null) {
                altProposed = altPrefix+slot; // proposed-u123-4 
                alt = LDMLUtilities.formatAlt(altType, altProposed);
                altTag =  "[@alt=\"" + alt + "\"]";
            } else {
                // no alt
                altTag = "";
            }
            //            String rawXpath = fullXpathMinusAlt + "[@alt='" + alt + "']";
            String refStr = "";
            if(refs.length()!=0) {
                refStr = "[@references=\""+refs+"\"]";
            }
            String rawXpath = fullPathMinusAlt + altTag + refStr; // refstr will get removed
            System.err.println("addDataToNextSlot:  rawXpath = " + rawXpath);
           // String xpath = CLDRFile.getDistinguishingXPath(rawXpath, null, false);  // removes  @used=  etc
           // if(!xpath.equals(rawXpath)) {
           //     logger.info("NORMALIZED:  was " + rawXpath + " now " + xpath);
           // }
            String xpath = fullPathMinusAlt + altTag;
            String oxpath = xpath+refStr+"[@draft=\"unconfirmed\"]";
            //int xpid = xpt.getByXpath(xpath);
            boolean has = ourSrc.hasValueAtDPath(xpath);
            System.err.println("Add: survey says " + has + " for " + xpath);
            if(has) {
                    continue;
            }
//            // Check to see if this slot is in use.
//            //synchronized(conn) {
//                try {
//                    stmts.queryValue.setString(1,locale.toString());
//                    stmts.queryValue.setInt(2,xpid);
//                    ResultSet rs = stmts.queryValue.executeQuery();
//                    if(rs.next()) {
//                        // already taken..
//                        //logger.info("Taken: " + altProposed);
//                        rs.close();
//                        continue;
//                    }
//                    rs.close();
//                } catch(SQLException se) {
//                    String complaint = "CLDRDBSource: Couldn't search for empty slot " + locale + ":" + xpid + "(" + xpath + ")='" + value + "' -- " + SurveyMain.unchainSqlException(se);
//                    logger.severe(complaint);
//                    throw new InternalError(complaint);
//                }
//            //}
            
            
            
            //int oxpid = xpt.getByXpath(oxpath);
            xpp.clear();
            xpp.initialize(oxpath);
            String lelement = xpp.getElement(-1);
            /* all of these are always at the end */
            String eAlt = xpp.findAttributeValue(lelement,LDMLConstants.ALT);
            String eDraft = xpp.findAttributeValue(lelement,LDMLConstants.DRAFT);
            
            /* special func to find this */
           // String eType = sm.xpt.typeFromPathToTinyXpath(xpath, xpp);
            String tinyXpath = xpp.toString();
            
           // int txpid = xpt.getByXpath(tinyXpath);
            
            /*SRL*/ 
            //                System.out.println(xpath + " l: " + locale);
            //                System.out.println(" <- " + oxpath);
            //                System.out.println(" t=" + eType + ", a=" + eAlt + ", d=" + eDraft);
            //                System.out.println(" => "+txpid+"#" + tinyXpath);
            ourSrc.putValueAtPath(oxpath, value);
            return altProposed;
        } // end for
        return null; // couldn't find a slot..
    
    }
    /**
     * 'shim' 
     * @param ourSrc
     * @param cf
     * @param string
     * @param id
     * @param newRef
     * @param uri
     * @return the altProposed string for the new slot, or null on failure
     * @deprecated SHIM- please rewrite caller
     */
    public static String addReferenceToNextSlot(XMLSource ourSrc, CLDRFile cf, String string,
            int id, String newRef, String uri) {
        System.err.println("@@unimp: addReferenceToNextSlot");
        return UserRegistry.makePassword(uri.toString()); // throw it a bone
        // TODO Auto-generated method stub
   //     SurveyMain.busted("unimplemented: addReferenceToNextSlot");
//        throw new InternalError("unimplemented: addReferenceToNextSlot");
//        return null;
    }
}
