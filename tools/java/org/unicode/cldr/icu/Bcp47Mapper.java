package org.unicode.cldr.icu;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A mapper that converts BCP 47 data from CLDR to the ICU data structure.
 * 
 * @author jchye
 */
public class Bcp47Mapper {
    private static final String[] KEYTYPE_FILES = {
        "calendar", "collation", "currency", "number"
    };
    private String sourceDir;

    public Bcp47Mapper(String bcp47Dir) {
        sourceDir = bcp47Dir;
    }

    /**
     * Fills an IcuData object with data of the given type.
     */
    public IcuData[] fillFromCldr() {
        IcuData timezoneData = new IcuData("common/bcp47/timezone.xml", "timezoneTypes", false);
        Map<String, String> keyMap = new HashMap<String, String>();
        // Timezone data is put in a different file.
        fillFromFile("timezone", timezoneData, keyMap);

        // Process the rest of the data.
        IcuData keyTypeData = new IcuData("common/bcp47/*.xml", "keyTypeData", false);
        for (String filename : KEYTYPE_FILES) {
            fillFromFile(filename, keyTypeData, keyMap);
        }
        // Add all the keyMap values into the IcuData file.
        for (String key : keyMap.keySet()) {
            keyTypeData.add("/keyMap/" + keyMap.get(key), key);
        }
        // Add aliases for timezone data.
        keyTypeData.add("/typeAlias/timezone:alias", "/ICUDATA/timezoneTypes/typeAlias/timezone");
        keyTypeData.add("/typeMap/timezone:alias", "/ICUDATA/timezoneTypes/typeMap/timezone");
        return new IcuData[] { timezoneData, keyTypeData };
    }

    private void fillFromFile(String filename, IcuData icuData, Map<String, String> keyMap) {
        KeywordHandler handler = new KeywordHandler(icuData, keyMap);
        MapperUtils.parseFile(new File(sourceDir, filename + ".xml"), handler);
    }

    /**
     * XML parser for BCP47 data.
     */
    private class KeywordHandler extends MapperUtils.EmptyHandler {
        private String typeAliasPrefix;
        private String typeMapPrefix;
        private IcuData icuData;
        private Map<String, String> keyMap;

        /**
         * KeywordHandler constructor.
         * 
         * @param icuData
         *            the IcuData object to store the parsed data
         * @param keyMap
         *            a mapping of keys to their aliases. These values will
         *            not be added to icuData by the handler
         */
        public KeywordHandler(IcuData icuData, Map<String, String> keyMap) {
            this.icuData = icuData;
            this.keyMap = keyMap;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attr) throws SAXException {
            // Format of BCP47 file:
            // <key name="tz" alias="timezone" description="Time zone key">
            // <type name="adalv" alias="Europe/Andorra" description="Andorra"/>
            // ...
            if (qName.equals("key")) {
                String keyAlias = null;
                if (attr != null) {
                    keyAlias = attr.getValue("alias");
                }
                if (keyAlias == null) {
                    keyAlias = attr.getValue("name");
                    System.err.println(Bcp47Mapper.class.getSimpleName() + " Note: BCP47 key " + keyAlias
                        + " didn't have the optional alias= value, mapping " + keyAlias + "->" + keyAlias);
                }
                keyAlias = keyAlias.toLowerCase();
                typeAliasPrefix = "/typeAlias/" + keyAlias + '/';
                typeMapPrefix = "/typeMap/" + keyAlias + '/';
                keyMap.put(attr.getValue("name"), keyAlias);
            } else if (qName.equals("type")) {
                String alias = attr.getValue("alias");
                if (alias == null) return;
                String[] aliases = alias.split("\\s+");
                String mainAlias = aliases[0];
                icuData.add(typeMapPrefix + formatName(mainAlias),
                    attr.getValue("name"));
                for (int i = 1; i < aliases.length; i++) {
                    icuData.add(typeAliasPrefix + formatName(aliases[i]),
                        mainAlias);
                }
            }
        }

        private String formatName(String str) {
            if (str.indexOf('/') > -1) {
                str = '"' + str.replace('/', ':') + '"';
            }
            return str;
        }
    }
}
