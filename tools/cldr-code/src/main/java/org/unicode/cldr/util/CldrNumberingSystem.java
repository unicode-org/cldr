package org.unicode.cldr.util;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;
import org.unicode.cldr.icu.LDMLConstants;

public enum CldrNumberingSystem {
    latin(null),
    defaultSystem("//ldml/numbers/defaultNumberingSystem"),
    nativeSystem("//ldml/numbers/otherNumberingSystems/native"),
    traditional("//ldml/numbers/otherNumberingSystems/traditional"),
    finance("//ldml/numbers/otherNumberingSystems/finance");

    public final String path;

    CldrNumberingSystem(String path) {
        this.path = path;
    }

    public static final String LATN_SYSTEM = "latn";
    private static final String LATIN_KIND = "Latin";

    /**
     * Get a map in which the keys are numbering system names like "latn", "arab", etc., and the
     * values are kinds like "default", "Latin", "native", etc. The first key is always the default
     * numbering system and is always mapped to "default".
     *
     * @param cldrFile the file for a locale
     * @return the map
     */
    public static LinkedHashMap<String, String> getMap(CLDRFile cldrFile) {

        LinkedHashMap<String, String> kindToPath = new LinkedHashMap<>();
        LinkedHashMap<String, String> systemToKind = new LinkedHashMap<>();
        kindToPath.put(LDMLConstants.DEFAULT, defaultSystem.path);
        // LATIN_KIND and LATN_SYSTEM are exceptionally linked without a path
        kindToPath.put(LATIN_KIND, LATN_SYSTEM);
        kindToPath.put(LDMLConstants.NATIVE, nativeSystem.path);
        kindToPath.put(LDMLConstants.TRADITIONAL, traditional.path);
        kindToPath.put(LDMLConstants.FINANCE, finance.path);
        for (String kind : kindToPath.keySet()) {
            String path = kindToPath.get(kind);
            String system = LATN_SYSTEM.equals(path) ? LATN_SYSTEM : cldrFile.getStringValue(path);
            if (system != null && !systemToKind.containsKey(system)) {
                systemToKind.put(system, kind);
            }
        }
        return systemToKind;
    }

    public static void writeLinks(Writer out, LinkedHashMap<String, String> numberingSystems)
            throws IOException {
        out.write(
                "Information is displayed below once for each of "
                        + numberingSystems.keySet().size()
                        + " numbering systems used for this locale: ");
        boolean needComma = false;
        for (String numberingSystem : numberingSystems.keySet()) {
            if (needComma) {
                out.write(", ");
            }
            // Enable clicking on the numbering system to scroll to it. Use onclick and
            // scrollIntoView instead of simple "href", which would fail in Survey Tool
            // due to its special handling of anchors.
            String link = linkId(numberingSystem);
            String onClick = "document.getElementById('" + link + "').scrollIntoView()";
            String kind = numberingSystems.get(numberingSystem);
            // For some reason, in the chart, this link lacks underline unless added here
            // explicitly. Ideally, style problems should be corrected in CSS rather than in
            // Java. A better solution is challenging since it needs to work both for
            // Survey Tool reports and for charts that are rendered outside of Survey Tool.
            out.write(
                    "<a style=\"text-decoration: underline;\" onclick=\""
                            + onClick
                            + "\">"
                            + numberingSystem
                            + " ("
                            + kind
                            + ")</a>");
            needComma = true;
        }
        out.write(".");
    }

    private static String linkId(String numberingSystem) {
        return "numbering-system-" + numberingSystem;
    }

    public static void writeHeader(
            LinkedHashMap<String, String> numberingSystems, String numberingSystem, Writer out)
            throws IOException {
        String kind = numberingSystems.get(numberingSystem);
        // Override the style here, since otherwise there may be no vertical space above the
        // header. A better solution is challenging since it needs to work both for
        // Survey Tool reports and for charts that are rendered outside of Survey Tool.
        out.write(
                "<h1 style=\"padding-top: 1em\" id=\""
                        + CldrNumberingSystem.linkId(numberingSystem)
                        + "\">Numbering System: "
                        + numberingSystem
                        + " ("
                        + kind
                        + ")</h1>");
    }
}
