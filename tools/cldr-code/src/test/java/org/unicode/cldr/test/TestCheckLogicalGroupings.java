package org.unicode.cldr.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.Options;
import org.unicode.cldr.test.CheckCLDR.Phase;
import org.unicode.cldr.util.*;
import org.unicode.cldr.util.CLDRFile.DraftStatus;

public class TestCheckLogicalGroupings {
    @Test
    void testSlovenianGroup() {
        CLDRConfig config = CLDRConfig.getInstance();
        Factory cldrFactory = config.getCldrFactory();
        CheckLogicalGroupings clg = new CheckLogicalGroupings(cldrFactory);
        final String localeID = "sl";
        // Prefix for the decade
        final String DECADE_PARENT =
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"duration-decade\"]/unitPattern";
        final String DECADE = DECADE_PARENT + "[@count=\"few\"]";
        String xpathAccusative = DECADE + "[@case=\"accusative\"]";
        String xpathNominative = DECADE + "";
        String accusativeValue = "{0} dekadi";

        SimpleXMLSource xs = new SimpleXMLSource(localeID);
        // Simplify: pre-load matching paths to get us started.
        // So we don't have to copy everything.
        // These will all be marked as approved, regardless of the data.
        CLDRFile baseFile = config.getCLDRFile(localeID, true);
        int n = 0;
        for (final String xpath : baseFile.fullIterable()) {
            if (!xpath.startsWith(DECADE_PARENT)) continue;
            // All other paths:
            xs.add(xpath, "{0} dekd" + n); // {0}  dekd0, dekd1, dekd2… 
        }
        // Now, set up our situation. Nominative is approved.
        xs.add(xpathNominative + DraftStatus.approved.asXpath(), "{0} dekade");
        // OK, now let's do per-path tests
        assertCheckStatus(
                localeID,
                clg,
                xpathAccusative,
                xs,
                accusativeValue,
                DraftStatus.unconfirmed,
                true); // still errs
        assertCheckStatus(
                localeID,
                clg,
                xpathAccusative,
                xs,
                accusativeValue,
                DraftStatus.provisional,
                true); // still errs
        assertCheckStatus(
                localeID,
                clg,
                xpathAccusative,
                xs,
                accusativeValue,
                DraftStatus.contributed,
                false); // no errs with contributed (this is the fix)
        assertCheckStatus(
                localeID,
                clg,
                xpathAccusative,
                xs,
                accusativeValue,
                DraftStatus.approved,
                false); // no errs with approved
    }

    private void assertCheckStatus(
            String localeID,
            CheckLogicalGroupings clg,
            String baseXpath,
            SimpleXMLSource xs,
            String value,
            DraftStatus toCheck,
            boolean expectError) {
        final String xpath = baseXpath + toCheck.asXpath();
        xs.add(xpath, value);
        CLDRFile f = new CLDRFile(xs);
        Options o =
                new Options(
                        CLDRLocale.getInstance(localeID),
                        Phase.VETTING,
                        Level.MODERN.getAltName(),
                        "organization");
        List<CheckStatus> possibleErrors = new LinkedList<>();
        clg.setCldrFileToCheck(f, o, possibleErrors);
        // no errors at this point
        assertTrue(possibleErrors.isEmpty());

        List<CheckStatus> possibleErrors2 = new LinkedList<>();
        clg.check(baseXpath, xpath, value, o, possibleErrors2);

        if (expectError) {
            assertNotEquals(
                    0,
                    possibleErrors2.size(),
                    "expected errors for status=" + toCheck + " on " + xpath);
        } else {
            assertEquals(
                    0,
                    possibleErrors2.size(),
                    () ->
                            "Expected 0 errors but got "
                                    + possibleErrors2.get(0).toString()
                                    + " for "
                                    + xpath);
        }
    }

    /**
     * All non-optional paths that are in the same logical group should have the same coverage level
     */
    @Test
    void testSameCoverageLevel() {
        String[] locales = {"am", "en", "fr", "pt_PT", "zh"};
        for (String localeId : locales) {
            sameLevel(localeId);
        }
    }

    void sameLevel(String localeId) {
        final CLDRConfig config = CLDRConfig.getInstance();
        final CLDRFile cldrFile = config.getCLDRFile(localeId, true);
        final SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
        final CoverageLevel2 coverageLevel = CoverageLevel2.getInstance(sdi, localeId);
        for (final String path : cldrFile.fullIterable()) {
            Set<String> grouping = LogicalGrouping.getPaths(cldrFile, path);
            if (grouping != null && grouping.size() > 1) {
                LogicalGrouping.removeOptionalPaths(grouping, cldrFile);
                if (grouping.size() > 1) {
                    testGrouping(grouping, coverageLevel, localeId);
                }
            }
        }
    }

    private void testGrouping(Set<String> grouping, CoverageLevel2 coverageLevel, String localeId) {
        Level firstLevel = Level.UNDETERMINED;
        for (final String path : grouping) {
            final Level level = coverageLevel.getLevel(path);
            assertNotEquals(level, Level.UNDETERMINED, localeId + " " + path);
            if (firstLevel == Level.UNDETERMINED) {
                firstLevel = level;
            } else if (level != firstLevel) {
                assertEquals(
                        firstLevel, level, groupingDescription(grouping, coverageLevel, localeId));
            }
        }
    }

    private String groupingDescription(
            Set<String> grouping, CoverageLevel2 coverageLevel, String localeId) {
        String desc = "Locale: " + localeId + "\n" + "Group size: " + grouping.size() + "\n";
        for (final String path : grouping) {
            final Level level = coverageLevel.getLevel(path);
            desc += level + " " + path + "\n";
        }
        return desc;
    }

    /** Present optional path plus missing required path in same group should cause error */
    @Test
    void testPresentOptionalPlusMissingRequired() {
        final String localeID = "am";
        final String value = "x";
        final CLDRConfig config = CLDRConfig.getInstance();
        final Factory cldrFactory = config.getCldrFactory();
        final CheckLogicalGroupings clg = new CheckLogicalGroupings(cldrFactory);
        final String requiredPath1 =
                "//ldml/dates/fields/field[@type=\"day\"]/relative[@type=\"-1\"]";
        final String requiredPath2 =
                "//ldml/dates/fields/field[@type=\"day\"]/relative[@type=\"0\"]";
        final String requiredPath3 =
                "//ldml/dates/fields/field[@type=\"day\"]/relative[@type=\"1\"]";
        final String optionalPath =
                "//ldml/dates/fields/field[@type=\"day\"]/relative[@type=\"-2\"]";
        final SimpleXMLSource xs = new SimpleXMLSource(localeID);
        // all paths missing: no error
        assertCheckStatus2(localeID, clg, requiredPath1, xs, false);
        xs.add(optionalPath, value);
        // present optional but missing required: error
        assertCheckStatus2(localeID, clg, requiredPath1, xs, true);
        // error is only on the first required path, no error on 2nd required path or on optional
        // path:
        assertCheckStatus2(localeID, clg, requiredPath2, xs, false);
        assertCheckStatus2(localeID, clg, optionalPath, xs, false);
        xs.add(requiredPath1, value);
        xs.add(requiredPath2, value);
        xs.add(requiredPath3, value);
        // all required paths present: no error
        assertCheckStatus2(localeID, clg, requiredPath1, xs, false);
    }

    private void assertCheckStatus2(
            String localeID,
            CheckLogicalGroupings clg,
            String xpath,
            SimpleXMLSource xs,
            boolean expectError) {
        final CLDRFile f = new CLDRFile(xs);
        final Options o =
                new Options(
                        CLDRLocale.getInstance(localeID),
                        Phase.VETTING,
                        Level.MODERN.getAltName(),
                        "organization");
        final List<CheckStatus> possibleErrors = new LinkedList<>();
        clg.setCldrFileToCheck(f, o, possibleErrors);
        assertEquals(
                0,
                possibleErrors.size(),
                () ->
                        "For setCldrFileToCheck, expected 0 errors but got "
                                + possibleErrors.get(0).toString()
                                + " for "
                                + xpath);
        final List<CheckStatus> possibleErrors2 = new LinkedList<>();
        clg.check(xpath, xpath, null, o, possibleErrors2);
        if (expectError) {
            assertNotEquals(0, possibleErrors2.size(), "expected errors on " + xpath);
        } else {
            assertEquals(
                    0,
                    possibleErrors2.size(),
                    () ->
                            "Expected 0 errors but got "
                                    + possibleErrors2.get(0).toString()
                                    + " for "
                                    + xpath);
        }
    }
}
