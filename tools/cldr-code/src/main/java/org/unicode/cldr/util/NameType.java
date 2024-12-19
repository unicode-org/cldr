package org.unicode.cldr.util;

import java.util.Locale;

/**
 * There are different types of name. This enum enumerates some of those types.
 *
 * <p>For example, "Sanskrit", "Devanagari", and "India" are names of a language, a script, and a
 * territory, respectively. A generic method that works for languages, scripts, territories, etc.,
 * may be called to get one of those names (or their equivalents for a locale other than English)
 * with a NameType parameter set to NameType.LANGUAGE, NameType.SCRIPT, or NameType.TERRITORY,
 * respectively, to indicate what type of name is requested (possibly with additional parameters
 * like a code such as "sa" for "Sanskrit"). The values starting with TZ_ are for time zones.
 */
public enum NameType {
    NONE,
    LANGUAGE,
    SCRIPT,
    TERRITORY,
    VARIANT,
    CURRENCY,
    CURRENCY_SYMBOL,
    TZ_EXEMPLAR,
    TZ_GENERIC_LONG,
    TZ_GENERIC_SHORT,
    TZ_STANDARD_LONG,
    TZ_STANDARD_SHORT,
    TZ_DAYLIGHT_LONG,
    TZ_DAYLIGHT_SHORT,
    KEY,
    KEY_TYPE,
    SUBDIVISION;

    /**
     * This data in NameTable is used for associating types of path with types of name. For legacy
     * reasons it still contains strings like "language" rather than the corresponding enum values
     * like NameType.LANGUAGE.
     *
     * <p>The order of rows must correspond to INDEX_LANGUAGE, INDEX_SCRIPT, etc. Caution: the
     * presence of "key|type" presents complications for refactoring. The row with "key|type" has
     * four strings, while the others have three.
     */
    private static final String[][] NameTable = {
        {"//ldml/localeDisplayNames/languages/language[@type=\"", "\"]", "language"},
        {"//ldml/localeDisplayNames/scripts/script[@type=\"", "\"]", "script"},
        {"//ldml/localeDisplayNames/territories/territory[@type=\"", "\"]", "territory"},
        {"//ldml/localeDisplayNames/variants/variant[@type=\"", "\"]", "variant"},
        {"//ldml/numbers/currencies/currency[@type=\"", "\"]/displayName", "currency"},
        {"//ldml/numbers/currencies/currency[@type=\"", "\"]/symbol", "currency-symbol"},
        {"//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/exemplarCity", "exemplar-city"},
        {"//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/long/generic", "tz-generic-long"},
        {"//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/short/generic", "tz-generic-short"},
        {"//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/long/standard", "tz-standard-long"},
        {"//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/short/standard", "tz-standard-short"},
        {"//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/long/daylight", "tz-daylight-long"},
        {"//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/short/daylight", "tz-daylight-short"},
        {"//ldml/localeDisplayNames/keys/key[@type=\"", "\"]", "key"},
        {"//ldml/localeDisplayNames/types/type[@key=\"", "\"][@type=\"", "\"]", "key|type"},
        {"//ldml/localeDisplayNames/subdivisions/subdivision[@type=\"", "\"]", "subdivision"},
    };

    /**
     * The numeric values of these constants must correspond to the order of rows in NameTable. For
     * example, the first row is for "language", which must match INDEX_LANGUAGE = 0. The second row
     * is for "script", which must match INDEX_SCRIPT = 1. As a special case, the row for "key|type"
     * must match INDEX_KEY_TYPE.
     */
    private static final int INDEX_NONE = -1,
            INDEX_LANGUAGE = 0,
            INDEX_SCRIPT = 1,
            INDEX_TERRITORY = 2,
            INDEX_VARIANT = 3,
            INDEX_CURRENCY = 4,
            INDEX_CURRENCY_SYMBOL = 5,
            INDEX_TZ_EXEMPLAR = 6,
            INDEX_TZ_GENERIC_LONG = 7,
            INDEX_TZ_GENERIC_SHORT = 8,
            INDEX_TZ_STANDARD_LONG = 9,
            INDEX_TZ_STANDARD_SHORT = 10,
            INDEX_TZ_DAYLIGHT_LONG = 11,
            INDEX_TZ_DAYLIGHT_SHORT = 12,
            INDEX_KEY = 13,
            INDEX_KEY_TYPE = 14,
            INDEX_SUBDIVISION = 15,
            INDEX_MAX = 15;

    private int nameTableIndex() {
        switch (this) {
            case NONE:
                return INDEX_NONE;
            case LANGUAGE:
                return INDEX_LANGUAGE;
            case SCRIPT:
                return INDEX_SCRIPT;
            case TERRITORY:
                return INDEX_TERRITORY;
            case VARIANT:
                return INDEX_VARIANT;
            case CURRENCY:
                return INDEX_CURRENCY;
            case CURRENCY_SYMBOL:
                return INDEX_CURRENCY_SYMBOL;
            case TZ_EXEMPLAR:
                return INDEX_TZ_EXEMPLAR;
            case TZ_GENERIC_LONG:
                return INDEX_TZ_GENERIC_LONG;
            case TZ_GENERIC_SHORT:
                return INDEX_TZ_GENERIC_SHORT;
            case TZ_STANDARD_LONG:
                return INDEX_TZ_STANDARD_LONG;
            case TZ_STANDARD_SHORT:
                return INDEX_TZ_STANDARD_SHORT;
            case TZ_DAYLIGHT_LONG:
                return INDEX_TZ_DAYLIGHT_LONG;
            case TZ_DAYLIGHT_SHORT:
                return INDEX_TZ_DAYLIGHT_SHORT;
            case KEY:
                return INDEX_KEY;
            case KEY_TYPE:
                return INDEX_KEY_TYPE;
            case SUBDIVISION:
                return INDEX_SUBDIVISION;
        }
        throw new RuntimeException("Unrecognized NameType in nameTableIndex: " + this);
    }

    private static NameType fromNameTableIndex(int index) {
        switch (index) {
            case INDEX_NONE:
                return NONE;
            case INDEX_LANGUAGE:
                return LANGUAGE;
            case INDEX_SCRIPT:
                return SCRIPT;
            case INDEX_TERRITORY:
                return TERRITORY;
            case INDEX_VARIANT:
                return VARIANT;
            case INDEX_CURRENCY:
                return CURRENCY;
            case INDEX_CURRENCY_SYMBOL:
                return CURRENCY_SYMBOL;
            case INDEX_TZ_EXEMPLAR:
                return TZ_EXEMPLAR;
            case INDEX_TZ_GENERIC_LONG:
                return TZ_GENERIC_LONG;
            case INDEX_TZ_GENERIC_SHORT:
                return TZ_GENERIC_SHORT;
            case INDEX_TZ_STANDARD_LONG:
                return TZ_STANDARD_LONG;
            case INDEX_TZ_STANDARD_SHORT:
                return TZ_STANDARD_SHORT;
            case INDEX_TZ_DAYLIGHT_LONG:
                return TZ_DAYLIGHT_LONG;
            case INDEX_TZ_DAYLIGHT_SHORT:
                return TZ_DAYLIGHT_SHORT;
            case INDEX_KEY:
                return KEY;
            case INDEX_KEY_TYPE:
                return KEY_TYPE;
            case INDEX_SUBDIVISION:
                return SUBDIVISION;
        }
        throw new RuntimeException("Unrecognized index in fromNameTableIndex: " + index);
    }

    /**
     * Get the NameType corresponding to the given path.
     *
     * @param xpath the given path, such as "//ldml/localeDisplayNames/scripts/script[@type=\""
     * @return the NameType, such as SCRIPT
     */
    public static NameType fromPath(String xpath) {
        int index = getIndexFromTable(xpath);
        return fromNameTableIndex(index);
    }

    /**
     * @return the xpath used to access data of a given type
     */
    public String getKeyPath(String code) {
        switch (this) {
            case VARIANT:
                code = code.toUpperCase(Locale.ROOT);
                break;
            case KEY:
                code = CLDRFile.fixKeyName(code);
                break;
            case TZ_DAYLIGHT_LONG:
            case TZ_DAYLIGHT_SHORT:
            case TZ_EXEMPLAR:
            case TZ_GENERIC_LONG:
            case TZ_GENERIC_SHORT:
            case TZ_STANDARD_LONG:
            case TZ_STANDARD_SHORT:
                code = CLDRFile.getLongTzid(code);
                break;
        }
        String[] nameTableRow = NameTable[this.nameTableIndex()];
        if (code.contains("|")) {
            // Special handling for "key|type" for KEY_TYPE
            String[] codes = code.split("\\|");
            return nameTableRow[0]
                    + CLDRFile.fixKeyName(codes[0])
                    + nameTableRow[1]
                    + codes[1]
                    + nameTableRow[2];
        } else {
            return nameTableRow[0] + code + nameTableRow[1];
        }
    }

    public String getNameName() {
        int index = this.nameTableIndex();
        String[] nameTableRow = NameTable[index];
        return nameTableRow[nameTableRow.length - 1];
    }

    /** Gets the display name for a type */
    public String getNameTypeName() {
        int index = this.nameTableIndex();
        try {
            String[] nameTableRow = NameTable[index];
            return nameTableRow[nameTableRow.length - 1];
        } catch (Exception e) {
            return "Illegal Type Name: " + index;
        }
    }

    /**
     * Get the code used to access data of a given type from the path.
     *
     * <p>For example, given //ldml/localeDisplayNames/languages/language[@type="mul"], return "mul"
     *
     * @param path the xpath
     * @return the code, or null if not found
     */
    public static String getCode(String path) {
        int index = getIndexFromTable(path);
        if (index == INDEX_NONE) {
            throw new IllegalArgumentException("Illegal type in path: " + path);
        }
        String[] nameTableRow = NameTable[index];
        int start = nameTableRow[0].length();
        int end = path.indexOf(nameTableRow[1], start);
        return path.substring(start, end);
    }

    /**
     * @param typeString a string such as "language", "script", "territory", "region", ...
     * @return the corresponding NameType
     */
    public static NameType typeNameToCode(String typeString) {
        if (typeString.equalsIgnoreCase("region")) {
            typeString = "territory";
        }
        int index = INDEX_NONE;
        for (int i = 0; i <= INDEX_MAX; ++i) {
            String[] nameTableRow = NameTable[i];
            String s = nameTableRow[nameTableRow.length - 1];
            if (typeString.equalsIgnoreCase(s)) {
                index = i;
                break;
            }
        }
        if (index == INDEX_NONE) {
            return NONE;
        }
        return fromNameTableIndex(index);
    }

    /**
     * Get the string to match the beginning of paths corresponding to this NameType
     *
     * <p>For example, for LANGUAGE return "//ldml/localeDisplayNames/languages/language[@type=\""
     *
     * @return the string
     */
    public String getPathStart() {
        int index = this.nameTableIndex();
        return NameTable[index][0];
    }

    private static int getIndexFromTable(String xpath) {
        for (int i = 0; i < NameTable.length; ++i) {
            if (!xpath.startsWith(NameTable[i][0])) continue;
            if (xpath.indexOf(NameTable[i][1], NameTable[i][0].length()) >= 0) return i;
        }
        return INDEX_NONE;
    }
}
