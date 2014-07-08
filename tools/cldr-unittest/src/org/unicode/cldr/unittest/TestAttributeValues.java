package org.unicode.cldr.unittest;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import org.unicode.cldr.unittest.CheckResult.ResultStatus;
import org.unicode.cldr.unittest.ObjectMatcherFactory;
import org.unicode.cldr.unittest.ObjectMatcherFactory.MatcherPattern;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.AttributeValidityInfo;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Ints;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.dev.util.CollectionUtilities.ObjectMatcher;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.UnicodeSet;

public class TestAttributeValues extends TestFmwk {

    /**
     * Joiner using commas (comma,space), skipping nulls
     */
    private final static Joiner COMMA_JOINER = Joiner.on(", ").skipNulls();

    /**
     * Joiner using semicolons, skipping nulls
     */
    private final static Joiner SEMICOLON_JOINER = Joiner.on(";").skipNulls();

    /**
     * Splitter for whitespace (\\s+), omitting empty strings
     */
    private final static Splitter WHITESPACE_SPLTTER = Splitter.on(
            PatternCache.get("\\s+")).omitEmptyStrings();

    /**
     * Splitter for Commas, omitting empties, skipping nulls
     */
    private final static Splitter COMMA_SPLITTER = Splitter.on(",")
            .omitEmptyStrings().trimResults();

    /**
     * Source directories; entries that do not start with slash (/) are relative
     * to CLDR_DIR
     */
    private String[] sourceDirs = new String[] {
            "common",
            "exemplars",
            "keyboards",
            "seed"
    };

    /**
     * Output to CSV
     */
    private boolean csvOutput = false;

    /**
     * location of the CSV file
     */
    private static final String CSV_FILE = "/Users/ribnitz/Documents/data_errors.csv";

    /**
     * Set containing entries that have been deprecated, but where the time set
     * for deprecation is not yet reached; as a consequence, these will not
     * trigger deprecation warnings.
     */
    private static final Set<String> DEPRECATION_OVERRIDES = ImmutableSet.of(
            "AN",
            "SCOUSE",
            "BOONT",
            "HEPLOC");

    /**
     * Multimap containing associations of deprecated attributes; indexed by the
     * attribute.
     */
    private final Multimap<String, DeprecatedAttributeInfo> deprecatedAttributeMap = LinkedHashMultimap
            .create();

    /**
     * Small class holding information on a deprecated items, such as the
     * locale, a set of replacements and its path, used as a value in the
     * multimap deprecatedAttributeMap above.
     * 
     * @author ribnitz
     * 
     */
    private static class DeprecatedAttributeInfo {
        private final String locale;
        private final String pathToLocaleFile;
        private final Set<String> replacements = new HashSet<>();
        private final String path;
        private final int hashCode;

        public DeprecatedAttributeInfo(String locale, String path,
                String pathToLocaleFile) {
            this(locale, path, Collections.<String> emptyList(),
                    pathToLocaleFile);
        }

        public DeprecatedAttributeInfo(String locale, String path,
                Collection<String> replacements, String pathToLocaleFile) {
            // Sanity checks: neither locale nor path must be empty.
            if (locale == null) {
                throw new IllegalArgumentException("Locale must not be null");
            }
            if (locale.isEmpty()) {
                throw new IllegalArgumentException("Locale must not be empty");
            }
            if (path == null) {
                throw new IllegalArgumentException("Path must not be null");
            }
            if (path.isEmpty()) {
                throw new IllegalArgumentException("Path must not be empty");
            }
            this.locale = locale;
            this.path = path;
            this.pathToLocaleFile = pathToLocaleFile;
            this.replacements.addAll(replacements);
            hashCode = Objects.hash(locale, replacements, path,
                    pathToLocaleFile);
        }

        public String getLocale() {
            return locale;
        }

        public Collection<String> getReplacements() {
            return Collections.unmodifiableSet(replacements);
        }

        public String getPath() {
            return path;
        }

        public String getPathToLocaleFile() {
            return pathToLocaleFile;
        }

        public int hashCode() {
            return hashCode;
        }

        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            if (this == other) {
                return true;
            }
            if (hashCode != other.hashCode()) {
                return false;
            }
            if (getClass().equals(other.getClass())) {
                return false;
            }
            DeprecatedAttributeInfo o = (DeprecatedAttributeInfo) other;
            if (!locale.equals(o.getLocale())) {
                return false;
            }
            if (path != o.getPath()) {
                return false;
            }
            if (!replacements.equals(o.getReplacements())) {
                return false;
            }
            if (pathToLocaleFile != null) {
                if (!pathToLocaleFile.equals(o.pathToLocaleFile)) {
                    return false;
                }
            } else {
                // the pathToLocaleFile of this item is null, if the other item
                // path isn't, they are unequal
                if (o.pathToLocaleFile != null) {
                    return false;
                }
            }
            return true;
        }
    }

    private final static Set<String> FILE_EXCLUSIONS = ImmutableSet.of(
            "coverageLevels.xml", "pluralRanges.xml");

    /**
     * Set of unknown final elements
     */
    private final Set<String> unknownFinalElements = new HashSet<>();

    /**
     * Set of unhandled paths
     */
    private final Set<String> unhandledPaths = new HashSet<>();

    private Set<String> localeSet = new HashSet<>();
    private final Set<String> elementOrder = new LinkedHashSet<String>();
    private final Set<String> attributeOrder = new LinkedHashSet<String>();

    private final Map<String, Map<String, MatcherPattern>> element_attribute_validity = new HashMap<String, Map<String, MatcherPattern>>();
    private final Map<String, MatcherPattern> common_attribute_validity = new HashMap<String, MatcherPattern>();
    final static Map<String, MatcherPattern> variables = new HashMap<String, MatcherPattern>();
    // static VariableReplacer variableReplacer = new VariableReplacer(); //
    // note: this can be coalesced with the above
    // -- to do later.
    private boolean initialized = false;
    private LocaleMatcher localeMatcher;
    private final Map<String, Map<String, String>> code_type_replacement = new TreeMap<String, Map<String, String>>();
    private SupplementalDataInfo supplementalData;

    PluralInfo pluralInfo;

    XPathParts parts = new XPathParts(null, null);
    static final UnicodeSet DIGITS = new UnicodeSet("[0-9]").freeze();

    /**
     * Callable returning the value of csvOutput, used to call the factory
     * method on CheckResult.
     */
    private class CheckCSVValuePred implements Callable<Boolean> {
        @Override
        public Boolean call() {
            return csvOutput;
        }
    }

    /**
     * Predicate to filter the results, will return only those where the
     * ResultStatus matches
     * 
     * @author ribnitz
     * 
     */
    private static final class CheckResultPredicate implements
            Predicate<CheckResult> {
        private final ResultStatus success;

        public CheckResultPredicate(ResultStatus success) {
            this.success = success;
        }

        @Override
        public boolean apply(CheckResult input) {
            return input.getStatus() == success;
        }
    }

    /**
     * Tightly coupled matcher for Locales
     * 
     * @author ribnitz
     * 
     */
    private static class LocaleMatcher implements ObjectMatcher<String> {
        ObjectMatcher<String> grandfathered = variables.get("$grandfathered").matcher;
        ObjectMatcher<String> language = variables.get("$language").matcher;
        ObjectMatcher<String> script = variables.get("$script").matcher;
        ObjectMatcher<String> territory = variables.get("$territory").matcher;

        ObjectMatcher<String> variant = variables.get("$variant").matcher;
        LocaleIDParser lip = new LocaleIDParser();
        static LocaleMatcher singleton = null;
        static Object sync = new Object();

        private LocaleMatcher(boolean b) {
        }

        public static LocaleMatcher make() {
            synchronized (sync) {
                if (singleton == null) {
                    singleton = new LocaleMatcher(true);
                }
            }
            return singleton;
        }

        public boolean matches(String value) {
            if (grandfathered.matches(value))
                return true;
            lip.set((String) value);
            String field = lip.getLanguage();
            if (!language.matches(field))
                return false;
            field = lip.getScript();
            if (field.length() != 0 && !script.matches(field))
                return false;
            field = lip.getRegion();
            if (field.length() != 0 && !territory.matches(field))
                return false;
            String[] fields = lip.getVariants();
            for (String varField : fields) {
                if (!variant.matches(varField))
                    return false;
            }
            return true;
        }
    }

    /**
     * Predicate that will result in a file in the file walk to be included in
     * the set of Files
     * 
     * @author ribnitz
     * 
     */
    private static class FileAcceptPredicate implements Predicate<Path> {

        /**
         * Filenames that should be excluded
         */
        private String[] fnameExclusions = new String[] {
                // "Latn","FONIPA", "Arab","Ethi"
                };

        /**
         * Path components that should be exculded
         */
        private final static String[] EXCLUDED_PATHS = new String[] { /* "bcp47" */};
        private final Collection<String> excludedFiles;

        public FileAcceptPredicate(Collection<String> excluded) {
            excludedFiles = excluded;
        }

        @Override
        public boolean apply(Path input) {

            String fname = input.getFileName().toString();
            if (!fname.endsWith(".xml")) {
                return false;
            }
            if (excludedFiles.contains(fname)) {
                return false;
            }
            for (String cur : fnameExclusions) {
                if (fname.contains(cur)) {
                    return false;
                }
            }
            boolean add = true;
            for (int i = 0; i < input.getNameCount(); i++) {
                if (!add) {
                    break;
                }
                String curPath = input.getName(i).toString();
                for (String exc : EXCLUDED_PATHS) {
                    if (curPath.contains(exc)) {
                        add = false;
                        break;
                    }
                }
            }
            return add;
        }

    }

    /**
     * Class iterating over Files, adding all matching files to Set, which is
     * then returned.
     * 
     * @author ribnitz
     * 
     */
    private static class LocaleSetGenerator {

        public static Set<Path> generateLocaleSet(Iterable<File> sourceFiles,
                final Collection<String> excludedFiles) {
            final Set<Path> fileSet = new HashSet<>();
            final Predicate<Path> acceptPredicate = new FileAcceptPredicate(
                    excludedFiles);
            for (File sourceFile : sourceFiles) {
                try {
                    Files.walkFileTree(sourceFile.toPath(),
                            new SimpleFileVisitor<Path>() {

                                public FileVisitResult visitFile(
                                        Path file,
                                        BasicFileAttributes attrs)
                                        throws IOException {

                                    if (acceptPredicate.apply(file)) {
                                        fileSet.add(file);
                                    }
                                    return FileVisitResult.CONTINUE;
                                }

                            });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return fileSet;
        }
    }

    private static final Relation<PluralInfo.Count, String> PLURAL_EXCEPTIONS = Relation
            .of(
                    new EnumMap<PluralInfo.Count, Set<String>>(
                            PluralInfo.Count.class), HashSet.class);
    static {
        PLURAL_EXCEPTIONS.put(PluralInfo.Count.many, "hr");
        PLURAL_EXCEPTIONS.put(PluralInfo.Count.many, "sr");
        PLURAL_EXCEPTIONS.put(PluralInfo.Count.many, "sh");
        PLURAL_EXCEPTIONS.put(PluralInfo.Count.many, "bs");
        PLURAL_EXCEPTIONS.put(PluralInfo.Count.few, "ru");
    }

    private static boolean isPluralException(Count countValue, String locale) {
        Set<String> exceptions = PLURAL_EXCEPTIONS.get(countValue);
        if (exceptions == null) {
            return false;
        }
        if (exceptions.contains(locale)) {
            return true;
        }
        int bar = locale.indexOf('_'); // catch bs_Cyrl, etc.
        if (bar > 0) {
            String base = locale.substring(0, bar);
            if (exceptions.contains(base)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns replacement, or null if there is none. "" if the code is
     * deprecated, but without a replacement. Input is of the form $language
     * 
     * @return
     */
    private String getReplacement(String value, String attributeValue) {
        Map<String, String> type_replacement = code_type_replacement.get(value);
        if (type_replacement == null) {
            return null;
        }
        return type_replacement.get(attributeValue);
    }

    LocaleIDParser localeIDParser = new LocaleIDParser();

    private void initialize(Iterable<String> paths,
            Collection<CheckResult> possibleErrors, Factory fact,
            File supplementalDir, String localeID) {
        supplementalData = SupplementalDataInfo.getInstance(supplementalDir);
        if (localeID != null) {
            pluralInfo = supplementalData.getPlurals(PluralType.cardinal,
                    localeID);
        }
        // isEnglish = "en".equals(localeIDParser.set(language));
        synchronized (elementOrder) {
            if (!initialized) {
                getMetadata(new FactoryBasedMetadataHandler(fact,
                        supplementalData));
                // getMetadata(fact.getSupplementalMetadata(),
                // supplementalData,fact.getSupplementalMetadata().getComparator(),fact.getSupplementalMetadata().iterator().next());
                initialized = true;
                localeMatcher = LocaleMatcher.make();
                // cleanup locale list
                Iterator<String> lIter = localeSet.iterator();
                while (lIter.hasNext()) {
                    String loc = lIter.next();
                    if (!localeMatcher.matches(loc)) {
                        lIter.remove();
                    }
                }
            }
        }
    }

    /**
     * Interface used for accessing MetaData information
     * 
     * @author ribnitz
     * 
     */
    private static interface MetadataInterface extends Iterable<String> {
        Iterator<String> iterator();

        Iterator<String> iterator(Comparator<String> comparator);

        String getStringValue(String path);

        String getFullXPath(String path);

        Comparator<String> getComparator();

        SupplementalDataInfo getSupplementalDataInfo();
    }

    private static class SupplementalMetaDataReader {
        private final Collection<String> supplementalData;

        public SupplementalMetaDataReader(Factory aFactory) {
            File spuDir = aFactory.getSupplementalDirectory();
            File supplementalMetaDataFile = Paths.get(spuDir.getAbsolutePath(),
                    "supplementalMetaData.xml").toFile();
            XMLFileReader xfr = new XMLFileReader();
            PathValueHandler pvHandler = new PathValueHandler();
            xfr.setHandler(pvHandler);
            xfr.read(supplementalMetaDataFile.getAbsolutePath(), -1, false);
            supplementalData = pvHandler.getPaths();
        }

        public Iterator<String> iterator() {
            return Collections.unmodifiableCollection(supplementalData)
                    .iterator();
        }

        public Iterator<String> iterator(Comparator<String> comparator) {
            TreeSet<String> ts = new TreeSet<>(comparator);
            ts.addAll(supplementalData);
            return Collections.unmodifiableSet(ts).iterator();
        }

        public String getFullXPath(String p) {
            for (String path : supplementalData) {
                if (path.startsWith(p) || p.contains(p)) {
                    return path;
                }
            }
            return null;

        }
    }

    private static class FactoryBasedMetadataHandler implements
            MetadataInterface {
        private final Factory factory;
        private final SupplementalDataInfo sdi;

        public FactoryBasedMetadataHandler(Factory aFacotry,
                SupplementalDataInfo sdi) {
            factory = aFacotry;
            this.sdi = sdi;
        }

        @Override
        public Iterator<String> iterator() {
            return factory.getSupplementalMetadata().iterator();
        }

        @Override
        public Iterator<String> iterator(Comparator<String> comparator) {
            return factory.getSupplementalMetadata().iterator(null, comparator);
        }

        @Override
        public String getStringValue(String path) {
            return factory.getSupplementalMetadata().getStringValue(path);
        }

        @Override
        public Comparator<String> getComparator() {
            return factory.getSupplementalMetadata().getComparator();
        }

        @Override
        public String getFullXPath(String path) {
            return factory.getSupplementalMetadata().getFullXPath(path);
        }

        @Override
        public SupplementalDataInfo getSupplementalDataInfo() {
            return sdi;
        }

    }

    private void getMetadata(final MetadataInterface metadata) {
        // sorting is expensive, but we need it here.
        SupplementalDataInfo sdi = metadata.getSupplementalDataInfo();
        elementOrder.addAll(sdi.getElementOrder());
        attributeOrder.addAll(sdi.getAttributeOrder());
        Collection<String> defaultContentLocales = sdi
                .getDefaultContentLocales();
        Map<String, Map<String, R2<List<String>, String>>> aliases = sdi
                .getLocaleAliasInfo();
        Set<String> invalidLocales = new HashSet<>();
        for (String locale : defaultContentLocales) {
            if (!localeSet.contains(locale)) {
                invalidLocales.add(locale);
            }
        }
        Map<String, R2<String, String>> validityInfo = sdi.getValidityInfo();
        // Map<String,VariableWithValue> vars=new HashMap<>();
        for (Entry<String, R2<String, String>> cur : validityInfo.entrySet()) {
            String varName = cur.getKey();
            String type = cur.getValue().get0();
            String val = cur.getValue().get1();
            // types: choice, regex, locale,
            MatcherPattern mp = getMatcherPattern(val,
                    ImmutableMap.<String, String> of("type", type), null, sdi);
            if (mp != null) {
                variables.put(varName, mp);
            }
        }
        Set<AttributeValidityInfo> attributeValidityMap = sdi
                .getAttributeValidity();
        for (AttributeValidityInfo info : attributeValidityMap) {
            // String attribute = entry.getKey();
            // AttributeValidityInfo info=entry.getValue();
            int jj = 0;
            MatcherPattern mp = getMatcherPattern(
                    info.getValue(),
                    (Map<String, String>) (info.getType() == null ? Collections
                            .emptyMap() : ImmutableMap.<String, String> of(
                            "type", info.getType())), null,
                    metadata.getSupplementalDataInfo());
            if (mp == null) {
                // System.out.println("Failed to make matcher for: " + value +
                // "\t" + path);
                continue;
            }
            Set<String> attributeValues = info.getAttributes();
            Set<String> elementList = info.getElements();
            if (elementList == null) {
                addAttributes(attributeValues, common_attribute_validity, mp);
            } else {
                // Iterable<String>
                // elementList=WHITESPACE_SPLTTER.split(elementsString.trim());
                for (String element : elementList) {
                    Map<String, MatcherPattern> attribute_validity = element_attribute_validity
                            .get(element);
                    if (attribute_validity == null)
                        element_attribute_validity
                                .put(element,
                                        attribute_validity = new TreeMap<String, MatcherPattern>());
                    addAttributes(attributeValues, attribute_validity, mp);
                }
            }

            // } catch (RuntimeException e) {
            // int jj=1;
            // // System.err
            // // .println("Problem with: " + path + ", \t" + info.getValue());
            // e.printStackTrace();
            // }
        }
        if (!invalidLocales.isEmpty()) {
            int jjj = 1;
        }
        String path2 = metadata.iterator().next();
        final Comparator<String> comp;
        if (!path2.startsWith("//ldml")) {
            comp = null;
        } else {
            comp = metadata.getComparator();
        }
        for (String p : new Iterable<String>() {

            @Override
            public Iterator<String> iterator() {
                return metadata.iterator(comp);
            }

        }) {
            String value = metadata.getStringValue(p);
            String path = metadata.getFullXPath(p);
            parts.set(path);
            String lastElement = parts.getElement(-1);
            if (false) {
                System.out
                        .println("Path: "
                                + path
                                + (value != null && !value.isEmpty() ? " value:"
                                        + value
                                        : "") + " last Element: " + lastElement);
            }
            // if (lastElement.equals("elementOrder")) {
            // elementOrder.addAll(WHITESPACE_SPLTTER.splitToList(value.trim()));
            // } else if (lastElement.equals("attributeOrder")) {
            // attributeOrder.addAll(WHITESPACE_SPLTTER.splitToList(value.trim()));
            // } else
            if (lastElement.equals("suppress")) {
                // skip for now
                unhandledPaths.add("Unhandled path (suppress):" + path);
            } else if (lastElement.equals("serialElements")) {
                // skip for now
                // unhandledPaths.add("Unhandled path (serialElement):"+path);
            } else if (lastElement.equals("attributes")) {
                // skip for now
            }
            // else if (lastElement.equals("variable")) {
            // // String oldValue = value;
            // // value = variableReplacer.replace(value);
            // // if (!value.equals(oldValue)) System.out.println("\t" +
            // oldValue + " => " + value);
            // Map<String, String> attributes = parts.getAttributes(-1);
            // MatcherPattern mp = getMatcherPattern(value, attributes, path,
            // metadata.getSupplementalDataInfo());
            // if (mp != null) {
            // String id = attributes.get("id");
            // variables.put(id, mp);
            // // variableReplacer.add(id, value);
            // }
            // }
            // else if (lastElement.equals("attributeValues")) {
            // try {
            // Map<String, String> attributes = parts.getAttributes(-1);
            //
            // MatcherPattern mp = getMatcherPattern(value, attributes, path,
            // metadata.getSupplementalDataInfo());
            // if (mp == null) {
            // // System.out.println("Failed to make matcher for: " + value +
            // "\t" + path);
            // continue;
            // }
            // Iterable<String> attributeList
            // =WHITESPACE_SPLTTER.split(attributes.get("attributes").trim());
            // String elementsString = (String) attributes.get("elements");
            // if (elementsString == null) {
            // addAttributes(attributeList, common_attribute_validity, mp);
            // } else {
            // Iterable<String>
            // elementList=WHITESPACE_SPLTTER.split(elementsString.trim());
            // for (String element: elementList) {
            // Map<String, MatcherPattern> attribute_validity =
            // element_attribute_validity.get(element);
            // if (attribute_validity == null)
            // element_attribute_validity.put(element,
            // attribute_validity = new TreeMap<String, MatcherPattern>());
            // addAttributes(attributeList, attribute_validity, mp);
            // }
            // }
            //
            // } catch (RuntimeException e) {
            // System.err
            // .println("Problem with: " + path + ", \t" + value);
            // e.printStackTrace();
            // }
            // }
            else if (lastElement.equals("version")) {

                // skip for now
                // unhandledPaths.add("Unhandled path (version):"+path);
            } else if (lastElement.equals("generation")) {
                // skip for now
            } else if (lastElement.endsWith("Alias")) {
                String code = "$"
                        + lastElement.substring(0, lastElement.length() - 5);
                Map<String, String> type_replacement = code_type_replacement
                        .get(code);
                if (type_replacement == null) {
                    code_type_replacement.put(code,
                            type_replacement = new TreeMap<String, String>());
                }
                Map<String, String> attributes = parts.getAttributes(-1);
                String type = attributes.get("type");
                String replacement = attributes.get("replacement");
                if (replacement == null) {
                    replacement = "";
                }
                type_replacement.put(type, replacement);
            } else if (lastElement.equals("territoryAlias")) {
                // skip for now
                unhandledPaths.add("Unhandled path (territoryAlaias):" + path);
            } else if (lastElement.equals("deprecatedItems")) {
                // skip for now
                // unhandledPaths.add("Unhandled path (deprecatedItems):"+path);
            } else if (lastElement.endsWith("Coverage")) {
                // skip for now
                // skip for now
                unhandledPaths.add("Unhandled path (Coverage):" + path);
            } else if (lastElement.endsWith("skipDefaultLocale")) {
                // skip for now
                unhandledPaths
                        .add("Unhandled path (SkipdefaultLocale):" + path);
            } else if (lastElement.endsWith("defaultContent")) {
                // String locPath = extractValueList(path,"locales");
                // Iterable<String> locales=WHITESPACE_SPLTTER.split(locPath);
                // //Set<String> invalidLocales=new HashSet<>();
                // for (String loc: locales) {
                // if (!localeSet.contains(loc)) {
                // invalidLocales.add(loc);
                // }
                // }
                if (!invalidLocales.isEmpty()) {
                    unhandledPaths.add(invalidLocales.size()
                            + " invalid locales: "
                            + COMMA_JOINER.join(invalidLocales) + " Path:"
                            + path);
                }
                // unhandledPaths.add("Unhandled path (defaultContent):"+path);
            } else if (lastElement.endsWith("distinguishingItems")) {
                // skip for now
                // unhandledPaths.add("Unhandled path (distinguishingItems):"+path);
            } else if (lastElement.endsWith("blockingItems")) {
                // skip for now
                // .add("Unhandled path (blockingItems):"+path);
            } else if (lastElement.equals("variable")) {
                // skip for now
                // .add("Unhandled path (blockingItems):"+path);
            } else if (lastElement.equals("attributeValues")) {
                // skip for now
                // .add("Unhandled path (blockingItems):"+path);
            } else {
                System.out.println("Unknown final element: " + lastElement + " in " + path);
                unknownFinalElements.add(path);
            }
        }
    }

    private static String extractValueList(String path, String pattern) {
        // filter the path
        int pathStart = path.indexOf(pattern);
        String locPath = null;
        if (pathStart > -1) {
            int strStart = pathStart + pattern.length();
            int strEnd = path.indexOf("\"", strStart + 3);
            locPath = path.substring(strStart + 2, strEnd);
        }
        return locPath;
    }

    private MatcherPattern getBcp47MatcherPattern(SupplementalDataInfo sdi,
            String key) {
        MatcherPattern m = new MatcherPattern();
        Relation<R2<String, String>, String> bcp47Aliases = sdi
                .getBcp47Aliases();
        Set<String> values = new TreeSet<String>();
        for (String value : sdi.getBcp47Keys().getAll(key)) {
            if (key.equals("cu")) { // Currency codes are in upper case.
                values.add(value.toUpperCase());
            } else {
                values.add(value);
            }
            R2<String, String> keyValue = R2.of(key, value);
            Set<String> aliases = bcp47Aliases.getAll(keyValue);
            if (aliases != null) {
                values.addAll(aliases);
            }
        }

        // Special case exception for generic calendar, since we don't want to
        // expose it in bcp47
        if (key.equals("ca")) {
            values.add("generic");
        }

        m.value = key;
        m.pattern = values.toString();
        m.matcher = ObjectMatcherFactory.createCollectionMatcher(values);
        return m;

    }

    private MatcherPattern getMatcherPattern(String value,
            Map<String, String> attributes, String path,
            SupplementalDataInfo sdi) {
        String typeAttribute = attributes.get("type");
        MatcherPattern result = variables.get(value);
        if (result != null) {
            MatcherPattern temp = new MatcherPattern();
            temp.pattern = result.pattern;
            temp.matcher = result.matcher;
            temp.value = value;
            result = temp;
            if ("list".equals(typeAttribute)) {
                temp.matcher = ObjectMatcherFactory
                        .createListMatcher(result.matcher);
            }
            return result;
        }

        result = new MatcherPattern();
        result.pattern = value;
        result.value = value;
        if (typeAttribute == null) {
            if (value.startsWith("$")) {
                typeAttribute = "choice";
            }
        }
        if ("choice".equals(typeAttribute)
                || "given".equals(attributes.get("order"))) {
            List<String> valueList = WHITESPACE_SPLTTER.splitToList(value
                    .trim());
            result.matcher = ObjectMatcherFactory
                    .createCollectionMatcher(valueList);
        } else if ("bcp47".equals(typeAttribute)) {
            result = getBcp47MatcherPattern(sdi, value);
        } else if ("regex".equals(typeAttribute)) {
            result.matcher = ObjectMatcherFactory.createRegexMatcher(value,
                    Pattern.COMMENTS); // Pattern.COMMENTS to get whitespace
        } else if ("list".equals(typeAttribute)) {
            result.matcher = ObjectMatcherFactory.createRegexMatcher(value,
                    Pattern.COMMENTS); // Pattern.COMMENTS to get whitespace
        } else if ("locale".equals(typeAttribute)) {
            // locale: localeMatcher will test for language values, and fail
            // with a NPE.
            if (variables.containsKey("$language")) {
                result.matcher = LocaleMatcher.make();
            } else {
                // no language in the variables
                System.out
                        .println("Empty locale type element at Path: " + path);
                result.matcher = ObjectMatcherFactory
                        .createNullHandlingMatcher(variables, "$language", true);
            }
        } else if ("notDoneYet".equals(typeAttribute)
                || "notDoneYet".equals(value)) {
            result.matcher = ObjectMatcherFactory.createRegexMatcher(".*",
                    Pattern.COMMENTS);
        } else {
            System.out.println("unknown type; value: <" + value + ">,\t"
                    + typeAttribute + ",\t" + attributes + ",\t"
                    + path);
            return null;
        }
        return result;
    }

    private void addAttributes(Iterable<String> attributes,
            Map<String, MatcherPattern> attribute_validity, MatcherPattern mp) {
        for (String attribute : attributes) {
            MatcherPattern old = attribute_validity.get(attribute);
            if (old != null) {
                mp.matcher = ObjectMatcherFactory.createOrMatcher(old.matcher,
                        mp.matcher);
                mp.pattern = old.pattern + " OR " + mp.pattern;
            }
            attribute_validity.put(attribute, mp);
        }
    }

    /**
     * Test case: there should be no unknown final elements
     */
    public void TestNoUnhandledFinalElements() {
        for (String s : unknownFinalElements) {
            errln("The following final elements are unknown: " + s);
        }
    }

    /**
     * Test case: there should be no unhandled paths
     */
    public void TestNoUnhandledPaths() {
        if (!unhandledPaths.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(unhandledPaths.size() + " unhandled paths:\r\n");
            for (String p : unhandledPaths) {
                sb.append(p);
                sb.append("\r\n");
            }
            errln(sb.toString());
        }
    }

    /**
     * Simple PathValueHandler, simply adds the path to a set; has an accessor
     * for the set, and a method of clearing the set
     * 
     * @author ribnitz
     * 
     */
    private static class PathValueHandler extends XMLFileReader.SimpleHandler {
        private final Collection<String> paths = new LinkedHashSet<>();

        @Override
        public void handlePathValue(String path, String value) {
            paths.add(path);
        }

        public Collection<String> getPaths() {
            if (paths.isEmpty()) {
                return Collections.emptySet();
            }
            return ImmutableSet.copyOf(paths);
        }

        public void reset() {
            paths.clear();
        }
    }

    private Iterable<File> getSourceDirs() {
        String cldrDir = System.getProperty("CLDR_DIR");
        if (!cldrDir.endsWith("/")) {
            cldrDir += "/";
        }
        List<File> sourceFiles = new ArrayList<>();
        for (String sd : sourceDirs) {
            if (sd.startsWith("/")) {
                sourceFiles.add(new File(sd));
            } else {
                sourceFiles.add(new File(cldrDir + sd));
            }
        }
        return sourceFiles;
    }

    public void TestAttributes() {
        CLDRConfig cldrConf = CLDRConfig.getInstance();
        final Iterable<Path> fileSet = LocaleSetGenerator.generateLocaleSet(
                getSourceDirs(), FILE_EXCLUSIONS);
        final Set<File> dirs = new HashSet<>();
        for (Path cur : fileSet) {
            String curName = cur.getFileName().toString();
            localeSet.add(curName.substring(0, curName.lastIndexOf(".")));
            dirs.add(cur.getParent().toFile());
        }

        String factoryFilter = "(" + Joiner.on("|").join(localeSet) + ")";
        Factory cldrFactory = SimpleFactory.make(dirs.toArray(new File[0]),
                factoryFilter);
        // .setSupplementalDirectory(new
        // File(CLDRPaths.SUPPLEMENTAL_DIRECTORY));
        supplementalData = cldrConf.getSupplementalDataInfo();
        Set<String> availableLocales = cldrFactory.getAvailable();
        // System.out.println("Testing: en");
        List<CheckResult> results = new ArrayList<>();
        String rootLocaleID = cldrConf.getEnglish().getLocaleID();
        initialize(cldrConf.getEnglish(), results, cldrFactory,
                cldrFactory.getSupplementalDirectory(), rootLocaleID);
        Set<String> localesToTest = new HashSet<>(availableLocales);
        localesToTest.remove(rootLocaleID);
        int numTested = 1;
        XMLFileReader xfr = new XMLFileReader();
        PathValueHandler pvHandler = new PathValueHandler();
        xfr.setHandler(pvHandler);
        for (Path p : fileSet) {
            File f = p.toFile();
            String fName = p.getFileName().toString();
            String c = fName.substring(0, fName.lastIndexOf(".xml"));
            // for (String c:localesToTest) {
            //System.out.println("Testing: " + c);
            xfr.read(f.getAbsolutePath(), -1, false);
            initialize(pvHandler.getPaths(), results, cldrFactory,
                    cldrFactory.getSupplementalDirectory(), null);
            for (String curPath : pvHandler.getPaths()) {
                // handleCheck(curPath, curPath, "", results,c, f,c);
                handleCheck(curPath, curPath, "", results, c, c, f);
            }
            pvHandler.reset();
            numTested++;
        }
        List<CheckResult> warnings = Collections.emptyList();
        List<CheckResult> errors = Collections.emptyList();
        StringBuilder sb = new StringBuilder();
        // deprecation
        if (!deprecatedAttributeMap.isEmpty()) {
            listDeprecatedItems(false);
        }
        // did we get some errors or warnings?
        if (!results.isEmpty()) {
            warnings = FluentIterable.from(results)
                    .filter(new CheckResultPredicate(ResultStatus.warning))
                    .toList();
            errors = FluentIterable.from(results)
                    .filter(new CheckResultPredicate(ResultStatus.error))
                    .toList();

            System.out.println(getResultMessages(warnings, "warnings"));
            System.out.println(getResultMessages(errors, "errors"));
        }
        boolean successful = warnings.isEmpty() && errors.isEmpty();
        if (!successful) {
            if (csvOutput) {
                sb.setLength(0);
                compileProblemList(warnings, sb);
                compileProblemList(errors, sb);
                File errFile = new File(CSV_FILE);
                if (errFile.exists()) {
                    errFile.delete();
                }
                try {
                    if (errFile.createNewFile()) {
                        try (Writer wr = new BufferedWriter(new FileWriter(
                                errFile))) {
                            wr.write(sb.toString());
                        }
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            sb.setLength(0);
            if (errors.size() > 0) {
                sb.append(errors.size());
                sb.append(" errors");
            }
            if (errors.size() > 0 && warnings.size() > 0) {
                sb.append(" and ");
            }
            if (warnings.size() > 0) {
                sb.append(warnings.size());
                sb.append(" warnings");
            }
            sb.append("\r\n");
            if (!unhandledPaths.isEmpty()) {
                sb.append(unhandledPaths.size() + " unhandled paths:\r\n");
                for (String p : unhandledPaths) {
                    sb.append(p);
                    sb.append("\r\n");
                }
            }
            logln(sb.toString());
            System.out.println(numTested + " CLDRFiles tested");
        }

    }

    private void listDeprecatedItems(boolean verbose) {
        StringBuilder sb = new StringBuilder();
        List<Map.Entry<String, Collection<DeprecatedAttributeInfo>>> toSort = new ArrayList<>(
                deprecatedAttributeMap.asMap().entrySet());
        Collections
                .sort(toSort,
                        new Comparator<Map.Entry<String, Collection<DeprecatedAttributeInfo>>>() {
                            @Override
                            public int compare(
                                    Map.Entry<String, Collection<DeprecatedAttributeInfo>> e1,
                                    Map.Entry<String, Collection<DeprecatedAttributeInfo>> e2) {
                                return Ints.compare(e2.getValue().size(), e1
                                        .getValue().size());
                            }
                        });
        for (Map.Entry<String, Collection<DeprecatedAttributeInfo>> attr : toSort) {
            Collection<DeprecatedAttributeInfo> deprecatedItems = attr
                    .getValue();
            String key = attr.getKey();
            int size = deprecatedItems.size();
            sb.append("Deprecated attribute: " + key + ": "
                    + deprecatedItems.size());
            if (size > 1) {
                sb.append(" entries\r\n");
            } else {
                sb.append(" entry\r\n");
            }
            DeprecatedAttributeInfo info = deprecatedItems.iterator().next();
            Collection<String> replacements = info.getReplacements();
            if (!replacements.isEmpty()) {
                sb.append("Possible replacements: ");
                sb.append(COMMA_JOINER.join(replacements));
                // sb.append("\r\n");
            } else {
                sb.append("Consider removing\r\n");
            }
            System.out.println(sb.toString());
            sb.setLength(0);

            if (!verbose && deprecatedItems.size() > 5) {
                sb.append(listDeprecationDetails(new ArrayList<>(
                        deprecatedItems).subList(0, 5), false));
            } else {
                sb.append(listDeprecationDetails(deprecatedItems, false));
            }
        }
        System.out.print(sb.toString());
        sb.setLength(0);
    }

    private String listDeprecationDetails(
            Collection<DeprecatedAttributeInfo> deprecatedItems,
            boolean filterAliases) {
        StringBuilder sb = new StringBuilder();
        if (!filterAliases) {
            for (DeprecatedAttributeInfo info : deprecatedItems) {
                String locPath = info.getPathToLocaleFile();
                if (locPath != null) {
                    sb.append("Locale file: "
                            + CLDRDirFormatter.stripCLDRDir(locPath) + " ");
                }
                sb.append("Locale: " + info.getLocale() + " - Path: "
                        + info.getPath() + "\r\n");
            }
        } else {
            for (DeprecatedAttributeInfo info : deprecatedItems) {
                if (!info.getPath().contains("Alias")) {
                    sb.append("Locale: " + info.getLocale() + " - Path: "
                            + info.getPath() + "\r\n");
                }
            }
        }
        return sb.toString();
    }

    public void TestStaticInterface() {
        CLDRConfig config = CLDRConfig.getInstance();
        TestAttributeValues.performCheck(config.getEnglish(),
                config.getCldrFactory(), supplementalData,
                config.getEnglish().getLocaleID(), null);
    }

    private void compileProblemList(List<CheckResult> problems, StringBuilder sb) {
        if (csvOutput) {
            for (CheckResult problem : problems) {
                sb.append(SEMICOLON_JOINER.join(new Object[] {
                        problem.getStatus().name(),
                        problem.getLocale(),
                        problem.getMessage(),
                        problem.getPath() }));
                sb.append("\r\n");
            }
        } else {
            for (CheckResult problem : problems) {
                sb.append(problem.getStatus() + " " + problem.getLocale() + " "
                        + problem.getMessage());
                sb.append("\r\n");
            }
        }
    }

    /**
     * Perform a check of fileToCheck, providing the factory, and
     * SupplementalDataInfo
     * 
     * @param fileToCheck
     * @param fact
     * @param sdi
     * @return an Iterable over the results.
     */
    public static Iterable<CheckResult> performCheck(Iterable<String> paths,
            Factory fact, SupplementalDataInfo sdi,
            String localeID, String filename) {
        List<CheckResult> results = new ArrayList<>();
        TestAttributeValues tav = new TestAttributeValues();
        tav.supplementalData = sdi;
        tav.initialize(paths, results, fact, fact.getSupplementalDirectory(),
                localeID);
        File f = new File(filename);
        for (String curPath : paths) {
            tav.handleCheck(curPath, curPath, "", results, localeID, localeID,
                    f.canRead() ? f : null);
        }
        if (results.isEmpty()) {
            return Collections.emptyList();
        }
        return ImmutableList.copyOf(results);
    }

    /**
     * Static method to check a resource provided in an input stream, with the
     * given name
     * 
     * @param fInStream
     *            the input stream, will be closed
     * @param fileName
     *            the name of the resource, if will be resolved to a filename,
     *            if provided.
     * @return an Iterable of CheckResult items.
     */
    public static Iterable<CheckResult> performCheck(InputStream fInStream,
            String fileName) {
        XMLFileReader xfr = new XMLFileReader();
        PathValueHandler pvHandler = new PathValueHandler();
        xfr.setHandler(pvHandler);
        try (BufferedInputStream in = new BufferedInputStream(fInStream)) {
            xfr.read(fileName, fInStream, -1, false);
            Collection<String> paths = pvHandler.getPaths();
            CLDRConfig config = CLDRConfig.getInstance();
            return performCheck(paths, config.getCldrFactory(),
                    config.getSupplementalDataInfo(), null, fileName);
        } catch (IOException e) {
            CheckResult cr = new CheckResult().setStatus(ResultStatus.error)
                    .setMessage("IOException on reading file {0}: {1}",
                            new Object[] {
                                    fileName,
                                    e.getStackTrace()
                            });
            return ImmutableSet.of(cr);
        }
    }

    private String getResultMessages(Collection<CheckResult> result,
            String resultType) {
        StringBuilder sb = new StringBuilder();
        if (result.size() > 0) {
            sb.append("The following " + resultType + " occurred:");
            sb.append("\r\n");
            for (CheckResult cur : result) {
                sb.append(cur.getMessage());
                sb.append("\r\n");
            }
        }
        return sb.toString();
    }

    private void check(Map<String, MatcherPattern> attribute_validity,
            String attribute, String attributeValue, List<CheckResult> result,
            String path, String locale, Path localePath) {
        if (attribute_validity == null)
            return; // no test
        MatcherPattern matcherPattern = attribute_validity.get(attribute);
        if (matcherPattern == null)
            return; // no test
        if (matcherPattern.matcher.matches(attributeValue))
            return;
        // special check for deprecated codes
        String replacement = getReplacement(matcherPattern.value,
                attributeValue);
        boolean listDeprecated = shouldListDeprecated(attribute,
                attributeValue, path, locale);
        if (replacement != null) {
            // if (isEnglish) return; // don't flag English
            if (replacement.length() == 0) {
                if (listDeprecated) {
                    deprecatedAttributeMap.put(
                            attribute + "=" + attributeValue,
                            new DeprecatedAttributeInfo(locale, path,
                                    localePath != null ? localePath.toFile()
                                            .getAbsolutePath() : null));
                }
            } else {
                if (listDeprecated) {
                    deprecatedAttributeMap
                            .put(attribute + "=" + attributeValue,
                                    new DeprecatedAttributeInfo(locale, path,
                                            WHITESPACE_SPLTTER
                                                    .splitToList(replacement),
                                            localePath != null ? localePath
                                                    .toFile().getAbsolutePath()
                                                    : null));
                }
            }
        } else {
            String pattern = matcherPattern.pattern;
            // for now: disregard missing variable expansions
            if (pattern != null && !pattern.trim().startsWith("$")) {
                // does the attributeValue contain spaces?
                List<String> elemList = WHITESPACE_SPLTTER
                        .splitToList(attributeValue);
                List<String> disallowedElems = new ArrayList<>();

                if (elemList.size() > 1) {
                    Collection<String> allowedElements = WHITESPACE_SPLTTER
                            .splitToList(matcherPattern.pattern);
                    // is this likely a regex?
                    boolean likelyRegex = isLikelyRegex(allowedElements);
                    if (likelyRegex) {
                        Pattern pat = PatternCache.get(matcherPattern.pattern);
                        for (String elem : elemList) {
                            if (!pat.matcher(elem).matches()) {
                                disallowedElems.add(elem);
                            }
                        }
                    } else {
                        for (String elem : elemList) {
                            if (!allowedElements.contains(elem)) {
                                disallowedElems.add(elem);
                            }
                        }
                    }
                    if (!disallowedElems.isEmpty()) {
                        Object[] params = new Object[] {
                                localePath != null ? CLDRDirFormatter
                                        .stripCLDRDir(localePath.toFile())
                                        : locale,
                                attribute,
                                COMMA_JOINER.join(disallowedElems),
                                COMMA_JOINER.join(allowedElements),
                                path
                        };
                        result.add(new CheckResult()
                                .setStatus(ResultStatus.warning)
                                .setLocale(locale)
                                .setPath(path)
                                .setMessage(
                                        "Locale {0}: Unexpected attribute value {1}={2}: expected: {3}; please add to supplemental "
                                                +
                                                "data. Path: {4}",
                                        params));
                    }
                } else {
                    // root locale?
                    if (locale.equals(CLDRConfig.getInstance().getEnglish()
                            .getLocaleID())) {
                        // root locale
                        String mp = null;
                        if (matcherPattern.pattern.contains(" ")) {
                            mp = COMMA_JOINER.join(WHITESPACE_SPLTTER
                                    .split((matcherPattern.pattern)));
                        } else {
                            mp = matcherPattern.pattern;
                        }
                        result.add(new CheckResult()
                                .setStatus(ResultStatus.warning)
                                .setLocale(locale)
                                .setPath(path)
                                .setMessage(
                                        "Locale {0}: Unexpected attribute value {1}={2}: expected: {3}; please add to supplemental data. Path: {4}",
                                        new Object[] { locale, attribute,
                                                attributeValue, mp, path }));
                        matcherPattern.matcher = ObjectMatcherFactory
                                .createOrMatcher(
                                        matcherPattern.matcher,
                                        ObjectMatcherFactory
                                                .createStringMatcher(attributeValue));
                    } else {
                        // the values obtained at this point are of the form [
                        // A, B, C, D,..]
                        String patStr = matcherPattern.pattern;
                        List<String> values = null;
                        if (patStr.startsWith("[") && patStr.endsWith("]")) {
                            patStr = patStr.substring(1,
                                    patStr.lastIndexOf("]"));

                            values = COMMA_SPLITTER.splitToList(patStr);
                        }

                        /*
                         * Some currency data sets have an entry DEFAULT which
                         * does not correspond to a currency.
                         */
                        if (!attributeValue.equals("DEFAULT")) {
                            String replacedPattern;
                            if (values != null && !values.isEmpty()) {
                                replacedPattern = COMMA_JOINER.join(values);
                            } else {
                                replacedPattern = matcherPattern.pattern;
                            }
                            Object[] param = new Object[] {
                                    localePath != null ? CLDRDirFormatter
                                            .stripCLDRDir(localePath.toFile())
                                            : locale,
                                    attribute,
                                    attributeValue,
                                    replacedPattern,
                                    path };
                            CheckResult cr = CheckResult
                                    .create(ResultStatus.error,
                                            locale,
                                            path,
                                            new CheckCSVValuePred(),
                                            "Locale {0}: Unexpected attribute value {1}={2}: expected: {3}",
                                            "Locale {0}: Unexpected attribute value {1}={2}: expected: {3}  Path: {4}",
                                            param, param);
                            result.add(cr);
                        }
                    }
                }
            }
        }
    }

    private final static Set<String> REGEX_INDICATORS_CONTAINMENT = ImmutableSet
            .of("[", "]");
    private final static Set<String> REGEX_INDICATORS_END = ImmutableSet
            .of("(");

    private static boolean isLikelyRegex(Iterable<String> allowedElements) {
        for (String item : allowedElements) {
            for (String contained : REGEX_INDICATORS_CONTAINMENT) {
                if (item.contains(contained)) {
                    return true;
                }
            }
            for (String end : REGEX_INDICATORS_END) {
                if (item.endsWith(end)) {
                    return true;
                }
            }

        }
        return false;
    }

    /**
     * This method returning true means that the item will be listed as
     * deprecated
     */
    private boolean shouldListDeprecated(String attribute,
            String attributeValue, String path, String locale) {
        boolean isMetadataAlias = path.contains("metadata/alias");
        if (locale.equals("en")) {
            return false;
        }
        if (DEPRECATION_OVERRIDES.contains(attributeValue)) {
            return false;
        }
        if (attribute.equals("type") && isMetadataAlias) {
            return false;
        }
        if (path.contains("currencyData")) {
            return false;
        }
        if (path.contains("[@status=\"deprecated\"]")) {
            return false;
        }
        return true;
    }

    private void handleCheck(String path, String fullPath, String value,
            List<CheckResult> result, String localeId, String sourceLocaleId,
            File localeFile) {

        if (fullPath == null)
            return; // skip paths that we don't have
        if (fullPath.indexOf('[') < 0)
            return; // skip paths with no attributes

        pluralInfo = supplementalData.getPlurals(PluralType.cardinal,
                sourceLocaleId);
        PluralInfo ordinalPlurals = supplementalData.getPlurals(
                PluralType.ordinal, sourceLocaleId);

        PluralInfo pInfo;
        // skip paths that are not in the immediate locale
        if (!localeId.equals(sourceLocaleId)) {
            return;
        }
        parts.set(fullPath);
        for (int i = 0; i < parts.size(); ++i) {
            if (parts.getAttributeCount(i) == 0)
                continue;
            Map<String, String> attributes = parts.getAttributes(i);
            String element = parts.getElement(i);

            Map<String, MatcherPattern> attribute_validity = element_attribute_validity
                    .get(element);
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                String attribute = entry.getKey();
                String attributeValue = entry.getValue();
                // check the common attributes first
                check(common_attribute_validity, attribute, attributeValue,
                        result, fullPath, sourceLocaleId, localeFile.toPath());
                // then for the specific element
                check(attribute_validity, attribute, attributeValue, result,
                        fullPath, sourceLocaleId, localeFile.toPath());

                // now for plurals
                if (attribute.equals("count")) {
                    if (DIGITS.containsAll(attributeValue)) {
                        // ok, keep going
                    } else {
                        final Count countValue = PluralInfo.Count
                                .valueOf(attributeValue);
                        // cardinal or ordinal?
                        String pluralType = extractValueList(fullPath, "type");
                        if (pluralType != null
                                && (pluralType.equals("cardinal") || pluralType
                                        .equals("ordinal"))) {
                            if (pluralType.equals("cardinal")) {
                                pInfo = pluralInfo;
                            }
                            else {
                                pInfo = ordinalPlurals;
                            }
                            Set<Count> cSet = pInfo.getCounts();

                            if (cSet.size() == 1) {

                                // set of only one entry; this means: only check
                                // if the value is one of several allowed ones,
                                // w/o respect to the language
                                if (!Count.VALUES.contains(countValue)) {
                                    String strippedPath = null;
                                    if (localeFile != null) {
                                        strippedPath = CLDRDirFormatter
                                                .stripCLDRDir(localeFile);
                                    }

                                    result.add(
                                            new CheckResult()
                                                    .setStatus(
                                                            ResultStatus.error)
                                                    .setPath(path)
                                                    .setMessage(
                                                            "Locale {0}: Illegal plural value {1}; must be one of: {2} Path: {3}",
                                                            new Object[] {
                                                                    strippedPath != null ? strippedPath
                                                                            : sourceLocaleId,
                                                                    countValue,
                                                                    pluralInfo
                                                                            .getCounts(),
                                                                    path }));
                                }

                            } else if (!cSet.contains(countValue)
                                    && !isPluralException(countValue,
                                            sourceLocaleId)) {
                                String strippedPath = null;
                                if (localeFile != null) {
                                    strippedPath = CLDRDirFormatter
                                            .stripCLDRDir(localeFile);
                                }
                                result.add(
                                        new CheckResult()
                                                .setStatus(ResultStatus.error)
                                                .setPath(path)
                                                .setMessage(
                                                        "Locale {0}: Illegal plural value {1}; must be one of: {2} Path: {3}",
                                                        new Object[] {
                                                                strippedPath != null ? strippedPath
                                                                        : sourceLocaleId,
                                                                countValue,
                                                                pluralInfo
                                                                        .getCounts(),
                                                                path }));
                            }
                        }
                    }
                }
            }
        }
    }

    /***
     * Helper class for formatting a Path, by only listing the elements below
     * CDLR_DIR if the path is under that directory
     * 
     * @author ribnitz
     * 
     */
    private static class CLDRDirFormatter {
        /**
         * The CLDR_DIR property
         */
        private final static String CLDR_DIR = System.getProperty("CLDR_DIR");

        /**
         * Strip the CLDR_DIR component from the path provided, if the path is
         * below CLDR_DIR
         * 
         * @param path
         * @return
         */
        public static String stripCLDRDir(String path) {
            return stripCLDRDir(new File(path));
        }

        /***
         * Helper method that will remove the path up to the directory specified
         * in CLDR_DIR, and replace that part with "{CLDR_DIR}/", if that part
         * of the path matches
         * 
         * @param localeFile
         * @return
         */
        public static String stripCLDRDir(File localeFile) {
            if (localeFile == null) {
                throw new IllegalArgumentException(
                        "Please call with non-null file");
            }

            String canonical = localeFile.getAbsolutePath();
            if (canonical.lastIndexOf(CLDR_DIR) != -1) {
                int startPos = canonical.lastIndexOf(CLDR_DIR)
                        + CLDR_DIR.length();
                return "{CLDR_DIR}/" + canonical.substring(startPos);
            }
            return localeFile.getAbsolutePath();
        }
    }

    public static void main(String[] args) {
        TestAttributeValues tav = new TestAttributeValues();
        tav.run(args);
    }
}
