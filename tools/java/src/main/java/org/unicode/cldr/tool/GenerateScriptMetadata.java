package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.FileCopier;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.util.VersionInfo;

public class GenerateScriptMetadata {
    public static void main(String[] args) throws IOException {
        PrintWriter out = FileUtilities.openUTF8Writer(CLDRPaths.COMMON_DIRECTORY + "/properties", "scriptMetadata.txt");
        // PrintWriter out = new PrintWriter(System.out);
//        FileUtilities.appendFile(GenerateScriptMetadata.class, "GenerateScriptMetadata.txt", out);
        FileCopier.copy(GenerateScriptMetadata.class, "GenerateScriptMetadata.txt", out);

        Set<R3<Integer, String, Info>> sorted = new TreeSet<R3<Integer, String, Info>>();
        for (String script : ScriptMetadata.getScripts()) {
            Info i = ScriptMetadata.getInfo(script);
            R3<Integer, String, Info> r = Row.of(i.rank, script, i);
            sorted.add(r);
        }
        if (ScriptMetadata.errors.size() > 0) {
            System.err.println(CollectionUtilities.join(ScriptMetadata.errors, "\n\t"));
            //throw new IllegalArgumentException();
        }
        VersionInfo currentUnicodeVersion = UCharacter.getUnicodeVersion();
        for (R3<Integer, String, Info> s : sorted) {
            String script = s.get1();
            Info i = s.get2();
            String comment = i.age.compareTo(currentUnicodeVersion) > 0 ? "  # provisional data for future Unicode " + i.age.getVersionString(2, 2) + " script"
                : "";
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
                + comment);
            // RTL? LB letters? Shaping Req? IME? Has Case?
        }
        out.println();
        out.println("# EOF");
        out.close();
    }
}
