package org.unicode.cldr.unittest;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class NumberingSystemsTest extends TestFmwk {
    static CLDRConfig testInfo = CLDRConfig.getInstance();

    public static void main(String[] args) {
        new NumberingSystemsTest().run(args);
    }

    public void TestFile() {
        CLDRFile file = testInfo.getSupplementalFactory().make(
            "numberingSystems", false);
        XPathParts parts = new XPathParts();
        for (String path : file) {
            parts.set(path);
            if (!"numberingSystems".equals(parts.getElement(1))) {
                continue;
            }
            String id = parts.getAttributeValue(2, "id");
            String digits = parts.getAttributeValue(2, "digits");
            if (digits == null) {
                continue;
            }
            assertEquals("Must have 10 values", 10,
                UTF16.countCodePoint(digits));
            int cp;
            int value = 0;
            for (int i = 0; i < digits.length(); i += UTF16.getCharCount(cp)) {
                cp = UTF16.charAt(digits, i);
                assertEquals("Value mismatch", value++,
                    UCharacter.getNumericValue(cp));
            }
            try {
                UnicodeSet script = new UnicodeSet().applyPropertyAlias(
                    "script", id);
                if (!script.containsAll(digits)) {
                    if (id.equals("latn")
                        && digits.equals("0123456789")
                        || id.equals("arab")
                            && digits
                                .equals("\u0660\u0661\u0662\u0663\u0664\u0665\u0666\u0667\u0668\u0669")) {
                        // ok
                    } else {
                        errln("Script doesn't match digits: " + id + ", "
                            + digits);
                    }
                }
            } catch (Exception e) {
                logln(id + " not a script");
            }
        }
    }
}
