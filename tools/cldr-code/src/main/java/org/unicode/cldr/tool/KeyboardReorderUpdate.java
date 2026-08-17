package org.unicode.cldr.tool;

import com.ibm.icu.lang.UScript;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.TempPrintWriter;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XMLFileReader.SimpleHandler;
import org.unicode.cldr.util.XPathParts;

public class KeyboardReorderUpdate extends SimpleHandler implements Runnable {
    private static final String REORDERS_PATH = "//keyboard/reorders";
    private static final String REORDERS_FINAL_PATH = "//keyboard/transforms[@type=\"final\"]";
    private static final String REORDERS_DATA_PREFIX = "//keyboard/reorders/reorder";
    private static final String REORDERS_FINAL_PREFIX =
            "//keyboard/transforms[@type=\"final\"]/transform";

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(KeyboardReorderUpdate.class.getSimpleName());

    final File reorderFile;

    public KeyboardReorderUpdate(String f) throws IOException {
        reorderFile = new File(f);
        if (!reorderFile.isFile() || !reorderFile.canRead()) {
            throw new IOException("Could not read reorderFile " + f);
        }
    }

    /**
     * This file is https://github.com/keymanapp/ldml-keyboards-dev/blob/master/shared/reorders.xml
     * or update thereof
     *
     * @param reorderFile
     */
    public static void updateFrom(final String reorderFile) throws IOException {
        new KeyboardReorderUpdate(reorderFile).run();
    }

    @Override
    public void run() {
        if (!IMPORT_PATH.toFile().isDirectory()) {
            throw new IllegalArgumentException("Not a dir : " + IMPORT_PATH);
        }
        logger.info("Reading " + reorderFile.getAbsolutePath());
        XMLFileReader.processPathValues(reorderFile.getAbsolutePath(), false, this);
        finalizeScript(); // one finalize at end

        writeAll();
    }

    @Override
    public void handleComment(String path, String comment) {
        if (path.equals(REORDERS_PATH) || path.equals(REORDERS_FINAL_PATH)) {
            handleScriptComment(path, comment.trim());
        } else {
            logger.warning(path + " <!--" + comment + "-->");
        }
    }

    private void handleScriptComment(String path, String script) {
        if (script.startsWith("Unknowns")) {
            logger.info("Ignored: " + path + " " + script);
            return;
        }
        int code = UScript.getCodeFromName(script);
        if (code == UScript.INVALID_CODE) {
            throw new RuntimeException("Unhandled script: " + script);
        } else {
            handleScript(path, code);
        }
    }

    private void handleScript(String path, int code) {
        handleScript(path, code, UScript.getShortName(code), UScript.getName(code));
    }

    /** current location, for output */
    private String currentInfo() {
        return String.format("%s (%s): ", currentScript, currentScriptName);
    }

    private void handleScript(String path, int code, String shortName, String name) {
        finalizeScript();

        currentParentPath = path;
        currentScript = shortName;
        currentScriptName = name;
        logger.info(path + " SCRIPT: " + currentInfo());
    }

    String currentScript = null;
    String currentScriptName = null;
    String currentParentPath = null;
    List<String> currentPaths = new ArrayList<>();

    /** finalize anything pending and clears */
    private void finalizeScript() {
        if (!currentPaths.isEmpty()) {
            if (currentScript == null) {
                throw new IllegalArgumentException(
                        "reorder paths before the first <!-- script --> comment!");
            }
        }
        // clear
        clear();
    }

    private class ScriptReorders {
        List<ScriptReorder> reorders = new ArrayList<>();
        List<ScriptFinal> finals = new ArrayList<>();

        boolean isEmpty() {
            return reorders.isEmpty() && finals.isEmpty();
        }

        public void addFinalPath(String path) {
            finals.add(new ScriptFinal(path));
        }

        public void addReorderPath(String path) {
            reorders.add(new ScriptReorder(path));
        }

        public void writeTransformGroups(PrintWriter pw) {
            if (!reorders.isEmpty()) {
                pw.println("\t<transformGroup>");
                reorders.forEach(r -> r.write(pw));
                pw.println("\t</transformGroup>");
            }
            if (!finals.isEmpty()) {
                pw.println("\t<transformGroup>");
                finals.forEach(f -> f.write(pw));
                pw.println("\t</transformGroup>");
            }
        }
    }

    private abstract class ScriptTransformRule {
        abstract void write(PrintWriter pw);

        protected void write(PrintWriter pw, XPathParts xpp) {
            pw.print("\t\t");
            xpp.writeLastElement(pw, XPathParts.XML_NO_VALUE);
        }
    }

    private final class ScriptReorder extends ScriptTransformRule {
        public String order;
        public String from;
        public String tertiary;
        public String tertiary_base;
        private String before;

        // private String pre_base;

        public ScriptReorder(final String path) {
            XPathParts xpp = XPathParts.getFrozenInstance(path);
            order = xpp.getAttributeValue(-1, "order");
            from = xpp.getAttributeValue(-1, "from");
            before = xpp.getAttributeValue(-1, "before");
            // pre_base = xpp.getAttributeValue(-1, "pre_base");
            tertiary = xpp.getAttributeValue(-1, "tertiary");
            tertiary_base = xpp.getAttributeValue(-1, "tertiary_base");
        }

        public void write(PrintWriter pw) {
            XPathParts xpp =
                    XPathParts.getFrozenInstance("//transforms/transformGroup/reorder")
                            .cloneAsThawed();
            xpp.setAttribute(-1, "from", deQuadifyEscapes(from));
            xpp.setAttribute(-1, "order", order);
            xpp.setAttribute(-1, "before", deQuadifyEscapes(before));
            // did not exist
            // xpp.setAttribute(-1, "preBase", pre_base);
            xpp.setAttribute(-1, "tertiary", tertiary);
            xpp.setAttribute(-1, "tertiaryBase", tertiary_base); // note change from 2.0 to 3.0 name
            write(pw, xpp);
        }
    }

    private final class ScriptFinal extends ScriptTransformRule {
        public String from;
        public String to;

        public ScriptFinal(final String path) {
            XPathParts xpp = XPathParts.getFrozenInstance(path);
            from = xpp.getAttributeValue(-1, "from");
            to = xpp.getAttributeValue(-1, "to");
        }

        public void write(PrintWriter pw) {
            XPathParts xpp =
                    XPathParts.getFrozenInstance("//transforms/transformGroup/transform")
                            .cloneAsThawed();
            xpp.setAttribute(-1, "from", deQuadifyEscapes(from));
            xpp.setAttribute(-1, "to", deQuadifyEscapes(to));
            write(pw, xpp);
        }
    }

    private Map<String, ScriptReorders> scriptMap = new TreeMap<>();

    private void clear() {
        currentScript = null;
        currentScriptName = null;
        currentParentPath = null;
        currentPaths.clear();
    }

    @Override
    public void handlePathValue(String path, String value) {
        if (path.startsWith(REORDERS_DATA_PREFIX)) {
            if (value == null || !value.isBlank()) {
                throw new IllegalArgumentException(
                        "in " + currentInfo() + " unexpected reorder value " + path + "=" + value);
            }
            handleReorderPath(path);
        } else if (path.startsWith(REORDERS_FINAL_PREFIX)) {
            if (value == null || !value.isBlank()) {
                throw new IllegalArgumentException(
                        "in " + currentInfo() + " unexpected final value " + path + "=" + value);
            }
            handleFinalPath(path);
        } else {
            logger.info(currentInfo() + " " + path + "=" + value);
        }
    }

    private void handleReorderPath(String path) {
        logger.finer(currentInfo() + path);
        getScript(currentScript).addReorderPath(path);
    }

    private ScriptReorders getScript(String s) {
        if (s == null) throw new NullPointerException("Null script");
        return scriptMap.computeIfAbsent(s, (k) -> new ScriptReorders());
    }

    private void handleFinalPath(String path) {
        logger.finer(currentInfo() + path);
        getScript(currentScript).addFinalPath(path);
    }

    private void writeAll() {
        for (final String s : scriptMap.keySet()) {
            ScriptReorders reorders = getScript(s);
            logger.info(
                    "Script "
                            + s
                            + " - "
                            + reorders.reorders.size()
                            + " reorders and "
                            + reorders.finals.size()
                            + " finals");
            write(s, reorders);
        }
    }

    /** write one script */
    private void write(String s, ScriptReorders reorders) {
        try (TempPrintWriter pw = new TempPrintWriter(getOutputFile(s)); ) {
            pw.skipCopyright(true);
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            pw.println("<!--\n" + CldrUtility.getCopyrightString() + "\n-->\n");
            pw.println(
                    "<!-- Importable reorders for "
                            + s
                            + " :  Generated by KeyboardReorderUpdate -->");
            pw.println("<!DOCTYPE transforms SYSTEM \"../dtd/ldmlKeyboard3.dtd\">");
            pw.println("<transforms type=\"simple\">");
            reorders.writeTransformGroups(pw.asPrintWriter());
            pw.println("</transforms>");
        }
    }

    private final Path BASE_PATH = new File(CLDRPaths.BASE_DIRECTORY).toPath();
    private final Path IMPORT_PATH = BASE_PATH.resolve("keyboards/import/");

    private File getOutputFile(String s) {
        return IMPORT_PATH.resolve("reorder-" + s + ".xml").toFile();
    }

    /**
     * convert /uABCD to /u{ABCD} </code>
     *
     * @return
     */
    public static final String deQuadifyEscapes(final String in) {
        if (in == null) return in;
        return in.replaceAll("\\\\u([0-9A-F]{4})", "\\\\u{$1}");
    }
}
