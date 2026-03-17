package org.unicode.cldr.util;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Utility for writing a 'tree' of CLDR files */
public class CLDRTreeWriter implements AutoCloseable {
    private final Set<CLDRLocale> locales = new TreeSet<>();
    private final Set<CLDRLocale> removed = new TreeSet<>();
    private final File pathFile;

    private final List<CLDRFile> deferWrite = new LinkedList<>();

    /**
     * @param path root of files to write
     */
    public CLDRTreeWriter(String forPath) {
        pathFile = new File(forPath);
    }

    /** mark a file as written */
    public void write(CLDRFile f) throws IOException {
        final String locale = f.getLocaleID();
        final CLDRLocale loc = CLDRLocale.getInstance(locale);
        try (TempPrintWriter pw =
                TempPrintWriter.openUTF8Writer(pathFile, locale + ".xml")
                        .skipCopyright(true)
                        .noDiff()) {
            f.write(pw.asPrintWriter());
            locales.add(loc);
            removed.remove(loc);
            // System.out.println("# Wrote: " + pw.filename);
        }
    }

    public void deferWrite(CLDRFile f) {
        deferWrite.add(f.cloneAsThawed());
    }

    /** remove a file */
    public void delete(String locale) {
        delete(CLDRLocale.getInstance(locale));
    }

    public File getFile(CLDRLocale locale) {
        return new File(pathFile, locale + ".xml");
    }

    /** remove a file */
    public void delete(CLDRLocale locale) {
        locales.remove(locale);
        removed.add(locale);
        System.out.println("# Removed: " + pathFile.getPath() + "/" + locale + ".xml");
    }

    @Override
    public void close() throws IOException {
        // write any deferred items
        final Map<String, IOException> errs = new ConcurrentHashMap<>();
        deferWrite.stream()
                .parallel()
                .forEach(
                        f -> {
                            try {
                                write(f);
                            } catch (IOException e) {
                                errs.put(f.getLocaleID(), e);
                            }
                        });

        // throw any IOExceptions (first will fail)
        for (final IOException e : errs.values()) {
            throw e;
        }

        Set<CLDRLocale> missingParents = new TreeSet<CLDRLocale>();
        // collect missing parents
        for (final CLDRLocale locale : locales) {
            ensureParentExists(locale.getParent(), missingParents);
        }
        // effect any additions
        for (final CLDRLocale locale : missingParents) {
            CLDRFile wasMissing = new CLDRFile(new SimpleXMLSource(locale.getBaseName()));
            System.out.println("# Writing missing parent: " + getFile(locale).getPath());
            write(wasMissing); // no longer missing.
        }
        // effect any removals
        for (final CLDRLocale locale : removed) {
            getFile(locale).deleteOnExit();
        }
        System.out.println(
                "# for "
                        + pathFile
                        + " - wrote "
                        + locales.size()
                        + " and removed "
                        + removed.size());
    }

    /** recursively add any missing files. */
    private void ensureParentExists(CLDRLocale locale, Set<CLDRLocale> missingParents)
            throws IOException {
        if (locale == null || locales.contains(locale)) return;
        if (getFile(locale).canRead() && !removed.contains(locale)) return; // parent file exists.
        // else missing, need to write
        missingParents.add(locale);
        ensureParentExists(locale.getParent(), missingParents);
    }
}
