/************************************************************************
* Copyright (c) 2005, Sun Microsystems and others.  All Rights Reserved.
*************************************************************************/

package org.unicode.cldr.ooo;

import java.io.File;
import java.util.Hashtable;

import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.util.LDMLUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class supplementalMetadata
{
    // Hashtable : key = old iso code code,  value = new iso code
    // i.e. <"YU", "CS">
    private Hashtable m_LegacyTerritories = new Hashtable();
   
    private Document m_doc = null;
   
    /** Creates a new instance of supplementalData */
    public supplementalMetadata( String dir)
    {
        String filename = dir + "/" + "supplementalMetadata.xml";
        File file = new File(filename);
        if (file.exists())
        {
            System.err.println ("INFO: reading " + filename + "   this may take a couple of minutes.....");
            m_doc = LDMLUtilities.parse(filename, true);
            readLegacyTerritories ();   
           System.err.println ("INFO: finished reading " + filename);
        }
        else
           System.err.println ("WARNING: " + filename + " not found");
    }
    
    

    //puts fractions in Hashtable , key = curr code, value = fractions
    // <info iso4217="ADP" digits="0" rounding="0"/>
    // <info iso4217="BHD" digits="3" rounding="0"/>
    private void readLegacyTerritories ()
    {
        String SearchLocation = "//supplementalData/metadata/alias/territoryAlias";
        NodeList nl = LDMLUtilities.getNodeList(m_doc, SearchLocation);
        for (int i=0; i < nl.getLength(); i++)
        {
            String old_terr = LDMLUtilities.getAttributeValue(nl.item(i), LDMLConstants.TYPE);
            String new_terr = LDMLUtilities.getAttributeValue(nl.item(i), LDMLConstants.REPLACEMENT);
    //        System.err.println (old_terr + "  " + new_terr);
            if (old_terr != null && new_terr != null)  //replacement terr is optional
                m_LegacyTerritories.put (old_terr, new_terr);
        }
    }
                 
 
       
    //returns the current code for this territory or jsut the input teritory if = current teritory
    //
    public String getCurrentTerritory (String territory)
    {
        String latest = territory;   //in most cases the input code is the latest one
        if (m_LegacyTerritories.get(territory) != null)
            latest = (String) m_LegacyTerritories.get(territory);
        return latest;

    }
    
}
