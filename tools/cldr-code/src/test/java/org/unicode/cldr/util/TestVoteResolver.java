package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
