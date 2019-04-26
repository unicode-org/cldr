/*
 ******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package org.unicode.cldr.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.ArrayComparator;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Log;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;

/**
 * Tool for applying modifications to the CLDR files. Use -h to see the options.
 * <p>
 * There are some environment variables that can be used with the program <br>
 * -DSHOW_FILES=<anything> shows all create/open of files.
 */
public class VettingAdder {

    private Map<String, Set<String>> locale_files = new TreeMap<String, Set<String>>();
    private Comparator<String> scomp = new UTF16.StringComparator();
    private Set<Object[]> conflictSet = new TreeSet<Object[]>(
        new ArrayComparator(new Comparator[] { scomp, scomp, scomp }));

    public VettingAdder(String sourceDirectory) throws IOException {
        addFiles(sourceDirectory);
    }

    private void addFiles(String sourceDirectory) throws IOException {
        File f = new File(sourceDirectory);
        String canonicalName = f.getCanonicalPath();
        if (!f.isDirectory()) {
            String name = f.getName();
            if (name.startsWith("fixed-")) return; // skip
            if (name.equals(".htaccess")) return; // skip
            if (!name.endsWith(".xml")) {
                Log.logln("Wrong filename format: " + f.getCanonicalPath());
                return;
            }
            String localeName = name.substring(0, name.length() - 4);
            Set<String> s = locale_files.get(localeName);
            if (s == null) {
                locale_files.put(localeName, s = new TreeSet<String>());
            }
            s.add(f.getParent());
        } else {
            String[] subnames = f.list();
            for (int i = 0; i < subnames.length; ++i) {
                addFiles(canonicalName + File.separatorChar + subnames[i]);
            }
        }
    }

    static class VettingInfo {
        private String value;
        private String fullPath;
        private String dir;

        public VettingInfo(String dir, String fullPath, String value) {
            this.value = value;
            this.fullPath = fullPath;
            this.dir = dir;
        }

        public String toString() {
            return "source: " + dir + ";\t value: <" + value + ">";
        }

        public int compareByPathAndValue(VettingInfo other) {
            int result;
            if (0 != (result = fullPath.compareTo(other.fullPath))) return result;
            if (0 != (result = value.compareTo(other.value))) return result;
            return 0;
        }
    }

    static Comparator PathAndValueComparator = new Comparator() {
        public int compare(Object o1, Object o2) {
            return ((VettingInfo) o1).compareByPathAndValue((VettingInfo) o2);
        }
    };

    static class VettingInfoSet {
        private Map<String, List<VettingInfo>> path_vettingInfoList = new TreeMap<String, List<VettingInfo>>();

        public void add(String path, String dir, String fullPath, String value) {
            VettingInfo vi = new VettingInfo(dir, fullPath, value);
            List<VettingInfo> s = path_vettingInfoList.get(path);
            if (s == null) path_vettingInfoList.put(path, s = new ArrayList<VettingInfo>(1));
            s.add(vi);
        }

        public Iterator<String> iterator() {
            return path_vettingInfoList.keySet().iterator();
        }

        public Collection<VettingInfo> get(String path) {
            return path_vettingInfoList.get(path);
        }
    }

    public Set<String> keySet() {
        return locale_files.keySet();
    }

    public void incorporateVetting(String locale, String targetDir) throws IOException {
        Set<String> s = locale_files.get(locale);
        Log.logln("Vetting Data for: " + locale);
        VettingInfoSet accum = new VettingInfoSet();
        for (Iterator<String> it2 = s.iterator(); it2.hasNext();) {
            String dir = it2.next() + File.separator;
            String fixedLocale = "fixed-" + locale + ".xml";
            fixXML(dir, locale + ".xml", dir, fixedLocale);
            CLDRFile cldr = SimpleFactory.makeFromFile(dir + fixedLocale, locale, DraftStatus.approved);
            for (Iterator<String> it3 = cldr.iterator(); it3.hasNext();) {
                String path = it3.next();
                String value = cldr.getStringValue(path);
                String fullPath = cldr.getFullXPath(path);
                // skip bogus values
                if (value.startsWith("//ldml") || value.length() == 0) {
                    Log.logln("Skipping: [" + value + "] for " + fullPath);
                    continue;
                }
                accum.add(stripAlt(path), dir, stripAlt(fullPath), value);
            }
        }
        // now walk though items. If there is a single value, keep it
        // otherwise show
        Set<VettingInfo> uniquePathAndValue = new TreeSet<VettingInfo>(PathAndValueComparator);
        CLDRFile cldrDelta = SimpleFactory.makeFile(locale);
        boolean gotOne = false;
        for (Iterator<String> it2 = accum.iterator(); it2.hasNext();) {
            String path = it2.next();
            Collection<VettingInfo> c = accum.get(path);
            uniquePathAndValue.clear();
            uniquePathAndValue.addAll(c);
            if (uniquePathAndValue.size() == 1) { // no conflict
                VettingInfo vi = uniquePathAndValue.iterator().next();
                cldrDelta.add(vi.fullPath, vi.value);
                gotOne = true;
            } else { // there is a conflict
                conflictSet.add(new Object[] { locale, path, c });
            }
        }
        if (gotOne) {
            Log.logln("Writing: " + targetDir + locale + ".xml");
            PrintWriter pw = FileUtilities.openUTF8Writer(targetDir, locale + ".xml");
            cldrDelta.write(pw);
            pw.close();
        } else {
            Log.logln("No data left in: " + targetDir + locale + ".xml");
        }
    }

    public void showSources() {
        for (Iterator<String> it = locale_files.keySet().iterator(); it.hasNext();) {
            String key = it.next();
            Set<String> s = locale_files.get(key);
            for (Iterator<String> it2 = s.iterator(); it2.hasNext();) {
                Log.logln(key + " \t" + it2.next());
                key = "";
            }
        }
    }

    public void fixXML(String inputDir, String inputFile, String outputDir, String outputFile) throws IOException {
        BufferedReader in = FileUtilities.openUTF8Reader(inputDir, inputFile);
        PrintWriter out = FileUtilities.openUTF8Writer(outputDir, outputFile);
        int haveLanguages = 0, haveScripts = 0, haveTerritories = 0, haveVariants = 0, haveKeys = 0, haveTypes = 0;
        int inLocaleDisplayNames = 0;
        while (true) {
            String line = in.readLine();
            if (line == null) break;
            String trimmed = line.trim();

            if (inLocaleDisplayNames == 1) {
                haveLanguages = fixItem(out, haveLanguages, trimmed, "<language ", "languages");
                haveScripts = fixItem(out, haveScripts, trimmed, "<script ", "scripts");
                haveTerritories = fixItem(out, haveTerritories, trimmed, "<territory ", "territories");
                haveVariants = fixItem(out, haveVariants, trimmed, "<variant ", "variants");
                haveKeys = fixItem(out, haveKeys, trimmed, "<key ", "keys");
                haveTypes = fixItem(out, haveTypes, trimmed, "<type ", "types");
            }

            if (trimmed.startsWith("<localeDisplayNames"))
                inLocaleDisplayNames = 1;
            else if (trimmed.startsWith("</localeDisplayNames")) inLocaleDisplayNames = 2;

            out.println(line);
        }
        in.close();
        out.close();
    }

    /**
     *
     */
    private int fixItem(PrintWriter out, int haveLanguages, String trimmed, String item, String fix) {
        if (trimmed.startsWith(item)) {
            if (haveLanguages == 0) {
                out.println("<" + fix + ">");
                haveLanguages = 1;
            }
            return haveLanguages;
        }
        if (haveLanguages == 1) {
            out.println("</" + fix + ">");
            haveLanguages = 2;
        }
        return haveLanguages;
    }

    /**
     * @return Returns the conflictSet.
     */
    public Set<Object[]> getConflictSet() {
        return conflictSet;
    }

    /**
     * @param cldrFactory
     * @throws IOException
     *
     */
    public void showFiles(Factory cldrFactory, String targetDir) throws IOException {
        english = cldrFactory.make("en", true);

        Log.logln("");
        Log.logln("A. Sources");
        Log.logln("");
        showSources();

        Log.logln("");
        Log.logln("B. Intermediate Results");
        Log.logln("");
        Set<String> vettedLocales = keySet();
        for (Iterator<String> it = vettedLocales.iterator(); it.hasNext();) {
            incorporateVetting(it.next(), targetDir);
        }

        Log.logln("");
        Log.logln("C. Conflicts");
        Log.logln("");
        showConflicts(cldrFactory);

        Log.logln("");
        Log.logln("D. Missing Vetting");
        Log.logln("");

        Set<String> availableLocales = new TreeSet<String>(cldrFactory.getAvailable());
        availableLocales.removeAll(vettedLocales);

        for (Iterator<String> it = availableLocales.iterator(); it.hasNext();) {
            String locale = it.next();
            CLDRFile cldr = cldrFactory.make(locale, false);
            for (Iterator<String> it2 = cldr.iterator(); it2.hasNext();) {
                String path = it2.next();
                String fullPath = cldr.getFullXPath(path);
                if (fullPath.indexOf("[@draft=") >= 0) {
                    Log.logln(locale + " \t" + english.getName(locale) + "\texample: " + fullPath);
                    break;
                }
            }
        }
    }

    CLDRFile english;

    /**
     *
     */
    private void showConflicts(Factory cldrFactory) {

        Set<Object[]> s = getConflictSet();
        String lastLocale = "";
        CLDRFile cldr = null;
        Transliterator any_latin = Transliterator.getInstance("any-latin");
        Set<String> emails = new LinkedHashSet<String>();
        String[] pieces = new String[5];

        for (Iterator<Object[]> it = s.iterator(); it.hasNext();) {
            Object[] items = it.next();
            String entry = "";
            if (!lastLocale.equals(items[0])) {
                showSet(emails);
                lastLocale = (String) items[0];
                cldr = cldrFactory.make(lastLocale, false);
                entry = "==========" + Utility.LINE_SEPARATOR + lastLocale + Utility.LINE_SEPARATOR;
            }
            String path = CLDRFile.getDistinguishingXPath((String) items[1], null, false);
            String current = cldr.getStringValue(path);
            entry += "\tpath:\t" + path + Utility.LINE_SEPARATOR + "\tcurrent value:\t" + getValue(any_latin, current)
                + Utility.LINE_SEPARATOR;

            entry += "\tEnglish value:\t" + getValue(any_latin, english.getStringValue(path)) + Utility.LINE_SEPARATOR;
            Collection<VettingInfo> c = (Collection<VettingInfo>) items[2];
            for (Iterator<VettingInfo> it2 = c.iterator(); it2.hasNext();) {
                VettingInfo vi = it2.next();
                entry += "\t\tvalue:\t" + getValue(any_latin, vi.value) + "\t source: " + vi.dir
                    + Utility.LINE_SEPARATOR;
                // get third field, that's the email
                Utility.split(vi.dir, '\\', pieces);
                emails.add(pieces[2]);
            }

            if (false) {
                System.out.println("path: " + path);
                for (int i = 0; i < items.length; ++i) {
                    System.out.println("item[" + i + "]: " + items[i]);
                }
            }
            Log.logln(entry);
        }
        showSet(emails);
    }

    /**
     *
     */
    private void showSet(Set<String> emails) {
        if (emails.size() == 0) return;
        String result = "Emails:\t";
        for (Iterator<String> it = emails.iterator(); it.hasNext();) {
            result += it.next() + ", ";
        }
        result += "cldr@unicode.org";
        emails.clear();
        Log.logln(result);
    }

    /**
     *
     */
    private String getValue(Transliterator some, String current) {
        if (current == null) current = "NULL";
        String other = some.transliterate(current);
        return "<" + current + ">" + (other.equals(current) ? "" : "\t[" + other + "]");
    }

    XPathParts tempParts = new XPathParts(null, null);

    /**
     *
     */
    private String stripAlt(String path) {
        XPathParts tempParts = XPathParts.getTestInstance(path);
        Map<String, String> x = tempParts.getAttributes(tempParts.size() - 1);
        String value = x.get("alt");
        if (value != null && value.startsWith("proposed")) {
            x.remove("alt");
            // System.out.println(path + "\t=>\t" + tempParts.toString());
            return tempParts.toString();
        }
        return path;
    }
}