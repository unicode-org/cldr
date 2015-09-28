package org.unicode.cldr.web;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRLocale.CLDRFormatter;

import com.ibm.icu.text.RuleBasedCollator;

public class LocaleTree {
    CLDRFormatter displayLocale;

    public LocaleTree(CLDRLocale.CLDRFormatter nf) {
        this.displayLocale = nf;
    }

    public void add(CLDRLocale localeName) {
        localeNameToCode.put(getLocaleDisplayName(localeName), (localeName));
        addLocaleToListMap((localeName));
    }

    // TODO: object
    /**
     * TreeMap of all locales.
     *
     * localeListMap = TreeMap [ (String langScriptDisplayName) , (String
     * localecode) ] subLocales = Hashtable [ localecode, TreeMap ] --> TreeMap
     * [ langScriptDisplayName, String localeCode ] example
     *
     * localeListMap English -> en Serbian -> sr Serbian (Cyrillic) -> sr_Cyrl
     * sublocales en -> [ "English (US)" -> en_US ], [ "English (Australia)" ->
     * en_AU ] ... sr -> "Serbian (Yugoslavia)" -> sr_YU
     */

    Map<String, CLDRLocale> localeListMap = new TreeMap<String, CLDRLocale>(RuleBasedCollator.getInstance());
    Map<String, CLDRLocale> localeNameToCode = new HashMap<String, CLDRLocale>();
    Map<CLDRLocale, Map<String, CLDRLocale>> subLocales = new HashMap<CLDRLocale, Map<String, CLDRLocale>>();

    private void addLocaleToListMap(CLDRLocale localeName) {
        String l = localeName.getLanguage();
        if ((l != null) && (l.length() == 0)) {
            l = null;
        }
        String s = localeName.getScript();
        if ((s != null) && (s.length() == 0)) {
            s = null;
        }
        String t = localeName.getCountry();
        if ((t != null) && (t.length() == 0)) {
            t = null;
        }
        String v = localeName.getVariant();
        if ((v != null) && (v.length() == 0)) {
            v = null;
        }

        if (l == null) {
            return; // no language??
        }

        String ls = ((s == null) ? l : (l + "_" + s)); // language and script
        CLDRLocale lsl = CLDRLocale.getInstance(ls);

        localeListMap.put(getLocaleDisplayName(lsl), lsl);

        Map<String, CLDRLocale> lm = subLocales.get(lsl);
        if (lm == null) {
            lm = new TreeMap<String, CLDRLocale>();
            subLocales.put(lsl, lm);
        }

        if (t != null || v != null) {
            if (v == null) {
                lm.put(localeName.getDisplayCountry(displayLocale), localeName);
            } else if (t != null) {
                lm.put(localeName.getDisplayCountry(displayLocale) + " (" + localeName.getDisplayVariant(displayLocale) + ")",
                    localeName);
            } else {
                lm.put("(" + localeName.getDisplayVariant(displayLocale) + ")",
                    localeName);
            }
        }
    }

    public CLDRLocale getLocaleCode(String localeName) {
        CLDRLocale loc = localeNameToCode.get(localeName);
        if (loc == null) {
            System.err.println("Cannot find locale for name '" + localeName + "'");
            return CLDRLocale.ROOT;
        }
        return loc;
    }

    public String getLocaleDisplayName(CLDRLocale locale) {
        return displayLocale.getDisplayName(locale, true, null);
    }

    public Map<String, CLDRLocale> getMap() {
        return localeListMap;
    }

    /**
     * @return a list of the 'top' locales (language, or language_script)
     */
    public Set<String> getTopLocales() {
        return localeListMap.keySet();
    }

    public Collection<CLDRLocale> getTopCLDRLocales() {
        return localeListMap.values();
    }

    public Map<String, CLDRLocale> getSubLocales(CLDRLocale locale) {
        return subLocales.get(locale);
    }

}
