package org.unicode.cldr.web.api;

import static org.unicode.cldr.web.CookieSession.sm;

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
import org.unicode.cldr.web.UserRegistry;

@Path("/xpath/alt")
@Tag(name = "xpath", description = "APIs for XPath info")
public class XPathAlt {

    @GET
    @Path("/{localeId}/{hexId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Fetch alt values from locale and XPath Hex ID",
            description =
                    "Looks up the alt values that could be added for a specific XPath Hex ID.")
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
                                        schema = @Schema(implementation = AltSetResponse.class))),
            })
    public Response getAltByHex(
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
        Set<String> set = getSet(locale, xpath);
        return Response.ok(new AltSetResponse(hexId, set)).build();
    }

    @Schema(description = "Response for XPath alt set query")
    public static final class AltSetResponse {

        @Schema(description = "Hex ID of XPath.")
        public final String hexId;

        @Schema(description = "Array of possible alt attributes.")
        public final String[] alt;

        public AltSetResponse(String hexId, Set<String> set) {
            this.hexId = hexId;
            this.alt = (set == null) ? new String[0] : set.toArray(new String[0]);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Add alt path", description = "Create a new alt path in the given locale")
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
                        description = "Alt path was added",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(implementation = AddAltPathRequest.class))),
            })
    public Response putByHex(
            @HeaderParam(Auth.SESSION_HEADER) String sessionString, AddAltPathRequest request) {
        CookieSession session;
        try {
            session = Auth.getSession(sessionString);
            if (session == null) {
                return Auth.noSessionResponse();
            }
            if (!UserRegistry.userIsTCOrStronger(session.user)) {
                return Response.status(403, "Forbidden").build();
            }
            session.userDidAction();
        } catch (Exception e) {
            return Response.status(500, "An exception occurred").entity(e).build();
        }
        CLDRLocale locale = CLDRLocale.getInstance(request.localeId);
        if (locale == null) {
            return STError.badLocale(request.localeId);
        }
        final String xpath = sm.xpt.getByStringID(request.hexId);
        if (xpath == null) {
            return STError.badPath(request.hexId);
        }
        String alt = getFromSet(request.alt, locale, xpath);
        if (alt == null) {
            return noAlt(request.hexId, request.alt);
        }
        return addPath(locale, xpath, alt, session);
    }

    private String getFromSet(String requestAlt, CLDRLocale locale, String xpath) {
        Set<String> set = getSet(locale, xpath);
        if (set == null || !set.contains(requestAlt)) {
            return null;
        }
        for (String alt : set) {
            if (alt.equals(requestAlt)) {
                // do not return requestAlt -- even though they're equal, alt differs from
                // requestAlt in the eyes of Code scanning / CodeQL warning,
                // "Polynomial regular expression used on uncontrolled data"
                // -- requestAlt is "uncontrolled data"
                // -- alt is "controlled data"
                return alt;
            }
        }
        return null;
    }

    private Response addPath(CLDRLocale locale, String xpath, String alt, CookieSession session) {
        CLDRFile cldrFile = getFile(locale);
        if (cldrFile == null) {
            return badFile(locale);
        }
        try {
            XPathParts xpp = XPathParts.getFrozenInstance(xpath).cloneAsThawed();
            xpp.addAttribute(LDMLConstants.ALT, alt);
            String newXpath = xpp.toString();
            if (cldrFile.isHere(newXpath)) { // compare STFactory.getPathsForFile()
                return alreadyExists(newXpath);
            }
            String newValue = null; // Abstain; CldrUtility.INHERITANCE_MARKER could give
            // StatusAction.FORBID_NULL
            Response r =
                    VoteAPIHelper.handleVote(
                            locale.getBaseName(),
                            newXpath,
                            newValue,
                            1 /* one vote */,
                            session,
                            false /* forbiddenIsOk */);
            int status = r.getStatus();
            if (status != 200) {
                return badHandleVote(newXpath, status);
            }
        } catch (Exception e) {
            return Response.status(500, "An exception occurred").entity(e).build();
        }
        return Response.ok().build();
    }

    public static class AddAltPathRequest {

        @Schema(description = "Locale ID")
        public String localeId;

        @Schema(description = "XPath Hex ID")
        public String hexId;

        @Schema(description = "New alt value")
        public String alt;
    }

    private Set<String> getSet(CLDRLocale locale, String xpath) {
        DtdData dtdData =
                DtdData.getInstance(
                        DtdType.fromPath(xpath), CLDRConfig.getInstance().getCldrBaseDirectory());
        XPathParts xpp = XPathParts.getFrozenInstance(xpath);
        String el = xpp.getElement(-1);
        DtdData.Element element = dtdData.getElementFromName().get(el);
        DtdData.Attribute attribute = element.getAttributeNamed(LDMLConstants.ALT);
        Set<String> set = (attribute == null) ? null : attribute.getMatchLiterals();
        if (set != null && !set.isEmpty()) {
            set = removePathsAlreadyPresent(set, locale, xpath);
        }
        return set;
    }

    private Set<String> removePathsAlreadyPresent(
            Set<String> origSet, CLDRLocale locale, String origXpath) {
        CLDRFile cldrFile = getFile(locale);
        if (cldrFile == null) {
            return null;
        }
        Set<String> set = new TreeSet<>();
        for (String alt : origSet) {
            XPathParts xpp = XPathParts.getFrozenInstance(origXpath).cloneAsThawed();
            xpp.addAttribute(LDMLConstants.ALT, alt);
            String newXpath = xpp.toString();
            if (!cldrFile.isHere(newXpath)) { // compare STFactory.getPathsForFile()
                set.add(alt);
            }
        }
        return set;
    }

    private CLDRFile getFile(CLDRLocale locale) {
        return sm.getSTFactory().make(locale.getBaseName(), true);
    }

    private Response noAlt(String hexId, String alt) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new STError("XPath Hex ID " + hexId + " alt value " + alt + " not found"))
                .build();
    }

    private Response badFile(CLDRLocale locale) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(
                        new STError(
                                "Cannot add alt path: null CLDRFile for locale "
                                        + locale.getBaseName()))
                .build();
    }

    private Response alreadyExists(String newXpath) {
        return Response.status(Response.Status.NOT_ACCEPTABLE)
                .entity(new STError("Alt path already exists: " + newXpath))
                .build();
    }

    private Response badHandleVote(String newXpath, int status) {
        return Response.status(status)
                .entity(new STError("Cannot add alt path: handleVote failed for " + newXpath))
                .build();
    }
}
