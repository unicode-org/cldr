package org.unicode.cldr.util;

import java.io.*;
import java.util.Map;
import java.util.TreeMap;

public class MissingXmlGetter {

    private final CLDRFile cldrFile;

    public MissingXmlGetter(CLDRLocale loc, Factory factory) {
        this.cldrFile = factory.make(loc.getBaseName(), true/* resolved */);
    }

    /**
     * Construct XML for the paths that are error/missing/provisional in a locale, with value placeholders,
     * so that somebody can download and edit the .xml file to fill in the values and then do a bulk
     * submission of that file
     *
     * @param coverageLevel the coverage level for paths to be included
     * @return the XML as a string
     * @throws IOException for StringWriter
     */
    public String getXml(Level coverageLevel) throws IOException {
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            // TODO: add a new option telling cldrFile.write to write only the error/missing/provisional paths
            // (at the given coverageLevel, adding a placeholder for the values) instead of
            // the present paths
            // Reference: https://unicode-org.atlassian.net/browse/CLDR-15707
            Map<String, Object> options = new TreeMap<>();
            cldrFile.write(pw, options);
            return sw.toString();
        }
    }
}
