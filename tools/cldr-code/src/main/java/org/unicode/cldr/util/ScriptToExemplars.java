package org.unicode.cldr.util;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.ibm.icu.text.UnicodeSet;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ScriptToExemplars {
    public static final String FILE_PATH = "data/locales/scriptToExemplars.txt";

    public static UnicodeSet getExemplars(String script) {
        return ScriptToExemplarsLoader.SINGLETON.getExemplars(script);
    }

    /** return the comment block from the original file */
    private static String getCommentBlock() {
        return String.join("\n", ScriptToExemplarsLoader.SINGLETON.comments) + '\n';
    }

    private static class ScriptToExemplarsLoader {
        private static final ScriptToExemplarsLoader SINGLETON = new ScriptToExemplarsLoader();
        private Map<String, UnicodeSet> data;
        private String[] comments;

        private UnicodeSet getExemplars(String script) {
            UnicodeSet result = data.get(script);
            return result == null ? UnicodeSet.EMPTY : result;
        }

        {
            Map<String, UnicodeSet> _data = Maps.newTreeMap();
            List<String> _comments = new ArrayList<String>();
            try (BufferedReader reader = FileReaders.openFile(ScriptToExemplars.class, FILE_PATH)) {
                Iterable<String> rlsi =
                        With.toIterable(new FileReaders.ReadLineSimpleIterator(reader));
                for (String line : rlsi) {
                    if (line.isBlank()) {
                        continue;
                    } else if (line.startsWith("#")) {
                        _comments.add(line.trim());
                        continue;
                    }
                    Iterator<String> parts = Splitter.on(';').trimResults().split(line).iterator();
                    String script = parts.next();
                    int size = Integer.parseInt(parts.next());
                    UnicodeSet uset = new UnicodeSet(parts.next()).freeze();
                    _data.put(script, uset);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            data = ImmutableMap.copyOf(_data);
            comments = _comments.toArray(new String[_comments.size()]);
        }
    }

    /** Called by LikelySubtagsTest.testGetResolvedScriptVsExemplars */
    public static void write(Map<String, UnicodeSet> expected) {
        final File file = new File(CLDRPaths.UTIL_SRC_DATA_DIR, FILE_PATH);
        try (TempPrintWriter out = new TempPrintWriter(file)) {
            // copy all comment lines in the file
            out.println(getCommentBlock());
            // copy all updated sets
            for (Map.Entry<String, UnicodeSet> entry : expected.entrySet()) {
                String script = entry.getKey();
                UnicodeSet flattened = entry.getValue();
                if (!flattened.isEmpty()) {
                    out.println(
                            script
                                    + " ;\t"
                                    + flattened.size()
                                    + " ;\t"
                                    + flattened.toPattern(false));
                }
            }
            System.err.println(
                    "Wrote: "
                            + file.getAbsolutePath()
                            + "\n Please check it carefully and commit it if needed.");
        }
    }
}
