package org.unicode.cldr.unittest;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.With;
import org.unicode.cldr.util.XPathParts;

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.util.TimeZone;

public class TestBCP47 extends TestFmwk {
    private static final int WARNING = LOG; // change to WARN to enable checking for non-bcp47 attributes
    private static final int ERROR = WARN; // change to ERR to enable test

    private static final CLDRConfig testInfo = CLDRConfig.getInstance();
    private static final SupplementalDataInfo SUPPLEMENTAL_DATA_INFO = testInfo.getSupplementalDataInfo();
    private static final CLDRFile ENGLISH = testInfo.getEnglish();
    private static final Relation<String, String> bcp47key_types = SUPPLEMENTAL_DATA_INFO.getBcp47Keys();
    private static final Relation<R2<String, String>, String> bcp47keyType_aliases = SUPPLEMENTAL_DATA_INFO.getBcp47Aliases();
    private static final Map<R2<String, String>, String> deprecated = SUPPLEMENTAL_DATA_INFO.getBcp47Deprecated();

    public static void main(String[] args) {
        new TestBCP47().run(args);
    }

    private static final ChainedMap.M3<String, String, String> keyTypeTranslations = ChainedMap.of(
        new TreeMap<String, Object>(),
        new TreeMap<String, Object>(),
        String.class);
    static {
        for (String path : With.in(ENGLISH.iterator("//ldml/localeDisplayNames/keys/key"))) {
            XPathParts parts = XPathParts.getFrozenInstance(path);
            String value = ENGLISH.getStringValue(path);
            String key = parts.getAttributeValue(-1, "type");
            keyTypeTranslations.put(key, "", value);
        }
        for (String path : With.in(ENGLISH.iterator("//ldml/localeDisplayNames/types/type"))) {
            XPathParts parts = XPathParts.getFrozenInstance(path);
            String value = ENGLISH.getStringValue(path);
            String key = parts.getAttributeValue(-1, "key");
            String type = parts.getAttributeValue(-1, "type");
            keyTypeTranslations.put(key, type, value);
        }
        for (String path : With.in(ENGLISH.iterator("//ldml/localeDisplayNames/transformNames/transformName"))) {
            XPathParts parts = XPathParts.getFrozenInstance(path);
            String value = ENGLISH.getStringValue(path);
            String type = parts.getAttributeValue(-1, "type");
            keyTypeTranslations.put("d0", type, value);
        }
    }

    public void TestEnglishKeyTranslations() {
        logKnownIssue("cldr7631", "Using just warnings for now, until issues are resolved. Change WARNING/ERROR when removing this.");
        ChainedMap.M3<String, String, String> foundEnglish = ChainedMap.of(
            new TreeMap<String, Object>(),
            new TreeMap<String, Object>(),
            String.class);
        for (String bcp47Key : bcp47key_types.keySet()) {
            final R2<String, String> keyRow = Row.of(bcp47Key, "");
            if ("true".equals(deprecated.get(keyRow))) {
                logln("Skipping deprecated key:\t" + bcp47Key);
                continue;
            }
            String keyTrans = keyTypeTranslations.get(bcp47Key, "");
            Set<String> keyAliases = CldrUtility.ifNull(bcp47keyType_aliases.get(keyRow), Collections.<String> emptySet());
            String engKey = bcp47Key;
            if (keyTrans != null) {
                foundEnglish.put(engKey, "", keyTrans);
            } else {
                for (String keyAlias : keyAliases) {
                    keyTrans = keyTypeTranslations.get(keyAlias, "");
                    if (keyTrans != null) {
                        engKey = keyAlias;
                        foundEnglish.put(engKey, "", keyTrans);
                        msg("Type for English 'key' translation is " + engKey + ", while bcp47 is " + bcp47Key, WARNING, true, true);
                        break;
                    }
                }
            }
            if (keyTrans != null) {
                logln(showData(bcp47Key, "", SUPPLEMENTAL_DATA_INFO.getBcp47Descriptions().get(keyRow), keyAliases, Collections.<String> emptySet(), keyTrans));
            } else {
                msg(showData(bcp47Key, "", SUPPLEMENTAL_DATA_INFO.getBcp47Descriptions().get(keyRow), keyAliases, Collections.<String> emptySet(), "MISSING"),
                    ERROR, true, true);
            }
            if (bcp47Key.equals("tz")) {
                continue;
                // handled elsewhere
            }
            for (String bcp47Type : bcp47key_types.get(bcp47Key)) {
                checkKeyType(bcp47Key, keyAliases, engKey, bcp47Type, foundEnglish);
                if (bcp47Type.equals("REORDER_CODE")) {
                    for (String subtype : Arrays.asList("space", "punct", "symbol", "currency", "digit")) {
                        checkKeyType(bcp47Key, keyAliases, engKey, subtype, foundEnglish);
                    }
                }
            }
        }
        for (R3<String, String, String> extra : keyTypeTranslations.rows()) {
            final String key = extra.get0();
            final String type = extra.get1();
            final String trans = extra.get2();
            if (foundEnglish.get(key, type) == null) {
                if (key.equals("x")) {
                    msg("OK Extra English: " + showData(key, type, "MISSING", Collections.<String> emptySet(), Collections.<String> emptySet(), trans), LOG,
                        true, true);
                } else {
                    msg("*Extra English: " + showData(key, type, "MISSING", Collections.<String> emptySet(), Collections.<String> emptySet(), trans), ERROR,
                        true, true);
                }
            }
        }
    }

    static final ImmutableSet<String> SKIP_TYPES = ImmutableSet.of("REORDER_CODE", "RG_KEY_VALUE", "SCRIPT_CODE", "SUBDIVISION_CODE", "CODEPOINTS", "PRIVATE_USE");

    private void checkKeyType(
        String bcp47Key,
        Set<String> keyAliases,
        String engKey,
        String bcp47Type,
        ChainedMap.M3<String, String, String> foundEnglish) {
        if (SKIP_TYPES.contains(bcp47Type)) {
            logln("Skipping generic key/type:\t" + bcp47Key + "/" + bcp47Type);
            return;
        }
        final R2<String, String> row = Row.of(bcp47Key, bcp47Type);
        if ("true".equals(deprecated.get(row))) {
            logln("Skipping deprecated key/type:\t" + bcp47Key + "/" + bcp47Type);
            return;
        }
        Set<String> typeAliases = CldrUtility.ifNull(bcp47keyType_aliases.get(row), Collections.<String> emptySet());
        String engType = bcp47Type;
        String trans = keyTypeTranslations.get(engKey, engType);
        if (trans != null) {
            foundEnglish.put(engKey, engType, trans);
        } else {
            for (String typeAlias : typeAliases) {
                trans = keyTypeTranslations.get(engKey, typeAlias);
                if (trans != null) {
                    engType = typeAlias;
                    foundEnglish.put(engKey, engType, trans);
                    msg("Type for English 'key+type' translation is " + engKey + "+" + engType + ", while bcp47 is " + bcp47Key + "+" + bcp47Type, WARNING,
                        true, true);
                    break;
                }
            }
        }
        if (trans == null) {
            switch (bcp47Key) {
            case "cu":
                trans = ENGLISH.getStringValue("//ldml/numbers/currencies/currency[@type=\"" + bcp47Type.toUpperCase(Locale.ENGLISH) + "\"]/displayName");
                break;
            }
        }
        if (trans != null) {
            logln(showData(bcp47Key, bcp47Type, SUPPLEMENTAL_DATA_INFO.getBcp47Descriptions().get(row), keyAliases, typeAliases, trans));
        } else {
            msg(showData(bcp47Key, bcp47Type, SUPPLEMENTAL_DATA_INFO.getBcp47Descriptions().get(row), keyAliases, typeAliases, "MISSING"), ERROR, true, true);
        }
    }

    private String showData(String key, String type, String bcp47Description, Set<String> keyAliases, Set<String> typeAliases, String eng) {
        return "key: " + key + "\taliases: " + keyAliases + (type.isEmpty() ? "" : "\ttype: " + type + "\taliases: " + typeAliases) + "\tbcp: "
            + bcp47Description + ",\teng: " + eng;
    }

    static final Set<String> BOGUS_TZIDS = ImmutableSet.of(
        "ACT", "AET", "AGT", "ART", "AST", "BET", "BST", "CAT", "CET", "CNT", "CST", "CTT", "EAT", "ECT", "EET", "Factory", "IET", "IST", "JST", "MET", "MIT",
        "NET", "NST", "PLT", "PNT", "PRT", "PST", "SST", "SystemV/AST4", "SystemV/AST4ADT", "SystemV/CST6", "SystemV/CST6CDT", "SystemV/EST5",
        "SystemV/EST5EDT", "SystemV/HST10", "SystemV/MST7", "SystemV/MST7MDT", "SystemV/PST8", "SystemV/PST8PDT", "SystemV/YST9", "SystemV/YST9YDT", "VST",
        "WET");

    public void testBcp47IdsForAllTimezoneIds() {
        Map<String, String> aliasToId = new TreeMap<>();
        Set<String> missingAliases = new TreeSet<>();
        Set<String> deprecatedAliases = new TreeSet<>();
        Set<String> deprecatedBcp47s = new TreeSet<>();
        Set<String> bcp47IdsNotUsed = new TreeSet<>();
        for (String bcp47Type : bcp47key_types.get("tz")) {
            R2<String, String> row = Row.of("tz", bcp47Type);
            Set<String> aliasSet = bcp47keyType_aliases.get(row);
            boolean itemIsDeprecated = "true".equals(deprecated.get(row));
            if (itemIsDeprecated) {
                deprecatedBcp47s.add(bcp47Type);
            }
            bcp47IdsNotUsed.add(bcp47Type);
            if (aliasSet == null) {
                continue;
            }
            for (String alias : aliasSet) {
                aliasToId.put(alias, bcp47Type);
                if (itemIsDeprecated) {
                    deprecatedAliases.add(alias);
                }
            }
        }

        warnln("CLDR deprecated bcp47 ids: " + deprecatedBcp47s);
        warnln("CLDR deprecated tzids: " + deprecatedAliases);


        for (String tzid : TimeZone.getAvailableIDs()) {
            if (BOGUS_TZIDS.contains(tzid)) {
                continue;
            }
            String bcp47Id = aliasToId.get(tzid);
            if (!assertNotNull(tzid, bcp47Id)) {
                missingAliases.add(tzid);
            } else {
                bcp47IdsNotUsed.remove(bcp47Id);
                // check that the canonical is the first alias.
                String canonical = TimeZone.getCanonicalID(tzid);
                R2<String, String> row = Row.of("tz", bcp47Id);
                Set<String> aliasSet = bcp47keyType_aliases.get(row);
                if (assertNotNull("alias for " + bcp47Id, aliasSet)) {
                    String first = aliasSet.iterator().next();
                    assertEquals("canonical == first alias", first, canonical);
                }
            }
        }
        if (bcp47IdsNotUsed.contains("unk")) {
            R2<String, String> row = Row.of("tz", "unk");
            Set<String> aliasSet = bcp47keyType_aliases.get(row);

            warnln("No TZDB id available in ICU for: " + "unk; " + aliasSet);
            bcp47IdsNotUsed.remove("unk");
        }
        LinkedHashSet<String> diff = new LinkedHashSet<>(bcp47IdsNotUsed);
        diff.removeAll(deprecatedBcp47s);
        if (!diff.isEmpty()) {
            errln("CLDR has unused bcp47 ids: " + diff);
        } else if (!bcp47IdsNotUsed.isEmpty()) {
            warnln("CLDR has unused (but deprecated) bcp47 ids: " + bcp47IdsNotUsed);
        }

        diff = new LinkedHashSet<>(missingAliases);
        diff.removeAll(deprecatedAliases);
        if (!diff.isEmpty()) {
            warnln("ICU has unused TZIDs: " + diff);
        } else if (!missingAliases.isEmpty()) {
            warnln("ICU has unused (but deprecated-in-CLDR) TZIDs ids: " + missingAliases);
        }
    }
}
