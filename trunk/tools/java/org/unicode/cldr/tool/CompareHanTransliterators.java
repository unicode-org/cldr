package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XMLFileReader.SimpleHandler;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;

public class CompareHanTransliterators {
    public static void main(String[] args) throws IOException {
        XMLFileReader reader = new XMLFileReader();
        MyContentHandler handler = new MyContentHandler();
        reader.setHandler(handler);
        reader.read(CLDRPaths.COMMON_DIRECTORY + "transforms/Han-Latin.xml",
            XMLFileReader.CONTENT_HANDLER, false);
        UnicodeMap<String> trunk = handler.map;

        handler.map = new UnicodeMap<String>();

        reader.read(CLDRPaths.LAST_DIRECTORY + "/common/transforms/Han-Latin.xml",
            XMLFileReader.CONTENT_HANDLER, false);
        UnicodeMap<String> old = handler.map;

        UnicodeSet merged = new UnicodeSet(trunk.keySet()).addAll(old.keySet());
        PrintWriter out = FileUtilities.openUTF8Writer(org.unicode.cldr.util.CLDRPaths.GEN_DIRECTORY, "han-transliterator-diff.txt");
        for (String s : merged) {
            String oldValue = old.get(s);
            if (oldValue == null) {
                continue;
            }
            String trunkValue = trunk.get(s);
            if (!CharSequences.equals(trunkValue, oldValue)) {
                out.println(Utility.hex(s) + "\t" + s + "\t" + oldValue + "\t" + trunkValue);
            }
        }
        out.close();
    }

    public static class MyContentHandler extends SimpleHandler {
        UnicodeMap<String> map = new UnicodeMap<String>();

        public void handlePathValue(String path, String value) {
            if (!path.contains("tRule")) return;
            int pos = value.indexOf('→');
            if (pos < 0) return;
            String source = value.substring(0, pos).trim();
            String target = value.substring(pos + 1).trim();
            if (UnicodeSet.resemblesPattern(source, 0)) {
                map.putAll(new UnicodeSet(source), target);
            } else if (UCharacter.codePointCount(source, 0, source.length()) == 1) {
                map.put(source, target);
            } else {
                throw new IllegalArgumentException(); // unexpected
            }
        }
    }
}
