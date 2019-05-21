package org.unicode.cldr.icu;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.InputStreamFactory;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.cldr.util.XMLFileReader;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Utility class for NewLdml2IcuConverter mappers.
 *
 * @author jchye
 */
public class MapperUtils {
    private static final Pattern VERSION_PATTERN = PatternCache.get("\\$Revision:\\s*([\\d.]+)\\s*\\$");

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
        if (inputFile == null) {
            System.err.println("Please call with non-null input file");
            return;
        }
        try (InputStream fis = InputStreamFactory.createInputStream(inputFile)) {
//            FileInputStream fis = new FileInputStream(inputFile);
            InputSource is = new InputSource(fis);
            // Set the system ID so the parser knows where to find the dtd.
            is.setSystemId(inputFile.toString());
            xmlReader.parse(is);
//            fis.close();
        } catch (IOException | SAXException e) {
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
        return (numDots > 2 ? "2" : "2.0") + version;
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

    public static IcuData[] toArray(Collection<IcuData> collection) {
        IcuData[] dataList = new IcuData[collection.size()];
        return collection.toArray(dataList);
    }

    /**
     * Returns a list of names of XML files in the specified directory.
     * @param sourceDir
     * @return
     */
    public static List<String> getNames(String sourceDir) {
        return getNames(new File(sourceDir));
    }

    /**
     * Returns a list of names of XML files in the specified directory.
     * @param sourceDir
     * @return
     */
    public static List<String> getNames(File sourceDir) {
        List<String> locales = new ArrayList<String>();
        for (String filename : sourceDir.list()) {
            if (!filename.endsWith(".xml")) continue;
            String locale = filename.substring(0, filename.length() - 4);
            locales.add(locale);
        }
        return locales;
    }
}
