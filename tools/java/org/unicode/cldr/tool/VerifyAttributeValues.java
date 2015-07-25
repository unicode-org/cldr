package org.unicode.cldr.tool;

import java.io.File;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.util.AttributeValueValidity;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.DayPeriodInfo;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.AttributeValueValidity.Status;
import org.unicode.cldr.util.DayPeriodInfo.Type;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdData.Attribute;
import org.unicode.cldr.util.DtdData.Element;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo.AttributeValidityInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XMLFileReader.SimpleHandler;
import org.unicode.cldr.util.XPathParts;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.impl.Row.R5;
import com.ibm.icu.util.Output;

public class VerifyAttributeValues extends SimpleHandler {
    private static final File BASE_DIR = new File(CLDRPaths.BASE_DIRECTORY);

    public static final Joiner SPACE_JOINER = Joiner.on(' ');
    public static final Splitter SPACE_SPLITTER = Splitter.on(' ').trimResults().omitEmptyStrings();

    public final static class Errors {

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

    @SuppressWarnings("unchecked")
    private static ChainedMap.M5<DtdType, String, String, String, Boolean> missing_dtd_element_attribute_values 
    = ChainedMap.of(new EnumMap(DtdType.class), new TreeMap(), new TreeMap(), new TreeMap(), Boolean.class);

    @SuppressWarnings("unchecked")
    final Errors file_element_attribute;

    final String file;
    DtdData dtdData;
    boolean isEnglish;
    PluralInfo cardinalPluralInfo;
    PluralInfo ordinalPluralInfo;
    DayPeriodInfo dayPeriodsFormat;
    DayPeriodInfo dayPeriodsSelection;

//    private static final UnicodeSet DIGITS = new UnicodeSet("[0-9]").freeze();
    private static final SupplementalDataInfo supplementalData = CLDRConfig.getInstance().getSupplementalDataInfo();



    private static final boolean FIND_MISSING = CldrUtility.getProperty("FIND_MISSING_ATTRIBUTE_TESTS", false);        // turn on to show <attributeValues> that are missing.


    private VerifyAttributeValues(String fileName, Errors file_element_attribute) {
        this.file_element_attribute = file_element_attribute;
        this.file = fileName.startsWith(BASE_DIR.toString()) ? fileName.substring(BASE_DIR.toString().length()) : fileName;
    }

//    private static final boolean SHOW_UNNECESSARY = false;      // turn on to show <attributeValues> we should delete.

//    private static LinkedHashSet<String> elementOrder = new LinkedHashSet<String>();
//    private static LinkedHashSet<String> attributeOrder = new LinkedHashSet<String>();
//    private static LinkedHashSet<String> serialElements = new LinkedHashSet<String>();
    // static VariableReplacer variableReplacer = new VariableReplacer(); // note: this can be coalesced with the above
    // -- to do later.
//    private static boolean initialized = false;
//    private static LocaleMatcher localeMatcher;

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
                String locale = name.substring(0,name.length()-4);
                cardinalPluralInfo = supplementalData.getPlurals(PluralType.cardinal, locale);
                ordinalPluralInfo = supplementalData.getPlurals(PluralType.ordinal, locale);
                dayPeriodsFormat = supplementalData.getDayPeriods(Type.format, locale);
                dayPeriodsSelection = supplementalData.getDayPeriods(Type.selection, locale);
            }
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
                Status haveTest = AttributeValueValidity.check(dtdData, element, attribute, attributeValue, reason );
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

                // now for plurals

//                if (attribute.equals("count")) {
//                    if (DIGITS.containsAll(attributeValue)) {
//                        // ok, keep going
//                    } else {
//                        final Count countValue = PluralInfo.Count.valueOf(attributeValue);
//                        if (!pluralInfo.getCounts().contains(countValue)
//                            && !isPluralException(countValue, locale)) {
//                            result.add(new CheckStatus()
//                            .setCause(this).setMainType(CheckStatus.errorType).setSubtype(Subtype.illegalPlural)
//                            .setMessage("Illegal plural value {0}; must be one of: {1}",
//                                new Object[] { countValue, pluralInfo.getCounts() }));
//                        }
//                    }
//                }

                // TODO check other variable elements, like dayPeriods
            }
        }
    }

    static final Relation<PluralInfo.Count, String> PLURAL_EXCEPTIONS = Relation.of(
        new EnumMap<PluralInfo.Count, Set<String>>(PluralInfo.Count.class), HashSet.class);

    static {
        PLURAL_EXCEPTIONS.put(PluralInfo.Count.many, "hr");
        PLURAL_EXCEPTIONS.put(PluralInfo.Count.many, "sr");
        PLURAL_EXCEPTIONS.put(PluralInfo.Count.many, "sh");
        PLURAL_EXCEPTIONS.put(PluralInfo.Count.many, "bs");
        PLURAL_EXCEPTIONS.put(PluralInfo.Count.few, "ru");
    }

    static boolean isPluralException(Count countValue, String locale) {
        Set<String> exceptions = PLURAL_EXCEPTIONS.get(countValue);
        if (exceptions == null) {
            return false;
        }
        if (exceptions.contains(locale)) {
            return true;
        }
        int bar = locale.indexOf('_'); // catch bs_Cyrl, etc.
        if (bar > 0) {
            String base = locale.substring(0, bar);
            if (exceptions.contains(base)) {
                return true;
            }
        }
        return false;
    }



    static void findAttributeValues(File file, int max, Errors errors) {
        final String name = file.getName();
        if (file.isDirectory() 
            && !name.equals("specs") 
            && !name.equals("tools")
            && !name.equals("keyboards") // TODO reenable keyboards
            ) {
            int count = max;
            for (File subfile : file.listFiles()) {
                final String subname = subfile.getName();
                if (--count < 0 
                    && !"en.xml".equals(subname)
                    && !"root.xml".equals(subname)
                    ) {
                    continue;
                }
                findAttributeValues(subfile, max, errors);
            }
        } else if (name.endsWith(".xml")){
            System.out.println(file);
            VerifyAttributeValues.check(file.toString(), errors);
        }
    }


    public static void main(String[] args) {
        //checkScripts();
        quickTest();
        Errors errors = new Errors();
        VerifyAttributeValues.findAttributeValues(BASE_DIR, 15, errors);

        System.out.println("\n* READ ERRORS *\n");
        int count = 0;
        for (AttributeValidityInfo item : AttributeValueValidity.getReadFailures()) {
            System.out.println(++count + "\t" + item);
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
            if (item.get4().equals("deprecated"))
                System.out.println(++count 
                    + "; \t" + item.get0()
                    + "; \t" + item.get1()
                    + "; \t" + item.get2()
                    + "; \t" + item.get3()
                    + " — \t" + item.get4()
                    );
        }

        System.out.println("\n* ERRORS *\n");
        count = 0;
        for (R5<String, String, String, String, String> item : errors.file_element_attribute.rows()) {
            if (!item.get4().equals("deprecated"))
                System.out.println(++count 
                    + "; \t" + item.get0()
                    + "; \t" + item.get1()
                    + "; \t" + item.get2()
                    + "; \t" + item.get3()
                    + " — \t" + item.get4()
                    );
        }
    }

    private static void quickTest() {
        for (String test : Arrays.asList(
            "[/common/supplemental/metaZones.xml, usesMetazone, to, 1983-03-31 16:00, illegal]"
//            "[/common/supplemental/metaZones.xml, mapZone, other, Africa_Central, illegal]",
//            "[/common/supplemental/numberingSystems.xml, numberingSystem, digits, 0123456789, illegal]",
//            "[/common/supplemental/ordinals.xml, pluralRules, locales, as bn, illegal]",
//            "[/common/supplemental/pluralRanges.xml, pluralRanges, locales, af bg ca en es et eu fi nb sv ur, illegal]",
//            "[/common/supplemental/supplementalData.xml, calendarPreference, ordering, buddhist gregorian, illegal]",
//            "[/common/supplemental/likelySubtags.xml, likelySubtag, from, abr, illegal]"
            )) {
            quickTest(test);
        }
    }

    private static Status quickTest(String test) {
        test = test.substring(1,test.length()-1);
        String[] parts = test.split(", ");
        Output<String> reason = new Output<>();
        Status value = AttributeValueValidity.check(DtdData.getInstance(DtdType.supplementalData), parts[1], parts[2], parts[3], reason);
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