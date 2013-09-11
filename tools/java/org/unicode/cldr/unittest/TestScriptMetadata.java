package org.unicode.cldr.unittest;

import java.util.BitSet;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.EnumLookup;
import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.IdUsage;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.cldr.draft.ScriptMetadata.Shaping;
import org.unicode.cldr.draft.ScriptMetadata.Trinary;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;

public class TestScriptMetadata extends TestFmwk {
    public static void main(String[] args) {
        new TestScriptMetadata().run(args);
    }

    public void TestLookup() {
        EnumLookup<IdUsage> temp = EnumLookup.of(IdUsage.class);
        assertEquals("", IdUsage.LIMITED_USE, temp.forString("limited Use"));
    }

    public void TestScriptOfSample() {
        BitSet bitset = new BitSet();
        for (String script : ScriptMetadata.getScripts()) {
            Info info0 = ScriptMetadata.getInfo(script);
            assertEquals("Sample must be single character", 1,
                info0.sampleChar.codePointCount(0, info0.sampleChar.length()));
            int scriptCode = UScript.getScriptExtensions(info0.sampleChar.codePointAt(0), bitset);
            assertTrue("Must have single, valid script " + scriptCode, scriptCode >= 0);
        }
    }

    public void TestBasic() {
        Info info0 = ScriptMetadata.getInfo(UScript.LATIN);
        if (ScriptMetadata.errors.size() != 0) {
            errln("ScriptMetadata initialization errors\t" + ScriptMetadata.errors.size() + "\t"
                + CollectionUtilities.join(ScriptMetadata.errors, "\n"));
        }

        // Latin Latn 2 L European Recommended no no no no
        assertEquals("Latin-rank", 2, info0.rank);
        assertEquals("Latin-country", "IT", info0.originCountry);
        assertEquals("Latin-sample", "L", info0.sampleChar);
        assertEquals("Latin-id usage", ScriptMetadata.IdUsage.RECOMMENDED, info0.idUsage);
        assertEquals("Latin-ime?", Trinary.NO, info0.ime);
        assertEquals("Latin-lb letters?", Trinary.NO, info0.lbLetters);
        assertEquals("Latin-rtl?", Trinary.NO, info0.rtl);
        assertEquals("Latin-shaping", Shaping.MIN, info0.shapingReq);
        assertEquals("Latin-density", 1, info0.density);

        info0 = ScriptMetadata.getInfo(UScript.HEBREW);
        assertEquals("Arabic-rtl", Trinary.YES, info0.rtl);
        assertEquals("Arabic-shaping", Shaping.NO, info0.shapingReq);

    }

    public void TestScripts() {
        UnicodeSet temp = new UnicodeSet();
        Set<String> missingScripts = new TreeSet<String>();
        Relation<IdUsage, String> map = Relation.of(new EnumMap<IdUsage, Set<String>>(IdUsage.class),
            LinkedHashSet.class);
        for (int i = UScript.COMMON; i < UScript.CODE_LIMIT; ++i) {
            Info info = ScriptMetadata.getInfo(i);
            if (info != null) {
                map.put(info.idUsage, UScript.getName(i) + "\t(" + UScript.getShortName(i) + ")\t" + info);
            } else {
                temp.applyIntPropertyValue(UProperty.SCRIPT, i); // TODO: What's the point of this?
                if (temp.size() != 0) {
                    errln("Missing data for " + UScript.getName(i) + "\t(" + UScript.getShortName(i));
                } else {
                    missingScripts.add(UScript.getShortName(i));
                }
            }
        }
        for (Entry<IdUsage, String> entry : map.keyValueSet()) {
            logln(entry.getValue());
        }
        if (!missingScripts.isEmpty() && !logKnownIssue("6647", "missing script metadata")) {
            errln("Also missing: " + missingScripts.toString());
        }
    }

    // lifted from ShowLanguages
    private static Set<String> getEnglishTypes(String type, int code, StandardCodes sc, CLDRFile english) {
        Set<String> result = new HashSet<String>(sc.getSurveyToolDisplayCodes(type));
        XPathParts parts = new XPathParts();
        for (Iterator<String> it = english.getAvailableIterator(code); it.hasNext();) {
            parts.set(it.next());
            String newType = parts.getAttributeValue(-1, "type");
            if (!result.contains(newType)) {
                result.add(newType);
            }
        }
        return result;
    }

    // lifted from ShowLanguages
    private static Set<String> getScriptsToShow(StandardCodes sc, CLDRFile english) {
        return getEnglishTypes("script", CLDRFile.SCRIPT_NAME, sc, english);
    }

    public void TestShowLanguages() {
        // lifted from ShowLanguages - this is what ShowLanguages tried to do.
        StandardCodes sc = StandardCodes.make();
        Factory cldrFactory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
        CLDRFile english = cldrFactory.make("en", true);
        Set<String> bads = new TreeSet<String>();

        for (String s : getScriptsToShow(sc, english)) {
            if (ScriptMetadata.getInfo(s) == null) {
                bads.add(s);
            }
        }
        if (!bads.isEmpty() && !logKnownIssue("6647", "missing script metadata")) {
            errln("No metadata for scripts: " + bads.toString());
        }
    }
}
