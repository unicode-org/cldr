package org.unicode.cldr.draft;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;

import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;

/**
 * Immutable class that performs transformations
 * @author markdavis
 */
public class RegexTransform implements com.ibm.icu.text.StringTransform {
  private static final char BUCKETSIZE = 257;
  private final List<Rule> rules;
  private final List<Rule>[] buckets = new List[BUCKETSIZE];

  public RegexTransform(List<Rule> rules2) {
    rules = new ArrayList<Rule>(rules2);
    
    // we now fill the buckets with rules, in order, that might match that bucket.
    for (int i = 0; i < BUCKETSIZE; ++i) {
      buckets[i] = new ArrayList<Rule>();
    }
    List<Matcher> matchers = new ArrayList<Matcher>();
    for (Rule rule : rules) {
      matchers.add(rule.getPostmatcher(""));
    }
    for (char i = 0; i < 0xD800; ++i) {
      String s = String.valueOf(i);
      int masked = i % BUCKETSIZE;
      for (int j = 0; j < matchers.size(); ++j) {
        Matcher matcher = matchers.get(j);
        if (matcher.reset(s).matches() || matcher.hitEnd()) {
          buckets[masked].add(rules.get(j));
        }
      }
    }
  }

  /**
   * right now, this doesn't do anything; later we can optimize by picking just those rules that could match
   * @param toProcess
   * @return
   */
  Iterator<Rule> iterator(CharSequence toProcess) {
    if (toProcess.length() > 0) {
      char c = toProcess.charAt(0);
      if (c < 0xD800) {
        return buckets[c % BUCKETSIZE].iterator();
      }
    }
    return rules.iterator();
  }

  public String transform(String text) {
    return new RegexTransformState(this, text).toString();
  }
  
  public String toString() {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < rules.size(); ++i) {
      result.append(rules.get(i)).append("\r\n");
    }
    return result.toString();
  }
}
