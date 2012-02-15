//
//  DataSection.java
//
//  Created by Steven R. Loomis on 18/11/2005.
//  Copyright 2005-2011 IBM. All rights reserved.
//

//  TODO: this class now has lots of knowledge about specific data types.. so does SurveyMain
//  Probably, it should be concentrated in one side or another- perhaps SurveyMain should call this
//  class to get a list of displayable items?

package org.unicode.cldr.web;
import java.sql.SQLException;
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
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.test.ExampleGenerator.ExampleContext;
import org.unicode.cldr.test.ExampleGenerator.ExampleType;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.LDMLUtilities;
import org.unicode.cldr.util.PathUtilities;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StringId;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.web.CLDRDBSourceFactory.DBEntry;
import org.unicode.cldr.web.DataSection.DataRow;
import org.unicode.cldr.web.DataSection.DataRow.CandidateItem;
import org.unicode.cldr.web.DataSection.ExampleEntry;
import org.unicode.cldr.web.SurveyMain.UserLocaleStuff;
import org.unicode.cldr.web.UserRegistry.User;
import org.unicode.cldr.web.Vetting.Status;
import org.unicode.cldr.web.WebContext.HTMLDirection;

import com.ibm.icu.text.Collator;
import com.ibm.icu.util.ULocale;

/** A data section represents a group of related data that will be displayed to users in a list
 * such as, "all of the language display names contained in the en_US locale".
 * It is sortable, as well, and has some level of persistence.
 * This class was formerly, and unfortunately, named DataPod
 **/

public class DataSection extends Registerable {
    private static final boolean DEBUG = false || CldrUtility.getProperty("TEST", false);

    /**
     * Trace in detail time taken to populate?
     */
    private static final boolean TRACE_TIME=false;
    
    
    private static final boolean NOINHERIT = true;
    /**
     * Show time taken to populate?
     */
    private static final boolean SHOW_TIME= false || TRACE_TIME ||  DEBUG || CldrUtility.getProperty("TEST", false);

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
    boolean isCalendar = false; // Is this a calendar section?
    boolean isMetazones = false; // Is this a metazones section?
    int skippedDueToCoverage = 0; // How many were skipped due to coverage?
//    boolean simple = false; // is it a 'simple code list'?
    
    public static final String DATASECTION_MISSING = "Inherited";
    public static final String DATASECTION_NORMAL = "Normal";
    public static final String DATASECTION_PRIORITY = "Priority";
    public static final String DATASECTION_PROPOSED = "Proposed";
    public static final String DATASECTION_VETPROB = "Vetting Issue";

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

    private String ptype; 
    DataSection(SurveyMain sm, CLDRLocale loc, String prefix, String ptype) {
        super(sm.lcr,loc); // initialize call to LCR

        this.sm = sm;
        this.ptype = ptype;
        xpathPrefix = prefix;
        fieldHash =  CookieSession.cheapEncode(sm.xpt.getByXpath(prefix));
        intgroup = loc.getLanguage(); // calculate interest group
    	ballotBox = sm.getSTFactory().ballotBoxForLocale(locale);
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
    	return p.getXpath();
    }
        
    
    final Collator myCollator = CodeSortMode.createCollator();
    
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
        
        public String prettyPath = null;
        public String uri = null; // URI for the type
        
        private String displayName = null;
        public String altType = null; // alt type (NOT to be confused with -proposedn)
        int xpathId = -1;

        private String xpath;
        private String winningValue;

        public DataRow(String xpath) {
            this.xpath = xpath;
            this.xpathId = sm.xpt.getByXpath(xpath);
            this.prettyPath = sm.xpt.getPrettyPath(xpathId);
            //this.setDisplayName(prettyPath);
            
            if(ballotBox==null) {
            	throw new InternalError("ballotBox is null;");
            }
            if(ballotBox.getResolver(xpath)==null) {
            	throw new InternalError(ballotBox.getClass().getName()+" [ballotBox].getResolver("+xpath+") is null");
            }
		    winningValue = ballotBox.getResolver(xpath).getWinningValue();
        }
        public String getXpath() {
            return xpath;
        }
        public int getXpathId() {
            return xpathId;
        }
        private String pp = null;
        public String getPrettyPath() {
            if(pp==null) {
                pp=sm.xpt.getPrettyPath(xpathId);
            }
            return pp;
        }
        
        // true even if only the non-winning subitems have tests.
        boolean hasTests = false;

        // the xpath id of the winner. If no winner or n/a, -1. 
        /**
         * @deprecated - winner is a value
         */
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
        public int reservedForSort[] = SortMode.reserveForSort(); // ordering for use in collator.
        
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
            public static final String altProposed = "n/a"; // proposed part of the name (or NULL for nondraft)
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
                    	votes = ballotBox.getVotesForValue(xpath, value);
                    }
                    checkedVotes = true;
                }
                return votes;
            }
            
            public String example = "";
            
            public String toString() { 
                return "{Item v='"+value+"', altProposed='"+altProposed+"', inheritFrom='"+inheritFrom+"'"
                    +(isWinner()?",winner":"")
                    +"}";
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
                    	if(DEBUG) System.err.println("err: " + status.getMessage() + ", test: " + status.getClass() + ", cause: " + status.getCause() + " on " + this.xpath);
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
                                        
                    if(((winningXpathId==-1)&&(xpathId==xpathId)) || (xpathId == winningXpathId)) {
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

			/**
			 *  print the cells which have to do with the item
			 * this may be called with NULL if there isn't a proposed item for this slot.
			 * it will be called once in the 'main' row, and once for any extra rows needed for proposed items
			 * @param ctx TODO
			 * @param fieldHash TODO
			 * @param resultXpath TODO
			 * @param ourVote TODO
			 * @param canModify TODO
			 * @param ourAlign TODO
			 * @param uf TODO
			 * @param zoomedIn TODO
			 * @param numberedItemsList All items are added here.
			 * @param exampleContext TODO
			 */ 
			void printCells(WebContext ctx, String fieldHash, String resultXpath, String ourVote, boolean canModify, String ourAlign, UserLocaleStuff uf, boolean zoomedIn, List<CandidateItem> numberedItemsList, ExampleContext exampleContext) {
			    // ##6.1 proposed - print the TOP item
			    
			    int colspan = 1;
			    String itemExample = null;
			    boolean haveTests = false;
			    
			    if(this != null) {
			        itemExample = uf.getExampleGenerator().getExampleHtml(xpath, value,
			                    zoomedIn?ExampleGenerator.Zoomed.IN:ExampleGenerator.Zoomed.OUT, exampleContext, ExampleType.NATIVE);
			        if((tests != null) || (examples != null)) {
			            haveTests = true;
			        }
			    } else {
			        itemExample = uf.getExampleGenerator().getExampleHtml(getXpath(), null,
			                zoomedIn?ExampleGenerator.Zoomed.IN:ExampleGenerator.Zoomed.OUT, exampleContext, ExampleType.NATIVE);
			    }
			    
			    ctx.print("<td  colspan='"+colspan+"' class='propcolumn' align='"+ourAlign+"' dir='"+ctx.getDirectionForLocale()+"' valign='top'>");
			    if((this != null)&&(value != null)) {
			        if(false)ctx.println("<div style='border: 2px dashed red'>altProposed="+altProposed+", inheritFrom="+inheritFrom+", pathWhereFound="+pathWhereFound+", confirmOnly="+new Boolean(confirmOnly)+"</div><br>");
					    boolean winner = 
					        ((resultXpath!=null)&&
					        (xpath!=null)&&
					        (xpath.equals(resultXpath))&& // todo: replace string.equals with comparison to item.xpathId . .
					        !isFallback);
					    boolean fallback = false;
					
					    String pClass ="";
					    if(winner) {
					        if(confirmStatus == Vetting.Status.APPROVED) {
					            pClass = "class='winner' title='Winning item.'";
					        } else if(confirmStatus != Vetting.Status.INDETERMINATE) {
					            pClass = "title='"+confirmStatus+"' ";
					        }
					    } else if(pathWhereFound != null) {
					        fallback = true;
					        pClass = "class='alias' title='alias from "+sm.xpt.getPrettyPath(pathWhereFound)+"'";
					    } else if (isFallback || (inheritFrom != null) /*&&(p.inheritFrom==null)*/) {
					        fallback = true; // this item is nver 'in' this localel
					        if(XMLSource.CODE_FALLBACK_ID.equals(inheritFrom)) {
					            pClass = "class='fallback_code' title='Untranslated Code'";
					        } else if("root".equals(inheritFrom)) {
					            pClass = "class='fallback_root' title='Fallback from Root'";
					        } else {
					            pClass = "class='fallback' title='Translated in "+new ULocale(inheritFrom).getDisplayName(ctx.displayLocale)+" and inherited here.'";
					        }
					    } else if(altProposed != null) {
					        pClass = "class='loser' title='proposed, losing item'";
					/*    } else if(p.inheritFrom != null) {
					        pClass = "class='missing'"; */
					    } else {
					        pClass = "class='loser'";
					    }
					
					
					    if(true /* !item.itemErrors */) {  // exclude item from voting due to errors?
					        if(canModify) {      
					            boolean checkThis = 
					                ((ourVote!=null)&&
					                (xpath!=null)&&
					                (ourVote.equals(value)));
					            
					            if(!isFallback) {
					                ctx.print("<input title='#"+xpathId+"' name='"+fieldHash+"'  value='"+
					                    ((altProposed!=null)?altProposed:SurveyMain.CONFIRM)+"' "+(checkThis?"CHECKED":"")+"  type='radio'>");
					            } else {
					                ctx.print("<input title='#"+xpathId+"' name='"+fieldHash+"'  value='"+SurveyMain.INHERITED_VALUE+"' type='radio'>");
					            }
					        } else {
					            ctx.print("<input title='#"+xpathId+"' type='radio' disabled>");
					        }
					    }
					
					    if(zoomedIn && (getVotes() != null)) {
					        int n = getVotes().size();
					        String title=""+ n+" vote"+((n>1)?"s":"");
					        if(canModify&&UserRegistry.userIsVetter(ctx.session.user)) {
					            title=title+": ";
								boolean first = true;
					            for(User theU : getVotes()) {
					                if(theU != null) {
					                    String add= theU.name + " of " + theU.org;
					                    title = title + add.replaceAll("'","\u2032"); // quote quotes
					                    if(first) {
					                        title = title+", ";
					                    } else {
											first = false;
										}
					                }
					            }
					        }
					        ctx.print("<span class='notselected' >"+ctx.iconHtml("vote",title)+"</span>"); // ballot box symbol
					    }
					
					    ctx.print("<span "+pClass+">");
					    String processed = null;
					    if(value.length()!=0) {
					        processed = ctx.processor.processForDisplay(sm.xpt.getById(getXpathId()),value);
					        ctx.print(processed);
					    } else {
					        ctx.print("<i dir='ltr'>(empty)</i>");
					        processed = null;
					    }
					    ctx.print("</span>");
					    if((!fallback||((previousItem==null)&&isParentFallback)||  // it's either: not inherited OR not a "shim"  and..
					        (pathWhereFound != null && !isFallback )) &&    // .. or it's an alias (that is still the 1.4 item)
					        xpathId == getXpathId()) {   // its xpath is the base xpath.
					        ctx.print(ctx.iconHtml("star","CLDR "+SurveyMain.getOldVersion()+" item"));
					    } else if (SurveyMain.isUnofficial && isParentFallback) {
					//            ctx.print(ctx.iconHtml("okay","parent fallback"));
					    }
					    
					    if((ctx.session.user!=null)&& (zoomedIn || ctx.prefBool(SurveyMain.PREF_DELETEZOOMOUT))) {
					        boolean adminOrRelevantTc = UserRegistry.userIsAdmin(ctx.session.user); //  ctx.session.user.isAdminFor(reg.getInfo(item.submitter));
					        if( (getVotes() == null) ||  adminOrRelevantTc || // nobody voted for it, or
					            ((getVotes().size()==1)&& getVotes().contains(ctx.session.user) ))  { // .. only this user voted for it
					            boolean deleteHidden = ctx.prefBool(SurveyMain.PREF_NOSHOWDELETE);
					            if(!deleteHidden && canModify && (submitter != -1) &&
					                !( (pathWhereFound != null) || isFallback || (inheritFrom != null) /*&&(p.inheritFrom==null)*/) && // not an alias / fallback / etc
					                ( adminOrRelevantTc || (submitter == ctx.session.user.id) ) ) {
					                    ctx.println(" <label nowrap class='deletebox' style='padding: 4px;'>"+ "<input type='checkbox' title='#"+xpathId+
					                        "' value='"+altProposed+"' name='"+fieldHash+SurveyMain.ACTION_DEL+"'>" +"Delete&nbsp;item</label>");
					            }
					        }
					    }
					    if(UserRegistry.userIsTC(ctx.session.user) && ctx.prefBool(SurveyMain.PREF_SHOWUNVOTE) &&votesByMyOrg(ctx.session.user)) {
					        ctx.println(" <label nowrap class='unvotebox' style='padding: 4px;'>"+ "<input type='checkbox' title='#"+xpathId+
					                "' value='"+altProposed+"' name='"+fieldHash+SurveyMain.ACTION_UNVOTE+"'>" +"Unvote&nbsp;item</label>");
					    }
					    if(zoomedIn) {
					        if(processed != null && /* direction.equals("rtl")&& */ SurveyMain.CallOut.containsSome(processed)) {
					            String altProcessed = processed.replaceAll("\u200F","\u200F<b dir=rtl>RLM</b>\u200F")
					                                           .replaceAll("\u200E","\u200E<b>LRM</b>\u200E");
					            ctx.print("<br><span class=marks>" + altProcessed + "</span>");
					        }
					    }
			    }
			    ctx.println("</td>");    
			    // 6.3 - If we are zoomed in, we WILL have an additional column withtests and/or references.
			    if(zoomedIn) {
			        if(true || haveTests) {
			            if(true || tests != null) {
			                ctx.println("<td nowrap class='warncell'>");
			                ctx.println("<span class='warningReference'>");
			                //ctx.print(ctx.iconHtml("warn","Warning"));
			            } else {
			                ctx.println("<td nowrap class='examplecell'>");
			                ctx.println("<span class='warningReference'>");
			            }
			            if(this!=null /* always number -- was haveTests*/) {
			                numberedItemsList.add(this);
			                int mySuperscriptNumber = numberedItemsList.size();  // which # is this item?
			                ctx.println("#"+mySuperscriptNumber+"</span>");
			            }
			            ctx.println("</td>");
			        } else {
			            ctx.println("<td></td>"); // no tests, no references
			        }
			    }
			
			    // ##6.2 example column. always present
			    if (hasExamples) {
			        if(itemExample!=null) {
			            ctx.print("<td class='generatedexample' valign='top' align='"+ourAlign+"' dir='"+ctx.getDirectionForLocale()+"' >");
			            ctx.print(itemExample.replaceAll("\\\\","\u200b\\\\")); // \u200bu
			            ctx.println("</td>");
			        } else {
			            ctx.println("<td></td>");
			        }
			    }
			}
        }
        
        CandidateItem previousItem = null;
        CandidateItem inheritedValue = null; // vetted value inherited from parent
        
        public String toString() {
            return "{DataRow t='"+prettyPath+"', n='"+getDisplayName()+"', x='"+"', item#='"+items.size()+"'}";
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
                        if((p1.value==null)&&(p2.value==null)) return 0;
                        if(p1.value == null) return -1;
                        if(p2.value == null) return 1;
                        return myCollator.compare(p1.value, p2.value);
                    }
                });
        public CandidateItem addItem(String value, List tests) {
            CandidateItem pi = new CandidateItem();
            pi.value = value;
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
                ret = ret + ":x" + CookieSession.cheapEncode(getXpathId());
                myFieldHash = ret;
            }
            return myFieldHash;
        }
        
        public String fullFieldHash() {
        	return /*section.*/fieldHash + fieldHash();
        }
        
//        Hashtable<String,DataRow> subRows = null;

//        public DataRow getSubDataRow(String altType) {
//            if(altType == null) {
//                return this;
//            }
//            if(subRows == null) {
//                subRows = new Hashtable<String,DataRow>();
//            }
//
//            DataRow p = subRows.get(altType);
//            if(p==null) {
//                p = new DataRow();
//                p.type = type;
//                p.altType = altType;
//                p.parentRow = this;
//                subRows.put(altType, p);
//            }
//            return p;
//        }
        
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
            
            if(xpathId == -1) {
                return;
            }
            

            String xpath = sm.xpt.getById(xpathId);
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
                    inheritedValue.xpathId = xpathId;
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
          return NAME_TYPE_PATTERN.matcher(prettyPath).matches();
        }

		public CLDRLocale getLocale() {
			return locale;
		}

		public String getIntgroup() {
			return intgroup;
		}

		private String resultXpath = null;
		private int resultType[] = new int[1];
		private int resultXpath_id = -1;
		private boolean errorNoOutcome  = false;
		
		/**
		 * @deprecated
		 */
		public String getResultXpath() {
			if(resultXpath == null) {
				if(xpathId==-1) return getXpath(); /* pseudo element. no real path */
				int resultType[] = new int[1];
				resultXpath_id =  sm.vet.queryResult(locale, xpathId, resultType);
				if(resultXpath_id != -1) {
					resultXpath = sm.xpt.getById(resultXpath_id); 
				} else {
					// noWinner = true;
				}
				errorNoOutcome = (resultType[0]==Vetting.RES_ERROR)&&(resultXpath_id==-1); // error item - NO result.
			}
			return resultXpath;
		}
		
		/**
		 * @deprecated
		 */
		public int getResultType() {
			getResultXpath();
			return resultType[0];
		}
		
		/**
		 * @deprecated
		 */
		public int getResultXpathId() {
			getResultXpath();
			return resultXpath_id;
		}
		
		/**
		 * @deprecated
		 */
		public boolean getErrorNoOutcome() {
			getResultXpath();
			return errorNoOutcome;
		}
		
		public String getWinningValue() {
			return winningValue;
		}
		
		/**
		 * @see #getCurrentItem()
		 * @return
		 */
		CandidateItem getWinningItem() {
    		for(CandidateItem i : items) {
                if(i.isWinner()) {
                    return i;
                }
            }
    
            return  getCurrentItem();
		}		
		
		/**
		 * Get a list of current CandidateItems for this Row.
		 * The top item will be the winning value, if any. 
		 * @deprecated use getCurrentItem - there's only one winner allowed.
		 * @see #getCurrentItem()
		 */
        public synchronized List<CandidateItem> getCurrentItems() {
            getCurrentItem();
            return currentItems;
        }
        
        /**
         * Returns the winning (current) item.
         * @return
         */
        public CandidateItem getCurrentItem() {
            if (this.currentItems == null) {

                getResultXpath(); /* Make sure the resultXpath is loaded. */

                List<DataSection.DataRow.CandidateItem> currentItems = new ArrayList<DataSection.DataRow.CandidateItem>();
                List<DataSection.DataRow.CandidateItem> proposedItems = new ArrayList<DataSection.DataRow.CandidateItem>();
                for (CandidateItem item : items) {
//                	if(true || (sm.isUnofficial && DEBUG)) System.err.println("Considering: " + item+", xpid="+item.xpathId+", result="+resultXpath_id+", base="+this.xpathId+", ENO="+errorNoOutcome);
                	// item.toString()
                	if (item.value.equals(winningValue) && !errorNoOutcome) {
                		if(!currentItems.isEmpty()) {
                			throw new InternalError(this.toString()+": can only have one candidate item, not " + currentItems.get(0) +" and " + item);
                		}
                		currentItems.add(item);
                	} else {
                		proposedItems.add(item);
                	}
                }
                // if there is an inherited value available - see if we need to
                // show it.
                if ((inheritedValue != null) && (inheritedValue.value != null)) { // or an alias
                    if (currentItems.isEmpty()) { // no other current items..
                        currentItems.add(inheritedValue);
                    } else {
                        boolean found = false; /* Have we found this value in the inherited items? If so, don't show it. */
                        for (DataSection.DataRow.CandidateItem i : proposedItems) {
                            if (inheritedValue.value.equals(i.value)) {
                                found = true;
                            }
                        }
                        if (!found)
                            for (DataSection.DataRow.CandidateItem i : currentItems) {
                                if (inheritedValue.value.equals(i.value)) {
                                    found = true;
                                }
                            }
                        if (!found) {
                            proposedItems.add(inheritedValue);
                        }
                    }
                }
                this.currentItems = currentItems;
                this.proposedItems = proposedItems;
            }
            if(!this.currentItems.isEmpty())
                return this.currentItems.get(0);
            else    
                return null;
        }
		
		
		
		List<CandidateItem> currentItems = null, proposedItems=null;

		/**
		 * Calculated coverage level for this row.
		 */
        public int coverageValue;

		/**
		 * Get a list of proposed items, if any.
		 */
		public List<CandidateItem> getProposedItems() {
			if(currentItems==null) {
				getCurrentItems();
			}
			return proposedItems;
		}
        
		
		/**
		 * Return the CandidateItem for a particular user ID
		 * @param userId
		 * @return
		 */
		public CandidateItem getVotesForUser(int userId) {
		    UserRegistry.User infoForUser = sm.reg.getInfo(userId); /* see gatherVotes - getVotes() is populated with a set drawn from the getInfo() singletons. */
		    if(infoForUser==null) return null;
            for(CandidateItem item: getCurrentItems()) {
                Set<User> votes = item.getVotes();
                if(votes!=null && votes.contains(infoForUser)) {
                    return item;
                }
            }
		    for(CandidateItem item: getProposedItems()) {
		        Set<User> votes = item.getVotes();
		        if(votes!=null && votes.contains(infoForUser)) {
		            return item;
		        }
		    }
		    return null; /* not found. */
		}

		/**
		 * Returns true if a user has voted or not.
		 * @param userId
		 * @return true if user has voted at all, false otherwise. Will return false if user changes their vote back to no opinion.
		 */
		public boolean userHasVoted(int userId) {
		    return getVotesForUser(userId)!=null;
		}
        private void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
		/**
		 * Print empty cells. Not the equivalent of printCells(..., null), which will still print an example.
		 * @param ctx
		 * @param ourAlign TODO
		 * @param zoomedIn TODO
		 * @param section
		 */
		void printEmptyCells(WebContext ctx, String ourAlign, boolean zoomedIn) {
		    int colspan = zoomedIn?1:1;
		    ctx.print("<td  colspan='"+colspan+"' class='propcolumn' align='"+ourAlign+"' dir='"+ctx.getDirectionForLocale()+"' valign='top'>");
		    ctx.println("</td>");    
		    if(zoomedIn) {
		        ctx.println("<td></td>"); // no tests, no references
		    }
		    if (hasExamples) { 
		        // ##6.2 example column
		        ctx.println("<td></td>");
		    }
		}
		/**
		 * Show a single pea
		 * @param ctx TODO
		 * @param section TODO
		 * @param uf TODO
		 * @param cf TODO
		 * @param ourSrc TODO
		 * @param canModify TODO
		 * @param refs TODO
		 * @param checkCldr TODO
		 * @param zoomedIn TODO
		 */
		void showDataRow(WebContext ctx, DataSection xxx, UserLocaleStuff uf, CLDRFile cf, XMLSource ourSrc, boolean canModify, String[] refs, CheckCLDR checkCldr, boolean zoomedIn) {
			String specialUrl = getSpecialURL(ctx);
		    
		    String ourAlign = "left";
		    if(ctx.getDirectionForLocale().equals(HTMLDirection.RIGHT_TO_LEFT)) {
		        ourAlign = "right";
		    }
		    
		    
		    boolean canSubmit = UserRegistry.userCanSubmitAnyLocale(ctx.session.user)  // formally able to submit
		        || (canModify/*&&p.hasProps*/); // or, there's modified data.
		
		    boolean showedRemoveButton = false; 
		    String fullPathFull = getXpath();
		    String boxClass = canModify?"actionbox":"disabledbox";
		    boolean isAlias = (fullPathFull.indexOf("/alias")!=-1);
		    WebContext refCtx = (WebContext)ctx.clone();
		    refCtx.setQuery("_",ctx.getLocale().getLanguage());
		    refCtx.setQuery(SurveyMain.QUERY_SECTION,"references");
		    //            ctx.println("<tr><th colspan='3' align='left'><tt>" + p.type + "</tt></th></tr>");
		
		    String fieldHash = fieldHash();
		    
		    // voting stuff
		    boolean somethingChecked = false; // have we presented a 'vote' yet?
		    int base_xpath = sm.xpt.xpathToBaseXpathId(fullPathFull);
		    String ourVote = null;
		    if(ctx.session.user != null) {
		    	ourVote = getBallotBox().getVoteValue(ctx.session.user, fullPathFull);
		    }
		    
		    boolean inheritedValueHasTestForCurrent = false; // if (no current items) && (inheritedValue.hastests) 
		
		    
		    // let's see what's inside.
		    // TODO: move this into the DataPod itself?
		    List<CandidateItem> currentItems = getCurrentItems();
		    List<CandidateItem> proposedItems = getProposedItems();
		    
		    
		    // Does the inheritedValue contain a test that we need to display?
		    if(proposedItems.isEmpty() && inheritedValue!=null && inheritedValue.value==null && inheritedValue.tests!=null) {
		        inheritedValueHasTestForCurrent = true;
		    }
		    
		    // calculate the max height of the current row.
		    int rowSpan = Math.max(proposedItems.size(),currentItems.size()); // what is the rowSpan needed for general items?
		    rowSpan = Math.max(rowSpan,1);
		    
		    /*  TOP BAR */
		    // Mark the line as disputed or insufficient, depending.
		    String rclass = "vother";
		    boolean foundError = hasErrors;
		    boolean foundWarning = hasWarnings;
		    
		    List<CandidateItem> numberedItemsList = new ArrayList<CandidateItem>();
		
		    String draftIcon = "";
		    switch (confirmStatus) {
		    	case APPROVED : draftIcon = ctx.iconHtml("okay", "APPROVED"); break;
		    	case CONTRIBUTED : draftIcon = ctx.iconHtml("conf","CONTRIBUTED"); break;
		    	case PROVISIONAL : draftIcon = ctx.iconHtml("conf2", "PROVISIONAL"); break;
		    	case UNCONFIRMED : draftIcon = ctx.iconHtml("ques", "UNCONFIRMED") ; break;
		   }
		
		    // calculate the class of data items
		    String statusIcon="";
		
		    String votedIcon=ctx.iconHtml("bar0", "You have not yet voted on this item.");
		    if (userHasVoted(ctx.userId())) {
		    	votedIcon=ctx.iconHtml("vote", "You have already voted on this item.");
		    }
		    {
		        int s = getResultType();
		        
		        if(foundError) {
		            rclass = "error";
		            statusIcon = ctx.iconHtml("stop","Errors - please zoom in");            
		        } else if(foundWarning && confirmStatus != Vetting.Status.INDETERMINATE) {
		            rclass = "warning";
		            statusIcon = ctx.iconHtml("warn","Warnings - please zoom in");            
		        }
		    }
		    
		    ctx.println("<tr  id='r_"+fullFieldHash()+"'  class='topbar'>");
		  //  String baseInfo = "#"+base_xpath+", w["+Vetting.typeToStr(resultType[0])+"]:" + resultXpath_id;
		    
		     
		    ctx.print("<th rowspan='"+rowSpan+"' class='"+rclass+"' valign='top' width='30'>");
		    if(!zoomedIn) {
		        if(specialUrl != null) {
		            ctx.print("<a class='notselected' "+ctx.atarget()+" href='"+specialUrl+"'>"+statusIcon+"</a>");
		        } else {
		            sm.fora.showForumLink(ctx,this,parentRow.getXpathId(),statusIcon);
		        }
		    } else {
		        ctx.print(statusIcon);
		    }
		    ctx.println("</th>");
		    
		    // Code for 2nd column
		    ctx.print("<th rowspan='"+rowSpan+"' class='"+rclass+"' valign='top' width='30'>");
		    ctx.print(draftIcon);
		    ctx.println("</th>");
		    
		    // Code for 3rd column
		    if (canModify) {
		        ctx.print("<th rowspan='"+rowSpan+"' class='"+rclass+"' valign='top' width='30'>");
		        ctx.print(votedIcon);
		        ctx.println("</th>");
		    }
		    // ##2 code
		    ctx.print("<th rowspan='"+rowSpan+"' class='botgray' valign='top' align='left'>");
		    //if(p.displayName != null) { // have a display name - code gets its own box
		    int xfind = ctx.fieldInt(SurveyMain.QUERY_XFIND);
		    if(xfind==base_xpath) {
		        ctx.print("<a name='x"+xfind+"'>");
		    }
		    
		    sm.printItemTypeName(ctx, this, canModify, zoomedIn, specialUrl);
		
		    
		    if(altType != null) {
		        ctx.print("<br> ("+altType+" alternative)");
		    }
		    if(xfind==base_xpath) {
		        ctx.print("</a>"); // for the <a name..>
		    }
		    ctx.println("</th>");
		    
		    // ##3 display / Baseline
		    ExampleContext exampleContext = new ExampleContext();
		    
		    // calculate winner
		    // ##5 current control ---
		    CandidateItem topCurrent = null;
		    if(currentItems.size() > 0) {
		        topCurrent = currentItems.get(0);
		    }
		    if((topCurrent == null) && inheritedValueHasTestForCurrent) { // bring in the inheritedValue if it has a meaningful test..
		        topCurrent = inheritedValue;
		    }
		    
		    // Prime the Pump  - Native must be called first.
		    if(topCurrent != null) {
		        /* ignored */ uf.getExampleGenerator().getExampleHtml(topCurrent.xpath, topCurrent.value,
		                zoomedIn?ExampleGenerator.Zoomed.IN:ExampleGenerator.Zoomed.OUT, exampleContext, ExampleType.NATIVE);
		    } else {
		        // no top item, so use NULL
		        /* ignored */ uf.getExampleGenerator().getExampleHtml(getXpath(), null,
		                zoomedIn?ExampleGenerator.Zoomed.IN:ExampleGenerator.Zoomed.OUT, exampleContext, ExampleType.NATIVE);
		    }
		
		    String baseExample = sm.getBaselineExample().getExampleHtml(fullPathFull, getDisplayName(), zoomedIn?ExampleGenerator.Zoomed.IN:ExampleGenerator.Zoomed.OUT,
		            exampleContext, ExampleType.ENGLISH);
		    int baseCols = 1;
		
		    ctx.println("<th rowspan='"+rowSpan+"'  style='padding-left: 4px;' colspan='"+baseCols+"' valign='top' align='left' class='botgray'>");
		    if(getDisplayName() != null) {
				if(uri != null) {
					ctx.print("<a class='refuri' href='"+uri+"'>");
				}
		        ctx.print(getDisplayName()); // ##3 display/Baseline
				if(uri != null) {
					ctx.print("</a>");
				}
		    }
		    ctx.println("</th>");
		    
		    
		    // ##2 and a half - baseline sample
		    if (hasExamples) {
		        if(baseExample != null) {
		            ctx.print("<td rowspan='"+rowSpan+"' align='left' valign='top' class='generatedexample'>"+ 
		                    baseExample.replaceAll("\\\\","\u200b\\\\") + "</td>");
		        } else {
		            ctx.print("<td rowspan='"+rowSpan+"' >" + "</td>"); // empty box for baseline
		        }
		    }
		    topCurrent.printCells(ctx,fieldHash,getResultXpath(),ourVote,canModify,ourAlign,uf,zoomedIn,numberedItemsList,exampleContext);
		
		    // ## 6.1, 6.2 - Print the top proposed item. Can be null if there aren't any.
		    CandidateItem topProposed = null;
		    if(proposedItems.size() > 0) {
		        topProposed = proposedItems.get(0);
		    }
		    if(topProposed != null) {
		        topProposed.printCells(ctx,fieldHash,getResultXpath(),ourVote,canModify,ourAlign,uf,zoomedIn,numberedItemsList,exampleContext);
		    } else {
		        printEmptyCells(ctx, ourAlign, zoomedIn);
		    }
		
		    boolean areShowingInputBox = (canSubmit && !isAlias && canModify && !confirmOnly && (zoomedIn||!zoomOnly));
		
		    // submit box
		    if( canModify && (SurveyMain.isPhaseSubmit()==true)
				|| UserRegistry.userIsTC(ctx.session.user)
				|| ( UserRegistry.userIsVetter(ctx.session.user) && ctx.session.user.userIsSpecialForCLDR15(locale))
		        || ((SurveyMain.isPhaseVetting() || SurveyMain.isPhaseVettingClosed()) && ( hasErrors  ||
		                              hasProps  || hasWarnings||  (getResultType()== Vetting.RES_DISPUTED) ))) {
		        String changetoBox = "<td id='i_"+fullFieldHash()+"' width='1%' class='noborder' rowspan='"+rowSpan+"' valign='top'>";
		        // ##7 Change
		        if(canModify && canSubmit && (zoomedIn||!zoomOnly)) {
		            changetoBox = changetoBox+("<input name='"+fieldHash+"' id='"+fieldHash+"_ch' value='"+SurveyMain.CHANGETO+"' type='radio' >");
		        } else {
		            //changetoBox = changetoBox+("<input type='radio' disabled>"); /* don't show the empty input box */
		        }
		        
		        changetoBox=changetoBox+("</td>");
		        
		        if(!(ctx.getDirectionForLocale() == HTMLDirection.RIGHT_TO_LEFT)) {
		            ctx.println(changetoBox);
		        }
		        
		        ctx.print("<td width='25%' class='noborder' rowspan='"+rowSpan+"' valign='top'>");
		        
		        boolean badInputBox = false;
		        
		        
		        if(areShowingInputBox) {
		            String oldValue = (String)ctx.temporaryStuff.get(fieldHash+SurveyMain.QUERY_VALUE_SUFFIX);
		            String fClass = "inputbox";
		            if(oldValue==null) {
		                oldValue="";
		            } else {
		                ctx.print(ctx.iconHtml("stop","this item was not accepted.")+"this item was not accepted.<br>");
		                //fClass = "badinputbox";
		                badInputBox = true;
		            }
		            if(valuesList != null) {
		                ctx.print("<select onclick=\"document.getElementById('"+fieldHash+"_ch').click()\" name='"+fieldHash+"_v'>");
		                ctx.print("  <option value=''></option> ");
		                for(String s : valuesList ) {
		                    ctx.print("  <option value='"+s+"'>"+s+"</option> ");
		                }
		                ctx.println("</select>");
		            } else {
		                // regular change box (text)
		                ctx.print("<input dir='"+ctx.getDirectionForLocale()+"' onfocus=\"document.getElementById('"+fieldHash+"_ch').click()\" name='"+fieldHash+"_v' " + 
		                        " onchange=\"do_change('"+ fullFieldHash() +"',this.value,"+ getXpathId() +",'"+getLocale() +"', '"+ ctx.session +"')\"" +
		                        " value='"+oldValue+"' class='"+fClass+"'>");
		                ctx.print("<div id=\"e_"+ fullFieldHash() +"\" ><!--  errs for this item --></div>");
		            }
		            // references
		            if(badInputBox) {
		               // ctx.print("</span>");
		            }
		
		            if(canModify && zoomedIn && (altType == null) && UserRegistry.userIsTC(ctx.session.user)) {  // show 'Alt' popup for zoomed in main items
		                ctx.println ("<br> ");
		                ctx.println ("<span id='h_alt"+fieldHash+"'>");
		                ctx.println ("<input onclick='javascript:show(\"alt" + fieldHash + "\")' type='button' value='Create Alternate'></span>");
		                ctx.println ("<!-- <noscript> </noscript> -->" + 
		                            "<span style='display: none' id='alt" + fieldHash + "'>");
		                String[] altList = sm.supplemental.getValidityChoice("$altList");
		                ctx.println ("<label>Alt: <select name='"+fieldHash+"_alt'>");
		                ctx.println ("  <option value=''></option>");
		                for(String s : altList) {
		                    ctx.println ("  <option value='"+s+"'>"+s+"</option>");
		                }
		                ctx.println ("</select></label></span>");
		            }
		           
		              
		            ctx.println("</td>");
		        } else  {
		            if(!zoomedIn && zoomOnly) {
		                ctx.println("<i>Must zoom in to edit</i>");
		            }
		            ctx.println("</td>");
		        }
		
		        if(ctx.getDirectionForLocale()==HTMLDirection.RIGHT_TO_LEFT) {
		            ctx.println(changetoBox);
		        }
		
		    } else {
		        areShowingInputBox = false;
		        if (canModify) {
		        	ctx.println("<td rowspan='"+rowSpan+"' colspan='2'></td>");  // no 'changeto' cells.
		        }
		    }
		
		    // No/Opinion.
		    if (canModify) {
		        ctx.print("<td width='20' rowspan='"+rowSpan+"'>");
		        ctx.print("<input name='"+fieldHash+"' value='"+SurveyMain.DONTCARE+"' type='radio' "
		            +((ourVote==null)?"CHECKED":"")+" >");
		        ctx.print("</td>");
		    }
		    
		    
		    if(ctx.prefBool(SurveyMain.PREF_SHCOVERAGE)) {
		    	ctx.print("<th>Cov="+coverageValue+"<br/>V:"+userHasVoted(ctx.userId())+"</th>");
		    }
		    
		    ctx.println("</tr>");
		    
		    // Were there any straggler rows we need to go back and show
		    
		    if(rowSpan > 1) {
		        for(int row=1;row<rowSpan;row++){
		            // do the rest of the rows -  ONLY those which did not have rowSpan='rowSpan'. 
		            
		            ctx.print("<tr>");
		
		            CandidateItem item = null;
		
		            // current item
		            if(currentItems.size() > row) {
		                item = currentItems.get(row);
		                item.printCells(ctx,fieldHash,getResultXpath(),ourVote,canModify,ourAlign,uf,zoomedIn,numberedItemsList,exampleContext);
		            } else {
		                item = null;
		                printEmptyCells(ctx, ourAlign, zoomedIn);
		            }
		
		            // #6.1, 6.2 - proposed items
		            if(proposedItems.size() > row) {
		                item = proposedItems.get(row);
		                item.printCells(ctx,fieldHash, getResultXpath(),ourVote,canModify,ourAlign,uf,zoomedIn,numberedItemsList,exampleContext);
		            } else {
		                item = null;
		                printEmptyCells(ctx, ourAlign, zoomedIn);
		            }
		                            
		            ctx.println("</tr>");
		        }
		    }
		
		    // REFERENCE row
		    if(areShowingInputBox) {
				ctx.print("<tr><td class='botgray' colspan="+SurveyMain.PODTABLE_WIDTH+">");
		        // references
		        if(zoomedIn) {
		            if((refs.length>0) && zoomedIn) {
		                String refHash = fieldHash;
		                Hashtable<String,CandidateItem> refsItemHash = (Hashtable<String, CandidateItem>)ctx.temporaryStuff.get("refsItemHash");
		                ctx.print("<label>");
		                ctx.print("<a "+ctx.atarget()+" href='"+refCtx.url()+"'>Add/Lookup References</a>");
		                if(areShowingInputBox) {
		                    ctx.print("&nbsp;<select name='"+fieldHash+"_r'>");
		                    ctx.print("<option value='' SELECTED>(pick reference)</option>");
		                    for(int i=0;i<refs.length;i++) {
		                        String refValue = null;
		                        // 1: look for a confirmed value
		                        
		                        CandidateItem refItem = refsItemHash.get(refs[i]);
		                        String refValueFull = "";
		                        if(refItem != null) {
		                            refValueFull = refValue = refItem.value;
		                        }
		                        if(refValue == null) {
		                            refValueFull = refValue = " (null) ";
		                        } else {
		                            if(refValue.length()>((SurveyMain.REFS_SHORTEN_WIDTH*2)-1)) {
		                                refValue = refValue.substring(0,SurveyMain.REFS_SHORTEN_WIDTH)+"\u2026"+
		                                            refValue.substring(refValue.length()-SurveyMain.REFS_SHORTEN_WIDTH); // "..."
		                            }
		                        }
		                        
		                        ctx.print("<option title='"+refValueFull+"' value='"+refs[i]+"'>"+refs[i]+": " + refValue+"</option>");
		                    }
		                    ctx.println("</select>");
		                }
		                ctx.print("</label>");
		            } else if(false) {
		                ctx.print("<a "+ctx.atarget()+" href='"+refCtx.url()+"'>Add Reference</a>");
		            }
		        }
		        ctx.println("</td></tr>");
		    }
		    
		    // now, if the warnings list isn't empty.. warnings rows
		    if(!numberedItemsList.isEmpty()) {
		        int mySuperscriptNumber =0;
		        for(CandidateItem item : numberedItemsList) {
		            mySuperscriptNumber++;
			if(item==null) continue; /* ?! */
		            if(item.tests==null && item.examples==null) 
		            	continue; /* skip rows w/o anything */
		            ctx.println("<tr class='warningRow'><td class='botgray'><span class='warningTarget'>#"+mySuperscriptNumber+"</span></td>");
		            if(item.tests != null) {
		                ctx.println("<td colspan='" + (SurveyMain.PODTABLE_WIDTH-1) + "' class='warncell'>");
		                for (Iterator it3 = item.tests.iterator(); it3.hasNext();) {
		                    CheckStatus status = (CheckStatus) it3.next();
		                    if (!status.getType().equals(status.exampleType)) {
		                        ctx.println("<span class='warningLine'>");
		                        String cls = SurveyMain.shortClassName(status.getCause());
		                        if(status.getType().equals(status.errorType)) {
		                            ctx.print(ctx.iconHtml("stop","Error: "+cls));
		                        } else {
		                            ctx.print(ctx.iconHtml("warn","Warning: "+cls));
		                        }
		                        ctx.print(status.toString());
		                        ctx.println("</span>");
		                        
		                        ctx.println(" For help, see <a " + ctx.atarget(WebContext.TARGET_DOCS) + 
		                        		" href='http://cldr.org/translation/fixing-errors'>Fixing Errors and Warnings</a>");
		                        ctx.print("<br>");
		                    }
		                }
		            } else {
		                ctx.println("<td colspan='" + (SurveyMain.PODTABLE_WIDTH-1) + "' class='examplecell'>");
		            }
		            if(item.examples != null) {
		                boolean first = true;
		                for(Iterator it4 = item.examples.iterator(); it4.hasNext();) {
		                    ExampleEntry e = (ExampleEntry)it4.next();
		                    if(first==false) {
		                    }
		                    String cls = SurveyMain.shortClassName(e.status.getCause());
		                    if(e.status.getType().equals(e.status.exampleType)) {
		                        SurveyMain.printShortened(ctx,e.status.toString());
		                        if(cls != null) {
		                            ctx.printHelpLink("/"+cls+"","Help");
		                        }
		                        ctx.println("<br>");
		                    } else {                        
		                        String theMenu = PathUtilities.xpathToMenu(sm.xpt.getById(parentRow.getXpathId()));
		                        if(theMenu==null) {
		                            theMenu="raw";
		                        }
		                        ctx.print("<a "+ctx.atarget(WebContext.TARGET_EXAMPLE)+" href='"+ctx.url()+ctx.urlConnector()+"_="+ctx.getLocale()+"&amp;x="+theMenu+"&amp;"+ SurveyMain.QUERY_EXAMPLE+"="+e.hash+"'>");
		                        ctx.print(ctx.iconHtml("zoom","Zoom into "+cls)+cls);
		                        ctx.print("</a>");
		                    }
		                    first = false;
		                }
		            }
		            ctx.println("</td>");
		            ctx.println("</tr>");
		        }
		    }
		    
			
		
		    if(zoomedIn && ( (!SurveyMain.isPhaseSubmit() && !SurveyMain.isPhaseBeta()) || SurveyMain.isUnofficial || true) ) {
		    
		    ctx.print("<tr>");
		    ctx.print("<th colspan=2>Votes</th>");
		    ctx.print("<td colspan="+(SurveyMain.PODTABLE_WIDTH-2)+">");
			
		    showVotingResults(ctx);
		        
		    ctx.println("</td></tr>");
		        
		        if(aliasFromLocale != null) {
		            ctx.print("<tr class='topgray'>");
		            ctx.print("<th class='topgray' colspan=2>Alias Items</th>");
		            ctx.print("<td class='topgray' colspan="+(SurveyMain.PODTABLE_WIDTH-2)+">");
		            
		            
		            String theURL = sm.fora.forumUrl(ctx, aliasFromLocale, aliasFromXpath);
		            String thePath = sm.xpt.getPrettyPath(aliasFromXpath);
		            String theLocale = aliasFromLocale;
		
		            ctx.println("Note: Before making changes here, first verify:<br> ");
		            ctx.print("<a class='alias' href='"+theURL+"'>");
		            ctx.print(thePath);
		            if(!aliasFromLocale.equals(locale)) {
		                ctx.print(" in " + new ULocale(theLocale).getDisplayName(ctx.displayLocale));
		            }
		            ctx.print("</a>");
		            ctx.print("");
		            
		            ctx.println("</td></tr>");
		        }
			}
		}
		String getSpecialURL(WebContext ctx) {
		    if(getXpath().startsWith(DataSection.EXEMPLAR_PARENT)) {
		        String zone = prettyPath;
		        int n = zone.lastIndexOf('/');
		        if(n!=-1) {
		            zone = zone.substring(0,n); //   blahblah/default/foo   ->  blahblah/default   ('foo' is lastType and will show up as the value)
		        }
		        return ctx.base()+"?"+
		                "_="+ctx.getLocale()+"&amp;x=timezones&amp;zone="+zone;                
		        /*
		         *                     if(section.xpath(p).startsWith(DataSection.EXEMPLAR_PARENT)) {
		                    String zone = p.prettyPath;
		                    int n = zone.lastIndexOf('/');
		                    if(n!=-1) {
		                        zone = zone.substring(0,n); //   blahblah/default/foo   ->  blahblah/default   ('foo' is lastType and will show up as the value)
		                    }
		                    showDataRow(ctx, section, p, uf, cf, ourSrc, canModify,ctx.base()+"?"+
		                            "_="+ctx.getLocale()+"&amp;x=timezones&amp;zone="+zone,refs,checkCldr, zoomedIn);                
		                } else {
		                }
		
		         */
		    }
		    return null;
		
		}
		/**
		 * @param ctx
		 */
		void showVotingResults(WebContext ctx) {
			
			BallotBox<User> ballotBox = sm.getSTFactory().ballotBoxForLocale(locale());
			ctx.put("ballotBox",ballotBox);
			ctx.put("resolver", ballotBox.getResolver(getXpath()));
			ctx.includeFragment("show_voting.jsp");
		}
    }

    Hashtable<String, DataRow> rowsHash = new Hashtable<String, DataRow>(); // hashtable of type->Row
 
    /**
     * get all rows.. unsorted.
     */
    public Collection<DataRow> getAll() {
        return rowsHash.values();
    }
    

    /** 
     * A class representing a list of rows, in sorted and divided order.
     */
    public class DisplaySet {
        public int size() {
            return rows.length;
        }
        SortMode sortMode = null;
        public boolean canName = true; // can use the 'name' view?
        public boolean isCalendar = false;
        public boolean isMetazones = false;
        DataRow rows[]; // list of peas in sorted order
        /**
         * Partitions divide up the peas into sets, such as 'proposed', 'normal', etc.
         * The 'limit' is one more than the index number of the last item.
         * In some cases, there is only one partition, and its name is null.
         */
        
        public Partition partitions[];  // display group partitions.  Might only contain one entry:  {null, 0, <end>}.  Otherwise, contains a list of entries to be named separately

        /**
         * 
         * @param myRows the original rows
         * @param myDisplayRows the rows in display order (?)
         * @param sortMode the sort mode to use
         */
        public DisplaySet(DataRow[] myRows, SortMode sortMode) {
            this.sortMode = sortMode;
            
            rows = myRows;

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
            partitions = Partition.createPartitions(sortMode.memberships(),rows);
        }

    }

	public static String PARTITION_ERRORS = "Error Values";
	public static String CHANGES_DISPUTED = "Disputed";
	public static String PARTITION_UNCONFIRMED = "Unconfirmed";
	public static String TENTATIVELY_APPROVED = "Tentatively Approved";
	public static String STATUS_QUO = "Status Quo";
    
	/**
	 * @deprecated
	 */
    public static final String VETTING_PROBLEMS_LIST[] = { 
        PARTITION_ERRORS,
        CHANGES_DISPUTED,
        PARTITION_UNCONFIRMED };

    
    DisplaySet createDisplaySet(SortMode sortMode, XPathMatcher matcher) {
        DisplaySet aDisplaySet = new DisplaySet(createSortedList(sortMode,matcher), sortMode);
        aDisplaySet.canName = canName;
        aDisplaySet.isCalendar = isCalendar;
        aDisplaySet.isMetazones = isMetazones;
        return aDisplaySet;
    }
        
    
    private DataRow[] createSortedList(SortMode sortMode, XPathMatcher matcher) {
        Set<DataRow> newSet;
        
        newSet = new TreeSet<DataRow>(sortMode.createComparator());
        
        if(matcher == null) {
            newSet.addAll(rowsHash.values()); // sort it    
        } else {
            for(Object o : rowsHash.values()) {
                DataRow p = (DataRow)o;
                                
///*srl*/         /*if(p.type.indexOf("Australia")!=-1)*/ {  System.err.println("xp: "+p.xpathSuffix+":"+p.type+"- match: "+(matcher.matcher(p.type).matches())); }

                if(!matcher.matches(p.getXpath(), p.getXpathId())) {
                    if(DEBUG) System.err.println("not match: " + p.xpathId + " / " + p.getXpath());
                    continue;
                    
                } else {
                	newSet.add(p);
                }
            }
        }
        String matchName = "(*)";
        if(matcher!=null) {
        	matchName = matcher.getName();
        }
        if(sm.isUnofficial) System.err.println("Loaded "+ newSet.size() + " from " + matchName + " - base xpath ("+rowsHash.size()+")  = " + this.xpathPrefix);
        return newSet.toArray(new DataRow[newSet.size()]);
    }

	/**
	 * Create, populate, and complete a DataSection given the specified locale and prefix
	 * @param ctx context to use (contains CLDRDBSource, etc.)
	 * @param locale locale
	 * @param prefix XPATH prefix
	 * @param simple if true, means that data is simply xpath+type. If false, all xpaths under prefix.
	 */
    public static DataSection make(WebContext ctx, CLDRLocale locale, String prefix, boolean simple, String ptype) {
    	DataSection section = new DataSection(ctx.sm, locale, prefix, ptype);
    	//        section.simple = simple;
    	SurveyMain.UserLocaleStuff uf = ctx.getUserFile();

    	final String[] prefixesWithExamples = { "currencies", "calendars", "codePatterns", "numbers", "localeDisplayPattern"};
    	for ( String s : prefixesWithExamples ) {
    		if ( prefix.contains(s)) {
    			section.hasExamples = true;
    			break;
    		}
    	}
    	if ( prefix.endsWith("ldml")) { // special check for the 'misc' section
    		section.hasExamples = true;
    	}
    	if(ctx.sm.supplementalDataDir == null) {
    		throw new InternalError("??!! ctx.sm.supplementalDataDir = null");
    	}
    	
//    	XMLSource ourSrc = uf.resolvedSource;
    	CLDRFile ourSrc = ctx.sm.getSTFactory().make(locale.getBaseName(), true, true).setSupplementalDirectory(ctx.sm.supplementalDataDir);
    	if(ourSrc.getSupplementalDirectory()==null) {
    		throw new InternalError("?!! ourSrc hsa no supplemental dir!");
    	}
    	synchronized(ctx.session) {
    		CheckCLDR checkCldr = uf.getCheck(ctx);
        	if(ourSrc.getSupplementalDirectory()==null) {
        		throw new InternalError("?!! ourSrc hsa no supplemental dir!");
        	}
    		if(checkCldr == null) {
    			throw new InternalError("checkCldr == null");
    		}
        	if(ourSrc.getSupplementalDirectory()==null) {
        		throw new InternalError("?!! ourSrc hsa no supplemental dir!");
        	}
    		String workingCoverageLevel = section.getPtype();
    		com.ibm.icu.dev.test.util.ElapsedTimer cet = null;
    		if(SHOW_TIME) {
    			cet= new com.ibm.icu.dev.test.util.ElapsedTimer();
    			System.err.println("Begin populate of " + locale + " // " + prefix+":"+workingCoverageLevel + " - is:" + ourSrc.getClass().getName());
    		}
        	if(ourSrc.getSupplementalDirectory()==null) {
        		throw new InternalError("?!! ourSrc hsa no supplemental dir!");
        	}
    		CLDRFile baselineFile = ctx.sm.getBaselineFile();
        	if(ourSrc.getSupplementalDirectory()==null) {
        		throw new InternalError("?!! ourSrc hsa no supplemental dir!");
        	}
    		section.skippedDueToCoverage=0;
        	if(ourSrc.getSupplementalDirectory()==null) {
        		throw new InternalError("?!! ourSrc hsa no supplemental dir!");
        	}
    		ctx.println("<script type=\"text/javascript\">document.getElementById('loadSection').innerHTML='Populating...';</script>"); ctx.flush();
        	if(ourSrc.getSupplementalDirectory()==null) {
        		throw new InternalError("?!! ourSrc hsa no supplemental dir!");
        	}
        	if(ourSrc.getSupplementalDirectory()==null) {
        		throw new InternalError("?!! ourSrc hsa no supplemental dir!");
        	}
        	try {
        		section.populateFrom(ourSrc, checkCldr, baselineFile, ctx.getOptionsMap(),workingCoverageLevel);
        	} finally {
        		if(NOINHERIT) {
        			SurveyLog.errln(" ************** ERROR:  NOINHERIT=true in DataSection.  Inherited items are not properly calculated. TODO");
        		}
        	}
        	int popCount = section.getAll().size();
    		/*            if(SHOW_TIME) {
	                System.err.println("DP: Time taken to populate " + locale + " // " + prefix +":"+ctx.defaultPtype()+ " = " + et + " - Count: " + pod.getAll().size());
	            }*/
    		ctx.println("<script type=\"text/javascript\">document.getElementById('loadSection').innerHTML='Completing..."+popCount+" items';</script>"); ctx.flush();
    		section.ensureComplete(ourSrc,  checkCldr, baselineFile, ctx.getOptionsMap(), workingCoverageLevel);
    		if(SHOW_TIME) {
    			int allCount = section.getAll().size();
    			System.err.println("Populate+complete " + locale + " // " + prefix +":"+section.getPtype()+ " = " + cet + " - Count: " + popCount+"+"+(allCount-popCount)+"="+allCount);
    		}
    	}
    	return section;
    }
    
    private String getPtype() {
        return ptype;
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
                                                    "/dayPeriodContext|"+
                                                    "/dayPeriodWidth|"+
                                                    "/dayPeriod|"+
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
    
    /**
     * Divider denoting a specific Continent division.
     */
	public static final String CONTINENT_DIVIDER = "\u2603";

	private BallotBox<User> ballotBox;
	
	public BallotBox<User> getBallotBox() { 
		return ballotBox;
	}
    
    private void populateFrom(CLDRFile ourSrc, CheckCLDR checkCldr, CLDRFile baselineFile, Map<String,String> options, String workingCoverageLevel) {
        //if (!ourSrc.isResolving()) throw new IllegalArgumentException("CLDRFile must be resolved");
        DBEntry vettedParentEntry = null;
        try  {
        	init();
        	XPathParts xpp = new XPathParts(null,null);
        	//        System.out.println("[] initting from pod " + locale + " with prefix " + xpathPrefix);
        	CLDRFile aFile = ourSrc;
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

        	int workingCoverageValue = SupplementalDataInfo.CoverageLevelInfo.strToCoverageValue(workingCoverageLevel);
        	// what to exclude under 'misc'

        	CLDRFile vettedParent = null;
        	CLDRLocale parentLoc = locale.getParent();
        	if(parentLoc != null) {
        		XMLSource vettedParentSource = sm.makeDBSource(parentLoc, true /*finalData*/, true);            
        		vettedParent = new CLDRFile(vettedParentSource).setSupplementalDirectory(sm.supplementalDataDir);
        		vettedParentEntry = sm.getDBSourceFactory().openEntry(vettedParentSource.getUnresolving());
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
        	
        	/* ** Determine which xpaths to show */
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
        			int continentStart = xpathPrefix.indexOf(DataSection.CONTINENT_DIVIDER);
        			if(continentStart>0) {
        				continent = xpathPrefix.substring(xpathPrefix.indexOf(DataSection.CONTINENT_DIVIDER)+1);
        			}
        			if(false) System.err.println(xpathPrefix+": -> continent " + continent);
        			if(!xpathPrefix.contains("@type")) { // if it's not a zoom-in..
        				workPrefix = "//ldml/dates/timeZoneNames/metazone";
        			}
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

        	isCalendar = xpathPrefix.startsWith("//ldml/dates/calendars");
        	isMetazones = xpathPrefix.startsWith("//ldml/dates/timeZoneNames/metazone");

        	List checkCldrResult = new ArrayList();

        	/* ** Build the set of xpaths */
        	// iterate over everything in this prefix ..
        	Set<String> baseXpaths = new HashSet<String>();
        	for(Iterator<String> it = aFile.iterator(workPrefix);it.hasNext();) {
        		String xpath = (String)it.next();

        		baseXpaths.add(xpath);
        	}
        	Set<String> allXpaths = new HashSet<String>();
        	Set<String> extraXpaths = new HashSet<String>();

        	allXpaths.addAll(baseXpaths);
        	if(aFile.getSupplementalDirectory()==null) {
        		throw new InternalError("?!! aFile hsa no supplemental dir!");
        	}
        	aFile.getExtraPaths(workPrefix,extraXpaths);
        	extraXpaths.removeAll(baseXpaths);
        	allXpaths.addAll(extraXpaths);

        	//        // Process extra paths.
        	if(DEBUG) System.err.println("@@X@ base["+workPrefix+"]: " + baseXpaths.size() + ", extra: " + extraXpaths.size());

        	/* ** iterate over all xpaths */
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

        		/* ** Skip unmatched things */
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
        			continue;
        		}

        		String fullPath = aFile.getFullXPath(xpath);
        		//int xpath_id = src.xpt.getByXpath(fullPath);
        		int base_xpath = sm.xpt.xpathToBaseXpathId(xpath);
        		String baseXpath = sm.xpt.getById(base_xpath);

        		// Filter out data that is higher than the desired coverage level
        		int	coverageValue = sdi.getCoverageValue(baseXpath,locale.toULocale());
        		if ( coverageValue > workingCoverageValue ) {
        			if ( coverageValue <= 100 ) {
        				skippedDueToCoverage++;
        			} // else: would never be shown, don't care
        			continue;
        		}

                String prettyPath = sm.xpt.getPrettyPath(base_xpath);

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
        		
                String value = isExtraPath?null:aFile.getStringValue(xpath);
                String peaSuffixXpath = null; // if non null:  write to suffixXpath


                type = prettyPath;
                

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
        		DataRow p = getDataRow(xpath);
        		p.winningXpathId = sm.getDBSourceFactory().getWinningPathId(base_xpath, locale, false);

        		p.coverageValue=coverageValue;

        		DataRow superP = p; // getDataRow(type);  // the 'parent' row (sans alt) - may be the same object
        		superP.coverageValue=coverageValue;

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
        		if(p.prettyPath.startsWith("layout/inText")) {
        			p.valuesList = LAYOUT_INTEXT_VALUES;
        			superP.valuesList = p.valuesList;
        		} else if(p.prettyPath.startsWith("defaultNumberingSystem")) { 
        			// Not all available numbering systems are good candidates for default numbering system.
        			// Things like "roman" shouldn't really be an option.  So, in the interest of simplicity,
        			// we are hard-coding the choices here.
        			String [] values = { "latn", "arab", "arabext", "armn", "beng", "deva", "ethi", "geor", "gujr", "guru", "hans", "hant", "hebr", "jpan", "khmr", "knda", "laoo", "mlym", "mong", "orya", "tamldec", "telu", "thai", "tibt" };
        			p.valuesList = values;
        			superP.valuesList = p.valuesList;
        		} else if(p.prettyPath.indexOf("commonlyUsed")!=-1) { 
        			p.valuesList = METAZONE_COMMONLYUSED_VALUES;
        			superP.valuesList = p.valuesList;
        		} else if(p.prettyPath.startsWith("layout/inList")) {
        			p.valuesList = LAYOUT_INLIST_VALUES;
        			superP.valuesList = p.valuesList;
        		}


        		if(TRACE_TIME) System.err.println("n05  "+(System.currentTimeMillis()-nextTime));


        		if((superP.getDisplayName() == null) ||
        				(p.getDisplayName() == null)) {
        			canName = false; // disable 'view by name' if not all have names.
        		}
        		if(TRACE_TIME) System.err.println("n06  "+(System.currentTimeMillis()-nextTime));

        		// If it is draft and not proposed.. make it proposed-draft 
        		if( ((eDraft!=null)&&(!eDraft.equals("false"))) &&
        				(altProposed == null) ) {
        			altProposed = SurveyMain.PROPOSED_DRAFT;
        		}

        		// Inherit display names.
        		if((superP != p) && (p.getDisplayName() == null)) {
        			p.setDisplayName(baselineFile.getStringValue(baseXpath)); 
        			if(p.getDisplayName() == null) {
        				p.setDisplayName(superP.getDisplayName()); // too: unscramble this a little bit
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
if(!NOINHERIT)        				p.updateInheritedValue(vettedParent, checkCldr, options); // update the tests
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
            		if(TRACE_TIME) System.err.println("n07.1  (check) "+(System.currentTimeMillis()-nextTime));
        			checkCldr.check(xpath, fullPath, isExtraPath?null:value, options, checkCldrResult);
            		if(TRACE_TIME) System.err.println("n07.2  (check) "+(System.currentTimeMillis()-nextTime));
        			checkCldr.getExamples(xpath, fullPath, isExtraPath?null:value, options, examplesResult);
        		}
        		DataSection.DataRow.CandidateItem myItem = null;

        		/*            if(p.attributeChoice != null) {
                String newValue = p.attributeChoice.valueOfXpath(fullPath);
//       System.err.println("ac:"+fullPath+" -> " + newValue);
                value = newValue;
            }*/
        		if(TRACE_TIME) System.err.println("n08  (check) "+(System.currentTimeMillis()-nextTime));
        		myItem = p.addItem( value, null);
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
        		myItem.submitter = sm.getDBSourceFactory().getSubmitterId(locale, myItem.xpathId);
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
        		
        		
        		Set<String> v = ballotBox.getValues(xpath);
        		if(v!=null) for(String avalue : v) {
        		    System.err.println(" //val='"+avalue+"' vs " + value);
        		    if(!avalue.equals(value)) {
        		       CandidateItem item2 = p.addItem(avalue, null);
        		       item2.xpath = xpath;
        		    }
        		}

        	}
        	//        aFile.close();
        } finally {
    	if(vettedParentEntry!=null) {
    		try {
				vettedParentEntry.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    }
    }
    /**
     * Makes sure this pod contains the peas we'd like to see.
     */
    private void ensureComplete(CLDRFile ourSrc, CheckCLDR checkCldr, CLDRFile baselineFile, Map<String,String> options, String workingCoverageLevel) {
    	//if (!ourSrc.isResolving()) throw new IllegalArgumentException("CLDRFile must be resolved");
//    	if(xpathPrefix.contains("@type")) {
//    		if(DEBUG) System.err.println("Bailing- no reason to complete a type-specifix xpath");
//    		return; // don't try to complete if it's a specific item.
//    	} else if(DEBUG) {
//    		System.err.println("Completing: " + xpathPrefix);
//    	}
    	
        SupplementalDataInfo sdi = sm.getSupplementalDataInfo();
        int workingCoverageValue = SupplementalDataInfo.CoverageLevelInfo.strToCoverageValue(workingCoverageLevel);
        if(xpathPrefix.startsWith("//ldml/"+"dates/timeZoneNames")) {
            // work on zones
            boolean isMetazones = xpathPrefix.startsWith("//ldml/"+"dates/timeZoneNames/metazone");
            boolean isSingleXPath = false;
            // Make sure the pod contains the peas we'd like to see.
            // regular zone
            
            Iterator zoneIterator;
            
            if(isMetazones) {
                if ( xpathPrefix.indexOf(CONTINENT_DIVIDER) > 0 ) {
                    String [] pieces = xpathPrefix.split(CONTINENT_DIVIDER,2);
                    xpathPrefix = pieces[0];
                    zoneIterator = sm.getMetazones(pieces[1]).iterator();
                } else { // This is just a single metazone from a zoom-in
                    Set<String> singleZone = new HashSet<String>();
                    XPathParts xpp = new XPathParts();
                    xpp.set(xpathPrefix);
                    String singleMetazoneName = xpp.findAttributeValue("metazone", "type");
                    singleZone.add(singleMetazoneName);
                    zoneIterator = singleZone.iterator();
                    
                    isSingleXPath = true;
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
                                "/short/standard"
            };
            
            String suffs[];
            if(isMetazones) {
                suffs = mzsuffs;
            } else {
                suffs = tzsuffs;
            }        

            String podBase = xpathPrefix;
            CLDRFile resolvedFile = ourSrc;
//            XPathParts parts = new XPathParts(null,null);
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
                    
                    if ( isSingleXPath && !xpathPrefix.contains(suff)) { // For a single xpath ( zoom-in ) only try to synthesize the one we need.
                        continue;
                    } else {
                        podBase = "//ldml/dates/timeZoneNames/metazone";
                    }
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
                            skippedDueToCoverage++;
                        } // else: would never be shown, don't care.
                        continue;
                    }
                    
                    DataSection.DataRow myp = getDataRow(rowXpath);
                    
                    myp.coverageValue = coverageValue;
                    
                    // set it up..
                    int base_xpath = sm.xpt.getByXpath(base_xpath_string);
                    
                    if(false) {
                        
                        // set up the pea
                        if(CheckCLDR.FORCE_ZOOMED_EDIT.matcher(base_xpath_string).matches()) {
                            myp.zoomOnly = true;
                        }

                        // set up tests
                        myp.setShimTests(base_xpath,base_xpath_string,checkCldr,options);
                    } else {
                        System.err.println("Note: Not setting up shims.");
                    }
                    
                    
                    myp.setDisplayName(baselineFile.getStringValue(podBase+ourSuffix+suff)); // use the baseline (English) data for display name.
                    
                }
            }
        } // tz
    }
// ==

    public DataRow getDataRow(String xpath) {
        if(xpath == null) {
            throw new InternalError("type is null");
        }
        if(rowsHash == null) {
            throw new InternalError("peasHash is null");
        }
        DataRow p = (DataRow)rowsHash.get(xpath);
        if(p == null) {
            p = new DataRow(xpath);
            addDataRow(p);
        }
        return p;
    }
    
//    private DataRow getDataRow(String type, String altType) {
//        if(altType == null) {
//            return getDataRow(type);
//        } else {
//            DataRow superDataRow = getDataRow(type);
//            return superDataRow.getSubDataRow(altType);
//        }
//    }
    
    /**
     * Linear search for matching item.
     * @param xpath
     * @return the matching DatRow
     */
    public DataRow getDataRow(int xpath) {
        return getDataRow(sm.xpt.getById(xpath));
//     	// TODO: replace with better sort.
//    	for(DataRow dr : rowsHash.values()) {
//    		if(dr.base_xpath == xpath) {
//    			return dr;
//    		}
//    		// search subrows
//    		if(dr.subRows!=null) {
//    			for(DataRow subRow : dr.subRows.values()) {
//    				if(subRow.base_xpath == xpath) {
//    					return subRow;
//    				}
//    			}
//    		}
//    	}
//    	// look for sub-row
//    	
//    	return null;
    }
    
    void addDataRow(DataRow p) {
        rowsHash.put(p.prettyPath, p);
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
    public int getSkippedDueToCoverage() {
        return skippedDueToCoverage;
    }
}
