package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibm.icu.util.Output;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.unittest.TestUtilities;
import org.unicode.cldr.util.VoteResolver.Status;

/**
 * @see org.unicode.cldr.unittest.TestUtilities
 * @see org.unicode.cldr.unittest.TestUtilities#TestUser
 */
public class TestVoteResolver {
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
