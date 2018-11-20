package org.unicode.cldr.tool;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.ChainedMap.M3;
import org.unicode.cldr.util.ChainedMap.M4;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.TreeMultimap;

public class ListCoverageLevels {
    public static void main(String[] args) {
        CLDRConfig config = CLDRConfig.getInstance();
        StandardCodes sc = config.getStandardCodes();
        SupplementalDataInfo sdi = config.getSupplementalDataInfo();
        LanguageTagParser ltp = new LanguageTagParser();
        Set<String> defaultContents = sdi.getDefaultContentLocales();
        PathStarrer starrer = new PathStarrer();
        Factory mainAndAnnotationsFactory = config.getMainAndAnnotationsFactory();
        Set<String> toTest = ImmutableSet.of("it", "root"); // mainAndAnnotationsFactory.getAvailable();
        
        M4<Level, String, String, Boolean> data = ChainedMap.of(new TreeMap<Level,Object>(), new TreeMap<String,Object>(), new TreeMap<String,Object>(), Boolean.class);
        TreeMultimap<String, Level> starredToLevels = TreeMultimap.create();
        for (String locale : toTest) {
            if (!ltp.set(locale).getRegion().isEmpty() || locale.equals("root")
                || defaultContents.contains(locale)) {
                continue;
            }
            CoverageLevel2 coverageLeveler = CoverageLevel2.getInstance(locale);
            //Level desiredLevel = sc.getLocaleCoverageLevel(Organization.cldr, locale);
            CLDRFile testFile = mainAndAnnotationsFactory.make(locale, false);
            for (String path : testFile.fullIterable()) {
                Level level = coverageLeveler.getLevel(path);
                String starred = starrer.set(path);
                String attributes = starrer.getAttributesString("|");
                data.put(level, starred, attributes, Boolean.TRUE);
                starredToLevels.put(starred, level);
            }
        }
        for (Entry<String, Collection<Level>> entry : starredToLevels.asMap().entrySet()) {
            System.out.println(entry.getKey() + "\t" + entry.getValue().size() + "\t" + entry.getValue());
        }
        for (Level level : data.keySet()) {
            M3<String, String, Boolean> data2 = data.get(level);
            for (String starred : data2.keySet()) {
                Set<String> attributes = data2.get(starred).keySet();
                System.out.println(level + "\t" + starred + "\t" + attributes.size() + "\t" + attributes);
            }
        }
    }
}