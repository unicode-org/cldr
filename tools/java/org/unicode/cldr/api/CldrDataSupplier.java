package org.unicode.cldr.api;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static org.unicode.cldr.api.CldrDataType.LDML;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.unicode.cldr.api.CldrData.PrefixVisitor;
import org.unicode.cldr.api.CldrData.ValueVisitor;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SimpleFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

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
    /**
     * Returns the current CLDR version string (e.g. {@code "36"}). This is just wrapping the
     * underlying CLDR version string to avoid users needing to import anything from outside the
     * "api" package.
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
        UNRESOLVED
    }

    /**
     * Returns a supplier for CLDR data in the specified CLDR project root directory. This must be
     * a directory which contains the standard CLDR {@code "common"} directory file hierarchy.
     *
     * @param cldrRootDir the root directory of a CLDR project containing the data to be read.
     * @return a supplier for CLDR data in the given path.
     */
    public static CldrDataSupplier forCldrFilesIn(Path cldrRootDir) {
        // Note that, unlike "withDraftStatusAtLeast()", adding a new fluent method to support
        // additional root directories is problematic, since:
        // 1) directories are conceptually only important for FileBasedDataSupplier (so a new
        //    fluent method in the supplier API makes no sense for other implementations).
        // 2) creating the directory map must happen before the supplier is returned (rather than
        //    just before it supplies any data) because of the getAvailableLocaleIds() method.
        //
        // Thus it seems better to just add an extra parameter to this method when/if needed.
        // TODO: Extend the API to allow source roots to be specified (but not via directory name).
        Set<String> rootDirs = ImmutableSet.of("common");
        return new FileBasedDataSupplier(
            createCldrDirectoryMap(cldrRootDir, rootDirs), CldrDraftStatus.UNCONFIRMED);
    }

    /**
     * Returns an unresolved CLDR data instance of a set of XML file. This is typically only used
     * for accessing additional CLDR data outside the CLDR project directories. The data in the
     * specified files is merged, and it is a error if the same path appears multiple times (i.e.
     * this input file must be "disjoint" in terms of the CLDR paths they specify).
     *
     * @param type the expected CLDR type of the data in the XML file.
     * @param draftStatus the desired status for filtering paths/values.
     * @param xmlFiles the CLDR XML files.
     * @return a data instance for the paths/values in the specified XML file.
     */
    public static CldrData forCldrFiles(
        CldrDataType type, CldrDraftStatus draftStatus, Set<Path> xmlFiles) {
        return new XmlDataSource(type, ImmutableSet.copyOf(xmlFiles), draftStatus);
    }

    private static Multimap<CldrDataType, Path> createCldrDirectoryMap(
        Path cldrRootDir, Set<String> rootDirs) {

        LinkedHashMultimap<CldrDataType, Path> multimap = LinkedHashMultimap.create();
        for (CldrDataType type : CldrDataType.values()) {
            type.getSourceDirectories()
                .flatMap(d -> rootDirs.stream().map(r -> cldrRootDir.resolve(r).resolve(d)))
                .filter(Files::isDirectory)
                .forEach(p -> multimap.put(type, p));
        }
        return multimap;
    }

    /**
     * Returns an in-memory supplier for the specified {@link CldrValue}s. This is useful for
     * testing or handling special case data. The default (arbitrary) path order is determined by
     * the order of values passed to this method.
     *
     * @param values the values (and associated paths) to include in the returned data.
     */
    public static CldrData forValues(Iterable<CldrValue> values) {
        return new InMemoryData(values);
    }

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
     * Returns an LDML data instance for the specified locale ID.
     *
     * <p>If {@code resolution} is set to {@link CldrResolution#RESOLVED RESOLVED} then values
     * inferred from parent locales and aliases will be produced by the supplier. Note that if an
     * unsupported locale ID is given (i.e. one not in the set returned by
     * {@link #getAvailableLocaleIds()}), then an empty data instance is returned.
     *
     * @param localeId the locale ID (e.g. "en_GB" or "root") for the returned data.
     * @param resolution whether to resolve CLDR values for the given locale ID according to the
     *     CLDR specification.
     * @return the specified locale based CLDR data (possibly empty).
     * @throws IllegalArgumentException if the locale ID is not structurally valid.
     */
    public abstract CldrData getDataForLocale(String localeId, CldrResolution resolution);

    /**
     * Returns an unmodifiable set of available locale IDs that this supplier can provide. This
     * need not be ordered.
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
            LocaleIds.checkCldrLocaleId(localeId);
            Factory factory = getFactory();
            if (factory.getAvailable().contains(localeId)) {
                return new CldrFileDataSource(
                    factory.make(localeId, resolution == CldrResolution.RESOLVED));
            }
            return NO_DATA;
        }

        @Override
        public Set<String> getAvailableLocaleIds() {
            return getFactory().getAvailable();
        }

        @Override
        public CldrData getDataForType(CldrDataType type) {
            ImmutableSet<Path> xmlFiles = listXmlFilesForType(type);
            if (!xmlFiles.isEmpty()) {
                return new XmlDataSource(type, xmlFiles, draftStatus);
            }
            return NO_DATA;
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
        // Note: "Files.walk()" warns about closing resources and suggests "try-with-resources" to
        // ensure closure, "flatMap()" (which is what calls this method) is defined to call close()
        // on each stream as it's added into the result, so in normal use this should all be fine.
        //
        // https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html#flatMap-java.util.function.Function-
        private static Stream<Path> listXmlFiles(Path dir) {
            try {
                return Files.walk(dir).filter(IS_XML_FILE);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private static final Predicate<Path> IS_XML_FILE =
            p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".xml");
    }

    private static final CldrData NO_DATA = new CldrData() {
        @Override public void accept(PathOrder order, ValueVisitor visitor) {}

        @Override public void accept(PathOrder order, PrefixVisitor visitor) {}

        @Override public CldrValue get(CldrPath path) {
            return null;
        }
    };
}
