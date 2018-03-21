package org.unicode.cldr.tool;

import java.util.Set;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.OfficialStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;

public class GetLanguageData {
    SupplementalDataInfo sdata = SupplementalDataInfo
        .getInstance(CLDRPaths.SUPPLEMENTAL_DIRECTORY);
    Factory cldrFactory = Factory
        .make(CLDRPaths.MAIN_DIRECTORY, ".*");
    CLDRFile english = cldrFactory.make("en", true);
    Set<String> euCountries = sdata.getContained("EU");
    Counter<String> languageToGdp = new Counter<String>();
    Counter<String> languageToPop = new Counter<String>();

    public static void main(String[] args) {
        new GetLanguageData().run();
    }

    private void run() {
        findSuspectData();
        System.out.println("Code\tLang\tLpop\tApprox. Gdp");
        for (String language : sdata.getLanguages()) {
            final long pop = languageToPop.getCount(language);
            System.out.print(language + "\t" + english.getName(language));
            if (pop > 0) {
                Pair<OfficialStatus, String> status = isOfficialLanguageOfEUCountry(language);
                System.out.print("\t" + pop //
                    + "\t" + languageToGdp.getCount(language) //
                    + "\t" + (status.getFirst().isOfficial() ? status.getFirst() : "") //
                    + "\t" + status.getSecond() //
                );
            }
            System.out.println();
        }
    }

    private void findSuspectData() {
        Set<String> territories = sdata.getTerritoriesWithPopulationData();
        for (String territory : territories) {
            double scale = 1.0;
            final PopulationData populationDataForTerritory = sdata
                .getPopulationDataForTerritory(territory);
            final double gdp = populationDataForTerritory.getGdp();
            double territoryPop = populationDataForTerritory.getPopulation();
            double langPop = 0;
            double officialLangPop = 0;
            Set<String> languages = sdata.getLanguagesForTerritoryWithPopulationData(territory);
            for (String language : languages) {
                if (language.equals("tl")) continue;
                PopulationData pop2 = sdata.getLanguageAndTerritoryPopulationData(language, territory);
                langPop += pop2.getPopulation();
                if (pop2.getOfficialStatus().isOfficial()) {
                    officialLangPop += pop2.getPopulation();
                }
            }
            final double missing = 0.75 * territoryPop - langPop;
            if (missing > 0) {
                System.out.println(territory //
                    + "\t" + english.getName("territory", territory) //
                    + "\t" + territoryPop //
                    + "\t" + langPop //
                    + "\t" + gdp //
                );
                scale = 1 + missing / officialLangPop;
                // scale up the official so that
                // official + non-official = 70% of total
                langPop = territoryPop * 0.75;
                System.out.println("\tScaling " + territory + "\t" + scale * 100 + "%");
            }
            long langUnknown = (long) territoryPop;
            for (String language : languages) {
                if (language.equals("tl")) continue;
                PopulationData pop2 = sdata.getLanguageAndTerritoryPopulationData(language, territory);
                double langPop2 = pop2.getPopulation();
                if (pop2.getOfficialStatus().isOfficial()) {
                    langPop2 *= scale;
                }
                languageToGdp.add(language, (long) (gdp * langPop2 / territoryPop));
                languageToPop.add(language, (long) (langPop2));
                langUnknown -= langPop2;
            }
            if (langUnknown > 0) {
                languageToGdp.add("und", (long) (gdp * langUnknown / territoryPop));
                languageToPop.add("und", (long) (langUnknown));
            }
        }
    }

    private Pair<OfficialStatus, String> isOfficialLanguageOfEUCountry(String language) {
        OfficialStatus bestStatus = OfficialStatus.unknown;
        String eu = "";
        double bestEuPop = 0;
        Set<String> territories = sdata.getTerritoriesForPopulationData(language);
        for (String territory : territories) {
            PopulationData pop = sdata.getLanguageAndTerritoryPopulationData(language, territory);
            OfficialStatus status = pop.getOfficialStatus();
            if (bestStatus.compareTo(status) < 0) {
                bestStatus = status;
            }
            if (status.isMajor() && euCountries.contains(territory)) {
                if (pop.getLiteratePopulation() > bestEuPop) {
                    bestEuPop = pop.getLiteratePopulation();
                    eu = territory;
                }
            }
        }
        return Pair.of(bestStatus, eu);
    }
}
