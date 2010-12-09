/*
 * Created on May 19, 2005
 * Copyright (C) 2004-2005, Unicode, Inc., International Business Machines Corporation, and others.
 * For terms of use, see http://www.unicode.org/terms_of_use.html
 */
package org.unicode.cldr.tool;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.icu.ICUResourceWriter;
import org.unicode.cldr.icu.LDML2ICUConverter;
import org.unicode.cldr.icu.ICUResourceWriter.Resource;
import org.unicode.cldr.util.LDMLUtilities;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.util.ULocale;


public class GenerateCldrCollationTests {
	String sourceDir;
    Set validLocales = new TreeSet();
    Map<String,Object> ulocale_rules = new TreeMap(GenerateCldrTests.ULocaleComparator);
    Map locale_types_rules = new TreeMap();
    Map collation_collation = new HashMap();
    RuleBasedCollator emptyCollator = (RuleBasedCollator) Collator.getInstance(new ULocale(""));

    public Set getAvailableSet() {
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
        Set s = GenerateCldrTests.getMatchingXMLFiles(sourceDir, localeRegex);
        for (Iterator it = s.iterator(); it.hasNext();) {
            getCollationRules((String) it.next());
        }

        // now fixup the validLocales, adding in what they inherit
        // TODO, add check: validSubLocales are masked by intervening locales.
        for (Iterator it = validLocales.iterator(); it.hasNext(); ) {
            String locale = (String) it.next();
            Map types_rules = (Map) locale_types_rules.get(locale);
            if (types_rules != null) Log.logln("Weird: overlap in validLocales: " + locale);
            else {
                for (String parentlocale = GenerateCldrTests.getParent(locale); parentlocale != null; parentlocale = GenerateCldrTests.getParent(parentlocale)) {
                    types_rules = (Map) locale_types_rules.get(parentlocale);
                    if (types_rules != null) {
                        locale_types_rules.put(locale, types_rules);
                        break;
                    }
                }
            }
        }
        // now generate the @-style locales
       ulocale_rules.put("root", Collator.getInstance(ULocale.ROOT));

        for (Iterator it = locale_types_rules.keySet().iterator(); it.hasNext(); ) {
            String locale = (String) it.next();
            Map types_rules = (Map) locale_types_rules.get(locale);
            for (Iterator it2 = types_rules.keySet().iterator(); it2.hasNext(); ) {
                String type = (String) it2.next();
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
        //Collator root = Collator.getInstance(ULocale.ROOT);
        for (Iterator<String> it = locales.iterator(); it.hasNext();) {
          String locale = it.next();
        	if (ulocale_rules.get(locale) != null) continue;
            String parent = LanguageTagParser.getParent(locale); // GenerateCldrTests.getParent(locale);
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
        Document doc = LDMLUtilities.getFullyResolvedLDML(sourceDir, locale, false, false, false, false);
        Node node = LDMLUtilities.getNode(doc, "//ldml/collations");
        LDML2ICUConverter cnv = new LDML2ICUConverter();
        StringBuilder stringBuilder = new StringBuilder();
        ICUResourceWriter.ResourceTable resource = (ICUResourceWriter.ResourceTable) cnv.parseCollations(node, null, stringBuilder, false);
        Map types_rules = new TreeMap();
        locale_types_rules.put(locale, types_rules);
        for (Resource current = resource.first; current != null; current = current.next) {
            if (current.name == null) {
            	Log.logln("Collation: null name found in " + locale);
            	continue;
            }
            if (current instanceof ICUResourceWriter.ResourceTable) {
                ICUResourceWriter.ResourceTable table = (ICUResourceWriter.ResourceTable) current;
                for (Resource current2 = table.first; current2 != null; current2 = current2.next) {
                    if (current2 instanceof ICUResourceWriter.ResourceString) {
                        ICUResourceWriter.ResourceString foo = (ICUResourceWriter.ResourceString) current2;
                        //System.out.println("\t" + foo.name + ", " + foo.val);
                        /* skip since the utilities have the wrong value
                        if (current.name.equals("validSubLocales")) {
                            // skip since it is wrong
                            log.println("Valid Sub Locale: " + foo.name);
                            validLocales.add(foo.name);
                        } else
                        */
                        if (foo.name.equals("Sequence")) {
                            // remove the \ u's, because they blow up
                            String rules = fromHex.transliterate(foo.val);
                            RuleBasedCollator fixed = generateCollator(locale, current.name, foo.name, rules);
                            if (fixed != null) {
                                Log.logln("Rules for: " + locale + ", " + current.name);
                                Log.logln(rules);
                                if (!rules.equals(foo.val)) {
                                    Log.logln("Original Rules from Ram: ");
                                    Log.logln(foo.val);
                                }
                                types_rules.put(current.name, fixed);
                            }
                        }
                    }
                }
            }
            //current.write(System.out,0,false);
        }
        // now get the valid sublocales
        Document doc2 = LDMLUtilities.parse(sourceDir + locale + ".xml", false);
        Node colls = LDMLUtilities.getNode(doc2,"//ldml/collations");
        String validSubLocales = LDMLUtilities.getAttributeValue(colls, "validSubLocales");
        if (validSubLocales != null) {
            String items[] = new String[100]; // allocate plenty
            com.ibm.icu.impl.Utility.split(validSubLocales, ' ', items);
            for (int i = 0; items[i].length() != 0; ++i) {
                Log.logln("Valid Sub Locale: " + items[i]);
                validLocales.add(items[i]);
            }
        }
    }

    /**
     * @param locale
     * @param current
     * @param foo
     * @param rules
     */
    private RuleBasedCollator generateCollator(String locale, String current, String foo, String rules) {
        RuleBasedCollator fixed = null;
        try {
            if (rules.equals("")) fixed = emptyCollator;
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
            Log.logln("***Cannot create collator from: " + locale + ", " + current + ", " + foo + ", " + rules);
            e.printStackTrace(Log.getLog());
            RuleBasedCollator coll = (RuleBasedCollator)Collator.getInstance(new ULocale(locale));
            String oldrules = coll.getRules();
            Log.logln("Old ICU4J: " + oldrules);
            Log.logln("Equal?: " + oldrules.equals(rules));
        }
        return fixed;
    }
}