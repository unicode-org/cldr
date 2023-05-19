package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.ibm.icu.util.Output;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.cldr.util.MapComparator;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.ConversionInfo;
import org.unicode.cldr.util.UnitConverter.UnitSystem;
import org.unicode.cldr.util.Units;
import org.unicode.cldr.util.Units.TypeAndCore;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;

public class UnitValidityCanonicalizer {
    static final SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
    static final UnitConverter uc = sdi.getUnitConverter();

    static final MapComparator<String> categoryComparator =
            new MapComparator<>(
                            Arrays.asList(
                                    "duration",
                                    "graphics",
                                    "length",
                                    "area",
                                    "volume",
                                    "acceleration",
                                    "speed",
                                    "mass",
                                    "energy",
                                    "magnetic",
                                    "power",
                                    "electric",
                                    "frequency",
                                    "pressure",
                                    "temperature",
                                    "digital",
                                    "short",
                                    "angle",
                                    "concentr",
                                    "consumption",
                                    "force",
                                    "light",
                                    "torque"))
                    .setErrorOnMissing(true)
                    .freeze();

    public static Comparator<String> UNIT_COMPARATOR =
            new Comparator<>() {

                @Override
                public int compare(String o1, String o2) {
                    TypeAndCore tc1 = Units.splitUnit(o1, new TypeAndCore());
                    TypeAndCore tc2 = Units.splitUnit(o2, new TypeAndCore());

                    // now we check the categories (=types)
                    int comp = categoryComparator.compare(tc1.type, tc2.type);
                    if (comp != 0) {
                        return comp;
                    }

                    // first get quantities
                    String q1 = getTweakedQuantityFromUnit(tc1);
                    String q2 = getTweakedQuantityFromUnit(tc2);
                    comp = q1.compareTo(q2); // fix later
                    if (comp != 0) {
                        return comp;
                    }

                    Set<UnitSystem> s1 = uc.getSystemsEnum(tc1.core);
                    Set<UnitSystem> s2 = uc.getSystemsEnum(tc2.core);
                    comp = Integer.compare(getOrder(s1), getOrder(s2));
                    if (comp != 0) {
                        return comp;
                    }

                    Output<String> metricUnit1 = new Output<>();
                    ConversionInfo ci1 = uc.parseUnitId(tc1.core, metricUnit1, false);

                    Output<String> metricUnit2 = new Output<>();
                    ConversionInfo ci2 = uc.parseUnitId(tc2.core, metricUnit2, false);

                    comp = ci1.factor.compareTo(ci2.factor);
                    if (comp != 0) {
                        return comp;
                    }

                    comp = ci2.offset.compareTo(ci2.offset);
                    if (comp != 0) {
                        return comp;
                    }

                    return o1.compareTo(o2);
                }
            };

    public static int getOrder(Set<UnitSystem> set) { // depends on the set being ordered
        for (UnitSystem us : set) {
            return us.ordinal();
        }
        return 666;
    }

    private static String getTweakedQuantityFromUnit(TypeAndCore tc1) {
        String result = uc.getQuantityFromUnit(tc1.core, false);
        return result == null ? "unknown" : result;
    }

    public static List<String> getInfo(String o1) {
        List<String> result = new ArrayList<>();
        TypeAndCore tc1 = Units.splitUnit(o1, new TypeAndCore());
        result.add(tc1.type);
        final String tweakedQuantity = getTweakedQuantityFromUnit(tc1);
        result.add(tweakedQuantity);
        result.add(tweakedQuantity.equals(tc1.type) ? "" : "≠");
        if (!tweakedQuantity.equals("unknown")) {
            result.add(uc.getSystemsEnum(tc1.core).toString());
            Output<String> metricUnit1 = new Output<>();
            final ConversionInfo parseUnitId = uc.parseUnitId(tc1.core, metricUnit1, false);
            result.add(parseUnitId.toDecimal());
            result.add(metricUnit1.toString());
        }
        return result;
    }

    public static void main(String[] args) {
        Map<Status, Set<String>> validityUnits =
                Validity.getInstance().getStatusToCodes(LstrType.unit);
        Set<String> sorted = new TreeSet<>(UNIT_COMPARATOR);
        System.out.println(
                "long unit\tST category\tquantity\tST!=Q\tsystems\tfactor / offset from:\tbase unit");
        for (Entry<Status, Set<String>> entry : validityUnits.entrySet()) {
            if (entry.getKey() == Status.deprecated) {
                continue;
            }
            sorted.addAll(entry.getValue());
            System.out.println("Status=" + entry.getKey());
            for (String s : sorted) {
                System.out.println(s + "\t" + Joiner.on('\t').join(getInfo(s)));
            }
        }
        System.out.println("\n#FOR VALIDITY");
        System.out.println(Joiner.on('\n').join(sorted));
    }
}
