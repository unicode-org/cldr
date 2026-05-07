package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;
import org.unicode.cldr.unittest.TestShim;

public class TestStringId {
    @Test
    void testStringIdMatch() {
        assumeTrue(TestShim.getInclusion() > 9, "Skip if not -e 10"); // skip test if not in exhaustive mode
        for(final String x : CLDRConfig.getInstance().getEnglish().fullIterable()) {
            final String id = StringId.getHexId(x);
            final int l = id.length();
            // Empirically, all StringIds are 11…16 character IDs. See also CLDR-6303
            assertTrue((l <= 16) && (l >= 11), () -> String.format("Length %d of %s = %s", l, id, x));
        }
    }
}
