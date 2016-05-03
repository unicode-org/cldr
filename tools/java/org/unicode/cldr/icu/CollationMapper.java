package org.unicode.cldr.icu;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PatternCache;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.MessageFormat;

/**
 * Converts CLDR collation files to the ICU format.
 * @author jchye
 */
public class CollationMapper extends Mapper {
    private static Pattern SPECIALS_PATH = PatternCache.get("//ldml/special/icu:([\\w_]++)\\[@icu:([\\w_]++)=\"([^\"]++)\"]");
    private String sourceDir;
    private Factory specialFactory;
    private Set<String> validSubLocales = new HashSet<String>();

    // TODO: CLDR 28 ticket #8289 "Move collator CLDR settings into ICU format"
    // deprecated the collation sub-elements
    // import, settings, suppress_contractions, and optimize
    // and changed the data from XML syntax to ICU syntax.
    // Remove conversion of these elements when we do not need to handle old data any more.

    // Some settings have to be converted to numbers.
    private Map<String, String> settingsMap = Builder.with(new HashMap<String, String>())
        .put("primary", "1")
        .put("secondary", "2")
        .put("tertiary", "3")
        .put("quarternary", "4")
        .put("identical", "5")
        .put("on", "2")
        .get();

    /**
     * @param sourceDir the source dir of the collation files
     * @param specialFactory the factory for any ICU-specific collation info
     */
    public CollationMapper(String sourceDir, Factory specialFactory) {
        this.sourceDir = sourceDir;
        this.specialFactory = specialFactory;
    }

    /**
     * @return CLDR data converted to an ICU-friendly format
     */
    @Override
    public IcuData[] fillFromCldr(String locale) {
        List<IcuData> dataList = new ArrayList<IcuData>();
        IcuData mainLocale = new IcuData("common/collation/" + locale + ".xml", locale, true);
        CollationHandler handler = new CollationHandler(mainLocale);
        File file = new File(sourceDir, locale + ".xml");
        MapperUtils.parseFile(file, handler);
        dataList.add(mainLocale);

        String[] subLocales = handler.getSubLocales();
        if (subLocales != null) {
            for (String subLocale : subLocales) {
                dataList.add(fillSubLocale(locale, subLocale));
                validSubLocales.add(subLocale);
            }
        }

        if (hasSpecialFile(locale)) {
            CLDRFile specialFile = specialFactory.make(locale, false);
            mainLocale.setFileComment("ICU <specials> source: <path>/xml/collation/" + locale + ".xml");
            for (String path : specialFile) {
                String fullPath = specialFile.getFullXPath(path);
                Matcher matcher = SPECIALS_PATH.matcher(fullPath);
                if (matcher.matches()) {
                    mainLocale.add(
                        MessageFormat.format("/{0}:process({1})", matcher.group(1), matcher.group(2)),
                        matcher.group(3));
                }
            }
        }

        return MapperUtils.toArray(dataList);
    }

    /**
     * Creates an IcuData object for the specified sublocale
     * @param locale the parent of the sublocale
     * @param subLocale the sublocale
     * @return
     */
    private IcuData fillSubLocale(String locale, String subLocale) {
        IcuData icuData = new IcuData("icu-config.xml & build.xml", subLocale, true);
        icuData.setFileComment("validSubLocale of \"" + locale + "\"");
        icuData.add("/___", "");
        return icuData;
    }

    /**
     * @param filename
     * @return true if a special XML file with the specified filename is available.
     */
    private boolean hasSpecialFile(String filename) {
        return specialFactory != null && specialFactory.getAvailable().contains(filename);
    }

    /**
     * The XML handler for collation data.
     */
    private class CollationHandler extends MapperUtils.EmptyHandler {
        private IcuData icuData;
        private StringBuilder currentText = new StringBuilder();
        private String collationType;
        private boolean isShort;
        private List<String> properties = new ArrayList<String>();
        private List<String> rules = new ArrayList<String>();
        private String[] subLocales;

        public CollationHandler(IcuData icuData) {
            this.icuData = icuData;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attr) throws SAXException {
            if (qName.equals("collation")) {
                DraftStatus status = DraftStatus.forString(attr.getValue("draft"));
                collationType = status.compareTo(DraftStatus.contributed) < 0 ? null : attr.getValue("type");
                isShort = attr.getValue("alt") != null;
                properties.clear();
                rules.clear();
            } else if (qName.equals("collations")) {
                String validSubLocales = attr.getValue("validSubLocales");
                if (validSubLocales != null) {
                    subLocales = validSubLocales.split("\\s++");
                }
            } else if (qName.equals("version")) {
                icuData.add("/Version", MapperUtils.formatVersion(attr.getValue("number")));
            }
            if (collationType == null) return;

            // Collation-specific elements.
            if (qName.equals("settings")) {
                for (int i = 0; i < attr.getLength(); i++) {
                    String name = attr.getLocalName(i);
                    String value = attr.getValue(i);
                    if (name.equals("strength") || name.equals("backwards")) {
                        value = settingsMap.get(value);
                    } else if (name.equals("hiraganaQuaternary")) {
                        name = "hiraganaQ";
                    }
                    properties.add(name + " " + value);
                }
            } else if (qName.equals("import")) {
                String value = attr.getValue("source");
                String type = attr.getValue("type");
                if (type != null) {
                    value += "-u-co-" + type;
                }
                properties.add("import " + value);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            // collationType will only be null if the draft status is insufficient.
            if (qName.equals("defaultCollation")) {
                icuData.add("/collations/default", currentText.toString());
            } else if (collationType == null) {
                currentText.setLength(0);
                return;
            }

            if (qName.equals("suppress_contractions")) {
                properties.add("suppressContractions " + currentText.toString());
            } else if (qName.equals("cr")) {
                String[] lines = currentText.toString().split("\n");
                for (String line : lines) {
                    int commentPos = Utility.quotedIndexOf(line, 0, line.length(), "#");
                    if (commentPos > -1) {
                        line = line.substring(0, commentPos);
                    }
                    line = line.trim();
                    if (line.length() > 0) {
                        rules.add(line);
                    }
                }
            } else if (qName.equals("collation")) {
                // Add attributes before the main rules.
                StringBuilder attrBuffer = new StringBuilder();
                if (properties.size() > 0) {
                    for (String property : properties) {
                        attrBuffer.append('[').append(property).append(']');
                    }
                    rules.add(0, attrBuffer.toString());
                }

                String[] rulesArray;
                if (rules.size() == 0) {
                    rulesArray = new String[] { "" };
                } else {
                    rulesArray = new String[rules.size()];
                    rules.toArray(rulesArray);
                }

                String rbPath = "/collations/" + collationType + "/Sequence";
                // Always prefer the short version.
                if (isShort || !icuData.containsKey(rbPath)) {
                    icuData.replace(rbPath, rulesArray);
                    icuData.replace("/collations/" + collationType + "/Version", new String[] { CLDRFile.GEN_VERSION });
                }
            }
            currentText.setLength(0);
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            currentText.append(ch, start, length);
        }

        public String[] getSubLocales() {
            return subLocales;
        }
    }

    @Override
    public Collection<String> getAvailable() {
        return MapperUtils.getNames(sourceDir);
    }

    @Override
    public Makefile generateMakefile(Collection<String> aliases) {
        Makefile makefile = new Makefile("COLLATION");
        makefile.addSyntheticAlias(aliases);
        makefile.addAliasSource();
        // Split sources into locales and sublocales.
        List<String> subLocales = new ArrayList<String>();
        List<String> locales = new ArrayList<String>();
        locales.add("$(COLLATION_EMPTY_SOURCE)");
        for (String source : sources) {
            if (validSubLocales.contains(source)) {
                subLocales.add(source);
            } else {
                locales.add(source);
            }
        }
        makefile.addEntry("COLLATION_EMPTY_SOURCE",
            "Empty locales, used for validSubLocale fallback.",
            subLocales);
        makefile.addSource(locales);
        return makefile;
    }
}
