package org.unicode.cldr.web.api;

import java.io.PrintWriter;
import java.util.*;
import java.util.logging.Logger;
import javax.json.bind.spi.JsonbProvider;
import javax.ws.rs.core.Response;
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
 * Note: The functions in this class needed to be separated from VoteAPI because of static init
 * problems.
 */
public class VoteAPIHelper {
    private static final boolean DEBUG_SERIALIZATION = false;

    public static final class VoteEntry {
        public final Integer overridedVotes;
        public final String userid;
        public final int votes;

        // TODO: CLDR-16829 remove these fields, and all 'redacted' parameters in this file.
        public final String email;
        public final VoteResolver.Level level;
        public final String name;
        public final String org;

        public VoteEntry(User u, Integer override, boolean redacted) {
            this.level = u.getLevel();
            this.org = u.getOrganization().toString();
            this.overridedVotes = override;
            this.userid = Integer.toString(u.id);
            this.votes = u.getVoteCount();
            if (!redacted) {
                this.email = u.email.replace("@", " (at) ");
                this.name = u.name;
            } else {
                this.email = "(hidden)";
                this.name = "User#" + userid;
            }
        }
    }

    static final Logger logger = SurveyLog.forClass(VoteAPIHelper.class);

    public static class ArgsForGet {
        String localeId;
        String sessionId;
        String page = null;
        public String xpstrid = null;
        Boolean getDashboard = false;

        public ArgsForGet(String loc, String session) {
            this.localeId = loc;
            this.sessionId = session;
        }
    }

    static Response handleGetOneRow(
            String loc, String session, String xpstrid, Boolean getDashboard) {
        ArgsForGet args = new ArgsForGet(loc, session);
        args.xpstrid = xpstrid;
        args.getDashboard = getDashboard;
        return handleGetRows(args);
    }

    static Response handleGetOnePage(String loc, String session, String page, String xpstrid) {
        ArgsForGet args = new ArgsForGet(loc, session);
        if ("auto".equals(page) && xpstrid != null && !xpstrid.isEmpty()) {
            args.page = getPageFromXpathStringId(xpstrid);
        } else {
            args.page = page;
        }
        return handleGetRows(args);
    }

    private static String getPageFromXpathStringId(String xpstrid) {
        try {
            String xpath = CookieSession.sm.xpt.getByStringID(xpstrid);
            if (xpath != null) {
                PathHeader ph = CookieSession.sm.getSTFactory().getPathHeader(xpath);
                if (ph != null) {
                    return ph.getPageId().name();
                }
            }
        } catch (Throwable t) {
            return null;
        }
        return null;
    }

    private static Response handleGetRows(ArgsForGet args) {
        final SurveyMain sm = CookieSession.sm;
        final CLDRLocale locale = CLDRLocale.getInstance(args.localeId);
        final CookieSession mySession = Auth.getSession(args.sessionId);
        if (mySession == null) {
            return Auth.noSessionResponse();
        }
        try {
            /** if true, hide emails. TODO: CLDR-16829 remove this parameter */
            final boolean redacted =
                    ((mySession.user == null) || (!mySession.user.getLevel().isGuest()));
            final RowResponse r = getRowsResponse(args, sm, locale, mySession, redacted);
            return Response.ok(r).build();
        } catch (Throwable t) {
            t.printStackTrace();
            if (!(t instanceof SurveyException
                    && ((SurveyException) t).getErrCode() != ErrorCode.E_INTERNAL)) {
                // log unknown or internal errs
                SurveyLog.logException(
                        logger, t, "Trying to load " + args.localeId + " / " + args.xpstrid);
            }
            return new STError(t).build();
        }
    }

    public static RowResponse getRowsResponse(
            ArgsForGet args,
            final SurveyMain sm,
            final CLDRLocale locale,
            final CookieSession mySession,
            final boolean redacted)
            throws SurveyException {
        final RowResponse r = new RowResponse();
        XPathMatcher matcher = null;
        PageId pageId = null;
        String xp = null;

        if (args.xpstrid == null && args.page != null) {
            try {
                pageId = PageId.valueOf(args.page);
            } catch (IllegalArgumentException iae) {
                throw new SurveyException(ErrorCode.E_BAD_SECTION);
            }
            if (pageId.getSectionId() == org.unicode.cldr.util.PathHeader.SectionId.Special) {
                throw new SurveyException(
                        ErrorCode.E_SPECIAL_SECTION,
                        "Items not visible - page " + pageId + " section " + pageId.getSectionId());
            }
            r.pageId = pageId.name();
        } else if (args.xpstrid != null && args.page == null) {
            xp = sm.xpt.getByStringID(args.xpstrid);
            if (xp == null) {
                throw new SurveyException(ErrorCode.E_BAD_XPATH);
            }
            matcher = XPathMatcher.exactMatcherForString(xp);
        } else {
            // Should not get here. but could be a 'not acceptable'
            throw new SurveyException(
                    ErrorCode.E_INTERNAL, // or E_BAD_XPATH?
                    "handleGetRows: need xpstrid or page, but not both");
        }
        final DataPage pageData = DataPage.make(pageId, mySession, locale, xp, matcher, null);
        pageData.setUserForVotelist(mySession.user);
        r.page = new RowResponse.Page();
        if (args.xpstrid != null) {
            r.setOneRowPath(args.xpstrid);
        }

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
            r.page.rows = makePageRows(dataRows, redacted);
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
        return r;
    }

    private static void debugSerialization(RowResponse r, ArgsForGet args) {
        try {
            JsonbProvider.provider().create().build().toJson(r, new PrintWriter(System.out));
        } catch (Throwable t) {
            t.printStackTrace();
            SurveyLog.logException(
                    logger, t, "DEBUG_SERIALIZATION: " + args.localeId + " / " + args.xpstrid);
        }
    }

    private static RowResponse.DisplaySets makeDisplaySets(Collection<DataRow> dataRows) {
        final RowResponse.DisplaySets displaySets = new RowResponse.DisplaySets();
        final SortMode sortMode = new PathHeaderSort();
        displaySets.ph = sortMode.createDisplaySet(null, dataRows);
        return displaySets;
    }

    private static Map<String, RowResponse.Row> makePageRows(
            Collection<DataRow> all, boolean redacted) {
        final Map<String, RowResponse.Row> rows = new HashMap<>();
        for (final DataRow r : all) {
            rows.put(r.fieldHash(), calculateRow(r, redacted));
        }
        return rows;
    }

    private static RowResponse.Row calculateRow(final DataRow r, boolean redacted) {
        final RowResponse.Row row = new RowResponse.Row();
        final VoteResolver<String> resolver = r.getResolver();
        final String xpath = r.getXpath();
        final PatternPlaceholders placeholders = PatternPlaceholders.getInstance();

        row.canFlagOnLosing = resolver.canFlagOnLosing();
        row.confirmStatus = r.getConfirmStatus();
        row.coverageValue = r.getCoverageValue();
        row.code = r.getCode();
        row.dir = r.getDirectionality(); // should not be needed, but there are some override
        // situations.
        row.displayExample = r.getDisplayExample();
        row.displayName = r.getDisplayName();
        row.rawEnglish = r.getRawEnglish();
        row.extraAttributes = r.getNonDistinguishingAttributes();
        row.flagged = r.isFlagged();
        row.hasVoted = r.userHasVoted();
        row.helpHtml = r.getHelpHTML();
        row.inheritedLocale = r.getInheritedLocaleName();
        row.inheritedValue = r.getInheritedValue();
        row.inheritedDisplayValue = r.getInheritedDisplayValue();
        row.inheritedXpid = r.getInheritedXPath();
        row.items = calculateItems(r, redacted);
        row.placeholderInfo = placeholders.get(xpath);
        row.placeholderStatus = placeholders.getStatus(xpath);
        row.rdf = r.getRDFURI();
        row.rowFlagged = r.isFlagged();
        row.statusAction = r.getStatusAction();
        row.translationHint = r.getTranslationHint();
        row.voteVhash = r.getVoteVHash();
        row.votingResults = getVotingResults(resolver);
        row.winningValue = r.getWinningValue();
        row.winningVhash = r.getWinningVHash();
        row.voteTranscript = r.getVoteTranscript();
        row.xpath = xpath;
        row.xpathId = CookieSession.sm.xpt.getByXpath(xpath);
        row.xpstrid = XPathTable.getStringIDString(xpath);
        row.fixedCandidates = r.fixedCandidates();
        return row;
    }

    /**
     * Convert the given back-end VoteResolver into a front-end VotingResults
     *
     * @param resolver the back-end VoteResolver
     * @return the VotingResults
     */
    public static <T> RowResponse.Row.VotingResults<T> getVotingResults(VoteResolver<T> resolver) {
        final RowResponse.Row.VotingResults<T> results = new RowResponse.Row.VotingResults<>();
        final EnumSet<Organization> conflictedOrgs = resolver.getConflictedOrganizations();
        /** array of Key, Value, Key, Value… */
        final List<Object> valueToVoteA = new ArrayList<>();
        final Map<T, Long> valueToVote = resolver.getResolvedVoteCountsIncludingIntraOrgDisputes();
        for (Map.Entry<T, Long> e : valueToVote.entrySet()) {
            valueToVoteA.add(e.getKey());
            valueToVoteA.add(String.valueOf(e.getValue()));
        }
        results.nameTime = resolver.getNameTime();
        results.requiredVotes = resolver.getRequiredVotes();
        results.value_vote = valueToVoteA.toArray(new Object[0]);
        results.valueIsLocked = resolver.isValueLocked();
        results.orgs = new HashMap<>();
        for (Organization o : Organization.values()) {
            final T orgVote = resolver.getOrgVote(o);
            if (orgVote != null) {
                final RowResponse.Row.OrgValueVotes<T> org = new RowResponse.Row.OrgValueVotes<>();
                org.conflicted = conflictedOrgs.contains(o);
                org.orgVote = orgVote;
                org.status = resolver.getStatusForOrganization(o).name();
                org.votes = resolver.getOrgToVotes(o);
                results.orgs.put(o.name(), org);
            }
        }
        return results;
    }

    private static Map<String, Candidate> calculateItems(final DataRow r, boolean redacted) {
        final Map<String, Candidate> items = new HashMap<>();
        for (final CandidateItem i : r.items.values()) {
            items.put(i.getValueHash(), calculateItem(i, redacted));
        }
        return items;
    }

    private static RowResponse.Row.Candidate calculateItem(
            final CandidateItem i, boolean redacted) {
        RowResponse.Row.Candidate c = new RowResponse.Row.Candidate();
        c.example = i.getExample();
        c.history = i.getHistory();
        c.isBaselineValue = i.isBaselineValue();
        c.pClass =
                i.getPClass(); // it might be better to pass underlying values (not CSS class) to FE
        c.rawValue = i.getValue();
        c.tests = getConvertedTests(i.getTests());
        c.value = i.getProcessedValue();
        c.valueHash = i.getValueHash();
        c.votes = calculateVotes(i.getVotes(), i.getOverrides(), redacted);
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

    private static Map<String, VoteEntry> calculateVotes(
            Set<User> users, Map<User, Integer> overrides, boolean redacted) {
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
            VoteEntry voteEntry = new VoteEntry(u, override, redacted);
            votes.put(voteEntry.userid, voteEntry);
        }
        return votes;
    }

    private static Dashboard.ReviewNotification[] getOnePathDash(
            String baseXp, CLDRLocale locale, CookieSession mySession) {
        Level coverageLevel = Level.get(mySession.getEffectiveCoverageLevel(locale.toString()));
        Dashboard.ReviewOutput ro =
                new Dashboard().get(locale, mySession.user, coverageLevel, baseXp);
        return ro.getNotifications();
    }

    // forbiddenIsOk is true when called from VoteAPI for the main, original usage of this method;
    // when true, it means
    // that even if we get statusAction.isForbidden, we'll return an OK (200) response, with
    // json.didVote false.
    //
    // forbiddenIsOk is false when called from XPathAlt for the special purpose of adding a new
    // path, so that
    // XPathAlt will get something other than OK (200) in case of failure.
    //
    // This method needs refactoring into smaller subroutines, with the lower-level details
    // separated from
    // the HTTP response concerns, so that VoteAPI and XPathAlt can share code without the
    // awkwardness of forbiddenIsOk.
    public static Response handleVote(
            String loc,
            String xp,
            String value,
            int voteLevelChanged,
            final CookieSession mySession,
            boolean forbiddenIsOk) {
        // translate this call into jax-rs Response
        try {
            final VoteResponse r =
                    getHandleVoteResponse(
                            loc, xp, value, voteLevelChanged, mySession, forbiddenIsOk);
            return Response.ok(r).build();
        } catch (Throwable se) {
            return new STError(se).build();
        }
    }
    /**
     * this function is the implementation of handleVote() but does not use any jax-rs, for unit
     * tests
     */
    public static VoteResponse getHandleVoteResponse(
            String loc,
            String xp,
            String value,
            int voteLevelChanged,
            final CookieSession mySession,
            boolean forbiddenIsOk)
            throws SurveyException {
        VoteResponse r = new VoteResponse();
        mySession.userDidAction();
        CLDRLocale locale = CLDRLocale.getInstance(loc);
        if (!UserRegistry.userCanModifyLocale(mySession.user, locale)) {
            throw new SurveyException(ErrorCode.E_NO_PERMISSION, "Not allowed to modify " + locale);
        }
        loc = locale.getBaseName(); // sanitized
        final SurveyMain sm = CookieSession.sm;
        CheckCLDR.Options options = DataPage.getSimpleOptions(locale);
        final STFactory stf = sm.getSTFactory();
        synchronized (mySession) {
            try {
                final String origValue = value;
                final Exception[] exceptionList = new Exception[1];
                final CLDRFile cldrFile = stf.make(loc, true, true);
                final String val = processValue(locale, xp, exceptionList, origValue, cldrFile);
                final List<CheckStatus> result = new ArrayList<>();
                if (val == null && origValue != null && !origValue.isEmpty()) {
                    normalizedToZeroLengthError(r, result);
                }
                final TestResultBundle cc = stf.getTestResult(locale, options);
                runTests(cc, xp, val, result);
                addDaipException(loc, xp, result, exceptionList, val, origValue);
                r.setTestResults(result);
                // Create a DataPage for this single XPath.
                DataPage page = DataPage.make(null, mySession, locale, xp, null, cc);
                page.setUserForVotelist(mySession.user);
                DataRow dataRow = page.getDataRow(xp);
                // First, calculate the status for showing (unless already set by
                // normalizedToZeroLengthError)
                if (r.statusAction == null) {
                    r.statusAction = calculateShowRowAction(cldrFile, xp, val, dataRow);
                }

                if (!r.statusAction.isForbidden()) {
                    CandidateInfo ci = calculateCandidateItem(result, val, dataRow);
                    // Now, recalculate the statusAction for accepting the new item
                    r.statusAction =
                            CLDRConfig.getInstance()
                                    .getPhase()
                                    .getAcceptNewItemAction(
                                            ci,
                                            dataRow,
                                            CheckCLDR.InputMethod.DIRECT,
                                            stf.getPathHeader(xp),
                                            mySession.user);
                    if (!r.statusAction.isForbidden()) {
                        try {
                            final BallotBox<UserRegistry.User> ballotBox =
                                    stf.ballotBoxForLocale(locale);
                            Integer withVote = (voteLevelChanged == 0) ? null : voteLevelChanged;
                            ballotBox.voteForValueWithType(
                                    mySession.user, xp, val, withVote, VoteType.DIRECT);
                            r.didVote = true;
                        } catch (VoteNotAcceptedException e) {
                            if (e.getErrCode() == ErrorCode.E_PERMANENT_VOTE_NO_FORUM) {
                                r.statusAction =
                                        CheckCLDR.StatusAction.FORBID_PERMANENT_WITHOUT_FORUM;
                            } else {
                                throw (e);
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                SurveyLog.logException(logger, t, "Processing submission " + locale + ":" + xp);
                throw new SurveyException(
                        ErrorCode.E_INTERNAL, "Processing submission " + locale + ":" + xp);
            }
        }
        if (!forbiddenIsOk && r.statusAction.isForbidden()) {
            throw new SurveyException(
                    ErrorCode.E_VOTE_NOT_ACCEPTED, "Status action is forbidden: " + r.statusAction);
        }
        return r;
    }

    private static void normalizedToZeroLengthError(VoteResponse r, List<CheckStatus> result) {
        final String message = "DAIP returned a 0 length string";
        r.didNotSubmit = message;
        r.statusAction = CheckCLDR.StatusAction.FORBID_ERRORS;
        String[] list = {message};
        result.add(
                new CheckStatus()
                        .setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.internalError)
                        .setCause(
                                new CheckCLDR() {
                                    @Override
                                    public CheckCLDR handleCheck(
                                            String path,
                                            String fullPath,
                                            String value,
                                            Options options,
                                            List<CheckStatus> result) {
                                        return null;
                                    }
                                })
                        .setMessage("Input Processor Error: {0}")
                        .setParameters(list));
    }

    private static void runTests(
            TestResultBundle cc, String xp, String val, final List<CheckStatus> result) {
        if (val != null) {
            cc.check(xp, result, val);
        }
    }

    private static String processValue(
            CLDRLocale locale,
            String xp,
            Exception[] exceptionList,
            final String origValue,
            CLDRFile cldrFile) {
        String val;
        if (origValue != null && !origValue.isEmpty()) {
            final DisplayAndInputProcessor daip = new DisplayAndInputProcessor(locale, true);
            if (VoteResolver.DROP_HARD_INHERITANCE) {
                daip.enableInheritanceReplacement(cldrFile);
            }
            val = daip.processInput(xp, origValue, exceptionList);
            if (val.isEmpty()
                    && DtdData.getValueConstraint(xp) == DtdData.Element.ValueConstraint.nonempty) {
                val = null; // the caller will recognize this as exceptional, not Abstain
            }
        } else {
            val = null;
        }
        return val;
    }

    private static CandidateInfo calculateCandidateItem(
            final List<CheckStatus> result, final String candVal, DataRow dataRow) {
        CandidateInfo ci;
        if (candVal == null) {
            ci = null; // abstention
        } else {
            ci = dataRow.getItem(candVal); // existing item?
            if (ci == null) { // no, new item
                ci =
                        new CandidateInfo() {
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

    private static CheckCLDR.StatusAction calculateShowRowAction(
            CLDRFile cldrFile, String xp, String val, DataRow dataRow) {
        CheckCLDR.StatusAction showRowAction = dataRow.getStatusAction();
        if (CldrUtility.INHERITANCE_MARKER.equals(val)) {
            if (dataRow.wouldInheritNull()) {
                showRowAction = CheckCLDR.StatusAction.FORBID_NULL;
            } else if (CheckForCopy.sameAsCode(val, xp, cldrFile.getUnresolved(), cldrFile)) {
                showRowAction = CheckCLDR.StatusAction.FORBID_CODE;
            }
        } else if (dataRow.isUnvotableRoot(val)) {
            showRowAction = CheckCLDR.StatusAction.FORBID_ROOT;
        }
        return showRowAction;
    }

    private static void addDaipException(
            String loc,
            String xp,
            final List<CheckStatus> result,
            Exception[] exceptionList,
            String val,
            String origValue) {
        if (exceptionList[0] != null) {
            result.add(
                    new CheckStatus()
                            .setMainType(CheckStatus.errorType)
                            .setSubtype(Subtype.internalError)
                            .setCause(
                                    new CheckCLDR() {

                                        @Override
                                        public CheckCLDR handleCheck(
                                                String path,
                                                String fullPath,
                                                String value,
                                                Options options,
                                                List<CheckStatus> result) {
                                            return null;
                                        }
                                    })
                            .setMessage("Input Processor Exception: {0}")
                            .setParameters(exceptionList));
            SurveyLog.logException(
                    logger,
                    exceptionList[0],
                    "DAIP, Processing "
                            + loc
                            + ":"
                            + xp
                            + "='"
                            + val
                            + "' (was '"
                            + origValue
                            + "')");
        }
    }
}
