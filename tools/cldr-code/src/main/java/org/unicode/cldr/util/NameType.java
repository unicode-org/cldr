package org.unicode.cldr.util;

import static org.unicode.cldr.util.StandardCodes.CodeType.*;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
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
 *
 * <p>Each type is associated with a category of xpaths that follow a certain pattern. A pattern
 * starts with a first string and ends with a second string, with room in between for a variable
 * type code. Or, for KEY_TYPE, there are first/second/third strings, with room in between for two
 * variable codes: a key code and a type code.
 */
public enum NameType {
    NONE("", ""),
    LANGUAGE("//ldml/localeDisplayNames/languages/language[@type=\"", "\"]"),
    SCRIPT("//ldml/localeDisplayNames/scripts/script[@type=\"", "\"]"),
    TERRITORY("//ldml/localeDisplayNames/territories/territory[@type=\"", "\"]"),
    VARIANT("//ldml/localeDisplayNames/variants/variant[@type=\"", "\"]"),
    CURRENCY("//ldml/numbers/currencies/currency[@type=\"", "\"]/displayName"),
    CURRENCY_SYMBOL("//ldml/numbers/currencies/currency[@type=\"", "\"]/symbol"),
    TZ_EXEMPLAR("//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/exemplarCity"),
    TZ_GENERIC_LONG("//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/long/generic"),
    TZ_GENERIC_SHORT("//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/short/generic"),
    TZ_STANDARD_LONG("//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/long/standard"),
    TZ_STANDARD_SHORT("//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/short/standard"),
    TZ_DAYLIGHT_LONG("//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/long/daylight"),
    TZ_DAYLIGHT_SHORT("//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/short/daylight"),
    KEY("//ldml/localeDisplayNames/keys/key[@type=\"", "\"]"),
    KEY_TYPE("//ldml/localeDisplayNames/types/type[@key=\"", "\"][@type=\"", "\"]"),
    SUBDIVISION("//ldml/localeDisplayNames/subdivisions/subdivision[@type=\"", "\"]");

    /**
     * The first, second, and (where applicable) third fragments comprising the pattern for the
     * category of xpaths corresponding to this NameType
     */
    private final String first, second, third;

    NameType(String first, String second) {
        this(first, second, "");
    }

    NameType(String first, String second, String third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public StandardCodes.CodeType toCodeType() {
        switch (this) {
            case LANGUAGE:
                return language;
            case SCRIPT:
                return script;
            case TERRITORY:
                return territory;
            case VARIANT:
                return variant;
            case CURRENCY:
            case CURRENCY_SYMBOL:
                return currency;
            case TZ_EXEMPLAR:
                return tzid;
            default:
                throw new IllegalArgumentException("Unsupported name type");
        }
    }

    /**
     * Get the NameType corresponding to the given path.
     *
     * @param xpath the given path, such as "//ldml/localeDisplayNames/scripts/script[@type=\""
     * @return the NameType, such as SCRIPT
     */
    public static NameType fromPath(String xpath) {
        for (NameType nameType : NameType.values()) {
            if (nameType != NONE
                    && xpath.startsWith(nameType.first)
                    && xpath.indexOf(nameType.second, nameType.first.length()) >= 0) {
                return nameType;
            }
        }
        return NONE;
    }

    /**
     * Get the xpath used to access data of a given type
     *
     * @param code the code such as "am", meaning "Amharic", if this is NameType.LANGUAGE
     * @return the path such as //ldml/localeDisplayNames/languages/language[@type="am"]
     */
    public String getKeyPath(String code) {
        code = fixCode(code);
        if (code.contains("|")) {
            // Special handling for "key|type" for KEY_TYPE
            if (!KEY_TYPE.equals(this)) {
                throw new IllegalArgumentException("Bar code is only for KEY_TYPE");
            }
            String[] codes = code.split("\\|");
            return first + fixKeyName(codes[0]) + second + codes[1] + third;
        } else {
            return first + code + second;
        }
    }

    private String fixCode(String code) {
        switch (this) {
            case VARIANT:
                return code.toUpperCase(Locale.ROOT);
            case KEY:
                return fixKeyName(code);
            case TZ_DAYLIGHT_LONG:
            case TZ_DAYLIGHT_SHORT:
            case TZ_EXEMPLAR:
            case TZ_GENERIC_LONG:
            case TZ_GENERIC_SHORT:
            case TZ_STANDARD_LONG:
            case TZ_STANDARD_SHORT:
                return CLDRFile.getLongTzid(code);
            default:
                return code;
        }
    }

    private static final ImmutableMap<String, String> FIX_KEY_NAME;

    static {
        ImmutableMap.Builder<String, String> temp = ImmutableMap.builder();
        for (String s :
                Arrays.asList(
                        "colAlternate",
                        "colBackwards",
                        "colCaseFirst",
                        "colCaseLevel",
                        "colNormalization",
                        "colNumeric",
                        "colReorder",
                        "colStrength")) {
            temp.put(s.toLowerCase(Locale.ROOT), s);
        }
        FIX_KEY_NAME = temp.build();
    }

    private static String fixKeyName(String code) {
        String result = FIX_KEY_NAME.get(code);
        return result == null ? code : result;
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
        NameType nameType = fromPath(path);
        if (nameType == NONE) {
            throw new IllegalArgumentException("Illegal type in path: " + path);
        }
        int start = nameType.first.length();
        int end = path.indexOf(nameType.second, start);
        return path.substring(start, end);
    }

    /**
     * Get the NameType represented by the given string. For backward compatibility, allow for:
     * uppercase or lowercase; hyphens in place of underscores; "exemplar-city" for TZ_EXEMPLAR;
     * "key|type" for KEY_TYPE; and "region" for TERRITORY
     *
     * @param typeString a string such as "language", "script", "tz-generic-long", "key|type", ...
     * @return the corresponding NameType, or NONE
     */
    public static NameType typeNameToCode(String typeString) {
        String s = typeString.toUpperCase().replace("-", "_");
        try {
            return NameType.valueOf(s);
        } catch (IllegalArgumentException e) {
            switch (s) {
                case "EXEMPLAR_CITY":
                    return TZ_EXEMPLAR;
                case "KEY|TYPE":
                    return KEY_TYPE;
                case "REGION":
                    return TERRITORY;
            }
        }
        return NONE;
    }

    /**
     * Get the string to match the beginning of paths corresponding to this NameType
     *
     * <p>For example, for LANGUAGE return "//ldml/localeDisplayNames/languages/language[@type=\""
     *
     * @return the string
     */
    public String getPathStart() {
        return first;
    }
}
