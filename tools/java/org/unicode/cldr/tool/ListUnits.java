package org.unicode.cldr.tool;

import java.util.HashSet;
import java.util.Set;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.XPathParts;

public class ListUnits {
public static void main(String[] args) {
    Factory cldrFactory = CLDRConfig.getInstance().getCldrFactory();
    XPathParts parts = new XPathParts();

    Set<String> seen = new HashSet<String>();
    for (String locale : cldrFactory.getAvailable()) {
        CLDRFile cldrFile = cldrFactory.make(locale, false);
        for (String path : cldrFile){
            if (!path.contains("/unit")) {
                continue;
            }
            parts.set(path);
            String unit = parts.findAttributeValue("unit", "type");
            if (unit == null) {
                continue;
            }
            if (!seen.contains(unit)) {
                seen.add(unit);
                System.out.println(unit.replaceFirst("-", "\t"));
            }
        }
    }
}
}
