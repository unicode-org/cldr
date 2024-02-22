package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibm.icu.util.Output;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.unicode.cldr.unittest.TestUtilities;
import org.unicode.cldr.util.VoteResolver.Status;

/**
 * @see org.unicode.cldr.unittest.TestUtilities
 * @see org.unicode.cldr.unittest.TestUtilities#TestUser
 */
public class TestVoteResolver {

    @Test
    void testDisputed() {
        final VoteResolver<String> vr = getStringResolver();
        vr.setLocale(
                CLDRLocale.getInstance("fr"), null); // NB: pathHeader is needed for annotations
        vr.setBaseline("Bouvet", Status.unconfirmed);
        vr.setBaileyValue("BV");

        // A date in 2017
        final Date t0 = new Date(1500000000000L);
        // A date in 2020
        final Date t1 = new Date(1600000000000L);

        assertTrue(t0.before(t1));

        // Vote with a date in the past, this will lose the org dispute
        vr.add("Bouvet", TestUtilities.TestUser.googleV.voterId, null, t0);

        vr.add("Illa Bouvet", TestUtilities.TestUser.googleV2.voterId, null, t1);
        vr.add("Illa Bouvet", TestUtilities.TestUser.appleV.voterId, null, t1);
        vr.add("Illa Bouvet", TestUtilities.TestUser.unaffiliatedS.voterId, null, t1);
        assertAll(
                "Verify the outcome",
                () -> assertEquals("Illa Bouvet", vr.getWinningValue()),
                () ->
                        assertEquals(
                                VoteResolver.VoteStatus.ok,
                                vr.getStatusForOrganization(Organization.google)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testPerf(boolean doGet) {
        final VoteResolver<String> vr = getStringResolver();
        vr.enableTranscript();

        for (int i = 0; i < 100; i++) {
            vr.clear();
            vr.setLocale(
                    CLDRLocale.getInstance("fr"), null); // NB: pathHeader is needed for annotations
            vr.setBaseline("bafut", Status.unconfirmed);
            vr.setBaileyValue("bfd");
            vr.add("bambara", TestUtilities.TestUser.appleV.voterId);
            vr.add("bafia", TestUtilities.TestUser.googleV.voterId);
            vr.add("bassa", TestUtilities.TestUser.googleV2.voterId);
            vr.add("bafut", TestUtilities.TestUser.unaffiliatedS.voterId);

            assertAll(
                    "Verify the outcome",
                    () -> assertEquals("bambara", vr.getWinningValue()),
                    () -> assertEquals(Status.provisional, vr.getWinningStatus()));

            // about 10x faster without calling get()
            if (doGet) {
                assertTrue(
                        vr.getTranscript().contains("earlier than 'bassa'"),
                        () -> "Transcript did not match expectations:\n" + vr.getTranscript());
            }
        }
    }

    @Test
    void testExplanations() {
        // Example from https://st.unicode.org/cldr-apps/v#/fr/Languages_A_D/54dc38b9b6c86cac
        final VoteResolver<String> vr = getStringResolver();

        vr.setLocale(
                CLDRLocale.getInstance("fr"), null); // NB: pathHeader is needed for annotations
        vr.setBaseline("bafut", Status.unconfirmed);
        vr.setBaileyValue("bfd");
        vr.add("bambara", TestUtilities.TestUser.appleV.voterId);
        vr.add("bafia", TestUtilities.TestUser.googleV.voterId);
        vr.add("bassa", TestUtilities.TestUser.googleV2.voterId);
        vr.add("bafut", TestUtilities.TestUser.unaffiliatedS.voterId);

        vr.enableTranscript(); // Should be recalculated from here.
        assertAll(
                "Verify the outcome",
                () -> assertEquals("bambara", vr.getWinningValue()),
                () -> assertEquals(Status.provisional, vr.getWinningStatus()));
        final String transcriptText = vr.getTranscript();
        System.out.println(transcriptText);
        System.out.println(vr.toString()); // NB:  toString() modifies the transcript!
        assertTrue(
                transcriptText.contains("earlier than 'bassa'"),
                () -> "Transcript did not match expectations:\n" + transcriptText);
    }

    private VoteResolver<String> getStringResolver() {
        return new VoteResolver<String>(TestUtilities.getTestVoterInfoList());
    }

    /**
     * Test VoteResolver.reviseInheritanceAsNeeded
     *
     * <p>Inheritance marker should remain unchanged if pathWhereFound.value.equals(path).
     *
     * <p>Bailey should change to inheritance marker if pathWhereFound.value.equals(path).
     *
     * <p>Inheritance marker should change to bailey if !pathWhereFound.value.equals(path).
     *
     * <p>Bailey should remain unchanged if !pathWhereFound.value.equals(path).
     *
     * <p>Any other value should remain unchanged no matter what.
     */
    @Test
    public void testReviseInheritance() {
        if (!VoteResolver.DROP_HARD_INHERITANCE) {
            return;
        }
        final String localeId = "fr_CA";
        final CLDRFile cldrFile = CLDRConfig.getInstance().getCLDRFile(localeId, true);

        // Go through all paths until we have tested one for which pathWhereFound is the same,
        // and another one for which pathWhereFound is different
        boolean gotSamePath = false, gotDifferentPath = false;
        for (String path : cldrFile.fullIterable()) {
            Output<String> pathWhereFound = new Output<>();
            String baileyValue = cldrFile.getBaileyValue(path, pathWhereFound, null);
            if (baileyValue == null) {
                continue;
            }
            String anyOtherValue = "x" + baileyValue; // anything but bailey or inheritance marker
            if (pathWhereFound.value.equals(path)) {
                if (!gotSamePath) {
                    gotSamePath = true;
                    testSamePath(path, baileyValue, anyOtherValue, cldrFile);
                }
            } else {
                if (!gotDifferentPath) {
                    gotDifferentPath = true;
                    testDifferentPath(path, baileyValue, anyOtherValue, cldrFile);
                }
            }
            if (gotSamePath && gotDifferentPath) {
                return;
            }
        }
        assertTrue(gotSamePath);
        assertTrue(gotDifferentPath);
    }

    private void testSamePath(
            String path, String baileyValue, String anyOtherValue, CLDRFile cldrFile) {
        String value1 =
                VoteResolver.reviseInheritanceAsNeeded(
                        path, CldrUtility.INHERITANCE_MARKER, cldrFile);
        assertEquals(
                CldrUtility.INHERITANCE_MARKER,
                value1,
                "inheritance marker should remain unchanged");

        String value2 = VoteResolver.reviseInheritanceAsNeeded(path, baileyValue, cldrFile);
        assertEquals(
                CldrUtility.INHERITANCE_MARKER,
                value2,
                "bailey should change to inheritance marker");

        String value3 = VoteResolver.reviseInheritanceAsNeeded(path, anyOtherValue, cldrFile);
        assertEquals(
                anyOtherValue,
                value3,
                "any other value should remain unchanged when paths are same");
    }

    private void testDifferentPath(
            String path, String baileyValue, String anyOtherValue, CLDRFile cldrFile) {
        String value1 =
                VoteResolver.reviseInheritanceAsNeeded(
                        path, CldrUtility.INHERITANCE_MARKER, cldrFile);
        assertEquals(baileyValue, value1, "inheritance marker should change to bailey");

        String value2 = VoteResolver.reviseInheritanceAsNeeded(path, baileyValue, cldrFile);
        assertEquals(baileyValue, value2, "bailey should remain unchanged");

        String value3 = VoteResolver.reviseInheritanceAsNeeded(path, anyOtherValue, cldrFile);
        assertEquals(
                anyOtherValue,
                value3,
                "any other value should remain unchanged when paths are different");
    }
}
