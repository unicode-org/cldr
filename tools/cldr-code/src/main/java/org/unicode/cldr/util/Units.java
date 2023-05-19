package org.unicode.cldr.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetSpanner;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.Validity.Status;

public class Units {

    private static final UnicodeSet WHITESPACE = new UnicodeSet("[:whitespace:]").freeze();
    public static Pattern NO_SPACE_PREFIX =
            Pattern.compile(
                    "\\}"
                            + ExampleGenerator.backgroundEndSymbol
                            + "?\\p{L}|\\p{L}"
                            + ExampleGenerator.backgroundStartSymbol
                            + "?\\{");

    public static String combinePattern(
            String unitFormat, String compoundPattern, boolean lowercaseUnitIfNoSpaceInCompound) {
        // meterFormat of the form {0} meters or {0} Meter
        // compoundPattern is of the form Z{0} or Zetta{0}

        // extract the unit
        String modUnit = (String) SPACE_SPANNER.trim(unitFormat.replace("{0}", ""));
        Object[] parameters = {modUnit};

        String modFormat =
                unitFormat.replace(modUnit, MessageFormat.format(compoundPattern, parameters));
        if (modFormat.equals(unitFormat)) {
            // didn't work, so fall back
            Object[] parameters1 = {unitFormat};
            modFormat = MessageFormat.format(compoundPattern, parameters1);
        }

        // hack to fix casing
        if (lowercaseUnitIfNoSpaceInCompound && NO_SPACE_PREFIX.matcher(compoundPattern).find()) {
            modFormat = modFormat.replace(modUnit, modUnit.toLowerCase(Locale.ENGLISH));
        }

        return modFormat;
    }

    static final UnicodeSetSpanner SPACE_SPANNER = new UnicodeSetSpanner(WHITESPACE);

    public static final Map<String, String> CORE_TO_TYPE;
    public static final Multimap<String, String> TYPE_TO_CORE;
    public static final BiMap<String, String> LONG_TO_SHORT;

    static {
        Set<String> VALID_UNITS =
                Validity.getInstance().getStatusToCodes(LstrType.unit).get(Status.regular);

        Map<String, String> coreToType = new TreeMap<>();
        Multimap<String, String> typeToCore = TreeMultimap.create();
        Map<String, String> longToShort = new TreeMap<>();
        for (String s : VALID_UNITS) {
            int dashPos = s.indexOf('-');
            String unitType = s.substring(0, dashPos);
            String coreUnit = s.substring(dashPos + 1);
            longToShort.put(s, coreUnit);
            // coreUnit = converter.fixDenormalized(coreUnit);
            coreToType.put(coreUnit, unitType);
            typeToCore.put(unitType, coreUnit);
        }
        CORE_TO_TYPE = ImmutableMap.copyOf(coreToType);
        TYPE_TO_CORE = ImmutableMultimap.copyOf(typeToCore);
        LONG_TO_SHORT = ImmutableBiMap.copyOf(longToShort);
    }

    public static class TypeAndCore {
        public String type;
        public String core;
    }
    /**
     * Returns the type and core for a unit, be it long or short
     *
     * @param longOrShortUnit
     * @param core
     * @return
     * @return
     */
    public static TypeAndCore splitUnit(String longOrShortUnit, TypeAndCore typeAndCore) {
        int dashPos = longOrShortUnit.indexOf('-');
        String unitType = longOrShortUnit.substring(0, dashPos);
        Collection<String> cores = TYPE_TO_CORE.get(unitType);
        if (cores.isEmpty()) { // short unit
            typeAndCore.type = CORE_TO_TYPE.get(longOrShortUnit);
            typeAndCore.core = longOrShortUnit;
        } else {
            typeAndCore.type = unitType;
            typeAndCore.core = longOrShortUnit.substring(dashPos + 1);
        }
        return typeAndCore;
    }

    public static String getShort(String longUnit) {
        return LONG_TO_SHORT.get(longUnit);
    }

    public static String getLong(String shortId) {
        return LONG_TO_SHORT.inverse().get(shortId);
    }
}
