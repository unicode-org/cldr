/**
 * Copyright (C) 2011 IBM Corporation and Others. All Rights Reserved.
 *
 */
package org.unicode.cldr.web;

import java.util.Comparator;

import org.unicode.cldr.web.DataSection.DataRow;
import org.unicode.cldr.web.Partition.Membership;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;

/**
 * @author srl
 *
 */
public class CodeSortMode extends SortMode {

	public static String name=SurveyMain.PREF_SORTMODE_CODE;
	
	/* (non-Javadoc)
	 * @see org.unicode.cldr.web.SortMode#getName()
	 */
	@Override
	String getName() {
		return name;
	}

	@Override
	Membership[] memberships() {
		return null;
	}

	@Override
	Comparator<DataRow> createComparator() {
		return comparator();
	}
	
	public static
	Comparator<DataRow> comparator() {
		return new Comparator<DataRow>() {
			final Collator myCollator = createCollator();
		      public int compare(DataRow p1, DataRow p2){
		        if(p1==p2) { 
		          return 0;
		        }
		        return myCollator.compare(p1.type, p2.type);
		      }
		    };
	}
	    
    public static Collator createCollator() {
        RuleBasedCollator rbc = 
            ((RuleBasedCollator)Collator.getInstance());
        rbc.setNumericCollation(true);
        return rbc;
    }

    @Override
	public String getDisplayName(DataRow p) {
    	return p.type; // always code.
    }

}
