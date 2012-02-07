package org.unicode.cldr.unittest;

import org.unicode.cldr.test.DisplayAndInputProcessor;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UnicodeSet;

public class TestDisplayAndInputProcessor extends TestFmwk{

    TestInfo info = TestAll.TestInfo.getInstance();

    public static void main(String[] args) {
        new TestDisplayAndInputProcessor().run(args);
    }

    public void TestAll() {
        showCldrFile(info.getEnglish());
        showCldrFile(info.getCldrFactory().make("wae", true));
    }

    private void showCldrFile(final CLDRFile cldrFile) {
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(cldrFile);
        Exception[] internalException = new Exception[1];
        for (String path : cldrFile) {
            String value = cldrFile.getStringValue(path);
            String display = daip.processForDisplay(path, value);
            internalException[0] = null;
            String input = daip.processInput(path, display, internalException);
            String diff = diff(value, input, path);
            if (diff != null) {
                errln("No roundtrip in DAIP:"
                        + "\n\tvalue<" + value 
                        + ">\n\tdisplay<" + display 
                        + ">\n\tinput<" + input 
                        + ">\n\tdiff<" + diff
                        + (internalException[0] != null ? ">\n\texcep<" + internalException[0] : "" )
                        + ">\n\tpath<" + path + ">");
                daip.processInput(path, value, internalException); // for debugging
            } else if (!CharSequences.equals(value, display) 
                    || !CharSequences.equals(value, input) 
                    || internalException[0] != null) {
                logln("DAIP Changes"
                		+ "\n\tvalue<" + value 
                        + ">\n\tdisplay<" + display 
                        + ">\n\tinput<" + input 
                        + ">\n\tdiff<" + diff
                        + (internalException[0] != null ? ">\n\texcep<" + internalException[0] : "" )
                        + ">\n\tpath<" + path + ">");
            }
        }
    }

    private String diff(String value, String input, String path) {
        if (value.equals(input)) {
            return null;
        }
        if (path.contains("/exemplarCharacters")) {
            try {
                UnicodeSet s1 = new UnicodeSet(value);
                UnicodeSet s2 = new UnicodeSet(input);
                if (!s1.equals(s2)) {
                    UnicodeSet temp = new UnicodeSet(s1).removeAll(s2);
                    UnicodeSet temp2 = new UnicodeSet(s2).removeAll(s1);
                    temp.addAll(temp2);
                    return temp.toPattern(false);
                }
            } catch (Exception e) {
                // TODO: handle exception
            }   
        }
        return "?";
    }
}
