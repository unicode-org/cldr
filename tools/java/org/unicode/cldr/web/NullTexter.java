//
//  NullTexter.java
//  cldrtools
//
//  Created by Steven R. Loomis on 3/26/2005.
//  Copyright 2005 IBM. All rights reserved.
//

package org.unicode.cldr.web;

public class NullTexter implements NodeSet.NodeSetTexter {
    public String text(NodeSet.NodeSetEntry e) {
        return e.type;
    }
}
