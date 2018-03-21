package org.unicode.cldr.icu;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.FileCopier;
import org.unicode.cldr.util.PatternCache;

import com.google.common.collect.ImmutableMap;
import com.ibm.icu.util.Calendar;

/**
 * Stores the content of a makefile and writes it to disk.
 * @author jchye
 */
class Makefile {
    private static final Pattern VARIABLE = PatternCache.get("\\$\\([\\w_]++\\)");

    private String prefix;
    private List<MakefileEntry> entries = new ArrayList<MakefileEntry>();

    private static final Comparator<String> valueComparator = new Comparator<String>() {
        @Override
        public int compare(String arg0, String arg1) {
            return arg0.compareTo(arg1);
        }
    };

    class MakefileEntry {
        String name;
        String comment;
        List<String> values = new ArrayList<String>();

        public MakefileEntry(String name, String comment) {
            this.name = name;
            this.comment = comment;
        }
    }

    public Makefile(String prefix) {
        this.prefix = prefix;
    }

    public MakefileEntry addEntry(String name, String comment) {
        MakefileEntry entry = new MakefileEntry(name, comment);
        entries.add(entry);
        return entry;
    }

    public MakefileEntry addEntry(String name, String comment, Collection<String> values) {
        MakefileEntry entry = addEntry(name, comment);
        entry.values.addAll(values);
        Collections.sort(entry.values, valueComparator);
        return entry;
    }

    public void addSyntheticAlias(Collection<String> aliases) {
        addEntry(prefix + "_SYNTHETIC_ALIAS",
            "Aliases without a corresponding xx.xml file (see icu-config.xml & build.xml)",
            aliases);
    }

    public void addAliasSource() {
        MakefileEntry entry = addEntry(prefix + "_ALIAS_SOURCE",
            "All aliases (to not be included under 'installed'), but not including root.");
        entry.values.add("$(" + prefix + "_SYNTHETIC_ALIAS)");
    }

    public void addSource(Collection<String> sources) {
        addEntry(prefix + "_SOURCE", "Ordinary resources", sources);
    }

    public void print(String outputDir, String filename) throws IOException {
        PrintWriter out = FileUtilities.openUTF8Writer(outputDir, filename);
        ImmutableMap<String, String> params = ImmutableMap.of(
            "%year%", String.valueOf(Calendar.getInstance().get(Calendar.YEAR)),
            "%prefix%", prefix,
            "%local%", filename.replace("files.mk", "local.mk"),
            "%version%", CLDRFile.GEN_VERSION);

        FileCopier.copyAndReplace(NewLdml2IcuConverter.class, "makefile_header.txt",
            Charset.forName("UTF-8"),
            params,
            out);

        for (MakefileEntry entry : entries) {
            out.println();
            out.append("# ").append(entry.comment).println();
            out.append(entry.name).append(" =");
            int lineCount = 0;
            for (String value : entry.values) {
                if (value.equals("root")) continue;
                if (lineCount == 4) {
                    out.append('\\').println();
                }
                out.append(' ').append(value);
                if (!VARIABLE.matcher(value).matches()) {
                    out.append(".txt");
                }
                lineCount = (lineCount + 1) % 5;
            }
            out.println();
            out.println();
        }
        out.close();
    }
}