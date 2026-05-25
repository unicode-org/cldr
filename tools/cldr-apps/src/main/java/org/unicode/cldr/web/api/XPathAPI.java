package org.unicode.cldr.web.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
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
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.PathHeader;
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

    /**
     * @see {@link PathHeader}
     */
    public final class PathHeaderInfo {
        public String sectionId;
        public String pageId;
        public int headerOrder;
        public String header;
        public long codeOrder;
        public String codeSubPrimaryOrder;
        public int codeSubSecondaryOrder;
        public String code;
        public String path;
        public String status;

        public PathHeaderInfo(final String path, PathHeader ph) {
            sectionId = ph.getSectionId().toString();
            pageId = ph.getPageId().toString();
            header = ph.getHeader();
            headerOrder = ph.getHeaderOrder();
            code = ph.getCode();
            codeOrder = ph.getCodeOrder();
            codeSubPrimaryOrder = ph.getCodeSubPrimaryOrder();
            codeSubSecondaryOrder = ph.getCodeSubSecondaryOrder();
            status = ph.getSurveyToolStatus().toString();
            this.path = path;
        }
    }

    @GET
    @Path("/ph/{path:.+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary =
                    "Fetch PathHeader info for an XPath, such as /ph//ldml/numbers/minimumGroupingDigits")
    @APIResponses({
        @APIResponse(responseCode = "404", description = "XPath not found"),
        @APIResponse(
                responseCode = "200",
                description = "PathHeader for a given path",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = PathHeaderInfo.class)))
    })
    public Response getPathHeaderByXPath(
            @Parameter(
                            required = true,
                            example = "/ldml/numbers/minimumGroupingDigits",
                            schema = @Schema(type = SchemaType.STRING))
                    @PathParam("path")
                    String path) {
        // the parsing is flexible here.
        // all of the following resolve to the same result
        // Escaped in the explorer:
        // - /cldr-apps/api/xpath/ph/%2F%2Fldml%2Fnumbers%2FminimumGroupingDigits
        // Elided slash:
        // - /cldr-apps/api/xpath/ph/ldml/numbers/minimumGroupingDigits
        // Keep the "//ldml" prefix for ease of URL generation
        // - /cldr-apps/api/xpath/ph//ldml/numbers/minimumGroupingDigits
        //
        if (!path.startsWith("//")) {
            if (path.startsWith("/")) {
                path = "/" + path;
            } else {
                path = "//" + path;
            }
        }
        path = CLDRFile.getDistinguishingXPath(path, null);
        try {
            return Response.ok(new PathHeaderInfo(path, PathHeader.getFactory().fromPath(path)))
                    .build();
        } catch (Throwable e) {
            System.err.println("For path " + path);
            e.printStackTrace();
            return Response.status(404).build();
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

    private final XPathTable getXPathTable() {
        return CookieSession.sm.xpt;
    }
}
