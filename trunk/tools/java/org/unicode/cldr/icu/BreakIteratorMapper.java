package org.unicode.cldr.icu;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.XPathParts;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.MessageFormat;

/**
 * Converts special ICU break iterator files for ICU.
 * @author jchye
 */
class BreakIteratorMapper extends Mapper {
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
     * @return CLDR data converted to an ICU-friendly format. (this processes the special data from the ICU side)
     */
    @Override
    public IcuData[] fillFromCldr(String locale) {
        IcuData icuData = new IcuData("common/segments/" + locale + ".xml ../../xml/brkitr/" + locale + ".xml", locale, true);
        CLDRFile specialsFile = specialFactory.make(locale, false);
        BreakIteratorHandler handler = new BreakIteratorHandler(icuData);
        File file = new File(sourceDir, locale + ".xml");
        MapperUtils.parseFile(file, handler);

        for (String path : specialsFile) {
            /*
             * example paths:
                //ldml/special/icu:breakIteratorData/icu:boundaries/icu:title[@icu:dependency="title.brk"]
                //ldml/special/icu:breakIteratorData/icu:dictionaries/icu:dictionary[@icu:dependency="laodict.dict"][@type="Laoo"]
             */
            if (!path.startsWith("//ldml/special/icu:breakIteratorData")) continue;
            String fullPath = specialsFile.getFullXPath(path);
            final XPathParts xpp = new XPathParts();
            xpp.set(fullPath);
            final String element = xpp.getElement(-1);
            final String element2 = xpp.getElement(-2);
            Set<String> source = null;
            if (!element.startsWith("icu:") || !element.startsWith("icu:")) {
                System.err.println("WARNING: brkiter: in " + locale + ".xml (ICU specials): Ignoring path: " + fullPath);
            } else {
                final String type;

                if (!element.startsWith("icu:") || !element.startsWith("icu:")) {
                    System.err.println("WARNING: brkiter: in " + locale + ".xml (ICU specials): Ignoring path (unknown special): " + fullPath);
                    continue;
                }

                if (element2.equals("icu:boundaries")) {
                    type = element.split(":")[1]; // icu:word -> "word"
                    source = brkSource;
                } else if (element2.equals("icu:dictionaries")) {
                    type = xpp.getAttributeValue(-1, "type"); // [@type="Laoo"] -> "Laoo"
                    source = dictSource;
                } else {
                    System.err
                        .println("WARNING: brkiter: in " + locale + ".xml (ICU specials): Ignoring path (unknown element: " + element2 + "): " + fullPath);
                    continue;
                }
                final String filename = xpp.getAttributeValue(-1, "icu:dependency");
                if (filename == null || type == null) {
                    System.err.println("WARNING: brkiter: in " + locale + ".xml (ICU specials): Malformed path: " + fullPath);
                } else {
                    final String altValue = xpp.getAttributeValue(-1, "alt");
                    final String extType = (altValue == null) ? type : type.concat("_").concat(altValue);
                    source.add(filename.substring(0, filename.lastIndexOf('.'))); // "title.brk" -> "title"
                    icuData.add(
                        MessageFormat.format("/{0}/{1}:process(dependency)", element2.split(":")[1], extType), filename); // "Laoo", "laodict.dict"
                }
            }
        }

        return new IcuData[] { icuData };
    }

    /**
     * The XML handler for break iterator data.  (from the CLDR side)
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
            if (qName.equals("segmentation")) {
                segPath = "/exceptions/" + attr.getValue("type") + ":array";
            } else if (qName.equals("version")) {
                icuData.add("/Version", new String[] { MapperUtils.formatVersion(attr.getValue("number")) });
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals("exception") || // deprecated name
                qName.equals("suppression")) { // new name
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
