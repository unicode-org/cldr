/*
 * Created on Jan 27, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.unicode.cldr.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.dev.test.util.BagFormatter;

/**
 * @author davis
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TestUtilities {
	public static void main(String[] args) throws IOException {
		printZoneSamples();
		//printCurrencies();
	}

	/**
	 * 
	 */
	private static void printCurrencies() {
		StandardCodes sc = StandardCodes.make();
		Set s = sc.getAvailableCodes("currency");
		for (Iterator it = s.iterator(); it.hasNext();) {
			String code = (String)it.next();
			String name = sc.getData("currency", code);
			List data = sc.getFullData("currency", code);
			System.out.println(code + "\t" + name + "\t" + data);
		}
	}

	/**
	 * @throws IOException
	 * 
	 */
	private static void printZoneSamples() throws IOException {
		String[] locales = {
				"en",
				"bg",
				"hi",
				"as" // picked deliberately because it has few itesm
		};
		String[] zones = {
				"America/Los_Angeles",
				"America/Argentina/Buenos_Aires",
				"America/Buenos_Aires",
				"America/Havana",
				"Australia/ACT",
				"Australia/Sydney",
		};
		String[][] fields = {
				{"daylight", "z", "zzzz"},
				{"standard", "z", "zzzz", "Z", "ZZZZ", "v", "vvvv"}
		};
    	Factory mainCldrFactory = Factory.make(Utility.COMMON_DIRECTORY + "main" + File.separator, ".*");
    	PrintWriter out = BagFormatter.openUTF8Writer(Utility.GEN_DIRECTORY, "timezone_samples.txt");

		for (int i = 0; i < locales.length; ++i) {
			String locale = locales[i];
			TimezoneFormatter tzf = new TimezoneFormatter(mainCldrFactory, locale);
			for (int j = 0; j < zones.length; ++j) {
				String zone = zones[j];
				for (int k = 0; k < fields.length; ++k) {
					String type = fields[k][0];
					boolean daylight = type.equals("daylight");
					for (int m = 1; m < fields[k].length; ++m) {
						String field = fields[k][m];
						String formatted = tzf.getFormattedZone(zone, field, daylight);
						out.println(locale
							+ "\t" + zone
							+ "\t" + type
							+ "\t" + field
							+ "\t" + formatted
						);
					}
				}
			}
		}
		out.close();
	}
}
