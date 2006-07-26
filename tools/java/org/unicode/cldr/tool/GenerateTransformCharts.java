/**
 *******************************************************************************
 * Copyright (C) 2001-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package org.unicode.cldr.tool;

import org.unicode.cldr.util.CLDRTransforms;
import org.unicode.cldr.util.Utility;

import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
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


    public static void main(String[] args) throws IOException {
        System.out.println("Start");
        populateScriptInfo();
        
        EquivClass equivalenceClass = new EquivClass(new UTF16.StringComparator(true,false,0)); // new ReverseComparator());
        Transliterator anyToLatin = Transliterator.getInstance("any-latin");

        UnicodeSet failNorm = new UnicodeSet();
        // Collator sc = Collator.getInstance(ULocale.ENGLISH);
        // sc.setStrength(Collator.IDENTICAL);
        Comparator sc = new UTF16.StringComparator(true, false, 0);
        Set latinFail = new TreeSet(new ArrayComparator(new Comparator[] { sc, sc, sc }));

        getEquivalentCharacters(equivalenceClass, latinFail);
        
        printChart(equivalenceClass, anyToLatin, failNorm, latinFail);
        System.out.println("Done");
    }

    private static void printChart(EquivClass equivalenceClass, Transliterator anyToLatin, UnicodeSet failNorm, Set latinFail) throws IOException {
        // collect equivalents
        PrintWriter pw = BagFormatter.openUTF8Writer(Utility.GEN_DIRECTORY + "gen/charts/transforms/", "transChart.html");
        pw
                .println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
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

    private static void getEquivalentCharacters(EquivClass equivalenceClass, Set latinFail) {
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
        if (true) return Transliterator.getInstance(id);
        return CLDRTransforms.getInstance(id);
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

    static class EquivClass {
        EquivClass(Comparator c) {
            comparator = c;
        }

        private HashMap itemToSet = new HashMap();

        private Comparator comparator;

        void add(Object a, Object b) {
            Set sa = (Set) itemToSet.get(a);
            Set sb = (Set) itemToSet.get(b);
            if (sa == null && sb == null) { // new set!
                Set s = new TreeSet(comparator);
                s.add(a);
                s.add(b);
                itemToSet.put(a, s);
                itemToSet.put(b, s);
            } else if (sa == null) {
                sb.add(a);
            } else if (sb == null) {
                sa.add(b);
            } else { // merge sets, dumping sb
                sa.addAll(sb);
                Iterator it = sb.iterator();
                while (it.hasNext()) {
                    itemToSet.put(it.next(), sa);
                }
            }
        }

        private class MyIterator implements Iterator {
            private Iterator it;

            MyIterator(Comparator comp) {
                TreeSet values = new TreeSet(comp);
                values.addAll(itemToSet.values());
                it = values.iterator();
            }

            public boolean hasNext() {
                return it.hasNext();
            }

            public Object next() {
                return it.next();
            }

            public void remove() {
                throw new IllegalArgumentException("can't remove");
            }
        }

        public Iterator getSetIterator(Comparator comp) {
            return new MyIterator(comp);
        }

    }
}