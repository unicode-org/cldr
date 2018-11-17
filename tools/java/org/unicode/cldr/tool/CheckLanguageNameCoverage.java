package org.unicode.cldr.tool;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

public class CheckLanguageNameCoverage {
    public static void main(String[] args) {
        CLDRConfig config = CLDRConfig.getInstance();
        CoverageLevel2 coverages = CoverageLevel2.getInstance(config.getSupplementalDataInfo(), "haw"); // pick something that's not likely to pull in others
        Validity validity = Validity.getInstance();
        Multimap<Level,String> levelToLangs = TreeMultimap.create();
        Map<String, Status> map = validity.getCodeToStatus(LstrType.language);

        Set<String> targets = new TreeSet(Arrays.asList("ceb",
            "ny",
            "co",
            "eo",
            "fy",
            "ht",
            "ha",
            "haw",
            "hmn",
            "ig",
            "jw",
            "ku",
            "la",
            "lb",
            "mg",
            "mt",
            "mi",
            "sm",
            "gd",
            "st",
            "sn",
            "so",
            "su",
            "tg",
            "xh",
            "yi",
            "yo"));
        for (String langCode : targets) {
            String path = CLDRFile.getKey(CLDRFile.LANGUAGE_NAME, langCode);
            Level level = coverages.getLevel(path);
            System.out.println(langCode + "\t" + level + "\t" + config.getEnglish().getName(langCode));
        }
        for (String langCode : map.keySet()) {
            String path = CLDRFile.getKey(CLDRFile.LANGUAGE_NAME, langCode);
            Level level = coverages.getLevel(path);
            if (level == null) continue;
            levelToLangs.put(level, langCode);
        }
        for (Entry<Level, Collection<String>> entry : levelToLangs.asMap().entrySet()) {
            System.out.println(entry.getKey() + "\t" + entry.getValue());
        }
    }
}
