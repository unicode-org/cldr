package org.unicode.cldr.unittest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.DiscreteComparator;
import org.unicode.cldr.util.ElementAttributeInfo;
import org.unicode.cldr.util.CLDRFile.DtdType;
import org.unicode.cldr.util.DiscreteComparator.Ordering;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.Relation;

/**
 * Tests basic compatibility between versions of CLDR. You need to set the property -DoldCommon=<whatever> for this to work.
 * @author markdavis
 */
public class TestDtdCompatibility extends TestFmwk {
  static final String oldCommon = CldrUtility.getProperty("oldCommon", "/Users/markdavis/Documents/workspace35/cldr-maint-1-7/common");

  public static void main(String[] args) {
    new TestDtdCompatibility().run(args);
  }

  public void TestBasicCompatibility() {
    // set up exceptions
    Set<String> changedToEmpty = new HashSet<String>(Arrays.asList(new String[] {"version", "languageCoverage", "scriptCoverage", "territoryCoverage", "currencyCoverage", "timezoneCoverage", "skipDefaultLocale"}));
    Set<String> PCDATA = new HashSet<String>();
    PCDATA.add("PCDATA");
    Set<String> EMPTY = new HashSet<String>();
    EMPTY.add("EMPTY"); 
    Set<String> VERSION = new HashSet<String>();
    VERSION.add("version"); 

    // test all DTDs
    for (DtdType dtd : DtdType.values()) {
      ElementAttributeInfo oldDtd = ElementAttributeInfo.getInstance(oldCommon, dtd);
      ElementAttributeInfo newDtd = ElementAttributeInfo.getInstance(dtd);

      Relation<String, String> oldElement2Children = oldDtd.getElement2Children();
      Relation<String, String> newElement2Children = newDtd.getElement2Children();

      Relation<String, String> oldElement2Attributes = oldDtd.getElement2Attributes();
      Relation<String, String> newElement2Attributes = newDtd.getElement2Attributes();

      for (String element : oldElement2Children.keySet()) {
        Set<String> oldChildren = oldElement2Children.getAll(element);
        Set<String> newChildren = newElement2Children.getAll(element);
        if (newChildren == null) {
          errln("Old " + dtd + " contains element not in new: <" + element + ">");
          continue;
        }
        Set<String> funny = containsInOrder(newChildren, oldChildren);
        if (funny != null) {
          if (changedToEmpty.contains(element) && oldChildren.equals(PCDATA) && newChildren.equals(EMPTY)) {
            // ok, skip
          } else {
            errln("Old " + dtd + " element <" + element + "> has children Missing/Misordered:\t" + funny + "\n\t\tOld:\t" + oldChildren + "\n\t\tNew:\t" + newChildren);
          }
        }

        Set<String> oldAttributes = oldElement2Attributes.getAll(element);
        if (oldAttributes == null) {
          oldAttributes = Collections.emptySet();
        }
        Set<String> newAttributes = newElement2Attributes.getAll(element);
        if (newAttributes == null) {
          newAttributes = Collections.emptySet();
        }
        if (!newAttributes.containsAll(oldAttributes)) {
          LinkedHashSet<String> missing = new LinkedHashSet<String>(oldAttributes);
          missing.removeAll(newAttributes);
          if (element.equals(dtd.toString()) && missing.equals(VERSION)) {
            // ok, skip
          } else {
            errln("Old " + dtd + " element <" + element + "> has attributes Missing:\t" + missing + "\n\t\tOld:\t" + oldAttributes + "\n\t\tNew:\t" + newAttributes);
          }
        }
      }
    }
  }

  private <T> Set<T> containsInOrder(Set<T> superset, Set<T> subset) {
    if (!superset.containsAll(subset)) {
      LinkedHashSet<T> missing = new LinkedHashSet<T>(subset);
      missing.removeAll(superset);
      return missing;
    }
    // ok, we know that they are subsets, try order
    Set<T> result = null;
    DiscreteComparator<T> comp = new DiscreteComparator.Builder<T>(Ordering.ARBITRARY).add(superset).get();
    T last = null;
    for (T item : subset) {
      if (last != null) {
        int order = comp.compare(last, item);
        if (order != -1) {
          if (result == null) {
            result = new HashSet<T>();
            result.add(last);
            result.add(item);
          }
        }
      }
      last = item;
    }
    return result;
  }
}
