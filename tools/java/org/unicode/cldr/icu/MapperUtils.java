package org.unicode.cldr.icu;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.RegexUtilities;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.MessageFormat;

/**
 * Utility class for NewLdml2IcuConverter mappers.
 * 
 * @author jchye
 */
public class MapperUtils {
    private static final Pattern VERSION_PATTERN = Pattern.compile("\\$Revision:\\s*([\\d.]+)\\s*\\$");

    /**
     * Parses an XML file.
     * 
     * @param inputFile
     *            the file to be parsed
     * @param handler
     *            the XML parser to be used
     */
    public static void parseFile(File inputFile, ContentHandler handler) {
        XMLReader xmlReader = XMLFileReader.createXMLReader(true);
        xmlReader.setContentHandler(handler);
        try {
            FileInputStream fis = new FileInputStream(inputFile);
            InputSource is = new InputSource(fis);
            // Set the system ID so the parser knows where to find the dtd.
            is.setSystemId(inputFile.toString());
            xmlReader.parse(is);
            fis.close();
        } catch (Exception e) {
            System.err.println("Error loading " + inputFile.getAbsolutePath());
            e.printStackTrace();
        }
    }

    public static String formatVersion(String value) {
        Matcher versionMatcher = VERSION_PATTERN.matcher(value);
        int versionNum;
        if (!versionMatcher.find()) {
            int failPoint = RegexUtilities.findMismatch(versionMatcher, value);
            String show = value.substring(0, failPoint) + "☹" + value.substring(failPoint);
            System.err.println("Warning: no version match with: " + show);
            versionNum = 0;
        } else {
            String rawVersion = versionMatcher.group(1);
            // No further processing needed, e.g. "1.1"
            if (rawVersion.contains(".")) {
                return rawVersion;
            }
            versionNum = Integer.parseInt(rawVersion);
        }
        String version = "";
        int numDots = 0;
        while (versionNum > 0) {
            version = "." + versionNum % 100 + version;
            versionNum /= 100;
            numDots++;
        }
        return (numDots > 1 ? "2" : "2.0") + version;
    }

    /**
     * Empty convenience superclass for all XML parsers used in mappers.
     * 
     * @author jchye
     * 
     */
    public static abstract class EmptyHandler implements ContentHandler {
        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
        }

        @Override
        public void startPrefixMapping(String arg0, String arg1) throws SAXException {
        }

        @Override
        public void endPrefixMapping(String arg0) throws SAXException {
        }

        @Override
        public void ignorableWhitespace(char[] arg0, int arg1, int arg2) throws SAXException {
        }

        @Override
        public void processingInstruction(String arg0, String arg1) throws SAXException {
        }

        @Override
        public void setDocumentLocator(Locator arg0) {
        }

        @Override
        public void skippedEntity(String arg0) throws SAXException {
        }

        @Override
        public void startDocument() throws SAXException {
        }

        @Override
        public void endDocument() throws SAXException {
        }
    }
}
