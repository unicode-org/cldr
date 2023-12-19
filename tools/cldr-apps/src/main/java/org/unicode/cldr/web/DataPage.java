//
//  DataPage.java
//
//  Created by Steven R. Loomis on 18/11/2005.
//  Copyright 2005-2014 IBM. All rights reserved.

// Formerly named "DataSection"; renamed, since this class now handles one "page" at a time, not one
// "section".
// A section is like "Locale Display Names"; a page is like "Languages (A-D)". Generally one section
// contains multiple pages.

package org.unicode.cldr.web;

import com.google.common.collect.ImmutableList;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.Output;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.test.*;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.InputMethod;
import org.unicode.cldr.test.CheckCLDR.Options;
import org.unicode.cldr.test.CheckCLDR.StatusAction;
import org.unicode.cldr.test.TestCache.TestResultBundle;
import org.unicode.cldr.util.*;
import org.unicode.cldr.util.CLDRInfo.CandidateInfo;
import org.unicode.cldr.util.CLDRInfo.PathValueInfo;
import org.unicode.cldr.util.CLDRInfo.UserInfo;
import org.unicode.cldr.util.GrammarInfo.GrammaticalFeature;
import org.unicode.cldr.util.GrammarInfo.GrammaticalScope;
import org.unicode.cldr.util.GrammarInfo.GrammaticalTarget;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.PathHeader.SurveyToolStatus;
import org.unicode.cldr.util.VoteResolver.Status;
import org.unicode.cldr.web.DataPage.DataRow.CandidateItem;
import org.unicode.cldr.web.UserRegistry.User;

/**
 * A DataPage represents a group of related data that will be displayed to users in a list such as,
 * "all of the language display names contained in the en_US locale". It is sortable, as well, and
 * has some level of persistence.
 */
public class DataPage {

    private static final Logger logger = SurveyLog.forClass(DataPage.class);

    /*
     * For debugging only (so far), setting USE_CANDIDATE_HISTORY to true causes
     * a "history" to be constructed and passed to the client for each CandidateItem,
     * indicating how/when/why it was added. This should be false for production.
     */
    private static final boolean USE_CANDIDATE_HISTORY = false;

    /**
     * The comparison-value CLDRFile for this DataPage This is English; see
     * SurveyMain.getEnglishFile
     */
    private CLDRFile comparisonValueFile;

    /** The CLDRFile for the root locale; use lazy initialization, see getRootFile */
    private CLDRFile rootFile = null;

    /** The DisplayAndInputProcessor for this DataPage */
    private DisplayAndInputProcessor processor = null;

    /**
     * A DataRow represents a "row" of data - a single distinguishing xpath
     *
     * <p>This class was formerly named "Pea"
     *
     * @author srl
     */
    public class DataRow implements PathValueInfo {

        /**
         * A CandidateItem represents a particular alternative item which could be chosen or voted
         * for.
         *
         * <p>Each DataRow has, in general, any number of these items in DataRow.items.
         */
        public class CandidateItem implements Comparable<CandidateItem>, CandidateInfo {

            /**
             * rawValue is the actual value of this CandidateItem, that is, the string for which a
             * user might vote.
             *
             * <p>This member is private; use the function getValue() to access it.
             *
             * <p>This member is "final", its value for each CandidateItem is set once and for all
             * when the constructor is called.
             *
             * <p>Renamed from "value" to "rawValue" 2018-8-13, for consistency with the client
             * variable name, to emphasize distinction from getProcessedValue(), and to reduce
             * confusion with other occurrences of the word "value".
             */
            private final String rawValue;

            /**
             * isBaselineValue means the value of this CandidateItem is equal to baselineValue,
             * which is a member of DataRow.
             *
             * <p>isBaselineValue is used on both server and client. On the client, it can result in
             * addIcon(choiceField,"i-star"), and that is its only usage on the client.
             */
            private boolean isBaselineValue = false;

            /** checkedVotes is used only in the function getVotes, for efficiency. */
            private boolean checkedVotes = false;

            /**
             * tests is included in json data sent to client
             *
             * <p>See setTests and getCheckStatusList
             */
            private List<CheckStatus> tests = null;

            public List<CheckStatus> getTests() {
                return tests;
            }

            /** Set of Users who voted on this item */
            private Set<UserRegistry.User> votes = null;

            /** see getValueHash */
            private String valueHash = null;

            /**
             * A history of events in the creation of this CandidateItem, for debugging and possibly
             * for inspection by users; unused (stays null) if USE_CANDIDATE_HISTORY is false.
             */
            private String history = null;

            public String getHistory() {
                return history;
            }

            /**
             * Create a new CandidateItem with the given value
             *
             * @param value, may be null if called by setShimTests!
             *     <p>This constructor sets this.rawValue, which is final (can't be changed once set
             *     here).
             */
            private CandidateItem(String value) {
                this.rawValue = value;
            }

            /**
             * Get the raw value of this CandidateItem
             *
             * @return the value, as a string
             *     <p>Compare getProcessedValue.
             */
            @Override
            public String getValue() {
                return rawValue;
            }

            /**
             * Get the value of this CandidateItem, processed for display.
             *
             * @return the processed value
             *     <p>This is what the client receives by the name "value". Compare what is called
             *     "rawValue" on both server and client.
             */
            public String getProcessedValue() {
                if (rawValue == null) {
                    return null;
                }
                try {
                    return DisplayAndInputProcessorFactory.make(locale)
                            .processForDisplay(xpath, rawValue);
                } catch (Throwable t) {
                    String msg = "getProcessedValue, while processing " + xpath + ":" + rawValue;
                    logger.log(java.util.logging.Level.FINE, msg, t);
                    return rawValue;
                }
            }

            /**
             * Get the hash of the raw value of this candidate item.
             *
             * @return the hash of the raw value
             */
            public String getValueHash() {
                if (valueHash == null) {
                    valueHash = DataPage.getValueHash(rawValue);
                }
                return valueHash;
            }

            /**
             * Compare this CandidateItem with the given other CandidateItem.
             *
             * @param other the other item with which to compare this one
             * @return 0 if they are the same item, else a positive or negative number obtained by
             *     comparing the two values as strings
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
             * @return the set of users
             */
            @Override
            public Collection<UserInfo> getUsersVotingOn() {
                Set<UserRegistry.User> uvotes = getVotes();
                if (uvotes == null) {
                    return Collections.emptySet();
                }
                // TODO: change return type for perf?
                return new TreeSet<>(uvotes);
            }

            /**
             * Get the set of votes for this CandidateItem
             *
             * @return the set of votes
             */
            public Set<UserRegistry.User> getVotes() {
                if (!checkedVotes)
                    synchronized (this) {
                        Set<User> rawVotes = ballotBox.getVotesForValue(xpath, rawValue);
                        if (!rawValue.equals(CldrUtility.INHERITANCE_MARKER)) {
                            // simple case - not triple up arrow just pass this through
                            this.votes = rawVotes;
                        } else {
                            // we need to collect triple up arrow AND hard vots
                            Set<User> allVotes = new TreeSet<User>();
                            if (rawVotes != null) {
                                allVotes.addAll(rawVotes);
                            }
                            // Also add in inherited value (hard) votes
                            Set<User> inhVotes = ballotBox.getVotesForValue(xpath, inheritedValue);
                            if (inhVotes != null) {
                                allVotes.addAll(inhVotes);
                            }
                            // null out if no votes
                            if (allVotes.isEmpty()) {
                                allVotes = null;
                            }
                            this.votes = allVotes;
                        }
                        checkedVotes = true;
                    }
                return votes;
            }

            /**
             * Get the class for this CandidateItem
             *
             * @return the class as a string, for example, "winner"
             *     <p>All return values: "winner", "alias", "fallback", "fallback_code",
             *     "fallback_root", "loser".
             *     <p>Called by CandidateItem.toJSONString (for item.pClass)
             *     <p>Relationships between class, color, and inheritance
             *     (http://cldr.unicode.org/translation/getting-started/guide#TOC-Inheritance): "The
             *     inherited values are color coded: 1. Darker [blue] The original is from a parent
             *     locale, such as if you are working in Latin American Spanish (es_419), this value
             *     is inherited from European Spanish (es). [corresponds with "fallback" and
             *     {background-color: #5bc0de;}] 2. Lighter [violet] The original is in the same
             *     locale, but has a different ID (row). [corresponds with "alias" and
             *     {background-color: #ddf;}] 3. Red The original is from the root." [corresponds
             *     with "fallback_root" or "fallback_code" and {background-color: #FFDDDD;}]
             */
            public String getPClass() {
                if (rawValue.equals(CldrUtility.INHERITANCE_MARKER)) {
                    if (pathWhereFound != null) {
                        return "alias";
                    }
                    if (getInheritedLocale() != null
                            && XMLSource.CODE_FALLBACK_ID.equals(
                                    getInheritedLocale().getBaseName())) {
                        return "fallback_code";
                    }
                    if (getInheritedLocale() != null
                            && XMLSource.ROOT_ID.equals(getInheritedLocale().getBaseName())) {
                        return "fallback_root";
                    }
                    return "fallback";
                }
                if (winningValue != null && winningValue.equals(rawValue)) {
                    /*
                     * An item can be both winning and inherited (alias/fallback). If an item is both
                     * winning and inherited, then its class/style/color is determined by inheritance,
                     * not by whether it's winning.
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
                boolean weHaveTests = false;
                int errorCount = 0;
                int warningCount = 0;
                for (final CheckStatus status : tests) {
                    logger.finest(() -> status + " on " + xpath);
                    if (status.getType() == CheckStatus.exampleType) {
                        continue; // does not count as an error or warning but included in payload
                    }
                    weHaveTests = true;
                    if (status.getType().equals(CheckStatus.errorType)) {
                        errorCount++;
                    } else if (status.getType().equals(CheckStatus.warningType)) {
                        warningCount++;
                    }
                }
                if (weHaveTests) {
                    if (errorCount > 0) /* row */ hasErrors = true;
                    if (warningCount > 0) /* row */ hasWarnings = true;
                }
                return weHaveTests;
            }

            public Map<User, Integer> getOverrides() {
                return ballotBox.getOverridesPerUser(xpath);
            }

            /**
             * Convert this CandidateItem to a string.
             *
             * <p>This function CandidateItem.toString is NOT called to make the json object
             * normally sent to the client.
             *
             * <p>TODO: document the purpose of this function CandidateItem.toString: who calls it;
             * does the output need to be json; is there any need to override the default
             * toString()?
             */
            @Override
            public String toString() {
                String winner =
                        (winningValue != null && winningValue.equals(rawValue)) ? ",winner" : "";
                return "{Item v='" + rawValue + "'" + winner + "}";
            }

            /**
             * Get the example for this CandidateItem
             *
             * @return the example HTML, as a string
             *     <p>Called only by DataPage.DataRow.CandidateItem.toJSONString()
             */
            public String getExample() {
                return nativeExampleGenerator.getExampleHtml(xpath, rawValue);
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

            public boolean isBaselineValue() {
                return isBaselineValue;
            }
        } // end of class CandidateItem

        /*
         * Class DataRow continues here.
         */

        /**
         * inheritedLocale is the locale from which this DataRow inherits.
         *
         * <p>DataRow.inheritedLocale on the server corresponds to theRow.inheritedLocale on the
         * client.
         *
         * <p>inheritedLocale is accessed from InterestSort.java for
         * Partition.Membership("Missing"), otherwise it could be private.
         */
        CLDRLocale inheritedLocale = null;

        /**
         * pathWhereFound, if not null, may be, for example:
         * //ldml/numbers/currencies/currency[@type="AUD"]/displayName[@count="one"]
         *
         * <p>It is the inheritance path for "sideways" inheritance.
         *
         * <p>If not null it may cause getPClass to return "alias".
         */
        private String pathWhereFound = null;

        /*
         * confirmStatus indicates the status of the winning value. It is sent to
         * the client, which displays a corresponding status icon in the "A"
         * ("Approval status") column. See VoteResolver.Status and VoteResolver.getWinningStatus.
         */
        private final Status confirmStatus;

        public Status getConfirmStatus() {
            return confirmStatus;
        }

        /** Calculated coverage level for this DataRow. */
        private int coverageValue;

        public int getCoverageValue() {
            return coverageValue;
        }

        /** the displayName is the value of the 'English' column. */
        private final String displayName;

        /** Same as displayName, but unprocessed */
        private final String rawEnglish;

        // these apply to the 'winning' item, if applicable
        boolean hasErrors = false;

        boolean hasMultipleProposals = false; // true if more than 1 proposal is available

        /** Does this DataRow have warnings? */
        boolean hasWarnings = false;

        /**
         * inheritedItem is a CandidateItem representing the vetted value inherited from parent.
         *
         * <p>Change for https://unicode.org/cldr/trac/ticket/11299 : formerly the rawValue for
         * inheritedItem was the Bailey value. Instead, now rawValue will be INHERITANCE_MARKER, and
         * the Bailey value will be stored in DataRow.inheritedValue.
         *
         * <p>inheritedItem is set by updateInheritedValue and by setShimTests
         */
        private CandidateItem inheritedItem = null;

        /**
         * inheritedValue is the "Bailey" value, that is, the value that this DataRow will have if
         * it inherits from another row or from another locale.
         */
        private String inheritedValue = null;

        public String getInheritedValue() {
            return inheritedValue;
        }

        private String inheritedDisplayValue = null;

        /** like getInheritedValue(), but processed */
        public String getInheritedDisplayValue() {
            return inheritedDisplayValue;
        }

        /** The winning item for this DataRow. */
        private CandidateItem winningItem = null;

        /**
         * The candidate items for this DataRow, stored in a Map whose keys are
         * CandidateItem.rawValue and whose values are CandidateItem objects.
         *
         * <p>Public for access by getRow.
         */
        public Map<String, CandidateItem> items = new TreeMap<>();

        /** Cache of field hash * */
        private String myFieldHash = null;

        /** Used only in the function getPrettyPath */
        private String pp = null;

        /**
         * The pretty path for this DataRow, set by the constructor.
         *
         * <p>Accessed by SortMode.java
         */
        public String prettyPath;

        /**
         * The winning value for this DataRow
         *
         * <p>It gets set by resolver.getWinningValue() by the DataRow constructor.
         */
        private final String winningValue;

        /** The xpath for this DataRow, assigned in the constructor. */
        private final String xpath;

        /** The xpathId for this DataRow, assigned in the constructor based on xpath. */
        private final int xpathId;

        /**
         * The baseline value for this DataRow, that is the previous release version plus latest XML
         * fixes by members of the technical committee (TC). In other words, the current "trunk"
         * value, where "trunk" refers to XML files in version control (on trunk, as opposed to any
         * branch).
         */
        private final String baselineValue;

        /** The baseline status for this DataRow (corresponding to baselineValue) */
        private final Status baselineStatus;

        /** The PathHeader for this DataRow, assigned in the constructor based on xpath. */
        private final PathHeader pathHeader;

        /** The voting transcript, set by the DataRow constructor */
        private final String voteTranscript;

        /** Are there fixed candidates (i.e. plus is prohibited) */
        private boolean haveFixedCandidates = false;

        public String getVoteTranscript() {
            return voteTranscript;
        }

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

            winningValue = resolver.getWinningValue();
            confirmStatus = resolver.getWinningStatus();
            voteTranscript = resolver.getTranscript();

            baselineValue = resolver.getBaselineValue();
            baselineStatus = resolver.getBaselineStatus();

            rawEnglish = comparisonValueFile.getStringValue(xpath);

            Output<String> pathWhereFound = new Output<String>(),
                    localeWhereFound = new Output<String>();
            comparisonValueFile.getStringValueWithBailey(xpath, pathWhereFound, localeWhereFound);
            final boolean samePath = xpath.equals(pathWhereFound.value);
            if (!samePath) {
                // zero out displayName if it's sideways inheritance
                displayName = "";
            } else {
                displayName = getBaselineProcessor().processForDisplay(xpath, rawEnglish);
            }

            addFixedCandidates();
        }

        /** check to see if there are any 'fixed' values, i.e. no freeform input is allowed. */
        public void addFixedCandidates() {
            Collection<String> candidates = getFixedCandidates();
            // Could have other XPaths here

            if (candidates == null || candidates.isEmpty()) {
                return;
            }
            haveFixedCandidates = true;
            for (final String candidate : candidates) {
                addItem(candidate, "fixed");
            }
        }

        private Collection<String> getFixedCandidates() {
            if (PatternCache.get("^//ldml/units/unitLength.*/unit.*/gender")
                    .matcher(xpath)
                    .matches()) {
                return grammarInfo.get(
                        GrammaticalTarget.nominal,
                        GrammaticalFeature.grammaticalGender,
                        GrammaticalScope.units);
            }
            return Collections.emptySet();
        }

        /**
         * Add a new CandidateItem to this DataRow, with the given value; or, if this DataRow
         * already has an item with this value, return it.
         *
         * <p>If the item is new, then: check whether the item is winning, and if so make
         * winningItem point to it; check whether the item matches baselineValue, and if so set
         * isBaselineValue = true.
         *
         * @param value
         * @param candidateHistory a string used for debugging and possibly also for describing to
         *     the user how/why/when/where the item was added
         * @return the new or existing item with the given value
         *     <p>Sequential order in which addItem may be called (as of 2019-04-19) for a given
         *     DataRow:
         *     <p>(1) For INHERITANCE_MARKER (if inheritedValue = ourSrc.getBaileyValue not null):
         *     in updateInheritedValue (called by populateFromThisXpath): inheritedItem =
         *     addItem(CldrUtility.INHERITANCE_MARKER, "inherited");
         *     <p>(2) For votes: in populateFromThisXpathAddItemsForVotes (called by
         *     populateFromThisXpath): CandidateItem item2 = row.addItem(avalue, "votes");
         *     <p>(3) For winningValue: in populateFromThisXpath: row.addItem(row.winningValue,
         *     "winning");
         *     <p>(4) For baselineValue (if not null): in populateFromThisXpath:
         *     row.addItem(row.baselineValue, "baseline");
         *     <p>(5) For ourValue: in addOurValue (called by populateFromThisXpath): CandidateItem
         *     myItem = row.addItem(ourValue, "our");
         */
        private CandidateItem addItem(String value, String candidateHistory) {
            if (value == null) {
                return null;
            }
            if (VoteResolver.DROP_HARD_INHERITANCE && value.equals(inheritedValue)) {
                value = CldrUtility.INHERITANCE_MARKER;
            }
            CandidateItem item = items.get(value);
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
            items.put(value, item);
            if (winningValue != null && winningValue.equals(value)) {
                winningItem = item;
            }
            if (baselineValue != null
                    && (baselineValue.equals(value)
                            || (CldrUtility.INHERITANCE_MARKER.equals(value)
                                    && baselineValue.equals(inheritedValue)))) {
                item.isBaselineValue = true;
            }
            return item;
        }

        /** Calculate the hash used for HTML forms for this DataRow. */
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
         *     <p>"The type DataPage.DataRow must implement the inherited abstract method
         *     CLDRInfo.PathValueInfo.getCurrentItem()
         */
        @Override
        public CandidateItem getCurrentItem() {
            return winningItem;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getRawEnglish() {
            return rawEnglish;
        }

        /** Get the locale for this DataRow */
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
         *     <p>Called only from SurveyAjax.java, as "ci = pvi.getItem(candVal)".
         */
        public CandidateItem getItem(String value) {
            return items.get(value);
        }

        /** Get a list of proposed items, if any, for this DataRow. */
        @Override
        public Collection<? extends CandidateInfo> getValues() {
            return items.values();
        }

        public String getWinningValue() {
            return winningValue;
        }

        /** Get the xpath for this DataRow */
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
         * If inheritedItem is null, possibly create inheritedItem; otherwise do nothing.
         *
         * <p>inheritedItem is normally null when setShimTests is called by populateFromThisXpath,
         * unless setShimTests has already been called by ensureComplete, for some timezones.
         *
         * <p>A Shim is a candidate item which does not correspond to actual XML data, but is
         * synthesized.
         *
         * @param base_xpath_string
         * @param checkCldr
         *     <p>Called by populateFromThisXpath (if isExtraPath), and by ensureComplete (for
         *     timezones)
         */
        private void setShimTests(String base_xpath_string, TestResultBundle checkCldr) {
            if (inheritedItem == null) {
                CandidateItem shimItem = new CandidateItem(null);
                List<CheckStatus> iTests = new ArrayList<>();
                checkCldr.check(base_xpath_string, iTests, null);
                STFactory.removeExcludedChecks(iTests);
                if (!iTests.isEmpty()) {
                    // Got a bite.
                    if (shimItem.setTests(iTests)) {
                        // had valid tests
                        inheritedItem = shimItem;
                    }
                }
            }
        }

        /** Convert this DataRow to a string. */
        @Override
        public String toString() {
            return "{DataRow n='"
                    + getDisplayName()
                    + "', x='"
                    + xpath
                    + "', item#='"
                    + items.size()
                    + "'}";
        }

        /**
         * Calculate the inherited item for this DataRow, possibly including tests; possibly set
         * some fields in the DataRow, which may include inheritedValue, inheritedItem,
         * inheritedLocale, pathWhereFound
         *
         * @param ourSrc the CLDRFile
         * @param checkCldr the tests to use
         *     <p>Called only by populateFromThisXpath, which is a method of DataPage.
         *     <p>Reference: Distinguish two kinds of votes for inherited value in Survey Tool
         *     https://unicode.org/cldr/trac/ticket/11299 This function formerly created a
         *     CandidateItem with value equal to the Bailey value, which made it look like a
         *     "hard/explicit" vote for the Bailey value, NOT a "soft/implicit" vote for
         *     inheritance, which would have value equal to INHERITANCE_MARKER. Horrible confusion
         *     was the result. This function has been changed to set the value to
         *     INHERITANCE_MARKER, and to store the actual Bailey value in the inheritedValue field
         *     of DataRow.
         *     <p>TODO: Get rid of, or merge with, the code that currently does
         *     'row.addItem(CldrUtility.INHERITANCE_MARKER, "getCountry")' in populateFromThisXpath.
         *     <p>Normally (always?) inheritedItem is null when this function is called; however, in
         *     principle it may be possible that inheritedItem isn't null due to ensureComplete
         *     calling setShimTests.
         */
        private void updateInheritedValue(CLDRFile ourSrc, TestResultBundle checkCldr) {
            long lastTime = System.currentTimeMillis();

            /*
             * Set the inheritedValue field of the DataRow containing this CandidateItem.
             * Also possibly set the inheritedLocale and pathWhereFound fields of the DataRow.
             */
            Output<String> inheritancePathWhereFound = new Output<>(); // may become pathWhereFound
            Output<String> localeWhereFound =
                    new Output<>(); // may be used to construct inheritedLocale
            inheritedValue =
                    ourSrc.getBaileyValue(xpath, inheritancePathWhereFound, localeWhereFound);

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
                 * TODO: what are the implications when ourSrc.getBaileyValue has returned null?
                 * Unless we're at root, shouldn't there always be a non-null inheritedValue here?
                 * See https://unicode.org/cldr/trac/ticket/11299
                 */
                // System.out.println("Warning: no inherited value in updateInheritedValue; xpath =
                // " + xpath);
                inheritedDisplayValue = null;
            } else {
                /*
                 * Unless this DataRow already has an item with value INHERITANCE_MARKER,
                 * add a new item, to be the inheritedItem of this DataRow, with value INHERITANCE_MARKER.
                 *
                 * Call addItem even if item with this value already exists, for simplicity and to update inheritedItem.history.
                 *
                 * Set inheritedItem = the item with value INHERITANCE_MARKER.
                 */
                inheritedItem = addItem(CldrUtility.INHERITANCE_MARKER, "inherited");

                if (TRACE_TIME) {
                    System.err.println("@@2:" + (System.currentTimeMillis() - lastTime));
                }
                inheritedLocale = CLDRLocale.getInstance(localeWhereFound.value);

                if (inheritancePathWhereFound.value != null
                        && !inheritancePathWhereFound.value.equals(xpath)) {
                    pathWhereFound = inheritancePathWhereFound.value;

                    if (TRACE_TIME) {
                        System.err.println("@@3:" + (System.currentTimeMillis() - lastTime));
                    }
                }
                // Note: the inherited value uses the child locale
                inheritedDisplayValue =
                        DisplayAndInputProcessorFactory.make(locale)
                                .processForDisplay(xpath, inheritedValue);
            }
            if ((checkCldr != null) && (inheritedItem != null) && (inheritedItem.tests == null)) {
                if (TRACE_TIME) {
                    System.err.println("@@5:" + (System.currentTimeMillis() - lastTime));
                }

                List<CheckStatus> iTests = new ArrayList<>();

                checkCldr.check(xpath, iTests, inheritedValue);
                STFactory.removeExcludedChecks(iTests);

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
         * @return true if user has voted at all, false otherwise. Return false if user changes
         *     their vote back to no opinion (abstain).
         */
        public boolean userHasVoted(int userId) {
            return ballotBox.userDidVote(sm.reg.getInfo(userId), getXpath());
        }

        public String getInheritedXPath() {
            if (pathWhereFound != null
                    && !pathWhereFound.equals(GlossonymConstructor.PSEUDO_PATH)) {
                return XPathTable.getStringIDString(pathWhereFound);
            }
            return null;
        }

        public String getWinningVHash() {
            return DataPage.getValueHash(winningValue);
        }

        public String getVoteVHash() {
            String voteVhash = null; // abstension
            if (userForVotelist != null) {
                String ourVote = ballotBox.getVoteValue(userForVotelist, xpath);
                if (ourVote != null) {
                    CandidateItem voteItem = items.get(ourVote);
                    if (voteItem == null) {
                        // inherited value matches inheritance marker and vice-versa
                        if (ourVote.equals(inheritedValue)) {
                            voteItem = items.get(CldrUtility.INHERITANCE_MARKER);
                        } else if (ourVote.equals(CldrUtility.INHERITANCE_MARKER)) {
                            voteItem = items.get(inheritedValue);
                        }
                    }
                    if (voteItem != null) {
                        voteVhash = voteItem.getValueHash();
                    } else {
                        logger.severe(
                                "Found ourVote = "
                                        + ourVote
                                        + " but did not find voteItem for xpath = "
                                        + xpath);
                    }
                }
            }
            return voteVhash;
        }

        public String getRDFURI() {
            return AbstractCacheManager.getInstance().resourceUriForXpath(xpath);
        }

        public String getHelpHTML() {
            return nativeExampleGenerator.getHelpHtml(xpath, rawEnglish);
        }

        public String getDisplayExample() {
            String displayExample = null;
            if (displayName != null) {
                displayExample =
                        sm.getComparisonValuesExample().getNonTrivialExampleHtml(xpath, rawEnglish);
            }
            return displayExample;
        }

        public boolean userHasVoted() {
            return userForVotelist != null && userHasVoted(userForVotelist.id);
        }

        public boolean isFlagged() {
            return sm.getSTFactory().getFlag(locale, xpathId);
        }

        public VoteResolver<String> getResolver() {
            return ballotBox.getResolver(xpath);
        }

        public String getDirectionality() {
            if (getPathHeader().getSurveyToolStatus() == SurveyToolStatus.LTR_ALWAYS) return "ltr";
            else return null;
        }

        public String getCode() {
            String code = "?";
            if (getPathHeader() != null) {
                code = getPathHeader().getCode();
            }
            return code;
        }

        /** A Map used only in getNonDistinguishingAttributes */
        private Map<String, String> nonDistinguishingAttributes = null;

        /** A boolean used only in getNonDistinguishingAttributes */
        private boolean checkedNDA = false;

        /** See addAnnotationRootValue and isUnvotableRoot. */
        private String unvotableRootValue = null;

        /**
         * Get the map of non-distinguishing attributes for this DataRow
         *
         * @return the map
         *     <p>Called only by DataRow.toJSONString
         */
        public Map<String, String> getNonDistinguishingAttributes() {
            if (!checkedNDA) {
                String fullDiskXPath = diskFile.getFullXPath(xpath);
                nonDistinguishingAttributes = sm.xpt.getUndistinguishingElementsFor(fullDiskXPath);
                checkedNDA = true;
            }
            return nonDistinguishingAttributes;
        }

        /**
         * Get the StatusAction for this DataRow
         *
         * @return the StatusAction
         */
        public StatusAction getStatusAction(InputMethod inputMethod) {
            // null because this is for display.
            return SurveyMain.phase()
                    .getCPhase()
                    .getShowRowAction(this, inputMethod, getPathHeader(), userForVotelist);
        }

        /**
         * Get the status action for a DIRECT (SurveyTool) action
         *
         * @return
         */
        public StatusAction getStatusAction() {
            return getStatusAction(InputMethod.DIRECT);
        }

        /** Get the baseline value for this DataRow */
        @Override
        public String getBaselineValue() {
            return baselineValue;
        }

        /**
         * Get the baseline status for this DataRow
         *
         * <p>Called by getShowRowAction
         */
        @Override
        public Status getBaselineStatus() {
            return baselineStatus;
        }

        /** Get the coverage level for this DataRow */
        @Override
        public Level getCoverageLevel() {
            // access the private method of the enclosing class
            return getCoverageInfo().getCoverageLevel(getXpath(), locale.getBaseName());
        }

        /**
         * Was there at least one vote for this DataRow at the end of data submission, or is there a
         * vote now?
         *
         * @return true if there was at least one vote
         *     <p>TODO: add check for whether there was a vote in data submission.
         */
        @Override
        public boolean hadVotesSometimeThisRelease() {
            return ballotBox.hadVotesSometimeThisRelease(xpathId);
        }

        /**
         * Does this DataRow have null for its inherited value?
         *
         * @return true if inheritedValue is null, else false.
         */
        public boolean wouldInheritNull() {
            return inheritedValue == null;
        }

        /**
         * For annotations, include the root value as a candidate item that can't be voted for. This
         * is so that people can search for, e.g., "E12" to find rows for emoji that are new.
         */
        private void addAnnotationRootValue() {
            if (AnnotationUtil.pathIsAnnotation(xpath)) {
                String rootValue = getRootFile().getStringValue(xpath);
                if (rootValue != null && !rootValue.equals(inheritedValue)) {
                    /*
                     * Complication: typically rootValue contains a hyphen, as in "E10-520",
                     * but if the user votes for that value, the hyphen will be converted into an
                     * en dash, as in "E10–520", when SurveyAjax.processRequest calls processInput.
                     * Call processInput here as well, so that isUnvotableRoot can match correctly.
                     */
                    unvotableRootValue =
                            DisplayAndInputProcessorFactory.make(locale)
                                    .processInput(xpath, rootValue, null);
                    addItem(unvotableRootValue, "root-annotation");
                }
            }
        }

        /**
         * Does the given value match the unvotable root value for this DataRow?
         *
         * <p>For some annotations, the root value may be shown as a candidate item for convenience
         * of searching, but it can't be voted for.
         *
         * <p>Reference: https://unicode-org.atlassian.net/browse/CLDR-11157
         *
         * @param val the given value
         * @return true if it matches unvotableRootValue, else false
         */
        public boolean isUnvotableRoot(String val) {
            return unvotableRootValue != null && unvotableRootValue.equals(val);
        }

        public CLDRLocale getInheritedLocale() {
            return inheritedLocale;
        }

        public String getInheritedLocaleName() {
            if (inheritedLocale != null) {
                return inheritedLocale.getBaseName();
            } else {
                return null;
            }
        }

        public String getTranslationHint() {
            return TranslationHints.get(xpath);
        }

        public boolean fixedCandidates() {
            return haveFixedCandidates;
        }
    }

    /*
     * The user somehow related to this DataPage?
     * TODO: clarify what userForVotelist means
     */
    private User userForVotelist = null;

    /**
     * Set the user for this DataPage
     *
     * <p>Somehow related to vote list?
     *
     * @param u the User
     *     <p>TODO: Determine whether we need DataPage to be user-specific, as userForVotelist
     *     implies
     *     <p>Called by getRow, make, submitVoteOrAbstention, and handleBulkSubmit
     */
    public void setUserForVotelist(User u) {
        userForVotelist = u;
    }

    /**
     * Get the CLDRFile for the root locale
     *
     * <p>Keep a reference since sm.getSTFactory().make() may be expensive. Use lazy initialization
     * since it may not be needed by every DataPage.
     */
    private CLDRFile getRootFile() {
        if (rootFile == null) {
            rootFile = sm.getSTFactory().make(CLDRLocale.ROOT.getBaseName(), true);
        }
        return rootFile;
    }

    /** A DisplaySet represents a list of rows, in sorted and divided order. */
    public static class DisplaySet {

        /**
         * Partitions divide up the rows into sets, such as 'proposed', 'normal', etc. The 'limit'
         * is one more than the index number of the last item. In some cases, there is only one
         * partition, and its name is null.
         *
         * <p>Display group partitions. Might only contain one entry: {null, 0, <end>}. Otherwise,
         * contains a list of entries to be named separately
         *
         * <p>public for VoteAPI usage, json serialization
         */
        public Partition[] partitions;

        private final DataRow[] rows; // list of rows in sorted order

        // public for VoteAPI usage, serialization for json
        // only the hashKey for each row
        public String[] getRows() {
            String[] hashKeys = new String[rows.length];
            for (int i = 0; i < rows.length; i++) {
                hashKeys[i] = rows[i].fieldHash();
            }
            return hashKeys;
        }

        // public for VoteAPI usage, json serialization
        public SortMode sortMode;

        /**
         * Create a DisplaySet object
         *
         * @param myRows the original rows
         * @param sortMode the sort mode to use
         */
        public DisplaySet(DataRow[] myRows, SortMode sortMode, Partition[] partitions) {
            this.sortMode = sortMode;
            this.partitions = partitions;
            rows = myRows;
        }
    }

    /** The ExampleEntry class represents an Example box, so that it can be stored and restored. */
    public class ExampleEntry {

        public DataPage.DataRow dataRow;

        public String hash;
        public DataRow.CandidateItem item;
        public DataPage page;
        public CheckCLDR.CheckStatus status;

        /**
         * Create a new ExampleEntry
         *
         * @param page the DataPage
         * @param row the DataRow
         * @param item the CandidateItem
         * @param status the CheckStatus
         */
        public ExampleEntry(
                DataPage page,
                DataRow row,
                DataRow.CandidateItem item,
                CheckCLDR.CheckStatus status) {
            this.page = page;
            this.dataRow = row;
            this.item = item;
            this.status = status;

            /*
             * unique serial #- covers item, status.
             *
             * fieldHash ensures that we don't get the wrong field.
             */
            hash = CookieSession.cheapEncode(DataPage.getN()) + row.fieldHash();
        }
    }

    /** Divider denoting a specific Continent division. */
    public static final String CONTINENT_DIVIDER = "~";

    private static final boolean DEBUG = CldrUtility.getProperty("TEST", false);

    /*
     * A Pattern matching paths that are always excluded
     */
    private static Pattern excludeAlways;

    /*
     * Has this DataPage been initialized?
     * Used only in the function init()
     */
    private static boolean isInitted = false;

    /*
     * A Pattern used only in the function isName()
     */
    private static final Pattern NAME_TYPE_PATTERN =
            PatternCache.get("[a-zA-Z0-9]+|.*exemplarCity.*");

    /** Trace in detail time taken to populate? */
    private static final boolean TRACE_TIME = false;

    /** Field to cache the Coverage info */
    private static CoverageInfo covInfo = null;

    /** Synchronization Mutex used for accessing/setting the coverageInfo object */
    private static final Object GET_COVERAGEINFO_SYNC = new Object();

    /*
     * Warn user why these messages are showing up.
     */
    static {
        if (TRACE_TIME) {
            System.err.println("DataPage: Note, TRACE_TIME is TRUE");
        }
    }

    /** A serial number, used only in the function getN() */
    private static int n = 0;

    /**
     * Get a unique serial number
     *
     * @return the number
     *     <p>Called only by the ExampleEntry constructor
     */
    protected static synchronized int getN() {
        return ++n;
    }

    /** Initialize this DataPage if it hasn't already been initialized */
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
            PatternCache.get(
                    "^//ldml/localeDisplayNames.*|"
                            /* these are excluded when 'misc' is chosen. */
                            + "^//ldml/characters/exemplarCharacters.*|"
                            + "^//ldml/numbers.*|"
                            + "^//ldml/units.*|"
                            + "^//ldml/references.*|"
                            + "^//ldml/dates/timeZoneNames/zone.*|"
                            + "^//ldml/dates/timeZoneNames/metazone.*|"
                            + "^//ldml/dates/calendar.*|"
                            + "^//ldml/identity.*");

            /* Always excluded. Compare with PathHeader/Coverage. */
            excludeAlways =
                    PatternCache.get(
                            "^//ldml/segmentations.*|"
                                    + "^//ldml/measurement.*|"
                                    + ".*week/minDays.*|"
                                    + ".*week/firstDay.*|"
                                    + ".*/usesMetazone.*|"
                                    + ".*week/weekendEnd.*|"
                                    + ".*week/weekendStart.*|"
                                    + "^//ldml/posix/messages/.*expr$|"
                                    + "^//ldml/dates/timeZoneNames/.*/GMT.*exemplarCity$|"
                                    + "^//ldml/dates/.*default"); // no defaults
            isInitted = true;
        }
    }

    /**
     * Create, populate, and complete a DataPage given the specified locale and prefix
     *
     * @param pageId the PageId, with a name such as "Generic" or "T_NAmerica", and an id with a
     *     name such as "DateTime" or "Locale_Display_Names"; or null
     * @param session
     * @param locale
     * @param prefix the XPATH prefix, such as ...; or null
     * @param matcher
     * @return the DataPage
     *     <p>Called by SurveyAjax.handleBulkSubmit and VoteApiHelper
     */
    public static DataPage make(
            PageId pageId,
            CookieSession session,
            CLDRLocale locale,
            String prefix,
            XPathMatcher matcher,
            TestResultBundle checkCldr) {

        SurveyMain sm =
                CookieSession
                        .sm; // TODO: non-deprecated way of getting sm -- could be ctx.sm unless ctx
        // is null

        DataPage page = new DataPage(pageId, sm, locale, prefix, matcher);

        CLDRFile ourSrc = sm.getSTFactory().make(locale.getBaseName());

        ourSrc.setSupplementalDirectory(sm.getSupplementalDirectory());

        if (session == null) {
            throw new InternalError("session == null");
        }
        if (session.user != null) {
            page.setUserForVotelist(session.user);
        }

        if (ourSrc.getSupplementalDirectory() == null) {
            throw new InternalError("?!! ourSrc hsa no supplemental dir!");
        }
        synchronized (session) {
            if (checkCldr == null) {
                checkCldr = sm.getSTFactory().getTestResult(locale, getSimpleOptions(locale));
            }
            if (checkCldr == null) {
                throw new InternalError("checkCldr == null");
            }
            page.comparisonValueFile = sm.getEnglishFile();

            page.nativeExampleGenerator =
                    TestCache.getExampleGenerator(locale, ourSrc, page.comparisonValueFile);

            page.populateFrom(ourSrc, checkCldr);
            /*
             * Call ensureComplete if and only if pageId is null. TODO: Explain, why?
             * pageId is null when called from submitVoteOrAbstention, and also
             * when a user selects a "Fix" button in the Dashboard. Ordinarily
             * when the user opens a page, pageId is not null.
             */
            if (pageId == null) {
                page.ensureComplete(checkCldr);
            }
        }
        return page;
    }

    /**
     * Get the options for the given WebContext, or, if the context is null, get the options for the
     * given CookieSession and CLDRLocale
     *
     * @param session
     * @param locale
     * @return the CheckCLDR.Options object
     */
    public static CheckCLDR.Options getOptions(CookieSession session, CLDRLocale locale) {
        CheckCLDR.Options options;
        final String def =
                CookieSession.sm.getListSetting(
                        session.settings(), SurveyMain.PREF_COVLEV, WebContext.PREF_COVLEV_LIST);

        final String org = session.getEffectiveCoverageLevel(locale.toString());

        options = getOptions(locale, def, org);
        return options;
    }

    private static CheckCLDR.Options getOptions(
            CLDRLocale locale, final String defaultLevel, final String org) {
        return new Options(locale, SurveyMain.getTestPhase(), defaultLevel, org);
    }

    /** Get options, but don't try to check user preferences */
    public static CheckCLDR.Options getSimpleOptions(CLDRLocale locale) {
        return new Options(
                locale,
                SurveyMain.getTestPhase(),
                Level.COMPREHENSIVE.name(), /* localeType is not used?! */
                "NOT USED");
    }

    private final BallotBox<User> ballotBox;

    /*
     * hashtable of type->Row
     */
    Hashtable<String, DataRow> rowsHash = new Hashtable<>();

    private final SurveyMain sm;
    private String xpathPrefix;

    private final CLDRLocale locale;
    private ExampleGenerator nativeExampleGenerator;
    private final XPathMatcher matcher;
    private final PageId pageId;
    private CLDRFile diskFile;

    private static final boolean DEBUG_DATA_PAGE = false;
    private String creationTime = null; // only used if DEBUG_DATA_PAGE

    private GrammarInfo grammarInfo;

    DataPage(PageId pageId, SurveyMain sm, CLDRLocale loc, String prefix, XPathMatcher matcher) {
        this.locale = loc;
        this.sm = sm;
        this.matcher = matcher;
        xpathPrefix = prefix;
        ballotBox = sm.getSTFactory().ballotBoxForLocale(locale);
        this.pageId = pageId;

        if (DEBUG_DATA_PAGE) {
            creationTime =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                            .format(Calendar.getInstance().getTime());
            System.out.println("🌴 Created new DataPage for loc " + loc + " at " + creationTime);
        }
        grammarInfo = sm.getSupplementalDataInfo().getGrammarInfo(locale.getBaseName());
    }

    /**
     * Add the given DataRow to this DataPage
     *
     * @param row the DataRow
     *     <p>Called only by getDataRow
     */
    void addDataRow(DataRow row) {
        rowsHash.put(row.xpath, row);
    }

    /**
     * Get the page id
     *
     * @return pageId
     *     <p>Called by getRow
     */
    public PageId getPageId() {
        return pageId;
    }

    /**
     * Create a DisplaySet for this DataPage
     *
     * @param sortMode
     * @return the DisplaySet
     *     <p>Called by getRow
     */
    public DisplaySet createDisplaySet(SortMode sortMode) {
        return sortMode.createDisplaySet(null /* matcher */, rowsHash.values());
    }

    /**
     * Makes sure this DataPage contains the rows we'd like to see related to timeZoneNames
     *
     * <p>Called only by DataPage.make, only when pageId == null
     *
     * <p>TODO: explain this mechanism and why it's limited to DataPage, not shared with Dashboard,
     * CLDRFile, or any other module. Is it in any way related to CLDRFile.getRawExtraPathsPrivate?
     */
    private void ensureComplete(TestResultBundle checkCldr) {
        if (!xpathPrefix.startsWith("//ldml/dates/timeZoneNames")) {
            return;
        }
        STFactory stf = sm.getSTFactory();
        // work on zones
        boolean isMetazones = xpathPrefix.startsWith("//ldml/dates/timeZoneNames/metazone");
        boolean isSingleXPath = false;
        // Make sure the DataPage contains the rows we'd like to see.
        // regular zone

        Set<String> zoneIterator;
        String podBase;

        if (isMetazones) {
            if (xpathPrefix.indexOf(CONTINENT_DIVIDER) > 0) {
                String[] pieces = xpathPrefix.split(CONTINENT_DIVIDER, 2);
                xpathPrefix = pieces[0];
                zoneIterator = sm.getMetazones(pieces[1]);
            } else { // This is just a single metazone from a zoom-in
                Set<String> singleZone = new HashSet<>();
                XPathParts xpp = XPathParts.getFrozenInstance(xpathPrefix);
                String singleMetazoneName = xpp.findAttributeValue("metazone", "type");
                if (singleMetazoneName == null) {
                    throw new NullPointerException(
                            "singleMetazoneName is null for xpp:" + xpathPrefix);
                }
                singleZone.add(singleMetazoneName);
                zoneIterator = singleZone;

                isSingleXPath = true;
            }
            podBase = "//ldml/dates/timeZoneNames/metazone";
        } else {
            if (xpathPrefix.indexOf(CONTINENT_DIVIDER) > 0) {
                throw new InternalError(
                        "Error: CONTINENT_DIVIDER found on non-metazone xpath " + xpathPrefix);
            }
            zoneIterator = StandardCodes.make().getGoodAvailableCodes("tzid");
            podBase = "//ldml/dates/timeZoneNames/zone";
        }
        if (!isSingleXPath && xpathPrefix.contains("@type")) {
            isSingleXPath = true;
        }

        final String[] tzsuffs = {"/exemplarCity"};
        final String[] mzsuffs = {
            "/long/generic",
            "/long/daylight",
            "/long/standard",
            "/short/generic",
            "/short/daylight",
            "/short/standard"
        };

        String[] suffs;
        if (isMetazones) {
            suffs = mzsuffs;
        } else {
            suffs = tzsuffs;
        }

        for (String zone : zoneIterator) {
            if (zone == null) {
                throw new NullPointerException(
                        "zoneIterator.next() returned null! zoneIterator.size: "
                                + zoneIterator.size()
                                + ", isEmpty: "
                                + zoneIterator.isEmpty());
            }
            /* some compatibility */
            String ourSuffix = "[@type=\"" + zone + "\"]";

            for (String suff : suffs) {
                if (isSingleXPath && !xpathPrefix.contains(suff)) {
                    continue; // Try not to add paths that won't be shown.
                }
                // synthesize a new row..
                String base_xpath_string = podBase + ourSuffix + suff;

                PathHeader ph = stf.getPathHeader(base_xpath_string);
                if (ph == null || ph.shouldHide()) {
                    continue;
                }

                int xpid = sm.xpt.getByXpath(base_xpath_string);
                if (matcher != null && !matcher.matches(base_xpath_string, xpid)) {
                    continue;
                }
                if (excludeAlways.matcher(base_xpath_string).matches()) {
                    continue;
                }

                // Only display metazone data for which an English value exists
                if (isMetazones && !Objects.equals(suff, "/commonlyUsed")) {
                    String engValue = comparisonValueFile.getStringValue(base_xpath_string);
                    if (engValue == null || engValue.length() == 0) {
                        continue;
                    }
                }
                // Filter out data that is higher than the desired coverage level
                int coverageValue =
                        getCoverageInfo().getCoverageValue(base_xpath_string, locale.getBaseName());

                DataRow myp = getDataRow(base_xpath_string); /* rowXPath */

                myp.coverageValue = coverageValue;

                // set up tests
                myp.setShimTests(base_xpath_string, checkCldr);
            } // end inner for loop
        } // end outer for loop
    }

    /**
     * Get the CoverageInfo object from CLDR for this DataPage
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
     * Get all rows for this DataPage, unsorted
     *
     * @return the Collection of DataRow
     */
    public Collection<DataRow> getAll() {
        return rowsHash.values();
    }

    /**
     * Get the row for the given xpath in this DataPage
     *
     * <p>Linear search for matching item.
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
     * Populate this DataPage
     *
     * @param ourSrc the CLDRFile
     * @param checkCldr the TestResultBundle
     */
    private void populateFrom(CLDRFile ourSrc, TestResultBundle checkCldr) {
        STFactory stf = sm.getSTFactory();
        diskFile = stf.getDiskFile(locale);
        String workPrefix = xpathPrefix;

        Set<String> allXpaths;

        Set<String> extraXpaths = null;

        if (pageId != null) {
            allXpaths = PathHeader.Factory.getCachedPaths(pageId.getSectionId(), pageId);
            allXpaths.retainAll(stf.getPathsForFile(locale));
        } else {
            init(); // pay for the patterns
            extraXpaths = new HashSet<>();

            /* Determine which xpaths to show */
            if (xpathPrefix.startsWith("//ldml/dates/timeZoneNames/metazone")) {
                String continent = null;
                int continentStart = xpathPrefix.indexOf(DataPage.CONTINENT_DIVIDER);
                if (continentStart > 0) {
                    continent =
                            xpathPrefix.substring(
                                    xpathPrefix.indexOf(DataPage.CONTINENT_DIVIDER) + 1);
                }
                if (DEBUG) {
                    System.err.println(xpathPrefix + ": -> continent " + continent);
                }
                if (!xpathPrefix.contains("@type")) {
                    // if it's not a zoom-in..
                    workPrefix = "//ldml/dates/timeZoneNames/metazone";
                }
            }

            /* Build the set of xpaths */
            // iterate over everything in this prefix ..
            Set<String> baseXpaths = stf.getPathsForFile(locale, xpathPrefix);

            allXpaths = new HashSet<>(baseXpaths);
            if (ourSrc.getSupplementalDirectory() == null) {
                throw new InternalError("?!! ourSrc hsa no supplemental dir!");
            }
            ourSrc.getExtraPaths(workPrefix, extraXpaths);
            extraXpaths.removeAll(baseXpaths);
            allXpaths.addAll(extraXpaths);

            // Process extra paths.
            if (DEBUG) {
                System.err.println(
                        "@@X@ base["
                                + workPrefix
                                + "]: "
                                + baseXpaths.size()
                                + ", extra: "
                                + extraXpaths.size());
            }
        }
        populateFromAllXpaths(allXpaths, workPrefix, ourSrc, extraXpaths, stf, checkCldr);
    }

    /**
     * Populate this DataPage with a row for each of the given xpaths
     *
     * @param allXpaths the set of xpaths
     * @param workPrefix
     * @param ourSrc
     * @param extraXpaths
     * @param stf
     * @param checkCldr
     */
    private void populateFromAllXpaths(
            Set<String> allXpaths,
            String workPrefix,
            CLDRFile ourSrc,
            Set<String> extraXpaths,
            STFactory stf,
            TestResultBundle checkCldr) {

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
                } else if (DEBUG) {
                    System.err.println("allPath: " + xpath);
                }
            }

            PathHeader ph = stf.getPathHeader(xpath);
            if (ph == null || ph.shouldHide()) {
                continue;
            }

            String fullPath = ourSrc.getFullXPath(xpath);
            int base_xpath = sm.xpt.xpathToBaseXpathId(xpath);
            String baseXpath = sm.xpt.getById(base_xpath);

            int coverageValue = getCoverageInfo().getCoverageValue(baseXpath, locale.getBaseName());
            if (fullPath == null) {
                fullPath = xpath; // (this is normal for 'extra' paths)
            }
            // Now we are ready to add the data
            populateFromThisXpath(
                    xpath, extraXpaths, ourSrc, fullPath, checkCldr, coverageValue, base_xpath);
        }
    }

    /**
     * Add data to this DataPage including a possibly new DataRow for the given xpath
     *
     * @param xpath
     * @param extraXpaths
     * @param ourSrc
     * @param fullPath
     * @param checkCldr
     * @param coverageValue
     * @param base_xpath
     */
    private void populateFromThisXpath(
            String xpath,
            Set<String> extraXpaths,
            CLDRFile ourSrc,
            String fullPath,
            TestResultBundle checkCldr,
            int coverageValue,
            int base_xpath) {

        /*
         * 'extra' paths get shim treatment
         *
         * NOTE: this is a sufficient but not a necessary condition for isExtraPath; if it gets false here,
         * it may still get true below if ourSrc.getStringValue returns null.
         */
        boolean isExtraPath = extraXpaths != null && extraXpaths.contains(xpath);

        String ourValue = isExtraPath ? null : ourSrc.getStringValue(xpath);
        if (ourValue == null) {
            /*
             * This happens, for example, with xpath = "//ldml/dates/timeZoneNames/metazone[@type=\"Kyrgystan\"]/long/generic"
             * at http://localhost:8080/cldr-apps/v#/fr_CA/CAsia/
             *
             * getStringValue calls getFallbackPath which calls getRawExtraPaths which contains xpath
             *
             * However, we could get ourValue == null due to various bugs when in reality the path is NOT "extra",
             * so this code is fragile and problematic. Possibly rename from isExtraPath to hasNullValue; possibly
             * don't use extraXpaths at all; possibly change the code further below that depends on this
             * boolean value...
             * Reference: https://unicode-org.atlassian.net/browse/CLDR-14945
             */
            if (DEBUG) {
                System.err.println(
                        "warning: populateFromThisXpath "
                                + this
                                + ": "
                                + locale
                                + ":"
                                + xpath
                                + " = NULL! wasExtraPath="
                                + isExtraPath);
            }
            isExtraPath = true;
        }

        // determine 'alt' param
        String alt = sm.xpt.altFromPathToTinyXpath(xpath);

        /* FULL path processing (references.. alt proposed.. ) */
        XPathParts xpp = XPathParts.getFrozenInstance(fullPath);
        String lelement = xpp.getElement(-1);
        xpp.findAttributeValue(lelement, LDMLConstants.ALT);
        String eDraft = xpp.findAttributeValue(lelement, LDMLConstants.DRAFT);

        String[] typeAndProposed = LDMLUtilities.parseAlt(alt);
        String altProp = typeAndProposed[1];

        CLDRFile.Status sourceLocaleStatus = new CLDRFile.Status();
        String sourceLocale = ourSrc.getSourceLocaleID(xpath, sourceLocaleStatus);

        /*
         * Dubious! The value could be inherited from another path in the same locale.
         * "ourValueIsInherited" doesn't appear to mean what the name seems to imply.
         */
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
         * Normally row.inheritedItem is null at this point, unless setShimTests has already been called
         * by ensureComplete, for some timezones. If row.inheritedItem is null, possibly create it.
         *
         * However, skip updateInheritedValue if isExtra. See setShimTests below, which may set inheritedItem
         * when isExtraPath.
         */
        if (row.inheritedItem == null && !isExtraPath) {
            row.updateInheritedValue(ourSrc, checkCldr);
        }
        row.addAnnotationRootValue();

        Set<String> v = ballotBox.getValues(xpath);
        if (v != null) {
            populateFromThisXpathAddItemsForVotes(v, xpath, row, checkCldr);
        }

        /*
         * Add an item for winningValue if there isn't one already.
         */
        if (row.winningValue != null) {
            row.addItem(row.winningValue, "winning");
        }

        /*
         * Add an item for the baseline value (trunk).
         */
        if (row.baselineValue != null) {
            row.addItem(row.baselineValue, "baseline");
        }

        row.coverageValue = coverageValue;

        if (row.inheritedItem == null && isExtraPath) {
            /*
             * This is an 'extra' item- it doesn't exist in xml (including root).
             * For example, isExtraPath may be true when xpath is:
             *  '//ldml/dates/timeZoneNames/metazone[@type="Mexico_Northwest"]/short/standard'
             * and the URL ends with "v#/aa/NAmerica/".
             * Set up 'shim' tests, to display coverage.
             */
            row.setShimTests(this.sm.xpt.getById(base_xpath), checkCldr);
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
        if (altProp != null
                && !ourValueIsInherited
                && !altProp.equals(SurveyMain.PROPOSED_DRAFT)) { // 'draft=true'
            row.hasMultipleProposals = true;
        }
        List<CheckStatus> checkCldrResult = new ArrayList<>();
        List<CheckStatus> examplesResult = new ArrayList<>();
        if (checkCldr != null) {
            checkCldr.check(xpath, checkCldrResult, isExtraPath ? null : ourValue);
            STFactory.removeExcludedChecks(checkCldrResult);
            checkCldr.getExamples(xpath, isExtraPath ? null : ourValue, examplesResult);
        }
        if (ourValue != null && ourValue.length() > 0) {
            addOurValue(ourValue, row, checkCldrResult, sourceLocaleStatus, xpath);
        }
    }

    /**
     * For each string in the given set, based on values that have votes, add an item to the given
     * row with that string as its value, unless the string matches ourValue.
     *
     * <p>Also run some tests if appropriate.
     *
     * @param v the set of values that have votes
     * @param xpath
     * @param row the DataRow
     * @param checkCldr the TestResultBundle, or null
     *     <p>TODO: populateFromThisXpathAddItemsForVotes could be a method of DataRow instead of
     *     DataPage, then wouldn't need row, xpath as params
     */
    private void populateFromThisXpathAddItemsForVotes(
            Set<String> v, String xpath, DataRow row, TestResultBundle checkCldr) {
        for (String avalue : v) {
            Set<User> votes = ballotBox.getVotesForValue(xpath, avalue);
            if (votes == null || votes.size() == 0) {
                continue;
            }
            CandidateItem item2 = row.addItem(avalue, "votes");
            if (avalue != null && checkCldr != null) {
                List<CheckStatus> item2Result = new ArrayList<>();
                checkCldr.check(xpath, item2Result, avalue);
                STFactory.removeExcludedChecks(item2Result);
                if (!item2Result.isEmpty()) {
                    item2.setTests(item2Result);
                }
            }
        }
    }

    /**
     * Add an item for "ourValue" to the given DataRow, and set various fields of the DataRow
     *
     * <p>TODO: rename this function and/or move parts elsewhere? The setting of various fields may
     * be more necessary than adding an item for ourValue. This function lacks a coherent purpose.
     *
     * @param ourValue
     * @param row
     * @param checkCldrResult
     * @param sourceLocaleStatus
     * @param xpath
     */
    private void addOurValue(
            String ourValue,
            DataRow row,
            List<CheckStatus> checkCldrResult,
            CLDRFile.Status sourceLocaleStatus,
            String xpath) {

        /*
         * Do not add ourValue if it matches inheritedValue. Otherwise we tend to get both "hard" and "soft"
         * inheritance items even when there are no votes yet in the current cycle. This is related
         * to the open question of how ourValue is related to winningValue (often but not always the same),
         * and why there is any need at all for ourValue in addition to winningValue.
         *
         * However, if ourValue, matching inheritedValue, has already been added (e.g., as winningValue,
         * baselineValue, or because it has votes), then "add" it again here so that we have myItem and
         * will call setTests.
         *
         * TODO: It would be better to consolidate where setTests is called for all items, to ensure
         * it's called once and only once for each item that needs it.
         */
        CandidateItem myItem = null;
        if (ourValue != null) {
            if (!VoteResolver.DROP_HARD_INHERITANCE
                    || !ourValue.equals(row.inheritedValue)
                    || row.items.get(ourValue) != null) {
                myItem = row.addItem(ourValue, "our");
                if (DEBUG) {
                    System.err.println(
                            "Added item " + ourValue + " - now items=" + row.items.size());
                }
            }
        }

        if (myItem != null && !checkCldrResult.isEmpty()) {
            myItem.setTests(checkCldrResult);
        }

        if (sourceLocaleStatus != null
                && sourceLocaleStatus.pathWhereFound != null
                && !sourceLocaleStatus.pathWhereFound.equals(xpath)) {
            row.pathWhereFound = sourceLocaleStatus.pathWhereFound;
        }
    }

    /** Convert this DataPage to a string. */
    @Override
    public String toString() {
        return "{"
                + getClass().getSimpleName()
                + " "
                + locale
                + ":"
                + xpathPrefix
                + " #"
                + super.toString()
                + ", "
                + getAll().size()
                + " items, pageid "
                + this.pageId
                + " } ";
    }

    public static String getValueHash(String str) {
        return (str == null) ? "null" : CookieSession.cheapEncodeString(str);
    }

    /**
     * Get the DisplayAndInputProcessor for this DataPage; if there isn't one yet, create it
     *
     * @return the processor
     *     <p>Called by getProcessedValue
     */
    private DisplayAndInputProcessor getBaselineProcessor() {
        if (processor == null) {
            processor = new DisplayAndInputProcessor(SurveyMain.TRANS_HINT_LOCALE, false);
        }
        return processor;
    }
}
