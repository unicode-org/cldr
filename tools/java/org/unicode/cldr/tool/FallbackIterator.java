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
  
  static {
    // TODO add "JP" (ja_JP_JP) "TH" (th_TH_TH) "NY" (no_NO_NY)
    // other languages with mixed scripts
    // Table 8 languages
    String[] data = {

            "canonicalize",   // mechanically generated

            // grandfathered
            "art-lojban;jbo",
            "cel-gaulish;xcg",    // Grandfathered code with special replacement: cel-gaulish
            "en-GB-oed;en-GB-x-oed",    // Grandfathered code with special replacement: en-GB-oed
            "i-ami;ami",
            "i-bnn;bnn",
            "i-default;und",    // Grandfathered code with special replacement: i-default
            "i-enochian;x-enochian",    // Grandfathered code with special replacement: i-enochian
            "i-hak;zh-hakka",
            "i-klingon;tlh",
            "i-lux;lb",
            "i-mingo;see",    // Grandfathered code with special replacement: i-mingo
            "i-navajo;nv",
            "i-pwn;pwn",
            "i-tao;tao",
            "i-tay;tay",
            "i-tsu;tsu",
            "no-bok;nb",
            "no-nyn;nn",
            "sgn-BE-fr;sfb",
            "sgn-BE-nl;vgt",
            "sgn-CH-de;sgg",
            "zh-cmn;cmn",
            "zh-cmn-Hans;cmn-Hans",
            "zh-cmn-Hant;cmn-Hant",
            "zh-gan;gan",
            "zh-guoyu;zh-cmn",
            "zh-hakka;hak",
            "zh-min;nan",   // Grandfathered code with special replacement: zh-min
            "zh-min-nan;nan",
            "zh-wuu;wuu",
            "zh-xiang;hsn",
            "zh-yue;yue",
            // language
            "in(-.*)?;id$1",
            "iw(-.*)?;he$1",
            "ji(-.*)?;yi$1",
            "jw(-.*)?;jv$1",
            // skipping sh, deprecated but no replacement
            // territory
            "(.*-)BU(-.*)?;$1MM$2",
            // skipping CS, deprecated but no replacement
            "(.*-)DD(-.*)?;$1DE$2",
            "(.*-)FX(-.*)?;$1FR$2",
            // skipping NT, deprecated but no replacement
            // skipping SU, deprecated but no replacement
            "(.*-)TP(-.*)?;$1TL$2",
            "(.*-)YD(-.*)?;$1YE$2",
            "(.*-)YU(-.*)?;$1CS$2",
            "(.*-)ZR(-.*)?;$1CD$2",

            "decanonicalize",   // mechanically generated

            "id(-.*)?;in$1",
            "he(-.*)?;iw$1",
            "yi(-.*)?;ji$1",
            "jv(-.*)?;jw$1",
            "(.*-)MM(-.*)?;$1BU$2",
            "(.*-)DE(-.*)?;$1DD$2",
            "(.*-)FR(-.*)?;$1FX$2",
            "(.*-)TL(-.*)?;$1TP$2",
            "(.*-)YE(-.*)?;$1YD$2",
            "(.*-)CS(-.*)?;$1YU$2",
            "(.*-)CD(-.*)?;$1ZR$2",
            
            // Not mechanically generated
            
            "canonicalize",
             
            // Table 8
            "arb(-.*)?;ar$1",
            "knn(-.*)?;kok$1",
            "mly(-.*)?;ms$1",
            "swh(-.*)?;sw$1",
            "uzn(-.*)?;uz$1",
            "cmn(-.*)?;zh$1",
            "uzn(-.*)?;uz$1",
            
            // special cases: no, sh
            
            "no(-.*)?;nb$1",
            "sh(?!-[a-zA-Z]{4}(?:-.*)?)(-.*)?;sr-Latn$1", // insert if no script
            "sh(-[a-zA-Z]{4}-.*);sr$1",
            
            "fallback",
            
            //"zh-Hant;zh-TW;zh",
            //"zh-Hans;zh-CN;zh",
            "zh-TW(-.*)?;zh-Hant-TW$1",
            "zh-HK(-.*)?;zh-Hant-HK$1",
            "zh-MO(-.*)?;zh-Hant-MO$1",
            "zh(?!-[a-zA-Z]{4}(?:-.*)?)(-.*);zh-Hans$1", // insert Hans if no script
            //"zh-SG;zh-Hans-SG*",
            //"zh-CN;zh-Hans-CN*",
            
            // normal truncation
            "(.*)-[^-]*;$1",
                        
            "decanonicalize",
            "zh-Hant;zh-TW",
            //"zh-TW;zh-Hant",
            "zh-Hant-TW(-.*)?;zh-Hant$1;zh-TW$1",
            "zh-Hant-HK(-.*)?;zh-HK$1",
            "zh-Hans;zh-CN",
            "zh-CN;zh-Hans",
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
        case canonicalize: CANONICALIZE_LIST.add(fallbackRule); break;
        case fallback: FALLBACK_LIST.add(fallbackRule); break;
        case decanonicalize: DECANONICALIZE_LIST.add(fallbackRule); break;
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
    items.add(original);
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
          // try out
          String last = items.get(items.size()-1);
          //if (last.endsWith("*")) {
            items.remove(items.size()-1);
            source = last; // last.substring(0,last.length()-1);
            continue fallback;
          //}
           // stop completely
          //break fallback;
        }
      }
      // if we don't get a match, we use regular truncation fallback to get the next item
      items.add(source);
      break;
//      if (!regularFallback.reset(source).matches()) {
//        break; // bail when we get to the top
//      }
//      source = regularFallback.group(1);
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
    //results.add(original);
    results.addAll(items);
    iterator = results.iterator();
    return this;
  }
  
  //Matcher regularFallback = Pattern.compile("(.*)[-_][^-_]*").matcher("");

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