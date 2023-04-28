package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.util.VersionInfo;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.tool.ToolConstants.ChartStatus;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdData.Attribute;
import org.unicode.cldr.util.DtdData.AttributeStatus;
import org.unicode.cldr.util.DtdData.Element;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.SupplementalDataInfo;

/**
 * Changed ShowDtdDiffs into a chart.
 *
 * @author markdavis
 */
public class ChartDtdDelta extends Chart {

    private static final Splitter SPLITTER_SPACE = Splitter.on(' ');

    private static final String NEW_PREFIX = "+";

    private static final String DEPRECATED_PREFIX = "⊖";
    private static final String UNDEPRECATED_PREFIX = "⊙"; // no occurances yet

    private static final String ORDERED_SIGN = "⇣";
    private static final String UNORDERED_SIGN = "⇟";

    private static final String TECHPREVIEW_SIGN = "🅟";
    private static final String UNTECHPREVIEW_SIGN = "ⓟ";

    private static final Set<String> OMITTED_ATTRIBUTES = Collections.singleton("⊕");

    public static void main(String[] args) {
        new ChartDtdDelta().writeChart(null);
    }

    @Override
    public String getDirectory() {
        return FormattedFileWriter.CHART_TARGET_DIR;
    }

    @Override
    public String getTitle() {
        return "DTD Deltas";
    }

    @Override
    public String getExplanation() {
        return "<p>Changes to the LDML DTDs over time.</p>\n"
                + "<ul>\n"
                + "<li>New elements or attributes are indicated with a + sign, and newly deprecated ones with a ⊖ sign.</li>\n"
                + "<li>Element attributes are abbreviated as ⊕ where is no change to them, "
                + "but the element is newly the child of another.</li>\n"
                + "<li>LDML DTDs have augmented data:\n"
                + "<ul><li>Attribute status is marked by: "
                + AttributeStatus.distinguished.shortName
                + "="
                + AttributeStatus.distinguished
                + ", "
                + AttributeStatus.value.shortName
                + "="
                + AttributeStatus.value
                + ", or "
                + AttributeStatus.metadata.shortName
                + "="
                + AttributeStatus.metadata
                + ".</li>\n"
                + "<li>Attribute value constraints are marked with ⟨…⟩ (for DTD constraints) and ⟪…⟫ (for augmented constraints, added in v35.0).</li>\n"
                + "<li>Changes in status or constraints are shown with ➠, with identical sections shown with ….</li>\n"
                + "<li>Newly ordered elements are indicated with "
                + ORDERED_SIGN
                + "; newly unordered with "
                + UNORDERED_SIGN
                + ".</li>\n"
                + "<li>Newly tech-preview items are marked with "
                + TECHPREVIEW_SIGN
                + "; newly graduated from tech preview with "
                + UNTECHPREVIEW_SIGN
                + ".</li>\n"
                + "<li>The following elements are skipped: "
                + SKIP_ELEMENTS
                + " and "
                + SKIP_TYPE_ELEMENTS
                + "</li>\n"
                + "<li>The following attributes are skipped: "
                + SKIP_ATTRIBUTES
                + " and "
                + SKIP_ATTRIBUTE_MATCHES
                + "</li>\n"
                + "</ul></li></ul>\n"
                + "<p>For more information, see the LDML spec.</p>";
    }

    @Override
    public void writeContents(FormattedFileWriter pw) throws IOException {
        TablePrinter tablePrinter =
                new TablePrinter()
                        .addColumn(
                                "Version",
                                "class='source'",
                                CldrUtility.getDoubleLinkMsg(),
                                "class='source'",
                                true)
                        .setSortPriority(0)
                        .setSortAscending(false)
                        .setBreakSpans(true)
                        .addColumn("Dtd Type", "class='source'", null, "class='source'", true)
                        .setSortPriority(1)
                        .addColumn(
                                "Intermediate Path", "class='source'", null, "class='target'", true)
                        .setSortPriority(2)
                        .addColumn("Element", "class='target'", null, "class='target'", true)
                        .setSpanRows(false)
                        .addColumn("Attributes", "class='target'", null, "class='target'", true)
                        .setSpanRows(false);

        String last = null;

        for (String current :
                ToolConstants.CHART_STATUS != ChartStatus.release
                        ? ToolConstants.CLDR_RELEASE_AND_DEV_VERSION_SET
                        : ToolConstants.CLDR_RELEASE_VERSION_SET) {
            System.out.println("DTD delta: " + current);
            final boolean finalVersion = current.equals(ToolConstants.DEV_VERSION);
            String currentName = finalVersion ? ToolConstants.CHART_DISPLAY_VERSION : current;
            for (DtdType type : TYPES) {
                String firstVersion = type.firstVersion; // FIRST_VERSION.get(type);
                if (firstVersion != null
                        && current != null
                        && current.compareTo(firstVersion) < 0) {
                    // skip if current is too old to have “type”
                    continue;
                }
                DtdData dtdCurrent = null;
                try {
                    dtdCurrent =
                            DtdData.getInstance(
                                    type,
                                    finalVersion
                                            // && ToolConstants.CHART_STATUS !=
                                            // ToolConstants.ChartStatus.release
                                            ? null
                                            : current);
                } catch (Exception e) {
                    if (!(e.getCause() instanceof FileNotFoundException)) {
                        throw e;
                    }
                    System.out.println(e.getMessage() + ", " + e.getCause().getMessage());
                    continue;
                }
                DtdData dtdLast = null;
                if (last != null && (firstVersion == null || last.compareTo(firstVersion) >= 0)) {
                    // only read if last isn’t too old to have “type”
                    dtdLast = DtdData.getInstance(type, last);
                }
                diff(currentName, dtdLast, dtdCurrent);
            }
            last = current;
            if (current.contentEquals(ToolConstants.CHART_VERSION)) {
                break;
            }
        }

        for (DiffElement datum : data) {
            tablePrinter
                    .addRow()
                    .addCell(datum.getVersionString())
                    .addCell(datum.dtdType)
                    .addCell(datum.newPath)
                    .addCell(datum.newElement)
                    .addCell(datum.attributeNames)
                    .finishRow();
        }
        pw.write(tablePrinter.toTable());
        pw.write(Utility.repeat("<br>", 50));
        try (PrintWriter tsvFile =
                FileUtilities.openUTF8Writer(
                        CLDRPaths.CHART_DIRECTORY + "/tsv/", "dtd_deltas.tsv")) {
            tablePrinter.toTsv(tsvFile);
        }
    }

    static final String NONE = " ";

    static final SupplementalDataInfo SDI = CLDRConfig.getInstance().getSupplementalDataInfo();

    static Set<DtdType> TYPES = EnumSet.allOf(DtdType.class);

    static {
        TYPES.remove(DtdType.ldmlICU);
    }

    static final Map<DtdType, String> FIRST_VERSION = new EnumMap<>(DtdType.class);

    static {
        FIRST_VERSION.put(DtdType.ldmlBCP47, "1.7.2");
        FIRST_VERSION.put(DtdType.keyboard, "22.1");
    }

    private void diff(String prefix, DtdData dtdLast, DtdData dtdCurrent) {
        Map<String, Element> oldNameToElement =
                dtdLast == null ? Collections.emptyMap() : dtdLast.getElementFromName();
        checkNames(
                prefix,
                dtdCurrent,
                dtdLast,
                oldNameToElement,
                "/",
                dtdCurrent.ROOT,
                new HashSet<Element>(),
                false);
    }

    static final DtdType DEBUG_DTD = null; // set to enable
    static final String DEBUG_ELEMENT = "lias";
    static final boolean SHOW = false;

    @SuppressWarnings("unused")
    private void checkNames(
            String version,
            DtdData dtdCurrent,
            DtdData dtdLast,
            Map<String, Element> oldNameToElement,
            String path,
            Element element,
            HashSet<Element> seen,
            boolean showAnyway) {
        String name = element.getName();

        if (SKIP_ELEMENTS.contains(name)) {
            return;
        }
        if (SKIP_TYPE_ELEMENTS.containsEntry(dtdCurrent.dtdType, name)) {
            return;
        }

        String newPath = path + "/" + element.name;

        // if an element is newly a child of another but has already been seen, you'll have special
        // indication
        if (seen.contains(element)) {
            if (showAnyway) {
                addData(dtdCurrent, NEW_PREFIX + name, version, newPath, OMITTED_ATTRIBUTES);
            }
            return;
        }

        seen.add(element);
        if (SHOW && ToolConstants.CHART_DISPLAY_VERSION.equals(version)) {
            System.out.println(dtdCurrent.dtdType + "\t" + name);
        }
        if (DEBUG_DTD == dtdCurrent.dtdType && name.contains(DEBUG_ELEMENT)) {
            int debug = 0;
        }

        Element oldElement = null;
        boolean ordered = element.isOrdered();
        boolean currentTechPreview = element.isTechPreview();

        if (!oldNameToElement.containsKey(name)) {
            Set<String> attributeNames =
                    getAttributeNames(
                            dtdCurrent,
                            dtdLast,
                            name,
                            Collections.emptyMap(),
                            element.getAttributes());
            final String prefix = NEW_PREFIX + (currentTechPreview ? TECHPREVIEW_SIGN : "");
            addData(
                    dtdCurrent,
                    prefix + name + (ordered ? ORDERED_SIGN : ""),
                    version,
                    newPath,
                    attributeNames);
        } else {
            oldElement = oldNameToElement.get(name);
            boolean oldOrdered = oldElement.isOrdered();
            Set<String> attributeNames =
                    getAttributeNames(
                            dtdCurrent,
                            dtdLast,
                            name,
                            oldElement.getAttributes(),
                            element.getAttributes());
            boolean currentDeprecated = element.isDeprecated();
            boolean lastDeprecated =
                    dtdLast == null
                            ? false
                            : oldElement.isDeprecated(); //  + (currentDeprecated ? "ⓓ" : "")
            boolean lastTechPreview =
                    dtdLast == null
                            ? false
                            : oldElement.isTechPreview(); //  + (currentDeprecated ? "ⓓ" : "")

            String deprecatedStatus =
                    currentDeprecated == lastDeprecated
                            ? ""
                            : currentDeprecated ? DEPRECATED_PREFIX : UNDEPRECATED_PREFIX;
            String orderingStatus =
                    (ordered == oldOrdered || currentDeprecated)
                            ? ""
                            : ordered ? ORDERED_SIGN : UNORDERED_SIGN;
            String previewStatus =
                    (currentTechPreview == lastTechPreview || currentDeprecated)
                            ? ""
                            : currentTechPreview ? TECHPREVIEW_SIGN : UNTECHPREVIEW_SIGN;

            if (!orderingStatus.isEmpty()
                    || !previewStatus.isEmpty()
                    || !deprecatedStatus.isEmpty()
                    || !attributeNames.isEmpty()) {
                addData(
                        dtdCurrent,
                        deprecatedStatus + previewStatus + name + orderingStatus,
                        version,
                        newPath,
                        attributeNames);
            }
        }
        if (element.getName().equals("coordinateUnit")) {
            System.out.println(version + "\toordinateUnit\t" + element.getChildren().keySet());
        }
        Set<Element> oldChildren =
                oldElement == null ? Collections.emptySet() : oldElement.getChildren().keySet();
        for (Element child : element.getChildren().keySet()) {
            showAnyway = true;
            for (Element oldChild : oldChildren) {
                if (oldChild.getName().equals(child.getName())) {
                    showAnyway = false;
                    break;
                }
            }
            checkNames(
                    version,
                    dtdCurrent,
                    dtdLast,
                    oldNameToElement,
                    newPath,
                    child,
                    seen,
                    showAnyway);
        }
    }

    enum DiffType {
        Element,
        Attribute,
        AttributeValue
    }

    private static class DiffElement {

        private static final String START_ATTR = "<div>";
        private static final String END_ATTR = "</div>";
        final VersionInfo version;
        final DtdType dtdType;
        final boolean isBeta;
        final String newPath;
        final String newElement;
        final String attributeNames;

        public DiffElement(
                DtdData dtdCurrent,
                String version,
                String newPath,
                String newElement,
                Set<String> attributeNames2) {
            isBeta = version.endsWith("β");
            try {
                this.version =
                        isBeta
                                ? VersionInfo.getInstance(
                                        version.substring(0, version.length() - 1))
                                : VersionInfo.getInstance(version);
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
            dtdType = dtdCurrent.dtdType;
            this.newPath = fix(newPath);
            this.attributeNames =
                    attributeNames2.isEmpty()
                            ? NONE
                            : START_ATTR
                                    + Joiner.on(END_ATTR + START_ATTR).join(attributeNames2)
                                    + END_ATTR;
            this.newElement = newElement;
        }

        private String fix(String substring) {
            int base = substring.indexOf('/', 2);
            if (base < 0) return "";
            int last = substring.lastIndexOf('/');
            if (last <= base) return "/";
            substring = substring.substring(base, last);
            return substring.replace("/", "\u200B/") + "/";
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("version", getVersionString())
                    .add("dtdType", dtdType)
                    .add("newPath", newPath)
                    .add("newElement", newElement)
                    .add("attributeNames", attributeNames)
                    .toString();
        }

        private String getVersionString() {
            return version.getVersionString(2, 4) + (isBeta ? "β" : "");
        }
    }

    List<DiffElement> data = new ArrayList<>();

    private void addData(
            DtdData dtdCurrent,
            String element,
            String prefix,
            String newPath,
            Set<String> attributeNames) {
        DiffElement item = new DiffElement(dtdCurrent, prefix, newPath, element, attributeNames);
        data.add(item);
    }

    static final Set<String> SKIP_ELEMENTS =
            ImmutableSet.of("generation", "identity", "special"); // , "telephoneCodeData"

    static final Multimap<DtdType, String> SKIP_TYPE_ELEMENTS =
            ImmutableMultimap.of(DtdType.ldml, "alias");

    static final Set<String> SKIP_ATTRIBUTES = ImmutableSet.of("references", "standard", "draft");

    static final Multimap<String, String> SKIP_ATTRIBUTE_MATCHES =
            ImmutableMultimap.of("alt", "", "alt", "⟪literal/variant⟫");

    private static Set<String> getAttributeNames(
            DtdData dtdCurrent,
            DtdData dtdLast,
            String elementName,
            Map<Attribute, Integer> attributesOld,
            Map<Attribute, Integer> attributes) {
        Set<String> names = new LinkedHashSet<>();
        if (elementName.equals("coordinateUnit")) {
            int debug = 0;
        }

        main:
        // we want to add a name that is new or that becomes deprecated
        for (Attribute attribute : attributes.keySet()) {
            String name = attribute.getName();
            if (SKIP_ATTRIBUTES.contains(name)) {
                continue;
            }
            String match = attribute.getMatchString();
            AttributeStatus status = attribute.attributeStatus;
            String display = NEW_PREFIX + name;
            //            if (isDeprecated(dtdCurrent, elementName, name)) { //
            // SDI.isDeprecated(dtdCurrent, elementName, name, "*")) {
            //                continue;
            //            }
            String oldMatch = "?";
            String pre, post;
            Attribute attributeOld = attribute.getMatchingName(attributesOld);
            if (attributeOld == null) {
                if (SKIP_ATTRIBUTE_MATCHES.containsEntry(name, match)) {
                    continue main;
                }
                display =
                        NEW_PREFIX
                                + name
                                + " "
                                + AttributeStatus.getShortName(status)
                                + " "
                                + match;
            } else if (attribute.isDeprecated() && !attributeOld.isDeprecated()) {
                display = DEPRECATED_PREFIX + name;
            } else {
                oldMatch = attributeOld.getMatchString();
                AttributeStatus oldStatus = attributeOld.attributeStatus;

                boolean matchEquals = match.equals(oldMatch);
                if (status != oldStatus) {
                    pre = AttributeStatus.getShortName(oldStatus);
                    post = AttributeStatus.getShortName(status);
                    if (!matchEquals) {
                        pre += " " + oldMatch;
                        post += " " + match;
                    }
                } else if (!matchEquals) {
                    if (oldMatch.isEmpty() && SKIP_ATTRIBUTE_MATCHES.containsEntry(name, match)) {
                        continue main;
                    }
                    pre = oldMatch;
                    post = match;
                } else {
                    continue main; // skip attribute entirely;
                }
                display = name + " " + diff(pre, post);
            }
            names.add(display);
        }
        return names;
    }

    public static String diff(String pre, String post) {
        Matcher matcherPre = Attribute.LEAD_TRAIL.matcher(pre);
        Matcher matcherPost = Attribute.LEAD_TRAIL.matcher(post);
        if (matcherPre.matches() && matcherPost.matches()) {
            List<String> preParts = SPLITTER_SPACE.splitToList(matcherPre.group(2));
            List<String> postParts = SPLITTER_SPACE.splitToList(matcherPost.group(2));
            pre = matcherPre.group(1) + remove(preParts, postParts) + matcherPre.group(3);
            post = matcherPost.group(1) + remove(postParts, preParts) + matcherPost.group(3);
        }
        return pre + "➠" + post;
    }

    private static String remove(List<String> main, List<String> toRemove) {
        List<String> result = new ArrayList<>();
        boolean removed = false;
        for (String s : main) {
            if (toRemove.contains(s)) {
                removed = true;
            } else {
                if (removed) {
                    result.add("…");
                    removed = false;
                }
                result.add(s);
            }
        }
        if (removed) {
            result.add("…");
        }
        return Joiner.on(" ").join(result);
    }

    //    private static boolean isDeprecated(DtdData dtdCurrent, String elementName, String
    // attributeName) {
    //        try {
    //            return dtdCurrent.isDeprecated(elementName, attributeName, "*");
    //        } catch (DtdData.IllegalByDtdException e) {
    //            return true;
    //        }
    //    }
}
