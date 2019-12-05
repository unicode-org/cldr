// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.cldr.api;

import static org.unicode.cldr.api.CldrData.PathOrder.ARBITRARY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.ibm.icu.dev.test.TestFmwk;

public class FilteredDataTest extends TestFmwk {
    public void TestSimple() {
        CldrValue keep =
            ldml("numbers/currencies/currency[@type=\"USD\"]/displayName", "US Dollar");
        CldrValue remove =
            ldml("numbers/currencies/currency[@type=\"USD\"]/symbol", "US$");
        CldrValue replace =
            ldml("units/durationUnit[@type=\"foo\"]/durationUnitPattern", "YYY");
        CldrValue replacement =
            ldml("units/durationUnit[@type=\"foo\"]/durationUnitPattern", "ZZZ");

        CldrData src = CldrDataSupplier.forValues(ImmutableList.of(keep, remove, replace));
        CldrData filtered = new FilteredData(src) {
            @Override protected CldrValue filter(CldrValue value) {
                if (value.equals(remove)) {
                    return null;
                } else if (value.equals(replace)) {
                    return replacement;
                } else {
                    return value;
                }
            }
        };

        List<CldrValue> filteredValues = new ArrayList<>();
        filtered.accept(ARBITRARY, filteredValues::add);
        assertEquals("filtered values", filteredValues, Arrays.asList(keep, replacement));

        assertNull("removed value is null", filtered.get(remove.getPath()));
        assertEquals("keep is unchanged", filtered.get(keep.getPath()), keep);
        assertEquals("filtered is replaced", filtered.get(replace.getPath()), replacement);
    }

    public void TestBadReplacementPath() {
        CldrValue replace =
            ldml("numbers/currencies/currency[@type=\"USD\"]/displayName", "VALUE");
        CldrValue replacement =
            ldml("numbers/currencies/currency[@type=\"USD\"]/symbol", "VALUE");

        CldrData src = CldrDataSupplier.forValues(ImmutableList.of(replace));
        CldrData filtered = new FilteredData(src) {
            @Override protected CldrValue filter(CldrValue value) {
                return replacement;
            }
        };
        try {
            filtered.accept(ARBITRARY, v -> {});
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertErrorMessageContains(e, "not permitted to modify distinguishing paths");
            assertErrorMessageContains(e, replace.toString());
            assertErrorMessageContains(e, replacement.toString());
        }
    }

    public void TestBadReplacementAttributes() {
        CldrValue replace =
            ldml("numbers/currencies/currency[@type=\"USD\"]/displayName", "XXX");
        CldrValue replacement =
            ldml("numbers/currencies/currency[@type=\"GBP\"]/displayName", "XXX");

        CldrData src = CldrDataSupplier.forValues(ImmutableList.of(replace));
        CldrData filtered = new FilteredData(src) {
            @Override protected CldrValue filter(CldrValue value) {
                return replacement;
            }
        };
        try {
            filtered.accept(ARBITRARY, v -> {});
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertErrorMessageContains(e, "not permitted to modify distinguishing paths");
            assertErrorMessageContains(e, replace.toString());
            assertErrorMessageContains(e, replacement.toString());
        }
    }

    private static CldrValue ldml(String path, String value) {
        return CldrValue.parseValue("//ldml/" + path, value);
    }

    private void assertErrorMessageContains(Throwable e, String expected) {
        // This test "framework" is such a limited API it encourages brittle assertions.
        assertTrue(
            "error message \"" + e.getMessage() + "\" contains \"" + expected + "\"",
            e.getMessage().contains(expected));
    }
}
