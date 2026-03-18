package org.unicode.cldr.unittest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Type;
import org.unicode.cldr.test.CheckNumbers;
import org.unicode.cldr.unittest.TestXMLSource.DummyXMLSource;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XMLSource;

public class TestCheckNumbers extends TestFmwkPlus {

    static CLDRConfig testInfo = CLDRConfig.getInstance();

    public static void main(String[] args) {
        new TestCheckNumbers().run(args);
    }

    public void checkSingularity(
            CheckNumbers checkNumbers,
            String locale,
            final String path,
            final String value,
            Subtype expectedSubtype) {
        XMLSource xmlSource = new DummyXMLSource();
        xmlSource.putValueAtDPath(path, value);
        xmlSource.setLocaleID(locale);
        CLDRFile cldrFileToCheck = new CLDRFile(xmlSource);

        final CheckCLDR.Options options = new CheckCLDR.Options(Collections.emptyMap());
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

    public void TestDefaultNumberingSystemInArAndChildren() {
        Factory cldrFactory = testInfo.getCldrFactory();
        SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
        Set<String> defaultContentLocales = sdi.getDefaultContentLocales();
        for (String locale : cldrFactory.getAvailable()) {
            if (locale.equals("ar")
                    || (locale.startsWith("ar_") && !defaultContentLocales.contains(locale))) {
                CLDRFile cldrFile = cldrFactory.make(locale, false);
                String defaultNumberSys =
                        cldrFile.getStringValue("//ldml/numbers/defaultNumberingSystem");
                if (defaultNumberSys == null) {
                    errln(
                            "Missing explicit defaultNumberingSystem entry in "
                                    + locale
                                    + ", see other ar_*");
                }
            }
        }
    }
}
