package org.unicode.cldr.web.api;

import java.io.*;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.tool.Chart;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.util.VoterReportStatus;
import org.unicode.cldr.util.VoterReportStatus.ReportAcceptability;
import org.unicode.cldr.util.VoterReportStatus.ReportId;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.DataPage;
import org.unicode.cldr.web.ReportsDB;
import org.unicode.cldr.web.ReportsDB.UserReport;
import org.unicode.cldr.web.STFactory;
import org.unicode.cldr.web.SubtypeToURLMap;
import org.unicode.cldr.web.SurveyAjax;
import org.unicode.cldr.web.SurveyMain;
import org.unicode.cldr.web.UserRegistry;

@Path("/voting/reports")
@Tag(name = "voting", description = "APIs for voting and retrieving vote and row data")
public class ReportAPI {
    @GET
    @Path("/users/{user}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get User Report Status",
            description = "Get one or all user’s report status")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Results of an All Reports Status request",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        type = SchemaType.ARRAY,
                                                        implementation = UserReport.class)))
            })
    public Response getAllReports(
            @HeaderParam(Auth.SESSION_HEADER) String session,
            @Parameter(
                            required = true,
                            example = "1",
                            schema =
                                    @Schema(
                                            type = SchemaType.STRING,
                                            description = "user ID or '-' for all"))
                    @PathParam("user")
                    String user)
            throws SQLException {
        final CookieSession mySession = Auth.getSession(session);
        if (mySession == null) {
            return Auth.noSessionResponse();
        }
        Integer id;
        if (user.equals("-")) {
            id = null;
            if (!UserRegistry.userIsManagerOrStronger(mySession.user)) {
                // only Manager+ can do this
                return Response.status(Status.FORBIDDEN).build();
            }
        } else {
            id = Integer.parseInt(user);
            UserRegistry.User u = mySession.sm.reg.getInfo(id);
            if (u == null) {
                return Response.status(Status.NOT_FOUND).build();
            } else if (!(mySession.user.id == id || mySession.user.isAdminFor(u))) {
                return Response.status(Status.FORBIDDEN).build();
            }
        }
        return Response.ok(ReportsDB.getInstance().getAllReports(id, null)).build();
    }

    @GET
    @Path("/users/{user}/locales/{locale}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get Report Status",
            description = "This handles a request for a specific user and locale report status")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Results of Report Status request",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                VoterReportStatus.ReportStatus
                                                                        .class))),
                @APIResponse(responseCode = "403", description = "Forbidden"),
            })
    public Response getReport(
            @Parameter(required = true, example = "1", schema = @Schema(type = SchemaType.INTEGER))
                    @PathParam("user")
                    Integer user,
            @Parameter(required = true, example = "mt", schema = @Schema(type = SchemaType.STRING))
                    @PathParam("locale")
                    String locale,
            @HeaderParam(Auth.SESSION_HEADER) String session) {
        final CookieSession mySession = Auth.getSession(session);
        if (mySession == null || mySession.user == null) {
            return Auth.noSessionResponse();
        }
        UserRegistry.User u = mySession.sm.reg.getInfo(user);
        if (u == null) {
            return Response.status(Status.NOT_FOUND).build();
        } else if (!(mySession.user.id == user || !mySession.user.isAdminFor(u))) {
            return Response.status(Status.FORBIDDEN).build();
        }

        return Response.ok()
                .entity(
                        ReportsDB.getInstance()
                                .getReportStatus(user, CLDRLocale.getInstance(locale)))
                .build();
    }

    @GET
    @Path("/locales/{locale}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List vetting results of one or more locales")
    @APIResponse(
            responseCode = "200",
            description = "list responses",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LocaleReportVettingResults.class)))
    public Response getReportLocaleStatus(
            @HeaderParam(Auth.SESSION_HEADER) String session,
            @Parameter(
                            required = true,
                            example = "en",
                            schema =
                                    @Schema(
                                            type = SchemaType.STRING,
                                            description = "Locale ID or '-' for all"))
                    @PathParam("locale")
                    String locale)
            throws SQLException {
        final CookieSession mySession = Auth.getSession(session);
        if (mySession == null) {
            return Auth.noSessionResponse();
        }
        Iterable<CLDRLocale> locales = null;
        CLDRLocale onlyLoc;
        if (locale.equals("-")) {
            locales = SurveyMain.getLocalesSet();
            onlyLoc = null;
        } else {
            onlyLoc = CLDRLocale.getInstance(locale);
            locales = Collections.singleton(onlyLoc);
        }
        LocaleReportVettingResults r = new LocaleReportVettingResults();
        // make a copy of the DB subset here, for performance.
        VoterReportStatus<Integer> db = ReportsDB.getInstance().clone(null, onlyLoc);
        VoteResolver<ReportAcceptability> res =
                new VoteResolver<>(CookieSession.sm.reg.getVoterInfoList()); // create

        // set of all valid userids
        final Set<Integer> allUsers = CookieSession.sm.reg.getVoterToInfo().keySet();
        for (final CLDRLocale loc : locales) {
            CheckCLDR.Phase phase = SurveyMain.checkCLDRPhase(loc);
            CheckCLDR.StatusAction showRowAction =
                    phase.getShowRowAction(
                            null /* not path based */,
                            CheckCLDR.InputMethod.DIRECT,
                            null /* Not path based */,
                            mySession.user);
            final boolean canModify = UserRegistry.userCanModifyLocale(mySession.user, loc);
            LocaleReportVettingResult rr = new LocaleReportVettingResult(showRowAction, canModify);
            rr.locale = loc.toString();
            for (final ReportId report : ReportId.getReportsAvailable()) {
                Map<Integer, ReportAcceptability> votes = new TreeMap<>();
                Map<ReportAcceptability, Set<Integer>> statistics =
                        db.updateResolver(loc, report, allUsers, res, votes);
                rr.reports.add(new ReportVettingResult(report, res, statistics, votes));
                statistics.values().forEach(s -> rr.addVoters(s));
            }
            r.locales.add(rr);
        }
        return Response.ok(r).build();
    }

    public static class LocaleReportVettingResults {
        public LocaleReportVettingResults() {}

        private Set<LocaleReportVettingResult> locales = new HashSet<LocaleReportVettingResult>();

        public LocaleReportVettingResult[] getLocales() {
            return locales.toArray(new LocaleReportVettingResult[0]);
        }
    }

    public static class LocaleReportVettingResult {
        @Schema(description = "True if user is allowed to vote for this report.")
        public final boolean canVote;

        public String locale;
        private Set<ReportVettingResult> reports = new HashSet<ReportVettingResult>();

        public ReportVettingResult[] getReports() {
            return reports.toArray(new ReportVettingResult[0]);
        }

        private Set<Integer> allUsers = new HashSet<Integer>();

        void addVoters(Set<Integer> s) {
            allUsers.addAll(s);
        }

        @Schema(description = "Total voters for this locale. Does not count abstentions.")
        public int getTotalVoters() {
            return allUsers.size();
        }

        public LocaleReportVettingResult(CheckCLDR.StatusAction action, boolean canModify) {
            canVote = canModify && action.canVote();
        }
    }

    public static class ReportVettingResult {
        public ReportVettingResult(
                ReportId id,
                VoteResolver<ReportAcceptability> res,
                Map<ReportAcceptability, Set<Integer>> statistics,
                Map<Integer, ReportAcceptability> votes) {
            this.report = id;
            this.status = res.getWinningStatus();
            if (this.status != VoteResolver.Status.missing) {
                this.acceptability = res.getWinningValue();
            } else {
                this.acceptability = null;
            }

            // Statistics has a map from each value to voter ids.
            // For now we just keep the totals
            final Set<Integer> vfa = statistics.get(ReportAcceptability.acceptable);
            if (vfa != null) {
                votersForAcceptable = vfa.size();
            } else {
                votersForAcceptable = 0;
            }
            final Set<Integer> vfna = statistics.get(ReportAcceptability.notAcceptable);
            if (vfna != null) {
                votersForNotAcceptable = vfna.size();
            } else {
                votersForNotAcceptable = 0;
            }
            Map<ReportAcceptability, Long> rvc =
                    res.getResolvedVoteCountsIncludingIntraOrgDisputes();
            acceptableScore = rvc.get(ReportAcceptability.acceptable);
            notAcceptableScore = rvc.get(ReportAcceptability.notAcceptable);
            // serialize the voteResolver
            transcript = res.getTranscript();
            resolvedVoteCounts = res.getResolvedVoteCountsIncludingIntraOrgDisputes();
            votingResults = VoteAPIHelper.getVotingResults(res);
            this.votes = votes;
        }

        public ReportId report;
        public VoteResolver.Status status;
        public ReportAcceptability acceptability;
        public int votersForAcceptable;
        public int votersForNotAcceptable;
        public Long acceptableScore;
        public Long notAcceptableScore;
        public String transcript;
        public Map<ReportAcceptability, Long> resolvedVoteCounts;
        public VoteAPI.RowResponse.Row.VotingResults<ReportAcceptability> votingResults;
        public Map<Integer, ReportAcceptability> votes;
    }

    @POST
    @Path("/users/{user}/locales/{locale}/reports/{report}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Update Report Status",
            description =
                    "This updates a specific user’s report status. "
                            + "You can only update your own status.")
    @APIResponses(
            value = {
                @APIResponse(responseCode = "204", description = "Updated OK"),
                @APIResponse(responseCode = "403", description = "Forbidden"),
                @APIResponse(responseCode = "401", description = "Unauthorized")
            })
    public Response updateReport(
            @Parameter(required = true, example = "1", schema = @Schema(type = SchemaType.INTEGER))
                    @PathParam("user")
                    Integer user,
            @Parameter(required = true, example = "mt", schema = @Schema(type = SchemaType.STRING))
                    @PathParam("locale")
                    String locale,
            // Note:  @Schema(implementation = ReportId.class) did not work here. The following
            // works.
            @Parameter(
                            required = true,
                            example = "compact",
                            schema = @Schema(type = SchemaType.STRING))
                    @PathParam("report")
                    ReportId report,
            @HeaderParam(Auth.SESSION_HEADER) String session,
            @Schema(description = "Two parameters ") ReportUpdate update) {
        final CookieSession mySession = Auth.getSession(session);
        if (mySession == null) {
            return Auth.noSessionResponse();
        }
        if (mySession.user == null || mySession.user.id != user) {
            return Response.status(Status.FORBIDDEN).build();
        }
        if (!report.isAvailable()) {
            return Response.status(Status.FORBIDDEN).build();
        }
        final CLDRLocale loc = CLDRLocale.getInstance(locale);
        // apply the same standard as for vetting.
        // First check whether they even have permission.
        if (!UserRegistry.userCanModifyLocale(mySession.user, loc)) {
            return Response.status(Status.FORBIDDEN).build();
        }
        CheckCLDR.StatusAction showRowAction =
                SurveyMain.checkCLDRPhase(loc)
                        .getShowRowAction(
                                null /* not path based */,
                                CheckCLDR.InputMethod.DIRECT,
                                null /* Not path based */,
                                mySession.user);

        if (!showRowAction.canVote()) {
            return Response.status(Status.FORBIDDEN).build();
        }

        ReportsDB.getInstance()
                .markReportComplete(user, loc, report, update.completed, update.acceptable);

        return Response.status(Status.NO_CONTENT).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List all report types")
    @APIResponse(
            responseCode = "200",
            description = "list responses",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema =
                                    @Schema(
                                            type = SchemaType.ARRAY,
                                            implementation = String.class)))
    public Response listReports() {
        return Response.ok().entity(ReportId.getReportsAvailable().toArray()).build();
    }

    @Schema(description = "update to user’s report status")
    public static final class ReportUpdate {
        public ReportUpdate() {}

        @Schema(
                description =
                        "True if user has completed evaluating this report, False if not complete. May not be !complete&&acceptable.")
        public boolean completed = false;

        @Schema(
                description =
                        "True if values were acceptable. False if values weren’t acceptable, but user has voted for correct ones.")
        public boolean acceptable = false;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/locales/{locale}/reports/{report}.html")
    @Operation(summary = "Get the report output")
    @APIResponse(
            responseCode = "200",
            description = "report HTML",
            content = @Content(mediaType = MediaType.TEXT_HTML))
    @APIResponse(responseCode = "404", description = "Locale not found")
    public Response getReportOutput(
            @Parameter(required = true, example = "mt", schema = @Schema(type = SchemaType.STRING))
                    @PathParam("locale")
                    String locale,
            // Note:  @Schema(implementation = ReportId.class) did not work here. The following
            // works.
            @Parameter(
                            required = true,
                            example = "compact",
                            schema = @Schema(type = SchemaType.STRING))
                    @PathParam("report")
                    ReportId report,
            @HeaderParam(Auth.SESSION_HEADER) String session) {
        final CookieSession mySession = Auth.getSession(session);
        // Here we just verify that there *is* a session
        if (mySession == null) {
            return Auth.noSessionResponse();
        }
        final CLDRLocale loc = CLDRLocale.getInstance(locale);
        if (!SurveyMain.getLocalesSet().contains(loc)) {
            // No such locale
            return Response.status(Status.NOT_FOUND).build();
        }
        if (!report.isAvailable()) {
            return Response.status(Status.FORBIDDEN).build();
        }
        final String result;
        try {
            result = writeReport(report, loc);
        } catch (IOException e) {
            return Response.status(500, "An exception occurred").entity(e).build();
        }
        return Response.ok(result).build();
    }

    private String writeReport(ReportId report, CLDRLocale loc) throws IOException {
        final Writer w = new StringWriter();
        final STFactory stf = CookieSession.sm.getSTFactory();
        final Chart chart = Chart.forReport(report, loc.getBaseName());
        if (chart != null) {
            chart.writeContents(
                    w,
                    stf,
                    stf.getTestResult(loc, DataPage.getSimpleOptions(loc)),
                    SubtypeToURLMap.getInstance());
        } else {
            switch (report) {
                    // "Old Three" reports
                case compact:
                case datetime:
                case zones:
                    // r_compact, r_datetime, r_zones
                    SurveyAjax.generateReport("r_" + report.name(), w, CookieSession.sm, loc);
                    break;
                default:
                    w.write("Could not load report: " + report.name());
            }
        }
        return w.toString();
    }
}
