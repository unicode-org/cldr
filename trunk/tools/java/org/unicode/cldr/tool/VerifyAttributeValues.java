package org.unicode.cldr.tool;

import java.io.File;
import java.io.PrintWriter;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.unicode.cldr.util.AttributeValueValidity;
import org.unicode.cldr.util.AttributeValueValidity.AttributeValueSpec;
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
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XMLFileReader.SimpleHandler;
import org.unicode.cldr.util.XPathParts;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.util.Output;

public class VerifyAttributeValues extends SimpleHandler {
    private static final File BASE_DIR = new File(CLDRPaths.BASE_DIRECTORY);
    private static final SupplementalDataInfo supplementalData = CLDRConfig.getInstance().getSupplementalDataInfo();

    public static final Joiner SPACE_JOINER = Joiner.on(' ');
    public static final Splitter SPACE_SPLITTER = Splitter.on(' ').trimResults().omitEmptyStrings();

    public final static class Errors {

        @SuppressWarnings({ "unchecked", "rawtypes" })
        final ChainedMap.M3<String, AttributeValueSpec, String> file_element_attribute = ChainedMap.of(new TreeMap(), new TreeMap(), String.class);

        public void put(String file, DtdType dtdType, String element, String attribute, String attributeValue, String problem) {
            file_element_attribute.put(file, new AttributeValueSpec(dtdType, element, attribute, attributeValue), problem);
        }

        public Iterable<R3<String, AttributeValueSpec, String>> getRows() {
            return file_element_attribute.rows();
        }
    }

    private DtdData dtdData; // set from first element read
    private final Errors file_element_attribute;
    private final String file;
    private final EnumMap<LocaleSpecific, Set<String>> localeSpecific = new EnumMap<>(LocaleSpecific.class);
    private final Set<AttributeValueSpec> missing;

    private VerifyAttributeValues(String fileName, Errors file_element_attribute, Set<AttributeValueSpec> missing) {
        this.file_element_attribute = file_element_attribute;
        this.file = fileName.startsWith(BASE_DIR.toString()) ? fileName.substring(BASE_DIR.toString().length()) : fileName;
        this.missing = missing;
    }

    /**
     * Check the filename—note that the errors and missing are <b>added to<b>, so clear if you want a fresh start!
     * @param fileName
     * @param errors
     * @param missing
     */
    public static void check(String fileName, Errors errors, Set<AttributeValueSpec> missing) {
        try {
            final VerifyAttributeValues platformHandler = new VerifyAttributeValues(fileName, errors, missing);
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
                String locale = name.substring(name.lastIndexOf('/') + 1, name.lastIndexOf('.'));
                localeSpecific.put(LocaleSpecific.pluralCardinal, supplementalData.getPlurals(PluralType.cardinal, locale).getPluralRules().getKeywords());
                localeSpecific.put(LocaleSpecific.pluralOrdinal, supplementalData.getPlurals(PluralType.ordinal, locale).getPluralRules().getKeywords());
                localeSpecific.put(LocaleSpecific.dayPeriodFormat, getPeriods(Type.format, locale));
                localeSpecific.put(LocaleSpecific.dayPeriodSelection, getPeriods(Type.selection, locale));
            } else {
                localeSpecific.clear();
            }
            AttributeValueValidity.setLocaleSpecifics(localeSpecific);
        }

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
                    file_element_attribute.put(file, dtdData.dtdType, element, attribute, attributeValue, "deprecated");
                    continue;
                }

                Output<String> reason = new Output<>();
                Status haveTest = AttributeValueValidity.check(dtdData, element, attribute, attributeValue, reason);
                switch (haveTest) {
                case ok:
                    break;
                case deprecated:
                case illegal:
                    file_element_attribute.put(file, dtdData.dtdType, element, attribute, attributeValue, reason.value);
                    break;
                case noTest:
                    missing.add(new AttributeValueSpec(dtdData.dtdType, element, attribute, attributeValue));
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
        }
        ;
        result.add("am");
        result.add("pm");
        return new LinkedHashSet<>(result);
    }

    public static int findAttributeValues(File file, int max, Matcher fileMatcher, Errors errors, Set<AttributeValueSpec> allMissing, PrintWriter out) {
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
                    && !"root.xml".equals(subname)) {
                    continue;
                }
                processed += findAttributeValues(subfile, max, fileMatcher, errors, allMissing, out);
            }
            if (out != null) {
                out.println("Processed files: " + processed + " \tin " + file);
                out.flush();
            }
            return processed;
        } else if (name.endsWith(".xml")) {
            if (fileMatcher == null || fileMatcher.reset(name.substring(0, name.length() - 4)).matches()) {
                check(file.toString(), errors, allMissing);
                return 1;
            }
        }
        return 0;
    }

}