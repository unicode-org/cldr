/**
 * Copyright (C) 2011 IBM Corporation and Others. All Rights Reserved.
 *
 */
package org.unicode.cldr.web;

import java.util.List;
import java.util.Vector;

import org.unicode.cldr.web.DataSection.DataRow;

/**
 * @author srl
 *
 * This would be Generic except generic arrays are UGLY. http://stackoverflow.com/questions/529085/java-how-to-generic-array-creation
 */
public class Partition {
    public static abstract class Membership {
    	public String name;
    	public String name() { return name; }
    	protected Membership(String name) { this.name = name; }
        public abstract boolean isMember(DataSection.DataRow p);
    };
    
    public Membership pm;

    public String name; // name of this partition
    public int start; // first item
    public int limit; // after last item

    public Partition(String n, int s, int l) {
        name = n;
        start = s;
        limit = l;
    }
    
    public Partition(Membership pm) {
        this.pm = pm;
        name = pm.name();
        start = -1;
        limit = -1;
    }
    
    public String toString() {
        return name + " - ["+start+".."+limit+"]";
    }

	public static Partition[] createPartitions(Membership[] memberships,
			DataRow rows[]) {
        Vector<Partition> v = new Vector<Partition>();
        if(memberships != null) { // something with partitions
        	Partition testPartitions[] = createPartitions(memberships);
        	
            // find the starts
            int lastGood = 0;
            for(int i=0;i<rows.length;i++) {
                DataRow p = rows[i];
                                    
                for(int j=lastGood;j<testPartitions.length;j++) {
                    if(testPartitions[j].pm.isMember(p)) {
                        if(j>lastGood) {
                            lastGood = j;
                        }
                        if(testPartitions[j].start == -1) {
                            testPartitions[j].start = i;
                        }
                        break; // sit here until we fail membership
                    }
                    
                    if(testPartitions[j].start != -1) {
                        testPartitions[j].limit = i;
                    }
                }
            }
            // catch the last item
            if((testPartitions[lastGood].start != -1) &&
                (testPartitions[lastGood].limit == -1)) {
                testPartitions[lastGood].limit = rows.length; // limit = off the end.
            }
                
            for(int j=0;j<testPartitions.length;j++) {
                if(testPartitions[j].start != -1) {
					if(testPartitions[j].start!=0 && v.isEmpty()) {
//						v.add(new Partition("Other",0,testPartitions[j].start));
					}
                    v.add(testPartitions[j]);
                }
            }
        } else {
            // default partition - e'erthing.
            v.add(new Partition(null, 0, rows.length));
        }
        return (Partition[])v.toArray(new Partition[0]); // fold it up
	}

	public static Partition[] createPartitions(Membership[] memberships) {
		if(memberships == null) {
			Partition empty[] = new Partition[1];
			empty[0] = new Partition(null, 0, 0);
		}
		Partition testPartitions[] = new Partition[memberships.length];
    	for(int i=0;i<memberships.length;i++) {
    		testPartitions[i] = new Partition(memberships[i]);
    	}
    	return testPartitions;
    }
};
