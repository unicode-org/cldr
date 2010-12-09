package org.unicode.cldr.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class VariantFolder {
  private AlternateFetcher alternateFetcher;

  private String source;

  private Set<String> result;

  public interface AlternateFetcher {
    /**
     * The input string MUST be in the output set. Note that the results must be
     * valid even if input string might not be on even code point boundaries.
     * For example, if the input is "XabY" where X and Y are have surrogates,
     * and the alternates are by case, then the results have to be {"XabY",
     * "XAbY", "XaBY", "XABY"}.
     * <p>The caller must never modify the set.
     * 
     * @param item
     * @return
     */
    Set<String> getAlternates(String item, Set<String> output);
  }

  /**
   * The class is designed to be immutable, at least as far as Java allows. That is, if the alternateFetcher is, then it will be.
   * @param alternateFetcher
   */
  public VariantFolder(AlternateFetcher alternateFetcher) {
    this.alternateFetcher = alternateFetcher;
  }

  // We keep track of the alternates for each combination of start,len
  // so with a length of 3 we have the following structure
  // {{0,1}, {1,2}, {2,3} -- length of 1
  // {0,2}, {1,3}
  // {0,3}}

  public Set<String> getClosure(String source) {
    int stringLength = source.length();
    if (stringLength == 0) {
      Set<String> result = new HashSet();
      result.add(source);
      return result;
    }
    Set<String>[][] combos = new Set[stringLength][];
    for (int i = 0; i < stringLength; ++i) {
      combos[i] = new Set[stringLength - i];
    }
    for (int i = 0; i < stringLength; ++i) {
      combos[0][i] = alternateFetcher.getAlternates(source.substring(i, i + 1),
          new HashSet<String>());
    }
    for (int level = 1; level < stringLength; ++level) {
      // at each level, we add strings of that length (plus 1)
      for (int start = 0; start < stringLength - level; ++start) {
        int limit = start + level + 1;
        // System.out.println(start + ", " + limit);
        // we first add any longer alternates
        Collection<String> current = combos[level][start] = new HashSet<String>();
        current.addAll(alternateFetcher.getAlternates(source.substring(start,
            limit), new HashSet<String>()));
        // then we add the cross product of shorter strings
        for (int breakPoint = start + 1; breakPoint < limit; ++breakPoint) {
          addCrossProduct(combos[breakPoint - start - 1][start], combos[limit
              - breakPoint - 1][breakPoint], current);
        }
      }
    }
    return combos[combos.length - 1][0];
  }

  private void addCrossProduct(Collection<String> source1,
      Collection<String> source2, Collection<String> output) {
    for (String x : source1) {
      for (String y : source2) {
        output.add(x + y);
      }
    }
  }

  public UnicodeSet getClosure(UnicodeSet input) {
    UnicodeSet result = new UnicodeSet();
    for( UnicodeSetIterator it = new UnicodeSetIterator(input); it.next();) {
      Set<String> temp = getClosure(it.getString());
      for (String s : temp) {
        result.add(s);
      }
    }
    return result;
  }
  
  public String reduce(String s) {
    Set<String> temp = getClosure(s);
    return temp.iterator().next();    
  }
  
  public UnicodeSet reduce(UnicodeSet input) {
    UnicodeSet result = new UnicodeSet();
    for( UnicodeSetIterator it = new UnicodeSetIterator(input); it.next();) {
      final String reduce = reduce(it.getString());
      result.add(reduce);
    }
    return result;
  }
}