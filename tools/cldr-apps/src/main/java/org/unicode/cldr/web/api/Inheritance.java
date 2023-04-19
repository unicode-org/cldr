package org.unicode.cldr.web.api;

import static org.unicode.cldr.web.CookieSession.sm;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.LocaleInheritanceInfo;
import org.unicode.cldr.util.StringId;

@Path("/xpath/inheritance")
@Tag(name = "xpath", description = "APIs for XPath info")
public class Inheritance {
    @GET
    @Path("/reasons")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns the descriptions of the reason enums")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Array of possible alt values",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = Map.class)))
            })
    public Response getReasons() {
        Map<String, String> map = new TreeMap<>();
        for (final LocaleInheritanceInfo.Reason r : LocaleInheritanceInfo.Reason.values()) {
            map.put(r.name(), r.getDescription());
        }
        return Response.ok(map).build();
    }

    @GET
    @Path("/locale/{localeId}/{hexId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Explain inheritance of an xpath",
            description = "Looks up the xpath and explains its inheritance")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "404",
                        description = "XPath Hex ID not found",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = STError.class))),
                @APIResponse(
                        responseCode = "200",
                        description = "Array of possible alt values",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                InheritanceResponse.class))),
            })
    public Response getInheritanceByHex(
            @Parameter(required = true, example = "aa", schema = @Schema(type = SchemaType.STRING))
                    @PathParam("localeId")
                    String localeId,
            @Parameter(
                            required = true,
                            example = "6154e7673c3829ce",
                            schema = @Schema(type = SchemaType.STRING))
                    @PathParam("hexId")
                    String hexId) {
        CLDRLocale locale = CLDRLocale.getInstance(localeId);
        if (locale == null) {
            return STError.badLocale(localeId);
        }
        String xpath = getXPathByHex(hexId);
        if (xpath == null) {
            return STError.badPath(hexId);
        }

        final CLDRFile f = sm.getSTFactory().make(locale, true);
        final List<LocaleInheritanceInfo> paths = f.getPathsWhereFound(xpath);
        return Response.ok(new InheritanceResponse(paths)).build();
    }

    private String getXPathByHex(String hexId) {
        String xpath = null;
        try {
            xpath = sm.xpt.getByStringID(hexId);
        } catch (RuntimeException e) {
            /*
             * Don't report the exception. This happens when it simply wasn't found.
             * Possibly getByStringID, or some version of it, should not throw an exception.
             */
        }
        return xpath;
    }

    /** Streamable version of {@link LocaleInheritanceInfo} */
    public static final class LocaleInheritance {
        public String xpath;
        public String locale;
        public LocaleInheritanceInfo.Reason reason;

        public LocaleInheritance(LocaleInheritanceInfo info) {
            this.reason = info.getReason();
            this.locale = info.getLocale();
            final String x = info.getPath();
            if (x != null) {
                this.xpath = StringId.getHexId(x);
            } else {
                this.xpath = null;
            }
        }
    }

    @Schema(description = "Response for Inheritance query")
    public static final class InheritanceResponse {
        @Schema(description = "Array of inheritance items.")
        public final LocaleInheritance[] items;

        public InheritanceResponse(List<LocaleInheritanceInfo> list) {
            items =
                    list.stream()
                            .map(i -> new LocaleInheritance(i))
                            .collect(Collectors.toList())
                            .toArray(new LocaleInheritance[list.size()]);
        }
    }
}
