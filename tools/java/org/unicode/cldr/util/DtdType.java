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

    public String header() {
        return "<?xml version='1.0' encoding='UTF-8' ?>\n"
            +"<!DOCTYPE " + this // supplementalData
            + " SYSTEM '../../" + dtdPath + "'>\n" // "common/dtd/ldmlSupplemental.dtd"
            +"<!--\n"
            +"Copyright © 1991-2013 Unicode, Inc.\n"
            +"CLDR data files are interpreted according to the LDML specification (http://unicode.org/reports/tr35/)\n"
            +"For terms of use, see http://www.unicode.org/copyright.html\n"
            +"-->\n"
            +"<" + this + ">\n";
    }
}