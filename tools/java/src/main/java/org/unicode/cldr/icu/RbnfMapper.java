package org.unicode.cldr.icu;

import java.io.File;
import java.util.Collection;

import org.unicode.cldr.util.SupplementalDataInfo;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.ibm.icu.impl.Utility;

public class RbnfMapper extends Mapper {
    private File sourceDir;
    private File specialsDir;
    private SupplementalDataInfo sdi;

    public RbnfMapper(File sourceDir, File specialsDir) {
        this.sourceDir = sourceDir;
        this.specialsDir = specialsDir;
    }

    /**
     * @return CLDR data converted to an ICU-friendly format
     */
    @Override
    public IcuData[] fillFromCldr(String locale) {
        IcuData icuData = new IcuData("common/rbnf/" + locale + ".xml", locale, true);
        RbnfHandler handler = new RbnfHandler(icuData);
        // Parse the specials first.
        File specialsFile = getSpecialsFile(locale);
        if (specialsFile != null) {
            icuData.setFileComment("ICU <specials> source: <path>/xml/rbnf/" + locale + ".xml");
            MapperUtils.parseFile(specialsFile, handler);
        }

        File file = new File(sourceDir, locale + ".xml");
        MapperUtils.parseFile(file, handler);

        if (sdi == null) {
            sdi = SupplementalDataInfo.getInstance();
        }
        String parent = sdi.getExplicitParentLocale(locale);
        if (parent != null) { // Empty except for version
            icuData.add("/%%Parent", parent);
        }
        return new IcuData[] { icuData };
    }

    /**
     * @param filename
     * @return true if a special XML file with the specified filename is available.
     */
    private File getSpecialsFile(String filename) {
        if (specialsDir == null) return null;
        File file = new File(specialsDir, filename + ".xml");
        return file.exists() ? file : null;
    }

    /**
     * The XML handler for collation data.
     */
    private class RbnfHandler extends MapperUtils.EmptyHandler {
        private IcuData icuData;
        private String rbPath;
        private StringBuilder currentText = new StringBuilder();
        private StringBuilder value = new StringBuilder();
        private boolean isLenient;

        public RbnfHandler(IcuData icuData) {
            this.icuData = icuData;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attr) throws SAXException {
            if (qName.equals("rulesetGrouping")) {
                rbPath = "/RBNFRules/" + attr.getValue("type");
            } else if (qName.equals("ruleset")) {
                String access = attr.getValue("access");
                String value = access != null && access.equals("private") ? "%%" : "%";
                String type = attr.getValue("type");
                value += type + ":";
                icuData.add(rbPath, value);
                isLenient = type.equals("lenient-parse");
            } else if (qName.equals("rbnfrule")) {
                if (isLenient) return;
                value.append(attr.getValue("value"));
                String radix = attr.getValue("radix");
                if (radix != null) {
                    value.append('/').append(radix);
                }
                value.append(": ");
            } else if (qName.equals("version")) {
                icuData.replace("/Version", new String[] { MapperUtils.formatVersion(attr.getValue("number")) });
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals("rbnfrule")) {
                // TODO(jchye): Utility.escape unicode-escapes all non-ASCII chars.
                // Find out what non-ASCII chars really need to be escaped.
                value.append(Utility.escape(currentText.toString()
                    .replace('→', '>')
                    .replace('←', '<')));
                icuData.add(rbPath, value.toString());
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
        return MapperUtils.getNames(sourceDir);
    }

    @Override
    public Makefile generateMakefile(Collection<String> aliases) {
        Makefile makefile = new Makefile("RBNF");
        makefile.addSyntheticAlias(aliases);
        makefile.addAliasSource();
        makefile.addSource(sources);
        return makefile;
    }
}
