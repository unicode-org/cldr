package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.ibm.icu.util.VersionInfo;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.tool.ToolConstants;
import org.unicode.cldr.util.StandardCodes.CodeType;

public class TestCoverageLevel2 {

    final int ITERATIONS = 100000; // keep this low for normal testing

    private static SupplementalDataInfo sdi;

    @BeforeAll
    private static void setup() {
        sdi = CLDRConfig.getInstance().getSupplementalDataInfo();
        CoverageLevel2 c = CoverageLevel2.getInstance(sdi, "fr_CA");
    }

    @Test
    public void TestCoveragePerf() {
        for (int i = 0; i < ITERATIONS; i++) {
            CoverageLevel2 c = CoverageLevel2.getInstance(sdi, "fr_CA");
            assertEquals(
                    Level.MODERATE,
                    c.getLevel(
                            "//ldml/characters/parseLenients[@scope=\"number\"][@level=\"lenient\"]/parseLenient[@sample=\",\"]"));
        }
    }

    @Test
    public void TestPriorBasicLanguage() throws IOException {
        // Fail if the language name is at above this level
        final Level failIfAbove = Level.MODERN;

        // we need the CLDR Archive dir for this.
        assumeTrue(TestCLDRPaths.canUseArchiveDirectory());

        // Previous CLDR version
        final VersionInfo prev = ToolConstants.previousVersion();
        // read coverageLevels.txt from the *previous* version
        final CalculatedCoverageLevels prevCovLevel = CalculatedCoverageLevels.forVersion(prev);
        // Our xpath: the language leaf
        final XPathParts xpp =
                XPathParts.getFrozenInstance("//ldml/localeDisplayNames/languages/language")
                        .cloneAsThawed();
        // CLDR English File
        final CLDRFile english = CLDRConfig.getInstance().getEnglish();

        // Result: locales not in en.xml
        final Set<String> notInEnglish = new TreeSet<>();
        // Result: locales not in coverage
        final Set<String> notInCoverage = new TreeSet<>();

        final Set<String> localesToCheck =
                SupplementalDataInfo.getInstance().getLanguageTcOrBasic();
        final Map<String, CoverageLevel2> covs = new HashMap<>();

        for (final String lang : localesToCheck) {
            covs.put(lang, CoverageLevel2.getInstance(sdi, lang));
        }

        for (final String lang : StandardCodes.make().getAvailableCodes(CodeType.language)) {
            if (prevCovLevel.isLocaleAtLeastBasic(lang)) {
                xpp.setAttribute(-1, LDMLConstants.TYPE, lang);
                final String xpath = xpp.toString();

                if (!english.isHere(xpath.toString())) {
                    // fail if not in English
                    notInEnglish.add(lang);
                }

                if (covs.values().stream()
                        .anyMatch((cov) -> cov.getLevel(xpath.toString()).isAbove(failIfAbove))) {
                    // fail if level > failIfAbove for any of those locales
                    notInCoverage.add(lang);
                }
            }
        }

        Assertions.assertAll(
                () ->
                        assertTrue(
                                notInEnglish.isEmpty(),
                                () ->
                                        "en.xml is missing translations for these languages' names:"
                                                + notInEnglish.toString()),
                () ->
                        assertTrue(
                                notInCoverage.isEmpty(),
                                () ->
                                        "coverageLevels.xml has a coverage level >"
                                                + failIfAbove
                                                + " for these language's names:"
                                                + notInCoverage.toString()));
    }
}
