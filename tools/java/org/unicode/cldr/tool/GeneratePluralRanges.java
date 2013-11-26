package org.unicode.cldr.tool;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.PluralRanges;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.FixedDecimal;
import com.ibm.icu.util.Output;

public class GeneratePluralRanges {
    private static final boolean MINIMAL = true;

    public static void main(String[] args) {
        new GeneratePluralRanges().reformatPluralRanges();
    }

    static TestInfo testInfo = TestInfo.getInstance();

    private static final SupplementalDataInfo SUPPLEMENTAL = testInfo.getSupplementalDataInfo();

    public static final Comparator<Set<String>> STRING_SET_COMPARATOR = new SetComparator<String, Set<String>>();
    public static final Comparator<Set<Count>> COUNT_SET_COMPARATOR = new SetComparator<Count, Set<Count>>();

    static final class SetComparator<T extends Comparable<T>, U extends Set<T>> implements Comparator<U> {
        public int compare(U o1, U o2) {
            return CollectionUtilities.compare((Collection<T>)o1, (Collection<T>)o2);
        }
    };


    public void reformatPluralRanges() {
        Map<Set<Count>, Relation<Set<String>, String>> seen 
        = new TreeMap<Set<Count>, Relation<Set<String>, String>>(COUNT_SET_COMPARATOR);

        for (String locale : SUPPLEMENTAL.getPluralRangesLocales()) {

            PluralRanges pluralRanges = SUPPLEMENTAL.getPluralRanges(locale);
            if (pluralRanges == null) {
                pluralRanges = new PluralRanges().freeze();
            }
            PluralInfo pluralInfo = SUPPLEMENTAL.getPlurals(locale);
            Set<Count> counts = pluralInfo.getCounts();

            Set<String> s;
            if (false) {
                System.out.println("Minimized, but not ready for prime-time");
                s = minimize(pluralRanges, pluralInfo);
            } else {
                s = reformat(pluralRanges, counts);
            }
            Relation<Set<String>, String> item = seen.get(counts);
            if (item == null) {
                seen.put(counts, 
                    item = Relation.of(new TreeMap<Set<String>,Set<String>>(STRING_SET_COMPARATOR), TreeSet.class));
            }
            item.put(s, locale);
        }

        for (Entry<Set<Count>, Relation<Set<String>, String>> entry0 : seen.entrySet()) {
            System.out.println("\n<!-- " + CollectionUtilities.join(entry0.getKey(), ", ") + " -->");
            for (Entry<Set<String>, Set<String>> entry : entry0.getValue().keyValuesSet()) {
                System.out.println("\t\t<pluralRanges locales=\"" + CollectionUtilities.join(entry.getValue(), " ") + "\">");
                for (String line : entry.getKey()) {
                    System.out.println("\t\t\t" + line);
                }
                System.out.println("\t\t</pluralRanges>");
            }
        }
    }

    enum RangeStrategy {other, end, start, mixed}

    public Set<String> reformat(PluralRanges pluralRanges, Set<Count> counts) {
        Set<String> s;
        s = new LinkedHashSet<String>();
        // first determine the general principle

        //        EnumSet<RangeStrategy> strategy = EnumSet.allOf(RangeStrategy.class);
        //        Count firstResult = null;
        //        for (Count start : counts) {
        //            for (Count end : counts) {
        //                Count result = pluralRanges.getExplicit(start, end);
        //                if (result == null) {
        //                    continue;
        //                } else if (firstResult == null) {
        //                    firstResult = result;
        //                }
        //                if (result != start) {
        //                    strategy.remove(RangeStrategy.start);
        //                }
        //                if (result != end) {
        //                    strategy.remove(RangeStrategy.end);
        //                }
        //                if (result != Count.other) {
        //                    strategy.remove(RangeStrategy.other);
        //                }
        //           }
        //        }
        //        s.add("<!-- Range Principle: " + strategy.iterator().next() + " -->");
        for (Count start : counts) {
            for (Count end : counts) {
                Count result = pluralRanges.getExplicit(start, end);
                if (result == null) {
                    continue;
                }
                String line = PluralRanges.showRange(start, end, result);
                s.add(line);
            }
        }
        return s;
    }

    Set<String> minimize(PluralRanges pluralRanges, PluralInfo pluralInfo) {
        int count = Count.VALUES.size();
        Set<String> result = new LinkedHashSet<String>();
        PluralRules pluralRules = pluralInfo.getPluralRules();
        // make it easier to manage
        PluralRanges.Matrix matrix = new PluralRanges.Matrix();
        boolean allOther = true;
        Output<FixedDecimal> maxSample = new Output();
        Output<FixedDecimal> minSample = new Output();
        for (Count s : Count.VALUES) {
            for (Count e : Count.VALUES) {
                if (!pluralInfo.rangeExists(s, e, minSample, maxSample)) {
                    continue;
                }
                Count r = pluralRanges.getExplicit(s,e);
                matrix.set(s, e, r);
                if (r != Count.other) {
                    allOther = false;
                }
            }
        }
        // if everything is 'other', we are done
//        if (allOther == true) {
//            return result;
//        }
        EnumSet<Count> endDone = EnumSet.noneOf(Count.class);
        EnumSet<Count> startDone = EnumSet.noneOf(Count.class);
        if (MINIMAL) {
            for (Count end : pluralInfo.getCounts()) {
                Count r = matrix.endSame(end);
                if (r != null 
                    //&& r != Count.other
                    ) {
                    result.add("<pluralRange" +
                        "              \t\tend=\"" + end 
                        + "\"\tresult=\"" + r + "\"/>"
                        );
                    endDone.add(end);
                }
            }
            Output<Boolean> emit = new Output();
            for (Count start : pluralInfo.getCounts()) {
                Count r = matrix.startSame(start, endDone, emit);
                if (r != null 
                    // && r != Count.other
                    ) {
                    if (emit.value) {
                        result.add("<pluralRange" +
                            "\tstart=\"" + start 
                            + "\"          \t\tresult=\"" + r + "\"/>"
                            );
                    }
                    startDone.add(start);
                }
            }
        }
        //Set<String> skip = new LinkedHashSet<String>();
        for (Count end : pluralInfo.getCounts()) {
            if (endDone.contains(end)) {
                continue;
            }
            for (Count start : pluralInfo.getCounts()) {
                if (startDone.contains(start)) {
                    continue;
                }
                Count r = matrix.get(start, end);
                if (r != null 
                    //&& !(MINIMAL && r == Count.other)
                    ) {
                    result.add(PluralRanges.showRange(start, end, r)
                        );
                } else {
                    result.add("<!-- <pluralRange" +
                        "\tstart=\"" + start 
                        + "\" \tend=\"" + end
                        + "\" \tresult=\"" + r + "\"/> -->"
                        );

                }

            }
        }
        return result;
    }


}
