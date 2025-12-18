package org.unicode.cldr.web.api;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Logger;
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
import org.unicode.cldr.util.LocaleNormalizer;
import org.unicode.cldr.util.LocaleSet;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.SurveyException;
import org.unicode.cldr.web.UserRegistry;

@Path("/locales")
@Tag(name = "locales", description = "APIs for locale lists")
public class LocaleList {

    private static final Logger logger = Logger.getLogger(LocaleList.class.getName());

    public static final class LocaleNormalizerResponse {
        @Schema(description = "Normalized locale array")
        public String normalized;

        @Schema(description = "List of messages of why some locales were rejected")
        public Map<String, LocaleNormalizer.LocaleRejection> messages;

        public LocaleNormalizerResponse(LocaleNormalizer n, final String normalized) {
            this.messages = n.getMessages();
            if (this.messages != null && this.messages.isEmpty()) {
                this.messages = null;
            }
            this.normalized = normalized;
        }
    }

    @Path("/normalize")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Normalize a list of Locales",
            description = "Return a normalized list of locales")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Normalized response",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                LocaleNormalizerResponse.class))),
            })
    public Response normalize(
            @Parameter(
                            description = "Space-separated list of locales",
                            required = true,
                            example = "jgo vec kjj",
                            schema = @Schema(type = SchemaType.STRING))
                    @QueryParam("locs")
                    String locs,
            @Parameter(
                            description = "Optional Organization, as a coverage limit",
                            example = "adlam",
                            schema = @Schema(type = SchemaType.STRING))
                    @QueryParam("org")
                    String org) {

        LocaleNormalizer ln = new LocaleNormalizer().disallowDefaultContent();
        String normalized;

        if (org == null || org.isBlank()) {
            normalized = ln.normalize(locs);
        } else {
            Organization o = Organization.fromString(org);
            if (o == null) {
                return new STError("Bad organization: " + org).build();
            }
            normalized = ln.normalizeForSubset(locs, o.getCoveredLocales());
        }
        final LocaleNormalizerResponse r = new LocaleNormalizerResponse(ln, normalized);
        return Response.ok().entity(r).build();
    }

    @Path("/combine-variants")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Combine regional variants and normalize a list of Locales",
            description = "Return a combined/normalized list of locales")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description =
                                "Combined/normalized response; e.g., given zh fr_BE fr_CA, return fr zh",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                LocaleNormalizerResponse.class))),
            })
    public Response combineRegionalVariants(
            @Parameter(
                            description = "Space-separated list of locales",
                            required = true,
                            example = "zh fr_BE fr_CA",
                            schema = @Schema(type = SchemaType.STRING))
                    @QueryParam("locs")
                    String locs) {
        LocaleNormalizer ln = new LocaleNormalizer();
        String normalized = ln.normalize(locs);
        LocaleSet locSet = LocaleNormalizer.setFromStringQuietly(normalized, null);
        LocaleSet langSet = locSet.combineRegionalVariants();
        String combinedNormalized = langSet.toString();
        final LocaleNormalizerResponse r = new LocaleNormalizerResponse(ln, combinedNormalized);
        return Response.ok().entity(r).build();
    }

    @Path("/org/{orgName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get the list of locales for an organization",
            description = "Return a list of locales")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Space-separated list of locale IDs",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                OrgLocalesResponse.class))),
            })
    public Response org(
            @Parameter(
                            description = "Organization name",
                            required = true,
                            example = "adlam",
                            schema = @Schema(type = SchemaType.STRING))
                    @PathParam("orgName")
                    String orgName) {
        Organization o = Organization.fromString(orgName);
        if (o == null) {
            return new STError("Bad organization: " + orgName).build();
        }
        LocaleSet localeSet = o.getCoveredLocales();
        OrgLocalesResponse response = new OrgLocalesResponse(localeSet);
        return Response.ok().entity(response).build();
    }

    public static class OrgLocalesResponse {
        @Schema(description = "Space-separated list of locale IDs")
        public String locales;

        OrgLocalesResponse(LocaleSet localeSet) {
            this.locales = localeSet.toString();
        }
    }

    @Path("/invalid")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get the list of invalid locale IDs assigned to users",
            description = "Return a list of descriptions of invalid locales")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Space-separated list of invalid locale IDs",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                InvalidLocalesResponse.class))),
                @APIResponse(responseCode = "403", description = "Forbidden"),
            })
    public Response invalid(@HeaderParam(Auth.SESSION_HEADER) String sessionString) {
        CookieSession session = Auth.getSession(sessionString);
        if (session == null) {
            return Auth.noSessionResponse();
        }
        if (!UserRegistry.userIsAdmin(session.user)) {
            return Response.status(403, "Forbidden").build();
        }
        session.userDidAction();
        List<InvalidLocale> list = new ArrayList<>();
        try {
            findInvalidLocales(list, LocaleNormalizer.InvalidLocaleAction.FIND);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        InvalidLocalesResponse response = new InvalidLocalesResponse(list);
        return Response.ok().entity(response).build();
    }

    @Path("/invalid/fix")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Fix", description = "Fix the invalid locale IDs assigned to users")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Successful fix",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                InvalidLocalesResponse.class))),
                @APIResponse(responseCode = "403", description = "Forbidden"),
                @APIResponse(responseCode = "404", description = "Count not matched"),
                @APIResponse(responseCode = "500", description = "Internal Server Error"),
            })
    public Response fixInvalid(
            @HeaderParam(Auth.SESSION_HEADER) String sessionString,
            FixInvalidLocalesRequest request) {
        CookieSession session = Auth.getSession(sessionString);
        if (session == null) {
            return Auth.noSessionResponse();
        }
        if (!UserRegistry.userIsAdmin(session.user)) {
            return Response.status(403, "Forbidden").build();
        }
        session.userDidAction();
        // Synchronize to prevent another Admin user from doing the same thing at the same time
        synchronized (LocaleList.class) {
            List<InvalidLocale> list = new ArrayList<>();
            try {
                findInvalidLocales(list, LocaleNormalizer.InvalidLocaleAction.FIND);
                if (list.size() != request.count) {
                    // The caller specified the wrong number of problems; their data may be out of
                    // date
                    return Response.status(404, "Count not matched").build();
                }
                list.clear();
                findInvalidLocales(list, LocaleNormalizer.InvalidLocaleAction.FIX);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            InvalidLocalesResponse response = new InvalidLocalesResponse(list);
            return Response.ok().entity(response).build();
        }
    }

    public static class InvalidLocalesResponse {
        @Schema(description = "Array of InvalidLocale objects")
        public final InvalidLocale[] problems;

        InvalidLocalesResponse(List<InvalidLocale> list) {
            this.problems = list.toArray(new InvalidLocale[0]);
        }
    }

    /**
     * Fill in the given empty list with all invalid locale IDs that are assigned to Survey Tool
     * users, and if action is FIX, fix them. "Invalid" here includes unknown IDs, locales that are
     * outside the user's organization's coverage, and default content locales.
     *
     * @param list the empty list to be filled in
     * @param action FIND or FIX
     */
    private static void findInvalidLocales(
            List<InvalidLocale> list, LocaleNormalizer.InvalidLocaleAction action)
            throws SQLException, SurveyException {
        // Map invalid locale ID to the number of users who have it and the reason it is invalid
        final LocaleNormalizer.ProblemMap problems = new LocaleNormalizer.ProblemMap();

        CookieSession.sm.reg.findInvalidUserLocales(problems, action);
        logger.warning(
                "Find invalid locales action = "
                        + action
                        + " got "
                        + problems.map.size()
                        + " problems");
        for (String id : new TreeSet<>(problems.map.keySet())) {
            list.add(new LocaleList.InvalidLocale(id, problems.map.get(id)));
        }
    }

    public static class InvalidLocale {
        public final String id;
        public final Integer userCount;
        public final String rejection;
        public final String solution;

        public InvalidLocale(String id, LocaleNormalizer.Problem problem) {
            this.id = id;
            this.userCount = problem.userCount;
            this.rejection = problem.rejection.toString();
            this.solution = combineSolutions(problem.solutions);
        }

        private String combineSolutions(Map<LocaleNormalizer.Solution, Integer> solutions) {
            StringBuilder s = new StringBuilder();
            for (LocaleNormalizer.Solution solution : solutions.keySet()) {
                if (s.length() > 0) {
                    s.append("; ");
                }
                s.append(solution.toString());
                Integer count = solutions.get(solution);
                if (count != 1) {
                    s.append(" (x️").append(count).append(")");
                }
            }
            return s.toString();
        }
    }

    public static class FixInvalidLocalesRequest {
        public Integer count;
    }
}
