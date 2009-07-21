package org.unicode.cldr.unittest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.FindDTDOrder;
import org.unicode.cldr.util.CldrUtility;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Differ;

public class TestMetadata extends TestFmwk {
  static TestInfo testInfo = TestInfo.getInstance();
  
  public static void main(String[] args) {
    new TestMetadata().run(args);
  }
 
  public void TestOrdering() {
    FindDTDOrder order = FindDTDOrder.getInstance();
    
    warnln("Make sure that all and only blocking elements are serialElements.");
    
    // First Elements
    
    List<String> dtdElementOrder = order.getElementOrder();
    List<String> cldrFileElementOrder = CLDRFile.getElementOrder();
    checkEquals("Element orderings", "CLDRFile", cldrFileElementOrder, "DTD", dtdElementOrder);

    List<String> metadataElementOrder = testInfo.getSupplementalDataInfo().getElementOrder();
    checkEquals("Element orderings", "supplemental", metadataElementOrder, "DTD", dtdElementOrder);

    // Then Serial Order
    Set<String> cldrFileSerialElements = new TreeSet<String>(CLDRFile.getSerialElements());
    Set<String> metadataSerialElements = new TreeSet<String>(testInfo.getSupplementalDataInfo().getSerialElements());
    checkEquals("Serial elements", "metadata", metadataSerialElements, "cldrFile", cldrFileSerialElements);
    
    // Then Attributes
    List<String> rawDtdAttributeOrder = order.getAttributeOrder();
    List<String> metadataAttributeOrder = testInfo.getSupplementalDataInfo().getAttributeOrder();
    List<String> cldrFileAttributeOrder = CLDRFile.getAttributeOrder();

    LinkedHashSet<String> modifiedDtdOrder = new LinkedHashSet<String>(cldrFileAttributeOrder);
    // add items, keeping the ordering stable
    modifiedDtdOrder.addAll(metadataAttributeOrder);
    modifiedDtdOrder.retainAll(rawDtdAttributeOrder); // remove any superfluous stuff
    modifiedDtdOrder.addAll(rawDtdAttributeOrder);
    
    // certain stuff always goes at the end
    modifiedDtdOrder.removeAll(order.getCommonAttributes());
    modifiedDtdOrder.addAll(order.getCommonAttributes());
    
    // now make a list for comparison
    List<String> dtdAttributeOrder = new ArrayList(modifiedDtdOrder);
    
    // fix to and from
    dtdAttributeOrder.remove("from");
    dtdAttributeOrder.add(dtdAttributeOrder.indexOf("to"), "from");

    checkEquals("Attribute orderings", "CLDRFile", cldrFileAttributeOrder, "DTD", dtdAttributeOrder);

    checkEquals("Attribute orderings", "supplemental", metadataAttributeOrder, "DTD", dtdAttributeOrder);
}

  private void checkEquals(String title, String firstTitle, Collection<String> cldrFileOrder, String secondTitle, Collection<String> dtdAttributeOrder) {
    if (!cldrFileOrder.equals(dtdAttributeOrder)) {
      errln(title + " differ:" + CldrUtility.LINE_SEPARATOR 
              + firstTitle + ": " + cldrFileOrder + CldrUtility.LINE_SEPARATOR 
              + secondTitle + ": " + dtdAttributeOrder + CldrUtility.LINE_SEPARATOR 
              + "to fix, replace in " + firstTitle + ":" + CldrUtility.LINE_SEPARATOR + "\t"
              + CldrUtility.join(dtdAttributeOrder, " ")
              );
      Differ differ = new Differ(200, 1);
      Iterator<String> oldIt = cldrFileOrder.iterator();
      Iterator<String> newIt = dtdAttributeOrder.iterator();
      while (oldIt.hasNext() || newIt.hasNext()) {
        if (oldIt.hasNext())
          differ.addA(oldIt.next());
        if (newIt.hasNext())
          differ.addB(newIt.next());
        differ.checkMatch(!oldIt.hasNext() && !newIt.hasNext());

        if (differ.getACount() != 0 || differ.getBCount() != 0) {
          final Object start = differ.getA(-1);
          if (start.toString().length() != 0) {
            logln("..." + CldrUtility.LINE_SEPARATOR + "\tSame: " + start);
          }
          for (int i = 0; i < differ.getACount(); ++i) {
            logln("\t"+firstTitle+": " + differ.getA(i));
          }
          for (int i = 0; i < differ.getBCount(); ++i) {
            logln("\t"+secondTitle+": " + differ.getB(i));
          }
          final Object end = differ.getA(differ.getACount());
          if (end.toString().length() != 0) {
            logln("Same: " + end + CldrUtility.LINE_SEPARATOR + "\t...");
          }
        }
      }
      logln("Done with differences");

    }
  }
}
