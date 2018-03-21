package org.unicode.cldr.unittest;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.unicode.cldr.tool.VerifyAttributeValues;
import org.unicode.cldr.tool.VerifyAttributeValues.Errors;
import org.unicode.cldr.util.AttributeValueValidity;
import org.unicode.cldr.util.AttributeValueValidity.AttributeValueSpec;
import org.unicode.cldr.util.AttributeValueValidity.MatcherPattern;
import org.unicode.cldr.util.AttributeValueValidity.Status;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.LanguageInfo;
import org.unicode.cldr.util.SupplementalDataInfo.AttributeValidityInfo;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.util.Output;

public class TestAttributeValues extends TestFmwk {
    private static final File BASE_DIR = new File(CLDRPaths.BASE_DIRECTORY);
    public static final Joiner SPACE_JOINER = Joiner.on(' ');
    public static final Splitter SPACE_SPLITTER = Splitter.on(' ').trimResults().omitEmptyStrings();
    static final Splitter SEMI_SPACE = Splitter.on(';').trimResults().omitEmptyStrings();
    private static final CLDRConfig config = CLDRConfig.getInstance();

    public static void main(String[] args) {
        new TestAttributeValues().run(args);
    }

    public void TestA() {
        MatcherPattern mp = AttributeValueValidity.getMatcherPattern("$language");
        for (String language : LanguageInfo.getAvailable()) {
            if (mp.matches(language, null)) {
                LanguageInfo languageInfo = LanguageInfo.get(language);
                show(language, languageInfo);
            }
        }
    }

    private void show(String language, LanguageInfo languageInfo) {
        logln(language
            + "\t" + config.getEnglish().getName(CLDRFile.LANGUAGE_NAME, language)
            + "\t" + languageInfo);
    }

    public void TestAttributeValueValidity() {
        for (String test : Arrays.asList(
            "supplementalData;     territoryAlias;     replacement;    AA")) {
            quickTest(test);
        }
    }

    private Status quickTest(String test) {
        List<String> parts = SEMI_SPACE.splitToList(test);
        Output<String> reason = new Output<>();
        Status value = AttributeValueValidity.check(DtdData.getInstance(DtdType.valueOf(parts.get(0))), parts.get(1), parts.get(2), parts.get(3), reason);
        if (value != Status.ok) {
            errln(test + "\t" + value + "\t" + reason);
        }
        return value;
    }

    public void TestSingleFile() {
        Errors errors = new Errors();
        Set<AttributeValueSpec> missing = new TreeSet<>();
        VerifyAttributeValues.check(CLDRPaths.MAIN_DIRECTORY + "en.xml", errors, missing);
        for (AttributeValueSpec entry1 : missing) {
            errln("Missing Tests: " + entry1);
        }
        for (R3<String, AttributeValueSpec, String> item : errors.getRows()) {
            errln(item.get0() + "; \t" + item.get2() + "; \t" + item.get1());
        }
    }

    public void TestCoreValidity() {
        int maxPerDirectory = getInclusion() <= 5 ? 20 : Integer.MAX_VALUE;
        Matcher fileMatcher = null;
        Set<AttributeValueSpec> missing = new LinkedHashSet<>();
        Errors errors = new Errors();
        VerifyAttributeValues.findAttributeValues(BASE_DIR, maxPerDirectory, fileMatcher, errors, missing, isVerbose() ? getErrorLogPrintWriter() : null);

        int count = 0;
        for (Entry<AttributeValidityInfo, String> entry : AttributeValueValidity.getReadFailures().entrySet()) {
            errln("Read error: " + ++count + "\t" + entry.getKey() + " => " + entry.getValue());
        }

        count = 0;
        for (R3<DtdType, String, String> entry1 : AttributeValueValidity.getTodoTests()) {
            warnln("Unfinished Test: " + ++count + "\t" + new AttributeValueSpec(entry1.get0(), entry1.get1(), entry1.get2(), "").toString());
        }

        count = 0;
        for (AttributeValueSpec entry1 : missing) {
            errln("Missing Test: " + entry1);
        }

        count = 0;
        for (R3<String, AttributeValueSpec, String> item : errors.getRows()) {
            if ("deprecated".equals(item.get2()))
                errln("Deprecated: " + ++count
                    + "; \t" + item.get0()
                    + "; \t" + item.get1().type
                    + "; \t" + item.get1().element
                    + "; \t" + item.get1().attribute
                    + "; \t" + item.get1().attributeValue
                    + "; \t" + item.get2());
        }

        count = 0;
        for (R3<String, AttributeValueSpec, String> item : errors.getRows()) {
            if (!"deprecated".equals(item.get2()))
                errln("Invalid: " + ++count
                    + "; \t" + item.get0()
                    + "; \t" + item.get1().type
                    + "; \t" + item.get1().element
                    + "; \t" + item.get1().attribute
                    + "; \t" + item.get1().attributeValue
                    + "; \t" + item.get2());
        }
    }
}
