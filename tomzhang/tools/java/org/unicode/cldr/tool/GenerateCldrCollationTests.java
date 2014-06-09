/*
 * Created on May 19, 2005
 * Copyright (C) 2004-2011, Unicode, Inc., International Business Machines Corporation, and others.
 * For terms of use, see http://www.unicode.org/terms_of_use.html
 */
package org.unicode.cldr.tool;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.icu.CollationMapper;
import org.unicode.cldr.icu.IcuData;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.Log;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.util.ULocale;

public class GenerateCldrCollationTests {
    String sourceDir;
    Set<String> validLocales = new TreeSet<String>();
    Map<String, Object> ulocale_rules = new TreeMap<String, Object>(GenerateCldrTests.ULocaleComparator);
    Map<String, Map<String, RuleBasedCollator>> locale_types_rules = new TreeMap<String, Map<String, RuleBasedCollator>>();
    Map<RuleBasedCollator, RuleBasedCollator> collation_collation = new HashMap<RuleBasedCollator, RuleBasedCollator>();
    RuleBasedCollator emptyCollator = (RuleBasedCollator) Collator.getInstance(new ULocale(""));

    public Set<String> getAvailableSet() {
        return ulocale_rules.keySet();
    }

    public RuleBasedCollator getInstance(String locale) {
        return (RuleBasedCollator) ulocale_rules.get(locale);
    }

    void show() {
        Log.logln("Showing Locales");
        Log.logln("Unique Collators: " + collation_collation.size());
        for (Iterator it2 = ulocale_rules.keySet().iterator(); it2.hasNext();) {
            ULocale locale = (ULocale) it2.next();
            RuleBasedCollator col = (RuleBasedCollator) ulocale_rules.get(locale);
            Log.logln("\t" + locale + ", " + col.getRules());
        }
    }

    GenerateCldrCollationTests(String sourceDir, String localeRegex, Set<String> locales) throws Exception {
        this.sourceDir = sourceDir;
        Set<String> s = GenerateCldrTests.getMatchingXMLFiles(sourceDir, localeRegex);
        for (Iterator<String> it = s.iterator(); it.hasNext();) {
            getCollationRules(it.next());
        }

        // now fixup the validLocales, adding in what they inherit
        // TODO, add check: validSubLocales are masked by intervening locales.
        for (Iterator<String> it = validLocales.iterator(); it.hasNext();) {
            String locale = it.next();
            Map<String, RuleBasedCollator> types_rules = locale_types_rules.get(locale);
            if (types_rules != null)
                Log.logln("Weird: overlap in validLocales: " + locale);
            else {
                for (String parentlocale = LocaleIDParser.getSimpleParent(locale); parentlocale != null; parentlocale = LocaleIDParser
                    .getSimpleParent(parentlocale)) {
                    types_rules = locale_types_rules.get(parentlocale);
                    if (types_rules != null) {
                        locale_types_rules.put(locale, types_rules);
                        break;
                    }
                }
            }
        }
        // now generate the @-style locales
        ulocale_rules.put("root", Collator.getInstance(ULocale.ROOT));

        for (Iterator<String> it = locale_types_rules.keySet().iterator(); it.hasNext();) {
            String locale = it.next();
            Map<String, RuleBasedCollator> types_rules = locale_types_rules.get(locale);
            for (Iterator<String> it2 = types_rules.keySet().iterator(); it2.hasNext();) {
                String type = it2.next();
                // TODO fix HACK
                if (type.equals("unihan")) {
                    if (!locale.startsWith("zh")) continue;
                }
                RuleBasedCollator col = (RuleBasedCollator) types_rules.get(type);
                String name = type.equals("standard") ? locale : locale + "@collation=" + type;
                ulocale_rules.put(name, col);
            }
        }
        // now flesh out
        // Collator root = Collator.getInstance(ULocale.ROOT);
        for (Iterator<String> it = locales.iterator(); it.hasNext();) {
            String locale = it.next();
            if (ulocale_rules.get(locale) != null) continue;
            String parent = LocaleIDParser.getSimpleParent(locale); // GenerateCldrTests.getParent(locale);
            if (parent == null) continue;
            try {
                ulocale_rules.put(locale, ulocale_rules.get(parent));
            } catch (RuntimeException e) {
                throw e;
            }
        }
    }

    static Transliterator fromHex = Transliterator.getInstance("hex-any");

    private void getCollationRules(String locale) throws Exception {
        System.out.println("Loading collation:\t" + locale);
        CollationMapper mapper = new CollationMapper(sourceDir, null);
        StringBuilder stringBuilder = new StringBuilder();
        TreeMap<String, RuleBasedCollator> types_rules = new TreeMap<String, RuleBasedCollator>();
        IcuData[] dataList = mapper.fillFromCldr(locale);
        IcuData icuData = dataList[0];
        for (String rbPath : icuData.keySet()) {
            if (!rbPath.endsWith("/Sequence")) continue;
            // remove the \ u's, because they blow up
            stringBuilder.setLength(0);
            for (String line : icuData.get(rbPath).get(0)) {
                stringBuilder.append(line);
            }
            String originalRules = stringBuilder.toString();
            String rules = fromHex.transliterate(originalRules);
            String name = rbPath.split("/")[2];
            RuleBasedCollator fixed = generateCollator(locale, name, rules);
            if (fixed != null) {
                Log.logln("Rules for: " + locale + ", " + name);
                Log.logln(rules);
                if (!rules.equals(originalRules)) {
                    Log.logln("Original Rules from Ram: ");
                    Log.logln(originalRules);
                }
                types_rules.put(name, fixed);
            }
            locale_types_rules.put(locale, types_rules);
        }
        // now get the valid sublocales
        for (int i = 1; i < dataList.length; i++) {
            IcuData subLocale = dataList[i];
            Log.logln("Valid Sub Locale: " + subLocale.getName());
            validLocales.add(subLocale.getName());
        }
    }

    /**
     * @param locale
     * @param current
     * @param foo
     * @param rules
     */
    private RuleBasedCollator generateCollator(String locale, String current, String rules) {
        RuleBasedCollator fixed = null;
        try {
            if (rules.equals(""))
                fixed = emptyCollator;
            else {
                rules = GenerateCldrTests.replace(rules, "[optimize[", "[optimize [");
                rules = GenerateCldrTests.replace(rules, "[suppressContractions[", "[suppressContractions [");
                RuleBasedCollator col = new RuleBasedCollator(rules);
                fixed = (RuleBasedCollator) collation_collation.get(col);
                if (fixed == null) {
                    collation_collation.put(col, col);
                    fixed = col;
                }
            }
        } catch (Exception e) {
            Log.logln("***Cannot create collator from: " + locale + ", " + current + ", " + rules);
            e.printStackTrace(Log.getLog());
            RuleBasedCollator coll = (RuleBasedCollator) Collator.getInstance(new ULocale(locale));
            String oldrules = coll.getRules();
            Log.logln("Old ICU4J: " + oldrules);
            Log.logln("Equal?: " + oldrules.equals(rules));
        }
        return fixed;
    }
}
