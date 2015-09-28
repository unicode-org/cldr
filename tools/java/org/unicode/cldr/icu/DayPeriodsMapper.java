package org.unicode.cldr.icu;

import java.io.File;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Class for converting CLDR dayPeriods data into a format suitable for writing
 * to ICU data. The regex-mapping method can't be used here because the special
 * handling of sets.
 *
 * @author jchye
 */
public class DayPeriodsMapper {
    private String supplementalDir;

    public DayPeriodsMapper(String supplementalDir) {
        this.supplementalDir = supplementalDir;
    }

    /**
     * @return CLDR data converted to an ICU-friendly format
     */
    public IcuData fillFromCldr() {
        IcuData icuData = new IcuData("dayPeriods.xml", "dayPeriods", false);
        DayPeriodsHandler handler = new DayPeriodsHandler(icuData);
        File inputFile = new File(supplementalDir, "dayPeriods.xml");
        MapperUtils.parseFile(inputFile, handler);
        return icuData;
    }

    private class DayPeriodsHandler extends MapperUtils.EmptyHandler {
        private IcuData icuData;
        private int setNum;
        private String selection;

        public DayPeriodsHandler(IcuData icuData) {
            this.icuData = icuData;
            setNum = 0;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attr) throws SAXException {
            //  <dayPeriodRuleSet type="selection">
            if (qName.equals("dayPeriodRuleSet")) {
                selection = attr.getValue("type");
                if (selection == null) {
                    selection = "";
                } else {
                    selection = "_" + selection;
                }
            } else if (qName.equals("dayPeriodRules")) {
                setNum++;
                String[] locales = attr.getValue("locales").split("\\s+");
                for (String locale : locales) {
                    icuData.add("/locales" + selection + "/" + locale, "set" + setNum);
                }
            } else if (qName.equals("dayPeriodRule")) {
                String type = attr.getValue("type");
                String prefix = "/rules/set" + setNum + "/" + type + "/";
                // Possible attribute names: type, before, after, at.
                for (int i = 0; i < attr.getLength(); i++) {
                    String attrName = attr.getLocalName(i);
                    if (!attrName.equals("type")) {
                        icuData.add(prefix + attrName, attr.getValue(i));
                    }
                }
            }
        }
    }
}
