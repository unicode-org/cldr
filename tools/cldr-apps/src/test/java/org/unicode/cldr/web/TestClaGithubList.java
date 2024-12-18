package org.unicode.cldr.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.web.ClaGithubList.SignEntry;
import org.unicode.cldr.web.ClaGithubList.SignStatus;

public class TestClaGithubList {
    @Test
    void testNull() {
        assertNotNull(ClaGithubList.getInstance());
    }

    @Test
    void testFailureMode() {
        // this will throw in unit test mode
        assertThrows(
                NullPointerException.class,
                () -> ClaGithubList.getInstance().getSignStatus("github"));
    }

    @Test
    void testCannedData() throws IOException {
        final ClaGithubList l = new ClaGithubList();
        Map<String, SignEntry> m = null;

        // directly read
        try (final InputStream is =
                        TestClaGithubList.class
                                .getResource("data/" + "test-signatures.json")
                                .openStream();
                final Reader r = new InputStreamReader(is); ) {
            m = l.readSigners(r);
        }

        // make this a final for the assertAll()
        final Map<String, SignEntry> s = m;

        // some basic asserts before we dig into the data
        assertNotNull(s);
        assertFalse(s.isEmpty());
        assertAll(
                "ClaGithubList tests",
                () -> assertEquals(3, s.size()),
                () ->
                        assertThat(s.keySet())
                                .containsExactlyInAnyOrder(
                                        "TEST$ind-signer", "TEST$revoker", "TEST$corp-signer"),
                () -> assertEquals(SignStatus.signed, s.get("TEST$ind-signer").getSignStatus()),
                () -> assertEquals(SignStatus.signed, s.get("TEST$corp-signer").getSignStatus()),
                () -> assertEquals(SignStatus.revoked, s.get("TEST$revoker").getSignStatus()));
    }
}
