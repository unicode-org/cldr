package org.unicode.cldr.unittest;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.util.ICUUncheckedIOException;

public class TestUnContainment extends TestFmwkPlus {
    static CLDRConfig testInfo = CLDRConfig.getInstance();
    private static final SupplementalDataInfo SUPPLEMENTAL_DATA_INFO = testInfo.getSupplementalDataInfo();
    Map<String, R2<List<String>, String>> regionToInfo = SUPPLEMENTAL_DATA_INFO
        .getLocaleAliasInfo()
        .get("territory");

    private static final Set<String> NOT_CLDR_TERRITORY_CODES = ImmutableSet.of("830"); // Channel Islands
    private static final Set<String> KNOWN_CONTAINMENT_EXCEPTIONS = ImmutableSet.of("AQ","680"); // Antarctica, Sark
    
    final Multimap<String, String> UnChildToParent;
    {
        Multimap<String, String> _UnChildToParent = TreeMultimap.create();
        Splitter tab = Splitter.on('\t').trimResults();
        try (BufferedReader unCodes = CldrUtility.getUTF8Data("external/UnCodes.txt");) {
            for (String line : FileUtilities.in(unCodes)) {
                List<String> items = tab.splitToList(line);
                if (line.isEmpty() || line.startsWith("Global Code")) {
                    continue;
                }
                String parent = null;
                for (int i = 0; i < 10; i += 2) {
                    String region = items.get(i);
                    if (!region.isEmpty()) {
                        region = unToCldrCode(region);
                        if (parent != null && region != null){
                            _UnChildToParent.put(region, parent);
                        }
                        if (region != null) {
                            parent = region;
                        }
                    }
                    if (i == 6) {
                        ++i; // hack because last two are out of order
                    }
                }
            }
            UnChildToParent = ImmutableSetMultimap.copyOf(_UnChildToParent);
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    public static void main(String[] args) {
        new TestUnContainment().run(args);
    }

    private String name(Collection<String> codes) {
        StringBuilder result = new StringBuilder();
        for (String code : codes) {
            if (result.length() != 0) {
                result.append(", ");
            }
            result.append(name(code));
        }
        return result.toString();
    }

    private String name(String code) {
        String name = testInfo.getEnglish().getName(CLDRFile.TERRITORY_NAME, code);
        return name + " (" + code + ")";
    }

    private String unToCldrCode(String code) {
        
        if (NOT_CLDR_TERRITORY_CODES.contains(code)) {
            return null;
        }
        
        R2<List<String>, String> codeInfo = regionToInfo.get(code);
        if (codeInfo != null) {
            if (codeInfo.get0() != null && !codeInfo.get0().isEmpty()) {
                code = codeInfo.get0().get(0);
            }
        }
        return code;
    }

    public void TestContainment() {
        
        /*
        CLDR
        <group type="001" contains="019 002 150 142 009"/> <!--World -->
        <group type="001" contains="EU EZ UN" status="grouping"/> <!--European Union, Eurozone, United Nations -->
        <group type="001" contains="QU" status="deprecated"/> <!--European Union -->
        <group type="011" contains="BF BJ CI CV GH GM GN GW LR ML MR NE NG SH SL SN TG"/> <!--Western Africa -->
         */
        for (Entry<String, Collection<String>> entry : UnChildToParent.asMap().entrySet()) {
            Collection<String> unParents = entry.getValue();
            String unChild = entry.getKey();
            //System.out.println(name(unParents) + "\t" + name(unChild));
            for (String unParent : unParents) {
                Set<String> children = SUPPLEMENTAL_DATA_INFO.getContained(unParent);
                if (children != null && children.contains(unChild)) {
                    continue;
                }
                // See CLDR ticket 10187 for rationalization on the known containment exceptions.
                if (KNOWN_CONTAINMENT_EXCEPTIONS.contains(unChild)) {
                    continue;
                }
                msg("UN containment doesn't match CLDR for " + name(unParent)
                    + ": cldr children " + children
                    + " don't contain UN " + name(unChild), ERR, true, true);
            }
        }
    }
}
