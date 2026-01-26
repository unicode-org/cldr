package org.unicode.cldr.tool;

import java.util.EnumSet;
import java.util.Set;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.DateTimeFormats;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.NameGetter;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;

public class ListH {
    static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    static final SupplementalDataInfo SDI = CLDR_CONFIG.getSupplementalDataInfo();
    static final StandardCodes SC = StandardCodes.make();
    static final Factory CF = CLDR_CONFIG.getCldrFactory();

    public static void main(String[] args) {
        Set<String> modernModerateLocales =
                SC.getLocaleCoverageLocales(
                        Organization.cldr, EnumSet.of(Level.MODERN, Level.MODERATE));
        NameGetter namer = new NameGetter(CLDR_CONFIG.getEnglish());
        System.out.println("\t" + "Locale" + "\t" + "H" + "\t" + "Hv" + "\t" + "EH");

        for (String locale : modernModerateLocales) {
            CLDRFile cldrFile = CF.make(locale, true);
            DateTimeFormats dtf = new DateTimeFormats(cldrFile, "gregorian");
            String patH = dtf.getBestPattern("H");
            String patHv = dtf.getBestPattern("Hv");
            String patEH = dtf.getBestPattern("EH");
            System.out.println(
                    namer.getNameFromIdentifier(locale)
                            + "\t"
                            + locale
                            + "\t"
                            + patH
                            + "\t"
                            + patHv
                            + "\t"
                            + patEH);
        }
    }
}
