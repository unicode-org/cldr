package org.unicode.cldr.util;

public class LocaleNames {

    /**
     * "Uncoded ... This subtag SHOULD NOT be used" per
     * https://datatracker.ietf.org/doc/html/rfc5646
     *
     * <p>"originally an abbreviation for 'miscellaneous'" per
     * https://en.wikipedia.org/wiki/ISO_639-3
     */
    public static final String MIS = "mis";

    /** "Multiple" per https://datatracker.ietf.org/doc/html/rfc5646 */
    public static final String MUL = "mul";

    /**
     * Per https://www.unicode.org/reports/tr35/ in a Unicode CLDR locale identifier (as contrasted
     * with a Unicode BCP 47 locale identifier), "und" is replaced by "root"
     */
    public static final String ROOT = "root";

    /** "Undetermined" per https://datatracker.ietf.org/doc/html/rfc5646 */
    public static final String UND = "und";

    /** Serves as a placeholder in some documentation and is used in some tests */
    public static final String XX_TEST = "xx";

    /** "Non-Linguistic, Not Applicable" per https://datatracker.ietf.org/doc/html/rfc5646 */
    public static final String ZXX = "zxx";
}
