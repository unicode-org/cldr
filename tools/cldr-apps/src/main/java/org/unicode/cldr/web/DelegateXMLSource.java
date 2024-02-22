package org.unicode.cldr.web;

import com.ibm.icu.util.VersionInfo;
import java.util.Iterator;
import java.util.Set;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts.Comments;

/**
 * An XMLSource that acts as a delegate for another XMLSource. This is the parent for STFactory's
 * DataBackedSource, which overrides with database content. The base class is readonly, but the
 * subclass may mutate the delegate
 *
 * @author srl
 */
class DelegateXMLSource extends XMLSource {
    /** May be mutated by subclass. */
    protected XMLSource delegate;

    public DelegateXMLSource(XMLSource source) {
        setLocaleID(source.getLocaleID());
        delegate = source;
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
        return delegate.getValueAtDPath(path);
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
        delegate.getPathsWithValue(valueToMatch, pathPrefix, result);
    }

    @Override
    public VersionInfo getDtdVersionInfo() {
        return delegate.getDtdVersionInfo();
    }

    private static void readonly() {
        throw new InternalError("This is a readonly instance.");
    }
}
