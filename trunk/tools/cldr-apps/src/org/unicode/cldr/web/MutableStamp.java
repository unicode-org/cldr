/**
 * Copyright (C) 2012 IBM Corporation and Others. All Rights Reserved.
 */
package org.unicode.cldr.web;

/**
 * @author srl A class of Stamp that can be updated
 */
public class MutableStamp extends Stamp {

    public static MutableStamp getInstance() {
        return new MutableStamp(Stamp.nextStampTime());
    }

    protected MutableStamp(long stamp) {
        super(stamp);
    }

    /**
     * Update the stamp to the next stamp
     *
     * @return
     */
    public long next() {
        return (stamp = Stamp.nextStampTime());
    }
}
