package org.unicode.cldr.tool;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;

/**
 * Generates information used for some internal formats. Internal Use.
 */
public class GenerateKaraList {
	/**
	 * Generates information used for some internal formats. Internal Use.
	 */
	public static void main(String[] args) throws IOException {
		cldrFactory = CLDRFile.Factory.make(CldrUtility.COMMON_DIRECTORY, ".*");
		english = cldrFactory.make("en", true);
		PrintWriter log = BagFormatter.openUTF8Writer(CldrUtility.GEN_DIRECTORY + "main/", "karaList.xml");
		Set locales = LanguageTagParser.getLanguageScript(cldrFactory.getAvailable());
		// hack for now
		locales.remove("sr");
		locales.remove("zh");
		StandardCodes codes = StandardCodes.make();
		log.println("<root>");
		printCodes(log, locales, codes.getAvailableCodes("language"), CLDRFile.LANGUAGE_NAME);
		printCodes(log, locales, codes.getAvailableCodes("territory"), CLDRFile.TERRITORY_NAME);
		printCodes(log, locales, codes.getAvailableCodes("currency"), CLDRFile.CURRENCY_NAME);
		//printCodes(log, locales, codes.getAvailableCodes("script"), "//ldml/localeDisplayNames/scripts/script", "script");
		log.println("</root>");
		log.close();
		System.out.println("Done");
	}
	
	static CLDRFile english;
	static Factory cldrFactory;
	
	/*
	static final int LANGUAGE_NAME = 0, SCRIPT_NAME = 1, TERRITORY_NAME = 2, CURRENCY_NAME = 3, CURRENCY_SYMBOL = 4;
	static final String[][] NameTable = {
			{"//ldml/localeDisplayNames/languages/language[@type=\"", "\"]", "language"},
			{"//ldml/localeDisplayNames/scripts/script[@type=\"", "\"]", "script"},
			{"//ldml/localeDisplayNames/territories/territory[@type=\"", "\"]", "territory"},
			{"//ldml/numbers/currencies/currency[@type=\"", "\"]/displayName", "currency"},
			{"//ldml/numbers/currencies/currency[@type=\"", "\"]/symbol", "currency-symbol"}
	};
	
	private static String getName(CLDRFile english, int choice, String type, boolean skipDraft) {
		Value v = english.getValue(NameTable[choice][0] + type + NameTable[choice][1]);
		if (v == null || v.isDraft()) return null;
		return v.getStringValue();
	}
	*/
	
/*
<entry>
	<hdterm>Deutsche Mark</hdterm> // English name
	<hom>
		<epos>n</epos> //  this is the part of speech value. It is fixed.
		<sense>
			<eabbr>DEM</eabbr> //  English abbreviation - (only applicable for the currency entries)
			<target> // one target block for each language
				<tlanguage>German</tlanguage> // Indicates that the following translation is in German. We do use non-ISO values but  you can populate this with the 2 character code from the top of each xml file and we will do necessary mapping afterwards.
				<trans>Deutsche Mark</trans> // German name
				<tabbr>DM</tabbr> // German abbreviation (only applicable for the currency entries)
			</target>
			<target> 
				<tlanguage>French</tlanguage> 
				<trans>deutsche mark</trans>
				<tabbr>DEM</tabbr>
			</target>

...... additional <target> blocks for each language

		</sense>
    </hom>
</entry>
*/
	/**
	 * @param log
	 * @param locales
	 * @param availableCodes
	 * @param choice TODO
	 */
	private static void printCodes(PrintWriter log, Set locales, Set availableCodes, int choice) {
		boolean hasAbbreviation = choice == CLDRFile.CURRENCY_NAME;
		//boolean skipDraft = true;
		Set errors = new HashSet();
		for (Iterator it = availableCodes.iterator(); it.hasNext();) {
			String id = (String) it.next();
			String ename = english.getName(choice, id);
			if (ename == null) ename = "[untranslated: " + id + "]";
			System.out.println(id + "\t" + ename);
			log.println("\t<entry>");
			log.println("\t\t<hdterm>" + TransliteratorUtilities.toXML.transliterate(ename) + "</hdterm>\t<!-- "
					+ TransliteratorUtilities.toXML.transliterate(english.getNameName(choice)) + ": " + id + " -->"); // English name
			log.println("\t\t<hom>");
			log.println("\t\t\t<epos>n</epos>"); //  this is the part of speech value. It is fixed.
			log.println("\t\t\t<sense>");
			if (hasAbbreviation) {  // only applicable for the currency entries
				String aename = english.getName(CLDRFile.CURRENCY_SYMBOL, id);
				if (aename != null) {
					log.println("\t\t\t\t<eabbr>" + TransliteratorUtilities.toXML.transliterate(aename) + "</eabbr>");
				}
			}
			for (Iterator it2 = locales.iterator(); it2.hasNext();) {
				String locale = (String)it2.next();
				try {
					CLDRFile cldrfile = cldrFactory.make(locale, true);
					String trans = cldrfile.getName(choice, id);
					if (trans == null) continue;
					log.println("\t\t\t\t<target>");	// one target block for each language
					//String etrans = getName(english, "languages/language", locale, true);
					log.println("\t\t\t\t\t<tlanguage>" + locale + "</tlanguage>\t<!-- "
							+ TransliteratorUtilities.toXML.transliterate(english.getName(locale)) + " -->");	// We do use non-ISO values but  you can populate this with the 2 character code from the top of each xml file and we will do necessary mapping afterwards.
					log.println("\t\t\t\t\t<trans>" + TransliteratorUtilities.toXML.transliterate(trans) + "</trans>");
					if (hasAbbreviation) {
						String aename = cldrfile.getName(CLDRFile.CURRENCY_SYMBOL, id);
						if (aename != null && !aename.equals(id)) {
							log.println("\t\t\t\t\t<tabbr>" + TransliteratorUtilities.toXML.transliterate(aename) + "</tabbr>");
						}
					}
					log.println("\t\t\t\t</target>");
				} catch (RuntimeException e) {
					String s = e.getMessage();
					if (!errors.contains(s)) {
						System.out.println(s);
						errors.add(s);
					}
				}
			}
			log.println("\t\t\t</sense>");
			log.println("\t\t</hom>");
			log.println("\t</entry>");
			 // English name
			//if (id.length() == 4 && 'A' <= ch && ch <= 'Z') return getName(english, "scripts/script", id);
			//return getName(english, "territories/territory", id);
		}
		
	}
}