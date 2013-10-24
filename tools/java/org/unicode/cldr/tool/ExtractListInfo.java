package org.unicode.cldr.tool;

import java.util.LinkedHashSet;
import java.util.Set;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;

public class ExtractListInfo {
    public static final String[] paths = {
        "//ldml/listPatterns/listPattern/listPatternPart[@type=\"2\"]",
        "//ldml/listPatterns/listPattern/listPatternPart[@type=\"start\"]",
        "//ldml/listPatterns/listPattern/listPatternPart[@type=\"middle\"]",
        "//ldml/listPatterns/listPattern/listPatternPart[@type=\"end\"]",
    };

    public static void main(String[] args) {
        Factory cldrFactory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
        Set<String> skipped = new LinkedHashSet<String>();
        for (String locale : cldrFactory.getAvailableLanguages()) {
            CLDRFile file = cldrFactory.make(locale, true);
            StringBuilder sb = new StringBuilder("ListFormat.add(\"" + locale + "\"");
            boolean gotOne = locale.equals("root");
            for (int i = 0; i < paths.length; ++i) {
                final String value = file.getStringValue(paths[i]);
                sb.append(", \"" + value + "\"");
                if (!value.equals("{0}, {1}")) {
                    gotOne = true;
                }
            }
            sb.append(");");
            if (gotOne) {
                System.out.println(sb);
            } else {
                skipped.add(locale);
            }
        }
        System.out.println("Skipped: " + skipped);
    }
}
