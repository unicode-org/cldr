package org.unicode.cldr.test;

import org.unicode.cldr.unittest.TestVariantFolder.CaseVariantFolder;
import org.unicode.cldr.unittest.TestVariantFolder.CompatibilityFolder;
import org.unicode.cldr.unittest.TestVariantFolder.CanonicalFolder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Iso639Data;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.VariantFolder;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.Iso639Data.Scope;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.CanonicalIterator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TestMisc {
  
  static Currency SWISS_FRANC = Currency.getInstance("CHF");
  
  static class Lists {
    public static <E extends Comparable> List<E> sortedCopy(Collection<E> iterable)
    {
      List<E> list = new ArrayList();
      list.addAll(iterable);
      Collections.sort(list);
      return list;
    }
  }

  enum Foo {A, M, Z};

  public static void main(String[] args) {
    
    Transliterator en_ru = Transliterator.getInstance("en-ru");
    System.out.println("Mark + " + en_ru.transform("Mark"));
 
    Transliterator latn_cyrl = Transliterator.getInstance("Latn-Cyrl");
    System.out.println("Mark + " + latn_cyrl.transform("Mark"));

    Transliterator ulatn_ucyrl = Transliterator.getInstance("und_Latn-und_Cyrl");
    System.out.println("Mark + " + latn_cyrl.transform("Mark"));

    Locale locale = new Locale("abc-d αγζθ ?ef_g%hi","abc-d αγζθ ?ef_g%hi","abc-d αγζθ ?ef_g%hi");

    System.out.println("Locale locale = new Locale(\"abc-d αγζθ ?ef_g%hi\",\"abc-d αγζθ ?ef_g%hi\",\"abc-d αγζθ ?ef_g%hi\");");
    System.out.println("locale.toString() == \"" + locale + "\"");
    
    MyXSymbolTable sym = new MyXSymbolTable();
    BagFormatter bf = new BagFormatter();
    for (String test : new String[]{
        "[:reduceCase=[[Åå{fi}]]:]",
        "[:reduceCanonical=[[Åå{fi}]]:]",
        "[[,٫.]]",
        "[[,٫.][:close=compatibility:]]",
        "[[\\ ,٬.']]",
        "[[\\ ,٬.'][:close=compatibility:]]",
        "[[\u002E\u2024\uFE52\uFF0E\u3002][:close=compatibility:]]",
        "[[[\u002C \u002E \u066B \u2024 \u3002 \uFE52 \uFF0E、، \u002E \u2024 \uFE52 \uFF0E \u3002]-[\u002E\u2024\uFE52\uFF0E\u3002]][:close=compatibility:]]",
 
        
        
       "[[" +
       "\\u0020" +
       "[, ٬ .．․﹒ '＇ \u2018 \u2019 ]" +
       "-[.\u2024\u3002\uFE12\uFE52\uFF0E\uFF61]" +
       "-[,\u060C\u066B\u3001\uFE10\uFE11\uFE50\uFE51\uFF0C\uFF64]]" +
       "[:close=compatibility:]]",
         
         /*        "[[Åå{fi}][:close=canonical:]]",
        "[[Åå{fi}][:close=compatibility:]]",
        "[[Åå{fi}][:reduce=case:]]",
        "[[Åå{fi}][:reduce=canonical:]]",
        "[[Åå{fi}][:reduce=compatibility:]]",
        */
            }) {
      ParsePosition p = new ParsePosition(0);
      UnicodeSet set = new UnicodeSet(test,  p, sym);
      UnicodeSet codes = set.complement().complement();
      System.out.println(test + CldrUtility.LINE_SEPARATOR + 
          codes.toPattern(true) + CldrUtility.LINE_SEPARATOR + 
          bf.showSetNames(set.complement().complement()) + CldrUtility.LINE_SEPARATOR);
    }
    if (true) return;
      
      StandardCodes sc = StandardCodes.make();
      for (String s : new String[]{"language", "script", "territory"}) {
        System.out.println(s + ":\t" + sc.getGoodAvailableCodes(s).size());
      }
      if (true) return;
      
      Set<Foo> inFileOrder = EnumSet.allOf(Foo.class);
      List<Foo> inAlphaOrder = Lists.sortedCopy(inFileOrder);
      System.out.println(inFileOrder);
      System.out.println(inAlphaOrder);

      
    DecimalFormat currencyFormat = (DecimalFormat)NumberFormat.getCurrencyInstance(new ULocale("de-CH"));
    currencyFormat.setCurrency(SWISS_FRANC);
    // sometime later...
    // we want the financial format of the currency, not the retail format
    System.out.println("Retail:\t" + currencyFormat.format(123.53));
    BigDecimal increment = currencyFormat.getRoundingIncrement();
    System.out.println("Rounding Increment:\t" + increment);
    double double_increment = increment.doubleValue();
    System.out.println("Double rounding Increment:\t" + double_increment);
    double log = Math.log10(double_increment);
    System.out.println("Double log:\t" + log);
    double new_increment = Math.pow(10,Math.floor(log));
    System.out.println("Floored Increment:\t" + new_increment);
    currencyFormat.setRoundingIncrement(new_increment);
    System.out.println("Financial:\t" + currencyFormat.format(123.53));

    if (true) return;
  
      testWeights();
      if (true) return;
            
      testScripts();
     testToRegex();
      //checkEastAsianWidth();
      if (true) return;
      // import ICU
      UnicodeSet RTL = new UnicodeSet("[[:Bidi_Class=Arabic_Letter:][:Bidi_Class=Right_To_Left:]]");
      
      checkCollections();
      

      


      
      ExampleGenerator eg = new ExampleGenerator(CLDRFile.Factory.make(CldrUtility.MAIN_DIRECTORY,".*").make("en",false), CldrUtility.SUPPLEMENTAL_DIRECTORY);
      System.out.println(eg.getHelpHtml("//ldml/numbers/currencyFormats/currencyFormatLength/currencyFormat[@type=\"standard\"]/pattern[@type=\"standard\"][@draft=\"provisional\"]",""));
      System.out.println(eg.getHelpHtml("/exemplarCharacters",""));
      System.out.println(eg.getHelpHtml("/calendar/pattern",""));

      if (true) return;
      Set s = new HashSet(Arrays.asList("a", "A", "c"));
      Collator caselessCompare = Collator.getInstance(Locale.ENGLISH);
      caselessCompare.setStrength(Collator.PRIMARY);
      Set t = new TreeSet((Comparator)caselessCompare);
      t.addAll(Arrays.asList("a", "b", "c"));
      System.out.println("s equals t: " + s.equals(t));
      System.out.println("t equals s: " + t.equals(s));

      
      Set u = Collections.unmodifiableSet(t);
      System.out.println("s==t " + (s.equals(t)));
      System.out.println("s==u " + (s.equals(u)));
      UnicodeSet x = new UnicodeSet("[a-z]");
      UnicodeSet y = (UnicodeSet) new UnicodeSet("[a-z]").freeze();
      System.out.println("x==y " + (x.equals(y)));
    	//showEnglish();
    	//checkPrivateUse();
    	//testPopulous();
    	//checkDistinguishing();
      //checkEastAsianWidth();
      //checkEnglishPaths();
      System.out.println("Done");
    }
    
    private static void testWeights() {
      Factory cldrFactory = CLDRFile.Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
      CLDRFile english = cldrFactory.make("en", true);
      Set<Pair<Integer,String>> rel = new TreeSet();
      for (String desiredLocale : cldrFactory.getAvailable()) {
        int vote = CoverageLevel.Level.getDefaultWeight("google", desiredLocale);
        rel.add(new Pair(vote, desiredLocale));
      }
      for (Pair<Integer,String> p : rel) {
        System.out.println(p + "\t" + english.getName(p.getSecond()));
      }
    }

    private static void testScripts() {
      BagFormatter bf = new BagFormatter();
      
      
      UnicodeSet caseFolded = new  UnicodeSet();
      UnicodeSet simpleCaseFolded = new  UnicodeSet();
      for (int i = 0; i < 0x10FFFF; ++i) {
        String form = UTF16.valueOf(i);
        if (UCharacter.foldCase(form,true).equals(form)) {
          caseFolded.add(i);
        }
        if (UCharacter.foldCase(i,true) == i) {
          simpleCaseFolded.add(i);
        }
      }
      caseFolded.freeze();
      simpleCaseFolded.freeze();
      
      
    
      UnicodeSet functionalExceptCase = (UnicodeSet) new UnicodeSet("[" +
          "[:L:][:Mc:][:Mn:][:Nd:]" +
          "&[:^NFKC_QuickCheck=No:]" +
          "&[:^default_ignorable_code_point:]]").freeze();
      
      UnicodeSet asciiIdn = (UnicodeSet) new UnicodeSet("[-A-Z0-9]").freeze();
      
      UnicodeSet archaic =(UnicodeSet) new UnicodeSet("[" +
          "[:script=Bugi:]" +
          "[:script=Copt:]" +
          "[:script=Cprt:]" +
          "[:script=Dsrt:]" +
          "[:script=Glag:]" +
          "[:script=Goth:]" +
          "[:script=Hano:]" +
          "[:script=Ital:]" +
          "[:script=Khar:]" +
          "[:script=Linb:]" +
          "[:script=Ogam:]" +
          "[:script=Osma:]" +
          "[:script=Phag:]" +
          "[:script=Phnx:]" +
          "[:script=Runr:]" +
          "[:script=Shaw:]" +
          "[:script=Sylo:]" +
          "[:script=Syrc:]" +
          "[:script=Tagb:]" +
          "[:script=Tglg:]" +
          "[:script=Ugar:]" +
          "[:script=Xpeo:]" +
          "[:script=Xsux:]" +
//          "[:script=Arab:]" +
//          "[:script=Armn:]" +
//          "[:script=Beng:]" +
//          "[:script=Bopo:]" +
          "[:block=Combining_Diacritical_Marks _for_Symbols:]" +
          "[:block=Musical_Symbols:]" +
          "[:block=Ancient_Greek_Musical_Notation:]]").freeze();
      
      System.out.println("functionalExceptCase: " + functionalExceptCase);
      System.out.println("archaic: " + archaic);
      
      System.out.println("SimpleCaseFolded & !CaseFolded & Functional & !Archaic:" + CldrUtility.LINE_SEPARATOR 
          +  bf.showSetNames(new UnicodeSet(simpleCaseFolded)
              .removeAll(caseFolded)
              .retainAll(functionalExceptCase)
              .removeAll(archaic).
              removeAll(asciiIdn)
              ));
      
     UnicodeSet  functional = (UnicodeSet) new UnicodeSet(functionalExceptCase).retainAll(caseFolded).freeze();
     System.out.println("functional: " + functional.size());
     UnicodeSet  functionalAndNotArchaic = (UnicodeSet) new UnicodeSet(functional).removeAll(archaic).freeze();
     System.out.println("archaic: " + archaic.size());
     System.out.println("functionalAndNotArchaic: " + functionalAndNotArchaic.size());

      //System.out.println(bf.showSetNames("Case Folded", caseFolded,"Simple Case Folded", simpleCaseFolded));

      UnicodeSet functionalCommon = new UnicodeSet("[:script=common:]").retainAll(functional).removeAll(archaic).removeAll(asciiIdn);
      System.out.println("Common & Functional & !Archaic:" + CldrUtility.LINE_SEPARATOR + bf.showSetNames(functionalCommon));
 
      UnicodeSet functionalInherited = new UnicodeSet("[:script=inherited:]").retainAll(functional).removeAll(archaic).removeAll(asciiIdn);
      System.out.println("Inherited & Functional & !Archaic:" + CldrUtility.LINE_SEPARATOR + bf.showSetNames(functionalInherited));
      
      UnicodeSet nl =new UnicodeSet("[:Nl:]").retainAll(functional).removeAll(archaic);
      System.out.println("Nl:" + CldrUtility.LINE_SEPARATOR + bf.showSetNames(new UnicodeSet("[:Nl:]")));
      System.out.println("Nl & Functional & !Archaic:" + CldrUtility.LINE_SEPARATOR + bf.showSetNames(nl));
      
      UnicodeSet restrictedXidContinue = new UnicodeSet(
          "[[:xid_continue:]" +
          "&[:^NFKC_QuickCheck=No:]" +
          "&[:^default_ignorable_code_point:]" +
          "&[:^Pc:]]").retainAll(caseFolded);

      System.out.println(bf.showSetDifferences("IDNA Functional", functional,"Unicode XID & NFKC &!DefaultIgnorable &! Pc", restrictedXidContinue));
      
      Transliterator t = Transliterator.getInstance("lower");
      System.out.println("ABC " + t.transliterate("ABC"));
      /*
     generalCategory(cp) is {Ll, Lu, Lo, Lm, Mn, Mc, Nd}, AND
    * NFKC(cp) == cp, AND
    * casefold(cp) == cp, AND
    * !defaultIgnorableCodePoint(cp)
       */
      BitSet scripts = new BitSet();
      for (int cp = 0; cp < 0x10FFFF; ++cp) {
        int script = UScript.getScript(cp);
        if (script == UScript.COMMON || script == UScript.UNKNOWN || script == UScript.INHERITED) {
          continue;
        }
        scripts.set(script);
      }
      Set<String> toPrint = new TreeSet();
      for (int script= 0; script < scripts.size(); ++script) {
        if (!scripts.get(script)) continue;
        String code = UScript.getShortName(script);
        String name = UScript.getName(script);
        if (StandardCodes.isScriptModern(code)) {
          toPrint.add("modern\t" + code + "\t" + name);
        } else {
          toPrint.add("archaic\t" + code + "\t" + name);
        }
      }
      for (String line : toPrint) {
        System.out.println(line);
      }
    }

    private static void checkCollections() {
      System.out.println("Collections");
      new org.unicode.cldr.util.CldrUtility.Apply<String>() {
        public void apply(String item) {
          if (Iso639Data.getScope(item.toString()) != Scope.Collection) return;
          System.out.println(item + "\t" + CldrUtility.join(Iso639Data.getNames(item), ", "));
        }
      }.applyTo(Iso639Data.getAvailable());
      System.out.println(CldrUtility.LINE_SEPARATOR + "Macrolanguages");
      new org.unicode.cldr.util.CldrUtility.Apply<String>() {
        public void apply(String item) {
          if (Iso639Data.getScope(item.toString()) != Scope.Macrolanguage) return;
          System.out.println(item + "\t" + CldrUtility.join(Iso639Data.getNames(item), ", "));
        }
      }.applyTo(Iso639Data.getAvailable());
    }

    static void testToRegex() {
      String[] tests = {"\\-", "a", "d-f", "\\u2000" , "\\uAC00-\\uAC12", "{AB}", "{CDE}", "\\uFFF0-\\U0010000F", "\\U0010100F-\\U0010300F"}; // }; //
      for (int i =(1<<tests.length) - 1; i >= 0; --i) {
        String test = "[";
        for (int j = 0; j < tests.length; ++j) {
          if ((i & (1<<j)) != 0) {
            test += tests[j];
          }
        }
        test += "]";
        testToRegex(new UnicodeSet(test));
      }
    }

    private static void testToRegex(UnicodeSet test) {
      String formatted = CldrUtility.toRegex(test);
      System.out.println(test + "\t->\t" + formatted);
      Matcher newTest = Pattern.compile(formatted).matcher("");
      UnicodeSet failures = new UnicodeSet();
      for (UnicodeSetIterator it = new UnicodeSetIterator(test); it.next();) {
        if (!newTest.reset(it.getString()).matches()) {
          failures.add(it.getString());
        }
      }
      if (failures.size() != 0) {
        System.out.println("\tFailed on: " + failures);
      }
      System.out.flush();
    }
    
    static void checkEastAsianWidth() {
      UnicodeSet dontCares = (UnicodeSet) new UnicodeSet("[[:surrogate:][:unassigned:][:control:]]").freeze();
      UnicodeSet dontCares2 = (UnicodeSet) new UnicodeSet("[:^letter:]").freeze();
      
//      UnicodeSet wide = new UnicodeSet("[[:East_Asian_Width=wide:][:East_Asian_Width=fullwidth:][:Co:]]"); // remove supplementaries
//      System.out.format("Wide %s" + Utility.LINE_SEPARATOR + "" + Utility.LINE_SEPARATOR, wide);
//      System.out.format("Wide(spanned) %s" + Utility.LINE_SEPARATOR + "" + Utility.LINE_SEPARATOR, Utility.addDontCareSpans(wide, dontCares));
//      UnicodeSet zeroWidth = new UnicodeSet("[[:default_ignorable_code_point:][:Mn:][:Me:]-[:Noncharacter_Code_Point:]-[:Cc:]]"); // remove supplementaries
//      System.out.format("ZeroWidth %s" + Utility.LINE_SEPARATOR + "" + Utility.LINE_SEPARATOR, zeroWidth);
//      System.out.format("ZeroWidth(spanned) %s" + Utility.LINE_SEPARATOR + "" + Utility.LINE_SEPARATOR, Utility.addDontCareSpans(zeroWidth, dontCares));
      
      // P2. In each paragraph, find the first character of type L, AL, or R.
      UnicodeSet strongL = (UnicodeSet) new UnicodeSet("[[:BidiClass=L:]-[:unassigned:]]").freeze(); // 
      showSpans("Bidi L", strongL, dontCares);
      showSpans("Bidi L*", strongL, dontCares2);
      
      UnicodeSet strongRAL = (UnicodeSet) new UnicodeSet("[[:BidiClass=R:][:BidiClass=AL:]-[:unassigned:]]").freeze();
      showSpans("Bidi R,AL", strongRAL, dontCares);
      showSpans("Bidi R,AL*", strongRAL, dontCares2);
      
      UnicodeSet strong = (UnicodeSet) new UnicodeSet("[[:BidiClass=L:][:BidiClass=R:][:BidiClass=AL:]-[:unassigned:]]").freeze();
      showSpans("Strong", strong, dontCares);
      showSpans("Strong*", strong, dontCares2);

    }

    private static void showSpans(String title, UnicodeSet sourceSet, UnicodeSet dontCares) {
      System.out.println(title);
      System.out.format("\tSource Set: %s" + CldrUtility.LINE_SEPARATOR, sourceSet);
      System.out.format("\tDon't Cares: %s" + CldrUtility.LINE_SEPARATOR, dontCares);
      UnicodeSet spanned = new UnicodeSet(sourceSet).addBridges(dontCares);
      spanned = spanned.complement().complement();
      String spannedString = spanned.toString();
      String unescapedString = spanned.toPattern(false);
      System.out.format("\tRanges: %d" + CldrUtility.LINE_SEPARATOR, spanned.getRangeCount());
      System.out.format("\tStrlen(\\u): %d" + CldrUtility.LINE_SEPARATOR, spannedString.length());
      System.out.format("\tStrlen(!\\u): %d" + CldrUtility.LINE_SEPARATOR, unescapedString.length());
      String title2 = "Result";
      String sample = spannedString;
      if (false) {
        if (sample.length() > 60) {
          title2 = "Sample";
          sample = sample.substring(0,60) + " ...";
        }
      }
      System.out.format("\t%s: %s" + CldrUtility.LINE_SEPARATOR, title2, sample);
      System.out.println();
     }
    

    static int[] extraCJK = {

        0x3006,  // IDEOGRAPHIC CLOSING MARK;Lo
        0x302A,  // IDEOGRAPHIC LEVEL TONE MARK;Mn
        0x302B,  // IDEOGRAPHIC RISING TONE MARK;Mn
        0x302C,  // IDEOGRAPHIC DEPARTING TONE MARK;Mn
        0x302D,  // IDEOGRAPHIC ENTERING TONE MARK;Mn
        0x302E,  // HANGUL SINGLE DOT TONE MARK;Mn
        0x302F,  // HANGUL DOUBLE DOT TONE MARK;Mn
        0x3031,  // VERTICAL KANA REPEAT MARK;Lm
        0x3032,  // VERTICAL KANA REPEAT WITH VOICED SOUND MARK;Lm
        0x3033,  // VERTICAL KANA REPEAT MARK UPPER HALF;Lm
        0x3034,  // VERTICAL KANA REPEAT WITH VOICED SOUND MARK UPPER HALF;Lm
        0x3035,  // VERTICAL KANA REPEAT MARK LOWER HALF;Lm
        0x303C,  // MASU MARK;Lo
        0x3099,  // COMBINING KATAKANA-HIRAGANA VOICED SOUND MARK;Mn
        0x309A,  // COMBINING KATAKANA-HIRAGANA SEMI-VOICED SOUND MARK;Mn
        0x309B,  // KATAKANA-HIRAGANA VOICED SOUND MARK;Sk
        0x309C,  // KATAKANA-HIRAGANA SEMI-VOICED SOUND MARK;Sk
        0x30A0,  // KATAKANA-HIRAGANA DOUBLE HYPHEN;Pd
        0x30FC,  // KATAKANA-HIRAGANA PROLONGED SOUND MARK;Lm
        0xFF70,  // HALFWIDTH KATAKANA-HIRAGANA PROLONGED SOUND MARK;Lm
        0xFF9E,  // HALFWIDTH KATAKANA VOICED SOUND MARK;Lm
        0xFF9F,  // HALFWIDTH KATAKANA SEMI-VOICED SOUND MARK;Lm 
    };
    
    void checkCFK () {
    	//UnicodeSet Han, Hangul, Hiragana, Katakana, or Bopomofo
    }
    
    private static void checkDistinguishing() {
    	Factory cldrFactory = CLDRFile.Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
    	Set cldrFiles = cldrFactory.getAvailableLanguages();
    	Set distinguishing = new TreeSet();
    	Set nondistinguishing = new TreeSet();
    	XPathParts parts = new XPathParts();
		for (Iterator it = cldrFiles.iterator(); it.hasNext();) {
			CLDRFile cldrFile = cldrFactory.make(it.next().toString(), false);
			if (cldrFile.isNonInheriting()) continue;
			for (Iterator it2 = cldrFile.iterator(); it2.hasNext();) {
				String path = (String) it2.next();
				String fullPath = cldrFile.getFullXPath(path);
				if (path.equals(fullPath)) continue;
				parts.set(fullPath);
				for (int i = 0; i < parts.size(); ++i) {
					Map m = parts.getAttributes(i);
					if (m.size() == 0) continue;
					String element = parts.getElement(i);
					for (Iterator mit = m.keySet().iterator(); mit.hasNext();) {
						String attribute = (String) mit.next();
						if (CLDRFile.isDistinguishing(element, attribute)) {
							distinguishing.add(attribute + "\tD\t" + element);
						} else {
							nondistinguishing.add(attribute + "\tN\t" + element);
						}
					}
				}
			}
		}
		System.out.println("Distinguishing");
		for (Iterator it = distinguishing.iterator(); it.hasNext();) {
			System.out.println(it.next());
		}
		System.out.println();
		System.out.println("Non-Distinguishing");
		for (Iterator it = nondistinguishing.iterator(); it.hasNext();) {
			System.out.println(it.next());
		}
	}



	private static void showEnglish() {
    	Factory cldrFactory = CLDRFile.Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
    	String requestedLocale = "en";
		CLDRFile cldrFile = cldrFactory.make(requestedLocale, true);
		CLDRFile.Status status = new CLDRFile.Status();
		for (Iterator it = cldrFile.iterator(); it.hasNext();) {
			String requestedPath = (String) it.next();
			String localeWhereFound = cldrFile.getSourceLocaleID(requestedPath, status);
			if (!localeWhereFound.equals(requestedLocale) || !status.pathWhereFound.equals(requestedPath)) {
				System.out.println("requested path:\t" + requestedPath
						+ "\tfound locale:\t" + localeWhereFound
						+ "\tsame?\t" + localeWhereFound.equals(requestedLocale)
						+ "\tfound path:\t" + status.pathWhereFound
						+ "\tsame?\t" + status.pathWhereFound.equals(requestedPath)
						);
			}
		}
	}
    private static void checkPrivateUse() {
    	Factory cldrFactory = CLDRFile.Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
    	String requestedLocale = "en";
		CLDRFile cldrFile = cldrFactory.make(requestedLocale, true);
		CLDRFile.Status status = new CLDRFile.Status();
		StandardCodes sc = StandardCodes.make();
		XPathParts parts = new XPathParts();
		Set careAbout = new HashSet(Arrays.asList(new String[]{"language", "script", "territory", "variant"}));
		HashMap foundItems = new HashMap();
		TreeSet problems = new TreeSet();
		for (Iterator it = cldrFile.iterator("", new UTF16.StringComparator(true,false, 0)); it.hasNext();) {
			String requestedPath = (String) it.next();
			parts.set(requestedPath);
			String element = parts.getElement(-1);
			if (!careAbout.contains(element)) continue;
			String type = parts.getAttributeValue(-1,"type");
			if (type == null) continue;
			Set foundSet = (Set)foundItems.get(element);
			if (foundSet == null) foundItems.put(element, foundSet =new TreeSet());
			foundSet.add(type);

			List data = sc.getFullData(element, type);
			if (data == null) {
				problems.add("No RFC3066bis data for: " + element + "\t" + type + "\t" + cldrFile.getStringValue(requestedPath));
				continue;
			}
			if (isPrivateOrDeprecated(data)) {
				problems.add("Private/Deprecated Data for: " + element + "\t" + type + "\t"
						+ cldrFile.getStringValue(requestedPath) + "\t" + data);
			}
			//String canonical_value = (String)data.get(2);
		}
		for (Iterator it = problems.iterator(); it.hasNext();) {
			System.out.println(it.next());
		}
		for (Iterator it = careAbout.iterator(); it.hasNext();) {
			String element = (String) it.next();
			Set real = sc.getAvailableCodes(element);
			Set notFound = new TreeSet(real);
			notFound.removeAll((Set)foundItems.get(element));
			for (Iterator it2 = notFound.iterator(); it2.hasNext();) {
				String type = (String) it2.next();
				List data = sc.getFullData(element, type);
				if (isPrivateOrDeprecated(data)) continue;
				System.out.println("Missing Translation for: " + element + "\t" + type + "\t"
						+ "\t" + data);
			}
		}
	}

    static boolean isPrivateOrDeprecated(List data) {
    	if (data.toString().indexOf("PRIVATE") >= 0) {
    		return true;
    	}
    	if ("PRIVATE USE".equals(data.get(0))) return true;
    	if (data.size() < 3) return false;
    	if (data.get(2) == null) return false;
    	if (data.get(2).toString().length() != 0) return true;
    	return false;
    }
    
	static void testPopulous() {
        Factory cldrFactory = CLDRFile.Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
        CLDRFile supp = cldrFactory.make("supplementalData", false);
        CLDRFile temp = CLDRFile.make("supplemental");
        temp.setNonInheriting(true);
        XPathParts parts = new XPathParts(null, null);
        for (Iterator it = supp.iterator(null, CLDRFile.ldmlComparator); it.hasNext();) {
            String path = it.next().toString();
            String value = supp.getStringValue(path);
            String fullPath = supp.getFullXPath(path);
            parts.set(fullPath);
            //Map attributes = parts.getAttributes(-1);
            String type = parts.getAttributeValue(-1, "type");
            String pop = (String) language_territory_hack_map.get(type);
            if (pop != null) {
                parts.putAttributeValue(-1, "mostPopulousTerritory", pop);
                fullPath = parts.toString();
            }
            temp.add(fullPath, value);
        }
        PrintWriter pw = new PrintWriter(System.out);
        temp.write(pw);
        pw.close();
    }
    private static final Map language_territory_hack_map = new HashMap();
    private static final String[][] language_territory_hack = {
        {"af", "ZA"},
        {"am", "ET"},
        {"ar", "SA"},
        {"as", "IN"},
        {"ay", "PE"},
        {"az", "AZ"},
        {"bal", "PK"},
        {"be", "BY"},
        {"bg", "BG"},
        {"bn", "IN"},
        {"bs", "BA"},
        {"ca", "ES"},
        {"ch", "MP"},
        {"cpe", "SL"},
        {"cs", "CZ"},
        {"cy", "GB"},
        {"da", "DK"},
        {"de", "DE"},
        {"dv", "MV"},
        {"dz", "BT"},
        {"el", "GR"},
        {"en", "US"},
        {"es", "ES"},
        {"et", "EE"},
        {"eu", "ES"},
        {"fa", "IR"},
        {"fi", "FI"},
        {"fil", "PH"},
        {"fj", "FJ"},
        {"fo", "FO"},
        {"fr", "FR"},
        {"ga", "IE"},
        {"gd", "GB"},
        {"gl", "ES"},
        {"gn", "PY"},
        {"gu", "IN"},
        {"gv", "GB"},
        {"ha", "NG"},
        {"he", "IL"},
        {"hi", "IN"},
        {"ho", "PG"},
        {"hr", "HR"},
        {"ht", "HT"},
        {"hu", "HU"},
        {"hy", "AM"},
        {"id", "ID"},
        {"is", "IS"},
        {"it", "IT"},
        {"ja", "JP"},
        {"ka", "GE"},
        {"kk", "KZ"},
        {"kl", "GL"},
        {"km", "KH"},
        {"kn", "IN"},
        {"ko", "KR"},
        {"kok", "IN"},
        {"ks", "IN"},
        {"ku", "TR"},
        {"ky", "KG"},
        {"la", "VA"},
        {"lb", "LU"},
        {"ln", "CG"},
        {"lo", "LA"},
        {"lt", "LT"},
        {"lv", "LV"},
        {"mai", "IN"},
        {"men", "GN"},
        {"mg", "MG"},
        {"mh", "MH"},
        {"mk", "MK"},
        {"ml", "IN"},
        {"mn", "MN"},
        {"mni", "IN"},
        {"mo", "MD"},
        {"mr", "IN"},
        {"ms", "MY"},
        {"mt", "MT"},
        {"my", "MM"},
        {"na", "NR"},
        {"nb", "NO"},
        {"nd", "ZA"},
        {"ne", "NP"},
        {"niu", "NU"},
        {"nl", "NL"},
        {"nn", "NO"},
        {"no", "NO"},
        {"nr", "ZA"},
        {"nso", "ZA"},
        {"ny", "MW"},
        {"om", "KE"},
        {"or", "IN"},
        {"pa", "IN"},
        {"pau", "PW"},
        {"pl", "PL"},
        {"ps", "PK"},
        {"pt", "BR"},
        {"qu", "PE"},
        {"rn", "BI"},
        {"ro", "RO"},
        {"ru", "RU"},
        {"rw", "RW"},
        {"sd", "IN"},
        {"sg", "CF"},
        {"si", "LK"},
        {"sk", "SK"},
        {"sl", "SI"},
        {"sm", "WS"},
        {"so", "DJ"},
        {"sq", "CS"},
        {"sr", "CS"},
        {"ss", "ZA"},
        {"st", "ZA"},
        {"sv", "SE"},
        {"sw", "KE"},
        {"ta", "IN"},
        {"te", "IN"},
        {"tem", "SL"},
        {"tet", "TL"},
        {"th", "TH"},
        {"ti", "ET"},
        {"tg", "TJ"},
        {"tk", "TM"},
        {"tkl", "TK"},
        {"tvl", "TV"},
        {"tl", "PH"},
        {"tn", "ZA"},
        {"to", "TO"},
        {"tpi", "PG"},
        {"tr", "TR"},
        {"ts", "ZA"},
        {"uk", "UA"},
        {"ur", "IN"},
        {"uz", "UZ"},
        {"ve", "ZA"},
        {"vi", "VN"},
        {"wo", "SN"},
        {"xh", "ZA"},
        {"zh", "CN"},
        {"zh_Hant", "TW"},
        {"zu", "ZA"},
        {"aa", "ET"},
        {"byn", "ER"},
        {"eo", "DE"},
        {"gez", "ET"},
        {"haw", "US"},
        {"iu", "CA"},
        {"kw", "GB"},
        {"sa", "IN"},
        {"sh", "HR"},
        {"sid", "ET"},
        {"syr", "SY"},
        {"tig", "ER"},
        {"tt", "RU"},
        {"wal", "ET"},  };
    static {
        for (int i = 0; i < language_territory_hack.length; ++i) {
            language_territory_hack_map.put(language_territory_hack[i][0],language_territory_hack[i][1]);
        }
    }

  static class MyXSymbolTable extends UnicodeSet.XSymbolTable {
      static VariantFolder caseFolder = new VariantFolder(new CaseVariantFolder());
      static VariantFolder canonicalFolder = new VariantFolder(new CanonicalFolder());
      static VariantFolder compatibilityFolder = new VariantFolder(new CompatibilityFolder());
      public boolean applyPropertyAlias(String propertyName, String propertyValue, UnicodeSet result) {
        if (propertyName.equalsIgnoreCase("close")) {
          if (propertyValue.equalsIgnoreCase("case")) {
            result.addAll(caseFolder.getClosure(result));
          } else if (propertyValue.equalsIgnoreCase("canonical")) {
            result.addAll(canonicalFolder.getClosure(result));   
          } else if (propertyValue.equalsIgnoreCase("compatibility")) {
            result.addAll(compatibilityFolder.getClosure(result));   
          }
          return true;
        } else if (propertyName.equalsIgnoreCase("reduce")) {
          if (propertyValue.equalsIgnoreCase("case")) {
            UnicodeSet temp = caseFolder.reduce(result);
            result.clear().addAll(temp);
          } else if (propertyValue.equalsIgnoreCase("canonical")) {
            UnicodeSet temp = canonicalFolder.reduce(result);
            result.clear().addAll(temp);
          } else if (propertyValue.equalsIgnoreCase("compatibility")) {
            UnicodeSet temp = compatibilityFolder.reduce(result);
            result.clear().addAll(temp);
          }
          return true;
        } else if (propertyName.equalsIgnoreCase("reduceCase")) {
          UnicodeSet temp = caseFolder.reduce(new UnicodeSet(propertyValue.replace(
              "·]", ":]")));
          result.clear().addAll(temp);
          return true;
        } else if (propertyName.equalsIgnoreCase("reduceCanonical")) {
          UnicodeSet temp = canonicalFolder.reduce(new UnicodeSet(propertyValue.replace(
              "·]", ":]")));
          result.clear().addAll(temp);
          return true;
        } else if (propertyName.equalsIgnoreCase("reduceCase")) {
          UnicodeSet temp = caseFolder.reduce(new UnicodeSet(propertyValue.replace(
              "·]", ":]")));
          result.clear().addAll(temp);
          return true;
        }
        return false;
      }
    }


}