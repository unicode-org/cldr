package org.unicode.cldr.tool;

import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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
import org.unicode.cldr.util.Splitters;
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

    static Path archiveDir =
            Paths.get(CLDRPaths.ARCHIVE_DIRECTORY); // Replace with your directory path

    public static void main(String[] args) throws IOException {
        System.out.println(CldrVersion.LAST_RELEASE_EACH_YEAR);

        System.out.println(Changes.header());

        CldrVersion nextVersion = null;
        for (CldrVersion previousVersion : CldrVersion.LAST_RELEASE_EACH_YEAR) {
            if (nextVersion != null) {
                compare(nextVersion, previousVersion);
            }
            nextVersion = previousVersion;
        }
    }

    private static class Changes {
        Counter<ChangeType> changeTypes = new Counter<>();
        Set<String> locales = new TreeSet<>();
        Counter<DtdType> dtdTypes = new Counter<>();

        static String header() {
            return Joiners.TAB.join(
                    "Version",
                    "Year",
                    Joiners.TAB.join(ChangeType.values()),
                    "Locales",
                    Joiners.TAB.join(DtdType.values()));
        }

        @Override
        public String toString() {
            return Joiners.TAB.join(
                    List.of(ChangeType.values()).stream()
                            .map(x -> String.valueOf(changeTypes.get(x)))
                            .collect(Collectors.joining("\t")),
                    locales.size(),
                    List.of(DtdType.values()).stream()
                            .map(x -> String.valueOf(dtdTypes.get(x)))
                            .collect(Collectors.joining("\t")));
        }
    }

    static final Set<String> SKIP_COMMON_SUBDIRS =
            Set.of(
                    "collation",
                    "annotations",
                    "annotationsDerived",
                    "casing",
                    "subdivisions",
                    "supplemental-temp");

    private static void compare(CldrVersion nextVersion, CldrVersion previousVersion)
            throws IOException {
        //        if (nextVersion.compareTo(CldrVersion.v2_0_1) > 0) { // for debugging
        //            return;
        //        }
        Path release = archiveDir.resolve("cldr-" + nextVersion + "/common");
        Path previousRelease = archiveDir.resolve("cldr-" + previousVersion + "/common");
        int commonIndex = release.getNameCount();
        Map<Path, String> failures = new TreeMap<>();

        Changes changes = new Changes();

        try (Stream<Path> stream =
                Files.walk(release).collect(Collectors.toList()).parallelStream()) {
            stream.filter(
                            x ->
                                    x.getFileName().toString().endsWith(".xml")
                                            && !SKIP_COMMON_SUBDIRS.contains(
                                                    x.getName(commonIndex).toString()))
                    .forEach(
                            x -> {
                                String error =
                                        getChanges(
                                                changes,
                                                x,
                                                replaceBase(x, release, previousRelease));
                                if (error != null) {
                                    failures.put(x, error);
                                }
                            });
        }
        System.out.println(Joiners.TAB.join(nextVersion, nextVersion.getYear(), changes));
        if (nextVersion == CldrVersion.LAST_RELEASE_EACH_YEAR.get(0)) {
            System.out.println(changes.locales);
        }
        if (!failures.isEmpty()) {
            System.out.println(failures);
        }
    }

    private static Path replaceBase(Path x, Path xPrefix, Path otherPrefix) {
        Path relativePath = xPrefix.relativize(x);
        return otherPrefix.resolve(relativePath);
    }

    private static String getChanges(Changes changes, Path x, Path previousRelease) {
        CLDRFile current = null;
        CLDRFile last = null;
        try {
            current =
                    CLDRFile.loadFromFile(
                            x.toFile(), x.getFileName().toString(), DraftStatus.contributed);
            last =
                    !previousRelease.toFile().exists()
                            ? null
                            : CLDRFile.loadFromFile(
                                    previousRelease.toFile(),
                                    previousRelease.getFileName().toString(),
                                    DraftStatus.contributed);
        } catch (Exception e) {
            return e.getMessage();
        }
        boolean isLdml = current.getDtdType() == DtdType.ldml;
        if (isLdml) {
            changes.locales.add(x.getFileName().toString());
        }
        boolean mayHaveValueAttributes = !isLdml;
        DtdData dtdData = current.getDtdData();
        // could optimize by finding elements with value attributes and caching
        for (String currentPath : current) {
            changes.dtdTypes.add(dtdData.dtdType, 1);
            String currentValue = current.getStringValue(currentPath);
            String lastValue = last == null ? null : last.getStringValue(currentPath);
            if (currentPath.contains("/annotations/")) {
                Set<String> currentSet = getVBarSet(currentValue);
                Set<String> lastSet = getVBarSet(lastValue);
                int sameCount = Sets.intersection(currentSet, lastSet).size();
                changes.changeTypes.add(ChangeType.same, sameCount);
                int addCount = currentSet.size() - sameCount;
                int deleteCount = lastSet.size() - sameCount;
                int changeCount = Math.min(addCount, deleteCount);
                changes.changeTypes.add(ChangeType.changed, addCount);
                changes.changeTypes.add(ChangeType.added, addCount - changeCount);
                changes.changeTypes.add(ChangeType.deleted, deleteCount - changeCount);
            } else {
                ChangeType changeType = ChangeType.getDiff(currentValue, lastValue);
                changes.changeTypes.add(changeType, 1);
            }
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
                            changes.changeTypes.add(
                                    ChangeType.getDiff(currentAttributeValue, lastAttributeValue),
                                    1);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static Set<String> getVBarSet(String currentValue) {
        return currentValue == null
                ? Set.of()
                : Set.copyOf(Splitters.VBAR.splitToList(currentValue));
    }
}
