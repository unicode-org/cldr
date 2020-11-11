package org.unicode.cldr.util;

import java.util.HashSet;

/**
 * Like CLDRFile, with an added feature for recording the paths for which
 * getStringValue, etc., are called.
 *
 * The first intended usage is for TestExampleDependencies, to identify all the paths on
 * which a given example depends. Before calling ExampleGenerator.getExampleHtml, TestExampleDependencies
 * calls clearRecordedPaths. After getting each example, TestExampleDependencies calls getRecordedPaths
 * to get the set of all paths in this file that were accessed to generate the example.
 */
public class RecordingCLDRFile extends CLDRFile {
    private HashSet<String> recordedPaths = new HashSet<>();

    public RecordingCLDRFile(XMLSource dataSource) {
        super(dataSource);
    }

    public RecordingCLDRFile(XMLSource dataSource, XMLSource... resolvingParents) {
        super(dataSource, resolvingParents);
    }

    public void clearRecordedPaths() {
        recordedPaths.clear();
    }

    public HashSet<String> getRecordedPaths() {
        return recordedPaths;
    }

    @Override
    public String getStringValue(String xpath) {
        recordPath(xpath);
        return super.getStringValue(xpath);
    }

    @Override
    public String getWinningValue(String xpath) {
        recordPath(xpath);
        return super.getWinningValue(xpath);
    }

    @Override
    public String getConstructedValue(String xpath) {
        recordPath(xpath);
        return super.getConstructedValue(xpath);
    }

    private void recordPath(String xpath) {
        recordedPaths.add(xpath);
    }
}
