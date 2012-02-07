package org.unicode.cldr.test;

import java.util.List;
import java.util.Map;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;

/**
 * Subclass of CheckCLDR that requires a factory during checking.
 * @author jchye
 */
abstract class FactoryCheckCLDR extends CheckCLDR {
    private Factory factory;
    private CLDRFile resolvedCldrFileToCheck;

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
    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Map<String, String> options, List<CheckStatus> possibleErrors) {
        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        resolvedCldrFileToCheck = null;
        return this;
    }

    public Factory getFactory() {
        return factory;
    }
}