package org.unicode.cldr.tool;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.text.PluralRules;

public class WritePluralRules {
    static SupplementalDataInfo sInfo = CLDRConfig.getInstance().getSupplementalDataInfo();

    public static void main(String[] args) {
        Relation<PluralRules, String> rulesToLocales = Relation.of(new TreeMap<PluralRules, Set<String>>(new PluralRulesComparator()), TreeSet.class);
        for (String locale : sInfo.getPluralLocales(PluralType.cardinal)) {
            if (locale.equals("root")) {
                continue;
            }
            PluralRules rules = forLocale(locale);
            //            PluralRules existingRules = stringToRules.get(rules.toString());
            //            if (existingRules == null) {
            //                stringToRules.put(rules.toString(), existingRules = rules);
            //            }
            rulesToLocales.put(rules, locale);
        }
        System.out.println(
            formatPluralHeader(PluralType.cardinal, "WritePluralRules"));
        TreeSet<Entry<PluralRules, Set<String>>> sorted = new TreeSet<Entry<PluralRules, Set<String>>>(new HackComparator());
        sorted.addAll(rulesToLocales.keyValuesSet());
        for (Entry<PluralRules, Set<String>> entry : sorted) {
            PluralRules rules = entry.getKey();
            Set<String> values = entry.getValue();
            //String comment = hackComments.get(locales);
            System.out.println(formatPluralRuleHeader(values));
            for (String keyword : rules.getKeywords()) {
                String rule = rules.getRules(keyword);
                if (rule == null) {
                    continue;
                }
                System.out.println(formatPluralRule(keyword, rule, "", false));
            }
            System.out.println(formatPluralRuleFooter());
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
        System.out.println(formatPluralFooter());
    }

    public static String formatPluralRuleFooter() {
        return "        </pluralRules>";
    }

    public static String formatPluralRule(String keyword, String rule, String samples, boolean newLine) {
        if (rule == null) {
            rule = "";
        } else {
            PluralRules rules = PluralRules.createRules(keyword + ":" + rule);
            rule = rules.getRules(keyword);
        }
        if (newLine) {
            rule = "\n                " + rule;
            samples = samples.replace("\t@", "\n                @");
        }
        String result = ("            <pluralRule count=\"" + keyword + "\">"
            + rule
            + samples.replace('\t', ' ')
            + (newLine ? "\n            " : "")
            + "</pluralRule>");
        return result;
    }

    public static String formatPluralRuleHeader(Set<String> values) {
        String locales = CollectionUtilities.join(values, " ");
        String result = ("        <pluralRules locales=\"" + locales + "\">"
        //+ (comment != null ? comment : "")
        );
        return result;
    }

    public static String formatPluralFooter() {
        return "    </plurals>\n" +
            "</supplementalData>";
    }

    public static String formatPluralHeader(PluralType type, String filename) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
            + "<!DOCTYPE supplementalData SYSTEM \"../../common/dtd/ldmlSupplemental.dtd\">\n"
            + "<!--\n"
            + "Copyright © 1991-2015 Unicode, Inc.\n"
            + "CLDR data files are interpreted according to the LDML specification (http://unicode.org/reports/tr35/)\n"
            + "For terms of use, see http://www.unicode.org/copyright.html\n"
            + "-->\n"
            + "<supplementalData>\n"
            + "    <version number=\"$Revision" +
            "$\"/>\n"
            + "    <plurals type=\"" + type + "\">\n"
            + "        <!-- For a canonicalized list, use " + filename + " -->\n";
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

        @Override
        public int compare(Entry<PluralRules, Set<String>> o1, Entry<PluralRules, Set<String>> o2) {
            Integer firstLocale1 = HACK_ORDER_PLURALS.get(o1.getValue().iterator().next());
            Integer firstLocale2 = HACK_ORDER_PLURALS.get(o2.getValue().iterator().next());
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

    static Map<String, Integer> HACK_ORDER_PLURALS = new HashMap<String, Integer>();
    static Map<String, Integer> HACK_ORDER_ORDINALS = new HashMap<String, Integer>();
    static {
//        int i = 0;
//        for (String s : "ar he iw af asa ast az bem bez bg brx cgg chr ckb dv ee el eo es eu fo fur fy gsw ha haw hu jgo jmc ka kaj kcg kk kkj kl ks ksb ku ky lb lg mas mgo ml mn nah nb nd ne nn nnh no nr ny nyn om or os pap ps rm rof rwk saq seh sn so sq ss ssy st syr ta te teo tig tk tn tr ts uz ve vo vun wae xh xog ak bh guw ln mg nso pa ti wa ff fr hy kab lv iu kw naq se sma smi smj smn sms ga mo ro lt be cs sk pl sl mt mk cy lag shi br ksh tzm gv gd bm bo dz id in ig ii ja jbo jv jw kde kea km ko lkt lo ms my nqo sah ses sg th to vi wo yo zh fil tl ca de en et fi gl it nl sv sw ur yi ji pt da pt_PT am bn fa gu hi kn mr zu is si bs hr sh sr ru uk"
//            .split(" ")) {
//            HACK_ORDER_PLURALS.put(s, i++);
//        }
//        i = 0;
//        for (String s : "af fil hu sv en it ca mr gu hi bn zu".split(" ")) {
//            HACK_ORDER_ORDINALS.put(s, i++);
//        }
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
        PluralRules override = null; // PluralRulesFactory.getPluralOverrides().get(new ULocale(locale));
        return override != null
            ? override
            : sInfo.getPlurals(locale).getPluralRules();
    }
}