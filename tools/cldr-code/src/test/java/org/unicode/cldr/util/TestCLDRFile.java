package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.unicode.cldr.test.CheckMetazones;
import org.unicode.cldr.util.CLDRFile.DraftStatus;

/**
 * This contains additional tests in JUnit.
 *
 * @see {@link org.unicode.cldr.unittest.TestCLDRFile}
 * @see {@link CLDRFile}
 */
public class TestCLDRFile {

    static Factory factory = null;

    @BeforeAll
    public static void setUp() {
        factory = CLDRConfig.getInstance().getFullCldrFactory();
    }

    @ParameterizedTest
    @ValueSource(strings = {"de","fr","root",})
    public void TestExtraMetazonePaths(String locale) {
        CLDRFile f = factory.make(locale, true);
        assertNotNull(f, "CLDRFile for " + locale);
        Set<String> rawExtraPaths = f.getRawExtraPaths();
        assertNotNull(rawExtraPaths, "RawExtraPaths for " + locale);
        for (final String path : rawExtraPaths) {
            if (path.indexOf("/metazone") >= 0) {
                assertFalse(CheckMetazones.isDSTPathForNonDSTMetazone(path), "DST path for non-DST zone: "+locale+":"+path);
            }
        }
    }

    @ParameterizedTest
    @EnumSource( value = DraftStatus.class )
    public void TestDraftStatus(DraftStatus status) {
        final String asXpath = status.asXpath();
        if (status == DraftStatus.approved) {
            assertEquals("", asXpath, "for " + status);
        } else {
            assertNotEquals("", asXpath, "for " + status);
        }
        assertAll("misc tests for " + status,
            () -> assertEquals(status, DraftStatus.forXpath("//ldml/someLeaf" + asXpath)),
            () -> assertEquals(status, DraftStatus.forString(status.name())),
            () -> assertEquals(status, DraftStatus.forString(status.name().toUpperCase()))
        );
    }

    @Test
    public void FailTest() throws Exception {
        // TODO: remove this!
        // TODO: remove this!
        // TODO: remove this!
        // TODO: remove this!
        // TODO: remove this!
        // TODO: remove this!
        throw new Exception();
    }
}
