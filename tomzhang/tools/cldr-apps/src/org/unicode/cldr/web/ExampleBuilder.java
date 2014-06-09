package org.unicode.cldr.web;

import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.test.ExampleGenerator.ExampleContext;
import org.unicode.cldr.test.ExampleGenerator.ExampleType;
import org.unicode.cldr.util.CLDRFile;

public class ExampleBuilder {
    ExampleContext ec_e, ec_n;
    ExampleGenerator eg_e, eg_n;

    public ExampleBuilder(CLDRFile englishFile, CLDRFile cldrFile) {
        eg_e = new ExampleGenerator(englishFile, englishFile, englishFile.getSupplementalDirectory().getPath());
        eg_n = new ExampleGenerator(cldrFile, englishFile, englishFile.getSupplementalDirectory().getPath());
        ec_e = new ExampleContext();
        ec_n = new ExampleContext();
    }

    synchronized String getExampleHtml(String xpath, String value, ExampleType type) {
        String s;
        if (type == ExampleType.ENGLISH) {
            s = eg_e.getExampleHtml(xpath, value, ec_e, type);
        } else {
            s = eg_n.getExampleHtml(xpath, value, ec_n, type);
        }
        // if(SurveyMain.isUnofficial) System.err.println(s + "  = geh + " +
        // xpath + ", " + value + ", " + zoomed + ", (ec)," + type);
        return s;
    }

    public String getHelpHtml(String xpath, String value) {
        return eg_e.getHelpHtml(xpath, value, true);
    }
}
