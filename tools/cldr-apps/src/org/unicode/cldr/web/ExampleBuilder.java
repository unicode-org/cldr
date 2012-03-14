package org.unicode.cldr.web;

import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.test.ExampleGenerator.ExampleContext;
import org.unicode.cldr.test.ExampleGenerator.ExampleType;
import org.unicode.cldr.test.ExampleGenerator.Zoomed;
import org.unicode.cldr.util.CLDRFile;

public class ExampleBuilder {
	ExampleContext ec;
	ExampleGenerator eg;
	public ExampleBuilder(CLDRFile englishFile, CLDRFile cldrFile)  {
		eg = new ExampleGenerator(englishFile, cldrFile, englishFile.getSupplementalDirectory().getPath());
		ec = new ExampleContext();
	}
	
	synchronized String getExampleHtml(String xpath, String value, Zoomed zoomed, ExampleType type) {
		return eg.getExampleHtml(xpath, value, zoomed, ec, type);
	}
}
