package org.unicode.cldr.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.CLDRFile.Value;
import org.xml.sax.SAXParseException;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.impl.Utility;

public class Misc {
	public static void main(String[] args) throws IOException {
			printSupplementalData();
	}
	
	private static void printSupplementalData() {
		Factory cldrFactory = Factory.make("C:\\ICU4C\\locale\\common\\main\\", ".*", null);
		CLDRFile supp = cldrFactory.make("supplementalData", false);
		XPathParts parts = new XPathParts(null);
		for (Iterator it = supp.keySet().iterator(); it.hasNext();) {
			String path = (String) it.next();
			Value v = supp.getValue(path);
			parts.set(v.getFullXPath());
			Map m = parts.findAttributes("language");
			if (m == null) continue;
			System.out.println("Type: " + m.get("type") 
					+ "\tscripts: " + m.get("scripts")
					+ "\tterritories: " + m.get("territories")
					);
		}
	}
	
	private static void compareLists() throws IOException {
		BufferedReader in = BagFormatter.openUTF8Reader("", "language_list.txt");
		String[] pieces = new String[4];
		Factory cldrFactory = Factory.make("C:\\ICU4C\\locale\\common\\main\\", ".*", null);
		//CLDRKey.main(new String[]{"-mde.*"});
		Set locales = cldrFactory.getAvailable();
		Set cldr = new TreeSet();
		LocaleIDParser parser = new LocaleIDParser();
		for (Iterator it = locales.iterator(); it.hasNext();) {
			// if doesn't have exactly one _, skip
			String locale = (String)it.next();
			parser.set(locale);
			if (parser.getScript().length() == 0 && parser.getRegion().length() == 0) continue;
			if (parser.getVariants().length > 0) continue;
			cldr.add(locale.replace('_', '-'));
		}
		
		Set tex = new TreeSet();
		while (true) {
			String line = in.readLine();
			if (line == null) break;
			line = line.trim();
			if (line.length() == 0) continue;
			int p = line.indexOf(' ');
			tex.add(line.substring(0,p));
		}
		Set inCldrButNotTex = new TreeSet(cldr);
		inCldrButNotTex.removeAll(tex);
		System.out.println(" inCldrButNotTex " + inCldrButNotTex);
		Set inTexButNotCLDR = new TreeSet(tex);
		inTexButNotCLDR.removeAll(cldr);
		System.out.println(" inTexButNotCLDR " + inTexButNotCLDR);
	}
}