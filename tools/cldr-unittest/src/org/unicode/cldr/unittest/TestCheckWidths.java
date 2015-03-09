package org.unicode.cldr.unittest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Type;
import org.unicode.cldr.test.CheckWidths;
import org.unicode.cldr.unittest.TestXMLSource.DummyXMLSource;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.XMLSource;

public class TestCheckWidths extends TestFmwkPlus {
    public static void main(String[] args) {
        new TestCheckWidths().run(args);
    }

    public void TestBasic() {
        CheckWidths x = new CheckWidths();
        XMLSource xmlSource = new DummyXMLSource();
        final String path = "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"short\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"1000\"][@count=\"one\"]";
        final String value = "0 xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        xmlSource.putValueAtDPath(path, value);
        xmlSource.setLocaleID("und");
        CLDRFile cldrFileToCheck = new CLDRFile(xmlSource);
        Map<String, String> options = Collections.EMPTY_MAP;
        List<CheckStatus> possibleErrors = new ArrayList<>();
        x.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        assertEquals("setCldrFileToCheck", Collections.EMPTY_LIST,
            possibleErrors);
        possibleErrors.clear();
        x.check(path, path, value, options, possibleErrors);
        if (assertNotEquals("path", Collections.EMPTY_LIST, possibleErrors)) {
            CheckStatus err = possibleErrors.get(0);
            assertEquals("errorType", Type.Error, err.getType());
            Subtype exp = err.getSubtype();
            assertEquals("errorSubType", Subtype.valueTooWide, err.getSubtype());
        }
    }
}
