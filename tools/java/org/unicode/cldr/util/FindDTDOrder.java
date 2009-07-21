package org.unicode.cldr.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DeclHandler;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.dev.test.util.XEquivalenceClass;
import com.ibm.icu.dev.test.util.XEquivalenceMap;
import com.ibm.icu.impl.Differ;
import com.ibm.icu.text.UTF16;

public class FindDTDOrder implements DeclHandler, ContentHandler, ErrorHandler {
  private static final Pattern FIRST_LETTER_CHANGE = Pattern.compile("(\\S)\\S*");
  static final boolean SHOW_PROGRESS = CldrUtility.getProperty("verbose",false);
  static final boolean SHOW_ALL = CldrUtility.getProperty("show_all", false);

  private static FindDTDOrder INSTANCE;

  private boolean recordingAttributeElements;
  
  public static void main(String[] args) throws IOException {
    FindDTDOrder me = getInstance();
    me.showData();
  }

  public static FindDTDOrder getInstance() {
    synchronized (FindDTDOrder.class) {
      if (INSTANCE == null) {
        try {
          FindDTDOrder me = new FindDTDOrder();
          XMLReader xmlReader = CLDRFile.createXMLReader(true);
          xmlReader.setContentHandler(me);
          xmlReader.setErrorHandler(me);
          xmlReader.setProperty(
              "http://xml.org/sax/properties/declaration-handler", me);
  
          FileInputStream fis;
          InputSource is;
          me.recordingAttributeElements = true;
          String filename = CldrUtility.MAIN_DIRECTORY + "/root.xml";
          fis = new FileInputStream(filename);
          is = new InputSource(fis);
          is.setSystemId(filename);
          xmlReader.parse(is);
          fis.close();
  
          me.recordingAttributeElements = false;
          filename = CldrUtility.SUPPLEMENTAL_DIRECTORY
              + "/supplementalData.xml";
          fis = new FileInputStream(filename);
          is = new InputSource(fis);
          is.setSystemId(filename);
          xmlReader.parse(is);
          fis.close();
          me.attributeList = Collections.unmodifiableList(new ArrayList(me.attributeSet));
          me.checkData();
          me.orderingList = Collections.unmodifiableList(me.orderingList);
  
          //me.writeAttributeElements();
          INSTANCE = me;
        } catch (Exception e) {
          throw (IllegalArgumentException) new IllegalArgumentException().initCause(e);
        }
      }
    }
    return INSTANCE;
  }

  public void writeAttributeElements() {
    System.out.println(CldrUtility.LINE_SEPARATOR + "======== Start Attributes to Elements (unblocked) " + CldrUtility.LINE_SEPARATOR);
    for (String attribute : attributeToElements.keySet()) {
      Set<String> filtered = new TreeSet();
      for (String element : attributeToElements.getAll(attribute)) {
        if (!isBlocked(element)) {
          filtered.add(element);
        }
      }
      System.out.println(attribute + "\t" + CldrUtility.join(filtered, " "));
    }
    System.out.println(CldrUtility.LINE_SEPARATOR + "======== End Attributes to Elements" + CldrUtility.LINE_SEPARATOR);
    System.out.println(CldrUtility.LINE_SEPARATOR + "======== Start Elements to Children (skipping alias, special)" + CldrUtility.LINE_SEPARATOR);
    showElementTree("ldml", "", new HashSet<String>());
    System.out.println(CldrUtility.LINE_SEPARATOR + "======== End Elements to Children" + CldrUtility.LINE_SEPARATOR);
  }

  private void showElementTree(String element, String indent, HashSet<String> seenSoFar) {
    // skip blocking elements
    if (isBlocked(element)
     ) {
      return;
    }
    Set<String> children = elementToChildren.getAll(element);
    if (seenSoFar.contains(element)) {
      System.out.println(indent + element + (children == null || children.size() == 0 ? "" : "\t*dup*\t" + children));
      return;
    }
    System.out.println(indent + element);
    seenSoFar.add(element);
    if (children != null) {
      indent += "\t";
      for (String child : children) {
        showElementTree(child, indent, seenSoFar);
      }
    }
  }

  private boolean isBlocked(String element) {
    return isAncestorOf("supplementalData", element)
        || isAncestorOf("collation", element)
        || isAncestorOf("cldrTest", element)
        || isAncestorOf("transform", element);
  }
  
  Relation<String,String> ancestorToDescendant = null;
  
  private boolean isAncestorOf(String possibleAncestor, String possibleDescendent) {
    if (ancestorToDescendant == null) {
      ancestorToDescendant = new Relation(new TreeMap(), TreeSet.class);
      buildPairwiseRelations(new ArrayList(), "ldml");     
    }
    Set<String> possibleDescendents = ancestorToDescendant.getAll(possibleAncestor);
    if (possibleDescendents == null) return false;
    return possibleDescendents.contains(possibleDescendent);
  }

  private void buildPairwiseRelations(List<String> parents, String current) {
    Set<String> children = elementToChildren.getAll(current);
    if (children == null || children.size() == 0) return;
    
    // we make a new list, since otherwise the iteration fails in recursion (because of the modification)
    // if this were performance-sensitive we'd do it differently
    ArrayList<String> newParents = new ArrayList<String>(parents);
    newParents.add(current);
    
    for (String child : children) {
      for (String ancestor : newParents) {
        ancestorToDescendant.put(ancestor, child);
        buildPairwiseRelations(newParents, child);
      }
    }
  }

  PrintWriter log = null;

  Set elementOrderings = new LinkedHashSet(); // set of orderings

  Set allDefinedElements = new LinkedHashSet();

  boolean showReason = false;

  Object DONE = new Object(); // marker

  Relation<String, String> elementToChildren = new Relation(new TreeMap(),
      TreeSet.class);

  FindDTDOrder() {
    log = new PrintWriter(System.out);
  }

  private List orderingList = new ArrayList();

  public void checkData() {
    // verify that the ordering is the consistent for all child elements
    // do this by building an ordering from the lists.
    // The first item has no greater item in any set. So find an item that is
    // only first
    MergeLists<String> mergeLists = new MergeLists<String>(new TreeSet(new UTF16.StringComparator(true, false, 0)))
    .add(Arrays.asList("ldml"))
    .addAll(elementOrderings); // 
    List<String> result = mergeLists.merge();
    Collection badOrder = MergeLists.hasConsistentOrderWithEachOf(result, elementOrderings);
    if (badOrder != null) {
      throw new IllegalArgumentException("Failed to find good order: " + badOrder);
    }

    showReason = false;
    orderingList.add("ldml");
    if (SHOW_PROGRESS)
      log.println("SHOW_PROGRESS ");
    for (Iterator it = elementOrderings.iterator(); it.hasNext();) {
      Object value = it.next();
      log.println(value);
    }
    while (true) {
      Object first = getFirst();
      if (first == DONE)
        break;
      if (first != null) {
        // log.println("Adding:\t" + first);
        if (orderingList.contains(first)) {
          throw new IllegalArgumentException("Already present: " + first);
        }
        orderingList.add(first);
      } else {
        showReason = true;
        getFirst();
        if (SHOW_PROGRESS)
          log.println();
        if (SHOW_PROGRESS)
          log.println("Failed ordering. So far:");
        for (Iterator it = orderingList.iterator(); it.hasNext();)
          if (SHOW_PROGRESS)
            log.print("\t" + it.next());
        if (SHOW_PROGRESS)
          log.println();
        if (SHOW_PROGRESS)
          log.println("Items:");
        // for (Iterator it = element_childComparator.keySet().iterator();
        // it.hasNext();) showRow(it.next(), true);
        if (SHOW_PROGRESS)
          log.println();
        break;
      }
    }
    System.out.println("New code: " + result);
    System.out.println("Old code: " + orderingList);
    System.out.println("New code2: " + CldrUtility.breakLines(CldrUtility.join(result, " "), sep, FIRST_LETTER_CHANGE.matcher(""), 80));
    
    Set missing = new TreeSet(allDefinedElements);
    missing.removeAll(orderingList);
    orderingList.addAll(missing);

    attributeEquivalents = new XEquivalenceClass(null);
    for (Iterator it = attribEquiv.keySet().iterator(); it.hasNext();) {
      Object ename = it.next();
      Set s = (Set) attribEquiv.get(ename);
      Iterator it2 = s.iterator();
      Object first = it2.next();
      while (it2.hasNext()) {
        attributeEquivalents.add(first, it2.next(), ename);
      }
    }
    
  }
  
  String sep = CldrUtility.LINE_SEPARATOR + "\t\t\t";

  private void showData() throws IOException {
    // finish up
    log.println("Successful Ordering");
    log.print("Old Attributes: ");
    log.println(CLDRFile.attributeOrdering.getOrder());
    
    log.print("*** New Attributes: ");
    log.println(breakLines(attributeSet));
    log.println("*** Replace in supplementalMetadata attributeOrder and in CLDRFile attributeOrdering ***");
    
    log.println("Attribute Eq: ");
    for (Iterator it = attributeEquivalents.getSamples().iterator(); it.hasNext();) {
      log.println("\t"
          + getJavaList(new TreeSet(attributeEquivalents.getEquivalences(it.next()))));
    }
    for (Iterator it = attributeEquivalents.getEquivalenceSets().iterator(); it.hasNext();) {
      Object last = null;
      Set s = (Set) it.next();
      for (Iterator it2 = s.iterator(); it2.hasNext();) {
        Object temp = it2.next();
        if (last != null)
          log.println(last + " ~ " + temp + "\t" + attributeEquivalents.getReasons(last, temp));
        last = temp;
      }
      log.println();
    }

    String oldOrder = getJavaList(CLDRFile.elementOrdering.getOrder());
    log.println("Old Element Ordering: "
        + oldOrder);
    
    String newOrder = '"'+breakLines(orderingList)+'"';
    if(newOrder.equals(oldOrder)) {
        log.println(" *** New Element Ordering: <same>");
    } else {
        log.println("*** New Element Ordering: " + newOrder);// getJavaList(orderingList));
    }
    log.println("*** Replace in supplementalMetadata elementOrder and in CLDRFile elementOrdering ***");

    log.println("Old Size: " + CLDRFile.elementOrdering.getOrder().size());
    Set temp = new HashSet(CLDRFile.elementOrdering.getOrder());
    temp.removeAll(orderingList);
    log.println("Old - New: " + temp);
    log.println("New Size: " + orderingList.size());
    temp = new HashSet(orderingList);
    temp.removeAll(CLDRFile.elementOrdering.getOrder());
    log.println("New - Old: " + temp);

    Differ differ = new Differ(200, 1);
    Iterator oldIt = CLDRFile.elementOrdering.getOrder().iterator();
    Iterator newIt = orderingList.iterator();
    while (oldIt.hasNext() || newIt.hasNext()) {
      if (oldIt.hasNext())
        differ.addA(oldIt.next());
      if (newIt.hasNext())
        differ.addB(newIt.next());
      differ.checkMatch(!oldIt.hasNext() && !newIt.hasNext());

      if (differ.getACount() != 0 || differ.getBCount() != 0) {
        log.println("Same: " + differ.getA(-1));
        for (int i = 0; i < differ.getACount(); ++i) {
          log.println("\tOld: " + differ.getA(i));
        }
        for (int i = 0; i < differ.getBCount(); ++i) {
          log.println("\t\tNew: " + differ.getB(i));
        }
        log.println("Same: " + differ.getA(differ.getACount()));
      }
    }
    log.println("Done with differences");

    log.flush();
    
    Log.setLogNoBOM(CldrUtility.GEN_DIRECTORY + "/supplemental/supplementalMetadata.xml");
    BufferedReader oldFile = BagFormatter.openUTF8Reader(CldrUtility.SUPPLEMENTAL_DIRECTORY+'/', "supplementalMetadata.xml");
    String sep = CldrUtility.LINE_SEPARATOR + "\t\t\t";

    CldrUtility.copyUpTo(oldFile, Pattern.compile("\\s*<attributeOrder>\\s*"), Log.getLog(), true);
    Log.println(sep + breakLines(attributeSet) + sep + "</attributeOrder>");
    CldrUtility.copyUpTo(oldFile, Pattern.compile("\\s*</attributeOrder>\\s*"), null, true);
    
    CldrUtility.copyUpTo(oldFile, Pattern.compile("\\s*<elementOrder>\\s*"), Log.getLog(), true);
    Log.println(sep + breakLines(orderingList) + sep + "</elementOrder>");
    CldrUtility.copyUpTo(oldFile, Pattern.compile("\\s*</elementOrder>\\s*"), null, true);

    CldrUtility.copyUpTo(oldFile, null, Log.getLog(), false);

    Log.close();
    oldFile.close();
  }

  private String breakLines(Collection orderingList) {
    final String joined = CldrUtility.join(orderingList," ");
    return joined; // return Utility.breakLines(joined, sep, FIRST_LETTER_CHANGE.matcher(""), 80);
  }

  private String getJavaList(Collection orderingList) {
    boolean first2 = true;
    StringBuffer result = new StringBuffer();
    result.append('"');
    for (Iterator it = orderingList.iterator(); it.hasNext();) {
      if (first2)
        first2 = false;
      else
        result.append(" ");
      result.append(it.next().toString());
    }
    result.append('"');
    return result.toString();
  }

  /**
   * @param parent
   * @param skipEmpty
   *          TODO
   */
  // private void showRow(Object parent, boolean skipEmpty) {
  // List items = (List) element_childComparator.get(parent);
  // if (skipEmpty && items.size() == 0) return;
  // if (SHOW_PROGRESS) log.print(parent);
  // for (Iterator it2 = items.iterator(); it2.hasNext();) if (SHOW_PROGRESS)
  // log.print("\t" + it2.next());
  // if (SHOW_PROGRESS) log.println();
  // }
  /**
   * @param orderingList
   */
  private Object getFirst() {
    Set firstItems = new TreeSet();
    Set nonFirstItems = new TreeSet();
    for (Iterator it = elementOrderings.iterator(); it.hasNext();) {
      List list = (List) it.next();
      if (list.size() != 0) {
        firstItems.add(list.get(0));
        for (int i = 1; i < list.size(); ++i) {
          nonFirstItems.add(list.get(i));
        }
      }
    }
    if (firstItems.size() == 0 && nonFirstItems.size() == 0)
      return DONE;
    firstItems.removeAll(nonFirstItems);
    if (firstItems.size() == 0)
      return null; // failure
    Object result = firstItems.iterator().next();
    removeEverywhere(result);
    return result;
  }

  /**
   * @param possibleFirst
   */
  private void removeEverywhere(Object possibleFirst) {
    // and remove from all the lists
    for (Iterator it2 = elementOrderings.iterator(); it2.hasNext();) {
      List list2 = (List) it2.next();
      if (SHOW_PROGRESS && list2.contains(possibleFirst)) {
        log.println("Removing " + possibleFirst + " from " + list2);
      }
      while (list2.remove(possibleFirst))
        ; // repeat until returns false
    }
  }

  // private boolean isNeverNotFirst(Object possibleFirst) {
  // if (showReason) if (SHOW_PROGRESS) log.println("Trying: " + possibleFirst);
  // for (Iterator it2 = element_childComparator.keySet().iterator();
  // it2.hasNext();) {
  // Object key = it2.next();
  // List list2 = (List) element_childComparator.get(key);
  // int pos = list2.indexOf(possibleFirst);
  // if (pos > 0) {
  // if (showReason) {
  // if (SHOW_PROGRESS) log.print("Failed at:\t");
  // showRow(key, false);
  // }
  // return false;
  // }
  // }
  // return true;
  // }

  static final Set ELEMENT_SKIP_LIST = new HashSet(Arrays.asList(new String[] {
      "collation", "base", "settings", "suppress_contractions", "optimize",
      "rules", "reset", "context", "p", "pc", "s", "sc", "t", "tc", "q", "qc",
      "i", "ic", "extend", "x" }));

  static final Set SUBELEMENT_SKIP_LIST = new HashSet(Arrays
      .asList(new String[] { "PCDATA", "EMPTY", "ANY" }));

  // refine later; right now, doesn't handle multiple elements well.
  public void elementDecl(String name, String model) throws SAXException {
    // if (ELEMENT_SKIP_LIST.contains(name)) return;
    if (name.indexOf("contractions") >= 0
        || model
            .indexOf("[alias, base, settings, suppress, contractions, optimize, rules, special]") >= 0) {
      System.out.println("debug");
    }
    allDefinedElements.add(name);
    if (SHOW_PROGRESS) {
      log.println("Element\t" + name + "\t" + model);
    }
    String[] list = model.split("[^-_A-Z0-9a-z]+");
    List mc = new ArrayList();
    /*
     * if (name.equals("currency")) { mc.add("alias"); mc.add("symbol");
     * mc.add("pattern"); }
     */
    for (int i = 0; i < list.length; ++i) {
      if (list[i].length() == 0)
        continue;
      if (list[i].equals("ANY") && !name.equals("special")) {
        System.err.println("WARNING- SHOULD NOT HAVE 'ANY': " + name + "\t"
            + model);
      }
      if (SUBELEMENT_SKIP_LIST.contains(list[i]))
        continue;
      // if (SHOW_PROGRESS) log.print("\t" + list[i]);
      if (mc.contains(list[i])) {
        if (name.equals("currency") && list[i].equals("displayName") || list[i].equals("symbol") || list[i].equals("pattern")) {
          // do nothing, exception
        } else if (name.equals("rules") && list[i].equals("reset")) {
          // do nothing, exception
        } else {
          throw new IllegalArgumentException ("Duplicate element in definition of  " + name
              + ":\t" + list[i] + ":\t" + Arrays.asList(list) + ":\t" + mc);
        }
      } else {
        mc.add(list[i]);
      }
    }
    if (recordingAttributeElements) {
      Set children = new TreeSet(mc);
      children.remove("alias");
      children.remove("special");
      children.remove("cp");
      elementToChildren.putAll(name, children);
    }
    allDefinedElements.addAll(mc);

    if (mc.size() < 1) {
      if (SHOW_PROGRESS) {
        log.println("\tSKIPPING\t" + name + "\t" + mc);
      }
    } else {
      if (SHOW_PROGRESS) {
        log.println("\t" + name + "\t" + mc);
      }
      elementOrderings.add(mc);
    }

    // if (SHOW_PROGRESS) log.println();
  }

  Set skipCommon = new LinkedHashSet(Arrays.asList(new String[] {"validSubLocales", 
      "standard", "references",
      "alt", "draft",
      }));

  Set attributeSet = new TreeSet();
  {
    attributeSet.add("_q");
  }
  List attributeList;

  TreeMap attribEquiv = new TreeMap();

  Relation<String, String> attributeToElements = new Relation(new TreeMap(),
      TreeSet.class);
  private XEquivalenceClass attributeEquivalents;

  public void attributeDecl(String eName, String aName, String type,
      String mode, String value) throws SAXException {
    if (SHOW_ALL)
      log.println("attributeDecl");
    // if (SHOW_ALL) log.println("Attribute\t" + eName + "\t" +
    // aName + "\t" + type + "\t" + mode + "\t" + value);
    if (SHOW_PROGRESS) System.out.println("Attribute\t" + eName + "\t" + aName + "\t" + type
        + "\t" + mode + "\t" + value);
    if (!skipCommon.contains(aName)) {
      attributeSet.add(aName);
      Set l = (Set) attribEquiv.get(eName);
      if (l == null)
        attribEquiv.put(eName, l = new TreeSet());
      l.add(aName);
    }
    if (recordingAttributeElements) {
      attributeToElements.put(aName, eName);
    }
  }

  public void internalEntityDecl(String name, String value) throws SAXException {
    if (SHOW_ALL)
      log.println("internalEntityDecl");
    // if (SHOW_ALL) log.println("Internal Entity\t" + name +
    // "\t" + value);
  }

  public void externalEntityDecl(String name, String publicId, String systemId)
      throws SAXException {
    if (SHOW_ALL)
      log.println("externalEntityDecl");
    // if (SHOW_ALL) log.println("Internal Entity\t" + name +
    // "\t" + publicId + "\t" + systemId);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#endDocument()
   */
  public void endDocument() throws SAXException {
    if (SHOW_ALL)
      log.println("endDocument");
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#startDocument()
   */
  public void startDocument() throws SAXException {
    if (SHOW_ALL)
      log.println("startDocument");
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#characters(char[], int, int)
   */
  public void characters(char[] ch, int start, int length) throws SAXException {
    if (SHOW_ALL)
      log.println("characters");
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
   */
  public void ignorableWhitespace(char[] ch, int start, int length)
      throws SAXException {
    if (SHOW_ALL)
      log.println("ignorableWhitespace");
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#endPrefixMapping(java.lang.String)
   */
  public void endPrefixMapping(String prefix) throws SAXException {
    if (SHOW_ALL)
      log.println("endPrefixMapping");
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#skippedEntity(java.lang.String)
   */
  public void skippedEntity(String name) throws SAXException {
    if (SHOW_ALL)
      log.println("skippedEntity");
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
   */
  public void setDocumentLocator(Locator locator) {
    if (SHOW_ALL)
      log.println("setDocumentLocator");
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#processingInstruction(java.lang.String,
   *      java.lang.String)
   */
  public void processingInstruction(String target, String data)
      throws SAXException {
    if (SHOW_ALL)
      log.println("processingInstruction");
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#startPrefixMapping(java.lang.String,
   *      java.lang.String)
   */
  public void startPrefixMapping(String prefix, String uri) throws SAXException {
    if (SHOW_ALL)
      log.println("startPrefixMapping");
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#endElement(java.lang.String,
   *      java.lang.String, java.lang.String)
   */
  public void endElement(String namespaceURI, String localName, String qName)
      throws SAXException {
    if (SHOW_ALL)
      log.println("endElement");
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#startElement(java.lang.String,
   *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
   */
  public void startElement(String namespaceURI, String localName, String qName,
      Attributes atts) throws SAXException {
    if (SHOW_ALL)
      log.println("startElement");
  }

  /* (non-Javadoc)
   * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
   */
  public void error(SAXParseException exception) throws SAXException {
    if (SHOW_ALL)
      log.println("error");
    throw exception;
  }

  /* (non-Javadoc)
   * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
   */
  public void fatalError(SAXParseException exception) throws SAXException {
    if (SHOW_ALL)
      log.println("fatalError");
    throw exception;
  }

  /* (non-Javadoc)
   * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
   */
  public void warning(SAXParseException exception) throws SAXException {
    if (SHOW_ALL)
      log.println("warning");
    throw exception;
  }

  public List<String> getAttributeOrder() {
    return attributeList;
  }

  public List<String> getElementOrder() {
    return orderingList;
  }

  public Set<String> getCommonAttributes() {
    return skipCommon;
  }

}