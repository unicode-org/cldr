package org.unicode.cldr.rdf;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRConfig;

import com.google.common.collect.Multimap;

/**
 * Utility to aid in writing .tsv files
 */
public class TsvWriter {
    /**
     * Write a Multimap as a tsv with pairs
     * @param fn filename
     * @param map
     * @param k key column name
     * @param v value column name
     * @throws IOException
     */
    public static void writeTsv(String fn, Multimap<String, String> map, String k, String v) throws IOException {
        System.out.println("Writing " + fn);
        try(PrintWriter w = FileUtilities.openUTF8Writer(getTsvDir(), fn)) {
            writeRow(w, k, v); // header
            map.entries().forEach(e -> writeRow(w, e.getKey(), e.getValue()));
        }
    }

    /**
     * Write a Map as a tsv with pairs
     * @param fn filename
     * @param map
     * @param k key column name
     * @param v value column name
     * @throws IOException
     */
    public static void writeTsv(String fn, Map<String, String> map, String k, String v) throws IOException {
        System.out.println("Writing " + fn);
        try(PrintWriter w = FileUtilities.openUTF8Writer(getTsvDir(), fn)) {
            writeRow(w, k, v); // header
            map.entrySet().forEach(e -> writeRow(w, e.getKey(), e.getValue()));
        }
    }

    public static File getTsvDir() {
        File base = CLDRConfig.getInstance().getCldrBaseDirectory();
        File tsvDir = new File(base, "tools/cldr-rdf/external");
        tsvDir.mkdirs();
        return tsvDir;
    }

    /**
     * Write a TSV row
     * @param w stream to write to
     * @param elements columns to write
     */
    public static void writeRow(PrintWriter w, CharSequence... elements) {
        w.println(String.join("\t", elements));
    }
}
