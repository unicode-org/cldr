package org.unicode.cldr.web.api;

import java.util.List;
import java.util.Map;

import javax.json.bind.annotation.JsonbProperty;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.util.PatternPlaceholders.PlaceholderInfo;
import org.unicode.cldr.util.PatternPlaceholders.PlaceholderStatus;
import org.unicode.cldr.web.CookieSession;
// import org.unicode.cldr.web.DataSection;
import org.unicode.cldr.web.SubtypeToURLMap;
import org.unicode.cldr.web.api.VoteAPIHelper.VoteEntry;

/*
 * TODO: use this instead of the deprecated SurveyAjax.getRow. First we must implement a replacement
 * here for the "dashboard=true" query parameter getRow uses for calling VettingViewerQueue.getErrorOnPath.
 * Reference: https://unicode-org.atlassian.net/browse/CLDR-14745
 */

@Path("/voting")
@Tag(name = "voting", description = "APIs for voting and retrieving vote and row data")
public class VoteAPI {
    final boolean DEBUG = false;

    @GET
    @Path("/{locale}/row/{xpath}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get row info",
        description = "Get info for a single xpath")
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "200",
                description = "Vote results",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = RowResponse.class))),
            @APIResponse(
                responseCode = "401",
                description = "Authorization required, send a valid session id"),
            @APIResponse(
                responseCode = "403",
                description = "Forbidden, no access to this data"),
            @APIResponse(
                responseCode = "404",
                description = "Row or Locale does not exist"),
            @APIResponse(
                responseCode = "500",
                description = "Internal Server Error",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = STError.class))),
        })
    public Response getRow(
        @Parameter(required = true, example = "br",
            schema = @Schema(type = SchemaType.STRING)) @PathParam("locale") String loc,

        @Parameter(example = "132345490064d839",
            schema = @Schema(type = SchemaType.STRING)) @PathParam("xpath") String xpath,

        @QueryParam("dashboard") @Schema(required = false, description = "Whether to get dashboard info") Boolean getDashboard,

        @HeaderParam(Auth.SESSION_HEADER) String session) {
        return VoteAPIHelper.handleGetOneRow(loc, session, xpath, getDashboard);
    }

    @GET
    @Path("/{locale}/page/{page}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get page info",
        description = "Get all info for all xpaths in a page")
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "200",
                description = "Vote results",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = RowResponse.class))),
            @APIResponse(
                responseCode = "401",
                description = "Authorization required, send a valid session id"),
            @APIResponse(
                responseCode = "403",
                description = "Forbidden, no access to this data"),
            @APIResponse(
                responseCode = "404",
                description = "Row or Locale does not exist"),
            @APIResponse(
                responseCode = "500",
                description = "Internal Server Error",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = STError.class))),
        })
    public Response getPage(
        @Parameter(required = true, example = "br",
            schema = @Schema(type = SchemaType.STRING)) @PathParam("locale") String loc,

        @Parameter(example = "Languages_K_N",
            schema = @Schema(type = SchemaType.STRING)) @PathParam("page") String page,

        @HeaderParam(Auth.SESSION_HEADER) String session) {
        return VoteAPIHelper.handleGetOnePage(loc, session, page);
    }

    public static final class RowResponse {

        public static final class Row {

            public static final class Candidate {
                public String value;
                public String displayValue;
                public String pClass;
                public String example;
                public boolean isBaselineValue;
                public VoteEntry[] votes;
            }

            public Candidate[] items;
            public String xpath;
            public String code;
            public VoteResolver<String> resolver;
            public String dir;
            public StatusAction statusAction;
            public String displayName;
            public String displayExample;
            public Map<String, String> extraAttributes;
            public boolean hasVoted;
            public boolean flagged;
            public VoteResolver<String> voteResolver;
            public String helpHtml;
            public String rdf;
            public String inheritedLocale;
            public String winningValue;
            public String inheritedXpath;
            @Schema(description = "status of placeholder value", example = "REQUIRED")
            public PlaceholderStatus placeholderStatus;
            @Schema(description = "map of placeholder string to example value")
            public Map<String, PlaceholderInfo> placeholderInfo;
        }

        public String localeDisplayName;
        public String pageId;
        public boolean isReadOnly;
        @Schema(description = "If set, row is not available because there is a Default Content parent. See the specified locale instead.")
        public String dcParent;
        public Row[] rows;
        public JSONArray issues;
    }

    @POST
    @Path("/{locale}/row/{xpath}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Submit a vote",
        description = "Submit a specific vote")
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "200",
                description = "Vote operation completed (but check result status)",
                content = @Content(mediaType = "application/json",
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
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = STError.class))),
        })
    public Response vote(
        @Parameter(required = true, example = "br",
            schema = @Schema(type = SchemaType.STRING)) @PathParam("locale") String loc,

        @Parameter(required = true, example = "132345490064d839",
            schema = @Schema(type = SchemaType.STRING)) @PathParam("xpath") String xpath,

        @HeaderParam(Auth.SESSION_HEADER) String session,

        VoteRequest request) {
        // Verify session
        final CookieSession mySession = Auth.getSession(session);
        if (mySession == null) {
            return Auth.noSessionResponse();
        }

        return VoteAPIHelper.handleVote(loc, xpath, request, mySession);
    }

    final static public class VoteResponse {
        @Schema(description = "true if voting succeeded.")
        public boolean didVote;
        @Schema(description = "If set, some other reason why the submission failed.")
        public String didNotSubmit;
        @Schema(description = "VoteResolver info about the voting results")
        public VoteResolver<String> submitResult;
        @Schema(description = "If not ALLOW_*, gives reason why the voting was not allowed.")
        public StatusAction statusAction = null;
        @Schema()
        public boolean dataEmpty;
        @Schema(description = "Tests which were run.")
        public String testsRun;
        @Schema(description = "Results of status checks.")
        public CheckStatusSummary[] results;

        void setResults(List<CheckStatus> results) {
            this.results = new CheckStatusSummary[results.size()];
            for (int i = 0; i < results.size(); i++) {
                this.results[i] = new CheckStatusSummary(results.get(i));
            }
        }
    }

    final static public class CheckStatusSummary {
        public String message;
        public Type type;
        public Subtype subtype;
        public String subtypeUrl;
        public Phase phase;

        @JsonbProperty("class")
        private String clazz;

        public CheckStatusSummary(CheckStatus checkStatus) {
            this.message = checkStatus.getMessage();
            this.type = checkStatus.getType();
            // cause
            CheckCLDR cause = checkStatus.getCause();
            if (cause != null) {
                this.clazz = cause.getClass().getSimpleName();
                this.phase = cause.getPhase();
            }
            // subtype
            this.subtype = checkStatus.getSubtype();
            if (this.subtype != null) {
                this.subtypeUrl = SubtypeToURLMap.forSubtype(this.subtype); // could be null.
            }
        }
    }
}
