package org.unicode.cldr.unittest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.ElementAttributeInfo;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.DtdType;
import org.unicode.cldr.util.XMLFileReader.SimpleHandler;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UnicodeSet;

public class TestPathsModule extends TestFmwk {

  public static void main(String[] args) {
    new TestPathsModule().run(args);
  }

  private static final Matcher DIR_FILTER = Pattern.compile(CldrUtility.getProperty("dir", ".?")).matcher("");
  private static final Matcher FILE_FILTER = Pattern.compile(CldrUtility.getProperty("file", ".?")).matcher("");
  private static final Matcher PATH_FILTER = Pattern.compile(CldrUtility.getProperty("path", ".?")).matcher("");
  private static final Matcher VALUE_FILTER = Pattern.compile(CldrUtility.getProperty("value", ".?")).matcher("");
  private static final Matcher TEST_FILTER = Pattern.compile(CldrUtility.getProperty("test", ".?")).matcher("");
  static final Relation<String, String> mainInfo = ElementAttributeInfo.getInstance(DtdType.ldml).getElement2Attributes();
  static final Relation<String, String> suppInfo = ElementAttributeInfo.getInstance(DtdType.supplementalData).getElement2Attributes();

  public void TestPaths() throws IOException {
    Map<String, Exception> cantRead = new LinkedHashMap<String, Exception>();
    List<PathTest> tests = new ArrayList<PathTest>();
    tests.add(new DistinguishingText());
    tests.add(new GatherValueCharacters());

    // filter out the tests we don't want
    for (Iterator<PathTest> it = tests.iterator(); it.hasNext();) {
      PathTest item = it.next();
      if (!TEST_FILTER.reset(item.getClass().getName()).find()) {
        it.remove();
      }
    }

    for (File dir : new File(CldrUtility.COMMON_DIRECTORY).listFiles()) {
      final String subdirectory = dir.getName();
      if (!dir.isDirectory() || !DIR_FILTER.reset(subdirectory).find()) {
        continue;
      }
      logln("Testing:\t" + dir);

      for (File file : dir.listFiles()) {
        String fullFileName = file.getCanonicalPath();
        String filename = file.getName();
        if (filename.startsWith("#") || !filename.endsWith(".xml") || !FILE_FILTER.reset(filename).find())
          continue;
        //logln("\tTesting:\t" + file);
        //System.out.print(".");
        //System.out.flush();
        // System.out.println("\t\t" + file);
        String name = dir.getName() + "/" + file.getName();
        name = name.substring(0, name.length() - 4); // strip .xml
        MyHandler myHandler = new MyHandler(dir, filename, tests);
        try {
          XMLFileReader reader = new XMLFileReader().setHandler(myHandler);
          reader.read(fullFileName, XMLFileReader.CONTENT_HANDLER, true);
        } catch (Exception e) {
          cantRead.put(name, e);
        }
      }
    }
    for (String name : cantRead.keySet()) {
      Exception exception = cantRead.get(name);
      errln(name + "\t" + exception.getMessage() + "\t" + Arrays.asList(exception.getStackTrace()));
    }
    for (PathTest test : tests) {
      test.finish();
    }
  }

  class MyHandler extends SimpleHandler {
    final List<PathTest> tests;

    DtdType dtdType = null;
    final XPathParts fullParts = new XPathParts();

    public MyHandler(File dir, String filename, List<PathTest> tests2) {
      tests = tests2;
      for (PathTest test : tests) {
        test.start(dir, filename);
      }
    }

    public void handlePathValue(String path, String value) {
      if (!PATH_FILTER.reset(path).find()) {
        return;
      }
      if (!VALUE_FILTER.reset(value).find()) {
        return;
      }
      fullParts.set(path);
      for (PathTest test : tests) {
        test.test(fullParts, value);
      }
    }

  }

  static class PathTest {
    protected DtdType dtdType;
    protected String locale;
    protected File dir;

    public void start(File dir, String locale) {
      this.dir = dir;
      this.locale = locale;
      this.dtdType = null;
    }

    public void test(XPathParts fullParts, String value) {
      if (dtdType == null) {
        dtdType = DtdType.valueOf(fullParts.getElement(0));
      }
    }

    public void finish() {

    }
  }

  static final Normalizer2 nfkd = Normalizer2.getInstance(null, "nfc", Normalizer2.Mode.DECOMPOSE);
  static final UnicodeSet nonspacing = new UnicodeSet("[[:Mn:][:Me:]]");

  class GatherValueCharacters extends PathTest {
    UnicodeSet chars = new UnicodeSet();

    UnicodeSet temp = new UnicodeSet();
    
    @Override
    public void test(XPathParts fullParts, String value) {
      super.test(fullParts, value);
      if (value.length() == 0) {
        return;
      }
      String draftValue = fullParts.findFirstAttributeValue("draft");
      if (draftValue != null) {
        if (draftValue.equals("unconfirmed") || draftValue.equals("provisional")) {
          return;
        }
      }
      temp.clear()
      .addAll(value)
      .addAll(nfkd.normalize(value))
      .retainAll(nonspacing);
      if (!chars.containsAll(temp)) {
        chars.addAll(temp);
        logln("Adding\t" + temp + "\t" + dir + "\t" + locale);
      }
    }
    @Override
    public void finish() {
      super.finish();
      System.out.println(chars);
    }
  }

  enum OrderedChildren {all, some, none}

  /**
   * This test checks for the following:
   * <ul>
   * <li>Non-distinguishing attributes must only be on leaf nodes</li>
   * <li>If an element is ordered, then its children must be.</li>
   * </ul>
   * Note that a leaf node is a final one OR the last one before any ordered element.
   */
  class DistinguishingText extends PathTest {

    private Relation<R3<DtdType, String, String>,String> nonFinalNonDistingishing 
    = new Relation(new TreeMap(), TreeSet.class);

    private Relation<R2<DtdType, String>,String> illFormedOrder 
    = new Relation(new TreeMap(), TreeSet.class);

    private Map<String, OrderedChildren> orderedChildrenStatus 
    = new TreeMap<String, OrderedChildren>();

    public void test(XPathParts fullParts, String value) {
      super.test(fullParts, value);
      int size = fullParts.size();
      int firstQ = findFirstQ(fullParts, size);
      int firstLeaf = (firstQ >= 0 ? firstQ : size) - 1;
      for (int i = 0; i < size; ++i) {
        String element = fullParts.getElement(i);
        boolean leafElement = i == firstLeaf;
        for (String attribute : fullParts.getAttributes(i).keySet()) {
          boolean distinguishing = CLDRFile.isDistinguishing(dtdType, element, attribute);
          if (!leafElement && !distinguishing) {
            R3<DtdType, String, String> row = Row.of(dtdType, element, attribute);
            nonFinalNonDistingishing.put(row, locale);
            //System.out.println("Non-Distinguishing, non-final:\t" + row);
          }
        }
      }
    }

    /**
     * Get the first ordered element, AND make a number of consistency checks.
     * @param fullParts
     * @param size
     * @return -1 if no _q, otherwise index of first _q
     */
    private int findFirstQ(XPathParts fullParts, int size) {
      int firstQ = -1;
      String lastElement = "";
      for (int i = 0; i < size; ++i) {
        String element = fullParts.getElement(i);
        boolean hasq = CLDRFile.isOrdered(element, dtdType);
        // check that all children have consistent ordering status
        OrderedChildren status = orderedChildrenStatus.get(lastElement);
        if (status == null) {
          orderedChildrenStatus.put(lastElement, hasq ? OrderedChildren.all : OrderedChildren.none);
        } else switch (status) {
        case all: if (!hasq) orderedChildrenStatus.put(lastElement, OrderedChildren.some); break;
        case none: if (hasq) orderedChildrenStatus.put(lastElement, OrderedChildren.some); break;
        }
        // find the first ordered element AND check that its children are all ordered
        if (hasq) {
          if (firstQ < 0) {
            firstQ = i;
          }
        } else {
          if (firstQ >= 0) { // missing!
            R2<DtdType, String> row = Row.of(dtdType, element);
            illFormedOrder.put(row, locale);
          }
        }
        lastElement = element;
      }
      return firstQ;
    }

    public void finish() {
      super.finish();
      for (R3<DtdType, String, String> item : nonFinalNonDistingishing.keySet()) {
        List<String> samples = new ArrayList<String>(nonFinalNonDistingishing.getAll(item));
        if (samples.size() > 5) samples = samples.subList(0, 4);
        errln("Attribute is not on leaf element and not distinguishing:\t" + item + "\t" + samples);
      }
      for (R2<DtdType, String> item : illFormedOrder.keySet()) {
        List<String> samples = new ArrayList<String>(illFormedOrder.getAll(item));
        if (samples.size() > 5) {
          samples = samples.subList(0, 4);
          samples.set(4, "...");
        }
        errln("Child of ordered element is not ordered:\t" + item + "\t" + samples);
      }
      for (String element : orderedChildrenStatus.keySet()) {
        OrderedChildren status = orderedChildrenStatus.get(element);
        if (status == OrderedChildren.some){
          errln("Brother of ordered element is not ordered items not consistent:\t" + element);
        }
      }
    }
  }
}
