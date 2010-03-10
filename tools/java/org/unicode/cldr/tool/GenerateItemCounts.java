package org.unicode.cldr.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.XMLFileReader.SimpleHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R4;

public class GenerateItemCounts {
  private static final String OUT_DIRECTORY = CldrUtility.GEN_DIRECTORY + "/itemcount/"; //CldrUtility.MAIN_DIRECTORY;
  private Map<String,List<StackTraceElement>> cantRead = new TreeMap<String,List<StackTraceElement>>();

  private static String[] DIRECTORIES = {
    "cldr-release-1-1",
    "cldr-release-1-2",
    "cldr-release-1-3",
    "cldr-release-1-4",
    "cldr-release-1-5",
    "cldr-release-1-6",
    "cldr-release-1-7/common",
    "cldr-svn2/common"
  };

  public static void main(String[] args) throws IOException {
    Pattern dirPattern = null;
    for (String arg : args) {
      if (arg.equals("summary")) {
        doSummary();
        return;
      } else {
        dirPattern = Pattern.compile(arg);
      }
    }
    GenerateItemCounts main = new GenerateItemCounts();
    try {
      for (String dir : DIRECTORIES) {
        if (dirPattern != null && !dirPattern.matcher(dir).find()) continue;
        String fulldir = new File(CldrUtility.BASE_DIRECTORY + "../" + dir).getCanonicalPath();
        PrintWriter summary = BagFormatter.openUTF8Writer(OUT_DIRECTORY, "count_" + dir.replace("/", "_") + ".txt");
        main.summarizeCoverage(summary, fulldir);
        summary.close();
      }
    } finally {
      if (main.cantRead.size() != 0) {
        System.out.println("Couldn't read:\t");
        for (String file : main.cantRead.keySet()) {
          System.out.println(file + "\t" + main.cantRead.get(file));
        }
      }
      System.out.println("DONE");
    }
  }

  private static void doSummary() throws IOException {
    Map<String,R4<Counter<String>,Counter<String>,Counter<String>,Counter<String>>> key_release_count = new TreeMap();
    Matcher countryLocale = Pattern.compile("([a-z]{2,3})(_[A-Z][a-z]{3})?(_[A-Z]{2})(_.*)?").matcher("");
    List<String> releases = new ArrayList<String>();
    int releaseCount = 1;
    Relation<String,String> release_keys = new Relation(new TreeMap<String,String>(), TreeSet.class);
    for (File subdir : new File(OUT_DIRECTORY).listFiles()) {
      if (!subdir.getName().startsWith("count_")) continue;
      String releaseNum = "1." + releaseCount++;
      releases.add(releaseNum);
      BufferedReader in = BagFormatter.openUTF8Reader("", subdir.getCanonicalPath());
      while (true) {
        String line = in.readLine();
        if (line == null) break;
        String[] parts = line.split("\t");
        String file = parts[0];
        long valueCount = Long.parseLong(parts[1]);
        long valueLen = Long.parseLong(parts[2]);
        long attrCount = Long.parseLong(parts[3]);
        long attrLen = Long.parseLong(parts[4]);
        if (valueCount + attrCount == 0) continue;
        String[] names = file.split("/");
        String key = names[1];
        if (countryLocale.reset(key).matches()) {
          String script = countryLocale.group(2);
          String newKey = countryLocale.group(1) + (script == null ? "" : script);
          //System.out.println(key + " => " + newKey);
          key = newKey;
        }
        release_keys.put(releaseNum, key);
        R4<Counter<String>,Counter<String>,Counter<String>,Counter<String>> release_count = key_release_count.get(key);
        if (release_count == null) {
          release_count = Row.of(new Counter<String>(), new Counter<String>(), new Counter<String>(), new Counter<String>());
          key_release_count.put(key, release_count);
        }
        release_count.get0().add(releaseNum, valueCount);
        release_count.get1().add(releaseNum, valueLen);
        release_count.get2().add(releaseNum, attrCount);
        release_count.get3().add(releaseNum, attrLen);
      }
      in.close();
    }
    PrintWriter summary = BagFormatter.openUTF8Writer(OUT_DIRECTORY, "summary.txt");
    for (String file : releases) {
      summary.print("\t" + file + "\tlen");
    }
    summary.println();
    for (String key : key_release_count.keySet()) {
      summary.print(key);
      R4<Counter<String>,Counter<String>,Counter<String>,Counter<String>> release_count = key_release_count.get(key);
      for (String release2 : releases) {
        long count = release_count.get0().get(release2) + release_count.get2().get(release2);
        long len = release_count.get1().get(release2) + release_count.get3().get(release2);
        summary.print("\t" + count + "\t" + len);
      }
      summary.println();
    }
    for (String release : release_keys.keySet()) {
      summary.println("Release:\t" + release + "\t" + release_keys.getAll(release).size());
    }
    summary.close();
  }

  static final Set<String> ATTRIBUTES_TO_SKIP = Builder.with(new HashSet<String>()).addAll("version", "references", "standard", "draft").freeze();
  
  static class MyHandler extends SimpleHandler {
    XPathParts parts = new XPathParts();
    long valueCount;
    long valueLen;
    long attributeCount;
    long attributeLen;
    @Override
    public void handlePathValue(String path, String value) {
      if (value.length() != 0) {
        valueCount++;
        valueLen += value.length();
      }
      if (path.contains("[@") && !path.contains("/identity/")) {
        parts.set(path);
        int i = parts.size()-1; // only look at last item
        Collection<String> attributes = parts.getAttributeKeys(i);
        if (attributes.size() != 0) {
          String element = parts.getElement(i);
          for (String attribute : attributes) {
            if (ATTRIBUTES_TO_SKIP.contains(attribute)
                    || CLDRFile.isDistinguishing(element, attribute)) {
              continue;
            }
            String attrValue = parts.getAttributeValue(i, attribute);
            String[] valueParts = attrValue.split("\\s");
            for (String valuePart : valueParts) {
              attributeCount++;
              attributeLen += valuePart.length();
            }
          }
        }
      }
    }
  }

  public MyHandler check(File systemID, String name) {
    MyHandler myHandler = new MyHandler();
    try {
      XMLFileReader reader = new XMLFileReader().setHandler(myHandler);
      reader.read(systemID.toString(), XMLFileReader.CONTENT_HANDLER, true);
    } catch (Exception e) {
      cantRead.put(name, Arrays.asList(e.getStackTrace()));
    }
    return myHandler;

    //    try {
    //      FileInputStream fis = new FileInputStream(systemID);
    //      XMLFileReader xmlReader = XMLFileReader.createXMLReader(true);
    //      xmlReader.setErrorHandler(new MyErrorHandler());
    //      MyHandler myHandler = new MyHandler();
    //      smlReader
    //      xmlReader.setHandler(myHandler);
    //      InputSource is = new InputSource(fis);
    //      is.setSystemId(systemID.toString());
    //      xmlReader.parse(is);
    //      fis.close();
    //      return myHandler;
    //    } catch (SAXParseException e) {
    //      System.out.println("\t" + "Can't read " + systemID);
    //      System.out.println("\t" + e.getClass() + "\t" + e.getMessage());
    //    } catch (SAXException e) {
    //      System.out.println("\t" + "Can't read " + systemID);
    //      System.out.println("\t" + e.getClass() + "\t" + e.getMessage());
    //    } catch (IOException e) {
    //      System.out.println("\t" + "Can't read " + systemID);
    //      System.out.println("\t" + e.getClass() + "\t" + e.getMessage());
    //    }      
  }

  static class MyErrorHandler implements ErrorHandler {
    public void error(SAXParseException exception) throws SAXException {
      System.out.println("\r\nerror: " + XMLFileReader.showSAX(exception));
      throw exception;
    }
    public void fatalError(SAXParseException exception) throws SAXException {
      System.out.println("\r\nfatalError: " + XMLFileReader.showSAX(exception));
      throw exception;
    }
    public void warning(SAXParseException exception) throws SAXException {
      System.out.println("\r\nwarning: " + XMLFileReader.showSAX(exception));
      throw exception;
    }
  }

  private void summarizeCoverage(PrintWriter summary, String commonDir) {
    System.out.println(commonDir);
    File commonDirectory = new File(commonDir);
    if (!commonDirectory.exists()) {
      System.out.println("Doesn't exist:\t" + commonDirectory);
    }
    for (File subdir : commonDirectory.listFiles()) {
      if (!subdir.isDirectory()) continue;
      System.out.println("\t" + subdir);
      for (File file : subdir.listFiles()) {
        if (!file.toString().endsWith(".xml")) continue;
        System.out.print(".");
        System.out.flush();
        //System.out.println("\t\t" + file);
        String name = subdir.getName() + "/" + file.getName();
        name = name.substring(0,name.length()-4); // strip .xml
        MyHandler handler = check(file, name);
        summary.println(name + "\t" + handler.valueCount + "\t" + handler.valueLen + "\t" + handler.attributeCount + "\t" + handler.attributeLen);
      }
      System.out.println();
    }
  }
}
