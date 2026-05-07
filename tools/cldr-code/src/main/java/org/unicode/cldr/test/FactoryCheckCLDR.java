package org.unicode.cldr.test;

import java.util.List;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PathHeader;

/**
 * Subclass of CheckCLDR that requires a factory during checking.
 *
 * @author jchye
 */
abstract class FactoryCheckCLDR extends CheckCLDR {
    private Factory factory;
    private CLDRFile resolvedCldrFileToCheck;
    private PathHeader.Factory pathHeaderFactory;

    @Override
    public synchronized CLDRFile getEnglishFile() {
        if (super.getEnglishFile() != null) {
            return super.getEnglishFile();
        }
        try {
            return getFactory().make("en", true);
        } catch (Exception e) {
            return CLDRConfig.getInstance().getEnglish();
        }
    }

    public synchronized PathHeader.Factory getPathHeaderFactory() {
        if (pathHeaderFactory == null) {
            pathHeaderFactory =
                    PathHeader.getFactory(
                            getEnglishFile() != null
                                    ? getEnglishFile()
                                    : getFactory().make("en", true));
        }
        return pathHeaderFactory;
    }

    public FactoryCheckCLDR(Factory factory) {
        super();
        this.factory = factory;
    }

    public CLDRFile getResolvedCldrFileToCheck() {
        if (resolvedCldrFileToCheck == null) {
            resolvedCldrFileToCheck = factory.make(getCldrFileToCheck().getLocaleID(), true);
        }
        return resolvedCldrFileToCheck;
    }

    @Override
    public CheckCLDR handleSetCldrFileToCheck(
            CLDRFile cldrFileToCheck, Options options, List<CheckStatus> possibleErrors) {
        super.handleSetCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        resolvedCldrFileToCheck = null;
        return this;
    }

    public Factory getFactory() {
        return factory;
    }

    /**
     * Return a reference for {@link #getMessage} that refers to an item.
     *
     * @param path the XPath
     */
    public String getPathReferenceForMessage(String path) {
        return getPathReferenceForMessage(path, true);
    }

    /**
     * Return a reference for {@link #getMessage} that refers to a header.
     *
     * @param path the XPath to any item in that header
     */
    public String getPathReferenceToHeaderForMessage(String path) {
        return getPathReferenceForMessage(path, false);
    }

    /**
     * Return a reference for {@link #getMessage} that refers to an item.
     *
     * <p>TODO CLDR-15428 this would be better handled as structured data in the CheckStatus.
     *
     * @param path the XPath
     * @param codeOnly if true, refer to an item. If false, refer to the enclosing header.
     */
    public String getPathReferenceForMessage(String path, boolean codeOnly) {
        return renderPathReference(
                getCldrFileToCheck().getLocaleID(), path, codeOnly, getPathHeaderFactory());
    }

    /**
     * Return a reference for {@link #getMessage} that refers to an item.
     *
     * @param locale locale in question
     * @param path the XPath
     */
    public static String getPathReferenceForMessage(String locale, String path) {
        return getPathReferenceForMessage(locale, path, false);
    }

    /**
     * Return a reference for {@link #getMessage} that refers to an enclosing header.
     *
     * @param locale locale in question
     * @param path the XPath to an item
     */
    public static String getPathReferenceToHeaderForMessage(String locale, String path) {
        return getPathReferenceForMessage(locale, path, true);
    }

    /**
     * Return a reference for {@link #getMessage} that refers to an item.
     *
     * <p>Private, call this from {@link #getPathReferenceForMessage(String, String)} or {@link
     * #getPathReferenceToHeaderForMessage(String, String)} instead.
     *
     * @param path the XPath
     * @param codeOnly if true, refer to an item. If false, refer to the enclosing header.
     */
    private static String getPathReferenceForMessage(String locale, String path, boolean codeOnly) {
        final PathHeader.Factory phf = PathHeader.getFactory();
        return renderPathReference(locale, path, codeOnly, phf);
    }

    // TODO CLDR-15428: FINAL_TESTING may not be the right check.
    /** true if to render pathReference as code (vs URL) */
    private static final boolean renderPathReferenceAsCode =
            CLDRConfig.getInstance().getPhase() == Phase.FINAL_TESTING;

    /**
     * Internal function to render, see {@link #getPathReferenceForMessage(String)} etc. TODO
     * CLDR-15428 this would be better handled as structured data in the CheckStatus.
     */
    private static String renderPathReference(
            String locale, String path, boolean codeOnly, final PathHeader.Factory phf) {
        final PathHeader ph = phf.fromPath(path);
        final String disp = codeOnly ? ph.getCode() : ph.getHeaderCode();

        if (renderPathReferenceAsCode) {
            return disp;
        }

        if (!ph.getSurveyToolStatus().visible()) {
            // not visible, so don't show it as a URL.
            return String.format(
                    "<span class=\"pathReference deactivated\" title='Not Visible: %s'>%s</span>",
                    path, disp);
        }
        final String url = CLDRConfig.getInstance().urls().forPathHeader(locale, ph);
        return "<a class=\"pathReference\" href=\"" + url + "\">" + disp + "</a>";
    }
}
