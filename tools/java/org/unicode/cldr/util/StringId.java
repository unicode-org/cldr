package org.unicode.cldr.util;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Produce an ID for a string based on a long hash. When used properly, the odds
 * of collision are so low that the ID can be used as a proxy for the
 * original string. The ID is non-negative. The algorithm uses SHA-1 over the
 * UTF-8 bytes in the string. Also provides lookup for long previously generated for string.
 *
 * @author markdavis
 */
public final class StringId {
    private static final Map<String, Long> STRING_TO_ID = new ConcurrentHashMap<String, Long>();
    private static final Map<Long, String> ID_TO_STRING = new ConcurrentHashMap<Long, String>();
    private static final MessageDigest digest;
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final int RETRY_LIMIT = 9;
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
     * @param string
     *            input string.
     * @return a value from 0 to 0x7FFFFFFFFFFFFFFFL.
     */
    public static long getId(CharSequence charSequence) {
        String string = charSequence.toString();
        Long resultLong = STRING_TO_ID.get(string);
        if (resultLong != null) {
            return resultLong;
        }
        int retryCount = RETRY_LIMIT;
        while (true) {
            try {
                synchronized (digest) {
                    byte[] hash = digest.digest(string.getBytes(UTF_8));
                    long result = 0;
                    for (int i = 0; i < 8; ++i) {
                        result <<= 8;
                        result ^= hash[i];
                    }
                    // mash the top bit to make things easier
                    result &= 0x7FFFFFFFFFFFFFFFL;
                    STRING_TO_ID.put(string, result);
                    ID_TO_STRING.put(result, string);
                    return result;
                }
            } catch (RuntimeException e) {
                if (--retryCount < 0) {
                    throw e;
                }
            }
        }
    }

    /**
     * Get the hex ID for a string.
     *
     * @param string
     *            input string.
     * @return a string with the hex value
     */
    public static String getHexId(CharSequence string) {
        return Long.toHexString(getId(string));
    }

    /**
     * Get the hex ID for a string.
     *
     * @param string
     *            input string.
     * @return a string with the hex value
     */
    public static String getStringFromHexId(String string) {
        return getStringFromId(Long.parseLong(string, 16));
    }

    /**
     * Returns string previously used to generate the longValue with getId.
     * @param longValue
     * @return String previously used to generate the longValue with getId.
     */
    public static String getStringFromId(long longValue) {
        return ID_TO_STRING.get(longValue);
    }
}