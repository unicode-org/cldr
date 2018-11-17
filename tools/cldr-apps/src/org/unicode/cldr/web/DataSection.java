//
//  DataSection.java
//
//  Created by Steven R. Loomis on 18/11/2005.
//  Copyright 2005-2014 IBM. All rights reserved.
//

//  TODO: this class now has lots of knowledge about specific data types.. so does SurveyMain
//  Probably, it should be concentrated in one side or another- perhaps SurveyMain should call this
//  class to get a list of displayable items?  A: no, use PathHeader.

package org.unicode.cldr.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONString;
import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.InputMethod;
import org.unicode.cldr.test.CheckCLDR.Options;
import org.unicode.cldr.test.CheckCLDR.StatusAction;
import org.unicode.cldr.test.DisplayAndInputProcessor;
import org.unicode.cldr.test.ExampleGenerator.ExampleContext;
import org.unicode.cldr.test.ExampleGenerator.ExampleType;
import org.unicode.cldr.test.TestCache.TestResultBundle;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRInfo.CandidateInfo;
import org.unicode.cldr.util.CLDRInfo.PathValueInfo;
import org.unicode.cldr.util.CLDRInfo.UserInfo;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CoverageInfo;
import org.unicode.cldr.util.LDMLUtilities;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.PathHeader.SectionId;
import org.unicode.cldr.util.PathHeader.SurveyToolStatus;
import org.unicode.cldr.util.PathUtilities;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.util.VoteResolver.Status;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.web.DataSection.DataRow.CandidateItem;
import org.unicode.cldr.web.SurveyMain.UserLocaleStuff;
import org.unicode.cldr.web.UserRegistry.User;

import com.google.common.collect.ImmutableList;
import com.ibm.icu.text.Collator;
import com.ibm.icu.util.Output;

/**
 * A DataSection represents a group of related data that will be displayed to
 * users in a list such as,
 * "all of the language display names contained in the en_US locale". It is
 * sortable, as well, and has some level of persistence.
 *
 * This class was formerly named DataPod
 */
public class DataSection implements JSONString {

    /*
     * For debugging only (so far), setting USE_CANDIDATE_HISTORY to true causes
     * a "history" to be constructed and passed to the client for each CandidateItem,
     * indicating how/when/why it was added. This should be false for production.
     */
    private static boolean USE_CANDIDATE_HISTORY = false;

    /*
     * A placeholder for winningValue when resolver.getWinningValue()
     * returns null, due to unsolved bugs.
     */
    private static String ERROR_NO_WINNING_VALUE = "error-no-winning-value";

    /*
     * TODO: order classes consistently; inner classes should all be at top or all be at bottom.
     * Default for Eclipse "Sort Members" is to put Types, including inner classes, before all
     * other members; however, it also alphabetizes methods, which may not be helpful.
     */
    
    /**
     * The baseline CLDRFile for this DataSection
     */
    public CLDRFile baselineFile;
    
    /**
     * The DisplayAndInputProcessor for this DataSection
     */
    private DisplayAndInputProcessor processor = null;

    /**
     * A DataRow represents a "row" of data - a single distinguishing xpath
     *
     * This class was formerly named "Pea"
     *
     * @author srl
     */
    public class DataRow implements JSONString, PathValueInfo {

        /**
         * A CandidateItem represents a particular alternative item which could be chosen or voted for.
         *
         * Each DataRow has, in general, any number of these items in DataRow.items.
         */
        public class CandidateItem implements Comparable<CandidateItem>, JSONString, CandidateInfo {

            /**
             * rawValue is the actual value of this CandidateItem, that is, the string for which
             * a user might vote.
             *
             * This member is private; use the function getValue() to access it.
             *
             * This member is "final", its value for each CandidateItem is set once and for all
             * when the constructor is called.
             * 
             * Renamed from "value" to "rawValue" 2018-8-13, for consistency with the client variable name,
             * to emphasize distinction from getProcessedValue(), and to reduce confusion with other
             * occurrences of the word "value".
             */
            final private String rawValue;

            /**
             * isOldValue means the value of this CandidateItem is equal to oldValue, which is a member of DataRow.
             *
             * isOldValue is used on both server and client. On the client, it can result in addIcon(choiceField,"i-star"),
             * and that is its only usage on the client.
             */
            private boolean isOldValue = false;

            /**
             * checkedVotes is used only in the function getVotes, for efficiency.
             */
            private boolean checkedVotes = false;

            /**
             * TODO: document the purpose of examples, non-null only in a short block of code
             * currently (2018-8-10) near end of populateFrom.
             */
            private Vector<ExampleEntry> examples = null;

            /**
             * tests is included in json data sent to client
             *
             * See setTests and getCheckStatusList
             */
            private List<CheckStatus> tests = null;

            /**
             * Set of Users who voted on this item
             */
            private Set<UserRegistry.User> votes = null;

            /**
             * see getValueHash
             */
            private String valueHash = null;

            /**
             * A history of events in the creation of this CandidateItem,
             * for debugging and possibly for inspection by users;
             * unused if USE_CANDIDATE_HISTORY is false.
             */
            private String history = null;
            
            /**
             * Create a new CandidateItem with the given value
             *
             * @param value,  may be null if called by setShimTests!
             *
             * This constructor sets this.rawValue, which is final (can't be changed once set here).
             */
            private CandidateItem(String value) {
                this.rawValue = value;
            }

            /**
             * Get the raw value of this CandidateItem
             *
             * @return the value, as a string
             *
             * Compare getProcessedValue.
             */
            @Override
            public String getValue() {
                return rawValue;
            }

            /**
             * Get the value of this CandidateItem, processed for display.
             *
             * @return the processed value
             *
             * Called only by CandidateItem.toJSONString
             *
             * This is what the client receives by the name "value".
             * Compare what is called "rawValue" on both server and client.
             */
            private String getProcessedValue() {
                if (rawValue == null) {
                    return null;
                }
                try {
                    return getProcessor().processForDisplay(xpath, rawValue);
                } catch (Throwable t) {
                    if (SurveyLog.DEBUG) {
                        SurveyLog.logException(t, "While processing " + xpath + ":" + rawValue);
                    }
                    return rawValue;
                }
            }

            /**
             * Get the hash of the raw value of this candidate item.
             *
             * @return the hash of the raw value
             */
            private String getValueHash() {
                if (valueHash == null) {
                    valueHash = DataSection.getValueHash(rawValue);
                }
                return valueHash;
            }

            /**
             * Compare this CandidateItem with the given other CandidateItem.
             *
             * @param other the other item with which to compare this one
             * @return 0 if they are the same item, else a positive or negative number
             *           obtained by comparing the two values as strings
             */
            @Override
            public int compareTo(CandidateItem other) {
                if (other == this) {
                    return 0;
                }
                return rawValue.compareTo(other.rawValue);
            }

            /**
             * Get the set of users who have voted for this CandidateItem
             *
             *  @return the set of users
             */
            @Override
            public Collection<UserInfo> getUsersVotingOn() {
                Set<UserRegistry.User> uvotes = getVotes();
                if (uvotes == null) {
                    return Collections.emptySet();
                }
                TreeSet<UserInfo> ts = new TreeSet<UserInfo>(uvotes);
                // TODO: change return type for perf?
                return ts;
            }

            /**
             * Get the set of votes for this CandidateItem
             *
             * @return the set of votes
             */
            private Set<UserRegistry.User> getVotes() {
                if (!checkedVotes) {
                    votes = ballotBox.getVotesForValue(xpath, rawValue);
                    checkedVotes = true;
                }
                return votes;
            }

            /**
             * Get the class for this CandidateItem
             *
             * @return the class as a string, for example, "winner"
             * 
             * All return values: "winner", "alias", "fallback", "fallback_code", "fallback_root", "loser".
             * 
             * Called by CandidateItem.toJSONString (for item.pClass)
             * 
             * Relationships between class, color, and inheritance (http://cldr.unicode.org/index/survey-tool/guide#TOC-Inheritance):
             * "The inherited values are color coded:
             *  1.  Darker [blue] The original is from a parent locale, such as if you are working in
             *      Latin American Spanish (es_419), this value is inherited from European Spanish (es).
             *      [corresponds with "fallback" and {background-color: #5bc0de;}]
             *  2.  Lighter [violet] The original is in the same locale, but has a different ID (row).
             *      [corresponds with "alias" and {background-color: #ddf;}]
             *  3.  Red  The original is from the root."
             *      [corresponds with "fallback_root" or "fallback_code" and {background-color: #FFDDDD;}]
             */
            private String getPClass() {
                if (rawValue.equals(CldrUtility.INHERITANCE_MARKER)) {
                    if (pathWhereFound != null) {
                        /*
                         * surveytool.css has:
                         *  .alias {background-color: #ddf;}
                         *  
                         *  This can happen when called from CandidateItem.toJSONString (for item.pClass).
                         *  Try http://localhost:8080/cldr-apps/v#/aa/Fields/
                         */
                        return "alias";
                    }
                    /*
                     * surveytool.css has:
                     *  .fallback {background-color:#5bc0de;padding: .1em .4em .2em;}
                     *  .fallback_root, .fallback_code {border: 1px dotted #f00 !important;background-color: #FFDDDD;}
                     */
                    if (inheritedLocale != null && XMLSource.CODE_FALLBACK_ID.equals(inheritedLocale.getBaseName())) {
                        return "fallback_code";
                    }
                    if (inheritedLocale == CLDRLocale.ROOT) {
                        return "fallback_root";
                    }
                    return "fallback";
                }
                if (winningValue != null && winningValue.equals(rawValue)) {
                    /*
                     * An item can be both winning and inherited (alias/fallback). If an item is both
                     * winning and inherited, then its class/style/color is determined by inheritance,
                     * not by whether it's winning.
                     * 
                     * redesign.css has:
                     * .winner, .value {font-weight:bold;}
                     * .value-div .winner, .value-div .value {font-size:16px;}
                     */
                    return "winner";
                }
                return "loser";
            }

            /**
             * Set the tests for this CandidateItem
             *
             * @return true if any valid tests were found, else false
             */
            private boolean setTests(List<CheckStatus> testList) {
                tests = ImmutableList.copyOf(testList);
                // only consider non-example tests as notable.
                boolean weHaveTests = false;
                int errorCount = 0;
                int warningCount = 0;
                for (Iterator<CheckStatus> it3 = tests.iterator(); it3.hasNext();) {
                    CheckCLDR.CheckStatus status = it3.next();
                    if (!status.getType().equals(CheckStatus.exampleType)) {
                        // skip codefallback exemplar complaints (i.e. 'JPY'
                        // isn't in exemplars).. they'll show up in missing
                        if (DEBUG)
                            System.err.println("err: " + status.getMessage() + ", test: " + status.getClass() + ", cause: "
                                + status.getCause() + " on " + xpath);
                        weHaveTests = true;
                        if (status.getType().equals(CheckStatus.errorType)) {
                            errorCount++;
                        } else if (status.getType().equals(CheckStatus.warningType)) {
                            warningCount++;
                        }
                    }
                }
                if (weHaveTests) {
                    if (errorCount > 0) /* row */
                        hasErrors = true;
                    if (warningCount > 0) /* row */
                        hasWarnings = true;
                    // propagate to parent
                    if (errorCount > 0) /* row */
                        parentRow.hasErrors = true;
                    if (warningCount > 0) /* row */
                        parentRow.hasWarnings = true;
                }
                return weHaveTests;
            }

            /**
             * Convert this CandidateItem to a JSON string
             *
             * Typical sequence of events in making the json object normally sent to the client:
             *
             * DataSection.toJSONString calls DataSection.DataRow.toJSONString repeatedly for each DataRow.
             * DataSection.DataRow.toJSONString calls DataSection.DataRow.CandidateItem.toJSONString
             * repeatedly for each CandidateItem.
             *
             * This function CandidateItem.toJSONString actually gets called indirectly from this line in
             * DataSection.DataRow.toJSONString: jo.put("items", itemsJson) (NOT from the earlier loop on items.values).
             * Stack trace:
             * DataSection$DataRow$CandidateItem.toJSONString()
             * JSONObject.valueToString(Object)
             * JSONObject.toString()
             * JSONObject.valueToString(Object)
             * JSONObject.toString()
             * DataSection$DataRow.toJSONString() -- line with jo.put("items", itemsJson)
             * DataSection.toJSONString()
             * JSONObject.valueToString(Object)
             * JSONWriter.value(Object)
             * RefreshRow.jsp -- line with .key("section").value(section)
             *
             * @return the JSON string. For example: {"isBailey":false,"tests":[],"rawValue":"↑↑↑","valueHash":"4oaR4oaR4oaR","pClass":"loser",
             *      "isFallback":false,"value":"↑↑↑","isOldValue":false,"example":"<div class='cldr_example'>2345<\/div>"}
             */
            @Override
            public String toJSONString() throws JSONException {
                JSONObject j = new JSONObject()
                    .put("valueHash", getValueHash())
                    .put("rawValue", rawValue)
                    .put("value", getProcessedValue())
                    .put("example", getExample())
                    .put("isOldValue", isOldValue)
                    .put("pClass", getPClass())
                    .put("tests", SurveyAjax.JSONWriter.wrap(this.tests));                
                if (USE_CANDIDATE_HISTORY) {
                    j.put("history", history);
                }
                Set<User> theVotes = getVotes();
                if (theVotes != null && !theVotes.isEmpty()) {
                    JSONObject voteList = new JSONObject();
                    for (UserRegistry.User u : theVotes) {
                        if (u.getLevel() == VoteResolver.Level.locked) {
                            continue; // don't care
                        }
                        JSONObject uu = new JSONObject();
                        uu.put("org", u.getOrganization());
                        uu.put("level", u.getLevel());
                        Integer voteCount = null;
                        Map<User, Integer> overrides = ballotBox.getOverridesPerUser(xpath);
                        if (overrides != null) {
                            voteCount = overrides.get(u);
                        }
                        uu.put("overridedVotes", voteCount);
                        if (voteCount == null) {
                            voteCount = u.getLevel().getVotes();
                        }
                        uu.put("votes", voteCount);
                        if (userForVotelist != null) {
                            uu.put("name", u.name);
                            uu.put("email", u.email.replace("@", " (at) "));
                        }
                        voteList.put(Integer.toString(u.id), uu);
                    }
                    j.put("votes", voteList);
                }
                return j.toString();
            }

            /**
             * Convert this CandidateItem to a string.
             *
             * This function CandidateItem.toString is NOT called to make the json object normally sent to the client.
             *
             * TODO: document the purpose of this function CandidateItem.toString: who calls it; does the
             * output need to be json; is there any need to override the default toString()?
             */
            @Override
            public String toString() {
                String winner = (winningValue != null && winningValue.equals(rawValue)) ? ",winner" : "";
                return "{Item v='" + rawValue + "'" + winner + "}";
            }

            /**
             * Get the example for this CandidateItem
             *
             * @return the example, as a string, or null if examplebuilder is null
             */
            private String getExample() {
                if (examplebuilder == null) {
                    return null;
                } else {
                    return getExampleBuilder().getExampleHtml(xpath, rawValue, ExampleType.NATIVE);
                }
            }

            /**
             * Get the list of CheckStatus objects for this CandidateItem
             *
             * @return the list
             */
            @Override
            public List<CheckStatus> getCheckStatusList() {
                if (tests == null) {
                    return Collections.emptyList();
                } else {
                    return tests;
                }
            }
        } // end of class CandidateItem

        /*
         * Class DataRow continues here.
         */

        /**
         * inheritedLocale is the locale from which this DataRow inherits.
         *
         * DataRow.inheritedLocale on the server corresponds to theRow.inheritedLocale on the client.
         *
         * inheritedLocale is accessed from InterestSort.java for Partition.Membership("Missing"), otherwise it could be private.
         */
        CLDRLocale inheritedLocale = null;

        /**
         * pathWhereFound, if not null, may be, for example:
         * //ldml/numbers/currencies/currency[@type="AUD"]/displayName[@count="one"]
         * 
         * It is the inheritance path for "sideways" inheritance.
         *
         * If not null it may cause getPClass to return "alias".
         */
        private String pathWhereFound = null;

        /*
         * TODO: document confirmStatus and other members of DataRow
         */
        Status confirmStatus;

        /**
         * Calculated coverage level for this DataRow.
         */
        public int coverageValue;

        private String displayName = null;
        // these apply to the 'winning' item, if applicable
        boolean hasErrors = false;

        boolean hasMultipleProposals = false; // true if more than 1 proposal is available

        /**
         * Does this DataRow have warnings?
         */
        boolean hasWarnings = false;

        /**
         * inheritedItem is a CandidateItem representing the vetted value inherited from parent.
         *
         * Change for https://unicode.org/cldr/trac/ticket/11299 : formerly the rawValue for
         * inheritedItem was the Bailey value. Instead, now rawValue will be INHERITANCE_MARKER,
         * and the Bailey value will be stored in DataRow.inheritedValue.
         * 
         * inheritedItem is set by updateInheritedValue and by setShimTests
         */
        private CandidateItem inheritedItem = null;

        /**
         * inheritedValue is the "Bailey" value, that is, the value that this DataRow will have if
         * it inherits from another row or from another locale.
         */
        private String inheritedValue = null;

        /**
         * The winning item for this DataRow.
         */
        private CandidateItem winningItem = null;

        /**
         * The candidate items for this DataRow, stored in a Map whose keys are CandidateItem.rawValue
         * and whose values are CandidateItem objects.
         * 
         * Public for access by RefreshRow.jsp.
         */
        public Map<String, CandidateItem> items = new TreeMap<String, CandidateItem>();

        /** Cache of field hash **/
        String myFieldHash = null;

        /* parentRow - defaults to self if it is a "super" (i.e. parent without any
         * alternate)
         */
        public DataRow parentRow = this;

        /**
         * Used only in the function getPrettyPath
         */
        private String pp = null;
        
        /**
         * The pretty path for this DataRow, set by the constructor.
         * 
         *  Accessed by NameSort.java, SortMode.java, datarow_short_code.jsp
         */
        public String prettyPath = null;

        /*
         * Ordering for use in collator
         * 
         * Referenced by SortMode.java
         */
        public int reservedForSort[] = SortMode.reserveForSort();
        
        /**
         * The winning value for this DataRow
         * 
         * It gets set by resolver.getWinningValue() by the DataRow constructor.
         */
        private String winningValue;

        /**
         * the xpath id of the winner. If no winner or n/a, -1.
         *
         * @deprecated - winner is a value
         *
         * Although deprecated, still referenced in InterestSort.java
         */
        @Deprecated
        int winningXpathId = -1;

        /**
         * The xpath for this DataRow, assigned in the constructor.
         */
        private String xpath;

        /**
         * The xpathId for this DataRow, assigned in the constructor based on xpath.
         * 
         * Accessed by SortMode.java
         */
        int xpathId = -1;
        
        /**
         * The value for this DataRow in the previous release version.
         */
        private String oldValue;
        
        /**
         * The status for this DataRow in the previous release version.
         */
        private Status oldStatus;

        /**
         * The PathHeader for this DataRow, assigned in the constructor based on xpath.
         */
        private PathHeader pathHeader;

        /**
         * Create a new DataRow for the given xpath.
         * 
         * @param xpath
         */
        public DataRow(String xpath) {
            this.xpath = xpath;
            this.xpathId = sm.xpt.getByXpath(xpath);
            this.prettyPath = sm.xpt.getPrettyPath(xpathId);
            pathHeader = sm.getSTFactory().getPathHeader(xpath); // may be null

            if (ballotBox == null) {
                throw new InternalError("ballotBox is null;");
            }
            VoteResolver<String> resolver = ballotBox.getResolver(xpath);
            /*
             * Note: we set winningValue = resolver.getWinningValue() here, but we
             * may change winningValue subsequently in fixWinningValue().
             * 
             * There is a need to address the situation if winningValue gets null or empty string here.
             * Fix the bug where winningVhash is empty or undefined on client.
             * See https://unicode.org/cldr/trac/ticket/11299 "Example C".
             */
            winningValue = resolver.getWinningValue();
            confirmStatus = resolver.getWinningStatus();

            oldValue = resolver.getLastReleaseValue();
            oldStatus = resolver.getLastReleaseStatus();

            this.displayName = baselineFile.getStringValue(xpath);
        }

        /**
         * Add a new CandidateItem to this DataRow, with the given value; or, if this DataRow already
         * has an item with this value, return it.
         *
         * If the item is new, then:
         *  check whether the item is winning, and if so make winningItem point to it;
         *  check whether the item matches oldValue, and if so set isOldValue = true.
         * 
         * @param value
         * @param candidateHistory a string used for debugging and possibly also for describing to the user
         *          how/why/when/where the item was added
         * @return the new or existing item with the given value
         *
         * Sequential order in which addItem may be called (as of 2018-09-10) for a given DataRow:
         *
         * (1) For INHERITANCE_MARKER (if inheritedValue = ourSrc.getConstructedBaileyValue not null):
         *     in updateInheritedValue (called by populateFromThisXpath):
         *         inheritedItem = addItem(CldrUtility.INHERITANCE_MARKER, "inheritedItem");
         *
         * (2) For votes:
         *     in populateFromThisXpathAddItemsForVotes (called by populateFromThisXpath):
         *         CandidateItem item2 = row.addItem(avalue, "votes");
         *
         * (3) For winningValue:
         *     in populateFromThisXpath:
         *         row.addItem(row.winningValue, "winningValue");
         *
         * (4) For oldValue (if not null, not in votes, and not same as ourValue):
         *     in populateFromThisXpath:
         *         row.addItem(row.oldValue, "oldValue");
         *
         * (5) For INHERITANCE_MARKER (if not in votes and locale.getCountry isn't empty):
         *     in populateFromThisXpath:
         *         row.addItem(CldrUtility.INHERITANCE_MARKER, "getCountry");
         *
         * (6) For ourValue:
         *     in addOurValue (called by populateFromThisXpath):
         *         CandidateItem myItem = row.addItem(ourValue, "ourValue");
         */
        private CandidateItem addItem(String value, String candidateHistory) {
            /*
             * TODO: Clarify the purpose of changing null to empty string here, rather than
             * simply doing nothing, or reporting an error. There appears to be no reason to
             * add an item with null or empty value.
             */
            final String kValue = (value == null) ? "" : value;
            CandidateItem item = items.get(kValue);
            if (item != null) {
                if (USE_CANDIDATE_HISTORY) {
                    item.history += "+" + candidateHistory;
                }
                return item;
            }
            item = new CandidateItem(value);
            if (USE_CANDIDATE_HISTORY) {
                item.history = candidateHistory;
            }
            items.put(kValue, item);
            if (winningValue != null && winningValue.equals(value)) {
                winningItem = item;
            }
            if (oldValue != null && oldValue.equals(value)) {
                item.isOldValue = true;
            }
            return item;
        }

        /**
         * Calculate the hash used for HTML forms for this DataRow.
         */
        public String fieldHash() { // deterministic. No need for sync.
            if (myFieldHash == null) {
                String ret = "";
                ret = ret + "_x" + CookieSession.cheapEncode(getXpathId());
                myFieldHash = ret;
            }
            return myFieldHash;
        }

        /**
         * Return the winning (current) item for this DataRow.
         *
         * @return winningItem
         * 
         * "The type DataSection.DataRow must implement the inherited abstract method CLDRInfo.PathValueInfo.getCurrentItem()
         */
        public CandidateItem getCurrentItem() {
            return winningItem;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getIntgroup() {
            return intgroup;
        }

        /**
         * Get the locale for this DataRow
         */
        @Override
        public CLDRLocale getLocale() {
            return locale;
        }

        public String getPrettyPath() {
            if (pp == null) {
                pp = sm.xpt.getPrettyPath(xpathId);
            }
            return pp;
        }

        public PathHeader getPathHeader() {
            return pathHeader;
        }

        /**
         * Get the CandidateItem for this DataRow that has the given value.
         * 
         * @param value
         * @return the CandidateItem or null if none has that value
         * 
         * Called only from SurveyAjax.java, as "ci = pvi.getItem(candVal)".
         */
        public CandidateItem getItem(String value) {
            return items.get(value);
        }

        /**
         * Get a list of proposed items, for this DataRow.
         *
         * @deprecated use getValues() instead
         * @see #getValues()
         * 
         * Called by getVotesForUser.
         * 
         * Also called from row.jsp -- how to replace getItems() there with getValues()? Confusion over return type,
         *    Collection<? extends CandidateInfo>
         *       versus
         *    Collection<CandidateItem>
         *    
         * Also called from r_txt.jsp whose usage isn't clear.
         */
        @SuppressWarnings("unchecked")
        public Collection<CandidateItem> getItems() {
            return ((Collection<CandidateItem>) getValues());
        }

        /**
         * Get a list of proposed items, if any, for this DataRow.
         */
        @Override
        public Collection<? extends CandidateInfo> getValues() {
            return items.values();
        }

        public String getSpecialURL(WebContext ctx) {
            if (getXpath().startsWith(DataSection.EXEMPLAR_PARENT)) {
                String zone = prettyPath;
                int n = zone.lastIndexOf('/');
                if (n != -1) {
                    zone = zone.substring(0, n);
                    /*
                     *  blahblah/default/foo -> blahblah/default
                     *  ('foo' is lastType and will show up as the value)
                     */
                }
                return ctx.base() + "?" + "_=" + ctx.getLocale() + "&amp;x=timezones&amp;zone=" + zone;
            }
            return null;
        }

        /**
         * Return the CandidateItem for a particular user ID
         *
         * @param userId
         * @return
         */
        public CandidateItem getVotesForUser(int userId) {
            UserRegistry.User infoForUser = sm.reg.getInfo(userId);
            /* see gatherVotes - getVotes() is populated with a
             * set drawn from the getInfo() singletons.
             */
            if (infoForUser == null) {
                return null;
            }
            for (CandidateItem item : getItems()) {
                Set<User> votes = item.getVotes();
                if (votes != null && votes.contains(infoForUser)) {
                    return item;
                }
            }
            return null; /* not found. */
        }

        public String getWinningValue() {
            return winningValue;
        }

        /**
         * Get the xpath for this DataRow
         */
        @Override
        public String getXpath() {
            return xpath;
        }

        public int getXpathId() {
            return xpathId;
        }

        public boolean isName() {
            return NAME_TYPE_PATTERN.matcher(prettyPath).matches();
        }

        /**
         * 
         * @param ctx
         * @param canModify
         * @param zoomedIn
         * @param specialUrl
         * @return
         * 
         * Called only by row.jsp
         */
        public String itemTypeName(WebContext ctx, boolean canModify, boolean zoomedIn, String specialUrl) {
            StringBuilder sb = new StringBuilder();
            String disputeIcon = "";
            if (canModify) {
                if (DisputePageManager.getOrgDisputeCount(ctx.session.user.voterOrg(), getLocale(), getXpathId())) {
                    disputeIcon = ctx.iconHtml("disp", "Vetter Dispute");
                }
            }
            sb.append("<tt title='" + sm.xpt.getPrettyPath(getXpathId()) + "' >");
            String typeShown = prettyPath.replaceAll("/", "/\u200b");
            if (!zoomedIn) {
                if (specialUrl != null) {
                    sb.append("<a class='notselected' " + ctx.atarget() + " href='" + specialUrl + "'>" + typeShown + disputeIcon
                        + "</a>");
                } else {
                    sb.append(sm.fora.getForumLink(ctx, this, parentRow.getXpathId(), typeShown + disputeIcon));
                }
            } else {
                sb.append(typeShown + disputeIcon);
            }
            sb.append("</tt>");
            return sb.toString();
        }

        /**
         * If inheritedItem is null, possibly create inheritedItem; otherwise do nothing.
         *
         * inheritedItem is normally null when setShimTests is called by populateFromThisXpath,
         * unless setShimTests has already been called by ensureComplete, for some timezones.
         *
         * A Shim is a candidate item which does not correspond to actual XML
         * data, but is synthesized.
         *
         * @param base_xpath
         * @param base_xpath_string
         * @param checkCldr
         *
         * Called by populateFromThisXpath (if isExtraPath), and by ensureComplete (for timezones)
         */
        private void setShimTests(int base_xpath, String base_xpath_string, TestResultBundle checkCldr) {
            if (inheritedItem == null) {
                CandidateItem shimItem = new CandidateItem(null);
                List<CheckStatus> iTests = new ArrayList<CheckStatus>();
                checkCldr.check(base_xpath_string, iTests, null);
                if (!iTests.isEmpty()) {
                    // Got a bite.
                    if (shimItem.setTests(iTests)) {
                        // had valid tests
                        inheritedItem = shimItem;
                    }
                }
            }
        }

        /**
         * Does the DataSection, to which this DataRow belongs, have examples?
         *
         * @return true or false
         * 
         * TODO: Why is this a method of DataRow, when the field of the same name that it returns
         * is a field of DataSection??
         * 
         * Called only by row.jsp
         */
        public boolean hasExamples() {
            return hasExamples;
        }

        /**
         * @param uf
         * @param zoomedIn
         * @param exampleContext
         * @param topCurrent
         * @return
         */
        public String getBaseExample(UserLocaleStuff uf, boolean zoomedIn, ExampleContext exampleContext, CandidateItem topCurrent) {
            String baseExample;
            // Prime the Pump - Native must be called first.
            if (topCurrent != null) {
                /* ignored */uf.getExampleGenerator().getExampleHtml(getXpath(), topCurrent.rawValue,
                    exampleContext, ExampleType.NATIVE);
            } else {
                // no top item, so use NULL
                /* ignored */uf.getExampleGenerator().getExampleHtml(getXpath(), null,
                    exampleContext, ExampleType.NATIVE);
            }

            baseExample = sm.getBaselineExample().getExampleHtml(xpath, getDisplayName(),
                exampleContext, ExampleType.ENGLISH);
            return baseExample;
        }

        /**
         * Get the status icon for this DataRow
         *
         * @param ctx the WebContext
         * @return the status icon, as a string
         *
         * Called only by row.jsp
         */
        public String getStatusIcon(WebContext ctx) {
            String statusIcon = "";
            if (hasErrors) {
                statusIcon = ctx.iconHtml("stop", "Errors - please zoom in");
            } else if (hasWarnings) {
                statusIcon = ctx.iconHtml("warn", "Warnings - please zoom in");
            }
            return statusIcon;
        }

        /**
         * Get the row class for this DataRow
         *
         * @return "vother, "error", or "warning"
         */
        public String getRowClass() {
            // calculate the class of data items
            String rclass = "vother";
            if (hasErrors) {
                rclass = "error";
            } else if (hasWarnings) {
                rclass = "warning";
            }
            return rclass;
        }

        /**
         * Get an icon indicating whether the user has voted for this DataRow
         *
         * @return the html string
         */
        public String getVotedIcon(WebContext ctx) {
            String votedIcon = ctx.iconHtml("bar0", "You have not yet voted on this item.");
            if (userHasVoted(ctx.userId())) {
                votedIcon = ctx.iconHtml("vote", "You have already voted on this item.");
            }
            return votedIcon;
        }

        /**
         * Get an icon indicating the draft status for this DataRow
         *
         * @return the html string
         */
        public String getDraftIcon(WebContext ctx) {
            String draftIcon = "";
            switch (confirmStatus) {
            case approved:
                draftIcon = ctx.iconHtml("okay", "APPROVED");
                break;
            case contributed:
                draftIcon = ctx.iconHtml("conf", "CONTRIBUTED");
                break;
            case provisional:
                draftIcon = ctx.iconHtml("conf2", "PROVISIONAL");
                break;
            case unconfirmed:
                draftIcon = ctx.iconHtml("ques", "UNCONFIRMED");
                break;
            case missing:
                draftIcon = ctx.iconHtml("bar1", "MISSING");
            }
            return draftIcon;
        }

        /**
         * Show a row of limited data.
         *
         * @param ctx the WebContext
         */
        void showDataRowShort(WebContext ctx) {
            ctx.put(WebContext.DATA_ROW, this);
            String whichFragment = (String) ctx.get(SurveyMain.DATAROW_JSP);
            if (whichFragment == null) {
                whichFragment = SurveyMain.DATAROW_JSP_DEFAULT;
            }
            ctx.includeFragment(whichFragment);
        }

        /**
         * Show the voting results for this DataRow
         *
         * @param ctx the WebContext
         *
         * Called by RefreshRow.jsp
         */
        public void showVotingResults(WebContext ctx) {
            BallotBox<User> ballotBox = sm.getSTFactory().ballotBoxForLocale(locale);
            ctx.put("ballotBox", ballotBox);
            ctx.put("resolver", ballotBox.getResolver(getXpath()));
            ctx.includeFragment("show_voting.jsp");
        }

        /**
         * Convert this DataRow to a string.
         */
        @Override
        public String toString() {
            return "{DataRow n='" + getDisplayName() + "', x='" + xpath + "', item#='" + items.size() + "'}";
        }

        /**
         * After constructing this new DataRow, change its winningValue if needed for consistency
         * with inheritedValue, ourValue, and ourValueIsInherited. Also in some circumstances fix
         * inheritedValue if it is null.
         *
         * We may need to fix winningValue if:
         *     (A) it's null (due to bugs, or ...?)
         *     (B) it should be INHERITANCE_MARKER but isn't
         *
         * The circumstances under which winningValue and/or ourValue may be null aren't very clear;
         * sometimes "extra paths" are involved. This should be documented more clearly, or any bugs
         * that result in bogus null should be fixed.
         *
         * When "soft" inheritance is currently winning, we always want winningValue to be INHERITANCE_MARKER,
         * but resolver.getWinningValue() may return lastReleaseValue (not INHERITANCE_MARKER) if there are
         * currently no votes. TODO: determine when, if ever, winningValue should be changed to INHERITANCE_MARKER
         * if winningValue equals inheritedValue.
         *
         * We have two ways of getting the currently winning value for a row:
         *     (1) winningValue = resolver.getWinningValue()
         *     (2) ourValue = ourSrc.getStringValue(xpath)
         * Often these are equal. They can be different when one or the other (but not both) is null,
         * or when winningValue is INHERITANCE_MARKER. ourValue is never (?) INHERITANCE_MARKER.
         *
         * ourValue and winningValue can also be different even when neither is null or INHERITANCE_MARKER,
         * and that is especially perplexing. This happens, for example at v#/pt_PT/Buddhist
         * ourValueIsInherited = true
         * ourValue = "d MMM y G" and winningValue = "dd/MM/y G" are DIFFERENT
         * xpath = //ldml/dates/calendars/calendar[@type="buddhist"]/dateFormats/dateFormatLength[@type="medium"]/dateFormat[@type="standard"]/pattern[@type="standard"]
         *
         * Another example, also at v#/pt_PT/Buddhist
         * ourValueIsInherited = false
         * ourValue = E, d 'de' MMM – E, d 'de' MMM 'de' y G and winningValue = E, d – E, d 'de' MMM 'de' y G
         * xpath = //ldml/dates/calendars/calendar[@type="buddhist"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id="yMMMEd"]/greatestDifference[@id="d"]
         *
         * Other urls that may produce unexpected results if debugging code is added in this function:
         *
         * TODO: Fix resolver.getWinningValue to make this function unnecessary. Questions:
         *    (1) What does it mean, and what should be done, when ourValue and winningValue aren't equal?
         *    (2) How to recognize when winningValue needs changing to INHERITANCE_MARKER (ideally not here but in getWinningValue)?
         * Note: When winningValue equals ourValue and ourValueIsInherited is true, it would be tempting to infer that
         * winningValue should be INHERITANCE_MARKER, and to expect inheritedValue and ourValue should be equal.
         * Code like this could be considered:
         *
         * if (ourValue.equals(winningValue) && ourValueIsInherited) {
         *     winningValue = CldrUtility.INHERITANCE_MARKER;
         *     assert(inheritedValue.equals(ourValue));
         * }
         *
         * (We already have something similar below, but only if winningValue == null && inheritedValue == null.)
         *
         * However, there's an alternative explanation in such cases, which is that winningValue represents a
         * winning "hard" vote for the explicit value, while ourValue represents a losing "soft" vote for inheritance.
         *
         * Note: due to the existence of this function, the client should use theRow.winningValue, not vr.winningValue. 
         *
         * http://localhost:8080/cldr-apps/v#/en_CA/Gregorian/
         *
         * Reference: https://unicode.org/cldr/trac/ticket/11299
         *
         * @param ourValue the String that was obtained as ourSrc.getStringValue(xpath)
         * @param ourValueIsInherited the boolean that was obtained as !(sourceLocale.equals(locale.toString()))
         */
        private void fixWinningValue(String ourValue, boolean ourValueIsInherited) {
            if (winningValue == null) {
                if (inheritedValue != null) {
                    /*
                     * Formerly something equivalent to this was done on the client. TODO: explain the justification; why
                     * treat soft inheritance as the default, rather than, for example, the old (last release) value? 
                     * Anyway, if this happens, it should happen here on the server, not on the client.
                     * And, preferably resolver.getWinningValue() should never return null.
                     */
                    winningValue = CldrUtility.INHERITANCE_MARKER;
                } else if (ourValue != null) {
                    /*
                     * This happens, for example at v#/en/Symbols
                     * ourValue = E
                     * xpath = //ldml/numbers/symbols[@numberSystem="gong"]/exponential
                     * http://localhost:8080/cldr-apps/v#/aa/Numbering_Systems/
                     * TODO: fix or explain this seeming contradiction, and decide what to do about it. For now,
                     * use ourValue and ourValueIsInherited instead of winningValue, with the rationale that
                     * resolver.getWinningValue() and ourSrc.getStringValue(xpath) are both supposed to give the
                     * winning value, but the former might be buggy (null) in some circumstances where the latter isn't. 
                     */
                    if (ourValueIsInherited) {
                        /***
                        System.out.println("Debugging in fixWinningValue: winningValue is null" +
                            "; ourValueIsInherited = " + ourValueIsInherited +
                            "; setting winningValue = INHERITANCE_MARKER; inheritedValue = ourValue = " + ourValue +
                            "; xpath = " + xpath);
                        ***/
                        winningValue = CldrUtility.INHERITANCE_MARKER;
                        inheritedValue = ourValue;
                    } else {
                        /***
                        System.out.println("Debugging in fixWinningValue: winningValue is null" +
                            "; ourValueIsInherited = " + ourValueIsInherited +
                            "; setting winningValue = ourValue; ourValue = " + ourValue +
                            "; xpath = " + xpath);
                        ***/
                        winningValue = ourValue;
                    }
                } else {
                    /*
                     * TODO: Fix the bugs that lead to winningValue being null, which happens often, but never should.
                     * In the meantime, the client should ALWAYS get a winningVhash, corresponding to winningValue and
                     * winningItem, even if it's a placeholder or error message, since determination of the winning value
                     * is the server's responsibility. Sometimes null winningValue goes with "Status:Missing" (black X icon);
                     * even then an inherited value should normally be available as fallback.
                     * 
                     * Note that setting a non-null winningValue here affects subsequent creation of winningItem.
                     */
                    /***
                    System.out.println("Debugging in fixWinningValue: ERROR_NO_WINNING_VALUE; xpath = " + xpath);
                    ***/
                    winningValue = ERROR_NO_WINNING_VALUE;
                }
            }
        }

        /**
         * Calculate the inherited item for this DataRow, possibly including tests;
         * possibly set some fields in the DataRow, which may include inheritedValue,
         * inheritedItem, inheritedLocale, pathWhereFound
         *
         * @param ourSrc the CLDRFile
         * @param checkCldr the tests to use
         *
         * Called only by populateFromThisXpath, which is a method of DataSection.
         * 
         * Reference: Distinguish two kinds of votes for inherited value in Survey Tool
         *     https://unicode.org/cldr/trac/ticket/11299
         * This function formerly created a CandidateItem with value equal to the Bailey value,
         * which made it look like a "hard/explicit" vote for the Bailey value, NOT a "soft/implicit"
         * vote for inheritance, which would have value equal to INHERITANCE_MARKER. Horrible confusion
         * was the result. This function has been changed to set the value to INHERITANCE_MARKER, and to store
         * the actual Bailey value in the inheritedValue field of DataRow.
         *
         * TODO: Get rid of, or merge with, the code that currently does 'row.addItem(CldrUtility.INHERITANCE_MARKER, "getCountry")' in populateFromThisXpath.
         * 
         * Normally (always?) inheritedItem is null when this function is called; however, in principle
         * it may be possible that inheritedItem isn't null due to ensureComplete calling setShimTests.
         */
        private void updateInheritedValue(CLDRFile ourSrc, TestResultBundle checkCldr) {
            long lastTime = System.currentTimeMillis();

            /*
             * Set the inheritedValue field of the DataRow containing this CandidateItem.
             * Also possibly set the inheritedLocale and pathWhereFound fields of the DataRow.
             *
             * TODO: fix bug, return value of getConstructedBaileyValue here gives
             * inheritedValue = "Chineesisch (Veräifachti Chineesischi Schrift)"
             * but getResolverInternal gets baileyValue = "Veräifachts Chineesisch".
             * That's for http://localhost:8080/cldr-apps/v#/gsw_FR/Languages_A_D/3f16ed8804cebb7d
             */
            Output<String> inheritancePathWhereFound = new Output<String>(); // may become pathWhereFound
            Output<String> localeWhereFound = new Output<String>(); // may be used to construct inheritedLocale
            inheritedValue = ourSrc.getConstructedBaileyValue(xpath, inheritancePathWhereFound, localeWhereFound);
            
            if (TRACE_TIME) {
                System.err.println("@@1:" + (System.currentTimeMillis() - lastTime));
            }
            if (inheritedValue == null) {
                /*
                 * No inherited value.
                 *
                 * This happens often. For example, v#/en/Alphabetic_Information
                 * xpath = //ldml/characters/exemplarCharacters[@type="index"]
                 * In the caller, ourValueIsInherited is false and ourValue isn't null
                 * 
                 * Another example: v#/pt_PT/Gregorian
                 * xpath = //ldml/dates/calendars/calendar[@type="gregorian"]/dateTimeFormats/availableFormats/dateFormatItem[@id="yMMMEEEEd"]
                 * ourValueIsInherited = false; ourValue = "EEEE, d/MM/y"; isExtraPath = false
                 *
                 * TODO: what are the implications when ourSrc.getConstructedBaileyValue has returned null?
                 * Unless we're at root, shouldn't there always be a non-null inheritedValue here?
                 * See https://unicode.org/cldr/trac/ticket/11299
                 */
                // System.out.println("Warning: no inherited value in updateInheritedValue; xpath = " + xpath);
            } else {
                /*
                 * Unless this DataRow already has an item with value INHERITANCE_MARKER,
                 * add a new item, to be the inheritedItem of this DataRow, with value INHERITANCE_MARKER.
                 *
                 * Call addItem even if item with this value already exists, for simplicity and to update inheritedItem.history.
                 *
                 * Set inheritedItem = the item with value INHERITANCE_MARKER.
                 */                    
                inheritedItem = addItem(CldrUtility.INHERITANCE_MARKER, "inheritedItem");

                if (TRACE_TIME) {
                    System.err.println("@@2:" + (System.currentTimeMillis() - lastTime));
                }
                inheritedLocale = CLDRLocale.getInstance(localeWhereFound.value);

                if (inheritancePathWhereFound.value != null && !inheritancePathWhereFound.value.equals(xpath)) {
                    pathWhereFound = inheritancePathWhereFound.value;

                    if (TRACE_TIME) {
                        System.err.println("@@3:" + (System.currentTimeMillis() - lastTime));
                    }

                    /*
                     * TODO: does xpathToBaseXpathId have any useful side-effect here? If not,
                     * delete this call to it. Formerly the return value was assigned to
                     * aliasFromXpath, whose value was unused.
                     */
                    sm.xpt.xpathToBaseXpathId(inheritancePathWhereFound.value);

                    if (TRACE_TIME) {
                        System.err.println("@@4:" + (System.currentTimeMillis() - lastTime));
                    }
                }                    
            }
            if ((checkCldr != null) && (inheritedItem != null) && (inheritedItem.tests == null)) {
                if (TRACE_TIME) {
                    System.err.println("@@5:" + (System.currentTimeMillis() - lastTime));
                }

                List<CheckStatus> iTests = new ArrayList<CheckStatus>();

                checkCldr.check(xpath, iTests, inheritedValue);

                if (TRACE_TIME) {
                    System.err.println("@@6:" + (System.currentTimeMillis() - lastTime));
                }
                
                if (!iTests.isEmpty()) {
                    inheritedItem.setTests(iTests);
                }
            }
            if (TRACE_TIME) {
                System.err.println("@@7:" + (System.currentTimeMillis() - lastTime));
            }
        }

        /**
         * Has the given user voted or not?
         *
         * @param userId
         * @return true if user has voted at all, false otherwise. Return
         *         false if user changes their vote back to no opinion (abstain).
         */
        public boolean userHasVoted(int userId) {
            return ballotBox.userDidVote(sm.reg.getInfo(userId), getXpath());
        }

        /**
         * Convert this DataRow to a JSON string.
         *
         * This function DataSection.DataRow.toJSONString plays a key role in preparing data for the client (survey.js).
         *
         * Typical sequence of events:
         *
         * DataSection.toJSONString calls DataSection.DataRow.toJSONString repeatedly for each DataRow.
         * DataSection.DataRow.toJSONString calls DataSection.DataRow.CandidateItem.toJSONString
         * repeatedly for each CandidateItem.
         *
         * TODO: It would be cleaner, and might be more testable and less bug-prone, to separate
         * the construction of the JSON from the completion and validation of the DataRow.
         * It might be best to make all the values needed by client into fields of the DataRow,
         * if they aren't already; or make them fields of a new object, and then we could do
         * consistency checking on that object. The DataRow object itself is complex; the test
         * would be specifically for the data representing what becomes "theRow" on the client,
         * AFTER that data has all been prepared/derived. See checkDataRowConsistency for work
         * in progress.
         */
        @Override
        public String toJSONString() throws JSONException {

            try {
                String winningVhash = DataSection.getValueHash(winningValue);

                String voteVhash = "";
                if (userForVotelist != null) {
                    String ourVote = ballotBox.getVoteValue(userForVotelist, xpath);
                    if (ourVote != null) {
                        CandidateItem voteItem = items.get(ourVote);
                        if (voteItem != null) {
                            voteVhash = voteItem.getValueHash();
                        }
                    }
                }

                JSONObject itemsJson = new JSONObject();
                for (CandidateItem i : items.values()) {
                    String key = i.getValueHash();
                    if (itemsJson.has(key)) {
                        System.out.println("Error: value hash key " + key + " is duplicate");
                    }
                    itemsJson.put(key, i);
                }

                String displayExample = null;
                ExampleBuilder b = getExampleBuilder();
                if (b != null) {
                    displayExample = b.getExampleHtml(xpath, displayName, ExampleType.ENGLISH);
                }

                String code = "?";
                PathHeader ph = getPathHeader();
                if (ph != null) {
                    code = ph.getCode();
                }

                VoteResolver<String> resolver = ballotBox.getResolver(xpath);
                JSONObject voteResolver = SurveyAjax.JSONWriter.wrap(resolver);

                boolean rowFlagged = sm.getSTFactory().getFlag(locale, xpathId);

                String xpstrid = XPathTable.getStringIDString(xpath);

                StatusAction statusAction = getStatusAction();

                Map<String, String> extraAttributes = getNonDistinguishingAttributes();

                boolean hasVoted = (userForVotelist != null) ? userHasVoted(userForVotelist.id) : false;

                String inheritedXpid = (pathWhereFound != null) ? XPathTable.getStringIDString(pathWhereFound) : null;

                boolean canFlagOnLosing = (resolver.getRequiredVotes() == VoteResolver.HIGH_BAR);

                String dir = (ph.getSurveyToolStatus() == SurveyToolStatus.LTR_ALWAYS) ? "ltr" : null;

                if (DEBUG) {
                    checkDataRowConsistency();
                }

                /*
                 * When the second argument to JSONObject.put is null, then the key is removed if present.
                 * Here that means that the client will not receive anything for that key!
                 */
                JSONObject jo = new JSONObject();
                /*
                 * At last count (2018-09-10) there are 22 key/value pairs here, of which 8 are fields
                 * of DataRow, and 14 are local variables in this function. Some of the local variables
                 * like voteResolver and itemsJson are specially "wrapped up" for json; maybe with those
                 * as exceptions, the rest should become fields of DataRow to facilitate consistency
                 * checking without sending them all as parameters to checkDataRowConsistency.
                 * Anyway, try to keep the names same on server and client, and avoid using function calls
                 * or compound expressions for the arguments passed to jo.put here. 
                 */
                jo.put("canFlagOnLosing", canFlagOnLosing);
                jo.put("code", code);
                jo.put("confirmStatus", confirmStatus);
                jo.put("coverageValue", coverageValue);
                jo.put("dir", dir);
                jo.put("displayExample", displayExample);
                jo.put("displayName", displayName);
                jo.put("extraAttributes", extraAttributes);
                jo.put("hasVoted", hasVoted);
                jo.put("inheritedLocale", inheritedLocale);
                jo.put("inheritedValue", inheritedValue);
                jo.put("inheritedXpid", inheritedXpid);
                jo.put("items", itemsJson);
                jo.put("rowFlagged", rowFlagged);
                jo.put("statusAction", statusAction);
                jo.put("voteResolver", voteResolver);
                jo.put("voteVhash", voteVhash);
                jo.put("winningValue", winningValue);
                jo.put("winningVhash", winningVhash);
                jo.put("xpath", xpath);
                jo.put("xpathId", xpathId);
                jo.put("xpstrid", xpstrid);
                return jo.toString();
            } catch (Throwable t) {
                SurveyLog.logException(t, "Exception in DataRow.toJSONString of " + this);
                throw new JSONException(t);
            }
        }

        /**
         * Check whether the data for this row is consistent.
         *
         * This function may serve as a basis for more automated testing, and a place
         * to put some debugging code while work is on progress.
         *
         * TODO: clarify what needs to be checked here for completeness+consistency.
         * References: https://unicode.org/cldr/trac/ticket/11299
         * and https://unicode.org/cldr/trac/ticket/11238
         *
         * Compare fixWinningValue(); here we're not fixing (changing) anything, only
         * reporting.
         *
         * JSON sent from server to client must be COMPLETE and CONSISTENT
         * Establish and test rules like:
         *   winningVHash cannot be empty;
         *   There must be an item that corresponds to the winningValue;
         *   There must be an item with INHERITANCE_MARKER;
         *   ...
         *
         * We may get winningValue and inheritedValue both null,
         * such as for "http://localhost:8080/cldr-apps/v#/fr_CA/CAsia/"
         * xpath = //ldml/dates/timeZoneNames/metazone[@type="Kyrgystan"]/long/generic
         * xpath = //ldml/dates/timeZoneNames/metazone[@type="Qyzylorda"]/long/generic
         * These are "extra" paths.
         *
         * Note: client doesn't use DataRow.winningValue currently. It uses winningVhash. Also,
         * client uses vr.winningValue, a.k.a. theRow.voteResolver.winningValue, which gets
         * set by wrap() in SurveyAjax.java in "put("winningValue", r.getWinningValue())"...
         * Need to check voteResolver.winningValue for consistency too!
         *
         * A bug may occur on the client if there is no item for winningVhash.
         *
         * See updateRowProposedWinningCell in survey.js:
         * addVitem(children[config.proposedcell], tr, theRow, theRow.items[theRow.winningVhash], cloneAnon(protoButton));
         *
         * We get an error "item is undefined" in addVitem if theRow.items[theRow.winningVhash] isn't defined.
         *
         * Get that, for example, at http://localhost:8080/cldr-apps/v#/fr_CA/CAsia/
         */
        private void checkDataRowConsistency() {
            if (winningValue == null) {
                System.out.println("Error in checkDataRowConsistency: winningValue is null; xpath = " + xpath +
                    "; inheritedValue = " + inheritedValue);
            }
            if (getItem(winningValue) == null) {
                System.out.println("Error in checkDataRowConsistency: getItem(winningValue) is null; xpath = " + xpath +
                    "; inheritedValue = " + inheritedValue);
            }
            /*
             * It is probably a bug if we have an item with INHERITANCE_MARKER
             * but inheritedLocale and pathWhereFound (basis of inheritedXpid) are both null.
             * This happens with "example C" in https://unicode.org/cldr/trac/ticket/11299#comment:15 .
             * See showItemInfoFn in survey.js
             */
            if (inheritedItem != null && inheritedLocale == null && pathWhereFound == null) {
                System.out.println("Error in checkDataRowConsistency: inheritedItem without inheritedLocale or pathWhereFound" +
                    "; xpath = " + xpath + "; inheritedValue = " + inheritedValue);
            }
            /*
             * Note: we could also report if ERROR_NO_WINNING_VALUE.equals(winningValue) here; or uncomment
             * debugging code in fixWinningValue.
             */
        }

        /*
         * A Map used only in getNonDistinguishingAttributes
         */
        private Map<String, String> nonDistinguishingAttributes = null;

        /*
         * A boolean used only in getNonDistinguishingAttributes
         */
        private boolean checkedNDA = false;

        /**
         * Get the map of non-distinguishing attributes for this DataRow
         *
         * @return the map
         *
         * Called only by DataRow.toJSONString
         */
        private Map<String, String> getNonDistinguishingAttributes() {
            if (checkedNDA == false) {
                String fullDiskXPath = diskFile.getFullXPath(xpath);
                nonDistinguishingAttributes = sm.xpt.getUndistinguishingElementsFor(fullDiskXPath, new XPathParts(null, null));
                checkedNDA = true;
            }
            return nonDistinguishingAttributes;
        }

        /**
         * Get the StatusAction for this DataRow
         *
         * @return the StatusAction
         */
        public StatusAction getStatusAction() {
            // null because this is for display.
            return SurveyMain.phase().getCPhase()
                .getShowRowAction(this, InputMethod.DIRECT, getPathHeader().getSurveyToolStatus(), userForVotelist);
        }

        /**
         * Get the last release value for this DataRow
         */
        @Override
        public String getLastReleaseValue() {
            return oldValue;
        }

        /**
         * Get the last release value for this DataRow
         */
        @Override
        public Status getLastReleaseStatus() {
            return oldStatus;
        }

        /**
         * Get the coverage level for this DataRow
         */
        @Override
        public Level getCoverageLevel() {
            // access the private method of the enclosing class
            return getCoverageInfo().getCoverageLevel(getXpath(), locale.getBaseName());
        }

        /**
         * Was there at least one vote for this DataRow at the end of data submission, or is
         * there a vote now?
         *
         * @return true if there was at least one vote
         *
         * TODO: add check for whether there was a vote in data submission.
         */
        @Override
        public boolean hadVotesSometimeThisRelease() {
            // for (CandidateInfo candidateInfo : getValues()) {
            // if (candidateInfo.getUsersVotingOn().size() != 0) {
            // return true;
            // }
            // }
            return ballotBox.hadVotesSometimeThisRelease(xpathId);
        }
    }

    /*
     * The user somehow related to this DataSection?
     * TODO: clarify what userForVotelist means
     */
    private User userForVotelist = null;

    /**
     * Set the user and the file for this DataSection
     *
     * Somehow related to vote list?
     *
     * @param u the User
     * @param f the CLDRFile
     */
    public void setUserAndFileForVotelist(User u, CLDRFile f) {
        userForVotelist = u;
        getExampleBuilder(f);
    }

    /**
     * A DisplaySet represents list of rows, in sorted and divided order.
     */
    public static class DisplaySet implements JSONString {
        public boolean canName = true; // can use the 'name' view?
        public boolean isCalendar = false;
        public boolean isMetazones = false;

        /**
         * Partitions divide up the rows into sets, such as 'proposed',
         * 'normal', etc. The 'limit' is one more than the index number of the
         * last item. In some cases, there is only one partition, and its name
         * is null.
         *
         * Display group partitions. Might only contain one entry: {null, 0, <end>}.
         * Otherwise, contains a list of entries to be named separately
         */
        public Partition partitions[];

        DataRow rows[]; // list of rows in sorted order
        SortMode sortMode = null;

        /**
         * Create a DisplaySet object
         *
         * @param myRows
         *            the original rows
         * @param myDisplayRows
         *            the rows in display order (?)
         * @param sortMode
         *            the sort mode to use
         */
        public DisplaySet(DataRow[] myRows, SortMode sortMode, Partition partitions[]) {
            this.sortMode = sortMode;
            this.partitions = partitions;
            rows = myRows;
        }

        /**
         * Show a "skip box" for this DisplaySet
         *
         * TODO: explain what a "skip box" is
         *
         * @param ctx the WebContext
         * @param skip an integer, meaning what?
         *
         * @return an integer related to the "skip" parameter
         *
         * Called only by DataSection.showSection
         */
        private int showSkipBox(WebContext ctx, int skip) {
            int total = size();
            DataRow displayList[] = null;
            if (this != null) {
                displayList = rows;
            }
            ctx.println("<div class='pager' style='margin: 2px'>");

            // TODO: replace with ctx.fieldValue("skip",-1)
            if (skip <= 0) {
                skip = 0;
            }
            // calculate nextSkip
            int from = skip + 1;
            int to = from + ctx.prefCodesPerPage() - 1;
            if (to >= total) {
                to = total;
            }

            ctx.println("<div style='float: right;'>Items " + from + " to " + to + " of " + total + "</div>");
            ctx.println("<p class='hang' > <b>Sorted:</b>  ");

            showSkipBox_menu(ctx, SurveyMain.PREF_SORTMODE_CODE, "Code");
            if (isCalendar) {
                showSkipBox_menu(ctx, SurveyMain.PREF_SORTMODE_CODE_CALENDAR, "Type");
            }
            if (isMetazones) {
                showSkipBox_menu(ctx, SurveyMain.PREF_SORTMODE_METAZONE, "Type");
            }
            showSkipBox_menu(ctx, SurveyMain.PREF_SORTMODE_WARNING, "Priority");
            if (canName) {
                showSkipBox_menu(ctx, SurveyMain.PREF_SORTMODE_NAME, SurveyMain.BASELINE_LANGUAGE_NAME + "-" + "Name");
            }
            WebContext subCtx = (WebContext) ctx.clone();
            if (skip > 0) {
                subCtx.setQuery("skip", new Integer(skip).toString());
            }

            ctx.println("</p>");

            // Print navigation
            if (total >= (ctx.prefCodesPerPage())) {
                int prevSkip = skip - ctx.prefCodesPerPage();
                if (prevSkip < 0) {
                    prevSkip = 0;
                }
                ctx.print("<p class='hang'>");
                if (skip <= 0) {
                    ctx.print("<span class='pagerl_inactive'>\u2190&nbsp;prev"
                        + "</span>&nbsp;");
                } else {
                    ctx.print("<a class='pagerl_active' href=\"" + ctx.url() + ctx.urlConnector() + "skip="
                        + new Integer(prevSkip) + "\">" + "\u2190&nbsp;prev");
                    ctx.print("</a>&nbsp;");
                }
                int nextSkip = skip + ctx.prefCodesPerPage();
                if (nextSkip >= total) {
                    nextSkip = -1;
                    if (total >= (ctx.prefCodesPerPage())) {
                        ctx.println(" <span class='pagerl_inactive' >" + "next&nbsp;"
                            + "\u2192" + "</span>");
                    }
                } else {
                    ctx.println(" <a class='pagerl_active' href=\"" + ctx.url() + ctx.urlConnector() + "skip="
                        + new Integer(nextSkip) + "\">" + "next&nbsp;"
                        + "\u2192" + "</a>");
                }
                ctx.print("</p>");
            }

            if (total >= (ctx.prefCodesPerPage())) {
                if (partitions.length > 1) {
                    ctx.println("<table summary='navigation box' style='border-collapse: collapse'><tr valign='top'><td>");
                }
                if (skip > 0) {
                    if (skip >= total) {
                        skip = 0;
                    }
                }
                if (partitions.length > 1) {
                    ctx.println("</td>");
                }
                for (int j = 0; j < partitions.length; j++) {
                    if (j > 0) {
                        ctx.println("<tr valign='top'><td></td>");
                    }
                    if (partitions[j].name != null) {
                        ctx.print("<td  class='pagerln' align='left'><p style='margin-top: 2px; margin-bottom: 2px;' class='hang'><b>"
                            + partitions[j].name + ":</b>");
                    }
                    int ourStart = partitions[j].start;
                    int ourLimit = partitions[j].limit;
                    for (int i = ourStart; i < ourLimit; i = (i - (i % ctx.prefCodesPerPage())) + ctx.prefCodesPerPage()) {
                        // skip at the top of the page
                        int pageStart = i - (i % ctx.prefCodesPerPage());

                        int end = pageStart + ctx.prefCodesPerPage() - 1;
                        if (end >= ourLimit) {
                            end = ourLimit - 1;
                        }
                        // \u2013 = --
                        // \u2190 = <--
                        // \u2026 = ...
                        boolean isus = (pageStart == skip);
                        if (isus) {
                            if (((i != pageStart) || (i == 0)) && (partitions[j].name != null)) {
                                ctx.print(" <b><a class='selected' style='text-decoration:none' href='#" + partitions[j].name
                                    + "'>");
                            } else {
                                ctx.println(" <b class='selected'>");
                            }
                        } else {
                            ctx.print(" <a class='notselected' href=\"" + ctx.url() + ctx.urlConnector() + "skip=" + pageStart);
                            if ((i != pageStart) && (partitions[j].name != null)) {
                                ctx.println("#" + partitions[j].name);
                            }
                            ctx.println("\">"); // skip to the pageStart
                        }
                        if (displayList != null) {
                            String iString = sortMode.getDisplayName(displayList[i]);
                            if (iString.length() > SurveyMain.PAGER_SHORTEN_WIDTH) {
                                iString = iString.substring(0, SurveyMain.PAGER_SHORTEN_WIDTH) + "\u2026";
                            }
                            ctx.print(iString);
                        } else {
                            ctx.print("" + (i + 1));
                            ctx.print("\u2013" + (end + 1));
                        }
                        if (isus) {
                            if (((i != pageStart) || (i == 0)) && (partitions[j].name != null)) {
                                ctx.print("</a></b> ");
                            } else {
                                ctx.println("</b> ");
                            }
                        } else {
                            ctx.println("</a> ");
                        }
                    }
                    if (partitions.length > 1) {
                        ctx.print("</p>");
                    }
                    if (partitions.length > 1) {
                        ctx.println("</td></tr>");
                    }
                }
                if (partitions.length > 1) {
                    ctx.println("</table>");
                }
            } // no multiple pages
            else {
                if (partitions.length > 1) {
                    ctx.println("<br><b>Items:</b><ul>");
                    for (int j = 0; j < partitions.length; j++) {
                        ctx.print("<b><a class='selected' style='text-decoration:none' href='#" + partitions[j].name + "'>");
                        ctx.print(partitions[j].name + "</a></b> ");
                        if (j < partitions.length - 1) {
                            ctx.println("<br>");
                        }
                    }
                    ctx.println("</ul>");
                }
            }
            ctx.println("</div>");
            return skip;
        }

        /**
         * Show a "skip box menu" for this DisplaySet
         *
         * @param ctx
         * @param aMode
         * @param aDesc
         *
         * Called only by showSkipBox
         */
        private void showSkipBox_menu(WebContext ctx, String aMode, String aDesc) {
            WebContext nuCtx = (WebContext) ctx.clone();
            nuCtx.addQuery(SurveyMain.PREF_SORTMODE, aMode);

            if (!sortMode.getName().equals(aMode)) {
                nuCtx.print("<a class='notselected' href='" + nuCtx.url() + "'>");
            } else {
                nuCtx.print("<span class='selected'>");
            }
            nuCtx.print(aDesc);
            if (!sortMode.getName().equals(aMode)) {
                nuCtx.println("</a>");
            } else {
                nuCtx.println("</span>");
            }

            nuCtx.println(" ");
        }

        /**
         * Get the size of this DisplaySet
         *
         * @return the number of rows
         */
        public int size() {
            return rows.length;
        }

        /**
         * Convert this DisplaySet to a JSON string.
         */
        @Override
        public String toJSONString() throws JSONException {
            JSONArray r = new JSONArray();
            for (DataRow row : rows) {
                r.put(row.fieldHash());
            }
            JSONArray p = new JSONArray();
            for (Partition partition : partitions) {
                p.put(new JSONObject().put("name", partition.name).put("start", partition.start).put("limit", partition.limit)
                    .put("helptext", partition.helptext));
            }
            return new JSONObject().put("canName", canName).put("displayName", sortMode.getDisplayName())
                .put("isCalendar", isCalendar).put("isMetazones", isMetazones).put("sortMode", sortMode.getName())
                .put("rows", r).put("partitions", p).toString();
        }

    }

    /**
     * The ExampleEntry class represents an Example box, so that it can be stored and
     * restored.
     */
    public class ExampleEntry {

        public DataSection.DataRow dataRow;

        public String hash = null;
        public DataRow.CandidateItem item;
        public DataSection section;
        public CheckCLDR.CheckStatus status;

        /**
         * Create a new ExampleEntry
         *
         * @param section the DataSection
         * @param row the DataRow
         * @param item the CandidateItem
         * @param status the CheckStatus
         */
        public ExampleEntry(DataSection section, DataRow row, DataRow.CandidateItem item, CheckCLDR.CheckStatus status) {
            this.section = section;
            this.dataRow = row;
            this.item = item;
            this.status = status;

            /*
             * unique serial #- covers item, status.
             *
             * fieldHash ensures that we don't get the wrong field.
             */
            hash = CookieSession.cheapEncode(DataSection.getN()) + row.fieldHash();
        }
    }

    /*
     * A string used only in VETTING_PROBLEMS_LIST
     */
    private static String CHANGES_DISPUTED = "Disputed";

    /**
     * Divider denoting a specific Continent division.
     */
    public static final String CONTINENT_DIVIDER = "~";

    private static final boolean DEBUG = false || CldrUtility.getProperty("TEST", false);

    /*
     * A Pattern matching paths that are always excluded
     */
    private static Pattern excludeAlways;

    public static final String EXEMPLAR_PARENT = "//ldml/dates/timeZoneNames/zone";

    private static final String fromto[] = { "^days/(.*)/(sun)$", "days/1-$2/$1", "^days/(.*)/(mon)$", "days/2-$2/$1",
        "^days/(.*)/(tue)$", "days/3-$2/$1", "^days/(.*)/(wed)$", "days/4-$2/$1", "^days/(.*)/(thu)$", "days/5-$2/$1",
        "^days/(.*)/(fri)$", "days/6-$2/$1", "^days/(.*)/(sat)$", "days/7-$2/$1", "^months/(.*)/month/([0-9]*)$",
        "months/$2/$1", "^([^/]*)/months/(.*)/month/([0-9]*)$", "$1/months/$3/$2", "^eras/(.*)/era/([0-9]*)$", "eras/$2/$1",
        "^([^/]*)/eras/(.*)/era/([0-9]*)$", "$1/eras/$3/$2", "^([ap]m)$", "ampm/$1", "^quarter/(.*)/quarter/([0-9]*)$",
        "quarter/$2/$1", "^([^/]*)/([^/]*)/time$", "$1/time/$2", "^([^/]*)/([^/]*)/date", "$1/date/$2", "/alias$", "",
        "/displayName\\[@count=\"([^\"]*)\"\\]$", "/count=$1", "\\[@count=\"([^\"]*)\"\\]$", "/count=$1",
        "^unit/([^/]*)/unit([^/]*)/", "$1/$2/", "dateTimes/date/availablesItem", "available date formats:",
        /*
         * "/date/availablesItem.*@_q=\"[0-9]*\"\\]\\[@id=\"([0-9]*)\"\\]",
         * "/availableDateFormats/$1"
         */
        // "/date/availablesItem.*@_q=\"[0-9]*\"\\]","/availableDateFormats"
    };
    private static Pattern fromto_p[] = new Pattern[fromto.length / 2];

    /*
     * Has this DataSection been initialized?
     * Used only in the function init()
     */
    private static boolean isInitted = false;

    /*
     * A Pattern used only in the function isName()
     */
    private static final Pattern NAME_TYPE_PATTERN = PatternCache.get("[a-zA-Z0-9]+|.*exemplarCity.*");

    /*
     * A string used only in VETTING_PROBLEMS_LIST
     */
    private static String PARTITION_ERRORS = "Error Values";

    /*
     * A string used only in VETTING_PROBLEMS_LIST
     */
    private static String PARTITION_UNCONFIRMED = "Unconfirmed";

    /**
     * Trace in detail time taken to populate?
     */
    private static final boolean TRACE_TIME = false;

    /**
     * Show time taken to populate?
     */
    private static final boolean SHOW_TIME = false || TRACE_TIME || DEBUG || CldrUtility.getProperty("TEST", false);

    /**
     * An array of strings, used only in showSection()
     * @deprecated
     */
    @Deprecated
    public static final String VETTING_PROBLEMS_LIST[] = { PARTITION_ERRORS, CHANGES_DISPUTED, PARTITION_UNCONFIRMED };

    /**
     * Field to cache the Coverage info
     */
    private static CoverageInfo covInfo = null;

    /**
     * Synchronization Mutex used for accessing/setting the coverageInfo object
     */
    private static final Object GET_COVERAGEINFO_SYNC = new Object();

    /**
     * Warn user why these messages are showing up.
     */
    static {
        if (TRACE_TIME == true) {
            System.err.println("DataSection: Note, TRACE_TIME is TRUE");
        }
    }

    /*
     * A serial number, used only in the function getN()
     */
    private static int n = 0;

    /**
     * Get a unique serial number
     *
     * @return the number
     *
     * Called only by the ExampleEntry constructor
     */
    protected static synchronized int getN() {
        return ++n;
    }

    /**
     * Initialize this DataSection if it hasn't already been initialized
     */
    private static synchronized void init() {
        if (!isInitted) {
            /*
             * TODO: Does this call to PatternCache.get("\\[@(?:type|key)=['\"]([^'\"]*)['\"]\\]")
             * have a useful side-effect? If not, delete the call.
             * It was formerly assigned to typeReplacementPattern, no longer used.
             * The call to PatternCache.get("([^/]*)/(.*)") was not assigned; presumably
             * it has a useful side-effect?
             */
            PatternCache.get("\\[@(?:type|key)=['\"]([^'\"]*)['\"]\\]");
            PatternCache.get("([^/]*)/(.*)");

            /* This one is only used with non-pageID use. */
            PatternCache.get("^//ldml/localeDisplayNames.*|"
               /* these are excluded when 'misc' is chosen. */
               + "^//ldml/characters/exemplarCharacters.*|" + "^//ldml/numbers.*|" + "^//ldml/units.*|"
               + "^//ldml/references.*|" + "^//ldml/dates/timeZoneNames/zone.*|" + "^//ldml/dates/timeZoneNames/metazone.*|"
               + "^//ldml/dates/calendar.*|" + "^//ldml/identity.*");

            /* Always excluded. Compare with PathHeader/Coverage. */
            excludeAlways = PatternCache.get("^//ldml/segmentations.*|" + "^//ldml/measurement.*|" + ".*week/minDays.*|"
                + ".*week/firstDay.*|" + ".*/usesMetazone.*|" + ".*week/weekendEnd.*|" + ".*week/weekendStart.*|" +
                "^//ldml/posix/messages/.*expr$|" + "^//ldml/dates/timeZoneNames/.*/GMT.*exemplarCity$|"
                + "^//ldml/dates/.*default"); // no defaults

            int pn;
            for (pn = 0; pn < fromto.length / 2; pn++) {
                fromto_p[pn] = PatternCache.get(fromto[pn * 2]);
            }
            isInitted = true;
        }
    }

    /**
     * @return a new XPathMatcher that matches all paths in the hacky
     *         excludeAlways regex. For testing.
     * @deprecated
     *
     * Referenced only in CLDR23Tool.jsp -- which is what??
     */
    public static XPathMatcher getHackyExcludeMatcher() {
        init();
        return new XPathMatcher() {

            @Override
            public String getName() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public boolean matches(String xpath, int xpid) {
                return excludeAlways.matcher(xpath).matches();
            }
        };
    }

    /**
     * Create, populate, and complete a DataSection given the specified locale
     * and prefix
     *
     * @param ctx
     *            context to use (contains CLDRDBSource, etc.)
     * @param locale
     *            locale
     * @param prefix
     *            XPATH prefix
     * @param simple
     *            if true, means that data is simply xpath+type. If false, all
     *            xpaths under prefix.
     */
    public static DataSection make(PageId pageId, WebContext ctx, CookieSession session, CLDRLocale locale, String prefix,
        XPathMatcher matcher, boolean showLoading, String ptype) {

        SurveyMain sm = CookieSession.sm; // TODO: non-deprecated way of getting sm

        DataSection section = new DataSection(pageId, sm, locale, prefix, matcher, ptype);

        section.hasExamples = true;

        CLDRFile ourSrc = sm.getSTFactory().make(locale.getBaseName(), true, true);

        ourSrc.setSupplementalDirectory(sm.getSupplementalDirectory());
        if (ctx != null) {
            section.setUserAndFileForVotelist(ctx.session != null ? ctx.session.user : null, ourSrc);
        } else if (session != null && session.user != null) {
            section.setUserAndFileForVotelist(session.user, ourSrc);
        }
        if (ourSrc.getSupplementalDirectory() == null) {
            throw new InternalError("?!! ourSrc hsa no supplemental dir!");
        }
        synchronized (session) {
            TestResultBundle checkCldr = sm.getSTFactory().getTestResult(locale, getOptions(ctx, session, locale));
            if (ourSrc.getSupplementalDirectory() == null) {
                throw new InternalError("?!! ourSrc hsa no supplemental dir!");
            }
            if (checkCldr == null) {
                throw new InternalError("checkCldr == null");
            }
            if (ourSrc.getSupplementalDirectory() == null) {
                throw new InternalError("?!! ourSrc hsa no supplemental dir!");
            }
            String workingCoverageLevel = section.getPtype();
            com.ibm.icu.dev.util.ElapsedTimer cet = null;
            if (showLoading && SHOW_TIME) {
                cet = new com.ibm.icu.dev.util.ElapsedTimer();
                System.err.println("Begin populate of " + locale + " // " + prefix + ":" + workingCoverageLevel + " - is:"
                    + ourSrc.getClass().getName());
            }
            if (ourSrc.getSupplementalDirectory() == null) {
                throw new InternalError("?!! ourSrc hsa no supplemental dir!");
            }
            section.baselineFile = sm.getBaselineFile();
            if (ourSrc.getSupplementalDirectory() == null) {
                throw new InternalError("?!! ourSrc hsa no supplemental dir!");
            }
            section.skippedDueToCoverage = 0;
            if (ourSrc.getSupplementalDirectory() == null) {
                throw new InternalError("?!! ourSrc hsa no supplemental dir!");
            }
            if (showLoading && ctx != null) {
                ctx.println("<script type=\"text/javascript\">document.getElementById('loadSection').innerHTML='Loading...';</script>");
                ctx.flush();
            }
            if (ourSrc.getSupplementalDirectory() == null) {
                throw new InternalError("?!! ourSrc hsa no supplemental dir!");
            }
            if (ourSrc.getSupplementalDirectory() == null) {
                throw new InternalError("?!! ourSrc hsa no supplemental dir!");
            }
            section.populateFrom(ourSrc, checkCldr, workingCoverageLevel);
            int popCount = section.getAll().size();
            if (showLoading && ctx != null) {
                ctx.println("<script type=\"text/javascript\">document.getElementById('loadSection').innerHTML='Completing..."
                    + popCount + " items';</script>");
                ctx.flush();
            }
            if (pageId == null) {
                section.ensureComplete(ourSrc, checkCldr, workingCoverageLevel);
                popCount = section.getAll().size();
            }
            if (showLoading && ctx != null && SHOW_TIME) {
                int allCount = section.getAll().size();
                System.err.println("Populate+complete " + locale + " // " + prefix + ":" + section.getPtype() + " = " + cet
                    + " - Count: " + popCount + "+" + (allCount - popCount) + "=" + allCount);
            }
        }
        return section;
    }

    /**
     * Get the options for the given WebContext, or, if the context is null, get the
     * options for the given CookieSession and CLDRLocale
     *
     * @param ctx
     * @param session
     * @param locale
     * @return the CheckCLDR.Options object
     */
    public static CheckCLDR.Options getOptions(WebContext ctx, CookieSession session, CLDRLocale locale) {
        CheckCLDR.Options options;
        if (ctx != null) {
            options = (ctx.getOptionsMap());
        } else {
            final String def = CookieSession.sm
                .getListSetting(session.settings(), SurveyMain.PREF_COVLEV,
                    WebContext.PREF_COVLEV_LIST, false);

            final String org = session.getEffectiveCoverageLevel(locale.toString());

            options = new Options(locale, SurveyMain.getTestPhase(), def, org);
        }
        return options;
    }

    /**
     * Given a (cleaned, etc) xpath, return the podBase, i.e.,
     * context.getPod(base), that would be used to show that xpath. Keep this in
     * sync with SurveyMain.showLocale() where there is the list of menu items.
     *
     * @param xpath the xpath string
     *
     * @return a string, for example, "//ldml/units"
     */
    public static String xpathToSectionBase(String xpath) {
        int n;
        String base;

        // is it one of the prefixes we can check statically?
        String staticBases[] = {
            /*
             *  LOCALEDISPLAYNAMES
             */
            "//ldml/" + PathUtilities.NUMBERSCURRENCIES, "//ldml/" + "dates/timeZoneNames/zone",
            "//ldml/" + "dates/timeZoneNames/metazone",
            /*
             *  OTHERROOTS
             */
            SurveyMain.GREGO_XPATH, PathUtilities.LOCALEDISPLAYPATTERN_XPATH, PathUtilities.OTHER_CALENDARS_XPATH,
            "//ldml/units" };

        // is it one of the static bases?
        for (n = 0; n < staticBases.length; n++) {
            if (xpath.startsWith(staticBases[n])) {
                return staticBases[n];
            }
        }

        // dynamic LOCALEDISPLAYNAMES
        for (n = 0; n < PathUtilities.LOCALEDISPLAYNAMES_ITEMS.length; n++) {
            // is it a simple code list?
            base = PathUtilities.LOCALEDISPLAYNAMES + PathUtilities.LOCALEDISPLAYNAMES_ITEMS[n] + '/'
                + SurveyMain.typeToSubtype(PathUtilities.LOCALEDISPLAYNAMES_ITEMS[n]);
            // see: SurveyMain.showLocaleCodeList()
            if (xpath.startsWith(base)) {
                return base;
            }
        }

        // OTHERROOTS
        for (n = 0; n < SurveyMain.OTHERROOTS_ITEMS.length; n++) {
            base = "//ldml/" + SurveyMain.OTHERROOTS_ITEMS[n];
            if (xpath.startsWith(base)) {
                return base;
            }
        }

        return "//ldml"; // the "misc" pile.
    }

    private BallotBox<User> ballotBox;

    // UI strings
    boolean canName = true; // can the Display Name be used for sorting?

    // hash of examples
    Hashtable<String, ExampleEntry> exampleHash = new Hashtable<String, ExampleEntry>();

    /**
     * Does this DataSection have examples?
     * 
     * TODO: resolve confusion: this is a field of DataSection, but it's returned by a function with the same name in DataRow
     */
    public boolean hasExamples = false;

    /*
     * Interest group
     */
    public String intgroup;

    boolean isCalendar = false; // Is this a calendar section?
    boolean isMetazones = false; // Is this a metazones section?

    // TODO: myCollator unused? does createCollator have useful side-effect?
    final Collator myCollator = CodeSortMode.createCollator();

    private String ptype;

    /*
     * hashtable of type->Row
     */
    Hashtable<String, DataRow> rowsHash = new Hashtable<String, DataRow>();

    /*
     * How many were skipped due to coverage?
     */
    public int skippedDueToCoverage = 0;

    private SurveyMain sm;
    long touchTime = -1; // when has this DataRow been hit?
    public String xpathPrefix = null;

    private CLDRLocale locale;
    private ExampleBuilder examplebuilder;
    private XPathMatcher matcher;
    private PageId pageId;
    private CLDRFile diskFile;

    /**
     * Create a DataSection
     *
     * @param pageId
     * @param sm
     * @param loc
     * @param prefix
     * @param matcher
     * @param ptype
     */
    DataSection(PageId pageId, SurveyMain sm, CLDRLocale loc, String prefix, XPathMatcher matcher, String ptype) {
        this.locale = loc;
        this.sm = sm;
        this.ptype = ptype;
        this.matcher = matcher;
        xpathPrefix = prefix;
        intgroup = loc.getLanguage(); // calculate interest group
        ballotBox = sm.getSTFactory().ballotBoxForLocale(locale);
        this.pageId = pageId;
    }

    /**
     * Add the given DataRow to this DataSection
     *
     * @param row the DataRow
     *
     * Called only by getDataRow
     */
    void addDataRow(DataRow row) {
        rowsHash.put(row.xpath, row);
    }

    /**
     * Enregister an ExampleEntry
     *
     * Called only by populateFrom
     */
    ExampleEntry addExampleEntry(ExampleEntry e) {
        synchronized (exampleHash) {
            exampleHash.put(e.hash, e);
        }
        return e; // for the hash.
    }

    /**
     * Get the page id
     * 
     * @return pageId
     * 
     * Called from RefreshRow.jsp
     */
    public PageId getPageId() {
        return pageId;
    }

    /**
     * Create a DisplaySet for this DataSection
     *
     * @param sortMode
     * @param matcher
     * @return the DisplaySet
     * 
     * Called from RefreshRow.jsp
     */
    public DisplaySet createDisplaySet(SortMode sortMode, XPathMatcher matcher) {
        DisplaySet aDisplaySet = sortMode.createDisplaySet(matcher, rowsHash.values());
        aDisplaySet.canName = canName;
        aDisplaySet.isCalendar = isCalendar;
        aDisplaySet.isMetazones = isMetazones;
        return aDisplaySet;
    }

    /**
     * Makes sure this DataSection contains the rows we'd like to see.
     *
     * @obsolete not called anymore
     *
     * TODO: Actually still called by DataSection.make, clarify whether "obsolete"
     */
    private void ensureComplete(CLDRFile ourSrc, TestResultBundle checkCldr, String workingCoverageLevel) {

        STFactory stf = sm.getSTFactory();
        SectionId sectionId = (pageId != null) ? pageId.getSectionId() : null;

        int workingCoverageValue = Level.fromString(workingCoverageLevel).getLevel();
        if (sectionId == SectionId.Timezones || pageId == PageId.Timezone_Display_Patterns
            || (pageId == null && xpathPrefix.startsWith("//ldml/" + "dates/timeZoneNames"))) {
            // work on zones
            boolean isMetazones = (sectionId == SectionId.Timezones)
                || (pageId == null && xpathPrefix.startsWith("//ldml/" + "dates/timeZoneNames/metazone"));
            boolean isSingleXPath = false;
            // Make sure the DataSection contains the rows we'd like to see.
            // regular zone

            Set<String> zoneIterator;
            String podBase = xpathPrefix;

            if (isMetazones) {
                if (xpathPrefix.indexOf(CONTINENT_DIVIDER) > 0) {
                    String[] pieces = xpathPrefix.split(CONTINENT_DIVIDER, 2);
                    xpathPrefix = pieces[0];
                    zoneIterator = sm.getMetazones(pieces[1]);
                } else { // This is just a single metazone from a zoom-in
                    Set<String> singleZone = new HashSet<String>();
                    XPathParts xpp = new XPathParts();
                    xpp.set(xpathPrefix);
                    String singleMetazoneName = xpp.findAttributeValue("metazone", "type");
                    if (singleMetazoneName == null) {
                        throw new NullPointerException("singleMetazoneName is null for xpp:" + xpathPrefix);
                    }
                    singleZone.add(singleMetazoneName);
                    zoneIterator = singleZone;

                    isSingleXPath = true;
                }
                podBase = "//ldml/dates/timeZoneNames/metazone";
            } else {
                if (xpathPrefix.indexOf(CONTINENT_DIVIDER) > 0) {
                    throw new InternalError("Error: CONTINENT_DIVIDER found on non-metazone xpath " + xpathPrefix);
                }
                zoneIterator = StandardCodes.make().getGoodAvailableCodes("tzid");
                podBase = "//ldml/dates/timeZoneNames/zone";
            }
            if (!isSingleXPath && xpathPrefix.contains("@type")) {
                isSingleXPath = true;
            }

            final String tzsuffs[] = {
                "/exemplarCity" };
            final String mzsuffs[] = { "/long/generic", "/long/daylight", "/long/standard", "/short/generic", "/short/daylight",
                "/short/standard" };

            String suffs[];
            if (isMetazones) {
                suffs = mzsuffs;
            } else {
                suffs = tzsuffs;
            }

            CLDRFile resolvedFile = ourSrc;

            for (String zone : zoneIterator) {
                if (zone == null) {
                    throw new NullPointerException("zoneIterator.next() returned null! zoneIterator.size: " + zoneIterator.size()
                        + ", isEmpty: " + zoneIterator.isEmpty());
                }
                /** some compatibility **/
                String ourSuffix = "[@type=\"" + zone + "\"]";

                for (int i = 0; i < suffs.length; i++) {
                    String suff = suffs[i];

                    if (isSingleXPath && !xpathPrefix.contains(suff)) {
                        continue; // Try not to add paths that won't be shown.
                    }
                    // synthesize a new row..
                    String base_xpath_string = podBase + ourSuffix + suff;

                    SurveyToolStatus ststats = SurveyToolStatus.READ_WRITE;
                    PathHeader ph = stf.getPathHeader(base_xpath_string);
                    if (ph != null) {
                        ststats = stf.getPathHeader(base_xpath_string).getSurveyToolStatus();
                    }
                    if ((ststats == SurveyToolStatus.HIDE || ststats == SurveyToolStatus.DEPRECATED)) {
                        continue;
                    }

                    int xpid = sm.xpt.getByXpath(base_xpath_string);
                    if (matcher != null && !matcher.matches(base_xpath_string, xpid)) {
                        continue;
                    }
                    if (excludeAlways.matcher(base_xpath_string).matches()) {
                        continue;
                    }
                    if (resolvedFile.isPathExcludedForSurvey(base_xpath_string)) {
                        continue;
                    }

                    // Only display metazone data for which an English value exists
                    if (isMetazones && suff != "/commonlyUsed") {
                        String engValue = baselineFile.getStringValue(base_xpath_string);
                        if (engValue == null || engValue.length() == 0) {
                            continue;
                        }
                    }
                    // Filter out data that is higher than the desired coverage level
                    int coverageValue = getCoverageInfo().getCoverageValue(base_xpath_string, locale.getBaseName());
                    if (coverageValue > workingCoverageValue) {
                        if (coverageValue <= 100) {
                            // KEEP COUNT OF FILTERED ITEMS
                            skippedDueToCoverage++;
                        } // else: would never be shown, don't care.
                        continue;
                    }

                    DataSection.DataRow myp = getDataRow(base_xpath_string); /* rowXPath */

                    myp.coverageValue = coverageValue;

                    // set it up..
                    int base_xpath = sm.xpt.getByXpath(base_xpath_string);

                    // set up tests
                    myp.setShimTests(base_xpath, base_xpath_string, checkCldr);
                } // end inner for loop
            } // end outer for loop
        } // end if timezone
    }

    /**
     * Get the CoverageInfo object from CLDR for this DataSection
     *
     * @return the CoverageInfo
     */
    private CoverageInfo getCoverageInfo() {
        synchronized (GET_COVERAGEINFO_SYNC) {
            if (covInfo == null) {
                covInfo = CLDRConfig.getInstance().getCoverageInfo();
            }
            return covInfo;
        }
    }

    /**
     * Get all rows for this DataSection, unsorted
     *
     * @return the Collection of DataRow
     */
    public Collection<DataRow> getAll() {
        return rowsHash.values();
    }

    /**
     * Get the row for the given xpath in this DataSection
     *
     * Linear search for matching item.
     *
     * @param xpath the integer...
     * @return the matching DataRow
     * 
     * Called from r_rxt.jsp
     */
    public DataRow getDataRow(int xpath) {
        return getDataRow(sm.xpt.getById(xpath));
    }

    /**
     * Get the row for the given xpath in this DataSection
     *
     * Linear search for matching item.
     *
     * @param xpath the string...
     * @return the matching DataRow
     */
    public DataRow getDataRow(String xpath) {
        if (xpath == null) {
            throw new InternalError("type is null");
        }
        if (rowsHash == null) {
            throw new InternalError("rowsHash is null");
        }
        DataRow row = rowsHash.get(xpath);
        if (row == null) {
            row = new DataRow(xpath);
            addDataRow(row);
        }
        return row;
    }

    /**
     * Given a hash, see addExampleEntry, retrieve the ExampleEntry which has
     * section, row, candidateitem and status
     */
    ExampleEntry getExampleEntry(String hash) {
        synchronized (exampleHash) {
            return exampleHash.get(hash);
        }
    }

    /**
     * Get the ptype for this DataSection
     *
     * @return the ptype string
     */
    private String getPtype() {
        return ptype;
    }

    /**
     * Get the number of things that were skipped due to coverage
     *
     * TODO: clarify what kind of things, and what it means for them to be "skipped due to coverage"
     *
     * @return the number of things that were skipped
     */
    public int getSkippedDueToCoverage() {
        return skippedDueToCoverage;
    }

    /**
     * Populate this DataSection
     *
     * @param ourSrc the CLDRFile
     * @param checkCldr the TestResultBundle
     * @param workingCoverageLevel
     *
     * Called only by DataSection.make, as section.populateFrom(ourSrc, checkCldr, workingCoverageLevel).
     */
    private void populateFrom(CLDRFile ourSrc, TestResultBundle checkCldr, String workingCoverageLevel) {
        XPathParts xpp = new XPathParts(null, null);
        STFactory stf = sm.getSTFactory();
        CLDRFile oldFile = stf.getOldFileResolved(locale);
        diskFile = stf.getDiskFile(locale);
        List<CheckStatus> examplesResult = new ArrayList<CheckStatus>();
        String workPrefix = xpathPrefix;

        int workingCoverageValue = Level.fromString(workingCoverageLevel).getLevel();

        Set<String> allXpaths;

        Set<String> extraXpaths = null;
        List<CheckStatus> checkCldrResult = new ArrayList<CheckStatus>();

        if (pageId != null) {
            allXpaths = PathHeader.Factory.getCachedPaths(pageId.getSectionId(), pageId);
            allXpaths.retainAll(stf.getPathsForFile(locale));
        } else {
            init(); // pay for the patterns
            allXpaths = new HashSet<String>();
            extraXpaths = new HashSet<String>();

            /* Determine which xpaths to show */
            if (xpathPrefix.startsWith("//ldml/units") || xpathPrefix.startsWith("//ldml/numbers")) {
                canName = false;
            } else if (xpathPrefix.startsWith("//ldml/dates/timeZoneNames/metazone")) {
                String continent = null;
                int continentStart = xpathPrefix.indexOf(DataSection.CONTINENT_DIVIDER);
                if (continentStart > 0) {
                    continent = xpathPrefix.substring(xpathPrefix.indexOf(DataSection.CONTINENT_DIVIDER) + 1);
                }
                if (DEBUG) {
                    System.err.println(xpathPrefix + ": -> continent " + continent);
                }
                if (!xpathPrefix.contains("@type")) {
                    // if it's not a zoom-in..
                    workPrefix = "//ldml/dates/timeZoneNames/metazone";
                }
            } else if (xpathPrefix.equals("//ldml/references")) {
                canName = false; // disable 'view by name' for references
            }

            isCalendar = xpathPrefix.startsWith("//ldml/dates/calendars");
            isMetazones = xpathPrefix.startsWith("//ldml/dates/timeZoneNames/metazone");

            /* Build the set of xpaths */
            // iterate over everything in this prefix ..
            Set<String> baseXpaths = stf.getPathsForFile(locale, xpathPrefix);

            allXpaths.addAll(baseXpaths);
            if (ourSrc.getSupplementalDirectory() == null) {
                throw new InternalError("?!! ourSrc hsa no supplemental dir!");
            }
            ourSrc.getExtraPaths(workPrefix, extraXpaths);
            extraXpaths.removeAll(baseXpaths);
            allXpaths.addAll(extraXpaths);

            // Process extra paths.
            if (DEBUG) {
                System.err.println("@@X@ base[" + workPrefix + "]: " + baseXpaths.size() + ", extra: " + extraXpaths.size());
            }
        }
        populateFromAllXpaths(allXpaths, workPrefix, ourSrc, oldFile, extraXpaths, stf, workingCoverageValue, xpp, checkCldr, examplesResult, checkCldrResult);
    }
    
    /**
     * Populate this DataSection with a row for each of the given xpaths
     *
     * @param allXpaths the set of xpaths
     * @param workPrefix
     * @param ourSrc
     * @param oldFile
     * @param extraXpaths
     * @param stf
     * @param workingCoverageValue
     * @param xpp
     * @param checkCldr
     * @param examplesResult
     * @param checkCldrResult
     * 
     * TODO: resurrect SHOW_TIME and TRACE_TIME code, deleted in revision 14327, if and when needed for debugging.
     * It was deleted when this code was moved from populateFrom to new subroutine populateFromAllXpaths.
     */
    private void populateFromAllXpaths(Set<String> allXpaths, String workPrefix, CLDRFile ourSrc, CLDRFile oldFile, Set<String> extraXpaths, STFactory stf,
        int workingCoverageValue, XPathParts xpp, TestResultBundle checkCldr, List<CheckStatus> examplesResult, List<CheckStatus> checkCldrResult) {

        for (String xpath : allXpaths) {
            if (xpath == null) {
                throw new InternalError("null xpath in allXpaths");
            }
            int xpid = sm.xpt.getByXpath(xpath);
            if (pageId == null) {
                if (matcher != null && !matcher.matches(xpath, xpid)) {
                    continue;
                }
                if (!xpath.startsWith(workPrefix)) {
                    if (DEBUG && SurveyMain.isUnofficial()) {
                        System.err.println("@@ BAD XPATH " + xpath);
                    }
                    continue;
                } else if (ourSrc.isPathExcludedForSurvey(xpath)) {
                    if (DEBUG && SurveyMain.isUnofficial()) {
                        System.err.println("@@ excluded:" + xpath);
                    }
                    continue;
                } else if (DEBUG) {
                    System.err.println("allPath: " + xpath);
                }
            }

            SurveyToolStatus ststats = SurveyToolStatus.READ_WRITE;
            PathHeader ph = stf.getPathHeader(xpath);
            if (ph != null) {
                ststats = stf.getPathHeader(xpath).getSurveyToolStatus();
            }
            if ((ststats == SurveyToolStatus.HIDE || ststats == SurveyToolStatus.DEPRECATED)) {
                continue;
            }

            String fullPath = ourSrc.getFullXPath(xpath);
            int base_xpath = sm.xpt.xpathToBaseXpathId(xpath);
            String baseXpath = sm.xpt.getById(base_xpath);

            int coverageValue = getCoverageInfo().getCoverageValue(baseXpath, locale.getBaseName());
            if (coverageValue > workingCoverageValue) {
                if (coverageValue <= Level.COMPREHENSIVE.getLevel()) {
                    skippedDueToCoverage++;
                } // else: would never be shown, don't care
                continue;
            }
            if (fullPath == null) {
                fullPath = xpath; // (this is normal for 'extra' paths)
            }
            // Now we are ready to add the data
            populateFromThisXpath(xpath, extraXpaths, ourSrc, oldFile, xpp, fullPath, checkCldr, coverageValue, base_xpath, examplesResult, checkCldrResult);
        }
    }

    /**
     * Add data to this DataSection including a possibly new DataRow for the given xpath
     *
     * @param xpath
     * @param extraXpaths
     * @param ourSrc
     * @param oldFile
     * @param xpp
     * @param fullPath
     * @param checkCldr
     * @param coverageValue
     * @param base_xpath
     * @param examplesResult
     * @param checkCldrResult
     */
    private void populateFromThisXpath(String xpath, Set<String> extraXpaths, CLDRFile ourSrc, CLDRFile oldFile, XPathParts xpp, String fullPath,
        TestResultBundle checkCldr, int coverageValue, int base_xpath, List<CheckStatus> examplesResult, List<CheckStatus> checkCldrResult) {
        /*
         * 'extra' paths get shim treatment
         * 
         * NOTE: this is a sufficient but not a necessary condition for isExtraPath; if it gets false here,
         * it may still get true below if ourSrc.getStringValue returns null.
         */
        boolean isExtraPath = extraXpaths != null && extraXpaths.contains(xpath);

        /*
         * TODO: clarify the significance and usage of the local variable ourValue.
         * Formerly it was named "value"; the new name "ourValue" reflects that it is
         * gotten as ourSrc.getStringValue(xpath).
         *
         * If not null, it's ourSrc.getStringValue(xpath); not possible (confirm?) for that
         * to be CldrUtility.INHERITANCE_MARKER. getStringValue effectively resolves that to
         * the Bailey value -- could that possibly be causing a "hard" vote to be treated as
         * winning when in reality it may have been a "soft" vote that was winning?
         *
         * ourValue DOES reflect the current votes (if any). To see this, enter a new winning
         * value for a row, for example "test-bogus" in place of "occitan" in the first row
         * of http://localhost:8080/cldr-apps/v#/fr_CA/Languages_O_S/24368393ccee3451
         * and ourValue will get "test-bogus" here.
         * Then vote for "soft" inheritance, and ourValue will get "occitan" here (NOT CldrUtility.INHERITANCE_MARKER).
         * How does ourValue relate to winningValue, oldValue, etc.?
         * ourSrc contrasts with oldFile; both are type CLDRFile.
         * Each CLDRFile has a dataSource, which is an XMLSource, an abstract class.
         * These classes extends XMLSource: DelegateXMLSource, DummyXMLSource, ResolvingSource,
         * SimpleXMLSource. Generally (always?) in this context ourSrc.dataSource is a
         * ResolvingSource, and ourSrc.dataSource.currentSource is a DataBackedSource.
         * ourSrc is initialized as follows:
         *         CLDRFile ourSrc = sm.getSTFactory().make(locale.getBaseName(), true, true);
         * 
         * See fixWinningValue for more related comments.
         */

        String ourValue = isExtraPath ? null : ourSrc.getStringValue(xpath);
        if (ourValue == null) {
            /*
             * This happens, for example, with xpath = "//ldml/dates/timeZoneNames/metazone[@type=\"Kyrgystan\"]/long/generic"
             * at http://localhost:8080/cldr-apps/v#/fr_CA/CAsia/
             * 
             * getStringValue calls getFallbackPath which calls getRawExtraPaths which contains xpath
             */
            if (DEBUG) {
                System.err.println("warning: populateFromThisXpath " + this + ": " + locale + ":" + xpath + " = NULL! wasExtraPath="
                    + isExtraPath);
            }
            isExtraPath = true;
        }

        // determine 'alt' param
        String alt = sm.xpt.altFromPathToTinyXpath(xpath, xpp);

        /* FULL path processing (references.. alt proposed.. ) */
        xpp.clear();
        xpp.initialize(fullPath);
        String lelement = xpp.getElement(-1);
        xpp.findAttributeValue(lelement, LDMLConstants.ALT);
        String eDraft = xpp.findAttributeValue(lelement, LDMLConstants.DRAFT);

        String typeAndProposed[] = LDMLUtilities.parseAlt(alt);
        String altProp = typeAndProposed[1];

        CLDRFile.Status sourceLocaleStatus = new CLDRFile.Status();
        String sourceLocale = ourSrc.getSourceLocaleID(xpath, sourceLocaleStatus);

        boolean ourValueIsInherited = !(sourceLocale.equals(locale.toString()));

        /*
         * Load the 'data row' which represents one user visible row.
         * (may be nested in the case of alt types) (nested??)
         *  
         * Is it ever true that rowsHash already contains xpath here, or does getDataRow always create a new DataRow here?
         * Seemingly getDataRow always creates a new DataRow here.
         */
        DataRow row = getDataRow(xpath);

        /*
         * Call updateInheritedValue before fixWinningValue, so that the latter can use inheritedValue as needed.
         * 
         * Normally row.inheritedItem is null at this point, unless setShimTests has already been called
         * by ensureComplete, for some timezones. If row.inheritedItem is null, possibly create it.
         * 
         * However, skip updateInheritedValue if isExtra. See setShimTests below, which may set inheritedItem
         * when isExtraPath.
         */
        if (row.inheritedItem == null && !isExtraPath) {
            row.updateInheritedValue(ourSrc, checkCldr);
        }
        row.fixWinningValue(ourValue, ourValueIsInherited);

        Set<String> v = ballotBox.getValues(xpath);
        if (v != null) {
            populateFromThisXpathAddItemsForVotes(v, xpath, row, checkCldr);
        }

        /*
         * Add an item for winningValue if there isn't one already.
         */
        if (row.winningValue != null) {
            row.addItem(row.winningValue, "winningValue");
        }

        /*
         * Add an item for oldValue if there isn't one already.
         *
         * TODO: clarify whether the conditions "!row.oldValue.equals(ourValue)" and
         * "v == null || !v.contains(row.oldValue)" are needed here and if so why.
         */
        if (row.oldValue != null && !row.oldValue.equals(ourValue) && (v == null || !v.contains(row.oldValue))) {
            row.addItem(row.oldValue, "oldValue");
        }

        row.coverageValue = coverageValue;

        if (locale.getCountry() != null && locale.getCountry().length() > 0) {
            /*
             * If "vote for inherited" isn't already represented as an item, add it (child locales only).
             * 
             * TODO: Note that updateInheritedValue is called above, unless isExtraPath; normally
             * it's the job of updateInheritedValue to do addItem(CldrUtility.INHERITANCE_MARKER); is there
             * any need to call it here as well? setShimTests below may also do addItem(CldrUtility.INHERITANCE_MARKER).
             */            
            row.addItem(CldrUtility.INHERITANCE_MARKER, "getCountry");
        }

        if (row.inheritedItem == null && isExtraPath) {
            /*
             * This is an 'extra' item- it doesn't exist in xml (including root).
             * For example, isExtraPath may be true when xpath is:
             *  '//ldml/dates/timeZoneNames/metazone[@type="Mexico_Northwest"]/short/standard'
             * and the URL ends with "v#/aa/NAmerica/".
             * Set up 'shim' tests, to display coverage.
             */
            row.setShimTests(base_xpath, this.sm.xpt.getById(base_xpath), checkCldr);
        }
        if (row.getDisplayName() == null) {
            canName = false; // disable 'view by name' if not all have names.
        }

        // If it is draft and not proposed.. make it proposed-draft
        if (((eDraft != null) && (!eDraft.equals("false"))) && (altProp == null)) {
            altProp = SurveyMain.PROPOSED_DRAFT;
        }
        
        /*
         * If ourValue is inherited, do NOT add a CandidateItem for it.
         * TODO: clarify. If ourValue is inherited, then indeed there should be an item
         * for "soft" inheritance with INHERITANCE_MARKER, but no item for hard/explicit value unless it has votes.
         * Test if this value of ourValueIsInherited is reliable and consistent with what happens in updateInheritedValue.
         */
        if (ourValueIsInherited && !isExtraPath) {
             return;
        }
        if (altProp != null && !ourValueIsInherited && altProp != SurveyMain.PROPOSED_DRAFT) { // 'draft=true'
            row.hasMultipleProposals = true;
        }
        CLDRLocale setInheritFrom = ourValueIsInherited ? CLDRLocale.getInstance(sourceLocale) : null;
        if (checkCldr != null) {
            checkCldr.check(xpath, checkCldrResult, isExtraPath ? null : ourValue);
            checkCldr.getExamples(xpath, isExtraPath ? null : ourValue, examplesResult);
        }
        if (ourValue != null && ourValue.length() > 0) {
            addOurValue(ourValue, row, checkCldrResult, sourceLocaleStatus, xpath, setInheritFrom, examplesResult);
        }
    }

    /**
     * For each string in the given set, based on values that have votes,
     * add an item to the given row with that string as its value,
     * unless the string matches ourValue.
     * 
     * Also run some tests if appropriate.
     * 
     * @param v the set of values that have votes
     * @param xpath
     * @param row the DataRow
     * @param checkCldr the TestResultBundle, or null
     *
     * TODO: populateFromThisXpathAddItemsForVotes could be a method of DataRow instead of DataSection, then wouldn't need row, xpath as params
     */
    private void populateFromThisXpathAddItemsForVotes(Set<String> v, String xpath, DataRow row, TestResultBundle checkCldr) {
        for (String avalue : v) {
            CandidateItem item2 = row.addItem(avalue, "votes");
            if (avalue != null && checkCldr != null) {
                List<CheckStatus> item2Result = new ArrayList<CheckStatus>();
                checkCldr.check(xpath, item2Result, avalue);
                if (!item2Result.isEmpty()) {
                    item2.setTests(item2Result);
                }
            }
        }
    }

    /**
     * Add an item for "ourValue" to the given DataRow, and set various fields of the DataRow
     *
     * @param ourValue
     * @param row
     * @param checkCldrResult
     * @param sourceLocaleStatus
     * @param xpath
     * @param setInheritFrom
     * @param examplesResult
     * 
     * TODO: addOurValue could be a method of DataRow instead of DataSection, then wouldn't need row, xpath as params
     */
    private void addOurValue(String ourValue, DataRow row, List<CheckStatus> checkCldrResult,
            org.unicode.cldr.util.CLDRFile.Status sourceLocaleStatus, String xpath, CLDRLocale setInheritFrom,
            List<CheckStatus> examplesResult) {

        CandidateItem myItem = row.addItem(ourValue, "ourValue");

        if (DEBUG) {
            System.err.println("Added item " + ourValue + " - now items=" + row.items.size());
        }

        if (!checkCldrResult.isEmpty()) {
            myItem.setTests(checkCldrResult);
            /*
             * set the parent, can't reuse it if nonempty
             */
            checkCldrResult = new ArrayList<CheckStatus>();
        }

        if (sourceLocaleStatus != null && sourceLocaleStatus.pathWhereFound != null
                && !sourceLocaleStatus.pathWhereFound.equals(xpath)) {
            row.pathWhereFound = sourceLocaleStatus.pathWhereFound;
        }
        if (setInheritFrom != null) {
            row.inheritedLocale = setInheritFrom;
        }        

        /*
         * Store who voted for what. [ this could be loaded at displaytime..]
         * 
         * TODO: explain the following block.
         * myItem.examples is assigned to here, but not referenced anywhere else,
         * so what is this block for, and does examples need to be a member of
         * CandidateItem rather than just a local variable here?
         */
        if (!examplesResult.isEmpty()) {
            // reuse the same ArrayList unless it contains something
            if (myItem.examples == null) {
                myItem.examples = new Vector<ExampleEntry>();
            }
            for (Iterator<CheckStatus> it3 = examplesResult.iterator(); it3.hasNext();) {
                CheckCLDR.CheckStatus status = it3.next();
                myItem.examples.add(addExampleEntry(new ExampleEntry(this, row, myItem, status)));
            }
        }
    }

    /**
     * Show a DataSection to the user. Caller must hold session sync.
     *
     * @param ctx
     *            the context to show
     * @param canModify
     *            user is allowed to modify
     * @param only_prefix_xpath
     *            only show this prefix
     * @param zoomedIn
     *            show in zoomed-in mode
     *
     * There are two functions by this name. This one has a String for the 3rd param;
     * the other has an XPathMatcher.
     *
     * Called only by showXpath in SurveyForum.java, always with zoomedIn = true
     */
    void showSection(WebContext ctx, boolean canModify, String only_prefix_xpath, boolean zoomedIn) {
        XPathMatcher matcher = BaseAndPrefixMatcher.getInstance(-1, only_prefix_xpath);

        // Call the method below that has the same name, different parameters
        showSection(ctx, canModify, matcher, zoomedIn);
    }

    /**
     * Show a DataSection to the user.
     *
     * @param ctx
     * @param canModify
     * @param matcher
     * @param zoomedIn
     *
     * Called by another function named showSection in this file, and also by showPathList in SurveyMain.java
     */
    void showSection(WebContext ctx, boolean canModify, XPathMatcher matcher, boolean zoomedIn) {
        int skip = 0; // where the index points to
        int oskip = ctx.fieldInt("skip", 0); // original skip from user.

        UserLocaleStuff uf = null;
        synchronized (ctx.session) {
            uf = ctx.getUserFile();

            /*
             * TODO: remove this call to getCheck unless it has useful side effects. Formerly the return value was assigned
             * to a variable checkCldr which was not accessed.
             */
            uf.getCheck(ctx.getEffectiveCoverageLevel(ctx.getLocale().toString()), ctx.getOptionsMap());

            boolean disputedOnly = ctx.field("only").equals("disputed");

            DisplaySet dSet = getDisplaySet(ctx, matcher);

            if (dSet.size() == 0) {
                ctx.println("<h3>There are no items to display on this page ");
                if (getSkippedDueToCoverage() > 0) {
                    ctx.println("due to the selected coverage level. To see " + getSkippedDueToCoverage() + " skipped items, "
                        + "click on ");

                    WebContext subCtx2 = new WebContext(ctx);
                    subCtx2.removeQuery(SurveyMain.QUERY_LOCALE);
                    subCtx2.removeQuery(SurveyMain.QUERY_LOCALE);
                    subCtx2.removeQuery(SurveyForum.F_FORUM);
                    SurveyMain.printMenu(subCtx2, "", "options", "My Options", SurveyMain.QUERY_DO);

                    ctx.println("and set your coverage level to a higher value.");
                }
                ctx.println("</h3>");
                return;
            }

            boolean checkPartitions = (dSet.partitions.length > 0) && (dSet.partitions[0].name != null);
            int moveSkip = -1; // move the "skip" marker?
            int xfind = ctx.fieldInt(SurveyMain.QUERY_XFIND);
            if (xfind != -1) {
                // see if we can find this base_xpath somewhere..
                int pn = 0;
                for (int i = 0; i < dSet.rows.length && (moveSkip == -1); i++) {
                    DataRow row = dSet.rows[i];
                    if (row.getXpathId() == xfind) {
                        moveSkip = pn;
                    }
                    pn++;
                }
                if (moveSkip != -1) {
                    /*
                     * make it fall on a page boundary
                     */
                    oskip = (moveSkip / ctx.prefCodesPerPage()) * ctx.prefCodesPerPage();
                }
            }
            // -----
            if (!(matcher != null && matcher.getXPath() != XPathTable.NO_XPATH)) {
                skip = dSet.showSkipBox(ctx, oskip);
            } else {
                skip = 0;
            }

            ctx.printUrlAsHiddenFields();

            if (disputedOnly == true) {
                ctx.println("(<b>Disputed Only</b>)<br><input type='hidden' name='only' value='disputed'>");
            }

            ctx.println("<input type='hidden' name='skip' value='" + ctx.field("skip") + "'>");
            DataSection.printSectionTableOpen(ctx, this, zoomedIn, canModify);

            int rowStart = skip; // where should it start?
            int rowCount = ctx.prefCodesPerPage(); // how many to show?

            if (disputedOnly) {
                // we want to go from VETTING_PROBLEMS_LIST[0]..VETTING_PROBLEMS_LIST[n] in range.
                for (int j = 0; j < dSet.partitions.length; j++) {
                    for (String part : DataSection.VETTING_PROBLEMS_LIST) {
                        if (dSet.partitions[j].name.equals(part)) {
                            if (rowStart != skip) { // set once
                                rowStart = dSet.partitions[j].start;
                            }
                            // keep setting this
                            rowCount = (dSet.partitions[j].limit - rowStart);
                        }
                    }
                }
            }

            int rowEnd = rowStart + rowCount;
            if (rowEnd > dSet.rows.length) {
                rowEnd = dSet.rows.length;
            }
            for (int i = rowStart; i < rowEnd; i++) {
                if (checkPartitions) {
                    for (int j = 0; j < dSet.partitions.length; j++) {
                        if ((dSet.partitions[j].name != null)
                            && ((i == dSet.partitions[j].start) || ((i == rowStart) && (i >= dSet.partitions[j].start) && (i < dSet.partitions[j].limit)))) {
                            // ensure the first item has a header.
                            ctx.print("<tr class='heading'><th class='partsection' align='left' colspan='"
                                + SurveyMain.PODTABLE_WIDTH + "'>" + "<a name='" + dSet.partitions[j].name + "'");
                            if (!dSet.partitions[j].helptext.isEmpty()) {
                                ctx.print("title='" + dSet.partitions[j].helptext + "'");
                            }
                            ctx.println(">" + dSet.partitions[j].name + "</a>" + "</th>");
                            ctx.println("</tr>");
                        }
                    }
                }
            }
            sm.printSectionTableClose(ctx, this, canModify);

            if (!(matcher != null && matcher.getXPath() != XPathTable.NO_XPATH)) {
                dSet.showSkipBox(ctx, oskip);
            }

            if (!canModify) {
                ctx.println("<hr> <i>You are not authorized to make changes to this locale.</i>");
            }
        }
    }

    /**
     * Show a single item, in a very limited view.
     *
     * @param ctx
     * @param item_xpath
     *            xpath of the one item to show
     *
     *  Called only by showXpathShort in SurveyForum.java
     *  
     *  Changed name from showPeasShort to showDataRowsShort, and moved from between the two methods
     *  named showSection, 2018-8-19
     */
    public void showDataRowsShort(WebContext ctx, int item_xpath) {
        DataRow row = getDataRow(item_xpath);
        if (row != null) {
            row.showDataRowShort(ctx);
        } else {
            ctx.println("<tr><td colspan='2'>" + ctx.iconHtml("stop", "internal error")
                + "<i>internal error: nothing to show for xpath " + item_xpath + "," + " " + sm.xpt.getById(item_xpath)
                + "</i></td></tr>");
        }
    }

    /**
     * @param ctx
     * @param matcher
     * @return
     */
    public DisplaySet getDisplaySet(WebContext ctx, XPathMatcher matcher) {
        SortMode sortMode = getSortMode(ctx);

        // Get the set of things to display.
        DisplaySet dSet = createDisplaySet(sortMode, matcher);
        return dSet;
    }

    /**
     * @param ctx
     * @return
     */
    public SortMode getSortMode(WebContext ctx) {
        // get the name of the sortmode
        String sortModeName = SortMode.getSortMode(ctx, this);
        // are we in 'disputed-only' mode?
        if (ctx.field("only").equals("disputed")) {
            /*
             * so that disputed shows up on top - force the sortmode.
             */
            sortModeName = SurveyMain.PREF_SORTMODE_WARNING;
        }
        SortMode sortMode = SortMode.getInstance(sortModeName);
        return sortMode;
    }

    /**
     * Convert this DataSection to a string.
     */
    @Override
    public String toString() {
        return "{" + getClass().getSimpleName() + " " + locale + ":" + xpathPrefix + " #" + super.toString() + ", "
            + getAll().size() + " items, pageid " + this.pageId + " } ";
    }

    public void touch() {
        touchTime = System.currentTimeMillis();
    }


    /**
     * Print something...
     *
     * @param ctx
     * @param section
     * @param zoomedIn
     * @param canModify
     * 
     * Called by showXpath in SurveyForum.java, and by showSection
     */
    static void printSectionTableOpen(WebContext ctx, DataSection section, boolean zoomedIn, boolean canModify) {
        ctx.println("<a name='st_data'></a>");
        ctx.println("<table summary='Data Items for " + ctx.getLocale().toString() + " " + section.xpathPrefix
            + "' class='data' border='0'>");

        int table_width = section.hasExamples ? 13 : 10;
        int itemColSpan;
        if (!canModify) {
            table_width -= 4; // No vote, change, or no opinion columns
        }
        if (zoomedIn) {
            table_width += 2;
            itemColSpan = 2; // When zoomed in, Proposed and Other takes up 2 columns
        } else {
            itemColSpan = 1;
        }

        ctx.println("<tr><td colspan='" + table_width + "'>");
        // dataitems_header.jspf
        // some context
        ctx.put(WebContext.DATA_SECTION, section);
        ctx.put(WebContext.ZOOMED_IN, new Boolean(zoomedIn));
        ctx.includeFragment("dataitems_header.jsp");
        ctx.println("</td></tr>");

        ctx.println("<tr class='headingb'>\n" + " <th width='30'>St.</th>\n" + // 1
            " <th width='30'>Draft</th>\n"); // 1
        if (canModify) {
            ctx.print(" <th width='30'>Voted</th>\n"); // 1
        }
        ctx.print(" <th>Code</th>\n" + // 2
            " <th title='[" + SurveyMain.BASELINE_LOCALE + "]'>" + SurveyMain.BASELINE_LANGUAGE_NAME + "</th>\n");
        if (section.hasExamples) {
            ctx.print(" <th title='" + SurveyMain.BASELINE_LANGUAGE_NAME + " [" + SurveyMain.BASELINE_LOCALE
                + "] Example'><i>Ex</i></th>\n");
        }

        ctx.print(" <th colspan=" + itemColSpan + ">" + SurveyMain.getProposedName() + "</th>\n");
        if (section.hasExamples) {
            ctx.print(" <th title='Proposed Example'><i>Ex</i></th>\n");
        }
        ctx.print(" <th colspan=" + itemColSpan + ">" + SurveyMain.CURRENT_NAME + "</th>\n");
        if (section.hasExamples) {
            ctx.print(" <th title='Current Example'><i>Ex</i></th>\n");
        }
        if (canModify) {
            ctx.print(" <th colspan='2' >Change</th>\n"); // 8
            ctx.print("<th width='20' title='No Opinion'>n/o</th>\n"); // 5
        }
        ctx.println("</tr>");
    }

    public static String getValueHash(String str) {
        return (str == null) ? "null" : CookieSession.cheapEncodeString(str);
    }

    public static String fromValueHash(String s) {
        return (s == null || s.equals("null")) ? null : CookieSession.cheapDecodeString(s);
    }

    enum EPrintCellSet {
        doShowValue, doShowRef, doShowExample
    };

    enum EShowDataRowSet {
        doShowTR, doShowValueRows, doShowOtherRows, doShowVettingRows
    };

    public static final EnumSet<EShowDataRowSet> kAjaxRows = EnumSet.of(EShowDataRowSet.doShowValueRows);

    /**
     * Convert this DataSection to a JSON string.
     */
    @Override
    public String toJSONString() throws JSONException {
        JSONObject itemList = new JSONObject();
        JSONObject result = new JSONObject();
        try {
            for (DataRow d : rowsHash.values()) {
                try {
                    String str = d.toJSONString();
                    JSONObject obj = new JSONObject(str);
                    itemList.put(d.fieldHash(), obj);
                } catch (JSONException ex) {
                    SurveyLog.logException(ex, "JSON serialization error for row: "
                        + d.xpath + " : Full row is: " + d.toString());
                    throw new JSONException(ex);
                }
            }
            result.put("rows", itemList);
            result.put("hasExamples", hasExamples);
            result.put("xpathPrefix", xpathPrefix);
            result.put("skippedDueToCoverage", getSkippedDueToCoverage());
            result.put("coverage", getPtype());
            return result.toString();
        } catch (Throwable t) {
            SurveyLog.logException(t, "Trying to load rows for " + this.toString());
            throw new JSONException(t);
        }
    }

    /**
     * Get the DisplayAndInputProcessor for this DataSection; if there isn't one yet, create it
     *
     * @return the processor
     *
     * Called by getProcessedValue
     */
    private DisplayAndInputProcessor getProcessor() {
        if (processor == null) {
            processor = new DisplayAndInputProcessor(SurveyMain.BASELINE_LOCALE, false);
        }
        return processor;
    }

    /**
     * @return the examplebuilder
     */
    private ExampleBuilder getExampleBuilder(CLDRFile file) {
        if (examplebuilder == null) {
            examplebuilder = new ExampleBuilder(sm.getBaselineFile(), file);
        }
        return examplebuilder;
    }

    private ExampleBuilder getExampleBuilder() {
        return examplebuilder;
    }
}
