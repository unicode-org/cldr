package org.unicode.cldr.icu;

import java.io.File;
import java.io.FileInputStream;

import org.unicode.cldr.util.XMLFileReader;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * Utility class for NewLdml2IcuConverter mappers.
 * @author jchye
 */
public class MapperUtils {
    /**
     * Parses an XML file.
     * @param inputFile the file to be parsed
     * @param handler the XML parser to be used
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
}
