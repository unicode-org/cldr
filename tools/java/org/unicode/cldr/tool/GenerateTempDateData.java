package org.unicode.cldr.tool;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.dev.test.util.BagFormatter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.ListResourceBundle;
import java.util.Set;

public class GenerateTempDateData {
    /*
<dates>
<calendars>
<calendar type="gregorian">
<dateTimeFormats>
<availableFormats>
<dateFormatItem id="HHmm" draft="provisional">HH:mm</dateFormatItem>
    */
    public static void main(String[] args) throws IOException {
        Factory cldrFactory = CLDRFile.Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
        Set x = cldrFactory.getAvailable();
        XPathParts parts = new XPathParts();
        PrintWriter pw = BagFormatter.openUTF8Writer(CldrUtility.GEN_DIRECTORY + "datedata/" ,"DateData.java");
        pw.println("package com.ibm.icu.impl.data;");
        pw.println("import java.util.ListResourceBundle;");
        pw.println("class DateData { // extracted from CLDR 1.4");
        for (Iterator it = x.iterator(); it.hasNext();) {
            String locale = (String) it.next();
            CLDRFile file = cldrFactory.make(locale, false);
            if (file.isNonInheriting()) continue;
            System.out.println(locale);
            boolean gotOne = false;
            for (Iterator it2 = file.iterator("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/"); it2.hasNext();) {
                String path = (String) it2.next();
                if (path.indexOf("dateTimeFormats/availableFormats/dateFormatItem") >= 0) {
                    gotOne = doHeader(pw, locale, gotOne);
                    String id = parts.set(path).getAttributeValue(-1,"id");
                    String pattern = file.getStringValue(path);
                    pw.println("     {\"pattern/" + id + "\",\"" + com.ibm.icu.impl.Utility.escape(pattern) + "\"},");
                } else if (path.indexOf("dateTimeFormats/appendItems") >= 0) {
                    gotOne = doHeader(pw, locale, gotOne);
                    String request = parts.set(path).getAttributeValue(-1,"request");
                    String pattern = file.getStringValue(path);
                    pw.println("     {\"append/" + request + "\",\"" + com.ibm.icu.impl.Utility.escape(pattern) + "\"},");
                } else if (path.indexOf("fields/field") >= 0) {
                    gotOne = doHeader(pw, locale, gotOne);
                    String type = parts.set(path).getAttributeValue(-2,"type");
                    String pattern = file.getStringValue(path);
                    pw.println("     {\"field/" + type + "\",\"" + com.ibm.icu.impl.Utility.escape(pattern) + "\"},");
                } 
            }
            if (gotOne) {
                pw.println(" };}}");
            }
        }
        pw.println("}");
        pw.close();
   }

    private static boolean doHeader(PrintWriter pw, String locale, boolean gotOne) {
        if (!gotOne) {
            gotOne = true;
            String suffix = locale.equals("root") ? "" : "_" + locale;
            pw.println(" public static class MyDateResources" + suffix + " extends ListResourceBundle {");
            pw.println("  protected Object[][] getContents() {");
            pw.println("   return new Object[][] {");
        }
        return gotOne;
    }
    
    /*
     *  * public class MyResources_fr extends ListResourceBundle {
 *     protected Object[][] getContents() {
 *         return new Object[][] = {
 *         // LOCALIZE THIS
 *             {"s1", "Le disque \"{1}\" {0}."},          // MessageFormat pattern
 *             {"s2", "1"},                               // location of {0} in pattern
 *             {"s3", "Mon disque"},                      // sample disk name
 *             {"s4", "ne contient pas de fichiers"},     // first ChoiceFormat choice
 *             {"s5", "contient un fichier"},             // second ChoiceFormat choice
 *             {"s6", "contient {0,number} fichiers"},    // third ChoiceFormat choice
 *             {"s7", "3 mars 1996"},                     // sample date
 *             {"s8", new Dimension(1,3)}                 // real object, not just string
 *         // END OF MATERIAL TO LOCALIZE
 *         };
 *     }
 * }

     */
    static class RBundle extends ListResourceBundle {
        protected Object[][] getContents() {
            // TODO Auto-generated method stub
            return null;
        }
    }
}