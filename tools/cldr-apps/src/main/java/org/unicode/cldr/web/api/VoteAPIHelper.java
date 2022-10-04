package org.unicode.cldr.web.api;

import java.io.PrintWriter;
import java.util.*;
import java.util.logging.Logger;

import javax.json.bind.spi.JsonbProvider;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.test.CheckForCopy;
import org.unicode.cldr.test.DisplayAndInputProcessor;
import org.unicode.cldr.test.TestCache.TestResultBundle;
import org.unicode.cldr.util.*;
import org.unicode.cldr.util.CLDRInfo.CandidateInfo;
import org.unicode.cldr.util.CLDRInfo.UserInfo;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.web.*;
import org.unicode.cldr.web.BallotBox.VoteNotAcceptedException;
import org.unicode.cldr.web.DataPage.DataRow;
import org.unicode.cldr.web.DataPage.DataRow.CandidateItem;
import org.unicode.cldr.web.SurveyException.ErrorCode;
import org.unicode.cldr.web.UserRegistry.User;
import org.unicode.cldr.web.api.VoteAPI.RowResponse;
import org.unicode.cldr.web.api.VoteAPI.RowResponse.Row.Candidate;
import org.unicode.cldr.web.api.VoteAPI.VoteResponse;

/**
 * Note: The functions in this class needed to be separated from VoteAPI because of static init problems.
 */
public class VoteAPIHelper {
    private static final boolean DEBUG_SERIALIZATION = false;

    public static final class VoteEntry {
        public final String email;
        public final VoteResolver.Level level;
        public final String name;
        public final String org;
        public final Integer overridedVotes;
        public final String userid;
        public final int votes;

        public VoteEntry(User u, Integer override) {
            this.email = u.email.replace("@", " (at) ");
            this.level = u.getLevel();
            this.org = u.getOrganization().toString();
            this.overridedVotes = override;
            this.name = u.name;
            this.userid = Integer.toString(u.id);
            this.votes = u.getVoteCount();
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
            r.page = new RowResponse.Page();

            // don't return default content
            CLDRLocale dcParent = SupplementalDataInfo.getInstance().getBaseFromDefaultContent(locale);
            if (dcParent != null) {
                r.dcParent = dcParent.getBaseName();
                r.page.nocontent = true;
            } else {
                r.canModify = UserRegistry.userCanModifyLocale(mySession.user, locale);
                r.localeDisplayName = locale.getDisplayName();
                r.page.nocontent = false;
                Collection<DataRow> dataRows = pageData.getAll();
                r.page.rows = makePageRows(dataRows);
                if (args.page != null) {
                    r.displaySets = makeDisplaySets(dataRows);
                }
            }
            if (args.getDashboard) {
                r.notifications = getOnePathDash(XPathTable.xpathToBaseXpath(xp), locale, mySession);
            }
            if (DEBUG_SERIALIZATION) {
                debugSerialization(r, args);
            }
            return Response.ok(r).build();
        } catch (Throwable t) {
            t.printStackTrace();
            SurveyLog.logException(logger, t, "Trying to load " + args.localeId + " / " + args.xpstrid);
            return new STError(t).build(); // 500
        }
    }

    private static void debugSerialization(RowResponse r, ArgsForGet args) {
        try {
            JsonbProvider.provider().create().build().toJson(r, new PrintWriter(System.out));
        } catch (Throwable t) {
            t.printStackTrace();
            SurveyLog.logException(logger, t, "DEBUG_SERIALIZATION: " + args.localeId + " / " + args.xpstrid);
        }
    }

    private static RowResponse.DisplaySets makeDisplaySets(Collection<DataRow> dataRows) {
        final RowResponse.DisplaySets displaySets = new RowResponse.DisplaySets();
        final SortMode sortMode = new PathHeaderSort();
        displaySets.ph = sortMode.createDisplaySet(null, dataRows);
        return displaySets;
    }

    private static Map<String, RowResponse.Row> makePageRows(Collection<DataRow> all) {
        final Map<String, RowResponse.Row> rows = new HashMap<>();
        for (final DataRow r : all) {
            rows.put(r.fieldHash(), calculateRow(r));
        }
        return rows;
    }

    private static RowResponse.Row calculateRow(final DataRow r) {
        final RowResponse.Row row = new RowResponse.Row();
        final VoteResolver<String> resolver = r.getResolver();
        final String xpath = r.getXpath();
        final PatternPlaceholders placeholders = PatternPlaceholders.getInstance();

        row.canFlagOnLosing = resolver.canFlagOnLosing();
        row.confirmStatus = r.getConfirmStatus();
        row.coverageValue = r.getCoverageValue();
        row.code = r.getCode();
        row.dir = r.getDirectionality(); // should not be needed, but there are some override situations.
        row.displayExample = r.getDisplayExample();
        row.displayName = r.getDisplayName();
        row.extraAttributes = r.getNonDistinguishingAttributes();
        row.flagged = r.isFlagged();
        row.hasVoted = r.userHasVoted();
        row.helpHtml = r.getHelpHTML();
        row.inheritedLocale = r.getInheritedLocaleName();
        row.inheritedValue = r.getInheritedValue();
        row.inheritedXpid = r.getInheritedXPath();
        row.items = calculateItems(r);
        row.placeholderInfo = placeholders.get(xpath);
        row.placeholderStatus = placeholders.getStatus(xpath);
        row.rdf = r.getRDFURI();
        row.rowFlagged = r.isFlagged();
        row.statusAction = r.getStatusAction();
        row.voteVhash = r.getVoteVHash();
        row.votingResults = getVotingResults(resolver);
        row.winningValue = r.getWinningValue();
        row.winningVhash = r.getWinningVHash();
        row.xpath = xpath;
        row.xpathId = CookieSession.sm.xpt.getByXpath(xpath);
        row.xpstrid = XPathTable.getStringIDString(xpath);
        return row;
    }

    /**
     * Convert the given back-end VoteResolver into a front-end VotingResults
     *
     * @param resolver the back-end VoteResolver
     * @return the VotingResults
     */
    private static RowResponse.Row.VotingResults getVotingResults(VoteResolver<String> resolver) {
        final RowResponse.Row.VotingResults results = new RowResponse.Row.VotingResults();
        final EnumSet<Organization> conflictedOrgs = resolver.getConflictedOrganizations();
        final List<String> valueToVoteA = new ArrayList<>();
        final Map<String, Long> valueToVote = resolver.getResolvedVoteCounts();
        for (Map.Entry<String, Long> e : valueToVote.entrySet()) {
            valueToVoteA.add(e.getKey());
            valueToVoteA.add(String.valueOf(e.getValue()));
        }
        results.nameTime = resolver.getNameTime();
        results.requiredVotes = resolver.getRequiredVotes();
        results.value_vote = valueToVoteA.toArray(new String[0]);
        results.valueIsLocked = resolver.isValueLocked();
        results.orgs = new HashMap<>();
        for (Organization o : Organization.values()) {
            final String orgVote = resolver.getOrgVote(o);
            if (orgVote != null) {
                final RowResponse.Row.OrgValueVotes org = new RowResponse.Row.OrgValueVotes();
                org.conflicted = conflictedOrgs.contains(o);
                org.orgVote = orgVote;
                org.status = resolver.getStatusForOrganization(o).name();
                org.votes = resolver.getOrgToVotes(o);
                results.orgs.put(o.name(), org);
            }
        }
        return results;
    }

    private static Map<String, Candidate> calculateItems(final DataRow r) {
        final Map<String, Candidate> items = new HashMap<>();
        for (final CandidateItem i : r.items.values()) {
            items.put(i.getValueHash(), calculateItem(i));
        }
        return items;
    }

    private static RowResponse.Row.Candidate calculateItem(final CandidateItem i) {
        RowResponse.Row.Candidate c = new RowResponse.Row.Candidate();
        c.example = i.getExample();
        c.history = i.getHistory();
        c.isBaselineValue = i.isBaselineValue();
        c.pClass = i.getPClass(); // it might be better to pass underlying values (not CSS class) to FE
        c.rawValue = i.getValue();
        c.tests = getConvertedTests(i.getTests());
        c.value = i.getProcessedValue();
        c.valueHash = i.getValueHash();
        c.votes = calculateVotes(i.getVotes(), i.getOverrides());
        return c;
    }

    private static List<VoteAPI.CheckStatusSummary> getConvertedTests(List<CheckStatus> tests) {
        List<VoteAPI.CheckStatusSummary> newTests = new ArrayList<>();
        if (tests != null) {
            for (CheckStatus test : tests) {
                newTests.add(new VoteAPI.CheckStatusSummary(test));
            }
        }
        return newTests;
    }

    private static Map<String, VoteEntry> calculateVotes(Set<User> users, Map<User, Integer> overrides) {
        if (users == null) {
            return null;
        }
        Map<String, VoteEntry> votes = new HashMap<>(users.size());
        for (final User u : users) {
            if (UserRegistry.userIsLocked(u)) {
                continue;
            }
            Integer override = null;
            if (overrides != null) {
                override = overrides.get(u);
            }
            VoteEntry voteEntry = new VoteEntry(u, override);
            votes.put(voteEntry.userid, voteEntry);
        }
        return votes;
    }

    private static Dashboard.ReviewNotification[] getOnePathDash(String baseXp, CLDRLocale locale, CookieSession mySession) {
        Level coverageLevel = Level.get(mySession.getEffectiveCoverageLevel(locale.toString()));
        Dashboard.ReviewOutput ro = new Dashboard().get(locale, mySession.user, coverageLevel, baseXp);
        return ro.getNotifications();
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
                // NOTE: tests may be run twice, since we call both runTests and DataPage.make!
                // -- If so, this should be corrected before we actually use this code!
                DataPage page = DataPage.make(null, null, mySession, locale, xp, null);
                page.setUserForVotelist(mySession.user);
                DataRow pvi = page.getDataRow(xp);
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
