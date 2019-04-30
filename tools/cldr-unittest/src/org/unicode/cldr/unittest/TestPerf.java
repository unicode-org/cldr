package org.unicode.cldr.unittest;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdData.AttributeValueComparator;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Timer;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.util.Output;

public class TestPerf extends TestFmwkPlus {
    public static void main(String[] args) {
        new TestPerf().run(args);
    }

    static final int ITERATIONS = 20;
    static final Set<String> testPaths;
    static final int elementSize;
    static final Set<String> elements = new HashSet<String>();
    static final Set<String> attributes = new HashSet<String>();
    static final Set<String> attributeValues = new HashSet<String>();
    static final String[] sortedArray;

    static {
        Set<String> testPaths_ = new HashSet<String>();
        CollectionUtilities.addAll(CLDRConfig.getInstance().getEnglish()
            .iterator(), testPaths_);
        testPaths = Collections.unmodifiableSet(testPaths_);
        Set<String> sorted = new TreeSet<String>(
            CLDRFile.getComparator(DtdType.ldml));
        sorted.addAll(testPaths);
        sortedArray = sorted.toArray(new String[sorted.size()]);

        // warmup
        int size = 0;
        for (String p : testPaths) {
            XPathParts xpp = XPathParts.getFrozenInstance(p);
            size += xpp.size();
            for (int i = 0; i < xpp.size(); ++i) {
                elements.add(xpp.getElement(i));
                for (Entry<String, String> attributeAndValue : xpp
                    .getAttributes(i).entrySet()) {
                    String attribute = attributeAndValue.getKey();
                    String value = attributeAndValue.getValue();
                    if (attributes.add(attribute)) {
                        // System.out.println("Adding " + attribute + ", " + p);
                    }
                    attributeValues.add(value);
                }
            }
        }
        elementSize = size;
    }

    public void TestA() {
        logln("Path count: " + testPaths.size());
        logln("Elements: " + elements.size());
        logln("Attributes: " + attributes.size() + "\t" + attributes);
        logln("AttributeValues: " + attributeValues.size());
    }

    @Override
    protected void init() throws Exception {
        super.init();
    }

    public void TestXPathParts() {
        Timer t = new Timer();
        t.start();
        int size = 0;
        for (String p : testPaths) {
            for (int i = 0; i < ITERATIONS; ++i) {
                XPathParts xpp = XPathParts.getTestInstance(p);
                size += xpp.size();
            }
        }
        long duration = t.stop();
        assertRelation("", true, duration / ITERATIONS / 1000000.0, LEQ, 50.0); // 47231000
    }

    public void TestMutableXPathParts() {
        Timer t = new Timer();
        t.start();
        int size = 0;
        for (String p : testPaths) {
            for (int i = 0; i < ITERATIONS; ++i) {
                XPathParts xpp = XPathParts.getTestInstance(p);
                size += xpp.size();
            }
        }
        long duration = t.stop();
        assertRelation("", true, duration / ITERATIONS / 1000000.0, LEQ, 50.0); // 47231000
        assertEquals("", elementSize, size / ITERATIONS);
    }

    public void TestFastFrozenXPathParts() {
        Timer t = new Timer();
        t.start();
        int size = 0;
        for (String p : testPaths) {
            for (int i = 0; i < ITERATIONS; ++i) {
                XPathParts xpp = XPathParts.getFrozenInstance(p);
                size += xpp.size();
            }
        }
        long duration = t.stop();
        assertRelation("", true, duration / ITERATIONS / 1000000.0, LEQ, 50.0);
        assertEquals("", elementSize, size / ITERATIONS);
    }

    public void TestFastXPathParts() {
        Timer t = new Timer();
        t.start();
        int size = 0;
        for (String p : testPaths) {
            for (int i = 0; i < ITERATIONS; ++i) {
                XPathParts xpp = XPathParts.getInstance(p);
                size += xpp.size();
            }
        }
        long duration = t.stop();
        assertRelation("", true, duration / ITERATIONS / 1000000.0, LEQ, 50.0);
        assertEquals("", elementSize, size / ITERATIONS);
    }

    public void TestXPathPartsWithComparators() {
        DtdData dtdData = DtdData.getInstance(DtdType.ldml);

        XPathParts newParts = new XPathParts();
        for (String path : sortedArray) {
            /*
             * TODO: getTestInstance; how, for (dtdData.getAttributeComparator(), null)?
             */
            String newPath = newParts.set(path).toString();
            assertEquals("path", path, newPath);
        }
    }

    public void TestPathComparison() {
        DtdData dtdData = DtdData.getInstance(DtdType.ldml);
        AttributeValueComparator avc = new AttributeValueComparator() {
            @Override
            public int compare(String element, String attribute, String value1,
                String value2) {
                Comparator<String> comp = CLDRFile.getAttributeValueComparator(
                    element, attribute);
                return comp.compare(value1, value2);
            }
        };
        Comparator<String> comp = dtdData.getDtdComparator(avc);

        int iterations = 50;
        Output<Integer> failures = new Output<Integer>();

        // warmup
        checkCost(sortedArray, CLDRFile.getComparator(DtdType.ldml), 1,
            failures);
        assertRelation("CLDRFile.ldmlComparator-check", true, failures.value,
            LEQ, 0);
        double seconds = checkCost(sortedArray,
            CLDRFile.getComparator(DtdType.ldml), iterations, failures);
        assertRelation("CLDRFile.ldmlComparator", true, seconds, LEQ, 0.1);
        // logln(title + "\tTime:\t" + timer.toString(iterations));

        // warmup
        checkCost(sortedArray, comp, 1, failures);
        assertRelation("DtdComparator-check", true, failures.value, LEQ, 0);
        double newSeconds = checkCost(sortedArray, comp, iterations, failures);

        // new code needs to be twice as fast
        assertRelation("DtdComparator", true, newSeconds, LEQ, seconds * .5);
    }

    private double checkCost(String[] sortedArray, Comparator<String> comp,
        int iterations, Output<Integer> failures2) {
        Timer timer = new Timer();
        int failures = 0;
        for (int i = 0; i < iterations; ++i) {
            String lastPath = null;
            for (String currentPath : sortedArray) {
                if (lastPath != null) {
                    if (comp.compare(lastPath, currentPath) > 0) {
                        failures++;
                    }
                }
                lastPath = currentPath;
            }
        }
        timer.stop();
        failures2.value = failures;
        return timer.getSeconds() / iterations;
    }

    public void TestUnused() {

    }
}
