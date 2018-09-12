package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTool;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Iso639Data;
import org.unicode.cldr.util.Iso639Data.Scope;
import org.unicode.cldr.util.Iso639Data.Source;
import org.unicode.cldr.util.Iso639Data.Type;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.XPathParts.Comments.CommentType;

import com.ibm.icu.impl.Relation;
import com.ibm.icu.text.MessageFormat;

@CLDRTool(alias = "langdata", description = "Generate a list of ISO639 language data. Use '--en' to build en.xml.")
public class GenerateLanguageData {
    // static StandardCodes sc = StandardCodes.make();
    static SupplementalDataInfo supplementalData = SupplementalDataInfo.getInstance(CLDRPaths.SUPPLEMENTAL_DIRECTORY);
    static Iso639Data iso639Data = new Iso639Data();

    public static void main(String[] args) throws IOException {
        MessageFormat nameFmt = new MessageFormat("{0,plural, one{# other name} other{# other names}}");
        Set<String> bcp47languages = StandardCodes.make().getAvailableCodes("language");

        if (args.length > 0 && args[0].equals("--en")) {
            CLDRConfig cldrConfig = CLDRConfig.getInstance();
            XPathParts xpp = new XPathParts(null, null)
                .addElement(LDMLConstants.LDML)
                .addElement(LDMLConstants.LDN)
                .addElement(LDMLConstants.LANGUAGES)
                .addElement(LDMLConstants.LANGUAGE);
            // could add draft status here, ex:
            xpp.setAttribute(-1, LDMLConstants.DRAFT, DraftStatus.unconfirmed.toString());
            System.out.println("generating en.xml.. to " + CLDRPaths.GEN_DIRECTORY);
            SimpleXMLSource xmlSource = new SimpleXMLSource("en");
            CLDRFile newEn = new CLDRFile(xmlSource);
            CLDRFile oldEn = cldrConfig.getEnglish();
            Set<String> all = Iso639Data.getAvailable();
            newEn.addComment("//ldml", "by " +
                GenerateLanguageData.class.getSimpleName() +
                " from Iso639Data v" + iso639Data.getVersion() + " on " + new java.util.Date()
                + " - " + all.size() + " codes.",
                CommentType.PREBLOCK);
            System.out.println(all.size() + " ISO 639 codes to process");
            for (String languageCode : all) {
                xpp.setAttribute(-1, LDMLConstants.TYPE, languageCode);
                String xpath = xpp.toString();

                Set<String> names = Iso639Data.getNames(languageCode);

                newEn.add(xpp.toString(), oldEn.getName(languageCode));

                String oldValue = oldEn.getStringValue(xpath);

                if (oldValue != null &&
                    !oldValue.equals(languageCode)) {
                    newEn.addComment(xpath,
                        "was already in en.xml",
                        CommentType.LINE);
                }
            }
            final String filename = newEn.getLocaleID() + ".xml";
            try (PrintWriter w = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY, filename)) {
                newEn.write(w);
                System.out.println("Wrote to " + CLDRPaths.GEN_DIRECTORY + "/" + filename);
            }
            return;
        }

        // Set<String> languageRegistryCodes = sc.getAvailableCodes("language");
        Set<String> languageCodes = new TreeSet<String>(Iso639Data.getAvailable());

        System.out.println("Macrolanguages");
        for (String languageCode : languageCodes) {
            Set<String> suffixes = Iso639Data.getEncompassedForMacro(languageCode);
            if (suffixes == null) continue;
            // System.out.println(
            // languageCode
            // + "\t"
            // //+ "\t" + iso639Data.getSource(languageCode)
            // + "\t" + (bcp47languages.contains(languageCode) ? "" : "new")
            // //+ "\t" + iso639Data.getScope(languageCode)
            // //+ "\t" + iso639Data.getType(languageCode)
            // + "\t" + Utility.join(iso639Data.getNames(languageCode),"; ")
            // );
            for (String suffix : new TreeSet<String>(suffixes)) {
                System.out.println(
                    languageCode
                        + "\t" + (bcp47languages.contains(languageCode) ? "4646" : "new")
                        + "\t" + Iso639Data.getNames(languageCode).iterator().next() // Utility.join(iso639Data.getNames(languageCode),"; ")
                        + "\t" + suffix
                        // + "\t" + iso639Data.getSource(suffix)
                        + "\t" + (bcp47languages.contains(suffix) ? "4646" : "new")
                        // + "\t" + iso639Data.getScope(suffix)
                        // + "\t" + iso639Data.getType(suffix)
                        + "\t" + Iso639Data.getNames(suffix).iterator().next());
            }
        }
        System.out.println("All");
        // languageCodes.addAll(languageRegistryCodes);
        Relation<String, String> type_codes = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);
        for (String languageCode : languageCodes) {
            Scope scope = Iso639Data.getScope(languageCode);
            Type type = Iso639Data.getType(languageCode);
            Set<String> names = Iso639Data.getNames(languageCode);
            Source source = Iso639Data.getSource(languageCode);
            String prefix = Iso639Data.getMacroForEncompassed(languageCode);
            Set<String> prefixNames = prefix == null ? null : Iso639Data.getNames(prefix);
            String prefixName = prefixNames == null || prefixNames.size() == 0 ? "" : prefixNames.iterator().next()
                + "::\t";
            String fullCode = (prefix != null ? prefix + "-" : "") + languageCode;
            String scopeString = String.valueOf(scope);
            if (Iso639Data.getEncompassedForMacro(languageCode) != null) {
                scopeString += "*";
            }
            System.out.println(
                fullCode
                    + "\t" + source
                    + "\t" + scopeString
                    + "\t" + type
                    + "\t" + prefixName + CldrUtility.join(names, "\t"));
            type_codes.put(source + "\t" + scopeString + "\t" + type, fullCode);
        }
        for (String type : type_codes.keySet()) {
            Set<String> codes = type_codes.getAll(type);
            System.out.println(codes.size() + "\t" + type + "\t" + truncate(codes));
        }
    }

    private static String truncate(Object codes) {
        String result = codes.toString();
        if (result.length() < 100) return result;
        return result.substring(0, 99) + '\u2026';
    }
}
