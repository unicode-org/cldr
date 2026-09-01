package org.unicode.cldr.unittest;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.XMLSource;

public class TestFactory extends Factory {
    private Map<String, CLDRFile> resolved = new TreeMap<>();
    private Map<String, CLDRFile> unresolved = new TreeMap<>();

    TestFactory() {
        XMLSource mySource = registerXmlSource(new SimpleXMLSource("root"));
        CLDRFile testFile = new CLDRFile(mySource);
        addFile(testFile);
    }

    TestFactory(XMLSource root) {
        XMLSource mySource = registerXmlSource(root);
        CLDRFile rootFile = new CLDRFile(mySource);
        addFile(rootFile);
    }

    public void addFile(CLDRFile testFile) {
        registerXmlSource(testFile);
        final String localeID = testFile.getLocaleID();
        unresolved.put(localeID, testFile);
        org.unicode.cldr.util.XMLSource.ResolvingSource rs =
                makeResolvingSource(localeID, DraftStatus.unconfirmed);
        CLDRFile resolvedFile = new CLDRFile(rs);
        resolved.put(localeID, resolvedFile);
    }

    public void addFile(XMLSource source) {
        registerXmlSource(source);
        final String localeID = source.getLocaleID();
        final CLDRFile unresolvedFile = new CLDRFile(source);
        unresolved.put(localeID, unresolvedFile);
        org.unicode.cldr.util.XMLSource.ResolvingSource rs =
                makeResolvingSource(localeID, DraftStatus.unconfirmed);
        CLDRFile resolvedFile = new CLDRFile(rs);
        resolved.put(localeID, resolvedFile);
    }

    @Override
    public File[] getSourceDirectories() {
        return null;
    }

    @Override
    public File getSupplementalDirectory() {
        // punt to the real one
        return CLDRConfig.getInstance().getCldrFactory().getSupplementalDirectory();
    }

    @Override
    public List<File> getSourceDirectoriesForLocale(String localeID) {
        return null;
    }

    @Override
    protected CLDRFile handleMake(
            String localeID, boolean isResolved, DraftStatus madeWithMinimalDraftStatus) {
        final CLDRFile retFile = isResolved ? resolved.get(localeID) : unresolved.get(localeID);
        if (retFile == null) {
            throw new NullPointerException(
                    "Could not handleMake of " + localeID + (isResolved ? " resolved" : ""));
        }
        return retFile;
    }

    @Override
    public DraftStatus getMinimalDraftStatus() {
        return DraftStatus.unconfirmed;
    }

    @Override
    protected Set<String> handleGetAvailable() {
        return unresolved.keySet();
    }

    public static TestFactory makeFileWithValues(
            String locale,
            Map<String, String> rootPathValuePairs,
            Map<String, String> localePathValuePairs) {
        TestFactory factory = new TestFactory();
        XMLSource rootSource = new SimpleXMLSource("root");
        for (Entry<String, String> entry : rootPathValuePairs.entrySet()) {
            rootSource.putValueAtPath(entry.getKey(), entry.getValue());
        }
        factory.addFile(new CLDRFile(rootSource));

        XMLSource localeSource = new SimpleXMLSource(locale);
        for (Entry<String, String> entry : localePathValuePairs.entrySet()) {
            localeSource.putValueAtPath(entry.getKey(), entry.getValue());
        }
        factory.addFile(new CLDRFile(localeSource));
        return factory;
    }
}
