package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Emoji;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.util.UnicodeMap;

public class PrepareRootAnnotations {

    public static void main(String[] args) throws IOException {
        // flesh out root with new values
        Factory factoryAnnotations = SimpleFactory.make(CLDRPaths.ANNOTATIONS_DIRECTORY, ".*");
        CLDRFile oldAnnotations = factoryAnnotations.make("root", false);
        UnicodeMap<String> oldValues = new UnicodeMap<>();
        for (String path : oldAnnotations) {
            XPathParts parts = XPathParts.getFrozenInstance(path);
            if (parts.getElement(1).equals("identity")) {
                continue;
            }
            String cp = parts.getAttributeValue(-1, "cp");
            String value = oldAnnotations.getStringValue(path);
            oldValues.put(cp, value);
        }
        CLDRFile annotations = oldAnnotations.cloneAsThawed();
        int counter = oldValues.size();
        for (String cp : Emoji.getNonConstructed()) {
            String value = oldValues.get(cp);
            if (value == null) {
                oldValues.put(cp, value = "E" + (counter++));
            }
            String base = "//ldml/annotations/annotation[@cp=\"" + cp + "\"]";
            String namePath = base + Emoji.TYPE_TTS;
            String keywordPath = base;
            annotations.add(namePath, value);
            annotations.add(keywordPath, value);
        }

        try (PrintWriter pw = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY + "annotations/", "root.xml")) {
            annotations.write(pw);
            pw.flush();
        }
    }
}
