package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.tool.SubdivisionNode.SubDivisionExtractor;
import org.unicode.cldr.tool.SubdivisionNode.SubdivisionSet;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row.R2;

public class GenerateSubdivisions {
    private static final String ISO_COUNTRY_CODES = CLDRPaths.CLDR_PRIVATE_DIRECTORY + "iso_country_codes/";
    static final String ISO_SUBDIVISION_CODES = ISO_COUNTRY_CODES + "iso_country_codes.xml";


    // TODO: consider whether to use the last archive directory to generate
    // There are pros and cons. 
    // Pros are that we don't introduce "fake" deprecated elements that are introduced and deprecated during the 6 month CLDR cycle
    // Cons are that we may have to repeat work


    static final class SubdivisionInfo {
        static final SupplementalDataInfo SDI = SupplementalDataInfo.getInstance(CLDRPaths.LAST_RELEASE_DIRECTORY + "common/supplemental/");

        static final Map<String, R2<List<String>, String>> SUBDIVISION_ALIASES_FORMER = SDI.getLocaleAliasInfo().get("subdivision");

        static final SubdivisionNames SUBDIVISION_NAMES_ENGLISH_FORMER = new SubdivisionNames("en");

        static final Validity VALIDITY_FORMER = Validity.getInstance(CLDRPaths.LAST_RELEASE_DIRECTORY + "common/validity/");

        static final Relation<String, String> formerRegionToSubdivisions = Relation.of(new HashMap<String, Set<String>>(), TreeSet.class, SubdivisionNode.ROOT_COL);
        static {
            Map<Status, Set<String>> oldSubdivisionData = VALIDITY_FORMER.getStatusToCodes(LstrType.subdivision);
            for (Entry<Status, Set<String>> e : oldSubdivisionData.entrySet()) {
                final Status status = e.getKey();
                if (status != Status.unknown) { // special is a hack
                    for (String sdCode : e.getValue()) {
                        final String region = SubdivisionNames.getRegionFromSubdivision(sdCode);
                        formerRegionToSubdivisions.put(region, sdCode);
                    }
                }
            }
            formerRegionToSubdivisions.freeze();
        }

        static final Multimap<String, String> subdivisionIdToOld = HashMultimap.create();
        static {
            for (Entry<String, R2<List<String>, String>> entry : SUBDIVISION_ALIASES_FORMER.entrySet()) {
                String oldId = entry.getKey();
                for (String newId : entry.getValue().get0()) {
                    subdivisionIdToOld.put(newId, oldId);
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        // TODO Restructure so that this call is done first to process the iso data
        // then the extraction uses that data.
        // also restructure the SubdivisionInfo to not be static
        boolean preprocess = args.length > 0;
        if (preprocess) {
            for (String source : Arrays.asList(
                "2015-05-04_iso_country_code_ALL_xml",
                "2016-01-13_iso_country_code_ALL_xml",
                "2016-12-09_iso_country_code_ALL_xml",
                "2017-02-12_iso_country_code_ALL_xml",
                "2017-09-15_iso_country_code_ALL_xml",
                "2018-02-20_iso_country_code_ALL_xml",
                "2018-09-02_iso_country_code_ALL_xml")) {
                SubdivisionSet sdset1 = new SubdivisionSet(CLDRPaths.CLDR_PRIVATE_DIRECTORY + source + "/iso_country_codes.xml");
                try (PrintWriter pw = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY, "subdivision/" + source + ".txt")) {
                    sdset1.print(pw);
                }
            }
            return;
        }

        SubdivisionSet sdset1 = new SubdivisionSet(GenerateSubdivisions.ISO_SUBDIVISION_CODES);
        SubDivisionExtractor sdset = new SubDivisionExtractor(sdset1,
            SubdivisionInfo.VALIDITY_FORMER, 
            SubdivisionInfo.SUBDIVISION_ALIASES_FORMER, 
            SubdivisionInfo.formerRegionToSubdivisions);

        try (PrintWriter pw = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY, "subdivision/subdivisions.xml")) {
            sdset.printXml(pw);
        }
        try (PrintWriter pw = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY, "subdivision/subdivisionAliases.txt")) {
            sdset.printAliases(pw);
        }
        try (PrintWriter pw = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY, "subdivision/en.xml")) {
            sdset.printEnglish(pw);
        }
        try (PrintWriter pw = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY, "subdivision/categories.txt")) {
            sdset.printSamples(pw);
        }
        try (PrintWriter pw = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY, "subdivision/en.txt")) {
            sdset.printEnglishComp(pw);
        }
        try (PrintWriter pw = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY, "subdivision/en-full.txt")) {
            sdset.printEnglishCompFull(pw);
        }
        try (PrintWriter pw = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY, "subdivision/missing-mid.txt")) {
            sdset.printMissingMIDs(pw);
        }
    }

}
