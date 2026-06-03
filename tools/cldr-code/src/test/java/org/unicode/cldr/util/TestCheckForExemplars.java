package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.concurrent.NotThreadSafe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.test.CheckCLDR.Options;
import org.unicode.cldr.test.CheckForExemplars;
import org.unicode.cldr.unittest.TestDisplayAndInputProcessor;

@TestInstance(Lifecycle.PER_CLASS)
@NotThreadSafe
public class TestCheckForExemplars {
    List<CheckStatus> possibleErrors = new ArrayList<>();
    Options options = new Options();

    Factory factory = null;
    CheckForExemplars cfe = null;
    static final String BASIC_PATH =
            "//ldml/localeDisplayNames/types/type[@type=\"hant\"][@key=\"numbers\"]";
    static final Subtype ZAWGYI_SUBTYPE = Subtype.misencodedZawgyi;

    @BeforeAll
    public void setUp() {
        factory = CLDRConfig.getInstance().getCldrFactory();
        cfe = new CheckForExemplars(factory);
        cfe.setEnglishFile(CLDRConfig.getInstance().getEnglish());
    }

    @BeforeEach
    public void clear() {
        possibleErrors.clear();
    }

    @ParameterizedTest
    @ValueSource(
            // strings that will trigger the warning
            strings = {
                TestDisplayAndInputProcessor.zawgyi_z_mi,
                "\u1031\u1019\u102c\u1004\u1039\u1038\u101b\u102e\u0020\u0028\u1014"
                        + "\u101A\u1030\u1038\u1007\u102E\u101C\u1014\u1039\u1000\u107D\u103C\u1014\u1039\u1038\u101B\u103D\u102D",
                "ABCDE " + TestDisplayAndInputProcessor.zawgyi_z_mi + "XYZ",
                "\u1000\u1005\u102C\u1038\u101E\u1019\u102C\u1038",
                "\u1021\u101E\u1004\u1039\u1038\u1019\u103D",
                "\u1031\u1040\u1037",
                "\u1041\u1040\u1037",
            })
    void TestZawgyiWarning(final String value) {
        cfe.setCldrFileToCheck(factory.make("my", true), options, possibleErrors);
        assertTrue(possibleErrors.isEmpty(), () -> possibleErrors.toString());
        assertHadCheck(value, ZAWGYI_SUBTYPE, BASIC_PATH);
    }

    private void assertHadCheck(
            final String value, final Subtype expectedSubtype, final String path) {
        cfe.check(path, path, value, options, possibleErrors);
        assertTrue(
                possibleErrors.stream().anyMatch(e -> e.getSubtype() == expectedSubtype),
                () ->
                        String.format(
                                "Did not get check %s: %s, but had %s",
                                expectedSubtype, value, possibleErrors.toString()));
    }
}
