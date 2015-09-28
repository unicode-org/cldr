package org.unicode.cldr.util;

/**
 * Generate URLs to parts of CLDR and the SurveyTool.
 * You can change the urls used with for example,  -DCLDR_SURVEY_BASE=http://st.unicode.org/smoketest
 *
 * @author srl
 *
 */
public abstract class CLDRURLS {
    public static final String DEFAULT_HOST = "st.unicode.org";
    public static final String DEFAULT_PATH = "/cldr-apps";
    public static final String DEFAULT_BASE = "http://" + DEFAULT_HOST + DEFAULT_PATH;
    public static final String CLDR_NEWTICKET_URL = "http://unicode.org/cldr/trac/newticket";
    /**
     * Override this property if you want to change the absolute URL to the SurveyTool base from DEFAULT_BASE
     */
    public static final String CLDR_SURVEY_BASE = "CLDR_SURVEY_BASE";
    /**
     * Override this property if you want to change the relative URL to the SurveyTool base from DEFAULT_PATH (within SurveyTool only)
     */
    public static final String CLDR_SURVEY_PATH = "CLDR_SURVEY_PATH";

    /**
     *  "special" pages
     * @author srl
     *
     */
    public enum Special {
        /**
         * The 'main' view
         */
        Survey(""),
        /**
         * The list of locales
         */
        Locales,
        /**
         * The vetting viewer (i.e. Dashboard)
         */
        Vetting("r_vetting_json"),
        /**
         * Forums.  use "id" for the numeric post id
         */
        Forum;

        Special(String s) {
            this.id = s;
        }

        /**
         * Convenience - just lowercases
         */
        Special() {
            this.id = this.name().toLowerCase();
        }

        private final String id;
    }

    protected static String VPATH = "/v#";

    /**
     * Get the relative base URL for the SurveyTool.
     * This may be "/cldr-apps", for example.
     * @return example, "/cldr-apps"
     */
    public abstract String base();

    /**
     * please use CLDRLocale instead
     * @param locale
     * @param xpath
     * @return
     */
    public String forXpath(String locale, String xpath) {
        return forXpath(CLDRLocale.getInstance(locale), xpath);
    }

    /**
     * Get a link to a specific xpath and locale.
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
     * @param locale
     * @param hexid
     * @return
     */
    public final String forXpathHexId(String locale, String hexid) {
        return forXpathHexId(CLDRLocale.getInstance(locale), hexid);
    }

    /**
     * Get a link to a specific xpath hex ID and locale.
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
     * @param locale
     * @param hexid
     * @return
     */
    public final String forXpathHexId(String locale, PathHeader.PageId page, String hexid) {
        return forXpathHexId(CLDRLocale.getInstance(locale), page, hexid);
    }

    /**
     * Get a link to a specific xpath hex ID and locale.
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
     * @param locale
     * @return
     */
    public final String forLocale(CLDRLocale locale) {
        return forXpath(locale, null);
    }

    public final String forSpecial(Special special, CLDRLocale locale, PathHeader.PageId page, String hexid) {
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
    final public void assertIsHexId(String hexid) throws IllegalArgumentException {
        if (hexid != null && hexid.startsWith("/")) {
            throw new IllegalArgumentException("This function takes a hex StringID: perhaps you meant to use forXpath() instead.");
        }
    }

    /**
     * @param xpath
     * @throws IllegalArgumentException
     */
    final public void assertIsXpath(String xpath) throws IllegalArgumentException {
        if (xpath != null && !xpath.startsWith("/")) {
            throw new IllegalArgumentException("This function takes an XPath: perhaps you meant to use forXpathHexId() instead.");
        }
    }

    /**
     * please use CLDRLocale instead
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
     * @param locale
     * @param pathHeader
     * @return
     */
    public final String forPathHeader(CLDRLocale locale, PathHeader pathHeader) {
        return forSpecial(Special.Survey, locale, pathHeader.getPageId(), StringId.getHexId(pathHeader.getOriginalPath()));
    }
}
