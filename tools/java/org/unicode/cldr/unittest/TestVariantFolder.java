package org.unicode.cldr.unittest;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.VariantFolder;

import com.ibm.icu.dev.test.util.XEquivalenceClass;
import com.ibm.icu.dev.test.util.XEquivalenceClass.SetMaker;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.CanonicalIterator;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;

public class TestVariantFolder {
  public static void main(String[] args) {
    VariantFolder variantFolder = new VariantFolder(new CaseVariantFolder());
    String[] tests = {"abc", "aß", "\uFB01sh", "Åbë"};
    for (String test : tests) {
      Set<String> set = variantFolder.getClosure(test);
      System.out.println(test + "\t" + set.size() + "\t" + new TreeSet(set));
      final Set closed = closeUnderCanonicalization(set, new TreeSet());
      System.out.println(test + "\t" + closed.size() + "\t" + closed);
    }
    
    variantFolder = new VariantFolder(new CompatibilityFolder());
    String[] testSets = {
        "[:Word_Break=ExtendNumLet:]",
        "[:Word_Break=Format:]",
        "[:Word_Break=Katakana:]",
        "[[:Word_Break=MidLetter:]\u2018]",
        "[:Word_Break=MidNum:]",
        "[[:Word_Break=MidNum:]-[\\uFE13]]",
        "[:Word_Break=Numeric:]",
        "[\\u0027\\u2018\\u2019\\u002e]",
    };
    for (String testSet : testSets) {
      UnicodeSet source = new UnicodeSet(testSet);
      Set<String> target = new TreeSet<String>();
      for (UnicodeSetIterator it = new UnicodeSetIterator(source); it.next();) {
        Set<String> closure = variantFolder.getClosure(it.getString());
        target.addAll(closure);
      }
      UnicodeSet utarget = new UnicodeSet();
      utarget.addAll(target);
      System.out.println(testSet + " => " + new UnicodeSet(utarget).removeAll(source));
    }
  }
  
  static CanonicalIterator canonicalterator = new CanonicalIterator("");
  static Set closeUnderCanonicalization(Set<String> source, Set<String> output) {
    for (String item : source) {
      canonicalterator.setSource(item);
      for (String equiv = canonicalterator.next(); equiv != null; equiv = canonicalterator.next()) {
        output.add(equiv);
      }
    }
    return output;
  }
  
  public static class CompatibilityFolder implements VariantFolder.AlternateFetcher {
    private static final UnicodeSet NORMAL_CHARS = new UnicodeSet("[^[:c:]]");
    static XEquivalenceClass equivalents = new XEquivalenceClass("none", mySetMaker);
    static {
      for (UnicodeSetIterator it = new UnicodeSetIterator(NORMAL_CHARS); it.next();) {
        String item = it.getString();
        equivalents.add(item, Normalizer.decompose(item,true));
        equivalents.add(item, Normalizer.compose(item,true));
      }
    }

    public Set<String> getAlternates(String item, Set<String> output) {
      output.add(item);
      return equivalents.getEquivalences(item);
    }
    
  }
  
  static SetMaker mySetMaker = new SetMaker() {
     Comparator c = new UTF16.StringComparator(true,false,0);
     Comparator bestIsLowest = new Comparator() {
      public int compare(Object o1, Object o2) {
        String s1 = o1.toString();
        String s2 = o2.toString();
        final boolean casefold1 = UCharacter.foldCase(s1,true).equals(s1);
        final boolean casefold2 = UCharacter.foldCase(s2,true).equals(s2);
        if (casefold1 != casefold2) {
          return casefold1 ? -1 : 1;
        }
        final boolean canonical1 = Normalizer.isNormalized(s1, Normalizer.COMPOSE, 0);
        final boolean canonical2 = Normalizer.isNormalized(s2, Normalizer.COMPOSE, 0);
        if (canonical1 != canonical2) {
          return canonical1 ? -1 : 1;
        }
        int len1 = s1.codePointCount(0,s1.length());
        int len2 = s2.codePointCount(0,s2.length());
        if (len1 != len2) {
          return len1 - len2;
        }
        return c.compare(s1, s2);
      }
       
     };
    public Set make() {
      return new TreeSet(bestIsLowest);
    }
  };
  
  private static final UnicodeSet NORMAL_CHARS = new UnicodeSet("[^[:c:]]");
  
  public static class CanonicalFolder implements VariantFolder.AlternateFetcher {
    static XEquivalenceClass equivalents = new XEquivalenceClass("none", mySetMaker);
    static {
      for (UnicodeSetIterator it = new UnicodeSetIterator(NORMAL_CHARS); it.next();) {
        String item = it.getString();
        equivalents.add(item, Normalizer.decompose(item,false));
        equivalents.add(item, Normalizer.compose(item,false));
      }
    }

    public Set<String> getAlternates(String item, Set<String> output) {
      output.add(item);
      return equivalents.getEquivalences(item);
    }
    
  }
  
  public static class CaseVariantFolder implements VariantFolder.AlternateFetcher {
    private static final UnicodeSet NORMAL_CHARS = new UnicodeSet("[^[:c:]]");
    static XEquivalenceClass equivalents = new XEquivalenceClass("none", mySetMaker);
    static {
      for (UnicodeSetIterator it = new UnicodeSetIterator(NORMAL_CHARS); it.next();) {
        String item = it.getString();
        equivalents.add(item, UCharacter.toLowerCase(item));
        equivalents.add(item, UCharacter.toUpperCase(item));
        equivalents.add(item, UCharacter.foldCase(item, true));
        equivalents.add(item, UCharacter.toTitleCase(ULocale.ROOT, item,null));
      }
    }
    
    public Set<String> getAlternates(String item, Set<String> output) {
      output.add(item);
      return equivalents.getEquivalences(item);
    } 
  }
}