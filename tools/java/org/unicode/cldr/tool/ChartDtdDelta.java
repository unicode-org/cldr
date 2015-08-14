package org.unicode.cldr.tool;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdData.Attribute;
import org.unicode.cldr.util.DtdData.Element;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.google.common.base.MoreObjects;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Utility;

/**
 * Changed ShowDtdDiffs into a chart.
 * @author markdavis
 */
public class ChartDtdDelta extends Chart {

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
        return "<p>Shows additions to the LDML dtds over time. New elements or attributes are indicated with a + sign. "
            + "Currently deprecated elements and attributes are omitted."
            + "<p>";
    }

    @Override
    public void writeContents(FormattedFileWriter pw) throws IOException {

        TablePrinter tablePrinter = new TablePrinter()
        .addColumn("Version", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
        .setSortPriority(1)
        .setBreakSpans(true)
        .addColumn("Dtd Type", "class='source'", null, "class='source'", true)
        .setSortPriority(2)

        .addColumn("Intermediate Path", "class='source'", null, "class='target'", true)
        .setSortPriority(3)

        .addColumn("Element", "class='target'", null, "class='target'", true)
        .setSpanRows(false)
        .addColumn("Attributes", "class='target'", null, "class='target'", true)
        .setSpanRows(false)
        ;

        String last = null;
        for (String current : CLDR_VERSIONS) {
            String currentName = current == null ? "trunk" : current;
            for (DtdType type : TYPES) {
                String firstVersion = FIRST_VERSION.get(type);
                if (firstVersion != null && current != null && current.compareTo(firstVersion) < 0) {
                    continue;
                }
                DtdData dtdCurrent = null;
                try {
                    dtdCurrent = DtdData.getInstance(type, current);
                } catch (Exception e) {
                    if (!(e.getCause() instanceof FileNotFoundException)) {
                        throw e;
                    }
                    System.out.println(e.getMessage() + ", " + e.getCause().getMessage());
                    continue;
                }
                DtdData dtdLast = null;
                if (last != null) {
                    try {
                        dtdLast = DtdData.getInstance(type, last);
                    } catch (Exception e) {
                        if (!(e.getCause() instanceof FileNotFoundException)) {
                            throw e;
                        }
                    }
                }
                diff(currentName, dtdLast, dtdCurrent);
            }
            last = current;
        }

        for (DiffElement datum : data) {
            tablePrinter.addRow()
            .addCell(datum.version)
            .addCell(datum.dtdType)
            .addCell(datum.newPath)
            .addCell(datum.newElement)
            .addCell(datum.attributeNames)
            .finishRow();
        }
        pw.write(tablePrinter.toTable());
        pw.write(Utility.repeat("<br>", 50));
    }

    static final String NONE = " ";
    
    static final SupplementalDataInfo SDI = CLDRConfig.getInstance().getSupplementalDataInfo();

    static final List<String> CLDR_VERSIONS = Arrays.asList(
        "1.1.1",
        "1.2.0",
        "1.3.0",
        "1.4.1",
        "1.5.1",
        "1.6.1",
        "1.7.2",
        "1.8.1",
        "1.9.1",
        "2.0.1",
        "21.0",
        "22.1",
        "23.1",
        "24.0",
        "25.0",
        "26.0",
        "27.0",
        null
        );
    static Set<DtdType> TYPES = EnumSet.allOf(DtdType.class);
    static {
        TYPES.remove(DtdType.ldmlICU);
    }

    static final Map<DtdType,String> FIRST_VERSION = new EnumMap<>(DtdType.class);
    static {
        FIRST_VERSION.put(DtdType.ldmlBCP47, "1.7.2");
        FIRST_VERSION.put(DtdType.keyboard, "22.1");
        FIRST_VERSION.put(DtdType.platform, "22.1");
    }

    private void diff(String prefix, DtdData dtdLast, DtdData dtdCurrent) {
        Map<String, Element> oldNameToElement = dtdLast == null ? Collections.EMPTY_MAP : dtdLast.getElementFromName();
        checkNames(prefix, dtdCurrent, oldNameToElement, "/", dtdCurrent.ROOT, new HashSet<Element>());
    }


    private void checkNames(String version, DtdData dtdCurrent, Map<String, Element> oldNameToElement, String path, Element element, HashSet<Element> seen) {
        if (seen.contains(element)) {
            return;
        }
        seen.add(element);
        String name = element.getName();
        if (SKIP_ELEMENTS.contains(name)) {
            return;
        }

        if (isDeprecated(dtdCurrent.dtdType, name, "*")) { // SDI.isDeprecated(dtdCurrent.dtdType, name, "*", "*")) {
            return;
        }
        String newPath = path + "/" + element.name;
        if (path.contains("subdivisions")) {
            int debug = 0;
        }
        if (!oldNameToElement.containsKey(name)) {
            Set<String> attributeNames = getAttributeNames(dtdCurrent, name, Collections.EMPTY_MAP, element.getAttributes());
            addData(dtdCurrent, "+" + name, version, newPath, attributeNames);
        } else {
            Element oldElement = oldNameToElement.get(name);
            Set<String> attributeNames = getAttributeNames(dtdCurrent, name, oldElement.getAttributes(), element.getAttributes());
            if (!attributeNames.isEmpty()) {
                addData(dtdCurrent, name, version, newPath, attributeNames);
            }
        }
        for (Element child : element.getChildren().keySet()) {
            checkNames(version, dtdCurrent, oldNameToElement, newPath, child, seen);
        }
    }

    enum DiffType {Element, Attribute, AttributeValue}

    private static class DiffElement {

        final String version;
        final DtdType dtdType;
        final String newPath;
        final String newElement;
        final String attributeNames;

        public DiffElement(DtdData dtdCurrent, String version, String newPath, String newElement, Set<String> attributeNames2) {
            this.version = version;
            dtdType = dtdCurrent.dtdType;
            this.newPath = fix(newPath);
            this.attributeNames = attributeNames2.isEmpty() ? NONE : "+" + CollectionUtilities.join(attributeNames2, ", +");
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
                .add("version", version)
                .add("dtdType", dtdType)
                .add("newPath", newPath)
                .add("newElement", newElement)
                .add("attributeNames", attributeNames)
                .toString();
        }
    }

    List<DiffElement> data = new ArrayList<>();

    private void addData(DtdData dtdCurrent, String element, String prefix, String newPath, Set<String> attributeNames) {
        DiffElement item = new DiffElement(dtdCurrent, prefix, newPath, element, attributeNames);
        data.add(item);
    }

    static final Set<String> SKIP_ELEMENTS = new HashSet<>(Arrays.asList("generation", "identity", "alias", "special", "telephoneCodeData"));
    static final Set<String> SKIP_ATTRIBUTES = new HashSet<>(Arrays.asList("references", "standard", "draft", "alt"));

    private static Set<String> getAttributeNames(DtdData dtdCurrent, String elementName, Map<Attribute, Integer> attributesOld, Map<Attribute, Integer> attributes) {
        Set<String> names = new LinkedHashSet<>();
        main:
            for (Attribute attribute : attributes.keySet()) {
                String name = attribute.getName();
                if (SKIP_ATTRIBUTES.contains(name)) {
                    continue;
                }
                if (isDeprecated(dtdCurrent.dtdType, elementName, name)) { // SDI.isDeprecated(dtdCurrent, elementName, name, "*")) {
                    continue;
                }
                for (Attribute attributeOld : attributesOld.keySet()) {
                    if (attributeOld.name.equals(name)) {
                        continue main;
                    }
                }
                names.add(name);
            }
        return names;
    }

    private static boolean isDeprecated(DtdType dtdType, String elementName, String attributeName) {
        try {
            return DtdData.getInstance(dtdType).isDeprecated(elementName, attributeName, "*");
        } catch (DtdData.IllegalByDtdException e) {
            return true;
        }
    }
}
