package org.unicode.cldr.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.unicode.cldr.util.XMLFileReader.LoggingHandler;
import org.unicode.cldr.util.XMLFileReader.SimpleHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class TestDoctypeXmlStreamWrapper {
    private static final int COUNT = 10; // increase this for perf testing
    private static final String COMMON_MT = CLDRPaths.BASE_DIRECTORY + "/common/main/mt.xml";
    private static final String KEYBOARDS_MT = CLDRPaths.BASE_DIRECTORY + "/keyboards/3.0/mt.xml";

    // make sure we get some basic loading first before starting the clock
    @BeforeAll
    public static final void SetupStuff() throws IOException {
        TestDoctypeXmlStreamWrapper t = new TestDoctypeXmlStreamWrapper();
        t.TestReadJar();
        t.TestReadCommon();
    }

    @Test
    void TestProcessPathValues() {
        for (int i = 0; i < COUNT; i++) {
            XMLFileReader.processPathValues(COMMON_MT, true, new SimpleHandler());
        }
    }

    @Test
    void TestReadJar() throws IOException {
        for (int i = 0; i < COUNT; i++) {
            new XMLFileReader()
                    .setHandler(new XMLFileReader.SimpleHandler())
                    .readCLDRResource("dl_iso_table_a1.xml", -1, false);
        }
    }

    @Test
    void TestReadCommon() throws FileNotFoundException, IOException {
        for (int i = 0; i < COUNT; i++) {
            new XMLFileReader()
                    .setHandler(new XMLFileReader.SimpleHandler())
                    .read(COMMON_MT, -1, true);
        }
    }

    @Test
    void TestReadKeyboard() throws FileNotFoundException, IOException {
        if (true /* TODO CLDR-17574 With v46, parsing issues for keyboard xml files */) {
            return;
        }
        for (int i = 0; i < COUNT; i++) {
            new XMLFileReader()
                    .setHandler(new XMLFileReader.SimpleHandler())
                    .read(KEYBOARDS_MT, -1, true);
        }
    }

    @Test
    void TestReadKeyboardByte() throws IOException, SAXException {
        // verify that reading via InputStream (byte) works as well
        if (true /* TODO CLDR-17574 With v46, parsing issues for keyboard xml files */) {
            return;
        }
        try (InputStream fis = new FileInputStream(KEYBOARDS_MT); ) {
            InputSource is = new InputSource(fis);
            is.setSystemId(KEYBOARDS_MT);
            is = DoctypeXmlStreamWrapper.wrap(is);
            XMLFileReader.createXMLReader(-1, true, new LoggingHandler()).parse(is);
        }
    }

    @Test
    void TestReadKeyboardChar() throws IOException, SAXException {
        // verify that reading via Reader (char) works as well
        if (true /* TODO CLDR-17574 With v46, parsing issues for keyboard xml files */) {
            return;
        }
        try (InputStream fis = new FileInputStream(KEYBOARDS_MT);
                InputStreamReader isr = new InputStreamReader(fis); ) {
            InputSource is = new InputSource(isr);
            is.setSystemId(KEYBOARDS_MT);
            is = DoctypeXmlStreamWrapper.wrap(is);
            XMLFileReader.createXMLReader(-1, true, new LoggingHandler()).parse(is);
        }
    }

    @ParameterizedTest(name = "[{index}] wrapped={arguments}")
    @ValueSource(booleans = {false, true, false, true})
    public void TestBytePerf(boolean wrapped) throws IOException, SAXException {
        for (int i = 0; i < COUNT; i++) {
            // mimic XMLFileHandler.read() here, but with wrapping enabled/disabled
            try (InputStream fis = new FileInputStream(COMMON_MT); ) {
                InputSource is = new InputSource(fis);
                is.setSystemId(COMMON_MT);
                if (wrapped) is = DoctypeXmlStreamWrapper.wrap(is);
                XMLFileReader.createXMLReader(-1, true, new LoggingHandler()).parse(is);
            }
        }
    }

    @ParameterizedTest(name = "[{index}] wrapped={arguments}")
    @ValueSource(booleans = {false, true, false, true})
    public void TestCharPerf(boolean wrapped) throws IOException, SAXException {
        for (int i = 0; i < COUNT; i++) {
            // mimic XMLFileHandler.read() here, but with wrapping enabled/disabled
            try (InputStream fis = new FileInputStream(COMMON_MT);
                    InputStreamReader isr = new InputStreamReader(fis); ) {
                InputSource is = new InputSource(isr);
                is.setSystemId(COMMON_MT);
                if (wrapped) is = DoctypeXmlStreamWrapper.wrap(is);
                XMLFileReader.createXMLReader(-1, true, new LoggingHandler()).parse(is);
            }
        }
    }
}
