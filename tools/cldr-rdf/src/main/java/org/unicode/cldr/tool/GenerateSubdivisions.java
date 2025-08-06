package org.unicode.cldr.tool;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row.R2;
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
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;

public class GenerateSubdivisions {
    private static final String ISO_COUNTRY_CODES =
            CLDRPaths.CLDR_PRIVATE_DIRECTORY + "iso_country_codes/";
    static final String ISO_SUBDIVISION_CODES = ISO_COUNTRY_CODES + "iso_country_codes.xml";

    // TODO: consider whether to use the last archive directory to generate
    // There are pros and cons.
    // Pros are that we don't introduce "fake" deprecated elements that are introduced and
    // deprecated during the 6 month CLDR cycle
    // Cons are that we may have to repeat work

    static final class SubdivisionInfo {
        static final SupplementalDataInfo SDI_LAST =
                SupplementalDataInfo.getInstance(
                        CLDRPaths.LAST_RELEASE_DIRECTORY + "common/supplemental/");

        static final Map<String, R2<List<String>, String>> SUBDIVISION_ALIASES_FORMER =
                SDI_LAST.getLocaleAliasInfo().get("subdivision");

        static final SubdivisionNames SUBDIVISION_NAMES_ENGLISH_FORMER =
                new SubdivisionNames("en", "main", "subdivisions");

        static final Validity VALIDITY_FORMER =
                Validity.getInstance(CLDRPaths.LAST_RELEASE_DIRECTORY + "common/validity/");

        static final Relation<String, String> formerRegionToSubdivisions =
                Relation.of(
                        new HashMap<>(),
                        TreeSet.class,
                        CLDRConfig.getInstance().getComparatorRoot());

        static {
            Map<Status, Set<String>> oldSubdivisionData =
                    VALIDITY_FORMER.getStatusToCodes(LstrType.subdivision);
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
            for (Entry<String, R2<List<String>, String>> entry :
                    SUBDIVISION_ALIASES_FORMER.entrySet()) {
                String oldId = entry.getKey();
                for (String newId : entry.getValue().get0()) {
                    subdivisionIdToOld.put(newId, oldId);
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        CLDRConfig.getInstance().getSupplementalDataInfo();
        String outputDirectoryName = CLDRPaths.GEN_DIRECTORY + "subdivision";
        // TODO Restructure so that this call is done first to process the iso data
        // then the extraction uses that data.
        // also restructure the SubdivisionInfo to not be static
        boolean preprocess = args.length > 0;
        if (preprocess) {
            for (String source :
                    Arrays.asList(
                            "2015-05-04_iso_country_code_ALL_xml",
                            "2016-01-13_iso_country_code_ALL_xml",
                            "2016-12-09_iso_country_code_ALL_xml",
                            "2017-02-12_iso_country_code_ALL_xml",
                            "2017-09-15_iso_country_code_ALL_xml",
                            "2018-02-20_iso_country_code_ALL_xml",
                            "2018-09-02_iso_country_code_ALL_xml",
                            "2019-02-26_iso_country_code_ALL_xml",
                            "2020-03-05_iso_country_code_ALL_xml",
                            "2020-09-09_iso_country_code_ALL_xml",
                            "2021-09-14_iso_country_code_ALL_xml",
                            "2022-02-22_iso_country_code_ALL_xml",
                            "2022-03-18_iso_country_code_ALL_xml",
                            "2022-08-26_iso_country_code_ALL_xml")) {
                SubdivisionSet sdset1 =
                        new SubdivisionSet(
                                CLDRPaths.CLDR_PRIVATE_DIRECTORY
                                        + source
                                        + "/iso_country_codes.xml");
                String outputFileName = source + ".txt";
                System.out.println("Writing " + outputDirectoryName + "/" + outputFileName);
                try (PrintWriter pw =
                        FileUtilities.openUTF8Writer(outputDirectoryName, outputFileName)) {
                    sdset1.print(pw);
                }
            }
            return;
        }

        SubdivisionSet sdset1 = new SubdivisionSet(GenerateSubdivisions.ISO_SUBDIVISION_CODES);
        SubDivisionExtractor sdset =
                new SubDivisionExtractor(
                        sdset1,
                        SubdivisionInfo.VALIDITY_FORMER,
                        SubdivisionInfo.SUBDIVISION_ALIASES_FORMER,
                        SubdivisionInfo.formerRegionToSubdivisions);
        String outputFileName = "subdivisions.xml";
        System.out.println("Writing " + outputDirectoryName + "/" + outputFileName);
        try (PrintWriter pw = FileUtilities.openUTF8Writer(outputDirectoryName, outputFileName)) {
            sdset.printXml(pw);
        }
        outputFileName = "subdivisionAliases.txt";
        System.out.println("Writing " + outputDirectoryName + "/" + outputFileName);
        try (PrintWriter pw = FileUtilities.openUTF8Writer(outputDirectoryName, outputFileName)) {
            sdset.printAliases(pw);
        }
        outputFileName = "en.xml";
        System.out.println("Writing " + outputDirectoryName + "/" + outputFileName);
        try (PrintWriter pw = FileUtilities.openUTF8Writer(outputDirectoryName, outputFileName)) {
            sdset.printEnglish(pw);
        }
        outputFileName = "categories.txt";
        System.out.println("Writing " + outputDirectoryName + "/" + outputFileName);
        try (PrintWriter pw = FileUtilities.openUTF8Writer(outputDirectoryName, outputFileName)) {
            sdset.printSamples(pw);
        }
        outputFileName = "en.txt";
        System.out.println("Writing " + outputDirectoryName + "/" + outputFileName);
        try (PrintWriter pw = FileUtilities.openUTF8Writer(outputDirectoryName, outputFileName)) {
            sdset.printEnglishComp(pw);
        }
        outputFileName = "en-full.txt";
        System.out.println("Writing " + outputDirectoryName + "/" + outputFileName);
        try (PrintWriter pw = FileUtilities.openUTF8Writer(outputDirectoryName, outputFileName)) {
            sdset.printEnglishCompFull(pw);
        }
        outputFileName = "missing-mid.txt";
        System.out.println("Writing " + outputDirectoryName + "/" + outputFileName);
        try (PrintWriter pw = FileUtilities.openUTF8Writer(outputDirectoryName, outputFileName)) {
            sdset.printMissingMIDs(pw);
        }
    }
}
