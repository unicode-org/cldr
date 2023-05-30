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
        rational(new Params().setHelp("value, as rational").setMatch(".+")),
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
        UnitConverter uc = SDI.getUnitConverter();
        String rationalString = MyOptions.rational.option.getValue();
        String fromString = MyOptions.from.option.getValue();
        Rational rational = uc.parseRational(rationalString);
        Output<String> metricUnit = new Output<>();
        ConversionInfo convInfo = uc.parseUnitId(fromString, metricUnit, false);
        Rational toValue = convInfo.convert(rational);
        System.out.println(toValue.toString(FormatStyle.simple) + "\t" + metricUnit);
        if (MyOptions.to.option.doesOccur()) {
            String toString = MyOptions.to.option.getValue();
            Rational newRational = uc.convert(rational, fromString, toString, false);
            System.out.println(
                    newRational.toString(FormatStyle.simple)
                            + "\t"
                            + toString
                            + "\t≡ "
                            + newRational.doubleValue());
        }
    }
}
