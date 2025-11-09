package org.unicode.cldr.tool;

import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdData.Attribute;
import org.unicode.cldr.util.DtdData.AttributeStatus;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Joiners;
import org.unicode.cldr.util.XPathParts;

public class GenerateFullCldrGrowth {

    enum ChangeType {
        same,
        added,
        deleted,
        changed;

        static ChangeType getDiff(String current, String last) {
            return Objects.equal(current, last)
                    ? ChangeType.same
                    : current == null
                            ? ChangeType.deleted
                            : last == null ? ChangeType.added : ChangeType.changed;
        }
    }

    public static void main(String[] args) throws IOException {
        Path archiveDir =
                Paths.get(CLDRPaths.ARCHIVE_DIRECTORY); // Replace with your directory path

        // get subdirs in alpha order
        List<Path> orderedCldrDirs =
                Files.list(archiveDir)
                        .filter(
                                x ->
                                        x.getFileName().toString().startsWith("cldr-")
                                                && x.getFileName().toString().compareTo("cldr-2")
                                                        >= 0)
                        .sorted(Comparator.comparing(Path::getFileName))
                        .collect(Collectors.toList());

        Path previousRelease = null;
        System.out.println("Version\t" + Joiners.TAB.join(ChangeType.values()));
        for (Path release : orderedCldrDirs) {
            if (previousRelease != null) {
                compare(release, previousRelease);
            }
            previousRelease = release;
        }
    }

    private static void compare(Path release, Path previousRelease) throws IOException {
        Counter<ChangeType> changes = new Counter<>();
        try (Stream<Path> stream = Files.walk(release.resolve("common"))) {
            stream.filter(
                            x ->
                                    x.getFileName().toString().endsWith(".xml")
                                            && !x.toString().contains("/collation/")) //  &&
                    // x.toString().contains("/annotations/")
                    .forEach(x -> getChanges(changes, x, replaceBase(x, release, previousRelease)));
        }
        System.out.println(
                release.getFileName()
                        + "\t"
                        + List.of(ChangeType.values()).stream()
                                .map(x -> String.valueOf(changes.get(x)))
                                .collect(Collectors.joining("\t")));
    }

    private static Path replaceBase(Path x, Path xPrefix, Path otherPrefix) {
        Path relativePath = xPrefix.relativize(x);
        return otherPrefix.resolve(relativePath);
    }

    private static void getChanges(Counter<ChangeType> changes, Path x, Path previousRelease) {
        CLDRFile current =
                CLDRFile.loadFromFile(
                        x.toFile(), x.getFileName().toString(), DraftStatus.contributed);
        CLDRFile last =
                !previousRelease.toFile().exists()
                        ? null
                        : CLDRFile.loadFromFile(
                                previousRelease.toFile(),
                                previousRelease.getFileName().toString(),
                                DraftStatus.contributed);
        boolean mayHaveValueAttributes = current.getDtdType() != DtdType.ldml;
        DtdData dtdData = current.getDtdData();
        // could optimize by finding elements with value attributes and caching
        for (String currentPath : current) {
            String currentValue = current.getStringValue(currentPath);
            String lastValue = last == null ? null : last.getStringValue(currentPath);
            ChangeType changeType = ChangeType.getDiff(currentValue, lastValue);
            changes.add(changeType, 1);
            if (mayHaveValueAttributes) {
                XPathParts currentParts =
                        XPathParts.getFrozenInstance(current.getFullXPath(currentPath));
                XPathParts lastParts =
                        last == null
                                ? null
                                : XPathParts.getFrozenInstance(last.getFullXPath(currentPath));
                for (int i = 0; i < currentParts.size(); ++i) {
                    String element = currentParts.getElement(i);
                    Map<String, String> currentAttributes = currentParts.getAttributes(i);
                    Map<String, String> lastAttributes =
                            lastParts == null ? Collections.emptyMap() : lastParts.getAttributes(i);
                    for (String attribute :
                            Sets.union(currentAttributes.keySet(), lastAttributes.keySet())) {
                        Attribute attributeInfo = dtdData.getAttribute(element, attribute);
                        if (attributeInfo != null
                                && attributeInfo.attributeStatus == AttributeStatus.value) {
                            String currentAttributeValue = currentAttributes.get(attribute);
                            String lastAttributeValue = lastAttributes.get(attribute);
                            changes.add(
                                    ChangeType.getDiff(currentAttributeValue, lastAttributeValue),
                                    1);
                        }
                    }
                }
            }
        }
    }
}
