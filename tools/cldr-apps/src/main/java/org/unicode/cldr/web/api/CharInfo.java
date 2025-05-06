package org.unicode.cldr.web.api;

import java.util.HashMap;
import java.util.Map;
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
import org.unicode.cldr.util.CodePointEscaper;

@Path("/info/chars")
@Tag(name = "info", description = "General Information")
public class CharInfo {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get Escaping Map",
            description = "This returns a list of escapable characters")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Results of Character request",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = EscapedCharInfo.class))),
            })
    public Response getEscapedCharInfo() {
        return Response.ok(EscapedCharInfo.INSTANCE).build();
    }

    /** unpacks the enum into a struct */
    public static final class EscapedCharEntry {
        public final String name;
        public final String shortName;
        public final String description;

        public EscapedCharEntry(final CodePointEscaper c) {
            name = c.name();
            shortName = c.getShortName();
            description = c.getDescription();
        }
    }

    public static final class EscapedCharInfo {
        public final String forceEscapeRegex =
                CodePointEscaper.regexPattern(CodePointEscaper.ESCAPE_IN_SURVEYTOOL, "\\u{", "}");
        public final Map<String, EscapedCharEntry> names = new HashMap<>();

        EscapedCharInfo() {
            for (final CodePointEscaper c : CodePointEscaper.values()) {
                names.put(c.getString(), new EscapedCharEntry(c));
            }
        }

        /** Constant data, so a singleton is fine */
        public static final EscapedCharInfo INSTANCE = new EscapedCharInfo();
    }
}
