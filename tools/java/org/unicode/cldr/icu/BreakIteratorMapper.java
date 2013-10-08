package org.unicode.cldr.icu;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.MessageFormat;

/**
 * Converts special ICU break iterator files for ICU.
 * @author jchye
 */
class BreakIteratorMapper extends Mapper {
    private static Pattern BOUNDARY_PATH = Pattern.compile(
        "//ldml/special/icu:breakIteratorData/icu:([\\w_]++)/icu:([\\w_]++)\\[@icu:([\\w_]++)=\"([^\"]++)\"]");
    private static Pattern DICTIONARY_PATH = Pattern.compile(
        "//ldml/special/icu:breakIteratorData/icu:([\\w_]++)/icu:[\\w_]++\\[@type=\"([\\w_]++)\"]\\[@icu:([\\w_]++)=\"([^\"]++)\"]");
    
    private String sourceDir;
    private Factory specialFactory;
    private Set<String> brkSource = new HashSet<String>();
    private Set<String> dictSource = new HashSet<String>();

    /**
     * @param specialFactory the factory containing the ICU xml files for break iterators.
     */
    public BreakIteratorMapper(String sourceDir, Factory specialFactory) {
        this.sourceDir = sourceDir;
        this.specialFactory = specialFactory;
    }

    /**
     * @return CLDR data converted to an ICU-friendly format
     */
    @Override
    public IcuData[] fillFromCldr(String locale) {
        IcuData icuData = new IcuData("common/segments/" + locale + ".xml", locale, true);
        CLDRFile file = specialFactory.make(locale, false);
        BreakIteratorHandler handler = new BreakIteratorHandler(icuData);
        File file_ = new File(sourceDir, locale + ".xml");
        MapperUtils.parseFile(file_, handler);
        
        for (String path : file) {
            if (!path.startsWith("//ldml/special")) continue;
            String fullPath = file.getFullXPath(path);
            Matcher matcher = BOUNDARY_PATH.matcher(fullPath);
            boolean matches = matcher.matches();
            Set<String> source = null;
            if (matches) {
                source = brkSource;
            } else {
                matcher = DICTIONARY_PATH.matcher(fullPath);
                matches = matcher.matches();
                source = dictSource;
            }
            if (matches) {
                String filename = matcher.group(4);
                source.add(filename.substring(0, filename.lastIndexOf('.')));
                icuData.add(
                    MessageFormat.format("/{0}/{1}:process({2})", matcher.group(1), matcher.group(2), matcher.group(3)),
                    filename);
            }
        }
        
        return new IcuData[] { icuData };
    }
    
    /**
     * The XML handler for collation data.
     */
    private class BreakIteratorHandler extends MapperUtils.EmptyHandler {
        private IcuData icuData;
        private String segPath;
        private StringBuilder currentText = new StringBuilder();
        private StringBuilder value = new StringBuilder();

        public BreakIteratorHandler(IcuData icuData) {
            this.icuData = icuData;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attr) throws SAXException {
            if (qName.equals("segmentations")) {
                segPath = "/Segmentations";
            } else if (qName.equals("Segmentation")) {
                segPath = "/segmentation/" + attr.getValue("type");
            } else if (qName.equals("version")) {
                icuData.add("/Version", new String[] { MapperUtils.formatVersion(attr.getValue("number")) });
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals("exception")) {
                value.append(Utility.escape(currentText.toString()));
                icuData.add(segPath, value.toString());
                currentText.setLength(0);
                value.setLength(0);
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            currentText.append(ch, start, length);
        }
    }

    @Override
    public Collection<String> getAvailable() {
        return specialFactory.getAvailable();
    }

    @Override
    public Makefile generateMakefile(Collection<String> aliases) {
        Makefile makefile = new Makefile("BRK_RES");
        makefile.addSyntheticAlias(aliases);
        makefile.addAliasSource();
        // Add variables for non-XML source files.
        makefile.addEntry("BRK_DICT_SOURCE", "List of dictionary files (dict).", dictSource);
        makefile.addEntry("BRK_SOURCE", "List of break iterator files (brk).", brkSource);
        makefile.addSource(sources);
        return makefile;
    }
}
