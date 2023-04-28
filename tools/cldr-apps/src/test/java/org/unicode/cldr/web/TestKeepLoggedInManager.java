package org.unicode.cldr.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

public class TestKeepLoggedInManager {
    @Test
    public void TestSaveKey() throws IOException {
        final File dir =
                Files.createTempDirectory(TestKeepLoggedInManager.class.getSimpleName()).toFile();
        dir.deleteOnExit();

        KeepLoggedInManager klm = new KeepLoggedInManager(dir);

        // create key.
        SecretKey k1 = klm.getKey();
        assertNotNull(k1);

        // reset it
        klm.resetKey();
        SecretKey k2 = klm.getKey();
        assertNotNull(k2);
        assertNotEquals(k1, k2);

        KeepLoggedInManager klm2 = new KeepLoggedInManager(dir);

        assertEquals(klm.getKey(), klm2.getKey());
    }

    @Test
    public void TestRandomJwt() throws IOException {
        // test that we can generate a JWT using the 'usual' method and verify it
        final File dir =
                Files.createTempDirectory(TestKeepLoggedInManager.class.getSimpleName() + "2")
                        .toFile();
        dir.deleteOnExit();
        // Will generate a random key. Check round trip.
        KeepLoggedInManager klm = new KeepLoggedInManager(dir);

        final String SUBJECT = "1234";
        final String jwt = klm.createJwtForSubject(SUBJECT);
        assertNotNull(jwt);
        assertEquals(SUBJECT, klm.getSubject(jwt));
    }

    @Test
    public void TestBadJwt() throws IOException {
        // test that we can generate a JWT using the 'usual' method and verify it
        final File dir =
                Files.createTempDirectory(TestKeepLoggedInManager.class.getSimpleName() + "3")
                        .toFile();
        dir.deleteOnExit();
        // Will generate a random key. Check round trip.
        KeepLoggedInManager klm = new KeepLoggedInManager(dir);

        // JWT with some other key
        assertNull(
                klm.getSubject(
                        "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0IiwiaWF0IjoxNjgxNDEyODU4fQ.qYdLAYGPZUBGFaCZ4F4CP5erNPyU8EF8yrg2ONm1SRY"));

        // JWT with a bad key
        assertNull(klm.getSubject("not a real key"));
        assertNull(klm.getSubject("≈"));

        final String SUBJECT = "1234";
        final String expiredJwt = klm.createExpiredJwtForSubject(SUBJECT);

        // long expired JWT
        assertNull(klm.getSubject(expiredJwt));
    }
}
