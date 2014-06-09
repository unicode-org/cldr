package org.unicode.cldr.test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.ApproximateWidth;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.NodeListIterator;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.VariableAndPathParser;
import org.unicode.cldr.util.VariableAndPathParser.ExtractablePath;
import org.unicode.cldr.util.VariableAndPathParser.ExtractableVariable;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CheckWidths extends CheckCLDR {
    /**
     * Set to TRUE to read the constants for the RegexLookup from a file
     */
    private static final boolean READ_FROM_FILE = true;
    /**
     * Path of the width specification file, relative to the data directory
     */
    private static final String WIDTH_SPECIFICATION_FILE = "test/widthSpecification.xml";

    // remember to add this class to the list in CheckCLDR.getCheckAll
    // to run just this test, on just locales starting with 'nl', use CheckCLDR with -fnl.* -t.*CheckWidths.*

    private static final double EM = ApproximateWidth.getWidth("月");

    private static final boolean DEBUG = true;

    private enum Measure {
        CODE_POINTS, DISPLAY_WIDTH
    }

    private enum LimitType {
        MINIMUM, MAXIMUM
    }

    private enum Special {
        NONE, QUOTES, PLACEHOLDERS, NUMBERSYMBOLS
    }

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\d\\}");

    private static class Limit {
        final double warningReference;
        final double errorReference;
        final LimitType limit;
        final Measure measure;
        final Special special;
        final String message;
        final Subtype subtype;
        final boolean debug;

        public Limit(double warningReference, double errorReference, Measure measure, LimitType limit, Special special, boolean debug) {
            this.debug = debug;
            this.warningReference = warningReference;
            this.errorReference = errorReference;
            this.limit = limit;
            this.measure = measure;
            this.special = special;
            switch (limit) {
            case MINIMUM:
                this.message = measure == Measure.CODE_POINTS
                    ? "Expected no fewer than {0} character(s), but was {1}."
                    : "Too narrow by about {2}% (with common fonts).";
                this.subtype = Subtype.valueTooNarrow;
                break;
            case MAXIMUM:
                this.message = measure == Measure.CODE_POINTS
                    ? "Expected no more than {0} character(s), but was {1}."
                    : "Too wide by about {2}% (with common fonts).";
                this.subtype = Subtype.valueTooWide;
                break;
            default:
                throw new IllegalArgumentException();
            }
        }

        public Limit(double d, double e, Measure displayWidth, LimitType maximum, Special placeholders) {
            this(d, e, displayWidth, maximum, placeholders, false);
        }

        boolean hasProblem(String value, List<CheckStatus> result, CheckCLDR cause) {
            switch (special) {
            case QUOTES:
                value = value.replace("'", "");
                break;
            case PLACEHOLDERS:
                value = PLACEHOLDER_PATTERN.matcher(value).replaceAll("");
                break;
            case NUMBERSYMBOLS:
                value = value.replaceAll("[\u200E\u200F]", ""); // don't include LRM/RLM when checking length of number symbols
                break;
            case NONE:
                break; // do nothing
            }
            double valueMeasure = measure == Measure.CODE_POINTS ? value.codePointCount(0, value.length()) : ApproximateWidth.getWidth(value);
            CheckStatus.Type errorType = CheckStatus.warningType;
            switch (limit) {
            case MINIMUM:
                if (valueMeasure >= warningReference) {
                    return false;
                }
                if (valueMeasure < errorReference && cause.getPhase() != Phase.BUILD) {
                    errorType = CheckStatus.errorType;
                }
                break;
            case MAXIMUM:
                if (valueMeasure <= warningReference) {
                    return false;
                }
                if (valueMeasure > errorReference && cause.getPhase() != Phase.BUILD) {
                    errorType = CheckStatus.errorType;
                }
                break;
            }
            // the 115 is so that we don't show small percentages
            // the /10 ...*10 is to round to multiples of 10% percent
            double percent = (int) (Math.abs(115 * valueMeasure / warningReference - 100.0d) / 10 + 0.49999d) * 10;
            result.add(new CheckStatus().setCause(cause)
                .setMainType(errorType)
                .setSubtype(subtype)
                .setMessage(message, warningReference, valueMeasure, percent));
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            long temp;
            temp = Double.doubleToLongBits(errorReference);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            result = prime * result + ((limit == null) ? 0 : limit.hashCode());
            result = prime * result + ((measure == null) ? 0 : measure.hashCode());
            result = prime * result + ((message == null) ? 0 : message.hashCode());
            result = prime * result + ((special == null) ? 0 : special.hashCode());
            result = prime * result + ((subtype == null) ? 0 : subtype.hashCode());
            temp = Double.doubleToLongBits(warningReference);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Limit other = (Limit) obj;
            if (Double.doubleToLongBits(errorReference) != Double.doubleToLongBits(other.errorReference)) {
                return false;
            }
            if (limit != other.limit) {
                return false;
            }
            if (measure != other.measure) {
                return false;
            }
            if (message == null) {
                if (other.message != null) {
                    return false;
                }
            } else if (!message.equals(other.message)) {
                return false;
            }
            if (special != other.special) {
                return false;
            }
            if (subtype != other.subtype) {
                return false;
            }
            if (Double.doubleToLongBits(warningReference) != Double.doubleToLongBits(other.warningReference)) {
                return false;
            }
            return true;
        }

    }

    // WARNING: errors must occur before warnings!!
    // we allow unusual units and English units to be a little longer
    static final String ALLOW_LONGER = "(area-acre" +
        "|area-square-foot" +
        "|area-square-mile" +
        "|length-foot" +
        "|length-inch" +
        "|length-mile" +
        "|length-light-year" +
        "|length-yard" +
        "|mass-ounce" +
        "|mass-pound" +
        "|power-horsepower" +
        "|pressure-inch-hg" +
        "|speed-mile-per-hour" +
        "|temperature-fahrenheit" +
        "|volume-cubic-mile" +
        "|acceleration-g-force" +
        "|speed-kilometer-per-hour" +
        "|speed-meter-per-second" +
        ")";

    static boolean LOOKUP_ASSEMBLED = false;

    private final static Object INITIALIZE_LOKUP_LOCK = new Object();

    static RegexLookup<Limit[]> lookup = new RegexLookup<Limit[]>()
        .setPatternTransform(RegexLookup.RegexFinderTransformPath);
    /*
     .addVariable("%A", "\"[^\"]+\"")
     .add("//ldml/delimiters/(quotation|alternateQuotation)", new Limit[] {
         new Limit(1, 1, Measure.CODE_POINTS, LimitType.MAXIMUM, Special.NONE)
     })

     // Numeric items should be no more than a single character

     .add("//ldml/numbers/symbols[@numberSystem=%A]/(decimal|group|minus|percent|perMille|plus)", new Limit[] {
         new Limit(1, 1, Measure.CODE_POINTS, LimitType.MAXIMUM, Special.NUMBERSYMBOLS)
     })

     // Now widths
     // The following are rough measures, just to check strange cases

     .add("//ldml/characters/ellipsis[@type=\"(final|initial|medial)\"]", new Limit[] {
         new Limit(2 * EM, 5 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
     })

     .add("//ldml/localeDisplayNames/localeDisplayPattern/", new Limit[] { // {0}: {1}, {0} ({1}), ,
         new Limit(2 * EM, 3 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
     })

     .add("//ldml/listPatterns/listPattern/listPatternPart[@type=%A]", new Limit[] { // {0} and {1}
         new Limit(5 * EM, 10 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
     })

     .add("//ldml/dates/timeZoneNames/fallbackFormat", new Limit[] { // {1} ({0})
         new Limit(2 * EM, 3 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
     })

     .add("//ldml/dates/timeZoneNames/(regionFormat|hourFormat)", new Limit[] { // {0} Time,
         // +HH:mm;-HH:mm
         new Limit(10 * EM, 20 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
     })

     .add("//ldml/dates/timeZoneNames/(gmtFormat|gmtZeroFormat)", new Limit[] { // GMT{0}, GMT
         new Limit(5 * EM, 10 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
     })

     // Narrow items

     .add("//ldml/dates/calendars/calendar.*[@type=\"narrow\"](?!/cyclic|/dayPeriod|/monthPattern)", new Limit[] {
         new Limit(1.5 * EM, 2.25 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.NONE)
     })
     // \"(?!am|pm)[^\"]+\"\\

     // Compact number formats

     .add("//ldml/numbers/decimalFormats[@numberSystem=%A]/decimalFormatLength[@type=\"short\"]/decimalFormat[@type=%A]/pattern[@type=\"1",
         new Limit[] {
         new Limit(4 * EM, 5 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.QUOTES)
         })
     // Catch -future/past Narrow units  and allow much wider values
     .add("//ldml/units/unitLength[@type=\"narrow\"]/unit[@type=\"[^\"]+-(future|past)\"]/unitPattern", new Limit[] {
         new Limit(10 * EM, 15 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
     })
     // Catch special units and allow a bit wider
     .add("//ldml/units/unitLength[@type=\"narrow\"]/unit[@type=\"" + ALLOW_LONGER + "\"]/unitPattern", new Limit[] {
         new Limit(4 * EM, 5 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
     })
     // Narrow units
     .add("//ldml/units/unitLength[@type=\"narrow\"]/unit[@type=%A]/unitPattern", new Limit[] {
         new Limit(3 * EM, 4 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
     })
     // Short units
     .add("//ldml/units/unitLength[@type=\"short\"]/unit[@type=%A]/unitPattern", new Limit[] {
         new Limit(5 * EM, 10 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
     })

     // Currency Symbols
     .add("//ldml/numbers/currencies/currency[@type=%A]/symbol", new Limit[] {
         new Limit(3 * EM, 5 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
     });  */

    Set<Limit> found = new LinkedHashSet<Limit>();

    /**
     * Class controlling variable and path extraction
     * @author ribnitz
     *
     */
    private static class CheckWidthPathAndVariableExtractor implements
        ExtractablePath<String, Limit>, ExtractableVariable {
        private static CheckWidthPathAndVariableExtractor instance = null;

        private CheckWidthPathAndVariableExtractor() {
        }

        public static CheckWidthPathAndVariableExtractor getInstance() {
            synchronized (CheckWidthPathAndVariableExtractor.class) {
                if (instance == null) {
                    instance = new CheckWidthPathAndVariableExtractor();
                }
                return instance;
            }
        }

        /**
         * Given a node, extract a variable definition, and return it as a Map.Entry
         * @param aNode - the node to process
         * @return a Map.Entry with the variable name as key, and its value as value
         */
        @Override
        public Map.Entry<String, String> extractVariable(Node aNode) {
            // the current node has two attributes, name and value
            if (aNode.hasAttributes()) {
                NamedNodeMap nodeMap = aNode.getAttributes();
                Node nameNode = nodeMap.getNamedItem("name");
                String varName = nameNode.getNodeValue();
                Node valNode = nodeMap.getNamedItem("value");
                String varVal = valNode.getNodeValue();
                return new SimpleImmutableEntry<String, String>(varName, varVal);
            }
            return null;
        }

        /**
         * Given a Node, returns the value of the first child, if existent, or null otherwise 
         * @param aNode
         * @return
         */
        private String getValueOfFirstChild(Node aNode) {
            if (aNode != null && aNode.hasChildNodes()) {
                Node firstChild = aNode.getFirstChild();
                return firstChild.getNodeValue();
            }
            return null;
        }

        /**
         * Treat a node which contains a double value, but which may contain an attribute, which will 
         * change the calculation of that value, if set to true.
         * @param curChild
         * @return
         */
        private double extractErrorOrWarningValue(Node curChild) {
            String cs = getValueOfFirstChild(curChild);
            // the current child is supposed to have one child, which holds a numeric value
            double d = cs == null ? 0 : Double.parseDouble(cs);
            double multiplier = 1;
            if (curChild.hasAttributes()) {
                Node relAttr = curChild.getAttributes().getNamedItem("relativeToEM");
                String relVal = relAttr.getNodeValue();
                if (relVal != null && !relVal.isEmpty()) {
                    if (relVal.equalsIgnoreCase("true") || relVal.equals("1")) {
                        multiplier = EM;
                    }
                }
            }
            return d * multiplier;
        }

        /**
         * Given a DOM Node, extract the information contained therein, and return it as a Key-Value pair
         * @param aNode the DOM Node to extract from
         * @return a Key/Value pair with the key and the data
         */
        @Override
        public Map.Entry<String, Limit> extractPath(Node aNode) {
            double errRef = 0;
            double warnRef = 0;
            LimitType curLim = null;
            Measure measure = null;
            String curPath = null;
            Special special = null;
            // iterate through the children
            if (aNode.hasChildNodes()) {
                NodeList children = aNode.getChildNodes();
                if (children != null && children.getLength() > 0) {
                    Iterator<Node> childNodeIter = new NodeListIterator(children);
                    while (childNodeIter.hasNext()) {
                        Node curChild = childNodeIter.next();
                        if (curChild instanceof Element) {
                            Element el = (Element) curChild;
                            String tagname = el.getTagName();
                            String nodeVal = null;
                            // Switch on String requires Java 7
                            switch (tagname) {
                            case "errorReference":
                                errRef = extractErrorOrWarningValue(curChild);
                                break;
                            case "warningReference":
                                warnRef = extractErrorOrWarningValue(curChild);
                                break;
                            case "limit":
                                // the current child has a child (Text node) containing 
                                // the value
                                nodeVal = getValueOfFirstChild(curChild);
                                curLim = LimitType.valueOf(nodeVal);
                                break;
                            case "measure":
                                nodeVal = getValueOfFirstChild(curChild);
                                measure = Measure.valueOf(nodeVal);
                                break;
                            case "special":
                                nodeVal = getValueOfFirstChild(curChild);
                                special = Special.valueOf(nodeVal);
                                break;
                            case "pathName":
                                nodeVal = getValueOfFirstChild(curChild);
                                curPath = nodeVal;
                                break;
                            }
                        }
                    }
                    // construct a limit that we can associate with the path
                    Limit l = new Limit(warnRef, errRef, measure, curLim, special);
                    return new SimpleImmutableEntry<String, Limit>(curPath, l);
                }
            }
            return null;
        }
    }

    private static interface LookupInitializable {
        void initialize();
    }

    /**
     * Initializer that hold an object it can synchronize on, for initialization 
     * @author ribnitz
     *
     */
    private static abstract class SynchronizingLookupInitializer implements LookupInitializable {
        /**
         * Object subclasses can use for synchronization in initialize
         */
        protected final Object syncObject;

        public SynchronizingLookupInitializer(Object aLock) {
            syncObject = aLock;
        }
    }

    /**
     * Initializer that whose values are obtained through a Reader
     * @author ribnitz
     *
     */
    private static class StreamBasedLookupInitializer extends SynchronizingLookupInitializer {
        /**
         * XPath expression used to access the Paths; this is supposed to return a NodeList
         */
        private static final String XPATH_PATHS = "//paths/path";

        /**
         * XPath expression used to access the variables; this is supposed to return a NodeList
         */
        private static final String XPATH_VARIABLES = "//variables/variable";

        /**
         * Internal buffer used for parsing
         */
        private final byte[] buf;

        public StreamBasedLookupInitializer(Object aLock, Reader in) throws IOException {
            super(aLock);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(in)) {
                String s = null;
                while ((s = br.readLine()) != null) {
                    sb.append(s);
                }
            }
            buf = sb.toString().getBytes();
        }

        @Override
        public void initialize() {
            synchronized (syncObject) {
                if (!LOOKUP_ASSEMBLED) {
                    CheckWidthPathAndVariableExtractor exractor = CheckWidthPathAndVariableExtractor.getInstance();
                    try (Reader rdr = new InputStreamReader(new ByteArrayInputStream(buf))) {
                        VariableAndPathParser<String, Limit> vpp = new VariableAndPathParser<>(rdr);
                        vpp.setVariableExtractor(exractor);
                        vpp.setPathExtractor(exractor);
                        Map<String, String> variablesReadFromFile = vpp.getVariables(XPATH_VARIABLES);
                        if (!variablesReadFromFile.isEmpty()) {
                            // add the variables
                            Iterator<String> iter = variablesReadFromFile.keySet().iterator();
                            while (iter.hasNext()) {
                                String varKey = iter.next();
                                String varVal = variablesReadFromFile.get(varKey);
                                lookup.addVariable(varKey, varVal);
                            }
                        }
                        Map<String, Limit> pathsReadFromFile = vpp.getPaths(XPATH_PATHS);
                        if (!pathsReadFromFile.isEmpty()) {
                            Iterator<String> iter = pathsReadFromFile.keySet().iterator();
                            while (iter.hasNext()) {
                                String curKey = iter.next();
                                Limit curVal = pathsReadFromFile.get(curKey);
                                lookup.add(curKey, new Limit[] { curVal });
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Initializer that uses static values
     * @author ribnitz
     *
     */
    private static class StaticValueLookupInitializer extends SynchronizingLookupInitializer {

        public StaticValueLookupInitializer(Object aLock) {
            super(aLock);
        }

        @Override
        public void initialize() {
            synchronized (syncObject) {
                lookup.addVariable("%A", "\"[^\"]+\"")
                    .add("//ldml/delimiters/(quotation|alternateQuotation)", new Limit[] {
                        new Limit(1, 1, Measure.CODE_POINTS, LimitType.MAXIMUM, Special.NONE)
                    })

                    // Numeric items should be no more than a single character

                    .add("//ldml/numbers/symbols[@numberSystem=%A]/(decimal|group|minus|percent|perMille|plus)", new Limit[] {
                        new Limit(1, 1, Measure.CODE_POINTS, LimitType.MAXIMUM, Special.NUMBERSYMBOLS)
                    })

                    // Now widths
                    // The following are rough measures, just to check strange cases

                    .add("//ldml/characters/ellipsis[@type=\"(final|initial|medial)\"]", new Limit[] {
                        new Limit(2 * EM, 5 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
                    })

                    .add("//ldml/localeDisplayNames/localeDisplayPattern/", new Limit[] { // {0}: {1}, {0} ({1}), ,
                        new Limit(2 * EM, 3 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
                    })

                    .add("//ldml/listPatterns/listPattern/listPatternPart[@type=%A]", new Limit[] { // {0} and {1}
                        new Limit(5 * EM, 10 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
                    })

                    .add("//ldml/dates/timeZoneNames/fallbackFormat", new Limit[] { // {1} ({0})
                        new Limit(2 * EM, 3 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
                    })

                    .add("//ldml/dates/timeZoneNames/(regionFormat|hourFormat)", new Limit[] { // {0} Time,
                        // +HH:mm;-HH:mm
                        new Limit(10 * EM, 20 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
                    })

                    .add("//ldml/dates/timeZoneNames/(gmtFormat|gmtZeroFormat)", new Limit[] { // GMT{0}, GMT
                        new Limit(5 * EM, 10 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
                    })

                    // Narrow items

                    .add("//ldml/dates/calendars/calendar.*[@type=\"narrow\"](?!/cyclic|/dayPeriod|/monthPattern)", new Limit[] {
                        new Limit(1.5 * EM, 2.25 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.NONE)
                    })
                    // \"(?!am|pm)[^\"]+\"\\

                    // Compact number formats

                    .add("//ldml/numbers/decimalFormats[@numberSystem=%A]/decimalFormatLength[@type=\"short\"]/decimalFormat[@type=%A]/pattern[@type=\"1",
                        new Limit[] {
                        new Limit(4 * EM, 5 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.QUOTES)
                        })
                    // Catch -future/past Narrow units  and allow much wider values
                    .add("//ldml/units/unitLength[@type=\"narrow\"]/unit[@type=\"[^\"]+-(future|past)\"]/unitPattern", new Limit[] {
                        new Limit(10 * EM, 15 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
                    })
                    // Catch special units and allow a bit wider
                    .add("//ldml/units/unitLength[@type=\"narrow\"]/unit[@type=\"" + ALLOW_LONGER + "\"]/unitPattern", new Limit[] {
                        new Limit(4 * EM, 5 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
                    })
                    // Narrow units
                    .add("//ldml/units/unitLength[@type=\"narrow\"]/unit[@type=%A]/unitPattern", new Limit[] {
                        new Limit(3 * EM, 4 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
                    })
                    // Short units
                    .add("//ldml/units/unitLength[@type=\"short\"]/unit[@type=%A]/unitPattern", new Limit[] {
                        new Limit(5 * EM, 10 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
                    })

                    // Currency Symbols
                    .add("//ldml/numbers/currencies/currency[@type=%A]/symbol", new Limit[] {
                        new Limit(3 * EM, 5 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
                    });
            }
        }

    }

    @SuppressWarnings("rawtypes")
    public CheckCLDR handleCheck(String path, String fullPath, String value, Options options,
        List<CheckStatus> result) {
        if (value == null) {
            return this; // skip
        }
        //        String testPrefix = "//ldml/units/unitLength[@type=\"narrow\"]";
        //        if (path.startsWith(testPrefix)) {
        //            int i = 0;
        //        }
        // Limits item0 =
        // lookup.get("//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"short\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"1000000000\"][@count=\"other\"]");
        // item0.check("123456789", result, this);

        synchronized (INITIALIZE_LOKUP_LOCK) {
            if (!LOOKUP_ASSEMBLED) {
                LookupInitializable initializer = null;
                if (READ_FROM_FILE) {
                    //  lookup the variables etc. form a File
                    try (Reader is = CldrUtility.getUTF8Data(WIDTH_SPECIFICATION_FILE)) {
                        initializer = new StreamBasedLookupInitializer(INITIALIZE_LOKUP_LOCK, is);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        //use the static version
                        initializer = new StaticValueLookupInitializer(INITIALIZE_LOKUP_LOCK);
                    }
                } else {
                    initializer = new StaticValueLookupInitializer(INITIALIZE_LOKUP_LOCK);
                }
                if (initializer != null) {
                    initializer.initialize();
                    LOOKUP_ASSEMBLED = true;
                }
            }
        }

        Limit[] items = lookup.get(path);
        if (items != null) {
            for (Limit item : items) {
                if (item.hasProblem(value, result, this)) {
                    if (DEBUG && !found.contains(item)) {
                        found.add(item);
                    }
                    break; // only one error per item
                }
            }
        }
        return this;
    }
}
