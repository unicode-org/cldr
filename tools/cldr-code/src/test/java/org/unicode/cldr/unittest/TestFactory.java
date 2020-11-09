package org.unicode.cldr.unittest;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.XMLSource;

public class TestFactory extends Factory {
    private Map<String, CLDRFile> resolved = new TreeMap<>();
    private Map<String, CLDRFile> unresolved = new TreeMap<>();
    {
        XMLSource mySource = new SimpleXMLSource("root");
        CLDRFile testFile = new CLDRFile(mySource);
        addFile(testFile);
    }

    public void addFile(CLDRFile testFile) {
        final String localeID = testFile.getLocaleID();
        unresolved.put(localeID, testFile);
        org.unicode.cldr.util.XMLSource.ResolvingSource rs = makeResolvingSource(localeID, DraftStatus.unconfirmed);
        CLDRFile resolvedFile = new CLDRFile(rs);
        resolved.put(localeID, resolvedFile);
    }

    @Override
    public File[] getSourceDirectories() {
        return null;
    }

    @Override
    public List<File> getSourceDirectoriesForLocale(String localeID) {
        return null;
    }

    @Override
    protected CLDRFile handleMake(String localeID, boolean isResolved, DraftStatus madeWithMinimalDraftStatus) {
        return isResolved ? resolved.get(localeID) : unresolved.get(localeID);
    }

    @Override
    public DraftStatus getMinimalDraftStatus() {
        return DraftStatus.unconfirmed;
    }

    @Override
    protected Set<String> handleGetAvailable() {
        return unresolved.keySet();
    }
}