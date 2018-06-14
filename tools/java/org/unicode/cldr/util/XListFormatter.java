package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.ListFormatter;
import com.ibm.icu.text.SimpleFormatter;
import com.ibm.icu.text.UTF16;

public class XListFormatter {
    
    public enum ListTypeLength {
        AND_WIDE(""), 
        AND_SHORT("[@type=\"standard-short\"]"), 
        OR_WIDE("[@type=\"or\"]"), 
        UNIT_WIDE("[@type=\"unit\"]"), 
        UNIT_SHORT("[@type=\"unit-short\"]"), 
        UNIT_NARROW("[@type=\"unit-narrow\"]")
        ;

        public static final ListTypeLength NORMAL = AND_WIDE;
        
        final String typeString;

        static final Map<String, ListTypeLength> stringToEnum;
        static {
            Map<String, ListTypeLength> _stringToEnum = new LinkedHashMap<>();
            for (ListTypeLength value : values()) {
                if (value != NORMAL) {
                    _stringToEnum.put(value.typeString.substring(value.typeString.indexOf('"')+1, value.typeString.lastIndexOf('"')), value);
                }
            }
            stringToEnum = ImmutableMap.copyOf(_stringToEnum);
        }
        private ListTypeLength(String typeString) {
            this.typeString = typeString;
        }
        public static ListTypeLength from(String listPatternType) {
            if (listPatternType == null) {
                return NORMAL;
            }
            return stringToEnum.get(listPatternType);
        }
        public String getPath() {
            return "//ldml/listPatterns/listPattern"
                + typeString
                + "/listPatternPart[@type=\"{0}\"]";
        }
    }
    
    private ListFormatter listFormatter;
    
    public XListFormatter(CLDRFile cldrFile, ListTypeLength listTypeLength) {
        SimpleFormatter listPathFormat = SimpleFormatter.compile(listTypeLength.getPath());
        String doublePattern = cldrFile.getWinningValue(listPathFormat.format("2"));
        String startPattern = cldrFile.getWinningValue(listPathFormat.format("start"));
        String middlePattern = cldrFile.getWinningValue(listPathFormat.format("middle"));
        String endPattern = cldrFile.getWinningValue(listPathFormat.format("end"));
        listFormatter = new ListFormatter(doublePattern, startPattern, middlePattern, endPattern);
    }
    
    public String format(Object... items) {
        return listFormatter.format(items);
    }

    public String format(Collection<?> items) {
        return listFormatter.format(items);
    }
    
    public String formatCodePoints(String items) {
        List<String> source = new ArrayList<>();
        for (int sourceInt : CharSequences.codePoints(items)) { // TODO add utility in CharSequences
           source.add(UTF16.valueOf(sourceInt));
        }
        return listFormatter.format(source);
    }
}