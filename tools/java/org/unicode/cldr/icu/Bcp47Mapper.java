package org.unicode.cldr.icu;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.google.common.collect.ImmutableSet;

/**
 * A mapper that converts BCP 47 data from CLDR to the ICU data structure.
 *
 * @author jchye
 */
public class Bcp47Mapper {
//    private static final String[] KEYTYPE_FILES = {
//        "calendar", "collation", "currency", "number", "variant"
//    };
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
        for (File file : new File(sourceDir).listFiles()) {
            final String filenameXml = file.getName();
            if (filenameXml.endsWith(".xml") && !filenameXml.equals("timezone.xml")) {
                fillFromFile(filenameXml.substring(0, filenameXml.length() - 4),
                    keyTypeData, keyMap);
            }
        }
        // Add all the keyMap values into the IcuData file.
        for (Entry<String, String> kmData : keyMap.entrySet()) {
            String bcpKey = kmData.getKey();
            String key = kmData.getValue();
            if (bcpKey.startsWith("@")) {
                keyTypeData.add("/" + bcpKey.substring(1), key);
                continue;
            }
            if (bcpKey.equals(key)) {
                // empty value to indicate the BCP47 key is same with the legacy key
                bcpKey = "";
            }
            keyTypeData.add("/keyMap/" + key, bcpKey);
        }
        // Add aliases for timezone data.
        keyTypeData.add("/typeAlias/timezone:alias", "/ICUDATA/timezoneTypes/typeAlias/timezone");
        keyTypeData.add("/typeMap/timezone:alias", "/ICUDATA/timezoneTypes/typeMap/timezone");
        keyTypeData.add("/bcpTypeAlias/tz:alias", "/ICUDATA/timezoneTypes/bcpTypeAlias/tz");
        return new IcuData[] { timezoneData, keyTypeData };
    }

    private void fillFromFile(String filename, IcuData icuData, Map<String, String> keyMap) {
        KeywordHandler handler = new KeywordHandler(icuData, keyMap);
        MapperUtils.parseFile(new File(sourceDir, filename + ".xml"), handler);
    }

    static final Set<String> SKIP_KEY_ATTRIBUTES = ImmutableSet.of(
        "name", "alias", "description", "since", "extension");
    static final Set<String> SKIP_TYPE_ATTRIBUTES = ImmutableSet.of(
        "name", "alias", "description", "since", "preferred");

    /**
     * XML parser for BCP47 data.
     */
    private class KeywordHandler extends MapperUtils.EmptyHandler {
        private String typeAliasPrefix;
        private String typeMapPrefix;
        private String bcpTypeAliasPrefix;
        private String lastKeyName;
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
            if (attr == null) {
                return;
            }

            if (qName.equals("key")) {
                String keyName = attr.getValue("name");
                if (keyName == null) {
                    return;
                }

                String keyAlias = attr.getValue("alias");
                if (keyAlias == null) {
                    keyAlias = keyName;
//                    System.err.println(Bcp47Mapper.class.getSimpleName() + " Info: BCP47 key " + keyName
//                        + " didn't have the optional alias= value, mapping " + keyName + "->" + keyName);
                }

                keyName = keyName.toLowerCase();
                keyAlias = keyAlias.toLowerCase();

                typeAliasPrefix = "/typeAlias/" + keyAlias + '/';
                typeMapPrefix = "/typeMap/" + keyAlias + '/';
                keyMap.put(keyName, keyAlias);
                bcpTypeAliasPrefix = "/bcpTypeAlias/" + keyName + '/';
                lastKeyName = keyName;
                addOtherInfo(qName, attr, keyName, SKIP_KEY_ATTRIBUTES);
            } else if (qName.equals("type")) {
                String typeName = attr.getValue("name");
                if (typeName == null) {
                    return;
                }

                // BCP47 type alias (maps deprecated type to preferred type)
                String preferredTypeName = attr.getValue("preferred");
                if (preferredTypeName != null) {
                    icuData.add(bcpTypeAliasPrefix + typeName, preferredTypeName);
                    return;
                }

                String alias = attr.getValue("alias");
                if (alias == null) {
                    // Generate type map entry using empty value
                    // (an empty value indicates same type name
                    // is used for both BCP47 and legacy type.
                    icuData.add(typeMapPrefix + typeName, "");
                } else {
                    String[] aliases = alias.split("\\s+");
                    String mainAlias = aliases[0];
                    icuData.add(typeMapPrefix + formatName(mainAlias), typeName);
                    for (int i = 1; i < aliases.length; i++) {
                        icuData.add(typeAliasPrefix + formatName(aliases[i]), mainAlias);
                    }
                }
                addOtherInfo(qName, attr, lastKeyName + "/" + typeName, SKIP_KEY_ATTRIBUTES);
            }
        }

        private void addOtherInfo(String qName, Attributes attr, String typeName, Set<String> excludedAttributes) {
            for (int i = 0; i < attr.getLength(); ++i) {
                String name = attr.getQName(i);
                String value = attr.getValue(i);
                if (excludedAttributes.contains(name)
                    || name.equals("deprecated") && value.equals("false")) {
                    continue;
                }
                keyMap.put("@" + qName + "Info/" + name + "/" + typeName, value);
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
