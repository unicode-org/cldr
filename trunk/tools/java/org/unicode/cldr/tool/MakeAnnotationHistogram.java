package org.unicode.cldr.tool;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.unicode.cldr.util.Annotations;
import org.unicode.cldr.util.Annotations.AnnotationSet;
import org.unicode.cldr.util.Counter;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class MakeAnnotationHistogram {
    private static final Integer ZERO = (Integer) 0;

    public static void main(String[] args) {
        AnnotationSet english = Annotations.getDataSet("en");
        UnicodeSet codes = english.getExplicitValues().keySet();
        Multimap<String, ULocale> missingCodeToLocales = TreeMultimap.create();
        Map<String, Counter<Integer>> codeToCounter = new TreeMap<>();
        int maxmax = 0;
        for (String locale : Annotations.getAvailable()) {
            ULocale ulocale = new ULocale(locale);
            AnnotationSet annotationSet = Annotations.getDataSet(locale);
            Counter<Integer> counter = new Counter<>();

            int max = 0;
            for (String code : codes) {
                String name = annotationSet.getShortName(code);
                if (name == null) {
                    missingCodeToLocales.put(code, ulocale);
                    continue;
                }
                int clusterCount = getCount(name, ulocale);
                counter.add(clusterCount, 1);
                max = Math.max(max, clusterCount);

                Counter<Integer> counterForCode = codeToCounter.get(code);
                if (counterForCode == null) {
                    codeToCounter.put(code, counterForCode = new Counter<>());
                }
                counterForCode.add(clusterCount, 1);
            }
            System.out.print(locale + "\t" + ulocale.getDisplayName());
            for (int i = 1; i <= max; ++i) {
                System.out.print("\t" + emptyIfZero(counter.getCount(i)));
            }
            System.out.println();
            if (maxmax < max) {
                maxmax = max;
            }
        }
        System.out.println("Missing");
        for (Entry<String, Collection<ULocale>> entry : missingCodeToLocales.asMap().entrySet()) {
            System.out.println(entry.getKey() + "\t" + entry.getValue());
        }
        System.out.println("CodeToGCs");
        for (Entry<String, Counter<Integer>> entry : codeToCounter.entrySet()) {
            String code = entry.getKey();
            Counter<Integer> counter = entry.getValue();
            System.out.print(code);
            for (int i = 1; i <= maxmax; ++i) {
                System.out.print("\t" + emptyIfZero(counter.getCount(i)));
            }
            System.out.println();
        }
    }

    private static String emptyIfZero(long count) {
        return count == 0 ? "" : String.valueOf(count);
    }

    private static int getCount(String name, ULocale locale) {
        BreakIterator boundary = BreakIterator.getCharacterInstance(locale);
        int count = 0;
        boundary.setText(name);

        int start = boundary.first();
        for (int end = boundary.next(); end != BreakIterator.DONE; start = end, end = boundary.next()) {
            if (name.charAt(start) == ' ') {
                continue;
            }
            ++count;
        }
        return count;
    }
}
