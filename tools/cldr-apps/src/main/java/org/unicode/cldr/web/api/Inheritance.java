package org.unicode.cldr.web.api;

import static org.unicode.cldr.web.CookieSession.sm;

import java.util.Arrays;
import java.util.List;
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
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.STFactory;

@Path("/xpath/inheritance")
@Tag(name = "xpath", description = "APIs for XPath info")
public class Inheritance {
    public static final class ReasonInfo {
        ReasonInfo(LocaleInheritanceInfo.Reason v) {
            this.reason = v.name();
            this.terminal = v.isTerminal();
            this.description = v.getDescription();
        }

        @Schema(description = "reason id", example = "codeFallback")
        public final String reason;

        @Schema(description = "true if a terminal reason", example = "true")
        public final boolean terminal;

        @Schema(
                description =
                        "Description for reason. String substitution of 'attribute' may be required.")
        public final String description;
    }

    public static final ReasonInfo[] reasons =
            Arrays.stream(LocaleInheritanceInfo.Reason.values())
                    .map(v -> new ReasonInfo(v))
                    .toArray(l -> new ReasonInfo[l]);

    @GET
    @Path("/reasons")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns the descriptions of the reason enums")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Array of possible reasons",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        type = SchemaType.ARRAY,
                                                        implementation = ReasonInfo.class)))
            })
    public Response getReasons() {
        return Response.ok(reasons).build();
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
        final String xpath = sm.xpt.getByStringID(hexId);
        if (xpath == null) {
            return STError.badPath(hexId);
        }

        final CLDRFile f = sm.getSTFactory().make(locale, true);
        final List<LocaleInheritanceInfo> paths = f.getPathsWhereFound(xpath);
        return Response.ok(new InheritanceResponse(paths)).build();
    }

    /** Streamable version of {@link LocaleInheritanceInfo} */
    public static final class LocaleInheritance {
        @Schema(description = "xpath stringid to item")
        public String xpath;

        @Schema(description = "locale of item if present")
        public String locale;

        @Schema(description = "xpath full of item")
        public String xpathFull;

        @Schema(description = "which attribute was implicated in a change")
        public String attribute;

        @Schema(description = "reason for the entry")
        public LocaleInheritanceInfo.Reason reason;

        @Schema(description = "true if not shown in the SurveyTool")
        public boolean hidden = false;

        public LocaleInheritance(LocaleInheritanceInfo info, final STFactory stf) {
            this.reason = info.getReason();
            this.locale = info.getLocale();
            this.attribute = info.getAttribute();
            final String x = info.getPath();
            if (x != null) {
                this.xpath = StringId.getHexId(x);
                CookieSession.sm.xpt.getByXpath(x); // make sure it's in the XPT!
            } else {
                this.xpath = null;
            }
            this.xpathFull = x;
            if (this.locale != null && !this.locale.isEmpty() && this.xpath != null) {
                // Fairly complex to answer this.
                this.hidden = !stf.isVisibleInSurveyTool(this.locale, x);
            } else {
                // without a locale or xpath, must be hidden
                this.hidden = true;
            }
        }
    }

    @Schema(description = "Response for Inheritance query")
    public static final class InheritanceResponse {
        @Schema(description = "Array of inheritance items.")
        public final LocaleInheritance[] items;

        public InheritanceResponse(List<LocaleInheritanceInfo> list) {
            final STFactory stf = CookieSession.sm.getSTFactory();
            items =
                    list.stream()
                            .map(i -> new LocaleInheritance(i, stf))
                            .collect(Collectors.toList())
                            .toArray(new LocaleInheritance[list.size()]);
        }
    }
}
