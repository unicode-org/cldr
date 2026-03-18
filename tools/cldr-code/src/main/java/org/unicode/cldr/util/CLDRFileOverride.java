package org.unicode.cldr.util;

import com.ibm.icu.util.VersionInfo;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.unicode.cldr.util.XPathParts.Comments;

public class CLDRFileOverride extends CLDRFile {

    /**
     * Silently ignore missing paths for the locale used for the Comparison Value column in Survey
     * Tool. This happens for some light-speed unit paths with @case="accusative", which have values
     * for some locales such as Amharic ("am") but not for English.
     */
    static final String COMPARISON_LOCALE = "en";

    /**
     * Overrides the values of the sourceFile. The keys of the overrides must already be in the
     * XMLSource source
     */
    public CLDRFileOverride(CLDRFile sourceFile, Map<String, String> valueOverrides) {
        super(new XMLSourceMapOverride(sourceFile.dataSource, valueOverrides));
    }

    /**
     * Overrides the values of the source. <br>
     * The keys of the overrides must already be in the XMLSource source as DPaths. And the full
     * paths cannot be changed <br>
     * Note: this is similar to the top-level DelegateXMLSource in the apps directory, but not the
     * same, since it provides for the map.
     */
    public static class XMLSourceMapOverride extends XMLSource {
        private final XMLSource delegate;

        private final Map<String, String> overrides;

        public XMLSourceMapOverride(XMLSource source, Map<String, String> overrides) {
            overrides.keySet().stream()
                    .forEach(
                            x -> {
                                if (source.getValueAtDPath(x) == null) {
                                    String loc = source.getLocaleID();
                                    if (!COMPARISON_LOCALE.equals(loc)) {
                                        throw new IllegalArgumentException(
                                                "loc=" + loc + "; path=" + x);
                                    }
                                }
                            });
            this.overrides = overrides;
            setLocaleID(source.getLocaleID());
            delegate = source;
        }

        @Override
        public boolean isResolving() {
            return delegate.isResolving();
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.icu.util.Freezable#freeze()
         */
        @Override
        public XMLSource freeze() {
            readonly();
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.unicode.cldr.util.XMLSource#getFullPathAtDPath(java.lang.String)
         */
        @Override
        public String getFullPathAtDPath(String path) {
            return delegate.getFullPathAtDPath(path);
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.unicode.cldr.util.XMLSource#getValueAtDPath(java.lang.String)
         */
        @Override
        public String getValueAtDPath(String path) {
            String override = overrides.get(path);
            return override != null ? override : delegate.getValueAtDPath(path);
        }

        /*
         * (non-Javadoc)
         *
         * @see org.unicode.cldr.util.XMLSource#getXpathComments()
         */
        @Override
        public Comments getXpathComments() {
            return delegate.getXpathComments();
        }

        /*
         * (non-Javadoc)
         *
         * @see org.unicode.cldr.util.XMLSource#iterator()
         */
        @Override
        public Iterator<String> iterator() {
            return delegate.iterator();
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.unicode.cldr.util.XMLSource#putFullPathAtDPath(java.lang.String,
         * java.lang.String)
         */
        @Override
        public void putFullPathAtDPath(String distinguishingXPath, String fullxpath) {
            readonly();
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.unicode.cldr.util.XMLSource#putValueAtDPath(java.lang.String,
         * java.lang.String)
         */
        @Override
        public void putValueAtDPath(String distinguishingXPath, String value) {
            readonly();
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.unicode.cldr.util.XMLSource#removeValueAtDPath(java.lang.String)
         */
        @Override
        public void removeValueAtDPath(String distinguishingXPath) {
            readonly();
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.unicode.cldr.util.XMLSource#setXpathComments(org.unicode.cldr
         * .util.XPathParts.Comments)
         */
        @Override
        public void setXpathComments(Comments comments) {
            readonly();
        }

        @Override
        public void getPathsWithValue(String valueToMatch, String pathPrefix, Set<String> result) {
            throw new UnsupportedOperationException("support later when needed");
            // TODO (if needed)
            // call delegate.getPathsWithValue(valueToMatch, pathPrefix, result);
            // remove the items that collide with the map
            // then look through the map to see if any matches there need to be added
        }

        @Override
        public VersionInfo getDtdVersionInfo() {
            return delegate.getDtdVersionInfo();
        }

        private static void readonly() {
            throw new InternalError("This is a readonly instance.");
        }
    }
}
