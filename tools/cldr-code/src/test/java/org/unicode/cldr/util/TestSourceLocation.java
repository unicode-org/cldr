package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.XMLSource.SourceLocation;

public class TestSourceLocation {
    @Test
    void testSourcePath() {
        final String path_f = "/a/b/c/d.xml";
        final String path_b = "/a/b";
        final SourceLocation location = new SourceLocation(path_f, 123, 456);
        assertEquals(path_f + ":123:456: ", location.toString());
        assertEquals("c/d.xml" + ":123:456: ", location.toString(path_b));
        assertEquals("c/d.xml" + ":123:456: ", location.toString(path_b + "/")); // with trailing slash
    }
}
