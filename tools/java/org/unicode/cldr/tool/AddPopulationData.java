package org.unicode.cldr.tool;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter2;
import org.unicode.cldr.util.StandardCodes;

import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class AddPopulationData {
    static boolean SHOW_SKIP = CldrUtility.getProperty("SHOW_SKIP", false);
    static boolean SHOW_ALTERNATE_NAMES = CldrUtility.getProperty("SHOW_ALTERNATE_NAMES", false);

    enum WBLine {
        // "Afghanistan","AFG","GNI, PPP (current international $)","NY.GNP.MKTP.PP.CD","..","..","13144920451.3325","16509662130.816","18932631964.8727","22408872945.1924","25820670505.2627","30783369469.7509","32116190092.1429","..",

        Country_Name, Country_Code, Series_Name, Series_Code, 
        YR2000, YR2001, YR2002, YR2003, YR2004, YR2005, YR2006, YR2007, YR2008, YR2009, YR2010;
        String get(String[] pieces) {
            return pieces[ordinal()];
        }
    }

    enum FBLine {
        Rank, Country, Value, Year;
        String get(String[] pieces) {
            return pieces[ordinal()];
        }
    }

    private static final String        GCP                   = "NY.GNP.MKTP.PP.CD";
    private static final String        POP                   = "SP.POP.TOTL";
    private static final String        EMPTY                 = "..";
    private static Counter2<String> worldbank_gdp        = new Counter2<String>();
    private static Counter2<String> worldbank_population = new Counter2<String>();
    private static Counter2<String> un_literacy   = new Counter2<String>();

    private static Counter2<String> factbook_gdp          = new Counter2<String>();
    private static Counter2<String> factbook_population   = new Counter2<String>();
    private static Counter2<String> factbook_literacy   = new Counter2<String>();

    private static CountryData other = new CountryData();


    static class CountryData {
        private static Counter2<String> population   = new Counter2<String>();
        private static Counter2<String> gdp   = new Counter2<String>();
        private static Counter2<String> literacy   = new Counter2<String>();
    }

    public static void main(String[] args) throws IOException {


        System.out.println("Code"
                + "\t" + "Name"
                + "\t" + "Pop"
                + "\t" + "GDP-PPP"
                + "\t" + "UN Literacy"
        );

        for (String country : ULocale.getISOCountries()) {
            showCountryData(country);
        }
        Set<String> outliers = new TreeSet<String>();
        outliers.addAll(factbook_population.keySet());
        outliers.addAll(worldbank_population.keySet());
        outliers.addAll(factbook_gdp.keySet());
        outliers.addAll(worldbank_gdp.keySet());
        outliers.addAll(un_literacy.keySet());
        outliers.removeAll(Arrays.asList(ULocale.getISOCountries()));
        System.out.println("Probable Mistakes");
        for (String country : outliers) {
            showCountryData(country);
        }
        Set<String> altNames = new TreeSet<String>();
        String oldCode = "";
        int alt = 0;
        for (String display : CountryCodeConverter.names()) {
            String code = CountryCodeConverter.getCodeFromName(display);
            String icu = ULocale.getDisplayCountry("und-" + code, "en");
            if (!display.equalsIgnoreCase(icu)) {       
                altNames.add(code + "\t" + display + "\t" + icu);
            }
        }
        oldCode = "";
        if (SHOW_ALTERNATE_NAMES) {
            for (String altName : altNames) {
                String[] pieces = altName.split("\t");
                String code = pieces[0];
                if (code.equals("ZZ")) continue;
                if (!code.equals(oldCode)) {
                    alt = 0;
                    oldCode = code;
                    System.out.println();
                }
                System.out.println(code + "; " + pieces[2] + "; " + pieces[1]);
                //System.out.println("<territory type=\"" + code + "\" alt=\"v" + (++alt) + "\">" + pieces[1] + "</territory> <!-- " + pieces[2] + " -->");
            }
        }
    }

    private static void showCountryData(String country) {
        System.out.println(country
                + "\t" + ULocale.getDisplayCountry("und-" + country, "en")
                + "\t" + getPopulation(country)
                + "\t" + getGdp(country)
                + "\t" + getLiteracy(country)
        );
    }

    public static Double getLiteracy(String country) {
        return firstNonZero(factbook_literacy.getCount(country),
                un_literacy.getCount(country),
                other.literacy.getCount(country));
    }

    public static Double getGdp(String country) {
        return firstNonZero(factbook_gdp.getCount(country),
                worldbank_gdp.getCount(country),
                other.gdp.getCount(country));
    }

    public static Double getPopulation(String country) {
        return firstNonZero(factbook_population.getCount(country),
                worldbank_population.getCount(country),
                other.population.getCount(country));
    }

    private static Double firstNonZero(Double...items) {
        for (Double item : items) {
            if (item.doubleValue() != 0) {
                return item;
            }
        }
        return 0.0;
    }

    static String[] splitCommaSeparated(String line) {
        // items are separated by ','
        // each item is of the form abc...
        // or "..." (required if a comma or quote is contained)
        // " in a field is represented by ""
        List<String> result = new ArrayList<String>();
        StringBuilder item = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); ++i) {
            char ch = line.charAt(i); // don't worry about supplementaries
            switch(ch) {
            case '"': 
                inQuote = !inQuote;
                // at start or end, that's enough
                // if get a quote when we are not in a quote, and not at start, then add it and return to inQuote
                if (inQuote && item.length() != 0) {
                    item.append('"');
                    inQuote = true;
                }
                break;
            case ',':
                if (!inQuote) {
                    result.add(item.toString());
                    item.setLength(0);
                } else {
                    item.append(ch);
                }
                break;
            default:
                item.append(ch);
                break;
            }
        }
        result.add(item.toString());
        return result.toArray(new String[result.size()]);
    }

    private static void loadFactbookInfo(String filename, final Counter2<String> factbookGdp) throws IOException {
        FileUtilities.handleFile(filename, new FileUtilities.LineHandler() {
            public boolean handle(String line) {
                if (line.length() == 0 || line.startsWith("This tab") || line.startsWith("Rank")
                        || line.startsWith(" This file")) {
                    return false;
                }
                String[] pieces = line.split("\t");
                String code = CountryCodeConverter.getCodeFromName(FBLine.Country.get(pieces));
                if (code == null) {
                    return false;
                }
                String valueString = FBLine.Value.get(pieces).trim();
                if (valueString.startsWith("$")) {
                    valueString = valueString.substring(1);
                }
                valueString = valueString.replace(",", "");
                double value = Double.parseDouble(valueString.trim());
                factbookGdp.add(code, value);
                // System.out.println(Arrays.asList(pieces));
                return true;
            }
        });
    }

    static final NumberFormat dollars = NumberFormat.getCurrencyInstance(ULocale.US);
    static final NumberFormat number = NumberFormat.getNumberInstance(ULocale.US);

    static class MyLineHandler implements FileUtilities.LineHandler {
        CountryData countryData;

        public MyLineHandler(CountryData countryData) {
            super();
            this.countryData = countryData;
        }
        public boolean handle(String line) throws ParseException {
            if (line.startsWith("#")) return true;
            if (line.length() == 0) {
                return true;
            }
            String[] pieces = line.split(";");
            final String code = pieces[0].trim();
            if (code.equals("Code")) {
                return false;
            }
            // Code;Name;Type;Data;Source
            final String typeString = pieces[2].trim();
            final String data = pieces[3].trim();
            if (typeString.equals("gdp-ppp")) {
                if (StandardCodes.isCountry(data)) {
                    Double otherPop = getPopulation(data);
                    Double otherGdp = getGdp(data);
                    Double myPop = getPopulation(code);
                    if (myPop.doubleValue() == 0 || otherPop.doubleValue() == 0 || otherGdp.doubleValue() == 0) {
                        otherPop = getPopulation(data);
                        otherGdp = getPopulation(data);
                        myPop = getPopulation(code);
                        throw new IllegalArgumentException("Zero population");
                    }
                    countryData.gdp.add(code, otherGdp * myPop / otherPop);
                } else {
                    countryData.gdp.add(code, dollars.parse(data).doubleValue());
                }
            } else if (typeString.equals("population")) {
                if (StandardCodes.isCountry(data)) {
                    throw new IllegalArgumentException("Population can't use other country's");
                }
                countryData.population.add(code, number.parse(data).doubleValue());
            } else if (typeString.equals("literacy")) {
                if (StandardCodes.isCountry(data)) {
                    Double otherPop = getLiteracy(data);
                    countryData.literacy.add(code, otherPop);
                } else {
                    countryData.literacy.add(code, number.parse(data).doubleValue());
                }
            } else {
                throw new IllegalArgumentException("Illegal type");
            }
            return true;
        }
    }

    static final UnicodeSet DIGITS = (UnicodeSet) new UnicodeSet("[:Nd:]").freeze();

    private static void loadFactbookLiteracy() throws IOException {
        final String filename = "external/factbook_literacy.html";
        FileUtilities.handleFile(filename, new FileUtilities.LineHandler() {
            Matcher m = Pattern.compile("<strong>total population:</strong>\\s*(?:above\\s*)?(?:[0-9]+-)?([0-9]*\\.?[0-9]*)%.*").matcher("");
            //Matcher m = Pattern.compile("<i>total population:</i>\\s*(?:above\\s*)?(?:[0-9]+-)?([0-9]*\\.?[0-9]*)%.*").matcher("");
            //Matcher codeMatcher = Pattern.compile("<a href=\"../geos/[^\\.]+.html\" class=\"CountryLink\">([^<]+)</a>").matcher("");
            Matcher codeMatcher = Pattern.compile(">([^<]+)<").matcher("");
            String code = null;
            public boolean handle(String line) throws ParseException {
                // <i>total population:</i> 43.1% 
                line = line.trim();
                if (line.contains("fl_region")) {
                    if (!codeMatcher.reset(line).find()) {
                        throw new IllegalArgumentException("bad regex match: file changed format");
                    }
                    String trialCode = codeMatcher.group(1);
                    code = CountryCodeConverter.getCodeFromName(trialCode);
                    System.out.println(trialCode + "\t" + code);
                    if (code == null) {
                        throw new IllegalArgumentException("bad country: change countryToCode()\t" + trialCode + "\t" + code);
                    }
                    return true;

                }
                //        if (line.contains("CountryLink")) {
                //          if (!codeMatcher.reset(line).matches()) {
                //            throw new IllegalArgumentException("mismatched line: " + code);
                //          }       
                //          code = countryToCode(codeMatcher.group(1));
                //          if (code == null) {
                //            throw new IllegalArgumentException("bad country");
                //          }
                //          return true;
                //        }
                if (!line.contains("total population:")) {
                    return true;
                }
                if (code == null) {
                    throw new IllegalArgumentException("Bad code: " + code);
                }
                if (!m.reset(line).matches()) {
                    throw new IllegalArgumentException("mismatched line: " + code);
                }
                // <a href="../geos/al.html" class="CountryLink">Albania</a>
                // AX Aland Islands www.aland.ax  26,200  $929,773,254
                final String percentString = m.group(1);
                final double percent = number.parse(percentString).doubleValue();
                if (factbook_literacy.getCount(code) != 0) {
                    System.out.println("Duplicate literacy in FactBook: " + code);
                    return false;
                }
                factbook_literacy.add(code, percent);
                code = null;
                return true;
            }
        });
    }


    private static void loadWorldBankInfo() throws IOException {
        final String filename = "external/world_bank_data.csv";

        //List<List<String>> data = SpreadSheet.convert(CldrUtility.getUTF8Data(filename));

        FileUtilities.handleFile(filename, new FileUtilities.LineHandler() {
            public boolean handle(String line) {
                if (line.contains("Series Code")) {
                    return false;
                }
                String[] pieces = splitCommaSeparated(line);

                //String[] pieces = line.substring(1, line.length() - 2).split("\"\t\"");

                final String seriesCode = WBLine.Series_Code.get(pieces);

                String last = null;
                for (WBLine i : WBLine.values()) {
                    if (i.compareTo(WBLine.YR2000) >= 0) {
                        String current = i.get(pieces);
                        if (current.length() != 0 && !current.equals(EMPTY)) {
                            last = current;
                        }
                    }
                }
                if (last == null) {
                    return false;
                }
                String country = CountryCodeConverter.getCodeFromName(WBLine.Country_Name.get(pieces));
                if (country == null) {
                    return false;
                }
                double value;
                try {
                    value = Double.parseDouble(last);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("File changed format: need to modify code");
                }
                if (seriesCode.equals(GCP)) {
                    worldbank_gdp.add(country, value);
                } else if (seriesCode.equals(POP)) {
                    worldbank_population.add(country, value);
                } else {
                    throw new IllegalArgumentException();
                }
                return true;
            }
        });
    }


    private static void loadUnLiteracy() throws IOException {
        FileUtilities.handleFile("external/un_literacy.csv", new FileUtilities.LineHandler() {
            public boolean handle(String line) {
                // Afghanistan,2000, ,28,43,13,,34,51,18
                String[] pieces = splitCommaSeparated(line);
                if (pieces.length != 10 || !DIGITS.containsAll(pieces[1])) {
                    return false;
                }
                String code = CountryCodeConverter.getCodeFromName(pieces[0]);
                if (code == null) {
                    return false;
                }
                String totalLiteracy = pieces[3];
                if (totalLiteracy.equals("�") || totalLiteracy.isEmpty()) {
                    return true;
                }
                double percent = Double.parseDouble(totalLiteracy);
                un_literacy.add(code, percent);
                return true;
            }
        });
    }

    static {
        try {
            loadFactbookLiteracy();
            loadUnLiteracy();

            loadFactbookInfo("external/factbook_gdp_ppp.txt", factbook_gdp);
            loadFactbookInfo("external/factbook_population.txt", factbook_population);
            FileUtilities.handleFile("external/other_country_data.txt", new MyLineHandler(other));

            loadWorldBankInfo();
            StandardCodes sc = StandardCodes.make();
            StringBuilder myErrors = new StringBuilder();
            for (String territory : sc.getGoodAvailableCodes("territory")) {
                if (!sc.isCountry(territory)) {
                    continue;
                }
                double gdp = getGdp(territory);
                double literacy = getLiteracy(territory);
                double population = getPopulation(territory);
                if (gdp == 0) {
                    // AX;Aland Islands;population;26,200;www.aland.ax
                    myErrors.append("\n" + territory + ";" + sc.getData("territory", territory) + ";gdp-ppp;0;reason");
                }
                if (literacy == 0) {
                    myErrors.append("\n" + territory + ";" + sc.getData("territory", territory) + ";literacy;0;reason");
                }
                if (population == 0) {
                    myErrors.append("\n" + territory + ";" + sc.getData("territory", territory) + ";population;0;reason");
                }
            }
            if (myErrors.length() != 0) {
                throw new IllegalArgumentException("Missing Country values, edit external/other_country_data to fix:" + myErrors);
            }
        } catch (IOException e) {
        }
    }
}
