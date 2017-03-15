package org.unicode.cldr.test;

import java.util.ArrayList;
import java.util.List;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.XMLSource;

/**
 * Caches tests
 * Call XMLSource.addListener() on the instance to notify it of changes to the XMLSource.
 *
 * @author srl
 * @see XMLSource#addListener(org.unicode.cldr.util.XMLSource.Listener)
 */
public abstract class TestCache implements XMLSource.Listener {
    public class TestResultBundle {
        protected List<CheckStatus> possibleProblems = new ArrayList<CheckStatus>();
        CLDRFile file;
        CheckCLDR cc = createCheck();
        private CheckCLDR.Options options;

        protected TestResultBundle(CheckCLDR.Options options) {
            cc.setCldrFileToCheck(file = getFactory().make(options.getLocale().getBaseName(), true), this.options = options,
                possibleProblems);
        }

        public List<CheckStatus> getPossibleProblems() {
            return possibleProblems;
        }

        public void check(String path, List<CheckStatus> result) {
            cc.check(path, file.getFullXPath(path), file.getStringValue(path), options, result);
        }

        public void check(String path, List<CheckStatus> result, String value) {
            cc.check(path, file.getFullXPath(path), value, options, result);
        }

        public void getExamples(String path, String value, List<CheckStatus> result) {
            cc.getExamples(path, file.getFullXPath(path), value, options, result);
        }
    }

    private Factory factory = null;
    private String nameMatcher = null;;
    CLDRFile displayInformation = null;

    protected Factory getFactory() {
        return factory;
    }

    /**
     * Set up the basic info needed for tests
     *
     * @param factory
     * @param nameMatcher
     * @param displayInformation
     */
    public void setFactory(Factory factory, String nameMatcher, CLDRFile displayInformation) {
        if (this.factory != null) {
            throw new InternalError("setFactory() can only be called once.");
        }
        this.factory = factory;
        this.nameMatcher = nameMatcher;
        this.displayInformation = displayInformation;
    }

    /**
     * Get the bundle for this test
     */
    public abstract TestResultBundle getBundle(CheckCLDR.Options options);

    /**
     * Create a check using the options
     */
    protected CheckCLDR createCheck() {
        CheckCLDR checkCldr;
        checkCldr = CheckCLDR.getCheckAll(getFactory(), nameMatcher);
        return checkCldr;
    }
}
