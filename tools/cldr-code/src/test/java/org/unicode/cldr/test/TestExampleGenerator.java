package org.unicode.cldr.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XMLSource.ResolvingSource;

public class TestExampleGenerator {

    @Test
    public void testPersonNamesGwen() {
        final String loc = "es";
        final String X_GIVEN = "//ldml/personNames/sampleName[@item=\"givenSurname\"]/nameField[@type=\"given\"]";
        final String X_SURNAME = "//ldml/personNames/sampleName[@item=\"givenSurname\"]/nameField[@type=\"surname\"]";
        final String X_PATTERN = "//ldml/personNames/personName[@length=\"long\"][@usage=\"addressing\"][@style=\"formal\"][@order=\"sorting\"]/namePattern";

        final CLDRFile english = CLDRConfig.getInstance().getEnglish();
        final XMLSource source = new SimpleXMLSource(loc);
        // add a bunch of English stuff
        for (final String x : english.fullIterable() ) {
            if (!x.startsWith("//ldml/personNames")) continue;
            source.add(english.getFullXPath(x), english.getStringValue(x));
        }
        source.add(X_PATTERN, "{surname}, {given}");
        source.add(X_GIVEN, "Fred");
        source.add(X_SURNAME, "Person");

        final XMLSource root = new SimpleXMLSource("root");
        final List<XMLSource> sourceList = new LinkedList<>();
        sourceList.add(source);
        sourceList.add(root);
        final ResolvingSource rs = new ResolvingSource(sourceList);
        final CLDRFile afile = new CLDRFile(rs);
        ExampleGenerator eg = new ExampleGenerator(afile, english, english.getSupplementalDirectory().getAbsolutePath());
        assertNotNull(eg);


        final String html1 = eg.getExampleHtml(X_PATTERN, source.getValueAtDPath(X_PATTERN));
        // html chunk…
        assertTrue(html1.contains(">Person, Fred<"), () -> "Expected '>Person, Fred<' in the morass of " + html1);
        source.add(X_GIVEN, "Gwen");
        eg.updateCache(X_GIVEN); // Notify the ExampleGenerator that there's a change

        final String html2 = eg.getExampleHtml(X_PATTERN, source.getValueAtDPath(X_PATTERN));
        // html chunk…
        assertTrue(html2.contains(">Person, Gwen<"), () -> "Expected '>Person, Gwen<' in the morass of " + html2);
    }
}
