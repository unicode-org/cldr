package org.unicode.cldr.tool;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;

import com.ibm.icu.impl.Relation;
import com.ibm.icu.text.DateTimePatternGenerator;

public class CompareStockDatetime {
    public static void main(String[] args) {
        CLDRConfig info = ToolConfig.getToolInstance();
        Factory cldrFactory = info.getCldrFactory();
        String[][] data = {
            { "date", "full" },
            { "date", "long" },
            { "date", "medium" },
            { "date", "short" },
            { "time", "full" },
            { "time", "long" },
            { "time", "medium" },
            { "time", "short" },
        };

        Map<String, Relation<String, String>> lengthToSkeletonToLocales = new TreeMap<String, Relation<String, String>>();
        // new Relation(new TreeMap(), TreeSet.class);
        Set<String> defaultContentLocales = info.getSupplementalDataInfo().getDefaultContentLocales();

        DateTimePatternGenerator dtpg = DateTimePatternGenerator.getEmptyInstance();
        for (String locale : cldrFactory.getAvailableLanguages()) {
            if (defaultContentLocales.contains(locale)) {
                continue;
            }
            System.out.println(locale);
            CLDRFile cldrFile = cldrFactory.make(locale, true);
            for (String[] row : data) {
                String type = row[0];
                String length = row[1];
                String xpath = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/" +
                    type + "Formats/" +
                    type + "FormatLength[@type=\"" + length + "\"]/" +
                    type + "Format[@type=\"standard\"]/pattern[@type=\"standard\"]";
                String value = cldrFile.getStringValue(xpath);
                String skeleton = dtpg.getSkeleton(value);
                skeleton = skeleton.replace("yyyy", "y");
                String key = type + "-" + length;
                Relation<String, String> skeletonToLocales = lengthToSkeletonToLocales.get(key);
                if (skeletonToLocales == null) {
                    lengthToSkeletonToLocales.put(key, skeletonToLocales = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class));
                }
                skeletonToLocales.put(skeleton, locale);
                // System.out.println(key + "\t" + skeleton + "\t" + locale);
            }
        }
        System.out.println();
        for (Entry<String, Relation<String, String>> entry : lengthToSkeletonToLocales.entrySet()) {
            for (Entry<String, Set<String>> entry2 : entry.getValue().keyValuesSet()) {
                System.out.println(entry.getKey() + "\t" + entry2.getKey() + "\t" + entry2.getValue());
            }
        }
    }
}
