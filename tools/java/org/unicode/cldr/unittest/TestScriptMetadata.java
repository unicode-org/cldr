package org.unicode.cldr.unittest;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.draft.EnumLookup;
import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.IdUsage;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.cldr.draft.ScriptMetadata.Shaping;
import org.unicode.cldr.draft.ScriptMetadata.Trinary;

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
        Relation<IdUsage, String> map = Relation.of(new EnumMap<IdUsage, Set<String>>(IdUsage.class),
            LinkedHashSet.class);
        for (int i = UScript.COMMON; i < UScript.CODE_LIMIT; ++i) {
            Info info = ScriptMetadata.getInfo(i);
            if (info != null) {
                map.put(info.idUsage, UScript.getName(i) + "\t(" + UScript.getShortName(i) + ")\t" + info);
            } else {
                temp.applyIntPropertyValue(UProperty.SCRIPT, i);
                if (temp.size() != 0) {
                    errln("Missing data for " + UScript.getName(i) + "\t(" + UScript.getShortName(i));
                }
            }
        }
        for (Entry<IdUsage, String> entry : map.keyValueSet()) {
            logln(entry.getValue());
        }
    }
}
