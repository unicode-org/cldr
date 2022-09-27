package org.unicode.cldr.web.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.test.CheckForCopy;
import org.unicode.cldr.test.DisplayAndInputProcessor;
import org.unicode.cldr.test.TestCache.TestResultBundle;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRInfo.CandidateInfo;
import org.unicode.cldr.util.CLDRInfo.UserInfo;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.PatternPlaceholders;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.web.BallotBox;
import org.unicode.cldr.web.BallotBox.VoteNotAcceptedException;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.DataPage;
import org.unicode.cldr.web.DataPage.DataRow;
import org.unicode.cldr.web.DataPage.DataRow.CandidateItem;
import org.unicode.cldr.web.STFactory;
import org.unicode.cldr.web.SurveyException.ErrorCode;
import org.unicode.cldr.web.SurveyLog;
import org.unicode.cldr.web.SurveyMain;
import org.unicode.cldr.web.UserRegistry;
import org.unicode.cldr.web.UserRegistry.User;
import org.unicode.cldr.web.XPathMatcher;
import org.unicode.cldr.web.XPathTable;
import org.unicode.cldr.web.api.VoteAPI.RowResponse;
import org.unicode.cldr.web.api.VoteAPI.RowResponse.Row.Candidate;
import org.unicode.cldr.web.api.VoteAPI.VoteResponse;

/**
 * Note: The functions in this class needed to be separated from VoteAPI because of static init problems.
 *
 * As of 2022-04-07, this class hasn't been used yet, except for testing in conjunction
 * with CldrRows.vue and CldrRow.vue. This is intended to be a modernized replacement
 * for SurveyAjax.getRow (WHAT_GETROW). It isn't ready to be used since it doesn't produce
 * json compatible with the front end. References:
 * https://unicode-org.atlassian.net/browse/CLDR-15368
 * https://unicode-org.atlassian.net/browse/CLDR-15403
 */
public class VoteAPIHelper {
    public static final class VoteEntry {
        public Object userid;
        public int votes;
        public Integer override;

        public VoteEntry(int id, int votes, Integer override) {
            this.userid = id;
            this.votes = votes;
            this.override = override;
        }
    }

    static final Logger logger = SurveyLog.forClass(VoteAPIHelper.class);

    private static class ArgsForGet {
        String localeId;
        String sessionId;
        String page = null;
        String xpstrid = null;
        Boolean getDashboard = false;

        public ArgsForGet(String loc, String session) {
            this.localeId = loc;
            this.sessionId = session;
        }
    }

    static Response handleGetOneRow(String loc, String session, String xpstrid, Boolean getDashboard) {
        ArgsForGet args = new ArgsForGet(loc, session);
        args.xpstrid = xpstrid;
        args.getDashboard = getDashboard;
        return handleGetRows(args);
    }

    static Response handleGetOnePage(String loc, String session, String page) {
        ArgsForGet args = new ArgsForGet(loc, session);
        args.page = page;
        return handleGetRows(args);
    }

    private static Response handleGetRows(ArgsForGet args) {
        final SurveyMain sm = CookieSession.sm;
        final CLDRLocale locale = CLDRLocale.getInstance(args.localeId);
        final CookieSession mySession = Auth.getSession(args.sessionId);
        if (mySession == null) {
            return Auth.noSessionResponse();
        }
        try {
            final RowResponse r = new RowResponse();
            XPathMatcher matcher = null;
            PageId pageId = null;
            String xp = null;

            if (args.xpstrid == null && args.page != null) {
                try {
                    pageId = PageId.valueOf(args.page);
                } catch (IllegalArgumentException iae) {
                    return Response.status(404).entity(new STError(ErrorCode.E_BAD_SECTION)).build();
                }
                if (pageId.getSectionId() == org.unicode.cldr.util.PathHeader.SectionId.Special) {
                    return new STError(ErrorCode.E_SPECIAL_SECTION, "Items not visible - page " + pageId + " section " + pageId.getSectionId()).build();
                }
                r.pageId = pageId.name();
            } else if (args.xpstrid != null && args.page == null) {
                xp = sm.xpt.getByStringID(args.xpstrid);
                if (xp == null) {
                    return Response.status(404).entity(new STError(ErrorCode.E_BAD_XPATH)).build();
                }
                matcher = XPathMatcher.getMatcherForString(xp); // single string
            } else {
                // Should not get here.
                return new STError(ErrorCode.E_INTERNAL, "handleGetRows: need xpstrid or page, but not both").build();
            }
            final DataPage pageData = DataPage.make(pageId, null, mySession, locale, xp, matcher);
            pageData.setUserForVotelist(mySession.user);

            // don't return default content
            CLDRLocale dcParent = SupplementalDataInfo.getInstance().getBaseFromDefaultContent(locale);
            if (dcParent != null) {
                r.dcParent = dcParent.getBaseName();
            } else {
                r.isReadOnly = STFactory.isReadOnlyLocale(locale);
                r.localeDisplayName = locale.getDisplayName();
                r.rows = calculateRows(pageData.getAll());
            }
            if (args.getDashboard) {
                /*
                 * TODO: implement single-path dashboard in this modern-api context
                 * See existing implementation in SurveyAjax.java, Dashboard.java
                 * Reference: https://unicode-org.atlassian.net/browse/CLDR-15368
                 */
                return new STError(ErrorCode.E_INTERNAL, "handleGetRows: single-path dashboard not implemented yet").build();
            }
            return Response.ok(r).build();
        } catch (Throwable t) {
            t.printStackTrace();
            SurveyLog.logException(logger, t, "Trying to load " + args.localeId + " / " + args.xpstrid);
            return new STError(t).build(); // 500
        }
    }

    private static RowResponse.Row[] calculateRows(Collection<DataRow> all) {
        List<RowResponse.Row> list = new LinkedList<>();
        for (final DataRow r : all) {
            list.add(calculateRow(r));
        }
        return list.toArray(new RowResponse.Row[0]);
    }

    private static RowResponse.Row calculateRow(final DataRow r) {
        RowResponse.Row row = new RowResponse.Row();
        // from DataPage.DataRow.toJSONString()

        // TODO: ourVote
        final String xpath = r.getXpath();
        row.xpstrid = XPathTable.getStringIDString(xpath);

        row.code = r.getCode();
        row.resolver = r.getResolver();
        row.dir = r.getDirectionality(); // TODO: should not be needed, but there are some override situations.
        row.displayExample = r.getDisplayExample();
        row.displayName = r.getDisplayName();
        row.extraAttributes = r.getNonDistinguishingAttributes();
        row.hasVoted = r.userHasVoted();
        row.inheritedLocale = r.getInheritedLocaleName();
        // NB: "winningValue" is in the resolver info.
        row.inheritedXpath = r.getInheritedXPath();
        row.flagged = r.isFlagged();
        row.statusAction = r.getStatusAction();
        row.voteResolver = r.getResolver();
        row.helpHtml = r.getHelpHTML();
        row.rdf = r.getRDFURI();

        row.items = calculateItems(r);
        PatternPlaceholders placeholders = PatternPlaceholders.getInstance();
        row.placeholderStatus = placeholders.getStatus(xpath);
        row.placeholderInfo = placeholders.get(xpath);
        return row;
    }

    private static Candidate[] calculateItems(final DataRow r) {
        // Add candidate items
        List<RowResponse.Row.Candidate> items = new LinkedList<>();
        for (final CandidateItem i : r.items.values()) {
            RowResponse.Row.Candidate c = calculateItem(i);
            items.add(c);
        }
        return items.toArray(new Candidate[0]);
    }

    private static RowResponse.Row.Candidate calculateItem(final CandidateItem i) {
        // from DataPage.DataRow.CandidateItem.toJSONString()
        // Ideally this would go into RowResponse.Row.Candidate but can't due to
        // static initialization problems.
        RowResponse.Row.Candidate c = new RowResponse.Row.Candidate();
        c.value = i.getValue();
        c.displayValue = i.getProcessedValue();
        c.example = i.getExample();
        c.isBaselineValue = i.isBaselineValue();
        // TODO: pass underlying values to FE. Don't pass CSS class.
        //    c.pClass = i.getPClass();
        // TODO: c.tests = i.tests
        c.votes = calculateVotes(i.getVotes(), i.getOverrides());
        return c;
    }

    private static VoteEntry[] calculateVotes(Set<User> votes, Map<User, Integer> overrides) {
        if (votes == null) return new VoteEntry[0];
        List<VoteEntry> entries = new ArrayList<>(votes.size());
        for (final User u : votes) {
            if (UserRegistry.userIsLocked(u)) {
                continue;
            }
            Integer override = null;
            if (overrides != null) {
                override = overrides.get(u);
            }
            entries.add(new VoteEntry(u.id, u.getVoteCount(), override));
        }
        return entries.toArray(new VoteEntry[0]);
    }

    static Response handleVote(String loc, String xpstrid, VoteRequest request, final CookieSession mySession) {
        VoteResponse r = new VoteResponse();

        mySession.userDidAction();

        CLDRLocale locale = CLDRLocale.getInstance(loc);
        if (!UserRegistry.userCanModifyLocale(mySession.user, locale)) {
            return Response.status(Status.FORBIDDEN).build();
        }
        final SurveyMain sm = CookieSession.sm;
        final String xp = sm.xpt.getByStringID(xpstrid);
        if (xp == null) {
            return Response.status(Status.NOT_FOUND).build(); // no XPath found
        }
        CheckCLDR.Options options = DataPage.getOptions(null, mySession, locale); // TODO: why null for WebContext here?
        final STFactory stf = sm.getSTFactory();
        synchronized (mySession) {
            try {
                final String origValue = request.value;
                final Exception[] exceptionList = new Exception[1];
                final String val = processValue(locale, xp, exceptionList, origValue);
                final List<CheckStatus> result = new ArrayList<>();
                final TestResultBundle cc = stf.getTestResult(locale, options);
                runTests(mySession, r, locale, sm, cc, xp, val, result);
                addDaipException(loc, xp, result, exceptionList, val, origValue);
                r.setResults(result);
                r.testsRun = cc.toString();
                // Create a DataPage for this single XPath.
                DataPage section = DataPage.make(null, null, mySession, locale, xp, null);
                section.setUserForVotelist(mySession.user);
                DataRow pvi = section.getDataRow(xp);
                // First, calculate the status for showing
                r.statusAction = calculateShowRowAction(locale, stf, xp, val, pvi);

                if (!r.statusAction.isForbidden()) {
                    CandidateInfo ci = calculateCandidateItem(result, val, pvi);
                    // Now, recalculate the statusACtion for accepting the new item
                    r.statusAction = CLDRConfig.getInstance().getPhase()
                        .getAcceptNewItemAction(ci, pvi, CheckCLDR.InputMethod.DIRECT,
                            stf.getPathHeader(xp), mySession.user);
                    if (!r.statusAction.isForbidden()) {
                        try {
                            final BallotBox<UserRegistry.User> ballotBox = stf.ballotBoxForLocale(locale);
                            if (request.voteLevelChanged == 0) { // treat 0 as null
                                request.voteLevelChanged = null;
                            }
                            ballotBox.voteForValue(mySession.user, xp, val, request.voteLevelChanged);
                            r.submitResult = ballotBox.getResolver(xp);
                            r.didVote = true;
                        } catch (VoteNotAcceptedException e) {
                            if (e.getErrCode() == ErrorCode.E_PERMANENT_VOTE_NO_FORUM) {
                                r.statusAction = CheckCLDR.StatusAction.FORBID_PERMANENT_WITHOUT_FORUM;
                            } else {
                                throw (e);
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                SurveyLog.logException(logger, t, "Processing submission " + locale + ":" + xp);
                return (new STError(t).build());
            }
        }

        return Response.ok(r).build();
    }

    private static void runTests(final CookieSession mySession, VoteResponse r, CLDRLocale locale, final SurveyMain sm, TestResultBundle cc, String xp,
        String val, final List<CheckStatus> result) {
        if (val != null) {
            try (SurveyMain.UserLocaleStuff uf = sm.getUserFile(mySession, locale)) {
                final CLDRFile file = uf.cldrfile;
                cc.check(xp, result, val);
                r.dataEmpty = file.isEmpty();
            }
        }
    }

    private static String processValue(CLDRLocale locale, String xp, Exception[] exceptionList, final String origValue) {
        String val;
        if (origValue != null) {
            final DisplayAndInputProcessor daip = new DisplayAndInputProcessor(locale, true);
            val = daip.processInput(xp, origValue, exceptionList);
        } else {
            val = null;
        }
        return val;
    }

    private static CandidateInfo calculateCandidateItem(final List<CheckStatus> result, final String candVal, DataRow pvi) {
        CandidateInfo ci;
        if (candVal == null) {
            ci = null; // abstention
        } else {
            ci = pvi.getItem(candVal); // existing item?
            if (ci == null) { // no, new item
                ci = new CandidateInfo() {
                    @Override
                    public String getValue() {
                        return candVal;
                    }

                    @Override
                    public Collection<UserInfo> getUsersVotingOn() {
                        return Collections.emptyList(); // No users voting - yet.
                    }

                    @Override
                    public List<CheckStatus> getCheckStatusList() {
                        return result;
                    }
                };
            }
        }
        return ci;
    }

    private static CheckCLDR.StatusAction calculateShowRowAction(CLDRLocale locale, STFactory stf, String xp, String val, DataRow pvi) {
        CheckCLDR.StatusAction showRowAction = pvi.getStatusAction();
        if (CldrUtility.INHERITANCE_MARKER.equals(val)) {
            if (pvi.wouldInheritNull()) {
                showRowAction = CheckCLDR.StatusAction.FORBID_NULL;
            } else {
                // TODO: expensive, the caller probably should provide a CLDRFile.
                CLDRFile cldrFile = stf.make(locale.getBaseName(), true, true);
                if (CheckForCopy.sameAsCode(val, xp, cldrFile.getUnresolved(), cldrFile)) {
                    showRowAction = CheckCLDR.StatusAction.FORBID_CODE;
                }
            }
        } else if (pvi.isUnvotableRoot(val)) {
            showRowAction = CheckCLDR.StatusAction.FORBID_ROOT;
        }
        return showRowAction;
    }

    private static void addDaipException(String loc, String xp, final List<CheckStatus> result, Exception[] exceptionList, String val, String origValue) {
        if (exceptionList[0] != null) {
            result.add(new CheckStatus().setMainType(CheckStatus.errorType)
                .setSubtype(Subtype.internalError)
                .setCause(new CheckCLDR() {

                    @Override
                    public CheckCLDR handleCheck(String path, String fullPath, String value, Options options,
                        List<CheckStatus> result) {
                        return null;
                    }
                })
                .setMessage("Input Processor Exception: {0}")
                .setParameters(exceptionList));
            SurveyLog.logException(logger, exceptionList[0], "DAIP, Processing " + loc + ":" + xp + "='" + val
                + "' (was '" + origValue + "')");
        }
    }
}
