package org.unicode.cldr.tool;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.unittest.TestAll;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.NumberInfo;
import com.ibm.icu.util.ULocale;

public class WritePluralRules {
    static final SupplementalDataInfo sInfo = TestAll.TestInfo.getInstance().getSupplementalDataInfo();
    public static void main(String[] args) {
        Relation<PluralRules,String> rulesToLocales = Relation.of(new TreeMap<PluralRules,Set<String>>(new PluralRulesComparator()), TreeSet.class);
        for (String locale : sInfo.getPluralLocales(PluralType.cardinal)) {
            if (locale.equals("root")) {
                continue;
            }
            PluralRules rules = forLocale(locale);
            rulesToLocales.put(rules, locale);
        }
        System.out.println(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        +"<!DOCTYPE supplementalData SYSTEM \"../../common/dtd/ldmlSupplemental.dtd\">\n"
                        +"<!--\n"
                        +"Copyright © 1991-2013 Unicode, Inc.\n"
                        +"CLDR data files are interpreted according to the LDML specification (http://unicode.org/reports/tr35/)\n"
                        +"For terms of use, see http://www.unicode.org/copyright.html\n"
                        +"-->\n"
                        +"<supplementalData>\n"
                        +"    <version number=\"$Revision: 8490 $\"/>\n"
                        +"    <generation date=\"$Date: 2013-04-19 07:59:01 +0200 (Fri, 19 Apr 2013) $\"/>\n"
                        +"    <plurals>\n"
                        +"        <!-- if locale is known to have no plurals, there are no rules -->"
                );
        TreeSet<Entry<PluralRules, Set<String>>> sorted = new TreeSet<Entry<PluralRules, Set<String>>>(new HackComparator());
        sorted.addAll(rulesToLocales.keyValuesSet());
        for (Entry<PluralRules, Set<String>> entry : sorted) {
            PluralRules rules = entry.getKey();
            Set<String> values = entry.getValue();
            String locales = CollectionUtilities.join(values, " ");
            //String comment = hackComments.get(locales);
            System.out.println("        <pluralRules locales=\"" + locales + "\">"
                    //+ (comment != null ? comment : "")
                    );
            for (String keyword : rules.getKeywords()) {
                String rule = rules.getRules(keyword);
                if (rule == null) {
                    continue;
                }
                System.out.println("            <pluralRule count=\"" + keyword + "\">" + rule + "</pluralRule>");
            }
            System.out.println("        </pluralRules>");
            /*
        <pluralRules locales="ar">
            <pluralRule count="zero">n is 0</pluralRule>
            <pluralRule count="one">n is 1</pluralRule>
            <pluralRule count="two">n is 2</pluralRule>
            <pluralRule count="few">n mod 100 in 3..10</pluralRule>
            <pluralRule count="many">n mod 100 in 11..99</pluralRule>
        </pluralRules>

             */
        }
        System.out.println("    </plurals>\n" +
        		"</supplementalData>");
    }
    
//    static Map<String,String> hackComments = new HashMap<String,String>();
//    static {
//        hackComments.put("ga", " <!-- http://unicode.org/cldr/trac/ticket/3915 -->");
//        hackComments.put("mt", " <!-- from Tamplin's data -->");
//        hackComments.put("mk", " <!-- from Tamplin's data -->");
//        hackComments.put("cy", " <!-- from http://www.saltcymru.org/wordpress/?p=99&lang=en -->");
//        hackComments.put("br", " <!-- from http://unicode.org/cldr/trac/ticket/2886 -->");
//    }
    
    static class HackComparator implements Comparator<Entry<PluralRules, Set<String>>> {
        // we get the order of the first items in each of the old rules, and use that order where we can.
        PluralRulesComparator prc = new PluralRulesComparator();
        static Map<String,Integer> hackMap = new HashMap<String,Integer>();
        static {
            int i = 0;
            for (String s : "az ar he asa af lg vo ak ff lv iu ga ro mo lt be bs cs sk pl sl mt mk cy lag shi br ksh tzm gv gd".split(" ")) {
                hackMap.put(s, i++);
            }
        }
        @Override
        public int compare(Entry<PluralRules, Set<String>> o1, Entry<PluralRules, Set<String>> o2) {
            Integer firstLocale1 = hackMap.get(o1.getValue().iterator().next());
            Integer firstLocale2 = hackMap.get(o2.getValue().iterator().next());
            if (firstLocale1 != null) {
                if (firstLocale2 != null) {
                    return firstLocale1 - firstLocale2;
                }
                return -1;
            } else if (firstLocale2 != null) {
                return 1;
            } else { // only if BOTH are null, use better comparison
                return prc.compare(o1.getKey(), o2.getKey());
            }
        }
    }
    
    static class PluralRulesComparator implements Comparator<PluralRules> {
        CollectionUtilities.CollectionComparator<String> comp = new CollectionUtilities.CollectionComparator<String>();

        @Override
        public int compare(PluralRules arg0, PluralRules arg1) {
            Set<String> key0 = arg0.getKeywords();
            Set<String> key1 = arg1.getKeywords();
            int diff = comp.compare(key0, key1);
            if (diff != 0) {
                return diff;
            }
            return arg0.toString().compareTo(arg1.toString());
        }
    }
    
    static PluralRules forLocale(String locale) {
        PluralRules override = OVERRIDES.get(locale);
        return override != null 
                ? override
                        : sInfo.getPlurals(locale).getPluralRules();
    }

    // copied from PluralRulesFactory
    static Relation<ULocale,NumberInfo> EXTRA_SAMPLES = Relation.of(new HashMap<ULocale,Set<NumberInfo>>(), HashSet.class); 
    static Map<String,PluralRules> OVERRIDES = new HashMap<String,PluralRules>(); 
    static {
        String[][] overrides = {
                {"bn", "one: n within 0..1"},
                {"en,ca,de,et,fi,gl,it,nl,sv,sw,ta,te,ur", "one: j is 1"},
                {"pt", "one: n is 1 or f is 1"},
                {"cs,sk", "one: j is 1;  few: j in 2..4; many: v is not 0"},
                //{"cy", "one: n is 1;  two: n is 2;  few: n is 3;  many: n is 6"},
                //{"el", "one: j is 1 or i is 0 and f is 1"},
                {"da,is", "one: j is 1 or f is 1"},
                {"fil,tl", "one: j in 0..1"},
                {"he,iw", "one: j is 1;  two: j is 2; many: j is not 0 and j mod 10 is 0", "10,20"},
                {"hi", "one: n within 0..1"},
                {"hy", "one: n within 0..2 and n is not 2"},
//                {"hr", "one: j mod 10 is 1 and j mod 100 is not 11;  few: j mod 10 in 2..4 and j mod 100 not in 12..14;  many: j mod 10 is 0 or j mod 10 in 5..9 or j mod 100 in 11..14"},
                {"lv", "zero: n mod 10 is 0" +
                        " or n mod 10 in 11..19" +
                        " or v is 2 and f mod 10 in 11..19;" +
                        "one: n mod 10 is 1 and n mod 100 is not 11" +
                        " or v is 2 and f mod 10 is 1 and f mod 100 is not 11" +
                        " or v is not 2 and f mod 10 is 1"},
//                {"lv", "zero: n mod 10 is 0" +
//                        " or n mod 10 in 11..19" +
//                        " or v in 1..6 and f is not 0 and f mod 10 is 0" +
//                        " or v in 1..6 and f mod 10 in 11..19;" +
//                        "one: n mod 10 is 1 and n mod 100 is not 11" +
//                        " or v in 1..6 and f mod 10 is 1 and f mod 100 is not 11" +
//                        " or v not in 0..6 and f mod 10 is 1"},
                {"pl", "one: j is 1;  few: j mod 10 in 2..4 and j mod 100 not in 12..14;  many: j is not 1 and j mod 10 in 0..1 or j mod 10 in 5..9 or j mod 100 in 12..14"},
                {"sl", "one: j mod 100 is 1;  two: j mod 100 is 2;  few: j mod 100 in 3..4 or v is not 0"},
//                {"sr", "one: j mod 10 is 1 and j mod 100 is not 11" +
//                        " or v in 1..6 and f mod 10 is 1 and f mod 100 is not 11" +
//                        " or v not in 0..6 and f mod 10 is 1;" +
//                        "few: j mod 10 in 2..4 and j mod 100 not in 12..14" +
//                        " or v in 1..6 and f mod 10 in 2..4 and f mod 100 not in 12..14" +
//                        " or v not in 0..6 and f mod 10 in 2..4"
//                },
                {"sr,hr,sh", "one: j mod 10 is 1 and j mod 100 is not 11" +
                        " or f mod 10 is 1 and f mod 100 is not 11;" +
                        "few: j mod 10 in 2..4 and j mod 100 not in 12..14" +
                        " or f mod 10 in 2..4 and f mod 100 not in 12..14"
                },
                        // +
                        //                            " ; many: j mod 10 is 0 " +
                        //                            " or j mod 10 in 5..9 " +
                        //                            " or j mod 100 in 11..14" +
                        //                            " or v in 1..6 and f mod 10 is 0" +
                        //                            " or v in 1..6 and f mod 10 in 5..9" +
                        //                            " or v in 1..6 and f mod 100 in 11..14" +
                        //                    " or v not in 0..6 and f mod 10 in 5..9"
                {"mo,ro", "one: j is 1; few: v is not 0 or n is 0 or n is not 1 and n mod 100 in 1..19"},
                {"ru", "one: j mod 10 is 1 and j mod 100 is not 11;" +
                        " many: j mod 10 is 0 or j mod 10 in 5..9 or j mod 100 in 11..14"
//                        + "; many: j mod 10 is 0 or j mod 10 in 5..9 or j mod 100 in 11..14"
                        },
                        {"uk", "one: j mod 10 is 1 and j mod 100 is not 11;  " +
                                "few: j mod 10 in 2..4 and j mod 100 not in 12..14;  " +
                                "many: j mod 10 is 0 or j mod 10 in 5..9 or j mod 100 in 11..14"},
                                {"zu", "one: n within 0..1"},
        };
        for (String[] pair : overrides) {
            for (String locale : pair[0].split("\\s*,\\s*")) {
                if (OVERRIDES.containsKey(locale)) {
                    throw new IllegalArgumentException("Duplicate locale: " + locale);
                }
                try {
                    PluralRules rules = PluralRules.parseDescription(pair[1]);
                    OVERRIDES.put(locale, rules);
                } catch (Exception e) {
                    throw new IllegalArgumentException(locale + "\t" + pair[1], e);
                }
            }
        }
    }

}
