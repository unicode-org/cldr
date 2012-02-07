package org.unicode.cldr.draft;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UTF16;


public class ScriptMetadata {
    enum Group {
        unknown, European, East_Asian,Southeast_Asian, Middle_Eastern, South_Asian, Central_Asian, African, American
    }
    enum IdUsage {
        unknown, Recommended, Limited_Use, Exclusion
    }
    enum RTL {unknown, no, RTL}
    enum LbLetters {unknown, no, Yes}
    enum ShapingReq {unknown, no, Yes}
    enum IME {unknown, no, Yes}

    public static class Info {
        final int rank;
        final int sampleChar;
        final Group group;
        final IdUsage idUsage;
        final RTL rtl;
        final LbLetters lbLetters;
        final ShapingReq shapingReq;
        final IME ime;
        private Info(String[] items) {
            // 3,Han,Hani,1.1,"74,519",字,5B57,East_Asian,Recommended,no,Yes,no,Yes
            rank = Integer.parseInt(items[0]);
            sampleChar = items[5].codePointAt(0);
            group = Group.valueOf(fix(items[7]));
            idUsage = IdUsage.valueOf(fix(items[8]));
            rtl = RTL.valueOf(fix(items[9]));
            lbLetters = LbLetters.valueOf(fix(items[10]));
            shapingReq = ShapingReq.valueOf(fix(items[11]));
            ime = IME.valueOf(fix(items[12]));
        }
        String fix(String in) {
            return in.replace("n/a", "unknown").replace("?","unknown");
        }
        public String toString() {
            return rank + "\t" + UTF16.valueOf(sampleChar) 
            + "\t" + group
            + "\t" + idUsage
            + "\t" + rtl
            + "\t" + lbLetters
            + "\t" + shapingReq
            + "\t" + ime;
        }
    }

    public static Set<String> errors = new LinkedHashSet<String>();

    private static class MyFileReader extends FileUtilities.SemiFileReader {
        public Map<String, Info> data = new HashMap<String, Info>();
        @Override
        protected boolean isCodePoint() {
            return false;
        }
        @Override
        protected String[] splitLine(String line) {
            return FileUtilities.splitCommaSeparated(line);
        };
        @Override
        protected boolean handleLine(int lineCount, int start, int end, String[] items) {
            if (lineCount < 3) return true; // header lines
            Info info;
            try {
                info = new Info(items);
            } catch (Exception e) {
                errors.add(e.getMessage());
                return true;
            }

            String script = items[2];
            data.put(script, info);
            return true;
        }
        @Override
        public MyFileReader process(Class classLocation, String fileName) {
            super.process(classLocation, fileName);
            return this;
        }

    }
    static Map<String,Info> data = new MyFileReader().process(ScriptMetadata.class, "Script_Metadata.csv").data;

    public static Info getInfo(String s) {
        return data.get(s);
    }

    public static Info getInfo(int i) {
        return data.get(UScript.getShortName(i));
    }
}
