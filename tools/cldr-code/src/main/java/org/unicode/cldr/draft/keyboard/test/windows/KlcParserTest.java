package org.unicode.cldr.draft.keyboard.test.windows;

import java.io.IOException;

import org.unicode.cldr.draft.keyboard.windows.KlcParser;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.ibm.icu.dev.test.TestFmwk;

public class KlcParserTest extends TestFmwk {

    public void testParse() throws IOException {
        String layout = Resources.toString(Resources.getResource(KlcParserTest.class, "test_layout.txt"),
            Charsets.UTF_8);
        System.out.println(KlcParser.parseLayout(layout));
    }
}
