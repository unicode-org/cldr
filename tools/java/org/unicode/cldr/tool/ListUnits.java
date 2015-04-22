package org.unicode.cldr.tool;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.XPathParts;

public class ListUnits {
    enum Type {
        root, 
        en, 
        other;
        
        static Type fromString(String type) {
            return type.equals("en") ? en : type.equals("root") ? root : other;
        }
    }
    
    public static void main(String[] args) {
        Factory cldrFactory = CLDRConfig.getInstance().getCldrFactory();
        Set<String> seen = new HashSet<>();

        LinkedHashSet<String> items = new LinkedHashSet<>();
        items.add("root");
        items.add("en");
        items.addAll(cldrFactory.getAvailableLanguages());
        Map<String,Data> rootMap = new HashMap<>();
        Map<String,Data> enMap = new HashMap<>();
        
        for (String locale : items) {
            Type type = Type.fromString(locale);
            CLDRFile cldrFile = cldrFactory.make(locale, false);
            Set<String> units = getUnits(cldrFile, type == Type.root ? rootMap : type == Type.en ? enMap : null);
            if (type == Type.en) {
                TreeSet<String> missing = new TreeSet<>(seen);
                missing.removeAll(units);
                for (String unit : missing) {
                    // locale=en ; action=add ; new_path=//ldml/localeDisplayNames/territories/territory[@type="PS"][@alt="short"] ; new_value=Palestine
                    Data data = rootMap.get(unit);
                    if (data != null) {
                        System.out.println(data);
                    }
                }
            }
            for (String unit : units) {
                if (!seen.contains(unit)) {
                    System.out.println("\t" + unit.replace("/", "\t")
                        .replaceFirst("-", "\t") + "\t" + locale);
                    seen.add(unit);
                }
            }
        }

    }

    static final class Data {
        public Data(String path2, String stringValue) {
            path = path2;
            value = stringValue;
        }
        final String path;
        final String value;
        public String toString() {
            return "locale=en"
            + " ; action=add"
            + " ; new_path=" + path 
            + " ; new_value=" + value;
        }
    }
    
    private static Set<String> getUnits(CLDRFile cldrFile, Map<String,Data> extra) {
        Set<String> seen = new TreeSet<String>();
        for (String path : cldrFile){
            if (!path.contains("/unit")) {
                continue;
            }
            XPathParts parts = XPathParts.getFrozenInstance(path);
            String unit = parts.findAttributeValue("unit", "type");
            if (unit == null) {
                continue;
            }
            String length = parts.findAttributeValue("unitLength", "type");
            String per = "perUnitPattern".equals(parts.getElement(-1)) ? "per" : "";
            String key = unit + "/" + length + "/" + per;
            seen.add(key);
            if (extra != null && !path.endsWith("/alias")) {
                extra.put(key, new Data(path, cldrFile.getStringValue(path)));
            }
        }
        return seen;
    }
}
