/************************************************************************
* Copyright (c) 2005, Sun Microsystems and others.  All Rights Reserved.
*************************************************************************/

package org.unicode.cldr.ooo;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

public class Utilities
{
    //creates the printStream for output
    public static PrintStream setLocaleWriter(String localeStr, String enc, String subDir)
    {
        //enc if specified will be used to compose output filename, so it should normally be null (except for Solaris maybe)
        if ((localeStr == null) || (subDir == null))
            return null;
        
        //create the subdir if it doesn't already exist
        File dir = new File(".", subDir);
        if (dir.exists() == false)
            dir.mkdir();
        
        PrintStream ps = null;
        try
        {
            String fileName = getOutputFilename(localeStr, enc, subDir);
            if ((fileName != null) && (fileName.compareTo("")!=0))
            {
                FileOutputStream fos = new FileOutputStream(new File(fileName));
                ps = new PrintStream(fos, true, "UTF8");
                Logging.Log1("Writing to file : " + fileName);
            }
        }
        catch (FileNotFoundException e )
        {
            System.err.println("Unable to create output file");
        }
        catch (UnsupportedEncodingException e)
        {
            System.err.println("Unsupported encoding");
        }
        return ps;
    }
    
    public static PrintStream setLocaleWriter2(String localeStr, String enc, String fullPath)
    {
        //enc if specified will be used to compose output filename, so it should normally be null (except for Solaris maybe)
        if ((localeStr == null) || (fullPath == null))
            return null;
        
        //create the subdir if it doesn't already exist
        File dir = new File(fullPath);
        if (dir.exists() == false)
            dir.mkdir();
        
        PrintStream ps = null;
        try
        {
            String fileName = getOutputFilename2(localeStr, enc, fullPath);
            if ((fileName != null) && (fileName.compareTo("")!=0))
            {
                FileOutputStream fos = new FileOutputStream(new File(fileName));
                ps = new PrintStream(fos, true, "UTF8");
                Logging.Log1("Writing to file : " + fileName);
            }
        }
        catch (FileNotFoundException e )
        {
            System.err.println("Unable to create output file");
        }
        catch (UnsupportedEncodingException e)
        {
            System.err.println("Unsupported encoding");
        }
        return ps;
    }
    
    
    // generate name of output file from locale name, charmap encoding and subDir name
    public static String getOutputFilename(String localeStr, String enc, String subDir)
    {
        if ((localeStr == null) || (subDir == null))
            return null;
        
        //jdk Locale class gives "iw" for "he", workaround below
        if (localeStr.compareTo("iw")==0)
            localeStr = "he";
        else if (localeStr.compareTo("iw_IL")==0)
            localeStr = "he_IL";
        
        String localeFileName = "";
        if ((enc != null) && (enc.compareTo("")!=0))
            localeFileName = "./" + subDir + "/" + localeStr + "." + enc + ".xml";
        else
            localeFileName = "./" + subDir + "/" + localeStr + ".xml";
        
        return localeFileName;
    }
    
    // generate name of output file from locale name, charmap encoding and fullPath name
    public static String getOutputFilename2(String localeStr, String enc, String fullPath)
    {
        if ((localeStr == null) || (fullPath == null))
            return null;
        
        //jdk Locale class gives "iw" for "he", workaround below
        if (localeStr.compareTo("iw")==0)
            localeStr = "he";
        else if (localeStr.compareTo("iw_IL")==0)
            localeStr = "he_IL";
        
        String localeFileName = "";
        if ((enc != null) && (enc.compareTo("")!=0))
            localeFileName = fullPath + "/" + localeStr + "." + enc + ".xml";
        else
            localeFileName = fullPath + "/" + localeStr + ".xml";
        
        return localeFileName;
    }
    
    //convert Locale object to CLDR locale file name
    public static String stringFromLocale(Locale locale)
    {
        String localeStr = "";
        if (locale == null)
            return localeStr;
        
        localeStr = locale.getLanguage();
        
        //no script on Locale
        
        String territory = locale.getCountry();
        if (territory != "")
        {
            localeStr += "_";
            localeStr += territory;
        }
        
        String variant = locale.getVariant();
        if (variant != "")
        {
            localeStr += "_";
            localeStr += variant;
        }
        return localeStr;
    }
    
    public static Locale localeFromString(final String localeName)
    {
        if (localeName == null) return new Locale("", "", "");
        String language = localeName;
        String country = "";
        String variant = "";
        
        int ndx = language.indexOf('_');
        if (ndx >= 0)
        {
            country = language.substring(ndx+1);
            language = language.substring(0, ndx);
        }
        ndx = country.indexOf('_');
        if (ndx >= 0)
        {
            variant = country.substring(ndx+1);
            country = country.substring(0, ndx);
        }
        return new Locale(language, country, variant);
    }
    
    
    public static Locale localeFromStringWithOptions(final String localeName)
    {
        if (localeName == null) return new Locale("", "", "");
        
        String variant = "";
        String language = "";
        String country = "";
        
        int ndx = localeName.indexOf('@');
        if (ndx > 0)
            language = localeName.substring(0, ndx);
        else
            language = localeName;
        
        ndx = language.indexOf('_');
        if (ndx >= 0)
        {
            country = language.substring(ndx+1);
            language = language.substring(0, ndx);
        }
        ndx = country.indexOf('_');
        if (ndx >= 0)
        {
            variant = country.substring(ndx+1);
            country = country.substring(0, ndx);
        }
        return new Locale(language, country, variant);
    }
    
    public static String extractLocaleFromFilename(String filename)
    {
        int lastSlash = filename.lastIndexOf('/');
        if (lastSlash < 0)
            lastSlash=-1;
        int lastDot = filename.lastIndexOf('.');
        String localeStr = filename.substring(lastSlash +1, lastDot);
        return localeStr;
    }
}
