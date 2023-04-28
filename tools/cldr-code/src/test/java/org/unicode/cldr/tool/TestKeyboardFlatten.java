package org.unicode.cldr.tool;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.unicode.cldr.util.CLDRConfig;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class TestKeyboardFlatten {
    @ParameterizedTest
    @ValueSource(
            strings = {
                "KeyboardFlatten/broken-import-wrongparent.xml",
                "KeyboardFlatten/broken-import-unknownbase.xml",
                "KeyboardFlatten/broken-import-unknownver.xml",
                "KeyboardFlatten/broken-import-missing.xml",
            })
    void TestBrokenImports(final String path) throws IOException {
        try (final InputStream input = TestKeyboardFlatten.class.getResourceAsStream(path); ) {
            final InputSource source = new InputSource(input);
            // Expect failure.
            assertThrows(
                    IllegalArgumentException.class,
                    () -> KeyboardFlatten.flatten(source, path, System.out));
        }
    }

    @Test
    void TestImportMaltese()
            throws TransformerConfigurationException, SAXException, TransformerException,
                    TransformerFactoryConfigurationError, IOException {
        final File base = CLDRConfig.getInstance().getCldrBaseDirectory();
        final File mtxml = new File(base, "keyboards/3.0/mt.xml");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        KeyboardFlatten.flatten(mtxml.getAbsolutePath(), baos);
        baos.close();
        String outstr = baos.toString("UTF-8");
        assertTrue(outstr.contains("<key id=\"bang\"")); // has one of the imported sequences
        assertTrue(outstr.contains("<key id=\"pound\"")); // has one of the imported sequences
        assertFalse(outstr.contains("<import")); // no imports
    }
}
