package org.unicode.cldr.tool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.util.ULocale;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.personname.PersonNameFormatter;
import org.unicode.cldr.util.personname.PersonNameFormatter.FallbackFormatter;
import org.unicode.cldr.util.personname.PersonNameFormatter.Formality;
import org.unicode.cldr.util.personname.PersonNameFormatter.FormatParameters;
import org.unicode.cldr.util.personname.PersonNameFormatter.Length;
import org.unicode.cldr.util.personname.PersonNameFormatter.Order;
import org.unicode.cldr.util.personname.PersonNameFormatter.SampleType;
import org.unicode.cldr.util.personname.PersonNameFormatter.Usage;
import org.unicode.cldr.util.personname.SimpleNameObject;
import org.unicode.cldr.util.personname.SimpleNameObject.ShowStyle;

public class GeneratePersonNameTest {

    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    private static final Factory FACTORY = CONFIG.getCldrFactory();

    public static void main(String[] args) {
        Set<String> seenAlready = new HashSet<>();
        for (String locale : ImmutableList.of("ja", "zh", "zh_Hant", "yue", "th", "lo")) { // "my",
            CLDRFile cldrFile = FACTORY.make(locale, true);
            PersonNameFormatter pnf = new PersonNameFormatter(cldrFile);
            Map<SampleType, SimpleNameObject> sampleNames =
                    PersonNameFormatter.loadSampleNames(cldrFile);
            if (sampleNames == null) {
                System.out.println("formatter; " + locale + "; NO SAMPLE NAMES" + "\n");
                continue;
            }
            SimpleNameObject sampleName1 = sampleNames.get(SampleType.nativeGGS);
            if (sampleName1 == null) {
                System.out.println("formatter; " + locale + "; NO SAMPLE NAMES" + "\n");
                continue;
            }
            Map<ULocale, Order> localeToOrder = pnf.getNamePatternData().getLocaleToOrder();
            Multimap<Order, String> orderToLocales = TreeMultimap.create();
            for (Entry<ULocale, Order> entry : localeToOrder.entrySet()) {
                orderToLocales.put(entry.getValue(), entry.getKey().toLanguageTag());
            }

            System.out.println(
                    locale
                            + ";\t"
                            + "givenFirst; "
                            + PersonNameFormatter.JOIN_SPACE.join(
                                    orderToLocales.get(Order.givenFirst)));
            System.out.println(
                    locale
                            + ";\t"
                            + "surnameFirst; "
                            + PersonNameFormatter.JOIN_SPACE.join(
                                    orderToLocales.get(Order.surnameFirst)));
            FallbackFormatter f = pnf.getFallbackInfo();
            String fsr = f.getForeignSpaceReplacement();

            for (SampleType sampleType : Arrays.asList(SampleType.nativeGGS, SampleType.nativeGS)) {
                SimpleNameObject sampleName = sampleNames.get(sampleType);

                System.out.println(
                        "\n"
                                + locale
                                + ";\t"
                                + "name; "
                                + locale
                                + "; "
                                + SimpleNameObject.show(
                                        sampleName.getPatternData(), ShowStyle.semicolon));

                seenAlready.clear();

                for (boolean hasSpace : Arrays.asList(true, false)) {
                    for (Order order : Order.ALL) {
                        for (Length length : Length.ALL) {
                            for (Usage usage : Usage.ALL) {
                                for (Formality formality : Formality.ALL) {
                                    FormatParameters parameters =
                                            new FormatParameters(order, length, usage, formality);
                                    String result = pnf.format(sampleName, parameters);
                                    result = result.replace(" ", fsr);
                                    if (!seenAlready.contains(result)) {
                                        if (result.contains(" ") != hasSpace) {
                                            continue;
                                        }
                                        System.out.println(
                                                locale
                                                        + ";\t"
                                                        + "format"
                                                        + "; "
                                                        + parameters.dashed()
                                                        + ";\t"
                                                        + result
                                                        + "\t; "
                                                        + (hasSpace ? "space" : ""));
                                        seenAlready.add(result);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            System.out.println();
        }
    }
}
