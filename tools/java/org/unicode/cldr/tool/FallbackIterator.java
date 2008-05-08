/**
 * 
 */
package org.unicode.cldr.tool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FallbackIterator implements Iterator<String> {
  private static final boolean DEBUG = false;

  private static class FallbackRule {
    Matcher matcher;
    String[] additions;
    String source;
    public FallbackRule(String string) {
      String[] data = string.split(";");
      boolean first = true;
      List<String> strings = new ArrayList<String>();
      for (String datum : data) {
        if (first) {
          matcher = Pattern.compile(datum).matcher("");
          first = false;
        } else {
          strings.add(datum);
        }
      }
      additions = strings.toArray(new String[strings.size()]);
    }
    public boolean matches(String source) {
      this.source = source;
      return matcher.reset(source).matches();
    }
    public List<String> getAdditions() {
      List<String> results = new ArrayList<String>(additions.length);
      results.add(source);
      for (String addition : additions) {
        String copy = addition;
        for (int i = 0; i <= matcher.groupCount(); ++i) {
          final String group = matcher.group(i);
          copy = copy.replace("$" + i, group == null ? "" : group); // optimize later
        }
        results.add(copy);
      }
      return results;
    }
    public String toString() {
      return "{" + matcher.toString() + ", " + Arrays.asList(additions) + "}";
    }
  }

  private static final List<FallbackIterator.FallbackRule> FALLBACK_LIST = new ArrayList<FallbackIterator.FallbackRule>();
  private static final List<FallbackIterator.FallbackRule> CANONICALIZE_LIST = new ArrayList<FallbackIterator.FallbackRule>();
  private static final List<FallbackIterator.FallbackRule> DECANONICALIZE_LIST = new ArrayList<FallbackIterator.FallbackRule>();
  private enum Type {canonicalize, fallback, decanonicalize};
  
  static { // load array
    String[] data = {
            "canonicalize",
            "cmn(-.*)?;zh$1",
            "zh-cmn(-.*)?;zh$1",
            "(.*-)YU(-.*)?;$1CS$2",
            "no(-.*)?;nb$1",
            "fallback",
            "zh-Hant;zh-TW;zh",
            "zh-Hans;zh-CN;zh",
            //"zh-Hant-TW;zh-Hant;zh-TW;zh",
            "zh-TW;zh-Hant-TW;zh-Hant;zh",
            "zh-CN;zh-Hans-CN;zh-Hans;zh",
            "zh-TW(-.*);zh-Hant-TW;zh-Hant;zh",
            "zh-CN(-.*);zh-Hans-CN;zh-Hans;zh",
            "decanonicalize",
            "zh(-.*)?;cmn$1", // ;zh-cmn$1",
            "(.*-)CS(-.*)?;$1YU$2",
            "nb(-.*)?;no$1",
    };
    // do this way to emulate reading from file
    Type type = null;
    for (String row : data) {
      if (!row.contains(";")) {
        type = Type.valueOf(row);
        continue;
      }
      final FallbackRule fallbackRule = new FallbackRule(row);
      switch (type) {
        case canonicalize: CANONICALIZE_LIST.add(fallbackRule);
        case fallback: FALLBACK_LIST.add(fallbackRule);
        case decanonicalize: DECANONICALIZE_LIST.add(fallbackRule);
      }
      if (DEBUG) System.out.println(fallbackRule);
    }
  }
  // we can look at doing this incrementally later on, but for now just generate and delegate.
  private static Iterator<String> emptyIterator;
  static {
    List<String> foo = Collections.emptyList();
    emptyIterator = foo.iterator();
  }
  private Iterator<String> iterator = emptyIterator;

  public FallbackIterator() {
  }

  public FallbackIterator(String source) {
    this.set(source);
  }

  public FallbackIterator set(String source) {
    // all of this can be optimized later
    String original = source;
    List<String> items = new ArrayList<String>();
    // canonicalize (normally in constructor)
    canonicalize:
    while (true) {
      for (FallbackIterator.FallbackRule rule : CANONICALIZE_LIST) {
        if (rule.matches(source)) {
          source = rule.getAdditions().get(1);
          continue canonicalize; // try again for others
        }
      }
      break;
    }
    // fallback
    fallback:
    while (true) {
      for (FallbackIterator.FallbackRule rule : FALLBACK_LIST) {
        if (rule.matches(source)) {
          items.addAll(rule.getAdditions());
          break fallback;
        }
      }
      // if we don't get a match, we use regular truncation fallback to get the next item
      items.add(source);
      if (!regularFallback.reset(source).matches()) {
        break; // bail when we get to the top
      }
      source = regularFallback.group(1);
    }
    // for each decanonicalize rule, add all premutations it generates
    for (FallbackIterator.FallbackRule rule : DECANONICALIZE_LIST) {
      List<String> localExpanded = new ArrayList<String>();
      for (String localeItem : items) {
        if (rule.matches(localeItem)) {
          localExpanded.addAll(rule.getAdditions());
        } else {
          localExpanded.add(localeItem);
        }
      }
      items = localExpanded;
    }
    // if we didn't need to start with the original, we could skip this bit.
    LinkedHashSet<String> results = new LinkedHashSet<String>();
    results.add(original);
    results.addAll(items);
    iterator = results.iterator();
    return this;
  }
  
  Matcher regularFallback = Pattern.compile("(.*)[-_][^-_]*").matcher("");

  public void remove() {
    throw new UnsupportedOperationException(); 
  }

  public boolean hasNext() {
    return iterator.hasNext();
  }

  public String next() {
    return iterator.next();
  }
}