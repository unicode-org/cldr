/*
 *******************************************************************************
 * Copyright (C) 2005, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on May 11, 2005
 * @author Ram Viswanadha
 */

package org.unicode.cldr.icu;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.unicode.cldr.util.LDMLUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.icu.dev.tool.UOption;
/**
 * This allows you to delete LDML nodes in CLDR.
 * 
 * @author ram
 */
public class LDMLNodesDeleter {
    /**
     * These must be kept in sync with getOptions().
     */
    private static final int HELP1 = 0,
                             HELP2 = 1,
                             SOURCEDIR = 2,
                             DESTDIR = 3,
                             XPATH = 4,
                             UOPTION_LIMIT = 5;
    
    private static final UOption[] options = new UOption[] {
            UOption.HELP_H(),
            UOption.HELP_QUESTION_MARK(),
            UOption.SOURCEDIR(),
            UOption.DESTDIR(),  
            UOption.create("xpath", 'x', UOption.REQUIRES_ARG),
    };
    

    public static void main(String[] args) {
        LDMLNodesDeleter del = new LDMLNodesDeleter();
        del.processArgs(args);
    }
    
    private void usage() {
        System.out.println("\nUsage: LDMLNodesDeleter [OPTIONS] [XPATH1] [XPATH2]\n\n"+
                "This program is used to delete nodes from the given LDML file.\n"+
                "Please refer to the following options. Options are not case sensitive.\n"+
                "Options:\n"+
                "-s or --sourcedir          source directory, followed by the path, default is current directory.\n" +
                "-d or --destdir            destination directory, followed by the path, default is current directory.\n"+
                "-x or --xpath              XPath of the nodes to be deleted. You can pass in multiple XPaths seperated by a colon.\n"+
                "-h or -? or --help         this usage text.\n"+
                "example: com.ibm.icu.dev.tool.cldr.LDMLNodesDeleter -s locale/common/main -d locale/common/main/modified -x //ldml/dates/calendars/calendar[@type='chinese']://ldml/characters en.xml");
         System.exit(-1);
    }
    private String xpath = null;
    private String sourcedir = null;
    private String destdir = null;
    /*<!DOCTYPE ldml SYSTEM "   http://www.unicode.org/cldr/dtd/1.1/ldml.dtd" 
[
      <!ENTITY % icu SYSTEM " http://www.unicode.org/cldr/dtd/1.1/ldmlICU.dtd"> 
%icu;
]
>*/
    private static final String copyright = "<!--\n"+
            " Copyright (c) 2002-2005 International Business Machines Corporation and others. All rights reserved.\n"+
            "-->\n";
    private static final String docType = "<!DOCTYPE ldml SYSTEM \"http://www.unicode.org/cldr/dtd/1.3/ldml.dtd\"\n"+ 
        "[\n   <!ENTITY % icu SYSTEM \" http://www.unicode.org/cldr/dtd/1.3/ldmlICU.dtd\">\n   %icu;\n]\n>\n";
    private void processArgs(String[] args) {
        int remainingArgc = 0;
        try{
            remainingArgc = UOption.parseArgs(args, options);
        }catch (Exception e){
            System.err.println("ERROR: "+ e.toString());
            e.printStackTrace();
            usage();
        }
        if(args.length==0 || options[HELP1].doesOccur || options[HELP2].doesOccur) {
            usage();
        }

        if(options[SOURCEDIR].doesOccur) {
            sourcedir = options[SOURCEDIR].value;
        }
        if(options[DESTDIR].doesOccur) {
            destdir = options[DESTDIR].value;
        }
        if(options[XPATH].doesOccur) {
            xpath = options[XPATH].value;
        }

        if(destdir==null){
           throw new RuntimeException("Destination not specified");
        }
        if(remainingArgc<1){
            usage();
            System.exit(-1);
        }
        for(int i=0; i<remainingArgc; i++){
            String fn = args[i];
            String sourceFN = LDMLUtilities.getFullPath(LDMLUtilities.XML, fn, sourcedir);
            String destFN = LDMLUtilities.getFullPath(LDMLUtilities.XML, fn, destdir);
            try{
                Document source = LDMLUtilities.parse(sourceFN, false);
                Node src = LDMLUtilities.getNode(source, "//ldml");
                String[] list = xpath.split(":");
                for(int j=0; j<list.length; j++){
                    System.out.println("INFO: Deleting nodes with xpath: "+list[j]);
                    deleteNodes(source,  list[j]);
                }
                modifyIdentity(source);
                source.normalize();
                OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(destFN),"UTF-8");
                PrintWriter pw = new PrintWriter(writer);
                LDMLUtilities.printDOMTree(src,pw,docType, copyright);
                writer.flush(); 
             }catch( Exception e){ 
                 e.printStackTrace();
                 //System.exit(-1);
             }
        }
    }
    private void modifyIdentity(Document doc){
        Node version = LDMLUtilities.getNode(doc,"//ldml/identity/version");
        NamedNodeMap al = version.getAttributes();
        Node number = al.getNamedItem(LDMLConstants.NUMBER);
        number.setNodeValue("$Revision$");
        Node gen = LDMLUtilities.getNode(doc,"//ldml/identity/generation");
        al = gen.getAttributes();
        Node date = al.getNamedItem("date");
        date.setNodeValue("$Date$");
    }
    private void deleteNodes(Document doc, String xpath){
        NodeList list = LDMLUtilities.getNodeList(doc, xpath);
        if(list!=null&&list.getLength()>0){
            // node list is dynamic .. if a node is deleted, then 
            // list is immidiately updated.
            // so first cache the nodes returned and do stuff 
            Node[] nodes = new Node[list.getLength()];
            for(int i=0; i<list.getLength(); i++){
                nodes[i] = list.item(i);
            }
            for(int i=0;i<nodes.length;i++){
                Node parent = nodes[i].getParentNode();
                parent.removeChild(nodes[i]);
            }
        }
    }
}
