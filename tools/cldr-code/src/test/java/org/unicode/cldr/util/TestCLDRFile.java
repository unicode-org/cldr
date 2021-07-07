package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.unicode.cldr.test.CheckMetazones;

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
}
