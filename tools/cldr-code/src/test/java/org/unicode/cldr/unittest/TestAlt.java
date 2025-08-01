package org.unicode.cldr.unittest;

import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.util.Output;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.XPathParts;

public class TestAlt extends TestFmwk {
    static CLDRConfig testInfo = CLDRConfig.getInstance();

    public static void main(String[] args) {
        new TestAlt().run(args);
    }

    public void testValues() {
        Factory cldrFactory = testInfo.getCldrFactory();
        HashMap<String, String> altPaths = new HashMap<>();
        Multimap<String, R4<String, String, String, String>> altStarred = TreeMultimap.create();
        final Set<String> available = new LinkedHashSet<>();
        available.add("root");
        available.add("en");
        for (String locale : cldrFactory.getAvailable()) {
            if (locale.startsWith("en_")) {
                available.add(locale);
            }
        }
        available.addAll(cldrFactory.getAvailable());

        for (String locale : available) {
            CLDRFile cldrFile = cldrFactory.make(locale, false);
            for (String xpath : cldrFile) {
                if (altPaths.containsKey(xpath)) {
                    continue;
                }
                if (!xpath.contains("alt")) {
                    continue;
                }
                XPathParts parts = XPathParts.getFrozenInstance(xpath);
                for (int i = 0; i < parts.size(); ++i) {
                    String altValue = parts.getAttributeValue(i, "alt");
                    if (altValue != null) {
                        altPaths.put(xpath, locale);
                        logln(locale + "\t" + xpath);
                        final List<String> attributes = new ArrayList<>();
                        String starredPath = starSkippingAlt(parts.cloneAsThawed(), attributes);
                        String attrs = Joiner.on("|").join(attributes);
                        final XPathParts noAlt = parts.cloneAsThawed().removeAttribute(i, "alt");
                        String plainPath = noAlt.toString();
                        altStarred.put(
                                starredPath,
                                Row.of(
                                        locale,
                                        cldrFile.getStringValue(plainPath),
                                        cldrFile.getStringValue(xpath),
                                        attrs));
                    }
                }
            }
        }
        for (Entry<String, Collection<R4<String, String, String, String>>> entry :
                altStarred.asMap().entrySet()) {
            System.out.println(entry.getKey() + "\t" + Joiner.on("\t").join(entry.getValue()));
        }
    }

    private String starSkippingAlt(XPathParts parts, List<String> attributes) {
        attributes.clear();
        for (int i = 0; i < parts.size(); ++i) {
            for (String key : parts.getAttributeKeys(i)) {
                if (!"alt".equals(key)) {
                    attributes.add(parts.getAttributeValue(i, key));
                    parts.setAttribute(i, key, PathStarrer.SIMPLE_STAR_PATTERN);
                }
            }
        }
        return parts.toString();
    }

    public void testAlt() {
        Output<String> pathWhereFound = new Output<>();
        Output<String> localeWhereFound = new Output<>();

        CLDRFile testCldrFile = CLDRConfig.getInstance().getCLDRFile("fr_CA", true);
        String plain = "//ldml/localeDisplayNames/languages/language[@type=\"af\"]";

        String expected = testCldrFile.getStringValue(plain);
        String altMedium = "[@alt=\"medium\"]";

        String actual = testCldrFile.getStringValue(plain + altMedium);
        assertEquals(plain + altMedium, expected, actual);
        Status status = new Status();
        localeWhereFound.value = testCldrFile.getSourceLocaleID(plain + altMedium, status);
        pathWhereFound.value = status.pathWhereFound;
        assertEquals("Regular, pathWhereFound", plain, pathWhereFound.value);
        assertEquals("Regular, localeWhereFound", "fr_CA", localeWhereFound.value);

        String actualBailey =
                testCldrFile.getBaileyValue(plain + altMedium, pathWhereFound, localeWhereFound);
        assertEquals("Bailey, " + plain + altMedium, expected, actualBailey);

        assertEquals("Bailey, pathWhereFound", plain, pathWhereFound.value);
        assertEquals("Bailey, localeWhereFound", "fr_CA", localeWhereFound.value);

        // TODO check constructed
    }
}
