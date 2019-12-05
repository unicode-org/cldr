package org.unicode.cldr.api;

import static com.google.common.collect.ImmutableList.toImmutableList;

import java.io.PrintWriter;

import com.google.common.collect.ImmutableList;
import com.ibm.icu.dev.test.TestFmwk.TestGroup;

public class AllTests extends TestGroup {
    private static final ImmutableList<Class<?>> TEST_CLASSES = ImmutableList.of(
        CldrFileDataSourceTest.class,
        CldrPathTest.class,
        CldrValueTest.class,
        FilteredDataTest.class,
        PathMatcherTest.class,
        PrefixVisitorTest.class,
        XmlDataSourceTest.class);
    
    public static void main(String[] args) {
        System.setProperty("CLDR_ENVIRONMENT", "UNITTEST");
        new AllTests().run(args, new PrintWriter(System.out));
    }

    private static String[] getTestClassNames() {
        return TEST_CLASSES.stream()
            .map(Class::getName)
            .collect(toImmutableList()).toArray(new String[0]);
    }
    
    public AllTests() {
        super("org.unicode.cldr.api", getTestClassNames(), "API unit tests");
    }
}
