package org.unicode.cldr.util;

/**
 * Generate URLs to parts of CLDR and the SurveyTool. You can change the urls used with for example,
 * -DCLDR_SURVEY_BASE=http://st.unicode.org/smoketest
 *
 * @author srl
 */
public abstract class CLDRURLS {
    /** Base URL for the CLDR repository */
    public static final String CLDR_REPO_BASE = "https://github.com/unicode-org/cldr";

    public static final String DEFAULT_COMMIT_BASE = CLDR_REPO_BASE + "/commit/";
    /** Hostname for the Survey Tool */
    public static final String DEFAULT_HOST = "st.unicode.org";

    public static final String DEFAULT_PATH = "/cldr-apps";
    public static final String DEFAULT_BASE = "https://" + DEFAULT_HOST + DEFAULT_PATH;
    /** URL for filing a new ticket */
    public static final String CLDR_NEWTICKET_URL =
            "https://cldr.unicode.org/index/bug-reports#TOC-Filing-a-Ticket";

    public static final String CLDR_REPO_ROOT = "https://github.com/unicode-org/cldr";
    public static final String CLDR_HOMEPAGE = "https://cldr.unicode.org";
    public static final String UNICODE_CONSORTIUM = "The Unicode Consortium";
    public static final String CLDR_UPDATINGDTD_URL = CLDR_HOMEPAGE + "/development/updating-dtds";
    /** Our license, in SPDX format */
    public static final String UNICODE_SPDX = "Unicode-DFS-2016";
    /**
     * See:
     * https://spdx.github.io/spdx-spec/appendix-V-using-SPDX-short-identifiers-in-source-files/
     */
    public static final String UNICODE_SPDX_HEADER = "SPDX-License-Identifier: " + UNICODE_SPDX;
    /**
     * Override this property if you want to change the absolute URL to the SurveyTool base from
     * DEFAULT_BASE
     */
    public static final String CLDR_SURVEY_BASE = "CLDR_SURVEY_BASE";
    /**
     * Override this property if you want to change the relative URL to the SurveyTool base from
     * DEFAULT_PATH (within SurveyTool only)
     */
    public static final String CLDR_SURVEY_PATH = "CLDR_SURVEY_PATH";

    public static final String TOOLSURL = "http://cldr.unicode.org/tools/";

    /**
     * "special" pages
     *
     * @author srl
     */
    public enum Special {
        /** The 'main' view */
        Survey(""),
        /** The list of locales */
        Locales,
        /** The vetting viewer (i.e. Dashboard) */
        Vetting("dashboard"),
        /** Forums. use "id" for the numeric post id */
        Forum;

        Special(String s) {
            this.id = s;
        }

        /** Convenience - just lowercases */
        Special() {
            this.id = this.name().toLowerCase();
        }

        private final String id;
    }

    protected static String VPATH = "/v#";
    /** Constant for an unknown git revision. Use the same in the builders. */
    public static final String UNKNOWN_REVISION = "(unknown)";

    public static final String GENERAL_HELP_URL = "https://cldr.unicode.org/translation/";
    public static final String ADMIN_HELP_URL =
            "https://cldr.unicode.org/index/survey-tool/survey-tool-administration";
    public static final String CAPITALIZATION_URL =
            "https://cldr.unicode.org/translation/translation-guide-general/capitalization";
    public static final String CHARACTERS_HELP = "https://cldr.unicode.org/translation/characters";
    public static final String CHARACTER_LABELS =
            "https://cldr.unicode.org/translation/characters/character-labels";
    public static final String CHARTS_URL = "https://cldr.unicode.org/index/charts#TOC-Summary";
    public static final String COMPOUND_UNITS_HELP =
            "https://cldr.unicode.org/translation/units/unit-names-and-patterns#h.7kdholgw8hpx";
    public static final String COUNTRY_NAMES =
            "https://cldr.unicode.org/translation/displaynames/countryregion-territory-names";
    public static final String CURRENCY_NAMES =
            "https://cldr.unicode.org/translation/currency-names-and-symbols";
    public static final String DATE_TIME_HELP =
            "https://cldr.unicode.org/translation/date-time/date-time-names#h.ewzjebmpoi4k";
    public static final String DATE_TIME_NAMES =
            "https://cldr.unicode.org/translation/date-time/datetime-names";
    public static final String DATE_TIME_NAMES_CYCLIC =
            "https://cldr.unicode.org/translation/date-time/date-time-names#h.h0vy2eyzcj0n";
    public static final String DATE_TIME_NAMES_FIELD =
            "https://cldr.unicode.org/translation/date-time/date-time-names#h.8wfk3599ck9x";
    public static final String DATE_TIME_NAMES_MONTH =
            "https://cldr.unicode.org/translation/date-time/date-time-names#h.fww3pfyk0uwn";
    public static final String DATE_TIME_NAMES_RELATIVE =
            "https://cldr.unicode.org/translation/date-time/date-time-names#h.aevw0tiix80v";
    public static final String DATE_TIME_PATTERNS =
            "https://cldr.unicode.org/translation/date-time/date-time-patterns";
    public static final String DATE_TIME_PATTERNS_URL =
            "https://cldr.unicode.org/translation/date-time/date-time-patterns";
    public static final String ERRORS_URL =
            "https://cldr.unicode.org/translation/error-and-warning-codes";
    public static final String EXEMPLAR_CHARACTERS =
            "https://cldr.unicode.org/translation/core-data/exemplars";
    public static final String GRAMMATICAL_INFLECTION =
            "https://cldr.unicode.org/translation/grammatical-inflection";
    public static final String KEY_NAMES =
            "https://cldr.unicode.org/translation/displaynames/countryregion-territory-names#h.x27jspwj91af";
    public static final String LANGUAGE_NAMES =
            "https://cldr.unicode.org/translation/displaynames/languagelocale-names";
    public static final String LISTS_HELP =
            "https://cldr.unicode.org/translation/miscellaneous-displaying-lists";
    public static final String LOCALE_PATTERN =
            "https://cldr.unicode.org/translation/displaynames/languagelocale-name-patterns";
    public static final String NUMBERING_SYSTEMS =
            "https://cldr.unicode.org/translation/core-data/numbering-systems";
    public static final String NUMBERS_HELP =
            "https://cldr.unicode.org/translation/currency-names-and-symbols";
    public static final String NUMBERS_PLURAL =
            "https://cldr.unicode.org/translation/number-currency-formats/number-and-currency-patterns#h.mnb2fmj0pt72";
    public static final String NUMBERS_SHORT =
            "https://cldr.unicode.org/translation/number-currency-formats/number-and-currency-patterns#h.eradhhuxzqqz";
    public static final String NUMBER_PATTERNS =
            "https://cldr.unicode.org/translation/number-currency-formats/number-and-currency-patterns#h.j899g3kk2p1z";
    public static final String PARSE_LENIENT =
            "https://cldr.unicode.org/translation/core-data/characters#h.j3x0cwalqgqt";
    public static final String PERSON_NAME_FORMATS =
            "https://cldr.unicode.org/translation/miscellaneous-person-name-formats";
    public static final String PLURALS_HELP =
            "https://cldr.unicode.org/translation/getting-started/plurals";
    public static final String PLURALS_HELP_MINIMAL =
            "https://cldr.unicode.org/translation/getting-started/plurals#h.pnla5cp3nl4l";
    public static final String SCRIPT_NAMES =
            "https://cldr.unicode.org/translation/displaynames/script-names";
    public static final String SHORT_CHARACTER_NAMES =
            "https://cldr.unicode.org/translation/characters/short-names-and-keywords#h.4a8wthj27m74";
    public static final String TRANSFORMS_HELP = "https://cldr.unicode.org/translation/transforms";
    public static final String TYPOGRAPHIC_NAMES =
            "https://cldr.unicode.org/translation/characters/typographic-names";
    public static final String TZ_CITY_NAMES =
            "https://cldr.unicode.org/translation/time-zones-and-city-names";
    public static final String UNITS_HELP = "https://cldr.unicode.org/translation/units";
    /*
     * TODO: UNITS_MISC_HELP, formerly "https://cldr.unicode.org/translation/units-1/misc",
     * now temporarily (?) the same as UNITS_HELP until a distinct URL is identified
     * Reference: https://unicode-org.atlassian.net/browse/CLDR-15080
     */
    public static final String UNITS_MISC_HELP = UNITS_HELP;

    public static final String PRIORITY_SUMMARY_HELP_URL =
            "https://cldr.unicode.org/translation/getting-started/vetting-view#TOC-Priority-Items";

    public static final String UPDATING_CURRENCY_CODES =
            "https://cldr.unicode.org/development/updating-codes/update-currency-codes";

    public static final String CLDR_ARCHIVE =
            "https://cldr.unicode.org/development/creating-the-archive";

    /**
     * Get the relative base URL for the SurveyTool. This may be "/cldr-apps", for example.
     *
     * @return example, "/cldr-apps"
     */
    public abstract String base();

    /**
     * please use CLDRLocale instead
     *
     * @param locale
     * @param xpath
     * @return
     */
    public String forXpath(String locale, String xpath) {
        return forXpath(CLDRLocale.getInstance(locale), xpath);
    }

    /**
     * Get a link to a specific xpath and locale.
     *
     * @param locale locale to view
     * @param xpath the xpath to view
     */
    public final String forXpath(CLDRLocale locale, String xpath) {
        assertIsXpath(xpath);
        final String hexid = (xpath == null) ? null : StringId.getHexId(xpath);
        return forXpathHexId(locale, hexid);
    }

    /**
     * please use CLDRLocale instead
     *
     * @param locale
     * @param hexid
     * @return
     */
    public final String forXpathHexId(String locale, String hexid) {
        return forXpathHexId(CLDRLocale.getInstance(locale), hexid);
    }

    /**
     * Get a link to a specific xpath hex ID and locale.
     *
     * @param locale
     * @param hexid
     * @return
     */
    public final String forXpathHexId(CLDRLocale locale, String hexid) {
        assertIsHexId(hexid);
        return forSpecial(Special.Survey, locale, (String) null, hexid);
    }

    /**
     * please use CLDRLocale instead
     *
     * @param locale
     * @param hexid
     * @return
     */
    public final String forXpathHexId(String locale, PathHeader.PageId page, String hexid) {
        return forXpathHexId(CLDRLocale.getInstance(locale), page, hexid);
    }

    /**
     * Get a link to a specific xpath hex ID and locale.
     *
     * @param locale
     * @param hexid
     * @return
     */
    public final String forXpathHexId(CLDRLocale locale, PathHeader.PageId page, String hexid) {
        assertIsHexId(hexid);
        return forSpecial(Special.Survey, locale, page, hexid);
    }

    /**
     * please use CLDRLocale instead
     *
     * @param locale
     * @param page
     * @return
     */
    public final String forPage(String locale, PathHeader.PageId page) {
        return forPage(CLDRLocale.getInstance(locale), page);
    }

    public final String forPage(CLDRLocale locale, PathHeader.PageId page) {
        return forSpecial(Special.Survey, locale, page.name(), null);
    }

    /**
     * Get a link to a specific locale in the SurveyTool.
     *
     * @param locale
     * @return
     */
    public final String forLocale(CLDRLocale locale) {
        return forXpath(locale, null);
    }

    public final String forSpecial(
            Special special, CLDRLocale locale, PathHeader.PageId page, String hexid) {
        return forSpecial(special, locale, page.name(), hexid);
    }

    public final String forSpecial(Special special) {
        return forSpecial(special, (CLDRLocale) null, (String) null, null);
    }

    public final String forSpecial(Special special, CLDRLocale locale) {
        return forSpecial(special, locale, (String) null, null);
    }

    /**
     * Get a link from all of the parts.
     *
     * @param special
     * @param locale
     * @param page
     * @param xpath
     * @return
     */
    public String forSpecial(Special special, CLDRLocale locale, String page, String hexid) {
        StringBuilder sb = new StringBuilder(base());
        sb.append(VPATH);
        if (special != null) {
            sb.append(special.id);
        }
        sb.append('/');
        if (locale != null) {
            sb.append(locale.getBaseName());
        }
        sb.append('/');
        if (page != null) {
            sb.append(page);
        }
        sb.append('/');
        if (hexid != null) {
            sb.append(hexid);
        }
        return sb.toString();
    }

    /**
     * @param hexid
     * @throws IllegalArgumentException
     */
    public final void assertIsHexId(String hexid) throws IllegalArgumentException {
        if (hexid != null && hexid.startsWith("/")) {
            throw new IllegalArgumentException(
                    "This function takes a hex StringID: perhaps you meant to use forXpath() instead.");
        }
    }

    /**
     * @param xpath
     * @throws IllegalArgumentException
     */
    public final void assertIsXpath(String xpath) throws IllegalArgumentException {
        if (xpath != null && !xpath.startsWith("/")) {
            throw new IllegalArgumentException(
                    "This function takes an XPath: perhaps you meant to use forXpathHexId() instead.");
        }
    }

    /**
     * please use CLDRLocale instead
     *
     * @param vetting
     * @param localeID
     * @return
     */
    public final String forSpecial(Special special, String localeID) {
        return forSpecial(special, CLDRLocale.getInstance(localeID));
    }

    public final String forPathHeader(String locale, PathHeader pathHeader) {
        return forPathHeader(CLDRLocale.getInstance(locale), pathHeader);
    }

    /**
     * This is the preferred function for jumping to an item relatively. It will reduce blinkage.
     *
     * @param locale
     * @param pathHeader
     * @return
     */
    public final String forPathHeader(CLDRLocale locale, PathHeader pathHeader) {
        return forSpecial(
                Special.Survey,
                locale,
                pathHeader.getPageId(),
                StringId.getHexId(pathHeader.getOriginalPath()));
    }

    /**
     * For a given hash, return as a link
     *
     * @param hash
     * @return
     */
    public static String gitHashToLink(String hash) {
        if (!isKnownHash(hash))
            return "<span class=\"githashLink\">" + hash + "</span>"; // Not linkifiable
        return "<a class=\"githashLink\" href=\""
                + CldrUtility.getProperty("CLDR_COMMIT_BASE", DEFAULT_COMMIT_BASE)
                + hash
                + "\">"
                + hash.substring(0, 8)
                + "</a>";
    }

    /**
     * Is this a 'known' git hash? Or unknown?
     *
     * @param hash
     * @return true if known, false if (unknown)
     */
    public static boolean isKnownHash(String hash) {
        return !hash.equals(UNKNOWN_REVISION);
    }

    /**
     * Convert a URL into an HTML link to itself
     *
     * @param url
     * @return
     */
    public static final String toHTML(String url) {
        return "<a href=\"" + url + "\">" + url + "</a>";
    }

    /**
     * Provide the styles for inclusion into the ST &lt;head&gt; element.
     *
     * @return
     */
    public static String getVettingViewerHeaderStyles() {
        return "<style>\n"
                + ".hide {display:none}\n"
                + ".vve {}\n"
                + ".vvn {}\n"
                + ".vvp {}\n"
                + ".vvl {}\n"
                + ".vvm {}\n"
                + ".vvu {}\n"
                + ".vvw {}\n"
                + ".vvd {}\n"
                + ".vvo {}\n"
                + "</style>";
    }
}
