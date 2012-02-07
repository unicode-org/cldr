package org.unicode.cldr.unittest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts.Comments;

import com.ibm.icu.dev.test.TestFmwk;

public class TestXMLSource extends TestFmwk {
    private class DummyXMLSource extends XMLSource {
        HashMap<String, String> valueMap = new HashMap();

        @Override
        public Object freeze() {
            return null;
        }

        @Override
        public void putFullPathAtDPath(String distinguishingXPath, String fullxpath) {
        }

        @Override
        public void putValueAtDPath(String distinguishingXPath, String value) {
            valueMap.put(distinguishingXPath, value);
        }

        @Override
        public void removeValueAtDPath(String distinguishingXPath) {}

        @Override
        public String getValueAtDPath(String path) {
            return valueMap.get(path);
        }

        @Override
        public String getFullPathAtDPath(String path) {
            return null;
        }

        @Override
        public Comments getXpathComments() {
            return null;
        }

        @Override
        public void setXpathComments(Comments comments) {}

        @Override
        public Iterator<String> iterator() {
            return valueMap.keySet().iterator();
        }
    }

    public static void main(String[] args) {
        new TestXMLSource().run(args);
    }
    
    public void TestGetPathsWithValue() {
        XMLSource source = new DummyXMLSource();
        source.putValueAtDPath("//ldml/foo", "x");
        source.putValueAtDPath("//ldml/foo[@alt=\"proposed5\"]", "x");
        source.putValueAtDPath("//ldml/foo[@alt=\"short\"]", "x");
        source.putValueAtDPath("//ldml/foo[@alt=\"short-proposed-x\"]", "x");
        source.putValueAtDPath("//ldml/foo[@alt=\"short-proposed-x\"][@type=\"wide\"]", "x");
        source.putValueAtDPath("//ldml/foo[@alt=\"short-x\"]", "x");

        Set<String> result = new HashSet<String>();
        source.getPathsWithValue("x", "//ldml/foo/bar", result);
        assertEquals("no paths should be matched", 0, result.size());
        result.clear();
        
        String xpath = "//ldml/foo";
        source.getPathsWithValue("x", xpath, result);
        assertEquals("Set matched but incorrect: " + result.toString(), 2, result.size());
        assertTrue(xpath + " not found", result.contains(xpath));
        assertTrue("//ldml/foo[@alt=\"proposed5\"] not found", result.contains("//ldml/foo[@alt=\"proposed5\"]"));
        result.clear();

        xpath = "//ldml/foo[@alt=\"short\"]";
        source.getPathsWithValue("x", xpath, result);
        assertEquals("Set matched but incorrect: " + result.toString(), 3, result.size());
        assertTrue(xpath + " not found", result.contains(xpath));
        assertTrue("//ldml/foo[@alt=\"short-proposed-x\"] not found", result.contains("//ldml/foo[@alt=\"short-proposed-x\"]"));
        assertTrue("//ldml/foo[@alt=\"short-proposed-x\"][@type=\"wide\"] not found", result.contains("//ldml/foo[@alt=\"short-proposed-x\"][@type=\"wide\"]"));
        result.clear();

        xpath = "//ldml/foo[@alt=\"short-proposed\"]";
        source.getPathsWithValue("x", xpath, result);
        assertEquals("Set matched but incorrect: " + result.toString(), 3, result.size());
        assertTrue("//ldml/foo[@alt=\"short-proposed-x\"] not found", result.contains("//ldml/foo[@alt=\"short-proposed-x\"]"));
        assertTrue("//ldml/foo[@alt=\"short\"] not found", result.contains("//ldml/foo[@alt=\"short\"]"));
        assertTrue("//ldml/foo[@alt=\"short-proposed-x\"][@type=\"wide\"] not found", result.contains("//ldml/foo[@alt=\"short-proposed-x\"][@type=\"wide\"]"));
        result.clear();
    }
}
