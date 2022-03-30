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
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.web.BallotBox;
import org.unicode.cldr.web.BallotBox.VoteNotAcceptedException;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.Dashboard;
import org.unicode.cldr.web.DataSection;
import org.unicode.cldr.web.DataSection.DataRow;
import org.unicode.cldr.web.DataSection.DataRow.CandidateItem;
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

import com.ibm.icu.util.Output;

/**
 * Note: The functions in this class needed to be separated from VoteAPI because of static init problems.
 */
public class VoteAPIHelper {
    public static final class VoteEntry {
        public Object userid;
        public int votes;
        public Integer override = null;

        public VoteEntry(int id, int votes, Integer override) {
            this.userid = id;
            this.votes = votes;
            this.override = override;
        }
    }

    static final boolean DEBUG = false;
    static final Logger logger = SurveyLog.forClass(VoteAPIHelper.class);

    private static class ArgsForGet {
        String localeId;
        String sessionId;
        String page = null;
        String xpath = null;
        Boolean getDashboard = false;

        public ArgsForGet(String loc, String session) {
            this.localeId = loc;
            this.sessionId = session;
        }
    }

    static Response handleGetOneRow(String loc, String session, String xpath, Boolean getDashboard) {
        ArgsForGet args = new ArgsForGet(loc, session);
        args.xpath = xpath;
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

            if (args.xpath == null && args.page != null) {
                try {
                    pageId = PageId.valueOf(args.page);
                } catch (IllegalArgumentException iae) {
                    return Response.status(404).entity(new STError(ErrorCode.E_BAD_SECTION)).build();
                }
                if (pageId != null && pageId.getSectionId() == org.unicode.cldr.util.PathHeader.SectionId.Special) {
                    return new STError(ErrorCode.E_SPECIAL_SECTION, "Items not visible - page " + pageId + " section " + pageId.getSectionId()).build();
                }
                r.pageId = pageId.name();
            } else if (args.xpath != null && args.page == null) {
                xp = sm.xpt.getByStringID(args.xpath);
                if (xp == null) {
                    return Response.status(404).entity(new STError(ErrorCode.E_BAD_XPATH)).build();
                }
                matcher = XPathMatcher.getMatcherForString(xp); // single string
            } else {
                // Should not get here.
                return new STError(ErrorCode.E_INTERNAL, "handleGetRows: need xpath or page, but not both").build();
            }
            final DataSection pageData = DataSection.make(pageId, null, mySession, locale, xp, matcher);
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
                r.issues = new Dashboard().getErrorOnPath(locale, null /* WebContext */, mySession, args.xpath);
            }
            return Response.ok(r).build();
        } catch (Throwable t) {
            SurveyLog.logException(logger, t, "Trying to load " + args.localeId + " / " + args.xpath);
            return new STError(t).build(); // 500
        }
    }

    private static RowResponse.Row[] calculateRows(Collection<DataRow> all) {
        List<RowResponse.Row> list = new LinkedList<>();
        for (final DataRow r : all) {
            list.add(calculateRow(r));
        }
        return list.toArray(new RowResponse.Row[list.size()]);
    }

    private static RowResponse.Row calculateRow(final DataRow r) {
        RowResponse.Row row = new RowResponse.Row();
        // from DataSection.DataRow.toJSONString()

        // TODO: ourVote
        row.xpath = XPathTable.getStringIDString(r.getXpath());

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

        row.items = calculateItems(r, row);
        return row;
    }

    private static Candidate[] calculateItems(final DataRow r, RowResponse.Row row) {
        // Add candidate items
        List<RowResponse.Row.Candidate> items = new LinkedList<>();
        for (final CandidateItem i : r.items.values()) {
            RowResponse.Row.Candidate c = calculateItem(i);
            items.add(c);
        }
        return items.toArray(new RowResponse.Row.Candidate[items.size()]);
    }

    private static RowResponse.Row.Candidate calculateItem(final CandidateItem i) {
        // from DataSection.DataRow.CandidateItem.toJSONString()
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
            entries.add(new VoteEntry(u.id, u.getLevel().getVotes(), override));
        }
        return entries.toArray(new VoteEntry[entries.size()]);
    }

    static Response handleVote(String loc, String xpath, VoteRequest request, final CookieSession mySession) {
        VoteResponse r = new VoteResponse();

        mySession.userDidAction();

        CLDRLocale locale = CLDRLocale.getInstance(loc);
        if (!UserRegistry.userCanModifyLocale(mySession.user, locale)) {
            return Response.status(Status.FORBIDDEN).build();
        }
        final SurveyMain sm = CookieSession.sm;
        final String xp = sm.xpt.getByStringID(xpath);
        if (xp == null) {
            return Response.status(Status.NOT_FOUND).build(); // no XPath found
        }
        CheckCLDR.Options options = DataSection.getOptions(null, mySession, locale); // TODO: why null for WebContext here?
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
                // Create a DataSection for this single XPath.
                DataSection section = DataSection.make(null, null, mySession, locale, xp, null);
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
            try (SurveyMain.UserLocaleStuff uf = sm.getUserFile(mySession, locale);) {
                final CLDRFile file = uf.cldrfile;
                final String checkval = val;
                if (CldrUtility.INHERITANCE_MARKER.equals(val)) {
                    final Output<String> localeWhereFound = new Output<>();
                    /*
                     * TODO: this looks dubious, see https://unicode.org/cldr/trac/ticket/11299
                     * temporarily for debugging, don't change checkval, but do call
                     * getBaileyValue in order to get localeWhereFound
                     */
                    // checkval = file.getBaileyValue(xp, null, localeWhereFound);
                    file.getBaileyValue(xp, null, localeWhereFound);
                }
                cc.check(xp, result, checkval);
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
