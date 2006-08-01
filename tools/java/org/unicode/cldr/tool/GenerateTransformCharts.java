/**
 *******************************************************************************
 * Copyright (C) 2001-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package org.unicode.cldr.tool;

import org.unicode.cldr.util.CLDRTransforms;
import org.unicode.cldr.util.SimpleEquivalenceClass;
import org.unicode.cldr.util.Utility;

import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.RuleBasedTransliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.dev.test.util.ArrayComparator;
import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.sun.org.apache.xalan.internal.xsltc.compiler.XPathParser;
// import com.ibm.icu.impl.Utility;

import java.util.*;
import java.io.*;

public class GenerateTransformCharts {
    
    static int[] indicScripts = { UScript.LATIN, UScript.COMMON, UScript.DEVANAGARI,
            UScript.BENGALI, UScript.GURMUKHI, UScript.GUJARATI,
            UScript.ORIYA, UScript.TAMIL, UScript.TELUGU, UScript.KANNADA,
            UScript.MALAYALAM, };
    static String[] names = new String[indicScripts.length];
    static UnicodeSet[] sets = new UnicodeSet[indicScripts.length];
    static Transliterator[] fallbacks = new Transliterator[indicScripts.length];
    static         UnicodeSet lengthMarks = new UnicodeSet(
    "[\u09D7\u0B56-\u0B57\u0BD7\u0C56\u0CD5-\u0CD6\u0D57\u0C55\u0CD5]");

    static         String testString = "\u0946\u093E";
    private static boolean useICU = true;
    
    // Latin    Arabic  Bengali     Cyrillic    Devanagari  Greek   Greek/UNGEGN    Gujarati    Gurmukhi    Hangul  Hebrew  Hiragana    Kannada     Katakana    Malayalam   Oriya   Tamil   Telugu  Thai


    public static void main(String[] args) throws IOException {
        System.out.println("Start");
        try {
            if (true) {
                showLatin();
                return;
            }
            populateScriptInfo();
            
            SimpleEquivalenceClass equivalenceClass = new SimpleEquivalenceClass(new UTF16.StringComparator(true,false,0)); // new ReverseComparator());
            Transliterator anyToLatin = Transliterator.getInstance("any-latin");

            UnicodeSet failNorm = new UnicodeSet();
            // Collator sc = Collator.getInstance(ULocale.ENGLISH);
            // sc.setStrength(Collator.IDENTICAL);
            Comparator sc = new UTF16.StringComparator(true, false, 0);
            Set latinFail = new TreeSet(new ArrayComparator(new Comparator[] { sc, sc, sc }));

            getEquivalentCharacters(equivalenceClass, latinFail);
            
            printChart(equivalenceClass, anyToLatin, failNorm, latinFail);
        } finally {
            System.out.println("Done");
        }
        
    }

    private static void printChart(SimpleEquivalenceClass equivalenceClass, Transliterator anyToLatin, UnicodeSet failNorm, Set latinFail) throws IOException {
        // collect equivalents
        PrintWriter pw = BagFormatter.openUTF8Writer(Utility.GEN_DIRECTORY + "gen/charts/transforms/", "transChart.html");
        pw.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
        pw.println("<title>Indic Transliteration Chart</title><style>");
        pw.println("td { text-align: Center; font-size: 200% }");
        pw.println("tt { font-size: 50% }");
        pw.println("td.miss { background-color: #CCCCFF }");
        pw.println("td.none { background-color: #FFCCCC }");
        pw.println("</style></head><body bgcolor='#FFFFFF'>");

        pw.println("<table border='1' cellspacing='0'><tr>");
        for (int i = 0; i < indicScripts.length; ++i) {
            pw.print("<th width='10%'>" + names[i].substring(0, 3) + "</th>");
        }
        pw.println("</tr>");

        Comparator mySetComparator = new CollectionOfComparablesComparator();
        Iterator rit = equivalenceClass.getSetIterator(mySetComparator);
        Set last = null;
        while (rit.hasNext()) {
            Set equivs = (Set) rit.next();
            mySetComparator.compare(last, equivs);
            last = equivs;
            pw.print("<tr>");
            // transliterate the first item into latin
            Iterator sit = equivs.iterator();
            String source = (String) sit.next();
            String item = anyToLatin.transliterate(source);
            if (source.equals(item))
                item = ""; // failure
            // pw.print("<td>" + item + "</td>");
            showCell(pw, item, "");
            // now show the other cells.
            for (int i = 1; i < indicScripts.length; ++i) {
                item = findItemInScript(equivs, sets[i]);
                String classString = "";
                if (item.equals("")) {
                    classString = " class='miss'";
                    String temp = fallbacks[i].transliterate(source);
                    if (!temp.equals("") && !temp.equals(source))
                        item = temp;
                }
                showCell(pw, item, classString);
            }
            /*
             * Iterator sit = equivs.iterator(); while (sit.hasNext()) { String
             * item = (String)sit.next(); pw.print("<td>" + item + "</td>"); }
             */
            pw.println("</tr>");
        }
        pw.println("</table>");
        if (false) {
            pw.println("<h2>Failed Normalization</h2>");

            UnicodeSetIterator it = new UnicodeSetIterator(failNorm);
            UnicodeSet pieces = new UnicodeSet();
            while (it.next()) {
                String s = UTF16.valueOf(it.codepoint);
                String d = Normalizer.normalize(s, Normalizer.NFD, 0);
                pw.println("Norm:" + s + ", " + com.ibm.icu.impl.Utility.hex(s)
                        + " " + UCharacter.getName(it.codepoint) + "; " + d
                        + ", " + com.ibm.icu.impl.Utility.hex(d) + ", ");
                pw.println(UCharacter.getName(d.charAt(1)) + "<br>");
                if (UCharacter.getName(d.charAt(1)).indexOf("LENGTH") >= 0)
                    pieces.add(d.charAt(1));
            }
            pw.println(pieces);
        }
        if (true) {
            pw.println("<h2>Failed Round-Trip</h2>");
            pw.println("<table border='1' cellspacing='0'>");
            pw
                    .println("<tr><th width='33%'>Indic</th><th width='33%'>Latin</th<th width='33%'>Indic</th></tr>");

            Iterator cit = latinFail.iterator();
            while (cit.hasNext()) {
                pw.println("<tr>");
                String[] badItems = (String[]) cit.next();
                //pw.println("<th>" + badItems[0] + "</th>");
                for (int i = 0; i < badItems.length; ++i) {
                    showCell(pw, badItems[i], " class='miss'");
                }
                pw.println("</tr>");
            }
            pw.println("</table>");
        }

        pw.println("</table></body></html>");
        pw.close();
    }

    private static void getEquivalentCharacters(SimpleEquivalenceClass equivalenceClass, Set latinFail) {
        UnicodeSet failures = new UnicodeSet();
        for (int i = 0; i < indicScripts.length; ++i) {
            if (indicScripts[i] == UScript.LATIN)
                continue;
            String source = names[i];
            System.out.println(source);
            UnicodeSet sourceChars = sets[i];

            for (int j = 0; j < indicScripts.length; ++j) {
                if (i == j) {
                    continue;
                }
                String target = names[j];
                Transliterator forward = getTransliterator(source, target);
                Transliterator backward = getTransliterator(target, source);
                UnicodeSetIterator it = new UnicodeSetIterator(sourceChars);
                while (it.next()) {
                    if (lengthMarks.contains(it.codepoint))
                        continue;
                    String s = it.getString();
                    s = Normalizer.normalize(s, Normalizer.NFC, 0);
                    // if (!Normalizer.isNormalized(s,Normalizer.NFC,0))
                    // continue;
//                    if (!s.equals(Normalizer.normalize(s, Normalizer.NFD, 0))) {
//                        failNorm.add(it.codepoint);
//                    }
                    String t = forward.transliterate(s); // fix(forward.transliterate(s));
                    if (t.equals(testString)) {
                        System.out.println("debug");
                    }

                    String r = backward.transliterate(t); // fix(backward.transliterate(t));
                    if (Normalizer.compare(s, r, 0) == 0 && Normalizer.compare(s,t,0) != 0) {
                        if (indicScripts[j] != UScript.LATIN)
//                            if (s.compareTo("9") <= 0 || t.compareTo("9")<= 0) {
//                                System.out.println(s + "\t" + t);
//                            }
                            equivalenceClass.add(s, t);
                    } else {
                        failures.add(it.codepoint);
                        if (indicScripts[j] == UScript.LATIN) {
                            //String age = UCharacter.getStringPropertyValue(UProperty.AGE, it.codepoint, 0);
                            latinFail.add(new String[] { s, t, r });
                        }
                    }
                }
            }
        }
        for (UnicodeSetIterator it = new UnicodeSetIterator(failures); it.next();) {
            String v = it.getString();
            equivalenceClass.add(v, v); // add singleton
        }
    }

    private static void populateScriptInfo() {
        for (int i = 0; i < indicScripts.length; ++i) {
            if (indicScripts[i] == UScript.COMMON) {
                names[i] = "InterIndic";
                sets[i] = new UnicodeSet(); //  - [\uE000 \uE066 \uE051-\uE054 \uE064 \uE065 \uE070 \uE073 \uE075 \uE03A]
                fallbacks[i] = Transliterator.getInstance("null");
                continue;
            }
            names[i] = UScript.getName(indicScripts[i]);
            sets[i] = new UnicodeSet("[[:" + names[i] + ":]&[[:L:][:M:][:Nd:]]]"); // 
            fallbacks[i] = Transliterator.getInstance("any-" + names[i]);
        }
        // populate the interindic set
        // add everything that maps FROM private use
        for (int i = 2; i < indicScripts.length; ++i) {
            Transliterator forward = getTransliterator(names[i], "InterIndic");
            for (UnicodeSetIterator it = new UnicodeSetIterator(sets[i]); it.next();) {
                String resultFromInterindic = it.getString();
                if (lengthMarks.containsAll(resultFromInterindic)) continue;
                sets[1].addAll(forward.transliterate(resultFromInterindic));
            }
        }
        sets[UScript.COMMON].retainAll(new UnicodeSet("[:Co:]"));
        System.out.println("InterIndic: " + sets[1]);
    }

    private static String findItemInScript(Set equivs, UnicodeSet scriptSet) {
        Iterator sit;
        String item;
        sit = equivs.iterator();
        item = "";
        // find the item that is all in script i
        while (sit.hasNext()) {
            String trial = (String) sit.next();
            if (!scriptSet.containsAll(trial))
                continue;
            item = trial;
            break;
        }
        return item;
    }

    private static Transliterator getTransliterator(String source, String target) {
        String id = source + '-' + target;
        if (id.indexOf("InterIndic") >= 0) id = "NFD; " + id + "; NFC";
        if (useICU) return Transliterator.getInstance(id);
        return CLDRTransforms.getInstance(id);
    }
    
    private static void showLatin() throws IOException {
        ParsedTransformID parsedID = new ParsedTransformID();
        UnicodeSet stuffToSkip = new UnicodeSet("[[:HangulSyllableType=LVT:][:HangulSyllableType=LV:][:NFKD_QuickCheck=No:][\\U00010000-\\U0010FFFF]]");

        Set s = getAvailableTransliterators();
        Set ids = new TreeSet();
        Map id_unmapped = new HashMap();
        Map id_noRoundTrip = new HashMap();
        Set latinItems = new TreeSet(Collator.getInstance(ULocale.ENGLISH));
        
        // gather info
        for (Iterator i = s.iterator(); i.hasNext();) {
            String id = (String)i.next();
            UnicodeSet unmapped = new UnicodeSet();
            id_unmapped.put(id, unmapped);
            UnicodeSet noRoundTrip = new UnicodeSet();
            id_noRoundTrip.put(id, noRoundTrip);
            parsedID.set(id);
            if (!parsedID.target.equals("Latin")) continue;
            if (parsedID.source.equals("Han")) continue;
            String script = parsedID.source;
            int scriptCode = UScript.getCodeFromName(script);
            if (scriptCode < 0) {
                System.out.println("Skipping id: " + script);
                continue;
            }
            ids.add(id);
            String scriptName = UScript.getShortName(scriptCode);
            UnicodeSet targetSet = new UnicodeSet("[:script=" + scriptName + ":]");
            targetSet.removeAll(stuffToSkip);

            Transliterator native_latin = getTransliterator(parsedID.source, parsedID.target);
            Transliterator latin_native = getTransliterator(parsedID.target, parsedID.source);
            for (UnicodeSetIterator it = new UnicodeSetIterator(targetSet); it.next();) {
                String source = it.getString();
                String target = native_latin.transliterate(source);
                if (source.equals(target)) {
                    unmapped.add(source);
                    continue;
                }
                if (target.length() != 0) {
                    latinItems.add(target);
                }
                String back = latin_native.transliterate(target);
                if (!source.equals(back)) {
                    noRoundTrip.add(source);
                }
            }
        }
        
        PrintWriter pw = BagFormatter.openUTF8Writer(Utility.GEN_DIRECTORY + "gen/charts/transforms/", "transLatin.html");
        pw.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
        pw.println("<title>Latin Transliteration Chart</title><style>");
        pw.println("td { text-align: Center; font-size: 200% }");
        pw.println("tt { font-size: 50% }");
        pw.println("td.miss { background-color: #CCCCFF }");
        pw.println("td.none { background-color: #FFCCCC }");
        pw.println("</style></head><body bgcolor='#FFFFFF'>");

        pw.println("<table border='1' cellspacing='0'>");
        pw.println("<tr><th>Latin</th>");       
        for (Iterator it2 = ids.iterator(); it2.hasNext();) {
            parsedID.set((String)it2.next());
            pw.println("<th>" + parsedID.source + (parsedID.variant == null ? "" : "/" + parsedID.variant) + "</th>");       
        }
        pw.println("</tr>");
        
        UnicodeSet sourceSet = new UnicodeSet();
        UnicodeSet targetSet = new UnicodeSet();
        
        for (Iterator it = latinItems.iterator(); it.hasNext();) {
            String target = (String)it.next();
            pw.println("<tr>");
            showCell(pw, target, "");
            for (Iterator it2 = ids.iterator(); it2.hasNext();) {
                String id = (String)it2.next();
                parsedID.set(id);
                Transliterator native_latin = getTransliterator(parsedID.source, parsedID.target);
                Transliterator latin_native = getTransliterator(parsedID.target, parsedID.source);

                String source = latin_native.transliterate(target);
                // if some character is not transliterated, suppress
                if (sourceSet.clear().addAll(source).containsSome(targetSet.clear().addAll(target))) {
                    showCell(pw, "", "");
                    continue;
                }
                String back = native_latin.transliterate(source);

                String classString = target.equals(back) ? "" : " class='miss'";
                showCell(pw, source, classString);
            }
            pw.println("</tr>");
        }
        pw.println("</table>");
        
        pw.println("<h2>Unmapped</h2>");
        pw.println("<table border='1' cellspacing='0'>");
        for (Iterator it2 = ids.iterator(); it2.hasNext();) {
            String id = (String)it2.next();
            UnicodeSet unmapped = (UnicodeSet) id_unmapped.get(id);
            if (unmapped.size() == 0) continue;
            pw.println("<tr>");       
            parsedID.set(id);
            pw.println("<th>" + parsedID.source + (parsedID.variant == null ? "" : "/" + parsedID.variant) + "</th>");
             for (UnicodeSetIterator it = new UnicodeSetIterator(unmapped); it.next();) {
                String source = it.getString();
                showCell(pw, source, " class='missing'");
            }           
            pw.println("</tr>");
        }
        pw.println("</table>");
        
        pw.println("<h2>No Round Trip</h2>");
        pw.println("<table border='1' cellspacing='0'>");
        for (Iterator it2 = ids.iterator(); it2.hasNext();) {
            String id = (String)it2.next();
            UnicodeSet noRoundTrip = (UnicodeSet) id_noRoundTrip.get(id);
            if (noRoundTrip.size() == 0) continue;
            pw.println("<tr>");       
            parsedID.set(id);
            pw.println("<th>" + parsedID.source + (parsedID.variant == null ? "" : "/" + parsedID.variant) + "</th>");
             for (UnicodeSetIterator it = new UnicodeSetIterator(noRoundTrip); it.next();) {
                String source = it.getString();
                showCell(pw, source, " class='missing'");
            }           
            pw.println("</tr>");
        }
        pw.println("</table>");
        
        pw.println("</body></html>");
        pw.close();
    }
    
    private static class TranslitStatus {
        String source;
        String back;

        public TranslitStatus(String source, String back) {
            this.source = source;
            this.back = back;
        }
    }
    
    public static class ParsedTransformID {
        public String source;
        public String target;
        public String variant;
        public void set(String id) {
            variant = null;
            int pos = id.indexOf('-');
            if (pos < 0) {
                source = "Any";
                target = id;
                return;
            }
            source = id.substring(0,pos);
            int pos2 = id.indexOf('/', pos);
            if (pos2 < 0) {
                target = id.substring(pos+1);
                return;
            }
            target = id.substring(pos+1, pos2);
            variant = id.substring(pos2+1);
        }
    }
    
    private static Set getAvailableTransliterators() {
        if (useICU ) {
            Set results = new HashSet();
            for (Enumeration e = Transliterator.getAvailableIDs(); e.hasMoreElements();) {
                results.add(e.nextElement());
            }
            return results;
        }
        else return CLDRTransforms.getAvailableTransforms();
    }

    private static void showCell(PrintWriter pw, String item, String classString) {
        String backup = item;
        String name = getName(item, "; ");
        if (item.equals("")) {
            backup = "\u00a0";
            classString = " class='none'";
            name = "{missing}";
        } else if (item.charAt(0) >= '\uE000') {
            backup = "\u00a0";
        }

        name = TransliteratorUtilities.toXML.transliterate(name);
        backup = TransliteratorUtilities.toHTML.transliterate(backup);
        pw.print("<td" + classString + " title='" + name + "'>\u00a0\u00a0"
                + backup + "\u00a0\u00a0<br><tt>"
                + com.ibm.icu.impl.Utility.hex(item) + "</tt></td>");
    }

    public static String fix(String s) {
        if (s.equals("\u0946\u093E"))
            return "\u094A";
        if (s.equals("\u0C46\u0C3E"))
            return "\u0C4A";
        if (s.equals("\u0CC6\u0CBE"))
            return "\u0CCA";

        if (s.equals("\u0947\u093E"))
            return "\u094B";
        if (s.equals("\u0A47\u0A3E"))
            return "\u0A4B";
        if (s.equals("\u0AC7\u0ABE"))
            return "\u0ACB";
        if (s.equals("\u0C47\u0C3E"))
            return "\u0C4B";
        if (s.equals("\u0CC7\u0CBE"))
            return "\u0CCB";

        // return Normalizer.normalize(s,Normalizer.NFD,0);
        return s;
    }

    public static String getName(String s, String separator) {
        int cp;
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(s, i);
            if (i != 0)
                sb.append(separator);
            sb.append(UCharacter.getName(cp));
        }
        return sb.toString();
    }

    static class CollectionOfComparablesComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            if (o1 == null) {
                if (o2 == null) return 0;
                return -1;
            } else if (o2 == null) {
                return 1;
            }
            Iterator i1 = ((Collection) o1).iterator();
            Iterator i2 = ((Collection) o2).iterator();
            while (i1.hasNext() && i2.hasNext()) {
                Comparable a = (Comparable) i1.next();
                Comparable b = (Comparable) i2.next();
                int result = a.compareTo(b);
                if (result != 0) {
                    return result;
                }
            }
            // if we run out, the shortest one is first
            if (i1.hasNext())
                return 1;
            if (i2.hasNext())
                return -1;
            return 0;
        }

    }

    static class ReverseComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            String a = o1.toString();
            char a1 = a.charAt(0);
            String b = o2.toString();
            char b1 = b.charAt(0);
            if (a1 < 0x900 && b1 > 0x900)
                return -1;
            if (a1 > 0x900 && b1 < 0x900)
                return +1;
            return a.compareTo(b);
        }
    }


}