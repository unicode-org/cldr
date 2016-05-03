package org.unicode.cldr.tool;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.ArrayComparator;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class GenerateG2xG2 {
    static CLDRFile english;
    static CLDRFile root;

    public static void main(String[] args) throws Exception {
        if (showLocales(-1)) return;
        // showCollator();

        String sourceLanguage = "G5";
        String targetLanguage = "G5";
        Factory cldrFactory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
        english = cldrFactory.make("en", true);
        root = cldrFactory.make("root", true);
        StandardCodes sc = StandardCodes.make();
        Map<Organization, Map<String, Level>> type_code_value = sc.getLocaleTypes();
        Set<String> sourceSet = new TreeSet<String>();
        Set<String> targetLanguageSet = new TreeSet<String>();
        targetLanguageSet.add("no");
        addPriority("G2", "nn");
        addPriority("G2", "no");
        targetLanguageSet.add("nn");
        Set<String> targetScriptSet = new TreeSet<String>();
        Set<String> targetRegionSet = new TreeSet<String>();
        Set<String> targetTZSet = new TreeSet<String>();
        Set<String> targetCurrencySet = new TreeSet<String>();
        for (Organization type : type_code_value.keySet()) {
            Map<String, Level> code_value = type_code_value.get(type);
            if (!type.equals(Organization.ibm)) continue;
            for (String locale : code_value.keySet()) {
                if (locale.equals("no")) continue;
                String priority = code_value.get(locale).toString();
                ULocale ulocale = new ULocale(locale);
                String language = ulocale.getLanguage();
                String script = ulocale.getScript();
                String territory = ulocale.getCountry();
                if (sourceLanguage.compareTo(priority) >= 0) {
                    if (language.equals("no")) language = "nn";
                    locale = new ULocale(language, script).toString();
                    sourceSet.add(locale);
                    addPriority(priority, locale);
                }
                if (targetLanguage.compareTo(priority) >= 0) {
                    targetLanguageSet.add(language);
                    targetScriptSet.add(script);
                    targetRegionSet.add(territory);
                    addPriority(priority, language);
                    addPriority(priority, script);
                    addPriority("G4", territory); // will normally be overridden
                }
            }
        }
        // set the priorities for territories
        Map<String, List<String>> worldBankInfo = sc.getWorldBankInfo();
        Set<String> euCodes = new HashSet<String>(Arrays.asList(new String[] { "AT", "BE", "CY", "CZ", "DK", "EE",
            "FI", "FR", "DE", "GR", "HU", "IT", "LV", "LT", "LU", "MT", "NL", "PL", "PT", "SI", "ES", "SE", "GB" }));
        for (String countryCode : worldBankInfo.keySet()) {
            if (priorityMap.get(countryCode) == null) continue; // only use ones we already have: defaults G4
            List<String> values = worldBankInfo.get(countryCode);
            double gdp = Double.parseDouble(values.get(1));
            if (gdp >= 1E+13)
                addPriority("G0", countryCode);
            else if (gdp >= 1E+12)
                addPriority("G1", countryCode);
            else if (gdp >= 1E+11)
                addPriority("G2", countryCode);
            else if (euCodes.contains(countryCode)) addPriority("G3", countryCode);
            // else if (gdp >= 1E+10) addPriority("G4", countryCode);
        }
        // fill in the currencies, and TZs for the countries that have multiple zones
        Map<String, Set<String>> c2z = sc.getCountryToZoneSet();
        SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo.getInstance();
        Set<String> mainTimeZones = supplementalDataInfo.getCanonicalTimeZones();
        for (Iterator<String> it = targetRegionSet.iterator(); it.hasNext();) {
            String country = it.next();
            String priority = priorityMap.get(country);
            for (Iterator<String> it2 = getCurrency(country).iterator(); it2.hasNext();) {
                String currency = it2.next();
                targetCurrencySet.add(currency);
                addPriority(priority, currency);
            }
            Set<String> s = c2z.get(country);
            if (s.size() == 1) continue;
            for (Iterator<String> it2 = s.iterator(); it2.hasNext();) {
                String tzid = it2.next();
                if (!mainTimeZones.contains(tzid)) continue;
                targetTZSet.add(tzid);
                addPriority(priority, tzid);
            }
        }
        // print out missing translations.
        PrintWriter pw = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY, "G2xG2.txt");
        // show priorities
        Comparator<String> comp = new UTF16.StringComparator();
        @SuppressWarnings("unchecked")
        Set<String[]> priority_set = new TreeSet<String[]>(new ArrayComparator(new Comparator[] { comp, comp, comp }));
        for (Iterator<String> it = priorityMap.keySet().iterator(); it.hasNext();) {
            String code = it.next();
            String priority = priorityMap.get(code);
            if (priority == null) continue;
            int type = getType(code);
            // if (type != CLDRFile.TERRITORY_NAME) continue;
            priority_set.add(new String[] { priority, type + "", code });
        }
        String lastPriority = "";
        //String lastType = "";
        for (Iterator<String[]> it = priority_set.iterator(); it.hasNext();) {
            String[] items = it.next();
            if (!lastPriority.equals(items[0])) {
                lastPriority = items[0];
                pw.println();
                // pw.println(lastPriority);
            }
            String typeName = getTypeName(items[2]);
            pw.println(lastPriority + "\t" + typeName + "\t" + items[2] + "\t(" + getItemName(english, items[2]) + ")");
        }
        pw.flush();
        // print out missing translations.
        for (Iterator<String> it = sourceSet.iterator(); it.hasNext();) {
            String sourceLocale = it.next();
            System.out.print(sourceLocale + ", ");
            CLDRFile sourceData = cldrFactory.make(sourceLocale, true);
            pw.println();
            String title = sourceLocale;
            checkItems(pw, title, sourceData, CLDRFile.LANGUAGE_NAME, targetLanguageSet);
            checkItems(pw, title, sourceData, CLDRFile.SCRIPT_NAME, targetScriptSet);
            checkItems(pw, title, sourceData, CLDRFile.TERRITORY_NAME, targetRegionSet);
            checkItems(pw, title, sourceData, CLDRFile.CURRENCY_NAME, targetCurrencySet);
            // only check timezones if exemplar characters don't include a-z
            String v = sourceData.getStringValue("//ldml/characters/exemplarCharacters");
            UnicodeSet exemplars = new UnicodeSet(v);
            if (exemplars.contains('a', 'z')) continue;
            checkItems(pw, title, sourceData, CLDRFile.TZ_EXEMPLAR, targetTZSet);
        }
        pw.println();
        pw.println("Sizes - incremental");
        pw.println();
        int runningTotalCount = 0;
        int runningMissingCount = 0;
        NumberFormat percent = NumberFormat.getPercentInstance();
        percent.setMinimumFractionDigits(1);
        NumberFormat nf = NumberFormat.getInstance();
        nf.setGroupingUsed(true);
        nf.setMinimumFractionDigits(0);
        for (Iterator<String> it = totalMap.keySet().iterator(); it.hasNext();) {
            String key = it.next();
            Totals t = totalMap.get(key);
            runningTotalCount = t.totalCount;
            runningMissingCount = t.missingCount;
            pw.println(key.substring(0, 2) + "\t" + key.substring(2) + "\t" + runningMissingCount
                + "\t" + runningTotalCount
                + "\t" + percent.format(runningMissingCount / (0.0 + runningTotalCount)));
        }
        pw.close();
        System.out.println();
        System.out.println("Done");
    }

    private static boolean showLocales(int choice) throws Exception {
        ULocale desiredDisplayLocale = ULocale.ENGLISH;
        Set<String> testSet = new TreeSet<String>();
        StandardCodes sc = StandardCodes.make();
        {
            Set<String> countries = sc.getGoodAvailableCodes("territory");
            Factory cldrFactory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
            english = cldrFactory.make("en", true);
            for (Iterator<String> it = countries.iterator(); it.hasNext();) {
                String territory = it.next();
                if (territory.charAt(0) < 'A') continue;
                String locale = "haw-" + territory;
                System.out.print(locale + ": " + english.getName(locale) + ", ");
            }
            if (true) return true;
        }

        if (choice == -1) {

            testSet.addAll(sc.getGoodAvailableCodes("currency"));
            Factory cldrFactory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
            english = cldrFactory.make("en", false);
            for (Iterator it = testSet.iterator(); it.hasNext();) {
                String country = (String) it.next();
                System.out.println(country + "\t" + english.getName(CLDRFile.CURRENCY_NAME, country));
            }
            return true;
        } else if (choice == 0) { // get available
            ULocale[] list = BreakIterator.getAvailableULocales();
            for (int i = 0; i < list.length; ++i) {
                testSet.add(list[i].toString());
            }
        } else {
            boolean USE_3066bis = choice == 2;
            // produce random list of RFC3066 language tags
            Set<String> grandfathered = sc.getAvailableCodes("grandfathered");
            List<String> language_subtags = new ArrayList<String>(sc.getGoodAvailableCodes("language"));
            List<String> script_subtags = new ArrayList<String>(sc.getGoodAvailableCodes("script"));
            List<String> region_subtags = new ArrayList<String>(sc.getGoodAvailableCodes("territory"));
            for (String possibility : grandfathered) {
                System.out.println(possibility);
                if (new ULocale(possibility).getScript().length() != 0) {
                    System.out.println("\tAdding");
                    testSet.add(possibility);
                }
            }
            if (!USE_3066bis) for (Iterator it = region_subtags.iterator(); it.hasNext();) {
                String possibility = (String) it.next();
                if (possibility.compareTo("A") < 0) it.remove();
            }
            Random rand = new Random();
            for (int i = 0; i < 200; ++i) {
                int r = rand.nextInt(language_subtags.size());
                String result = language_subtags.get(rand.nextInt(language_subtags.size()));
                if (USE_3066bis && rand.nextDouble() > 0.5) {
                    result += "-" + script_subtags.get(rand.nextInt(script_subtags.size()));
                }
                if (rand.nextDouble() > 0.1) {
                    result += "-" + region_subtags.get(rand.nextInt(region_subtags.size()));
                }
                testSet.add(result);
            }
        }
        for (Iterator<String> it = testSet.iterator(); it.hasNext();) {
            ULocale language = new ULocale(it.next());
            System.out.println(language + " \t" + language.getDisplayName(desiredDisplayLocale));
        }
        return true;
    }

    private static void showCollator() throws Exception {
        RuleBasedCollator col = (RuleBasedCollator) Collator.getInstance(new ULocale("zh"));
        showExample(col);
        String rules = col.getRules(false);
        // System.out.println(com.ibm.icu.impl.Utility.escape(rules));
        rules += "& \u93CA < A <<< a & \u7C3F < B <<< b";
        RuleBasedCollator col2 = new RuleBasedCollator(rules);
        showExample(col2);
    }

    private static void showExample(RuleBasedCollator col) {
        String samples = "a A b B \u5416 \u93CA \u516b \u7C3F";
        Set<String> s = new TreeSet<String>(col);
        s.addAll(Arrays.asList(samples.split(" ")));
        System.out.println(com.ibm.icu.impl.Utility.escape(s.toString()));
    }

    static Map<String, String> priorityMap = new TreeMap<String, String>();

    static void addPriority(String priority, String code) {
        if (code.length() == 0) return;
        String oldPriority = priorityMap.get(code);
        if (oldPriority == null || priority.compareTo(oldPriority) < 0) priorityMap.put(code, priority);
        System.out.println(code + ": " + priority);
    }

    static class Totals {
        int totalCount;
        int missingCount;
    }

    static Map<String, Totals> totalMap = new TreeMap<String, Totals>();

    static void checkItems(PrintWriter pw, String sourceLocale, CLDRFile sourceData, int type, Set<String> targetItemSet) {
        for (Iterator<String> it2 = targetItemSet.iterator(); it2.hasNext();) {
            String item = it2.next();
            if (item.length() == 0) continue;
            String key = priorityMap.get(sourceLocale) + "" + priorityMap.get(item);
            Totals t = totalMap.get(key);
            if (t == null) totalMap.put(key, t = new Totals());
            t.totalCount++;
            String translation = getItemName(sourceData, type, item);
            String rootName = getItemName(root, type, item);
            if (rootName.equals(translation)) {
                t.missingCount++;
                pw.println(priorityMap.get(sourceLocale)
                    + "\t" + sourceLocale +
                    "\t(" + english.getName(sourceLocale) + ": "
                    + sourceData.getName(sourceLocale) + ")"
                    + "\t" + priorityMap.get(item)
                    + "\t" + item
                    + "\t(" + getItemName(english, type, item) + ")");
            }
        }
    }

    private static String getItemName(CLDRFile data, String item) {
        return getItemName(data, getType(item), item);
    }

    private static int getType(String item) {
        int type = CLDRFile.LANGUAGE_NAME;
        if (item.indexOf('/') >= 0)
            type = CLDRFile.TZ_EXEMPLAR; // America/Los_Angeles
        else if (item.length() == 4)
            type = CLDRFile.SCRIPT_NAME; // Hant
        else if (item.charAt(0) <= '9')
            type = CLDRFile.TERRITORY_NAME; // 001
        else if (item.charAt(0) < 'a') {
            if (item.length() == 3)
                type = CLDRFile.CURRENCY_NAME;
            else
                type = CLDRFile.TERRITORY_NAME; // US or USD
        }
        return type;
    }

    private static String getTypeName(String item) {
        switch (getType(item)) {
        case CLDRFile.LANGUAGE_NAME:
            return "Lang";
        case CLDRFile.TZ_EXEMPLAR:
            return "Zone";
        case CLDRFile.SCRIPT_NAME:
            return "Script";
        case CLDRFile.TERRITORY_NAME:
            return "Region";
        case CLDRFile.CURRENCY_NAME:
            return "Curr.";
        }
        return "?";
    }

    private static String getItemName(CLDRFile data, int type, String item) {
        String result;
        if (type == CLDRFile.LANGUAGE_NAME) {
            result = data.getName(item);
        } else if (type != CLDRFile.TZ_EXEMPLAR) {
            result = data.getName(type, item);
        } else {
            String prefix = "//ldml/dates/timeZoneNames/zone[@type=\"" + item + "\"]/exemplarCity";
            result = data.getStringValue(prefix);
        }
        return result == null ? item : result;
    }

    static Map<String, List<String>> territory_currency = null;

    private static List<String> getCurrency(String territory) {
        if (territory_currency == null) {
            territory_currency = new TreeMap<String, List<String>>();
            Factory cldrFactory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
            CLDRFile supp = cldrFactory.make(CLDRFile.SUPPLEMENTAL_NAME, false);
            XPathParts parts = new XPathParts(new UTF16.StringComparator(), null);
            for (String path : supp) {
                if (path.indexOf("/currencyData") >= 0) {
                    // <region iso3166="AR">
                    // <currency iso4217="ARS" from="1992-01-01"/>
                    if (path.indexOf("/region") >= 0) {
                        parts.set(supp.getFullXPath(path));
                        Map<String, String> attributes = parts.getAttributes(parts.size() - 2);
                        String iso3166 = attributes.get("iso3166");
                        attributes = parts.getAttributes(parts.size() - 1);
                        String iso4217 = attributes.get("iso4217");
                        String to = attributes.get("to");
                        if (to != null) continue;
                        List<String> info = territory_currency.get(iso3166);
                        if (info == null) territory_currency.put(iso3166, info = new ArrayList<String>());
                        info.add(iso4217);
                        // System.out.println(iso3166 + " => " + iso4217);
                    }
                }
            }
        }
        return territory_currency.get(territory);
    }
}