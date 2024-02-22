package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.ibm.icu.text.UnicodeSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TestPUAs {

    public static final class PuaHelper {
        final Pattern COLLATOR_TYPE =
                PatternCache.get("//ldml/collations/collation\\[@type=\"([^\"]+)\"\\].*/cr");
        final Factory collatorFactory =
                Factory.make(CLDRPaths.COMMON_DIRECTORY + "collation/", ".*");
        final UnicodeSet IS_PUA = new UnicodeSet("[:Co:]").freeze();
    }

    static final PuaHelper INSTANCE = new PuaHelper();

    /** Create a stream of args */
    public static Stream<Arguments> collatorProvider() {
        final List<Arguments> args = new LinkedList<Arguments>();
        for (final String locale : INSTANCE.collatorFactory.getAvailable()) {
            CLDRFile cldrFile = INSTANCE.collatorFactory.make(locale, false); // don't need resolved
            Matcher m = INSTANCE.COLLATOR_TYPE.matcher("");
            Set<String> results = new LinkedHashSet<>();
            for (String path : cldrFile) {
                if (m.reset(path).matches()) {
                    String type = m.group(1);
                    boolean newOne = results.add(type);
                    if (newOne) {
                        args.add(arguments(locale, type, path));
                    }
                }
            }
        }
        return args.stream();
    }

    @ParameterizedTest(name = "{0}:{1}")
    @MethodSource("collatorProvider")
    /** Assert no PUA in Collator rules */
    public void TestCollatorPUA(String locale, String type, String path) {
        CLDRFile cldrFile = INSTANCE.collatorFactory.make(locale, false); // don't need resolved
        String rules = cldrFile.getStringValue(path);
        assertFalse(
                INSTANCE.IS_PUA.containsSome(rules),
                () -> {
                    return locale
                            + ":"
                            + type
                            + " contained PUAs: "
                            + UnicodeSet.fromAll(rules).retainAll(INSTANCE.IS_PUA).toString();
                });
    }
}
