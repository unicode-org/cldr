/*
 ******************************************************************************
 * Copyright (C) 2004-2013, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package org.unicode.cldr.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Iso639Data;
import org.unicode.cldr.util.IsoCurrencyParser;
import org.unicode.cldr.util.IsoCurrencyParser.Data;
import org.unicode.cldr.util.IsoRegionData;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Log;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.Tabber;
import org.unicode.cldr.util.TimezoneFormatter;
import org.unicode.cldr.util.UnicodeSetPrettyPrinter;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.props.ICUPropertyFactory;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMapIterator;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

/**
 * Simple program to count the amount of data in CLDR. Internal Use.
 */
public class CountItems {

    private static final Collator ROOT_PRIMARY_COLLATOR = Collator.getInstance(ULocale.ROOT)
        .setStrength2(Collator.PRIMARY);

    static final String needsTranslationString = "America/Buenos_Aires " // America/Rio_Branco
        + " America/Manaus America/Belem "
        + " America/Campo_Grande America/Sao_Paulo "
        + " Australia/Perth Australia/Darwin Australia/Brisbane Australia/Adelaide Australia/Sydney Australia/Hobart "
        + " America/Vancouver America/Edmonton America/Regina America/Winnipeg America/Toronto America/Halifax America/St_Johns "
        + " Asia/Jakarta "
        + " America/Tijuana America/Hermosillo America/Chihuahua America/Mexico_City "
        + " Europe/Moscow Europe/Kaliningrad Europe/Moscow Asia/Yekaterinburg Asia/Novosibirsk Asia/Yakutsk Asia/Vladivostok"
        + " Pacific/Honolulu America/Indiana/Indianapolis America/Anchorage "
        + " America/Los_Angeles America/Phoenix America/Denver America/Chicago America/Indianapolis"
        + " America/New_York";

    static final Map<String, String> country_map = CollectionUtilities.asMap(new String[][] {
        { "AQ", "http://www.worldtimezone.com/time-antarctica24.php" },
        { "AR", "http://www.worldtimezone.com/time-south-america24.php" },
        { "AU", "http://www.worldtimezone.com/time-australia24.php" },
        { "BR", "http://www.worldtimezone.com/time-south-america24.php" },
        { "CA", "http://www.worldtimezone.com/time-canada24.php" },
        { "CD", "http://www.worldtimezone.com/time-africa24.php" },
        { "CL", "http://www.worldtimezone.com/time-south-america24.php" },
        { "CN", "http://www.worldtimezone.com/time-cis24.php" },
        { "EC", "http://www.worldtimezone.com/time-south-america24.php" },
        { "ES", "http://www.worldtimezone.com/time-europe24.php" },
        { "FM", "http://www.worldtimezone.com/time-oceania24.php" },
        { "GL", "http://www.worldtimezone.com/index24.php" },
        { "ID", "http://www.worldtimezone.com/time-asia24.php" },
        { "KI", "http://www.worldtimezone.com/time-oceania24.php" },
        { "KZ", "http://www.worldtimezone.com/time-cis24.php" },
        { "MH", "http://www.worldtimezone.com/time-oceania24.php" },
        { "MN", "http://www.worldtimezone.com/time-cis24.php" },
        { "MX", "http://www.worldtimezone.com/index24.php" },
        { "MY", "http://www.worldtimezone.com/time-asia24.php" },
        { "NZ", "http://www.worldtimezone.com/time-oceania24.php" },
        { "PF", "http://www.worldtimezone.com/time-oceania24.php" },
        { "PT", "http://www.worldtimezone.com/time-europe24.php" },
        { "RU", "http://www.worldtimezone.com/time-russia24.php" },
        { "SJ", "http://www.worldtimezone.com/index24.php" },
        { "UA", "http://www.worldtimezone.com/time-cis24.php" },
        { "UM", "http://www.worldtimezone.com/time-oceania24.php" },
        { "US", "http://www.worldtimezone.com/time-usa24.php" },
        { "UZ", "http://www.worldtimezone.com/time-cis24.php" }, });

    /**
     * Count the data.
     *
     * @throws IOException
     */
    public static void main(String[] args) throws Exception {
        double deltaTime = System.currentTimeMillis();
        try {
            String methodName = System.getProperty("method");
            if (methodName != null) {
                CldrUtility.callMethod(methodName, CountItems.class);
            } else {
                ShowZoneEquivalences.getZoneEquivalences();
            }
        } finally {
            deltaTime = System.currentTimeMillis() - deltaTime;
            System.out.println("Elapsed: " + deltaTime / 1000.0 + " seconds");
            System.out.println("Done");
        }
    }

    static void subheader(PrintWriter out, Tabber tabber) {
        // out.println("<tr><td colspan='6' class='gap'>&nbsp;</td></tr>");
        out.println(tabber.process("Cnty" + "\t" + "Grp" + "\t" + "ZoneID" + "\t"
            + "Formatted ID" + "\t" + "MaxOffset" + "\t" + "MinOffset"));
    }

    /**
     *
     */
    private static void getPatternBlocks() {
        UnicodeSet patterns = new UnicodeSet("[:pattern_syntax:]");
        UnicodeSet unassigned = new UnicodeSet("[:unassigned:]");
        UnicodeSet punassigned = new UnicodeSet(patterns).retainAll(unassigned);
        UnicodeMap<String> blocks = ICUPropertyFactory.make().getProperty("block")
            .getUnicodeMap();
        blocks.setMissing("<Reserved-Block>");
        // blocks.composeWith(new UnicodeMap().putAll(new UnicodeSet(patterns).retainAll(unassigned),"<reserved>"),
        // new UnicodeMap.Composer() {
        // public Object compose(int codePoint, Object a, Object b) {
        // if (a == null) {
        // return b;
        // }
        // if (b == null) {
        // return a;
        // }
        // return a.toString() + " " + b.toString();
        // }});
        for (UnicodeMapIterator<String> it = new UnicodeMapIterator<String>(blocks); it
            .nextRange();) {
            UnicodeSet range = new UnicodeSet(it.codepoint, it.codepointEnd);
            boolean hasPat = range.containsSome(patterns);
            String prefix = !hasPat ? "Not-Syntax"
                : !range.containsSome(unassigned) ? "Closed" : !range
                    .containsSome(punassigned) ? "Closed2" : "Open";

            boolean show = (prefix.equals("Open") || prefix.equals("Closed2"));

            if (show)
                System.out.println();
            System.out.println(prefix + "\t" + range + "\t" + it.value);
            if (show) {
                System.out.println(new UnicodeMap<String>().putAll(unassigned, "<reserved>")
                    .putAll(punassigned, "<reserved-for-syntax>").setMissing(
                        "<assigned>")
                    .putAll(range.complement(), null));
            }
        }
    }

    /**
     * @throws IOException
     *
     */
    private static void showExemplars() throws IOException {
        PrintWriter out = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY, "fixed_exemplars.txt");
        Factory cldrFactory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
        Set<String> locales = cldrFactory.getAvailable();
        for (Iterator<String> it = locales.iterator(); it.hasNext();) {
            System.out.print('.');
            String locale = it.next();
            CLDRFile cldrfile = cldrFactory.make(locale, false);
            String v = cldrfile
                .getStringValue("//ldml/characters/exemplarCharacters");
            if (v == null)
                continue;
            UnicodeSet exemplars = new UnicodeSet(v);
            if (exemplars.size() != 0 && exemplars.size() < 500) {
                Collator col = Collator.getInstance(new ULocale(locale));
                Collator spaceCol = Collator.getInstance(new ULocale(locale));
                spaceCol.setStrength(Collator.PRIMARY);
                out.println(locale + ":\t\u200E" + v + '\u200E');
                // String fixedFull = CollectionUtilities.prettyPrint(exemplars, col, false);
                // System.out.println(" =>\t" + fixedFull);
                // verifyEquality(exemplars, new UnicodeSet(fixedFull));
                String fixed = new UnicodeSetPrettyPrinter()
                    .setOrdering(col != null ? col : Collator.getInstance(ULocale.ROOT))
                    .setSpaceComparator(spaceCol != null ? spaceCol : ROOT_PRIMARY_COLLATOR)
                    .setCompressRanges(true)
                    .format(exemplars);
                out.println(" =>\t\u200E" + fixed + '\u200E');

                verifyEquality(exemplars, new UnicodeSet(fixed));
                out.flush();
            }
        }
        out.close();
    }

    /**
     *
     */
    private static void verifyEquality(UnicodeSet exemplars, UnicodeSet others) {
        if (others.equals(exemplars))
            return;
        System.out.println("FAIL\ta-b\t"
            + new UnicodeSet(exemplars).removeAll(others));
        System.out.println("\tb-a\t" + new UnicodeSet(others).removeAll(exemplars));
    }

    /**
     *
     */
    public static void generateSupplementalCurrencyItems() {
        IsoCurrencyParser isoCurrencyParser = IsoCurrencyParser.getInstance();
        Relation<String, Data> codeList = isoCurrencyParser.getCodeList();
        Map<String, String> numericTocurrencyCode = new TreeMap<String, String>();
        StringBuffer list = new StringBuffer();

        for (Iterator<String> it = codeList.keySet().iterator(); it.hasNext();) {
            String currencyCode = it.next();
            int numericCode = -1;
            Set<Data> dataSet = codeList.getAll(currencyCode);
            boolean first = true;
            for (Data data : dataSet) {
                if (first) {
                    first = false;
                }
                numericCode = data.getNumericCode();
            }

            String strNumCode = "" + numericCode;
            String otherCode = numericTocurrencyCode.get(strNumCode);
            if (otherCode != null) {
                System.out.println("Warning: duplicate code " + otherCode +
                    "for " + numericCode);
            }
            numericTocurrencyCode.put(strNumCode, currencyCode);
            if (list.length() != 0)
                list.append(" ");
            String currencyLine = "<currencyCodes type=" + "\"" + currencyCode +
                "\"" + " numeric=" + "\"" + numericCode + "\"/>";
            list.append(currencyLine);
            System.out.println(currencyLine);

        }
        System.out.println();

    }

    /**
     *
     */
    public static void generateCurrencyItems() {
        IsoCurrencyParser isoCurrencyParser = IsoCurrencyParser.getInstance();
        Relation<String, Data> codeList = isoCurrencyParser.getCodeList();
        StringBuffer list = new StringBuffer();
        for (Iterator<String> it = codeList.keySet().iterator(); it.hasNext();) {
            // String lastField = (String) it.next();
            // String zone = (String) fullMap.get(lastField);
            String currencyCode = it.next();
            Set<Data> dataSet = codeList.getAll(currencyCode);
            boolean first = true;
            for (Data data : dataSet) {
                if (first) {
                    System.out.print(currencyCode);
                    first = false;
                }
                System.out.println("\t" + data);
            }

            if (list.length() != 0)
                list.append(" ");
            list.append(currencyCode);

        }
        System.out.println();
        String sep = CldrUtility.LINE_SEPARATOR + "\t\t\t\t";
        // "((?:[-+_A-Za-z0-9]+[/])+[A-Za-z0-9])[-+_A-Za-z0-9]*"
        String broken = CldrUtility.breakLines(list.toString(), sep, PatternCache.get(
            "([A-Z])[A-Z][A-Z]").matcher(""), 80);
        assert (list.toString().equals(broken.replace(sep, " ")));
        //System.out.println("\t\t\t<variable id=\"$currency\" type=\"choice\">"
        //    + broken + CldrUtility.LINE_SEPARATOR + "\t\t\t</variable>");
        Set<String> isoTextFileCodes = StandardCodes.make().getAvailableCodes(
            "currency");
        Set<String> temp = new TreeSet<String>(codeList.keySet());
        temp.removeAll(isoTextFileCodes);
        if (temp.size() != 0) {
            throw new IllegalArgumentException("Missing from ISO4217.txt file: " + temp);
        }
    }

    public static void genSupplementalZoneData() throws IOException {
        genSupplementalZoneData(false);
    }

    public static void genSupplementalZoneData(boolean skipUnaliased)
        throws IOException {
        RuleBasedCollator col = (RuleBasedCollator) Collator.getInstance();
        col.setNumericCollation(true);
        StandardCodes sc = StandardCodes.make();
        Map<String, String> zone_country = sc.getZoneToCounty();
        Map<String, Set<String>> country_zone = sc.getCountryToZoneSet();
        Factory cldrFactory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
        CLDRFile english = cldrFactory.make("en", true);

        writeZonePrettyPath(col, zone_country, english);
        writeMetazonePrettyPath();

        Map<String, String> old_new = sc.getZoneLinkold_new();
        Map<String, Set<String>> new_old = new TreeMap<String, Set<String>>();

        for (Iterator<String> it = old_new.keySet().iterator(); it.hasNext();) {
            String old = it.next();
            String newOne = old_new.get(old);
            Set<String> oldSet = new_old.get(newOne);
            if (oldSet == null)
                new_old.put(newOne, oldSet = new TreeSet<String>());
            oldSet.add(old);
        }
        Map<String, String> fullMap = new TreeMap<String, String>(col);
        for (Iterator<String> it = zone_country.keySet().iterator(); it.hasNext();) {
            String zone = it.next();
            String defaultName = TimezoneFormatter.getFallbackName(zone);
            Object already = fullMap.get(defaultName);
            if (already != null)
                System.out.println("CONFLICT: " + already + ", " + zone);
            fullMap.put(defaultName, zone);
        }
        // fullSet.addAll(zone_country.keySet());
        // fullSet.addAll(new_old.keySet());

        System.out
            .println("<!-- Generated by org.unicode.cldr.tool.CountItems -->");
        System.out.println("<supplementalData>");
        System.out.println("\t<timezoneData>");
        System.out.println();

        Set<String> multizone = new TreeSet<String>();
        for (Iterator<String> it = country_zone.keySet().iterator(); it.hasNext();) {
            String country = it.next();
            Set<String> zones = country_zone.get(country);
            if (zones != null && zones.size() != 1)
                multizone.add(country);
        }

        System.out.println("\t\t<zoneFormatting multizone=\""
            + toString(multizone, " ") + "\"" + " tzidVersion=\""
            + sc.getZoneVersion() + "\"" + ">");

        Set<String> orderedSet = new TreeSet<String>(col);
        orderedSet.addAll(zone_country.keySet());
        orderedSet.addAll(sc.getDeprecatedZoneIDs());
        StringBuffer tzid = new StringBuffer();

        for (Iterator<String> it = orderedSet.iterator(); it.hasNext();) {
            // String lastField = (String) it.next();
            // String zone = (String) fullMap.get(lastField);
            String zone = it.next();
            if (tzid.length() != 0)
                tzid.append(' ');
            tzid.append(zone);

            String country = zone_country.get(zone);
            if (country == null)
                continue; // skip deprecated

            Set<String> aliases = new_old.get(zone);
            if (aliases != null) {
                aliases = new TreeSet<String>(aliases);
                aliases.remove(zone);
            }
            if (skipUnaliased)
                if (aliases == null || aliases.size() == 0)
                    continue;

            System.out.println("\t\t\t<zoneItem"
                + " type=\""
                + zone
                + "\""
                + " territory=\""
                + country
                + "\""
                + (aliases != null && aliases.size() > 0 ? " aliases=\""
                    + toString(aliases, " ") + "\"" : "")
                + "/>");
        }

        System.out.println("\t\t</zoneFormatting>");
        System.out.println();
        System.out.println("\t</timezoneData>");
        System.out.println("</supplementalData>");
        System.out.println();
        String sep = CldrUtility.LINE_SEPARATOR + "\t\t\t\t";
        // "((?:[-+_A-Za-z0-9]+[/])+[A-Za-z0-9])[-+_A-Za-z0-9]*"
        String broken = CldrUtility.breakLines(tzid, sep, PatternCache.get(
            "((?:[-+_A-Za-z0-9]+[/])+[-+_A-Za-z0-9])[-+_A-Za-z0-9]*").matcher(""),
            80);
        assert (tzid.toString().equals(broken.replace(sep, " ")));
        System.out.println("\t\t\t<variable id=\"$tzid\" type=\"choice\">" + broken
            + CldrUtility.LINE_SEPARATOR + "\t\t\t</variable>");
    }

    public static void writeMetazonePrettyPath() {
        CLDRConfig testInfo = ToolConfig.getToolInstance();
        Map<String, Map<String, String>> map = testInfo.getSupplementalDataInfo().getMetazoneToRegionToZone();
        Map zoneToCountry = testInfo.getStandardCodes().getZoneToCounty();
        Set<Pair<String, String>> results = new TreeSet<Pair<String, String>>();
        Map<String, String> countryToContinent = getCountryToContinent(testInfo.getSupplementalDataInfo(),
            testInfo.getEnglish());

        for (String metazone : map.keySet()) {
            Map<String, String> regionToZone = map.get(metazone);
            String zone = regionToZone.get("001");
            if (zone == null) {
                throw new IllegalArgumentException("Missing 001 for metazone " + metazone);
            }
            String continent = zone.split("/")[0];

            final Object country = zoneToCountry.get(zone);
            results.add(new Pair<String, String>(continent + "\t" + country + "\t" + countryToContinent.get(country)
                + "\t" + metazone, metazone));
        }
        for (Pair<String, String> line : results) {
            System.out.println("'" + line.getSecond() + "'\t>\t'\t" + line.getFirst() + "\t'");
        }
    }

    private static Map<String, String> getCountryToContinent(SupplementalDataInfo supplementalDataInfo, CLDRFile english) {
        Relation<String, String> countryToContinent = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);
        Set<String> continents = new HashSet<String>(Arrays.asList("002", "019", "142", "150", "009"));
        // note: we don't need more than 3 levels
        for (String continent : continents) {
            final Set<String> subcontinents = supplementalDataInfo.getContained(continent);
            countryToContinent.putAll(subcontinents, continent);
            for (String subcontinent : subcontinents) {
                if (subcontinent.equals("EU")) continue;
                final Set<String> countries = supplementalDataInfo.getContained(subcontinent);
                countryToContinent.putAll(countries, continent);
            }
        }
        // convert to map
        Map<String, String> results = new TreeMap<String, String>();
        for (String item : countryToContinent.keySet()) {
            final Set<String> containees = countryToContinent.getAll(item);
            if (containees.size() != 1) {
                throw new IllegalArgumentException(item + "\t" + containees);
            }
            results.put(item, english.getName(CLDRFile.TERRITORY_NAME, containees.iterator().next()));
        }
        return results;
    }

    private static void writeZonePrettyPath(RuleBasedCollator col, Map<String, String> zone_country,
        CLDRFile english) throws IOException {
        System.out.println("Writing zonePrettyPath");
        Set<String> masked = new HashSet<String>();
        Map<String, String> zoneNew_Old = new TreeMap<String, String>(col);
        String lastZone = "XXX";
        for (String zone : new TreeSet<String>(zone_country.keySet())) {
            String[] parts = zone.split("/");
            String newPrefix = zone_country.get(zone); // english.getName("tzid", zone_country.get(zone),
            // false).replace(' ', '_');
            if (newPrefix.equals("001")) {
                newPrefix = "ZZ";
            }
            parts[0] = newPrefix;
            String newName;
            if (parts.length > 2) {
                System.out.println("\tMultifield: " + zone);
                if (parts.length == 3 && parts[1].equals("Argentina")) {
                    newName = parts[0] + "/" + parts[1];
                } else {
                    newName = CldrUtility.join(parts, "/");
                }
            } else {
                newName = CldrUtility.join(parts, "/");
            }
            zoneNew_Old.put(newName, zone);
            if (zone.startsWith(lastZone)) {
                masked.add(zone); // find "masked items" and do them first.
            } else {
                lastZone = zone;
            }
        }

        Log.setLog(CLDRPaths.GEN_DIRECTORY + "/supplemental/prettyPathZone.txt");
        String lastCountry = "";
        for (int i = 0; i < 2; ++i) {
            Set<String> orderedList = zoneNew_Old.keySet();
            if (i == 0) {
                Log
                    .println("# Short IDs for zone names: country code + last part of TZID");
                Log
                    .println("# First are items that would be masked, and are moved forwards and sorted in reverse order");
                Log.println();
                //Comparator c;
                Set<String> temp = new TreeSet<String>(new ReverseComparator<Object>(col));
                temp.addAll(orderedList);
                orderedList = temp;
            } else {
                Log.println();
                Log.println("# Normal items, sorted by country code");
                Log.println();
            }

            // do masked items first

            for (String newName : orderedList) {
                String oldName = zoneNew_Old.get(newName);
                if (masked.contains(oldName) != (i == 0)) {
                    continue;
                }
                String newCountry = newName.split("/")[0];
                if (!newCountry.equals(lastCountry)) {
                    Log.println("# " + newCountry + "\t"
                        + english.getName("territory", newCountry));
                    lastCountry = newCountry;
                }
                Log.println("\t'" + oldName + "'\t>\t'" + newName + "';");
            }
        }
        Log.close();
        System.out.println("Done Writing zonePrettyPath");
    }

    public static class ReverseComparator<T> implements Comparator<T> {
        Comparator<T> other;

        public ReverseComparator(Comparator<T> other) {
            this.other = other;
        }

        public int compare(T o1, T o2) {
            return other.compare(o2, o1);
        }
    }

    public static void getSubtagVariables2() throws IOException {
        Log.setLogNoBOM(CLDRPaths.GEN_DIRECTORY + "/supplemental", "supplementalMetadata.xml");
        BufferedReader oldFile = FileUtilities.openUTF8Reader(CLDRPaths.SUPPLEMENTAL_DIRECTORY, "supplementalMetadata.xml");
        CldrUtility.copyUpTo(oldFile, PatternCache.get("\\s*<!-- start of data generated with CountItems.*"),
            Log.getLog(), true);

        Map<String, String> variableSubstitutions = getVariables(VariableType.partial);
        for (Entry<String, String> type : variableSubstitutions.entrySet()) {
            Log.println(type.getValue());
        }

        // String sep = CldrUtility.LINE_SEPARATOR + "\t\t\t";
        // String broken = CldrUtility.breakLines(CldrUtility.join(defaultLocaleContent," "), sep,
        // PatternCache.get("(\\S)\\S*").matcher(""), 80);
        //
        // Log.println("\t\t<defaultContent locales=\"" + broken + "\"");
        // Log.println("\t\t/>");

        // Log.println("</supplementalData>");
        CldrUtility.copyUpTo(oldFile, PatternCache.get("\\s<!-- end of data generated by CountItems.*"), null, true);
        CldrUtility.copyUpTo(oldFile, null, Log.getLog(), true);

        Log.close();
        oldFile.close();
    }

    static final SupplementalDataInfo supplementalData = SupplementalDataInfo
        .getInstance(CLDRPaths.SUPPLEMENTAL_DIRECTORY);
    static final StandardCodes sc = StandardCodes.make();

    public static void getSubtagVariables() {
//        This section no longer necessary, as it has been replaced by the new attributeValueValidity.xml
//
//        System.out.println("Validity variables");
//        System.out.println("Cut/paste into supplementalMetadata.xml under the line");
//        System.out.println("<!-- start of data generated with CountItems tool ...");
//        Map<String, String> variableSubstitutions = getVariables(VariableType.partial);

//        for (Entry<String, String> type : variableSubstitutions.entrySet()) {
//            System.out.println(type.getValue());
//        }
//        System.out.println("<!-- end of Validity variables generated with CountItems tool ...");
//        System.out.println();
        System.out.println("Language aliases");
        System.out.println("Cut/paste into supplementalMetadata.xml under the line");
        System.out.println("<!-- start of data generated with CountItems tool ...");

        Map<String, Map<String, String>> languageReplacement = StandardCodes.getLStreg().get("language");
        Map<String, Map<String, R2<List<String>, String>>> localeAliasInfo = supplementalData.getLocaleAliasInfo();
        Map<String, R2<List<String>, String>> languageAliasInfo = localeAliasInfo.get("language");

        Set<String> available = Iso639Data.getAvailable();
        // <languageAlias type="aju" replacement="jrb"/> <!-- Moroccan Judeo-Arabic ⇒ Judeo-Arabic -->
        Set<String> bad3letter = new HashSet<String>();
        for (String lang : available) {
            if (lang.length() != 2) continue;
            String target = lang;
            Map<String, String> lstregData = languageReplacement.get(lang);
            if (lstregData == null) {
                throw new IllegalArgumentException("illegal language code");
            } else {
                String replacement = lstregData.get("Preferred-Value");
                if (replacement != null) {
                    target = replacement;
                }
            }
            String alpha3 = Iso639Data.toAlpha3(lang);
            bad3letter.add(alpha3);
            String targetAliased;
            if (languageAliasInfo.containsKey(target)) {
                targetAliased = CollectionUtilities.join(languageAliasInfo.get(target).get0(), " ");
            } else {
                targetAliased = target;
            }
            System.out.println("\t\t\t<languageAlias type=\"" + alpha3 + "\" replacement=\"" + targetAliased
                + "\" reason=\"overlong\"/> <!-- " +
                Iso639Data.getNames(target) + " -->");
        }
        System.out.println("\t\t\t<!-- Bibliographic -->");
        TreeMap<String, String> sorted = new TreeMap<>();
        for (String hasBiblio : Iso639Data.hasBiblio3()) {
            String biblio = Iso639Data.toBiblio3(hasBiblio);
            sorted.put(biblio, hasBiblio);
        }
        for (Entry<String, String> entry : sorted.entrySet()) {
            String biblio = entry.getKey();
            String hasBiblio = entry.getValue();
            System.out.println("\t\t\t<languageAlias type=\"" + biblio + "\" replacement=\"" + hasBiblio
                + "\" reason=\"bibliographic\"/> <!-- " +
                Iso639Data.getNames(hasBiblio) + " -->");
        }
        System.out.println("<!-- end of Language alises generated with CountItems tool ...");

        Set<String> encompassed = Iso639Data.getEncompassed();
        Set<String> macros = Iso639Data.getMacros();
        Map<String, String> encompassed_macro = new HashMap<String, String>();
        for (Entry<String, R2<List<String>, String>> typeAndData : languageAliasInfo.entrySet()) {
            String type = typeAndData.getKey();
            R2<List<String>, String> data = typeAndData.getValue();
            List<String> replacements = data.get0();
            if (!encompassed.contains(type)) continue;
            if (replacements == null || replacements.size() != 1) continue;
            String replacement = replacements.get(0);
            if (macros.contains(replacement)) {
                // we have a match, encompassed => replacement
                encompassed_macro.put(type, replacement);
            }
        }
        Set<String> missing = new TreeSet<String>();
        missing.addAll(macros);
        missing.remove("no");
        missing.remove("sh");

        missing.removeAll(encompassed_macro.values());
        if (missing.size() != 0) {
            for (String missingMacro : missing) {
                System.err.println("ERROR: Missing <languageAlias type=\"" + "???" + "\" replacement=\"" + missingMacro
                    + "\"/> <!-- ??? => " +
                    Iso639Data.getNames(missingMacro) + " -->");
                System.out.println("\tOptions for ???:");
                for (String enc : Iso639Data.getEncompassedForMacro(missingMacro)) {
                    System.out.println("\t" + enc + "\t// " + Iso639Data.getNames(enc));
                }
            }
        }
        // verify that every macro language has a encompassed mapping to it
        // and remember those codes

        // verify that nobody contains a bad code

        for (Entry<String, R2<List<String>, String>> typeAndData : languageAliasInfo.entrySet()) {
            String type = typeAndData.getKey();
            List<String> replacements = typeAndData.getValue().get0();
            if (replacements == null) continue;
            for (String replacement : replacements) {
                if (bad3letter.contains(replacement)) {
                    System.err.println("ERROR: Replacement(s) for type=\"" + type +
                        "\" contains " + replacement + ", which should be: " + Iso639Data.fromAlpha3(replacement));
                }
            }
        }

        // get the bad ISO codes

        Factory cldrFactory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
        CLDRFile english = cldrFactory.make("en", true);

        Set<String> territories = new TreeSet<String>();
        Relation<String, String> containers = supplementalData.getTerritoryToContained();
        for (String region : sc.getAvailableCodes("territory")) {
            if (containers.containsKey(region)) continue;
            territories.add(region);
        }
        System.out.println();
        System.out.println("Territory aliases");
        System.out.println("Cut/paste into supplementalMetadata.xml under the line");
        System.out.println("<!-- start of data generated with CountItems tool ...");
        //final Map<String, R2<List<String>, String>> territoryAliasInfo = localeAliasInfo.get("territory");

        addRegions(english, territories, "alpha3", "EA,EU,IC".split(","), new Transform<String, String>() {
            public String transform(String region) {
                return IsoRegionData.get_alpha3(region);
            }
        });
        addRegions(english, territories, "numeric", "AC,CP,DG,EA,EU,IC,TA".split(","), new Transform<String, String>() {
            public String transform(String region) {
                return IsoRegionData.getNumeric(region);
            }
        });
        System.out.println("<!-- end of Territory alises generated with CountItems tool ...");
        System.out.println();
        System.out.println("Deprecated codes check (informational)");
        // check that all deprecated codes are in fact deprecated
        Map<String, Map<String, Map<String, String>>> fullData = StandardCodes.getLStreg();

        checkCodes("language", sc, localeAliasInfo, fullData);
        checkCodes("script", sc, localeAliasInfo, fullData);
        checkCodes("territory", sc, localeAliasInfo, fullData);
        System.out.println("End of Deprecated codes check...");

        // generate mapping equivalences
        // { "aar", "aar", "aa" }, // Afar
        // b, t, bcp47
        System.out.println();
        System.out.println("Mapping equivalences - (Informational only...)");
        System.out.println("{ bib , tech , bcp47 }");

        Set<R3<String, String, String>> rows = new TreeSet<R3<String, String, String>>();
        for (String lang : Iso639Data.getAvailable()) {
            String bib = Iso639Data.toBiblio3(lang);
            String tech = Iso639Data.toAlpha3(lang);
            R3<String, String, String> row = Row.of(tech, bib, lang);
            rows.add(row);
        }
        for (R3<String, String, String> row : rows) {
            String tech = row.get0();
            String bib = row.get1();
            String lang = row.get2();
            String name = Iso639Data.getNames(lang).iterator().next(); // english.getName(lang);
            if ((bib != null && !lang.equals(bib)) || (tech != null && !lang.equals(tech))) {
                System.out.println("  { \"" + bib + "\", \"" + tech + "\", \"" + lang + "\" },  // " + name);
            }
        }
        System.out.println("End of Mapping equivalences...");

        // generate the codeMappings
        // <codeMappings>
        // <territoryCodes type="CS" numeric="891" alpha3="SCG" fips10="YI"/>

        System.out.println();
        System.out.println("Code Mappings");
        System.out.println("Cut/paste into supplementaData.xml under the line");
        System.out.println("<!-- start of data generated with CountItems tool ...");
        List<String> warnings = new ArrayList<String>();
        territories.add("QO");
        territories.add("EU");
        // territories.add("MF");
        //Map<String, R2<List<String>, String>> territoryAliases = supplementalData.getLocaleAliasInfo().get("territory");
        Relation<String, String> numeric2region = Relation.of(new HashMap<String, Set<String>>(), TreeSet.class);
        Relation<String, String> alpha32region = Relation.of(new HashMap<String, Set<String>>(), TreeSet.class);
        for (String region : territories) {
            String numeric = IsoRegionData.getNumeric(region);
            String alpha3 = IsoRegionData.get_alpha3(region);
            numeric2region.put(numeric, region);
            alpha32region.put(alpha3, region);
        }

        System.out.println("    <codeMappings>");

        for (String region : territories) {
            String numeric = IsoRegionData.getNumeric(region);
            String alpha3 = IsoRegionData.get_alpha3(region);
            String fips10 = IsoRegionData.get_fips10(region);
            System.out.println("        <territoryCodes"
                + " type=\"" + region + "\""
                + (numeric == null ? "" : " numeric=\"" + numeric + "\"")
                + (alpha3 == null ? "" : " alpha3=\"" + alpha3 + "\"")
                + (fips10 == null || fips10.equals(region) ? "" : " fips10=\"" + fips10 + "\"")
                + "/>");
        }
        System.out.println("    </codeMappings>");
        System.out.println("<!-- end of Code Mappings generated with CountItems tool ...");
        System.out.println(CollectionUtilities.join(warnings, CldrUtility.LINE_SEPARATOR));
    }

    enum VariableType {
        full, partial
    }

    public static Map<String, String> getVariables(VariableType variableType) {
        String sep = CldrUtility.LINE_SEPARATOR + "\t\t\t\t";
        Map<String, String> variableSubstitutions = new LinkedHashMap<String, String>();
        for (String type : new String[] { "grandfathered", "territory", "script", "variant" }) {
            Set<String> i;
            i = (variableType == VariableType.full || type.equals("grandfathered")) ? sc.getAvailableCodes(type) : sc.getGoodAvailableCodes(type);
            addVariable(variableSubstitutions, type, i, sep);
        }

        Relation<String, String> bcp47Keys = supplementalData.getBcp47Keys();
        Relation<R2<String, String>, String> aliases = supplementalData.getBcp47Aliases();
        for (String key : bcp47Keys.keySet()) {
            Set<String> keyAliases = aliases.getAll(Row.of(key, ""));
            Set<String> rawsubtypes = bcp47Keys.getAll(key);
            TreeSet<String> subtypes = new TreeSet<String>();
            for (String subtype : rawsubtypes) {
                Set<String> keySubtypeAliases = aliases.getAll(Row.of(key, subtype));
                if (keySubtypeAliases != null) {
                    subtypes.addAll(keySubtypeAliases);
                }
            }
            subtypes.addAll(rawsubtypes);
            String alias = (keyAliases == null ? key : keyAliases.iterator().next()) + "_XXX";
            addVariable(variableSubstitutions, alias, subtypes, sep);
        }
        return variableSubstitutions;
    }

    private static final Pattern BreakerPattern = PatternCache.get("([-_A-Za-z0-9])[-/+_A-Za-z0-9]*");

    private static void addVariable(Map<String, String> variableSubstitutions, String type, Set<String> sinput,
        String sep) {
        TreeSet<String> s = new TreeSet<String>(ROOT_PRIMARY_COLLATOR);
        s.addAll(sinput);

        StringBuffer b = new StringBuffer();
        for (String code : s) {
            if (b.length() != 0)
                b.append(' ');
            b.append(code);
        }
        // "((?:[-+_A-Za-z0-9]+[/])+[A-Za-z0-9])[-+_A-Za-z0-9]*"
        String broken = CldrUtility.breakLines(b, sep, BreakerPattern.matcher(""), 80);
        assert (b.toString().equals(broken.replace(sep, " ")));
        variableSubstitutions.put(type, "\t\t\t<variable id=\"$" + type
            + "\" type=\"choice\">" + broken + CldrUtility.LINE_SEPARATOR + "\t\t\t</variable>");
    }

    private static void checkCodes(String type, StandardCodes sc,
        Map<String, Map<String, R2<List<String>, String>>> localeAliasInfo,
        Map<String, Map<String, Map<String, String>>> fullData) {
        Map<String, Map<String, String>> typeData = fullData.get("territory".equals(type) ? "region" : type);
        Map<String, R2<List<String>, String>> aliasInfo = localeAliasInfo.get(type);
        for (String code : sc.getAvailableCodes(type)) {
            Map<String, String> subdata = typeData.get(code);
            String deprecated = subdata.get("Deprecated");
            if (deprecated == null) continue;
            String replacement = subdata.get("Preferred-Value");
            R2<List<String>, String> supplementalReplacements = aliasInfo.get(code);
            if (supplementalReplacements == null) {
                System.out.println("Deprecated in LSTR, but not in supplementalData: " + type + "\t" + code + "\t"
                    + replacement);
            }
        }
    }

    private static void addRegions(CLDRFile english, Set<String> availableCodes, String codeType, String[] exceptions,
        Transform<String, String> trans) {
        Set<String> missingRegions = new TreeSet<String>();
        Set<String> exceptionSet = new HashSet<String>(Arrays.asList(exceptions));
        List<String> duplicateDestroyer = new ArrayList<String>();
        for (String region : availableCodes) {

            if (exceptionSet.contains(region)) continue;
            String alpha3 = trans.transform(region);
            if (alpha3 == null) {
                missingRegions.add(region);
                continue;
            }
            Map<String, R2<List<String>, String>> territoryAliasInfo = supplementalData.getLocaleAliasInfo().get("territory");
            String result;
            if (territoryAliasInfo.containsKey(region)) {
                result = CollectionUtilities.join(territoryAliasInfo.get(region).get0(), " ");
            } else {
                result = region;
            }
            String name = english.getName(CLDRFile.TERRITORY_NAME, result);
            if (!(duplicateDestroyer.contains(alpha3 + result + name))) {
                duplicateDestroyer.add(alpha3 + result + name);
                System.out.println("\t\t\t<territoryAlias type=\"" + alpha3 + "\" replacement=\"" + result
                    + "\" reason=\"overlong\"/> <!-- " + name + " -->");
            }
        }
        for (String region : missingRegions) {
            String name = english.getName(CLDRFile.TERRITORY_NAME, region);
            System.err.println("ERROR: Missing " + codeType + " code for " + region + "\t" + name);
        }
    }

    /**
     *
     */
    private static String toString(Collection aliases, String separator) {
        StringBuffer result = new StringBuffer();
        boolean first = true;
        for (Iterator<Object> it = aliases.iterator(); it.hasNext();) {
            Object item = it.next();
            if (first)
                first = false;
            else
                result.append(separator);
            result.append(item);
        }
        return result.toString();
    }

    public static void showZoneInfo() throws IOException {
        StandardCodes sc = StandardCodes.make();
        Map<String, String> m = sc.getZoneLinkold_new();
        int i = 0;
        System.out.println("/* Generated by org.unicode.cldr.tool.CountItems */");
        System.out.println();
        i = 0;
        System.out.println("/* zoneID, canonical zoneID */");
        for (Iterator<String> it = m.keySet().iterator(); it.hasNext();) {
            String old = it.next();
            String newOne = m.get(old);
            System.out.println("{\"" + old + "\", \"" + newOne + "\"},");
            ++i;
        }
        System.out.println("/* Total: " + i + " */");

        System.out.println();
        i = 0;
        System.out.println("/* All canonical zoneIDs */");
        for (Iterator<String> it = sc.getZoneData().keySet().iterator(); it.hasNext();) {
            String old = it.next();
            System.out.println("\"" + old + "\",");
            ++i;
        }
        System.out.println("/* Total: " + i + " */");

        Factory mainCldrFactory = Factory.make(CLDRPaths.COMMON_DIRECTORY + "main"
            + File.separator, ".*");
        CLDRFile desiredLocaleFile = mainCldrFactory.make("root", true);
        String temp = desiredLocaleFile
            .getFullXPath("//ldml/dates/timeZoneNames/singleCountries");
        String singleCountriesList = new XPathParts(null, null).set(temp)
            .findAttributes("singleCountries").get("list");
        Set<String> singleCountriesSet = new TreeSet<String>(CldrUtility.splitList(singleCountriesList,
            ' '));

        Map<String, String> zone_countries = StandardCodes.make().getZoneToCounty();
        Map<String, Set<String>> countries_zoneSet = StandardCodes.make().getCountryToZoneSet();
        System.out.println();
        i = 0;
        System.out.println("/* zoneID, country, isSingle */");
        for (Iterator<String> it = zone_countries.keySet().iterator(); it.hasNext();) {
            String old = it.next();
            String newOne = zone_countries.get(old);
            Set<String> s = countries_zoneSet.get(newOne);
            String isSingle = (s != null && s.size() == 1 || singleCountriesSet
                .contains(old)) ? "T" : "F";
            System.out.println("{\"" + old + "\", \"" + newOne + "\", \"" + isSingle
                + "\"},");
            ++i;
        }
        System.out.println("/* Total: " + i + " */");

        if (true)
            return;

        Factory cldrFactory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
        Map<Organization, Map<String, Level>> platform_locale_status = StandardCodes.make().getLocaleTypes();
        Map<String, Level> onlyLocales = platform_locale_status.get(Organization.ibm);
        Set<String> locales = onlyLocales.keySet();
        CLDRFile english = cldrFactory.make("en", true);
        for (Iterator<String> it = locales.iterator(); it.hasNext();) {
            String locale = it.next();
            System.out.println(locale + "\t" + english.getName(locale) + "\t"
                + onlyLocales.get(locale));
        }
    }

    static final NumberFormat decimal = NumberFormat.getNumberInstance();
    static {
        decimal.setGroupingUsed(true);
    }

    public static void countItems() {
        // CLDRKey.main(new String[]{"-mde.*"});
        String dir = CldrUtility.getProperty("source", CLDRPaths.MAIN_DIRECTORY);
        Factory cldrFactory = Factory.make(dir, ".*");
        countItems(cldrFactory, false);
    }

    /**
     * @param cldrFactory
     * @param resolved
     */
    private static int countItems(Factory cldrFactory, boolean resolved) {
        int count = 0;
        int resolvedCount = 0;
        Set<String> locales = cldrFactory.getAvailable();
        Set<String> keys = new HashSet<String>();
        Set<String> values = new HashSet<String>();
        Set<String> fullpaths = new HashSet<String>();
        Matcher alt = CLDRFile.ALT_PROPOSED_PATTERN.matcher("");

        Set<String> temp = new HashSet<String>();
        for (Iterator<String> it = locales.iterator(); it.hasNext();) {
            String locale = it.next();
            if (CLDRFile.isSupplementalName(locale))
                continue;
            CLDRFile item = cldrFactory.make(locale, false);

            temp.clear();
            for (Iterator<String> it2 = item.iterator(); it2.hasNext();) {
                String path = it2.next();
                if (alt.reset(path).matches()) {
                    continue;
                }
                temp.add(path);
                keys.add(path);
                values.add(item.getStringValue(path));
                fullpaths.add(item.getFullXPath(path));
            }
            int current = temp.size();

            CLDRFile itemResolved = cldrFactory.make(locale, true);
            temp.clear();
            CollectionUtilities.addAll(itemResolved.iterator(), temp);
            int resolvedCurrent = temp.size();

            System.out.println(locale + "\tPlain:\t" + current + "\tResolved:\t"
                + resolvedCurrent + "\tUnique Paths:\t" + keys.size()
                + "\tUnique Values:\t" + values.size() + "\tUnique Full Paths:\t"
                + fullpaths.size());
            count += current;
            resolvedCount += resolvedCurrent;
        }
        System.out.println("Total Items\t" + decimal.format(count));
        System.out
            .println("Total Resolved Items\t" + decimal.format(resolvedCount));
        System.out.println("Unique Paths\t" + decimal.format(keys.size()));
        System.out.println("Unique Values\t" + decimal.format(values.size()));
        System.out
            .println("Unique Full Paths\t" + decimal.format(fullpaths.size()));
        return count;
    }

}
