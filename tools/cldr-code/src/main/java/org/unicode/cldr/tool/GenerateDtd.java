package org.unicode.cldr.tool;

import com.ibm.icu.text.CaseMap;
import java.io.IOException;
import java.util.Locale;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTool;
import org.unicode.cldr.util.DTD2Markdown;
import org.unicode.cldr.util.DTD2XSD;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.TempPrintWriter;

@CLDRTool(alias = "generate-dtd", description = "BRS: Reformat all DTDs")
public class GenerateDtd {

    private static final CaseMap.Title TO_TITLE_WHOLE_STRING_NO_LOWERCASE =
            CaseMap.toTitle().wholeString().noLowercase();

    public static void main(String[] args) throws IOException {
        final DTD2Markdown dtd2md = new DTD2Markdown();
        // System.setProperty("show_all", "true");
        for (DtdType type : DtdType.values()) {
            if (type.getStatus() != DtdType.DtdStatus.active) {
                continue;
            } else if (type == DtdType.ldmlICU) {
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
            String dtdPath = CLDRPaths.BASE_DIRECTORY + type.dtdPath;
            try (TempPrintWriter out = TempPrintWriter.openUTF8Writer(dtdPath)) {
                out.println(data);
                System.err.println("Wrote DTD: " + dtdPath);
            }

            // Write XSD
            DTD2XSD.write(data, type);

            // Write Markdown
            dtd2md.write(data, type);
        }
    }
}
