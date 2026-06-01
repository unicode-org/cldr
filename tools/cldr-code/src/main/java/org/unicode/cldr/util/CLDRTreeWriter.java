package org.unicode.cldr.util;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Utility for writing a 'tree' of CLDR files */
public class CLDRTreeWriter implements AutoCloseable {
    private final Set<CLDRLocale> locales = ConcurrentHashMap.newKeySet();
    private final Set<CLDRLocale> removed = ConcurrentHashMap.newKeySet();
    private final File pathFile;

    private final Queue<CLDRFile> deferWrite = new ConcurrentLinkedQueue<>();

    /** if true, fixup missing stub files (ie. kk_CN -> kk) */
    private boolean writeMissingParents = true;

    /** if called, don't try to fixup missing parent */
    public void skipMissingParents() {
        writeMissingParents = false;
    }

    /**
     * @param path root of files to write
     */
    public CLDRTreeWriter(String forPath) {
        pathFile = new File(forPath);
    }

    /** mark a file as written */
    public void write(CLDRFile f) throws IOException {
        internalWrite(f);
        internalMarkWritten(f);
    }

    private void internalMarkWritten(CLDRLocale loc) {
        locales.add(loc);
        removed.remove(loc);
    }

    private void internalMarkWritten(CLDRFile f) {
        final String locale = f.getLocaleID();
        final CLDRLocale loc = CLDRLocale.getInstance(locale);
        internalMarkWritten(loc);
    }

    private void internalWrite(CLDRFile f) throws IOException {
        final String locale = f.getLocaleID();
        try (TempPrintWriter pw =
                TempPrintWriter.openUTF8Writer(pathFile, locale + ".xml")
                        .skipCopyright(true)
                        .noDiff()) {
            f.write(pw.asPrintWriter());
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
        // do this in parallel so we can maximize the IO
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
        if (writeMissingParents) {
            for (final CLDRLocale locale : locales) {
                ensureParentExists(locale.getParent(), missingParents);
            }
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
