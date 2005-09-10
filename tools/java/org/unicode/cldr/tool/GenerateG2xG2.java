package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.dev.test.util.ArrayComparator;
import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class GenerateG2xG2 {
	static CLDRFile english;
	static CLDRFile root;
	
	public static void main(String[] args) throws IOException {
		String sourceLanguage = "G5";
		String targetLanguage = "G5";
		Factory cldrFactory = Factory.make(Utility.MAIN_DIRECTORY, ".*");
		english = cldrFactory.make("en", true);
		root = cldrFactory.make("root", true);
		StandardCodes sc = StandardCodes.make();
		Map type_code_value = sc.getLocaleTypes();
		Set sourceSet = new TreeSet();
		Set targetLanguageSet = new TreeSet();
		targetLanguageSet.add("no");
		addPriority("G2","nn");
		addPriority("G2","no");
		targetLanguageSet.add("nn");
		Set targetScriptSet = new TreeSet();
		Set targetRegionSet = new TreeSet();
		Set targetTZSet = new TreeSet();
		Set targetCurrencySet = new TreeSet();
		for (Iterator it = type_code_value.keySet().iterator(); it.hasNext();) {
			Object type = it.next();
			Map code_value = (Map) type_code_value.get(type);
			if (!type.equals("IBM")) continue;
			for (Iterator it2 = code_value.keySet().iterator(); it2.hasNext();) {
				String locale = (String)it2.next();
				if (locale.equals("no")) continue;
				String priority = (String) code_value.get(locale);
				ULocale ulocale = new ULocale(locale);
				String language = ulocale.getLanguage();
				String script = ulocale.getScript();
				String territory = ulocale.getCountry();
				if (sourceLanguage.compareTo(priority) >= 0) {
					if (language.equals("no")) language = "nn";
					locale = new ULocale(language, script).toString();
					sourceSet.add(locale);
					addPriority(priority, locale);
				}
				if (targetLanguage.compareTo(priority) >= 0) {
					targetLanguageSet.add(language);
					targetScriptSet.add(script);
					targetRegionSet.add(territory);
					addPriority(priority, language);
					addPriority(priority, script);
					addPriority(priority, territory);
				}
			}
		}
		// fill in the currencies, and TZs for the countries that have multiple zones
		Map c2z = sc.getCountryToZoneSet();
		for (Iterator it = targetRegionSet.iterator(); it.hasNext();) {
			String country = (String)it.next();
			String priority = (String)priorityMap.get(country);
			for (Iterator it2 = getCurrency(country).iterator(); it2.hasNext();) {
				String currency = (String)it2.next();
				targetCurrencySet.add(currency);
				addPriority(priority, currency);
			}
			Set s = (Set) c2z.get(country);
			if (s.size() == 1) continue;
			for (Iterator it2 = s.iterator(); it2.hasNext();) {
				String tzid = (String)it2.next();
				targetTZSet.add(tzid);
				addPriority(priority, tzid);
			}
		}
		// print out missing translations.
		PrintWriter pw = BagFormatter.openUTF8Writer("c:/", "G2xG2.txt");
		// show priorities
		Comparator comp = new UTF16.StringComparator();
		Set priority_set = new TreeSet(new ArrayComparator(new Comparator[]{comp, comp, comp}));
		for (Iterator it = priorityMap.keySet().iterator(); it.hasNext();) {
			String code = (String)it.next();
			String priority = (String)priorityMap.get(code);
			int type = getType((String)code);
			priority_set.add(new String[]{priority, type+"", code});
		}
		String lastPriority = "";
		String lastType = "";
		for (Iterator it = priority_set.iterator(); it.hasNext();) {
			String[] items = (String[])it.next();
			if (!lastPriority.equals(items[0])) {
				lastPriority = items[0];
				pw.println();
				pw.println(lastPriority);
			}
			pw.println("\t" + items[2] + "\t(" + getItemName(english, items[2]) + ")");
		}
		pw.flush();
		// print out missing translations.
		for (Iterator it = sourceSet.iterator(); it.hasNext();) {
			String sourceLocale = (String)it.next();
			System.out.print(sourceLocale + ", ");
			CLDRFile sourceData = cldrFactory.make(sourceLocale, true);
			pw.println();
			String title = sourceLocale;
			checkItems(pw, title, sourceData, sourceData.LANGUAGE_NAME, targetLanguageSet);
			checkItems(pw, title, sourceData, sourceData.SCRIPT_NAME, targetScriptSet);
			checkItems(pw, title, sourceData, sourceData.TERRITORY_NAME, targetRegionSet);
			checkItems(pw, title, sourceData, sourceData.CURRENCY_NAME, targetCurrencySet);
			// only check timezones if exemplar characters don't include a-z
			String v = sourceData.getStringValue("/ldml/characters/exemplarCharacters");
			UnicodeSet exemplars = new UnicodeSet(v);
			if (exemplars.contains('a', 'z')) continue;
			checkItems(pw, title, sourceData, sourceData.TZID, targetTZSet);
		}
		pw.println();
		pw.println("Sizes");
		pw.println();
		int runningTotalCount = 0;
		int runningMissingCount = 0;
		NumberFormat percent = NumberFormat.getPercentInstance();
		percent.setMinimumFractionDigits(1);
		NumberFormat nf = NumberFormat.getInstance();
		nf.setGroupingUsed(true);
		nf.setMinimumFractionDigits(0);
		for (Iterator it = totalMap.keySet().iterator(); it.hasNext();) {
			Object key = it.next();
			Totals t = (Totals) totalMap.get(key);
			runningTotalCount += t.totalCount;
			runningMissingCount += t.missingCount;
			pw.println(key + ":\t" + runningMissingCount 
					+ "\t" + runningTotalCount 
					+ "\t" + percent.format(runningMissingCount/(0.0+runningTotalCount)));
		}
		pw.close();
		System.out.println();
		System.out.println("Done");
	}

	static Map priorityMap = new TreeMap();
	static void addPriority(String priority, String code) {
		if (code.length() == 0) return;
		Object oldPriority = priorityMap.get(code);
		if (oldPriority == null || priority.compareTo(oldPriority) < 0) priorityMap.put(code, priority);
	}
	
	static class Totals {
		int totalCount;
		int missingCount;
	}
	static Map totalMap = new TreeMap();
	
	static void checkItems(PrintWriter pw, String sourceLocale, CLDRFile sourceData, int type, Set targetItemSet) {
		for (Iterator it2 = targetItemSet.iterator(); it2.hasNext();) {
			String item = (String)it2.next();
			if (item.length() == 0) continue;
			String key = priorityMap.get(sourceLocale) + "" + priorityMap.get(item);
			Totals t = (Totals) totalMap.get(key);
			if (t == null) totalMap.put(key, t = new Totals());
			t.totalCount++;
			String translation = getItemName(sourceData, type, item);
			String rootName = getItemName(root, type, item);
			if (rootName.equals(translation)) {
				t.missingCount++;
				pw.println(priorityMap.get(sourceLocale)
						+ "\t" + sourceLocale + 
						"\t(" + english.getName(sourceLocale, false) + ": "
						+ sourceData.getName(sourceLocale, false) + ")" 
						+ "\t" + priorityMap.get(item)
						+ "\t" + item 
						+ "\t(" + getItemName(english, type, item) + ")");
			}
		}
	}
	private static String getItemName(CLDRFile data, String item) {
		return getItemName(data, getType(item), item);
	}
	private static int getType(String item) {
		int type = CLDRFile.LANGUAGE_NAME;
		if (item.indexOf('/') >= 0) type = CLDRFile.TZID; // America/Los_Angeles
		else if (item.length() == 4) type = CLDRFile.SCRIPT_NAME; // Hant
		else if (item.charAt(0) <= '9') type = CLDRFile.TERRITORY_NAME; // 001
		else if (item.charAt(0) < 'a') {
			if (item.length() == 3) type = CLDRFile.CURRENCY_NAME;
			else type = CLDRFile.TERRITORY_NAME; // US or USD
		}
		return type;
	}
	private static String getItemName(CLDRFile data, int type, String item) {
		String result;
		if (type == data.LANGUAGE_NAME) {
			result = data.getName(item, false);
		} else if (type != data.TZID) {
			result = data.getName(type, item, false);
		} else {
			String prefix = "/ldml/dates/timeZoneNames/zone[@type=\"" + item + "\"]/exemplarCity";
			result = data.getStringValue(prefix);
		}
		return result == null ? item : result;
	}
	static Map territory_currency = null;
	
	private static Collection getCurrency(String territory) {
		if (territory_currency == null) {
			territory_currency = new TreeMap();
			Factory cldrFactory = Factory.make(Utility.MAIN_DIRECTORY, ".*");
			CLDRFile supp = cldrFactory.make(CLDRFile.SUPPLEMENTAL_NAME, false);
			XPathParts parts = new XPathParts(new UTF16.StringComparator(), null);
			for (Iterator it = supp.keySet().iterator(); it.hasNext();) {
				String path = (String) it.next();
				if (path.indexOf("/currencyData") >= 0) {
					//<region iso3166="AR">
					//	<currency iso4217="ARS" from="1992-01-01"/>
					if (path.indexOf("/region") >= 0) {
						parts.set(supp.getFullXPath(path));
						Map attributes = parts.getAttributes(parts.size() - 2);
						String iso3166 = (String)attributes.get("iso3166");
						attributes = parts.getAttributes(parts.size() - 1);						
						String iso4217 = (String) attributes.get("iso4217");
						String to = (String) attributes.get("to");
						if (to != null) continue;
						Collection info = (Collection) territory_currency.get(iso3166);
						if (info == null) territory_currency.put(iso3166, info = new ArrayList());
						info.add(iso4217);
						System.out.println(iso3166 + " => " + iso4217);
					}
				}			
			}
		}
		return (Collection)territory_currency.get(territory); 
	}
}