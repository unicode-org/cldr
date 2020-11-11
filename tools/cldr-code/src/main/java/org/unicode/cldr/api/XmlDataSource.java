package org.unicode.cldr.api;

import static com.google.common.base.CharMatcher.whitespace;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;

/** Serializes a set of LDML XML files as a sequence of {@code CldrValue}s. */
final class XmlDataSource implements CldrData {
    private static final Splitter TRIMMING_LINE_SPLITTER =
        Splitter.on('\n').trimResults().omitEmptyStrings();
    private static final CharMatcher NOT_WHITESPACE = whitespace().negate();

    private final CldrDataType dtdType;
    private final ImmutableSet<Path> xmlFiles;
    private final CldrDraftStatus minimalDraftStatus;
    private final Function<Path, Reader> openFn;

    // Memoized data map to avoid loading/parsing files more than once.
    private volatile ImmutableMap<CldrPath, CldrValue> pathValueMap = null;
    // Whether the memoized map is in DTD order (we re-sort after it's cached).
    private volatile boolean isDtdOrder = false;
    // Lock to protect state of memoized map and ordering flag. These fields are volatile because
    // we're using "double checked locking" to read the cached map.
    // See: https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
    private final Object cacheLock = new Object();

    // TODO: Handle draft status properly (it's a METADATA attribute).
    //
    // It LOOKS like the current code (see XMLSource and SimpleXMLSource) will handle multiple
    // equivalent distinguishing paths with draft status by preserving the _last_ one found into
    // the CLDRFile. This is awkward if we just want to stream the paths directly from the XML,
    // since we are not keeping track of the paths already seen (maybe we should).
    //
    // However in practice, there's only ever one path with draft status present in the non-LDML
    // files, so it should be enough to simply include/exclude based on the status (and maybe
    // ignore the possibility of the same distinguishing path appearing twice??)
    //
    // It terms of having multiple draft status attributes on a path, it seems that the current
    // CLDRFile code has a "top-most one wins" strategy, which can be affected in this code by
    // simply setting draft status the first time it's present on an element.
    XmlDataSource(CldrDataType dtdType, Set<Path> xmlFiles, CldrDraftStatus draftStatus) {
        this(dtdType, xmlFiles, draftStatus, XmlDataSource::openFile);
    }

    // @VisibleForTesting
    XmlDataSource(
        CldrDataType dtdType, Set<Path> xmlFiles, CldrDraftStatus draftStatus, Function<Path, Reader> openFn) {
        this.xmlFiles = ImmutableSet.copyOf(xmlFiles);
        this.dtdType = dtdType;
        this.minimalDraftStatus = checkNotNull(draftStatus);
        this.openFn = checkNotNull(openFn);
    }

    private Map<CldrPath, CldrValue> getPathValueMap(PathOrder order) {
        // XML is always at least using nested grouping, so the only question is whether to sort it
        // into DTD order or not. Obviously this changes if there's ever another ordering possible.
        boolean mustSort = (order == PathOrder.DTD);
        ImmutableMap<CldrPath, CldrValue> localMapRef = pathValueMap;
        if (localMapRef == null) {
            Map<CldrPath, CldrValue> map = mustSort ? new TreeMap<>() : new LinkedHashMap<>();
            read(value -> map.put(value.getPath(), value), dtdType, true);
            // Avoid work with the lock held...
            localMapRef = ImmutableMap.copyOf(map);
            // There's a race condition here whereby two threads can decide to create the map
            // but in different orders and then the flags get out of sync with the map contents.
            synchronized (cacheLock) {
                if (pathValueMap == null) {
                    this.pathValueMap = localMapRef;
                    this.isDtdOrder = mustSort;
                    return localMapRef;
                }
            }
        }
        // Path value map exists, but might need resorting (don't read the dtd order earlier since
        // it could have just been modified by another thread).
        if (mustSort && !isDtdOrder) {
            ImmutableMap.Builder<CldrPath, CldrValue> map = ImmutableSortedMap.naturalOrder();
            localMapRef = map.putAll(pathValueMap).build();
            synchronized (cacheLock) {
                if (!isDtdOrder) {
                    this.pathValueMap = localMapRef;
                    this.isDtdOrder = true;
                }
            }
        }
        return localMapRef;
    }

    @Override
    public void accept(PathOrder order, ValueVisitor visitor) {
        getPathValueMap(order).values().forEach(visitor::visit);
    }

    @Override
    public CldrValue get(CldrPath path) {
        return getPathValueMap(PathOrder.ARBITRARY).get(path);
    }

    // Helper used to open files but which allows alternate implementation for in-memory testing.
    private static Reader openFile(Path p) {
        try {
            return Files.newBufferedReader(p);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void read(ValueVisitor visitor, CldrDataType dtdType, boolean validating) {
        XMLReader xmlReader = createXmlReader(validating);
        xmlReader.setErrorHandler(ERROR_HANDLER);
        xmlReader.setContentHandler(new PathValueHandler(visitor, dtdType));
        for (Path p : xmlFiles) {
            try (Reader r = openFn.apply(p)) {
                InputSource src = new InputSource(r);
                // Important: The system ID is a URI or path which should identify the XML file so
                // that a relative path to the DTD can be resolved. Thus if the XML contains
                // <!DOCTYPE ldmlBCP47 SYSTEM "../../common/dtd/ldmlBCP47.dtd">
                // then the location of "ldmlBCP47.dtd" can be properly determined. Thus even for
                // testing, a suitable path from which the DTD can be determined must be used.
                src.setSystemId(p.toString());
                parseXml(xmlReader, src, p);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static void parseXml(XMLReader xmlReader, InputSource src, Path path) {
        try {
            xmlReader.parse(src);
        } catch (IncompatibleDtdException e) {
            // Do nothing and just exit if the root element is not for our expected DTD. This is
            // fine, since we currently look for an XML files to parse, so cannot have strong
            // expectations.
        } catch (SAXParseException e) {
            throw new IllegalArgumentException(
                "error reading " + path + " (line " + e.getLineNumber() + ")", e);
        } catch (SAXException | IOException e) {
            throw new IllegalArgumentException("error reading " + path, e);
        } catch (RuntimeException e) {
            // TODO: Solve this properly by using a parser that we completely control.
            throw new RuntimeException("\n"
                + "------------------------------------------------------------------\n"
                + "Unknown error reading " + path + "\n"
                + "\n"
                + "This can sometimes be caused by using a version of Java which does\n"
                + "not support all the XML parsing features required by this tool.\n"
                + "Try setting the JAVA_HOME environment variable to a different Java\n"
                + "release, if possible.\n"
                + "------------------------------------------------------------------\n>",
                e);
        }
    }

    private static XMLReader createXmlReader(boolean validating) {
        XMLReader xmlReader;
        try {
            xmlReader = XMLReaderFactory.createXMLReader();
            xmlReader.setFeature("http://xml.org/sax/features/validation", validating);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
        return xmlReader;
    }

    private final class PathValueHandler extends DefaultHandler {
        private final ValueVisitor visitor;
        private final CldrDataType dataType;
        private final Multiset<String> orderedElementIndices = HashMultiset.create();

        private final Map<AttributeKey, String> valueAttributes = new LinkedHashMap<>();
        private CldrPath path = null;
        // You can get multiple calls to "characters()" for a single element.
        private StringBuilder elementText = new StringBuilder();
        private boolean wasLeafElement = true;

        private PathValueHandler(ValueVisitor visitor, CldrDataType dataType) {
            this.visitor = visitor;
            this.dataType = dataType;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if (path == null && !qName.equals(dataType.getLdmlName())) {
                throw new IncompatibleDtdException();
            }
            int sortIndex = -1;
            if (CldrPaths.isOrdered(dataType, qName)) {
                // Zero first time we hit an ordered element.
                sortIndex = orderedElementIndices.size();
                orderedElementIndices.add(qName);
            }
            path = extendPath(path, qName, attributes, sortIndex, dataType, valueAttributes::put);
            elementText.setLength(0);
            wasLeafElement = true;
        }

        // Note: This is not invoked for self-closing elements (e.g. "<foo/>").
        @Override
        public void characters(char[] ch, int start, int length) {
            elementText.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            // False if we have, since starting this element, entered and exited a child element.
            if (wasLeafElement) {
                if (hasAllowedDraftStatus(path) && CldrPaths.shouldEmit(path)) {
                    // IMPORTANT: The CldrFile class doesn't just trim whitespace at the start and
                    // end of values, it also removes any blank lines (unconditionally) on the
                    // apparent assumption that it is never important. However it doesn't remove
                    // leading whitespace if there isn't a newline in the value.
                    //
                    // Thus: "  foo  \n  bar  " becomes "  foo\nbar  ", and not "foo\nbar".
                    //
                    // This relates to the fact that whitespace (in many forms) is permitted in
                    // CLDR values and the Display and Input Processor (DAIP) which write CLDR
                    // values has special rules about whitespace handling. See "addPath()" and
                    // "trimWhitespaceSpecial()" in CLDRFile.
                    //
                    // This is duplicated here but could be adapted or modified if necessary. In
                    // particular this is not the same as trimAndCollapseFrom() in CharMatcher.
                    //
                    // The least hacky way to do this is to trim and remove all whitespace, and
                    // then add back any leading/trailing whitespace (providing it doesn't contain
                    // a newline).
                    String value = String.join("\n", TRIMMING_LINE_SPLITTER.split(elementText));
                    if (!value.isEmpty()) {
                        // Not empty means there's at least one non-whitespace character in the
                        // original value, so the code below has something to anchor to. We also
                        // normalise any leading/trailing whitespace to a single ASCII space.
                        int n = NOT_WHITESPACE.indexIn(elementText);
                        if (n > 0 && elementText.charAt(n) != '\n') {
                            value = Strings.repeat(" ", n) + value;
                        }
                        n = NOT_WHITESPACE.lastIndexIn(elementText);
                        if (n < elementText.length() - 1 && elementText.charAt(n) != '\n') {
                            value = value + Strings.repeat(" ", (elementText.length() - 1) - n);
                        }
                    }
                    visitor.visit(CldrValue.create(value, valueAttributes, path));
                }
            } else {
                checkState(whitespace().matchesAllOf(elementText),
                    "mixed content found in XML file: %s", path);
            }
            elementText.setLength(0);

            // Slightly subtle way to ensure that we reset the sort index for any _child_ elements
            // of the path we are leaving (we do not reset the sort index of the current path).
            // This is technically optional since the only requirement of the source index is that
            // it's monotonically increasing in "encounter" order. In fact the whole idea of having
            // a sort index is weird when considering that CLDR data can be split over multiple XML
            // files which are not necessarily processed together (consider two data instances,
            // both with the same ordered elements which are then merged).
            // TODO: Figure out how to protect against duplicate paths with equal sort indices!!
            orderedElementIndices.elementSet().removeIf(s -> !path.containsElement(s));
            path = path.getParent();

            // We just want to remove the attributes which share the name of the element we are
            // leaving. Since element names are never repeated in a CLDR path, this is safe.
            valueAttributes.keySet().removeIf(k -> k.getElementName().equals(qName));
            wasLeafElement = false;
        }
    }

    private boolean hasAllowedDraftStatus(CldrPath path) {
        return path.getDraftStatus().map(s -> s.compareTo(minimalDraftStatus) >= 0).orElse(true);
    }

    /**
     * Extends an existing path (or null) according to the currently visited XML element
     * information. This is designed to only be called by SAX parser handlers and must not be made
     * public in this class.
     *
     * @param parent parent path (or null if this is the root element).
     * @param elementName qualified element name as provided by the SAX parser.
     * @param xmlAttributes attributes as provided by the SAX parser.
     * @param dataType the DTD information to allow attributes to be filtered and sorted correctly.
     * @param valueAttributeCollector a collector for any non-distinguishing value attributes
     *     encountered during processing (they get "saved up" to go on the CldrValue).
     */
    private static CldrPath extendPath(
        /* @Nullable */ CldrPath parent,
        String elementName,
        Attributes xmlAttributes,
        int sortIndex,
        CldrDataType dataType,
        BiConsumer<AttributeKey, String> valueAttributeCollector) {
        List<String> attributeKeyValuePairs = ImmutableList.of();
        CldrDraftStatus draftStatus = null;
        if (xmlAttributes.getLength() > 0) {
            draftStatus = CldrDraftStatus.forString(xmlAttributes.getValue("draft"));

            // XML attributes are NOT necessarily ordered by DTD ordering, so we must fix that.
            Stream<Entry<String, String>> sortedAttributes =
                IntStream.range(0, xmlAttributes.getLength())
                    .mapToObj(xmlAttributes::getQName)
                    .sorted(dataType.getAttributeComparator())
                    .map(s -> Maps.immutableEntry(s, xmlAttributes.getValue(s)));

            // New variable needed because of lambdas needing "effectively final" instances.
            List<String> valueAttributes = new ArrayList<>();
            CldrPaths
                .processAttributes(sortedAttributes, elementName, valueAttributeCollector, dataType)
                .forEach(e -> {
                    valueAttributes.add(e.getKey());
                    valueAttributes.add(e.getValue());
                });
            attributeKeyValuePairs = valueAttributes;
        }
        // IMPORTANT: XML based CLDR data currently invents sort indices based on "encounter order"
        // in the XML, but this doesn't prevent duplicate sort indices if several data sources with
        // the same path structure are created and then merged. At the moment there are no cases
        // where paths from different XML files could share sort indices on the same element, but
        // there should probably be code to handle it one way or another.
        // TODO: Figure out how to handle sort indices split over multiple files (possibly error).
        return new CldrPath(
            parent, elementName, attributeKeyValuePairs, dataType, draftStatus, sortIndex);
    }

    // Handler used by the XML SAX parser to handle various events during parsing.
    private static ErrorHandler ERROR_HANDLER = new ErrorHandler() {
        @Override
        public void warning(SAXParseException exception) { }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            throw exception;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
        }
    };

    // A private exception used to allow non-matching DTDs to be ignored.
    private static final class IncompatibleDtdException extends RuntimeException { }
}
