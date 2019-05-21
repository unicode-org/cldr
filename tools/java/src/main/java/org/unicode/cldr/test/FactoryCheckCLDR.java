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
            pathHeaderFactory = PathHeader.getFactory(getEnglishFile() != null ? getEnglishFile() : getFactory().make("en", true));
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
    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Options options,
        List<CheckStatus> possibleErrors) {
        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        resolvedCldrFileToCheck = null;
        return this;
    }

    public Factory getFactory() {
        return factory;
    }
    
    public String getPathReferenceForMessage(String path, boolean codeOnly) {
        PathHeader pathHeader = getPathHeaderFactory().fromPath(path);
        String referenceToOther;
        String code = codeOnly ? pathHeader.getCode() : pathHeader.getHeaderCode();
        if (getPhase() == Phase.FINAL_TESTING) {
            referenceToOther = code; // later make this more readable.
        } else {
            referenceToOther = "<a href=\""
                + CLDRConfig.getInstance().urls().forPathHeader(getCldrFileToCheck().getLocaleID(), pathHeader)
                + "\">" +
                code
                + "</a>";
        }
        return referenceToOther;
    }
}