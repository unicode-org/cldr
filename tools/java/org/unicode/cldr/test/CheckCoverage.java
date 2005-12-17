package org.unicode.cldr.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.text.UnicodeSet;

/**
 * Checks locale data for coverage.<br>
 * Options:<br>
 * CheckCoverage.requiredLevel=value to override the required level. values: comprehensive, modern, moderate, basic<br>
 * CheckCoverage.skip=true to skip a locale. For console testing, you want to skip the non-language locales, since
 * they don't typically add, just replace. See CheckCLDR for an example.
 * @author davis
 *
 */
public class CheckCoverage extends CheckCLDR {
    static final boolean DEBUG = false;
    private static CoverageLevel coverageLevel = new CoverageLevel();

    private boolean skip; // set to null if we should not be checking this file

    public CheckCLDR handleCheck(String path, String fullPath, String value,
            Map options, List result) {
        // for now, skip all but localeDisplayNames
        if (skip) return this;
        if (path.indexOf("localeDisplayNames") < 0 && path.indexOf("currencies") < 0 && path.indexOf("exemplarCity") < 0) return this;

        // skip all items that are in anything but raw codes
        String source = getCldrFileToCheck().getSourceLocaleID(path);
        if (!source.equals(XMLSource.CODE_FALLBACK_ID)) return this;
        
        // check to see if the level is good enough
        Level level = coverageLevel.getCoverageLevel(path, fullPath, value);
        if (level == Level.SKIP) return this;
        Level requiredLevel = coverageLevel.getRequiredLevel();
        if (options != null) {
            String optionLevel = (String) options.get("CheckCoverage.requiredLevel");
            if (optionLevel != null) requiredLevel = Level.get(optionLevel);
        }
        if (requiredLevel.compareTo(level) >= 0) {
            result.add(new CheckStatus().setType(CheckStatus.errorType)
                    .setMessage("Needed to meet {0} coverage level.", new Object[] { level }));
        } else if (DEBUG) {
            System.out.println(level + "\t" + path);
        }
        return this;
    }

    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck,
            Map options, List possibleErrors) {
        if (cldrFileToCheck == null) return this;
        skip = true;
        if (options != null && options.get("CheckCoverage.skip") != null) return this;
        
        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);

        if (cldrFileToCheck.getLocaleID().equals("root")) return this;
        coverageLevel.setFile(cldrFileToCheck, options);
        skip = false;
        return this;
    }

    static class Level implements Comparable {
        static List all = new ArrayList();

        private byte level;

        private String name;

        static final Level SKIP = new Level(0, "none"), BASIC = new Level(2,
                "basic"), MODERATE = new Level(4, "moderate"),
                MODERN = new Level(6, "modern"), COMPREHENSIVE = new Level(10,
                        "comprehensive");

        private Level(int i, String string) {
            level = (byte) i;
            name = string;
            all.add(this);
        }

        public static Level get(String name) {
            for (int i = 0; i < all.size(); ++i) {
                Level item = (Level) all.get(i);
                if (item.name.equalsIgnoreCase(name)) return item;
            }
            return SKIP;
        }

        public String toString() {
            return name;
        }

        public int compareTo(Object o) {
            int otherLevel = ((Level) o).level;
            return level < otherLevel ? -1 : level > otherLevel ? 1 : 0;
        }
    }

    /**
     * <!-- Target-Language is the language under consideration.
     * Target-Territories is the list of territories found by looking up
     * Target-Language in the <languageData> elements in supplementalData.xml
     * Language-List is Target-Language, plus o basic: Chinese, English, French,
     * German, Italian, Japanese, Portuguese, Russian, Spanish (de, en, es, fr,
     * it, ja, pt, ru, zh) o moderate: basic + Arabic, Hindi, Korean,
     * Indonesian, Dutch, Bengali, Turkish, Thai, Polish (ar, hi, ko, in, nl,
     * bn, tr, th, pl). If an EU language, add the remaining official EU
     * languages, currently: Danish, Greek, Finnish, Swedish, Czech, Estonian,
     * Latvian, Lithuanian, Hungarian, Maltese, Slovak, Slovene (da, el, fi, sv,
     * cs, et, lv, lt, hu, mt, sk, sl) o modern: all languages that are official
     * or major commercial languages of modern territories Target-Scripts is the
     * list of scripts in which Target-Language can be customarily written
     * (found by looking up Target-Language in the <languageData> elements in
     * supplementalData.xml). Script-List is the Target-Scripts plus the major
     * scripts used for multiple languages o Latin, Simplified Chinese,
     * Traditional Chinese, Cyrillic, Arabic (Latn, Hans, Hant, Cyrl, Arab)
     * Territory-List is the list of territories formed by taking the
     * Target-Territories and adding: o basic: Brazil, China, France, Germany,
     * India, Italy, Japan, Russia, United Kingdom, United States (BR, CN, DE,
     * GB, FR, IN, IT, JP, RU, US) o moderate: basic + Spain, Canada, Korea,
     * Mexico, Australia, Netherlands, Switzerland, Belgium, Sweden, Turkey,
     * Austria, Indonesia, Saudi Arabia, Norway, Denmark, Poland, South Africa,
     * Greece, Finland, Ireland, Portugal, Thailand, Hong Kong SAR China, Taiwan
     * (ES, BE, SE, TR, AT, ID, SA, NO, DK, PL, ZA, GR, FI, IE, PT, TH, HK, TW).
     * If an EU language, add the remaining member EU countries: Luxembourg,
     * Czech Republic, Hungary, Estonia, Lithuania, Latvia, Slovenia, Slovakia,
     * Malta (LU, CZ, HU, ES, LT, LV, SI, SK, MT). o modern: all current ISO
     * 3166 territories, plus the UN M.49 regions in supplementalData.xml
     * Currency-List is the list of current official currencies used in any of
     * the territories in Territory-List, found by looking at the region
     * elements in supplementalData.xml. Calendar-List is the set of calendars
     * in customary use in any of Target-Territories, plus Gregorian.
     * Timezone-List is the set of all timezones for multi-zone territories in
     * Target-Territories, plus each of following timezones whose territories is
     * in Territory-List: o Brazil: America/Buenos_Aires, America/Rio_Branco,
     * America/Campo_Grande, America/Sao_Paulo o Australia: Australia/Perth,
     * Australia/Darwin, Australia/Brisbane, Australia/Adelaide,
     * Australia/Sydney, Australia/Hobart o Canada: America/Vancouver,
     * America/Edmonton, America/Regina, America/Winnipeg, America/Toronto,
     * America/Halifax, America/St_Johns o Mexico: America/Tijuana,
     * America/Hermosillo, America/Chihuahua, America/Mexico_City o US:
     * Pacific/Honolulu, America/Anchorage, America/Los_Angeles,
     * America/Phoenix, America/Denver, America/Chicago, America/Indianapolis,
     * America/New_York
     */
    static public class CoverageLevel {
        private static Object sync = new Object();

        // commmon stuff, set once
        private static Map coverageData = new TreeMap();
        private static Map base_language_level = new TreeMap();
        private static Map base_script_level = new TreeMap();
        private static Map base_territory_level = new TreeMap();
        private static Set minimalTimezones;

        private static Map language_scripts = new TreeMap();

        private static Map language_territories = new TreeMap();
        
        private static Set modernLanguages = new TreeSet();
        private static Set modernScripts = new TreeSet();
        private static Set modernTerritories = new TreeSet();
        private static Map locale_requiredLevel = new TreeMap();
        private static Map territory_currency = new TreeMap();
        private static Map territory_timezone = new TreeMap();
        private static Set modernCurrencies = new TreeSet();
         
        // current stuff, set according to file
  
        private boolean initialized = false;

        private transient LocaleIDParser parser = new LocaleIDParser();

        private transient XPathParts parts = new XPathParts(null, null);

        private Map language_level = new TreeMap();

        private Level requiredLevel;

        private Map script_level = new TreeMap();
        private Map zone_level = new TreeMap();

        private Map territory_level = new TreeMap();
        private Map currency_level = new TreeMap();
        
        StandardCodes sc = StandardCodes.make();
        
        boolean latinScript = false;

        public void setFile(CLDRFile file, Map options) {
            synchronized (sync) {
                if (!initialized) {
                    init(file);
                    initialized = true;
                }
            }
            latinScript = false;
            UnicodeSet exemplars = file.getExemplarSet("");
            if (exemplars != null) {
                UnicodeSet auxexemplars = file.getExemplarSet("auxiliary");
                if (auxexemplars != null) exemplars.addAll(auxexemplars);
                latinScript = exemplars.contains('A','Z');
            }
            
            parser.set(file.getLocaleID());
            String language = parser.getLanguage();
            requiredLevel = (Level) locale_requiredLevel.get(parser.getLanguageScript());
            if (requiredLevel == null) requiredLevel = (Level) locale_requiredLevel.get(language);
            if (requiredLevel == null) requiredLevel = Level.BASIC;
            
            // do the work of putting together the coverage info
            language_level.clear();
            script_level.clear();

            language_level.putAll(base_language_level);
            language_level.put(language, Level.BASIC);

            script_level.putAll(base_script_level);
            putAll(script_level, (Set) language_scripts.get(language), Level.BASIC);

            territory_level.putAll(base_territory_level);
            putAll(territory_level, (Set) language_territories.get(language), Level.BASIC);
            
            // set currencies, timezones according to territory level
            currency_level.clear();
            putAll(currency_level, modernCurrencies, Level.MODERN);
            for (Iterator it = territory_level.keySet().iterator(); it.hasNext();) {
                String territory = (String) it.next();
                Level level = (Level) territory_level.get(territory);
                Set currencies = (Set) territory_currency.get(territory);
                setIfBetter(currencies, level, currency_level);
                Set timezones = (Set) territory_timezone.get(territory);
                if (timezones != null) {
                // only worry about the ones that are "moderate"
                    timezones.retainAll(minimalTimezones);
                    setIfBetter(timezones, level, zone_level);
                }
            }

            if (DEBUG) {
                System.out.println("Required Level: " + requiredLevel);
                System.out.println(language_level);               
                System.out.println(script_level);
                System.out.println(territory_level);
                System.out.println(currency_level);
                System.out.println("file-specific info set");
                System.out.flush();
            }
        }

        private void putAll(Map targetMap, Collection keyset, Object value) {
            if (keyset == null) return;
            for (Iterator it2 = keyset.iterator(); it2.hasNext();) {
                targetMap.put(it2.next(), value);
            }
        }

        private void setIfBetter(Set keySet, Level level, Map targetMap) {
            if (keySet == null) return;
            for (Iterator it2 = keySet.iterator(); it2.hasNext();) {
                Object script = it2.next();
                Level old = (Level) targetMap.get(script);
                if (old == null || level.compareTo(old) < 0) {
                    //System.out.println("\t" + script + "\t(" + old + ")");
                    targetMap.put(script, level);
                }
            }
        }

        public Level getCoverageLevel(String path, String fullPath, String value) {
            parts.set(fullPath);
            String lastElement = parts.getElement(-1);
            String type = (String) parts.getAttributes(-1).get("type");
            Level result = null;
            String part1 = parts.getElement(1);
            if (lastElement.equals("exemplarCity")) {
                if (latinScript) {
                    result = Level.SKIP;
                } else {
                    type = (String) parts.getAttributes(-2).get("type"); // it's one level up
                    result = (Level) zone_level.get(type);
                }
            } else if (part1.equals("localeDisplayNames")) {
                if (lastElement.equals("language")) {
                    // <language type=\"aa\">Afar</language>"
                    result = (Level) language_level.get(type);
                } else if (lastElement.equals("territory")) {
                    result = (Level) territory_level.get(type);
                } else if (lastElement.equals("script")) {
                    result = (Level) script_level.get(type);
                }
            } else if (part1.equals("numbers")) {
                /*
                 * <numbers> ? <currencies> ? <currency type="BRL"> <displayName draft="true">Brazilian Real</displayName>
                 */
                if (latinScript && lastElement.equals("symbol")) {
                    result = Level.SKIP;
                } else if (lastElement.equals("displayName") || lastElement.equals("symbol")) {
                    String currency = (String) parts.getAttributes(-2).get("type");
                    result = (Level) currency_level.get(currency);
                }
            }
            if (result == null) result = Level.COMPREHENSIVE;
            return result;
        }
        
        // ========== Initialization Stuff ===================

        public void init(CLDRFile file) {
            try {
                CLDRFile metadata = file.make("supplementalMetadata", false);
                getMetadata(metadata);

                CLDRFile data = file.make("supplementalData", false);
                getData(data);

                // put into an easier form to use
                
                Map type_languages = (Map) coverageData.get("languageCoverage");
                Utility.putAllTransposed(type_languages, base_language_level);
                Map type_scripts = (Map) coverageData.get("scriptCoverage");
                Utility.putAllTransposed(type_scripts, base_script_level);
                Map type_territories = (Map) coverageData.get("territoryCoverage");
                Utility.putAllTransposed(type_territories, base_territory_level);
                
                Map type_timezones = (Map) coverageData.get("timezoneCoverage");
                minimalTimezones = (Set) type_timezones.get(Level.MODERATE);
                
                // add the modern stuff, after doing both of the above
                
                modernLanguages.removeAll(base_language_level.keySet());
                putAll(base_language_level, modernLanguages, Level.MODERN);
                
                modernScripts.removeAll(base_script_level.keySet());
                putAll(base_script_level, modernScripts, Level.MODERN);
                
                modernTerritories.removeAll(base_territory_level.keySet());
                putAll(base_territory_level, modernTerritories, Level.MODERN);
                
                // set up the required levels
                try {
                    // just for now
                    Map platform_local_level = sc.getLocaleTypes();
                    Map locale_level = (Map) platform_local_level.get("IBM");
                    for (Iterator it = locale_level.keySet().iterator(); it.hasNext();) {
                        String locale = (String) it.next();
                        parser.set(locale);
                        String level = (String) locale_level.get(locale);
                        requiredLevel = Level.BASIC;
                        if ("G0".equals(level)) requiredLevel = Level.COMPREHENSIVE;
                        else if ("G1".equals(level)) requiredLevel = Level.MODERN;
                        else if ("G2".equals(level)) requiredLevel = Level.MODERATE;
                        String key = parser.getLanguage();
                        Level old = (Level) locale_requiredLevel.get(key);
                        if (old == null || old.compareTo(requiredLevel) > 0) {
                            locale_requiredLevel.put(key, requiredLevel);
                        }
                        String oldKey = key;
                        key = parser.getLanguageScript();
                        if (!key.equals(oldKey)) {
                            old = (Level) locale_requiredLevel.get(key);
                            if (old == null || old.compareTo(requiredLevel) > 0) {
                                locale_requiredLevel.put(key, requiredLevel);
                            }
                        }
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
                if (DEBUG) {
                    System.out.println(base_language_level);               
                    System.out.println(base_script_level);
                    System.out.println(base_territory_level);
                    System.out.println("common info set");
                    System.out.flush();
                }
            } catch (RuntimeException e) {
                throw e; // just for debugging
            }
        }

        private void getMetadata(CLDRFile metadata) {
            for (Iterator it = metadata.iterator(); it.hasNext();) {
                String path = (String) it.next();
                path = metadata.getFullXPath(path);
                parts.set(path);
                String lastElement = parts.getElement(-1);
                Map attributes = parts.getAttributes(-1);
                String type = (String) attributes.get("type");
                if (parts.containsElement("coverageAdditions")) {
                    // System.out.println(path);
                    // System.out.flush();
                    //String value = metadata.getStringValue(path);
                    // <languageCoverage type="basic" values="de en es fr it ja
                    // pt ru zh"/>
                    Level level = Level.get(type);
                    String values = (String) attributes.get("values");
                    Utility.addTreeMapChain(coverageData, new Object[] {
                            lastElement, level,
                            new TreeSet(Arrays.asList(values.split("\\s+"))) });
                }
            }
        }
        
        Set multizoneTerritories = null;
        
        private void getData(CLDRFile data) {
            for (Iterator it = data.iterator(null, CLDRFile.ldmlComparator); it.hasNext();) {
                String path = (String) it.next();
                //String value = metadata.getStringValue(path);
                path = data.getFullXPath(path);
                parts.set(path);
                String lastElement = parts.getElement(-1);
                Map attributes = parts.getAttributes(-1);
                String type = (String) attributes.get("type");
                //System.out.println(path);
                if (lastElement.equals("zoneItem")) {
                    if (multizoneTerritories == null) {
                        Map multiAttributes = parts.getAttributes(-2);
                        String multizone = (String) multiAttributes.get("multizone");
                        multizoneTerritories = new TreeSet(Arrays.asList(multizone.split("\\s+")));
                    }
                    //<zoneItem type="Africa/Abidjan" territory="CI"/>
                    String territory = (String) attributes.get("territory");
                    if (!multizoneTerritories.contains(territory)) continue;
                    Set territories = (Set) territory_timezone.get(territory);
                    if (territories == null) territory_timezone.put(territory, territories = new TreeSet());
                    territories.add(type);
                } else if (parts.containsElement("calendarData")) {
                    // System.out.println(path);
                    // System.out.flush();
                    // we have element, type, subtype, and values
                    Set values = new TreeSet(
                            Arrays.asList(((String) attributes
                                    .get("territories")).split("\\s+")));
                    Utility.addTreeMapChain(coverageData, new Object[] {
                            lastElement, type, values });
                } else if (parts.containsElement("languageData")) {
                    // <language type="ab" scripts="Cyrl" territories="GE"
                    // alt="secondary"/>
                    String alt = (String) attributes.get("alt");
                    if (alt != null) continue;
                    modernLanguages.add(type);
                    String scripts = (String) attributes.get("scripts");
                    if (scripts != null) {
                        Set scriptSet = new TreeSet(Arrays.asList(scripts
                                .split("\\s+")));
                        modernScripts.addAll(scriptSet);
                        Utility.addTreeMapChain(language_scripts,
                                new Object[] {type, scriptSet});
                    }
                    String territories = (String) attributes
                            .get("territories");
                    if (territories != null) {
                        Set territorySet = new TreeSet(Arrays
                                .asList(territories
                                        .split("\\s+")));
                        modernTerritories.addAll(territorySet);
                        Utility.addTreeMapChain(language_territories,
                                new Object[] {type, territorySet});
                    }
                } else if (parts.containsElement("currencyData") && lastElement.equals("currency")) {
                    //         <region iso3166="AM"><currency iso4217="AMD" from="1993-11-22"/>
                    // if the 'to' value is less than 10 years, it is not modern
                    String to = (String) attributes.get("to");
                    String currency = (String) attributes.get("iso4217");
                    if (to == null || to.compareTo("1995") >= 0) {
                        modernCurrencies.add(currency);
                        // only add current currencies to must have list
                        if (to == null) {
                            String region = (String) parts.getAttributes(-2).get("iso3166");
                            Set currencies = (Set) territory_currency.get(region);
                            if (currencies == null) territory_currency.put(region, currencies = new TreeSet());
                            currencies.add(currency);
                        }
                    }
                }
            }
        }

        public Level getRequiredLevel() {
            return requiredLevel;
        }
    }

}