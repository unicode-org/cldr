package org.unicode.cldr.unittest.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableSet;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.FileReaders;
import org.unicode.cldr.web.SubtypeToURLMap;

public class TestSubtypeToURLMap {

    @Test
    public void TestFlatFile() throws FileNotFoundException, IOException {
        final String path = "subtypes.txt";
        SubtypeToURLMap map = getTestDataAsReader(path);
        assertMapOk(map);
    }

    private SubtypeToURLMap getTestDataAsReader(final String path) {
        SubtypeToURLMap map =
                SubtypeToURLMap.getInstance(
                        FileReaders.openFile(TestSubtypeToURLMap.class, "data/" + path));
        return map;
    }

    @Test
    public void TestFlatFileURL() throws FileNotFoundException, IOException, URISyntaxException {
        final String path = "subtypes.txt";
        SubtypeToURLMap map = getTestDataAsUrl(path);
        assertMapOk(map);
    }

    private SubtypeToURLMap getTestDataAsUrl(final String path)
            throws IOException, URISyntaxException {
        SubtypeToURLMap map =
                SubtypeToURLMap.getInstance(TestSubtypeToURLMap.class.getResource("data/" + path));
        return map;
    }

    @Test
    public void TestHTMLURL() throws FileNotFoundException, IOException, URISyntaxException {

        SubtypeToURLMap map = getTestDataAsUrl("subtypes.html");
        assertMapOk(map);
    }

    private void assertMapOk(SubtypeToURLMap map) {
        assertNotNull(map, "SubtypeToURLMap from subtypes.txt");
        Collection<Subtype> handledTypes = map.getHandledTypes();
        Collection<Subtype> unHandledTypes = map.getUnhandledTypes();
        assertTrue(
                handledTypes.contains(Subtype.numberPatternNotCanonical),
                "numberPatternNotCanonical is handled");
        assertFalse(
                unHandledTypes.contains(Subtype.numberPatternNotCanonical),
                "numberPatternNotCanonical is handled");
        assertEquals(
                "https://cldr.unicode.org/translation/number-currency-formats/number-symbols",
                map.get(Subtype.numberPatternNotCanonical),
                "numberPatternNotCanonical URL");
        assertFalse(
                handledTypes.contains(Subtype.auxiliaryExemplarsOverlap),
                "auxiliaryExemplarsOverlap is NOT handled");
        assertTrue(
                unHandledTypes.contains(Subtype.auxiliaryExemplarsOverlap),
                "auxiliaryExemplarsOverlap is NOT handled");
    }

    @Test
    public void TestEmpty() throws IOException, URISyntaxException {
        Assertions.assertEquals(
                0,
                getTestDataAsUrl("subtypes_empty.txt").getHandledTypes().size(),
                "subtypes_empty.txt: empty: should have zero handled types");
    }

    @Test
    public void TestFailure() {
        ImmutableSet<String> failingMaps =
                ImmutableSet.of(
                        "subtypes_bad_badtype.txt",
                        "subtypes_bad_badurl.txt",
                        "subtypes_bad_dangling.txt",
                        "subtypes_bad_dangling2.txt",
                        "subtypes_bad_noend.txt",
                        "subtypes_bad_nostart.txt",
                        "subtypes_bad_nourl.txt");
        Assertions.assertAll(
                "bad maps",
                failingMaps.stream()
                        .map(
                                path ->
                                        () ->
                                                Assertions.assertThrows(
                                                        IllegalArgumentException.class,
                                                        () -> {
                                                            getTestDataAsUrl(path);
                                                        },
                                                        path + ": Should fail to parse")));
    }
}
