package org.unicode.cldr.util;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import java.util.Map;
import java.util.TreeMap;

public enum RbnfData {
    INSTANCE;

    private final Map<String, Multimap<String, String>> localeToTypesToSubtypes;
    private final Multimap<String, String> rbnfTypeToLocales;

    {
        Map<String, Multimap<String, String>> _localeToRbnfType = new TreeMap<>();
        Multimap<String, String> _rbnfTypeToLocales = TreeMultimap.create();
        Factory factory = CLDRConfig.getInstance().getRBNFFactory();
        for (String locale : factory.getAvailable()) {
            CLDRFile cldrFile = factory.make(locale, false);
            Multimap<String, String> typeToSubtype = _localeToRbnfType.get(locale);
            if (typeToSubtype == null) {
                _localeToRbnfType.put(locale, typeToSubtype = TreeMultimap.create());
            }
            for (String dpath : cldrFile) {
                String path = cldrFile.getFullXPath(dpath);
                XPathParts parts = XPathParts.getFrozenInstance(path);
                if (!"rbnf".equals(parts.getElement(1))
                        || !"ruleset".equals(parts.getElement(3))
                        || "private".equals(parts.getAttributeValue(3, "access"))) {
                    continue;
                }
                String fullType = parts.getAttributeValue(3, "type");
                String rbnfType;
                String rbnfSubtype;
                if (fullType.startsWith("spellout") || fullType.startsWith("digits")) {
                    int index2 = fullType.indexOf('-', fullType.indexOf('-') + 1);
                    if (index2 == -1) {
                        rbnfType = fullType;
                        rbnfSubtype = "DEFAULT";
                    } else {
                        rbnfType = fullType.substring(0, index2);
                        rbnfSubtype = fullType.substring(index2 + 1);
                    }
                } else {
                    rbnfType = "UNKNOWN";
                    rbnfSubtype = fullType;
                }
                typeToSubtype.put(rbnfType, rbnfSubtype);
                _rbnfTypeToLocales.put(rbnfType, locale);
            }
        }
        this.localeToTypesToSubtypes = CldrUtility.protectCollection(_localeToRbnfType);
        this.rbnfTypeToLocales = CldrUtility.protectCollection(_rbnfTypeToLocales);
    }

    public Multimap<String, String> getRbnfTypeToLocales() {
        return rbnfTypeToLocales;
    }

    public Map<String, Multimap<String, String>> getLocaleToTypesToSubtypes() {
        return localeToTypesToSubtypes;
    }

    public String getPath(String rbnfType) {
        return "//ldml/rbnf/rulesetGrouping[@type=\"SpelloutRules\"]/ruleset[@type=\""
                + rbnfType
                + "\"]";
    }
}
