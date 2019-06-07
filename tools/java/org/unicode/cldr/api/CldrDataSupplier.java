package org.unicode.cldr.api;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.unicode.cldr.api.CldrData.PrefixVisitor;
import org.unicode.cldr.api.CldrData.ValueVisitor;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SimpleFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static org.unicode.cldr.api.CldrDataType.LDML;

/**
 * The main API for accessing {@link CldrPath} and {@link CldrValue} instances for CLDR data. This
 * API abstracts the data sources, file names and other implementation details of CLDR to provide
 * a clean way to access CLDR data.
 *
 * <p>{@code CldrData} instances are obtained from an appropriate {@code CldrDataSupplier}, and
 * accept a {@link ValueVisitor} or {@link PrefixVisitor} to iterate over the data.
 *
 * <p>For example the following code prints every value (including its associated distinguishing
 * path) in the BCP-47 data in DTD order:
 * <pre>{@code
 *   CldrDataSupplier supplier = CldrDataSupplier.forFilesIn(rootDir);
 *   CldrData bcp47Data = supplier.getDataForType(CldrDataType.BCP47);
 *   bcp47Data.accept(PathOrder.DTD, System.out::println);
 * }</pre>
 *
 * <p>Note that while the paths of values visited in a single {@link CldrData} instance are unique,
 * there is nothing to prevent duplication between multiple data sources. This is particularly
 * important when considering "ordered" elements with a sort index, since it represents "encounter
 * order" and so any merging of values would have to track and rewrite sort indices carefully. It
 * is recommended that if multiple {@code CldrData} instances are to be processed, users ensure
 * that no path prefixes be shared between them. See also {@link CldrPath#getSortIndex()}.
 *
 * <p>Note that because the distinguishing paths associated with a {@link CldrValue} are unique per
 * visitation, the special "version" path/value must be omitted (e.g. "//ldml/version") since it
 * would otherwise appear multiple times. This should be fine, since the version is always available
 * via {@link #getCldrVersionString()} and this mechanism is scheduled for deprecation anyway.
 */
public abstract class CldrDataSupplier {
    // The set of top level directories could be more than these, but the API should not require
    // callers to know about directory names or structure itself, so if this is to be configurable
    // then it should be via methods like "withSeedFiles()" or similar. For now the "seed" directory
    // is included, but others, such as "exemplars" and "keyboards" are not. This is sufficient for
    // the API's current use, but will ultimately need addressing.
    //
    // TODO: Extend the API to allow source roots to be specified (but not via directory name).
    private static final ImmutableSet<String> ROOT_DIRECTORIES =
        ImmutableSet.of("common", "seed");

    /**
     * Returns the current CLDR version string (e.g. {@code "36"}). This is just wrapping the
     * underlying CLDR version string to avoid callers needing to import anything from outside the
     * "icu" API package.
     */
    public static String getCldrVersionString() {
        return CLDRFile.GEN_VERSION;
    }

    /** Options for controlling how locale-based LDML data is processed. */
    public enum CldrResolution {
        /**
         * Locale-based CLDR data should include resolved values from other "parent" locales
         * according to the CLDR specification.
         */
        RESOLVED,

        /**
         * Locale-based CLDR data should only include values specified directly in the specified
         * locale.
         */
        UNRESOLVED;
    }

    /**
     * Returns a supplier for CLDR data in the specified CLDR project root directory. This must be
     * a directory which contains the standard CLDR {@code "common"} directory file hierarchy.
     *
     * @param cldrRootDir the root directory of a CLDR project containing the data to be read.
     * @return a supplier for CLDR data in the given path.
     */
    public static CldrDataSupplier forCldrFilesIn(Path cldrRootDir) {
        return new FileBasedDataSupplier(
            createCldrDirectoryMap(cldrRootDir), CldrDraftStatus.UNCONFIRMED);
    }

    /**
     * Returns an unresolved CLDR data instance of a single XML file. This is typically only used
     * for accessing additional CLDR data outside the CLDR project directories.
     *
     * @param type the expected CLDR type of the data in the XML file.
     * @param xmlFile the CLDR XML file.
     * @param draftStatus the desired status for filtering paths/values.
     * @return a data instance for the paths/values in the specified XML file.
     */
    public static CldrData forCldrFile(CldrDataType type, Path xmlFile, CldrDraftStatus draftStatus) {
        return new XmlDataSource(type, ImmutableSet.of(xmlFile), draftStatus);
    }

    private static Multimap<CldrDataType, Path> createCldrDirectoryMap(Path cldrRootDir) {
        LinkedHashMultimap<CldrDataType, Path> multimap = LinkedHashMultimap.create();
        for (CldrDataType type : CldrDataType.values()) {
            type.getSourceDirectories()
                .flatMap(d -> ROOT_DIRECTORIES.stream().map(r -> cldrRootDir.resolve(r).resolve(d)))
                .filter(Files::isDirectory)
                .forEach(p -> multimap.put(type, p));
        }
        return multimap;
    }

    /**
     * Returns an in-memory supplier for the specified {@link CldrValue}s. This is useful for
     * testing or handling special case data.
     * @param values
     */
    // Note: This could be made public if necessary.
    static CldrData forValues(Iterable<CldrValue> values) {
        return new InMemoryData(values);
    }

    // Package protected to keep suppliers in a closed type hierarchy.
    CldrDataSupplier() {}

    /**
     * Returns a modified data supplier which only provides paths/values with a draft status at or
     * above the specified value. To create a supplier that will process all CLDR paths/values, use
     * {@link CldrDraftStatus#UNCONFIRMED UNCONFIRMED}.
     *
     * @param draftStatus the desired status for filtering paths/values.
     * @return a modified supplier which filters by the specified status.
     */
    public abstract CldrDataSupplier withDraftStatusAtLeast(CldrDraftStatus draftStatus);

    /**
     * Returns an LDML data supplier for the specified locale ID.
     *
     * <p>If {@code resolution} is set to {@link CldrResolution#RESOLVED RESOLVED} then values
     * inferred from parent locales and aliases will be produced by the supplier.
     *
     * @param localeId the locale ID (e.g. "en_GB") for the returned data.
     * @param resolution whether to resolve CLDR values for the given locale ID according to the
     *                   CLDR specification.
     * @return the specified locale based CLDR data.
     */
    public abstract CldrData getDataForLocale(String localeId, CldrResolution resolution);

    /**
     * Returns an unmodifiable set of available locale IDs that this supplier can provide.
     *
     * @return the set of available locale IDs.
     */
    public abstract Set<String> getAvailableLocaleIds();

    /**
     * Returns a data supplier for non-locale specific CLDR data of the given type.
     *
     * @param type the required non-{@link CldrDataType#LDML LDML} data type.
     * @return the specified non-locale based CLDR data.
     * @throws IllegalArgumentException if {@link CldrDataType#LDML} is given.
     */
    public abstract CldrData getDataForType(CldrDataType type);

    private static final class FileBasedDataSupplier extends CldrDataSupplier {
        private final ImmutableSetMultimap<CldrDataType, Path> directoryMap;
        private final CldrDraftStatus draftStatus;

        // Created on-demand to keep constructor simple (in a fluent API you might create several
        // variants of a supplier but only get data from one, or only use non-LDML XML data).
        private Factory factory = null;

        private FileBasedDataSupplier(
            Multimap<CldrDataType, Path> directoryMap, CldrDraftStatus draftStatus) {
            this.directoryMap = ImmutableSetMultimap.copyOf(directoryMap);
            this.draftStatus = checkNotNull(draftStatus);
        }

        // Locking should be no issue, since contention on these supplier instance is expected to
        // be minimal.
        private synchronized Factory getFactory() {
            if (factory == null) {
                File[] dirArray =
                    getDirectoriesForType(LDML).map(Path::toFile).toArray(File[]::new);
                checkArgument(dirArray.length > 0,
                    "no LDML directories exist: %s", directoryMap.get(LDML));
                factory = SimpleFactory.make(dirArray, ".*", draftStatus.getRawStatus());
            }
            return factory;
        }

        @Override
        public CldrDataSupplier withDraftStatusAtLeast(CldrDraftStatus draftStatus) {
            return new FileBasedDataSupplier(directoryMap, draftStatus);
        }

        @Override
        public CldrData getDataForLocale(String localeId, CldrResolution resolution) {
            return new CldrFileDataSource(
                getFactory().make(localeId, resolution == CldrResolution.RESOLVED));
        }

        @Override
        public Set<String> getAvailableLocaleIds() {
            return getFactory().getAvailable();
        }

        @Override
        public CldrData getDataForType(CldrDataType type) {
            return new XmlDataSource(type, listXmlFilesForType(type), draftStatus);
        }

        private Stream<Path> getDirectoriesForType(CldrDataType type) {
            return directoryMap.get(type).stream().filter(Files::exists);
        }

        private ImmutableSet<Path> listXmlFilesForType(CldrDataType type) {
            ImmutableSet<Path> xmlFiles = getDirectoriesForType(type)
                .flatMap(FileBasedDataSupplier::listXmlFiles)
                .collect(toImmutableSet());
            checkArgument(!xmlFiles.isEmpty(),
                "no XML files exist within directories: %s", directoryMap.get(type));
            return xmlFiles;
        }

        // This is a separate function because stream functions cannot throw checked exceptions.
        //
        // Note: "Files.list()" warns about closing resources and suggests "try-with-resources" to
        // ensure closure, "flatMap()" (which is what calls this method) is defined to call close()
        // on each stream as it's added into the result, so in normal use this should all be fine.
        //
        // https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html#flatMap-java.util.function.Function-
        private static Stream<Path> listXmlFiles(Path dir) {
            try {
                return Files.list(dir).filter(IS_XML_FILE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private static final Predicate<Path> IS_XML_FILE =
            p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".xml");
    }
}
