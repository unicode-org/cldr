/*
 ******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
*/
package org.unicode.cldr.test;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.xml.sax.SAXException;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Utility;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.StringValue;
import org.unicode.cldr.util.CLDRFile.Value;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UnicodeSet;

/**
 * Initial version of CLDR tests
 */
public class CLDRTest extends TestFmwk {
	private boolean skipDraft = true;
	private Set locales;
	private Set languageLocales;
	private CLDRFile.Factory cldrFactory;
	
	/**
	 * TestFmwk boilerplate
	 */
	public static void main(String[] args) throws Exception {
		double deltaTime = System.currentTimeMillis();
        new CLDRTest().run(args);
        deltaTime = System.currentTimeMillis() - deltaTime;
        System.out.println("Seconds: " + deltaTime/1000);
    }
	
	/**
	 * TestFmwk boilerplate
	 */
	public CLDRTest() throws SAXException, IOException {
		// TODO parameterize the directory and filter
		cldrFactory = CLDRFile.Factory.make("C:\\ICU4C\\locale\\common\\main\\", ".*", null);
		//CLDRKey.main(new String[]{"-mde.*"});
		locales = cldrFactory.getAvailable();
		languageLocales = cldrFactory.getAvailableLanguages();
	}
	
	/**
	 * Check to make sure that the currency formats are kosher.
	 */
	public void TestCurrencyFormats() {
	    //String decimal = "/ldml/numbers/decimalFormats/decimalFormatLength/decimalFormat[@type=\"standard\"]/";
	    //String currency = "/ldml/numbers/currencyFormats/currencyFormatLength/currencyFormat[@type=\"standard\"]/";
		for (Iterator it = locales.iterator(); it.hasNext();) {
			String locale = (String)it.next();
			boolean isPOSIX = locale.indexOf("POSIX") >= 0;
			logln("Testing: " + locale);
			CLDRFile item = cldrFactory.make(locale, false);
			for (Iterator it2 = item.keySet().iterator(); it2.hasNext();) {
				String xpath = (String) it2.next();
				if (!xpath.startsWith("/ldml/numbers/")) {
					continue;
				}
				CLDRFile.StringValue value = (StringValue) item.getValue(xpath);
				byte type;
				if (xpath.startsWith("/ldml/numbers/currencyFormats/")) {
					type = CURRENCY_TYPE;
				} else if (xpath.startsWith("/ldml/numbers/decimalFormats/")) {
					type = DECIMAL_TYPE;
				} else if (xpath.startsWith("/ldml/numbers/percentFormats/")) {
					type = PERCENT_TYPE;
				} else if (xpath.startsWith("/ldml/numbers/scientificFormats/")) {
					type = SCIENTIFIC_TYPE;
				} else if (xpath.startsWith("/ldml/numbers/currencies/currency/")
						&& xpath.indexOf("pattern") >= 0) {
					type = CURRENCY_TYPE;
					System.out.println(xpath + value);
				} else {
					continue;
				}
				// at this point, we only have currency formats
				String pattern = getCanonicalPattern(value.getStringValue(), type, isPOSIX);
				if (!pattern.equals(value.getStringValue())) {
					String draft = "";
					if (value.getFullXPath().indexOf("[@draft=\"true\"]") >= 0) draft = " [draft]";
					assertEquals(getLocaleAndName(locale) + draft + " " + TYPE_NAME[type] + " pattern incorrect", pattern, value.getStringValue());
				}
			}
		}
	}
	
	static final byte CURRENCY_TYPE = 0, DECIMAL_TYPE = 1, PERCENT_TYPE = 2, SCIENTIFIC_TYPE = 3;
	static final String[] TYPE_NAME = {"currency", "decimal", "percent", "scientific"};
	static int[][] DIGIT_COUNT = {{1,2,2}, {1,0,3}, {1,0,0}, {0,0,0}};
	static int[][] POSIX_DIGIT_COUNT = {{1,2,2}, {1,0,6}, {1,0,0}, {1,6,6}};
	
	String getCanonicalPattern(String inpattern, byte type, boolean isPOSIX) {
		// TODO fix later to properly handle quoted ;
		DecimalFormat df = new DecimalFormat(inpattern);
		int decimals = type == CURRENCY_TYPE ? 2 : 1;
		int[] digits = isPOSIX ? POSIX_DIGIT_COUNT[type] : DIGIT_COUNT[type];
		df.setMinimumIntegerDigits(digits[0]);
		df.setMinimumFractionDigits(digits[1]);
		df.setMaximumFractionDigits(digits[2]);
		String pattern = df.toPattern();

		int pos = pattern.indexOf(';');
		if (pos < 0) return pattern + ";-" + pattern;
		return pattern;
	}
	
	static class ValueCount {
		int count = 1;
		Value value;
	}
	
	/**
	 * Verify that if all the children of a language locale do not have the same value for the same key.
	 */
	public void TestCommonChildren() {
		Map currentValues = new TreeMap();
		Set okValues = new TreeSet();
	
		for (Iterator it = languageLocales.iterator(); it.hasNext();) {
			String parent = (String)it.next();
			logln("Testing: " + parent);		
			currentValues.clear();
			okValues.clear();
			Set availableWithParent = cldrFactory.getAvailableWithParent(parent, true);
			for (Iterator it1 = availableWithParent.iterator(); it1.hasNext();) {
				String locale = (String)it1.next();
				logln("\tTesting: " + locale);
				CLDRFile item = cldrFactory.make(locale, false);
				// Walk through all the xpaths, adding to currentValues
				// Whenever two values for the same xpath are different, we remove from currentValues, and add to okValues
				for (Iterator it2 = item.keySet().iterator(); it2.hasNext();) {
					String xpath = (String) it2.next();
					if (okValues.contains(xpath)) continue;
					if (xpath.startsWith("/ldml/identity/")) continue; // skip identity elements
					Value v = item.getValue(xpath);
					ValueCount last = (ValueCount) currentValues.get(xpath);
					if (last == null) {
						ValueCount vc = new ValueCount();
						vc.value = v;
						currentValues.put(xpath, vc);
					} else if (v.equals(last.value)) {
						last.count++;
					} else {
						okValues.add(xpath);
						currentValues.remove(xpath);
					}
				}
				// at the end, only the keys left in currentValues are (possibly) faulty
				// they are actually bad IFF either 
				// (a) the count is equal to the total (thus all children are the same), or
				// (b) their value is the same as the parent's resolved value (thus all children are the same or the same
				// as the inherited parent value).
			}
			if (currentValues.size() == 0) continue;
			int size = availableWithParent.size();
			CLDRFile parentCLDR = cldrFactory.make(parent, true);
			XPathParts p = new XPathParts();
			for (Iterator it2 = currentValues.keySet().iterator(); it2.hasNext();) {
				String xpath = (String) it2.next();
				ValueCount vc = (ValueCount) currentValues.get(xpath);
				if (vc.count == size || vc.value.equals(parentCLDR.getValue(xpath))) {
					String draft = "";
					if (true) {
						if (vc.value.getFullXPath().indexOf("[@draft=\"true\"]") >= 0) draft = " [draft]";
					} else {
						p.set(vc.value.getFullXPath());
						if (p.containsAttributeValue("draft","true")) draft = " [draft]";
					}
					String count = (vc.count == size ? "" : vc.count + "/") + size;
					errln(getLocaleAndName(parent) + draft +
							", all children (" + count + ") have same value for:\t"
							+ xpath + ";\t" + vc.value.getStringValue());
				}
			}
		}
	}
	
	/**
	 * Check that the exemplars include all characters in the data.
	 */
	public void TestThatExemplarsContainAll() {
		UnicodeSet commonAndInherited = new UnicodeSet("[[:script=common:][:script=inherited:][:alphabetic=false:]]"); // 
		Set counts = new TreeSet();
		int totalCount = 0;
		UnicodeSet localeMissing = new UnicodeSet();
		for (Iterator it = locales.iterator(); it.hasNext();) {
			String locale = (String)it.next();
			if (locale.equals("root")) continue; 
			CLDRFile resolved = cldrFactory.make(locale, true);
			UnicodeSet exemplars = getExemplarSet(resolved,"");
			if (exemplars.size() == 0) {
				errln(getLocaleAndName(locale) + " has empty exemplar set");
			}
			exemplars.addAll(getExemplarSet(resolved,"standard"))
			.addAll(getExemplarSet(resolved,"auxiliary"))
			.addAll(commonAndInherited);
			CLDRFile plain = cldrFactory.make(locale, false);
			int count = 0;
			localeMissing.clear();
			for (Iterator it2 = plain.keySet().iterator(); it2.hasNext();) {
				String xpath = (String) it2.next();
				if (xpath.indexOf("/exemplarCharacters") > 0
						|| xpath.indexOf("/pattern") > 0) continue; // skip some items.
				Value pvalue = plain.getValue(xpath);
				if (skipDraft && pvalue.getFullXPath().indexOf("[@draft=\"true\"") > 0) continue;
				String value = pvalue.getStringValue();
				if (!exemplars.containsAll(value)) {
					count++;
					UnicodeSet missing = new UnicodeSet().addAll(value).removeAll(exemplars);
					localeMissing.addAll(missing);
					errln(getLocaleAndName(locale) + "\t" + xpath + "/" + value + " contains " + missing + ", not in exemplars");					
				}
			}
			NumberFormat nf = new DecimalFormat("000");
			if (count != 0) {
				totalCount += count;
				counts.add(nf.format(count) + "\t" + getLocaleAndName(locale) + "\t" + localeMissing);
			}
		}
		for (Iterator it = counts.iterator(); it.hasNext();) {
			logln(it.next().toString());
		}
		logln("Total Count: " + totalCount);
	}

	/**
	 * @param resolved
	 * @return
	 */
	public UnicodeSet getExemplarSet(CLDRFile resolved, String type) {
		if (type.length() != 0) type = "[@type=\"" + type + "\"]";
		Value v = resolved.getValue("/ldml/characters/exemplarCharacters" + type);
		if (v == null) return new UnicodeSet();
		String pattern = v.getStringValue();
		if (pattern.indexOf("[:") >= 0 || pattern.indexOf("\\p{") > 0) {
			errln(getLocaleName(resolved.getKey()) + " exemplar pattern contains property: " + pattern);
		}
		UnicodeSet result = new UnicodeSet(v.getStringValue(), UnicodeSet.CASE);
		if (type.length() != 0) System.out.println("fetched set for " + type);
		return result;
	}
	
	Map localeNameCache = new HashMap();
	CLDRFile english = null;

	public String getLocaleAndName(String locale) {
		return locale + " (" + getLocaleName(locale) + ")";
	}

	public String getLocaleName(String locale) {
		String name = (String) localeNameCache.get(locale);
		if (name != null) return name;
		if (english == null) english = cldrFactory.make("en", true);
		String[] pieces = new String[10];
		Utility.split(locale, '_', pieces);
		int i = 0;
		String result = getName(english, "languages/language", pieces[i++]);
		if (pieces[i].length() == 0) return result;
		if (pieces[i].length() == 4) {
			result += " " + getName(english, "scripts/script", pieces[i++]);
		}
		if (pieces[i].length() == 0) return result;
		result += " " + getName(english, "territories/territory", pieces[i++]);
		if (pieces[i].length() == 0) return result;
		result += " " + getName(english, "variant/variants", pieces[i++]);
		localeNameCache.put(locale, result);
		return result;
	}

	/**
	 * @param english
	 * @param pieces
	 * @param i
	 * @return
	 */
	private String getName(CLDRFile english, String kind, String type) {
		Value v = english.getValue("/ldml/localeDisplayNames/" + kind + "[@type=\"" + type + "\"]");
		if (v == null) return "<" + type + ">";
		return v.getStringValue();
	}
	
	/**
	 * Make sure we are only using attribute values that are in RFC3066, the Olson database (with aliases removed)
	 * or ISO 4217
	 * @throws IOException
	 */
	public void TestForIllegalAttributeValues()  {
		// check for illegal attribute values that are not in the DTD
		XPathParts parts = new XPathParts();
		Map result = new TreeMap();
		for (Iterator it = locales.iterator(); it.hasNext();) {
			String locale = (String)it.next();
			logln("Testing: " + locale);
			CLDRFile item = cldrFactory.make(locale, false);
			result.clear();
			for (Iterator it2 = item.keySet().iterator(); it2.hasNext();) {
				String xpath = (String) it2.next();
				CLDRFile.StringValue value = (StringValue) item.getValue(xpath);
				parts.set(value.getFullXPath());
				for (int i = 0; i < parts.size(); ++i) {
					String element = parts.getElement(i);
					Map attributes = parts.getAttributes(i);
					if (attributes == null) continue;
					for (Iterator it3 = attributes.keySet().iterator(); it3.hasNext();) {
						String attribute = (String) it3.next();
						String avalue = (String) attributes.get(attribute);
						checkValidity(element, attribute, avalue, result);
					}
				}
			}
			String localeName = getLocaleAndName(locale);
			for (Iterator it3 = result.keySet().iterator(); it3.hasNext();) {
				String code = (String) it3.next();
				Set avalues = (Set) result.get(code);
				errln(getLocaleAndName(locale) + "\tillegal attribute Value for " + code + ", value:\t" + show(avalues));
			}
		}
	}
	
	/**
	 * @param avalues
	 * @return
	 */
	private String show(Collection avalues) {
		StringBuffer result = new StringBuffer("{");
		boolean first = true;
		for (Iterator it3 = avalues.iterator(); it3.hasNext();) {
			if (first) first = false;
			else result.append(", ");
			result.append(it3.next().toString());
		}
		result.append("}");
		return result.toString();
	}

	/**
	 * @param element
	 * @param attribute
	 * @param avalue
	 * @throws IOException
	 */
	private void checkValidity(String element, String attribute, String avalue, Map results)  {
		StandardCodes codes = StandardCodes.make();
		if (attribute.equals("type")) {
			if (element.equals("currency")) checkCodes("currency", avalue, codes, results);
			else if (element.equals("script")) checkCodes("script", avalue, codes, results);
			else if (element.equals("territory")) checkCodes("region", avalue, codes, results);
			else if (element.equals("language")) checkCodes("language", avalue, codes, results);
			else if (element.equals("zone")) checkCodes("tzid", avalue, codes, results);
		}
	}

	/**
	 * @param locale
	 * @param avalue
	 * @param codes
	 */
	private void checkCodes(String code, String avalue, StandardCodes codes, Map results) {
		if (codes.getData(code, avalue) == null) {
			Set s = (Set) results.get(code);
			if (s == null) {
				s = new TreeSet();
				results.put(code, s);
			}
			s.add(avalue);
		}
	}

	/**
	 * Verify that English has everything translated.
	 * @throws IOException
	 */
	public void TestTranslatedCodes() {
		// just test English for now
		if (english == null) english = cldrFactory.make("en", true);		
		checkTranslatedCodes(english);
	}

	/**
	 * @throws IOException
	 */
	private void checkTranslatedCodes(CLDRFile cldrfile)  {
		StandardCodes codes = StandardCodes.make();
		checkTranslatedCode(cldrfile, codes, "currency", "/ldml/numbers/currencies/currency");
		checkTranslatedCode(cldrfile, codes, "tzid", "/ldml/dates/timeZoneNames/zone");
		checkTranslatedCode(cldrfile, codes, "language", "/ldml/localeDisplayNames/languages/language");
		checkTranslatedCode(cldrfile, codes, "script", "/ldml/localeDisplayNames/scripts/script");
		checkTranslatedCode(cldrfile, codes, "region", "/ldml/localeDisplayNames/territories/territory");
		checkTranslatedCode(cldrfile, codes, "variant", "/ldml/localeDisplayNames/variants/variant");
	}

	/**
	 * @param codes
	 * @param type
	 * @param fragment
	 */
	private void checkTranslatedCode(CLDRFile cldrfile, StandardCodes codes, String type, String fragment) {
		Set codeItems = codes.getAvailableCodes(type);
		int count = 0;
		for (Iterator it = codeItems.iterator(); it.hasNext();) {
			String code = (String) it.next();
			String rfcname = codes.getData(type, code);
			if (rfcname.equals("ZZ")) continue;
			++count;
			String fullFragment = fragment + "[@type=\"" + code + "\"]";
			Value v = cldrfile.getValue(fullFragment);
			if (v == null) {
				errln(type + " missing English translation for: "  + code + "\t(" + rfcname + ")");
				continue;
			}
			String translation = v.getStringValue();
			if (!translation.equalsIgnoreCase(rfcname)) {
				logln(type + " translation differs from RFC, check: " + code + "\trfc: " + rfcname + "\tcldr: " + translation);
			}
		}
		logln("Total " + type + ":\t" + count);
	}
}

/*    private static final int
HELP1 = 0,
HELP2 = 1,
SOURCEDIR = 2,
DESTDIR = 3,
MATCH = 4,
SKIP = 5,
TZADIR = 6,
NONVALIDATING = 7,
SHOW_DTD = 8,
TRANSLIT = 9;
options[SOURCEDIR].value

private static final UOption[] options = {
		UOption.HELP_H(),
		UOption.HELP_QUESTION_MARK(),
		UOption.SOURCEDIR().setDefault("C:\\ICU4C\\locale\\common\\main\\"),
		UOption.DESTDIR().setDefault("C:\\DATA\\GEN\\cldr\\mainCheck\\"),
		UOption.create("match", 'm', UOption.REQUIRES_ARG).setDefault(".*"),
		UOption.create("skip", 'z', UOption.REQUIRES_ARG).setDefault("zh_(C|S|HK|M).*"),
		UOption.create("tzadir", 't', UOption.REQUIRES_ARG).setDefault("C:\\ICU4J\\icu4j\\src\\com\\ibm\\icu\\dev\\tool\\cldr\\"),
		UOption.create("nonvalidating", 'n', UOption.NO_ARG),
		UOption.create("dtd", 'w', UOption.NO_ARG),
		UOption.create("transliterate", 'y', UOption.NO_ARG), };

private static String timeZoneAliasDir = null;
* /

public static void main(String[] args) throws SAXException, IOException {
	UOption.parseArgs(args, options);
	localeList = getMatchingXMLFiles(options[SOURCEDIR].value, options[MATCH].value);
	/*
    log = BagFormatter.openUTF8Writer(options[DESTDIR].value, "log.txt");
    try {
    	for (Iterator it = getMatchingXMLFiles(options[SOURCEDIR].value, options[MATCH].value).iterator(); it.hasNext();) {
    		String name = (String) it.next();
    		for (int i = 0; i <= 1; ++i) {
    			boolean resolved = i == 1;
        		CLDRKey key = make(name, resolved);
        		
        		PrintWriter pw = BagFormatter.openUTF8Writer(options[DESTDIR].value, name + (resolved ? "_r" : "") + ".txt");
				write(pw, key);
    	        pw.close();
    	        
    		}
    	}
    } finally {
    	log.close();
    	System.out.println("Done");
    }
    */
