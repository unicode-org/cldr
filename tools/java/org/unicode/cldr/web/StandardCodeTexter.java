//
//  StandardCodeTexter.java
//  cldrtools
//
//  Created by Steven R. Loomis on 3/26/2005.
//  Copyright 2005 IBM. All rights reserved.
//


package org.unicode.cldr.web;

import org.unicode.cldr.util.StandardCodes;

public class StandardCodeTexter implements NodeSet.NodeSetTexter {
    StandardCodes standardCodes = StandardCodes.make();
    String type;
    public StandardCodeTexter(String superType) {
        type = SurveyMain.typeToSubtype(superType);
    }
    public String text(NodeSet.NodeSetEntry e) {
        return standardCodes.getData(type, e.type);
    }
}
