package org.unicode.cldr.util;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.ibm.icu.text.UnicodeSet;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class ScriptToExemplars {
    public static UnicodeSet getExemplars(String script) {
        return ScriptToExemplarsLoader.SINGLETON.data.get(script);
    }

    private static class ScriptToExemplarsLoader {
        private static final ScriptToExemplarsLoader SINGLETON = new ScriptToExemplarsLoader();
        private Map<String, UnicodeSet> data;

        {
            Map<String, UnicodeSet> _data = Maps.newTreeMap();
            try (BufferedReader reader =
                    FileReaders.openFile(
                            ScriptToExemplars.class, "data/locales/scriptToExemplars.txt")) {
                Iterable<String> rlsi =
                        With.toIterable(new FileReaders.ReadLineSimpleIterator(reader));
                for (String line : rlsi) {
                    if (line.isBlank() || line.startsWith("#")) {
                        continue;
                    }
                    Iterator<String> parts = Splitter.on(';').trimResults().split(line).iterator();
                    _data.put(parts.next(), new UnicodeSet(parts.next()).freeze());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            data = ImmutableMap.copyOf(_data);
        }
    }
}
