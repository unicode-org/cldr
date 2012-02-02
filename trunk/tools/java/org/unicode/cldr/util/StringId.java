package org.unicode.cldr.util;

import java.nio.charset.Charset;
import java.security.MessageDigest;

/**
 * Produce an ID for a string based on a long hash. When used properly, the odds
 * of collision are so low that the ID can be used as a proxy for the
 * original string. The ID is non-negative. The algorithm uses SHA-1 over the
 * UTF-8 bytes in the string.
 * 
 * @author markdavis
 */
public final class StringId {
    private static final MessageDigest digest;
    private static final Charset       UTF_8 = Charset.forName("UTF-8");
    static {
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (Exception e) {
            throw new IllegalArgumentException(e); // darn'd checked exceptions
        }
    }

    /**
     * Get the ID for a string.
     * 
     * @param string input string.
     * @return a value from 0 to 0x7FFFFFFFFFFFFFFFL.
     */
    public static long getId(CharSequence string) {
        byte[] hash = digest.digest(string.toString().getBytes(UTF_8));
        long result = 0;
        for (int i = 0; i < 8; ++i) {
            result <<= 8;
            result ^= hash[i];
        }
        // mash the top bit to make things easier
        result &= 0x7FFFFFFFFFFFFFFFL;
        return result;
    }
}