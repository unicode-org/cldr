package org.unicode.cldr.tool;

import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.CLDRFile.DtdType;

public class GenerateReformattedXml {
    public static void main(String[] args) {
        for (DtdType dtdType : DtdType.values()) {
            System.out.println("\n#####\n\t" + dtdType + "\n#####");
            System.out.println(DtdData.getInstance(dtdType));
        }
    }
}
