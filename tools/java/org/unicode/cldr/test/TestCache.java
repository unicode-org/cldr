package org.unicode.cldr.test;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Factory;

public abstract class TestCache {
    public class TestResultBundle {
        protected List<CheckStatus> possibleProblems = new ArrayList<CheckStatus>();
        CLDRFile file;
        CheckCLDR cc= createCheck();
        private Map<String, String> options;

        protected TestResultBundle(CLDRLocale locale, Map<String, String> options) {
            cc.setCldrFileToCheck(file=getFactory().make(locale.getBaseName(), true), this.options=options, possibleProblems);
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
        
        public void getExamples(String path, String value, List result) {
            cc.getExamples(path,  file.getFullXPath(path), value, options, result);
        }
    }
    
    private Factory factory= null;
    private String nameMatcher = null;;
    CLDRFile displayInformation = null;
    protected Factory getFactory() {
        return factory;
    }
    
    /**
     * Set up the basic info needed for tests
     * @param factory
     * @param nameMatcher
     * @param displayInformation
     */
    public void setFactory(Factory factory, String nameMatcher, CLDRFile displayInformation) {
        if(this.factory!=null) {
            throw new InternalError("setFactory() can only be called once.");
        }
        this.factory =factory;
        this.nameMatcher =nameMatcher;
        this.displayInformation = displayInformation;
    }
    
    /**
     * Something changed at this xpath.
     */
    public abstract void notifyChange(CLDRLocale locale, String xpath);
    
    /**
     * Get the bundle for this test
     * @param locale
     */
    public abstract TestResultBundle getBundle(CLDRLocale locale, Map<String, String> options);
    
    
    /**
     * Create a check using the options
     */
    protected CheckCLDR createCheck() {
        CheckCLDR checkCldr;
        checkCldr = CheckCLDR.getCheckAll(getFactory(), nameMatcher);
        return checkCldr;
    }
}
