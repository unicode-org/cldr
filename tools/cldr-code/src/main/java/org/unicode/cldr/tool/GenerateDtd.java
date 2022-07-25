package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTool;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdData.Attribute;
import org.unicode.cldr.util.DtdData.Element;
import org.unicode.cldr.util.DtdData.Mode;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.TempPrintWriter;

import com.ibm.icu.text.CaseMap;
import com.ibm.icu.text.ListFormatter;
import com.ibm.icu.text.ListFormatter.Type;
import com.ibm.icu.text.ListFormatter.Width;

@CLDRTool(alias = "generate-dtd", description = "BRS: Reformat all DTDs")
public class GenerateDtd {

    private static final CaseMap.Title TO_TITLE_WHOLE_STRING_NO_LOWERCASE = CaseMap.toTitle().wholeString().noLowercase();

    public static void main(String[] args) throws IOException {
        Path dtd2md = CLDRPaths.getDtd2Md();
        // make sure the paths exist
        dtd2md.toFile().mkdirs();
        //System.setProperty("show_all", "true");
        for (DtdType type : DtdType.values()) {
            if (type == DtdType.ldmlICU) {
                continue;
            }
            DtdData data = DtdData.getInstance(type);
            String name = type.toString();
            if (!name.startsWith("ldml")) {
                name = "ldml" + TO_TITLE_WHOLE_STRING_NO_LOWERCASE.apply(Locale.ROOT, null, name);
                if (name.endsWith("Data")) {
                    name = name.substring(0, name.length() - 4);
                }
            }
            try (TempPrintWriter out = TempPrintWriter.openUTF8Writer(CLDRPaths.BASE_DIRECTORY + type.dtdPath)) {
                out.println(data);
                System.err.println("Wrote: " + CLDRPaths.BASE_DIRECTORY + type.dtdPath);
            }
            // create markdown
            final File mdFile = dtd2md.resolve("./elements_" + name + ".md").toFile();
            writeMarkdown(mdFile, data, type);
        }
    }

    private static void writeMarkdown(File mdFile, DtdData data, DtdType type) {
        try (TempPrintWriter out = TempPrintWriter.openUTF8Writer(mdFile.getAbsolutePath())) {
            out.println("# DTD data: " + type);
            out.println();
            for (final DtdData.Element e : data.getElements()) {
                out.println("## Element: " + e.getName());
                out.println();
                out.println("> <small>");
                out.println(">");
                out.println("> Parents: " + getParents(data, e));
                out.println(">");
                out.println("> Children: " + getChildren(e));
                out.println(">");
                out.println("> Occurrence: " + getOccurrence(e));
                out.println("> </small>");
                out.println();
                final Set<Attribute> attrs = e.getAttributes().keySet();
                if (!attrs.isEmpty()) {
                    final TreeSet<Attribute> ts = new TreeSet<>(new Comparator<Attribute>() {
                        @Override
                        public int compare(Attribute o1, Attribute o2) {
                            return o1.getName().compareTo(o2.getName());
                        }
                    });
                    ts.addAll(attrs);
                    for (final Attribute a : ts) {
                        out.println("_Attribute:_ " + escapeStr(a.getName()) + getAttr(a));
                        out.println();
                    }
                }
                out.println("* * *");
                out.println();
            }
            System.err.println("Wrote: " + mdFile.getPath());
        }
    }

    private static String getAttr(Attribute a) {
        if (a.mode == Mode.FIXED) {
            return " (fixed by DTD)";
        } else if (a.mode == Mode.OPTIONAL) {
            return ""; // " (optional)";
        } else if (a.mode == Mode.REQUIRED) {
            return " (required)";
        } else {
            return "";
        }
    }

    private static String getOccurrence(Element e) {
        return "?, ?";
    }

    private static String getChildren(Element e) {
        return setToString(e.getChildren().keySet().stream());
    }

    private static String setToString(Stream<Element> children) {
        final String ret = lf.format(
            children
                .map(Element::getName)
                .sorted()
                .map(GenerateDtd::linkifyElementStr)
                .collect(Collectors.toList()));
        if (ret.isEmpty()) {
            return "_none_";
        } else {
            return ret;
        }
    }

    private static String getParents(DtdData data, final Element e) {
        return setToString(data.getElements()
            .stream()
            .filter(p -> p.getChildren().containsKey(e)));
    }

    final static ListFormatter lf = ListFormatter.getInstance(Locale.ENGLISH, Type.AND, Width.NARROW);

    /**
     * Escape a string for markdown
     */
    public static String escapeStr(String t) {
        return "`" + t + "`";
    }

    /**
     * Linkify an element to a link to `#Element_t`
     * @param t
     * @return
     */
    public static String linkifyElementStr(String t) {
        if (t.equals("special")) {
            // this one is... special!
            return "[_special_](tr35.md#special)";
        }
        return "[" + t + "](#Element_" + t + ")";
    }
}
