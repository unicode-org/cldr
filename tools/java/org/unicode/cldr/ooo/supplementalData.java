/************************************************************************
* Copyright (c) 2005, Sun Microsystems and others.  All Rights Reserved.
*************************************************************************/

package org.unicode.cldr.ooo;

import org.unicode.cldr.util.LDMLUtilities;
import org.unicode.cldr.icu.LDMLConstants;

/**
 *
 * reads supplementalData.xml into memory
 */

import java.util.*;
import org.w3c.dom.*;
import java.io.*;

public class supplementalData
{
    // Hashtable : key = iso4217 code,  value = fractions
    private Hashtable m_fractionsData = new Hashtable();
    
    //Hashtable of vectors , key = iso3166 region code, vector[0] = default currency, vector[1] - vecotr[n] = deprecated currencies starting with msot recent
    private Hashtable m_currencuiesInRegion = new Hashtable();
        
    //key = number of days, value = string of 2 letter iso country codes i.e. <"1", "GB IE US IT"> territory=001 => all otheers
    private Hashtable m_minDays = new Hashtable();

    //key = day, value = string of 2 letter iso country codes i.e. <"sun", "GB IE US IT">  territory=001 => all otheers
    private Hashtable m_firstDay = new Hashtable();

    //key = metric or US, value = string of 2 letter iso country codes i.e. <"1", "GB IE US IT"> territory=001 => all otheers
    private Hashtable m_measSys = new Hashtable();
    
    private Document m_doc = null;
    
    private supplementalMetadata m_supplementalMetadata = null;
   
    /** Creates a new instance of supplementalData */
    public supplementalData( String dir  /*dir containing supplemeentalData.xml*/)
    {
        String filename = dir + "/" + "supplementalData.xml";
        
        File file = new File(filename);
        if (file.exists())
        {
            System.err.println ("INFO: reading " + filename + "   this may take a couple of minutes.....");
            m_doc = LDMLUtilities.parse(filename, true);
            
            //get the fractions
            readFractions ();
            // for cldr before 1.3 this won't work as currency data was stored differently
            readCurrencies (); 
            readMinDays ();
            readFirstDay ();
            readMeasSys ();        
           System.err.println ("INFO: finished reading " + filename);
        }
        else
            System.err.println ("WARNING: " + filename + " not found");
        
        // we need tohe metadata for legacy territories
        m_supplementalMetadata = new supplementalMetadata (dir);
    }
    
    
    // and puts currency code data in Hashtable of Vectors 
    //      outer key = iso3166 country, vector[0] = current iso4217 code, vector[1]= previous iso4217 code and so on ....
    //      <region iso3166="GR">
    //           <currency iso4217="EUR" from="2001-01-01"/>
    //           <currency iso4217="GRD" from="1954-05-01" to="2002-02-28"/>
    //  would give key = "GR", vector[0] = "EUR", vector[1] = "GRD"
    private void readCurrencies ()
    {
        String SearchLocation = "//supplementalData/currencyData/region";
        NodeList nl = LDMLUtilities.getNodeList(m_doc, SearchLocation);
        for (int i=0; i < nl.getLength(); i++)
        {
            String country = LDMLUtilities.getAttributeValue(nl.item(i), LDMLConstants.ISO_3166);
            //now get all the currencies used in this country
            SearchLocation = "//supplementalData/currencyData/region[@iso3166=\"" + country + "\"]/currency";
            NodeList nl2 = LDMLUtilities.getNodeList(m_doc, SearchLocation);
            Vector currs = new Vector ();
            for (int j=0; j < nl2.getLength(); j++)
            {
                String curr = LDMLUtilities.getAttributeValue(nl2.item(j), LDMLConstants.ISO_4217);
                if (curr.equals("XXX")) continue;  //region has no currency
                String from = LDMLUtilities.getAttributeValue(nl2.item(j), LDMLConstants.FROM);  //YYYY-MM-DD or just YYYY
                String to = LDMLUtilities.getAttributeValue(nl2.item(j), LDMLConstants.TO);    //YYYY-MM-DD
           //     System.err.println (country + "  " + curr + "  " + from + "  " + (to!=null ? to : ""));
                
                //we add the currenct currency and the previous one (if it exists) only, that's all OO.org wants
                //put the currenct curr in vector position 0
                Calendar today = Calendar.getInstance ();
                                
                Calendar cal_from = Calendar.getInstance ();
                String [] ymd = from.split("-");
                String y = ymd[0];
                String m = ymd.length>1 ? ymd[1] : "1";
                String d = ymd.length>2 ? ymd[2] : "1";
                cal_from.set (Integer.parseInt(y), Integer.parseInt(m), Integer.parseInt(d));
                
                Calendar cal_to = null;
                if (to != null)
                {
                    cal_to = Calendar.getInstance ();
                    ymd = to.split("-");
                    y = ymd[0];
                    m = ymd.length>1 ? ymd[1] : "12";
                    d = ymd.length>2 ? ymd[2] : "31";
                    cal_to.set (Integer.parseInt(y), Integer.parseInt(m), Integer.parseInt(d));
                }

                //if to not set and today is after the from date then we have the currenct currency
                if (cal_from.before (today))
                {
                    if ((cal_to==null || cal_to.after (today)) && currs.size()==0)
                    {  //found the current currency
                        if (country.equals("CS") && curr.equals("EUR"))
                            continue;   //CS has double circulation, prefer CSD to EUR as it is most widely used

                        currs.add (0, curr);
                        continue;
                    }
                }

                if (currs.size () >=1)  //have found the default one so now put the most recent deprecated one
                {                     //assumes currencies are in inverse time order in supplemental
                    currs.add (curr);
                }
                
            }
       
      //DEBUG      
      //      System.err.print (country + " : " );
      //      for (int k=0; k < currs.size(); k++)
      //          System.err.print ((String) currs.elementAt(k) + "  ");
      //      System.err.println ("\n");
            m_currencuiesInRegion.put (country, currs);
        }
                    
    }
     
    //puts fractions in Hashtable , key = curr code, value = fractions
    // <info iso4217="ADP" digits="0" rounding="0"/>
    // <info iso4217="BHD" digits="3" rounding="0"/>
    private void readFractions ()
    {
        String SearchLocation = "//supplementalData/currencyData/fractions/info";
        NodeList nl = LDMLUtilities.getNodeList(m_doc, SearchLocation);
        for (int i=0; i < nl.getLength(); i++)
        {
            String currency = LDMLUtilities.getAttributeValue(nl.item(i), LDMLConstants.ISO_4217);
            String digits = LDMLUtilities.getAttributeValue(nl.item(i), LDMLConstants.DIGITS);
     //       System.err.println (currency + "  " + digits);
            m_fractionsData.put (currency, digits);
        }
    }
                 
     //puts minDays in Hashtable , key = num days , value = string of country codes
    // <minDays count="1" territories="001"/>
    // <minDays count="4" territories="AT BE CA CH DE DK FI FR IT LI LT LU MC MT NO SE SK"/>
    private void readMinDays ()
    {
        String SearchLocation = "//supplementalData/weekData/minDays";
        NodeList nl = LDMLUtilities.getNodeList(m_doc, SearchLocation);
        for (int i=0; i < nl.getLength(); i++)
        {
            String count = LDMLUtilities.getAttributeValue(nl.item(i), LDMLConstants.COUNT);
            String terrs = LDMLUtilities.getAttributeValue(nl.item(i), LDMLConstants.TERRITORIES);
      //      System.err.println (count + "  " + terrs);
            
            //there can be multiple entries with the same minDays, store together in Hashtable
            String s = (String) m_minDays.get (count);
            if (s == null)
                m_minDays.put (count, terrs); 
            else
            {
                s = s + " " + terrs; 
                m_minDays.put (count, s);  //overwrite existing entry
            }
        }
   //     System.err.println ("in Hashtable 1 :" + m_minDays.get("1"));
  //      System.err.println ("in Hashtable 4 :" + m_minDays.get("4"));
    }
    
    // puts FirstDay in Hashtable , key = first day , value = string of country codes
    private void readFirstDay ()
    {
        String SearchLocation = "//supplementalData/weekData/firstDay";
        NodeList nl = LDMLUtilities.getNodeList(m_doc, SearchLocation);
        for (int i=0; i < nl.getLength(); i++)
        {
            String first = LDMLUtilities.getAttributeValue(nl.item(i), LDMLConstants.DAY);
            String terrs = LDMLUtilities.getAttributeValue(nl.item(i), LDMLConstants.TERRITORIES);
   //         System.err.println (first + "  " + terrs);
            
            //there can be multiple entries with the same minDays, store together in Hashtable
            String s = (String) m_firstDay.get (first);
            if (s == null)
                m_firstDay.put (first, terrs); 
            else
            {
                s = s + " " + terrs; 
                m_firstDay.put (first, s);  //overwrite existing entry
            }
        }
  //      System.err.println ("in Hashtable sun :" + m_firstDay.get("sun"));
  //      System.err.println ("in Hashtable mon :" + m_firstDay.get("mon"));
  //      System.err.println ("in Hashtable thu :" + m_firstDay.get("thu"));
  //      System.err.println ("in Hashtable fri :" + m_firstDay.get("fri"));
  //      System.err.println ("in Hashtable sat :" + m_firstDay.get("sat"));
    }
    
    
    // puts FirstDay in Hashtable , key = first day , value = string of country codes
    private void readMeasSys ()
    {
        String SearchLocation = "//supplementalData/measurementData/measurementSystem";
        NodeList nl = LDMLUtilities.getNodeList(m_doc, SearchLocation);
        for (int i=0; i < nl.getLength(); i++)
        {
            String type = LDMLUtilities.getAttributeValue(nl.item(i), LDMLConstants.TYPE);
            String terrs = LDMLUtilities.getAttributeValue(nl.item(i), LDMLConstants.TERRITORIES);
    //        System.err.println (type + "  " + terrs);
            
            //there can be multiple entries with the same minDays, store together in Hashtable
            String s = (String) m_measSys.get (type);
            if (s == null)
                m_measSys.put (type, terrs); 
            else
            {
                s = s + " " + terrs; 
                m_measSys.put (type, s);  //overwrite existing entry
            }
        }
   //     System.err.println ("in Hashtable metric :" + m_measSys.get("metric"));
  //      System.err.println ("in Hashtable US :" + m_measSys.get("US"));

    }
    

    
    //gets the decimal places for this currency code, if not found return the default 2 
    public int getDigits (String iso4217Code)
    {
        int digits = 2;
        if (iso4217Code == null)
            return digits;
        
        Object obj = m_fractionsData.get(iso4217Code);
        if (obj !=null)
            digits = Integer.parseInt ((String)obj);
        
        return digits;
    }
   
    //get the valid currencies for the region
    //returned Vector[0] = default , Vector[1] -> Vector[n] = legacy
    public Vector getCurrencies (String region)
    {        
        if (region == null)
            return null;
        
        region = m_supplementalMetadata.getCurrentTerritory (region);
       
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
        
        region = m_supplementalMetadata.getCurrentTerritory (region);

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
        
        
    private String getTerritory (Hashtable table, String region)
    {
        if (table==null)
            return null;
        if (region==null)  //lang only locales will return the ROW value
            region="";
         
        region = m_supplementalMetadata.getCurrentTerritory (region);

        String key =null;
        String row =null;  //rest of world 001
        Set s = table.keySet();
        Object [] keys = s.toArray ();
        for (int i=0; i < keys.length; i++)
        {
            String terrs = (String) table.get (keys[i]);
            if (terrs.indexOf(region) > -1)
                return (String) keys[i];
            
            if (terrs.equals("001"))
                row = (String) keys[i];
        }
        
        //didn't find a match for the territory so return the match for rest of world'
        return row;
    }
    
    public String getMinDays(String region)
    {
        return getTerritory(m_minDays, region);
    }
    
    public String getFirstDay(String region)
    {
        return getTerritory(m_firstDay, region);
    }
    
    public String getMessSys(String region)
    {
        return getTerritory(m_measSys, region);
    }
}
