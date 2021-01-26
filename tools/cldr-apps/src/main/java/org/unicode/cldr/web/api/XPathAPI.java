package org.unicode.cldr.web.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.unicode.cldr.web.CookieSession;

@Path("/xpath")
public class XPathAPI {
    @GET @Path("{hexId}")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(
        summary = "Fetch XPath string from hex id",
        description = "Looks up a specific hex ID.")
    public Response getByHex(
        @Parameter(required = true, example = "1234abcd") String hexId) {

        // the actual implementation
        return Response.ok(CookieSession.sm.xpt.getByStringID(hexId)).build();
    }

}
