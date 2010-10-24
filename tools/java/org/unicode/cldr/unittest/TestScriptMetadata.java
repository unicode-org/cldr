package org.unicode.cldr.unittest;

import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.Info;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.lang.UScript;

public class TestScriptMetadata extends TestFmwk {
    public static void main(String[] args) {
        new TestScriptMetadata().run(args);
    }
    public void TestBasic() {
        Info info0 = ScriptMetadata.getInfo(UScript.LATIN);
        if (ScriptMetadata.errors.size() != 0) {
            System.out.println(CollectionUtilities.join(ScriptMetadata.errors, "\n"));
        }
        for (int i = UScript.COMMON; i < UScript.CODE_LIMIT; ++i) {
            Info info = ScriptMetadata.getInfo(i);
            if (info != null) {
                System.out.println(UScript.getName(i) + "\t" + UScript.getShortName(i) + "\t" + info);
            }
        }
    }
}
