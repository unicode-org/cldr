package org.unicode.cldr.tool;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.Timer;
import org.unicode.cldr.util.XPathParts;

import com.google.common.base.Splitter;
import com.ibm.icu.text.UnicodeSet;

public class ListUnits {
    private static final UnicodeSet BIDI_CONTROL = new UnicodeSet("[:bidi_control:]").freeze();
    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    private static final SupplementalDataInfo SUPP = CONFIG.getSupplementalDataInfo();
    private static final Task TASK = Task.listSimpleUnits;

    private enum Task {
        listUnits, listSimpleUnits, showDecimals, getDigits,
    }

    enum Type {
        root, en, other;
        static Type fromString(String type) {
            return type.equals("en") ? en : type.equals("root") ? root : other;
        }
    }

    public static void main(String[] args) {
        Factory cldrFactory = CONFIG.getCldrFactory();
        Set<String> defaultContent = SUPP.getDefaultContentLocales();
        Set<String> seen = new HashSet<>();

        LinkedHashSet<String> items = new LinkedHashSet<>();
        items.add("root");
        items.add("en");
        items.addAll(cldrFactory.getAvailableLanguages());
        Map<String, Data> rootMap = new HashMap<>();
        Map<String, Data> enMap = new HashMap<>();

        Timer timer = new Timer();
        int count = 0;
        XPathParts parts = new XPathParts();
        Splitter SEMI = Splitter.on(";").trimResults();
        Matcher currencyMatcher = PatternCache.get("([^0#]*).*[0#]([^0#]*)").matcher("");

        for (String locale : items) {
            Type type = Type.fromString(locale);
            if (type == Type.root || type == Type.en || defaultContent.contains(locale)) {
                continue;
            }
            CLDRFile cldrFile = cldrFactory.make(locale, true);
//            DecimalFormat format = new DecimalFormat(currencyPattern);
//            String prefix = format.getPositivePrefix();
//            String suffix = format.getPositiveSuffix();

//            ICUServiceBuilder builder = new ICUServiceBuilder().setCldrFile(cldrFile);
//            DecimalFormat format = builder.getCurrencyFormat("XXX");
//            String prefix = format.getPositivePrefix().replace("XXX", "\u00a4");
//            String suffix = format.getPositiveSuffix().replace("XXX", "\u00a4");
            switch (TASK) {
            case showDecimals: {
                String compactPathPrefix = "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"short\"]";
                String currencyPattern = cldrFile
                    .getStringValue(
                        "//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]/currencyFormatLength/currencyFormat[@type=\"standard\"]/pattern[@type=\"standard\"]");
                String firstPart = SEMI.split(currencyPattern).iterator().next();
                if (!currencyMatcher.reset(firstPart).matches()) {
                    throw new IllegalArgumentException("bad matcher");
                }
                String prefix = currencyMatcher.group(1);
                String suffix = currencyMatcher.group(2);
                System.out.println("\n#" + locale + "\t«" + prefix + "»\t«" + suffix + "»\t«" + currencyPattern + "»");
                TreeMap<String, String> data = new TreeMap<>();
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
                break;
            }
            case listUnits:
            case listSimpleUnits: {
                Set<String> units = getUnits(cldrFile, TASK, type == Type.root ? rootMap : type == Type.en ? enMap : null);
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
                Splitter HYPHEN = Splitter.on('-');
                String oldBase = "";
                for (String unit : units) {
                    if (!seen.contains(unit)) {
                        switch (TASK) {
                        case listSimpleUnits:
                            String base = HYPHEN.split(unit).iterator().next();
                            if (!base.equals(oldBase)) {
                                oldBase = base;
                                System.out.println();
                            } else {
                                System.out.print(' ');
                            }
                            System.out.print(unit);
                            break;
                        case listUnits:
                            System.out.println("\t" + unit.replace("/", "\t")
                                .replaceFirst("-", "\t") + "\t" + locale);
                            break;
                        }
                        seen.add(unit);
                    }
                }
                break;
            }
            case getDigits: {
                getDigits(cldrFile);
                break;
            }
            }
        }
        System.out.println();
        System.out.println("#Done: " + count + ", " + timer);
    }

    static void getDigits(CLDRFile cldrFile) {
        System.out.println(cldrFile.getLocaleID());
        String numberSystem = cldrFile.getWinningValue("//ldml/numbers/defaultNumberingSystem");
        Set<String> seen = new HashSet<>();
        seen.add(numberSystem);
        Pair<UnicodeSet, UnicodeSet> main = getCharacters(cldrFile, numberSystem);
        System.out.println("\tdefault: " + numberSystem + ", " + main.getFirst().toPattern(false) + ", " + main.getSecond().toPattern(false));
        for (Iterator<String> it = cldrFile.iterator("//ldml/numbers/otherNumberingSystems"); it.hasNext();) {
            String path = it.next();
            String otherNumberingSystem = cldrFile.getWinningValue(path);
            if (seen.contains(otherNumberingSystem)) {
                continue;
            }
            seen.add(otherNumberingSystem);
            main = getCharacters(cldrFile, otherNumberingSystem);
            System.out.println("\tother: " + otherNumberingSystem
                + ", " + main.getFirst().toPattern(false) + "\t" + main.getSecond().toPattern(false));
        }
    }

    private static Pair<UnicodeSet, UnicodeSet> getCharacters(CLDRFile cldrFileToCheck, String numberSystem) {
        String digitString = SUPP.getDigits(numberSystem);
        UnicodeSet digits = digitString == null ? UnicodeSet.EMPTY : new UnicodeSet().addAll(digitString);

        UnicodeSet punctuation = new UnicodeSet();
        Set<String> errors = new LinkedHashSet<>();
        add(cldrFileToCheck, "decimal", numberSystem, punctuation, errors);
        //add(cldrFileToCheck, "exponential", numberSystem, punctuation, errors);
        add(cldrFileToCheck, "group", numberSystem, punctuation, errors);
        //add(cldrFileToCheck, "infinity", numberSystem, punctuation, errors);
        add(cldrFileToCheck, "minusSign", numberSystem, punctuation, errors);
        //add(cldrFileToCheck, "nan", numberSystem, punctuation, errors);
        add(cldrFileToCheck, "list", numberSystem, punctuation, errors);
        add(cldrFileToCheck, "percentSign", numberSystem, punctuation, errors);
        add(cldrFileToCheck, "perMille", numberSystem, punctuation, errors);
        add(cldrFileToCheck, "plusSign", numberSystem, punctuation, errors);
        // symbols.setZeroDigit(getSymbolString(cldrFileToCheck, "nativeZeroDigit", numberSystem));
        if (!errors.isEmpty() && digitString != null) {
            System.out.println("Missing: " + numberSystem + "\t" + errors);
        }
        punctuation.removeAll(BIDI_CONTROL);
        return Pair.of(digits, punctuation);
    }

    private static void add(CLDRFile cldrFileToCheck, String subtype, String numberSystem, UnicodeSet punctuation, Set<String> errors) {
        final String result = getSymbolString(cldrFileToCheck, subtype, numberSystem);
        if (result == null) {
            errors.add(subtype);
        } else {
            punctuation.addAll(result);
        }
    }

    private static String getSymbolString(CLDRFile cldrFile, String key, String numsys) {
        return cldrFile.getWinningValue("//ldml/numbers/symbols[@numberSystem=\"" + numsys + "\"]/" + key);
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

    private static Set<String> getUnits(CLDRFile cldrFile, Task task, Map<String, Data> extra) {
        Set<String> seen = new TreeSet<String>();
        for (String path : cldrFile) {
            if (!path.contains("/unit")) {
                continue;
            }
            XPathParts parts = XPathParts.getFrozenInstance(path);
            String unit = parts.findAttributeValue("unit", "type");
            if (unit == null) {
                continue;
            }
            String key = unit;
            if (task == Task.listUnits) {
                String length = parts.findAttributeValue("unitLength", "type");
                String per = "perUnitPattern".equals(parts.getElement(-1)) ? "per" : "";
                key = unit + "/" + length + "/" + per;
            }
            seen.add(key);
            if (extra != null && !path.endsWith("/alias")) {
                extra.put(key, new Data(path, cldrFile.getStringValue(path)));
            }
        }
        return seen;
    }
}
