/*
 * CompareFlex.java
 *
 * Created on 2005-12-23, 10.40.PD
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package org.unicode.cldr.ooo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

/**
 *
 * @author pn153353
 */
public class CompareFlex
{
    private String m_dir1 = "/users/peter/CLDR/CVS_unicode_latest/cldr/dropbox/flex";
    private String m_dir2 = "/home/pn153353/CLDR/BUGS/flex_date_time/pass_4/ldml2";

    /** Creates a new instance of CompareFlex */
    public CompareFlex()
    {
        
        Vector allFiles = GetTotalFileList ();
        boolean doComparison = true;
        for (int i=0; i < allFiles.size(); i++)
        {
            doComparison = true;
            
            String file = (String) allFiles.elementAt(i);
            File f1 = new File(m_dir1, file); 
            if (!f1.exists())
            {
                try
                {
                    BufferedWriter out = new BufferedWriter(new FileWriter("comparison.log",true));
                    out.write("MISSING LOCALE " + file + " in " + m_dir1 +"\n");
                    out.close();
                }
                catch (IOException e)
                {}
           //     System.err.println ("No " + file + " in " + m_dir1 );
                doComparison = false;
            }
            
            File f2 = new File(m_dir2, file); 
            if (!f2.exists())
            {
                try
                {
                    BufferedWriter out = new BufferedWriter(new FileWriter("comparison.log",true));
                    out.write("MISSING LOCALE " + file + " in " + m_dir2 +"\n");
                    out.close();
                }
                catch (IOException e)
                {}
             //   System.err.println ("No " + file + " in " + m_dir2 );           
                doComparison = false;
            }
            
            if (doComparison == false) continue;
            
            try
            {
                BufferedWriter out = new BufferedWriter(new FileWriter("comparison.log",true));
                out.write("\nComparing : " + file + "\n");
                out.close();
            }
            catch (IOException e)
            {}         
           CompareDateFormatItems (file);
        }
        
    }
   
    //return a vector of files which exist in either dir or in both
    private Vector GetTotalFileList ()
    {
        File inDir1 = new File(m_dir1); 
        String [] fileList1 = inDir1.list();
        
        File inDir2 = new File(m_dir2);  
        String [] fileList2 = inDir2.list(); 
        
        Vector allFiles = new Vector ();
        for (int i=0; i<fileList1.length; i++)
        {
            if (fileList1[i].endsWith(".xml"))
                allFiles.add(fileList1[i]);
        }
        for (int i=0; i<fileList2.length; i++)
        {
            if ( (!allFiles.contains(fileList2[i])) && (fileList2[i].endsWith(".xml")))
            {
                allFiles.add(fileList2[i]);
            }
        }   
        System.err.println (allFiles.size());
        return allFiles;
    }
    
    private void CompareDateFormatItems (String file)
    {
        String locale = file.substring(file.indexOf("."));
        
        LDMLReaderForOO reader1 = new LDMLReaderForOO(m_dir1 + "/" + file); 
        reader1.readDocument (locale, false);
        reader1.readInXML (false);
        
        LDMLReaderForOO reader2 = new LDMLReaderForOO(m_dir2 + "/" + file);
        reader2.readDocument (locale, false);
        reader2.readInXML (false);

        //highlight any mismatches or omissions
        Vector v1 = new Vector ();
        Vector v2 = new Vector ();        
        for (int i=0; i < reader1.m_DateFormatItems.size(); i++)
        {
            String pattern = (String) reader1.m_DateFormatItems.elementAt(i);
            if (reader2.m_DateFormatItems.contains (pattern) == false)
                v2.add(pattern);
            
        /*    if (file.compareTo("de_AT.xml")==0)
            {
                System.err.println(Integer.toString(i) + "  " + pattern);
            }*/
        }
        for (int i=0; i < reader2.m_DateFormatItems.size(); i++)
        {
            String pattern = (String) reader2.m_DateFormatItems.elementAt(i);
            if (reader1.m_DateFormatItems.contains(pattern) == false)
                 v1.add(pattern);
            
            
          /*  if (file.compareTo("de_AT.xml")==0)
            {
                System.err.println(Integer.toString(i) + "  " + pattern);
            }*/
        }
        
        try
        {
            BufferedWriter out = new BufferedWriter(new FileWriter("comparison.log",true));
            out.write("\t"+ m_dir1 + "/"+file+ " is missing :\n");
            out.close();
        }
        catch (IOException e)
        {}
        for (int i=0; i < v1.size();i++)
        {
            try
            {
                BufferedWriter out = new BufferedWriter(new FileWriter("comparison.log",true));
                out.write("\t\t"+(String)v1.elementAt(i)+"\n");
                out.close();
            }
            catch (IOException e)
            {}
        }
        
        try
        {
            BufferedWriter out = new BufferedWriter(new FileWriter("comparison.log",true));
            out.write("\t"+ m_dir2 + "/"+file+ " is missing :\n");
            out.close();
        }
        catch (IOException e)
        {}
        for (int i=0; i < v2.size();i++)
        {
            try
            {
                BufferedWriter out = new BufferedWriter(new FileWriter("comparison.log",true));
                out.write("\t\t"+(String)v2.elementAt(i)+"\n");
                out.close();
            }
            catch (IOException e)
            {}
        }
    }
    
    
    public static void main(String[] args)
    {
        try
        {
            new CompareFlex ();
        }
        catch (Throwable t)
        {
            t.printStackTrace();
            System.err.println("Unknown error: "+t);
        }
    }
}
