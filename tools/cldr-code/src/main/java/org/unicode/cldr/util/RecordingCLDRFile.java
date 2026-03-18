package org.unicode.cldr.util;

import java.util.HashSet;

/**
 * Like CLDRFile, with an added feature for recording the paths for which getStringValue, etc., are
 * called.
 *
 * <p>The first intended usage is for GenerateExampleDependencies, to identify all the paths on
 * which a given example depends. Before calling ExampleGenerator.getExampleHtml,
 * GenerateExampleDependencies calls clearRecordedPaths. After getting each example,
 * GenerateExampleDependencies calls getRecordedPaths to get the set of all paths in this file that
 * were accessed to generate the example.
 */
public class RecordingCLDRFile extends CLDRFile {
    private final HashSet<String> recordedPaths = new HashSet<>();

    private boolean recordingIsEnabled = false;

    public RecordingCLDRFile(XMLSource dataSource) {
        super(dataSource);
    }

    public RecordingCLDRFile(XMLSource dataSource, XMLSource... resolvingParents) {
        super(dataSource, resolvingParents);
    }

    public void enableRecording() {
        recordingIsEnabled = true;
    }

    public void disableRecording() {
        recordingIsEnabled = false;
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
        if (recordingIsEnabled) {
            recordedPaths.add(xpath);
        }
    }
}
