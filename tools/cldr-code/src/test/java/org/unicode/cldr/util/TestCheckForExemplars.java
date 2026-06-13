package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.concurrent.NotThreadSafe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
        setLocaleId("my");
        CheckStatus cs = assertHadSingleCheck(value, ZAWGYI_SUBTYPE, BASIC_PATH);
        if (cs != null) {
            // check the message
            assertTrue(
                    cs.getMessage().contains("Zawgyi") && cs.getMessage().contains("confidence"),
                    () -> "Unexpected message:" + cs.getMessage());
        }
    }

    private CheckStatus assertHadSingleCheck(
            final String value, final Subtype expectedSubtype, final String path) {
        Optional<CheckStatus> csOptional = assertHadCheck(value, expectedSubtype, path);
        CheckStatus cs = csOptional.get();
        return cs;
    }

    private void setLocaleId(final String localeId) {
        cfe.setCldrFileToCheck(factory.make(localeId, true), options, possibleErrors);
        assertTrue(possibleErrors.isEmpty(), () -> possibleErrors.toString());
    }

    @Test
    void TestHindiExemplars() {
        final String UM_XPath = "//ldml/localeDisplayNames/territories/territory[@type=\"UM\"]";
        final String UM_hi = "यू॰एस॰ आउटलाइंग द्वीपसमूह";
        setLocaleId("hi");
        List<CheckStatus> checks = runChecks(UM_hi, UM_XPath);

        // should not fail
        assertTrue(checks.isEmpty(), () -> checks.toString());
    }

    /**
     * Check that the specified Check is triggered.
     *
     * @return an example message for validation
     */
    private Optional<CheckStatus> assertHadCheck(
            final String value, final Subtype expectedSubtype, final String path) {
        final Stream<CheckStatus> haveExpected = runChecks(value, expectedSubtype, path);
        // the first such error
        final Optional<CheckStatus> first = haveExpected.findFirst();
        assertNotNull(
                first.get(),
                () ->
                        String.format(
                                "Did not get check %s: %s, but had %s",
                                expectedSubtype, value, possibleErrors.toString()));
        return first;
    }

    private Stream<CheckStatus> runChecks(
            final String value, final Subtype expectedSubtype, final String path) {
        // stream of errors in the expected subtype
        Stream<CheckStatus> errorStream = runChecks(value, path).stream();
        if (expectedSubtype == null) return errorStream;
        final Stream<CheckStatus> haveExpected =
                errorStream.filter(e -> e.getSubtype() == expectedSubtype);
        return haveExpected;
    }

    private List<CheckStatus> runChecks(final String value, final String path) {
        cfe.check(path, path, value, options, possibleErrors);
        return possibleErrors;
    }
}
