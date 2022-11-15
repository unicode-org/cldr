package org.unicode.cldr.web.api;

import java.util.*;
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
import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.util.*;
import org.unicode.cldr.web.CookieSession;

@Path("/xpath/alt")
@Tag(name = "xpath", description = "APIs for XPath info")
public class XPathAlt {

    @GET
    @Path("/{hexId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Fetch alt values from XPath Hex ID",
        description = "Looks up the possible alt values for a specific XPath Hex ID."
    )
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "404",
                description = "XPath Hex ID not found",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = STError.class))
            ),
            @APIResponse(
                responseCode = "200",
                description = "Array of possible alt values",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = AltSet.class))
            ),
        }
    )
    public Response getAltByHex(
        @Parameter(
            required = true,
            example = "6154e7673c3829ce",
            schema = @Schema(type = SchemaType.STRING)
        ) @PathParam("hexId") String hexId
    ) {
        String xpath = getXPathByHex(hexId);
        if (xpath == null) {
            return Response
                .status(Response.Status.NOT_FOUND)
                .entity(new STError("XPath Hex ID " + hexId + " not found"))
                .build();
        }
        DtdData dtdData = DtdData.getInstance(DtdType.fromPath(xpath), CLDRConfig.getInstance().getCldrBaseDirectory());
        XPathParts xpp = XPathParts.getFrozenInstance(xpath);
        String el = xpp.getElement(-1);
        DtdData.Element element = dtdData.getElementFromName().get(el);
        DtdData.Attribute attribute = element.getAttributeNamed(LDMLConstants.ALT);
        Set<String> set = (attribute == null) ? null : attribute.getMatchLiterals();
        return Response.ok(new AltSet(hexId, set)).build();
    }

    private String getXPathByHex(String hexId) {
        String xpath = null;
        try {
            xpath = CookieSession.sm.xpt.getByStringID(hexId);
        } catch (RuntimeException e) {
            /*
             * Don't report the exception. This happens when it simply wasn't found.
             * Possibly getByStringID, or some version of it, should not throw an exception.
             */
        }
        return xpath;
    }

    @Schema(description = "Return value for XPath alt set query")
    public static final class AltSet {

        @Schema(description = "Hex ID of XPath.")
        public String hexId;

        @Schema(description = "Array of possible alt attributes.")
        public final String[] alt;

        public AltSet(String hexId, Set<String> set) {
            this.hexId = hexId;
            this.alt = (set == null) ? new String[0] : set.toArray(new String[0]);
        }
    }
}
