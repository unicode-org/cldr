package org.unicode.cldr.unittest;

import java.util.ArrayList;
import java.util.List;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.Options;
import org.unicode.cldr.test.CheckDisplayCollisions;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.XMLSource;

public class TestCheckDisplayCollisions extends TestFmwkPlus {
    public static void main(String[] args) {
        new TestCheckDisplayCollisions().run(args);
    }

    public void testInheritance() {
        XMLSource rootSource = new SimpleXMLSource("root");
        CLDRFile root = new CLDRFile(rootSource);

        XMLSource enSource = new SimpleXMLSource("en");
        CLDRFile en = new CLDRFile(enSource);

        XMLSource frSource = new SimpleXMLSource("fr");
        frSource.putValueAtPath("//ldml/annotations/annotation[@cp=\"🦂\"][@type=\"tts\"]", "scorpion");
        frSource.putValueAtPath("//ldml/annotations/annotation[@cp=\"♏\"][@type=\"tts\"]", "scorpion zodiac");
        CLDRFile fr = new CLDRFile(frSource);

        XMLSource frCaSource = new SimpleXMLSource("fr_CA");
        frSource.putValueAtPath("//ldml/annotations/annotation[@cp=\"♏\"][@type=\"tts\"]", "scorpion");
        CLDRFile frCA = new CLDRFile(frCaSource);

        TestFactory factory = new TestFactory();
        factory.addFile(root);
        factory.addFile(en);
        factory.addFile(fr);
        factory.addFile(frCA);

        CheckDisplayCollisions cdc = new CheckDisplayCollisions(factory);
        CLDRFile frCaResolved = factory.make("fr_CA", true);
        cdc.setEnglishFile(CLDRConfig.getInstance().getEnglish());
        checkFile(cdc, frCA, frCaResolved);
    }

    private void checkFile(CheckDisplayCollisions cdc, CLDRFile frCa, CLDRFile frCaResolved) {
        List<CheckStatus> possibleErrors = new ArrayList<>();
        Options options = new Options();
        cdc.setCldrFileToCheck(frCa, options, possibleErrors);
        if (!possibleErrors.isEmpty()) {
            System.out.println("init: " + possibleErrors);
            possibleErrors.clear();
        }
        for (String path : frCaResolved) {
            String value = frCaResolved.getStringValue(path);
            cdc.check(path, path, value, options, possibleErrors);
            if (!possibleErrors.isEmpty()) {
                System.out.println(path + "\t" + possibleErrors);
                possibleErrors.clear();
            }
        }
    }
}
