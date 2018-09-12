package org.unicode.cldr.icu;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.ibm.icu.impl.Row.R2;

/**
 * Class for converting CLDR plurals files to a format suitable for outputting
 * ICU data with. It might be possible for PluralsMapper and LdmlLocaleMapper to
 * share a parent class, but there isn't currently a need for that so they're
 * kept separate for the time being.
 *
 * @author jchye
 */
public class PluralsMapper {
    private int numRuleSets = 0;
    private String supplementalDir;
    private Map<String, Integer> ruleOrder;

    /**
     * Constructor. A SupplementalDataInfo object is used rather than the
     * supplemental directory because the supplemental data parsing is already
     * done for us. The RegexLookup method used by LdmlLocaleMapper wouldn't
     * work well, since there would only be one regex.
     *
     * @param supplementalDataInfo
     */
    public PluralsMapper(String supplementalDir) {
        this.supplementalDir = supplementalDir;
        ruleOrder = new HashMap<String, Integer>();
    }

    /**
     * @return CLDR data converted to an ICU-friendly format
     */
    public IcuData fillFromCldr() {
        IcuData icuData = new IcuData("plurals.xml, ordinals.xml", "plurals", false);
        fillType(PluralType.cardinal, icuData);
        fillType(PluralType.ordinal, icuData);
        return icuData;
    }

    private void fillType(PluralType type, IcuData icuData) {
        PluralsHandler handler = new PluralsHandler(type, icuData);
        String filename = type == PluralType.cardinal ? "plurals.xml" : "ordinals.xml";
        File inputFile = new File(supplementalDir, filename); // handle ordinals too.
        MapperUtils.parseFile(inputFile, handler);
    }

    private class PluralsHandler extends MapperUtils.EmptyHandler {
        private StringBuffer currentText;
        private String currentCount;
        private String[] currentLocales;
        // List of plural counts and corresponding rules.
        private List<R2<String, String>> currentRules;
        private IcuData icuData;
        String prefix;

        public PluralsHandler(PluralType type, IcuData icuData) {
            this.icuData = icuData;
            prefix = type == PluralType.cardinal ? "/locales/" : "/locales_ordinals/";
            currentText = new StringBuffer();
            currentRules = new ArrayList<R2<String, String>>();
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            currentText.append(ch, start, length);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attr) throws SAXException {
            if (qName.equals("pluralRules")) {
                currentLocales = attr.getValue("locales").split("\\s+");
            } else if (qName.equals("pluralRule")) {
                currentCount = attr.getValue("count");
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals("pluralRules")) {
                // add locale path to ICU data
                StringBuffer ruleBuffer = new StringBuffer();
                for (R2<String, String> rule : currentRules) {
                    ruleBuffer.append(rule.toString() + '\n');
                }
                Integer setNum = ruleOrder.get(ruleBuffer.toString());
                // Only add the rules to the ICU file if they aren't a duplicate
                // of an earlier rule set.
                if (setNum == null) {
                    setNum = numRuleSets;
                    ruleOrder.put(ruleBuffer.toString(), setNum);
                    for (R2<String, String> rule : currentRules) {
                        icuData.add("/rules/set" + numRuleSets + '/' + rule.get0(), rule.get1());
                    }
                    numRuleSets++;
                }
                String setName = currentRules.size() == 0 ? "" : "set" + setNum;
                for (String locale : currentLocales) {
                    icuData.add(prefix + locale, setName);
                }
                currentRules.clear();
            } else if (qName.equals("pluralRule")) {
                currentRules.add(new R2<String, String>(currentCount,
                    currentText.toString()));
                currentText.setLength(0);
            }
        }
    }
}
