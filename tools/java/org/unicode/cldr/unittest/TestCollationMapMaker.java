package org.unicode.cldr.unittest;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.tool.GenerateTransformCharts;
import org.unicode.cldr.util.CollationMapMaker;
import org.unicode.cldr.util.Log;
import com.ibm.icu.dev.test.util.Relation;
import org.unicode.cldr.util.CldrUtility;

import com.ibm.icu.dev.test.util.CaseIterator;
import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.dev.test.util.PrettyPrinter;
import com.ibm.icu.impl.MultiComparator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class TestCollationMapMaker {
  static Transliterator javaEscape = Transliterator.getInstance("[^ \\u0009 \\u000A \\u000D \\u0020-\\u007F] hex/java");
  
  public static void main(String[] args) throws IOException {
    testTranslit();
    Log.setLog(CldrUtility.GEN_DIRECTORY + "CollationMapLog.txt");
    CaseIterator caseIterator = new CaseIterator();
    caseIterator.reset("aa");
    while(true) {
      String item = caseIterator.next();
      if (item == null) break;
      System.out.println(item);
    }
    RuleBasedCollator col = (RuleBasedCollator) Collator.getInstance(new ULocale("da_DK"));
    col.setStrength(Collator.SECONDARY);
    col.setAlternateHandlingShifted(true);
    
    Comparator c = new com.ibm.icu.impl.MultiComparator(new Comparator[]{col, new com.ibm.icu.text.UTF16.StringComparator(true,false,0)});
    Map mapping = new CollationMapMaker().generateCollatorFolding(col, new TreeMap<CharSequence,String>());
    Relation<String,String> inverse = new Relation(new TreeMap(c), TreeSet.class);
    inverse.addAllInverted(mapping);
    UnicodeSet unicodeSet = new UnicodeSet();
    PrettyPrinter pretty = new PrettyPrinter().setOrdering(Collator.getInstance(ULocale.ROOT)).setSpaceComparator(Collator.getInstance(ULocale.ROOT).setStrength2(RuleBasedCollator.PRIMARY));
    for (String target : inverse.keySet()) {
      unicodeSet.clear();
      unicodeSet.addAll(inverse.getAll(target));
      if (target.length() > 0) {
        unicodeSet.add(target);
      }
      String unicodeSetName = pretty.toPattern(unicodeSet);
      String name = GenerateTransformCharts.getName(target, ", ");
      Log.logln(com.ibm.icu.impl.Utility.hex(target) + " ( " + target + " ) " + name +  "\t" + unicodeSetName);
    }
  }

  private static void testTranslit() {
     System.out.println(javaEscape.transform("\u0001\u001F" + CldrUtility.LINE_SEPARATOR + "\u0061\u00A5\uFFFF\uD800\uDC00"));
  }
}