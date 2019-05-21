package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdType;

import com.ibm.icu.text.CaseMap;

public class GenerateDtd {

    private static final CaseMap.Title TO_TITLE_WHOLE_STRING_NO_LOWERCASE = CaseMap.toTitle().wholeString().noLowercase();

    public static void main(String[] args) throws IOException {
        //System.setProperty("show_all", "true");
        for (DtdType type : DtdType.values()) {
            if (type == DtdType.ldmlICU) {
                continue;
            }
            DtdData data = DtdData.getInstance(type);
            String name = type.toString();
            if (!name.startsWith("ldml")) {
                name = "ldml" + TO_TITLE_WHOLE_STRING_NO_LOWERCASE.apply(Locale.ROOT, null, name);
                if (name.endsWith("Data")) {
                    name = name.substring(0, name.length() - 4);
                }
            }
            try (PrintWriter out = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY + "dtd/", name + ".dtd")) {
                out.println(data);
            }
        }
    }
}
