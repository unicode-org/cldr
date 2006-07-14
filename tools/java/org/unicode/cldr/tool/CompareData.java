package org.unicode.cldr.tool;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.PrettyPath;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.util.ULocale;

public class CompareData {
	
	private static final int
	HELP1 = 0,
	HELP2 = 1,
	SOURCEDIR = 2,
	DESTDIR = 3,
	MATCH = 4
	;
	
	private static final UOption[] options = {
		UOption.HELP_H(),
		UOption.HELP_QUESTION_MARK(),
		UOption.SOURCEDIR().setDefault(Utility.BASE_DIRECTORY ),
		UOption.DESTDIR().setDefault(Utility.BASE_DIRECTORY + "../cldr-last/"),
		UOption.create("match", 'm', UOption.REQUIRES_ARG).setDefault(".*"),
	};
	
	String[] directoryList = {"main", "collation", "segmentations"}; 
	
	static RuleBasedCollator uca = (RuleBasedCollator) Collator.getInstance(ULocale.ROOT);
	{
		uca.setNumericCollation(true);
	}
	
	static PrettyPath prettyPathMaker = new PrettyPath();
	static CLDRFile english;
	static Set locales;
	static Factory cldrFactory;
	
	public static void main(String[] args) throws Exception {
		double deltaTime = System.currentTimeMillis();
		try {
			UOption.parseArgs(args, options);
			String sourceDir = options[SOURCEDIR].value + "common/main/";
			System.out.println(new File(sourceDir).getCanonicalPath());
			String compareDir = options[DESTDIR].value + "common/main/";
			System.out.println(new File(compareDir).getCanonicalPath());
			
			cldrFactory = Factory.make(sourceDir, options[MATCH].value);
			Factory oldFactory =  Factory.make(compareDir, options[MATCH].value);
			
			locales = new TreeSet(cldrFactory.getAvailable());
			new Utility.MatcherFilter(options[MATCH].value).retainAll(locales);
			Set pathsSeen = new HashSet();
			int newItemsTotal = 0;
			int replacementItemsTotal = 0;
			int deletedItemsTotal = 0;
			int sameItemsTotal = 0;

			
			for (Iterator it = locales.iterator(); it.hasNext();) {				
				int newItems = 0;
				int replacementItems = 0;
				int deletedItems = 0;
				int sameItems = 0;
				String locale = (String) it.next();
				if (locale.startsWith("supplem") || locale.startsWith("character")) continue;
				CLDRFile file = (CLDRFile) cldrFactory.make(locale, false);
				try {
					CLDRFile oldFile = (CLDRFile) oldFactory.make(locale, false);
					pathsSeen.clear();
					for (Iterator it2 = file.iterator(); it2.hasNext();) {
						String path = (String) it2.next();
						String value = file.getStringValue(path);
						String oldValue = oldFile.getStringValue(path);
						if (oldValue == null) {
							newItems++;
						} else if (!value.equals(oldValue)) {
							replacementItems++;
						} else {
							sameItems++;
						}
						pathsSeen.add(path);
					}
					for (Iterator it2 = oldFile.iterator(); it2.hasNext();) {
						String path = (String) it2.next();
						if (!pathsSeen.contains(path)) {
							deletedItems++;
						}
					}
				} catch (Exception e) {
					newItems = size(file.iterator());
				}
				String langScript = new LocaleIDParser().set(file.getLocaleID()).getLanguageScript();
				System.out.println(langScript + "\t" + file.getLocaleID() + "\t" + sameItems + "\t" + newItems + "\t" + replacementItems + "\t" + deletedItems);
				newItemsTotal += newItems;
				replacementItemsTotal += replacementItems;
				deletedItemsTotal += deletedItems;
				sameItemsTotal += sameItems;
			}       
			System.out.println("TOTAL" + "\t" + "\t" + sameItemsTotal + "\t" + newItemsTotal + "\t" + replacementItemsTotal + "\t" + deletedItemsTotal);
		} finally{
			deltaTime = System.currentTimeMillis() - deltaTime;
			System.out.println("Elapsed: " + deltaTime/1000.0 + " seconds");
			System.out.println("Done");
		}
		
	}

	private static int size(Iterator iterator) {
		int count = 0;
		for (; iterator.hasNext();) {
			iterator.next();
			++count;
		}
		return count;
	}
}