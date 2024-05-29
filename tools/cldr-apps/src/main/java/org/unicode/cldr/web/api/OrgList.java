package org.unicode.cldr.web.api;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.StandardCodes;

@Path("/organizations")
@Tag(name = "organizations", description = "Get the list of Survey Tool organizations")
public class OrgList {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get Organization Map",
            description = "This handles a request for the list of organizations")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Results of Organization request",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = OrgMapResponse.class))),
            })
    public Response getOrgs() {
        try {
            OrgMapResponse response = new OrgMapResponse();
            return Response.ok(response, MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            return Response.status(500, "An exception occurred").entity(e).build();
        }
    }

    @Schema(description = "Response for organizations query")
    public static final class OrgMapResponse {

        @Schema(description = "Map from short names to display names")
        public Map<String, String> map = orgList.get();

        private static Supplier<Map<String, String>> orgList =
                Suppliers.memoize(
                        () -> {
                            Map<String, String> map = new TreeMap<>();
                            for (Organization o : Organization.values()) {
                                if (o.visibleOnFrontEnd()) {
                                    map.put(o.name(), o.getDisplayName());
                                }
                            }
                            return map;
                        });
    }

    @GET
    @Path("/coverage")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get Organization Coverage Goals")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "results of Coverage Goals request",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                OrgCoverageResponse.class)))
            })
    public Response getOrgCoverage() {
        return Response.ok(new OrgCoverageResponse()).build();
    }

    public static final class OrgCoverageResponse {
        @Schema(description = "Map from organization to locale and level")
        public Map<Organization, Map<String, Level>> organization_locale_level =
                StandardCodes.make().getLocaleTypes();

        @Schema(description = "List of 'TC Orgs'")
        public Set<Organization> tc_orgs = Organization.getTCOrgs();
    }
}
