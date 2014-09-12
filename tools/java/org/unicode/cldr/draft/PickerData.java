package org.unicode.cldr.draft;

import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.unicode.cldr.draft.CharacterListCompressor.Interval;
import org.unicode.cldr.draft.picker.CharData;

public class PickerData {
    /*
    public static String[][][] CATEGORIES = {
    {{"Symbol"},
    {"Alchemical Symbols@Other ","A2j1dA"},
    {"Arrows","%=68k11I3706:%M%G7AnTMm6e6HDk%`O728F1f4V1PNF2WF1G}58?]514M]Ol1%2l2Q%W06gQ01U:1Un2Mb>$0M"},
     */
    static final List<String> CATEGORIES;
    public static int CATEGORY_TITLE = 0;
    public static int SUBCATEGORY_OFFSET = 1;
    public static int SUBCATEGORY_TITLE = 0;
    public static int SUBCATEGORY_BASE88 = 1;

    static {
        List<String> categories = new LinkedList<String>(/*CharData.CATEGORIES.length*/);
        for (int i = 0; i < CharData.CATEGORIES.length; i++) {
            categories.add(CharData.CATEGORIES[i][CATEGORY_TITLE][CATEGORY_TITLE]);
        }
        CATEGORIES = categories;
    }

    public static final List<String> getCategories() {
        return CATEGORIES;
    }

    public static String[] getSubCategories(int c) {
        Vector<String> v = new Vector<String>();
        for (int i = SUBCATEGORY_OFFSET; i < CharData.CATEGORIES[c].length; i++) {
            v.add(CharData.CATEGORIES[c][i][SUBCATEGORY_TITLE]);
        }
        return v.toArray(new String[v.size()]);
    }

    public static List<Interval> getStringArray(int a, int b) {
        return CharacterListCompressor.base88DecodeList(CharData.CATEGORIES[a][b + SUBCATEGORY_OFFSET][SUBCATEGORY_BASE88]);
    }

}
