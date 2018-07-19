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
import org.unicode.cldr.web.WebContext.HTMLDirection;

import com.ibm.icu.text.Collator;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

/**
 * A data section represents a group of related data that will be displayed to
 * users in a list such as,
 * "all of the language display names contained in the en_US locale". It is
 * sortable, as well, and has some level of persistence. This class was
 * formerly, and unfortunately, named DataPod
 **/

public class DataSection implements JSONString {
    public CLDRFile baselineFile;
    private DisplayAndInputProcessor processor = null;

    /**
     * This class represents a "row" of data - a single distinguishing xpath
     * This class was formerly (and unfortunately) named "Pea"
     *
     * @author srl
     *
     */
    public class DataRow implements JSONString, PathValueInfo {

        // String inheritFrom = null;
        // String pathWhereFound = null;
        /**
         * The Item is a particular alternate which could be chosen It was
         * previously named "Item"
         */
        public class CandidateItem implements Comparable<CandidateItem>, JSONString, CandidateInfo {
            public static final String altProposed = "n/a"; // proposed part of
            // the name (or NULL
            // for nondraft)
            boolean checkedVotes = false;
            public String example = "";
            public Vector<ExampleEntry> examples = null;
            public int id = -1; // id of CLDR_DATA table row
            CLDRLocale inheritFrom = null;
            boolean isFallback = false; // item is from the parent locale -
            // don't consider it a win.
            boolean isParentFallback = false; // true if it is not actually part
            // of this locale,but is just
            // the parent fallback (
            // .inheritedValue );
            boolean itemErrors = false;
            String pathWhereFound = null;
            // public List examplesList = null;
            // String references = null;
            // public int submitter = -1; // if this was submitted via ST,
            // record
            // user id. ( NOT from XML - in other
            // words, we won't be parsing
            // 'proposed-uXX' items. )
            public List<CheckStatus> tests = null;
            final public String value; // actual value

            public Set<UserRegistry.User> votes = null; // Set of Users who voted on this item

            private String originalValueHash = null; // see getOriginalValueHash
            private String adjustedValueHash = null; // see getAdjustedValueHash

            public boolean isOldValue = false;
            //private String dv = null;
            public boolean isBailey = false; // is this the fallback value?

            public String getProcessedValue() {
                if (value == null)
                    return null;
                try {
                    return getProcessor().processForDisplay(xpath, value);
                } catch (Throwable t) {
                    if (SurveyLog.DEBUG) SurveyLog.logException(t, "While processing " + xpath + ":" + value);
                    return value;
                }
            }

            private CandidateItem(String value) {
                this.value = value;
            }

            /**
             * Get the hash of the value of this candidate item.
             *
             * This item's value is assumed to have been set already.
             *
             * Here, "original" means "not adjusted"; compare getAdjustedValueHash.
             *
             * @return the hash of the original value 
             */
            public String getOriginalValueHash() {
                if (originalValueHash == null) {
                    originalValueHash = DataSection.getValueHash(value);
                }
                return originalValueHash;
            }

            /**
             * Get the adjusted hash of the value of this candidate item.
             *
             * Here, "adjusted" means that under certain conditions this function may return the
             * hash of CldrUtility.INHERITANCE_MARKER instead of the hash of this item's value.
             * Compare getOriginalValueHash.
             *
             * The conditions are: isFallback && !locale.isLanguageLocale().
             *
             * TODO: Document the reasons for the adjustment. Does it still serve a purpose, or should
             * all getAdjustedValueHash be changed to getOriginalValueHash? The adjustment lead in one
             * context to the browser console error "there is no Bailey Target item".
             *
             * @return the adjusted hash
             */
            public String getAdjustedValueHash() {
                if (adjustedValueHash == null) {
                    if (isFallback && !locale.isLanguageLocale()) {
                        adjustedValueHash = DataSection.getValueHash(CldrUtility.INHERITANCE_MARKER);
                    } else {
                        adjustedValueHash = DataSection.getValueHash(value);
                    }
                }
                return adjustedValueHash;
            }

            @Override
            public int compareTo(CandidateItem other) {
                if (other == this) {
                    return 0;
                }
                CandidateItem i = (CandidateItem) other;
                int rv = value.compareTo(i.value);
                return rv;
            }

            @Override
            public Collection<UserInfo> getUsersVotingOn() {
                Set<UserRegistry.User> uvotes = getVotes();
                if (uvotes == null)
                    return Collections.emptySet();
                TreeSet<UserInfo> ts = new TreeSet<UserInfo>(uvotes); // TODO:
                // change
                // return
                // type
                // for
                // perf?
                return ts;
            }

            public Set<UserRegistry.User> getVotes() {
                if (!checkedVotes) {
                    votes = ballotBox.getVotesForValue(xpath, value);
                    checkedVotes = true;
                }
                return votes;
            }

            /**
             * Is this a winning (non fallback) item?
             */
            public boolean isWinner() {
                if (winningValue != null) {
                    return winningValue.equals(value);
                } else {
                    return false;
                }
            }

            /**
             * print the cells which have to do with the item this may be called
             * with NULL if there isn't a proposed item for this slot. it will
             * be called once in the 'main' row, and once for any extra rows
             * needed for proposed items
             *
             * @param ctx
             *            TODO
             * @param ourVote
             *            TODO
             * @param canModify
             *            TODO
             * @param ourAlign
             *            TODO
             * @param uf
             *            TODO
             * @param zoomedIn
             *            TODO
             * @param numberedItemsList
             *            All items are added here.
             * @param exampleContext
             *            TODO
             */
            void printCell(WebContext ctx, String ourVote, boolean canModify, String ourAlign, UserLocaleStuff uf,
                boolean zoomedIn, List<CandidateItem> numberedItemsList, ExampleContext exampleContext) {
                printCell(ctx, ourVote, canModify, ourAlign, uf, zoomedIn, numberedItemsList, exampleContext,
                    EnumSet.allOf(EPrintCellSet.class));
            }

            /**
             * print the cells which have to do with the item this may be called
             * with NULL if there isn't a proposed item for this slot. it will
             * be called once in the 'main' row, and once for any extra rows
             * needed for proposed items
             *
             * @param ctx
             *            TODO
             * @param ourVote
             *            TODO
             * @param canModify
             *            TODO
             * @param ourAlign
             *            TODO
             * @param uf
             *            TODO
             * @param zoomedIn
             *            TODO
             * @param numberedItemsList
             *            All items are added here.
             * @param exampleContext
             *            TODO
             * @param options
             *            TODO
             */
            void printCell(WebContext ctx, String ourVote, boolean canModify, String ourAlign, UserLocaleStuff uf,
                boolean zoomedIn, List<CandidateItem> numberedItemsList, ExampleContext exampleContext,
                EnumSet<EPrintCellSet> options) {
                String fieldHash = fieldHash();
                // ##6.1 proposed - print the TOP item
                int colspan = 1;
                String itemExample = null;

                if (options.contains(EPrintCellSet.doShowValue)) {
                    ctx.print("<td  colspan='" + colspan + "' class='propcolumn' align='" + ourAlign + "' dir='"
                        + ctx.getDirectionForLocale() + "' valign='top'>");
                    if (value != null) {
                        String pClass = getPClass(ctx);

                        if (true /* !item.itemErrors */) { // exclude item from
                            // voting due to errors?
                            if (ctx.getCanModify()) {
                                /* TODO: should this be getAdjustedValueHash or getOriginalValueHash? */
                                ctx.print("<button class='ichoice' title='#" + "x" + "' name='" + fieldHash + "'  value='"
                                    + getAdjustedValueHash() + "' " + " onclick=\"do_change('" + fullFieldHash() + "','','"
                                    + getAdjustedValueHash() + "'," + getXpathId() + ",'" + getLocale() + "', '" + ctx.session
                                    + "')\"" + "  type='button'>"
                                    + ctx.iconHtml(checkThis(ourVote) ? "radx" : "rado", "Vote") + "</button>");
                            } // else, can't vote- no radio buttons.
                        }

                        if (zoomedIn && (getVotes() != null)) {
                            int n = getVotes().size();
                            String title = "" + n + " vote" + ((n > 1) ? "s" : "");
                            if (canModify && UserRegistry.userIsVetter(ctx.session.user)) {
                                title = title + ": ";
                                boolean first = true;
                                for (User theU : getVotes()) {
                                    if (theU != null) {
                                        String add = theU.name + " of " + theU.org;
                                        title = title + add.replaceAll("'", "\u2032"); // quote
                                        // quotes
                                        if (first) {
                                            title = title + ", ";
                                        } else {
                                            first = false;
                                        }
                                    }
                                }
                            }
                            ctx.print("<span class='notselected' >" + ctx.iconHtml("vote", title) + "</span>"); // ballot
                            // box
                            // symbol
                        }

                        ctx.print("<span " + pClass + ">");
                        String processed = null;
                        if (value.length() != 0) {
                            processed = ctx.processor.processForDisplay(sm.xpt.getById(getXpathId()), value);
                            ctx.print(processed);
                        } else {
                            ctx.print("<i dir='ltr'>(empty)</i>");
                            processed = null;
                        }
                        ctx.print("</span>");
                        if (this == previousItem) { // its xpath is the base
                            // xpath.
                            ctx.print(ctx.iconHtml("star", "CLDR " + SurveyMain.getOldVersion() + " item"));
                        } else if (SurveyMain.isUnofficial() && isParentFallback) {
                            // ctx.print(ctx.iconHtml("okay","parent fallback"));
                        }

                        if ((ctx.session.user != null) && (zoomedIn || ctx.prefBool(SurveyMain.PREF_DELETEZOOMOUT))) {
                            // boolean adminOrRelevantTc =
                            // UserRegistry.userIsAdmin(ctx.session.user); //
                            // ctx.session.user.isAdminFor(reg.getInfo(item.submitter));
                            // if ((getVotes() == null) || adminOrRelevantTc ||
                            // // nobody
                            // ((getVotes().size() == 1) &&
                            // getVotes().contains(ctx.session.user))) { // ..
                            // boolean deleteHidden =
                            // ctx.prefBool(SurveyMain.PREF_NOSHOWDELETE);
                            // if (!deleteHidden && canModify && (submitter !=
                            // -1)
                            // && !((pathWhereFound != null) || isFallback ||
                            // (inheritFrom != null)) &&
                            // (adminOrRelevantTc || (submitter ==
                            // ctx.session.user.id))) {
                            // ctx.println(" <label nowrap class='deletebox' style='padding: 4px;'>"
                            // + "<input type='checkbox' title='#" + xpathId +
                            // "' value='" + altProposed + "' name='"
                            // + fieldHash + SurveyMain.ACTION_DEL + "'>" +
                            // "Delete&nbsp;item</label>");
                            // }
                            // }
                        }
                        if (UserRegistry.userIsTC(ctx.session.user) && ctx.prefBool(SurveyMain.PREF_SHOWUNVOTE)
                            && votesByMyOrg(ctx.session.user)) {
                            ctx.println(" <label nowrap class='unvotebox' style='padding: 4px;'>"
                                + "<input type='checkbox' title='#" + xpathId + "' value='" + altProposed + "' name='"
                                + fieldHash + SurveyMain.ACTION_UNVOTE + "'>" + "Unvote&nbsp;item</label>");
                        }
                        if (zoomedIn) {
                            if (processed != null && /*
                                                     * direction.equals("rtl")&&
                                                     */SurveyMain.CallOut.containsSome(processed)) {
                                String altProcessed = processed.replaceAll("\u200F", "\u200F<b dir=rtl>RLM</b>\u200F")
                                    .replaceAll("\u200E", "\u200E<b>LRM</b>\u200E");
                                ctx.print("<br><span class=marks>" + altProcessed + "</span>");
                            }
                        }
                    }
                    ctx.println("</td>");
                }
                // 6.3 - If we are zoomed in, we WILL have an additional column
                // withtests and/or references.
                if (options.contains(EPrintCellSet.doShowRef) && zoomedIn) {
                    ctx.println("<td nowrap class='warncell'>");
                    ctx.println("<span class='warningReference'>");
                    // ctx.print(ctx.iconHtml("warn","Warning"));
                    if (this != null /* always number -- was haveTests */) {
                        numberedItemsList.add(this);
                        int mySuperscriptNumber = numberedItemsList.size(); // which
                        // #
                        // is
                        // this
                        // item?
                        ctx.println("#" + mySuperscriptNumber + "</span>");
                    }
                    ctx.println("</td>");
                }

                // ##6.2 example column. always present
                if (options.contains(EPrintCellSet.doShowExample) && hasExamples) {
                    itemExample = uf.getExampleGenerator().getExampleHtml(xpath, value,
                        exampleContext, ExampleType.NATIVE);
                    if (itemExample != null) {
                        ctx.print("<td class='generatedexample' valign='top' align='" + ourAlign + "' dir='"
                            + ctx.getDirectionForLocale() + "' >");
                        ctx.print(itemExample.replaceAll("\\\\", "\u200b\\\\")); // \u200bu
                        ctx.println("</td>");
                    } else {
                        ctx.println("<td></td>");
                    }
                }
            }

            /**
             * @param ourVote
             * @return
             */
            public boolean checkThis(String ourVote) {
                boolean checkThis = ((ourVote != null) && (xpath != null) && (ourVote.equals(value)));
                return checkThis;
            }

            /**
             * @param ctx
             * @return
             */
            public String getPClass(WebContext ctx) {
                String pClass;
                if (isWinner() && !isFallback && inheritFrom == null) {
                    if (confirmStatus == Status.approved) {
                        pClass = "class='winner' title='Winning item.'";
                    } else if (confirmStatus == Status.missing) {
                        pClass = "title='" + confirmStatus + "' ";
                    } else {
                        pClass = "class='winner' title='" + confirmStatus + "' ";
                    }
                } else if (pathWhereFound != null) {
                    pClass = "class='alias' title='alias from " + sm.xpt.getPrettyPath(pathWhereFound) + "'";
                } else if (isFallback || (inheritFrom != null)) {
                    if (isOldValue) {
                        pClass = "class='fallback' title='Previous Version'";
                    } else if (inheritFrom != null && XMLSource.CODE_FALLBACK_ID.equals(inheritFrom.getBaseName())) {
                        pClass = "class='fallback_code' title='Untranslated Code'";
                    } else if (inheritFrom == CLDRLocale.ROOT) {
                        pClass = "class='fallback_root' title='Fallback from Root'";
                    } else {
                        pClass = "class='fallback' title='Translated in "
                            + ((inheritFrom == null) ? "(unknown)" : CLDRLocale.getDefaultFormatter().getDisplayName(
                                inheritFrom))
                            + " and inherited here.'";
                    }
                } else if (altProposed != null) {
                    pClass = "class='loser' title='proposed, losing item'";
                    /*
                     * } else if(p.inheritFrom != null) { pClass =
                     * "class='missing'";
                     */
                } else {
                    pClass = "class='loser'";
                }
                return pClass;
            }

            public String getPClass() {
                String pClass;
                if (isWinner() && !isFallback && inheritFrom == null) {
                    if (confirmStatus == Status.approved) {
                        pClass = "winner"; // ' title='Winning item.'";
                    } else if (confirmStatus == Status.missing) {
                        pClass = "winner"; // was missing- doesn't matter
                    } else {
                        pClass = "winner"; // class='winner'
                        // title='"+confirmStatus+"' ";
                    }
                } else if (pathWhereFound != null) {
                    pClass = "alias"; // class='alias' title='alias from
                    // " + sm.xpt.getPrettyPath(pathWhereFound) + "'";
                } else if (isFallback || (inheritFrom != null)) {
                    if (inheritFrom != null && XMLSource.CODE_FALLBACK_ID.equals(inheritFrom.getBaseName())) {
                        pClass = "fallback_code"; // class='fallback_code'
                        // title='Untranslated Code'";
                    } else if (inheritFrom == CLDRLocale.ROOT) {
                        pClass = "fallback_root"; // class='fallback_root'
                        // title='Fallback from
                        // Root'";
                    } else {
                        pClass = "fallback"; // class='fallback'
                        // title='Translated in "
                        // + ((inheritFrom==null)?"(unknown)":
                        // CLDRLocale.getDefaultFormatter().getDisplayName(inheritFrom))
                        // + " and inherited here.'";
                    }
                } else if (altProposed != null) {
                    pClass = "loser"; // class='loser' title='proposed, losing
                    // item'";
                    /*
                     * } else if(p.inheritFrom != null) { pClass =
                     * "class='missing'";
                     */
                } else {
                    pClass = "loser"; // class='loser'";
                }
                return pClass;
            }

            /* return true if any valid tests were found */
            public boolean setTests(List<CheckStatus> testList) {
                tests = testList;
                // only consider non-example tests as notable.
                boolean weHaveTests = false;
                int errorCount = 0;
                int warningCount = 0;
                for (Iterator<CheckStatus> it3 = tests.iterator(); it3.hasNext();) {
                    CheckCLDR.CheckStatus status = it3.next();
                    if (status.getType().equals(CheckStatus.exampleType)) {
                        // throw new
                        // InternalError("Not supposed to be any examples here.");
                        /*
                         * if(myItem.examples == null) { myItem.examples = new
                         * Vector(); } myItem.examples.add(addExampleEntry(new
                         * ExampleEntry(this,p,myItem,status)));
                         */
                    } else /*
                           * if (!(isCodeFallback && (status.getCause()
                           * instanceof
                           * org.unicode.cldr.test.CheckForExemplars)))
                           */ {
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
                    /* row */hasTests = true;
                    parentRow.hasTests = true;

                    if (errorCount > 0) /* row */
                        hasErrors = true;
                    if (warningCount > 0) /* row */
                        hasWarnings = true;
                    // propagate to parent
                    if (errorCount > 0) /* row */
                        parentRow.hasErrors = true;
                    if (warningCount > 0) /* row */
                        parentRow.hasWarnings = true;

                    if (errorCount > 0) /* row */ {
                        itemErrors = true;
                        anyItemHasErrors = true;
                        parentRow.anyItemHasErrors = true;
                    }
                    if (warningCount > 0) /* row */
                        anyItemHasWarnings = true;
                    // propagate to parent
                    if (warningCount > 0) /* row */
                        parentRow.anyItemHasWarnings = true;
                }
                return weHaveTests;
            }

            @Override
            public String toString() {
                return "{Item v='" + value + "', altProposed='" + altProposed + "', inheritFrom='" + inheritFrom + "'"
                    + (isWinner() ? ",winner" : "") + (isFallback ? ",isFallback" : "")
                    + (isParentFallback ? ",isParentFallback" : "") + "}";
            }

            /**
             * Did any voters in my org vote for it?
             *
             * @param me
             */
            public boolean votesByMyOrg(User me) {
                if (me == null || getVotes() == null)
                    return false;
                for (UserRegistry.User u : getVotes()) {
                    if (u.org.equals(me.org) && u.id != me.id) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public String toJSONString() throws JSONException {
                // Commented out items are ones not currently used, but ones that were in the old ST so
                // we may wish to use them in the future.  We don't pass them along in order to save resources.
                // JCE: 2013-05-29
                JSONObject j = new JSONObject()
                    /* TODO: should this be getAdjustedValueHash or getOriginalValueHash? */
                    .put("valueHash", getAdjustedValueHash())
                    .put("rawValue", value)
                    .put("value", getProcessedValue())
                    .put("example", getExample())
                    .put("isOldValue", isOldValue)
                    .put("isBailey", isBailey)
                    //                        .put("inheritFrom", inheritFrom)
                    //                        .put("inheritFromDisplay", ((inheritFrom != null) ? inheritFrom.getDisplayName() : null))
                    .put("isFallback", isFallback)
                    .put("pClass", getPClass())
                    .put("tests", SurveyAjax.JSONWriter.wrap(this.tests));
                Set<User> theVotes = getVotes();
                if (theVotes != null && !theVotes.isEmpty()) {
                    JSONObject voteList = new JSONObject();
                    for (UserRegistry.User u : theVotes) {
                        if (u.getLevel() == VoteResolver.Level.locked)
                            continue; // dont care
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

            private String getExample() {
                if (examplebuilder == null) {
                    return null;
                } else {
                    return getExampleBuilder().getExampleHtml(xpath, value, ExampleType.NATIVE);
                }
            }

            @Override
            public String getValue() {
                return value;
            }

            @Override
            public List<CheckStatus> getCheckStatusList() {
                if (tests == null) {
                    return Collections.emptyList();
                } else {
                    return tests;
                }
            }
        }

        // Do some items alias to a different base xpath or locale?
        String aliasFromLocale = null; // locale, if different
        int aliasFromXpath = -1; // xpath, if different
        public int allVoteType = 0; // bitmask of all voting types included

        public String altType = null; // alt type (NOT to be confused with
        // -proposedn)
        boolean anyItemHasErrors = false;

        // do any items have warnings or errs?
        boolean anyItemHasWarnings = false;
        // what kind of row is this?
        public boolean confirmOnly = false; // if true: don't accept new data,
        // this row is something that might
        // be confusing to input.
        Status confirmStatus;

        /**
         * Calculated coverage level for this row.
         */
        public int coverageValue;
        List<CandidateItem> candidateItems = null;

        private String displayName = null;
        // these apply to the 'winning' item, if applicable
        boolean hasErrors = false;
        boolean hasInherited = false; // True if has inherited value
        boolean hasMultipleProposals = false; // true if more than 1 proposal is
        // available

        boolean hasProps = false; // true if has some proposed items

        // true even if only the non-winning subitems have tests.
        boolean hasTests = false;

        boolean hasWarnings = false;
        public CandidateItem inheritedValue = null; // vetted value inherited
        // from
        // parent

        private CandidateItem winningItem = null;
        public Map<String, CandidateItem> items = new TreeMap<String, CandidateItem>();

        String myFieldHash = null;
        /** Cache of field hash **/
        public DataRow parentRow = this; // parent - defaults to self if it is a
        // "super" (i.e. parent without any
        // alternate)

        private String pp = null;
        public String prettyPath = null;

        CandidateItem previousItem = null;
        public int reservedForSort[] = SortMode.reserveForSort(); // ordering
        // for use
        // in
        // collator.
        public String uri = null; // URI for the type

        String[] valuesList = null; // if non null - list of acceptable values.

        public int voteType = 0; // status of THIS item
        private String winningValue;

        // the xpath id of the winner. If no winner or n/a, -1.
        /**
         * @deprecated - winner is a value
         */
        @Deprecated
        int winningXpathId = -1;

        private String xpath;

        int xpathId = -1;
        public boolean zoomOnly = false; // if true - don't show any editing in
        public String oldValue;
        private PathHeader pathHeader;

        // the zoomout view, they must zoom
        // in.

        public DataRow(String xpath) {
            this.xpath = xpath;
            this.xpathId = sm.xpt.getByXpath(xpath);
            this.prettyPath = sm.xpt.getPrettyPath(xpathId);
            pathHeader = sm.getSTFactory().getPathHeader(xpath); // may be null
            // this.setDisplayName(prettyPath);

            if (ballotBox == null) {
                throw new InternalError("ballotBox is null;");
            }
            VoteResolver<String> resolver = ballotBox.getResolver(xpath);
            winningValue = resolver.getWinningValue();
            confirmStatus = resolver.getWinningStatus();
            this.displayName = baselineFile.getStringValue(xpath);
        }

        public CandidateItem addItem(String value) {
            final String kValue = (value == null) ? "" : value;
            CandidateItem pi = items.get(kValue);
            //            if (DEBUG)
            //                System.err.println("Adding VItem value=" + kValue + " ret=" + pi + ", of " + items.size());
            if (pi != null)
                return pi;
            pi = new CandidateItem(value);
            items.put(kValue, pi);
            if (pi.isWinner()) {
                winningItem = pi;
            }
            if (oldValue != null && oldValue.equals(value)) {
                previousItem = pi;
                pi.isOldValue = true;
            }
            return pi;
        }

        /**
         * Calculate the hash used for HTML forms for this row
         */
        public String fieldHash() { // deterministic. No need for sync.
            if (myFieldHash == null) {
                String ret = "";
                ret = ret + "_x" + CookieSession.cheapEncode(getXpathId());
                myFieldHash = ret;
            }
            return myFieldHash;
        }

        // Hashtable<String,DataRow> subRows = null;

        // public DataRow getSubDataRow(String altType) {
        // if(altType == null) {
        // return this;
        // }
        // if(subRows == null) {
        // subRows = new Hashtable<String,DataRow>();
        // }
        //
        // DataRow p = subRows.get(altType);
        // if(p==null) {
        // p = new DataRow();
        // p.type = type;
        // p.altType = altType;
        // p.parentRow = this;
        // subRows.put(altType, p);
        // }
        // return p;
        // }

        /**
         * @deprecated now the same as fieldHash.
         * @return
         */
        public String fullFieldHash() {
            return fieldHash();
        }

        /**
         * Returns the winning (current) item.
         *
         * @return
         */
        public CandidateItem getCurrentItem() {
            return winningItem;
            // if (this.currentItems == null) {
            // if(false) System.err.println("Getting current items for " + xpath
            // + " from " + items.size() + " items.: " + this);
            // List<DataSection.DataRow.CandidateItem> currentItems = new
            // ArrayList<DataSection.DataRow.CandidateItem>();
            // List<DataSection.DataRow.CandidateItem> proposedItems = new
            // ArrayList<DataSection.DataRow.CandidateItem>();
            // for (CandidateItem item : items) {
            //
            // if(false || (sm.isUnofficial && DEBUG))
            // System.err.println("Considering: " +
            // item+", xpid="+item.xpathId+", result="+""+", base="+this.xpathId+", ENO="+errorNoOutcome);
            // item.toString();
            //
            // if (item.value.equals(winningValue) && !errorNoOutcome) {
            // if (!currentItems.isEmpty()) {
            // throw new InternalError(this.toString() +
            // ": can only have one candidate item, not "
            // + currentItems.get(0) + " and " + item);
            // }
            // currentItems.add(item);
            // } else {
            // proposedItems.add(item);
            // }
            // }
            // // if there is an inherited value available - see if we need to
            // // show it.
            // if ((inheritedValue != null) && (inheritedValue.value != null)) {
            // // or
            // // an
            // // alias
            // if (currentItems.isEmpty()) { // no other current items..
            // currentItems.add(inheritedValue);
            // } else {
            // boolean found = false; /*
            // * Have we found this value in
            // * the inherited items? If so,
            // * don't show it.
            // */
            // for (DataSection.DataRow.CandidateItem i : proposedItems) {
            // if (inheritedValue.value.equals(i.value)) {
            // found = true;
            // }
            // }
            // if (!found)
            // for (DataSection.DataRow.CandidateItem i : currentItems) {
            // if (inheritedValue.value.equals(i.value)) {
            // found = true;
            // }
            // }
            // if (!found) {
            // proposedItems.add(inheritedValue);
            // }
            // }
            // }
            // this.currentItems = currentItems;
            // this.proposedItems = proposedItems;
            // }
            // if (!this.currentItems.isEmpty())
            // return this.currentItems.get(0);
            // else
            // return null;
        }

        public String getDisplayName() {
            return displayName;
        }

        /**
         * @deprecated
         */
        @Deprecated
        public boolean getErrorNoOutcome() {
            return winningValue == null;
        }

        public String getIntgroup() {
            return intgroup;
        }

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
         * Get a list of proposed items, if any.
         *
         * @deprecated use getValues() instead
         * @see #getValues()
         */
        @SuppressWarnings("unchecked")
        public Collection<CandidateItem> getItems() {
            return ((Collection<CandidateItem>) getValues());
        }

        public CandidateItem getItem(String value) {
            if (value.equals(CldrUtility.INHERITANCE_MARKER)) {
                for (CandidateItem item : items.values()) {
                    if (item.isFallback) {
                        return item;
                    }
                }
                return null;
            }
            return items.get(value);
        }

        @Override
        public Collection<? extends CandidateInfo> getValues() {
            return items.values();
            // return new ArrayList<CandidateInfo>(getItems());
        }

        public String getSpecialURL(WebContext ctx) {
            if (getXpath().startsWith(DataSection.EXEMPLAR_PARENT)) {
                String zone = prettyPath;
                int n = zone.lastIndexOf('/');
                if (n != -1) {
                    zone = zone.substring(0, n); // blahblah/default/foo ->
                    // blahblah/default ('foo'
                    // is lastType and will show
                    // up as the value)
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
            UserRegistry.User infoForUser = sm.reg.getInfo(userId); /*
                                                                    * see
                                                                    * gatherVotes
                                                                    * -
                                                                    * getVotes
                                                                    * () is
                                                                    * populated
                                                                    * with a
                                                                    * set drawn
                                                                    * from the
                                                                    * getInfo()
                                                                    * singletons
                                                                    * .
                                                                    */
            if (infoForUser == null)
                return null;
            for (CandidateItem item : getItems()) {
                Set<User> votes = item.getVotes();
                if (votes != null && votes.contains(infoForUser)) {
                    return item;
                }
            }
            return null; /* not found. */
        }

        /**
         * @see #getCurrentItem()
         * @return
         */
        CandidateItem getWinningItem() {
            if (winningValue == null)
                return null;
            if (winningValue.equals(CldrUtility.INHERITANCE_MARKER)) {
                for (CandidateItem ci : items.values()) {
                    if (ci.isFallback) {
                        return ci;
                    }
                }
            }
            CandidateItem ci = items.get(winningValue);
            //            if (DEBUG)
            //                System.err.println("WV = '" + winningValue + "' and return is " + ci);
            return ci;
        }

        public String getWinningValue() {
            return winningValue;
        }

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
         * Print empty cells. Not the equivalent of printCells(..., null), which
         * will still print an example.
         *
         * @param ctx
         * @param ourAlign
         *            TODO
         * @param zoomedIn
         *            TODO
         * @param section
         * @deprecated HTML
         */
        @Deprecated
        void printEmptyCells(WebContext ctx, String ourAlign, boolean zoomedIn) {
            int colspan = zoomedIn ? 1 : 1;
            ctx.print("<td  colspan='" + colspan + "' class='propcolumn' align='" + ourAlign + "' dir='"
                + ctx.getDirectionForLocale() + "' valign='top'>");
            ctx.println("</td>");
            if (zoomedIn) {
                ctx.println("<td></td>"); // no tests, no references
            }
            if (hasExamples()) {
                // ##6.2 example column
                ctx.println("<td></td>");
            }
        }

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
         * A Shim is a candidate item which does not correspond to actual XML
         * data, but is synthesized.
         *
         * @param base_xpath
         * @param base_xpath_string
         * @param checkCldr
         * @param options
         */
        void setShimTests(int base_xpath, String base_xpath_string, TestResultBundle checkCldr, Map<String, String> options) {
            CandidateItem shimItem = inheritedValue;

            if (shimItem == null) {
                shimItem = new CandidateItem(null);
                shimItem.isFallback = false;

                List<CheckStatus> iTests = new ArrayList<CheckStatus>();
                checkCldr.check(base_xpath_string, iTests, null);
                if (!iTests.isEmpty()) {
                    // Got a bite.
                    if (shimItem.setTests(iTests)) {
                        // had valid tests
                        inheritedValue = shimItem;
                        inheritedValue.isParentFallback = true;
                    }
                }
            } else {
                // if(SurveyMain.isUnofficial)
                // System.err.println("already have inherited @ " +
                // base_xpath_string);
            }
        }

        public boolean hasExamples() {
            return hasExamples;
        }

        /**
         * Show a single row
         *
         * @param ctx
         *            TODO
         * @param uf
         *            TODO
         * @param canModify
         *            TODO
         * @param checkCldr
         *            TODO
         * @param zoomedIn
         *            TODO
         * @param options
         *            TODO
         * @param cf
         *            TODO
         * @param ourSrc
         *            TODO
         * @param section
         *            TODO
         * @see #processDataRowChanges(WebContext, SurveyMain, CLDRFile,
         *      BallotBox, DataSubmissionResultHandler)
         * @deprecated HTML (old)
         */
        public void showDataRow(WebContext ctx, UserLocaleStuff uf, boolean canModify, CheckCLDR checkCldr, boolean zoomedIn,
            EnumSet<EShowDataRowSet> options) {
            String ourAlign = ctx.getAlign();
            boolean canSubmit = UserRegistry.userCanSubmitAnyLocale(ctx.session.user) || (canModify);
            boolean isAlias = (xpath.indexOf("/alias") != -1);
            String fieldHash = fieldHash();
            ExampleContext exampleContext = new ExampleContext();

            if (true) {
                STFactory.unimp();
                return;
            } // -------------------------------------------------

            CandidateItem topCurrent = getWinningItem();
            String baseExample = getBaseExample(uf, zoomedIn, exampleContext, topCurrent);

            if (options.contains(EShowDataRowSet.doShowTR)) {
                ctx.println("<tr id='r_" + fullFieldHash() + "' class='topbar'>");
            }
            /* TOP BAR */
            // Mark the line as disputed or insufficient, depending.

            ctx.put(WebContext.DATA_ROW, this);
            ctx.put("DataRow.xpath", xpath);
            ctx.put("DataRow.ballotBox", getBallotBox());
            ctx.put(WebContext.CAN_MODIFY, canSubmit);
            ctx.put(WebContext.ZOOMED_IN, zoomedIn);
            ctx.put("DataRow.baseExample", baseExample);
            ctx.includeFragment("row.jsp");

            // TODO below should all move into the JSP..
            String ourVote = null;
            if (ctx.session.user != null) {
                ourVote = ballotBox.getVoteValue(ctx.session.user, xpath);
            }
            int rowSpan = 1;
            List<CandidateItem> numberedItemsList = new ArrayList<CandidateItem>();

            if (topCurrent != null) {
                ctx.print("<!-- topCurrent = " + topCurrent + " -->");
                topCurrent.printCell(ctx, ourVote, canModify, ourAlign, uf, zoomedIn, numberedItemsList, exampleContext,
                    EnumSet.allOf(EPrintCellSet.class));
            } else {
                ctx.print("<!-- topCurrent==null -->");
                printEmptyCells(ctx, ourAlign, zoomedIn);
            }
            /*
             * if(getItems().size() == 2) { getItems().get(1).printCell(ctx,
             * ourVote, canModify, ourAlign, uf, zoomedIn, numberedItemsList,
             * exampleContext, EnumSet.allOf(EPrintCellSet.class)); }else
             */if (getItems().size() > 1) {

                ctx.print("<td colspan='" + (zoomedIn ? 2 : 1) + "'><table class='innerPropItems'>");
                for (CandidateItem item : getItems()) {
                    if (item == winningItem) {
                        continue;
                    }
                    ctx.print("<tr>");
                    item.printCell(ctx, ourVote, canModify, ourAlign, uf, zoomedIn, numberedItemsList, exampleContext,
                        kValueCells);
                    ctx.print("</tr>");
                }
                ctx.print("</table></td>");
                if (hasExamples) {
                    ctx.print("<td colspan='" + (zoomedIn ? 2 : 1) + "'><table class='innerPropItems'>");
                    for (CandidateItem item : getItems()) {
                        if (item == winningItem) {
                            continue;
                        }
                        ctx.print("<tr>");
                        item.printCell(ctx, ourVote, canModify, ourAlign, uf, zoomedIn, numberedItemsList, exampleContext,
                            kExampleCells);
                        ctx.print("</tr>");
                    }
                    ctx.print("</table></td>");
                }
            } else {
                printEmptyCells(ctx, ourAlign, zoomedIn);
            }

            boolean areShowingInputBox = (canSubmit && !isAlias && canModify && !confirmOnly && (zoomedIn || !zoomOnly));
            boolean areShowingInputColumn = canModify
                && (SurveyMain.isPhaseSubmit() == true)
                || UserRegistry.userIsTC(ctx.session.user)
                || (UserRegistry.userIsVetter(ctx.session.user) && ctx.session.user.userIsSpecialForCLDR15(locale))
                || ((SurveyMain.isPhaseVetting() || SurveyMain.isPhaseVettingClosed()) && (hasErrors || hasProps
                    || hasWarnings || (false)));

            // submit box
            if (areShowingInputColumn) {
                String changetoBox = "<td id='i_" + fullFieldHash() + "' width='1%' class='noborder' rowspan='" + rowSpan
                    + "' valign='top'>";
                // ##7 Change
                if (canModify && canSubmit && (zoomedIn || !zoomOnly)) {
                    changetoBox = changetoBox + "<button id='submit_" + fullFieldHash() + "' class='isubmit' onclick=\"isubmit('"
                        + fullFieldHash() + "'," + getXpathId() + ",'" + getLocale() + "', '" + ctx.session
                        + "')\" type='button'   >" + ctx.iconHtml("rado", "submit this value") + "</button>";
                }

                changetoBox = changetoBox + ("</td>");

                if (!(ctx.getDirectionForLocale() == HTMLDirection.RIGHT_TO_LEFT)) {
                    ctx.println(changetoBox);
                }

                ctx.print("<td  id='chtd_" + fullFieldHash() + "' class='noborder' rowspan='" + rowSpan + "' valign='top'>");

                boolean badInputBox = false;

                if (areShowingInputBox) {
                    String oldValue = (String) ctx.temporaryStuff.get(fieldHash + SurveyMain.QUERY_VALUE_SUFFIX);
                    String fClass = "inputbox";
                    if (oldValue == null) {
                        oldValue = "";
                    } else {
                        ctx.print(ctx.iconHtml("stop", "this item was not accepted.") + "this item was not accepted.<br>");
                        // fClass = "badinputbox";
                        badInputBox = true;
                    }
                    if (valuesList != null) {
                        // ctx.print("<select onclick=\"document.getElementById('"
                        // + fieldHash + "_ch').click()\" name='"
                        // + fieldHash + "_v'>");
                        // ctx.print("  <option value=''></option> ");
                        // for (String s : valuesList) {
                        // ctx.print("  <option value='" + s + "'>" + s +
                        // "</option> ");
                        // }
                        // ctx.println("</select>");
                    } else {
                        // regular change box (text)
                        ctx.print("<input  id='ch_" + fullFieldHash() + "' dir='" + ctx.getDirectionForLocale()
                            + "' onfocus=\"icancel('" + fullFieldHash() + "')\" name='" + fieldHash + "_v' "
                            + " onblur=\"do_change('" + fullFieldHash() + "',this.value,''," + getXpathId() + ",'"
                            + getLocale() + "', '" + ctx.session + "','verify')\"" + " value='" + oldValue + "' class='"
                            + fClass + "'>");
                        ctx.print("<button onclick=\"icancel('" + fullFieldHash() + "'," + getXpathId() + ",'" + getLocale()
                            + "', '" + ctx.session + "')\" type='button' class='icancel'  id='cancel_" + fullFieldHash()
                            + "' >Cancel</button> ");
                    }
                    // references
                    if (badInputBox) {
                        // ctx.print("</span>");
                    }

                    // if (false && canModify && zoomedIn && (altType == null)
                    // && UserRegistry.userIsTC(ctx.session.user)) { // show
                    // // 'Alt'
                    // // popup
                    // // for
                    // // zoomed
                    // // in
                    // // main
                    // // items
                    // ctx.println("<br> ");
                    // ctx.println("<span id='h_alt" + fieldHash + "'>");
                    // ctx.println("<input onclick='javascript:show(\"alt" +
                    // fieldHash
                    // + "\")' type='button' value='Create Alternate'></span>");
                    // ctx.println("<!-- <noscript> </noscript> -->" +
                    // "<span style='display: none' id='alt" + fieldHash +
                    // "'>");
                    // String[] altList =
                    // sm.supplemental.getValidityChoice("$altList");
                    // ctx.println("<label>Alt: <select name='" + fieldHash +
                    // "_alt'>");
                    // ctx.println("  <option value=''></option>");
                    // for (String s : altList) {
                    // ctx.println("  <option value='" + s + "'>" + s +
                    // "</option>");
                    // }
                    // ctx.println("</select></label></span>");
                    // }

                    ctx.println("</td>");
                } else {
                    if (!zoomedIn && zoomOnly) {
                        ctx.println("<i>Must zoom in to edit</i>");
                    }
                    ctx.println("</td>");
                }

                if (ctx.getDirectionForLocale() == HTMLDirection.RIGHT_TO_LEFT) {
                    ctx.println(changetoBox);
                }

            } else {
                areShowingInputBox = false;
                if (canModify) {
                    ctx.println("<td rowspan='" + rowSpan + "' colspan='2'></td>"); // no
                    // 'changeto'
                    // cells.
                }
            }

            // No/Opinion.
            if (canModify) {
                ctx.print("<td width='20' rowspan='" + rowSpan + "'>");
                ctx.print("<button class='ichoice' name='" + fieldHash + "' value='" + SurveyMain.DONTCARE + "' type='button' "
                    + " onclick=\"do_change('" + fullFieldHash() + "','','null'," + getXpathId() + ",'" + getLocale()
                    + "', '" + ctx.session + "')\"" + ">" + ctx.iconHtml((ourVote == null) ? "radx" : "rado", "No Opinion")
                    + "</button>");
                ctx.print("</td>");
            }

            if (ctx.prefBool(SurveyMain.PREF_SHCOVERAGE)) {
                ctx.print("<th>Cov=" + coverageValue + "<br/>V:" + userHasVoted(ctx.userId()) + "</th>");
            }

            if (options.contains(EShowDataRowSet.doShowTR)) {
                ctx.println("</tr>");
            }

            if (options.contains(EShowDataRowSet.doShowOtherRows)) {
                // Were there any straggler rows we need to go back and show

                // REFERENCE row
                if (areShowingInputBox) {
                    ctx.print("<tr id='r2_" + fullFieldHash() + "'><td class='botgray' colspan=" + SurveyMain.PODTABLE_WIDTH + 2
                        + ">");
                    ctx.print("<div class='itemerrs' id=\"e_" + fullFieldHash() + "\" ><!--  errs for this item --></div>");
                    ctx.println("</td></tr>");
                }

                // now, if the warnings list isn't empty.. warnings rows
                if (!numberedItemsList.isEmpty()) {
                    int mySuperscriptNumber = 0;
                    for (CandidateItem item : numberedItemsList) {
                        mySuperscriptNumber++;
                        if (item == null)
                            continue; /* ?! */
                        if (item.tests == null && item.examples == null)
                            continue; /* skip rows w/o anything */
                        ctx.println("<tr class='warningRow'><td class='botgray'><span class='warningTarget'>#"
                            + mySuperscriptNumber + "</span></td>");
                        if (item.tests != null) {
                            ctx.println("<td colspan='" + (SurveyMain.PODTABLE_WIDTH - 1) + "' class='warncell'>");
                            for (Iterator<CheckStatus> it3 = item.tests.iterator(); it3.hasNext();) {
                                CheckStatus status = it3.next();
                                if (!status.getType().equals(CheckStatus.exampleType)) {
                                    ctx.println("<span class='warningLine'>");
                                    String cls = SurveyMain.shortClassName(status.getCause());
                                    if (status.getType().equals(CheckStatus.errorType)) {
                                        ctx.print(ctx.iconHtml("stop", "Error: " + cls));
                                    } else {
                                        ctx.print(ctx.iconHtml("warn", "Warning: " + cls));
                                    }
                                    ctx.print(status.toString());
                                    ctx.println("</span>");

                                    ctx.println(" For help, see <a " + ctx.atarget(WebContext.TARGET_DOCS)
                                        + " href='http://cldr.org/translation/fixing-errors'>Fixing Errors and Warnings</a>");
                                    ctx.print("<br>");
                                }
                            }
                        } else {
                            ctx.println("<td colspan='" + (SurveyMain.PODTABLE_WIDTH - 1) + "' class='examplecell'>");
                        }
                        if (item.examples != null) {
                            boolean first = true;
                            for (Iterator<ExampleEntry> it4 = item.examples.iterator(); it4.hasNext();) {
                                ExampleEntry e = it4.next();
                                if (first == false) {
                                }
                                String cls = SurveyMain.shortClassName(e.status.getCause());
                                if (e.status.getType().equals(CheckStatus.exampleType)) {
                                    SurveyMain.printShortened(ctx, e.status.toString());
                                    if (cls != null) {
                                        ctx.printHelpLink("/" + cls + "", "Help");
                                    }
                                    ctx.println("<br>");
                                } else {
                                    String theMenu = PathUtilities.xpathToMenu(sm.xpt.getById(parentRow.getXpathId()));
                                    if (theMenu == null) {
                                        theMenu = "raw";
                                    }
                                    ctx.print("<a " + ctx.atarget(WebContext.TARGET_EXAMPLE) + " href='" + ctx.url()
                                        + ctx.urlConnector() + "_=" + ctx.getLocale() + "&amp;x=" + theMenu + "&amp;"
                                        + SurveyMain.QUERY_EXAMPLE + "=" + e.hash + "'>");
                                    ctx.print(ctx.iconHtml("zoom", "Zoom into " + cls) + cls);
                                    ctx.print("</a>");
                                }
                                first = false;
                            }
                        }
                        ctx.println("</td>");
                        ctx.println("</tr>");
                    }
                }
            }

            if (options.contains(EShowDataRowSet.doShowVettingRows)) {

                if (zoomedIn && ((!SurveyMain.isPhaseSubmit() && !SurveyMain.isPhaseBeta()) || SurveyMain.isUnofficial() || true)) {

                    ctx.print("<tr>");
                    ctx.print("<th colspan=2>Votes</th>");
                    ctx.print("<td id='voteresults_" + fieldHash() + "' colspan=" + (SurveyMain.PODTABLE_WIDTH - 2) + ">");

                    showVotingResults(ctx);

                    ctx.println("</td></tr>");

                    if (aliasFromLocale != null) {
                        ctx.print("<tr class='topgray'>");
                        ctx.print("<th class='topgray' colspan=2>Alias Items</th>");
                        ctx.print("<td class='topgray' colspan=" + (SurveyMain.PODTABLE_WIDTH - 2) + ">");

                        String theURL = SurveyForum.forumUrl(ctx, aliasFromLocale, aliasFromXpath);
                        String thePath = sm.xpt.getPrettyPath(aliasFromXpath);
                        String theLocale = aliasFromLocale;

                        ctx.println("Note: Before making changes here, first verify:<br> ");
                        ctx.print("<a class='alias' href='" + theURL + "'>");
                        ctx.print(thePath);
                        if (!aliasFromLocale.equals(locale.getBaseName())) {
                            ctx.print(" in " + new ULocale(theLocale).getDisplayName(ctx.displayLocale));
                        }
                        ctx.print("</a>");
                        ctx.print("");

                        ctx.println("</td></tr>");
                    }
                }
            }
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
                /* ignored */uf.getExampleGenerator().getExampleHtml(getXpath(), topCurrent.value,
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
         * @param ctx
         * @return
         */
        public String getStatusIcon(WebContext ctx) {
            String statusIcon = "";
            if (hasErrors) {
                statusIcon = ctx.iconHtml("stop", "Errors - please zoom in");
            } else if (hasWarnings /*
                                   * && confirmStatus !=
                                   * Vetting.Status.INDETERMINATE
                                   */) {
                statusIcon = ctx.iconHtml("warn", "Warnings - please zoom in");
            }
            return statusIcon;
        }

        /**
         * @param foundWarning
         * @return
         */
        public String getRowClass() {
            // calculate the class of data items
            String rclass = "vother";
            if (hasErrors) {
                rclass = "error";
            } else if (hasWarnings /*
                                   * && confirmStatus !=
                                   * Vetting.Status.INDETERMINATE
                                   */) {
                rclass = "warning";
            }
            return rclass;
        }

        /**
         * @param ctx
         * @return
         */
        public String getVotedIcon(WebContext ctx) {
            String votedIcon = ctx.iconHtml("bar0", "You have not yet voted on this item.");
            if (userHasVoted(ctx.userId())) {
                votedIcon = ctx.iconHtml("vote", "You have already voted on this item.");
            }
            return votedIcon;
        }

        /**
         * @param ctx
         * @return
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
         * @param ctx
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
         * @param ctx
         */
        public void showVotingResults(WebContext ctx) {

            BallotBox<User> ballotBox = sm.getSTFactory().ballotBoxForLocale(locale());
            ctx.put("ballotBox", ballotBox);
            ctx.put("resolver", ballotBox.getResolver(getXpath()));
            ctx.includeFragment("show_voting.jsp");
        }

        @Override
        public String toString() {
            return "{DataRow n='" + getDisplayName() + "', x='" + xpath + "', item#='" + items.size() + "'}";
        }

        /**
         * Calculate the item from the vetted parent locale, possibly including
         * tests
         *
         * @param vettedParent
         *            CLDRFile for the parent locale, resolved with vetting on ( really just the current )
         * @param checkCldr
         *            The tests to use
         * @param options
         *            Test options
         */
        void updateInheritedValue(CLDRFile vettedParent, TestResultBundle checkCldr, Map<String, String> options) {
            long lastTime = System.currentTimeMillis();
            if (vettedParent == null) {
                return;
            }

            if (xpathId == -1) {
                return;
            }

            String xpath = sm.xpt.getById(xpathId);
            if (TRACE_TIME)
                System.err.println("@@0:" + (System.currentTimeMillis() - lastTime));
            if (xpath == null) {
                return;
            }

            if ((vettedParent != null) && (inheritedValue == null)) {
                //String value = vettedParent.getStringValue(xpath);
                Output<String> pathWhereFound = new Output<String>();
                Output<String> localeWhereFound = new Output<String>();
                String value = vettedParent.getConstructedBaileyValue(xpath, pathWhereFound, localeWhereFound);
                if (TRACE_TIME)
                    System.err.println("@@1:" + (System.currentTimeMillis() - lastTime));

                if (value == null) {
                    // no inherited value
                } else if (!items.containsKey(value)) {
                    inheritedValue = addItem(value);
                    if (TRACE_TIME)
                        System.err.println("@@2:" + (System.currentTimeMillis() - lastTime));
                    inheritedValue.isParentFallback = true;

                    //CLDRFile.Status sourceLocaleStatus = new CLDRFile.Status();
                    if (TRACE_TIME)
                        System.err.println("@@3:" + (System.currentTimeMillis() - lastTime));
                    String sourceLocale = localeWhereFound.value; // WAS: vettedParent.getSourceLocaleID(xpath, sourceLocaleStatus);
                    if (TRACE_TIME)
                        System.err.println("@@4:" + (System.currentTimeMillis() - lastTime));

                    inheritedValue.inheritFrom = CLDRLocale.getInstance(sourceLocale);

                    if (/*sourceLocaleStatus != null && sourceLocaleStatus. */pathWhereFound.value != null
                        && !/*sourceLocaleStatus.*/pathWhereFound.value.equals(xpath)) {
                        inheritedValue.pathWhereFound = pathWhereFound.value;
                        if (TRACE_TIME)
                            System.err.println("@@5:" + (System.currentTimeMillis() - lastTime));

                        // set up Pod alias-ness
                        aliasFromLocale = sourceLocale;
                        aliasFromXpath = sm.xpt.xpathToBaseXpathId(pathWhereFound.value);
                        if (TRACE_TIME)
                            System.err.println("@@6:" + (System.currentTimeMillis() - lastTime));
                    }

                    inheritedValue.isBailey = true;
                    inheritedValue.isFallback = true;
                } else { // item already contained
                    CandidateItem otherItem = items.get(value);
                    otherItem.isBailey = true;
                    otherItem.isFallback = true;
                    inheritedValue = otherItem;
                    // throw new InternalError("could not get inherited value: "
                    // + xpath);
                }
            }

            if ((checkCldr != null) && (inheritedValue != null) && (inheritedValue.tests == null)) {
                if (TRACE_TIME)
                    System.err.println("@@7:" + (System.currentTimeMillis() - lastTime));
                List<CheckStatus> iTests = new ArrayList<CheckStatus>();
                checkCldr.check(xpath, iTests, inheritedValue.value);
                if (TRACE_TIME)
                    System.err.println("@@8:" + (System.currentTimeMillis() - lastTime));
                // checkCldr.getExamples(xpath, fullPath, value,
                // ctx.getOptionsMap(), examplesResult);
                if (!iTests.isEmpty()) {
                    inheritedValue.setTests(iTests);
                    if (TRACE_TIME)
                        System.err.println("@@9:" + (System.currentTimeMillis() - lastTime));
                }
            }
            if (TRACE_TIME)
                System.err.println("@@10:" + (System.currentTimeMillis() - lastTime));
        }

        /**
         * Returns true if a user has voted or not.
         *
         * @param userId
         * @return true if user has voted at all, false otherwise. Will return
         *         false if user changes their vote back to no opinion.
         */
        public boolean userHasVoted(int userId) {
            return ballotBox.userDidVote(sm.reg.getInfo(userId), getXpath());
        }

        /**
         * Per row
         */
        @Override
        public String toJSONString() throws JSONException {

            try {
                String winningVhash = "";
                CandidateItem winningItem = getWinningItem();
                if (winningItem != null) {
                    /* Use getOriginalValueHash here, not getAdjustedValueHash. This is to fix
                     * a regression due to changeset 14263 as seen on smoketest, described as
                     * "seemingly duplicate votes visible" at https://unicode.org/cldr/trac/ticket/10521 */
                    winningVhash = winningItem.getOriginalValueHash();
                }
                String voteVhash = "";
                String ourVote = null;
                if (userForVotelist != null) {
                    ourVote = ballotBox.getVoteValue(userForVotelist, xpath);
                    if (ourVote != null) {
                        CandidateItem voteItem = items.get(ourVote);
                        if (voteItem != null) {
                            /* TODO: should this be getAdjustedValueHash or getOriginalValueHash? */
                            voteVhash = voteItem.getAdjustedValueHash();
                        }
                    }
                }

                JSONObject itemsJson = new JSONObject();
                for (CandidateItem i : items.values()) {
                    /* getOriginalValueHash, not getAdjustedValueHash, is required here.
                     * items.values() may include an item with value ↑↑↑ INHERITANCE_MARKER and isBailey = false, and an item
                     * with an inherited value and isBailey = true, isFallback = true, locale.isLanguageLocale() = false.
                     * If getAdjustedValueHash were called here (as it was, under the old name getValueHash), both items would
                     * get the same key DataSection.getValueHash(CldrUtility.INHERITANCE_MARKER) = 4oaR4oaR4oaR, and only the
                     * last one would remain in itemsJson, leading to the browser console error "there is no Bailey Target item".
                     * See https://unicode.org/cldr/trac/ticket/10521
                     * See http://www.ietf.org/rfc/rfc4627.txt - "the names within an object SHOULD be unique".
                     */
                    String key = i.getOriginalValueHash();
                    if (itemsJson.has(key)) {
                        System.out.println("Warning: value hash key " + key + " is duplicate");
                    }
                    itemsJson.put(key, i);
                }

                String displayExample = null;
                //String displayHelp = null;
                ExampleBuilder b = getExampleBuilder();
                if (b != null) {
                    displayExample = b.getExampleHtml(xpath, displayName, ExampleType.ENGLISH);
                    // displayHelp = b.getHelpHtml(xpath, displayName);
                }
                String pathCode = "?";
                PathHeader ph = getPathHeader();
                if (ph != null) {
                    pathCode = ph.getCode();
                }

                VoteResolver<String> resolver = ballotBox.getResolver(xpath);
                JSONObject jo = new JSONObject();
                jo.put("xpath", xpath);
                jo.put("xpid", xpathId);
                jo.put("rowFlagged", sm.getSTFactory().getFlag(locale, xpathId));
                jo.put("xpstrid", XPathTable.getStringIDString(xpath));
                jo.put("winningValue", winningValue != null ? winningValue : "");
                jo.put("displayName", displayName);
                jo.put("displayExample", displayExample);
                // .put("showstatus",
                // (ph!=null)?ph.getSurveyToolStatus():null)
                jo.put("statusAction", getStatusAction());
                //.put("prettyPath", getPrettyPath())
                jo.put("code", pathCode);
                jo.put("extraAttributes", getNonDistinguishingAttributes());
                jo.put("coverageValue", coverageValue);
                jo.put("hasErrors", hasErrors);
                jo.put("hasWarnings", hasWarnings);
                jo.put("confirmStatus", confirmStatus);
                jo.put("hasVoted", userForVotelist != null ? userHasVoted(userForVotelist.id) : false);
                jo.put("winningVhash", winningVhash);
                jo.put("ourVote", ourVote);
                jo.put("voteVhash", voteVhash);
                jo.put("voteResolver", SurveyAjax.JSONWriter.wrap(resolver));
                jo.put("items", itemsJson);
                jo.put("inheritedValue", inheritedValue != null ? inheritedValue.value : null);
                jo.put("inheritedXPath", inheritedValue != null ? inheritedValue.pathWhereFound : null);
                jo.put("inheritedXpid",
                    (inheritedValue != null && inheritedValue.pathWhereFound != null) ? XPathTable.getStringIDString(inheritedValue.pathWhereFound) : null);
                jo.put("inheritedLocale", inheritedValue != null ? inheritedValue.inheritFrom : null);
                jo.put("inheritedPClass", inheritedValue != null ? inheritedValue.getPClass() : "fallback");
                jo.put("canFlagOnLosing", resolver.getRequiredVotes() == VoteResolver.HIGH_BAR);
                if (ph.getSurveyToolStatus() == SurveyToolStatus.LTR_ALWAYS) {
                    jo.put("dir", "ltr");
                }
                return jo.toString();
            } catch (Throwable t) {
                SurveyLog.logException(t, "Exception in toJSONString of " + this);
                throw new JSONException(t);
            }
        }

        Map<String, String> nonDistinguishingAttributes = null;
        boolean checkedNDA = false;

        private Map<String, String> getNonDistinguishingAttributes() {
            if (checkedNDA == false) {
                String fullDiskXPath = diskFile.getFullXPath(xpath);
                nonDistinguishingAttributes = sm.xpt.getUndistinguishingElementsFor(fullDiskXPath, new XPathParts(null, null));
                checkedNDA = true;
            }
            return nonDistinguishingAttributes;
        }

        public StatusAction getStatusAction() {
            // null because this is for display.
            return SurveyMain.phase().getCPhase()
                .getShowRowAction(this, InputMethod.DIRECT, getPathHeader().getSurveyToolStatus(), userForVotelist);
        }

        @Override
        public String getLastReleaseValue() {
            return oldValue;
        }

        @Override
        public Level getCoverageLevel() {
            // access the private method of the enclosing class
            return getCoverageInfo().getCoverageLevel(getXpath(), locale.getBaseName());
        }

        /**
         * There was at least one vote at the end of DataSubmission, or there is
         * a vote now. TODO: add check for whether there was a vote in data
         * submission.
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

    private User userForVotelist = null;

    public void setUserAndFileForVotelist(User u, CLDRFile f) {
        userForVotelist = u;
        getExampleBuilder(f);
    }

    /**
     * A class representing a list of rows, in sorted and divided order.
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
         */

        public Partition partitions[]; // display group partitions. Might only
        // contain one entry: {null, 0, <end>}.
        // Otherwise, contains a list of entries
        // to be named separately
        DataRow rows[]; // list of rows in sorted order
        SortMode sortMode = null;

        /**
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

        public int showSkipBox(WebContext ctx, int skip) {
            int total = size();
            DataRow displayList[] = null;
            if (this != null) {
                displayList = rows;
            }
            ctx.println("<div class='pager' style='margin: 2px'>");
            if (false && (ctx.getLocale() != null) && UserRegistry.userCanModifyLocale(ctx.session.user, ctx.getLocale())) { // at
                // least
                // street
                // level
                if ((ctx.field(SurveyMain.QUERY_SECTION).length() > 0)
                    && !ctx.field(SurveyMain.QUERY_SECTION).equals(SurveyMain.xMAIN)) {
                    // ctx.println("<input  type='submit' value='" +
                    // SurveyMain.getSaveButtonText() + "'>"); //
                    // style='float:left'
                }
            }
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

            if (true) { // showsearchmode
                ctx.println("<div style='float: right;'>Items " + from + " to " + to + " of " + total + "</div>");
                ctx.println("<p class='hang' > " + // float: right;
                // tyle='margin-left: 3em;'
                    "<b>Sorted:</b>  ");
                {
                    // boolean sortAlpha =
                    // (sortMode.equals(PREF_SORTMODE_ALPHA));

                    // showSkipBox_menu(ctx, sortMode, PREF_SORTMODE_ALPHA,
                    // "Alphabetically");
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
                }

                {
                    WebContext subCtx = (WebContext) ctx.clone();
                    if (skip > 0) {
                        subCtx.setQuery("skip", new Integer(skip).toString());
                    }
                }

                ctx.println("</p>");
            }

            // Print navigation
            if (true) { // showsearchmode

                if (total >= (ctx.prefCodesPerPage())) {
                    int prevSkip = skip - ctx.prefCodesPerPage();
                    if (prevSkip < 0) {
                        prevSkip = 0;
                    }
                    ctx.print("<p class='hang'>");
                    if (skip <= 0) {
                        ctx.print("<span class='pagerl_inactive'>\u2190&nbsp;prev"/*
                                                                                  * +
                                                                                  * ctx
                                                                                  * .
                                                                                  * prefCodesPerPage
                                                                                  * (
                                                                                  * )
                                                                                  */
                            + "" + "</span>&nbsp;");
                    } else {
                        ctx.print("<a class='pagerl_active' href=\"" + ctx.url() + ctx.urlConnector() + "skip="
                            + new Integer(prevSkip) + "\">" + "\u2190&nbsp;prev"/*
                                                                                * +
                                                                                * ctx
                                                                                * .
                                                                                * prefCodesPerPage
                                                                                * (
                                                                                * )
                                                                                */
                            + "");
                        ctx.print("</a>&nbsp;");
                    }
                    int nextSkip = skip + ctx.prefCodesPerPage();
                    if (nextSkip >= total) {
                        nextSkip = -1;
                        if (total >= (ctx.prefCodesPerPage())) {
                            ctx.println(" <span class='pagerl_inactive' >" + "next&nbsp;"/*
                                                                                         * +
                                                                                         * ctx
                                                                                         * .
                                                                                         * prefCodesPerPage
                                                                                         * (
                                                                                         * )
                                                                                         */
                                + "\u2192" + "</span>");
                        }
                    } else {
                        ctx.println(" <a class='pagerl_active' href=\"" + ctx.url() + ctx.urlConnector() + "skip="
                            + new Integer(nextSkip) + "\">" + "next&nbsp;"/*
                                                                          * +
                                                                          * ctx
                                                                          * .
                                                                          * prefCodesPerPage
                                                                          * (
                                                                          * )
                                                                          */
                            + "\u2192" + "</a>");
                    }
                    ctx.print("</p>");
                }
                // ctx.println("<br/>");
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
                            + partitions[j].name + ":</b>"
                        /* + "</td><td class='pagerln'>" */);
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

        public int size() {
            return rows.length;
        }

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
     * This class represents an Example box, so that it can be stored and
     * restored.
     */
    public class ExampleEntry {

        public DataSection.DataRow dataRow;

        public String hash = null;
        public DataRow.CandidateItem item;
        public DataSection section;
        public CheckCLDR.CheckStatus status;

        public ExampleEntry(DataSection section, DataRow p, DataRow.CandidateItem item, CheckCLDR.CheckStatus status) {
            this.section = section;
            this.dataRow = p;
            this.item = item;
            this.status = status;

            hash = CookieSession.cheapEncode(DataSection.getN()) + // unique
            // serial #-
            // covers
            // item,
            // status..
                p.fullFieldHash(); /*
                                   * fieldHash ensures that we don't get
                                   * the wrong field..
                                   */
        }
    }

    public static String CHANGES_DISPUTED = "Disputed";

    /**
     * Divider denoting a specific Continent division.
     */
    public static final String CONTINENT_DIVIDER = "~";

    public static final String DATASECTION_MISSING = "Inherited";

    public static final String DATASECTION_NORMAL = "Normal";
    public static final String DATASECTION_PRIORITY = "Priority";
    public static final String DATASECTION_PROPOSED = "Proposed";
    public static final String DATASECTION_VETPROB = "Vetting Issue";
    private static final boolean DEBUG = false || CldrUtility.getProperty("TEST", false);
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
    private static boolean isInitted = false;
    /**
     * @deprecated only used in non-pageID use.
     */
    private static Pattern mostPattern;

    private static int n = 0;
    static final Pattern NAME_TYPE_PATTERN = PatternCache.get("[a-zA-Z0-9]+|.*exemplarCity.*");

    //    private static final boolean NOINHERIT = true;

    // private static Pattern noisePattern;

    // public boolean exemplarCityOnly = false;

    public static String PARTITION_ERRORS = "Error Values";
    public static String PARTITION_UNCONFIRMED = "Unconfirmed";

    /**
     * Show time taken to populate?
     */
    private static final boolean TRACE_TIME = false;
    private static final boolean SHOW_TIME = false || TRACE_TIME || DEBUG || CldrUtility.getProperty("TEST", false);

    public static String STATUS_QUO = "Status Quo";

    public static String TENTATIVELY_APPROVED = "Tentatively Approved";
    /**
     * Trace in detail time taken to populate?
     */
    private static Pattern typeReplacementPattern;
    /**
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

    protected static synchronized int getN() {
        return ++n;
    } // serial number

    private static synchronized void init() {
        if (!isInitted) {
            typeReplacementPattern = PatternCache.get("\\[@(?:type|key)=['\"]([^'\"]*)['\"]\\]");
            PatternCache.get("([^/]*)/(.*)");

            /* This one is only used with non-pageID use. */
            mostPattern = PatternCache.get("^//ldml/localeDisplayNames.*|"
                + // these are excluded when 'misc' is chosen.
                "^//ldml/characters/exemplarCharacters.*|" + "^//ldml/numbers.*|" + "^//ldml/units.*|"
                + "^//ldml/references.*|" + "^//ldml/dates/timeZoneNames/zone.*|" + "^//ldml/dates/timeZoneNames/metazone.*|"
                + "^//ldml/dates/calendar.*|" + "^//ldml/identity.*");

            /* Always excluded. Compare with PathHeader/Coverage. */
            excludeAlways = PatternCache.get("^//ldml/segmentations.*|" + "^//ldml/measurement.*|" + ".*week/minDays.*|"
                + ".*week/firstDay.*|" + ".*/usesMetazone.*|" + ".*week/weekendEnd.*|" + ".*week/weekendStart.*|" +
                // "^//ldml/dates/.*localizedPatternChars.*|" +
                "^//ldml/posix/messages/.*expr$|" + "^//ldml/dates/timeZoneNames/.*/GMT.*exemplarCity$|" // //ldml/dates/timeZoneNames/zone[@type="Etc/GMT+11"]/exemplarCity
                + "^//ldml/dates/.*default");// no defaults

            int pn;
            for (pn = 0; pn < fromto.length / 2; pn++) {
                fromto_p[pn] = PatternCache.get(fromto[pn * 2]);
            }

        }
        isInitted = true;
    }

    /**
     * @return a new XPathMatcher that matches all paths in the hacky
     *         excludeAlways regex. For testing.
     * @deprecated
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
        DataSection section = new DataSection(pageId, session.sm, locale, prefix, matcher, ptype);

        section.hasExamples = true;

        // XMLSource ourSrc = uf.resolvedSource;
        CLDRFile ourSrc = session.sm.getSTFactory().make(locale.getBaseName(), true, true);

        ourSrc.setSupplementalDirectory(session.sm.getSupplementalDirectory());
        if (ctx != null) {
            section.setUserAndFileForVotelist(ctx.session != null ? ctx.session.user : null, ourSrc);
        } else if (session != null && session.user != null) {
            section.setUserAndFileForVotelist(session.user, ourSrc);
        }
        if (ourSrc.getSupplementalDirectory() == null) {
            throw new InternalError("?!! ourSrc hsa no supplemental dir!");
        }
        synchronized (session) {
            TestResultBundle checkCldr = session.sm.getSTFactory().getTestResult(locale, getOptions(ctx, session, locale));
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
            section.baselineFile = session.sm.getBaselineFile();
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
            section.populateFrom(ourSrc, checkCldr, null, workingCoverageLevel);
            int popCount = section.getAll().size();
            if (false)
                System.err.println("PopCount: " + popCount);
            if (false && popCount > 0) {
                System.err.println("Item[0] : " + section.getAll().iterator().next());
                System.err.println("Item[0] Items: " + section.getAll().iterator().next().items.size());
            }
            /*
             * if(SHOW_TIME) { System.err.println("DP: Time taken to populate "
             * + locale + " // " + prefix +":"+ctx.defaultPtype()+ " = " + et +
             * " - Count: " + pod.getAll().size()); }
             */
            if (showLoading && ctx != null) {
                ctx.println("<script type=\"text/javascript\">document.getElementById('loadSection').innerHTML='Completing..."
                    + popCount + " items';</script>");
                ctx.flush();
            }
            if (pageId == null) {
                section.ensureComplete(ourSrc, checkCldr, null, workingCoverageLevel);
                popCount = section.getAll().size();
                if (false)
                    System.err.println("New PopCount: " + popCount);
            }
            if (false && popCount > 0) {
                System.err.println("Item[0] : " + section.getAll().iterator().next());
                System.err.println("Item[0] Items: " + section.getAll().iterator().next().items.size());
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
     * @param ctx
     * @param session
     * @param locale
     * @return
     */
    public static CheckCLDR.Options getOptions(WebContext ctx, CookieSession session, CLDRLocale locale) {
        CheckCLDR.Options options;
        if (ctx != null) {
            options = (ctx.getOptionsMap());
        } else {
            // ugly
            final String def = CookieSession.sm
                .getListSetting(session.settings(), SurveyMain.PREF_COVLEV,
                    WebContext.PREF_COVLEV_LIST, false);

            final String org = session.getEffectiveCoverageLevel(locale.toString());

            options = new Options(locale, SurveyMain.getTestPhase(), def, org);
        }
        return options;
    }

    /**
     * Given a (cleaned, etc) xpath, this returns the podBase, i.e.
     * context.getPod(base), that would be used to show that xpath. Keep this in
     * sync with SurveyMain.showLocale() where there is the list of menu items.
     */
    public static String xpathToSectionBase(String xpath) {
        int n;
        String base;

        // is it one of the prefixes we can check statically?
        String staticBases[] = {
            // LOCALEDISPLAYNAMES
            "//ldml/" + PathUtilities.NUMBERSCURRENCIES, "//ldml/" + "dates/timeZoneNames/zone",
            "//ldml/" + "dates/timeZoneNames/metazone",
            // OTHERROOTS
            SurveyMain.GREGO_XPATH, PathUtilities.LOCALEDISPLAYPATTERN_XPATH, PathUtilities.OTHER_CALENDARS_XPATH,
            "//ldml/units" };

        // is it one of the static bases?
        for (n = 0; n < staticBases.length; n++) {
            if (xpath.startsWith(staticBases[n])) {
                return staticBases[n];
            }
        }

        // dynamic LOCALEDISPLAYNAMES
        for (n = 0; n < PathUtilities.LOCALEDISPLAYNAMES_ITEMS.length; n++) { // is
            // it
            // a
            // simple
            // code
            // list?
            base = PathUtilities.LOCALEDISPLAYNAMES + PathUtilities.LOCALEDISPLAYNAMES_ITEMS[n] + '/'
                + SurveyMain.typeToSubtype(PathUtilities.LOCALEDISPLAYNAMES_ITEMS[n]); // see:
            // SurveyMain.showLocaleCodeList()
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

    Hashtable<String, ExampleEntry> exampleHash = new Hashtable<String, ExampleEntry>(); // hash
    // of
    // examples

    private String sectionHash; // prefix string used for calculating html
    // fields

    public boolean hasExamples = false;
    public String intgroup;
    boolean isCalendar = false; // Is this a calendar section?
    boolean isMetazones = false; // Is this a metazones section?
    public String[] LAYOUT_INLIST_VALUES = { "titlecase-words", "titlecase-firstword", "lowercase-words", "mixed" }; // layout/inList/*
    // - from
    // UTS35

    public String[] LAYOUT_INTEXT_VALUES = { "titlecase-words", "titlecase-firstword", "lowercase-words", "mixed" }; // layout/inText/*
    // - from
    // UTS35

    public String[] METAZONE_COMMONLYUSED_VALUES = { "true", "false" }; // layout/inText/*
    // -
    // from
    // UTS35

    final Collator myCollator = CodeSortMode.createCollator();

    private String ptype;

    Hashtable<String, DataRow> rowsHash = new Hashtable<String, DataRow>(); // hashtable
    // of
    // type->Row
    public int skippedDueToCoverage = 0; // How many were skipped due to
    // coverage?

    private SurveyMain sm;
    long touchTime = -1; // when has this pod been hit?
    public String xpathPrefix = null;

    private CLDRLocale locale;
    private ExampleBuilder examplebuilder;
    private XPathMatcher matcher;
    private PageId pageId;
    private CLDRFile diskFile;

    DataSection(PageId pageId, SurveyMain sm, CLDRLocale loc, String prefix, XPathMatcher matcher, String ptype) {
        this.locale = loc;
        this.sm = sm;
        this.ptype = ptype;
        this.matcher = matcher;
        xpathPrefix = prefix;
        if (prefix != null) {
            sectionHash = CookieSession.cheapEncode(sm.xpt.getByXpath(prefix));
        }
        intgroup = loc.getLanguage(); // calculate interest group
        ballotBox = sm.getSTFactory().ballotBoxForLocale(locale);
        this.pageId = pageId;
    }

    void addDataRow(DataRow p) {
        rowsHash.put(p.xpath, p);
    }

    /**
     * Enregister an ExampleEntry
     */
    ExampleEntry addExampleEntry(ExampleEntry e) {
        synchronized (exampleHash) {
            exampleHash.put(e.hash, e);
        }
        return e; // for the hash.
    }

    public PageId getPageId() {
        return pageId;
    }

    public long age() {
        return System.currentTimeMillis() - touchTime;
    }

    public DisplaySet createDisplaySet(SortMode sortMode, XPathMatcher matcher) {
        DisplaySet aDisplaySet = sortMode.createDisplaySet(matcher, rowsHash.values());
        aDisplaySet.canName = canName;
        aDisplaySet.isCalendar = isCalendar;
        aDisplaySet.isMetazones = isMetazones;
        return aDisplaySet;
    }

    /**
     * Makes sure this pod contains the rows we'd like to see.
     *
     * @obsolete not called anymore
     */
    private void ensureComplete(CLDRFile ourSrc, TestResultBundle checkCldr, Map<String, String> options,
        String workingCoverageLevel) {
        // if (!ourSrc.isResolving()) throw new
        // IllegalArgumentException("CLDRFile must be resolved");
        // if(xpathPrefix.contains("@type")) {
        // if(DEBUG)
        // System.err.println("Bailing- no reason to complete a type-specifix xpath");
        // return; // don't try to complete if it's a specific item.
        // } else if(DEBUG) {
        // System.err.println("Completing: " + xpathPrefix);
        // }

        STFactory stf = sm.getSTFactory();
        SectionId sectionId = (pageId != null) ? pageId.getSectionId() : null;

        //SupplementalDataInfo sdi = sm.getSupplementalDataInfo();
        int workingCoverageValue = Level.fromString(workingCoverageLevel).getLevel();
        if (sectionId == SectionId.Timezones || pageId == PageId.Timezone_Display_Patterns
            || (pageId == null && xpathPrefix.startsWith("//ldml/" + "dates/timeZoneNames"))) {
            // work on zones
            boolean isMetazones = (sectionId == SectionId.Timezones)
                || (pageId == null && xpathPrefix.startsWith("//ldml/" + "dates/timeZoneNames/metazone"));
            boolean isSingleXPath = false;
            // Make sure the pod contains the rows we'd like to see.
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
                // "/long/generic", "/long/daylight", "/long/standard",
                // "/short/generic", "/short/daylight",
                // "/short/standard",
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
            // XPathParts parts = new XPathParts(null,null);
            // TimezoneFormatter timezoneFormatter = new
            // TimezoneFormatter(resolvedFile, true); // TODO: expensive here.

            for (String zone : zoneIterator) {
                if (zone == null) {
                    throw new NullPointerException("zoneIterator.next() returned null! zoneIterator.size: " + zoneIterator.size()
                        + ", isEmpty: " + zoneIterator.isEmpty());
                }
                // System.err.println(">> " + zone);
                /** some compatibility **/
                String ourSuffix = "[@type=\"" + zone + "\"]";
                if (isMetazones) {
                }

                for (int i = 0; i < suffs.length; i++) {
                    String suff = suffs[i];

                    if (isSingleXPath && !xpathPrefix.contains(suff)) {
                        continue; // Try not to add paths that won't be shown.
                        // } else {
                        // podBase = "//ldml/dates/timeZoneNames/metazone";
                    }
                    // synthesize a new row..
                    //String rowXpath = zone + suff;
                    String base_xpath_string = podBase + ourSuffix + suff;

                    SurveyToolStatus ststats = SurveyToolStatus.READ_WRITE;
                    PathHeader ph = stf.getPathHeader(base_xpath_string);
                    if (ph != null) {
                        ststats = stf.getPathHeader(base_xpath_string).getSurveyToolStatus();
                    }

                    // boolean cc_skip =
                    // CheckCLDR.skipShowingInSurvey.matcher(xpath).matches();
                    // if (cc_skip != (ststats==SurveyToolStatus.HIDE ||
                    // ststats==SurveyToolStatus.DEPRECATED)) {
                    // System.err.println(xpath +
                    // " - skipShowingInSurvey="+cc_skip+", ph="+ststats);
                    // }
                    //
                    // if(cc_skip) continue;

                    if ((ststats == SurveyToolStatus.HIDE || ststats == SurveyToolStatus.DEPRECATED)) {
                        // if(TRACE_TIME)
                        // System.err.println("ns1 8 "+(System.currentTimeMillis()-nextTime)
                        // + " " + xpath);
                        continue;
                    }

                    int xpid = sm.xpt.getByXpath(base_xpath_string);
                    if (matcher != null && !matcher.matches(base_xpath_string, xpid)) {
                        continue;
                    }
                    if (excludeAlways.matcher(base_xpath_string).matches()) {
                        continue;
                    }

                    // System.out.println("@@@ Considering: " +
                    // base_xpath_string + "  - matcher="+matcher);

                    if (resolvedFile.isPathExcludedForSurvey(base_xpath_string)) {
                        // if(SurveyMain.isUnofficial)
                        // System.err.println("@@ synthesized+excluded:" +
                        // base_xpath_string);
                        continue;
                    }

                    // Only display metazone data for which an English value
                    // exists
                    if (isMetazones && suff != "/commonlyUsed") {
                        String engValue = baselineFile.getStringValue(base_xpath_string);
                        if (engValue == null || engValue.length() == 0) {
                            continue;
                        }
                    }
                    // Filter out data that is higher than the desired coverage
                    // level
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
                    myp.setShimTests(base_xpath, base_xpath_string, checkCldr, options);
                }
            }
        } // tz
    }

    /**
     * Get the CoverageInfo object from CLDR
     * @return
     */
    private CoverageInfo getCoverageInfo() {
        synchronized (GET_COVERAGEINFO_SYNC) {
            if (covInfo == null) {
                covInfo = CLDRConfig.getInstance().getCoverageInfo();
            }
            return covInfo;
        }
    }

    // ==

    /**
     * get all rows.. unsorted.
     */
    public Collection<DataRow> getAll() {
        return rowsHash.values();
    }

    public BallotBox<User> getBallotBox() {
        return ballotBox;
    }

    /**
     * Linear search for matching item.
     *
     * @param xpath
     * @return the matching DatRow
     */
    public DataRow getDataRow(int xpath) {
        return getDataRow(sm.xpt.getById(xpath));
    }

    public DataRow getDataRow(String xpath) {
        if (xpath == null) {
            throw new InternalError("type is null");
        }
        if (rowsHash == null) {
            throw new InternalError("rowsHash is null");
        }
        DataRow p = rowsHash.get(xpath);
        if (p == null) {
            p = new DataRow(xpath);
            addDataRow(p);
        }
        return p;
    }

    // private DataRow getDataRow(String type, String altType) {
    // if(altType == null) {
    // return getDataRow(type);
    // } else {
    // DataRow superDataRow = getDataRow(type);
    // return superDataRow.getSubDataRow(altType);
    // }
    // }

    /**
     * Given a hash, see addExampleEntry, retrieve the ExampleEntry which has
     * section, row, candidateitem and status
     */
    ExampleEntry getExampleEntry(String hash) {
        synchronized (exampleHash) {
            return exampleHash.get(hash);
        }
    }

    private String getPtype() {
        return ptype;
    }

    public int getSkippedDueToCoverage() {
        return skippedDueToCoverage;
    }

    private void populateFrom(CLDRFile ourSrc, TestResultBundle checkCldr, Map<String, String> options,
        String workingCoverageLevel) {
        XPathParts xpp = new XPathParts(null, null);
        CLDRFile aFile = ourSrc;
        STFactory stf = sm.getSTFactory();
        CLDRFile oldFile = stf.getOldFile(locale);
        diskFile = stf.getDiskFile(locale);
        List<CheckStatus> examplesResult = new ArrayList<CheckStatus>();
        long lastTime = -1;
        String workPrefix = xpathPrefix;
        long nextTime = -1;
        int count = 0;
        long countStart = 0;
        if (SHOW_TIME) {
            lastTime = countStart = System.currentTimeMillis();
        }

        int workingCoverageValue = Level.fromString(workingCoverageLevel).getLevel();

        //        CLDRFile vettedParent = null;
        //        CLDRLocale parentLoc = locale.getParent();
        //        if (parentLoc != null) {
        //            XMLSource vettedParentSource = sm.makeDBSource(parentLoc, true /* finalData */, true);
        //            vettedParent = new CLDRFile(vettedParentSource).setSupplementalDirectory(SurveyMain.supplementalDataDir);
        //        }
        Set<String> allXpaths;

        String continent = null;
        Set<String> extraXpaths = null;
        List<CheckStatus> checkCldrResult = new ArrayList<CheckStatus>();

        if (pageId != null) {
            allXpaths = PathHeader.Factory.getCachedPaths(pageId.getSectionId(), pageId);
            allXpaths.retainAll(stf.getPathsForFile(locale));
        } else {
            init(); // pay for the patterns
            allXpaths = new HashSet<String>();
            extraXpaths = new HashSet<String>();

            /* ** Determine which xpaths to show */
            /* if (pageId != null) {
                // use pageid, ignore the rest
            } else */if (xpathPrefix.startsWith("//ldml/units")) {
                canName = false;
            } else if (xpathPrefix.startsWith("//ldml/numbers")) {
                if (-1 == xpathPrefix.indexOf("currencies")) {
                    canName = false; // sort numbers by code
                } else {
                    canName = false; // because symbols are included
                    // hackCurrencyDisplay = true;
                }
            } else if (xpathPrefix.startsWith("//ldml/dates")) {
                if (xpathPrefix.startsWith("//ldml/dates/timeZoneNames/zone")) {
                    // System.err.println("ZZ0");
                } else if (xpathPrefix.startsWith("//ldml/dates/timeZoneNames/metazone")) {
                    int continentStart = xpathPrefix.indexOf(DataSection.CONTINENT_DIVIDER);
                    if (continentStart > 0) {
                        continent = xpathPrefix.substring(xpathPrefix.indexOf(DataSection.CONTINENT_DIVIDER) + 1);
                    }
                    if (DEBUG)
                        System.err.println(xpathPrefix + ": -> continent " + continent);
                    if (!xpathPrefix.contains("@type")) { // if it's not a
                        // zoom-in..
                        workPrefix = "//ldml/dates/timeZoneNames/metazone";
                    }
                    // System.err.println("ZZ1");
                }
            } else if (xpathPrefix.equals("//ldml/references")) {
                canName = false; // disable 'view by name' for references
            }

            isCalendar = xpathPrefix.startsWith("//ldml/dates/calendars");
            isMetazones = xpathPrefix.startsWith("//ldml/dates/timeZoneNames/metazone");

            /* ** Build the set of xpaths */
            // iterate over everything in this prefix ..
            Set<String> baseXpaths = stf.getPathsForFile(locale, xpathPrefix);

            allXpaths.addAll(baseXpaths);
            if (aFile.getSupplementalDirectory() == null) {
                throw new InternalError("?!! aFile hsa no supplemental dir!");
            }
            aFile.getExtraPaths(workPrefix, extraXpaths);
            extraXpaths.removeAll(baseXpaths);
            allXpaths.addAll(extraXpaths);

            // // Process extra paths.
            if (DEBUG)
                System.err.println("@@X@ base[" + workPrefix + "]: " + baseXpaths.size() + ", extra: " + extraXpaths.size());

        }

        /* ** iterate over all xpaths */
        for (String xpath : allXpaths) {
            boolean confirmOnly = false;
            if (xpath == null) {
                throw new InternalError("null xpath in allXpaths");
            }
            int xpid = sm.xpt.getByXpath(xpath);
            if (pageId == null) {
                if (matcher != null && !matcher.matches(xpath, xpid)) {
                    continue;
                }
                if (!xpath.startsWith(workPrefix)) {
                    if (DEBUG && SurveyMain.isUnofficial())
                        System.err.println("@@ BAD XPATH " + xpath);
                    continue;
                } else if (aFile.isPathExcludedForSurvey(xpath)) {
                    if (DEBUG && SurveyMain.isUnofficial())
                        System.err.println("@@ excluded:" + xpath);
                    continue;
                } else if (DEBUG) {
                    System.err.println("allPath: " + xpath);
                }
            }
            boolean isExtraPath = extraXpaths != null && extraXpaths.contains(xpath); // 'extra'
            // paths get
            // shim
            // treatment
            // /*srl*/ if(xpath.indexOf("Adak")!=-1)
            // /*srl*/ {ndebug=true;System.err.println("p] "+xpath +
            // " - xtz = "+excludeTimeZones+"..");}

            if (SHOW_TIME) {
                count++;
                nextTime = System.currentTimeMillis();
                if ((nextTime - lastTime) > 10000) {
                    lastTime = nextTime;
                    System.err.println("[] " + locale + ":" + xpathPrefix + " #" + count + ", or "
                        + (((double) (System.currentTimeMillis() - countStart)) / count) + "ms per.");
                }
            }

            SurveyToolStatus ststats = SurveyToolStatus.READ_WRITE;
            PathHeader ph = stf.getPathHeader(xpath);
            if (ph != null) {
                ststats = stf.getPathHeader(xpath).getSurveyToolStatus();
            }

            // boolean cc_skip =
            // CheckCLDR.skipShowingInSurvey.matcher(xpath).matches();
            // if (cc_skip != (ststats==SurveyToolStatus.HIDE ||
            // ststats==SurveyToolStatus.DEPRECATED)) {
            // System.err.println(xpath +
            // " - skipShowingInSurvey="+cc_skip+", ph="+ststats);
            // }
            //
            // if(cc_skip) continue;

            if ((ststats == SurveyToolStatus.HIDE || ststats == SurveyToolStatus.DEPRECATED)) {
                // if(TRACE_TIME)
                // System.err.println("ns1 8 "+(System.currentTimeMillis()-nextTime)
                // + " " + xpath);
                continue;
            }

            String fullPath = aFile.getFullXPath(xpath);
            // int xpath_id = src.xpt.getByXpath(fullPath);
            int base_xpath = sm.xpt.xpathToBaseXpathId(xpath);
            String baseXpath = sm.xpt.getById(base_xpath);

            int coverageValue = getCoverageInfo().getCoverageValue(baseXpath, locale.getBaseName());
//            int coverageValue = sm.getSupplementalDataInfo().getCoverageValue(baseXpath, locale.getBaseName());
            if (coverageValue > workingCoverageValue) {
                if (coverageValue <= Level.COMPREHENSIVE.getLevel()) {
                    skippedDueToCoverage++;
                } // else: would never be shown, don't care
                continue;
            }

            // sm.xpt.getPrettyPath(base_xpath);

            if (fullPath == null) {
                fullPath = xpath; // (this is normal for 'extra' paths)
            }

            if (TRACE_TIME)
                System.err.println("ns0  " + (System.currentTimeMillis() - nextTime));
            String value = isExtraPath ? null : aFile.getStringValue(xpath);
            if (value == null) {
                // value = "(NOTHING)"; /* This is set to prevent crashes..
                // */
                if (DEBUG) {
                    System.err.println("warning: populatefrom " + this + ": " + locale + ":" + xpath + " = NULL! wasExtraPath="
                        + isExtraPath);
                }
                isExtraPath = true;
            }

            // determine 'alt' param
            String alt = sm.xpt.altFromPathToTinyXpath(xpath, xpp);

            // System.err.println("n03  "+(System.currentTimeMillis()-nextTime));

            /* FULL path processing (references.. alt proposed.. ) */
            xpp.clear();
            xpp.initialize(fullPath);
            String lelement = xpp.getElement(-1);
            xpp.findAttributeValue(lelement, LDMLConstants.ALT);
            // String eRefs = xpp.findAttributeValue(lelement,
            // LDMLConstants.REFERENCES);
            String eDraft = xpp.findAttributeValue(lelement, LDMLConstants.DRAFT);
            if (TRACE_TIME)
                System.err.println("n04  " + (System.currentTimeMillis() - nextTime));

            String typeAndProposed[] = LDMLUtilities.parseAlt(alt);
            String altProposed = typeAndProposed[1];

            // Now we are ready to add the data

            // Load the 'data row' which represents one user visible row of
            // options
            // (may be nested in the case of alt types)
            DataRow p = getDataRow(xpath);

            if (oldFile != null) {
                p.oldValue = oldFile.getStringValueWithBailey(xpath);
            } else {
                p.oldValue = null;
            }
            Set<String> v = ballotBox.getValues(xpath);
            if (v != null) {
                for (String avalue : v) {
                    if (DEBUG)
                        System.err.println(" //val='" + avalue + "' vs " + value + " in " + xpath);
                    if (!avalue.equals(value)) {
                        CandidateItem item2 = p.addItem(avalue);
                        if (avalue != null && (checkCldr != null)/*
                                                                 * &&(
                                                                 * altProposed
                                                                 * == null)
                                                                 */) {
                            List<CheckStatus> item2Result = new ArrayList<CheckStatus>();
                            checkCldr.check(xpath, item2Result, avalue);
                            // checkCldr.getExamples(xpath, isExtraPath ?
                            // null : value, examplesResult);
                            if (!item2Result.isEmpty()) {
                                item2.setTests(item2Result);
                            }
                        }
                    }
                }
            }
            if (p.oldValue != null && !p.oldValue.equals(value) && (v == null || !v.contains(p.oldValue))) {
                // if "oldValue" isn't already represented as an item, add it.
                CandidateItem oldItem = p.addItem(p.oldValue);
                oldItem.isOldValue = true;
            }
            if ((locale.getCountry() != null && locale.getCountry().length() > 0) && (v == null || !v.contains(CldrUtility.INHERITANCE_MARKER))) {
                // if "vote for inherited" isn't already represented as an item, add it (child locales only)
                p.addItem(CldrUtility.INHERITANCE_MARKER);
            }

            p.coverageValue = coverageValue;

            p.confirmOnly = confirmOnly;

            if (isExtraPath) {
                // This is an 'extra' item- it doesn't exist in xml (including root).
                // Set up 'shim' tests, to display coverage
                p.setShimTests(base_xpath, this.sm.xpt.getById(base_xpath), checkCldr, options);
                // System.err.println("Shimmed! "+xpath);
            } else if (p.inheritedValue == null) {
                // This item fell back from root. Make sure it has an Item, and that tests are run.
                p.updateInheritedValue(ourSrc, checkCldr, options);
            }

            if (TRACE_TIME)
                System.err.println("n05  " + (System.currentTimeMillis() - nextTime));

            if ((p.getDisplayName() == null)) {
                canName = false; // disable 'view by name' if not all have
                // names.
            }
            if (TRACE_TIME)
                System.err.println("n06  " + (System.currentTimeMillis() - nextTime));

            // If it is draft and not proposed.. make it proposed-draft
            if (((eDraft != null) && (!eDraft.equals("false"))) && (altProposed == null)) {
                altProposed = SurveyMain.PROPOSED_DRAFT;
            }

            if (TRACE_TIME)
                System.err.println("n06a  " + (System.currentTimeMillis() - nextTime));

            CLDRFile.Status sourceLocaleStatus = new CLDRFile.Status();
            if (TRACE_TIME)
                System.err.println("n06b  " + (System.currentTimeMillis() - nextTime));
            String sourceLocale = aFile.getSourceLocaleID(xpath, sourceLocaleStatus);
            if (TRACE_TIME)
                System.err.println("n06c  " + (System.currentTimeMillis() - nextTime));
            boolean isInherited = !(sourceLocale.equals(locale.toString()));
            if (TRACE_TIME)
                System.err.println("n06d  " + (System.currentTimeMillis() - nextTime));

            // ** IF it is inherited, do NOT add any Items.
            if (isInherited && !isExtraPath) {
                if (TRACE_TIME)
                    System.err.println("n06da  [src:" + sourceLocale + " vs " + locale + ", sttus:" + sourceLocaleStatus + "] "
                        + (System.currentTimeMillis() - nextTime));
                //if (!NOINHERIT)
                //    p.updateInheritedValue(vettedParent, checkCldr, options); // update
                // the
                // tests
                if (TRACE_TIME)
                    System.err.println("n06dc  " + (System.currentTimeMillis() - nextTime));
                continue;
            }
            if (TRACE_TIME)
                System.err.println("n06e  " + (System.currentTimeMillis() - nextTime));

            if (TRACE_TIME)
                System.err.println("n07  " + (System.currentTimeMillis() - nextTime));

            // ?? simplify this.
            if (altProposed == null) {
                if (!isInherited) {
                    // superP.hasInherited=false;
                    // p.hasInherited=false;
                } else {
                    p.hasInherited = true;
                }
            } else {
                if (!isInherited) {
                    p.hasProps = true;
                    if (altProposed != SurveyMain.PROPOSED_DRAFT) { // 'draft=true'
                        p.hasMultipleProposals = true;
                    }
                } else {
                    // inherited, proposed
                    // p.hasProps = true; // Don't mark as a proposal.
                    // superP.hasProps = true;
                    p.hasInherited = true;
                }
            }

            CLDRLocale setInheritFrom = (isInherited) ? CLDRLocale.getInstance(sourceLocale) : null;

            // if (isExtraPath) { // No real data items if it's an extra
            // path.
            // System.err.println("ExtraPath: "+xpath);
            // continue;
            // }

            // ***** Set up Candidate Items *****
            // These are the items users may choose between
            //
            if ((checkCldr != null)/* &&(altProposed == null) */) {
                if (TRACE_TIME)
                    System.err.println("n07.1  (check) " + (System.currentTimeMillis() - nextTime));
                checkCldr.check(xpath, checkCldrResult, isExtraPath ? null : value);
                if (TRACE_TIME)
                    System.err.println("n07.2  (check) " + (System.currentTimeMillis() - nextTime));
                checkCldr.getExamples(xpath, isExtraPath ? null : value, examplesResult);
            }

            if (value != null && value.length() > 0) {
                DataSection.DataRow.CandidateItem myItem = null;

                if (TRACE_TIME)
                    System.err.println("n08  (check) " + (System.currentTimeMillis() - nextTime));
                myItem = p.addItem(value);

                if (DEBUG) {
                    System.err.println("Added item " + value + " - now items=" + p.items.size());
                }
                // if("gsw".equals(type)) System.err.println(myItem + " - # " +
                // p.items.size());

                if (!checkCldrResult.isEmpty()) {
                    myItem.setTests(checkCldrResult);
                    // set the parent
                    checkCldrResult = new ArrayList<CheckStatus>(); // can't
                    // reuse it
                    // if
                    // nonempty
                }

                if (sourceLocaleStatus != null && sourceLocaleStatus.pathWhereFound != null
                    && !sourceLocaleStatus.pathWhereFound.equals(xpath)) {
                    // System.err.println("PWF diff: " + xpath + " vs " +
                    // sourceLocaleStatus.pathWhereFound);
                    myItem.pathWhereFound = sourceLocaleStatus.pathWhereFound;
                    // set up Pod alias-ness
                    p.aliasFromLocale = sourceLocale;
                    p.aliasFromXpath = sm.xpt.xpathToBaseXpathId(sourceLocaleStatus.pathWhereFound);
                }
                myItem.inheritFrom = setInheritFrom;
                if (setInheritFrom == null) {
                    myItem.isFallback = false;
                    myItem.isParentFallback = false;
                }
                // store who voted for what. [ this could be loaded at
                // displaytime..]
                // myItem.votes = sm.vet.gatherVotes(locale, xpath);

                if (!examplesResult.isEmpty()) {
                    // reuse the same ArrayList unless it contains something
                    if (myItem.examples == null) {
                        myItem.examples = new Vector<ExampleEntry>();
                    }
                    for (Iterator<CheckStatus> it3 = examplesResult.iterator(); it3.hasNext();) {
                        CheckCLDR.CheckStatus status = it3.next();
                        myItem.examples.add(addExampleEntry(new ExampleEntry(this, p, myItem, status)));
                    }
                    // myItem.examplesList = examplesResult;
                    // examplesResult = new ArrayList(); // getExamples will
                    // clear it.
                }

                // if ((eRefs != null) && (!isInherited)) {
                // myItem.references = eRefs;
                // }
            }
        }
        // aFile.close();
    }

    void showSection(WebContext ctx, boolean canModify, String only_prefix_xpath, boolean zoomedIn) {
        showSection(ctx, canModify, -1, only_prefix_xpath, zoomedIn);
    }

    /**
     *
     * Call this function to show a DataSection to the user. Caller must hold
     * session sync.
     *
     * @param ctx
     *            the context to show
     * @param canModify
     *            user is allowed to modify
     * @param only_base_xpath
     *            TODO
     * @param only_prefix_xpath
     *            only show this prefix
     * @param zoomedIn
     *            show in zoomed-in mode
     * @param only_base_path
     *            only show this base xpath
     *
     * @deprecated use a custom XPathmatcher
     */
    @Deprecated
    void showSection(WebContext ctx, boolean canModify, int only_base_xpath, String only_prefix_xpath, boolean zoomedIn) {
        showSection(ctx, canModify, BaseAndPrefixMatcher.getInstance(only_base_xpath, only_prefix_xpath), zoomedIn);
    }

    /**
     * Show a single item, in a very limited view.
     *
     * @param ctx
     * @param item_xpath
     *            xpath of the one item to show
     */
    public void showPeasShort(WebContext ctx, int item_xpath) {
        DataRow row = getDataRow(item_xpath);
        if (row != null) {
            row.showDataRowShort(ctx);
        } else {
            // if(this.isUnofficial) {
            ctx.println("<tr><td colspan='2'>" + ctx.iconHtml("stop", "internal error")
                + "<i>internal error: nothing to show for xpath " + item_xpath + "," + " " + sm.xpt.getById(item_xpath)
                + "</i></td></tr>");
            // }
        }
    }

    void showSection(WebContext ctx, boolean canModify, XPathMatcher matcher, boolean zoomedIn) {
        int skip = 0; // where the index points to
        int oskip = ctx.fieldInt("skip", 0); // original skip from user.

        // boolean partialPeas = (matcher != null); // true if we are filtering
        // the section
        boolean partialPeas = false; // TODO: always nonpartial
        UserLocaleStuff uf = null;
        synchronized (ctx.session) {
            uf = ctx.getUserFile();

            CheckCLDR checkCldr = uf.getCheck(ctx.getEffectiveCoverageLevel(ctx.getLocale().toString()), ctx.getOptionsMap());

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
            if ((xfind != -1) && !partialPeas) {
                // see if we can find this base_xpath somewhere..
                int pn = 0;
                for (int i = 0; i < dSet.rows.length && (moveSkip == -1); i++) {
                    DataRow p = dSet.rows[i];
                    if (p.getXpathId() == xfind) {
                        moveSkip = pn;
                    }
                    pn++;
                }
                if (moveSkip != -1) {
                    oskip = (moveSkip / ctx.prefCodesPerPage()) * ctx.prefCodesPerPage(); // make
                    // it
                    // fall
                    // on
                    // a
                    // page
                    // boundary
                }
            }
            // -----
            if (!partialPeas && !(matcher != null && matcher.getXPath() != XPathTable.NO_XPATH)) {
                skip = dSet.showSkipBox(ctx, oskip);
            } else {
                skip = 0;
            }

            if (!partialPeas) {
                ctx.printUrlAsHiddenFields();
            }

            if (disputedOnly == true) {
                ctx.println("(<b>Disputed Only</b>)<br><input type='hidden' name='only' value='disputed'>");
            }

            if (!partialPeas) {
                ctx.println("<input type='hidden' name='skip' value='" + ctx.field("skip") + "'>");
                DataSection.printSectionTableOpen(ctx, this, zoomedIn, canModify);
            }

            int rowStart = skip; // where should it start?
            int rowCount = ctx.prefCodesPerPage(); // hwo many to show?

            if (disputedOnly) {
                // we want to go from
                // VETTING_PROBLEMS_LIST[0]..VETTING_PROBLEMS_LIST[n] in range.
                for (int j = 0; j < dSet.partitions.length; j++) {
                    for (String part : DataSection.VETTING_PROBLEMS_LIST) {
                        if (dSet.partitions[j].name.equals(part)) {
                            if (rowStart != skip) { // set once
                                rowStart = dSet.partitions[j].start;
                            }
                            rowCount = (dSet.partitions[j].limit - rowStart); // keep
                            // setting
                            // this.
                        }
                    }
                }
            }

            int rowEnd = rowStart + rowCount;
            if (rowEnd > dSet.rows.length) {
                rowEnd = dSet.rows.length;
            }
            // if(partialPeas) { ??? }
            for (int i = rowStart; i < rowEnd; i++) {
                // for(ListIterator i =
                // dSet.rows.listIterator(peaStart);(partialPeas||(count<peaCount))&&i.hasNext();count++)
                // {
                DataRow p = dSet.rows[i];

                if ((!partialPeas) && checkPartitions) {
                    for (int j = 0; j < dSet.partitions.length; j++) {
                        if ((dSet.partitions[j].name != null)
                            && ((i == dSet.partitions[j].start) || ((i == rowStart) && (i >= dSet.partitions[j].start) && (i < dSet.partitions[j].limit)))) { // ensure
                            // the
                            // first
                            // item
                            // has
                            // a
                            // header.
                            ctx.print("<tr class='heading'><th class='partsection' align='left' colspan='"
                                + SurveyMain.PODTABLE_WIDTH + "'>" + "<a name='" + dSet.partitions[j].name + "'");
                            if (!dSet.partitions[j].helptext.isEmpty()) {
                                ctx.print("title='" + dSet.partitions[j].helptext + "'");
                            }
                            ctx.println(">" + dSet.partitions[j].name + "</a>" + "</th>");
                            // if(isUnofficial) {
                            // ctx.println("<td>Partition #"+j+": "+dSet.partitions[j].start+"-"+dSet.partitions[j].limit+"</td>");
                            // }
                            ctx.println("</tr>");
                        }
                    }
                }

                try {
                    p.showDataRow(ctx, uf, canModify, checkCldr, zoomedIn, kAllRows);

                } catch (Throwable t) {
                    // failed to show pea.
                    ctx.println("<tr class='topbar'><td colspan='8'><b>" + xpath(p) + "</b><br>");
                    SurveyLog.logException(t, ctx);
                    ctx.print(t);
                    ctx.print("</td></tr>");
                }
                // if(p.subRows != null) {
                // for(Iterator e = p.subRows.values().iterator();e.hasNext();)
                // {
                // DataSection.DataRow subDataRow =
                // (DataSection.DataRow)e.next();
                // try {
                // showDataRow(ctx, section, subDataRow, uf, cf, ourSrc,
                // canModify, null,refs,checkCldr, zoomedIn);
                // } catch(Throwable t) {
                // // failed to show sub-pea.
                // ctx.println("<tr class='topbar'><td colspan='8'>sub pea: <b>"+section.xpath(subDataRow)+"."+subDataRow.altType+"</b><br>");
                // ctx.print(t);
                // ctx.print("</td></tr>");
                // }
                // }
                // }
            }
            if (!partialPeas) {
                sm.printSectionTableClose(ctx, this, canModify);

                if (!(matcher != null && matcher.getXPath() != XPathTable.NO_XPATH)) {
                    /* skip = */dSet.showSkipBox(ctx, oskip);
                }

                if (!canModify) {
                    ctx.println("<hr> <i>You are not authorized to make changes to this locale.</i>");
                }
            }
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
        if (ctx.field("only").equals("disputed")) { // are we in
            // 'disputed-only' mode?
            sortModeName = SurveyMain.PREF_SORTMODE_WARNING; // so that
            // disputed
            // shows up
            // on top-
            // force the
            // sortmode.
        }
        SortMode sortMode = SortMode.getInstance(sortModeName);
        return sortMode;
    }

    @Override
    public String toString() {
        return "{" + getClass().getSimpleName() + " " + locale + ":" + xpathPrefix + " #" + super.toString() + ", "
            + getAll().size() + " items, pageid " + this.pageId + " } ";
    }

    public void touch() {
        touchTime = System.currentTimeMillis();
    }

    /**
     * @deprecated use p.xpath()
     * @param p
     * @see DataSection#xpath
     */
    @Deprecated
    public String xpath(DataRow p) {
        return p.getXpath();
    }

    /**
     * @deprecated use getLocale
     * @return
     * @see #getLocale
     */
    public CLDRLocale locale() {
        return locale;
    }

    /** width, in columns, of the typical data table **/

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
            itemColSpan = 2; // When zoomed in, Proposed and Other takes up 2
            // columns
        } else {
            itemColSpan = 1;
        }
        if (/* !zoomedIn */true) {
            ctx.println("<tr><td colspan='" + table_width + "'>");
            // dataitems_header.jspf
            // some context
            ctx.put(WebContext.DATA_SECTION, section);
            ctx.put(WebContext.ZOOMED_IN, new Boolean(zoomedIn));
            ctx.includeFragment("dataitems_header.jsp");
            ctx.println("</td></tr>");
        }
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

        // if(zoomedIn) {
        // List<String> refsList = new ArrayList<String>();
        // ctx.temporaryStuff.put("references", refsList);
        // }
    }

    public static String getValueHash(String str) {
        // return Long.toHexString(StringId.getId(str));
        return (str == null) ? "null" : CookieSession.cheapEncodeString(str);
    }

    public static String fromValueHash(String s) {
        return (s == null || s.equals("null")) ? null : CookieSession.cheapDecodeString(s);
    }

    enum EPrintCellSet {
        doShowValue, doShowRef, doShowExample
    };

    private static final EnumSet<EPrintCellSet> kValueCells = EnumSet.of(EPrintCellSet.doShowValue, EPrintCellSet.doShowRef);
    private static final EnumSet<EPrintCellSet> kExampleCells = EnumSet.of(EPrintCellSet.doShowExample);

    enum EShowDataRowSet {
        doShowTR, doShowValueRows, doShowOtherRows, doShowVettingRows
    };

    private static final EnumSet<EShowDataRowSet> kAllRows = EnumSet.allOf(EShowDataRowSet.class);
    public static final EnumSet<EShowDataRowSet> kAjaxRows = EnumSet.of(EShowDataRowSet.doShowValueRows);

    @Override
    public String toJSONString() throws JSONException {
        JSONObject itemList = new JSONObject();
        JSONObject result = new JSONObject();
        try {
//            for (Map.Entry<String, DataRow> e : rowsHash.entrySet()) {
//                itemList.put(e.getValue().fieldHash(), e.getValue());
//            }
            for (DataRow d : rowsHash.values()) {
                try {
                    String str = d.toJSONString();
                    JSONObject obj = new JSONObject(str);
                    itemList.put(d.fieldHash(), obj);
                } catch (JSONException ex) {
                    SurveyLog.logException(ex, "JSON serialization error for row: " + d.xpath + " : Full row is: " + d.toString());
                    throw new JSONException(ex);
                }
            }
            // String x = itemList.toString();
            // System.out.println("rows: " + x);
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
     * @return the processor
     */
    private DisplayAndInputProcessor getProcessor() {
        if (processor == null) {
            processor = new DisplayAndInputProcessor(SurveyMain.BASELINE_LOCALE, false);
        }
        return processor;
    }

    /**
     * @return the processor
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
