package org.unicode.cldr.util;

import java.util.Locale;

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

    // Caution: the presence of "key|type" presents difficulties for refactoring.
    // The row with "key|type" has four strings, while the others have three.
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

    public static NameType fromPath(String xpath) {
        int cldrInt = getNameTypeFromTable(xpath);
        return fromCldrInt(cldrInt);
    }

    @Deprecated
    private int toCldrInt() {
        switch (this) {
            case NONE:
                return CLDRFile.NO_NAME;
            case LANGUAGE:
                return CLDRFile.LANGUAGE_NAME;
            case SCRIPT:
                return CLDRFile.SCRIPT_NAME;
            case TERRITORY:
                return CLDRFile.TERRITORY_NAME;
            case VARIANT:
                return CLDRFile.VARIANT_NAME;
            case CURRENCY:
                return CLDRFile.CURRENCY_NAME;
            case CURRENCY_SYMBOL:
                return CLDRFile.CURRENCY_SYMBOL;
            case TZ_EXEMPLAR:
                return CLDRFile.TZ_EXEMPLAR;
            case TZ_GENERIC_LONG:
                return CLDRFile.TZ_GENERIC_LONG;
            case TZ_GENERIC_SHORT:
                return CLDRFile.TZ_GENERIC_SHORT;
            case TZ_STANDARD_LONG:
                return CLDRFile.TZ_STANDARD_LONG;
            case TZ_STANDARD_SHORT:
                return CLDRFile.TZ_STANDARD_SHORT;
            case TZ_DAYLIGHT_LONG:
                return CLDRFile.TZ_DAYLIGHT_LONG;
            case TZ_DAYLIGHT_SHORT:
                return CLDRFile.TZ_DAYLIGHT_SHORT;
            case KEY:
                return CLDRFile.KEY_NAME;
            case KEY_TYPE:
                return CLDRFile.KEY_TYPE_NAME;
            case SUBDIVISION:
                return CLDRFile.SUBDIVISION_NAME;
        }
        throw new RuntimeException("Unrecognized NameType in toCldrInt: " + this);
    }

    @Deprecated
    public static NameType fromCldrInt(int typeNum) {
        switch (typeNum) {
            case CLDRFile.NO_NAME:
                return NONE;
            case CLDRFile.LANGUAGE_NAME:
                return LANGUAGE;
            case CLDRFile.SCRIPT_NAME:
                return SCRIPT;
            case CLDRFile.TERRITORY_NAME:
                return TERRITORY;
            case CLDRFile.VARIANT_NAME:
                return VARIANT;
            case CLDRFile.CURRENCY_NAME:
                return CURRENCY;
            case CLDRFile.CURRENCY_SYMBOL:
                return CURRENCY_SYMBOL;
            case CLDRFile.TZ_EXEMPLAR:
                return TZ_EXEMPLAR;
            case CLDRFile.TZ_GENERIC_LONG:
                return TZ_GENERIC_LONG;
            case CLDRFile.TZ_GENERIC_SHORT:
                return TZ_GENERIC_SHORT;
            case CLDRFile.TZ_STANDARD_LONG:
                return TZ_STANDARD_LONG;
            case CLDRFile.TZ_STANDARD_SHORT:
                return TZ_STANDARD_SHORT;
            case CLDRFile.TZ_DAYLIGHT_LONG:
                return TZ_DAYLIGHT_LONG;
            case CLDRFile.TZ_DAYLIGHT_SHORT:
                return TZ_DAYLIGHT_SHORT;
            case CLDRFile.KEY_NAME:
                return KEY;
            case CLDRFile.KEY_TYPE_NAME:
                return KEY_TYPE;
            case CLDRFile.SUBDIVISION_NAME:
                return SUBDIVISION;
        }
        throw new RuntimeException("Unrecognized typeNum in fromCldrInt: " + typeNum);
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
        String[] nameTableRow = NameTable[this.toCldrInt()];
        if (code.contains("|")) {
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
        int index = this.toCldrInt();
        String[] nameTableRow = NameTable[index];
        return nameTableRow[nameTableRow.length - 1];
    }

    /** Gets the display name for a type */
    public String getNameTypeName() {
        int index = this.toCldrInt();
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
        int type = getNameTypeFromTable(path);
        if (type == CLDRFile.NO_NAME) {
            throw new IllegalArgumentException("Illegal type in path: " + path);
        }
        String[] nameTableRow = NameTable[type];
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
        int cldrInt = -1;
        for (int i = 0; i <= CLDRFile.LIMIT_TYPES; ++i) {
            String[] nameTableRow = NameTable[i];
            String s = nameTableRow[nameTableRow.length - 1];
            if (typeString.equalsIgnoreCase(s)) {
                cldrInt = i;
                break;
            }
        }
        if (cldrInt == -1) {
            return NONE;
        }
        return fromCldrInt(cldrInt);
    }

    /**
     * Get the string to match the beginning of paths corresponding to this NameType
     *
     * <p>For example, for LANGUAGE return "//ldml/localeDisplayNames/languages/language[@type=\""
     *
     * @return the string
     */
    public String getPathStart() {
        int index = this.toCldrInt();
        return NameTable[index][0];
    }

    private static int getNameTypeFromTable(String xpath) {
        for (int i = 0; i < NameTable.length; ++i) {
            if (!xpath.startsWith(NameTable[i][0])) continue;
            if (xpath.indexOf(NameTable[i][1], NameTable[i][0].length()) >= 0) return i;
        }
        return CLDRFile.NO_NAME;
    }
}
