package org.unicode.cldr.tool;

import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.Annotations;
import org.unicode.cldr.util.Annotations.AnnotationSet;
import org.unicode.cldr.util.CLDRConfig;

public class CheckAnnotations {
    public static void main(String[] args) {
        AnnotationSet data = Annotations.getDataSet("en");
        CLDRConfig config = CLDRConfig.getInstance();
        //UnicodeMap<Annotations> data2 = Annotations.getData("de");
        Set<String> sorted = new TreeSet<String>(config.getCollator());

        int i = 0;
        boolean needMore = true;
        for (int spaceCount = 1; needMore; ++spaceCount) {
            needMore = false;
            System.out.println("\nCOUNT: " + spaceCount);
            for (String key : data.keySet().addAllTo(sorted)) {
                final String shortName = data.getShortName(key);
                String spaces = shortName.replaceAll("\\S", "");
                if (spaces.length() != spaceCount) {
                    if (spaces.length() > spaceCount) {
                        needMore = true;
                    }
                    continue;
                }
                System.out.println(++i + "\t" + key + "\t" + shortName + "\t" + data.getKeywords(key));
            }
        }
    }
}
