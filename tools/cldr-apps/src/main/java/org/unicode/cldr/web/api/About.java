package org.unicode.cldr.web.api;

import com.ibm.icu.util.VersionInfo;
import java.util.Map;
import java.util.TreeMap;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.unicode.cldr.util.CLDRConfigImpl;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.SurveyMain;

@Path("/about")
@Tag(name = "about", description = "APIs for the About panel")
public class About {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get ST info",
            description = "Returns detailed information about the Survey Tool")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Details about the Survey Tool",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        type = SchemaType.OBJECT,
                                                        example =
                                                                "{\n"
                                                                        + "  \"CLDR_DATA_HASH\": \"f51da9c87d20b8d1236f0ea139a90ab49a879d65\",\n"
                                                                        + "  \"CLDR_CODE_HASH\": \"c27bb5ed340b63d4aa9fcb1ed948767d3a9869f3\",\n"
                                                                        + "  \"GEN_VERSION\": \"39\",\n"
                                                                        + "  \"ICU_VERSION\": \"68.1.0.0\",\n"
                                                                        + "  \"TRANS_HINT_LANGUAGE_NAME\": \"English\",\n"
                                                                        + "  \"TRANS_HINT_LOCALE\": \"en-ZZ\",\n"
                                                                        + "  \"java_vendor\": \"N/A\",\n"
                                                                        + "  \"java_version\": \"15.0.1\",\n"
                                                                        + "  \"java_vm_name\": \"OpenJDK 64-Bit Server VM\",\n"
                                                                        + "  \"java_vm_vendor\": \"Oracle Corporation\",\n"
                                                                        + "  \"java_vm_version\": \"15.0.1+9\",\n"
                                                                        + "  \"os_arch\": \"x86_64\",\n"
                                                                        + "  \"os_name\": \"Mac OS X\",\n"
                                                                        + "  \"os_version\": \"10.16\",\n"
                                                                        + "  \"serverInfo\": \"IBM WebSphere Liberty/21.0.0.1\",\n"
                                                                        + "  \"servletVersion\": \"4.0\"\n"
                                                                        + "}\n"
                                                                        + "")))
            })
    public Response getAbout() {
        Map<String, String> r = new TreeMap<>();
        String props[] = {
            "java.version", "java.vendor", "java.vm.version", "java.vm.vendor",
            "java.vm.name", "os.name", "os.arch", "os.version"
        };
        for (int i = 0; i < props.length; i++) {
            r.put(props[i].replace('.', '_'), java.lang.System.getProperty(props[i]));
        }
        r.put("GEN_VERSION", CLDRFile.GEN_VERSION);
        r.put("OLD_VERSION", SurveyMain.getOldVersion());
        r.put("ICU_VERSION", VersionInfo.ICU_VERSION.toString());
        try {
            ServletContext sc = CookieSession.sm.getServletContext();
            if (sc != null) {
                r.put("serverInfo", sc.getServerInfo());
                r.put("servletVersion", sc.getMajorVersion() + "." + sc.getMinorVersion());
            }
        } catch (Throwable t) {
            r.put("serverInfo", "Exception: " + t);
        }
        r.put("TRANS_HINT_LOCALE", SurveyMain.TRANS_HINT_LOCALE.toLanguageTag());
        r.put("TRANS_HINT_LANGUAGE_NAME", SurveyMain.TRANS_HINT_LANGUAGE_NAME);
        CLDRConfigImpl configImpl = CLDRConfigImpl.getInstance();
        if (SurveyMain.isConfigSetup) {
            for (String k : org.unicode.cldr.util.CLDRConfigImpl.ALL_GIT_HASHES) {
                r.put(k, configImpl.getProperty(k));
            }
        }
        if (SurveyMain.isDbSetup) {
            org.unicode.cldr.web.DBUtils d = org.unicode.cldr.web.DBUtils.getInstance();
            if (d != null) {
                r.put("hasDataSource", Boolean.toString(d.hasDataSource()));
                r.put("dbKind", "MySQL");
                r.put("dbInfo", d.getDBInfo());
            }
        }
        putMemoryStats(r);
        return Response.ok(r).build();
    }

    private void putMemoryStats(Map<String, String> r) {
        Runtime rt = Runtime.getRuntime();
        r.put("memoryTotal", Long.toString(rt.totalMemory()));
        r.put("memoryMax", Long.toString(rt.maxMemory()));
        r.put("memoryFree", Long.toString(rt.freeMemory()));
    }
}
