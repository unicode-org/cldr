package org.unicode.cldr.web.api;

import java.io.IOException;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.unicode.cldr.util.*;
import org.unicode.cldr.web.*;

@ApplicationScoped
@Path("/missingxml")
@Tag(name = "missing xml", description = "Generate XML for error/missing/provisional paths")
public class MissingPathXml {

    @GET
    @Path("/{locale}/{level}")
    @Produces(MediaType.APPLICATION_XML)
    @Operation(
            summary = "Generate missing XML",
            description =
                    "Generate XML for error/missing/provisional paths, as basis for bulk submission")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Missing XML results",
                        content =
                                @Content(
                                        mediaType = "application/xml",
                                        schema = @Schema(implementation = String.class))),
            })
    public Response getMissingXml(
            @PathParam("locale") @Schema(required = true, description = "Locale ID") String locale,
            @PathParam("level") @Schema(required = true, description = "Coverage Level")
                    String level,
            @HeaderParam(Auth.SESSION_HEADER) String sessionString) {
        final CLDRLocale loc = CLDRLocale.getInstance(locale);
        final CookieSession cs = Auth.getSession(sessionString);
        if (cs == null) {
            return Auth.noSessionResponse();
        }
        if (!UserRegistry.userCanModifyLocale(cs.user, loc)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        cs.userDidAction();

        // *Beware*  org.unicode.cldr.util.Level (coverage) ≠ VoteResolver.Level (user)
        final Level coverageLevel = org.unicode.cldr.util.Level.fromString(level);
        try {
            final Factory factory = CookieSession.sm.getSTFactory();
            final Factory baselineFactory = CookieSession.sm.getDiskFactory();
            final VettingViewer.UsersChoice<Organization> usersChoice =
                    new STUsersChoice(CookieSession.sm);
            final Organization usersOrg = cs.user.vrOrg();
            final MissingXmlGetter xmlGetter = new MissingXmlGetter(factory, baselineFactory);
            xmlGetter.setUserInfo(cs.user.id, usersOrg, usersChoice);
            final String xml = xmlGetter.getXml(loc, coverageLevel);
            return Response.ok().type(MediaType.APPLICATION_XML).entity(xml).build();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }
    }
}
