package org.unicode.cldr.util;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.Transform;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NameGetter {

    private final CLDRFile cldrFile;

    /**
     * Construct a new NameGetter.
     *
     * @param cldrFile must not be null
     */
    public NameGetter(CLDRFile cldrFile) {
        if (cldrFile == null) {
            throw new IllegalArgumentException("NameGetter must have non-null CLDRFile");
        }
        this.cldrFile = cldrFile;
    }

    public enum NameOpt {
        DEFAULT,
        COMPOUND_ONLY
    }

    private static final String GETNAME_LOCALE_SEPARATOR_PATH =
            "//ldml/localeDisplayNames/localeDisplayPattern/localeSeparator";
    private static final String GETNAME_LOCALE_PATTERN_PATH =
            "//ldml/localeDisplayNames/localeDisplayPattern/localePattern";
    private static final String GETNAME_LOCALE_KEY_TYPE_PATTERN_PATH =
            "//ldml/localeDisplayNames/localeDisplayPattern/localeKeyTypePattern";

    private static final Joiner JOIN_HYPHEN = Joiner.on('-');
    private static final Joiner JOIN_UNDERBAR = Joiner.on('_');

    /**
     * Get a name, given a NameType and a code.
     *
     * @param type the NameType such as NameType.TERRITORY
     * @param code a code such as "JP"
     * @return the name
     */
    public String getNameFromTypeEnumCode(NameType type, String code) {
        return getNameFromTypeCodeTransformPaths(type, code, null, null);
    }

    public String getNameFromTypeCodeAltpicker(
            NameType type, String code, Transform<String, String> altPicker) {
        return getNameFromTypeCodeTransformPaths(type, code, altPicker, null);
    }

    /**
     * Returns the name of the given identifier. Note that extensions must be specified using the
     * old "\@key=type" syntax.
     *
     * @param localeOrTZID the identifier, BCP47 or extension thereof, such as for locale or
     *     timezone ID
     * @return the name of the given identifier
     */
    public synchronized String getNameFromIdentifier(String localeOrTZID) {
        return getNameFromIdentifierOpt(localeOrTZID, NameOpt.DEFAULT);
    }

    public String getNameFromIdentifierCompoundOnly(String localeOrTZID) {
        return getNameFromIdentifierOpt(localeOrTZID, NameOpt.COMPOUND_ONLY);
    }

    /**
     * Returns the name of the given identifier. Note that extensions must be specified using the
     * old "\@key=type" syntax.
     *
     * @param localeOrTZID the identifier, BCP47 or extension thereof, such as for locale or
     *     timezone ID
     * @param nameOpt if COMPOUND_ONLY, returns "English (United Kingdom)" instead of "British
     *     English"
     * @return the name of the given identifier
     */
    public synchronized String getNameFromIdentifierOpt(String localeOrTZID, NameOpt nameOpt) {
        return getNameFromIdentifierOptAlt(localeOrTZID, nameOpt, null);
    }

    public String getNameFromParserOpt(LanguageTagParser lparser, NameOpt nameOpt) {
        return getNameFromOtherThings(
                lparser,
                nameOpt,
                null,
                cldrFile.getWinningValueWithBailey(GETNAME_LOCALE_KEY_TYPE_PATTERN_PATH),
                cldrFile.getWinningValueWithBailey(GETNAME_LOCALE_PATTERN_PATH),
                cldrFile.getWinningValueWithBailey(GETNAME_LOCALE_SEPARATOR_PATH),
                null);
    }

    public synchronized String getNameFromIdentifierEtc(
            String localeOrTZID,
            NameOpt nameOpt,
            String localeKeyTypePattern,
            String localePattern,
            String localeSeparator) {
        return getNameFromManyThings(
                localeOrTZID,
                nameOpt,
                localeKeyTypePattern,
                localePattern,
                localeSeparator,
                null,
                null);
    }

    /**
     * Returns the name of the given identifier. Note that extensions must be specified using the
     * old "\@key=type" syntax.
     *
     * @param localeOrTZID the identifier, BCP47 or extension thereof, such as for locale or
     *     timezone ID
     * @param nameOpt if COMPOUND_ONLY, returns "English (United Kingdom)" instead of "British
     *     English"
     * @param altPicker Used to select particular alts. For example, SHORT_ALTS can be used to get
     *     "English (U.K.)" instead of "English (United Kingdom)"
     * @return the name of the given identifier
     */
    public synchronized String getNameFromIdentifierOptAlt(
            String localeOrTZID, NameOpt nameOpt, Transform<String, String> altPicker) {
        return getNameFromIdentifierOptAltPaths(localeOrTZID, nameOpt, altPicker, null);
    }

    /**
     * Returns the name of the given identifier. Note that extensions must be specified using the
     * old "\@key=type" syntax.
     *
     * @param localeOrTZID the identifier, BCP47 or extension thereof, such as for locale or
     *     timezone ID
     * @param nameOpt if COMPOUND_ONLY, returns "English (United Kingdom)" instead of "British
     *     English"
     * @param altPicker Used to select particular alts. For example, SHORT_ALTS can be used to get
     *     "English (U.K.)" instead of "English (United Kingdom)"
     * @return the name of the given identifier
     */
    public synchronized String getNameFromIdentifierOptAltPaths(
            String localeOrTZID,
            NameOpt nameOpt,
            Transform<String, String> altPicker,
            Set<String> paths) {
        return getNameFromManyThings(
                localeOrTZID,
                nameOpt,
                cldrFile.getWinningValueWithBailey(GETNAME_LOCALE_KEY_TYPE_PATTERN_PATH),
                cldrFile.getWinningValueWithBailey(GETNAME_LOCALE_PATTERN_PATH),
                cldrFile.getWinningValueWithBailey(GETNAME_LOCALE_SEPARATOR_PATH),
                altPicker,
                paths);
    }

    /**
     * Returns the name of the given identifier. Note that extensions must be specified using the
     * old "\@key=type" syntax. Only used by ExampleGenerator.
     *
     * @param localeOrTZID the identifier, BCP47 or extension thereof, such as for locale or
     *     timezone ID
     * @param nameOpt if COMPOUND_ONLY, returns "English (United Kingdom)" instead of "British
     *     English"
     * @param localeKeyTypePattern the pattern used to format key-type pairs
     * @param localePattern the pattern used to format primary/secondary subtags
     * @param localeSeparator the list separator for secondary subtags
     * @param paths if non-null, fillin with contributory paths
     * @return the name of the given identifier
     */
    private synchronized String getNameFromManyThings(
            String localeOrTZID,
            NameOpt nameOpt,
            String localeKeyTypePattern,
            String localePattern,
            String localeSeparator,
            Transform<String, String> altPicker,
            Set<String> paths) {
        // Hack for seed
        if (localePattern == null) {
            localePattern = "{0} ({1})";
        }
        boolean isCompound = localeOrTZID.contains("_");
        String name =
                isCompound && nameOpt == NameOpt.COMPOUND_ONLY
                        ? null
                        : getNameFromTypeCodeTransformPaths(
                                NameType.LANGUAGE, localeOrTZID, altPicker, paths);

        // TODO - handle arbitrary combinations
        if (name != null && !name.contains("_") && !name.contains("-")) {
            name = replaceBracketsForName(name);
            return name;
        }
        LanguageTagParser lparser = new LanguageTagParser().set(localeOrTZID);
        return getNameFromOtherThings(
                lparser,
                nameOpt,
                altPicker,
                localeKeyTypePattern,
                localePattern,
                localeSeparator,
                paths);
    }

    private String getNameFromOtherThings(
            LanguageTagParser lparser,
            NameOpt nameOpt,
            Transform<String, String> altPicker,
            String localeKeyTypePattern,
            String localePattern,
            String localeSeparator,
            Set<String> paths) {
        String name;
        String original;

        // we need to check for prefixes, for lang+script or lang+country
        boolean haveScript = false;
        boolean haveRegion = false;
        // try lang+script
        if (nameOpt == NameOpt.COMPOUND_ONLY) {
            name =
                    getNameFromTypeCodeTransformPaths(
                            NameType.LANGUAGE, original = lparser.getLanguage(), altPicker, paths);
            if (name == null) name = original;
        } else { // nameOpt == NameOpt.DEFAULT
            String x = lparser.toString(LanguageTagParser.LANGUAGE_SCRIPT_REGION);
            name = getNameFromTypeCodeTransformPaths(NameType.LANGUAGE, x, altPicker, paths);
            if (name != null) {
                haveScript = haveRegion = true;
            } else {
                name =
                        getNameFromTypeCodeTransformPaths(
                                NameType.LANGUAGE,
                                lparser.toString(LanguageTagParser.LANGUAGE_SCRIPT),
                                altPicker,
                                paths);
                if (name != null) {
                    haveScript = true;
                } else {
                    name =
                            getNameFromTypeCodeTransformPaths(
                                    NameType.LANGUAGE,
                                    lparser.toString(LanguageTagParser.LANGUAGE_REGION),
                                    altPicker,
                                    paths);
                    if (name != null) {
                        haveRegion = true;
                    } else {
                        name =
                                getNameFromTypeCodeTransformPaths(
                                        NameType.LANGUAGE,
                                        original = lparser.getLanguage(),
                                        altPicker,
                                        paths);
                        if (name == null) {
                            name = original;
                        }
                    }
                }
            }
        }
        name = replaceBracketsForName(name);
        String extras = "";
        if (!haveScript) {
            extras =
                    addDisplayName(
                            lparser.getScript(),
                            NameType.SCRIPT,
                            localeSeparator,
                            extras,
                            altPicker,
                            paths);
        }
        if (!haveRegion) {
            extras =
                    addDisplayName(
                            lparser.getRegion(),
                            NameType.TERRITORY,
                            localeSeparator,
                            extras,
                            altPicker,
                            paths);
        }
        List<String> variants = lparser.getVariants();
        for (String orig : variants) {
            extras =
                    addDisplayName(
                            orig, NameType.VARIANT, localeSeparator, extras, altPicker, paths);
        }

        // Look for key-type pairs.
        for (Map.Entry<String, List<String>> extension :
                lparser.getLocaleExtensionsDetailed().entrySet()) {
            String key = extension.getKey();
            if (key.equals("h0")) {
                continue;
            }
            List<String> keyValue = extension.getValue();
            String oldFormatType =
                    (key.equals("ca") ? JOIN_HYPHEN : JOIN_UNDERBAR)
                            .join(keyValue); // default value
            // Check if key/type pairs exist in the CLDRFile first.
            String value = cldrFile.getKeyValueName(key, oldFormatType);
            if (value == null) {
                // if we fail, then we construct from the key name and the value
                String kname = cldrFile.getKeyName(key);
                if (kname == null) {
                    kname = key; // should not happen, but just in case
                }
                switch (key) {
                    case "t":
                        List<String> hybrid = lparser.getLocaleExtensionsDetailed().get("h0");
                        if (hybrid != null) {
                            kname = cldrFile.getKeyValueName("h0", JOIN_UNDERBAR.join(hybrid));
                        }
                        oldFormatType = getNameFromIdentifier(oldFormatType);
                        break;
                    case "cu":
                        oldFormatType =
                                getNameFromTypeCodeTransformPaths(
                                        NameType.CURRENCY_SYMBOL,
                                        oldFormatType.toUpperCase(Locale.ROOT),
                                        null,
                                        paths);
                        break;
                    case "tz":
                        if (paths != null) {
                            throw new IllegalArgumentException(
                                    "Error: getName(…) with paths doesn't handle timezones.");
                        }
                        oldFormatType =
                                getTZName(oldFormatType); // TODO: paths not handled here, yet
                        break;
                    case "kr":
                        oldFormatType = getReorderName(localeSeparator, keyValue, paths);
                        break;
                    case "rg":
                    case "sd":
                        oldFormatType =
                                getNameFromTypeCodeTransformPaths(
                                        NameType.SUBDIVISION, oldFormatType, null, paths);
                        break;
                    default:
                        oldFormatType = JOIN_HYPHEN.join(keyValue);
                }
                value = MessageFormat.format(localeKeyTypePattern, kname, oldFormatType);
                if (paths != null) {
                    paths.add(GETNAME_LOCALE_KEY_TYPE_PATTERN_PATH);
                }
            }
            value = replaceBracketsForName(value);
            if (paths != null && !extras.isEmpty()) {
                paths.add(GETNAME_LOCALE_SEPARATOR_PATH);
            }
            extras =
                    extras.isEmpty() ? value : MessageFormat.format(localeSeparator, extras, value);
        }
        // now handle stray extensions
        for (Map.Entry<String, List<String>> extension :
                lparser.getExtensionsDetailed().entrySet()) {
            String value =
                    MessageFormat.format(
                            localeKeyTypePattern,
                            extension.getKey(),
                            JOIN_HYPHEN.join(extension.getValue()));
            if (paths != null) {
                paths.add(GETNAME_LOCALE_KEY_TYPE_PATTERN_PATH);
            }
            extras =
                    extras.isEmpty() ? value : MessageFormat.format(localeSeparator, extras, value);
        }
        // fix this -- shouldn't be hardcoded!
        if (extras.isEmpty()) {
            return name;
        }
        if (paths != null) {
            paths.add(GETNAME_LOCALE_PATTERN_PATH);
        }
        return MessageFormat.format(localePattern, name, extras);
    }

    private static String replaceBracketsForName(String value) {
        return value.replace('(', '[').replace(')', ']').replace('（', '［').replace('）', '］');
    }

    /**
     * Utility for getting the name, given a code.
     *
     * @param nameType the NameType
     * @param code the code such as "JP"
     * @param codeToAlt - if not null, is called on the code. If the result is not null, then that
     *     is used for an alt value. If the alt path has a value it is used, otherwise the normal
     *     one is used. For example, the transform could return "short" for PS or HK or MO, but not
     *     US or GB.
     * @param paths if non-null, will have contributory paths on return
     * @return the name
     */
    private String getNameFromTypeCodeTransformPaths(
            NameType nameType,
            String code,
            Transform<String, String> codeToAlt,
            Set<String> paths) {
        String path = nameType.getKeyPath(code);
        String result = null;
        if (codeToAlt != null) {
            String alt = codeToAlt.transform(code);
            if (alt != null) {
                String altPath = path + "[@alt=\"" + alt + "\"]";
                result = cldrFile.getStringValueWithBaileyNotConstructed(altPath);
                if (paths != null && result != null) {
                    paths.add(altPath);
                }
            }
        }
        if (result == null) {
            result = cldrFile.getStringValueWithBaileyNotConstructed(path);
            if (paths != null && result != null) {
                paths.add(path);
            }
        }
        if (cldrFile.getLocaleID().equals("en")) {
            CLDRFile.Status status = new CLDRFile.Status();
            String sourceLocale = cldrFile.getSourceLocaleID(path, status);
            if (result == null || !sourceLocale.equals("en")) {
                if (nameType == NameType.LANGUAGE) {
                    Set<String> set = Iso639Data.getNames(code);
                    if (set != null) {
                        return set.iterator().next();
                    }
                    Map<String, Map<String, String>> map =
                            StandardCodes.getLStreg().get("language");
                    Map<String, String> info = map.get(code);
                    if (info != null) {
                        result = info.get("Description");
                    }
                } else if (nameType == NameType.TERRITORY) {
                    result = getLstrFallback("region", code);
                } else if (nameType == NameType.SCRIPT) {
                    result = getLstrFallback("script", code);
                }
            }
        }
        return result;
    }

    static final Pattern CLEAN_DESCRIPTION = Pattern.compile("([^(\\[]*)[(\\[].*");
    static final Splitter DESCRIPTION_SEP = Splitter.on('▪');

    private String getLstrFallback(String codeType, String code) {
        Map<String, String> info = StandardCodes.getLStreg().get(codeType).get(code);
        if (info != null) {
            String temp = info.get("Description");
            if (!temp.equalsIgnoreCase("Private use")) {
                List<String> temp2 = DESCRIPTION_SEP.splitToList(temp);
                temp = temp2.get(0);
                final Matcher matcher = CLEAN_DESCRIPTION.matcher(temp);
                if (matcher.lookingAt()) {
                    temp = matcher.group(1).trim();
                }
                return temp;
            }
        }
        return null;
    }

    /**
     * Gets timezone name. Not optimized.
     *
     * @param tzcode the code such as "gaza"
     * @return timezone name
     */
    private String getTZName(String tzcode) {
        String longid = CLDRFile.getLongTzid(tzcode);
        if (tzcode.length() == 4 && !tzcode.equals("gaza")) {
            return longid;
        }
        TimezoneFormatter tzf = new TimezoneFormatter(cldrFile);
        return tzf.getFormattedZone(longid, "VVVV", 0);
    }

    private String getReorderName(
            String localeSeparator, List<String> keyValues, Set<String> paths) {
        String result = null;
        for (String value : keyValues) {
            String name =
                    getNameFromTypeCodeTransformPaths(
                            NameType.SCRIPT,
                            Character.toUpperCase(value.charAt(0)) + value.substring(1),
                            null,
                            paths);
            if (name == null) {
                name = cldrFile.getKeyValueName("kr", value);
                if (name == null) {
                    name = value;
                }
            }
            result = result == null ? name : MessageFormat.format(localeSeparator, result, name);
        }
        return result;
    }

    /**
     * Adds the display name for a subtag to a string.
     *
     * @param subtag the subtag
     * @param type the type of the subtag
     * @param separatorPattern the pattern to be used for separating display names in the resultant
     *     string
     * @param extras the string to be added to
     * @return the modified display name string
     */
    private String addDisplayName(
            String subtag,
            NameType type,
            String separatorPattern,
            String extras,
            Transform<String, String> altPicker,
            Set<String> paths) {
        if (subtag.isEmpty()) {
            return extras;
        }
        String sname = getNameFromTypeCodeTransformPaths(type, subtag, altPicker, paths);
        if (sname == null) {
            sname = subtag;
        }
        sname = replaceBracketsForName(sname);

        if (extras.isEmpty()) {
            extras += sname;
        } else {
            extras = MessageFormat.format(separatorPattern, extras, sname);
        }
        return extras;
    }
}
