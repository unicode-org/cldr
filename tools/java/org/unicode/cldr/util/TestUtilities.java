/*
 **********************************************************************
 * Copyright (c) 2002-2004, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */

package org.unicode.cldr.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.tool.GenerateAttributeList;

import com.google.common.base.Joiner;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedBreakIterator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UniversalTimeScale;

/**
 * @author davis
 *
 *         TODO To change the template for this generated type comment go to Window -
 *         Preferences - Java - Code Style - Code Templates
 */
public class TestUtilities {
    enum State {
        a, b, c;
        public static State cc = c;
    }

    String s;

    public static void main(String[] args) throws Exception {
        try {
            checkStandardCodes();
            if (true) return;
            testExampleGenerator();
            for (String lang : Iso639Data.getAvailable()) {
                String biblio = Iso639Data.toBiblio3(lang);
                if (biblio == null) continue;
                String alpha = Iso639Data.toAlpha3(lang);
                if (!biblio.equals(alpha)) {
                    System.out.println(lang + "\t\t" + biblio + "\t\t" + alpha);
                }
            }
            System.out.println(State.a + ", " + State.b + ", " + State.c + ", " + State.cc);

            ULocale myLocale = null;
            String string1 = null, string2 = null;
            RuleBasedCollator col = (RuleBasedCollator) Collator.getInstance(myLocale);
            col.setNumericCollation(true);
            col.compare(string1, string2); // compare strings
            col.getRawCollationKey(string1, null); // get sort key (for indexing)

            testNames();
            testExampleGenerator();
            if (true)
                return;
            checkNumericTimezone();

            long foo = UniversalTimeScale.from(new Date().getTime(), UniversalTimeScale.JAVA_TIME);
            System.out.println("Current Universal Time: " + Long.toString(foo, 16));
            System.out.println("LVT_Syllable count: " + new UnicodeSet("[:Hangul_Syllable_Type=LVT_Syllable:]").size());
            System.out.println("LV_Syllable count: " + new UnicodeSet("[:Hangul_Syllable_Type=LV_Syllable:]").size());
            System.out.println("AC00 value: "
                + UCharacter.getIntPropertyValue('\uAC00', UProperty.HANGUL_SYLLABLE_TYPE));
            // checkTranslit();
            // writeMetaData();
            // testXMLFileReader();
            // testBreakIterator("&#x61;\n&#255;&#256;");

            // checkLanguages();
            // printCountries();
            // printZoneSamples();
            // printCurrencies();
        } finally {
            System.out.println("Done");
        }
    }

    private static void testNames() {
        Factory mainCldrFactory = Factory.make(CLDRPaths.COMMON_DIRECTORY + "main" + File.separator, ".*");
        CLDRFile english = mainCldrFactory.make("en", true);
        CLDRFile french = mainCldrFactory.make("fr", true);
        String[] tests = { "en", "en_AU", "de_CH", "de_Arab_CH", "gsw", "gsw_Arab", "zh_Hans", "zh_Hans_US",
            "zh_Hans_US_SAAHO" };
        for (String test : tests) {
            System.out.println(test + "\t" + english.getName(test) + "\t" + french.getName(test));
        }
    }

    private static void testExampleGenerator() throws IOException {
        System.out.println("Creating English CLDRFile");
        Factory mainCldrFactory = Factory.make(CLDRPaths.COMMON_DIRECTORY + "main" + File.separator, ".*");
        CLDRFile english = mainCldrFactory.make("en", true);
        System.out.println("Creating Example Generator");
        ExampleGenerator englishExampleGenerator = new ExampleGenerator(english, english,
            CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);
        // invoke once
        System.out.println("Processing paths");
        StringBuilder result = new StringBuilder();
        Relation<String, String> message_paths = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);
        for (String path : english) {
            String value = english.getStringValue(path);
            result.setLength(0);
            String examples = englishExampleGenerator.getExampleHtml(path, value);
            if (examples != null) {
                result.append(examples).append("<hr>");
            }
            String helpText = englishExampleGenerator.getHelpHtml(path, "@");
            if (helpText != null) {
                result.append(helpText).append("<hr>");
            } else {
                System.out.println("No help phrase for " + path);
            }
            if (result.length() != 0) {
                message_paths.put(result.toString(), path + "\t:\t" + value);
            } else {
                message_paths.put("\uFFFD<b>NO MESSAGE</b><hr>", path + "\t:\t" + value);
            }
        }
        PrintWriter out = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY + "test/", "test_examples.html");
        out.println("<html><body><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
        for (String message : message_paths.keySet()) {
            Set<String> paths = message_paths.getAll(message);
            out.println("<p>");
            out.println(CldrUtility.join(paths, "<br>" + CldrUtility.LINE_SEPARATOR));
            out.println("</p><blockquote>");
            out.println(message);
            out.println("</blockquote>");
        }
        out.println(CldrUtility.ANALYTICS);
        out.println("</body></html>");
        out.close();
    }

    private static void checkNumericTimezone() throws IOException {
        String[] map_integer_zones = new String[1000];
        StandardCodes sc = StandardCodes.make();
        Set<String> timezones = new TreeSet<>(sc.getGoodAvailableCodes("tzid"));
        Map<String, Integer> map_timezone_integer = new TreeMap<>();
        BufferedReader input = CldrUtility.getUTF8Data("timezone_numeric.txt");
        int maxNumeric = -1;
        Map<String, String> fixOld = sc.zoneParser.getZoneLinkold_new();
        while (true) {
            String line = input.readLine();
            if (line == null)
                break;
            String[] parts = line.split(";\\s*");
            int numeric = Integer.parseInt(parts[0]);
            String originalTzid = parts[1].trim();
            String fixedID = fixOld.get(originalTzid);
            if (fixedID == null) {
                if (!timezones.contains(originalTzid)) {
                    System.out.println(numeric + "\t" + originalTzid + "\tStrange ID: " + fixedID);
                }
                fixedID = originalTzid;
            } else {
                System.out.println("Replacing " + originalTzid + " with " + fixedID);
            }
            if (map_integer_zones[numeric] != null) {
                System.out.println("Duplicate number:" + numeric + ",\t" + fixedID + ",\t" + originalTzid + ",\t"
                    + map_integer_zones[numeric]);
                fixedID = "{" + originalTzid + "}";
            }
            if (map_timezone_integer.get(fixedID) != null) {
                System.out.println("Duplicate zone:" + numeric + ",\t" + fixedID + ",\t" + originalTzid + ",\t"
                    + map_timezone_integer.get(fixedID));
                fixedID = "{" + originalTzid + "}";
            }
            map_integer_zones[numeric] = fixedID;
            map_timezone_integer.put(fixedID, new Integer(numeric));
            if (maxNumeric < numeric)
                maxNumeric = numeric;
        }
        // get the differences (and sort them)
        RuleBasedCollator eng = (RuleBasedCollator) Collator.getInstance();
        eng.setNumericCollation(true);

        Set<String> extra = new TreeSet<>(eng);
        extra.addAll(map_timezone_integer.keySet());
        extra.removeAll(timezones);
        System.out.println("Extra: " + extra);
        Set<String> needed = new TreeSet<>(eng);
        needed.addAll(timezones);
        needed.removeAll(map_timezone_integer.keySet());
        System.out.println("Needed: " + needed);

        // fill in the slots with the missing items
        // make Etc/GMT go first
        int numeric = 1;
        List<String> ordered = new ArrayList<>(needed);
        // if (ordered.contains("Etc/GMT")) {
        // ordered.remove("Etc/GMT");
        // ordered.add(0,"Etc/GMT");
        // }

        for (String tzid : ordered) {
            while (map_integer_zones[numeric] != null)
                ++numeric; // find first free one
            if (maxNumeric < numeric)
                maxNumeric = numeric;
            map_integer_zones[numeric] = tzid;
            map_timezone_integer.put(tzid, new Integer(numeric));
        }

        // print it out
        Map<String, Set<String>> equiv = sc.zoneParser.getZoneLinkNew_OldSet();
        Set<String> old = new TreeSet<>();
        for (int i = 1; i <= maxNumeric; ++i) {
            Set<String> s = equiv.get(map_integer_zones[i]);
            if (s != null) {
                old.clear();
                old.addAll(s);
            }
            System.out.println("\t\"" + map_integer_zones[i] + "\",");
        }
    }

    private static void checkTranslit() {

        for (int i = 0; i < 0xFFFF; ++i) {
            checkTranslit(UTF16.valueOf(i));
        }
        PrintStream out = System.out;
        Transliterator toHTML = TransliteratorUtilities.toHTML;
        UnicodeSet a_out = new UnicodeSet("[:whitespace:]");
        for (UnicodeSetIterator it = new UnicodeSetIterator(a_out); it.next();) {
            int s = it.codepoint;
            String literal = toHTML.transliterate(UTF16.valueOf(s));
            out.println(com.ibm.icu.impl.Utility.hex(s, 4) + " (" + literal + ") " + UCharacter.getName(s));
        }
    }

    private static void checkTranslit(String string) {
        String html = TransliteratorUtilities.toHTML.transliterate(string);
        String reverse = TransliteratorUtilities.fromHTML.transliterate(html);
        if (!reverse.equals(string))
            System.out
                .println(string + "\t=>\t" + html + "\t=>\t" + reverse + (!reverse.equals(string) ? " FAIL" : ""));
        String htmlAscii = TransliteratorUtilities.toHTMLAscii.transliterate(string);
        String reverseAscii = TransliteratorUtilities.fromHTML.transliterate(htmlAscii);
        if (!reverseAscii.equals(string))
            System.out.println(string + "\t=>\t" + htmlAscii + "\t=>\t" + reverseAscii
                + (!reverseAscii.equals(string) ? " FAIL" : ""));
    }

    private static void writeMetaData() throws IOException {
        CLDRFile meta = SimpleFactory.makeFile("metaData").setNonInheriting(true);
        String[] elements = new String[] { "ldml", "identity", "alias", "localeDisplayNames", "layout", "characters",
            "delimiters", "measurement", "dates", "numbers", "collations", "posix",
            "segmentations", "references", "version", "generation", "language", "script", "territory", "variant",
            "languages", "scripts", "territories", "variants", "keys", "types",
            "measurementSystemNames", "key", "type", "measurementSystemName", "orientation", "inList",
            "exemplarCharacters", "mapping", "quotationStart", "quotationEnd", "alternateQuotationStart",
            "alternateQuotationEnd", "measurementSystem", "paperSize", "height", "width", "localizedPatternChars",
            "calendars", "timeZoneNames", "months", "monthNames", "monthAbbr", "days", "dayNames",
            "dayAbbr", "quarters", "week", "am", "pm", "eras", "dateFormats", "timeFormats", "dateTimeFormats",
            "fields", "month", "day", "quarter", "minDays", "firstDay", "weekendStart", "weekendEnd",
            "eraNames", "eraAbbr", "era", "pattern", "displayName", "dateFormatItem", "appendItem", "hourFormat",
            "hoursFormat", "gmtFormat", "regionFormat", "fallbackFormat", "abbreviationFallback",
            "preferenceOrdering", "singleCountries", "default", "calendar", "monthContext", "monthWidth", "dayContext",
            "dayWidth", "quarterContext", "quarterWidth", "dateFormatLength", "dateFormat",
            "timeFormatLength", "timeFormat", "dateTimeFormatLength", "availableFormats", "appendItems",
            "dateTimeFormat", "zone", "metazone", "long", "short", "usesMetazone", "exemplarCity", "generic",
            "standard", "daylight", "field", "relative", "symbols", "decimalFormats", "scientificFormats",
            "percentFormats", "currencyFormats", "currencies", "decimalFormatLength", "decimalFormat",
            "scientificFormatLength", "scientificFormat", "percentFormatLength", "percentFormat", "currencySpacing",
            "currencyFormatLength", "beforeCurrency", "afterCurrency", "currencyMatch",
            "surroundingMatch", "insertBetween", "currencyFormat", "currency", "symbol", "decimal", "group", "list",
            "percentSign", "nativeZeroDigit", "patternDigit", "plusSign", "minusSign",
            "exponential", "perMille", "infinity", "nan", "collation", "messages", "yesstr", "nostr", "yesexpr",
            "noexpr", "segmentation", "variables", "segmentRules", "special", "variable", "rule",
            "comment",
            // collation
            "base", "settings", "suppress_contractions", "optimize", "rules" };
        String list = String.join(" ", elements);
        String prefix = "//supplementalData[@version=\"1.4\"]/metaData/";
        meta.add(prefix + "elementOrder", list);

        String[] attOrder = new String[] { "_q",
            "type",
            // always after
            "key", "registry", "source", "target", "path", "day", "date", "version", "count", "lines", "characters",
            "before", "from", "to", "number", "time", "casing", "list", "uri", "iso4217",
            "digits", "rounding", "iso3166", "hex", "id", "request", "direction",
            // collation stuff
            "alternate", "backwards", "caseFirst", "caseLevel", "hiraganaQuarternary", "hiraganaQuaternary",
            "normalization", "numeric", "strength",
            // always near the end
            "validSubLocales", "standard", "references", "elements", "element", "attributes", "attribute",
            // these are always at the end
            "alt", "draft", };
        meta.add(prefix + "attributeOrder", String.join(" ", attOrder));

        String[] serialElements = new String[] { "variable", "comment",
            "tRule",
            // collation
            "reset", "p", "pc", "s", "sc", "t", "tc", "i", "ic", "x", "extend", "first_variable", "last_variable",
            "first_tertiary_ignorable", "last_tertiary_ignorable",
            "first_secondary_ignorable", "last_secondary_ignorable", "first_primary_ignorable",
            "last_primary_ignorable", "first_non_ignorable", "last_non_ignorable", "first_trailing", "last_trailing" };
        meta.add(prefix + "serialElements", String.join(" ", serialElements));
        /*
         *
         * <attributeValues elements="weekendStart weekendEnd" attributes="day"
         * order="given"> sun mon tue wed thu fri sat</attributeValues>
         *
         * if (attribute.equals("day")) { // && (element.startsWith("weekend") comp =
         * dayValueOrder; } else if (attribute.equals("type")) {
         *
         * else if (element.equals("day")) comp = dayValueOrder;
         *
         * else if (element.equals("zone")) comp = zoneOrder;
         */
        Factory cldrFactory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
        GenerateAttributeList attributes = new GenerateAttributeList(cldrFactory);
        Map<String, Map<String, Set<String>[]>> element_attribute_valueSet = attributes.getElement_attribute_valueSet();
        for (Iterator<String> it = element_attribute_valueSet.keySet().iterator(); it.hasNext();) {
            String element = it.next();
            Map<String, Set<String>[]> attribute_valueSet = element_attribute_valueSet.get(element);
            int size = attribute_valueSet.size();
            if (size == 0)
                continue;
            for (Iterator<String> it2 = attribute_valueSet.keySet().iterator(); it2.hasNext();) {
                String attribute = it2.next();
                Set<String>[] valueSets = attribute_valueSet.get(attribute);
                for (int i = 0; i < 2; ++i) {
                    meta.add(prefix + "valid/attributeValues" + "[@elements=\"" + element + "\"]" + "[@attributes=\""
                        + attribute + "\"]" + (i == 1 ? "[@x=\"true\"]" : ""),
                        Joiner.on(" ").join(valueSets[i]));
                }
            }
        }

        String[] dayValueOrder = new String[] { "sun", "mon", "tue", "wed", "thu", "fri", "sat" };
        meta.add(prefix + "valid/attributeValues[@order=\"given\"][@attributes=\"type\"][@elements=\"" + "day" + "\"]",
            String.join(" ", dayValueOrder));
        meta.add(prefix + "valid/attributeValues[@order=\"given\"][@attributes=\"" + "day" + "\"][@elements=\""
            + "firstDay weekendEnd weekendStart" + "\"]", String.join(" ", dayValueOrder));

        String[] widths = { "monthWidth", "dayWidth", "quarterWidth" };
        String[] widthOrder = new String[] { "abbreviated", "narrow", "wide" };
        meta.add(prefix + "valid/attributeValues[@order=\"given\"][@attributes=\"type\"][@elements=\""
            + String.join(" ", widths) + "\"]", String.join(" ", widthOrder));

        String[] formatLengths = { "dateFormatLength", "timeFormatLength", "dateTimeFormatLength",
            "decimalFormatLength", "scientificFormatLength", "percentFormatLength", "currencyFormatLength" };
        String[] lengthOrder = new String[] { "full", "long", "medium", "short" };
        meta.add(prefix + "valid/attributeValues[@order=\"given\"][@attributes=\"type\"][@elements=\""
            + String.join(" ", formatLengths) + "\"]", String.join(" ", lengthOrder));

        String[] dateFieldOrder = new String[] { "era", "year", "month", "week", "day", "weekday", "dayperiod", "hour",
            "minute", "second", "zone" };
        meta.add(prefix + "valid/attributeValues[@order=\"given\"][@attributes=\"type\"][@elements=\"field\"]",
            String.join(" ", dateFieldOrder));

        String[][] suppressData = { { "ldml", "version", "*" }, { "orientation", "characters", "left-to-right" },
            { "orientation", "lines", "top-to-bottom" }, { "weekendStart", "time", "00:00" },
            { "weekendEnd", "time", "24:00" }, { "dateFormat", "type", "standard" },
            { "timeFormat", "type", "standard" }, { "dateTimeFormat", "type", "standard" },
            { "decimalFormat", "type", "standard" }, { "scientificFormat", "type", "standard" },
            { "percentFormat", "type", "standard" }, { "currencyFormat", "type", "standard" },
            { "pattern", "type", "standard" }, { "currency", "type", "standard" }, { "collation", "type", "standard" },
            { "*", "_q", "*" }, };
        for (int i = 0; i < suppressData.length; ++i) {
            meta.add(prefix + "suppress/attributes" + "[@element=\"" + suppressData[i][0] + "\"][@attribute=\""
                + suppressData[i][1] + "\"][@attributeValue=\"" + suppressData[i][2] + "\"]", "");
        }
        // write out and look at
        PrintWriter out = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY + "meta/", "metaData.xml");
        meta.write(out);
        out.close();
        XMLFileReader xfr = new XMLFileReader().setHandler(new MyHandler());
        xfr.read(CLDRPaths.GEN_DIRECTORY + "meta/metaData.xml", XMLFileReader.CONTENT_HANDLER
            | XMLFileReader.ERROR_HANDLER, false);
    }

    private static void testXMLFileReader() {
        XMLFileReader xfr = new XMLFileReader().setHandler(new MyHandler());
        xfr.read(CLDRPaths.MAIN_DIRECTORY + "root.xml", -1, true);
    }

    static class MyHandler extends XMLFileReader.SimpleHandler {

        @Override
        public void handleAttributeDecl(String eName, String aName, String type, String mode, String value) {
            System.out.println("eName: " + eName + ",\t aName: " + aName + ",\t type: " + type + ",\t mode: " + mode
                + ",\t value: " + value);
        }

        @Override
        public void handleElementDecl(String name, String model) {
            System.out.println("name: " + name + ",\t model: " + model);
        }

        @Override
        public void handlePathValue(String path, String value) {
            System.out.println("path: " + path + ",\t value: " + value);
        }

        @Override
        public void handleComment(String path, String comment) {
            System.out.println("path: " + path + ",\t comment: " + comment);
        }

    }

    public static void testBreakIterator(String text) {
        System.out.println(text);
        String choice = "Line";

        String BASE_RULES = "'<' > '&lt;' ;" + "'<' < '&'[lL][Tt]';' ;" + "'&' > '&amp;' ;"
            + "'&' < '&'[aA][mM][pP]';' ;" + "'>' < '&'[gG][tT]';' ;" + "'\"' < '&'[qQ][uU][oO][tT]';' ; "
            + "'' < '&'[aA][pP][oO][sS]';' ; ";

        String CONTENT_RULES = "'>' > '&gt;' ;";

        String HTML_RULES = BASE_RULES + CONTENT_RULES + "'\"' > '&quot;' ; ";

        String HTML_RULES_CONTROLS = HTML_RULES
            + "([[:C:][:Z:][:whitespace:][:Default_Ignorable_Code_Point:][\\u0080-\\U0010FFFF]]) > &hex/xml($1) ; ";

        Transliterator toHTML = Transliterator.createFromRules("any-xml", HTML_RULES_CONTROLS, Transliterator.FORWARD);

        RuleBasedBreakIterator b;
        if (choice.equals("Word"))
            b = (RuleBasedBreakIterator) BreakIterator.getWordInstance();
        else if (choice.equals("Line"))
            b = (RuleBasedBreakIterator) BreakIterator.getLineInstance();
        else if (choice.equals("Sentence"))
            b = (RuleBasedBreakIterator) BreakIterator.getSentenceInstance();
        else
            b = (RuleBasedBreakIterator) BreakIterator.getCharacterInstance();

        Matcher decimalEscapes = PatternCache.get("&#(x?)([0-9]+);").matcher(text);
        // quick hack, since hex-any doesn't do decimal escapes
        int start = 0;
        StringBuffer result2 = new StringBuffer();
        while (decimalEscapes.find(start)) {
            int radix = decimalEscapes.group(2).length() == 0 ? 10 : 16;
            int code = Integer.parseInt(decimalEscapes.group(2), radix);
            result2.append(text.substring(start, decimalEscapes.start()) + UTF16.valueOf(code));
            start = decimalEscapes.end();
        }
        result2.append(text.substring(start));
        text = result2.toString();

        int lastBreak = 0;
        StringBuffer result = new StringBuffer();
        b.setText(text);
        b.first();
        for (int nextBreak = b.next(); nextBreak != BreakIterator.DONE; nextBreak = b.next()) {
            b.getRuleStatus();
            String piece = text.substring(lastBreak, nextBreak);
            piece = toHTML.transliterate(piece);
            piece = piece.replaceAll("&#xA;", "<br>");
            result.append("<span class='break'>").append(piece).append("</span>");
            lastBreak = nextBreak;
        }

        System.out.println(result);
    }

    private static void checkStandardCodes() {
        StandardCodes sc = StandardCodes.make();
        showCodes(sc, "language");
        showCodes(sc, "script");
        showCodes(sc, "territory");
        showCodes(sc, "tzid");
        showCodes(sc, "currency");

        Map<String, Map<String, Map<String, String>>> m = StandardCodes.getLStreg();
        // print lstreg first
        if (false) {
            System.out.println("Printing Data");
            for (Iterator it = m.keySet().iterator(); it.hasNext();) {
                String type = (String) it.next();
                Map subtagData = m.get(type);
                for (Iterator it2 = subtagData.keySet().iterator(); it2.hasNext();) {
                    String subtag = (String) it2.next();
                    Map labelData = (Map) subtagData.get(subtag);
                    System.out.println(type + "\t " + subtag + "\t " + labelData);
                }
            }
        }
        for (Iterator<String> it = m.keySet().iterator(); it.hasNext();) {
            String type = it.next();
            Map<String, Map<String, String>> subtagData = m.get(type);

            String oldType = type.equals("region") ? "territory" : type;
            Set<String> allCodes = sc.getAvailableCodes(oldType);
            Set<String> temp = new TreeSet<>(subtagData.keySet());
            temp.removeAll(allCodes);
            System.out.println(type + "\t in new but not old\t" + temp);

            temp = new TreeSet<>(allCodes);
            temp.removeAll(subtagData.keySet());
            System.out.println(type + "\t in old but not new\t" + temp);
        }
        for (Iterator<String> it = m.keySet().iterator(); it.hasNext();) {
            String type = it.next();
            Map<String, Map<String, String>> subtagData = m.get(type);
            String oldType = type.equals("region") ? "territory" : type;
            Set<String> goodCodes = sc.getGoodAvailableCodes(oldType);

            for (Iterator<String> it2 = subtagData.keySet().iterator(); it2.hasNext();) {
                String tag = it2.next();
                Map<String, String> data = subtagData.get(tag);
                List<String> sdata = sc.getFullData(oldType, tag);
                if (sdata == null) {
                    if (true)
                        continue;
                    System.out.println("new in ltru");
                    System.out.println("\t" + type + "\t" + tag + "\t" + data);
                    continue;
                }
                String description = sdata.get(0);
                boolean deprecated = !goodCodes.contains(tag);
                if (description.equalsIgnoreCase("PRIVATE USE")) {
                    // description = "";
                    deprecated = false;
                }
                String newDescription = data.get("Description");
                boolean newDeprecated = data.get("Deprecated") != null;
                if (!description.equals(newDescription)) {
                    System.out.println(type + "\t" + tag + "\tDescriptions differ: {" + description + "} ### {"
                        + newDescription + "}");
                }
                if (deprecated != newDeprecated) {
                    System.out.println(type + "\t" + tag + "\tDeprecated differs: {" + deprecated + "} ### {"
                        + newDeprecated + "}");
                }
            }
        }
        // print metadata
        for (Iterator<String> it = m.keySet().iterator(); it.hasNext();) {
            String type = it.next();
            Map<String, Map<String, String>> subtagData = m.get(type);
            String oldType = type.equals("region") ? "territory" : type;

            String aliasType =oldType.equals("legacy") ? "language" : oldType;
            Set<String> allCodes = new TreeSet<>();
            Set<String> deprecatedCodes = new TreeSet<>();

            for (Iterator<String> it2 = subtagData.keySet().iterator(); it2.hasNext();) {
                String tag = it2.next();
                Map<String, String> data = subtagData.get(tag);
                if (data.get("Deprecated") != null) {
                    String preferred = data.get("Preferred-Value");
                    String cldr = null != data.get("CLDR") ? "CLDR: " : "";
                    System.out.println("\t\t\t<" + aliasType + "Alias type=\"" + tag + "\""
                        + (preferred == null || preferred.length() == 0 ? "" : " replacement=\"" + preferred + "\"")
                        + "/> <!-- " + cldr
                        + data.get("Description") + " -->");
                    deprecatedCodes.add(tag);
                } else {
                    allCodes.add(tag);
                }
            }
            // get old ones
            Set<String> goodCodes = sc.getAvailableCodes(oldType);
            TreeSet<String> oldAndNotNew = new TreeSet<>(goodCodes);
            oldAndNotNew.removeAll(allCodes);
            oldAndNotNew.removeAll(deprecatedCodes);
            for (Iterator<String> it2 = oldAndNotNew.iterator(); it2.hasNext();) {
                String tag = it2.next();
                List<String> sdata = sc.getFullData(oldType, tag);
                String preferred = sdata.get(2);
                System.out.println("\t\t\t<" + aliasType + "Alias type=\"" + tag + "\" replacement=\"" + preferred
                    + "\"/> <!-- CLDR:" + sdata.get(0) + " -->");
            }
            String allCodeString = Joiner.on(" ").join(allCodes);
            System.out
                .println("\t\t\t<variable id=\"$" + oldType + "\" type=\"list\">" + allCodeString + "</variable>");
        }
    }

    private static void showCodes(StandardCodes sc, String type) {
        Set<String> codes = sc.getSurveyToolDisplayCodes(type);
        System.out.println("Survey Tool Codes " + codes.size() + "\t" + type);
        for (String code : codes) {
            System.out.println("\t" + code + "\t" + sc.getFullData(type, code));
        }
    }

    private static void checkLanguages() {
        // TODO Auto-generated method stub

        Factory mainCldrFactory = Factory.make(CLDRPaths.COMMON_DIRECTORY + "main" + File.separator, ".*");
        Set<String> availableLocales = mainCldrFactory.getAvailable();
        Set<String> available = new TreeSet<>();
        LocaleIDParser lip = new LocaleIDParser();
        for (Iterator<String> it = availableLocales.iterator(); it.hasNext();) {
            available.add(lip.set(it.next()).getLanguage());
        }
        Set<String> langHack = new TreeSet<>();
        for (int i = 0; i < language_territory_hack.length; ++i) {
            String lang = language_territory_hack[i][0];
            langHack.add(lang);
        }
        if (langHack.containsAll(available))
            System.out.println("All ok");
        else {
            available.removeAll(langHack);
            for (Iterator<String> it = available.iterator(); it.hasNext();) {
                String item = it.next();
                System.out.println("{\"" + item + "\", \"XXX\"},/t//"
                    + ULocale.getDisplayLanguage(item, ULocale.ENGLISH));
            }
        }
    }

    /**
     * @throws IOException
     *
     */
    private static void printCountries() throws IOException {
        Factory mainCldrFactory = Factory.make(CLDRPaths.COMMON_DIRECTORY + "main" + File.separator, ".*");
        CLDRFile english = mainCldrFactory.make("en", true);
        PrintWriter out = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY, "country_language_names.txt");
        StandardCodes sc = StandardCodes.make();
        for (Iterator<String> it = sc.getGoodAvailableCodes("language").iterator(); it.hasNext();) {
            String code = it.next();
            out.println(code + "\t" + english.getName(CLDRFile.LANGUAGE_NAME, code));
        }
        out.println("****");
        for (Iterator<String> it = sc.getGoodAvailableCodes("territory").iterator(); it.hasNext();) {
            String code = it.next();
            out.println(code + "\t" + english.getName(CLDRFile.TERRITORY_NAME, code));
        }
        out.println("****");
        for (Iterator<String> it = sc.getGoodAvailableCodes("script").iterator(); it.hasNext();) {
            String code = it.next();
            out.println(code + "\t" + english.getName(CLDRFile.SCRIPT_NAME, code));
        }
        out.close();
    }

    /**
     *
     */
    private static void printCurrencies() {
        StandardCodes sc = StandardCodes.make();
        Set<String> s = sc.getAvailableCodes("currency");
        for (Iterator<String> it = s.iterator(); it.hasNext();) {
            String code = it.next();
            String name = sc.getData("currency", code);
            List<String> data = sc.getFullData("currency", code);
            System.out.println(code + "\t" + name + "\t" + data);
        }
    }

    /**
     * @throws IOException
     * @throws ParseException
     *
     */
    private static void printZoneSamples() throws Exception {
        String[] locales = { "en", "en_GB", "de", "zh", "hi", "bg", "ru", "ja", "as" // picked
            // deliberately
            // because
            // it
            // has
            // few
            // itesm
        };
        String[] zones = { "America/Los_Angeles", "America/Argentina/Buenos_Aires", "America/Buenos_Aires",
            "America/Havana", "Australia/ACT", "Australia/Sydney", "Europe/London", "Europe/Moscow",
            "Etc/GMT+3" };
        String[][] fields = { { "2004-01-15T00:00:00Z", "Z", "ZZZZ", "z", "zzzz" },
            { "2004-07-15T00:00:00Z", "Z", "ZZZZ", "z", "zzzz", "v", "vvvv" } };
        Factory mainCldrFactory = Factory.make(CLDRPaths.COMMON_DIRECTORY + "main" + File.separator, ".*");
        PrintWriter out = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY, "timezone_samples.txt");
        long[] offsetMillis = new long[1];
        ParsePosition parsePosition = new ParsePosition(0);

        for (int i = 0; i < locales.length; ++i) {
            String locale = locales[i];
            TimezoneFormatter tzf = new TimezoneFormatter(mainCldrFactory, locale, false).setSkipDraft(true);
            for (int j = 0; j < zones.length; ++j) {
                String zone = zones[j];
                for (int k = 0; k < fields.length; ++k) {
                    String type = fields[k][0];
                    Date datetime = ICUServiceBuilder.isoDateParse(type);
                    for (int m = 1; m < fields[k].length; ++m) {
                        String field = fields[k][m];
                        String formatted = tzf.getFormattedZone(zone, field, datetime.getTime(), false);
                        parsePosition.setIndex(0);
                        String parsed = tzf.parse(formatted, parsePosition, offsetMillis);
                        if (parsed == null)
                            parsed = "FAILED PARSE";
                        else if (parsed.length() == 0)
                            parsed = format(offsetMillis[0]);
                        out.println("{\"" + locale + "\",\t\"" + zone + "\",\t\"" + type + "\",\t\"" + field
                            + "\",\t\"" + formatted + "\",\t\"" + parsed + "\"},");
                    }
                }
                out.println();
            }
            out.println("==========");
            out.println();
        }
        out.close();
    }

    /**
     * quick & dirty format
     */
    private static String format(long offsetMillis) {
        offsetMillis /= 60 * 1000;
        String sign = "+";
        if (offsetMillis < 0) {
            offsetMillis = -offsetMillis;
            sign = "-";
        }
        return sign + String.valueOf(offsetMillis / 60) + ":"
            + String.valueOf(100 + (offsetMillis % 60)).substring(1, 3);
    }

    private static final String[][] language_territory_hack = { { "af", "ZA" }, { "am", "ET" }, { "ar", "SA" },
        { "as", "IN" }, { "ay", "PE" }, { "az", "AZ" }, { "bal", "PK" }, { "be", "BY" },
        { "bg", "BG" }, { "bn", "IN" }, { "bs", "BA" }, { "ca", "ES" }, { "ch", "MP" }, { "cpe", "SL" },
        { "cs", "CZ" }, { "cy", "GB" }, { "da", "DK" }, { "de", "DE" }, { "dv", "MV" }, { "dz", "BT" },
        { "el", "GR" }, { "en", "US" }, { "es", "ES" }, { "et", "EE" }, { "eu", "ES" }, { "fa", "IR" }, { "fi", "FI" },
        { "fil", "PH" }, { "fj", "FJ" }, { "fo", "FO" }, { "fr", "FR" }, { "ga", "IE" },
        { "gd", "GB" }, { "gl", "ES" }, { "gn", "PY" }, { "gu", "IN" }, { "gv", "GB" }, { "ha", "NG" }, { "he", "IL" },
        { "hi", "IN" }, { "ho", "PG" }, { "hr", "HR" }, { "ht", "HT" }, { "hu", "HU" },
        { "hy", "AM" }, { "id", "ID" }, { "is", "IS" }, { "it", "IT" }, { "ja", "JP" }, { "ka", "GE" }, { "kk", "KZ" },
        { "kl", "GL" }, { "km", "KH" }, { "kn", "IN" }, { "ko", "KR" }, { "kok", "IN" },
        { "ks", "IN" }, { "ku", "TR" }, { "ky", "KG" }, { "la", "VA" }, { "lb", "LU" }, { "ln", "CG" }, { "lo", "LA" },
        { "lt", "LT" }, { "lv", "LV" }, { "mai", "IN" }, { "men", "GN" }, { "mg", "MG" },
        { "mh", "MH" }, { "mk", "MK" }, { "ml", "IN" }, { "mn", "MN" }, { "mni", "IN" }, { "mo", "MD" },
        { "mr", "IN" }, { "ms", "MY" }, { "mt", "MT" }, { "my", "MM" }, { "na", "NR" }, { "nb", "NO" },
        { "nd", "ZA" }, { "ne", "NP" }, { "niu", "NU" }, { "nl", "NL" }, { "nn", "NO" }, { "no", "NO" },
        { "nr", "ZA" }, { "nso", "ZA" }, { "ny", "MW" }, { "om", "KE" }, { "or", "IN" }, { "pa", "IN" },
        { "pau", "PW" }, { "pl", "PL" }, { "ps", "PK" }, { "pt", "BR" }, { "qu", "PE" }, { "rn", "BI" },
        { "ro", "RO" }, { "ru", "RU" }, { "rw", "RW" }, { "sd", "IN" }, { "sg", "CF" }, { "si", "LK" },
        { "sk", "SK" }, { "sl", "SI" }, { "sm", "WS" }, { "so", "DJ" }, { "sq", "CS" }, { "sr", "CS" }, { "ss", "ZA" },
        { "st", "ZA" }, { "sv", "SE" }, { "sw", "KE" }, { "ta", "IN" }, { "te", "IN" },
        { "tem", "SL" }, { "tet", "TL" }, { "th", "TH" }, { "ti", "ET" }, { "tg", "TJ" }, { "tk", "TM" },
        { "tkl", "TK" }, { "tvl", "TV" }, { "tl", "PH" }, { "tn", "ZA" }, { "to", "TO" },
        { "tpi", "PG" }, { "tr", "TR" }, { "ts", "ZA" }, { "uk", "UA" }, { "ur", "IN" }, { "uz", "UZ" },
        { "ve", "ZA" }, { "vi", "VN" }, { "wo", "SN" }, { "xh", "ZA" }, { "zh", "CN" },
        { "zh_Hant", "TW" }, { "zu", "ZA" }, { "aa", "ET" }, { "byn", "ER" }, { "eo", "DE" }, { "gez", "ET" },
        { "haw", "US" }, { "iu", "CA" }, { "kw", "GB" }, { "sa", "IN" }, { "sh", "HR" },
        { "sid", "ET" }, { "syr", "SY" }, { "tig", "ER" }, { "tt", "RU" }, { "wal", "ET" }, };

}
