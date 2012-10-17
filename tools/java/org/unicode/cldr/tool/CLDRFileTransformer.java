package org.unicode.cldr.tool;

import java.io.PrintWriter;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRTransforms;
import org.unicode.cldr.util.CLDRTransforms.ParsedTransformID;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.XMLSource;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;

/**
 * Transforms the contents of a CLDRFile.
 * 
 * @author jchye
 */
public class CLDRFileTransformer {
    private UnicodeSet cyrillic = new UnicodeSet("[:script=Cyrl:]");
    private UnicodeSet unconverted = new UnicodeSet();
    private Transliterator transliterator;

    /**
     * @param transformFilename
     *            the filename of the transform data
     * @param direction
     *            the direction that the transliteration should be in
     */
    public CLDRFileTransformer(String transformFilename, int direction) {
        CLDRTransforms transforms = CLDRTransforms.getInstance();
        ParsedTransformID directionInfo = new ParsedTransformID();
        String ruleString = transforms.getIcuRulesFromXmlFile(
            CldrUtility.COMMON_DIRECTORY + "transforms/",
            transformFilename, directionInfo);
        transliterator = Transliterator.createFromRules(directionInfo.getId(),
            ruleString, direction);
    }

    /**
     * NOTE: This method does not currently handle nested transliterators.
     * 
     * @param input
     * @return
     */
    public CLDRFile transformCldrFile(CLDRFile input) {
        XMLSource outputSource = new SimpleXMLSource("sr_Latn");
        for (String xpath : input) {
            String fullPath = input.getFullXPath(xpath);
            String value = input.getStringValue(xpath);
            value = transform(xpath, value);
            outputSource.putValueAtPath(fullPath, value);
        }
        return new CLDRFile(outputSource);
    }

    /**
     * Transforms a CLDRFile value into another form.
     */
    private String transform(String path, String value) {
        String transliterated;
        // TODO: Don't transform dates/patterns.
        if (path.contains("exemplarCharacters")) {
            UnicodeSet oldExemplars = new UnicodeSet(value);
            UnicodeSet newExemplars = new UnicodeSet();
            for (String ch : oldExemplars) {
                newExemplars.add(transliterator.transliterate(ch));
            }
            transliterated = newExemplars.toString();
        } else {
            transliterated = transliterator.transliterate(value);
        }
        if (cyrillic.containsSome(transliterated)) {
            unconverted.addAll(new UnicodeSet().addAll(cyrillic).retainAll(transliterated));
        }
        return transliterated;
    }

    public static void main(String[] args) throws Exception {
        Factory factory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
        CLDRFile input = factory.make("sr", false);
        CLDRFileTransformer transformer = new CLDRFileTransformer(
            "Serbian-Latin-BGN.xml", Transliterator.FORWARD);
        CLDRFile output = transformer.transformCldrFile(input);
        PrintWriter out = BagFormatter.openUTF8Writer(CldrUtility.GEN_DIRECTORY, output.getLocaleID() + ".xml");
        output.write(out);
        out.close();
        System.out.println("Untransformed characters: " + transformer.unconverted);
    }
}
