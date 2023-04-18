package org.unicode.cldr.tool;

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.text.MessageFormat;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.unicode.cldr.util.*;

public class GenerateNumberingSystemAliases {

    public static void main(String[] args) throws Exception {
        final Map<Pair<String, String>, String> missing = getMissingRootNumberingSystems();
        if (missing.isEmpty()) {
            System.out.println("Nothing to do, all OK.");
            return;
        }
        final String dir = CLDRPaths.MAIN_DIRECTORY;
        final String fileName = "root.xml";
        System.out.println("Starting update of " + dir + fileName);
        CLDRConfig config = CLDRConfig.getInstance();
        CLDRFile root = config.getCLDRFile(LocaleNames.ROOT, false).cloneAsThawed(); // unresolved

        XPathParts xpp =
                XPathParts.getFrozenInstance("//ldml/numbers/_____/alias")
                        .cloneAsThawed()
                        .setAttribute(-1, "source", "locale");

        // Modify it
        System.out.println(
                MessageFormat.format(
                        "Updating {0, plural, one {# alias} other {# aliases}} under //ldml/numbers in {1}",
                        missing.size(), fileName));
        for (final Map.Entry<Pair<String, String>, String> e : missing.entrySet()) {
            final String ns = e.getKey().getFirst();
            final String element = e.getKey().getSecond();
            final String path = e.getValue();

            xpp.setElement(-2, element)
                    .setAttribute(-2, "numberSystem", ns)
                    .setAttribute(
                            -1, "path", "../" + element + "[@numberSystem='latn']"); // CLDR-16488

            root.add(xpp.toString(), "");
        }

        try (TempPrintWriter out = TempPrintWriter.openUTF8Writer(dir, fileName)) {
            root.write(out.asPrintWriter());
        }
        System.out.println("Done. Don't forget to commit " + dir + fileName);
    }

    /**
     * Get all items missing from root
     *
     * @return Map: numberSystem,element -> xpath
     */
    public static Map<Pair<String, String>, String> getMissingRootNumberingSystems() {
        final CLDRConfig config = CLDRConfig.getInstance();
        final CLDRFile root = config.getRoot();

        final Set<String> expectedElement =
                ImmutableSet.of(
                        "symbols",
                        "currencyFormats",
                        "decimalFormats",
                        "miscPatterns",
                        "percentFormats",
                        "scientificFormats");

        XPathParts xpp =
                XPathParts.getFrozenInstance("//ldml/numbers/______") // will be filled in below
                        .cloneAsThawed();

        final Map<Pair<String, String>, String> missing = new TreeMap<>();

        for (final String ns : config.getSupplementalDataInfo().getNumericNumberingSystems()) {
            for (final String element : expectedElement) {
                xpp.setElement(-1, element);
                xpp.setAttribute(-1, "numberSystem", ns);
                final String basePath = xpp.toString();
                if (!root.iterator(basePath).hasNext()) {
                    missing.put(Pair.of(ns, element), basePath);
                }
            }
        }
        return missing;
    }
}
