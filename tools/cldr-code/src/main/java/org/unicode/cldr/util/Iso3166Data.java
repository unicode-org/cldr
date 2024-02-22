package org.unicode.cldr.util;

import com.google.common.base.Splitter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.unicode.cldr.draft.FileUtilities;

public class Iso3166Data {
    public enum Iso3166Status {
        officially_assigned,
        private_use,
        exceptionally_reserved,
        indeterminately_reserved,
        transitionally_reserved,
        formerly_used,
        out_of_scope
    }

    public static final Map<String, Iso3166Status> getIsoStatus() {
        return Iso3166Helper.INSTANCE.isoStatus;
    }

    public static final Map<String, String> getIsoDescription() {
        return Iso3166Helper.INSTANCE.isoDescription;
    }

    private static final class Iso3166Helper {
        final Map<String, Iso3166Status> isoStatus;
        final Map<String, String> isoDescription;

        public Iso3166Helper() {
            Map<String, Iso3166Status> isoStatus = new TreeMap<>();
            Map<String, String> isoDescription = new TreeMap<>();
            Splitter semi = Splitter.on(';').trimResults();
            for (String line :
                    FileUtilities.in(Iso3166Data.class, "data/external/iso_3166_status.txt")) {
                if (line.startsWith("#")) continue;
                List<String> parts = semi.splitToList(line);
                // AC ; Exceptionally reserved  ; Refers to the United Nations and reserved by the
                // ISO 3166 Maintenance Agency.
                final String regionCode = parts.get(0);
                isoStatus.put(
                        regionCode,
                        Iso3166Status.valueOf(
                                parts.get(1).toLowerCase(Locale.ROOT).replace(' ', '_')));
                isoDescription.put(regionCode, parts.get(1));
            }
            this.isoStatus = Collections.unmodifiableMap(isoStatus);
            this.isoDescription = Collections.unmodifiableMap(isoDescription);
        }

        public static final Iso3166Helper INSTANCE = new Iso3166Helper();
    }

    private static final List<String> DELETED3166 =
            Collections.unmodifiableList(
                    Arrays.asList(
                            new String[] {
                                "BQ", "BU", "CT", "DD", "DY", "FQ", "FX", "HV", "JT", "MI", "NH",
                                "NQ", "NT", "PC", "PU", "PZ", "RH", "SU", "TP", "VD", "WK", "YD",
                                "YU", "ZR"
                            }));

    public static List<String> getOld3166() {
        return DELETED3166;
    }

    private static final Set<String> REGION_CODES_NOT_FOR_TRANSLATION =
            Collections.unmodifiableSet(Collections.singleton("CQ"));

    /** Exceptionally reserved ISO-3166-1 region code, but should not be translated at present. */
    public static Set<String> getRegionCodesNotForTranslation() {
        return REGION_CODES_NOT_FOR_TRANSLATION;
    }

    /**
     * Exceptionally reserved ISO-3166-1 region code, but should not be translated at present.
     *
     * @param territory
     * @return
     */
    public static boolean isRegionCodeNotForTranslation(String territory) {
        return (REGION_CODES_NOT_FOR_TRANSLATION.contains(territory));
    }
}
