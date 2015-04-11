package org.unicode.cldr.tool;

import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdType;

public class GenerateReformattedXml {
    public static void main(String[] args) {
        for (DtdType dtdType : DtdType.values()) {
            if (args.length > 0 && !dtdType.toString().matches(args[0])) {
                continue;
            }
            System.out.println("\n#####\n\t" + dtdType + "\n#####");
            System.out.println(DtdData.getInstance(dtdType));
        }
    }
}
