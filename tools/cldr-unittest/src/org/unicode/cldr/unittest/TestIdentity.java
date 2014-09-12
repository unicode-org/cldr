package org.unicode.cldr.unittest;

import java.util.HashSet;
import java.util.Set;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagCanonicalizer;
import org.unicode.cldr.util.LanguageTagParser;

import com.ibm.icu.dev.test.TestFmwk;

public class TestIdentity extends TestFmwk {
	static TestInfo testInfo = TestInfo.getInstance();

	public static void main(String[] args) {
		new TestIdentity().run(args);
	}

	public void TestIdentityVsFilename() {

		LanguageTagParser ltp = new LanguageTagParser();
		LanguageTagCanonicalizer ltc = new LanguageTagCanonicalizer();

		Factory[] factories = { testInfo.getFullCldrFactory(),
				testInfo.getExemplarsFactory(), testInfo.getCollationFactory(),
				testInfo.getRBNFFactory() };

		for (Factory factory : factories) {
			for (String locale : factory.getAvailable()) {
				String canonicalLocaleID = ltc.transform(locale);
				ltp.set(locale);
				String fLanguage = ltp.getLanguage();
				String fScript = ltp.getScript().length() > 0 ? ltp.getScript()
						: "<missing>";
				String fTerritory = ltp.getRegion().length() > 0 ? ltp
						.getRegion() : "<missing>";
				Set<String> fVariants = new HashSet<String>(ltp.getVariants());
				CLDRFile localeData = factory.make(locale, false);
				String identity = localeData.getLocaleIDFromIdentity();
				ltp.set(identity);
				String iLanguage = ltp.getLanguage();
				if (!fLanguage.equals(iLanguage)) {
					errln("Language code for locale \""
							+ locale
							+ "\" does not match the identity section."
							+ "\n\tLocated in : "
							+ factory.getSourceDirectoryForLocale(locale)
									.getPath() + "\n\tValue in file name is: "
							+ fLanguage + "\n\tValue in identity section is: "
							+ iLanguage);
				}
				String iScript = ltp.getScript().length() > 0 ? ltp.getScript()
						: "<missing>";
				if (!fScript.equals(iScript)) {
					errln("Script code for locale \""
							+ locale
							+ "\" does not match the identity section."
							+ "\n\tLocated in : "
							+ factory.getSourceDirectoryForLocale(locale)
									.getPath() + "\n\tValue in file name is: "
							+ fScript + "\n\tValue in identity section is: "
							+ iScript);
				}
				String iTerritory = ltp.getRegion().length() > 0 ? ltp
						.getRegion() : "<missing>";
				if (!fTerritory.equals(iTerritory)) {
					errln("Territory code for locale \""
							+ locale
							+ "\" does not match the identity section."
							+ "\n\tLocated in : "
							+ factory.getSourceDirectoryForLocale(locale)
									.getPath() + "\n\tValue in file name is: "
							+ fTerritory + "\n\tValue in identity section is: "
							+ iTerritory);
				}
				Set<String> iVariants = new HashSet<String>(ltp.getVariants());
				if (!fVariants.equals(iVariants)) {
					errln("Variants for locale \""
							+ locale
							+ "\" do not match the identity section."
							+ "\n\tLocated in : "
							+ factory.getSourceDirectoryForLocale(locale)
									.getPath() + "\n\tValue in file name is: "
							+ fVariants.toString()
							+ "\n\tValue in identity section is: "
							+ iVariants.toString());
				}
				if (canonicalLocaleID != null) {
					ltp.set(canonicalLocaleID);
					String canonicalLanguage = ltp.getLanguage();
					if (!fLanguage.equals(canonicalLanguage)) {
						errln("Locale \""
								+ locale
								+ "\" uses a non-canonical language tag: "
								+ fLanguage
								+ "\n\tLocated in : "
								+ factory.getSourceDirectoryForLocale(locale)
										.getPath()
								+ "\n\tCanonical form would be : "
								+ canonicalLanguage);
					}
				}
			}
		}
	}
}
