/*
 *******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Jul 28, 2004
 *
 */
package org.unicode.cldr.util;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.util.XMLFileReader.SimpleHandler;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


/**
 * @author ram
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class LDMLUtilities {

    public static final int XML = 0,
                            TXT = 1;
    private static final boolean DEBUG = false;
    /**
     * Creates a fully resolved locale starting with root and 
     * @param sourceDir
     * @param locale
     * @return
     */
    public static Document getFullyResolvedLDML(String sourceDir, String locale, 
                                                boolean ignoreRoot, boolean ignoreUnavailable, 
                                                boolean ignoreIfNoneAvailable, boolean ignoreDraft){
        return getFullyResolvedLDML( sourceDir,  locale, ignoreRoot,  ignoreUnavailable, ignoreIfNoneAvailable, ignoreDraft, null);
    }
    private static Document getFullyResolvedLDML(String sourceDir, String locale, 
            boolean ignoreRoot, boolean ignoreUnavailable, 
            boolean ignoreIfNoneAvailable, boolean ignoreDraft, HashMap stack){
        Document full =null;
        if(stack != null){
            //For guarding against cicular references
            String key = "SRC:" + sourceDir+File.separator+locale+".xml";
            if(stack.get(key)!=null){
                System.err.println("Found circular aliases! " + key);
                System.exit(-1);
            }
            stack.put(key, "");
        }
        //System.err.println("In getFullyResolvedLDML "+sourceDir + " " + locale);
        try{
            full = parse(sourceDir+File.separator+ "root.xml", ignoreRoot);
           /*
            * Debugging
            *
            Node[] list = getNodeArray(full, LDMLConstants.ALIAS);
            if(list.length>0){
                System.err.println("Aliases not resolved!. list.getLength() returned "+ list.length);
            }*/
          
            if(DEBUG){
                try {
                     java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                             new  java.io.FileOutputStream("./" + File.separator 
                                     + "root_debug.xml"), "UTF-8");
                     LDMLUtilities.printDOMTree(full, new PrintWriter(writer),"http://www.unicode.org/cldr/dtd/1.3/ldml.dtd", null);
                     writer.flush();
                } catch (IOException e) {
                     //throw the exceptionaway .. this is for debugging
                }
            }
        }catch(RuntimeException ex){
            if(!ignoreRoot){
                throw ex;
            }
        }
        int index = locale.indexOf(".xml");
        if(index > -1){
            locale = locale.substring(0,index);
        }
        if(locale.equals("root")){
            full = resolveAliases(full, sourceDir, locale, ignoreDraft, stack);
            return full;
        }
        String[] constituents = locale.split("_");
        String loc=null;
        boolean isAvailable = false;
        //String lastLoc = "root";
        for(int i=0; i<constituents.length; i++){
            if(loc==null){
                loc = constituents[i];
            }else{
                loc = loc +"_"+ constituents[i];
            }
            Document doc = null;
            
            // Try cache
            //doc = readMergeCache(sourceDir, lastLoc, loc);
            //if(doc == null) { .. 
            String fileName = sourceDir+File.separator+loc+".xml";
            File file = new File(fileName);
            if(file.exists()){
                isAvailable = true;
                doc = parseAndResolveAlias(fileName, loc, ignoreUnavailable);

                /*
                 * Debugging
                 *
                Node[] list = getNodeArray(doc, LDMLConstants.ALIAS);
                if(list.length>0){
                    System.err.println("Aliases not resolved!. list.getLength() returned "+ list.length);
                }
                */
                if(full==null){
                    full = doc;
                }else{
                    StringBuffer xpath = new StringBuffer();
                    mergeLDMLDocuments(full, doc, xpath, loc, sourceDir, ignoreDraft, false);
                    if(DEBUG){
                        try {
                             java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                                     new  java.io.FileOutputStream("./" + File.separator + loc
                                             + "_debug.xml"), "UTF-8");
                             LDMLUtilities.printDOMTree(full, new PrintWriter(writer),"http://www.unicode.org/cldr/dtd/1.3/ldml.dtd", null);
                             writer.flush();
                        } catch (IOException e) {
                             //throw the exceptionaway .. this is for debugging
                        }
                    }
                }
                /*
                 * debugging
                 *
                Node ec = getNode(full, "//ldml/characters/exemplarCharacters");
                if(ec==null){
                    System.err.println("Could not find exemplarCharacters");
                }else{
                    System.out.println("The chars are: "+ getNodeValue(ec));
                }   
                */

                //writeMergeCache(sourceDir, lastLoc, loc, full);
                //lastLoc = loc;
            }else{
                if(!ignoreUnavailable){
                    throw new RuntimeException("Could not find: " +fileName);
                }
            }
            // TODO: investigate if we really need to revalidate the DOM tree!
            // full = revalidate(full, locale);
        }
        
        if(ignoreIfNoneAvailable==true && isAvailable==false){
            return null ;
        }
        
        if(DEBUG){
            try {
                 java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                         new  java.io.FileOutputStream("./" + File.separator + locale
                                 + "_ba_debug.xml"), "UTF-8");
                 LDMLUtilities.printDOMTree(full, new PrintWriter(writer),"http://www.unicode.org/cldr/dtd/1.3/ldml.dtd", null);
                 writer.flush();
            } catch (IOException e) {
                 //throw the exceptionaway .. this is for debugging
            }
        }
        // get the real locale name
        locale = getLocaleName(full);
       // Resolve the aliases once the data is built
        full = resolveAliases(full, sourceDir, locale, ignoreDraft, stack);

       if(DEBUG){
           try {
                java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                        new  java.io.FileOutputStream("./" + File.separator + locale
                                + "_aa_debug.xml"), "UTF-8");
                LDMLUtilities.printDOMTree(full, new PrintWriter(writer),"http://www.unicode.org/cldr/dtd/1.3/ldml.dtd", null);
                writer.flush();
           } catch (IOException e) {
                //throw the exceptionaway .. this is for debugging
           }
       }
       return full;
    }
    public static String getLocaleName(Document doc){

        Node ln = LDMLUtilities.getNode(doc, "//ldml/identity/language");
        Node tn = LDMLUtilities.getNode(doc, "//ldml/identity/territory");
        Node sn = LDMLUtilities.getNode(doc, "//ldml/identity/script");
        Node vn = LDMLUtilities.getNode(doc, "//ldml/identity/variant");
        
        StringBuffer locName = new StringBuffer(); 
        String lang = LDMLUtilities.getAttributeValue(ln, LDMLConstants.TYPE);
        if(lang!=null){ 
            locName.append(lang);
        }else{
            throw new IllegalArgumentException("Did not get any value for language node from identity.");
        }
        if(sn!=null){
            String script = LDMLUtilities.getAttributeValue(sn, LDMLConstants.TYPE);
            if(script!=null){
                locName.append("_");
                locName.append(script);
            }
        }
        if(tn!=null){
            String terr = LDMLUtilities.getAttributeValue(tn, LDMLConstants.TYPE);
            if(terr!=null){
                locName.append("_");
                locName.append(terr);
            }
        }
        if(vn!=null){
            String variant = LDMLUtilities.getAttributeValue(vn, LDMLConstants.TYPE);
            if(variant!=null && tn != null){
                locName.append("_");
                locName.append(variant);
            }
        }
        return locName.toString();
    }
// revalidate wasn't called anywhere.  
// TODO: if needed, reimplement using DOM level 3 
/* 
    public static Document revalidate(Document doc, String fileName){
        // what a waste!!
        // to revalidate an in-memory DOM tree we need to first
        // serialize it to byte array and read it back again.
        // in DOM level 3 implementation there is API to validate
        // in-memory DOM trees but the latest implementation of Xerces
        // can only validate against schemas not DTDs!!!
        try{
            // revalidate the document
            Serializer serializer = SerializerFactory.getSerializer(OutputProperties.getDefaultMethodProperties("xml"));
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            serializer.setOutputStream(os);
            DOMSerializer ds = serializer.asDOMSerializer();
            //ds.serialize(doc);
            os.flush();
            ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
            doc = parse(new InputSource(is),"Fully resolved: "+fileName, false);
            return doc;
        }catch(IOException ex){
            throw new RuntimeException(ex.getMessage());
        }
    }
*/
    @Deprecated
    public static String convertXPath2ICU(Node alias, Node namespaceNode, StringBuffer fullPath)
      throws TransformerException {
      StringBuilder sb = new StringBuilder(fullPath.toString());
      return convertXPath2ICU(alias, namespaceNode, sb);
    }
    
    public static String convertXPath2ICU(Node alias, Node namespaceNode, StringBuilder fullPath)
        throws TransformerException{
        Node context = alias.getParentNode();
        StringBuffer icu = new StringBuffer();
        String source = getAttributeValue(alias, LDMLConstants.SOURCE);
        String xpath = getAttributeValue(alias, LDMLConstants.PATH);
        
        // make sure that the xpaths are valid
        if(namespaceNode==null){
            XPathAPI_eval(context, fullPath.toString());
            if(xpath!=null){
                XPathAPI_eval(context,xpath);
            }
        }else{
            XPathAPI_eval(context, fullPath.toString(), namespaceNode);
            if(xpath!=null){
                XPathAPI_eval(context, xpath, namespaceNode);
            }
        }
        if(source.equals(LDMLConstants.LOCALE)){
            icu.append("/");
            icu.append(source.toUpperCase());
        }else{
            icu.append(source);
        }
        if(xpath!=null){
            StringBuilder resolved = XPathTokenizer.relativeToAbsolute(xpath, fullPath);
            // make sure that fullPath is not corrupted!
            XPathAPI_eval(context, fullPath.toString());
            
            //TODO .. do the conversion
            XPathTokenizer tokenizer = new XPathTokenizer(resolved.toString());
            
            String token = tokenizer.nextToken();
            while(token!=null){
                if(!token.equals("ldml")){
                    String equiv = getICUEquivalent(token);
                    if(equiv==null){
                        throw new IllegalArgumentException("Could not find ICU equivalent for token: " +token);
                    }
                    if(equiv.length()>0){
                        icu.append("/");
                        icu.append(equiv);
                    }
                }
                token = tokenizer.nextToken();
            }
        }
        return icu.toString();
    }
 
    public static String convertXPath2ICU(String source, String xpath, String basePath, String fullPath)
    throws TransformerException{
       //Node context = alias.getParentNode();
        StringBuffer icu = new StringBuffer();

        // TODO: make sure that the xpaths are valid. How?
        
        if(source.equals(LDMLConstants.LOCALE)){
            icu.append("/");
            icu.append(source.toUpperCase());
        }else{
            icu.append(source);
        }
        
        if(xpath!=null){
            StringBuffer fullPathBuffer = new StringBuffer(fullPath);
            StringBuffer resolved = XPathTokenizer.relativeToAbsolute(xpath, fullPathBuffer);
            // TODO: make sure that fullPath is not corrupted!  How?         
            //XPathAPI.eval(context, fullPath.toString());

            //TODO .. do the conversion
            XPathTokenizer tokenizer = new XPathTokenizer(resolved);

            String token = tokenizer.nextToken();
            while(token!=null){
                if(!token.equals("ldml")){
                    String equiv = getICUEquivalent(token);
                    if(equiv==null){
                        throw new IllegalArgumentException("Could not find ICU equivalent for token: " +token);
                    }
                    if(equiv.length()>0){
                        icu.append("/");
                        icu.append(equiv);
                    }
                }
                token = tokenizer.nextToken();
            }
        }
        return icu.toString();
    }


    public static String getDayIndexAsString(String type){
        if(type.equals("sun")){
            return "0";
        }else if(type.equals("mon")){
            return "1";
        }else if(type.equals("tue")){
            return "2";
        }else if(type.equals("wed")){
            return "3";
        }else if(type.equals("thu")){
            return "4";
        }else if(type.equals("fri")){
            return "5";
        }else if(type.equals("sat")){
            return "6";
        }else{
            throw new IllegalArgumentException("Unknown type: "+type);
        }
    }
    public static String getMonthIndexAsString(String type){
        return Integer.toString(Integer.parseInt(type)-1);
    }
    private static String getICUEquivalent(String token){
        int index = 0;
        if(token.indexOf(LDMLConstants.LDN) > -1){
            return "";
        }else if(token.indexOf(LDMLConstants.LANGUAGES) > -1){
            return "Languages";
        }else if(token.indexOf(LDMLConstants.LANGUAGE) > -1){
            return getAttributeValue(token, LDMLConstants.TYPE);
        }else if(token.indexOf(LDMLConstants.TERRITORIES) > -1){
            return "Countries";
        }else if(token.indexOf(LDMLConstants.TERRITORY) > -1){
            return getAttributeValue(token, LDMLConstants.TYPE);
        }else if(token.indexOf(LDMLConstants.SCRIPTS) > -1){
            return "Scripts";
        }else if(token.indexOf(LDMLConstants.SCRIPT) > -1){
            return getAttributeValue(token, LDMLConstants.TYPE);
        }else if(token.indexOf(LDMLConstants.VARIANTS) > -1){
            return "Variants";
        }else if(token.indexOf(LDMLConstants.VARIANT) > -1){
            return getAttributeValue(token, LDMLConstants.TYPE);
        }else if(token.indexOf(LDMLConstants.KEYS) > -1){
            return "Keys";
        }else if(token.indexOf(LDMLConstants.KEY) > -1){
            return getAttributeValue(token, LDMLConstants.TYPE);
        }else if(token.indexOf(LDMLConstants.TYPES) > -1){
            return "Types";
        }else if((index=token.indexOf(LDMLConstants.TYPE)) > -1 && token.charAt(index-1)!='@'){
            String type = getAttributeValue(token, LDMLConstants.TYPE);
            String key = getAttributeValue(token, LDMLConstants.KEY);
            return type+"/"+key;
        }else if(token.indexOf(LDMLConstants.LAYOUT) > -1){
            return "Layout";
        }else if(token.indexOf(LDMLConstants.ORIENTATION) > -1){
            //TODO fix this
        }else if(token.indexOf(LDMLConstants.CHARACTERS) > -1){
            return "";
        }else if(token.indexOf(LDMLConstants.EXEMPLAR_CHARACTERS) > -1){
            return "ExemplarCharacters";
        }else if(token.indexOf(LDMLConstants.MEASUREMENT) > -1){
            return "";
        }else if(token.indexOf(LDMLConstants.MS) > -1){
            return "MeasurementSystem";
        }else if(token.indexOf(LDMLConstants.PAPER_SIZE) > -1){
            return "PaperSize";
        }else if(token.indexOf(LDMLConstants.HEIGHT) > -1){
            return "0";
        }else if(token.indexOf(LDMLConstants.WIDTH) > -1){
            return "1";
        }else if(token.indexOf(LDMLConstants.DATES) > -1){
            return "";
        }else if(token.indexOf(LDMLConstants.LPC) > -1){
            return "localPatternCharacters";
        }else if(token.indexOf(LDMLConstants.CALENDARS) > -1){
            return "calendar";
        }else if(token.indexOf(LDMLConstants.DEFAULT) > -1){
            return "default";
        }else if(token.indexOf(LDMLConstants.CALENDAR) > -1){
            return getAttributeValue(token, LDMLConstants.TYPE);
        }else if(token.indexOf(LDMLConstants.ERAS) > -1){
            return "eras";
        }else if(token.indexOf(LDMLConstants.ERAABBR) > -1){
            return "abbreviated";
        }else if(token.indexOf(LDMLConstants.ERA) > -1){
            return getAttributeValue(token, LDMLConstants.TYPE);
        }else if(token.indexOf(LDMLConstants.NUMBERS) > -1){
            // TODO fix this
        }else if(token.indexOf(LDMLConstants.SYMBOLS) > -1){
            return "NumberElements";
        }else if(token.indexOf(LDMLConstants.DATE_FORMATS) > -1){
            // TODO fix this
        }else if(token.indexOf(LDMLConstants.DFL) > -1){
            // TODO fix this
        }else if(token.indexOf(LDMLConstants.DATE_FORMAT) > -1){
            // TODO fix this
        }else if(token.indexOf(LDMLConstants.TIME_FORMATS) > -1){
            // TODO fix this
        }else if(token.indexOf(LDMLConstants.TFL) > -1){
            // TODO fix this
        }else if(token.indexOf(LDMLConstants.TIME_FORMAT) > -1){
            // TODO fix this
        }else if(token.indexOf(LDMLConstants.DATE_TIME_FORMATS) > -1){
            // TODO fix this
            return "DateTimePatterns";
        }else if(token.indexOf(LDMLConstants.INTVL_FMTS) > -1){
            return "intervalFormats";
            // TODO fix this
        }else if(token.indexOf(LDMLConstants.DTFL) > -1){
            // TODO fix this
        }else if(token.indexOf(LDMLConstants.DATE_TIME_FORMAT) > -1){
            // TODO fix this
        }else if(token.indexOf(LDMLConstants.INTVL_FMTS) > -1){
            // TODO fix this
        }else if(token.indexOf(LDMLConstants.MONTHS) > -1){
            return "monthNames";
        }else if(token.indexOf(LDMLConstants.MONTH_CONTEXT) > -1){
            return getAttributeValue(token, LDMLConstants.TYPE);
        }else if(token.indexOf(LDMLConstants.MONTH_WIDTH) > -1){
            return getAttributeValue(token, LDMLConstants.TYPE);
        }else if(token.indexOf(LDMLConstants.MONTH) > -1){
            String valStr = getAttributeValue(token, LDMLConstants.TYPE);
            return getMonthIndexAsString(valStr);
        }else if(token.indexOf(LDMLConstants.DAYPERIODS) > -1){
            return "dayPeriods";
        }else if(token.indexOf(LDMLConstants.DAYS) > -1){
            return "dayNames";
        }else if(token.indexOf(LDMLConstants.DAY_CONTEXT) > -1){
            return getAttributeValue(token, LDMLConstants.TYPE);
        }else if(token.indexOf(LDMLConstants.DAY_WIDTH) > -1){
            return getAttributeValue(token, LDMLConstants.TYPE);
        }else if(token.indexOf(LDMLConstants.DAY) > -1){
            String dayName = getAttributeValue(token, LDMLConstants.TYPE);
            return getDayIndexAsString(dayName);   
        }else if(token.indexOf(LDMLConstants.QUARTER_WIDTH) > -1){
            return getAttributeValue(token, LDMLConstants.TYPE);
        }else if(token.indexOf(LDMLConstants.QUARTER_CONTEXT) > -1){
            return getAttributeValue(token, LDMLConstants.TYPE);
        }else if(token.indexOf(LDMLConstants.QUARTERS) > -1){
            return "quarters";
        }else if(token.indexOf(LDMLConstants.QUARTER) > -1){
            String valStr = getAttributeValue(token, LDMLConstants.TYPE);
            return getMonthIndexAsString(valStr);
        }else if(token.indexOf(LDMLConstants.COLLATIONS) > -1){
            return "collations";
        }else if(token.indexOf(LDMLConstants.COLLATION) > -1){
            return getAttributeValue(token, LDMLConstants.TYPE);
        }
        
        // TODO: this method is not finished yet 
        // the conversion of Xpath to ICU alias path
        // is not as straight forward as I thought
        // need to cater to idiosynchracies of each
        // element node :(
        throw new IllegalArgumentException("Unknown Xpath fragment: " + token);
    }
    /**
     * 
     * @param token XPath token fragment
     * @param attrib attribute whose value must be fetched
     * @return
     */
    private static String getAttributeValue(String token, String attrib){
        int attribStart = token.indexOf(attrib);
        int valStart = token.indexOf('=', attribStart)+1/*skip past the separtor*/;
        int valEnd = token.indexOf('@', valStart);
        if(valEnd <0){
            valEnd = valStart + (token.length()-valStart-1);
        }else{
            valEnd = token.length() - 1 /*valEnd should be index*/;
        }
        String value = token.substring(valStart, valEnd);
        int s = value.indexOf('\'');
        if(s>-1){
            s++;
            int e = value.lastIndexOf('\'');
            return value.substring(s,e);
        } else {
            // also handle ""
           s = value.indexOf('"');
           if(s>-1) {
               s++;
               int e = value.lastIndexOf('"');
               return value.substring(s,e);
           }
        }
        return value;
    }
    
    @Deprecated
    public static Node mergeLDMLDocuments(Document source, Node override, StringBuffer xpath, 
        String thisName, String sourceDir, boolean ignoreDraft,
        boolean ignoreVersion){
      StringBuilder sb = new StringBuilder(xpath.toString());
      return mergeLDMLDocuments(source, override, sb, thisName, sourceDir, ignoreDraft, ignoreVersion);
    }
    
    /**
     *   Resolved Data File
     *   <p>To produce fully resolved locale data file from CLDR for a locale ID L, you start with root, and 
     *   replace/add items from the child locales until you get down to L. More formally, this can be 
     *   expressed as the following procedure.</p>
     *   <ol>
     *     <li>Let Result be an empty LDML file.</li>
     *   
     *     <li>For each Li in the locale chain for L<ol>
     *       <li>For each element pair P in the LDML file for Li:<ol>
     *         <li>If Result has an element pair Q with an equivalent element chain, remove Q.</li>
     *         <li>Add P to Result.</li>
     *       </ol>
     *       </li>
     *     </ol>
     *   
     *     </li>
     *   </ol>
     *   <p>Note: when adding an element pair to a result, it has to go in the right order for it to be valid 
     *  according to the DTD.</p>
     *
     * @param source
     * @param override
     * @return the merged document
     */
    public static Node mergeLDMLDocuments(Document source, Node override, StringBuilder xpath, 
                                          String thisName, String sourceDir, boolean ignoreDraft,
                                          boolean ignoreVersion){
        if(source==null){
            return override;
        }
        if(xpath.length()==0){
            xpath.append("/");
        }
        
//        boolean gotcha = false;
//        String oldx = new String(xpath);
//        if(override.getNodeName().equals("week")) {
//            gotcha = true;
//            System.out.println("SRC: " + getNode(source, xpath.toString()).toString());
//            System.out.println("OVR: " + override.toString());
//        }
        
        // we know that every child xml file either adds or 
        // overrides the elements in parent
        // so we traverse the child, at every node check if 
        // if the node is present in the source,  
        // if (present)
        //    recurse to replace any nodes that need to be overridded
        // else
        //    import the node into source
        Node child = override.getFirstChild();
        while( child!=null){
            // we are only concerned with element nodes
            if(child.getNodeType()!=Node.ELEMENT_NODE){
                child = child.getNextSibling();
                continue;
            }   
            String childName = child.getNodeName();

            int savedLength=xpath.length();
            xpath.append("/");
            xpath.append(childName);
            appendXPathAttribute(child,xpath, false, false);
            Node nodeInSource = null;
            
            if(childName.indexOf(":")>-1){ 
                nodeInSource = getNode(source, xpath.toString(), child);
            }else{
                nodeInSource =  getNode(source, xpath.toString());
            }
            
            Node parentNodeInSource = null;
            if(nodeInSource==null){
                // the child xml has a new node
                // that should be added to parent
                String parentXpath = xpath.substring(0, savedLength);

                if(childName.indexOf(":")>-1){ 
                    parentNodeInSource = getNode(source, parentXpath, child);
                }else{
                    parentNodeInSource =  getNode(source,parentXpath);
                }
                if(parentNodeInSource==null){
                    throw new RuntimeException("Internal Error");
                }
                
                Node childToImport = source.importNode(child,true);
                parentNodeInSource.appendChild(childToImport);
            }else if( childName.equals(LDMLConstants.IDENTITY)){
                if(!ignoreVersion){
                    // replace the source doc
                    // none of the elements under collations are inherited
                    // only the node as a whole!!
                    parentNodeInSource = nodeInSource.getParentNode();
                    Node childToImport = source.importNode(child,true);
                    parentNodeInSource.replaceChild(childToImport, nodeInSource);
                }
            }else if( childName.equals(LDMLConstants.COLLATION)){
                // replace the source doc
                // none of the elements under collations are inherited
                // only the node as a whole!!
                parentNodeInSource = nodeInSource.getParentNode();
                Node childToImport = source.importNode(child,true);
                parentNodeInSource.replaceChild(childToImport, nodeInSource);
                //override the validSubLocales attribute
                String val = LDMLUtilities.getAttributeValue(child.getParentNode(), LDMLConstants.VALID_SUBLOCALE);
                NamedNodeMap map = parentNodeInSource.getAttributes();
                Node vs = map.getNamedItem(LDMLConstants.VALID_SUBLOCALE);
                vs.setNodeValue(val);
            }else{
                boolean childElementNodes = areChildrenElementNodes(child);
                boolean sourceElementNodes = areChildrenElementNodes(nodeInSource);
//                System.out.println(childName + ":" + childElementNodes + "/" + sourceElementNodes);
                if(childElementNodes &&  sourceElementNodes){
                    //recurse to pickup any children!
                    mergeLDMLDocuments(source, child, xpath, thisName, sourceDir, ignoreDraft, ignoreVersion);
                }else{
                    // we have reached a leaf node now get the 
                    // replace to the source doc 
                    parentNodeInSource = nodeInSource.getParentNode();
                    Node childToImport = source.importNode(child,true);
                    parentNodeInSource.replaceChild(childToImport, nodeInSource);
                }
            }
            xpath.delete(savedLength,xpath.length());
            child= child.getNextSibling();
        }
//        if(gotcha==true) {
//            System.out.println("Final: " + getNode(source, oldx).toString());
//        }
        return source;
    }
    private static Node[] getNodeArray(Document doc, String tagName){
        NodeList list =  doc.getElementsByTagName(tagName);
        // node list is dynamic .. if a node is deleted, then 
        // list is immidiately updated.
        // so first cache the nodes returned and do stuff 
        Node[] array = new Node[list.getLength()];
        for(int i=0; i<list.getLength(); i++){
            array[i] = list.item(i);
        }
        return array;
    }
    /**
     * Utility to create abosolute Xpath from 1.1 style alias element
     * @param node
     * @param type
     * @return
     */
    public static String getAbsoluteXPath(Node node, String type){
        StringBuffer xpath = new StringBuffer();
        StringBuffer xpathFragment = new StringBuffer();
        node = node.getParentNode(); // the node is alias node .. get its parent
        if(node==null){
            throw new IllegalArgumentException("Alias node's parent is null!");
        }
        xpath.append(node.getNodeName());
        if(type!=null){
            xpath.append("[@type='"+type+"']");  // TODO: double quotes?
        }
        Node parent = node;
        while((parent = parent.getParentNode())!=null){
            xpathFragment.setLength(0);
            xpathFragment.append(parent.getNodeName());
            if(parent.getNodeType()!= Node.DOCUMENT_NODE){
                appendXPathAttribute(parent, xpathFragment);
                xpath.insert(0,"/");
                xpath.insert(0, xpathFragment);
            }
        }
        xpath.insert(0, "//");
        return xpath.toString();
    }
    /**
     * 
     * @param n1
     * @param n2 preferred list
     * @param xpath
     * @return
     */
    private static Node[]  mergeNodeLists(Object[] n1, Object[] n2){
        StringBuffer xp1 = new StringBuffer();
        StringBuffer xp2 = new StringBuffer();
        int l1=xp1.length(), l2=xp2.length();
        HashMap map = new HashMap();
        if(n2==null|| n2.length==0){
            Node[] na = new Node[n1.length];
            for(int i=0;i<n1.length;i++){
                na[i]=(Node)n1[i];
            }
            return na;
        }
        for(int i=0; i<n1.length; i++){
            xp1.append(((Node)n1[i]).getNodeName());
            appendXPathAttribute((Node)n1[i], xp1);
            map.put(xp1.toString(), n1[i]);
            xp1.setLength(l1);
        }
        for(int i=0; i<n2.length; i++){
            xp2.append(((Node)n2[i]).getNodeName());
            appendXPathAttribute((Node)n2[i], xp2);
            map.put(xp2.toString(), n2[i]);
            xp2.setLength(l2);
        }
        Object[] arr =  map.values().toArray();
        Node[] na = new Node[arr.length];
        for(int i=0;i<arr.length;i++){
            na[i]=(Node)arr[i];
        }
        return na;
    }
    /**
     * 
     * @param fullyResolvedDoc
     * @param sourceDir
     * @param thisLocale
     */
    // TODO guard against circular aliases
    public static Document resolveAliases(Document fullyResolvedDoc, String sourceDir, String thisLocale,boolean ignoreDraft, HashMap stack){
       Node[] array = getNodeArray(fullyResolvedDoc, LDMLConstants.ALIAS);
       
       // resolve all the aliases by iterating over
       // the list of nodes
       Node[] replacementList = null;
       Node parent = null;   
       String source = null;
       String path = null;
       String type = null;
       for(int i=0; i < array.length ; i++){
           Node node = array[i];
           /*
           //stop inherited aliases from overwriting valid locale data
           //ldml.dtd does not allow alias to have any sibling elements
           boolean bFoundSibling = false;
           Node n = node.getNextSibling();
           while (n != null)
           {
               if (n.getNodeType() == Node.ELEMENT_NODE)
               {
             //      System.err.println ("it's an element node " + n.getNodeName () + "  " + n.getNodeValue());
                   bFoundSibling = true;
                   break;
               }
               n = n.getNextSibling();
           }
           if (bFoundSibling == true) 
               continue;
           */           
           //initialize the stack for every alias!
           stack = new HashMap();
           if(node==null){
               System.err.println("list.item("+i+") returned null!. The list reports it's length as: "+array.length);
               //System.exit(-1);
               continue;
           }
           parent = node.getParentNode();
          // boolean isDraft = isNodeDraft(node);
           source = getAttributeValue(node, LDMLConstants.SOURCE);
           path = getAttributeValue(node, LDMLConstants.PATH);
           type = getAttributeValue(parent, LDMLConstants.TYPE);
           if(parent.getParentNode()==null){
               // some of the nodes were orphaned by the previous alias resolution .. just continue
               continue;
           }
           if(source!=null && path==null ){
               //this LDML 1.1 style alias parse it
               path = getAbsoluteXPath(node, type);
           }
           String key = "SRC:" + thisLocale + ";XPATH:"+getAbsoluteXPath(node, type);
           if(stack.get(key)!=null){
               throw new IllegalStateException("Found circular aliases! " + key);
               
           }
           stack.put(key, "");
           if(source.equals(LDMLConstants.LOCALE)){
             
               Object[] aliasList = getChildNodeListAsArray(getNode(parent, path), false);
               Object[] childList = getChildNodeListAsArray(parent, true);
               replacementList = mergeNodeLists(aliasList, childList);
           }else if(source!=null && !source.equals(thisLocale)){
               // if source is defined then path should not be 
               // relative 
               if(path.indexOf("..")>0){
                   throw new IllegalArgumentException("Cannot parse relative xpath: " + path + 
                                                      " in locale: "+ source + 
                                                      " from source locale: "+thisLocale);
               }
               // this is a is an absolute XPath
               Document newDoc = getFullyResolvedLDML(sourceDir, source, false, true, false, ignoreDraft, stack);
               replacementList = getNodeListAsArray(newDoc, path);
           }else{
               // path attribute is referencing another node in this DOM tree
               replacementList = getNodeListAsArray(parent, path);
           }
           if(replacementList != null){
               parent.removeChild(node);
               int listLen = replacementList.length;
               if(listLen > 1){
                   // check if the whole locale is aliased
                   // if yes then remove the identity from
                   // the current document!
                   if(path!=null && path.equals("//ldml/*")){
                       Node[] identity = getNodeArray(fullyResolvedDoc, LDMLConstants.IDENTICAL);
                       for(int j=0; j<identity.length; j++){
                           parent.removeChild(node);
                       }
                   }else{
                       //remove all the children of the parent node
                       removeChildNodes(parent);
                   }
                   for(int j=0; j<listLen; j++){
                       // found an element node in the aliased resource
                       // add to the source
                       Node child = replacementList[j];
                       Node childToImport = fullyResolvedDoc.importNode(child,true);
                      // if(isDraft==true && childToImport.getNodeType() == Node.ELEMENT_NODE){
                      //     ((Element)childToImport).setAttribute("draft", "true");
                      // }
                       parent.appendChild(childToImport);
                   }
               }else{
                   Node replacement = replacementList[0];
                   //remove all the children of the parent node
                   removeChildNodes(parent);
                   for(Node child = replacement.getFirstChild(); child!=null; child=child.getNextSibling()){
                       // found an element node in the aliased resource
                       // add to the source
                       Node childToImport = fullyResolvedDoc.importNode(child,true);
                      // if(isDraft==true && childToImport.getNodeType() == Node.ELEMENT_NODE){
                      //     ((Element)childToImport).setAttribute("draft", "true");
                      // }
                       parent.appendChild(childToImport);
                   }
               }
              
           }else{
               throw new IllegalArgumentException("Could not find node for xpath: " + path + 
                       " in locale: "+ source + 
                       " from source locale: "+thisLocale);

           }
       }
       return fullyResolvedDoc;
    }
    private static void removeChildNodes(Node parent){
        Node[] children = toNodeArray(parent.getChildNodes());
        for(int j=0; j<children.length; j++){ 
            parent.removeChild(children[j]);
        }
    }
    //TODO add funtions for fetching legitimate children
    // for ICU 
    public boolean isParentDraft(Document fullyResolved, String xpath){
        Node node = getNode(fullyResolved, xpath);
        Node parentNode ;
        while((parentNode = node.getParentNode())!=null){
            String draft = getAttributeValue(parentNode, LDMLConstants.DRAFT);
            if(draft!=null ){
                if(draft.equals("true") || draft.equals("provisional") || draft.equals("unconfirmed")){
                    return true;
                }else{
                    return false;
                }
            }
        }
        // the default value is false if none specified
        return false;
    }
    public static boolean isNodeDraft(Node node){
        String draft = getAttributeValue(node, LDMLConstants.DRAFT);
        if(draft!=null ){
            if(draft.equals("true") || draft.equals("provisional") || draft.equals("unconfirmed")){
                return true;
            }else{
                return false;
            }
        }
        return false;
    }
    public static boolean isDraft(Node fullyResolved, StringBuffer xpath){
        Node current = getNode(fullyResolved, xpath.toString());
        String draft = null;
        while(current!=null && current.getNodeType()== Node.ELEMENT_NODE){
            draft = getAttributeValue(current, LDMLConstants.DRAFT);
            if(draft!=null){
                if(draft.equals("true") || draft.equals("provisional") || draft.equals("unconfirmed")){
                    return true;
                }else{
                    return false;
                }
            }
            current = current.getParentNode();
        }
        return false;
    }
    public static boolean isSiblingDraft(Node root){
        Node current = root;
        String draft = null;
        while(current!=null && current.getNodeType()== Node.ELEMENT_NODE){
            draft = getAttributeValue(current, LDMLConstants.DRAFT);
            if(draft!=null){
                if(draft.equals("true") || draft.equals("provisional") || draft.equals("unconfirmed")){
                    return true;
                }else{
                    return false;
                }
            }
            current = current.getNextSibling();
        }
        return false;
    }
    
    public static void appendAllAttributes(Node node, StringBuffer xpath){
        NamedNodeMap attr = node.getAttributes();
        int len = attr.getLength();
        if(len>0){
            for(int i=0; i<len; i++){
                Node item = attr.item(i);
                xpath.append("[@");
                xpath.append(item.getNodeName());
                xpath.append("='");
                xpath.append(item.getNodeValue());
                xpath.append("']");
            }
        }
    }
    private static boolean areChildNodesElements(Node node){
        NodeList list = node.getChildNodes();
        for(int i=0;i<list.getLength();i++){
            if(list.item(i).getNodeType()==Node.ELEMENT_NODE){
                return true;
            }
        }
        return false;
    }
    private static boolean areSiblingsOfNodeElements(Node node){
        NodeList list = node.getParentNode().getChildNodes();
        int count = 0;
        for(int i=0;i<list.getLength();i++){
            Node item = list.item(i); 
            if(item.getNodeType()==Node.ELEMENT_NODE){
                count++;
            }
            // the first child node of type element of <ldml> should be <identity>
            // here we figure out if any additional elements are there
            if(count>1){
                return true;
            }
        }
        return false; 
    }
    public static boolean isLocaleAlias(Document doc){
        return (getAliasNode(doc)!=null);
    }
    private static Node getAliasNode(Document doc){
        NodeList elements = doc.getElementsByTagName(LDMLConstants.IDENTITY);
        if(elements.getLength()==1){
            Node id = elements.item(0);
            Node sib = id;
            while((sib =sib.getNextSibling())!=null){
                if(sib.getNodeType()!=Node.ELEMENT_NODE){
                    continue;
                }
                if(sib.getNodeName().equals(LDMLConstants.ALIAS)){
                    return sib;
                }
            }
        }else{
            System.out.println("Error: elements returned more than 1 identity element!");
        }
        return null;
    }
    /**
     * Determines if the whole locale is marked draft. To accomplish this
     * the method traverses all leaf nodes to determine if all nodes are marked draft
     */
    private static boolean seenElementsOtherThanIdentity = false;
    public static final boolean isLocaleDraft(Node node){
        boolean isDraft = true;
        //fast path to check if <ldml> element is draft
        if(isNodeDraft(node)==true){
            return true;
        }
        for(Node child=node.getFirstChild(); child!=null; child=child.getNextSibling()){
            if(child.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            String name = child.getNodeName();
            //fast path to check if <ldml> element is draft
            if(name.equals(LDMLConstants.LDML) && isNodeDraft(child)==true){
                return true;
            }
            if(name.equals(LDMLConstants.IDENTITY)){
                seenElementsOtherThanIdentity=areSiblingsOfNodeElements(child);
                continue;
            }

            if(child.hasChildNodes() && areChildNodesElements(child)){
                isDraft = isLocaleDraft(child);
            }else{
                if(isNodeDraft(child) == false){
                    isDraft = false;
                }
            }
            if(isDraft == false){
                break;
            }
        }
        if(!seenElementsOtherThanIdentity){
            return false;
        }
        return isDraft;
     }
    /**
     * Appends the attribute values that make differentiate 2 siblings
     * in LDML
     * @param node
     * @param xpath
     * @deprecated - use version that takes StringBuilder instead
     */
    @Deprecated
    public static final void appendXPathAttribute(Node node, StringBuffer xpath){
      appendXPathAttribute(node,xpath,false,false);
    }
    /**
     * @deprecated
     */
    @Deprecated
    public static void appendXPathAttribute(Node node, StringBuffer xpath, boolean ignoreAlt, boolean ignoreDraft){
      StringBuilder sb = new StringBuilder(xpath.toString());
      appendXPathAttribute(node, sb, ignoreAlt, ignoreDraft);
    }
    
    public static final void appendXPathAttribute(Node node, StringBuilder xpath){
        appendXPathAttribute(node,xpath,false,false);
    }
    
    public static void appendXPathAttribute(Node node, StringBuilder xpath, boolean ignoreAlt, boolean ignoreDraft){
        boolean terminate = false;
        String val = getAttributeValue(node, LDMLConstants.TYPE);
        String and =  "][";//" and ";
        boolean isStart = true;
        String name = node.getNodeName();
        if(val!=null && !name.equals(LDMLConstants.DEFAULT)&& !name.equals(LDMLConstants.MS)){
            if(!(val.equals("standard")&& name.equals(LDMLConstants.PATTERN))){
               
                if(isStart){
                    xpath.append("[");
                    isStart=false;
                }
                xpath.append("@type='");
                xpath.append(val);
                xpath.append("'");
                terminate = true;
            }
        }
        if(!ignoreAlt) {
            val = getAttributeValue(node, LDMLConstants.ALT);
            if(val!=null){
                if(isStart){
                    xpath.append("[");
                    isStart=false;
                }else{
                    xpath.append(and);
                }
                xpath.append("@alt='");
                xpath.append(val);
                xpath.append("'");
                terminate = true;
            }
            
        }
        
        if(!ignoreDraft) {
            val = getAttributeValue(node, LDMLConstants.DRAFT);
            if(val!=null && !name.equals(LDMLConstants.LDML)){
                if(isStart){
                    xpath.append("[");
                    isStart=false;
                }else{
                    xpath.append(and);
                }
                xpath.append("@draft='");
                xpath.append(val);
                xpath.append("'");
                terminate = true;
            }
            
        }
        
        val = getAttributeValue(node, LDMLConstants.KEY);
        if(val!=null){
            if(isStart){
                xpath.append("[");
                isStart=false;
            }else{
                xpath.append(and);
            }
            xpath.append("@key='");
            xpath.append(val);
            xpath.append("'");
            terminate = true;
        }
        val = getAttributeValue(node, LDMLConstants.REGISTRY);
        if(val!=null){
            if(isStart){
                xpath.append("[");
                isStart=false;
            }else{
                xpath.append(and);
            }
            xpath.append("@registry='");
            xpath.append(val);
            xpath.append("'");
            terminate = true;
        }
        val = getAttributeValue(node, LDMLConstants.ID);
        if(val!=null){
            if(isStart){
                xpath.append("[");
                isStart=false;
            }else{
                xpath.append(and);
            }
            xpath.append("@id='");
            xpath.append(val);
            xpath.append("'");
            terminate = true;
        }
        if(terminate){
            xpath.append("]");
        }
    }
    /**
     * Ascertains if the children of the given node are element
     * nodes.
     * @param node
     * @return
     */
    public static boolean areChildrenElementNodes(Node node){
        NodeList list = node.getChildNodes();
        for(int i=0;i<list.getLength();i++){
            if(list.item(i).getNodeType()==Node.ELEMENT_NODE){
                return true;
            }
        }
        return false;  
    }
    public static Node[] getNodeListAsArray( Node doc, String xpath){
        try{
            NodeList list = XPathAPI_selectNodeList(doc, xpath);
            int length = list.getLength();
            if(length>0){
                Node[] array = new Node[length];
                for(int i=0; i<length; i++){
                    array[i] = list.item(i);
                }
                return array;
            }
            return null;
        }catch(TransformerException ex){
            throw new RuntimeException(ex.getMessage());
        } 
    }

    private static Object[] getChildNodeListAsArray( Node parent, boolean exceptAlias){

        NodeList list = parent.getChildNodes();
        int length = list.getLength();
        
        ArrayList al = new ArrayList();
        for(int i=0; i<length; i++){
            Node item  = list.item(i);
            if(item.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            if(exceptAlias && item.getNodeName().equals(LDMLConstants.ALIAS)){
               continue; 
            }
            al.add(item);
        }
        return al.toArray();
        
    }
    public static Node[] toNodeArray( NodeList list){
        int length = list.getLength();
        if(length>0){
            Node[] array = new Node[length];
            for(int i=0; i<length; i++){
                array[i] = list.item(i);
            }
            return array;
        }
        return null;
    }
    public static Node[] getElementsByTagName(Document doc, String tagName){
        try{
            NodeList list = doc.getElementsByTagName(tagName);
            int length = list.getLength();
            if(length>0){
                Node[] array = new Node[length];
                for(int i=0; i<length; i++){
                    array[i] = list.item(i);
                }
                return array;
            }
            return null;
        }catch(Exception ex){
            throw new RuntimeException(ex.getMessage());
        } 
    }
    
    /**
     * Fetches the list of nodes that match the given xpath
     * @param doc
     * @param xpath
     * @return
     */
    public static NodeList getNodeList( Document doc, String xpath){
        try{
            return XPathAPI_selectNodeList(doc, xpath);

        }catch(TransformerException ex){
            throw new RuntimeException(ex.getMessage());
        }   
    }
    
    public static final boolean isAlternate(Node node){
        NamedNodeMap attributes = node.getAttributes();
        Node attr = attributes.getNamedItem(LDMLConstants.ALT);
        if(attr!=null){
            return true;
        }
        return false;
    }

    private static final Node getNonAltNode(NodeList list /*, StringBuffer xpath*/){
        // A nonalt node is one which .. does not have alternate
        // attribute set
        Node node =null;
        for(int i =0; i<list.getLength(); i++){
            node = list.item(i);
            if(/*!isDraft(node, xpath)&& */!isAlternate(node)){
                return node;
            }
        }
        return null;
    }
    
    private static final Node getNonAltNodeIfPossible(NodeList list)
    {
        // A nonalt node is one which .. does not have alternate
        // attribute set
        Node node =null;
        for(int i =0; i<list.getLength(); i++)
        {
            node = list.item(i);
            if(/*!isDraft(node, xpath)&& */!isAlternate(node))
            {
                return node;
            }
        }
        if (list.getLength()>0)
            return list.item(0);   //if all have alt=.... then return the first one
        return null;
    }
        
    public static Node getNonAltNodeLike(Node parent, Node child){
        StringBuffer childXpath = new StringBuffer(child.getNodeName());
        appendXPathAttribute(child,childXpath,true/*ignore alt*/,true/*ignore draft*/);
        String childXPathString = childXpath.toString();
        for(Node other=parent.getFirstChild(); other!=null; other=other.getNextSibling() ){
            if((other.getNodeType()!=Node.ELEMENT_NODE)  || (other==child)) {
                continue;
            }
            StringBuffer otherXpath = new StringBuffer(other.getNodeName());
            appendXPathAttribute(other,otherXpath);
          //  System.out.println("Compare: " + childXpath + " to " + otherXpath);
            if(childXPathString.equals(otherXpath.toString())) {
              //  System.out.println("Match!");
                return other;
            }
        }
        return null;
    }


    /**
     * Fetches the node from the document that matches the given xpath.
     * The context namespace node is required if the xpath contains 
     * namespace elments
     * @param doc
     * @param xpath
     * @param namespaceNode
     * @return
     */
    public static Node getNode(Document doc, String xpath, Node namespaceNode){
        try{
            NodeList nl = XPathAPI_selectNodeList(doc, xpath, namespaceNode);
            int len = nl.getLength();
            //TODO watch for attribute "alt"
            if(len>1){
              throw new IllegalArgumentException("The XPATH returned more than 1 node!. Check XPATH: "+xpath);   
            }
            if(len==0){
                return null;
            }
            return nl.item(0);

        }catch(TransformerException ex){
            throw new RuntimeException(ex.getMessage());
        }
    }
    public static Node getNode(Node context, String resToFetch, Node namespaceNode){
        try{
            NodeList nl = XPathAPI_selectNodeList(context, "./"+resToFetch, namespaceNode);
            int len = nl.getLength();
            //TODO watch for attribute "alt"
            if(len>1){
              throw new IllegalArgumentException("The XPATH returned more than 1 node!. Check XPATH: "+resToFetch);   
            }
            if(len==0){
                return null;
            }
            return nl.item(0);

        }catch(TransformerException ex){
            throw new RuntimeException(ex.getMessage());
        }
    }
    /**
     * Fetches the node from the document which matches the xpath
     * @param node
     * @param xpath
     * @return
     */
    public static Node getNode(Node node, String xpath){
        try{
            NodeList nl = XPathAPI_selectNodeList(node, xpath);
            int len = nl.getLength();
            //TODO watch for attribute "alt"
            if(len>1){
                //PN Node best = getNonAltNode(nl);
                Node best = getNonAltNodeIfPossible(nl); //PN
                if(best != null) {
                    //System.err.println("Chose best node from " + xpath);
                    return best;
                }
                /* else complain */
                String all = ""; 
                int i;
                for(i=0;i<len;i++) {
                    all = all + ", " + nl.item(i);
                }
                throw new IllegalArgumentException("The XPATH returned more than 1 node!. Check XPATH: "+xpath + " = " + all);   
            }
            if(len==0){
                return null;
            }
            return nl.item(0);

        }catch(TransformerException ex){
            throw new RuntimeException(ex.getMessage());
        }
    }
    public static Node getNode(Node node, String xpath, boolean preferDraft, boolean preferAlt){
        try{
            NodeList nl = XPathAPI_selectNodeList(node, xpath);
            return getNode(nl, xpath, preferDraft, preferAlt);

        }catch(TransformerException ex){
            throw new RuntimeException(ex.getMessage());
        }
    }
    private static Node getVettedNode(NodeList list, StringBuffer xpath, boolean ignoreDraft){
        // A vetted node is one which is not draft and does not have alternate
        // attribute set
        Node node =null;
        for(int i =0; i<list.getLength(); i++){
            node = list.item(i);
            if(isDraft(node, xpath) && !ignoreDraft){
                continue;
            }
            if(isAlternate(node)){
                continue;
            }
            return node;
        }
        return null;
    }
    public static Node getVettedNode(Document fullyResolvedDoc, Node parent, String childName, StringBuffer xpath, boolean ignoreDraft){
        NodeList list = getNodeList(parent, childName, fullyResolvedDoc, xpath.toString());
        int oldLength=xpath.length();
        Node ret = null;

        if(list != null && list.getLength()>0){
            xpath.append("/");
            xpath.append(childName);
            ret = getVettedNode(list,xpath, ignoreDraft);
        }
        xpath.setLength(oldLength);
        return ret;
    }
    public static Node getNode(NodeList nl, String xpath, boolean preferDraft, boolean preferAlt){
        int len = nl.getLength();
        //TODO watch for attribute "alt"
        if(len>1){
            Node best = null;
            for(int i=0; i<len;i++){
                Node current = nl.item(i);
                if(!preferDraft && ! preferAlt){
                    if(!isNodeDraft(current) && ! isAlternate(current)){
                        best = current;
                        break;
                    }
                    continue;
                }else if(preferDraft && !preferAlt){
                    if(isNodeDraft(current) && ! isAlternate(current)){
                        best = current;
                        break;
                    }
                    continue;
                }else if(!preferDraft && preferAlt){
                    if(!isNodeDraft(current) && isAlternate(current)){
                        best = current;
                        break;
                    }
                    continue;
                }else{
                    if(isNodeDraft(current) || isAlternate(current)){
                        best = current;
                        break;
                    }
                    continue;
                }
            }
            if(best==null && preferDraft==true){
                best = getVettedNode(nl, new StringBuffer(xpath), false);
            }
            if(best != null){
                return best;
            }
            /* else complain */
            String all = ""; 
            int i;
            for(i=0;i<len;i++) {
                all = all + ", " + nl.item(i);
            }
            throw new IllegalArgumentException("The XPATH returned more than 1 node!. Check XPATH: "+xpath + " = " + all);   
        }
        if(len==0){
            return null;
        }
        return nl.item(0);

    }
    /**
     * 
     * @param context
     * @param resToFetch
     * @param fullyResolved
     * @param xpath
     * @return
     */
    public static Node getNode(Node context, String resToFetch, Document fullyResolved, String xpath){
        String ctx = "./"+ resToFetch;
        Node node = getNode(context, ctx);
        if(node == null && fullyResolved!=null){
            // try from fully resolved
            String path = xpath+"/"+resToFetch;
            node = getNode(fullyResolved, path);
        }
        return node;
    }
    /**
     * 
     * @param context
     * @param resToFetch
     * @return
     */
    public static NodeList getChildNodes(Node context, String resToFetch){
        String ctx = "./"+ resToFetch;
        NodeList list = getNodeList(context, ctx);
        return list;
    }
    /**
     * Fetches the node from the document that matches the given xpath.
     * The context namespace node is required if the xpath contains 
     * namespace elments
     * @param doc
     * @param xpath
     * @param namespaceNode
     * @return
     */
    public static NodeList getNodeList(Document doc, String xpath, Node namespaceNode){
        try{
            NodeList nl = XPathAPI_selectNodeList(doc, xpath, namespaceNode);
            if(nl.getLength()==0){
                return null;
            }
            return nl;

        }catch(TransformerException ex){
            throw new RuntimeException(ex.getMessage());
        }
    }
    /**
     * Fetches the node from the document which matches the xpath
     * @param node
     * @param xpath
     * @return
     */
    public static NodeList getNodeList(Node node, String xpath){
        try{
            NodeList nl = XPathAPI_selectNodeList(node, xpath);
            int len = nl.getLength();
            if(len==0){
                return null;
            }
            return nl;
        }catch(TransformerException ex){
            throw new RuntimeException(ex.getMessage());
        }
    }

    /**
     * Fetches node list from the children of the context node.
     * @param context
     * @param resToFetch
     * @param fullyResolved
     * @param xpath
     * @return
     */
    public static NodeList getNodeList(Node context, String resToFetch, Document fullyResolved, String xpath){
        String ctx = "./"+ resToFetch;
        NodeList list = getNodeList(context, ctx);
        if((list == null || list.getLength()>0) && fullyResolved!=null){
            // try from fully resolved
            String path = xpath+"/"+resToFetch;
            list = getNodeList(fullyResolved, path);
        }
        return list;
    }

    /**
     * Decide if the node is text, and so must be handled specially 
     * @param n
     * @return
     */
    private static boolean isTextNode(Node n) {
      if (n == null)
        return false;
      short nodeType = n.getNodeType();
      return nodeType == Node.CDATA_SECTION_NODE || nodeType == Node.TEXT_NODE;
    }   
    public static Node getAttributeNode(Node sNode, String attribName){
        NamedNodeMap attrs = sNode.getAttributes();
        if(attrs!=null){
           return attrs.getNamedItem(attribName);
        }
        return null;
    }
    /**
     * Utility method to fetch the attribute value from the given 
     * element node
     * @param sNode
     * @param attribName
     * @return
     */
    public static String getAttributeValue(Node sNode, String attribName){
        String value=null;
        NamedNodeMap attrs = sNode.getAttributes();
        if(attrs!=null){
            Node attr = attrs.getNamedItem(attribName);
            if(attr!=null){
                value = attr.getNodeValue();
            }
        }
        return value;
    }
    /**
     * Utility method to set the attribute value on the given 
     * element node
     * @param sNode
     * @param attribName
     * @param val
     */
    public static void setAttributeValue(Node sNode, String attribName, String val){

        Node attr = sNode.getAttributes().getNamedItem(attribName);
        if(attr!=null){
            attr.setNodeValue(val);
        } else {
            attr = sNode.getOwnerDocument().createAttribute(attribName);
            attr.setNodeValue(val);
            sNode.getAttributes().setNamedItem(attr);
        }
    }
    /**
     * Utility method to fetch the value of the element node
     * @param node
     * @return
     */
    public static String getNodeValue(Node node){
        for(Node child=node.getFirstChild(); child!=null; child=child.getNextSibling() ){
            if(child.getNodeType()==Node.TEXT_NODE){
                return child.getNodeValue();
            }
        }
        return null;
    }
    
    /**
     * Parse & resolve file level alias
     */
    public static Document parseAndResolveAlias(String filename, String locale, boolean ignoreError)throws RuntimeException {
        // Force filerefs to be URI's if needed: note this is independent of any other files
        String docURI = filenameToURL(filename);
        Document doc = parse(new InputSource(docURI),filename,ignoreError);
        NodeList elements = doc.getElementsByTagName(LDMLConstants.IDENTITY);
        if(elements.getLength()==1){
            Node id = elements.item(0);
            Node sib = id;
            while((sib =sib.getNextSibling())!=null){
                if(sib.getNodeType()!=Node.ELEMENT_NODE){
                    continue;
                }
                if(sib.getNodeName().equals(LDMLConstants.ALIAS)){
                    String source = LDMLUtilities.getAttributeValue(sib, LDMLConstants.SOURCE);
                    //String fn = filename.substring(0,filename.lastIndexOf(File.separator)+1)+source+".xml";
                    resolveAliases(doc,filename.substring(0,filename.lastIndexOf(File.separator)+1),locale, false, null);
                }
            }
        }else{
            System.out.println("Error: elements returned more than 1 identity element!");
        }
        if(DEBUG){
            try {
                 java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                         new  java.io.FileOutputStream("./" + File.separator + locale
                                 + "_debug_1.xml"), "UTF-8");
                 LDMLUtilities.printDOMTree(doc, new PrintWriter(writer),"http://www.unicode.org/cldr/dtd/1.3/ldml.dtd", null);
                 writer.flush();
            } catch (IOException e) {
                 //throw the exceptionaway .. this is for debugging
            }
        }
        return doc;
    }
    /**
     * Simple worker method to parse filename to a Document.  
     *
     * Attempts XML parse, then HTML parse (when parser available), 
     * then just parses as text and sticks into a text node.
     *
     * @param filename to parse as a local path
     *
     * @return Document object with contents of the file; 
     * otherwise throws an unchecked RuntimeException if there 
     * is any fatal problem
     */
    public static Document parse(String filename, boolean ignoreError)throws RuntimeException {
        // Force filerefs to be URI's if needed: note this is independent of any other files
        String docURI = filenameToURL(filename);
        return parse(new InputSource(docURI),filename,ignoreError);
    }
    public static Document parseAndResolveAliases(String locale, String sourceDir, boolean ignoreError, boolean ignoreDraft){
        try{
            Document full = parse(sourceDir+File.separator+ locale, ignoreError);
            if(full!=null){
                full = resolveAliases(full, sourceDir, locale, ignoreDraft, null);
            }
           /*
            * Debugging
            *
            Node[] list = getNodeArray(full, LDMLConstants.ALIAS);
            if(list.length>0){
                System.err.println("Aliases not resolved!. list.getLength() returned "+ list.length);
            }*/
            return full;
        }catch(Exception ex){
            if(!ignoreError){
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
        }
        return null;

    }
    
    private static ErrorHandler getNullErrorHandler(final String filename2, final boolean ignoreError) {
        // Local class: cheap non-printing ErrorHandler
        // This is used to suppress validation warnings
        ErrorHandler nullHandler = new ErrorHandler() {
            public void warning(SAXParseException e) throws SAXException {
                int col = e.getColumnNumber();
                String msg = (filename2 + ":" + e.getLineNumber()
                        + (col >= 0 ? ":" + col : "") + ": WARNING: "
                        + e.getMessage());
                        
                System.err.println(msg);
                if(!ignoreError) {
                    throw new RuntimeException(msg);
                }
            }

            public void error(SAXParseException e) throws SAXException {
                int col = e.getColumnNumber();
                String msg = (filename2 + ":" + e.getLineNumber()
                        + (col >= 0 ? ":" + col : "") + ": ERROR: "
                        + e.getMessage());
                System.err.println(msg);
                if(!ignoreError) {
                    throw new RuntimeException(msg);
                }
            }

            public void fatalError(SAXParseException e) throws SAXException {
                throw e;
            }
        };
        return nullHandler;
    }
    public static Document newDocument() {
        return newDocumentBuilder(false).newDocument();
    }
    private static DocumentBuilder newDocumentBuilder(boolean validating) {
        try
        {
            DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
            // Always set namespaces on
            dfactory.setNamespaceAware(true);
            dfactory.setValidating(validating);
            dfactory.setIgnoringComments(false);
            dfactory.setExpandEntityReferences(true);
            // Set other attributes here as needed
            //applyAttributes(dfactory, attributes);
            
            DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
            docBuilder.setEntityResolver(new CachingEntityResolver());
            return docBuilder;
        } catch(Throwable se) {
            System.err.println(": ERROR : trying to create documentBuilder: " + se.getMessage());
            se.printStackTrace();
            throw new RuntimeException(se);
        }
    }
    public static Document parse(InputSource docSrc, String filename, boolean ignoreError){
        Document doc = null;
        try
        {
            // First, attempt to parse as XML (preferred)...
            DocumentBuilder docBuilder = newDocumentBuilder(true);
            docBuilder.setErrorHandler(getNullErrorHandler(filename,ignoreError));
            doc = docBuilder.parse(docSrc);
        }
        catch (Throwable se)
        {
            // ... if we couldn't parse as XML, attempt parse as HTML...
            System.err.println(filename + ": ERROR :" + se.getMessage());
            se.printStackTrace();
            if(!ignoreError){
                throw new RuntimeException(se);
            }
        }
        return doc;
    }  // end of parse()

    /*
     * Utility method to translate a String filename to URL.  
     *
     * Note: This method is not necessarily proven to get the 
     * correct URL for every possible kind of filename; it should 
     * be improved.  It handles the most common cases that we've 
     * encountered when running Conformance tests on Xalan.
     * Also note, this method does not handle other non-file:
     * flavors of URLs at all.
     *
     * If the name is null, return null.
     * If the name starts with a common URI scheme (namely the ones 
     * found in the examples of RFC2396), then simply return the 
     * name as-is (the assumption is that it's already a URL)
     * Otherwise we attempt (cheaply) to convert to a file:/// URL.
     */
    public static String filenameToURL(String filename){
        // null begets null - something like the commutative property
        if (null == filename){
            return null;
        }

        // Don't translate a string that already looks like a URL
        if (filename.startsWith("file:")
            || filename.startsWith("http:")
            || filename.startsWith("ftp:")
            || filename.startsWith("gopher:")
            || filename.startsWith("mailto:")
            || filename.startsWith("news:")
            || filename.startsWith("telnet:")
           ){
               return filename;
           }
        

        File f = new File(filename);
        String tmp = null;
        try{
            // This normally gives a better path
            tmp = f.getCanonicalPath();
        }catch (IOException ioe){
            // But this can be used as a backup, for cases 
            //  where the file does not exist, etc.
            tmp = f.getAbsolutePath();
        }

        // URLs must explicitly use only forward slashes
        if (File.separatorChar == '\\') {
            tmp = tmp.replace('\\', '/');
        }
        // Note the presumption that it's a file reference
        // Ensure we have the correct number of slashes at the 
        //  start: we always want 3 /// if it's absolute
        //  (which we should have forced above)
        if (tmp.startsWith("/")){
            return "file://" + tmp;
        }
        else{
            return "file:///" + tmp;
        }
    }
    /**
     * Debugging method for printing out the DOM Tree
     * Prints the specified node, recursively. 
     * @param node
     * @param out
     * @throws IOException
     */
    public static void printDOMTree(Node node, PrintWriter out, String docType, String copyright) throws IOException 
    {
         try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            out.print("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
            if(copyright!=null){
                out.print(copyright);
            }
            if(docType!=null){
                out.print(docType);
            }
            
            //transformer.setParameter(entityName, entityRef );
            //transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, XLIFF_PUBLIC_NAME);
            transformer.transform(new DOMSource(node), new StreamResult(out));
        }
        catch (TransformerException te) {
            throw new IOException(te.getMessage());
        }
        
        //out.close();
    } // printDOMTree(Node, PrintWriter)
    
    private static String escapeForXML(String string){
        int len = string.length();
        StringBuffer ret = new StringBuffer(len);
        for(int i =0 ; i<len; i++){
           char ch = string.charAt(i);
           switch(ch){
               case '<':
                   ret.append("&lt;");
                   break;
               case '>':
                   ret.append("&gt;");
                   break;
               case '&':
                   ret.append("&amp;");
                   break;
               default :
                   ret.append(ch);
           }
        }
        return ret.toString();
    }
    
    // Utility functions, HTML and such.
    public static String CVSBASE="http://www.unicode.org/cldr/trac/browser/trunk";
    
    public static final String getCVSLink(String locale)
    {
        return "<a href=\""+CVSBASE+"/common/main/" + locale + ".xml\">";
    }
    
    public static final String getCVSLink(String locale, String version)
    {
        return "<a href=\""+CVSBASE+"/common/main/" + locale + ".xml?rev=" +
            version +"\">";

    }
    /**
     * Load the revision from CVS or from the Identity element.
     * @param fileName
     * @return
     */
    static public String loadFileRevision(String fileName)
    {
         int index = fileName.lastIndexOf(File.separatorChar);
         if(index==-1) {
            return null;
         }
         String sourceDir = fileName.substring(0, index);
         return loadFileRevision(sourceDir, new File(fileName).getName());    
    }
    // //ldml[@version="1.7"]/identity/version[@number="$Revision$"]
//    private static Pattern VERSION_PATTERN = Pattern.compile("//ldml[^/]*/identity/version\\[@number=\"[^0-9]*\\([0-9.]+\\).*");
    private static Pattern VERSION_PATTERN = Pattern.compile(".*identity/version.*Revision[: ]*([0-9.]*).*");
    /**
     * Load the revision from CVS or from the Identity element.
     * 
     * @param sourceDir
     * @param fileName
     * @return
     */
    static public String loadFileRevision(String sourceDir, String fileName) {
       String aVersion = null;
       File entriesFile = new File(sourceDir + File.separatorChar + "CVS","Entries");
       if(entriesFile.exists() && entriesFile.canRead()) {
          try{
            BufferedReader r = new BufferedReader(new FileReader(entriesFile.getPath()));
                String s;
                while((s=r.readLine())!=null) {
                    String lookFor = "/"+fileName+"/";
                    if(s.startsWith(lookFor)) {
                        String ver = s.substring(lookFor.length());
                        ver = ver.substring(0,ver.indexOf('/'));
                        aVersion = ver;
                    }
                }
                r.close();
          }  catch ( Throwable th ) {
                System.err.println(th.toString() + " trying to read CVS Entries file " + entriesFile.getPath());
                return null;
            }
       } else {
           // no CVS, use file ident.
           File xmlFile = new File(sourceDir, fileName);
           if(!xmlFile.exists()) return null;
           final String bVersion[] = { "unknown" };
           try {
               XMLFileReader xfr = new XMLFileReader().setHandler(new SimpleHandler() {
                   private boolean done=false;
//                  public void handleAttributeDecl(String eName, String aName, String type, String mode, String value) {
                   public void handlePathValue(String p, String v) {
                       if(!done) {
                           Matcher m = VERSION_PATTERN.matcher(p);
                           if(m.matches()) {
                               //System.err.println("Matches! "+p+" = "+m.group(1));
                               bVersion[0] = m.group(1);
                               done=true;
                           }
                       }
                   }
               });
               xfr.read(xmlFile.getPath(), -1, true);
               aVersion = bVersion[0]; // copy from input param
           } catch(Throwable t) {
               t.printStackTrace();
               aVersion = "err";
               System.err.println("Error reading version of " + xmlFile.getAbsolutePath() + ": " + t.toString());
           }
       }
       //System.err.println("v="+aVersion);
       return aVersion;
    }

//     // Caching Resolution
//     private static File getCacheName(String sourceDir, String last, String loc)
//     {
//     //File xCacheDir = new File((CachingEntityResolver.getCacheDir()!=null)?CachingEntityResolver.getCacheDir():"/tmp/cldrres");
//         return new  File(sourceDir).getName() + "_" + last + "." + loc;
//     }

//     Document readMergeCache(String sourceDir, String last, String loc)
//     {
//         File cacheName = getCacheName(String sourceDir, last, loc);
//         System.out.println(" M:  " + cacheName);
//         File cacheFile = new File(xCacheDir, cacheName + ".xml");
//         if(cacheFile.exists()) { // && is newer than last, loc
//             doc = parse(cacheFile.getPath(),ignoreUnavailable);
//         }
//         if(doc!=null) {
//             System.out.println("Cache hit for " + cacheName);
//         }
//         return doc;
//     }

//     void writeMergeCache(String sourceDir, String last, String loc, Document full)
//     {
//         try {
//             OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(cacheFile),"UTF-8");
//             PrintWriter pw = new PrintWriter(writer);
//             printDOMTree(full,pw);
//             long stop = System.currentTimeMillis();
//             long total = (stop-start);
//             double s = total/1000.;
//             System.out.println(" " + cacheName + " parse time: " + s + "s");
//             pw.println("<!-- " + cacheName + " parse time: " + s + "s -->");
//             writer.close();
//         } catch (Throwable t) {
//             System.err.println(t.toString() + " while trying to write cache file " + cacheName);
//         }
//     }

    public static  String getFullPath(int fileType, String fName, String dir){
        String str=null;
        int lastIndex1 = fName.lastIndexOf(File.separator, fName.length()) + 1/* add  1 to skip past the separator */; 
        int lastIndex2 = fName.lastIndexOf('.', fName.length());
        if (fileType == TXT) {
            if(lastIndex2 == -1){
                fName = fName.trim() + ".txt";
            }else{
                if(!fName.substring(lastIndex2).equalsIgnoreCase(".txt")){
                    fName =  fName.substring(lastIndex1,lastIndex2) + ".txt";
                }
            }
            if (dir != null && fName != null) {
                str = dir + "/" + fName.trim();                   
            } else {
                str = System.getProperty("user.dir") + "/" + fName.trim();
            }
        } else if(fileType == XML){
            if(lastIndex2 == -1){
                fName = fName.trim() + ".xml";
            }else{
                if(!fName.substring(lastIndex2).equalsIgnoreCase(".xml") && fName.substring(lastIndex2).equalsIgnoreCase(".xlf")){
                    fName = fName.substring(lastIndex1,lastIndex2) + ".xml";
                }
            }
            if(dir != null && fName != null) {
                str = dir + "/" + fName;
            } else if (lastIndex1 > 0) {
                str = fName;
            } else {
                str = System.getProperty("user.dir") + "/" + fName;
            }
        }else{
            System.err.println("Invalid file type.");
            System.exit(-1);
        }
        return str;
    } 

    /**
     * Returns the parent locale given a locale ID.
     * Returns "root" for any language locale.
     * Returns null for root's parent
     * @param locale Locale to take the parent of.
     * @return the parent locale, or null if "root" is passed in
     */
    public static final String getParent(String locale) {
        return LocaleIDParser.getParent(locale);
    }
    
    /** split an alt= tag into pieces. Any piece can be missing (== null)
     * Piece 0:  'alt type'.    null means this is the normal (non-alt) item.  Possible values are 'alternate', 'colloquial', etc.
     * Piece 1:  'proposed type'.  If non-null, this is a string beginning with 'proposed' and containing arbitrary other text.
     * 
     * STRING        0            1
     * -------------------------------
     * ""/null      null         null
     * something    something    null
     * something-proposed   something   proposed
     * something-proposed3   something   proposed3
     * proposed      null     proposed
     * somethingproposed  somethingproposed  null
     * 
     *
     * @param alt the alt tag to parse
     * @return a 2-element array containing piece 0 and piece 1
     * @see formatAlt
     */
    public static String[] parseAlt(String alt) {
        String[] ret = new String[2];
        if(alt==null) {
            ret[0]=null;
            ret[1]=null;
        } else {
            int l = alt.indexOf(LDMLConstants.PROPOSED);
            if(l==-1) { /* no PROPOSED */
                ret[0]=alt;  // all alt, 
                ret[1]=null; // no kind
            } else if(l==0) { /* begins with */
                ret[0]=null;  // all properties 
                ret[1]=alt;
            } else {
                if(alt.charAt(l-1) != '-') {
                    throw new InternalError("Expected '-' before " + LDMLConstants.PROPOSED + " in " + alt);
                }
                ret[0]=alt.substring(0,l-1);
                ret[1]=alt.substring(l);
                
                if(ret[0].length()==0) {
                    ret[0] = null;
                }
                if(ret[1].length()==0) {
                    ret[1] = null;
                }
            }
        }
        return ret;
    }
    
    /**
     * Format aan alt string from components.
     * @param altType optional alternate type (i.e. 'alternate' or 'colloquial').
     * @param proposedType  
     * @see parseAlt
     */
    public static String formatAlt(String altType, String proposedType) {

        if(((altType == null) || (altType.length()==0))  &&
           ((proposedType == null) || (proposedType.length()==0))) {
                return null;
        }
        
        if((proposedType==null)||(proposedType.length()==0)) {
            return altType; // no proposed type:  'alternate'
        } else if(!proposedType.startsWith(LDMLConstants.PROPOSED)) {
            throw new InternalError("proposedType must begin with " + LDMLConstants.PROPOSED);
        }
        
        if((altType==null)||(altType.length()==0)) {
            return proposedType; // Just a proposed type:  "proposed" or "proposed-3"
        } else {        
            return altType + "-" + proposedType; // 'alternate-proposed'
        }
    }
    
    /**
     * Compatibility.
     * @param node
     * @param xpath
     * @return
     * @throws TransformerException 
     */
    private static NodeList XPathAPI_selectNodeList(Node node, String xpath) throws TransformerException {
        XPathFactory  factory=XPathFactory.newInstance();
        XPath xPath=factory.newXPath();
        try {
            XPathExpression  xPathExpression=
                xPath.compile(xpath);
            return (NodeList)xPathExpression.evaluate( node, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new TransformerException("Exception in XPathAPI_selectNodeList: "+xpath,e);
        }
    }
    private static NodeList XPathAPI_selectNodeList(Document doc, String xpath,
            Node namespaceNode) throws TransformerException {
        XPathFactory  factory=XPathFactory.newInstance();
        XPath xPath=factory.newXPath();
        try {
            XPathExpression  xPathExpression=
                xPath.compile(xpath);
            return (NodeList)xPathExpression.evaluate( doc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new TransformerException("Exception in XPathAPI_selectNodeList: "+xpath,e);
        }
    }
    private static NodeList XPathAPI_selectNodeList(Node context, String xpath,
            Node namespaceNode) throws TransformerException {
        XPathFactory  factory=XPathFactory.newInstance();
        XPath xPath=factory.newXPath();
        try {
            XPathExpression  xPathExpression=
                xPath.compile(xpath);
            return (NodeList)xPathExpression.evaluate( context, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new TransformerException("Exception in XPathAPI_selectNodeList: "+xpath,e);
        }
    }
    private static void XPathAPI_eval(Node context, String string,
            Node namespaceNode) throws TransformerException {
        XPathAPI_selectNodeList(context, string, namespaceNode);
    }
    private static void XPathAPI_eval(Node context, String string) throws TransformerException {
        XPathAPI_selectNodeList(context, string);
    }
}