package org.unicode.cldr.api;

import com.google.common.collect.ImmutableMap;
import com.ibm.icu.dev.test.TestFmwk;

/**
 * Tests for the core CLDR value representation. Since this is an immutable value type, the tests
 * largely focus on things like parsing and validity checking. 
 */
public class CldrValueTest extends TestFmwk {

    public void TestNoValueAttributes() {
        String dpath = "//ldml/localeDisplayNames/territories/territory[@type=\"US\"]";
        CldrValue value = CldrValue.parseValue(dpath, "Hello World");
        assertEquals("value", "Hello World", value.getValue());
        assertTrue("no attributes", value.getValueAttributes().isEmpty());
        assertEquals("path", dpath, value.getPath().toString());
    }

    public void TestValueAttributes() {
        CldrValue v = CldrValue.parseValue("//supplementalData/info[@digits=\"2\"][@iso4217=\"AMD\"]"
            + "[@cashRounding=\"0\"][@rounding=\"0\"][@cashDigits=\"0\"]", "");
        assertEquals("value", "", v.getValue());
        // The map contains only the value attributes (no "iso4217") and in DTD order. 
        ImmutableMap<AttributeKey, String> expected = ImmutableMap.of(
            AttributeKey.keyOf("info", "digits"), "2",
            AttributeKey.keyOf("info", "rounding"), "0",
            AttributeKey.keyOf("info", "cashDigits"), "0",
            AttributeKey.keyOf("info", "cashRounding"), "0");
        assertEquals("attributes", expected, v.getValueAttributes());
        // Attribute order is normalized to DTD order.
        assertEquals("attribute order",
            expected.keySet().asList(), v.getValueAttributes().keySet().asList());
    }

    public void TestRetainExplicitNonDefault() {
        // The "deprecated" attribute has a default value of "false" and should be ignored, even if
        // explicitly present.
        CldrValue v = CldrValue.parseValue(
            "//ldmlBCP47/keyword"
                + "/key[@name=\"cu\"][@description=\"Currency type key\"][@alias=\"currency\"]"
                + "/type[@name=\"xau\"][@description=\"Gold\"][@deprecated=\"true\"]",
            "");
        assertEquals("path string",
            "//ldmlBCP47/keyword/key[@name=\"cu\"]/type[@name=\"xau\"]", v.getPath().toString());
        AttributeKey key = AttributeKey.keyOf("type", "deprecated");
        assertTrue("deprecated", v.getValueAttributes().containsKey(key));
    }

    public void TestValueEquality() {
        CldrValue v = CldrValue.parseValue(
            "//supplementalData/info[@iso4217=\"AMD\"][@digits=\"2\"]"
                + "[@rounding=\"0\"][@cashDigits=\"0\"][@cashRounding=\"0\"]", "value");
        // Same value, but attributes in a different order in the string (does not matter).
        CldrValue vDiffOrder = CldrValue.parseValue(
            "//supplementalData/info[@iso4217=\"AMD\"][@cashRounding=\"0\"]"
                + "[@cashDigits=\"0\"][@rounding=\"0\"][@digits=\"2\"]", "value");
        // Different value does matter.
        CldrValue vDiffValue = CldrValue.parseValue(
            "//supplementalData/info[@iso4217=\"AMD\"][@digits=\"2\"]"
                + "[@rounding=\"0\"][@cashDigits=\"0\"][@cashRounding=\"0\"]", "different");
        // Different "value" attribute value does matter.
        CldrValue vDiffAttr = CldrValue.parseValue(
            "//supplementalData/info[@iso4217=\"AMD\"][@digits=\"3\"]"
                + "[@rounding=\"0\"][@cashDigits=\"0\"][@cashRounding=\"0\"]", "value");
        // Differing "distinguishing" attribute does matter (that's part of the CldrPath).
        CldrValue vSameDiffPath = CldrValue.parseValue(
            "//supplementalData/info[@iso4217=\"XXX\"][@digits=\"2\"]"
                + "[@rounding=\"0\"][@cashDigits=\"0\"][@cashRounding=\"0\"]", "value");
        assertEquals("equal instances", v, vDiffOrder);
        assertNotEquals("unequal path", v, vSameDiffPath);
        assertNotEquals("unequal values", v, vDiffValue);
        assertNotEquals("unequal attributes", v, vDiffAttr);

        // Also do some hashcode checking (not all combinations).
        assertEquals("equal hashcode", v.hashCode(), vDiffOrder.hashCode());
        // Clash is technically possible but effectively impossible.
        assertNotEquals("unequal hashcode", v.hashCode(), vDiffValue.hashCode());
        assertNotEquals("unequal hashcode", v.hashCode(), vDiffAttr.hashCode());
    }
}
