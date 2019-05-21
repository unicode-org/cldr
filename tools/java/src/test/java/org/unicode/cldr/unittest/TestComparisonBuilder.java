package org.unicode.cldr.unittest;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.DiscreteComparator;
import org.unicode.cldr.util.DiscreteComparator.Builder;
import org.unicode.cldr.util.DiscreteComparator.CycleException;
import org.unicode.cldr.util.DiscreteComparator.MissingItemException;
import org.unicode.cldr.util.DiscreteComparator.Ordering;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.ElementAttributeInfo;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;

/**
 * Tests the ordering of DTD elements and attributes within the dtd files.
 */
public class TestComparisonBuilder extends TestFmwk {
    public static void main(String[] args) {
        new TestComparisonBuilder().run(args);
    }

    private void verifyOrdering(DiscreteComparator<String> comp,
        Set<String> children) {
        String last = null;
        for (String child : children) {
            if (last != null) {
                int compare = comp.compare(last, child);
                if (compare != -1) {
                    errln("Elements not ordered:\t" + last + ", " + child
                        + "; in " + children);
                    break;
                }
            }
            last = child;
        }
    }

    private void dtdAttributes() {
        // NOTE: This method was originally TestDtdAttributes. It's been
        // disabled
        // because DTD attributes aren't managed
        // by FindDtdOrder, which leads to a lot of ordering cycles being found.
        // There's no point in reordering the dtd contents to make this test
        // pass
        // unless FindDtdOrder also checks attributes.
        Builder<String> builder = new Builder<String>(Ordering.NATURAL);
        for (DtdType dtd : DtdType.values()) {
            Relation<String, String> eaInfo = ElementAttributeInfo.getInstance(
                dtd).getElement2Attributes();
            for (String element : eaInfo.keySet()) {
                Set<String> attributes = eaInfo.getAll(element);
                if (attributes.size() == 0)
                    continue;
                // logln(dtd + ": " + element + ": " + attributes);
                builder.add(attributes);
            }

        }
        DiscreteComparator<String> comp;
        try {
            comp = builder.get();
        } catch (CycleException e) {
            errln(e.getMessage() + ",\t" + builder.getCycle());
            throw e;
        }

        logln("Attribute Ordering:\t" + comp.getOrdering().toString());
        for (DtdType dtd : DtdType.values()) {
            // check that the ordering is right
            Relation<String, String> eaInfo = ElementAttributeInfo.getInstance(
                dtd).getElement2Attributes();
            for (String element : eaInfo.keySet()) {
                Set<String> children = eaInfo.getAll(element);
                verifyOrdering(comp, children);
            }
        }
    }

    public void TestDtdElements() {
        Set<String> specials = new HashSet<String>(Arrays.asList(new String[] {
            "EMPTY", "PCDATA", "ANY" }));
        for (DtdType dtd : DtdType.values()) {
            if (dtd.rootType != dtd) {
                continue;
            }
            Builder<String> builder = new Builder<String>(Ordering.NATURAL);
            builder.add(dtd.toString());
            Relation<String, String> eaInfo = ElementAttributeInfo.getInstance(
                dtd).getElement2Children();
            for (String element : eaInfo.keySet()) {
                Set<String> children = new LinkedHashSet<String>(
                    eaInfo.getAll(element));
                children.removeAll(specials);
                if (children.size() == 0)
                    continue;
                logln(dtd + ": " + element + ": " + children);
                builder.add(children);
            }

            DiscreteComparator<String> comp = builder.get();
            logln("Element Ordering: " + comp.getOrdering().toString());

            Relation<String, String> eaInfo2 = ElementAttributeInfo.getInstance(
                dtd).getElement2Children();
            // check that the ordering is right
            for (String element : eaInfo2.keySet()) {
                Set<String> elements = eaInfo2.getAll(element);
                Set<String> children = new LinkedHashSet<String>(elements);
                children.removeAll(specials);
                verifyOrdering(comp, children);
            }
            // check that all can be ordered
            try {
                Set<String> items = new TreeSet<String>(comp);
                items.addAll(eaInfo2.keySet()); // we'll get exception if it
                // fails
            } catch (Exception e) {
                Set<String> missing = new LinkedHashSet<String>(eaInfo2.keySet());
                missing.removeAll(comp.getOrdering());
                errln(dtd + "\t" + e.getClass().getName() + "\t"
                    + e.getMessage() + ";\tMissing: " + missing);
            }
        }
    }

    public void TestMonkey() {
        Random random = new Random(1);
        Set<R2<Integer, Integer>> soFar = new HashSet<R2<Integer, Integer>>();
        for (int j = 0; j < 100; ++j) {
            int itemCount = 50;
            int linkCount = 1000;
            buildNodes(random, soFar, random.nextInt(itemCount - 1) + 1,
                random.nextInt(linkCount));

            for (Ordering order : Ordering.values()) {
                Builder<Integer> builder = new Builder<Integer>(order);
                for (R2<Integer, Integer> pair : soFar) {
                    builder.add(pair.get0(), pair.get1());
                }
                logln(builder.toString());
                DiscreteComparator<Integer> comp = builder.get();
                logln("\t" + comp);

                for (R2<Integer, Integer> pair : soFar) {
                    int value = comp.compare(pair.get0(), pair.get1());
                    if (value >= 0) {
                        errln("Failure with " + pair);
                    }
                }
            }
        }
    }

    private void buildNodes(Random random, Set<R2<Integer, Integer>> soFar,
        int items, int links) {
        soFar.clear();
        for (int i = 0; i < links; ++i) {
            Integer start = random.nextInt(items);
            Integer end = random.nextInt(10);
            if (start.intValue() <= end.intValue())
                continue;
            soFar.add(Row.of(start, end));
        }
    }

    public void TestCycle3() {
        for (Ordering order : Ordering.values()) {
            Builder<String> builder = new Builder<String>(order);
            builder.add("c", "a");
            builder.add("a", "b");
            builder.add("b", "c");
            mustHaveException(builder);
        }
    }

    public void TestCycle7() {
        for (Ordering order : Ordering.values()) {
            Builder<String> builder = new Builder<String>(order);
            builder.add("f", "a");
            builder.add("a", "b");
            builder.add("b", "c");
            builder.add("b", "q");
            builder.add("c", "d");
            builder.add("d", "e");
            builder.add("e", "f");
            mustHaveException(builder);
        }
    }

    public void TestCycle2() {
        for (Ordering order : Ordering.values()) {
            Builder<String> builder = new Builder<String>(order);
            try {
                builder.add("c", "d");
                builder.add("d", "e");
                builder.add("e", "f");
                builder.add("b", "b");
                DiscreteComparator<String> comp = builder.get();
            } catch (CycleException e) {
                logln("Expected cycle and got one at:\t" + e.getMessage()
                    + ", " + builder.getCycle());
                return;
            }
            throw new IllegalArgumentException(
                "Failed to generate CycleException");
        }
    }

    public void TestFallback() {
        for (Ordering order : Ordering.values()) {
            DiscreteComparator<String> comp = new Builder<String>(order)
                .add("a", "b", "c", "d").add("a", "m", "n").get();
            logln("Ordering " + comp.getOrdering());
            expectException(comp, "a", "a1");
            expectException(comp, "a1", "b");
            expectException(comp, "a1", "a2");
        }
    }

    private void expectException(DiscreteComparator<String> comp, String a,
        String b) {
        try {
            comp.compare(a, b);
        } catch (MissingItemException e) {
            logln("Expected missing item and got one at:\t" + e.getMessage());
            return;
        }
        throw new IllegalArgumentException("Failed to generate CycleException");
    }

    public void TestFallback2() {
        for (Ordering order : Ordering.values()) {
            Builder<String> builder = new Builder<String>(order);
            builder.setFallbackComparator(new Comparator<String>() {
                public int compare(String o1, String o2) {
                    return o1.compareTo(o2);
                }
            });
            builder.add("a", "b");
            builder.add("b", "c");
            builder.add("b", "d");
            DiscreteComparator<String> comp = builder.get();
            int result = comp.compare("a", "a1");
            if (result != -1)
                errln("a >= a1");
            result = comp.compare("b", "a1");
            if (result != -1)
                errln("a1 >= b");
            result = comp.compare("a1", "a2");
            if (result != -1)
                errln("a1 >= a2");
        }
    }

    private <T> void mustHaveException(Builder<T> builder) {
        try {
            logln(builder.toString());
            DiscreteComparator<T> comp = builder.get();
        } catch (CycleException e) {
            logln("Expected cycle and got one at:\t" + e.getMessage() + ",\t"
                + builder.getCycle());
            return;
        }
        throw new IllegalArgumentException("Failed to generate CycleException");
    }
}
