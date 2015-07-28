package org.unicode.cldr.tool;

import java.io.File;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.AttributeValueValidity;
import org.unicode.cldr.util.AttributeValueValidity.LocaleSpecific;
import org.unicode.cldr.util.AttributeValueValidity.Status;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.DayPeriodInfo;
import org.unicode.cldr.util.DayPeriodInfo.DayPeriod;
import org.unicode.cldr.util.DayPeriodInfo.Type;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdData.Attribute;
import org.unicode.cldr.util.DtdData.Element;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.AttributeValidityInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XMLFileReader.SimpleHandler;
import org.unicode.cldr.util.XPathParts;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.impl.Row.R5;
import com.ibm.icu.util.Output;

public class VerifyAttributeValues extends SimpleHandler {
    private static final File BASE_DIR = new File(CLDRPaths.BASE_DIRECTORY);
    private static final SupplementalDataInfo supplementalData = CLDRConfig.getInstance().getSupplementalDataInfo();

    public static final Joiner SPACE_JOINER = Joiner.on(' ');
    public static final Splitter SPACE_SPLITTER = Splitter.on(' ').trimResults().omitEmptyStrings();

    public final static class Errors {

        @SuppressWarnings({ "unchecked", "rawtypes" })
        final ChainedMap.M5<String, String, String, String, String> file_element_attribute
        = ChainedMap.of(new TreeMap(), new TreeMap(), new TreeMap(), new TreeMap(), String.class);

        /**
         * @param file
         * @param element
         * @param attribute
         * @param value
         * @return
         * @see org.unicode.cldr.util.ChainedMap.M4#put(java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)
         */
        public void put(String file, String element, String attribute, String attributeValue, String value) {
            file_element_attribute.put(file, element, attribute, attributeValue, value);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static ChainedMap.M5<DtdType, String, String, String, Boolean> missing_dtd_element_attribute_values 
    = ChainedMap.of(new EnumMap(DtdType.class), new TreeMap(), new TreeMap(), new TreeMap(), Boolean.class);

    final Errors file_element_attribute;

    final String file;
    DtdData dtdData;
    boolean isEnglish;
    EnumMap<LocaleSpecific,Set<String>> localeSpecific = new EnumMap<>(LocaleSpecific.class);

    private VerifyAttributeValues(String fileName, Errors file_element_attribute) {
        this.file_element_attribute = file_element_attribute;
        this.file = fileName.startsWith(BASE_DIR.toString()) ? fileName.substring(BASE_DIR.toString().length()) : fileName;
    }

    public static void check(String fileName, Errors errors) {
        try {
            final VerifyAttributeValues platformHandler = new VerifyAttributeValues(fileName, errors);
            new XMLFileReader()
            .setHandler(platformHandler)
            .read(fileName, -1, true);
        } catch (Exception e) {
            throw new IllegalArgumentException(fileName, e);
        }
    }
    
    public void handlePathValue(String path, String value) {
        XPathParts parts = XPathParts.getFrozenInstance(path);
        if (dtdData == null) {
            dtdData = DtdData.getInstance(DtdType.valueOf(parts.getElement(0)));
            if (dtdData.dtdType == DtdType.ldml) {
                String name = file;
                String locale = name.substring(name.lastIndexOf('/')+1, name.lastIndexOf('.'));
                localeSpecific.put(LocaleSpecific.pluralCardinal, supplementalData.getPlurals(PluralType.cardinal, locale).getPluralRules().getKeywords());
                localeSpecific.put(LocaleSpecific.pluralOrdinal, supplementalData.getPlurals(PluralType.ordinal, locale).getPluralRules().getKeywords());
                localeSpecific.put(LocaleSpecific.dayPeriodFormat, getPeriods(Type.format, locale));
                localeSpecific.put(LocaleSpecific.dayPeriodSelection, getPeriods(Type.selection, locale));
            } else {
                localeSpecific.clear();
            }
            AttributeValueValidity.setLocaleSpecifics(localeSpecific);
        }
        // TODO validate count=, dayperiod=,...
        // TODO validate ldml file name, ids
        // <identity><language type="en"/>

        for (int i = 0; i < parts.size(); ++i) {
            if (parts.getAttributeCount(i) == 0) continue;
            Map<String, String> attributes = parts.getAttributes(i);
            String element = parts.getElement(i);
            if (element.equals("attributeValues")) {
                continue; // don't look at ourselves in the mirror
            }
            Element elementInfo = dtdData.getElementFromName().get(element);

            for (String attribute : attributes.keySet()) {
                Attribute attributeInfo = elementInfo.getAttributeNamed(attribute);
                if (!attributeInfo.values.isEmpty()) {
                    // we don't need to check, since the DTD will enforce values
                    continue;
                }
                String attributeValue = attributes.get(attribute);
                if (dtdData.isDeprecated(element, attribute, attributeValue)) {
                    if (isEnglish) {
                        continue; // don't flag English
                    }
                    file_element_attribute.put(file, element, attribute, attributeValue, "deprecated");
                    continue;
                }

                //                // special hack for         // <type key="calendar" type="chinese">Chinese Calendar</type>
//                if (element.equals("type") && attribute.equals("type")) {
//                    Set<String> typeValues = BCP47_KEY_VALUES.get(attributes.get("key"));
//                    if (!typeValues.contains(attributeValue)) {
//                        result.add(new CheckStatus()
//                        .setCause(this).setMainType(CheckStatus.errorType).setSubtype(Subtype.unexpectedAttributeValue)
//                        .setMessage("Unexpected Attribute Value {0}={1}: expected: {2}",
//                            new Object[] { attribute, attributeValue, typeValues }));
//                    }
//                    continue;
//                }
                // check the common attributes first

                Output<String> reason = new Output<>();
                Status haveTest = AttributeValueValidity.check(dtdData, element, attribute, attributeValue, reason);
                switch(haveTest) {
                case ok:
                    break;
                case deprecated: 
                case illegal:
                    file_element_attribute.put(file, element, attribute, attributeValue, reason.value);
                    break;
                case noTest:
                    missing_dtd_element_attribute_values.put(dtdData.dtdType, element, attribute, attributeValue, Boolean.TRUE);
                    break;
                }
            }
        }
    }

    private Set<String> getPeriods(Type selection, String locale) {
        Set<String> result = new TreeSet<>();
        final DayPeriodInfo dayPeriods = supplementalData.getDayPeriods(Type.format, locale);
        for (DayPeriod period : dayPeriods.getPeriods()) {
            result.add(period.toString());
        };
        result.add("am");
        result.add("pm");
        return new LinkedHashSet<>(result);
    }


    static int findAttributeValues(File file, int max, Matcher fileMatcher, Errors errors) {
        final String name = file.getName();
        if (file.isDirectory() 
            && !name.equals("specs") 
            && !name.equals("tools")
            && !file.toString().contains(".svn")
           // && !name.equals("keyboards") // TODO reenable keyboards
            ) {
            int processed = 0;
            int count = max;
            for (File subfile : file.listFiles()) {
                final String subname = subfile.getName();
                if (--count < 0 
                    && !"en.xml".equals(subname)
                    && !"root.xml".equals(subname)
                    ) {
                    continue;
                }
                processed += findAttributeValues(subfile, max, fileMatcher, errors);
            }
            System.out.println("Processed files: " + processed + " \tin " + file);
            return processed;
        } else if (name.endsWith(".xml")) {
            if (fileMatcher == null || fileMatcher.reset(name.substring(0, name.length()-4)).matches()) {
                check(file.toString(), errors);
                return 1;
            }
        }
        return 0;
    }


    public static void main(String[] args) {
        int maxPerDirectory = args.length > 0 ? Integer.parseInt(args[0]) : Integer.MAX_VALUE;
        Matcher fileMatcher = args.length > 1 ? Pattern.compile(args[1]).matcher("") : null;
        //checkScripts();
        quickTest();
        Errors errors = new Errors();
        VerifyAttributeValues.findAttributeValues(BASE_DIR, maxPerDirectory, fileMatcher, errors);

        System.out.println("\n* READ ERRORS *\n");
        int count = 0;
        for (Entry<AttributeValidityInfo, String> entry : AttributeValueValidity.getReadFailures().entrySet()) {
            System.out.println(++count + "\t" + entry.getKey() + " => " + entry.getValue());
        }

        System.out.println("\n* MISSING TESTS *\n");
        count = 0;
        for (Entry<DtdType, Map<String, Map<String, Map<String, Boolean>>>> entry1 : missing_dtd_element_attribute_values) {
            DtdType dtdType = entry1.getKey();
            for (Entry<String, Map<String, Map<String, Boolean>>> entry2 : entry1.getValue().entrySet()) {
                String element = entry2.getKey();
                for (Entry<String, Map<String, Boolean>> entry3 : entry2.getValue().entrySet()) {
                    String attribute = entry3.getKey();
                    Set<String> attributeValues = entry3.getValue().keySet();
                    System.out.println(AttributeValueValidity.getElementLine(dtdType, element, attribute, SPACE_JOINER.join(attributeValues)));
                }
            }
        }

        System.out.println("\n* TODO TESTS *\n");
        count = 0;
        for (R3<DtdType, String, String> entry1 : AttributeValueValidity.getTodoTests()) {
            System.out.println(++count + "\t" + AttributeValueValidity.getElementLine(entry1.get0(), entry1.get1(), entry1.get2(), ""));
        }

        System.out.println("\n* DEPRECATED *\n");
        count = 0;
        for (R5<String, String, String, String, String> item : errors.file_element_attribute.rows()) {
            if ("deprecated".equals(item.get4()))
                System.out.println(++count 
                    + "; \t" + item.get0()
                    + "; \t" + item.get1()
                    + "; \t" + item.get2()
                    + "; \t" + item.get3()
                    + "; \t" + item.get4()
                    );
        }

        System.out.println("\n* ERRORS *\n");
        count = 0;
        for (R5<String, String, String, String, String> item : errors.file_element_attribute.rows()) {
            if (!"deprecated".equals(item.get4()))
                System.out.println(++count 
                    + "; \t" + item.get0()
                    + "; \t" + item.get1()
                    + "; \t" + item.get2()
                    + "; \t" + item.get3()
                    + "; \t" + item.get4()
                    );
        }
    }

    private static void quickTest() {
        for (String test : Arrays.asList(
            "/common/supplemental/supplementalMetadata.xml;     territoryAlias;     replacement;    AA"
            )) {
            quickTest(test);
        }
    }

    static final Splitter SEMI_SPACE = Splitter.on(';').trimResults().omitEmptyStrings();
    
    private static Status quickTest(String test) {
        List<String> parts = SEMI_SPACE.splitToList(test);
        Output<String> reason = new Output<>();
        Status value = AttributeValueValidity.check(DtdData.getInstance(DtdType.supplementalData), parts.get(1), parts.get(2), parts.get(3), reason);
        if (value != value.ok) {
            System.out.println(test + "\t" + value + "\t" + reason);
        }
        return value;
    }
    
//    private static void checkScripts() {
//        Set<String> oldScripts = new HashSet<String>(SPACE_SPLITTER.splitToList(
//            "Afak Ahom Aghb Arab Armi Armn Avst " +
//            "Bali Bamu Bass Batk Beng " +
//            "Blis Bopo Brah Brai Bugi Buhd " +
//            "Cakm Cans Cari Cham Cher Cirt Copt Cprt Cyrl Cyrs " +
//            "Deva Dsrt Dupl " +
//            "Egyd Egyh Egyp Elba Ethi " +
//            "Geok Geor Glag Goth Gran Grek Gujr Guru " +
//            "Hang Hani Hano Hans Hant Hatr " +
//            "Hebr Hira Hluw Hmng Hrkt Hung " +
//            "Inds Ital " +
//            "Java Jpan Jurc " +
//            "Kali Kana Khar Khmr Khoj Knda Kore Kpel Kthi " +
//            "Lana Laoo Latf Latg Latn Lepc Limb Lina Linb Lisu Loma Lyci Lydi " +
//            "Mahj Mand Mani Maya Mend Merc " +
//            "Mero Mlym Modi Mong Moon Mroo Mtei Mult Mymr " +
//            "Narb Nbat Nkgb Nkoo Nshu " +
//            "Ogam Olck Orkh Orya Osma " +
//            "Palm Pauc Perm Phag Phli Phlp Phlv Phnx Plrd Prti " +
//            "Qaaa Qaab Qaac Qaad Qaae Qaaf Qaag Qaah Qaaj " +
//            "Qaak Qaal Qaam Qaan Qaao Qaap Qaaq " +
//            "Qaar Qaas Qaat Qaau Qaav Qaaw Qaax Qaay Qaaz Qaba Qabb Qabc " +
//            "Qabd Qabe Qabf Qabg " +
//            "Qabh Qabi Qabj Qabk Qabl Qabm Qabn Qabo Qabp Qabq Qabr Qabs Qabt Qabu Qabv " +
//            "Qabw " +
//            "Qabx " +
//            "Rjng Roro Runr " +
//            "Samr Sara Sarb Saur Sgnw Shaw Shrd Sidd Sind Sinh Sora Sund Sylo Syrc Syre " +
//            "Syrj " +
//            "Syrn " +
//            "Tagb Takr Tale Talu Taml Tang Tavt Telu Teng Tfng Tglg Thaa Thai Tibt Tirh " +
//            "Ugar " +
//            "Vaii Visp " +
//            "Wara Wole " +
//            "Xpeo Xsux " +
//            "Yiii " +
//            "Zinh Zmth Zsym Zxxx Zyyy Zzzz"));
//        Validity validity = Validity.getInstance();
//        Map<Validity.Status, Set<String>> scripts = validity.getData().get(LstrType.script);
//        HashSet<String> newScripts = new HashSet<String>();
//        for (Entry<Validity.Status, Set<String>> e : scripts.entrySet()) {
//            if (e.getKey() != Validity.Status.deprecated) {
//                newScripts.addAll(e.getValue());
//            }
//        }
//        if (!oldScripts.equals(newScripts)) {
//            HashSet<String> oldMinusNew = new HashSet<>(oldScripts);
//            oldMinusNew.removeAll(newScripts);
//            HashSet<String> newMinusOld = new HashSet<>(newScripts);
//            newMinusOld.removeAll(oldScripts);
//
//            throw new IllegalArgumentException("oldMinusNew: " + oldMinusNew + ", newMinusOld: " + newMinusOld);
//        }
//    }
}