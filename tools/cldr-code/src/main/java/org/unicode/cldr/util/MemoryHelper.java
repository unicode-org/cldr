package org.unicode.cldr.util;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

public class MemoryHelper {

    /**
     * Get the amount of memory still available for us to allocate,
     * including not only freeMemory but also maxMemory - totalMemory
     *
     * https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Runtime.html#freeMemory()
     *
     * Generally: freeMemory <= totalMemory <= maxMemory
     *
     * @param callerId a string identifying the caller, used in log if verbose
     * @param verbose if true, log the stats
     * @return the available memory, in bytes
     */
    public static synchronized long availableMemory(String callerId, boolean verbose) {
        final Runtime r = Runtime.getRuntime();
        long freeMem = r.freeMemory();
        long maxMem = r.maxMemory();
        long totalMem = r.totalMemory();
        if (freeMem > totalMem || totalMem > maxMem) {
            log(callerId, "Values returned by Runtime violate assumptions!");
            verbose = true;
        }
        long availMem = freeMem + maxMem - totalMem;
        if (verbose) {
            log(callerId, "Available memory: " + humanReadableByteCountSI(availMem) +
                "; free: " +  humanReadableByteCountSI(freeMem) +
                "; max: " +  humanReadableByteCountSI(maxMem) +
                "; total: " +  humanReadableByteCountSI(totalMem));
        }
        return availMem;
    }

    private static void log(String callerId, String message) {
        System.out.println("MemoryHelper[" + callerId + "]: " + message);
    }

    /**
     * Convert a byte count to a human readable string
     * Use SI (1 k = 1,000), not Binary (1 K = 1,024)
     *
     * @param bytes the number such as 1234567890
     * @return the formatted string such as "1.2 GB"
     *
     * Source: https://programming.guide/java/formatting-byte-size-to-human-readable-format.html
    */
    public static String humanReadableByteCountSI(long bytes) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format("%.1f %cB", bytes / 1000.0, ci.current());
    }
}
