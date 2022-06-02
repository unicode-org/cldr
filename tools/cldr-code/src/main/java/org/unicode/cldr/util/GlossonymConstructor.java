package org.unicode.cldr.util;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.Transform;
import com.ibm.icu.util.Output;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Automatically construct language names (glossonyms)
 *
 * Example: in German (de), for the path
 *
 *       //ldml/localeDisplayNames/languages/language[@type="ro_MD"]
 *
 * the value "Rumänisch (Republik Moldau)" is automatically constructed based on the code "ro_MD".
 *
 * The constructed value is a default if no preferable value is submitted or inherited.
 * A different (non-constructed) value, such as "Moldauisch", may become the winning
 * value instead of the constructed value.
 */
public class GlossonymConstructor {

    /**
     * Some paths with this prefix can get automatically constructed values
     */
    public static final String PATH_PREFIX = "//ldml/localeDisplayNames/languages/language[@type=\"";

    /**
     * The code such as "ro_MD" must contain an underscore, otherwise there is no constructed value.
     * Underscore also serves as a clue for recognizing a value that is from code-fallback; for example,
     * when the value "ro_MD" is inherited, the underscore implies it's a raw code ("bogus value") and should
     * be replaced by a constructed value like "Rumänisch (Republik Moldau)"; but when the value "Moldauisch"
     * (without underscore) is inherited, it should not be replaced by a constructed value
     */
    private static final String CODE_SEPARATOR = "_";

    /**
     * For "pathWhereFound" when the value is constructed.
     * It is non-null to satisfy TestPathHeadersAndValues.
     * It should not be treated as an actual path; for example, the
     * Survey Tool Info Panel should not show a broken "Jump to original" link.
     */
    public static final String PSEUDO_PATH = "constructed";

    /**
     * Is the given path eligible for getting a constructed value?
     *
     * @param xpath the given path
     * @return true if eligible
     */
    public static boolean pathIsEligible(String xpath) {
        return xpath.startsWith(PATH_PREFIX) && xpath.contains(CODE_SEPARATOR);
    }

    /**
     * Is the given value bogus, and therefore eligible for getting replaced by a constructed value?
     *
     * @param value the given value
     * @return true if bogus
     */
    public static boolean valueIsBogus(String value) {
        return (
            value == null ||
            value.contains(CODE_SEPARATOR) ||
            RegexUtilities.PATTERN_3_OR_4_DIGITS.matcher(value).find()
        );
    }

    private final CLDRFile cldrFile;

    public GlossonymConstructor(CLDRFile cldrFile) {
        this.cldrFile = cldrFile;
        if (!cldrFile.isResolved()) {
            throw new IllegalArgumentException("Unresolved CLDRFile in GlossonymConstructor constructor");
        }
    }

    /**
     * Get the constructed value and fill in tracking information about where it was found
     *
     * @param xpath the path
     * @param pathWhereFound if not null, to be filled in
     * @param localeWhereFound if not null, to be filled in
     * @return the constructed value, or null
     */
    public String getValueAndTrack(String xpath, Output<String> pathWhereFound, Output<String> localeWhereFound) {
        final String constructedValue = getValue(xpath);
        if (constructedValue != null) {
            if (localeWhereFound != null) {
                localeWhereFound.value = cldrFile.getLocaleID();
            }
            if (pathWhereFound != null) {
                pathWhereFound.value = PSEUDO_PATH;
            }
            return constructedValue;
        }
        return null;
    }

    /**
     * Get the constructed value for the given path
     *
     * @param xpath the given path
     * @return the constructed value, or null
     */
    public String getValue(String xpath) {
        if (pathIsEligible(xpath)) {
            return reallyGetValue(xpath);
        }
        return null;
    }

    private synchronized String reallyGetValue(String xpath) {
        final XPathParts parts = XPathParts.getFrozenInstance(xpath);
        final String type = parts.getAttributeValue(-1, "type");
        if (type.contains(CODE_SEPARATOR)) {
            final String alt = parts.getAttributeValue(-1, "alt");
            final CLDRFile.SimpleAltPicker altPicker = (alt == null) ? null : new CLDRFile.SimpleAltPicker(alt);
            final String value = cldrFile.getName(type, true, altPicker);
            if (!valueIsBogus(value)) {
                return value;
            }
        }
        return null;
    }
}
