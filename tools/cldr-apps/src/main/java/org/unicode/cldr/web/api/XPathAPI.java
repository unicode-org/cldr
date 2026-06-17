package org.unicode.cldr.web.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
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
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.XPathTable;

@Path("/xpath")
@Tag(name = "xpath", description = "APIs for XPath information")
public class XPathAPI {

    @Schema(description = "Return value for XPath query")
    public final class XPathInfo {
        @Schema(description = "XPath (String) value")
        private String xpath;

        @Schema(description = "Decimal (serial number). Not portable across instances.")
        private Integer decimalId;

        @Schema(description = "Hex (hash) of XPath.")
        private String hexId;

        public String getXpath() {
            return xpath;
        }

        public XPathInfo setXpath(String xpath) {
            this.xpath = xpath;
            return this;
        }

        public Integer getDecimalId() {
            return decimalId;
        }

        public XPathInfo setDecimalId(Integer decimalId) {
            this.decimalId = decimalId;
            return this;
        }

        public String getHexId() {
            return hexId;
        }

        public XPathInfo setHexId(String hexId) {
            this.hexId = hexId;
            return this;
        }
    }

    @POST
    @Path("/str")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Fetch hex and decimal id from XPath string",
            description = "Looks up the ids for a specific XPath string.")
    public Response getByString(XPathRequest request) {
        final String xpath = request.str;
        final XPathTable xpt = getXPathTable();
        final int decId = xpt.peekByXpath(xpath);
        if (decId == XPathTable.NO_XPATH) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new STError("XPath " + xpath + " not found"))
                    .build();
        }
        final String hexId = xpt.getStringIDString(decId);
        return Response.ok(new XPathInfo().setDecimalId(decId).setHexId(hexId).setXpath(xpath))
                .build();
    }

    @GET
    @Path("/hex/{hexId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Fetch XPath string from hex id",
            description = "Looks up a specific hex ID.")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "404",
                        description = "XPath not found",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = STError.class))),
                @APIResponse(
                        responseCode = "200",
                        description = "Details about a given XPath",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = XPathInfo.class)))
            })
    public Response getByHex(
            @Parameter(
                            required = true,
                            example = "1234abcd",
                            schema = @Schema(type = SchemaType.STRING))
                    @PathParam("hexId")
                    String hexId) {

        // the actual implementation
        XPathTable xpt = getXPathTable();
        final String xpath = xpt.getByStringID(hexId);
        if (xpath == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new STError("XPath " + hexId + " not found"))
                    .build();
        }
        return Response.ok(
                        new XPathInfo()
                                .setHexId(hexId)
                                .setDecimalId(xpt.getXpathIdFromStringId(hexId))
                                .setXpath(xpath))
                .build();
    }

    @GET
    @Path("/dec/{decId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Fetch XPath string from decimal id",
            description = "Looks up a specific decimal ID.")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "404",
                        description = "XPath not found",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = STError.class))),
                @APIResponse(
                        responseCode = "200",
                        description = "Details about a given XPath",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = XPathInfo.class)))
            })
    public Response getByDecimal(
            @Parameter(
                            required = true,
                            example = "5678",
                            schema = @Schema(type = SchemaType.INTEGER))
                    @PathParam("decId")
                    int decId) {

        XPathTable xpt = getXPathTable();
        String xpath = null;
        try {
            xpath = xpt.getById(decId);
        } catch (RuntimeException e) {
            /*
             * Don't report the exception. This happens when it simply wasn't found.
             * Possibly getById, or some version of it, should not throw an exception.
             */
        }
        if (xpath == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new STError("XPath #" + decId + " not found"))
                    .build();
        }
        return Response.ok(
                        new XPathInfo()
                                .setDecimalId(decId)
                                .setHexId(xpt.getStringIDString(decId))
                                .setXpath(xpath))
                .build();
    }

    public final class LocaleCoverageInfo {
        @Schema(description = "map from coverage level to array of xpath string ids")
        /**
         * Example: "CORE": [ "6cf943e652b01478", ], "MODERATE": [ "4a74ce35a9fa3778", ], "MODERN":
         * [ "11d5a244a70d1edd", "3a6f1bf0dd41ae4c", "3ebe0f49892a8a85", ],
         */
        public Map<String, Collection<String>> coverageToXpaths = new TreeMap<>();

        /** Compute all coverage levels for a locale. This is cached client-side. */
        public LocaleCoverageInfo(String locale) {
            SupplementalDataInfo sdi = CLDRConfig.getInstance().getSupplementalDataInfo();
            CLDRFile f = CookieSession.sm.getDiskFactory().make(locale, true);
            CoverageLevel2 cov = sdi.getCoverageLevelInfo(locale);
            // get all XPaths
            for (final String x : f.fullIterable()) {
                // get the coverage level
                final Level l = cov.getLevel(x);
                // get the set of XPaths at this coverage level
                final Collection<String> pathsAtThisCoverageLevel =
                        coverageToXpaths.computeIfAbsent(
                                l.name(), (String key) -> new ArrayList<>());
                // add the XPath StringId to the list
                pathsAtThisCoverageLevel.add(XPathTable.getStringIDString(x));
            }
        }
    }

    @GET
    @Path("/coverage/{locale}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Get all coverage buckets for a locale")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "404",
                        description = "Locale not found",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = STError.class))),
                @APIResponse(
                        responseCode = "200",
                        description = "Details about a given Locale",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(implementation = LocaleCoverageInfo.class)))
            })
    public Response getCoverageForLocale(
            @Parameter(required = true, example = "mt", schema = @Schema(type = SchemaType.STRING))
                    @PathParam("locale")
                    String locale,
            @HeaderParam(Auth.SESSION_HEADER) String session) {
        // somewhat expensive operation, so we'll verify session
        // Verify session
        final CookieSession mySession = Auth.getSession(session);
        if (mySession == null) {
            return Auth.noSessionResponse();
        }
        // somewhat expensive, so we'll require a user, for now
        if (mySession.user == null) {
            return Response.status(Response.Status.FORBIDDEN.getStatusCode()).build();
        }
        if (!CookieSession.sm.isValidLocale(CLDRLocale.getExistingInstance(locale))) {
            // locale not found
            return Response.status(404).build();
        }

        return Response.ok(new LocaleCoverageInfo(locale)).build();
    }

    private final XPathTable getXPathTable() {
        return CookieSession.sm.xpt;
    }
}
