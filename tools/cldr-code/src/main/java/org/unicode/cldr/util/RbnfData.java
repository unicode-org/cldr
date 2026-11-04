package org.unicode.cldr.util;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import java.util.TreeMap;
import org.unicode.cldr.util.NestedMap.ImmutableMultimap2;
import org.unicode.cldr.util.NestedMap.Multimap2;

public enum RbnfData {
    INSTANCE;

    private final ImmutableMultimap2<String, String, String> localeToTypesToSubtypes;
    private final Multimap<String, String> rbnfTypeToLocales;

    {
        Multimap2<String, String, String> _localeToRbnfType = Multimap2.create(TreeMap::new);
        Multimap<String, String> _rbnfTypeToLocales = TreeMultimap.create();
        Factory factory = CLDRConfig.getInstance().getRBNFFactory();
        for (String locale : factory.getAvailable()) {
            CLDRFile cldrFile = factory.make(locale, false);
            for (String dpath : cldrFile) {
                String path = cldrFile.getFullXPath(dpath);
                XPathParts parts = XPathParts.getFrozenInstance(path);

                /* new format is:
                 *         <rulesetGrouping type="OrdinalRules">
                 * <rbnfRules><![CDATA[
                 * %digits-ordinal:
                 */

                if (!"rbnfRules".equals(parts.getElement(-1))) {
                    continue;
                }
                for (String line : Splitters.EOL.splitToList(cldrFile.getStringValue(path))) {
                    line = line.trim();
                    if (!line.startsWith("%") //  
                            || line.startsWith("%%") //  
                            || !line.endsWith(":")) {
                        continue;
                    }
                    String fullType = line.substring(1, line.length() - 1);
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
                    _localeToRbnfType.put(locale, rbnfType, rbnfSubtype);
                    _rbnfTypeToLocales.put(rbnfType, locale);
                }
            }
        }
        this.localeToTypesToSubtypes = _localeToRbnfType.createImmutable();
        this.rbnfTypeToLocales = CldrUtility.protectCollection(_rbnfTypeToLocales);
    }

    public Multimap<String, String> getRbnfTypeToLocales() {
        return rbnfTypeToLocales;
    }

    public ImmutableMultimap2<String, String, String> getLocaleToTypesToSubtypes() {
        return localeToTypesToSubtypes;
    }

    public String getPath(String rbnfType) {
        return "//ldml/rbnf/rulesetGrouping[@type=\"SpelloutRules\"]/ruleset[@type=\""
                + rbnfType
                + "\"]";
    }
}
