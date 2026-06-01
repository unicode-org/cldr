package org.unicode.cldr.unittest;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.test.CheckCLDR.Options;
import org.unicode.cldr.util.CLDRConfig;

abstract class TestFmwkForChecks extends TestFmwkPlus {

    public void assertChecks(
            String locale,
            Map<String, String> pathValuePairs,
            TestFactory factory,
            Set<String> pathsExpectingError,
            CheckCLDR cdc,
            final Subtype expectedSubtype) {
        if (pathsExpectingError == null) pathsExpectingError = pathValuePairs.keySet();
        cdc.setEnglishFile(CLDRConfig.getInstance().getEnglish());

        List<CheckStatus> possibleErrors = new ArrayList<>();
        cdc.setCldrFileToCheck(
                factory.make(locale, true), new Options(ImmutableMap.of()), possibleErrors);
        assertEquals("top level errors", Collections.emptyList(), possibleErrors);
        for (Entry<String, String> entry : pathValuePairs.entrySet()) {
            possibleErrors.clear();
            final String path = entry.getKey();
            cdc.check(path, path, entry.getValue(), new Options(ImmutableMap.of()), possibleErrors);
            if (pathsExpectingError.contains(path)) {
                // We expect an error.
                if (!assertNotEquals(
                        "Expected:" + entry.toString(), Collections.emptyList(), possibleErrors)) {
                    continue; // get out, no reason for extra errors
                }
                // if there is an issue, there should be a single error for this xpath
                assertEquals("Expected:" + entry.toString(), 1, possibleErrors.size());
                assertEquals(
                        "Expected:" + entry.toString(),
                        expectedSubtype,
                        possibleErrors.get(0).getSubtype());
            } else {
                assertEquals(
                        "Unexpected: " + entry.toString(), Collections.emptyList(), possibleErrors);
            }
        }
    }
}
