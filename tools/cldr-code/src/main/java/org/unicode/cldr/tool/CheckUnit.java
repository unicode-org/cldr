package org.unicode.cldr.tool;

import com.ibm.icu.util.Output;
import java.util.Set;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.tool.Option.Params;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.Rational.FormatStyle;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.ConversionInfo;

public class CheckUnit {
    private static final SupplementalDataInfo SDI =
            CLDRConfig.getInstance().getSupplementalDataInfo();

    private enum MyOptions {
        rational(new Params().setHelp("value, as rational").setMatch(".+").setDefault("1")),
        from(new Params().setHelp("source unit").setMatch(".+")),
        to(new Params().setHelp("target as unit").setMatch(".+")),
        verbose(new Params().setHelp("verbose debugging messages")),
        ;

        // BOILERPLATE TO COPY
        final Option option;

        private MyOptions(Params params) {
            option = new Option(this, params);
        }

        private static Options myOptions = new Options();

        static {
            for (MyOptions option : MyOptions.values()) {
                myOptions.add(option, option.option);
            }
        }

        private static Set<String> parse(String[] args) {
            return myOptions.parse(MyOptions.values()[0], args, true);
        }
    }

    public static void main(String[] args) {
        MyOptions.parse(args);
        boolean verbose = MyOptions.verbose.option.doesOccur();
        UnitConverter uc = SDI.getUnitConverter();
        String rationalString = MyOptions.rational.option.getValue();
        String fromString = MyOptions.from.option.getValue();
        Rational rational = uc.parseRational(rationalString);
        Output<String> metricUnit = new Output<>();
        ConversionInfo convInfo = uc.parseUnitId(fromString, metricUnit, verbose);
        if (convInfo == null) {
            System.out.println("Can't parse unit: " + fromString);
            uc.parseUnitId(fromString, metricUnit, true);
            return;
        }
        Rational toValue = convInfo.convert(rational);
        String reducedUnit = uc.getReducedUnit(metricUnit.value);
        String standardUnit = uc.getStandardUnit(metricUnit.value);
        System.out.println(
                toValue.toString(FormatStyle.formatted)
                        + "\t"
                        + metricUnit
                        + (reducedUnit.equals(metricUnit.value) ? "" : "\t≡ " + reducedUnit)
                        + (standardUnit.equals(metricUnit.value) ? "" : "\t≡ " + standardUnit));
        if (MyOptions.to.option.doesOccur()) {
            String toString = MyOptions.to.option.getValue();
            convInfo = uc.parseUnitId(toString, metricUnit, verbose);
            if (convInfo == null) {
                System.out.println("Can't parse unit: " + toString);
                uc.parseUnitId(toString, metricUnit, true);
                return;
            }

            Rational newRational = uc.convert(rational, fromString, toString, verbose);
            if (newRational.equals(Rational.NaN)) {
                System.out.println(
                        "Can't convert between units: " + fromString + " to " + toString);
                uc.convert(rational, fromString, toString, true);
                return;
            }
            System.out.println(
                    newRational.toString(FormatStyle.formatted)
                            + "\t"
                            + toString
                            + "\t≡ "
                            + newRational.doubleValue());
        }
    }
}
