package org.unicode.cldr.draft;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;

/**
 * Immutable class that performs transformations
 * @author markdavis
 */
public class RegexTransform implements com.ibm.icu.text.StringTransform {
  private final List<Rule> rules;

  public RegexTransform(List<Rule> rules2) {
    rules = new ArrayList<Rule>(rules2);
  }

  /**
   * right now, this doesn't do anything; later we can optimize by picking just those rules that could match
   * @param toProcess
   * @return
   */
  Iterator<Rule> iterator(CharSequence toProcess) {
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
