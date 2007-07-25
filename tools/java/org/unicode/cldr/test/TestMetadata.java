package org.unicode.cldr.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Iterator;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.StandardCodes;


import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.Differ;
import org.unicode.cldr.icu.CollectionUtilities;

public class TestMetadata {
	public static void main(String[] args) {
		Factory cldrFactory = CLDRFile.Factory.make(Utility.MAIN_DIRECTORY, ".*");
        CLDRFile metadata = cldrFactory.make("supplementalMetadata", false);
//        Set allKeys = new TreeSet();
//        CollectionUtilities.addAll(metadata.iterator(), allKeys);
//        System.out.println("Keys: " + allKeys);
        // attribute order
        
        Set elements = new TreeSet();
        Set attributes = new TreeSet();
        Set elementOrderingLists = new LinkedHashSet();
        
        getElementsAndAttributes(Utility.MAIN_DIRECTORY + "root.xml", elements, attributes, elementOrderingLists);
        Set suppElements = new TreeSet();
        Set suppAttributes = new TreeSet();
        Set suppElementOrderingLists = new LinkedHashSet();
        getElementsAndAttributes(Utility.COMMON_DIRECTORY + "supplemental/characters.xml", suppElements, suppAttributes, suppElementOrderingLists);
        
        Set allElements = new TreeSet();
        allElements.addAll(elements);
        allElements.addAll(suppElements);
        Set allAttributes = new TreeSet();
        allAttributes.addAll(attributes);
        allAttributes.addAll(suppAttributes);
    	
        List attributeOrder = Arrays.asList(metadata.getStringValue("//supplementalData/metadata/attributeOrder").split("\\s+"));
        java.util.Collection programAttributeOrder = CLDRFile.getAttributeOrder();

        Set allAttributeOrder = new TreeSet();
        allAttributeOrder.addAll(attributeOrder);
        allAttributeOrder.addAll(programAttributeOrder);
        allAttributeOrder.remove("_q");
        if (showSetDifferences("dtd attributes", allAttributes, "attributeOrder+programAttributeOrder", allAttributeOrder)) {
        	System.out.println("ERROR: differences in sets!");
        }
        
        if (!attributeOrder.equals(programAttributeOrder)) {
        	System.out.println("ElementOrderDifference: ");
        	System.out.println(showDifference(programAttributeOrder, attributeOrder, ", "));
        	System.out.println("metadata: " + attributeOrder);
        	System.out.println("program: " + programAttributeOrder);
        	System.out.println("ERROR: differences in sets!");
        }

        List elementOrder = Arrays.asList(metadata.getStringValue("//supplementalData/metadata/elementOrder").split("\\s+"));
        List programElementOrder = (List) CLDRFile.getElementOrder();
        
        sublistCheck(elementOrderingLists, programElementOrder);
        sublistCheck(suppElementOrderingLists, programElementOrder);

        Set allElementOrder = new TreeSet();
        allElementOrder.addAll(elementOrder);
        allElementOrder.addAll(programElementOrder);
        if (showSetDifferences("dtd elements", allElements, "elementOrder+programElementOrder", allElementOrder)) {
        	System.out.println("ERROR: differences in sets!");
        }


        if (!elementOrder.equals(programElementOrder)) {
        	System.out.println("ElementOrderDifference: ");
        	System.out.println(showDifference(programElementOrder, elementOrder, ", "));
        	System.out.println("metadata: " + elementOrder);
        	System.out.println("program: " + programElementOrder);
        	System.out.println("ERROR: differences in sets!");
        }
        
        testZones(metadata);
        System.out.println("Done");
	}

	private static void sublistCheck(Set elementOrderingLists, List elementOrder) {
        for (Iterator it = elementOrderingLists.iterator(); it.hasNext();) {
        	LinkedHashSet sublist = (LinkedHashSet) it.next();
        	// verify that the elements are in the list in the right order.
        	int lastPosition = -1;
        	for (Iterator it2 = sublist.iterator(); it2.hasNext();) {
        		String item = (String) it2.next();
        		int position = elementOrder.indexOf(item);
        		if (position <= lastPosition) {
        			System.out.println("ERROR: elements out of order for: " + item + " in " + sublist);
        			return;
        		}
        	}
        	System.out.println("Elements in order for: " + sublist);
        }
	}
	
	private static boolean showSetDifferences(String name1, Collection set1, String name2, Collection set2) {
		boolean hasDifference = false;
		TreeSet temp = new TreeSet();
		temp.addAll(set1);
		temp.removeAll(set2);
		if (temp.size() != 0) {
			System.out.println(name1 + " minus " + name2 + ":\t" + temp);
			hasDifference |= true;
		}
		temp.clear();
		temp.addAll(set2);
		temp.removeAll(set1);
		if (temp.size() != 0) {
			System.out.println(name2 + " minus " + name1 + ":\t" + temp);
			hasDifference |= true;
		}
		return hasDifference;
	}



	private static void getElementsAndAttributes(String fileWithDTD, Collection elements, Collection attributes, Collection elementOrderingLists) {
		XMLFileReader xfr = new XMLFileReader().setHandler(new MyHandler(elements, attributes, elementOrderingLists));
		xfr.read(fileWithDTD, -1, true);
	}


	private static String showDifference(Collection a, Collection b, String separator) {
	        Differ differ = new Differ(300, 3);
	        StringBuffer out = new StringBuffer();
	        Iterator ai = a.iterator();
	        Iterator bi = b.iterator();
	        boolean first = true;
	        while (true) {
	        	boolean done = true;
	        	if (ai.hasNext()) {
	        		differ.addA(ai.next());
	        		done = false;
	        	}
	        	if (bi.hasNext()) {
	        		differ.addB(bi.next());
	        		done = false;
	        	}
	            differ.checkMatch(done);

	            if (differ.getACount() != 0 || differ.getBCount() != 0) {
	            	if (first) first = false;
	            	else out.append(separator);
	                out.append("...");
	            	out.append(separator).append(differ.getA(-1));
	            	
	                if (differ.getACount() != 0) {
	                    for (int i =0; i < differ.getACount(); ++i) {
	    	            	out.append(separator).append('-');
	                        out.append(differ.getA(i));
	                    }
	                }
	                if (differ.getBCount() != 0) {
	                    for (int i = 0; i < differ.getBCount(); ++i) {
	    	            	out.append(separator).append('+');
	                        out.append(differ.getB(i));
	                    }
	                }
	            	out.append(separator).append(differ.getA(differ.getACount()));
	            }
	            if (done) break;
	        }
	        return out.toString();
	    }

	private static void testZones(CLDRFile metadata) {
		String zoneList = null;
        for (Iterator it = metadata.iterator(); it.hasNext();) {
        	String key = (String) it.next();
        	if (key.indexOf("\"$tzid\"") >= 0) {
        		zoneList = metadata.getStringValue(key);
        		break;
        	}
        }
        
        String[] zones = zoneList.split("\\s+");
        Set metaZoneSet = new TreeSet();
        metaZoneSet.addAll(Arrays.asList(zones));

        StandardCodes sc = StandardCodes.make();
        Map new_oldZones = sc.getZoneData();
        Set stdZoneSet = new TreeSet();
        stdZoneSet.addAll(new_oldZones.keySet());
        
        if (metaZoneSet.equals(stdZoneSet)) {
        	System.out.println("Zone Set is up-to-date");
        } else {
        	Set diff = new TreeSet();
        	diff.addAll(metaZoneSet);
        	diff.removeAll(stdZoneSet);
        	System.out.println("Meta Zones - Std Zones: " + diff);
        	diff.clear();
        	diff.addAll(stdZoneSet);
        	diff.removeAll(metaZoneSet);
        	System.out.println("Std Zones - Meta Zones: " + diff);
        	
           	System.out.println("Meta Zones: " + metaZoneSet);
           	System.out.println("Std Zones: " + stdZoneSet);

        }
	}
	
	static class MyHandler extends XMLFileReader.SimpleHandler {
		Collection elements;
		Collection attributes;
		Collection elementOrderingLists;
		public MyHandler(Collection elements, Collection attributes, Collection elementOrderingLists) {
			this.elements = elements;
			this.attributes = attributes;
			this.elementOrderingLists = elementOrderingLists;
		}
		public void handleAttributeDecl(String eName, String aName, String type, String mode, String value) {
			attributes.add(aName);
//			System.out.println(
//					"eName: " + eName
//					+ ",\t aName: " + aName
//					+ ",\t type: " + type
//					+ ",\t mode: " + mode
//					+ ",\t value: " + value
//					);
		}

		public void handleElementDecl(String name, String model) {
			elements.add(name);
			LinkedHashSet ordering = new LinkedHashSet(Arrays.asList(model.split("[^-_a-zA-Z0-9]+")));
			ordering.remove("");
			ordering.remove("PCDATA");
			ordering.remove("EMPTY");
			if (ordering.size() > 1) {
				if (elementOrderingLists.add(ordering)) {
					//System.out.println(model + " =>\t" + ordering);					
				}
			}
//			System.out.println(
//					"name: " + name
//					+ ",\t model: " + model);
		}

//		public void handlePathValue(String path, String value) {
//			System.out.println(
//					"path: " + path
//					+ ",\t value: " + value);
//		}
//
//		public void handleComment(String path, String comment) {
//			System.out.println(
//					"path: " + path
//					+ ",\t comment: " + comment);
//		}
		
	}
}