package org.unicode.cldr.tool;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.test.CasingInfo;
import org.unicode.cldr.test.CheckConsistentCasing.CasingType;
import org.unicode.cldr.test.CheckConsistentCasing.CasingTypeAndErrFlag;
import org.unicode.cldr.test.CheckConsistentCasing.Category;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.ChainedMap.M3;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.XPathParts;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.text.DisplayContext;
import com.ibm.icu.text.LocaleDisplayNames;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class GenerateCasingChart {

    enum ContextTransformUsage {
        languages, script, keyValue, calendar_field, day_format_except_narrow, day_standalone_except_narrow, month_format_except_narrow, month_standalone_except_narrow, era_name, era_abbr, relative, currencyName, number_spellout
    }

    enum ContextTransformType {
        stand_alone, uiListOrMenu
    }

    enum ContextTransformValue {
        titlecase_firstword, none // for "∅∅∅"
    }

    /*
     *  <contextTransformUsage type="day-format-except-narrow">
            <contextTransform type="stand-alone">titlecase-firstword</contextTransform>
        </contextTransformUsage>
        <contextTransformUsage type="day-standalone-except-narrow">
            <contextTransform type="stand-alone">titlecase-firstword</contextTransform>
        </contextTransformUsage>
        <contextTransformUsage type="languages">
            <contextTransform type="uiListOrMenu">titlecase-firstword</contextTransform>
        </contextTransformUsage>
        <contextTransformUsage type="month-format-except-narrow">
            <contextTransform type="stand-alone">titlecase-firstword</contextTransform>
        </contextTransformUsage>
        <contextTransformUsage type="month-standalone-except-narrow">
            <contextTransform type="stand-alone">titlecase-firstword</contextTransform>
        </contextTransformUsage>
    
     */
    public static void main(String[] args) {
        check(ULocale.ENGLISH);
        check(ULocale.FRENCH);
        check(new ULocale("ca"));

        CasingInfo casingInfo = new CasingInfo();
        String dir = CLDRPaths.COMMON_DIRECTORY + "/casing";
        Splitter period = Splitter.on(".");
        Relation<Map<Category, CasingType>, String> infoToLocales = Relation.of(new HashMap(), TreeSet.class);
        Factory factory = CLDRConfig.getInstance().getFullCldrFactory();

        System.out.print("Locale\tLevel\tCount\tCLanguage");
        for (ContextTransformUsage x : ContextTransformUsage.values()) {
            System.out.print("\t" + x);
        }
//        for (Category x : Category.values()) {
//            System.out.print("\t" + x);
//        }
        System.out.println();

        boolean showCasing = false;
        UnicodeSet changesUpper = new UnicodeSet("[:CWU:]").freeze();
        XPathParts parts = new XPathParts();
        for (String localeFile : new File(dir).list()) {
            String locale = period.split(localeFile).iterator().next();
            Map<Category, CasingTypeAndErrFlag> info;
            try {
                info = casingInfo.getLocaleCasing(locale);
            } catch (Exception e) {
                System.out.println(locale + "\t\t\tMalformed, skipping");
                continue;
            }
            CLDRFile cldrFile = factory.make(locale, true);
            UnicodeSet exemplars = cldrFile.getExemplarSet("", WinningChoice.WINNING);

            M3<ContextTransformUsage, ContextTransformType, ContextTransformValue> data = ChainedMap.of(
                new LinkedHashMap<ContextTransformUsage, Object>(),
                new LinkedHashMap<ContextTransformType, Object>(),
                ContextTransformValue.class);

            Level level = CLDRConfig.getInstance().getStandardCodes().getLocaleCoverageLevel("cldr", locale);
            boolean hasCasedLetters = changesUpper.containsSome(exemplars);
            Set<String> items = new LinkedHashSet<>();
            CollectionUtilities.addAll(cldrFile.iterator("//ldml/contextTransforms"), items);
            if (!hasCasedLetters) {
                if (items.size() != 0) {
                    System.out.println(locale + "Uncased language has context!!!");
                }
                continue;
            }
            System.out.print(locale);
            System.out.print("\t" + level);
            System.out.print("\t" + items.size());
            System.out.print("\t" + info.get(Category.language).type());
            if (items.size() == 0) {
                System.out.println("\tNo Context Items");
                continue;
            }
            //System.out.print(hasCasedLetters ? "\tCased" : "\tUncased");
            if (showCasing) {
                for (Category x : Category.values()) {
                    CasingType value = info.get(x).type();
                    System.out.print("\t" + (value == null ? "n/a"
                        : value.toString().toUpperCase(Locale.ENGLISH).charAt(0)));

                }
                System.out.println();
            }
            for (String path : items) {
                /*  <contextTransformUsage type="day-format-except-narrow">
                <contextTransform type="stand-alone">titlecase-firstword</contextTransform>
                 */
                parts.set(path);
                ContextTransformUsage contextTransformUsage = ContextTransformUsage.valueOf(parts.getAttributeValue(-2, "type")
                    .replace("-", "_"));
                ContextTransformType contextTransformType = ContextTransformType.valueOf(parts.getAttributeValue(-1, "type")
                    .replace("-", "_"));
                String stringValue = cldrFile.getStringValue(path);
                ContextTransformValue contextTransformValue;
                if (stringValue.equals("∅∅∅")) {
                    contextTransformValue = ContextTransformValue.none;
                } else {
                    contextTransformValue = ContextTransformValue.valueOf(stringValue
                        .replace("-", "_"));
                }
                data.put(contextTransformUsage, contextTransformType, contextTransformValue);
            }

            //infoToLocales.put(info, locale);
            for (ContextTransformUsage contextTransformUsage : ContextTransformUsage.values()) {
                Map<ContextTransformType, ContextTransformValue> map = data.get(contextTransformUsage);
                System.out.print("\t" + (map == null ? "n/a" : map.entrySet().toString().replace("=", " = ")));
            }
            System.out.println();
        }
//        for (Entry<Map<Category, CasingType>, Set<String>> entry : infoToLocales.keyValuesSet()) {
//            System.out.println(entry.getValue() + "\t" + entry.getKey());
//        }
    }

    private static void check(ULocale locale) {
        LocaleDisplayNames localeDisplayNames = LocaleDisplayNames.getInstance(locale, DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU);
        System.out.println(
            locale.getDisplayName(ULocale.ENGLISH)
                + "\t" + locale.getDisplayName(locale)
                + "\t" + localeDisplayNames.localeDisplayName(locale));
    }

}
