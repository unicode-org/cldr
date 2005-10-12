/************************************************************************
* Copyright (c) 2005, Sun Microsystems and others.  All Rights Reserved.
*************************************************************************/

package org.unicode.cldr.ooo;

import org.unicode.cldr.util.LDMLUtilities;
import org.unicode.cldr.icu.LDMLConstants;

/**
 *
 * reads supplementalData.dtd into memory
 */

import java.util.*;
import org.w3c.dom.*;
import java.io.*;

public class supplementalData
{
    // Hashtable of Hashtables (outer key = iso4217 code,  inner key=other attr names, value=other attr value)
    private Hashtable m_fractionsData = new Hashtable();
    
    //Hashtalbe of vectors , key = iso3166 region code, vector[0] = default currency, vector[1] -> vector[n] = alternate currenies
    private Hashtable m_currencuiesInRegion = new Hashtable();
    
    /** Creates a new instance of supplementalData */
    public supplementalData( String filename)
    {
        File file = new File(filename);
        if (file.exists())
        {
            System.err.println ("INFO: reading " + filename + ".....");
            Document doc = LDMLUtilities.parse(filename, true);
            DOMWrapper domWrapper = new DOMWrapper(doc);
         
            //check the version , backwards compatability was broken between 1.2 and 1.3 for curencies in region
            String version = domWrapper.getAttributeFromElement(LDMLConstants.SUPPLEMENTAL_DATA, LDMLConstants.VERSION);
            try
            {
                if (Float.compare (Float.parseFloat(version),  (float)1.3) < 0)
                    System.err.println ("WARNING: Wrong version of supplementalData.xml (" + version + ") , must use 1.3 or later, currency info will be incorrect as a result");
            }
            catch (NumberFormatException e)
            {
                    System.err.println ("WARNING: Unable to read version of supplementalData.xml, currency info may be incorrect");                
            }
            
            //get the fractions
            m_fractionsData = domWrapper.getAttributesFromElement(LDMLConstants.FRACTIONS, LDMLConstants.INFO, LDMLConstants.ISO_4217);
        
            //get the currency codes by region
         //  1.2 m_currencuiesInRegion = domWrapper.getAttribsFrom_El_parent_GP (LDMLConstants.ALTERNATE, LDMLConstants.ISO_4217, 
         //                           LDMLConstants.CURRENCY, LDMLConstants.ISO_4217, 
         //                           LDMLConstants.REGION, LDMLConstants.ISO_3166);
   
            //1.3
           m_currencuiesInRegion = domWrapper.getAttribsFrom_El_parent (
                                    LDMLConstants.CURRENCY, LDMLConstants.ISO_4217, 
                                    LDMLConstants.REGION, LDMLConstants.ISO_3166);
    
           System.err.println ("INFO: finished reading " + filename);
        }
        else
            System.err.println ("WARNING: supplementalData.xml not found");
    }
    
    //gets the decimal places for this currency code, if not found return the default 2 
    public int getDigits (String iso4217Code)
    {
        int digits = 2;
        if (iso4217Code == null)
            return digits;
        
        Hashtable data = (Hashtable) m_fractionsData.get(iso4217Code);
        if (data != null)
        {
            Object obj = data.get (LDMLConstants.DIGITS);
            if (obj != null)
                digits = Integer.parseInt((String)obj);
        }
        return digits;
    }
   
    //get the valid currencies for the region
    //returned Vector[0] = default , Vecotr[1] = legacy
    public Vector getCurrencies (String region)
    {
        if (region == null)
            return null;
        
        Vector currs = null;
        Object obj = m_currencuiesInRegion.get(region);
        if (obj != null)
            currs = (Vector) obj;
        return currs;  
    }
    
    //determine if the specified currency is valid in the the given region
    public boolean isValidCurrency (String region, String currency)
    {
        if ((region == null) | (currency == null))
            return false;
        
        boolean bIsValid = false;
        Vector currs = getCurrencies (region);
        if ((currs != null) && (currs.size()>0))
        {
            if (currs.contains(currency)==true)
            {
                bIsValid = true;
            }
        }
        return bIsValid;
    }
        
        
}
