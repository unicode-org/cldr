package org.unicode.cldr.util;

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

    public static NameType fromPath(String xpath) {
        int cldrInt = CLDRFile.getNameType(xpath);
        return fromCldrInt(cldrInt);
    }

    @Deprecated
    public int toCldrInt() {
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

    public String getKeyPath(String code) {
        return CLDRFile.getKey(this.toCldrInt(), code);
    }

    public String getNameName() {
        return CLDRFile.getNameName(this.toCldrInt());
    }

    public String getNameTypeName() {
        return CLDRFile.getNameTypeName(this.toCldrInt());
    }

    public static String getCode(String path) {
        return CLDRFile.getCode(path);
    }

    public static NameType getNameType(String xpath) {
        int cldrInt = CLDRFile.getNameType(xpath);
        return fromCldrInt(cldrInt);
    }
}
