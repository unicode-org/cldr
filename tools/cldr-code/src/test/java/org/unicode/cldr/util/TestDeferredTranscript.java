package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TestDeferredTranscript {
    @Test
    public void TestBasic() {
        DeferredTranscript dt = new DeferredTranscript();

        dt.clear();

        assertEquals("", dt.get());
        assertEquals("", dt.get());
        dt.clear();
        dt.add("Hello");
        assertEquals("Hello", dt.get());
        // all of these are ignored, we've already committed the output.
        dt.add("Hello");
        dt.add("Hello");
        dt.add("Hello");
        dt.add("Goodbye %d times", 999);
        assertEquals("Hello", dt.get());
        dt.clear();
        dt.add("Hello");
        dt.add("Hello");
        assertEquals("Hello\nHello", dt.get());
        assertEquals("Hello\nHello", dt.get());
        dt.clear();
        assertEquals("", dt.get());
        dt.clear();
        dt.add("You have %d problems.", 0);
        dt.add("Thanks");
        assertEquals("You have 0 problems.\nThanks", dt.get());
        dt.add("Thanks");
        assertEquals("You have 0 problems.\nThanks", dt.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void TestPerf(boolean doGet) {
        DeferredTranscript dt = new DeferredTranscript();
        for (int i = 0; i < 100000; i++) {
            dt.add("Now you have %d problem(s).", i);
        }
        // 1M iterations:  2.3 seconds without get(), 10 seconds with
        if (doGet) {
            // if doGet is false, the above code is faster.
            // defer the formatting
            final String s0 = dt.get();
            assertNotNull(s0);
            final String s1 = dt.get();
            assertEquals(s0, s1);
        }
    }
}
