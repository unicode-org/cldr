//
//  DraftFirstTexter.java
//  cldrtools
//
//  Created by Steven R. Loomis on 3/14/2005.
//  Copyright 2005 IBM. All rights reserved.
//

package org.unicode.cldr.web;

import java.io.*;
import java.util.*;

// DOM imports
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.lang.UCharacter;

import org.unicode.cldr.util.*;
import org.unicode.cldr.icu.*;


import com.ibm.icu.lang.UCharacter;


public class DraftFirstTexter implements NodeSet.NodeSetTexter {
    public DraftFirstTexter(NodeSet.NodeSetTexter aTexter) {
        if(aTexter == null) {
            aTexter = new NullTexter();
        }
        subTexter = aTexter;
    }
    public String text(NodeSet.NodeSetEntry e) {
        String n = "";
        if(e.xpath != null) {
            n = e.xpath;
        }
        if( ((e.main != null)&&LDMLUtilities.isNodeDraft(e.main)) || // draft or
            (e.proposed != null) ) { // proposed
            return "0" + subTexter.text(e) + n;
        } else if ((e.main == null)&&(e.fallbackLocale == null)) { // missing
            return "6" + subTexter.text(e) + n;
        } else {
            return "2" + subTexter.text(e) + n; // normal
        }
    }
    NodeSet.NodeSetTexter subTexter;
}
