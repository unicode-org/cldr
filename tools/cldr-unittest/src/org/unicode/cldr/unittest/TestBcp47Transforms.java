package org.unicode.cldr.unittest;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.Builder.MBuilder;
import org.unicode.cldr.util.CLDRTransforms;
import org.unicode.cldr.util.CLDRTransforms.Direction;
import org.unicode.cldr.util.CLDRTransforms.MyHandler;
import org.unicode.cldr.util.CLDRTransforms.ParsedTransformID;
import org.unicode.cldr.util.CLDRTransforms.Visibility;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XMLFileReader;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.util.ULocale;

public class TestBcp47Transforms extends TestFmwk {

    public static void main(String[] args) {
        new TestBcp47Transforms().run(args);
    }

    public void TestNames() {
        SupplementalDataInfo suppData = SupplementalDataInfo.getInstance();
        Relation<String, String> extensionToKeys = suppData
            .getBcp47Extension2Keys();
        Set<String> keys = extensionToKeys.getAll("t");
        // extension="t" name="m0"
        Relation<String, String> keyToSubtypes = suppData.getBcp47Keys();
        Map<R2<String, String>, String> descriptions = suppData
            .getBcp47Descriptions();
        for (String key : keys) {
            for (String subtype : keyToSubtypes.getAll(key)) {
                String description = descriptions.get(Row.of(key, subtype));
                System.out.println(key + ", " + subtype + ", " + description);
            }
        }
        Map<String, String> old2newName = new TreeMap<String, String>();
        for (String file : Arrays.asList(new File(CLDRTransforms.TRANSFORM_DIR)
            .list())) {
            if (!file.endsWith(".xml"))
                continue;
            ParsedTransformID directionInfo = new ParsedTransformID();
            getIcuRulesFromXmlFile(CLDRTransforms.TRANSFORM_DIR, file,
                directionInfo);
            if (directionInfo.getVisibility() == Visibility.internal)
                continue;
            String source = directionInfo.source;
            String target = directionInfo.target;
            String variant = directionInfo.variant;
            String standard = getStandard0(source, target, variant);
            // System.out.println(standard
            // + "\t =>\t" + directionInfo
            // + "\tdirection:\t" + directionInfo.getDirection()
            // + "\tvisibility:\t" + directionInfo.getVisibility()
            // );
            if (!standard.contains("?")) {
                old2newName.put(directionInfo.toString(), standard);
            }
            if (directionInfo.getDirection() == Direction.both) {
                standard = getStandard0(source, target, variant);
                if (!standard.contains("?")) {
                    old2newName.put(directionInfo.toString(), standard);
                }
            }
        }
        for (String source : Collections.list(Transliterator
            .getAvailableSources())) {
            for (String target : Collections.list(Transliterator
                .getAvailableTargets(source))) {
                for (String variant : Collections.list(Transliterator
                    .getAvailableVariants(source, target))) {
                    if (variant.isEmpty())
                        variant = null;
                    String name = source + "-" + target
                        + (variant == null ? "" : "/" + variant);
                    if (!old2newName.containsKey(name)) {
                        String standard = getStandard0(source, target, variant);
                        if (!standard.contains("?")) {
                            old2newName.put(name, standard);
                        }
                    }
                }
            }
        }
        for (Entry<String, String> entry : old2newName.entrySet()) {
            System.out.println(entry);
        }
        System.out.println("Missing");
        for (Entry<String, Set<R2<Type, String>>> entry : MISSING
            .keyValuesSet()) {
            System.out.println(entry);
        }
    }

    enum Type {
        source, target, mechanism
    }

    private String getStandard0(String source, String target, String variant) {
        String id = source + "-" + target + "/" + variant;
        String newSource = getStandard(Type.source, source, id);
        String newTarget = getStandard(Type.target, target, id);
        String newMechanism = getStandard(Type.mechanism, variant, id);
        return newTarget + "-t-" + newSource
            + (newMechanism == null ? "" : "-m0-" + newMechanism);
    }

    static ULocale.Builder ubuilder = new ULocale.Builder();
    static Relation<String, Row.R2<Type, String>> MISSING = Relation.<String, Row.R2<Type, String>> of(
        new TreeMap<String, Set<Row.R2<Type, String>>>(),
        TreeSet.class);
    static StandardCodes sc = StandardCodes.make();

    static Map<String, String> SPECIAL_CASES;
    static Set<String> languages = sc.getAvailableCodes("language");
    static Set<String> scripts = new HashSet<String>();
    static Set<String> regions = new HashSet<String>();
    static {

        MBuilder<String, String, HashMap<String, String>> builder = Builder
            .with(new HashMap<String, String>());
        // add language names
        for (String s : languages) {
            final String data = sc.getData("language", s);
            add(builder, s, data);
        }
        // add script names. They override (eg Latin => und-Latn)
        for (String s : sc.getAvailableCodes("script")) {
            scripts.add(s.toLowerCase(Locale.ENGLISH));
            final String data = sc.getData("script", s);
            add(builder, "und-" + s, data);
            // System.out.println(data + "\t" + s);
        }
        for (String s : sc.getAvailableCodes("territory")) {
            regions.add(s.toLowerCase(Locale.ENGLISH));
        }
        // real special cases
        builder.put("any", "und").put("simplified", "Hans")
            .put("traditional", "Hant").put("ipa", "und-fonipa")
            .put("xsampa", "und-fonxsamp").put("japanesekana", "und-Hrkt");
        /*
         * source fullwidth source jamo target accents target ascii target
         * halfwidth target jamo target numericpinyin target publishing
         */
        SPECIAL_CASES = builder.freeze();

    }

    public static void add(
        MBuilder<String, String, HashMap<String, String>> builder,
        String code, String names) {
        names = names.toLowerCase(Locale.ENGLISH);
        if (!names.contains("▪")) {
            builder.put(names, code);
            return;
        }
        for (String name : names.split("▪")) {
            builder.put(name, code);
        }
    }

    private String getStandard(Type type, String source, String id) {
        source = source == null ? null : source.toLowerCase(Locale.ENGLISH);
        if (type == Type.mechanism) {
            if (source == null)
                return null;
            if (source.equals("bgn") || source.equals("ungegn"))
                return source;
            MISSING.put(source, Row.of(type, id));
            return "?" + source;
        }
        String special = SPECIAL_CASES.get(source);
        if (special != null) {
            return special;
        }
        int code;
        try {
            code = UCharacter.getPropertyValueEnum(UProperty.SCRIPT, source);
            return "und-" + UScript.getShortName(code);
        } catch (Exception e1) {
        }
        try {
            ULocale ulocale = new ULocale(source);
            // hack for now
            String language = ulocale.getLanguage();
            if (languages.contains(language)) {
                String script = ulocale.getScript();
                if (script.isEmpty()
                    || scripts.contains(script.toLowerCase(Locale.ENGLISH))) {
                    String region = ulocale.getCountry();
                    if (region.isEmpty()
                        || regions.contains(region
                            .toLowerCase(Locale.ENGLISH))) {
                        return ulocale.toLanguageTag();
                    }
                }
            }
        } catch (Exception e) {
        }
        // we failed
        MISSING.put(source, Row.of(type, id));
        return "?" + source;
    }

    public String getIcuRulesFromXmlFile(String dir, String cldrFileName,
        ParsedTransformID directionInfo) {
        final MyHandler myHandler = new CLDRTransforms.MyHandler(cldrFileName,
            directionInfo);
        XMLFileReader xfr = new XMLFileReader().setHandler(myHandler);
        xfr.read(dir + cldrFileName, XMLFileReader.CONTENT_HANDLER
            | XMLFileReader.ERROR_HANDLER, true);
        return myHandler.getRules();
    }
}
