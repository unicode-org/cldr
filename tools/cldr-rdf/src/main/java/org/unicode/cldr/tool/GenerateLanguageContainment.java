package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.util.ICUUncheckedIOException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.rdf.QueryClient;
import org.unicode.cldr.rdf.TsvWriter;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Containment;
import org.unicode.cldr.util.DiffLanguageGroups;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Iso639Data;
import org.unicode.cldr.util.Iso639Data.Type;
import org.unicode.cldr.util.LocaleNames;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StandardCodes.LstrField;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;

/**
 * This code generates language group containment based on Wikidata. For example, it finds: root >
 * Indo-European [Other] (ine) > Germanic [Other] (gem) > West Germanic languages (gmw) > English
 * (en)
 *
 * <p>To do this, it reads three tables from Wikidata, and combines them. The combination is not
 * trivial, because wikidata offers multiple "parents" for the same language, and many of the
 * parents do not have ISO codes. For the first problem, the software computes the possible parent
 * chains and picks among them. For the second problem, any parents without ISO codes are skipped
 * (after forming the chains, so the ultimate ancestors are still found). <br>
 * A number of debugging files are written to the external directory.
 *
 * <p>Some failures will be exposed by running this tool. Examples: <br>
 * <b>wikidata-entityToCode Multiple values:</b> Cebaara [Q1097512] [sef, sev]. <br>
 * If these are not CLDR languages then they do not need to be fixed. <br>
 * <b>wikidata-childToParent Multiple values:</b> Q118712 [Q118712] [German [de, Q18], English [en,
 * Q186]] <br>
 * Normally these don't need to be fixed; the generation code works around them. <br>
 * <b>Cycle in [dng, zhx]</b> from [[http://www.wikidata.org/entity/Q33050, <br>
 * These indicate that the Wikidata has a cycle in it. A => B => C => A. Ignore these unless the
 * cases are worth investigating.
 *
 * <p>Others are exposed by running TestLanguageGroup.java <br>
 * Error: (TestLanguageGroup.java:55) Single ancestor but not in ISOLATES: ce [Chechen] [ce] <br>
 * Check to see if the language has a language group (in this case not, so add to
 * TestLanguageGroup.ISOLATEs). <br>
 * For kea [Kabuverdianu] [kea], you can add cpp as the parent, as follows. <br>
 * <b>Missing.</b> If a child-parent relation is missing, you can add it to EXTRA_PARENT_CHILDREN so
 * that it shows up. For example, .put("gmw", "lb") says that West Germanic is the parent of
 * Luxembourgish. <br>
 * <b>Extra.</b> Sometimes wikidata has conflicting or erroneous entries. Those can be fixed by
 * adding to REMOVE_PARENT_CHILDREN. Use * to remove all children, such as .put("crp", "*") <br>
 * Sometimes the tool fails with JsonParseExceptions, but works if you rerun. <br>
 * Cycle in [dng, zhx] from ... Will be fixed by giving the language 'no parent' (mul)
 *
 * <p>
 */
public class GenerateLanguageContainment {
    static {
        System.out.println(
                "See the class description for GenerateLanguageContainment.java about fixing problems.");
    }

    private static final boolean ONLY_LIVING = false;
    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    private static final QueryClient queryClient = QueryClient.getInstance();

    static final Splitter TAB = Splitter.on('\t').trimResults();
    private static final Joiner JOIN_TAB = Joiner.on('\t');
    static final CLDRFile ENGLISH = CONFIG.getEnglish();
    static final String relDir = "../util/data/languages/";
    static final Map<String, R2<List<String>, String>> ALIAS_MAP =
            CONFIG.getSupplementalDataInfo().getLocaleAliasInfo().get("language");

    /** We load the SparQL queries using this helper object, to be able to catch exceptions… */
    static final class QueryHelper {
        public final Map<String, String> entityToLabel;
        public final Map<String, String> entityToCode;
        public final ImmutableMultimap<String, String> codeToEntity;
        public final Multimap<String, String> childToParent;

        QueryHelper() {
            try {
                entityToLabel =
                        loadQueryPairsUnique(
                                GenerateLanguageContainment.class,
                                "wikidata-entityToLabel",
                                null,
                                null,
                                null);

                entityToCode =
                        loadQueryPairsUnique(
                                GenerateLanguageContainment.class,
                                "wikidata-entityToCode",
                                code -> {
                                    code = code.replace("\"", "");
                                    R2<List<String>, String> v = ALIAS_MAP.get(code);
                                    String result = v == null ? code : v.get0().get(0);
                                    result = result.contains("_") ? code : result;
                                    return result;
                                },
                                code -> showNameAndCode(code),
                                NAME);

                codeToEntity =
                        ImmutableMultimap.copyOf(
                                Multimaps.invertFrom(
                                        Multimaps.forMap(entityToCode),
                                        LinkedHashMultimap.create()));

                childToParent =
                        loadQueryPairs(
                                GenerateLanguageContainment.class,
                                "wikidata-childToParent",
                                code -> showNameAndCode(code),
                                code -> showNameAndCode(code));

            } catch (Throwable t) {
                t.printStackTrace();
                throw new RuntimeException(t);
            }
        }

        String getEntityName(String key) {
            String code = getEntityCode(key);
            if (code != null) {
                try {
                    String name = NAME.apply(code);
                    if (name != null) {
                        return name;
                    }
                } catch (Exception e) {
                    // TODO: Why would NAME.apply throw?
                    // TODO: Need better handling here?
                }
            }
            String name = entityToLabel.get(key);
            if (name != null) {
                return name;
            }
            return afterLastSlash(key);
        }

        private String getEntityCode(String key) {
            return entityToCode == null ? null : entityToCode.get(key);
        }

        private String afterLastSlash(String key) {
            return key.substring(key.lastIndexOf('/') + 1, key.length() - 1);
        }

        public void writeTsvs() throws IOException {
            TsvWriter.writeTsv("childToParent.tsv", childToParent, "child", "parent");
            TsvWriter.writeTsv("entityToCode.tsv", entityToCode, "lang", "langCode");
            TsvWriter.writeTsv("entityToLabel.tsv", entityToLabel, "lang", "langLabel");
            SortedSetMultimap<String, String> childToParentWithCodes = TreeMultimap.create();
            for (Entry<String, String> entry : childToParent.entries()) {
                String child = entry.getKey();
                String parent = entry.getValue();
                childToParentWithCodes.put(showNameAndCode(child), showNameAndCode(parent));
            }
            TsvWriter.writeTsv(
                    "childToParentWithCodes.tsv",
                    childToParentWithCodes,
                    "childCode\tLabel",
                    "parentCode\tLabel");
        }

        public String showNameAndCode(String qid) {
            return getEntityName(qid)
                    + " ("
                    + (getEntityCode(qid) == null ? "" : getEntityCode(qid) + ", ")
                    + afterLastSlash(qid)
                    + ")";
        }

        public <T extends Iterable<String>> String showNameAndCode(T qids) {
            StringBuilder b = new StringBuilder();
            qids.forEach(
                    qid -> {
                        if (b.length() != 0) b.append(", ");
                        b.append(showNameAndCode(qid));
                    });
            return b.toString();
        }

        public <T extends Iterable<String>, U extends Iterable<T>> String showNameAndCode2(U qids) {
            StringBuilder b = new StringBuilder();
            qids.forEach(
                    qid -> {
                        if (b.length() != 0) b.append("; ");
                        b.append(showNameAndCode(qid));
                    });
            return b.toString();
        }
    }

    static final QueryHelper QUERY_HELPER = new QueryHelper();

    static final Function<String, String> NAME =
            code ->
                    code.equals(LocaleNames.MUL)
                            ? LocaleNames.ROOT
                            : ENGLISH.getName(code) + " (" + code + ")";

    static final Set<String> COLLECTIONS;

    static {
        Map<String, Map<LstrField, String>> languages =
                StandardCodes.getEnumLstreg().get(LstrType.language);
        Builder<String> _collections = ImmutableSet.<String>builder();
        for (Entry<String, Map<LstrField, String>> e : languages.entrySet()) {
            String scope = e.getValue().get(LstrField.Scope);
            if (scope != null && "Collection".equalsIgnoreCase(scope)) {
                _collections.add(e.getKey());
            }
        }
        COLLECTIONS = _collections.build();
    }

    static class Tree {
        Set<String> leaves = new LinkedHashSet<>();

        void add(List<String> chain) {
            Collections.reverse(chain);
        }
    }

    /** To add parent-child relations to Wikidata */
    static final Multimap<String, String> RESET_PARENT_CHILDREN =
            ImmutableMultimap.<String, String>builder()
                    .put(LocaleNames.MUL, LocaleNames.UND) // anomaly
                    .put(LocaleNames.MUL, "art") // no containing language family
                    .put(LocaleNames.MUL, "euq") // no containing language family
                    .put(LocaleNames.MUL, "jpx") // no containing language family
                    .put(LocaleNames.MUL, "tai") // no containing language family
                    .put(
                            LocaleNames.MUL,
                            "ko") // no containing language family (Altaic is too controversial)
                    .put(LocaleNames.MUL, "crp") // no containing language family
                    .put(LocaleNames.MUL, "kgp") // no containing language family
                    .put("alv", "agq")
                    .put("alv", "cch") // Atlantic–Congo <= cch [Atsam]
                    .put("alv", "kcg") // Atlantic–Congo <= kcg [Tyap]
                    .put("alv", "ken") // Atlantic–Congo <= ken [Kenyang]
                    .put("alv", "ngb")
                    .put("alv", "yav")
                    .put("ber", "zgh")
                    .put("bnt", "asa")
                    .put("bnt", "bez")
                    .put("bnt", "cgg")
                    .put("bnt", "ebu")
                    .put("bnt", "jmc")
                    .put("bnt", "ksb")
                    .put("bnt", "lag")
                    .put("bnt", "mer")
                    .put("bnt", "mgh")
                    .put("bnt", "nmg")
                    .put("bnt", "rof")
                    .put("bnt", "rwk")
                    .put("bnt", "sbp")
                    .put("bnt", "seh")
                    .put("bnt", "vun")
                    .put("bnt", "xog")
                    .put("cpp", "kea")
                    .put("euq", "eu")
                    .put("gmw", "ksh") // gmw = West Germanic
                    .put("gmw", "lb")
                    .put("gmw", "wae")
                    .put("grk", "el")
                    .put("grk", "gmy")
                    .put("grk", "grc")
                    .put("ira", "lrc")
                    .put("ira", "bgn") // Iranian <= Western Balochi
                    .put("inc", "trw") // Indo-Aryan <= Torwali
                    .put("jpx", "ja")
                    .put("ngb", "sg")
                    .put("roa", "cpf")
                    .put("roa", "cpp")
                    .put("sdv", "saq")
                    .put("son", "khq")
                    .put("sw", "swc")
                    .put("tai", "blt") // tai [Tai] <= blt [Tai Dam]
                    .put("tai", "lo")
                    .put("tai", "th")
                    .put("zlw", "szl") // West Slavic <= Silesian

                    // Restoring languages removed in 2024-08 wikidata
                    .put("inc", "ur") // Urdu is indic
                    .put("inc", "pa") // Punjabi is indic
                    .put("inc", "skr") // Saraiki is indic
                    .put("zls", "bs") // South Slavic (sh has problems)
                    .put("zls", "hr") // South Slavic (sh has problems)
                    .put("zls", "sr") // South Slavic (sh has problems)
                    .put("inc", "hi") // Indic
                    .put("inc", "kok") // Indic
                    .put("inc", "ks") // Indic
                    .put("inc", "mr") // Indic
                    .put("inc", "sd") // Indic
                    .put("cr", "csw") // Cree
                    .put("tai", "za") // Tai
                    .put("fiu", "hu") // Finno-Ugric
                    .put("alg", "cr") // Algonquin
                    .put("sit", "bo") // Sino-Tibetan
                    .put("poz", "mg") // Malayo-Polynesian languages
                    .put("esx", "iu") // Eskimo-Aleut languages
                    .put("esx", "kl") // Eskimo-Aleut languages
                    .build();

    /**
     * To remove parent-child relations from Wikidata, eg if a child has two parents (where that
     * causes problems). Don't do it if there is an explicit parent above.
     */
    static final Multimap<String, String> REMOVE_PARENT_CHILDREN =
            ImmutableMultimap.<String, String>builder()
                    .put("alv", "ukg") // ngf [Trans-New Guinea languages] <= ukg [Ukuriguma]
                    .put(
                            "crp",
                            "*") // general Creole group interferes with French/Spanish/... language
                    // grouping
                    .put("cus", "mhd") // bnt [Bantu] <= mhd [Mbugu] (not cus [Cushitic])
                    .put("gmw", "pih") // cpe [Creoles and pidgins, English based] <= pih
                    // [Pitcairn-Norfolk]
                    .put("inc", "rmg")
                    // Indo-European
                    .put("nic", "kcp") // ssa [Nilo-Saharan] <= kcp [Kanga]
                    .put("nic", "kec") // ssa [Nilo-Saharan] <= kec [Keiga]
                    .put("nic", "kgo") // ssa [Nilo-Saharan] <= kgo [Krongo]
                    .put("nic", "tbr") // ssa [Nilo-Saharan] <= tbr [Tumtum]
                    .put("nic", "tey") // ssa [Nilo-Saharan] <= tey [Tulishi]
                    .put("sit", "dz") // sit <= tbq <= dz
                    .put("sit", "zh")
                    .put("sla", "cu")
                    .put("tbq", "psq") // paa [Papuan]; for psq [Pasi] - not tbq [Tibeto-Burman
                    // languages]; (There is also a variety of the Sino-Tibetan Adi
                    // language called Pasi.
                    .build();

    static {
        // If a child is in RESET_PARENT_CHILDREN, it should not be in
        // REMOVE_PARENT_CHILDREN
        // That is because the RESET_PARENT_CHILDREN will cause the removal of any other
        // parents anyway.
        SetView<String> bad =
                Sets.intersection(
                        Set.copyOf(RESET_PARENT_CHILDREN.values()),
                        Set.copyOf(REMOVE_PARENT_CHILDREN.values()));
        if (!bad.isEmpty())
            System.err.println(
                    "Remove from REMOVE_PARENT_CHILDREN, child values: \""
                            + Joiner.on("\",\"").join(bad)
                            + "\"");
    }

    public static void main(String[] args) throws IOException {
        new GenerateLanguageContainment().run(args);
        if (Containment.hadErrors) {
            System.err.println("ERROR: Containment Errors detected, see errors above.");
            System.exit(1);
        }
    }

    void run(String[] args) throws IOException {
        if (true) {
            // check on items
            for (String check : Arrays.asList("sw", "km", "ksh", "wae", "kea", "mfe", "th", "lo")) {
                System.out.println("Checking " + ENGLISH.getName(check) + "[" + check + "]");
                Collection<String> entities = QUERY_HELPER.codeToEntity.get(check);
                if (entities.isEmpty()) {
                    System.out.println("no code for " + check + ": " + entities);
                    continue;
                }
                for (String entity : entities) {
                    Set<List<String>> ancestors = getAllAncestors(entity);
                    showEntityLists(entity + " parents ", ancestors);
                    System.out.println();
                }
            }
        }

        Map<Status, Set<String>> table = Validity.getInstance().getStatusToCodes(LstrType.language);
        TreeMultimap<String, String> _parentToChild = TreeMultimap.create();
        TreeSet<String> missing = new TreeSet<>(table.get(Status.regular));
        _parentToChild.put(LocaleNames.MUL, LocaleNames.UND);
        Set<String> skipping = new LinkedHashSet<>();
        for (String code : table.get(Status.regular)) {
            if (ONLY_LIVING) {
                Type type = Iso639Data.getType(code);
                if (type != Type.Living) {
                    continue;
                }
            }
            if (code.compareTo("hdz") > 0) {
                int debug = 0;
            }
            // if (COLLECTIONS.contains(code)) {
            // continue;
            // }
            Collection<String> entities = QUERY_HELPER.codeToEntity.get(code);
            if (entities.isEmpty()) {
                continue;
            }
            for (String entity : entities) {
                if (QUERY_HELPER.childToParent.get(entity).isEmpty()) {
                    continue;
                }
                Set<Set<String>> chains = getAncestors(entity, skipping);
                if (chains.size() > 1) {
                    int debug = 0;
                }
                for (Set<String> chain : chains) {
                    String last = null;
                    for (String link : chain) {
                        if (last != null) {
                            _parentToChild.put(link, last);
                        }
                        last = link;
                    }
                }
            }
        }
        System.out.println("Writing " + "skippingCodes.tsv");
        try (PrintWriter w =
                FileUtilities.openUTF8Writer(TsvWriter.getTsvDir(), "skippingCodes.tsv")) {
            // TsvWriter.writeRow(w, "childCode\tLabel", "parentCode\tLabel"); // header
            skipping.forEach(e -> w.println(e));
        }

        // preflight
        DiffLanguageGroups.show("en");

        Multimap<String, String> _childToParents =
                Multimaps.invertFrom(_parentToChild, TreeMultimap.create());

        System.out.println("\nOVERRIDE Remove parent");
        System.out.println("OVERRIDE\tParent\tChild\tNew Parents");
        for (Entry<String, String> entry : REMOVE_PARENT_CHILDREN.entries()) {
            final String parent = entry.getKey();
            final String child = entry.getValue();
            Set<String> oldChildren = _parentToChild.get(parent);
            String type;
            if (child.equals("*")) {
                if (oldChildren == null) {
                    type = "No remove";
                } else {
                    type = "Removing parent";
                    _parentToChild.removeAll(parent);
                    _childToParents = Multimaps.invertFrom(_parentToChild, TreeMultimap.create());
                }
            } else {
                if (oldChildren != null && oldChildren.contains(child)) {
                    _parentToChild.remove(parent, child);
                    _childToParents = Multimaps.invertFrom(_parentToChild, TreeMultimap.create());
                    type = "Removing parent";
                } else {
                    type = "No remove";
                }
            }
            System.out.println(
                    JOIN_TAB.join(
                            type,
                            DiffLanguageGroups.show(parent),
                            DiffLanguageGroups.show(child),
                            _childToParents.get(child)));
        }

        System.out.println("\nOVERRIDE Replace Parent");
        System.out.println("OVERRIDE\tParent\tChild");
        for (Entry<String, String> entry : RESET_PARENT_CHILDREN.entries()) {
            final String parent = entry.getKey();
            final String child = entry.getValue();
            Set<String> oldValues = _parentToChild.get(parent);
            Set<String> removals = new LinkedHashSet<>();

            String type;
            if (oldValues != null && oldValues.contains(child)) {
                type = "Redundant add";
            } else {
                type = "Changing";
                _parentToChild.put(parent, child);
                _childToParents = Multimaps.invertFrom(_parentToChild, TreeMultimap.create());
                Collection<String> newParents = _childToParents.get(child);
                if (newParents.size() > 1) {
                    for (String parent2 : newParents) {
                        if (!parent2.equals(parent)) {
                            _parentToChild.remove(parent2, child);
                            removals.add(parent2);
                        }
                        // rebuild
                        _childToParents =
                                Multimaps.invertFrom(_parentToChild, TreeMultimap.create());
                    }
                }
            }
            System.out.println(
                    JOIN_TAB.join(
                            type,
                            DiffLanguageGroups.show(parent),
                            DiffLanguageGroups.show(child),
                            _childToParents.get(child),
                            removals));
        }

        // special code for artificial
        for (String code : Iso639Data.getAvailable()) {
            Type type = Iso639Data.getType(code);
            if (type == Type.Constructed) {
                _parentToChild.put("art", code);
            }
        }

        Multimap<String, String> parentToChild = ImmutableMultimap.copyOf(_parentToChild);
        Multimap<String, String> childToParent =
                ImmutableMultimap.copyOf(
                        Multimaps.invertFrom(parentToChild, TreeMultimap.create()));
        System.out.println(
                "Checking " + "he" + "\t" + Containment.getAllDirected(childToParent, "he"));

        try (PrintWriter w =
                FileUtilities.openUTF8Writer(TsvWriter.getTsvDir(), "RawLanguageContainment.txt")) {
            print(w, parentToChild, new ArrayList<>(Arrays.asList(LocaleNames.MUL)));
        }
        SimpleXMLSource xmlSource = new SimpleXMLSource("languageGroup");
        xmlSource.setNonInheriting(true); // should be gotten from DtdType...
        CLDRFile newFile = new CLDRFile(xmlSource);
        newFile.setDtdType(DtdType.supplementalData);
        newFile.add("//" + DtdType.supplementalData + "/version[@number='$Revision$']", "");
        printXML(newFile, parentToChild);

        try (PrintWriter outFile =
                FileUtilities.openUTF8Writer(
                        CLDRPaths.SUPPLEMENTAL_DIRECTORY, "languageGroup.xml")) {
            newFile.write(outFile);
        } catch (IOException e1) {
            throw new ICUUncheckedIOException("Can't write to languageGroup.xml", e1);
        }

        // for (Entry<String,String> entry : childToParent.entries()) {
        // String childNames = getName(entityToCode, entityToLabel, entry.getKey());
        // String parentNames = getName(entityToCode, entityToLabel, entry.getValue());
        // System.out.println(entry.getKey() + "\t" + entry.getValue() + "\t" +
        // childNames + "\t" + parentNames);
        // }
        QUERY_HELPER.writeTsvs();
        DiffLanguageGroups.main(new String[] {});
    }

    private static void showEntityLists(String title, Set<List<String>> ancestors) {
        ancestors.forEach(
                new Consumer<List<String>>() {
                    @Override
                    public void accept(List<String> item) {
                        item.forEach(
                                new Consumer<String>() {
                                    @Override
                                    public void accept(String t) {
                                        System.out.println(
                                                t
                                                        + "\t"
                                                        + QUERY_HELPER.entityToCode.get(t)
                                                        + "\t"
                                                        + QUERY_HELPER.entityToLabel.get(t));
                                    }
                                });
                        System.out.println();
                    }
                });
    }

    private static void printXML(CLDRFile newFile, Multimap<String, String> parentToChild) {
        printXML(newFile, parentToChild, LocaleNames.MUL);
    }

    private static void printXML(
            CLDRFile newFile, Multimap<String, String> parentToChild, String base) {
        Collection<String> children = parentToChild.get(base);
        if (children.isEmpty()) {
            return;
        }
        if (base.equals(LocaleNames.UND)) {
            // skip, no good info
        } else {
            newFile.add(
                    "//"
                            + DtdType.supplementalData
                            + "/languageGroups/languageGroup[@parent=\""
                            + base
                            + "\"]",
                    Joiner.on(" ").join(children));
        }
        for (String child : children) {
            printXML(newFile, parentToChild, child);
        }
    }

    private static void print(
            Writer out, Multimap<String, String> parentToChild, List<String> line) {
        String current = line.get(line.size() - 1);
        Collection<String> children = parentToChild.get(current);
        if (children.isEmpty()) {
            try {
                String sep = "";
                for (String item : line) {
                    out.append(sep).append(NAME.apply(item));
                    sep = " > ";
                }
                out.append('\n');
                out.flush();
            } catch (IOException e) {
            }
        } else {
            for (String child : children) {
                line.add(child);
                print(out, parentToChild, line);
                line.remove(line.size() - 1);
            }
        }
    }

    private static Set<Set<String>> getAncestors(String leaf, Set<String> skipping) {
        Set<List<String>> items = Containment.getAllDirected(QUERY_HELPER.childToParent, leaf);
        Set<Set<String>> itemsFixed = new LinkedHashSet<>();
        main:
        for (List<String> item : items) {
            Set<String> chain = new LinkedHashSet<>();
            for (String id : item) {
                String code = QUERY_HELPER.entityToCode.get(id);
                if (code == null) {
                    continue;
                }

                // skip leaf nodes after the first

                if (!chain.isEmpty() && !COLLECTIONS.contains(code)) {
                    if (code.equals("zh")) {
                        code = "zhx"; // rewrite collections usage
                    } else {
                        skipping.add(
                                "Skipping inheritance from\t"
                                        + chain
                                        + "\t"
                                        + code
                                        + "\tfrom\t"
                                        + QUERY_HELPER.showNameAndCode2(items));
                        continue;
                    }
                }

                // check for cycle, and skip if we have one

                boolean changed = chain.add(code);
                if (!changed) {
                    log("Cycle in\t" + chain + "\tfrom\t" + QUERY_HELPER.showNameAndCode2(items));
                    continue main;
                }
            }
            if (chain.size() > 1) {
                chain.add(LocaleNames.MUL); // root
                itemsFixed.add(chain);
            }
        }
        // remove subsets
        // eg [[smp, he, mul], [smp, he, sem, afa, mul]]
        // => [[smp, he, sem, afa, mul]]
        if (itemsFixed.size() > 1) {
            Set<Set<String>> removals = new HashSet<>();
            for (Set<String> chain1 : itemsFixed) {
                for (Set<String> chain2 : itemsFixed) {
                    if (chain1.containsAll(chain2) && !chain2.containsAll(chain1)) {
                        removals.add(chain2);
                    }
                }
            }
            itemsFixed.removeAll(removals);
        }
        return itemsFixed;
        // TODO: delete this commented-out code?
        // while (true) {
        // String code = entityToCode.get(leaf);
        // if (code != null) {
        // chain.add(code);
        // }
        // Collection<String> parents = childToParent.get(leaf);
        // if (parents.isEmpty()) {
        // // clean up duplicates
        // chain = new ArrayList<>(new LinkedHashSet<>(chain));
        // // wikipedia has non-collections as parents. Remove those if they are not
        // first.
        // break;
        // }
        // leaf = getBest(parents);
        // }
        // String last = chain.get(0);
        // for (int i = 1; i < chain.size(); ++i) {
        // String item = chain.get(i);
        // if (!COLLECTIONS.contains(item)) {
        // chain.set(i, item.equals("zh") ? "zhx" : "");
        // DROPPED_PARENTS_TO_CHILDREN.put(item, last);
        // } else {
        // last = item;
        // }
        // }
        // chain.removeIf(x -> x.isEmpty());
        // if ("zh".equals(chain.get(0))) {
        // chain.add(1,"zhx");
        // }
        // last = chain.get(chain.size()-1);
        // if (!LocaleNames.MUL.equals(last)) {
        // chain.add(LocaleNames.MUL); // make sure we have root.
        // }
        // if (chain.size() == 2) {
        // chain.add(1,LocaleNames.UND);
        // }
        // return chain;
    }

    private static void log(String string) {
        System.out.println(string);
        // for (Entry<String, String> e : DROPPED_PARENTS_TO_CHILDREN.entries()) {
        // System.out.println(NAME.apply(e.getKey()) + "\t" + NAME.apply(e.getValue())
        // );
        // }
    }

    // TODO: This function is only called by other commented-out code above.
    // private static String getBest(Collection<String> parents) {
    // for (String parent : parents) {
    // String code = QUERY_HELPER.entityToCode.get(parent);
    // if (code == null) continue;
    // Type type = Iso639Data.getType(code);
    // if (type != Type.Living) {
    // continue;
    // }
    // return parent;
    // }
    // // failed
    // return parents.iterator().next();
    // }

    private static Multimap<String, String> loadQueryPairs(
            Class<?> class1,
            String file,
            Function<String, String> keyMapper,
            Function<String, String> valueMapper)
            throws IOException {
        System.out.println("QUERY: " + file);
        ResultSet rs = queryClient.execSelectFromSparql(file, QueryClient.WIKIDATA_SPARQL_SERVER);
        // the query must return exactly two variables.
        List<String> resultVars = rs.getResultVars();
        assertTwoVars(resultVars);
        final String keyName = resultVars.get(0);
        final String valueName = resultVars.get(1);

        ImmutableMultimap.Builder<String, String> _keyToValues = ImmutableMultimap.builder();
        for (; rs.hasNext(); ) {
            final QuerySolution qs = rs.next();
            String key = QueryClient.getStringOrNull(qs, keyName);
            String value = QueryClient.getStringOrNull(qs, valueName);
            _keyToValues.put(key, value);
        }
        ImmutableMultimap<String, String> result = _keyToValues.build();
        showDups(file, result, keyMapper, valueMapper);
        System.out.println("LOADED: " + file + " with rows " + rs.getRowNumber());
        return result;
    }

    /**
     * Assuming that the SPARQL query returns exactly 2 results, treat them as Key=Value.
     *
     * @param class1
     * @param file name of a sparql query, such as 'wikidata-childToParent'
     * @param fixValue
     * @param keyMapper
     * @param valueMapper
     * @return
     * @throws IOException
     */
    private static Map<String, String> loadQueryPairsUnique(
            Class<?> class1,
            String file,
            Function<String, String> fixValue,
            Function<String, String> keyMapper,
            Function<String, String> valueMapper)
            throws IOException {

        System.out.println("QUERY: " + file);
        ResultSet rs = queryClient.execSelectFromSparql(file, QueryClient.WIKIDATA_SPARQL_SERVER);

        // the query must return exactly two variables.
        List<String> resultVars = rs.getResultVars();
        assertTwoVars(resultVars);
        final String keyName = resultVars.get(0);
        final String valueName = resultVars.get(1);

        Map<String, String> _keyToValue = new TreeMap<>();
        Multimap<String, String> _keyToValues = TreeMultimap.create();
        for (; rs.hasNext(); ) {
            final QuerySolution qs = rs.next();
            String key = QueryClient.getStringOrNull(qs, keyName);
            String value = QueryClient.getStringOrNull(qs, valueName);
            if (fixValue != null) {
                value = fixValue.apply(value);
            }
            _keyToValues.put(key, value);
            String oldValue = _keyToValue.get(key);
            if (oldValue == null || oldValue.equals("kxm")) {
                _keyToValue.put(key, value);
            }
        }
        _keyToValue = ImmutableMap.copyOf(_keyToValue);
        showDups(file, _keyToValues, keyMapper, valueMapper);
        System.out.println("LOADED: " + file + " with rows " + rs.getRowNumber());
        return _keyToValue;
    }

    private static void assertTwoVars(List<String> resultVars) {
        if (resultVars.size() != 2) {
            throw new IllegalArgumentException(
                    "expected 2 result vars but got " + resultVars.size() + ": " + resultVars);
        }
    }

    private static void showDups(
            String file,
            Multimap<String, String> _keyToValues,
            Function<String, String> keyMapper,
            Function<String, String> valueMapper) {
        for (Entry<String, Collection<String>> entry : _keyToValues.asMap().entrySet()) {
            Collection<String> valueSet = entry.getValue();
            if (valueSet.size() > 1) {
                String key = entry.getKey();
                key = keyMapper == null ? key : keyMapper.apply(key);
                if (valueMapper != null) {
                    Set<String> result = new LinkedHashSet<>();
                    valueSet.stream().map(valueMapper).forEach(x -> result.add(x));
                    valueSet = result;
                }
                log(file + "\tMultiple values: " + key + "\t" + valueSet);
            }
        }
    }

    static Set<List<String>> getAllAncestors(String lang) {
        return Containment.getAllDirected(QUERY_HELPER.childToParent, lang);
    }
}
