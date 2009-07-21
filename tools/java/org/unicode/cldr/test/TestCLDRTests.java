/*
 * Created on Jan 28, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.unicode.cldr.test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.Collator;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.TreeSet;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.CldrUtility;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleDateFormat;

/**
 * Test the tests themselves. Internal use.
 */
public class TestCLDRTests extends TestFmwk {
    static final boolean DEBUG = false;
    
    ULocale uLocale = ULocale.ENGLISH;
    Locale oLocale = Locale.ENGLISH; // TODO Drop once ICU4J has ULocale everywhere
    PrintWriter log; 
    SAXParser SAX;
    TreeMap results = new TreeMap();

    public static void main(String[] args) throws Exception {
		double deltaTime = System.currentTimeMillis();		
		TestCLDRTests me = new TestCLDRTests();
		me.run(args);
		me.closeLog();
        deltaTime = System.currentTimeMillis() - deltaTime;
        System.out.println("Seconds: " + deltaTime/1000);
    }
    
    /**
	 * 
	 */
	private void closeLog() {
		log.close();
	}

	TestCLDRTests () throws IOException {
    	log = BagFormatter.openUTF8Writer(CldrUtility.GEN_DIRECTORY,"collationTestLog.txt");
    	log.write(0xFEFF);
    }
    
    Set languagesToTest;
    
    public void TestAll() throws Exception {
    	Map platform_locale_status = StandardCodes.make().getLocaleTypes();
    	Map onlyLocales = (Map) platform_locale_status.get("IBM");
    	Set locales = onlyLocales.keySet();
    	languagesToTest = (Set)new CldrUtility.Transform() {
    		LocaleIDParser lip = new LocaleIDParser();
    		public Object transform(Object source) {
				return lip.set(source.toString()).getLanguageScript();    			
    		}
    	}.transform(locales, new TreeSet());
    	languagesToTest.remove("th"); // JDK endless loop in collation

        File[] list = new File(CldrUtility.TEST_DIR).listFiles();
        for (int i = 0; i < list.length; ++i) {
        	String name = list[i].getName();
            if (!name.endsWith(".xml")) continue;
            doSingle(name.substring(0,name.length()-4));
        }
        logln("Testing done");
        if (results.size() != 0) {
        	errln("Cummulative errors: " + results);
        }
    }
    
    public void doSingle(String localeName) throws Exception {
        if (!languagesToTest.contains(localeName)) {
        	logln("Skipping " + localeName);
        	return;
        }
        logln("Testing " + localeName);
        uLocale = new ULocale(localeName);
        oLocale = uLocale.toLocale();
        
        File f = new File(CldrUtility.TEST_DIR, localeName + ".xml");
        SAX.parse(f, DEFAULT_HANDLER);
    }
    
    // ============ SAX Handler Infrastructure ============ 

    abstract public class Handler {
        Map settings = new TreeMap();
        String name;
        String attributes;

        void setName(String name) {
        	this.name = name;
        }
        void set(String attributeName, String attributeValue) {
            //if (DEBUG) System.out.println(attributeName + " \u2192 " + attributeValue);
            settings.put(attributeName, attributeValue);
        }
        void checkResult(String value) {
            try {
                handleResult(value);
            } catch (Exception e) {
                myerrln("Exception with result: <" + value + ">");
                e.printStackTrace();
            }
        }
        
        public void myerrln(String message) {
            String temp = uLocale + "\t" + message + "\t[" + name;
            for (Iterator it = settings.keySet().iterator(); it.hasNext();) {
                String attributeName = (String) it.next();
                String attributeValue = (String) settings.get(attributeName);
                temp += " " + attributeName + "=<" + attributeValue + ">";
            }
            temp += "]";
            errln(temp);
            if (log != null) log.println(temp);
            MutableInteger foo  = (MutableInteger) results.get(uLocale.getDisplayName());
            if (foo == null) results.put(uLocale.getDisplayName(), foo = new MutableInteger());
            foo.value++;
        }
        int lookupValue(Object x, Object[] list) {
            for (int i = 0; i < list.length; ++i) {
                if (x.equals(list[i])) return i;
            }
            myerrln("Unknown String: " + x);
            return -1;
        }
        abstract void handleResult(String value) throws Exception;
    }
    
    static class MutableInteger {
    	int value;
    	public String toString() {
    		return String.valueOf(value);
    	}
    }
    

	private static String show(Attributes attributes) {
		String result = "{";
		for (int i = 0; i < attributes.getLength(); ++i) {
			result += attributes.getQName(i) + "=\"" + attributes.getValue(i) + ";";
		}
		return result + "}";
	}
       
    public Handler getHandler(String name) {
        if (DEBUG) System.out.println("Creating Handler: " + name);
        Handler result = (Handler) RegisteredHandlers.get(name);
        if (result == null) System.out.println("Unexpected test type: " + name);
        return result;
    }
    
    public void addHandler(String name, Handler handler) {
    	handler.setName(name);
        RegisteredHandlers.put(name, handler);
    }
    Map RegisteredHandlers = new HashMap();
    
    // ============ Statics for Date/Number Support ============ 

    static TimeZone utc = TimeZone.getTimeZone("GMT");
    static DateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    {
        iso.setTimeZone(utc);
    }
    static int[] DateFormatValues = {-1, DateFormat.SHORT, DateFormat.MEDIUM, DateFormat.LONG, DateFormat.FULL};
    static String[] DateFormatNames = {"none", "short", "medium", "long", "full"};
    
    static String[] NumberNames = {"standard", "integer", "decimal", "percent", "scientific"};

    // ============ Handler for Collation ============ 
    {
		addHandler("collation", new Handler() {
			public void handleResult(String value) {
				Collator col = Collator.getInstance(uLocale.toLocale());
				boolean showedAttributes = false;
				String lastLine = "";
				for (int pos = 0; pos < value.length();) {
					int nextPos = value.indexOf('\n', pos);
					if (nextPos < 0)
						nextPos = value.length();
					String line = value.substring(pos, nextPos).trim(); // HACK for SAX
                    if (line.length() != 0) {  // HACK for SAX
    					int comp = col.compare(lastLine, line);
    					if (comp > 0) {
    						myerrln("<" + lastLine + "> should be leq <" + line + ">" + 
    								(showedAttributes ? "" : " " + attributes));
    						showedAttributes = true;
    					} else if (DEBUG) {
    						logln("OK: " + line);
    						log.println("OK: " + line);
    					}
                    }
					pos = nextPos + 1;
					lastLine = line;
				}
			}
		});
        
        // ============ Handler for Numbers ============ 
		addHandler("number", new Handler() {
			public void handleResult(String result) {
                NumberFormat nf = null;
                double v = Double.NaN;
                for (Iterator it = settings.keySet().iterator(); it.hasNext();) {
                    String attributeName = (String) it.next();
                    String attributeValue = (String) settings
                            .get(attributeName);
                    if (attributeName.equals("input")) {
                        v = Double.parseDouble(attributeValue);
                        continue;
                    }
                    // must be either numberType at this point
                    int index = lookupValue(attributeValue, NumberNames);
                    switch(index) {
                    case 0: nf = NumberFormat.getInstance(oLocale); break;
                    case 1: nf = NumberFormat.getIntegerInstance(oLocale); break;
                    case 2: nf = NumberFormat.getNumberInstance(oLocale); break;
                    case 3: nf = NumberFormat.getPercentInstance(oLocale); break;
                    case 4: nf = NumberFormat.getScientificInstance(oLocale); break;
                    }
                    String temp = nf.format(v).trim();
                    result = result.trim(); // HACK because of SAX
                    if (!temp.equals(result)) {
                        myerrln("Mismatched Number: CLDR: <" + result + ">, Host: <" + temp + ">");
                    }
                        
                }
			}
		});
        
        // ============ Handler for Dates ============ 
		addHandler("date", new Handler() {
			public void handleResult(String result) throws ParseException {
				int dateFormat = DateFormat.DEFAULT;
				int timeFormat = DateFormat.DEFAULT;
				Date date = new Date();
				for (Iterator it = settings.keySet().iterator(); it.hasNext();) {
					String attributeName = (String) it.next();
					String attributeValue = (String) settings
							.get(attributeName);
					if (attributeName.equals("input")) {
						date = iso.parse(attributeValue);
						continue;
					}
					// must be either dateType or timeType at this point
					int index = lookupValue(attributeValue, DateFormatNames);
					int value = DateFormatValues[index];
					if (attributeName.equals("dateType"))
						dateFormat = value;
					else
						timeFormat = value;

				}
				DateFormat dt = dateFormat == -1 ? DateFormat.getTimeInstance(timeFormat, oLocale)
                        : timeFormat == -1 ? DateFormat.getDateInstance(dateFormat, oLocale) 
                        : DateFormat.getDateTimeInstance(dateFormat, timeFormat, oLocale);
                dt.setTimeZone(utc);
				String temp = dt.format(date).trim();
                result = result.trim(); // HACK because of SAX
				if (!temp.equals(result)) {
					myerrln("Mismatched DateTime: CLDR: <" + result + ">, Host: <" + temp + ">");
				}
			}
		});
        
	}
    
    // ============ Gorp for SAX ============ 

    {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(true);
            SAX = factory.newSAXParser();
        } catch (Exception e) {
            throw new IllegalArgumentException("can't start");
        }
    }

    DefaultHandler DEFAULT_HANDLER = new DefaultHandler() {
        static final boolean DEBUG = false;
        StringBuffer lastChars = new StringBuffer();
        boolean justPopped = false;
        Handler handler;
        
        public void startElement(
            String uri,
            String localName,
            String qName,
            Attributes attributes)
            throws SAXException {
                //data.put(new ContextStack(contextStack), lastChars);
                //lastChars = "";
                try {
                    if (qName.equals("cldrTest")) {
                     // skip   
                    } else if (qName.equals("result")) {
                    	for (int i = 0; i < attributes.getLength(); ++i) {
                    		handler.set(attributes.getQName(i), attributes.getValue(i));
                        }
                    } else {
                    	handler = getHandler(qName);
                    	handler.attributes = show(attributes);
                        //handler.set("locale", uLocale.toString());
                    }
                    //if (DEBUG) System.out.println("startElement:\t" + contextStack);
                    justPopped = false;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    throw e;
                }
        }
        public void endElement(String uri, String localName, String qName)
            throws SAXException {
                try {
                    //if (DEBUG) System.out.println("endElement:\t" + contextStack);
                    if (qName.equals("result")) handler.checkResult(lastChars.toString());
                    else if (qName.length() != 0) {
                    	//logln("Unexpected contents of: " + qName + ", <" + lastChars + ">");
                    }
                    lastChars.setLength(0);                    
                    justPopped = true;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    throw e;
                }
            }
        // Have to hack around the fact that the character data might be in pieces
        public void characters(char[] ch, int start, int length)        
            throws SAXException {
                try {
                    String value = new String(ch,start,length);
                    if (DEBUG) System.out.println("characters:\t" + value);
                    lastChars.append(value);
                    justPopped = false;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    throw e;
                }
            }

        // just for debugging
        
        public void notationDecl (String name, String publicId, String systemId)
        throws SAXException {
            System.out.println("notationDecl: " + name
            + ", " + publicId
            + ", " + systemId
            );
        }

        public void processingInstruction (String target, String data)
        throws SAXException {
            System.out.println("processingInstruction: " + target + ", " + data);
        }
        
        public void skippedEntity (String name)
        throws SAXException
        {
            System.out.println("skippedEntity: " + name
            );
        }

        public void unparsedEntityDecl (String name, String publicId,
                        String systemId, String notationName)
        throws SAXException {
            System.out.println("unparsedEntityDecl: " + name
            + ", " + publicId
            + ", " + systemId
            + ", " + notationName
            );
        }

    };
}
