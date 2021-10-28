package org.unicode.cldr.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.json.Ldml2JsonConverter.JSONSection;
import org.unicode.cldr.json.Ldml2JsonConverter.RunType;
import org.unicode.cldr.util.FileProcessor;
import org.unicode.cldr.util.PatternCache;

/**
 * Reader for the JSON_config.txt type files
 */
public class LdmlConfigFileReader {
    final List<JSONSection> sections = new ArrayList<JSONSection>();
    /**
     * Map of package to description
     */
    final Map<String, String> packages = new TreeMap<>();
    final Map<String, String> dependencies = new HashMap<>();

    public List<JSONSection> getSections() {
        return sections;
    }

    public Set<String> getPackages() {
        return packages.keySet();
    }

    public Map<String, String> getPackageDescriptions() {
        return packages;
    }

    public Map<String, String> getDependencies() {
        return dependencies;
    }

    public void read(final String configFile, final RunType type) {
        FileProcessor myReader = new FileProcessor() {
            @Override
            protected boolean handleLine(int lineCount, String line) {
                String[] lineParts = line.trim().split("\\s*;\\s*");
                String key, value, section = null, path = null, packageName = null, dependency = null;
                String packageDesc = "A CLDR package with no packageDesc description.";
                boolean hasSection = false;
                boolean hasPath = false;
                boolean hasPackage = false;
                boolean hasDependency = false;
                for (String linePart : lineParts) {
                    int pos = linePart.indexOf('=');
                    if (pos < 0) {
                        throw new IllegalArgumentException();
                    }
                    key = linePart.substring(0, pos);
                    value = linePart.substring(pos + 1);
                    if (key.equals("section")) {
                        hasSection = true;
                        section = value;
                    } else if (key.equals("path")) {
                        hasPath = true;
                        path = value;
                    } else if (key.equals("package")) {
                        hasPackage = true;
                        packageName = value;
                    } else if (key.equals("packageDesc")) {
                        packageDesc = value;
                    } else if (key.equals("dependency")) {
                        hasDependency = true;
                        dependency = value;
                    }
                }
                if (hasSection && hasPath) {
                    JSONSection j = new JSONSection();
                    j.section = section;
                    j.pattern = PatternCache.get(path);
                    if (hasPackage) {
                        j.packageName = packageName;
                    }
                    sections.add(j);
                }
                if (hasDependency && hasPackage) {
                    dependencies.put(packageName, dependency);
                }
                if (hasPackage) {
                    packages.putIfAbsent(packageName, packageDesc);
                }
                return true;
            }
        };

        if (configFile != null) {
            myReader.process(configFile);
        } else {
            switch (type) {
            case main:
                myReader.process(Ldml2JsonConverter.class, "JSON_config.txt");
                break;
            case supplemental:
                myReader.process(Ldml2JsonConverter.class, "JSON_config_supplemental.txt");
                break;
            case segments:
                myReader.process(Ldml2JsonConverter.class, "JSON_config_segments.txt");
                break;
            case rbnf:
                myReader.process(Ldml2JsonConverter.class, "JSON_config_rbnf.txt");
                break;
            default:
                myReader.process(Ldml2JsonConverter.class, "JSON_config_" + type.name() + ".txt");
            }
        }

        // Add a section at the end of the list that will match anything not already matched.
        JSONSection j = new JSONSection();
        j.section = "other";
        j.pattern = PatternCache.get(".*");
        sections.add(j);
    }
}
