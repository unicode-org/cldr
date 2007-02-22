package org.unicode.cldr.test;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.PrettyPath;
import org.unicode.cldr.util.Relation;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.impl.PrettyPrinter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;


public class TestMisc {
    public static void main(String[] args) {
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
    
    static void checkEastAsianWidth() {
      UnicodeSet dontCares = new UnicodeSet("[[:Cs:][:Cn:][:Cc:][:Noncharacter_Code_Point:]]");
      
      UnicodeSet wide = new UnicodeSet("[[:East_Asian_Width=wide:][:East_Asian_Width=fullwidth:][:Co:]]"); // remove supplementaries
      System.out.format("Wide %s\r\n\r\n", wide);
      System.out.format("Wide(spanned) %s\r\n\r\n", Utility.addDontCareSpans(wide, dontCares));
      UnicodeSet zeroWidth = new UnicodeSet("[[:default_ignorable_code_point:][:Mn:][:Me:]-[:Noncharacter_Code_Point:]-[:Cc:]]"); // remove supplementaries
      System.out.format("ZeroWidth %s\r\n\r\n", zeroWidth);
      System.out.format("ZeroWidth(spanned) %s\r\n\r\n", Utility.addDontCareSpans(zeroWidth, dontCares));
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
    	Factory cldrFactory = CLDRFile.Factory.make(Utility.MAIN_DIRECTORY, ".*");
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
    	Factory cldrFactory = CLDRFile.Factory.make(Utility.MAIN_DIRECTORY, ".*");
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
    	Factory cldrFactory = CLDRFile.Factory.make(Utility.MAIN_DIRECTORY, ".*");
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
        Factory cldrFactory = CLDRFile.Factory.make(Utility.MAIN_DIRECTORY, ".*");
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

}