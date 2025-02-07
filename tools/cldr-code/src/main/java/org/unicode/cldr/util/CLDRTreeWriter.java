package org.unicode.cldr.util;

import java.io.*;
import java.util.*;
import org.unicode.cldr.draft.FileUtilities;

/** Utility for writing a 'tree' of CLDR files */
public class CLDRTreeWriter implements AutoCloseable {
    private final Set<CLDRLocale> locales = new TreeSet<>();
    private final Set<CLDRLocale> removed = new TreeSet<>();
    private final String path;

    /**
     * @param path root of files to write
     */
    public CLDRTreeWriter(String forPath) {
        path = forPath;
    }

    /** mark a file as written */
    public void write(CLDRFile f) throws IOException {
        final String locale = f.getLocaleID();
        final CLDRLocale loc = CLDRLocale.getInstance(locale);
        try (PrintWriter pw = FileUtilities.openUTF8Writer(path, locale + ".xml")) {
            f.write(pw);
            locales.add(loc);
            removed.remove(loc);
            System.out.println("# Wrote: " + path + "/" + locale + ".xml");
        }
    }

    /** remove a file */
    public void delete(String locale) {
        delete(CLDRLocale.getInstance(locale));
    }

    public File getFile(CLDRLocale locale) {
        return new File(path, locale + ".xml");
    }

    /** remove a file */
    public void delete(CLDRLocale locale) {
        locales.remove(locale);
        removed.add(locale);
        System.out.println("# Removed: " + path + "/" + locale + ".xml");
    }

    @Override
    public void close() throws IOException {
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
                "# for " + path + " - wrote " + locales.size() + " and removed " + removed.size());
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
