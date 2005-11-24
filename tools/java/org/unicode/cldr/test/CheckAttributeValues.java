package org.unicode.cldr.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.xpath.axes.MatchPatternIterator;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.dev.test.util.VariableReplacer;
import com.ibm.icu.impl.CollectionUtilities.ObjectMatcher;

public class CheckAttributeValues extends CheckCLDR {
	static LinkedHashSet elementOrder = new LinkedHashSet();
	static LinkedHashSet attributeOrder = new LinkedHashSet();
	static LinkedHashSet serialElements = new LinkedHashSet();
	//static Map suppress = new HashMap();
	// TODO change these to HashMap, once this is all debugged.
	static Map element_attribute_validity = new TreeMap();
	static Map common_attribute_validity = new TreeMap();
	static Map variables = new TreeMap();
	static boolean initialized = false;
	static LocaleMatcher localeMatcher;

	XPathParts parts = new XPathParts(null, null);
	public CheckCLDR handleCheck(String path, String fullPath, String value, List result) {
		parts.set(fullPath);
		for (int i = 0; i < parts.size(); ++i) {
			Map attributes = parts.getAttributes(i);
			String element = parts.getElement(i);
			if (attributes.size() == 0) continue;
			Map attribute_validity = (Map) element_attribute_validity.get(element);
			for (Iterator it = attributes.keySet().iterator(); it.hasNext();) {
				String attribute = (String) it.next();
				String attributeValue = (String) attributes.get(attribute);
				// check the common attributes first
				check(common_attribute_validity, attribute, attributeValue, result);
				// then for the specific element
				check(attribute_validity, attribute, attributeValue, result);
			}
			
		}
		return this;
	}
	private void check(Map attribute_validity, String attribute, String attributeValue, List result) {
		if (attribute_validity == null) return; // no test
		MatcherPattern matcherPattern = (MatcherPattern) attribute_validity.get(attribute);
		if (matcherPattern == null) return; // no test
		if (matcherPattern.matcher.matches(attributeValue)) return;
		result.add(new CheckStatus()
				 .setType(CheckStatus.errorType)
				 .setMessage("Invalid Attribute Value {0}={1}: expected: {2} {3}", 
						 new Object[]{attribute, attributeValue, matcherPattern.matcher.getClass().getName(), matcherPattern.pattern}));
	}
	
	public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, List possibleErrors) {
		if (cldrFileToCheck == null) return this;
		super.setCldrFileToCheck(cldrFileToCheck, possibleErrors);
		synchronized (elementOrder) {
			if (!initialized) {
				XMLFileReader xfr = new XMLFileReader().setHandler(new MetaDataHandler());
				xfr.read(Utility.COMMON_DIRECTORY + "/supplemental/metaData.xml",xfr.CONTENT_HANDLER,false);
				System.out.println("done reading"); System.out.flush();
				initialized = true;
				for (Iterator it = missing.iterator(); it.hasNext();) {
					System.out.println("\t\t\t<variable id=\"" + it.next() + "\" type=\"list\">stuff</variable>");
				}
				localeMatcher = new LocaleMatcher();
			}
		}
		if (!localeMatcher.matches(cldrFileToCheck.getLocaleID())) {
			possibleErrors.add(new CheckStatus()
					 .setType(CheckStatus.errorType)
					 .setMessage("Invalid Locale {0}", 
							 new Object[]{cldrFileToCheck.getLocaleID()}));
			
		}
		return this;
	}
	static Set missing = new TreeSet();
	static class MetaDataHandler extends XMLFileReader.SimpleHandler {
		XPathParts parts = new XPathParts(null, null);

		public void handlePathValue(String path, String value) {
			//System.out.println(path);
			//System.out.flush();
			parts.set(path);
			String lastElement = parts.getElement(-1);
			if (lastElement.equals("elementOrder")) {
				elementOrder.addAll(Arrays.asList(value.split("\\s+")));
			} else if (lastElement.equals("attributeOrder")) {
				attributeOrder.addAll(Arrays.asList(value.split("\\s+")));
			} else if (lastElement.equals("suppress")) {
				// skip for now
			} else if (lastElement.equals("serialElements")) {
				// skip for now
			} else if (lastElement.equals("attributes")) {
				// skip for now
			} else if (lastElement.equals("variable")) {
				Map attributes = parts.getAttributes(-1);
				MatcherPattern mp = getMatcherPattern(value, attributes);
				if (mp != null) {
					String id = (String) attributes.get("id");
					variables.put(id, mp);
				}
			} else if (lastElement.equals("attributeValues")) {
				try {
					String originalValue = value;
					Map attributes = parts.getAttributes(-1);
					
					MatcherPattern mp = getMatcherPattern(value, attributes);
					if (mp == null) {
						System.out.println("Failed to make matcher for: " + value + "\t" + path);
						return;
					}
					String[] attributeList = ((String) attributes
							.get("attributes")).split("\\s+");
					String elementsString = (String) attributes.get("elements");
					if (elementsString == null) {
						addAttributes(attributeList, common_attribute_validity, mp);
					} else {
						String[] elementList = elementsString.split("\\s+");
						for (int i = 0; i < elementList.length; ++i) {
							String element = elementList[i];
							// System.out.println("\t" + element);
							Map attribute_validity = (Map) element_attribute_validity
									.get(element);
							if (attribute_validity == null)
								element_attribute_validity.put(element,
										attribute_validity = new TreeMap());
							addAttributes(attributeList, attribute_validity, mp);
						}
					}

				} catch (RuntimeException e) {
					System.err
							.println("Problem with: " + path + ", \t" + value);
					e.printStackTrace();
				}
			} else if (lastElement.equals("version")) {
				// skip for now
			} else if (lastElement.equals("generation")) {
				// skip for now
			} else if (lastElement.equals("languageAlias")) {
				// skip for now
			} else if (lastElement.equals("territoryAlias")) {
				// skip for now
			} else if (lastElement.equals("deprecatedItems")) {
				// skip for now 
			} else {
				System.out.println("Unknown final element: " + path);
			}
		}
		
		private MatcherPattern getMatcherPattern(String value, Map attributes) {
			String typeAttribute = (String) attributes.get("type");
			MatcherPattern result = (MatcherPattern) variables.get(value);
			if (result != null) {
				if ("list".equals(typeAttribute)) {
					MatcherPattern temp = new MatcherPattern();
					temp.pattern = value;
					temp.matcher = new ListMatcher().set(result.matcher);
					result = temp;
				}
				return result;
			}

			result = new MatcherPattern();
			result.pattern = value;
			if ("choice".equals(typeAttribute)
					|| "given".equals(attributes.get("order"))) {
				result.matcher = new CollectionMatcher().set(new HashSet(Arrays.asList(value.split(" "))));
			} else if ("regex".equals(typeAttribute)) {
				result.matcher = new RegexMatcher().set(value, Pattern.COMMENTS); // Pattern.COMMENTS to get whitespace	
			} else if ("locale".equals(typeAttribute)) {
				result.matcher = new LocaleMatcher();
			} else {
				System.out.println("unknown type value: " + value + "\t" + attributes);
				return null;
			}
			return result;
		}

		private void addAttributes(String[] attributes, Map attribute_validity, MatcherPattern mp) {
			for (int i = 0; i < attributes.length; ++i) {
				String attribute = attributes[i];
				MatcherPattern old = (MatcherPattern) attribute_validity.get(attribute);
				if (old != null) {
					mp.matcher = new OrMatcher().set(old.matcher, mp.matcher);
					mp.pattern = old.pattern + " OR " + mp.pattern;
				}
				attribute_validity.put(attribute, mp);
			}
		}
	}
	private static class MatcherPattern {
		ObjectMatcher matcher;
		String pattern;
		public String toString() {
			return matcher.getClass().getName() + "\t" + pattern;
		}
	}
    public static class RegexMatcher implements ObjectMatcher {
        private java.util.regex.Matcher matcher;
        public ObjectMatcher set(String pattern) {
            matcher = Pattern.compile(pattern).matcher("");
            return this;
        }
        public ObjectMatcher set(String pattern, int flags) {
            matcher = Pattern.compile(pattern, flags).matcher("");
            return this;
        }
        public boolean matches(Object value) {
            matcher.reset(value.toString());
            return matcher.matches();
        }
    }
    public static class CollectionMatcher implements ObjectMatcher {
        private Collection collection;
        public ObjectMatcher set(Collection collection) {
            this.collection = collection;
            return this;
        }
        public boolean matches(Object value) {
            return collection.contains(value);
        }
    }
    public static class OrMatcher implements ObjectMatcher {
        private ObjectMatcher a;
        private ObjectMatcher b;
        public ObjectMatcher set(ObjectMatcher a, ObjectMatcher b) {
            this.a = a;
            this.b = b;
            return this;
        }
        public boolean matches(Object value) {
            return a.matches(value) || b.matches(value);
        }
    }
    public static class ListMatcher implements ObjectMatcher {
        private ObjectMatcher other;
        public ObjectMatcher set(ObjectMatcher other) {
            this.other = other;
            return this;
        }
        public boolean matches(Object value) {
        	String[] values = ((String)value).split("\\s+");
        	if (values.length == 1 && values[0].length() == 0) return true;
        	for (int i = 0; i < values.length; ++i) {
            	if (!other.matches(values[i])) {
            		return false;
            	}
        	}
        	return true;
        }
    }
    public static class LocaleMatcher implements ObjectMatcher {
    	ObjectMatcher grandfathered = ((MatcherPattern)variables.get("$grandfathered")).matcher;
    	ObjectMatcher language = ((MatcherPattern)variables.get("$language")).matcher;
    	ObjectMatcher script = ((MatcherPattern)variables.get("$script")).matcher;
    	ObjectMatcher territory = ((MatcherPattern)variables.get("$territory")).matcher;
    	ObjectMatcher variant = ((MatcherPattern)variables.get("$variant")).matcher;
    	LocaleIDParser lip = new LocaleIDParser();
        public boolean matches(Object value) {
        	if (grandfathered.matches(value)) return true;
        	lip.set((String)value);
        	String field = lip.getLanguage();
        	if (!language.matches(field)) return false;
        	field = lip.getScript();
        	if (field.length() != 0 && !script.matches(field)) return false;
        	field = lip.getRegion();
        	if (field.length() != 0 && !territory.matches(field)) return false;
        	String[] fields = lip.getVariants();
        	for (int i = 0; i < fields.length; ++i) {
        		if (!variant.matches(fields[i])) return false;
        	}
        	return true;
        }
    }

}