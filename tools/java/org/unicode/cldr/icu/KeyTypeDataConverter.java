package org.unicode.cldr.icu;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.unicode.cldr.icu.ICUResourceWriter.Resource;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceAlias;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceString;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceTable;
import org.unicode.cldr.util.LDMLUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class KeyTypeDataConverter {
    private final ICULog log;
    private final String bcp47Dir;
    private final String[] externalTypeKeys;
    private Collection<Document> documents;

    private static final String SOURCE_INFO = "common/bcp47/*.xml";
    private static final String EXTERNAL_TYPES_SUFFIX = "Types";


    private static final boolean DEBUG = false;

    public KeyTypeDataConverter(ICULog log, String bcp47Dir, String[] externalTypeKeys) {
        this.log = log;
        this.bcp47Dir = bcp47Dir;
        this.externalTypeKeys = (externalTypeKeys == null) ? new String[0] : externalTypeKeys;
    }

    public void convert(ICUWriter writer) {
        // creating key mapping data
        Map<String, String> keyMap = new TreeMap<String, String>();
        for (Document doc : getDocuments()) {
            addKeyMap(keyMap, doc);
        }

        if (DEBUG) {
            log.log("Key mappings ---------------------------------------------");
            dumpMap(keyMap);
        }

        // creating type mapping data
        Map<String, Map<String, String>> typeMaps = new TreeMap<String, Map<String, String>>();
        Map<String, Map<String, String>> typeAliases = new TreeMap<String, Map<String, String>>();
        for (Document doc : getDocuments()) {
            addTypeMaps(typeMaps, typeAliases, doc);
        }

        if (DEBUG) {
            for (Map.Entry<String, Map<String, String>> e : typeMaps.entrySet()) {
                log.log("\n\n\nType mappings for " + e.getKey() + " ---------------------------------------------");
                dumpMap(e.getValue());
            }

            for (Map.Entry<String, Map<String, String>> e : typeAliases.entrySet()) {
                log.log("\n\n\nAlias mappings for " + e.getKey() + " ---------------------------------------------");
                dumpMap(e.getValue());
            }
        }

        // write out keyTypeData.txt
        Resource cur;

        ResourceTable keyTypeDataRes = new ResourceTable();
        keyTypeDataRes.name = LDMLBCP47Constants.KEYTYPEDATA;
        keyTypeDataRes.annotation = ResourceTable.NO_FALLBACK;

        // keyMap
        ResourceTable keyMapRes = new ResourceTable();
        keyMapRes.name = LDMLBCP47Constants.KEYMAP;
        keyTypeDataRes.first = keyMapRes;

        cur = null;
        for (Entry<String, String> keyMapItem : keyMap.entrySet()) {
            ResourceString keyRes = new ResourceString();
            keyRes.name = keyMapItem.getKey();
            keyRes.val = keyMapItem.getValue();

            if (cur == null) {
                keyMapRes.first = keyRes;
            } else {
                cur.next = keyRes;
            }
            cur = keyRes;
        }

        // typeMap
        Resource typeMapRes = createTypeMapResource(typeMaps, null);
        keyMapRes.next = typeMapRes;

        // typeAlias
        Resource typeAliasRes = createTypeAliasResource(typeAliases, null);
        typeMapRes.next = typeAliasRes;

        writer.writeResource(keyTypeDataRes, SOURCE_INFO);

        // externalized type/alias map data
        for (String key : externalTypeKeys) {
            ResourceTable extTypeDataRes = new ResourceTable();
            extTypeDataRes.name = key + EXTERNAL_TYPES_SUFFIX;
            extTypeDataRes.annotation = ResourceTable.NO_FALLBACK;

            // typeMap
            Resource extTypeMapRes = createTypeMapResource(typeMaps, key);
            extTypeDataRes.first = extTypeMapRes;

            // typeAlias
            Resource extTypeAliasRes = createTypeAliasResource(typeAliases, key);
            extTypeMapRes.next = extTypeAliasRes;

            writer.writeResource(extTypeDataRes, SOURCE_INFO);
        }

    }

    private Collection<Document> getDocuments() {
        if (documents != null) {
            return documents;
        }

        documents = new ArrayList<Document>();

        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (name.endsWith(".xml")) {
                    return true;
                }
                return false;
            }
        };

        File dir = new File(bcp47Dir);
        String[] files = dir.list(filter);
        if (files == null) {
            String canonicalPath;
            try {
              canonicalPath = dir.getCanonicalPath();
            } catch (IOException e) {
              canonicalPath = e.getMessage();
            }
            log.error("BCP47 files are missing " + canonicalPath);
            System.exit(-1);
        }

        String dirPath = dir.getAbsolutePath();
        for (String fileName : files) {
            try {
                log.info("Parsing document " + fileName);
                String filePath = dirPath + File.separator + fileName;
                Document doc = LDMLUtilities.parse(filePath, false);
                documents.add(doc);
            } catch (Throwable se) {
              log.error("Parsing: " + fileName + " " + se.toString(), se);
              System.exit(1);
            }
        }
        return documents;
    }

    private static void addKeyMap(Map<String, String> keyMap, Document root) {
        for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (node.getNodeName().equals(LDMLBCP47Constants.LDMLBCP47)) {
                // Stop iterating over top-level elements, restart iterating over elements
                // under ldmlBCP47.
                node = node.getFirstChild();
                continue;
            }

            if (node.getNodeName().equals(LDMLBCP47Constants.KEYWORD)) {
                // iterating into elements under keyword
                node = node.getFirstChild();
                continue;
            }

            if (node.getNodeName().equals(LDMLBCP47Constants.KEY)) {
                String bcpKey = LDMLUtilities.getAttributeValue(node, LDMLBCP47Constants.NAME);
                String key = LDMLUtilities.getAttributeValue(node, LDMLBCP47Constants.ALIAS);
                if (key != null && !bcpKey.equals(key)) {
                    keyMap.put(escapeKey(key), bcpKey);
                }
            }
        }
    }

    private static void addTypeMaps(Map<String, Map<String, String>> typeMaps, Map<String, Map<String, String>> typeAliases, Document root) {
        for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (node.getNodeName().equals(LDMLBCP47Constants.LDMLBCP47)) {
                // Stop iterating over top-level elements, restart iterating over elements
                // under ldmlBCP47.
                node = node.getFirstChild();
                continue;
            }

            if (node.getNodeName().equals(LDMLBCP47Constants.KEYWORD)) {
                // iterating into elements under keyword
                node = node.getFirstChild();
                continue;
            }

            if (node.getNodeName().equals(LDMLBCP47Constants.KEY)) {
                String bcpKey = LDMLUtilities.getAttributeValue(node, LDMLBCP47Constants.NAME);
                String key = LDMLUtilities.getAttributeValue(node, LDMLBCP47Constants.ALIAS);
                if (key == null) {
                    key = bcpKey;
                }
                for (Node node2 = node.getFirstChild(); node2 != null; node2 = node2.getNextSibling()) {
                    if (node2.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    if (node2.getNodeName().equals(LDMLBCP47Constants.TYPE)) {
                        String bcpType = LDMLUtilities.getAttributeValue(node2, LDMLBCP47Constants.NAME);
                        String type = LDMLUtilities.getAttributeValue(node2, LDMLBCP47Constants.ALIAS);
                        if (type != null) {
                            // type may contain multiple values delimited by space character
                            String[] types = type.split(" ");
                            if (types.length > 1) {
                                type = types[0];

                                // add 2nd and following type values into the alias map
                                Map<String, String> singleTypeAliases = typeAliases.get(key);
                                if (singleTypeAliases == null) {
                                    singleTypeAliases = new TreeMap<String, String>();
                                    typeAliases.put(key, singleTypeAliases);
                                }
                                for (int i = 1; i < types.length; i++) {
                                    singleTypeAliases.put(escapeKey(types[i]), type);
                                }
                            }
                        }

                        if (type != null && !bcpType.equals(type)) {
                            // only populating mapping data when bcp47 representation is different
                            Map<String, String> singleTypeMap = typeMaps.get(key);
                            if (singleTypeMap == null) {
                                singleTypeMap = new TreeMap<String, String>();
                                typeMaps.put(key, singleTypeMap);
                            }
                            singleTypeMap.put(escapeKey(type), bcpType);
                        }
                    }
                }
            }
        }
    }

    private Resource createTypeMapResource(Map<String, Map<String, String>> typeMaps, String key) {
        ResourceTable typeMapRes = new ResourceTable();
        typeMapRes.name = LDMLBCP47Constants.TYPEMAP;

        Resource cur = null;
        for (Entry<String, Map<String, String>> typesForKeyItem : typeMaps.entrySet()) {
            String itemKey = typesForKeyItem.getKey();
            if (key != null && !itemKey.equals(key)) {
                // skip this key
                continue;
            }

            String aliasName = null;
            if (key == null) {
                for (String extKey : externalTypeKeys) {
                    if (extKey.equals(itemKey)) {
                        aliasName = "/" + itemKey + EXTERNAL_TYPES_SUFFIX
                                    + "/" + LDMLBCP47Constants.TYPEMAP
                                    + "/" + itemKey;
                        break;
                    }
                }
            }

            Resource res = null;
            if (aliasName != null) {
                // generating alias resource
                ResourceAlias typeMapForKeyResAlias = new ResourceAlias();
                typeMapForKeyResAlias.name = itemKey;
                typeMapForKeyResAlias.val = aliasName;

                res = typeMapForKeyResAlias;
            } else {
                // generating type mapping container table per key
                ResourceTable typeMapForKeyRes = new ResourceTable();
                typeMapForKeyRes.name = itemKey;

                Resource curTypeRes = null;
                for (Entry<String, String> typeItem : typesForKeyItem.getValue().entrySet()) {
                    // generating each type map data
                    ResourceString typeRes = new ResourceString();
                    typeRes.name = typeItem.getKey();
                    typeRes.val = typeItem.getValue();

                    if (curTypeRes == null) {
                        typeMapForKeyRes.first = typeRes;
                    } else {
                        curTypeRes.next = typeRes;
                    }
                    curTypeRes = typeRes;
                }

                res = typeMapForKeyRes;
            }

            if (cur == null) {
                typeMapRes.first = res;
            } else {
                cur.next = res;
            }
            cur = res;
        }

        return typeMapRes;
    }

    private Resource createTypeAliasResource(Map<String, Map<String, String>> typeAliases, String key) {
        ResourceTable typeAliasRes = new ResourceTable();
        typeAliasRes.name = LDMLBCP47Constants.TYPEALIAS;

        Resource cur = null;
        for (Entry<String, Map<String, String>> aliasesForKeyItem: typeAliases.entrySet()) {
            String itemKey = aliasesForKeyItem.getKey();
            if (key != null && !itemKey.equals(key)) {
                // skip this key
                continue;
            }

            String aliasName = null;
            if (key == null) {
                for (String extKey : externalTypeKeys) {
                    if (extKey.equals(itemKey)) {
                        aliasName = "/" + itemKey + EXTERNAL_TYPES_SUFFIX
                                    + "/" + LDMLBCP47Constants.TYPEALIAS
                                    + "/" + itemKey;
                        break;
                    }
                }
            }

            Resource res = null;

            if (aliasName != null) {
                // generating alias resource
                ResourceAlias aliasesForKeyResAlias = new ResourceAlias();
                aliasesForKeyResAlias.name = itemKey;
                aliasesForKeyResAlias.val = aliasName;

                res = aliasesForKeyResAlias;
            } else {
                // generating alias mapping container table per key
                ResourceTable aliasesForKeyRes = new ResourceTable();
                aliasesForKeyRes.name = itemKey;

                Resource curAliasRes = null;
                for (Entry<String, String> aliasItem : aliasesForKeyItem.getValue().entrySet()) {
                    // generating each alias map data
                    ResourceString aliasRes = new ResourceString();
                    aliasRes.name = aliasItem.getKey();
                    aliasRes.val = aliasItem.getValue();

                    if (curAliasRes == null) {
                        aliasesForKeyRes.first = aliasRes;
                    } else {
                        curAliasRes.next = aliasRes;
                    }
                    curAliasRes = aliasRes;
                }

                res = aliasesForKeyRes;
            }

            if (cur == null) {
                typeAliasRes.first = res;
            } else {
                cur.next = res;
            }
            cur = res;
        }

        return typeAliasRes;
    }

    private static String escapeKey(String key) {
        if (key.contains("/")) {
            key = "\"" + key.replace('/', ':') + "\"";
        }
        return key;
    }

    private void dumpMap(Map<String, String> map) {
        for (Map.Entry<String, String> e : map.entrySet()) {
            log.log(e.getKey() + " -> " + e.getValue());
        }
    }

    private static class LDMLBCP47Constants {
        static final String LDMLBCP47 = "ldmlBCP47";
        static final String KEYWORD = "keyword";
        static final String KEY = "key";
        static final String NAME = "name";
        static final String ALIAS = "alias";
        static final String TYPE = "type";
        static final String KEYTYPEDATA = "keyTypeData";
        static final String KEYMAP = "keyMap";
        static final String TYPEMAP = "typeMap";
        static final String TYPEALIAS = "typeAlias";
    }
}
