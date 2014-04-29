package org.unicode.cldr.tool;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.CLDRFile.DtdType;
import org.unicode.cldr.util.ChainedMap.M3;
import org.unicode.cldr.util.ChainedMap.M4;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathStarrer;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;

public class ShowStarredCoverage {
    static final CLDRConfig config = CLDRConfig.getInstance();
    public static void main(String[] args) {
        M4<Level, String, String, Boolean> levelToData = ChainedMap.of(
            new TreeMap<Level,Object>(), 
            new TreeMap<String,Object>(), 
            new TreeMap<String,Object>(), 
            Boolean.class);
        CLDRFile file = config.getCldrFactory().make("en", true);
        String fileLocale = file.getLocaleID();
        PathStarrer pathStarrer = new PathStarrer().setSubstitutionPattern("*");
        Status status = new Status();
        Counter<Level> counter = new Counter();
        for (String path : file) {
            if (path.endsWith("/alias") || path.startsWith("//ldml/identity")) {
                continue;
            }
            String locale = file.getSourceLocaleID(path, status);
            if (!path.equals(status.pathWhereFound) || !locale.equals(fileLocale)) {
                // path is aliased, skip
                continue;
            }
            if (config.getSupplementalDataInfo().isDeprecated(DtdType.ldml, path)) {
                continue;
            }
            String starred = pathStarrer.set(path);
            String attributes = CollectionUtilities.join(pathStarrer.getAttributes(),"|");
            Level level = config.getSupplementalDataInfo().getCoverageLevel(path, "en");
            levelToData.put(level, starred, attributes, Boolean.TRUE);
            counter.add(level, 1);
        }
        for (Level level : Level.values()) {
            System.out.println(counter.get(level) + "\t" + level);
        }
        for (Entry<Level, Map<String, Map<String, Boolean>>> entry : levelToData) {
            Level level = entry.getKey();
            for (Entry<String, Map<String, Boolean>> entry2 : entry.getValue().entrySet()) {
                String starred = entry2.getKey();
                Map<String, Boolean> attributes = entry2.getValue();
                int count = attributes.size();
                if (count < 1) {
                    count = 1;
                }
                System.out.println(count + "\t" + level + "\t" + starred + "\t" + CollectionUtilities.join(attributes.keySet(), ", "));
            }
        }
    }
}
