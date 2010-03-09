package org.unicode.cldr.unittest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.ElementAttributeInfo;
import org.unicode.cldr.util.CLDRFile.DtdType;

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
        if (!newChildren.containsAll(oldChildren)) {
          if (changedToEmpty.contains(element) && oldChildren.equals(PCDATA) && newChildren.equals(EMPTY)) {
            // ok, skip
          } else {
            LinkedHashSet<String> missing = new LinkedHashSet<String>(oldChildren);
            missing.removeAll(newChildren);
            errln("Old " + dtd + " element <" + element + "> has children not in new:\tMissing:\t" + missing + "\tOld:\t" + oldChildren + ",\tNew:\t" + newChildren);
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
            errln("Old " + dtd + " element <" + element + "> has attributes not in new:\tMissing:\t" + missing + "\tOld:\t" + oldAttributes + ",\tNew:\t" + newAttributes);
          }
        }
      }
    }
  }
}
