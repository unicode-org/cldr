/*
 * ModifyCase.java
 *
 * Created on November 29, 2006, 12:53 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.unicode.cldr.tool;

import org.unicode.cldr.util.LDMLUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import com.ibm.icu.lang.UCharacter;
import java.io.*;
import com.ibm.icu.dev.tool.UOption;

/**
 *
 * @author pn153353
 *
 * class will lower case data specified by an xpath and output the modified data only to a destination folder
 * then use CLDRModify  to merge this output with the originasl data, thereby lower casing the CLDR source
 *
 * TODO : handling of multiple xpaths not fully working - where elements have same parents - too amny parent elements get written
 */
public class ModifyCase 
{
    static final int INDENT = 8;
    static BufferedWriter m_out;
    
    static String [] m_locales;   // = {"bg", "cs", "da", "et", "el", "is", "lt", "ro", "sl", "uk"};
    static String [] m_xpaths; // = {"//ldml/localeDisplayNames/languages/language"};
    //       String xpath = "//ldml/localeDisplayNames/languages/language[@type='to']";
    static String m_sourceDir;    // = "/home/pn153353/pakua/CVS_unicode_latest/cldr/common/main";
    static String m_destDir;   // = "/home/pn153353/CLDR/BUGS/casing_1177/src";

 
    
    /** Creates a new instance of ModifyCase */
    public ModifyCase()
    {
    }
    
    	private static final int
                HELP1 = 0,
                HELP2 = 1,
                DESTDIR = 2,
                LOCALES = 3,
                SOURCEDIR = 4,
                XPATHS = 5
                ;
	
	private static final UOption[] options = {
            UOption.HELP_H(),
            UOption.HELP_QUESTION_MARK(),
            UOption.create("dest", 'd', UOption.REQUIRES_ARG),
            UOption.create("locales", 'l', UOption.REQUIRES_ARG),
            UOption.create("source", 's', UOption.REQUIRES_ARG),
            UOption.create("xpaths", 'x', UOption.REQUIRES_ARG),
	};
        
    	public static void main(String[] args)
        {
            UOption.parseArgs(args, options);
            if (processArgs () == false)
                return;
            
           for (int i=0; i < m_locales.length; i++)
            {
                System.err.println ("Locale : " + m_locales[i]);
                String srcfile = m_sourceDir + "/" + m_locales[i] + ".xml";
                String destfile = m_destDir  + "/" + m_locales[i] + ".xml";
                Document doc = LDMLUtilities.parse(srcfile, false);
                if (doc == null)
                    continue;
                try
                {
                    m_out = new BufferedWriter(new FileWriter(destfile));
                    openLDML(m_locales[i], doc);
                    
                    for (int j=0; j < m_xpaths.length; j++)
                    {
                        makeLowerCase(doc, m_xpaths[j]);
                    }
                    closeLDML();
                }
                catch (IOException e)
                {
                }
            }
        }
        
        private static void usage ()
        {
            System.err.println("org.unicode.cldr.tool.ModifyCase allows the casing of the first letter to be changed");
            System.err.println("The output is just the data category which has changed. Run CLDRModify to merge with source");
            System.err.println("-d : specify dest dir (must exist) where resulting modified data is written");
            System.err.println("-l : specify comma separated list of LDML locales to be changed");
            System.err.println("-s : specify src dir of LDML data to be modified");
            System.err.println("-x : specify comma separated list of xpaths to data to be modified");
            System.err.println("Example : ModifyCase -d /dest -s /cldr/comon/main -l bg,en,it,fr -x //ldml/localeDisplayNames/languages/language");      
        }
        
        private static boolean processArgs ()
        {
            if (options[HELP1].doesOccur || options[HELP2].doesOccur)
            {
                usage();
                return false;
            }
            if (options[DESTDIR].value == null || options[LOCALES].value == null ||
                    options[SOURCEDIR].value == null || options[XPATHS].value == null)
            {
                usage();
                return false;
            }

            m_destDir = options[DESTDIR].value;
            m_locales = options[LOCALES].value.split(",");
            m_sourceDir = options[SOURCEDIR].value;
            m_xpaths = options[XPATHS].value.split(",");          
            return true;
        }
        
       
        public static void openLDML (String locale, Document doc)
        {
            try
            {
                m_out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
                m_out.write("<!DOCTYPE ldml SYSTEM \"http://www.unicode.org/cldr/dtd/1.5/ldml.dtd\">\n");
                m_out.write("<ldml>\n");
                indent (INDENT);
                m_out.write("<identity>\n");
                Node n = LDMLUtilities.getNode(doc, "//ldml/identity/version/@number");
                indent (INDENT*2);
                m_out.write("<version number=\"" + LDMLUtilities.getNodeValue(n) + "\"/>\n");
                n = LDMLUtilities.getNode(doc, "//ldml/identity/generation/@date");
                indent (INDENT*2);
                m_out.write("<generation date=\"" +  LDMLUtilities.getNodeValue(n) + "\"/>\n");
                String parts [] = locale.split("_");
                indent (INDENT*2);
                m_out.write("<language type=\"" + parts[0] + "\"/>\n");
                if (parts.length >1)
                {
                    indent (INDENT*2);
                    m_out.write("<territory type=\"" + parts[1] + "\"/>\n");
                }
                indent (INDENT);
                m_out.write("</identity>\n");
            }
            catch (IOException e)
            {
            }   
        }
        
        public static void makeLowerCase (Document doc, String xpath)
        {    
            //parse the xpath to write the LDML
            try
            {
                //remove //ldml prefix and split
                String path = xpath.substring(xpath.indexOf("//ldml")+ 7);
                String parts [] = path.split("/");
                for (int i=0; i < parts.length-1; i++)
                {
                    indent (INDENT* (i+1));
                    if (addCasingAttribute (parts[i]))
                        m_out.write("<" + parts[i] + " casing=\"lowercase-words\">\n");
                    else
                        m_out.write("<" + parts[i] + ">\n");
                }
                
                Node n[] = LDMLUtilities.getNodeListAsArray(doc, xpath);
                if (n == null)  //just changing a single element
                {  //not tested, this may not work !
                    n = new Node[1];
                    n[0] = LDMLUtilities.getNode(doc, xpath);
                }
                
                for (int j=0; j < n.length; j++)
                {
                    if (n[j] != null)
                    {
                        String value = LDMLUtilities.getNodeValue(n[j]);
                        boolean bUpperFound = false;
                        for (int k=1; k < value.length(); k++)  //skip first char
                        {
                            int c = value.codePointAt(k);
                            if (UCharacter.isUUppercase(c))
                            {
                                bUpperFound = true;
                                break;
                            }
                        }
                        if (bUpperFound == true)  //don't convert where  an upper case is found mid sentence
                        {
                            NamedNodeMap map = n[j].getAttributes();
                            Node langnode = map.getNamedItem("type");
                            String lang = langnode.getNodeValue();
                            System.err.println("Skipping conversion of : " + lang + "  " + value);
                        }
                        
                        if (bUpperFound == false)  //don't convert where  an upper case is found mid sentence
                            value = value.toLowerCase();
 
                        indent(INDENT*parts.length);
                        m_out.write("<" + parts[parts.length-1] );
                                
                        NamedNodeMap map = n[j].getAttributes();
                        for (int k=0; k < map.getLength(); k++)
                        {
                            Node node = map.item(k); 
                            m_out.write(" " + node.getNodeName() + "=\"" + node.getNodeValue() + "\"");
                        }
                        m_out.write(">" + value + "</" + parts[parts.length-1] + ">\n");
                        
                    }
                }
                
                for (int i=parts.length-2; i>=0; i--)
                {
                    indent (INDENT* (i+1));
                    m_out.write("</" + parts[i] + ">\n");
                }
            }
            catch (IOException e)
            {
            }
            
            
            //      Factory cldrFactory = Factory.make(sourceDir, ".*");
            //      boolean makeResolved = false;
            //      CLDRFile file = (CLDRFile) cldrFactory.make(locale, makeResolved).cloneAsThawed();
            //      System.err.println ("res = " + file.getStringValue ("//ldml/localeDisplayNames/languages/language[@type=\"en\"]"));
            
        }
  
        public static void closeLDML()
        {
            try
            {
                m_out.write("</ldml>\n");
                m_out.close();
            }
            catch (IOException e)
            {
            }            
            
        }
        
        private static void indent (int n)
        {
            try
            {
                String spaces = "";
                for (int i=0; i < n; i++)
                    spaces += " ";
                m_out.write(spaces);
            }
            catch (IOException e)
            {
            }                
        }
        
        /* checks if the element can have a casing attribute */
        private static boolean addCasingAttribute (String element)
        {
            String [] elements_with_casing_attribute = {
                "languages", "scripts", "territories", "variants",
                "keys", "types", "measurementSystemNames", "monthWidth",
                "dayWidth", "quarterWidth", "long" /*tz*/, "fields", "currency"};
            
            for (int i=0; i < elements_with_casing_attribute.length; i++)
            {
                if (element.compareTo (elements_with_casing_attribute[i]) ==0)
                    return true;
            }
            return false;
        }
}
