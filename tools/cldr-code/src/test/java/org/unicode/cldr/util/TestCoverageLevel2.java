package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.ibm.icu.util.VersionInfo;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.MultipleFailuresError;
import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.testutil.TestWithKnownIssues;
import org.unicode.cldr.tool.ToolConstants;

public class TestCoverageLevel2 extends TestWithKnownIssues {

    final int ITERATIONS = 100000; // keep this low for normal testing

    @Test
    public void TestCoveragePerf() {
        final SupplementalDataInfo sdi = CLDRConfig.getInstance().getSupplementalDataInfo();
        for (int i = 0; i < ITERATIONS; i++) {
            CoverageLevel2 c = CoverageLevel2.getInstance(sdi, "fr_CA");
            assertEquals(
                    Level.MODERATE,
                    c.getLevel(
                            "//ldml/characters/parseLenients[@scope=\"number\"][@level=\"lenient\"]/parseLenient[@sample=\",\"]"));
        }
    }

    @Test
    public void TestPriorBasicCoverage() throws IOException {
        // we need the CLDR Archive dir for this.
        assumeTrue(TestCLDRPaths.canUseArchiveDirectory());

        // Previous CLDR version
        final VersionInfo prev = ToolConstants.previousVersion();

        // read coverageLevels.txt from the *previous* version
        final CalculatedCoverageLevels prevCovLevel = CalculatedCoverageLevels.forVersion(prev);

        final Set<String> localesToCheck =
                SupplementalDataInfo.getInstance().getLanguageTcOrBasic();

        final Map<String, CoverageLevel2> covs = new HashMap<>();

        final SupplementalDataInfo sdi = CLDRConfig.getInstance().getSupplementalDataInfo();
        for (final String lang : localesToCheck) {
            covs.put(lang, CoverageLevel2.getInstance(sdi, lang));
        }

        // CLDR English File
        final CLDRFile english = CLDRConfig.getInstance().getEnglish();

        Assertions.assertAll(
                () -> checkLanguageCoverage(prevCovLevel, covs, english),
                () -> checkScriptCoverage(prevCovLevel, covs, english),
                // region coverage elsewhere?
                () -> checkVariantCoverage(prevCovLevel, covs, english));
    }

    private void checkLanguageCoverage(
            final CalculatedCoverageLevels prevCovLevel,
            final Map<String, CoverageLevel2> covs,
            final CLDRFile english)
            throws MultipleFailuresError {

        // configuration
        final Level failIfAbove = Level.MODERN;
        final String XPATH = "//ldml/localeDisplayNames/languages/language";

        // all languages of previous coverage levels at basic+
        final Set<String> typesAtBasic =
                prevCovLevel.levels.keySet().stream()
                        .filter(l -> prevCovLevel.isLocaleAtLeastBasic(l))
                        .map(l -> CLDRLocale.getInstance(l).getLanguage())
                        .collect(Collectors.toSet());

        assertMissingCoverage(covs, english, failIfAbove, XPATH, typesAtBasic);
    }

    private void checkScriptCoverage(
            final CalculatedCoverageLevels prevCovLevel,
            final Map<String, CoverageLevel2> covs,
            final CLDRFile english)
            throws MultipleFailuresError {

        // configuration
        final Level failIfAbove = Level.MODERN;
        final String XPATH = "//ldml/localeDisplayNames/scripts/script";

        // all scripts of previous coverage levels at basic+
        final Set<String> typesAtBasic =
                prevCovLevel.levels.keySet().stream()
                        .filter(l -> prevCovLevel.isLocaleAtLeastBasic(l))
                        .map(l -> CLDRLocale.getInstance(l))
                        .map(
                                l -> {
                                    final CLDRLocale max = l.getMaximal();
                                    assertNotNull(max, () -> "Max locale for " + l);
                                    final String script = max.getScript();
                                    assertNotNull(
                                            script,
                                            () -> "Script for " + max + " which is max for " + l);
                                    return script;
                                })
                        .collect(Collectors.toSet());

        assertMissingCoverage(covs, english, failIfAbove, XPATH, typesAtBasic);
    }

    private void checkVariantCoverage(
            final CalculatedCoverageLevels prevCovLevel,
            final Map<String, CoverageLevel2> covs,
            final CLDRFile english)
            throws MultipleFailuresError {

        // configuration
        final Level failIfAbove = Level.MODERN;
        final String XPATH = "//ldml/localeDisplayNames/variants/variant";

        // We need all locales for looking for variants
        final Set<CLDRLocale> allLocales =
                CLDRConfig.getInstance().getFullCldrFactory().getAvailableCLDRLocales();

        // get all of the "raw" locales mentioned in coverage
        final Set<CLDRLocale> localesAtBasic =
                prevCovLevel.levels.keySet().stream()
                        .filter(l -> prevCovLevel.isLocaleAtLeastBasic(l))
                        .map(l -> CLDRLocale.getInstance(l))
                        .collect(Collectors.toSet());

        final Set<CLDRLocale> localesWithVariant =
                allLocales.stream()
                        .filter(l -> !l.getVariant().isEmpty())
                        .collect(Collectors.toSet());
        final Set<CLDRLocale> variantLocalesInCoverage =
                localesWithVariant.stream()
                        .filter(l -> localesAtBasic.stream().anyMatch(p -> l.childOf(p)))
                        .collect(Collectors.toSet());
        final Set<String> typesAtBasic =
                variantLocalesInCoverage.stream()
                        .map(l -> l.getVariant())
                        .collect(Collectors.toSet());

        assertMissingCoverage(covs, english, failIfAbove, XPATH, typesAtBasic);
    }

    /**
     * Given types (script code, etc) at basic, check coverage and English inclusion.
     *
     * @param XPath in question - this will be mutated.
     */
    private void assertMissingCoverage(
            final Map<String, CoverageLevel2> covs,
            final CLDRFile english,
            final Level failIfAbove,
            final String xpath,
            final Set<String> typesAtBasic)
            throws MultipleFailuresError {
        // Our xpath: the leaf node
        final XPathParts xpp = XPathParts.getFrozenInstance(xpath).cloneAsThawed();

        // Result: types not in en.xml
        final Set<String> notInEnglish = new TreeSet<>();
        // Result: types not in coverage
        final Set<String> notInCoverage = new TreeSet<>();

        collectMissingTypes(
                covs, english, failIfAbove, xpp, notInEnglish, notInCoverage, typesAtBasic);

        assertMissingCoverage(failIfAbove, notInEnglish, notInCoverage, xpp);
    }

    /**
     * Given types (script code, etc) at basic, check coverage and English inclusion.
     *
     * @param xpath XPath in question - this will be mutated.
     */
    private void collectMissingTypes(
            final Map<String, CoverageLevel2> covs,
            final CLDRFile english,
            final Level failIfAbove,
            final XPathParts xpp,
            final Set<String> notInEnglish,
            final Set<String> notInCoverage,
            final Set<String> typesAtBasic) {
        for (final String type : typesAtBasic) {
            xpp.setAttribute(-1, LDMLConstants.TYPE, type);
            final String xpath = xpp.toString();

            if (!english.isHere(xpath)) {
                // fail if not in English
                notInEnglish.add(type);
            }

            if (covs.values().stream()
                    .anyMatch((cov) -> cov.getLevel(xpath).isAbove(failIfAbove))) {
                // fail if level > failIfAbove for any of those locales
                notInCoverage.add(type);
            }
        }
    }

    /** Bring the bad news (if any). Reporting factored out here. */
    private void assertMissingCoverage(
            final Level failIfAbove,
            final Set<String> notInEnglish,
            final Set<String> notInCoverage,
            final XPathParts xpp)
            throws MultipleFailuresError {
        // given xpp is  scripts/script or languages/language, etc.
        final String plural = xpp.getElement(-2); // the plural form of what we're looking for
        Assertions.assertAll(
                () ->
                        assertTrue(
                                notInEnglish.isEmpty(),
                                () ->
                                        String.format(
                                                "en.xml missing these %s: %s",
                                                plural, notInEnglish.toString())),
                () -> {
                    final Supplier<String> formatter =
                            () ->
                                    String.format(
                                            "coverageLevels.xml has level > %s for these %s: %s",
                                            failIfAbove, plural, notInCoverage.toString());
                    if (!notInCoverage.isEmpty() && plural.equals("variants")) {
                        if (logKnownIssue("CLDR-18480", formatter.get())) {
                            return;
                        }
                    }
                    assertTrue(notInCoverage.isEmpty(), formatter);
                });
    }
}
