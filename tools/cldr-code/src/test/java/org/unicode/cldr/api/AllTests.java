package org.unicode.cldr.api;

import java.io.PrintWriter;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.icu.dev.test.TestFmwk.TestGroup;
import org.unicode.cldr.util.ShimmedMain;

public class AllTests extends TestGroup {
    public static void main(String[] args) {
        System.setProperty("CLDR_ENVIRONMENT", "UNITTEST");
        new AllTests().run(args, new PrintWriter(System.out));
    }

    private static String[] getTestClassNames() {
        return ShimmedMain.findAllTests(AllTests.class.getPackage());
    }

    public AllTests() {
        super("org.unicode.cldr.api", getTestClassNames(), "API unit tests");
    }

    @Test
    public void runApiTests() {
        main(ShimmedMain.getArgs(AllTests.class, "-n -q"));
    }
}
