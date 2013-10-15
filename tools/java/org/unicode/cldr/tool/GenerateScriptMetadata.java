package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.cldr.util.CLDRPaths;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.impl.Utility;

public class GenerateScriptMetadata {
    public static void main(String[] args) throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(CLDRPaths.COMMON_DIRECTORY + "/properties",
            "scriptMetadata.txt");
        // PrintWriter out = new PrintWriter(System.out);
        FileUtilities.appendFile(GenerateScriptMetadata.class, "GenerateScriptMetadata.txt", out);
        Set<R3<Integer, String, Info>> sorted = new TreeSet<R3<Integer, String, Info>>();
        for (String script : ScriptMetadata.getScripts()) {
            Info i = ScriptMetadata.getInfo(script);
            R3<Integer, String, Info> r = Row.of(i.rank, script, i);
            sorted.add(r);
        }
        for (R3<Integer, String, Info> s : sorted) {
            String script = s.get1();
            Info i = s.get2();
            out.println(script
                + "; " + i.rank
                + "; " + Utility.hex(i.sampleChar)
                + "; " + i.originCountry
                + "; " + i.density
                // + "; " + i.likelyLanguage
                + "; " + i.idUsage
                + "; " + i.rtl
                + "; " + i.lbLetters
                + "; " + i.shapingReq
                + "; " + i.ime
                + "; " + i.hasCase
                );
            // RTL? LB letters? Shaping Req? IME? Has Case?
        }
        out.println("\n# EOF");
        out.close();
    }
}
