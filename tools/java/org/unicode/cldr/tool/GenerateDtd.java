package org.unicode.cldr.tool;

import org.unicode.cldr.util.CLDRFile.DtdType;
import org.unicode.cldr.util.DtdData;

public class GenerateDtd {
    public static void main(String[] args) {
        System.setProperty("show_all", "true");
        for (DtdType type : DtdType.values()) {
            DtdData data = DtdData.getInstance(type);
        }
    }
}
