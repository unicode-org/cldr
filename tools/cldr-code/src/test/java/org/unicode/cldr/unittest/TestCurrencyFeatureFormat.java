package org.unicode.cldr.unittest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.unicode.cldr.tool.FeatureRegistry;
import org.unicode.cldr.tool.FeatureRegistry.Feature;
import org.unicode.cldr.tool.GenerateCurrencyFeatureTestData;

/**
 * Checks that each core TSV file of the feature-based currency test data matches the output of
 * {@link GenerateCurrencyFeatureTestData}. If it fails after a generator or ICU4J change,
 * regenerate the files.
 */
public class TestCurrencyFeatureFormat extends TestFmwkPlus {

    public static void main(String[] args) {
        new TestCurrencyFeatureFormat().run(args);
    }

    public void TestCoreFilesMatchGenerator() throws IOException {
        String category = GenerateCurrencyFeatureTestData.CATEGORY;
        List<Feature> features = FeatureRegistry.read(category);
        Set<String> registered = features.stream().map(Feature::id).collect(Collectors.toSet());
        for (Feature feature : features) {
            String name = FeatureRegistry.coreFileName(feature.id());
            Path file = FeatureRegistry.directory(category).resolve(name);
            if (!Files.exists(file)) {
                errln(name + " is missing: run GenerateCurrencyFeatureTestData");
                continue;
            }
            List<String> actual = Files.readAllLines(file, StandardCharsets.UTF_8);
            List<String> expected =
                    GenerateCurrencyFeatureTestData.coreFileLines(feature, registered);
            for (int i = 0; i < Math.max(actual.size(), expected.size()); i++) {
                assertEquals(
                        name + ":" + (i + 1),
                        i < expected.size() ? expected.get(i) : null,
                        i < actual.size() ? actual.get(i) : null);
            }
        }
    }
}
