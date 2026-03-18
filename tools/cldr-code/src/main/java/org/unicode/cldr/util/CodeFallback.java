package org.unicode.cldr.util;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class CodeFallback {

    public static XMLSource getConstructedItems() {
        return constructedItems;
    }

    private static final String[] keyDisplayNames = {
        "calendar", "cf", "collation", "currency", "hc", "lb", "ms", "numbers"
    };
    private static final String[][] typeDisplayNames = {
        {"account", "cf"},
        {"ahom", "numbers"},
        {"arab", "numbers"},
        {"arabext", "numbers"},
        {"armn", "numbers"},
        {"armnlow", "numbers"},
        {"bali", "numbers"},
        {"beng", "numbers"},
        {"brah", "numbers"},
        {"buddhist", "calendar"},
        {"cakm", "numbers"},
        {"cham", "numbers"},
        {"chinese", "calendar"},
        {"compat", "collation"},
        {"coptic", "calendar"},
        {"cyrl", "numbers"},
        {"dangi", "calendar"},
        {"deva", "numbers"},
        {"diak", "numbers"},
        {"dictionary", "collation"},
        {"ducet", "collation"},
        {"emoji", "collation"},
        {"eor", "collation"},
        {"ethi", "numbers"},
        {"ethiopic", "calendar"},
        {"ethiopic-amete-alem", "calendar"},
        {"fullwide", "numbers"},
        {"gara", "numbers"},
        {"geor", "numbers"},
        {"gong", "numbers"},
        {"gonm", "numbers"},
        {"gregorian", "calendar"},
        {"grek", "numbers"},
        {"greklow", "numbers"},
        {"gujr", "numbers"},
        {"gukh", "numbers"},
        {"guru", "numbers"},
        {"h11", "hc"},
        {"h12", "hc"},
        {"h23", "hc"},
        {"h24", "hc"},
        {"hanidec", "numbers"},
        {"hans", "numbers"},
        {"hansfin", "numbers"},
        {"hant", "numbers"},
        {"hantfin", "numbers"},
        {"hebr", "numbers"},
        {"hebrew", "calendar"},
        {"hmng", "numbers"},
        {"hmnp", "numbers"},
        {"indian", "calendar"},
        {"islamic", "calendar"},
        {"islamic-civil", "calendar"},
        {"islamic-rgsa", "calendar"},
        {"islamic-tbla", "calendar"},
        {"islamic-umalqura", "calendar"},
        {"iso8601", "calendar"},
        {"japanese", "calendar"},
        {"java", "numbers"},
        {"jpan", "numbers"},
        {"jpanfin", "numbers"},
        {"kali", "numbers"},
        {"kawi", "numbers"},
        {"khmr", "numbers"},
        {"knda", "numbers"},
        {"krai", "numbers"},
        {"lana", "numbers"},
        {"lanatham", "numbers"},
        {"laoo", "numbers"},
        {"latn", "numbers"},
        {"lepc", "numbers"},
        {"limb", "numbers"},
        {"loose", "lb"},
        {"mathbold", "numbers"},
        {"mathdbl", "numbers"},
        {"mathmono", "numbers"},
        {"mathsanb", "numbers"},
        {"mathsans", "numbers"},
        {"metric", "ms"},
        {"mlym", "numbers"},
        {"modi", "numbers"},
        {"mong", "numbers"},
        {"mroo", "numbers"},
        {"mtei", "numbers"},
        {"mymr", "numbers"},
        {"mymrepka", "numbers"},
        {"mymrpao", "numbers"},
        {"mymrshan", "numbers"},
        {"mymrtlng", "numbers"},
        {"nagm", "numbers"},
        {"nkoo", "numbers"},
        {"normal", "lb"},
        {"olck", "numbers"},
        {"onao", "numbers"},
        {"orya", "numbers"},
        {"osma", "numbers"},
        {"outlined", "numbers"},
        {"persian", "calendar"},
        {"phonebook", "collation"},
        {"pinyin", "collation"},
        {"roc", "calendar"},
        {"rohg", "numbers"},
        {"roman", "numbers"},
        {"romanlow", "numbers"},
        {"saur", "numbers"},
        {"search", "collation"},
        {"searchjl", "collation"},
        {"shrd", "numbers"},
        {"sind", "numbers"},
        {"sinh", "numbers"},
        {"sora", "numbers"},
        {"standard", "cf"},
        {"standard", "collation"},
        {"strict", "lb"},
        {"stroke", "collation"},
        {"sund", "numbers"},
        {"sunu", "numbers"},
        {"takr", "numbers"},
        {"talu", "numbers"},
        {"taml", "numbers"},
        {"tamldec", "numbers"},
        {"tnsa", "numbers"},
        {"telu", "numbers"},
        {"thai", "numbers"},
        {"tibt", "numbers"},
        {"tirh", "numbers"},
        {"tols", "numbers"},
        {"traditional", "collation"},
        {"unihan", "collation"},
        {"uksystem", "ms"},
        {"ussystem", "ms"},
        {"vaii", "numbers"},
        {"wara", "numbers"},
        {"wcho", "numbers"},
        {"zhuyin", "collation"}
    };
    // Start, reference: https://unicode-org.atlassian.net/browse/CLDR-18294
    // These codes are exceptional in the sense that adding code-fallback for them is expected
    // to be temporary for v47, and different handling will be implemented in v48.
    private static final String[] exceptionalLanguageTypes = {"gaa", "luo", "vai"};
    private static final String[] exceptionalScriptTypes = {
        "Ahom", "Arab", "Bali", "Cham", "Jamo", "Modi", "Newa", "Thai", "Toto"
    };
    private static final boolean SKIP_SINGLEZONES = false;
    private static final XMLSource constructedItems =
            new SimpleXMLSource(XMLSource.CODE_FALLBACK_ID);

    static {
        StandardCodes sc = StandardCodes.make();
        Map<String, Set<String>> countries_zoneSet = sc.zoneParser.getCountryToZoneSet();
        Map<String, String> zone_countries = sc.zoneParser.getZoneToCountry();
        List<NameType> nameTypeList =
                List.of(NameType.CURRENCY, NameType.CURRENCY_SYMBOL, NameType.TZ_EXEMPLAR);
        for (NameType nameType : nameTypeList) {
            StandardCodes.CodeType codeType = nameType.toCodeType();
            Set<String> codes = sc.getGoodAvailableCodes(codeType);
            for (String code : codes) {
                String value = code;
                if (nameType == NameType.TZ_EXEMPLAR) { // skip single-zone countries
                    if (SKIP_SINGLEZONES) {
                        String country = zone_countries.get(code);
                        Set<String> s = countries_zoneSet.get(country);
                        if (s != null && s.size() == 1) continue;
                    }
                    value = TimezoneFormatter.getFallbackName(value);
                }
                addFallbackCode(nameType, code, value);
            }
        }

        // Start, reference: https://unicode-org.atlassian.net/browse/CLDR-18294
        for (String code : exceptionalLanguageTypes) {
            constructedItems.putValueAtPath(NameType.LANGUAGE.getKeyPath(code), code);
        }
        for (String code : exceptionalScriptTypes) {
            constructedItems.putValueAtPath(NameType.SCRIPT.getKeyPath(code), code);
        }
        constructedItems.putValueAtPath(
                "//ldml/dates/timeZoneNames/metazone[@type=\"Acre\"]/long/generic", "Acre");
        // End, reference: https://unicode-org.atlassian.net/browse/CLDR-18294

        addFallbackCodeVariant(
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/eras/eraAbbr/era[@type=\"0\"]",
                "BCE");
        addFallbackCodeVariant(
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/eras/eraAbbr/era[@type=\"1\"]",
                "CE");
        addFallbackCodeVariant(
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/eras/eraNames/era[@type=\"0\"]",
                "BCE");
        addFallbackCodeVariant(
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/eras/eraNames/era[@type=\"1\"]",
                "CE");
        addFallbackCodeVariant(
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/eras/eraNarrow/era[@type=\"0\"]",
                "BCE");
        addFallbackCodeVariant(
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/eras/eraNarrow/era[@type=\"1\"]",
                "CE");

        for (String keyDisplayName : keyDisplayNames) {
            constructedItems.putValueAtPath(
                    "//ldml/localeDisplayNames/keys/key" + "[@type=\"" + keyDisplayName + "\"]",
                    keyDisplayName);
        }
        for (String[] typeDisplayName : typeDisplayNames) {
            constructedItems.putValueAtPath(
                    "//ldml/localeDisplayNames/types/type"
                            + "[@key=\""
                            + typeDisplayName[1]
                            + "\"]"
                            + "[@type=\""
                            + typeDisplayName[0]
                            + "\"]",
                    typeDisplayName[0]);
        }
        constructedItems.freeze();
    }

    private static void addFallbackCode(NameType nameType, String code, String value) {
        String fullpath = nameType.getKeyPath(code);
        addFallbackCodeToConstructedItems(fullpath, value, null);
    }

    private static void addFallbackCodeVariant(String fullpath, String value) {
        addFallbackCodeToConstructedItems(fullpath, value, "variant");
    }

    private static void addFallbackCodeToConstructedItems(
            String fullpath, String value, String alt) {
        if (alt != null) {
            // Insert the @alt= string after the last occurrence of "]"
            StringBuilder fullpathBuf = new StringBuilder(fullpath);
            fullpath =
                    fullpathBuf
                            .insert(fullpathBuf.lastIndexOf("]") + 1, "[@alt=\"" + alt + "\"]")
                            .toString();
        }
        constructedItems.putValueAtPath(fullpath, value);
        if (fullpath.startsWith("//ldml/numbers/currencies/currency")
                && fullpath.endsWith("/displayName")) {
            String otherPath = fullpath + "[@count=\"other\"]";
            constructedItems.putValueAtPath(otherPath, value);
        }
    }
}
