package org.unicode.cldr.web.api;

import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
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

@Path("/locales")
@Tag(name = "locales", description = "APIs for locale lists")
public class LocaleList {

    public final class LocaleNormalizerResponse {
        @Schema(description = "Normalized locale array")
        public String normalized;

        @Schema(description = "List of messages of why some locales were rejected")
        public Map<String, LocaleNormalizer.LocaleRejection> messages = null;

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
            description = "Return a list of all locales")
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
                            required = false,
                            example = "adlam",
                            schema = @Schema(type = SchemaType.STRING))
                    @QueryParam("org")
                    String org) {

        LocaleNormalizer ln = new LocaleNormalizer();
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
}
