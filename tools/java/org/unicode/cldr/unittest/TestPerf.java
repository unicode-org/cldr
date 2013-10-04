package org.unicode.cldr.unittest;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.Attributes;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.Timer;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.util.CollectionUtilities;

public class TestPerf extends TestFmwkPlus {
    public static void main(String[] args) {
        new TestPerf().run(args);
    }

    static final int ITERATIONS = 100;
    static final Set<String> testPaths;
    static final int elementSize;
    static final Set<String> elements = new HashSet<String>();
    static final Set<String> attributes = new HashSet<String>();
    static final Set<String> attributeValues = new HashSet<String>();

    static {
        Set<String> testPaths_ = new HashSet();
        CollectionUtilities.addAll(TestInfo.getInstance().getEnglish().iterator(), testPaths_);
        testPaths = Collections.unmodifiableSet(testPaths_);
        // warmup
        int size = 0;
        for (String p : testPaths) {
            XPathParts xpp = XPathParts.getFrozenInstance(p);
            size += xpp.size();
            for (int i = 0; i < xpp.size(); ++i) {
                elements.add(xpp.getElement(i));
                for (Entry<String, String> attributeAndValue : xpp.getAttributes(i).entrySet()) {
                    String attribute = attributeAndValue.getKey();
                    String value = attributeAndValue.getValue();
                    if (attributes.add(attribute)) {
                        System.out.println("Adding " +  attribute + ", " + p);
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
                XPathParts xpp = new XPathParts().set(p);
                size += xpp.size();
            }
        }
        long duration = t.stop();
        assertRelation("", true, duration/ITERATIONS/1000000.0, LEQ, 50.0); // 47231000
    }

    public void TestMutableXPathParts() {
        Timer t = new Timer();
        t.start();
        int size = 0;
        XPathParts xpp = new XPathParts();
        for (String p : testPaths) {
            for (int i = 0; i < ITERATIONS; ++i) {
                xpp.set(p);
                size += xpp.size();
            }
        }
        long duration = t.stop();
        assertRelation("", true, duration/ITERATIONS/1000000.0, LEQ, 50.0); // 47231000
        assertEquals("", elementSize, size/ITERATIONS);
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
        assertRelation("", true, duration/ITERATIONS/1000000.0, LEQ, 50.0);
        assertEquals("", elementSize, size/ITERATIONS);
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
        assertRelation("", true, duration/ITERATIONS/1000000.0, LEQ, 50.0);
        assertEquals("", elementSize, size/ITERATIONS);
    }
}
