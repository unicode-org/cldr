package org.unicode.cldr.web;

import javax.servlet.ServletContext;

import org.unicode.cldr.util.CLDRConfigImpl;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRURLS;

import com.ibm.icu.util.VersionInfo;

public class AboutST {
    public static void getJson(SurveyJSONWrapper r, SurveyMain sm) {
        String props[] = {
            "java.version", "java.vendor", "java.vm.version", "java.vm.vendor",
            "java.vm.name", "os.name", "os.arch", "os.version"};
        for (int i = 0; i < props.length; i++) {
            r.put(props[i].replace('.', '_'), java.lang.System.getProperty(props[i]));
        }
        r.put("GEN_VERSION", CLDRFile.GEN_VERSION);
        r.put("ICU_VERSION", VersionInfo.ICU_VERSION);
        ServletContext sc = sm.getServletContext();
        r.put("serverInfo", sc.getServerInfo());
        r.put("servletMajorVersion", sc.getMajorVersion());
        r.put("servletMinorVersion", sc.getMinorVersion());
        r.put("TRANS_HINT_LOCALE", SurveyMain.TRANS_HINT_LOCALE.toLanguageTag());
        r.put("TRANS_HINT_LANGUAGE_NAME", SurveyMain.TRANS_HINT_LANGUAGE_NAME);
        if (SurveyMain.isConfigSetup) {
            for (String k : org.unicode.cldr.util.CLDRConfigImpl.ALL_GIT_HASHES) {
                r.put(k, CLDRURLS.gitHashToLink(CLDRConfigImpl.getInstance().getProperty(k)));
            }
        }
        if (SurveyMain.isDbSetup) {
            org.unicode.cldr.web.DBUtils d = org.unicode.cldr.web.DBUtils.getInstance();
            if (d != null) {
                r.put("hasDataSource", d.hasDataSource());
                r.put("dbKind", DBUtils.getDBKind());
                r.put("dbInfo", d.getDBInfo());
            }
        }
    }
}
