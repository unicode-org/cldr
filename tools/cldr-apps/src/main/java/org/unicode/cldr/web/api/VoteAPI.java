package org.unicode.cldr.web.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.json.JSONArray;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Type;
import org.unicode.cldr.test.CheckCLDR.Phase;
import org.unicode.cldr.test.CheckCLDR.StatusAction;
import org.unicode.cldr.util.PatternPlaceholders.PlaceholderInfo;
import org.unicode.cldr.util.PatternPlaceholders.PlaceholderStatus;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.Dashboard;
import org.unicode.cldr.web.DataPage;
import org.unicode.cldr.web.SubtypeToURLMap;
import org.unicode.cldr.web.SurveyForum;
import org.unicode.cldr.web.api.VoteAPIHelper.VoteEntry;

@Path("/voting")
@Tag(name = "voting", description = "APIs for voting and retrieving vote and row data")
public class VoteAPI {

    @GET
    @Path("/{locale}/row/{xpstrid}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get row info", description = "Get info for a single xpath")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Vote results",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = RowResponse.class))),
                @APIResponse(
                        responseCode = "401",
                        description = "Authorization required, send a valid session id"),
                @APIResponse(
                        responseCode = "403",
                        description = "Forbidden, no access to this data"),
                @APIResponse(responseCode = "404", description = "Row or Locale does not exist"),
                @APIResponse(
                        responseCode = "500",
                        description = "Internal Server Error",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = STError.class))),
            })
    public Response getRow(
            @Parameter(required = true, example = "br", schema = @Schema(type = SchemaType.STRING))
                    @PathParam("locale")
                    String loc,
            @Parameter(example = "132345490064d839", schema = @Schema(type = SchemaType.STRING))
                    @PathParam("xpstrid")
                    String xpstrid,
            @QueryParam("dashboard")
                    @Schema(description = "Whether to get dashboard info")
                    @DefaultValue("false")
                    Boolean getDashboard,
            @HeaderParam(Auth.SESSION_HEADER) String session) {
        return VoteAPIHelper.handleGetOneRow(loc, session, xpstrid, getDashboard);
    }

    @GET
    @Path("/{locale}/page/{page}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get page info", description = "Get all info for all xpaths in a page")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Vote results",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = RowResponse.class))),
                @APIResponse(
                        responseCode = "401",
                        description = "Authorization required, send a valid session id"),
                @APIResponse(
                        responseCode = "403",
                        description = "Forbidden, no access to this data"),
                @APIResponse(
                        responseCode = "404",
                        description =
                                "Row or Locale does not exist. More details in the 'code' field.",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = STError.class))),
                @APIResponse(
                        responseCode = "500",
                        description = "Internal Server Error",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = STError.class))),
            })
    public Response getPage(
            @Parameter(required = true, example = "br", schema = @Schema(type = SchemaType.STRING))
                    @PathParam("locale")
                    String loc,
            @Parameter(example = "Languages_K_N", schema = @Schema(type = SchemaType.STRING))
                    @PathParam("page")
                    String page,
            @QueryParam("xpstrid")
                    @Schema(description = "Xpath string ID if page is auto")
                    @DefaultValue("")
                    String xpstrid,
            @HeaderParam(Auth.SESSION_HEADER) String session) {

        /*
         * The optional xpstrid query parameter enables requests like
         *    /cldr-apps/api/voting/fr/page/auto?xpstrid=2703e9d07ab2ef3a
         * so that a URL like
         *    https://cldr-smoke.unicode.org/cldr-apps/v#/zh_Hant//2703e9d07ab2ef3a
         * can be used instead of
         *    https://cldr-smoke.unicode.org/cldr-apps/v#/zh_Hant/Alphabetic_Information/2703e9d07ab2ef3a
         */
        return VoteAPIHelper.handleGetOnePage(loc, session, page, xpstrid);
    }

    /** Array of status items. Only stores one example entry per subtype. */
    public static final class EntireLocaleStatusResponse {
        public EntireLocaleStatusResponse() {}

        void addAll(Collection<CheckStatus> list) {
            for (final CheckStatus c : list) {
                add(c);
            }
        }

        void add(CheckStatus c) {
            if (!allSubtypes.contains(c.getSubtype())) {
                tests.add(new CheckStatusSummary(c));
                allSubtypes.add(c.getSubtype());
            }
        }

        @Schema(description = "list of test results")
        public List<CheckStatusSummary> tests = new ArrayList<>();

        // we only want to store one example for each subtype.
        private Set<CheckStatus.Subtype> allSubtypes = new HashSet<>();

        boolean isEmpty() {
            return tests.isEmpty();
        }
    }

    public static final class RowResponse {

        public static final class Row {

            public static final class Candidate {
                public String displayValue;
                public String example;
                public String history;
                public boolean isBaselineValue;
                public String pClass;
                public String rawValue;
                public List<CheckStatusSummary> tests;
                public String value;
                public String valueHash;
                public Map<String, VoteEntry> votes;
            }

            public static final class OrgValueVotes<T> {
                public boolean conflicted;
                public T orgVote; // value like "↑↑↑"
                public String status; // like "ok"
                public Map<T, Long> votes; // key is value like "↑↑↑"
            }

            public static final class VotingResults<T> {
                public Map<String, Long> nameTime;
                public Map<String, OrgValueVotes<T>>
                        orgs; // key is organization name like "apple", "google"
                public int requiredVotes;

                @Schema(description = "1-dimensional array of value, vote, value, vote…")
                public Object[] value_vote;

                public boolean valueIsLocked;
            }

            public boolean canFlagOnLosing;
            public String code;
            public VoteResolver.Status confirmStatus;
            public int coverageValue;
            public String dir;
            public String displayExample;
            public String displayName;
            public String rawEnglish;
            public Map<String, String> extraAttributes;
            public boolean flagged;
            public SurveyForum.PathForumStatus forumStatus;
            public boolean hasVoted;
            public String helpHtml;
            public String inheritedLocale;
            public String inheritedValue;
            public String inheritedDisplayValue;
            public String inheritedXpid;
            public Map<String, Candidate> items;

            @Schema(description = "map of placeholder string to example value")
            public Map<String, PlaceholderInfo> placeholderInfo;

            @Schema(description = "status of placeholder value", example = "REQUIRED")
            public PlaceholderStatus placeholderStatus;

            public String rdf;
            public boolean rowFlagged;
            public StatusAction statusAction;
            public String translationHint;
            public String voteVhash;
            public VotingResults<String> votingResults;
            public String winningValue;
            public String winningVhash;
            public String xpath;
            public int xpathId;
            public String xpstrid;

            @Schema(description = "prose description of voting outcome")
            public String voteTranscript;

            @Schema(description = "True if candidates are fixed (disable plus).", example = "false")
            public boolean fixedCandidates;
        }

        public static final class Page {
            public boolean nocontent; // true if dcParent is set
            public Map<String, RowResponse.Row> rows;
        }

        public static final class DisplaySets {
            public DataPage.DisplaySet ph; // path header; cf. PathHeaderSort.name
        }

        public Object canModify;

        @Schema(
                description =
                        "If set, row is not available because there is a Default Content parent. See the specified locale instead.")
        public String dcParent;

        public DisplaySets displaySets;
        public JSONArray issues;
        public String loc; // normally matches requested locale ID unless "USER" is requested
        public String localeDisplayName;
        public Dashboard.ReviewNotification[] notifications;
        public Page page;
        public String pageId;

        /**
         * If the request was for a single row only, include the hex xpath ID for that row here in
         * the response.
         */
        public String xpstrid = null;

        public void setOneRowPath(String xpstrid) {
            this.xpstrid = xpstrid;
        }
    }

    @POST
    @Path("/{locale}/row/{xpstrid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Submit a vote", description = "Submit a specific vote")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Vote operation completed (but check result status)",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = VoteResponse.class))),
                @APIResponse(
                        responseCode = "401",
                        description = "Authorization required, send a valid session id"),
                @APIResponse(
                        responseCode = "403",
                        description = "Forbidden, no access to make this vote"),
                @APIResponse(
                        responseCode = "500",
                        description = "Internal Server Error",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = STError.class))),
            })
    public Response vote(
            @Parameter(required = true, example = "br", schema = @Schema(type = SchemaType.STRING))
                    @PathParam("locale")
                    String loc,
            @Parameter(
                            required = true,
                            example = "132345490064d839",
                            schema = @Schema(type = SchemaType.STRING))
                    @PathParam("xpstrid")
                    String xpstrid,
            @HeaderParam(Auth.SESSION_HEADER) String session,
            VoteRequest request) {
        // Verify session
        final CookieSession mySession = Auth.getSession(session);
        if (mySession == null) {
            return Auth.noSessionResponse();
        }
        final String xp = CookieSession.sm.xpt.getByStringID(xpstrid);
        if (xp == null) {
            return Response.status(Response.Status.NOT_FOUND).build(); // no XPath found
        }
        return VoteAPIHelper.handleVote(
                loc,
                xp,
                request.value,
                request.voteLevelChanged,
                mySession,
                true /* forbiddenIsOk */);
    }

    public static final class VoteResponse {
        @Schema(description = "True if voting succeeded.")
        public boolean didVote;

        @Schema(description = "If set, some other reason why the submission failed.")
        public String didNotSubmit;

        @Schema(description = "If not ALLOW_*, gives reason why the voting was not allowed.")
        public StatusAction statusAction = null;

        @Schema(description = "Results of status checks.")
        public CheckStatusSummary[] testResults;

        @Schema(description = "True if testResults include warnings.")
        public boolean testWarnings;

        @Schema(description = "True if testResults include errors.")
        public boolean testErrors;

        void setTestResults(List<CheckStatus> testResults) {
            this.testResults = new CheckStatusSummary[testResults.size()];
            this.testWarnings = has(testResults, CheckStatus.warningType);
            this.testErrors = has(testResults, CheckStatus.errorType);
            for (int i = 0; i < testResults.size(); i++) {
                this.testResults[i] = new CheckStatusSummary(testResults.get(i));
            }
        }

        private static boolean has(List<CheckStatus> result, CheckStatus.Type type) {
            for (CheckStatus s : result) {
                if (s.getType().equals(type)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static final class CheckStatusSummary {
        public String message;
        public Type type;
        public Subtype subtype;
        public String subtypeUrl;
        public Phase phase;
        public String cause;
        public boolean entireLocale;

        public CheckStatusSummary(CheckStatus checkStatus) {
            this.message = checkStatus.getMessage();
            this.type = checkStatus.getType();
            CheckCLDR cccause = checkStatus.getCause();
            if (cccause != null) {
                this.cause = cccause.getClass().getSimpleName();
                this.phase = cccause.getPhase(); // unused on front end
            }
            // subtype
            this.subtype = checkStatus.getSubtype();
            if (this.subtype != null) {
                this.subtypeUrl = SubtypeToURLMap.forSubtype(this.subtype); // could be null.
            }
            this.entireLocale = checkStatus.getEntireLocale();
        }
    }

    @GET
    @Path("/{locale}/errors")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get overall errors", description = "Get overall errors for a locale")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Error results",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                EntireLocaleStatusResponse.class))),
                @APIResponse(responseCode = "204", description = "No errors in this locale"),
                @APIResponse(
                        responseCode = "403",
                        description = "Forbidden, no access to this data"),
                @APIResponse(responseCode = "404", description = "Locale does not exist"),
                @APIResponse(
                        responseCode = "500",
                        description = "Internal Server Error",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = STError.class))),
            })
    public Response getLocaleErrors(
            @Parameter(required = true, example = "br", schema = @Schema(type = SchemaType.STRING))
                    @PathParam("locale")
                    String loc) {
        return VoteAPIHelper.handleGetLocaleErrors(loc);
    }
}
