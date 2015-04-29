package org.unicode.cldr.tool;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Timer;
import org.unicode.cldr.util.XPathParts;

import com.google.common.base.Splitter;


public class ListUnits {
    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    private static final boolean SHOW_DECIMALS = false;

    enum Type {
        root, 
        en, 
        other;
        static Type fromString(String type) {
            return type.equals("en") ? en : type.equals("root") ? root : other;
        }
    }

    public static void main(String[] args) {
        Factory cldrFactory = CONFIG.getCldrFactory();
        Set<String> defaultContent = CONFIG.getSupplementalDataInfo().getDefaultContentLocales();
        Set<String> seen = new HashSet<>();

        LinkedHashSet<String> items = new LinkedHashSet<>();
        items.add("root");
        items.add("en");
        items.addAll(cldrFactory.getAvailableLanguages());
        Map<String,Data> rootMap = new HashMap<>();
        Map<String,Data> enMap = new HashMap<>();

        Timer timer = new Timer();
        int count = 0;
        XPathParts parts = new XPathParts();
        Splitter SEMI = Splitter.on(";").trimResults();
        Matcher currencyMatcher = Pattern.compile("([^0#]*).*[0#]([^0#]*)").matcher("");
        
        for (String locale : items) {
            Type type = Type.fromString(locale);
            if (type == Type.root || type == Type.en || defaultContent.contains(locale)) {
                continue;
            }
            CLDRFile cldrFile = cldrFactory.make(locale, true);
            String compactPathPrefix = "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"short\"]";
            String currencyPattern = cldrFile.getStringValue("//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]/currencyFormatLength/currencyFormat[@type=\"standard\"]/pattern[@type=\"standard\"]");
            String firstPart = SEMI.split(currencyPattern).iterator().next();
            if (!currencyMatcher.reset(firstPart).matches()) {
                throw new IllegalArgumentException("bad matcher");
            }
            String prefix = currencyMatcher.group(1);
            String suffix = currencyMatcher.group(2);
//            DecimalFormat format = new DecimalFormat(currencyPattern);
//            String prefix = format.getPositivePrefix();
//            String suffix = format.getPositiveSuffix();

//            ICUServiceBuilder builder = new ICUServiceBuilder().setCldrFile(cldrFile);
//            DecimalFormat format = builder.getCurrencyFormat("XXX");
//            String prefix = format.getPositivePrefix().replace("XXX", "\u00a4");
//            String suffix = format.getPositiveSuffix().replace("XXX", "\u00a4");
            if (SHOW_DECIMALS) {
                System.out.println("\n#" + locale + "\t«" + prefix + "»\t«" + suffix + "»\t«" + currencyPattern + "»");
                TreeMap<String,String> data = new TreeMap<>();
                for (String path : cldrFile.fullIterable()) {
//                    if (s.contains("decimalFormats")) {
//                        System.out.println(s);
//                    }
                    if (path.startsWith(compactPathPrefix)) {
                        String value = cldrFile.getStringValue(path);
                        String mod = path.replace("decimal", "currency") + "[@draft=\"provisional\"]";
                        //                        // locale=en ; action=add ; new_path=//ldml/localeDisplayNames/territories/territory[@type="PS"][@alt="short"] ; new_value=Palestine
                        data.put(mod, "locale=" + locale 
                            + " ; action=add" 
                            + " ; new_value=" + prefix + value + suffix
                            + " ; new_path=" + mod);
                    }
                }
                for (Entry<String, String> line : data.entrySet()) {
                    System.out.println(line.getValue());
                }
                data.clear();
            } else {
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
        System.out.println("#Done: " + count + ", " + timer);
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
