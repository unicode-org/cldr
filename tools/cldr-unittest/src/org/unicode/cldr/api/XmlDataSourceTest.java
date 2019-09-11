package org.unicode.cldr.api;

import com.google.common.base.Joiner;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.ibm.icu.dev.test.TestFmwk;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import static org.unicode.cldr.api.CldrData.PathOrder.ARBITRARY;
import static org.unicode.cldr.api.CldrData.PathOrder.DTD;
import static org.unicode.cldr.api.CldrData.PathOrder.NESTED_GROUPING;
import static org.unicode.cldr.api.CldrDataType.BCP47;
import static org.unicode.cldr.api.CldrDataType.SUPPLEMENTAL;
import static org.unicode.cldr.api.CldrDraftStatus.UNCONFIRMED;

/**
 * Tests XML file parsing and path/value generation. These focus on end-to-end parsing of fake data
 * into expected sequences of path/value pairs. In particular this tests that unwanted attributes
 * are not generated in the final paths and the output ordering is as expected.
 */
public class XmlDataSourceTest extends TestFmwk {
    // Note that the XML paths specified for the dummy XML data must still represent the expected
    // local file structure, since they are used to resolve relative references to the DTDs. This
    // assumes that when tests are run the working directory is the project "base directory" where
    // the Ant file is. This is all very unsatisfying and ideally the DTD information would be
    // accessed as more locally within this sub-project.
    // TODO: Fix this to have real DTDs avaiable in the class hierarchy.

    public void TestSimple() {
        ListMultimap<Path, String> files = LinkedListMultimap.create();
        addFile(files, "foo.xml",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>",
            "<!DOCTYPE ldmlBCP47 SYSTEM \"../../common/dtd/ldmlBCP47.dtd\">",
            "<ldmlBCP47>",
            "  <version number=\"42\"/>",
            "  <keyword>",
            "    <key name=\"cf\" description=\"Currency format key\" since=\"28\">",
            "      <type name=\"standard\" description=\"Standard format\" since=\"28\"/>",
            "      <type name=\"account\" description=\"Accounting format\" since=\"28\"/>",
            "    </key>",
            "    <key name=\"cu\" description=\"Currency type key\" alias=\"currency\">",
            "      <type name=\"adp\" description=\"Andorran Peseta\" since=\"1.9\"/>",
            "      <type name=\"zar\" description=\"South African Rand\"/>",
            "    </key>",
            "  </keyword>",
            "</ldmlBCP47>");
        addFile(files, "bar.xml",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>",
            "<!DOCTYPE ldmlBCP47 SYSTEM \"../../common/dtd/ldmlBCP47.dtd\">",
            "<ldmlBCP47>",
            "  <version number=\"42\"/>",
            "  <keyword>",
            "    <key name=\"tz\" description=\"Time zone key\" alias=\"timezone\">",
            "      <type name=\"adalv\" description=\"Andorra\" alias=\"Europe/Andorra\"/>",
            "      <type name=\"fjsuv\" description=\"Fiji\" alias=\"Pacific/Fiji\"/>",
            "    </key>",
            "  </keyword>",
            "</ldmlBCP47>");
        XmlDataSource xmlDataSource =
            new XmlDataSource(BCP47, files.keySet(), UNCONFIRMED, openFileFn(files));

        Map<CldrPath, CldrValue> expected = new LinkedHashMap<>();
        addTo(expected, "//ldmlBCP47/keyword"
            + "/key[@name=\"cf\"][@description=\"Currency format key\"][@since=\"28\"][@deprecated=\"false\"]"
            + "/type[@name=\"standard\"][@description=\"Standard format\"][@since=\"28\"][@deprecated=\"false\"]");
        addTo(expected, "//ldmlBCP47/keyword"
            + "/key[@name=\"cf\"][@description=\"Currency format key\"][@since=\"28\"][@deprecated=\"false\"]"
            + "/type[@name=\"account\"][@description=\"Accounting format\"][@since=\"28\"][@deprecated=\"false\"]");
        addTo(expected, "//ldmlBCP47/keyword"
            + "/key[@name=\"cu\"][@description=\"Currency type key\"][@alias=\"currency\"][@deprecated=\"false\"]"
            + "/type[@name=\"adp\"][@description=\"Andorran Peseta\"][@since=\"1.9\"][@deprecated=\"false\"]");
        addTo(expected, "//ldmlBCP47/keyword"
            + "/key[@name=\"cu\"][@description=\"Currency type key\"][@alias=\"currency\"][@deprecated=\"false\"]"
            + "/type[@name=\"zar\"][@description=\"South African Rand\"][@deprecated=\"false\"]");
        addTo(expected, "//ldmlBCP47/keyword"
            + "/key[@name=\"tz\"][@description=\"Time zone key\"][@alias=\"timezone\"][@deprecated=\"false\"]"
            + "/type[@name=\"adalv\"][@description=\"Andorra\"][@alias=\"Europe/Andorra\"][@deprecated=\"false\"]");
        addTo(expected, "//ldmlBCP47/keyword"
            + "/key[@name=\"tz\"][@description=\"Time zone key\"][@alias=\"timezone\"][@deprecated=\"false\"]"
            + "/type[@name=\"fjsuv\"][@description=\"Fiji\"][@alias=\"Pacific/Fiji\"][@deprecated=\"false\"]");
        ImmutableList<CldrPath> naturalOrderedPaths = ImmutableList.copyOf(expected.keySet());
        ImmutableList<CldrPath> dtdOrderedPaths = ImmutableList.sortedCopyOf(expected.keySet());
        // We want to check that DTD ordering will typically differ from "natural" order.
        assertNotEquals("check expectations", naturalOrderedPaths, dtdOrderedPaths);

        Map<CldrPath, CldrValue> out = new LinkedHashMap<>();
        xmlDataSource.accept(ARBITRARY, value -> out.put(value.getPath(), value));
        assertEquals("paths and values", expected, out);
        assertEquals("paths order", naturalOrderedPaths, ImmutableList.copyOf(out.keySet()));

        out.clear();
        xmlDataSource.accept(NESTED_GROUPING, value -> out.put(value.getPath(), value));
        assertEquals("paths and values", expected, out);
        assertEquals("paths order", naturalOrderedPaths, ImmutableList.copyOf(out.keySet()));

        out.clear();
        xmlDataSource.accept(DTD, value -> out.put(value.getPath(), value));
        assertEquals("paths and values", expected, out);
        assertEquals("paths order", dtdOrderedPaths, ImmutableList.copyOf(out.keySet()));
    }
    
    public void TestBadElementNesting() {
        ListMultimap<Path, String> files = LinkedListMultimap.create();
        String fakeXmlName = "bad.xml";
        addFile(files, fakeXmlName,
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>",
            "<!DOCTYPE supplementalData SYSTEM \"../../common/dtd/ldmlSupplemental.dtd\">",
            "<supplementalData>",
            "  <version number=\"42\"/>",
            "  <currencyData>",
            "    <fractions>",
            "      <info iso4217=\"ADP\" digits=\"0\" rounding=\"0\"/>",
            "    </fractions>",
            "    <region iso3166=\"AC\">",
            "      <currency iso4217=\"SHP\" from=\"1976-01-01\"/>",
            "    </region>",
            "    <fractions>",
            "      <info iso4217=\"AFN\" digits=\"0\" rounding=\"0\"/>",
            "    </fractions>",
            "    <region iso3166=\"AE\">",
            "      <currency iso4217=\"AED\" from=\"1973-05-19\"/>",
            "    </region>",
            "  </currencyData>",
            "</supplementalData>");
        XmlDataSource badDataSource =
            new XmlDataSource(SUPPLEMENTAL, files.keySet(), UNCONFIRMED, openFileFn(files));
        try {
            badDataSource.accept(ARBITRARY, v -> {});
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertErrorMessageContains(e, "error reading");
            assertErrorMessageContains(e, fakeXmlName);
        }
    }

    // This is an important restriction based on the observation that a lot of the existing path
    // and regex based transformation logic implicitly relies on " not being allowed in attribute
    // values. If it were, then the path string representation would have to change, path parsing
    // would need to be changed and a lot of the regex transformation logic would need to be
    // re-thought.
    public void TestDoubleQuotesDisallowedAsAttributeValue() {
        ListMultimap<Path, String> files = LinkedListMultimap.create();
        String fakeXmlName = "bad.xml";
        addFile(files, fakeXmlName,
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>",
            "<!DOCTYPE supplementalData SYSTEM \"../../common/dtd/ldmlSupplemental.dtd\">",
            "<supplementalData>",
            "  <version number=\"42\"/>",
            "  <characters>",
            "    <character-fallback>",
            "      <character value=\"'\">",
            "        <substitute>single-quote</substitute>",
            "      </character>",
            "      <character value='\"'>",
            "        <substitute>double-quote</substitute>",
            "      </character>",
            "    </character-fallback>",
            "  </characters>",
            "</supplementalData>");
        XmlDataSource badDataSource =
            new XmlDataSource(SUPPLEMENTAL, files.keySet(), UNCONFIRMED, openFileFn(files));
        try {
            badDataSource.accept(ARBITRARY, v -> {});
            fail("expected RuntimeException");
        } catch (RuntimeException e) {
            assertErrorMessageContains(e, "Unknown error");
            assertErrorMessageContains(e, fakeXmlName);
        }
    }

    public void TestNoDtdVersionPath() {
        ListMultimap<Path, String> files = LinkedListMultimap.create();
        addFile(files, "bad.xml",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>",
            "<!DOCTYPE supplementalData SYSTEM \"../../common/dtd/ldmlSupplemental.dtd\">",
            "<supplementalData>",
            "  <version number=\"42\"/>",
            "  <characters>",
            "    <character-fallback>",
            "      <character value=\"'\">",
            "        <substitute>single-quote</substitute>",
            "      </character>",
            "    </character-fallback>",
            "  </characters>",
            "</supplementalData>");
        XmlDataSource src =
            new XmlDataSource(SUPPLEMENTAL, files.keySet(), UNCONFIRMED, openFileFn(files));
        src.accept(DTD, v ->
            assertFalse("is DTD version string",
                v.getPath().toString().startsWith("//supplementalData/version")));
    }

    private static void addFile(ListMultimap<Path, String> files, String path, String... lines) {
        files.putAll(Paths.get(path), Arrays.asList(lines));
    }

    private static Function<Path, Reader> openFileFn(ListMultimap<Path, String> files) {
        return p -> new StringReader(Joiner.on('\n').join(files.get(p)));
    }

    private static void addTo(Map<CldrPath, CldrValue> map, String fullPath) {
        CldrValue v = CldrValue.parseValue(fullPath, "");
        map.put(v.getPath(), v);
    }

    private void assertErrorMessageContains(Throwable e, String expected) {
        // This test "framework" is such a limited API it encourages brittle assertions.
        assertTrue(
            "error message \"" + e.getMessage() + "\" contains \"" + expected + "\"",
            e.getMessage().contains(expected));
    }
}
