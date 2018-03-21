package org.unicode.cldr.util;

import java.io.PrintWriter;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class PluralSnapshot implements Comparable<PluralSnapshot> {

    public enum Plurals {
        zero, one, two, few, many, other("x");
        final String abb;

        Plurals(String s) {
            abb = s;
        }

        Plurals() {
            abb = name().substring(0, 1);
        }

        public String abbreviated() {
            return abb;
        }
    }

    public enum Integral {
        integer, fraction
    }

    static final int LEN = 128;

    static Set<Double> zeroOne = new TreeSet<Double>();
    static {
        zeroOne.add(0.0d);
        zeroOne.add(1.0d);
    }

    public final int count;
    public final int count01;

    EnumSet<Plurals> coveredBy01 = EnumSet.noneOf(Plurals.class);
    EnumSet<Plurals> not01 = EnumSet.noneOf(Plurals.class);

    Plurals[] plurals = new Plurals[LEN];

    private BitSet pluralsTransitionAt;

    static NumberFormat nf = NumberFormat.getInstance(ULocale.ENGLISH);

    public static class SnapshotInfo implements Iterable<Entry<PluralSnapshot, Set<String>>> {
        // private Relation<String,String> rulesToLocales = Relation.of(new HashMap<String,Set<String>>(),
        // TreeSet.class);
        private Relation<PluralSnapshot, String> snapshotToLocales = Relation.of(
            new TreeMap<PluralSnapshot, Set<String>>(), TreeSet.class);
        private BitSet pluralsTransitionAt = new BitSet();
        private Integral integral;

        private SnapshotInfo(PluralType pluralType, Integral integral) {
            this.integral = integral;
            SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo.getInstance();
            Map<String, PluralSnapshot> rulesToSnapshot = new HashMap<String, PluralSnapshot>();
            for (String locale : supplementalDataInfo.getPluralLocales(pluralType)) {
                PluralInfo plurals = supplementalDataInfo.getPlurals(pluralType, locale);
                String rules = plurals.getRules();
                PluralSnapshot snap = rulesToSnapshot.get(rules);
                if (snap == null) {
                    PluralRules pluralRules = PluralRules.createRules(rules);
                    snap = new PluralSnapshot(pluralRules, integral, pluralsTransitionAt);
                    rulesToSnapshot.put(rules, snap);
                }
                snapshotToLocales.put(snap, locale);
            }
        }

        public Iterator<Entry<PluralSnapshot, Set<String>>> iterator() {
            return snapshotToLocales.keyValuesSet().iterator();
        }

        public String toOverview() {
            StringBuilder result = new StringBuilder();
            result.append("Transitions:\t 0");
            for (int i = pluralsTransitionAt.nextSetBit(0); i >= 0; i = pluralsTransitionAt.nextSetBit(i + 1)) {
                result.append(",").append(i);
            }
            return result.toString();
        }

        public String toHtmlStringHeader() {
            StringBuilder result = new StringBuilder();
            result.append("<tr><th class='h'></th>");
            int next = -2;
            for (int i = pluralsTransitionAt.nextSetBit(0); i >= 0; i = next) {
                next = pluralsTransitionAt.nextSetBit(i + 1);
                result.append("<th class='h'>").append(i);
                if (integral == Integral.fraction) {
                    result.append(".x");
                }
                int vnext = next == -1 ? LEN : next;
                if (vnext > i + 1) {
                    result.append("-").append(String.valueOf(vnext - 1)
                        + (integral == Integral.fraction ? ".x" : ""));
                }
                result.append("</th>");
            }
            result.append("</tr>");
            return result.toString();
        }
    }

    private static final EnumMap<PluralType, EnumMap<Integral, SnapshotInfo>> SINGLETONS = new EnumMap<PluralType, EnumMap<Integral, SnapshotInfo>>(
        PluralType.class);
    static {
        SINGLETONS.put(PluralType.cardinal, new EnumMap<Integral, SnapshotInfo>(Integral.class));
        SINGLETONS.put(PluralType.ordinal, new EnumMap<Integral, SnapshotInfo>(Integral.class));
    }
    private EnumSet<Plurals> found;

    public static SnapshotInfo getInstance(PluralType pluralType, Integral integral) {
        EnumMap<Integral, SnapshotInfo> temp = SINGLETONS.get(pluralType);
        SnapshotInfo result = temp.get(integral);
        if (result == null) {
            temp.put(integral, result = new SnapshotInfo(pluralType, integral));
        }
        return result;
    }

    PluralSnapshot(PluralRules pluralRules, Integral integral, BitSet pluralsTransitionAt) {
        this.pluralsTransitionAt = pluralsTransitionAt;
        double offset = integral == Integral.integer ? 0 : 0.5;
        found = EnumSet.noneOf(Plurals.class);
        not01 = EnumSet.noneOf(Plurals.class);
        pluralsTransitionAt.set(0);
        for (int i = 0; i < plurals.length; ++i) {
            final double probe = i + offset;
            final Plurals plural = Plurals.valueOf(pluralRules.select(probe));
            plurals[i] = plural;
            found.add(plural);
            if (probe != 0.0d && probe != 1.0d) {
                not01.add(plural);
            }
            if (i > 0 && plural != plurals[i - 1]) {
                pluralsTransitionAt.set(i);
            }
        }
        coveredBy01.addAll(found);
        coveredBy01.removeAll(not01);
        count = found.size();
        count01 = 2 + not01.size();
    }

    @Override
    public int compareTo(PluralSnapshot other) {
        int diff = count - other.count;
        if (diff != 0) return diff;
        diff = UnicodeSet.compare(found, other.found);
        if (diff != 0) return diff;
        Iterator<Plurals> it = other.not01.iterator();
        for (Plurals p : not01) { // same length, so ok
            Plurals otherOne = it.next();
            diff = p.compareTo(otherOne);
            if (diff != 0) return diff;
        }
        for (int i = 0; i < plurals.length; ++i) {
            diff = plurals[i].compareTo(other.plurals[i]);
            if (diff != 0) return diff;
        }
        return 0;
    }

    public boolean equals(Object other) {
        return compareTo((PluralSnapshot) other) == 0;
    }

    public int hashCode() {
        return count; // brain dead but we don't care.
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Plurals: 0, 1, ").append(CollectionUtilities.join(not01, ", "));
        if (coveredBy01.size() != 0) {
            result.append("\nCovered by {0,1}:\t").append(coveredBy01);
        }
        result.append("\nInt:\t");
        appendItems(result, plurals, 0);
        return result.toString();
    }

    public String toHtmlString() {
        StringBuilder result = new StringBuilder();
        result.append("<tr>");

        Plurals lastItem = null;
        int colSpan = 0;
        for (int i = pluralsTransitionAt.nextSetBit(0); i >= 0; i = pluralsTransitionAt.nextSetBit(i + 1)) {
            Plurals item = plurals[i];
            if (item == lastItem) {
                colSpan += 1;
                continue;
            }
            if (lastItem != null) {
                appendCell(result, colSpan, lastItem);
                colSpan = 0;
            }
            colSpan += 1;
            lastItem = item;
        }
        appendCell(result, colSpan, lastItem);
        result.append("</tr>");
        return result.toString();
    }

    private void appendCell(StringBuilder result, int colSpan, Plurals item) {
        result.append("<td class='").append(item.abbreviated());
        if (coveredBy01.contains(item)) {
            result.append(" c01");
        }
        result.append("'");
        if (colSpan != 1) {
            result.append(" colSpan='" + colSpan + "'");
        }
        result.append(" title='").append(item.toString()).append("'>")
            .append(item.abbreviated()).append("</td>");
    }

    private static <T> void appendItems(StringBuilder result, T[] plurals3, double offset) {
        int start = 0;
        result.append(plurals3[0]).append("=").append(nf.format(start + offset));
        for (int i = 1; i < plurals3.length; ++i) {
            if (!plurals3[i].equals(plurals3[i - 1])) {
                if (i - 1 != start) {
                    result.append("-").append(nf.format(i - 1 + offset));
                }
                result.append("; ").append(plurals3[i]).append("=").append(nf.format(i + offset));
                start = i;
            }
        }
        if (plurals3.length - 1 != start) {
            result.append("-").append(nf.format(plurals3.length - 1 + offset));
        }
    }

    public static String getDefaultStyles() {
        return "<style type='text/css'>\n"
            +
            "td.l, td.z, td.o, td.t, td.f, td.m, td.x, th.h, table.pluralComp {border: 1px solid #666; font-size: 8pt}\n"
            +
            "table.pluralComp {border-collapse:collapse}\n" +
            "th.h {background-color:#EEE; border-top: 2px solid #000; border-bottom: 2px solid #000;}\n" +
            "td.l {background-color:#C0C; border-top: 2px solid #000; color:white; font-weight: bold}\n" +
            "td.z {background-color:#F00}\n" +
            "td.o {background-color:#DD0}\n" +
            "td.t {background-color:#0F0}\n" +
            "td.f {background-color:#0DD}\n" +
            "td.m {background-color:#99F}\n" +
            "td.x {background-color:#CCC}\n" +
            "td.c01 {text-decoration:underline}\n";
    }

    public static void writeTables(CLDRFile english, PrintWriter out) {
        for (PluralType pluralType : PluralType.values()) {
            for (Integral integral : Integral.values()) {
                if (pluralType == PluralType.ordinal && integral == Integral.fraction) {
                    continue;
                }
                SnapshotInfo info = PluralSnapshot.getInstance(pluralType, integral);

                System.out.println("\n" + integral + "\n");
                System.out.println(info.toOverview());

                String title = UCharacter.toTitleCase(pluralType.toString(), null)
                    + "-" + UCharacter.toTitleCase(integral.toString(), null);
                out.println("<h3>" + CldrUtility.getDoubleLinkedText(title) + "</h3>");
                if (integral == Integral.fraction) {
                    out.println("<p><i>This table has not yet been updated to capture the new types of plural fraction behavior.</i></p>");
                }
                out.println("<table class='pluralComp'>");
                int lastCount = -1;
                int lastCount01 = -1;

                out.println(info.toHtmlStringHeader());

                for (Entry<PluralSnapshot, Set<String>> ruleEntry : info) {
                    PluralSnapshot ss = ruleEntry.getKey();
                    Set<String> locales = ruleEntry.getValue();
                    System.out.println();
                    System.out.println(locales);
                    System.out.println(ss);
                    // if (ss.count != lastCount) {
                    // out.println(info.toHtmlStringHeader());
                    // lastCount = ss.count;
                    // lastCount01 = ss.count01;
                    // }
                    Map<String, String> fullLocales = new TreeMap<String, String>();
                    for (String localeId : locales) {
                        String name = english.getName(localeId);
                        fullLocales.put(name, localeId);
                    }
                    out.print("<tr><td rowSpan='2'>" + ss.count +
                        "</td><td class='l' colSpan='121'>");
                    int count = 0;
                    for (Entry<String, String> entry : fullLocales.entrySet()) {
                        String code = entry.getValue();
                        out.print("<span title='" + code + "'>"
                            + (count == 0 ? "" : ", ")
                            + CldrUtility.getDoubleLinkedText(code + "-comp", entry.getKey())
                            + "</span>");
                        count++;
                    }
                    out.println("</td></tr>");
                    out.println(ss.toHtmlString());
                    out.println(info.toHtmlStringHeader());
                }
                out.println("</table>");
            }
        }
    }
}
