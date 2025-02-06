package org.unicode.cldr.unittest;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Pair;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdData.Attribute;
import org.unicode.cldr.util.DtdData.Element;
import org.unicode.cldr.util.DtdType;

public class CheckLdmlDtdReferences {

    static Map<DtdType, DtdData> typeToData =
            ImmutableMap.of(
                    DtdType.ldml, DtdData.getInstance(DtdType.ldml),
                    DtdType.supplementalData, DtdData.getInstance(DtdType.supplementalData),
                    DtdType.ldmlBCP47, DtdData.getInstance(DtdType.ldmlBCP47));

    static Multimap<DtdType, String> mismatched = TreeMultimap.create();
    static Multimap<String, String> unrecognized = LinkedHashMultimap.create();

    static Multimap<DtdType, Element> missingElements = LinkedHashMultimap.create();
    static Multimap<DtdType, Attribute> missingAttributes = LinkedHashMultimap.create();
    static Map<DtdType, Multimap<Element, String>> foundElementsToLink = new LinkedHashMap<>();

    static Map<DtdType, Multimap<Element, Element>> childToParent = new LinkedHashMap<>();

    //    static Map<DtdType, Multimap<Element, String>> foundAttributesToLink = new
    // LinkedHashMap<>();
    static {
        for (DtdType dtdType : typeToData.keySet()) {
            final DtdData dtdData = typeToData.get(dtdType);
            missingElements.putAll(dtdType, dtdData.getElements());
            missingAttributes.putAll(dtdType, dtdData.getAttributes());
            foundElementsToLink.put(dtdType, LinkedHashMultimap.create());
            childToParent.put(
                    dtdType, getChildToParent(dtdData, dtdData.ROOT, LinkedHashMultimap.create()));
        }
    }

    public static void main(String[] args) throws IOException {
        try (Stream<Path> stream =
                Files.list(Paths.get(CLDRPaths.BASE_DIRECTORY + "docs", "ldml"))) {
            stream.forEach(
                    x -> {
                        try {
                            if (Files.isReadable(x) && x.endsWith(".md") && x.startsWith("tr35")) {
                                process(x);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }

        System.out.println("UNRECOGNIZED or deprecated");
        int counter = 0;
        for (Entry<String, String> entry : unrecognized.entries()) {
            if (entry.getValue().contains("*Alias")) {
                continue;
            }
            System.out.println(++counter + "\t?\t" + entry.getKey() + "\t" + entry.getValue());
        }
        if (counter == 0) {
            System.out.println("NONE");
        }

        System.out.println("\nNEEDS UPDATE");
        System.out.println("№\tDTD\tPreceding Link\tDocument line\tDTD line");
        counter = 0;
        for (Entry<DtdType, String> entry : mismatched.entries()) {
            System.out.println(++counter + "\t" + entry.getKey() + "\t" + entry.getValue());
        }
        if (counter == 0) {
            System.out.println("NONE");
        }

        System.out.println("\nMISSING ELEMENTS");
        System.out.println(
                "№\tDTD\tDTD line\tOk or missing\tPreceding Link?\tPreceding Link?\tPreceding Link?");
        counter = 0;
        for (DtdType dtdType : Arrays.asList(DtdType.ldml, DtdType.supplementalData)) {
            counter =
                    showMissing(
                            dtdType,
                            typeToData.get(dtdType).ROOT,
                            "",
                            new HashSet<Element>(),
                            counter);
        }
        if (counter == 0) {
            System.out.println("NONE");
        }

        System.out.println("\nINDEX");
        System.out.println("№\tDTD\tElement\tPreceding Link 1\tPreceding Link 2\tPreceding Link 3");
        counter = 0;
        for (Entry<DtdType, Multimap<Element, String>> entry : foundElementsToLink.entrySet()) {
            final DtdType dtdType = entry.getKey();
            for (Entry<Element, Collection<String>> entry2 : entry.getValue().asMap().entrySet()) {
                // tr35-general.md#Display_Name_Elements
                // https://unicode.org/reports/tr35/tr35-general.html#Display_Name_Elements
                final Element element = entry2.getKey();
                System.out.print(++counter + "\t" + dtdType + "\t" + element);
                for (String link : entry2.getValue()) {
                    System.out.print(
                            "\thttps://unicode.org/reports/tr35/" + link.replace(".md", ".html"));
                }
                System.out.println();
            }
        }
        if (counter == 0) {
            System.out.println("NONE");
        }
    }

    private static Multimap<Element, Element> getChildToParent(
            DtdData dtdData, Element current, Multimap<Element, Element> result) {
        Set<Element> children = current.getChildren().keySet();
        for (Element child : children) {
            result.put(child, current);
            getChildToParent(dtdData, child, result);
        }
        return result;
    }

    static final Set<String> SKIP_ELEMENTS = ImmutableSet.of("special", "alias");
    static final Set<String> SKIP_ATTRIBUTES =
            ImmutableSet.of("alias", "alt", "draft", "references");

    private static int showMissing(
            DtdType dtdType, Element element, String padding, HashSet<Element> seen, int counter) {
        if (seen.contains(element) || SKIP_ELEMENTS.contains(element.getName())) {
            return counter;
        }
        seen.add(element);

        boolean elementShown = false;
        if (missingElements.get(dtdType).contains(element)) {
            counter = showMissing(dtdType, element, padding, false, counter);
            elementShown = true;
        }
        Set<Attribute> attributes = element.getAttributes().keySet();
        for (Attribute attribute : attributes) {
            if (!attribute.isDeprecated()
                    && !SKIP_ATTRIBUTES.contains(attribute.getName())
                    && missingAttributes.get(dtdType).contains(attribute)) {
                if (!elementShown) {
                    counter = showMissing(dtdType, element, padding, true, counter);
                    elementShown = true;
                }
                System.out.println(
                        ++counter
                                + "\t"
                                + dtdType
                                + "\t"
                                + padding
                                + attribute.appendDtdString(new StringBuilder()));
            }
        }
        Set<Element> children = element.getChildren().keySet();
        for (Element child : children) {
            if (!child.isDeprecated()) {
                counter = showMissing(dtdType, child, padding + " ", seen, counter);
            }
        }
        return counter;
    }

    public static int showMissing(
            DtdType dtdType, Element element, String padding, boolean ok, int counter) {
        if (element.name.equals("script")) {
            int debug = 0;
        }
        String message = padding + dtdType + "\t" + element.toDtdString();
        if (ok) {
            message += "\t🆗" + getBest(dtdType, element);
        } else {
            message += "\t❌" + getBest(dtdType, element);
        }
        System.out.println(++counter + "\t" + message);
        return counter;
    }

    public static String getBest(DtdType dtdType, Element element) {
        final Multimap<Element, String> elementToLink = foundElementsToLink.get(dtdType);
        final Multimap<Element, Element> childToParents = childToParent.get(dtdType);
        Element original = element;
        String message = " ➡︎";
        int i = 0;
        while (true) {
            Collection<String> links = elementToLink.get(element);
            if (!links.isEmpty()) {
                return message
                        + " "
                        + (i == 0 ? "" : element.getName())
                        + "\t"
                        + Joiner.on('\t').join(links);
            }
            Collection<Element> parents = childToParents.get(element);
            if (parents.isEmpty()) {
                return message + "\tNONE";
            }
            element = parents.iterator().next(); // TODO fix to show all parents
            message += "➡︎";
            ++i;
        }
    }

    private static void process(Path file) throws IOException {
        DtdType dtdDtype =
                file.getFileName().toString().equals("info.md")
                        ? DtdType.supplementalData
                        : DtdType.ldml;
        lastLink = "?";
        Files.lines(file).forEach(x -> processLine(dtdDtype, file, x));
    }

    static final Pattern NAME_PATTERN = Pattern.compile("<a [^>]*name=\\\"([^\\\"]*)\\\"([^>]*)>");

    static final Pattern ELEMENT_PATTERN =
            Pattern.compile("<\\!ELEMENT\\s*" + "([a-zA-Z_0-9]*)\\s*" + "([^>]*)>");

    static final Pattern ATTLIST_PATTERN =
            Pattern.compile(
                    "<\\!ATTLIST\\s*"
                            + "([a-zA-Z_0-9]*)\\s*"
                            + "([a-zA-Z_0-9:]*)\\s*"
                            + "([^>]*)>");

    static String lastLink;

    private static void processLine(DtdType dtdType, Path file, String line) {
        /**
         * <a ... name="Territory_Data" ...> <!ELEMENT group EMPTY > <!ATTLIST group type NMTOKEN
         * #REQUIRED >
         */
        if (line.contains("<a name=\"locale_display_name_fields\"")) {
            int debug = 0;
        }
        Matcher nameMatcher = NAME_PATTERN.matcher(line);
        if (nameMatcher.find()) {
            lastLink = file.getFileName() + "#" + nameMatcher.group(1);
            return;
        }
        if (line.startsWith("<!ELEMENT unitPreferences")) {
            int debug = 0;
        }
        Matcher elementMatcher = ELEMENT_PATTERN.matcher(line);
        if (elementMatcher.find()) {
            String elementName = elementMatcher.group(1);
            String documentLine = elementMatcher.group(0);
            Pair<DtdType, Element> result = findElement(dtdType, elementName);
            if (result == null) {
                unrecognized.put(lastLink, documentLine);
            } else {
                dtdType = result.first;
                Element element = result.second;
                String canonicalDtd = element.toDtdString();
                verifyMatch(dtdType, documentLine, canonicalDtd);
                foundElementsToLink.get(dtdType).put(element, lastLink);
                missingElements.remove(dtdType, element);
            }
            return;
        }
        if (line.startsWith("<!ATTLIST foreignSpaceReplacement")) {
            int debug = 0;
        }
        Matcher attListMatcher = ATTLIST_PATTERN.matcher(line);
        if (attListMatcher.find()) {
            final String elementName = attListMatcher.group(1);
            final String attributeName = attListMatcher.group(2);
            final String documentLine = attListMatcher.group(0);
            Pair<DtdType, Attribute> result = findAttribute(dtdType, elementName, attributeName);
            if (result == null) {
                unrecognized.put(lastLink, documentLine);
            } else {
                dtdType = result.first;
                Attribute attribute = result.second;
                String canonicalDtd = attribute.appendDtdString(new StringBuilder()).toString();
                verifyMatch(dtdType, documentLine, canonicalDtd);
                missingAttributes.remove(dtdType, attribute);
            }
            return;
        }
    }

    public static Pair<DtdType, Element> findElement(DtdType guessDtdType, String elementName) {
        DtdData data = typeToData.get(guessDtdType);
        Element element = data.getElementFromName().get(elementName);
        if (element != null) {
            return Pair.of(guessDtdType, element);
        }
        for (DtdType dtdType2 : typeToData.keySet()) {
            if (dtdType2 != guessDtdType) {
                data = typeToData.get(dtdType2);
                element = data.getElementFromName().get(elementName);
                if (element != null) {
                    return Pair.of(dtdType2, element);
                }
            }
        }
        return null;
    }

    public static Pair<DtdType, Attribute> findAttribute(
            DtdType dtdType, String elementName, String attributeName) {
        DtdData data = typeToData.get(dtdType);
        Attribute attribute = data.getAttribute(elementName, attributeName);
        if (attribute != null) {
            return Pair.of(dtdType, attribute);
        }
        for (DtdType dtdType2 : typeToData.keySet()) {
            if (dtdType2 != dtdType) {
                data = typeToData.get(dtdType2);
                attribute = data.getAttribute(elementName, attributeName);
                if (attribute != null) {
                    return Pair.of(dtdType, attribute);
                }
            }
        }
        return null;
    }

    public static boolean verifyMatch(
            DtdType dtdType, final String documentLine, String canonicalDtd) {
        if (!matches(documentLine, canonicalDtd)) {
            mismatched.put(
                    dtdType, lastLink + "\tDOC:\t" + documentLine + "\t≠DTD:\t" + canonicalDtd);
            return true;
        }
        return false;
    }

    public static boolean matches(final String documentLine, String canonicalDtd) {
        return fixLine(canonicalDtd).equals(fixLine(documentLine));
    }

    public static String fixLine(String canonicalDtd) {
        return canonicalDtd
                .replace(" ", "")
                .replace("(special*)", "EMPTY")
                .replace(",special*", "");
    }

    public static DtdType getOtherType(DtdType dtdType) {
        return dtdType == DtdType.ldml ? DtdType.supplementalData : DtdType.ldml;
    }
}
