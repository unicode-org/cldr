package org.unicode.cldr.web.api;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.SurveyMain;
import org.unicode.cldr.web.api.WhatisResponse.StandardCodeResult;
import org.unicode.cldr.web.api.WhatisResponse.XpathResult;

@Path("/whatis")
@Tag(name = "xpath", description = "APIs for XPath info")
public class Whatis {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Look up a code",
            description =
                    "Searches for codes containing the given string, with the given base locale")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Look up a code",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = WhatisResponse.class))),
            })
    public Response getWhatis(
            @Parameter(required = true, example = "jgo", schema = @Schema(type = SchemaType.STRING))
                    @QueryParam("q")
                    String q,
            @Parameter(
                            required = true,
                            example = "en_US",
                            schema = @Schema(type = SchemaType.STRING))
                    @QueryParam("loc")
                    String loc) {

        q = q.trim();
        if (q.isEmpty()) {
            Map<String, String> r = new TreeMap<>();
            r.put("err", "empty query");
            return Response.ok(r).build();
        }
        WhatisResponse what = new WhatisResponse(q);
        if (q.startsWith("\\u") || q.startsWith("=")) {
            searchPathsAndValues(what); // doesn't use loc
        } else {
            searchStandardCodesExact(what, loc);
            searchStandardCodesFull(what, loc);
        }
        // return Response.ok(what).type(MediaType.APPLICATION_JSON_TYPE).build();
        return Response.ok().entity(what).build();
    }

    private void searchPathsAndValues(WhatisResponse what) {
        String q = what.q;
        String q2 = com.ibm.icu.impl.Utility.unescape(q);
        if (q2.startsWith("=")) {
            q2 = q2.substring(1);
        }
        what.q2 = q2;
        SurveyMain sm = CookieSession.sm;
        for (CLDRLocale loc : SurveyMain.getLocalesSet()) {
            what.localesSearched++;
            CLDRFile f = null;
            try {
                f = sm.getSTFactory().make(loc.getBaseName(), false);
            } catch (Throwable t) {
                what.err =
                        t.toString() + " loading " + loc.getDisplayName() + " - " + loc.toString();
            }
            if (f == null) {
                continue;
            }
            XpathResult r = null;
            for (String xp : f) {
                String str = f.getStringValue(xp);
                what.pathsSearched++;
                if (str.contains(q2)) {
                    if (r == null) {
                        r = what.new XpathResult(loc);
                    }
                    r.addMatch(what.new PathAndValue(xp, str));
                }
            }
            if (r != null) {
                what.addXpath(r);
            }
        }
    }

    private void searchStandardCodesExact(WhatisResponse what, String loc) {
        StandardCodes sc = StandardCodes.make();
        String q = what.q.toLowerCase();
        for (String type : sc.getAvailableTypes()) {
            for (String code : sc.getAvailableCodes(type)) {
                List<String> v = sc.getFullData(type, code);
                if (v == null || v.isEmpty()) {
                    continue;
                }
                if (code.toLowerCase().equals(q)) {
                    StandardCodeResult r = what.new StandardCodeResult(type, code);
                    for (String s : v) {
                        r.addData(what.new StandardCodeResultDataItem(s, true));
                    }
                    what.addExact(r);
                } else {
                    for (String s : v) {
                        if (s != null && s.toLowerCase().equals(q)) {
                            StandardCodeResult r = what.new StandardCodeResult(type, code);
                            for (String s2 : v) {
                                boolean isWinner = s2.toLowerCase().equals(q);
                                r.addData(what.new StandardCodeResultDataItem(s2, isWinner));
                            }
                            what.addExact(r);
                        }
                    }
                }
            }
        }
    }

    private void searchStandardCodesFull(WhatisResponse what, String loc) {
        StandardCodes sc = StandardCodes.make();
        String q = what.q.toLowerCase();
        for (String type : sc.getAvailableTypes()) {
            for (String code : sc.getAvailableCodes(type)) {
                List<String> v = sc.getFullData(type, code);
                StringBuilder allMatch = new StringBuilder();
                if (v != null && !v.isEmpty()) {
                    for (String s : v) {
                        allMatch.append(s).append("\n");
                    }
                }
                if (!code.toLowerCase().equals(q)
                        && (code.toLowerCase().contains(q)
                                || allMatch.toString().toLowerCase().contains(q))) {
                    StandardCodeResult r = what.new StandardCodeResult(type, code);
                    if (v != null && !v.isEmpty()) {
                        for (String s : v) {
                            r.addData(what.new StandardCodeResultDataItem(s, true));
                        }
                    }
                    what.addFull(r);
                }
            }
        }
    }
}
