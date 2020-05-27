package org.unicode.cldr.unittest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Type;
import org.unicode.cldr.test.CheckNumbers;
import org.unicode.cldr.unittest.TestXMLSource.DummyXMLSource;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.XMLSource;

public class TestCheckNumbers extends TestFmwkPlus {


    public static void main(String[] args) {
        new TestCheckNumbers().run(args);
    }

    public void TestSingularity() {
        CheckNumbers checkNumbers = new CheckNumbers(CLDRConfig.getInstance().getCldrFactory());

        // construct fake locale and test

        // should succeed, since "one" only has one number in ast.
        checkSingularity(checkNumbers, "en",
            "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"short\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"1000\"][@count=\"one\"]", 
            "K", null
            );
        // should fail, "one" may match both 1 and zero in french
        checkSingularity(checkNumbers, "fr",
            "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"short\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"1000\"][@count=\"one\"]", 
            "K", Subtype.missingZeros
            );

    }

    public void checkSingularity(CheckNumbers checkNumbers, String locale, final String path, final String value, Subtype expectedSubtype) {
        XMLSource xmlSource = new DummyXMLSource();
        xmlSource.putValueAtDPath(path, value);
        xmlSource.setLocaleID(locale);
        CLDRFile cldrFileToCheck = new CLDRFile(xmlSource);

        Map<String, String> options = Collections.emptyMap();
        List<CheckStatus> possibleErrors = new ArrayList<>();
        checkNumbers.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        assertEquals("setCldrFileToCheck", Collections.EMPTY_LIST, possibleErrors);
        possibleErrors.clear();

        checkNumbers.check(path, path, value, options, possibleErrors);
        if (expectedSubtype == null) {
            assertEquals("should have no errors: ", Collections.emptyList(), possibleErrors);
        } else {
            if (assertEquals("should have one error: ", 1, possibleErrors.size())) {
                CheckStatus err = possibleErrors.get(0);
                assertEquals("errorType", Type.Error, err.getType());
                Subtype exp = err.getSubtype();
                assertEquals("errorSubType", expectedSubtype, err.getSubtype());
            }
        }
    }
}
