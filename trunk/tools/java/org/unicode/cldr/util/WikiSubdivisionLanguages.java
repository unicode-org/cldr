package org.unicode.cldr.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.test.DisplayAndInputProcessor;
import org.unicode.cldr.tool.SubdivisionNode;
import org.unicode.cldr.util.CLDRFile.NumberingSystem;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.ChainedMap.M4;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.Validity.Status;

import com.google.common.base.Splitter;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.ULocale;

public final class WikiSubdivisionLanguages {
    static final SupplementalDataInfo SDI = SupplementalDataInfo.getInstance();
    static final Set<String> regularSubdivisions = Validity.getInstance().getStatusToCodes(LstrType.subdivision).get(Status.regular);

    static final Map<String, R2<List<String>, String>> SUBDIVISION_ALIASES = SDI.getLocaleAliasInfo().get("subdivision");

    private static final boolean DEBUG_CONSOLE = false;
    private static final String DEBUG_LANG_FILTER = null; // "az";

    private static final String BEFORE_TYPE = "//ldml/localeDisplayNames/subdivisions/subdivision[@type=\"";

    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    private static final Normalizer2 NFC = Normalizer2.getNFCInstance();

    enum Items {
        // http://www.wikidata.org/entity/Q24260    كانيلو  AD-02   ar
        wid, translation, subdivisionId, languageId
    }

    private static ChainedMap.M3<String, String, String> SUB_LANG_NAME = ChainedMap.of(new TreeMap<String, Object>(), new TreeMap<String, Object>(),
        String.class);
    private static ChainedMap.M3<String, String, String> LANG_SUB_NAME = ChainedMap.of(new TreeMap<String, Object>(), new TreeMap<String, Object>(),
        String.class);
    private static Set<String> bogus = new TreeSet<>();
    private static Multimap<Status, String> bogusStatus = TreeMultimap.create();

    public static String getSubdivisionName(String subdivisionId, String languageId) {
        return WikiSubdivisionLanguages.LANG_SUB_NAME.get(languageId, subdivisionId);
    }

    public static String getBestWikiEnglishName(String subdivisionId) {
        String languageId = "en";
        String name = WikiSubdivisionLanguages.getSubdivisionName(subdivisionId, languageId);
        if (name != null) {
            return name;
        }
        name = WikiSubdivisionLanguages.getSubdivisionName(subdivisionId, "es");
        if (name != null) {
            return name;
        }
        name = WikiSubdivisionLanguages.getSubdivisionName(subdivisionId, "fr");
        if (name != null) {
            return name;
        }
        Map<String, String> data = WikiSubdivisionLanguages.SUB_LANG_NAME.get(subdivisionId);
        // try Spanish, then French, then first other
        if (data != null) {
            return data.entrySet().iterator().next().getValue(); // get first
        }
        return null;
    }

    //static Map<String, String> WIKIDATA_TO_MID = new TreeMap<>();
    static {
        Splitter TAB = Splitter.on('\t').trimResults();
        File file = new File("data/external", "wikiSubdivisionLanguages.tsv");
        try {
            System.out.println(file.getCanonicalFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map<String, Status> codeToStatus = Validity.getInstance().getCodeToStatus(LstrType.subdivision);

        for (String line : FileUtilities.in(WikiSubdivisionLanguages.class, "data/external/wikiSubdivisionLanguages.tsv")) {

            List<String> data = TAB.splitToList(line);
            String subdivision = SubdivisionNode.convertToCldr(data.get(Items.subdivisionId.ordinal()));
            if (!regularSubdivisions.contains(subdivision)) {
                Status status = codeToStatus.get(subdivision);
                if (status == null) {
                    bogus.add(subdivision);
                } else {
                    bogusStatus.put(status, subdivision);
                }
                continue;
            }
            String lang = data.get(Items.languageId.ordinal());
            if (DEBUG_LANG_FILTER != null && !DEBUG_LANG_FILTER.equals(lang)) {
                continue;
            }
            String name = NFC.normalize(data.get(Items.translation.ordinal()));
            SUB_LANG_NAME.put(subdivision, lang, name);
//                WIKIDATA_TO_MID.put(subdivision, data.get(2));
            LANG_SUB_NAME.put(lang, subdivision, name);
        }
        // postprocess
        String oldLang = null;
        DisplayAndInputProcessor daip = null;
        Exception[] internalException = { null };

        for (R3<String, String, String> row : LANG_SUB_NAME.rows()) {
            String lang = row.get0();
            String subdivision = row.get1();
            String name = row.get2();
            if (!lang.equals(oldLang)) {
                oldLang = lang;
                daip = new DisplayAndInputProcessor(new ULocale(lang));
            }
            String path = getSubdivisionPath(subdivision);
            String name2 = daip.processInput(
                path,
                name.replace("\u00AD", ""),
                internalException);
            if (name2.contains("'")) {
                int debug = 0;
            }
            // TODO remove soft hyphen in DAIP
            if (internalException[0] != null) {
                throw new IllegalArgumentException(lang + "\t" + subdivision + "\t" + name, internalException[0]);
            } else if (!name.equals(name2)) {
                //System.out.println(lang + "\t" + subdivision + "\t" + name + "\t" + name2);
                SUB_LANG_NAME.put(subdivision, lang, name2);
                LANG_SUB_NAME.put(lang, subdivision, name2);
            }
        }

    }

    private static String getSubdivisionPath(String subdivision) {
        return BEFORE_TYPE + subdivision + "\"][@draft=\"contributed\"]";
    }

    private static String getSubdivisionFromPath(String path) {
        return path.substring(BEFORE_TYPE.length(), path.indexOf('"', BEFORE_TYPE.length()));
    }

    public static void main(String[] args) {
        Counter<String> counter = new Counter<>();
        Factory cldrFactory = CLDR_CONFIG.getCldrFactory();
        Factory cldrFactorySubdivisions = Factory.make(CLDRPaths.SUBDIVISIONS_DIRECTORY, ".*");
        CLDRFile file = null;
        UnicodeSet exemplars = null;

        ChainedMap.M4<Integer, String, String, String> exemplarFailureLangSubdivisionName = ChainedMap.of(
            new TreeMap<Integer, Object>(),
            new TreeMap<String, Object>(),
            new TreeMap<String, Object>(),
            String.class);

        for (Entry<String, Map<String, String>> entry : LANG_SUB_NAME) {
            String lang = entry.getKey();
            file = cldrFactory.make(lang, true);

            CLDRFile oldFileSubdivisions;
            try {
                oldFileSubdivisions = cldrFactorySubdivisions.make(lang, false);
            } catch (Exception e) {
                oldFileSubdivisions = new CLDRFile(new SimpleXMLSource(lang)).freeze();
            }

            Multimap<String, String> inverse = LinkedHashMultimap.create();
            CLDRFile fileSubdivisions = fixedFile(oldFileSubdivisions, inverse);

            UnicodeSet main = file.getExemplarSet("", WinningChoice.WINNING, 0);
            UnicodeSet auxiliary = file.getExemplarSet("auxiliary", WinningChoice.WINNING);
            UnicodeSet punctuation = file.getExemplarSet("punctuation", WinningChoice.WINNING);
            UnicodeSet numbers = file.getExemplarsNumeric(NumberingSystem.defaultSystem);
            exemplars = new UnicodeSet()
                .addAll(main)
                .addAll(auxiliary)
                .addAll(scriptsFor(main)) // broad test,...
                .addAll(punctuation)
                .addAll(numbers)
                .addAll(new UnicodeSet("[\\ ]")).freeze();

            for (Entry<String, String> entry2 : entry.getValue().entrySet()) {
                String subdivision = entry2.getKey();
                String name = entry2.getValue();
                if (name.equals("Böyük Britaniya")) {
                    int debug = 0;
                }
                String path = getSubdivisionPath(subdivision);
                String oldName = fileSubdivisions.getStringValue(path);
                if (oldName != null) {
                    if (!oldName.equals(name)) {
                        //System.out.println("Already has translation\t" + lang + "\t" + subdivision + "\t" + name + "\t" + oldName);
                    }
                    continue;
                }
                if (!exemplars.containsAll(name)) {
                    UnicodeSet exemplarFailures = new UnicodeSet().addAll(name).removeAll(exemplars);
                    addExemplarFailures(exemplarFailureLangSubdivisionName, exemplarFailures, lang, subdivision, name);
                    continue;
                }
                fileSubdivisions.add(path, name);
                inverse.put(name, path);
                counter.add(lang, 1);
            }

            // We now fix collisions
            for (Entry<String, Collection<String>> entry3 : inverse.asMap().entrySet()) {
                String name = entry3.getKey();
                if (name.isEmpty()) {
                    continue;
                }
                if (name.equals("Böyük Britaniya")) {
                    int debug = 0;
                }
                Collection<String> paths = entry3.getValue();
                if (paths.size() <= 1) {
                    continue;
                }
                if (paths.size() > 3) {
                    int debug = 0;
                }
                // we only care about collisions *within* a region.
                // so group them together
                Multimap<String, String> regionToPaths = LinkedHashMultimap.create();
                for (String path : paths) {
                    String sdId = getSubdivisionFromPath(path);
                    String region = sdId.substring(0, 2).toUpperCase(Locale.ROOT);
                    regionToPaths.put(region, path);
                }

                // Now fix as necessary
                for (Entry<String, Collection<String>> regionAndPaths : regionToPaths.asMap().entrySet()) {
                    Collection<String> paths2 = regionAndPaths.getValue();
                    int markerIndex = 0;
                    if (paths2.size() <= 1) {
                        continue;
                    }

                    // find if any of the paths are deprecated
                    for (Iterator<String> it = paths2.iterator(); it.hasNext();) {
                        String path = it.next();
                        String sdId = getSubdivisionFromPath(path);
                        if (!regularSubdivisions.contains(sdId)) { // deprecated
                            fileSubdivisions.remove(path);
                            it.remove();
                            fail("Duplicate, not regular ", lang, getSubdivisionFromPath(path), "REMOVING", -1);
                        }
                    }
                    if (paths2.size() <= 1) {
                        continue;
                    }

                    String otherId = null;
                    for (String path : paths2) {
//                    if (nuke) {
//                        if (oldFileSubdivisions.getStringValue(path) == null) {
//                            fileSubdivisions.remove(path); // get rid of new ones
//                            System.out.println("Removing colliding " + lang + "\t" + path + "\t" + name);
//                        }
                        if (markerIndex == 0) {
                            otherId = getSubdivisionFromPath(path);
                        } else {
                            String fixedName = name + MARKERS.get(markerIndex);
                            fail("Superscripting ", lang + "\t(" + otherId +")", getSubdivisionFromPath(path), fixedName, -1);
                            //System.out.println("Superscripting colliding:\t" + lang + "\t" + path + "\t" + fixedName);
                            fileSubdivisions.add(path, fixedName); // overwrite with superscripted
                        }
                        ++markerIndex;
                    }
                }
            }

            if (DEBUG_CONSOLE) {
                PrintWriter pw = new PrintWriter(System.out);
                fileSubdivisions.write(new PrintWriter(System.out));
                pw.flush();
            } else {
                try (PrintWriter out = FileUtilities.openUTF8Writer(CLDRPaths.SUBDIVISIONS_DIRECTORY, lang + ".xml")) {
                    fileSubdivisions.write(out);
                } catch (Exception e) {
                    throw new ICUUncheckedIOException(e);
                }
            }
        }
        fail("ExemplarFailures", exemplarFailureLangSubdivisionName);

        for (String lang : counter.getKeysetSortedByKey()) {
            fail("Superscripting", lang, String.valueOf(counter.get(lang)), null, -1);
        }
        System.out.println("Bogus subdivisionIds:\t" + "*" + "\t" + bogus.size() + "\t" + bogus);
        for (Entry<Status, Collection<String>> entry : bogusStatus.asMap().entrySet()) {
            System.out.println("SubdivisionId:\t\t"
                + ":\t" + entry.getKey() + "\t" + entry.getValue().size() + "\t" + entry.getValue());
        }
    }

    private static CLDRFile fixedFile(CLDRFile oldFileSubdivisions, Multimap<String, String> inverse) {
        CLDRFile fileSubdivisions = oldFileSubdivisions.cloneAsThawed();

        // for fixing collisions
        // we first add existing items
        Set<String> toRemove = new HashSet<>();
        Map<String,String> toAdd = new HashMap<>();

        for (String path : fileSubdivisions) {
            XPathParts parts = XPathParts.getFrozenInstance(path);
            if (!"subdivision".equals(parts.getElement(-1))) {
                continue;
            }
            String name = fileSubdivisions.getStringValue(path);
            if (name.equals("Böyük Britaniya")) {
                int debug = 0;
            }
            // handle aliases also
            String type = parts.getAttributeValue(-1, "type");
            R2<List<String>, String> replacement = SUBDIVISION_ALIASES.get(type);
            if (replacement != null) {
                String fullPath = oldFileSubdivisions.getFullXPath(path);
                XPathParts parts2 = XPathParts.getInstance(fullPath);
                for (String replacementType : replacement.get0()) {
                    parts2.setAttribute(-1, "type", replacementType);
                    toRemove.add(path);
                    path = parts2.toString();
                    toAdd.put(path, name);
                    System.out.println("Adding alias: " + replacementType + "«" + name + "»");
                    break;
                }
            }
            inverse.put(name, path);
        }
        fileSubdivisions.removeAll(toRemove, false);
        for (Entry<String, String> entry2 : toAdd.entrySet()) {
            fileSubdivisions.add(entry2.getKey(), entry2.getValue());
        }
        return fileSubdivisions;
    }

    private static void addExemplarFailures(M4<Integer, String, String, String> exemplarFailureLangSubdivisionName, UnicodeSet exemplarFailures,
        String language, String subdivision, String name) {
        for (String s : exemplarFailures) {
            exemplarFailureLangSubdivisionName.put(s.codePointAt(0), language, subdivision, name);
        }
    }

    private static void fail(String title, M4<Integer, String, String, String> exemplarFailureLangSubdivisionName) {
        for (R4<Integer, String, String, String> entry : exemplarFailureLangSubdivisionName.rows()) {
            fail(title, entry.get1(), entry.get2(), entry.get3(), entry.get0());
        }
    }

    private static void fail(String title, String lang, String subdivision, String name, int exemplarFailure) {
        System.out.println(title
            + ":\t" + lang
            + "\t" + subdivision
            + "\t" + (exemplarFailure < 0 ? "" : "«" + UTF16.valueOf(exemplarFailure) + "»")
            + "\t" + (exemplarFailure < 0 ? "" : "U+" + Utility.hex(exemplarFailure))
            + "\t" + CldrUtility.ifNull(getBestWikiEnglishName(subdivision), "")
            + "\t" + CldrUtility.ifNull(name, "").replace("\"", "&quot;"));
    }

    static final List<String> MARKERS = Arrays.asList("¹", "²", "³"); // if there are more than 3 of the same kind, throw exception

    private static UnicodeSet scriptsFor(UnicodeSet main) {
        UnicodeSet result = UnicodeSet.EMPTY;
        for (String s : main) {
            int scriptCode = UScript.getScript(s.codePointAt(0));
            if (scriptCode != UScript.COMMON || scriptCode != UScript.INHERITED) {
                result = new UnicodeSet().applyIntPropertyValue(UProperty.SCRIPT, scriptCode);
                if (scriptCode == UScript.LATIN) {
                    result.addAll("ʻ’&");
                }
                break;
            }
        }
        return result;
    }
}