package org.unicode.cldr.web.api;

import java.util.concurrent.ExecutionException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.SearchManager;
import org.unicode.cldr.web.SearchManager.SearchRequest;
import org.unicode.cldr.web.SearchManager.SearchResponse;

/**
 * API for querying SurveyTool
 *
 * @see {@link SearchManager}
 */
@Path("/search")
@Tag(name = "search", description = "APIs for Searching")
public class SearchAPI {

    protected static final class SearchAPIHelper {
        final SearchManager searchManager =
                SearchManager.forFactory(CookieSession.sm.getSTFactory());
        public static final SearchAPIHelper INSTANCE = new SearchAPIHelper();

        public static final SearchManager getSearchManager() {
            return INSTANCE.searchManager;
        }
    }
    ;

    @Path("/value")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Begin a new search",
            description = "Searches for paths containing the given string value")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Perform search",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = SearchResponse.class))),
                @APIResponse(
                        responseCode = "401",
                        description = "Unauthorized - need to be logged in as a valid user"),
                @APIResponse(responseCode = "404", description = "Locale not found"),
            })
    public Response newSearch(
            @Parameter(
                            required = false,
                            example = "jgo",
                            schema = @Schema(type = SchemaType.STRING))
                    @QueryParam("locale")
                    String loc,
            @HeaderParam(Auth.SESSION_HEADER) String session,
            @RequestBody(required = true) SearchRequest request) {
        final CookieSession mySession = Auth.getSession(session);
        // User must be logged in to use query function
        if (mySession == null) {
            return Auth.noSessionResponse();
        }

        final SearchManager searchManager = SearchAPIHelper.getSearchManager();

        if (loc == null
                || loc.isBlank()
                || !CookieSession.sm.isValidLocale(CLDRLocale.getInstance(loc))) {
            return Response.status(Status.NOT_FOUND).build();
        }
        SearchResponse search = searchManager.newSearch(request, loc);
        return Response.ok(search).build();
    }

    @Path("/status/{token}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get status of existing search",
            description = "Check on status of an existing search")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Get status of an existing search",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = SearchResponse.class))),
                @APIResponse(responseCode = "404", description = "Search not found")
            })
    public Response searchStatus(
            @PathParam("token") String token, @HeaderParam(Auth.SESSION_HEADER) String session) {
        final CookieSession mySession = Auth.getSession(session);
        // User must be logged in to use query function
        if (mySession == null) {
            return Auth.noSessionResponse();
        }

        final SearchManager searchManager = SearchAPIHelper.getSearchManager();

        try {
            final SearchResponse search = searchManager.getSearch(token);
            if (search == null) {
                return Response.status(Status.NOT_FOUND).build();
            }
            return Response.ok(search).build();
        } catch (ExecutionException e) {
            // error was thrown searching.
            return Response.status(500, e.getCause().getMessage()).build();
        }
    }

    @Path("/status/{token}")
    @DELETE
    @Operation(summary = "Cancel search", description = "Cancel an existing search")
    @APIResponses(
            value = {
                @APIResponse(responseCode = "204", description = "Search was deleted"),
                @APIResponse(responseCode = "404", description = "Search was not found"),
            })
    public Response searchDelete(
            @PathParam("token") String token, @HeaderParam(Auth.SESSION_HEADER) String session) {
        final CookieSession mySession = Auth.getSession(session);
        // User must be logged in to use query function
        if (mySession == null) {
            return Auth.noSessionResponse();
        }

        final SearchManager searchManager = SearchAPIHelper.getSearchManager();

        if (searchManager.deleteSearch(token)) {
            // Deleted OK
            return Response.status(Status.NO_CONTENT).build();
        } else {
            // Was not found
            return Response.status(Status.NOT_FOUND).build();
        }
    }
}
