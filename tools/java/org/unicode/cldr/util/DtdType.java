package org.unicode.cldr.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum DtdType {
    ldml("common/dtd/ldml.dtd"),
    ldmlICU("common/dtd/ldmlICU.dtd", ldml),
    supplementalData("common/dtd/ldmlSupplemental.dtd"),
    ldmlBCP47("common/dtd/ldmlBCP47.dtd"),
    keyboard("keyboards/dtd/ldmlKeyboard.dtd"),
    platform("keyboards/dtd/ldmlPlatform.dtd");

    static Pattern FIRST_ELEMENT = Pattern.compile("//([^/\\[]*)");

    public final String dtdPath;
    public final DtdType rootType;

    private DtdType(String dtdPath) {
        this.dtdPath = dtdPath;
        this.rootType = this;
    }

    private DtdType(String dtdPath, DtdType realType) {
        this.dtdPath = dtdPath;
        this.rootType = realType;
    }

    public static DtdType fromPath(String elementOrPath) {
        Matcher m = FIRST_ELEMENT.matcher(elementOrPath);
        m.lookingAt();
        return DtdType.valueOf(m.group(1));
    }
}