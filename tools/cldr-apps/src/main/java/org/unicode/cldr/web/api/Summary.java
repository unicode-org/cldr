package org.unicode.cldr.web.api;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
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
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.json.JSONException;
import org.unicode.cldr.util.*;
import org.unicode.cldr.web.*;
import org.unicode.cldr.web.Dashboard.ReviewOutput;
import org.unicode.cldr.web.VettingViewerQueue.LoadingPolicy;

@ApplicationScoped
@Path("/summary")
@Tag(name = "voting", description = "APIs for voting")
public class Summary {

    /** When to start a daily automatic snapshot */
    private static final int AUTO_SNAP_HOUR_OF_DAY = 8;

    private static final int AUTO_SNAP_MINUTE_OF_HOUR = 0; // 8:00 am
    private static final int AUTO_SNAP_MINIMUM_START_MINUTES = 3;
    private static final String AUTO_SNAP_TIME_ZONE = "America/Los_Angeles";
    private static ScheduledFuture<?> autoSnapshotFuture = null;

    /**
     * While an automatic snapshot is in progress, which may take several minutes, make repeated
     * requests with this delay in between. The log may show the progress as a percentage. A few
     * times per minute is similar to the frequency of http requests for manual summaries, and may
     * be convenient for debugging.
     */
    private static final long AUTO_SNAP_SLEEP_SECONDS = 20;

    /** An automatic snapshot should not take this long; bail out if it does. */
    private static final long AUTO_SNAP_MAX_DURATION_MINUTES = 30;

    /** jsonb enables converting an object to a json string, used for creating snapshots */
    private static final Jsonb jsonb = JsonbBuilder.create();

    /**
     * For saving and retrieving "snapshots" of Summary responses
     *
     * <p>Note: for debugging/testing without using db, new SurveySnapshotDb() can be changed here
     * to new SurveySnapshotMap()
     */
    private static final SurveySnapshot snap = new SurveySnapshotDb();

    private static final Logger logger = SurveyLog.forClass(Summary.class);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get Priority Items Summary",
            description =
                    "Also known as Vetting Summary, this like a Dashboard for multiple locales.")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Results of Summary operation",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = SummaryResponse.class)))
            })
    public Response doVettingSummary(
            SummaryRequest request, @HeaderParam(Auth.SESSION_HEADER) String sessionString) {
        try {
            CookieSession cs = Auth.getSession(sessionString);
            if (cs == null) {
                return Auth.noSessionResponse();
            }
            if (!UserRegistry.userCanUseVettingSummary(cs.user)) {
                return Response.status(Status.FORBIDDEN).build();
            }
            if (SurveySnapshot.SNAP_CREATE.equals(request.snapshotPolicy)
                    && !UserRegistry.userCanCreateSummarySnapshot(cs.user)) {
                return Response.status(Status.FORBIDDEN).build();
            }
            return getPriorityItemsSummary(cs, request);
        } catch (JSONException | IOException ioe) {
            return Response.status(500, "An exception occurred").entity(ioe).build();
        }
    }

    /**
     * Get the response for Priority Items Summary (possibly a snapshot)
     *
     * <p>Each request specifies a loading policy: START, NOSTART, or FORCESTOP. Typically, the
     * client makes a request with START, then makes repeated requests with NOSTART while the
     * responses have status PROCESSING (or WAITING), until there is a response with status READY.
     * The response with status READY contains the actual requested Priority Items Summary data.
     *
     * <p>Each request specifies a snapshot policy: SNAP_NONE, SNAP_CREATE, or SNAP_SHOW.
     *
     * @param cs the CookieSession identifying the user
     * @param request the SummaryRequest
     * @return the Response
     * @throws IOException
     * @throws JSONException
     */
    private Response getPriorityItemsSummary(CookieSession cs, SummaryRequest request)
            throws IOException, JSONException {
        cs.userDidAction();
        if (SurveySnapshot.SNAP_SHOW.equals(request.snapshotPolicy)) {
            return showSnapshot(request.snapshotId);
        }
        Organization usersOrg = cs.user.vrOrg();
        VettingViewerQueue vvq = VettingViewerQueue.getInstance();
        vvq.setSummarizeAllLocales(request.summarizeAllLocales);
        QueueMemberId qmi = new QueueMemberId(cs);
        SummaryResponse sr = getSummaryResponse(vvq, qmi, usersOrg, request.loadingPolicy);
        if (SurveySnapshot.SNAP_CREATE.equals(request.snapshotPolicy)
                && sr.status == VettingViewerQueue.Status.READY) {
            saveSnapshot(sr);
        }
        return Response.ok(sr, MediaType.APPLICATION_JSON).build();
    }

    /**
     * Get the response for Priority Items Summary (new, not an already existing snapshot)
     *
     * @param vvq the VettingViewerQueue
     * @param qmi the QueueMemberId
     * @param usersOrg the user's organization
     * @param loadingPolicy the LoadingPolicy
     * @return the SummaryResponse
     * @throws IOException
     * @throws JSONException
     */
    private SummaryResponse getSummaryResponse(
            VettingViewerQueue vvq,
            QueueMemberId qmi,
            Organization usersOrg,
            LoadingPolicy loadingPolicy)
            throws IOException, JSONException {
        SummaryResponse sr = new SummaryResponse();
        VettingViewerQueue.Args args = vvq.new Args(qmi, usersOrg, loadingPolicy);
        VettingViewerQueue.Results results = vvq.new Results();
        sr.message = vvq.getPriorityItemsSummaryOutput(args, results);
        sr.percent = vvq.getPercent();
        sr.status = results.status;
        sr.output = results.output.toString();
        return sr;
    }

    /**
     * Return a Response containing the snapshot with the given id
     *
     * @param snapshotId a timestamp identifying a snapshot
     * @return the Response
     */
    private Response showSnapshot(String snapshotId) {
        final String json = snap.get(snapshotId);
        if (json == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(json).build();
    }

    /**
     * Assign a new snapshot id to sr, then save sr as a json snapshot
     *
     * @param sr the SummaryResponse
     */
    private void saveSnapshot(SummaryResponse sr) {
        sr.snapshotId = SurveySnapshot.newId();
        final String json = jsonb.toJson(sr);
        if (json != null && !json.isEmpty()) {
            snap.put(sr.snapshotId, json);
        }
    }

    @GET
    @Path("/snapshots")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "List All Snapshots",
            description = "Get a list of all available snapshots of the Priority Items Summary")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Snapshot List",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                SnapshotListResponse.class)))
            })
    public Response listSnapshots(@HeaderParam(Auth.SESSION_HEADER) String sessionString) {
        CookieSession cs = Auth.getSession(sessionString);
        if (cs == null) {
            return Auth.noSessionResponse();
        }
        if (!UserRegistry.userCanUseVettingSummary(cs.user)) {
            return Response.status(Status.FORBIDDEN).build();
        }
        cs.userDidAction();
        SnapshotListResponse ret = new SnapshotListResponse(snap.list());
        return Response.ok().entity(ret).build();
    }

    @Schema(description = "Response for List Snapshots request")
    public static final class SnapshotListResponse {

        @Schema(description = "Array of available snapshots")
        public String[] array;

        public SnapshotListResponse(String[] array) {
            this.array = array;
        }
    }

    /** Schedule automatic snapshots, if enabled; this gets called when Survey Tool starts up */
    public static void scheduleAutomaticSnapshots() {
        if (!autoSnapshotsAreEnabled()) {
            return;
        }
        final Calendar when = getNextSnap();
        final long initialDelayMinutes = getMinutesUntil(when);
        final long repeatPeriodMinutes = TimeUnit.MINUTES.convert(1L, TimeUnit.DAYS);
        log(
                "Automatic Summary Snapshots are scheduled daily starting in "
                        + initialDelayMinutes
                        + " minutes, at "
                        + when.getTime());
        final ScheduledExecutorService exServ = SurveyThreadManager.getScheduledExecutorService();
        try {
            Summary summary = new Summary();
            Runnable r = summary.new AutoSnapper();
            autoSnapshotFuture =
                    exServ.scheduleAtFixedRate(
                            r, initialDelayMinutes, repeatPeriodMinutes, TimeUnit.MINUTES);
        } catch (Throwable t) {
            SurveyLog.logException(logger, t, "Exception while scheduling automatic snapshots");
            t.printStackTrace();
        }
    }

    private static boolean autoSnapshotsAreEnabled() {
        if (CLDRConfig.getInstance().getProperty("CLDR_AUTO_SNAP", false)) {
            log("Automatic Summary Snapshots (CLDR_AUTO_SNAP) are enabled");
            return true;
        } else {
            log("Automatic Summary Snapshots (CLDR_AUTO_SNAP) are not enabled");
            return false;
        }
    }

    private static Calendar getNextSnap() {
        final Calendar now = Calendar.getInstance(TimeZone.getTimeZone(AUTO_SNAP_TIME_ZONE));
        final Calendar today = scheduleToStartToday();
        if (now.compareTo(today) < 0) {
            return today;
        } else {
            return scheduleToStartTomorrow();
        }
    }

    private static Calendar scheduleToStartTomorrow() {
        final Calendar when = Calendar.getInstance(TimeZone.getTimeZone(AUTO_SNAP_TIME_ZONE));
        when.add(Calendar.DAY_OF_YEAR, 1);
        when.set(Calendar.HOUR_OF_DAY, AUTO_SNAP_HOUR_OF_DAY);
        when.set(Calendar.MINUTE, AUTO_SNAP_MINUTE_OF_HOUR);
        when.set(Calendar.SECOND, 0);
        when.set(Calendar.MILLISECOND, 0);
        return when;
    }

    private static Calendar scheduleToStartToday() {
        final Calendar when = Calendar.getInstance(TimeZone.getTimeZone(AUTO_SNAP_TIME_ZONE));
        when.set(Calendar.HOUR_OF_DAY, AUTO_SNAP_HOUR_OF_DAY);
        when.set(Calendar.MINUTE, AUTO_SNAP_MINUTE_OF_HOUR);
        when.set(Calendar.SECOND, 0);
        when.set(Calendar.MILLISECOND, 0);
        // Make sure the server has at least a few minutes to finish starting up before starting a
        // snapshot
        if (getMinutesUntil(when) < AUTO_SNAP_MINIMUM_START_MINUTES) {
            when.add(Calendar.MINUTE, AUTO_SNAP_MINIMUM_START_MINUTES);
        }
        return when;
    }

    private static long getMinutesUntil(Calendar when) {
        final long nowMillis = System.currentTimeMillis();
        final long whenMillis = when.getTimeInMillis();
        final long delayMillis = whenMillis - nowMillis;
        return TimeUnit.MINUTES.convert(delayMillis, TimeUnit.MILLISECONDS);
    }

    private class AutoSnapper implements Runnable {
        @Override
        public void run() {
            try {
                makeAutoPriorityItemsSnapshot();
            } catch (Throwable t) {
                SurveyLog.logException(logger, t, "Exception in AutoSnapper");
                t.printStackTrace();
            }
        }
    }

    private static final boolean ENABLE_ALL_LOCALE_SUMMARY = false;

    private void makeAutoPriorityItemsSnapshot() throws IOException, JSONException {
        boolean summarizeAllLocales = false;
        if (ENABLE_ALL_LOCALE_SUMMARY) {
            final SurveyMain.Phase phase = SurveyMain.getOverallSurveyPhase();
            summarizeAllLocales =
                    (phase == SurveyMain.Phase.VETTING || phase == SurveyMain.Phase.VETTING_CLOSED);
        }
        final VettingViewerQueue vvq = VettingViewerQueue.getInstance();
        vvq.setSummarizeAllLocales(summarizeAllLocales);
        final QueueMemberId qmi = new QueueMemberId();
        final Organization usersOrg = VettingViewer.getNeutralOrgForSummary();
        LoadingPolicy loadingPolicy = LoadingPolicy.START;
        SummaryResponse sr;
        int count = 0;
        log("Automatic Summary Snapshot, starting");

        boolean finished = false;
        final long startMillis = System.currentTimeMillis();
        do {
            sr = getSummaryResponse(vvq, qmi, usersOrg, loadingPolicy);
            loadingPolicy = LoadingPolicy.NOSTART;
            ++count;
            log("Automatic Summary Snapshot, got response " + count + "; percent = " + sr.percent);
            if (sr.status == VettingViewerQueue.Status.WAITING
                    || sr.status == VettingViewerQueue.Status.PROCESSING) {
                finished = autoSnapTooLong(startMillis) || autoSnapSleepCatchInterruption();
            } else {
                finished = true;
            }
        } while (!finished && !autoSnapshotFuture.isCancelled());
        if (sr.status == VettingViewerQueue.Status.READY) {
            saveSnapshot(sr);
            log("Automatic Summary Snapshot, saved " + sr.snapshotId);
        }
        log("Automatic Summary Snapshot, finished; status = " + sr.status);
    }

    private boolean autoSnapTooLong(long startMillis) {
        final long elapsedMillis = System.currentTimeMillis() - startMillis;
        final long elapsedMinutes = TimeUnit.MINUTES.convert(elapsedMillis, TimeUnit.MILLISECONDS);
        if (elapsedMinutes > AUTO_SNAP_MAX_DURATION_MINUTES) {
            log("Automatic Summary Snapshot timed out, stopping");
            return true;
        }
        return false;
    }

    private boolean autoSnapSleepCatchInterruption() {
        try {
            Thread.sleep(AUTO_SNAP_SLEEP_SECONDS * 1000L);
        } catch (InterruptedException e) {
            return true;
        }
        return false;
    }

    /** When Survey Tool is shutting down, cancel any scheduled/running automatic snapshot */
    public static void shutdown() {
        try {
            if (autoSnapshotFuture != null && !autoSnapshotFuture.isCancelled()) {
                log("Interrupting running auto-snapshot thread");
                autoSnapshotFuture.cancel(true);
            }
        } catch (Throwable t) {
            SurveyLog.logException(logger, t, "Exception interrupting Automatic Summary Snapshot");
            t.printStackTrace();
        }
    }

    public static final class CoverageStatusResponse {
        public String levelNames[] = new String[Level.values().length];
        public CalculateLocaleCoverage.CoverageResult[] results;

        public CoverageStatusResponse(Collection<CalculateLocaleCoverage.CoverageResult> results) {
            this.results = results.toArray(new CalculateLocaleCoverage.CoverageResult[0]);
            for (final Level l : Level.values()) {
                levelNames[l.ordinal()] = UCharacter.toTitleCase(ULocale.ENGLISH, l.name(), null);
            }
        }
    }

    @GET
    @Path("/dashboard/coverageStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Dashboard results",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                CoverageStatusResponse.class)))
            })
    public Response getCoverageStatus(@HeaderParam(Auth.SESSION_HEADER) String sessionString) {
        CookieSession cs = Auth.getSession(sessionString);
        if (cs == null) {
            return Auth.noSessionResponse();
        }
        if (cs.user == null || !cs.user.getLevel().canCreateSummarySnapshot()) {
            return Response.status(Status.UNAUTHORIZED).build();
        }
        cs.userDidAction();
        return Response.ok()
                .entity(
                        new CoverageStatusResponse(
                                CalculateLocaleCoverage.getCoverage(
                                        CookieSession.sm.getSTFactory())))
                .build();
    }

    @GET
    @Path("/dashboard/{locale}/{level}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Fetch the user's Dashboard for a locale",
            description = "Given a locale, get the summary information, aka Dashboard")
    @Counted(
            name = "getDashboardCount",
            absolute = true,
            description = "Number of dashboards computed")
    @Timed(absolute = true, name = "getDashboardTime", description = "Time to fetch the Dashboard")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Dashboard results",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                ReviewOutput.class))) // TODO:
                // SummaryResults.class
            })
    public Response getDashboard(
            @PathParam("locale") @Schema(required = true, description = "Locale ID") String locale,
            @PathParam("level") @Schema(required = true, description = "Coverage Level")
                    String level,
            @HeaderParam(Auth.SESSION_HEADER) String sessionString) {
        CLDRLocale loc = CLDRLocale.getInstance(locale);
        CookieSession cs = Auth.getSession(sessionString);
        if (cs == null) {
            return Auth.noSessionResponse();
        }
        cs.userDidAction();

        // *Beware*  org.unicode.cldr.util.Level (coverage) ≠ VoteResolver.Level (user)
        Level coverageLevel = org.unicode.cldr.util.Level.fromString(level);
        ReviewOutput ret = new Dashboard().get(loc, cs.user, coverageLevel, null /* xpath */);
        ret.coverageLevel = coverageLevel.name();

        return Response.ok().entity(ret).build();
    }

    @GET
    @Path("/participation/for/{user}/{locale}/{level}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Fetch another user's participation for a locale. Manager only.",
            description = "Given a locale, get the participation information, for another user")
    @Counted(name = "getUserCount", absolute = true, description = "Number of users computed")
    @Timed(
            absolute = true,
            name = "getParticipationTime",
            description = "Time to fetch the information")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Participation results",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                ParticipationResults.class)))
            })
    public Response getParticipationFor(
            @PathParam("user") @Schema(required = true, description = "User ID") Integer user,
            @PathParam("locale") @Schema(required = true, description = "Locale ID") String locale,
            @PathParam("level") @Schema(required = true, description = "Coverage Level or 'org'")
                    String level,
            @HeaderParam(Auth.SESSION_HEADER) String sessionString) {
        CLDRLocale loc = CLDRLocale.getInstance(locale);
        CookieSession cs = Auth.getSession(sessionString);
        if (cs == null) {
            return Auth.noSessionResponse();
        }
        if (!UserRegistry.userIsManagerOrStronger(cs.user)) {
            // exit early if user is not a manager.
            return Response.status(403, "Forbidden").build();
        }
        UserRegistry.User target = CookieSession.sm.reg.getInfo(user);
        if (target == null) {
            // Could not find userid
            return Response.status(404).build(); // user not found
        }
        if (!cs.user.isSameOrg(target) && !cs.user.getLevel().isAdmin()) {
            // not manager for target's org OR superadmin
            return Response.status(403, "Forbidden").build();
        }
        cs.userDidAction();

        // *Beware*  org.unicode.cldr.util.Level (coverage) ≠ VoteResolver.Level (user)
        Level coverageLevel = null;
        if (level.equals("org")) {
            coverageLevel =
                    StandardCodes.make().getLocaleCoverageLevel(target.getOrganization(), locale);
        } else {
            coverageLevel = org.unicode.cldr.util.Level.fromString(level);
        }
        ReviewOutput reviewOutput =
                new Dashboard().get(loc, target, coverageLevel, null /* xpath */);
        reviewOutput.coverageLevel = coverageLevel.name();

        ParticipationResults participationResults =
                new ParticipationResults(reviewOutput.voterProgress, reviewOutput.coverageLevel);
        return Response.ok().entity(participationResults).build();
    }

    public class ParticipationResults {
        public VoterProgress voterProgress;
        public String coverageLevel;

        public ParticipationResults(VoterProgress voterProgress, String coverageLevel) {
            this.voterProgress = voterProgress;
            this.coverageLevel = coverageLevel;
        }
    }

    private static void log(String message) {
        // TODO: how to enable logger.info? As currently configured, it doesn't print to console
        // Reference: https://unicode-org.atlassian.net/browse/CLDR-15369
        // logger.info("[Summary.logger] " + message);
        System.out.println(message);
    }
}
