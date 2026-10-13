package org.unicode.cldr.tool;


import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.XPathParts;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class FindRegionsInEmoji {

    static final Set<String> SKIP_EMOJI = ImmutableSet.of("🌍", "🌎", "🌏", "🗾", "🌏");
    static final Set<String> SKIP_REGION = ImmutableSet.of("002");

    public static void main(String[] args) {
        CLDRConfig cldrConfig = CLDRConfig.getInstance();
        Factory mainFactory = cldrConfig.getCldrFactory();
        Factory annotationsFactory = cldrConfig.getAnnotationsFactory();
        for (String locale : annotationsFactory.getAvailable()) {
            if (locale.equals("und")) {
                continue;
            }
            Map<String, String> nameToRegion;
            try {
                nameToRegion = getCountryNames(mainFactory, locale);
            } catch (Exception e) {
                System.out.println(locale + "\tCan't load");
                continue;
            }

            CLDRFile annotationsFile = annotationsFactory.make(locale, false); // don't care about inherited
            for (String path : annotationsFile) {
                XPathParts parts = XPathParts.getFrozenInstance(path);
                String emoji = parts.getAttributeValue(-1, "cp");
                if (SKIP_EMOJI.contains(emoji)) {
                    continue;
                }
                String annotation = annotationsFile.getStringValue(path);
                for (Entry<String, String> entry : nameToRegion.entrySet()) {
                    String nativeName = entry.getKey();
                    String code = entry.getValue();
                    if (annotation.contains(nativeName)) {
                        String englishName = cldrConfig.getEnglish().getName("region", code);
                        System.out.println(locale + "\t" + emoji + "\t" + code  + "\t" + englishName + "\t" + nativeName + "\t" + annotation);
                    }
                }
            }
        }
    }

    public static Map<String, String> getCountryNames(Factory mainFactory, String locale) {
        CLDRFile mainFile = mainFactory.make(locale, true); // resolved

        Map<String,String> nameToRegion = new TreeMap<>();
        for (Iterator<String> it = mainFile.getAvailableIterator(CLDRFile.TERRITORY_NAME); it.hasNext(); ) {
            String path = it.next();
            XPathParts parts = XPathParts.getFrozenInstance(path);
            String regionCode = parts.getAttributeValue(-1, "type");
            if (regionCode.length() > 2 || SKIP_REGION.contains(regionCode)) { // skip macro regions, others
                continue;
            }
            String regionName = mainFile.getStringValue(path);
            if (regionCode.equals(regionName)) {
                continue; // untranslated
            }
            nameToRegion.put(regionName, regionCode);
        }
        return ImmutableMap.copyOf(nameToRegion);
    }

}
