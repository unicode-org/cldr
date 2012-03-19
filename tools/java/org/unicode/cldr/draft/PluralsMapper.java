package org.unicode.cldr.draft;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;

/**
 * Class for converting CLDR plurals files to a format suitable for outputting
 * ICU data with. It might be possible for PluralsMapper and LdmlLocaleMapper to
 * share a parent class, but there isn't currently a need for that so they're
 * kept separate for the time being.
 * @author jchye
 */
public class PluralsMapper {
    private SupplementalDataInfo supplementalDataInfo;
    /**
     * Constructor. A SupplementalDataInfo object is used rather than the
     * supplemental directory because the supplemental data parsing is already
     * done for us. The RegexLookup method used by LdmlLocaleMapper wouldn't
     * work well, since there would only be one regex.
     * @param supplementalDataInfo
     */
    public PluralsMapper(SupplementalDataInfo supplementalDataInfo)  {
        this.supplementalDataInfo = supplementalDataInfo;
    }
    
    private static class LocaleInfo {
        String locale;
        PluralType type;

        public LocaleInfo(String locale, PluralType type) {
            this.locale = locale;
            this.type = type;
        }
    }
    
    /**
     * @return CLDR data converted to an ICU-friendly format
     */
    public IcuData fillFromCldr() {
        Map<String, List<LocaleInfo>> plurals = new LinkedHashMap<String, List<LocaleInfo>>();
        appendPluralInfo(PluralType.cardinal, plurals);
        appendPluralInfo(PluralType.ordinal, plurals);
        return getIcuData(plurals);
    }
    
    /**
     * Adds plural information of the specified type to a map.
     * @param type the type of plural information to be added
     * @param plurals the map to add the information to
     */
    private void appendPluralInfo(PluralType type, Map<String, List<LocaleInfo>> plurals) {
        for (String locale : supplementalDataInfo.getPluralLocales(type)) {
            if (locale.equals("root")) continue;
            String rules = supplementalDataInfo.getPlurals(type, locale).getRules();
            List<LocaleInfo> localeList = plurals.get(rules);
            if (localeList == null) {
                plurals.put(rules, localeList = new ArrayList<LocaleInfo>());
            }
            localeList.add(new LocaleInfo(locale, type));
        }
    }
    
    /**
     * Saves the specified map into an IcuData object.
     * @param plurals a map
     * @return the IcuData object
     */
    private IcuData getIcuData(Map<String, List<LocaleInfo>> plurals) {
        IcuData icuData = new IcuData("plurals.xml", "plurals", false);
        int setNum = 1;
        for (String rules : plurals.keySet()) {
            String setName = rules.length() == 0 ? "" : "set" + setNum;
            for (LocaleInfo info : plurals.get(rules)) {
                String prefix = info.type == PluralType.cardinal ? "/locales/" : "/locales_ordinals/";
                icuData.add(prefix + info.locale, setName);
            }
            if (rules.length() == 0) continue;
            String[] ruleArray = rules.split(";");
            for (String rule : ruleArray) {
                String[] parts = rule.split(":");
                icuData.add("/rules/set" + setNum + '/' + parts[0], parts[1]);
            }
            setNum++;
        }
        return icuData;
    }
}
