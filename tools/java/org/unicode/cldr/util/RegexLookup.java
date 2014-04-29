package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CldrUtility.VariableReplacer;
import org.unicode.cldr.util.RegexFileParser.RegexLineParser;
import org.unicode.cldr.util.RegexFileParser.VariableProcessor;
import org.unicode.cldr.util.RegexLookup.Finder;
import org.unicode.cldr.util.RegexLookup.Finder.Info;

import com.ibm.icu.text.Transform;
import com.ibm.icu.util.Output;

/**
 * Lookup items according to a set of regex patterns. Returns the value according to the first pattern that matches. Not
 * thread-safe.
 * 
 * @param <T>
 */
public class RegexLookup<T> implements Iterable<Map.Entry<Finder, T>> {
    private VariableReplacer variables = new VariableReplacer();
     private StorageIterfaceBase<T> storage;
//    private StarPatternMap<T> SPEntries;
//    private RegexTree<T> RTEntries;
    private Map<Finder, T> MEntries;
    private Transform<String, ? extends Finder> patternTransform = RegexFinderTransform;
    private Transform<String, ? extends T> valueTransform;
    private Merger<T> valueMerger;
    private final boolean allowNull = false;
    private static PathStarrer pathStarrer = new PathStarrer().setSubstitutionPattern("*");

    public enum LookupType {
        STAR_PATTERN_LOOKUP, OPTIMIZED_DIRECTORY_PATTERN_LOOKUP, STANDARD
    };

   
    private LookupType _lookupType;

    /*
     * STAR_PATTERN_LOOKUP
     * 
     * When true, RegexLookup can match regex's even faster than the OPTIMIZED_DIRECTORY_PATTERN_LOOKUP below.
     * It requires some additional structure, such that the only regular expression constructs such as (a|b) occur
     * in attributes, so that the star pattern for a given path can match the star pattern of a given regular expression,
     * thus greatly reducing the number of actual regex matches that need to occur.
     */

    /*
     * OPTIMIZED_DIRECTORY_PATTERN_LOOKUP
     * 
     * When true, RegexLookup can match regex's in O(log(n)) time, where n is the number of regex's stored. 
     * This is provided that all regex patterns follow a directory based format and all directories are separated by a forward slash '/'
     * for example: ^//ldml/dates/calendars/calendar[@type="([^"']++)"]/alias[@source="locale"][@path="../calendar[@type='([^"']++)']"]
     * 
     * When false, RegexLookup will match regex's in O(n) time, where n is the number of regex's stored.
     * However regex's no longer need to follow any specific format (Slower but more versatile).
     */

    public RegexLookup(LookupType type) {
        _lookupType = type;
        switch (type) {
        case STAR_PATTERN_LOOKUP:
         //   SPEntries = new StarPatternMap<T>();
            storage= new StarPatternMap<T>();
            break;
        case OPTIMIZED_DIRECTORY_PATTERN_LOOKUP:
         //   RTEntries = new RegexTree<T>();
            storage = new RegexTree<T>();
            break;
        default:
            MEntries = new LinkedHashMap<Finder, T>();
            break;
        }
    }

    public RegexLookup() {
        this(LookupType.OPTIMIZED_DIRECTORY_PATTERN_LOOKUP);
//        _lookupType = RegexLookup.LookupType.OPTIMIZED_DIRECTORY_PATTERN_LOOKUP;
//        RTEntries = new RegexTree<T>();
    }

    public abstract static class Finder {
        public static class Info {
            public String[] value;
        }
   //   abstract public String[] getInfo();

     // abstract public boolean find(String item, Object context);
        
       abstract  public boolean find(String item, Object context, Info info);

        public int getFailPoint(String source) {
            return -1;
        }
        // must also define toString
    }
    
    public static class RegexFinder extends Finder  { //implements FinderInfoGettable {
        protected final Matcher matcher;
        protected final Pattern pattern;

        /**
         * Results of the last call to find() or to match; child classes are responsible for
         * updating the value, if new calls are made.
         */
        protected volatile Info lastFound=new Info();
        protected final Object MATCHER_SYNC=new Object();
      
        public RegexFinder(String pattern) {
            this.pattern=Pattern.compile(pattern, Pattern.COMMENTS);
            matcher = this.pattern.matcher("");
        }
        
        public final boolean find(String item, Object context, Info info) {
            synchronized(MATCHER_SYNC) {
                boolean result=find(item,context);
                if (result && info!=null) {
                    int limit = matcher.groupCount() + 1;
                    String[] value = new String[limit];
                    for (int i = 0; i < limit; ++i) {
                        value[i] = matcher.group(i);
                    }         
                    info.value=value;
                    lastFound.value=value;
                }
                return result;
            }
        }
        
        protected boolean find(String item, Object context) {
            synchronized (MATCHER_SYNC) {
                try {
                    boolean result= matcher.reset(item).find();
                    if (result) {
                        int limit = matcher.groupCount() + 1;
                        String[] value = new String[limit];
                        for (int i = 0; i < limit; ++i) {
                            value[i] = matcher.group(i);
                        }         
                        lastFound.value=value;
                    }
                    return result;
                } catch (StringIndexOutOfBoundsException e) {
                    // We don't know what causes this error (cldrbug 5051) so
                    // make the exception message more detailed.
                    throw new IllegalArgumentException("Matching error caused by pattern: ["
                        + matcher.toString() + "] on text: [" + item + "]", e);
                }
            }
        }
        

        /**
         * Get information about the last find() operation. 
         * @return
         */
        public String[] getInfo() {
            if (lastFound==null) {
                return null;
            }
            return lastFound.value;
        }
        
        public String toString() {
            return pattern.pattern();
           // return matcher.pattern().pattern();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            } else {
                return toString().equals(obj.toString());
            }
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public int getFailPoint(String source) {
            synchronized (MATCHER_SYNC) {
                return RegexUtilities.findMismatch(matcher, source);
            }
        }
    }

    private static interface StorageIterfaceBase<T> {
        Set<Entry<Finder, T>> entrySet();
        T get(Finder finder);
        T get(String pattern, Object context, Output<String[]> arguments, Output<Finder> matcherFound);
        List<T> getAll(String pattern, Object context, List<Finder> matcherList,Output<String[]> firstInfo);
        void put(Finder pattern, T value);
        int size();
    }
    
//    private static class FinderWithInfo {
//        Finder _finder;
//        Info _info;
//        public FinderWithInfo(Finder finder,Info info) {
//            _info=info;
//            _finder=finder;
//        }
//    }
    private static class RegexTree<T> implements StorageIterfaceBase<T> {
        private RTNode root;
        private int _size;
        private RTNodeRankComparator rankComparator = new RTNodeRankComparator();

        public RegexTree() {
            root = new RTNode("", null);
            _size = 0;
        }

        public int size() {
            return _size;
        }

        public void put(Finder pattern, T value) {
            root.put(new RTNode(pattern, value, _size));
            _size++;
        }

        public T get(Finder finder) {
            return root.get(finder);
        }

        public List<T> getAll(String pattern, Object context, List<Finder> matcherList,Output<String[]> firstInfo) {
            List<RTNode> list = new ArrayList<RTNode>();
            List<T> retList = new ArrayList<T>();

            root.addToList(pattern, context, list);
            Collections.sort(list, rankComparator);

            if (firstInfo!=null && !list.isEmpty()) {
                //   RTNode firstNode=list.get(0);
////                synchronized(firstNode) {
                //     firstInfo.value=firstNode._info.value;
                Finder f=list.get(0)._finder;
                if (f instanceof RegexFinder) {
                    RegexFinder rf=(RegexFinder)f;
                    firstInfo.value=rf.getInfo();
                }
            }

            
            for (RTNode n : list) {
                retList.add(n._val);
                if (matcherList != null) {
                    matcherList.add(n._finder);
                }
            }

            return retList;
        }

        public T get(String pattern, Object context, Output<String[]> arguments, Output<Finder> matcherFound) {
            List<Finder> matcherList = new ArrayList<Finder>();
            Output<String[]> firstInfo=new Output<>();
            List<T> matches = getAll(pattern, context, matcherList,firstInfo); //need to get whole list because we want value that was entered first
            if (arguments != null) {
//               arguments.value = (matcherList.size() > 0) ? matcherList.get(0).getInfo() : null;
                arguments.value=firstInfo.value;
            }
            if (matcherFound != null) {
                matcherFound.value = (matcherList.size() > 0) ? matcherList.get(0) : null;
            }
            return (matches.size() > 0) ? matches.get(0) : null;
        }

        public Set<Entry<Finder, T>> entrySet() {
            LinkedHashMap<Finder, T> ret = new LinkedHashMap<Finder, T>();
            TreeSet<RTNode> s = new TreeSet<RTNode>(rankComparator);
            root.addToEntrySet(s);

            for (RTNode n : s) {
                ret.put(n._finder, n._val);
            }

            return ret.entrySet();
        }

        public class RTNode extends NodeBase<T> {
//            Finder _finder;
//            T _val;
            List<RTNode> _children = new ArrayList<RTNode>();
            int _rank=-1; //rank -1 means the node was not inserted, but only used for structural purposes

            //constructor for regular nodes with a Finder
            public RTNode(Finder finder, T val, int rank) {
                super(finder,val);
//                _finder = finder;
//                _val = val;
                _rank = rank;
            }

            //constructors for nodes without a Finder
            public RTNode(String key, T val) {
                super(new RegexFinder(key),val);
//                _finder = new RegexFinder(key);
//                _val = val;
//                _rank = -1;
            }

            public void put(RTNode node) {
                if (_children.size() == 0) {
                    _children.add(node);
                } else {
                    String maxSimilarChars = ""; //most similar characters up to the last similar directory
                    int insertIndex = 0;
                    for (int i = 0; i < _children.size(); i++) {
                        RTNode child = _children.get(i);
                        String childFinderPattern = child._finder.toString();
                        if (childFinderPattern.length() > 0 && childFinderPattern.charAt(childFinderPattern.length() - 1) == '$') {
                            childFinderPattern = childFinderPattern.substring(0, childFinderPattern.length() - 1); //takes into account the added "$"
                        }
                        else if (child._rank == -1) {
                            childFinderPattern = childFinderPattern.substring(0, childFinderPattern.length() - 2); //takes into account the added ".*"
                        }

                        //check if child has the same Finder as node to insert, then replace it
                        if (node._finder.equals(child._finder)) {
                            child._finder = node._finder;
                            child._val = node._val;
                            if (child._rank == -1) {
                                child._rank = node._rank;
                            } else {
                                _size--;
                            }
                            return;
                        }

                        //check if child is the parent of node
                        if (child._rank == -1 && node._finder.toString().startsWith(childFinderPattern)) {
                            child.put(node);
                            return;
                        }

                        //if not parent then check if candidate for most similar RTNode
                        String gcp = greatestCommonPrefix(childFinderPattern, node._finder.toString());
                        gcp = removeExtraChars(gcp);
                        if (gcp.length() > maxSimilarChars.length()) {
                            maxSimilarChars = gcp;
                            insertIndex = i;
                        }
                    }

                    String finderPattern = this._finder.toString();
                    if (finderPattern.length() > 0 && finderPattern.charAt(finderPattern.length() - 1) == '$') {
                        finderPattern = finderPattern.substring(0, finderPattern.length() - 1); //takes into account the added "$"
                    }
                    else if (!(finderPattern.equals("")) && this._rank == -1) {
                        finderPattern = finderPattern.substring(0, finderPattern.length() - 2); //takes into account the added ".*"
                    }

                    if ((maxSimilarChars).equals(finderPattern)) { //add under this if no similar children
                        _children.add(node);
                    } else {
                        //create the common parent of the chosen candidate above and node, then add to the insert index
                        RTNode newParent = new RTNode(maxSimilarChars + ".*", null);
                        newParent._children.add(this._children.get(insertIndex));
                        newParent._children.add(node);
                        this._children.remove(insertIndex);
                        this._children.add(insertIndex, newParent);
                    }
                }
            }

            //takes a string in a directory regex form and removes all characters up to the last full directory
            private String removeExtraChars(String input) {
                String ret = input.substring(0, Math.max(0, input.lastIndexOf('/')));
                while ((ret.lastIndexOf('(') != -1 && ret.lastIndexOf('(') > ret.lastIndexOf(')')) ||
                    (ret.lastIndexOf('[') != -1 && ret.lastIndexOf('[') > ret.lastIndexOf(']')) ||
                    (ret.lastIndexOf('{') != -1 && ret.lastIndexOf('{') > ret.lastIndexOf('}'))) {
                    ret = ret.substring(0, Math.max(0, ret.lastIndexOf('/')));
                }
                return ret;
            }

            //traverse tree to get value
            public T get(Finder finder) {
                T ret = null; //return value

                if (_children.size() == 0) {
                    return null;
                } else {
                    for (RTNode child : _children) {

                        //check if child is the node
                        if (child._rank != -1 && finder.equals(child._finder)) {
                            return child._val;
                        }

                        String childFinderPattern = child._finder.toString();

                        if (childFinderPattern.length() > 0 && childFinderPattern.charAt(childFinderPattern.length() - 1) == '$') {
                            childFinderPattern = childFinderPattern.substring(0, childFinderPattern.length() - 1); //takes into account the added "$"
                        }
                        else if (child._rank == -1) {
                            childFinderPattern = childFinderPattern.substring(0, childFinderPattern.length() - 2); //takes into account the added ".*"
                        }

                        //check if child is the parent of node
                        if (finder.toString().startsWith(childFinderPattern)) {
                            ret = child.get(finder);
                            if (ret != null) {
                                break;
                            }
                        }
                    }

                    return ret;
                }
            }

            //traverse tree to get an entry set
            public void addToEntrySet(TreeSet<RTNode> s) {
                if (_children.size() == 0) {
                    return;
                } else {
                    for (RTNode child : _children) {
                        if (child._rank != -1) {
                            s.add(child);
                        }
                        child.addToEntrySet(s);
                    }
                }
            }

            //traverse tree to get list of all values who's key matcher matches pattern
            public void addToList(String pattern, Object context, List<RTNode> list) {
                if (_children.size() == 0) {
                    return;
                } else {
                    Info firstInfo=new Info();
                    for (RTNode child : _children) {
                        boolean found;
                        synchronized (child._finder) {      
                            found = child._finder.find(pattern, context,firstInfo);
                        }

                        //check if child matches pattern
                        if (found) {
                            if (child._rank != -1) {
                                list.add(child);
                            }
                            // if this node's info value is unset, set it to the result of the
                            // lookup
                            if (child._info!=null && child._info.value==null) {
                                child._info.value=firstInfo.value;
                            }
                            //check if child is the parent of node then enter that node
                            child.addToList(pattern, context, list);
                        }
                    }
                }
            }

            public String toString() {
                return this._finder.toString();
            }

            //greatest common prefix between two strings
            public String greatestCommonPrefix(String a, String b) {
                int minLength = Math.min(a.length(), b.length());
                for (int i = 0; i < minLength; i++) {
                    if (a.charAt(i) != b.charAt(i)) {
                        return a.substring(0, i);
                    }
                }
                return a.substring(0, minLength);
            }
        }

        class RTNodeRankComparator implements Comparator<RTNode> {
            public int compare(RTNode a, RTNode b) {
                if (a == b) {
                    return 0;
                } else if (a == null) {
                    return -1;
                } else if (b == null) {
                    return 1;
                } else if (a._rank == b._rank) {
                    return 0;
                } else if (a._rank > b._rank) {
                    return 1;
                } else {
                    return -1;
                }
            }
        }
    }

    private static class StarPatternMap<T>implements StorageIterfaceBase<T> {
        private Map<String, List<SPNode>> _spmap;
        private int _size;

        public StarPatternMap() {
            _spmap = new HashMap<String, List<SPNode>>();
            _size = 0;
        }

        public int size() {
            return _size;
        }

        public void put(Finder pattern, T value) {
            //System.out.println("pattern.toString() is => "+pattern.toString());
            String starPattern = pathStarrer.transform2(pattern.toString().replaceAll("\\(\\[\\^\"\\]\\*\\)", "*"));
            //System.out.println("Putting => "+starPattern);
            List<SPNode> candidates = _spmap.get(starPattern);
            if (candidates == null) {
                candidates = new ArrayList<SPNode>();
            }
            SPNode newNode = new SPNode(pattern, value);
            candidates.add(newNode);
            _spmap.put(starPattern, candidates);
            _size++;
        }

        public T get(Finder finder) {
            String starPattern = pathStarrer.transform2(finder.toString());
            List<SPNode> candidates = _spmap.get(starPattern);
            if (candidates == null) {
                return null;
            }
            for (SPNode cand : candidates) {
                if (cand._finder.equals(finder)) {
                    return cand._val;
                }
            }
            return null;
        }

        public List<T> getAll(String pattern, Object context, List<Finder> matcherList,Output<String[]> firstInfo) {
            List<SPNode> list = new ArrayList<SPNode>();
            List<T> retList = new ArrayList<T>();

            String starPattern = pathStarrer.transform2(pattern);
            List<SPNode> candidates = _spmap.get(starPattern);
            if (candidates == null) {
                return retList;
            }
            for (SPNode cand : candidates) {
                Info info=new Info();
                if (cand._finder.find(pattern, context,info)) {
                    list.add(cand);
                    if (firstInfo!=null) {
                        firstInfo.value=info.value;
                    }
                }
            }

            for (SPNode n : list) {
                retList.add(n._val);
                if (matcherList != null) {
                    matcherList.add(n._finder);
                }
            }

            return retList;
        }

        public T get(String pattern, Object context, Output<String[]> arguments, Output<Finder> matcherFound) {
            List<Finder> matcherList = new ArrayList<Finder>();
            Output<String[]> firstInfo=new Output<>();
            List<T> matches = getAll(pattern, context, matcherList,firstInfo); //need to get whole list because we want value that was entered first
            if (arguments != null && firstInfo.value!=null) {
//                arguments.value = (matcherList.size() > 0) ? matcherList.get(0).getInfo() : null;
                arguments.value=matcherList.isEmpty()? null: firstInfo.value;
            }
            if (matcherFound != null) {
                matcherFound.value = (matcherList.size() > 0) ? matcherList.get(0) : null;
            }
            return (matches.size() > 0) ? matches.get(0) : null;
        }

        public Set<Entry<Finder, T>> entrySet() {
            LinkedHashMap<Finder, T> ret = new LinkedHashMap<Finder, T>();

            for (Entry<String, List<SPNode>> entry : _spmap.entrySet()) {
                List<SPNode> candidates = entry.getValue();
                for (SPNode node : candidates) {
                    ret.put(node._finder, node._val);
                }
            }
            return ret.entrySet();
        }

        public class SPNode extends NodeBase<T> {
//            Finder _finder;
//            T _val;

            public SPNode(Finder finder, T val) {
//                _finder = finder;
//                _val = val;
                super(finder,val);
            }

            public String toString() {
                return this._finder.toString();
            }
        }
    }
    private static class NodeBase<T> {
        Finder _finder;
        T _val;
        Info _info=new Info();
        public NodeBase(Finder finder, T value) {
            this._finder=finder;
            this._val=value;
        }
    }
    
    public static Transform<String, RegexFinder> RegexFinderTransform = new Transform<String, RegexFinder>() {
        public RegexFinder transform(String source) {
            return new RegexFinder(source);
        }
    };

    /**
     * The same as a RegexFinderTransform, except that [@ is changed to \[@, and ^ is added before //. To work better
     * with XPaths.
     */
    public static Transform<String, RegexFinder> RegexFinderTransformPath = new Transform<String, RegexFinder>() {
        public RegexFinder transform(String source) {
            final String newSource = source.replace("[@", "\\[@");
            return new RegexFinder(newSource.startsWith("//")
                ? "^" + newSource
                : newSource);
        }
    };

    /**
     * Allows for merging items of the same type.
     * 
     * @param <T>
     */
    public interface Merger<T> {
        T merge(T a, T into);
    }

    /**
     * Returns the result of a regex lookup.
     * 
     * @param source
     * @return
     */
    public final T get(String source) {
        return get(source, null, null, null, null);
    }

    /**
     * Returns the result of a regex lookup, with the group arguments that matched.
     * 
     * @param source
     * @param context
     *            TODO
     * @return
     */
    public T get(String source, Object context, Output<String[]> arguments) {
        return get(source, context, arguments, null, null);
    }

    /**
     * Returns the result of a regex lookup, with the group arguments that matched. Supplies failure cases for
     * debugging.
     * 
     * @param source
     * @param context
     * @return
     */
    public T get(String source, Object context, Output<String[]> arguments,
        Output<Finder> matcherFound, List<String> failures) {

        if (_lookupType == RegexLookup.LookupType.STAR_PATTERN_LOOKUP) {
         //   T ret = SPEntries.get(source, context, arguments, matcherFound);
            T ret = storage.get(source, context, arguments, matcherFound);
            if (ret != null) {
                return ret;
            }

            if (failures != null) {
                for (Map.Entry<Finder, T> entry : storage.entrySet()) {
//                for (Map.Entry<Finder, T> entry : SPEntries.entrySet()) {
                    Finder matcher = entry.getKey();
                    synchronized (matcher) {
                        int failPoint = matcher.getFailPoint(source);
                        String show = source.substring(0, failPoint) + "☹" + source.substring(failPoint) + "\t"
                            + matcher.toString();
                        failures.add(show);
                    }
                }
            }
        } else if (_lookupType == RegexLookup.LookupType.OPTIMIZED_DIRECTORY_PATTERN_LOOKUP) {
      //      T ret = RTEntries.get(source, context, arguments, matcherFound);
            T ret = storage.get(source, context, arguments, matcherFound);
            if (ret != null) {
                return ret;
            }

            if (failures != null) {
                for (Map.Entry<Finder, T> entry : storage.entrySet()) {
//                for (Map.Entry<Finder, T> entry : RTEntries.entrySet()) {
                    Finder matcher = entry.getKey();
                    synchronized (matcher) {
                        int failPoint = matcher.getFailPoint(source);
                        String show = source.substring(0, failPoint) + "☹" + source.substring(failPoint) + "\t"
                            + matcher.toString();
                        failures.add(show);
                    }
                }
            }
        } else {
            //slow but versatile implementation
            for (Map.Entry<Finder, T> entry : MEntries.entrySet()) {
                Finder matcher = entry.getKey();
                synchronized (matcher) {
                    Info firstInfo=new Info();
                    if (matcher.find(source, context,firstInfo)) {
                        if (arguments != null) {
//                            arguments.value = matcher.getInfo();
                            arguments.value=firstInfo.value;
                        }
                        if (matcherFound != null) {
                            matcherFound.value = matcher;
                        }
                        return entry.getValue();
                    } else if (failures != null) {
                        int failPoint = matcher.getFailPoint(source);
                        String show = source.substring(0, failPoint) + "☹" + source.substring(failPoint) + "\t"
                            + matcher.toString();
                        failures.add(show);
                    }
                }
            }
        }

        // not really necessary, but makes debugging easier.
        if (arguments != null) {
            arguments.value = null;
        }
        if (matcherFound != null) {
            matcherFound.value = null;
        }
        return null;
    }

    /**
     * Returns all results of a regex lookup, with the group arguments that matched. Supplies failure cases for
     * debugging.
     * 
     * @param source
     * @param context
     *            TODO
     * @return
     */
    public List<T> getAll(String source, Object context, List<Finder> matcherList, List<String> failures) {
        if (_lookupType == RegexLookup.LookupType.STAR_PATTERN_LOOKUP) {
            Output<String[]> firstInfo=new Output<>();
//            List<T> matches = SPEntries.getAll(source, context, matcherList,firstInfo);
            List<T> matches = storage.getAll(source, context, matcherList,firstInfo);
            if (matches != null) {
                return matches;
            }

            if (failures != null) {
                for (Map.Entry<Finder, T> entry : storage.entrySet()) {
//                for (Map.Entry<Finder, T> entry : SPEntries.entrySet()) {
                    Finder matcher = entry.getKey();
                    synchronized (matcher) {
                        int failPoint = matcher.getFailPoint(source);
                        String show = source.substring(0, failPoint) + "☹" + source.substring(failPoint) + "\t"
                            + matcher.toString();
                        failures.add(show);
                    }
                }
            }
            return null;
        } else if (_lookupType == RegexLookup.LookupType.OPTIMIZED_DIRECTORY_PATTERN_LOOKUP) {
            Output<String[]> info=new Output<>();
//            List<T> matches = RTEntries.getAll(source, context, matcherList,info);
            List<T> matches = storage.getAll(source, context, matcherList,info);
           
            if (matches != null) {
                return matches;
            }

            if (failures != null) {
                for (Map.Entry<Finder, T> entry : storage.entrySet()) {
//                for (Map.Entry<Finder, T> entry : RTEntries.entrySet()) {
                    Finder matcher = entry.getKey();
                    synchronized (matcher) {
                        int failPoint = matcher.getFailPoint(source);
                        String show = source.substring(0, failPoint) + "☹" + source.substring(failPoint) + "\t"
                            + matcher.toString();
                        failures.add(show);
                    }
                }
            }
            return null;
        } else {
            //slow but versatile implementation
            List<T> matches = new ArrayList<T>();
            for (Map.Entry<Finder, T> entry : MEntries.entrySet()) {
                Finder matcher = entry.getKey();
                Info firstInfo=new Info();
                if (matcher.find(source, context,firstInfo)) {
                    if (matcherList != null) {
                        matcherList.add(matcher);
                    }
                    matches.add(entry.getValue());
                } else if (failures != null) {
                    int failPoint = matcher.getFailPoint(source);
                    String show = source.substring(0, failPoint) + "☹" + source.substring(failPoint) + "\t"
                        + matcher.toString();
                    failures.add(show);
                }
            }
            return matches;
        }
    }

    /**
     * Find the patterns that haven't been matched. Requires the caller to collect the patterns that have, using
     * matcherFound.
     * 
     * @return outputUnmatched
     */
    public Map<String, T> getUnmatchedPatterns(Set<String> matched, Map<String, T> outputUnmatched) {
        outputUnmatched.clear();

        Set<Map.Entry<Finder, T>> entrySet;
        switch (_lookupType) {
        case STAR_PATTERN_LOOKUP:
//            entrySet = SPEntries.entrySet();
            entrySet= storage.entrySet();
            break;
        case OPTIMIZED_DIRECTORY_PATTERN_LOOKUP:
//            entrySet = RTEntries.entrySet();
            entrySet = storage.entrySet();
            break;
        default:
            entrySet = MEntries.entrySet();
            break;
        }

        for (Map.Entry<Finder, T> entry : entrySet) {
            String pattern = entry.getKey().toString();
            if (!matched.contains(pattern)) {
                outputUnmatched.put(pattern, entry.getValue());
            }
        }
        return outputUnmatched;
    }

    /**
     * Create a RegexLookup. It will take a list of key/value pairs, where the key is a regex pattern and the value is
     * what gets returned.
     * 
     * @param patternTransform
     *            Used to transform string patterns into a Pattern. Can be used to process replacements (like
     *            variables).
     * @param valueTransform
     *            Used to transform string values into another form.
     * @param valueMerger
     *            Used to merge values with the same key.
     */
    public static <T, U> RegexLookup<T> of(Transform<String, Finder> patternTransform,
        Transform<String, T> valueTransform, Merger<T> valueMerger) {
        return new RegexLookup<T>().setPatternTransform(patternTransform).setValueTransform(valueTransform)
            .setValueMerger(valueMerger);
    }

    public static <T> RegexLookup<T> of(Transform<String, T> valueTransform) {
        return new RegexLookup<T>().setValueTransform(valueTransform).setPatternTransform(RegexFinderTransform);
    }

    public static <T> RegexLookup<T> of() {
        return new RegexLookup<T>().setPatternTransform(RegexFinderTransform);
    }

    public RegexLookup<T> setValueTransform(Transform<String, ? extends T> valueTransform) {
        this.valueTransform = valueTransform;
        return this;
    }

    public RegexLookup<T> setPatternTransform(Transform<String, ? extends Finder> patternTransform) {
        this.patternTransform = patternTransform != null ? patternTransform : RegexFinderTransform;
        return this;
    }

    public RegexLookup<T> setValueMerger(Merger<T> valueMerger) {
        this.valueMerger = valueMerger;
        return this;
    }

    /**
     * Load a RegexLookup from a file. Opens a file relative to the class, and adds lines separated by "; ". Lines
     * starting with # are comments.
     */
    public RegexLookup<T> loadFromFile(Class<?> baseClass, String filename) {
        RegexFileParser parser = new RegexFileParser();
        parser.setLineParser(new RegexLineParser() {
            @Override
            public void parse(String line) {
                int pos = line.indexOf("; ");
                if (pos < 0) {
                    throw new IllegalArgumentException();
                }
                String source = line.substring(0, pos).trim();
                String target = line.substring(pos + 2).trim();
                try {
                    @SuppressWarnings("unchecked")
                    T result = valueTransform == null ? (T) target : valueTransform.transform(target);
                    add(source, result);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Failed to add <" + source + "> => <" + target + ">", e);
                }
            }
        });
        parser.setVariableProcessor(new VariableProcessor() {
            @Override
            public void add(String variable, String variableName) {
                addVariable(variable, variableName);
            }

            @Override
            public String replace(String str) {
                return variables.replace(str);
            }
        });
        parser.parse(baseClass, filename);
        return this;
    }

    public RegexLookup<T> addVariable(String variable, String variableValue) {
        if (!variable.startsWith("%")) {
            throw new IllegalArgumentException("Variables must start with %");
        }
        variables.add(variable.trim(), variableValue.trim());
        return this;
    }

    /**
     * Add a pattern/value pair.
     * 
     * @param stringPattern
     * @param target
     * @return this, for chaining
     */
    public RegexLookup<T> add(String stringPattern, T target) {
        if (stringPattern.contains("%")) {
            stringPattern = variables.replace(stringPattern);
        }
        Finder pattern0 = patternTransform.transform(stringPattern);
        return add(pattern0, target);
    }

    /**
     * Add a pattern/value pair.
     * 
     * @param pattern
     * @param target
     * @return this, for chaining
     */
    public RegexLookup<T> add(Finder pattern, T target) {
        if (!allowNull && target == null) {
            throw new NullPointerException("null disallowed, unless allowNull(true) is called.");
        }

        T old;
        switch (_lookupType) {
        case STAR_PATTERN_LOOKUP: // fallthrough
        case OPTIMIZED_DIRECTORY_PATTERN_LOOKUP:
            old = storage.get(pattern);
//            old = SPEntries.get(pattern);
            break;
//        case OPTIMIZED_DIRECTORY_PATTERN_LOOKUP:
//            old = storage.get(pattern);
//            old = RTEntries.get(pattern);
//            break;
        default:
            old = MEntries.get(pattern);
            break;
        }

        if (old == null) {
            switch (_lookupType) {
            case STAR_PATTERN_LOOKUP: // fallthrough
            case OPTIMIZED_DIRECTORY_PATTERN_LOOKUP:
                storage.put(pattern, target);
//                SPEntries.put(pattern, target);
                break;
//            case OPTIMIZED_DIRECTORY_PATTERN_LOOKUP:
//                storage.put(pattern, target);
//                RTEntries.put(pattern, target);
//                break;
            default:
                MEntries.put(pattern, target);
                break;
            }
        } else if (valueMerger != null) {
            valueMerger.merge(target, old);
        } else {
            throw new IllegalArgumentException("Duplicate matcher without Merger defined " + pattern + "; old: " + old
                + "; new: " + target);
        }
        return this;
    }

    @Override
    public Iterator<Map.Entry<Finder, T>> iterator() {
        switch (_lookupType) {
        case STAR_PATTERN_LOOKUP: // fall through
        case OPTIMIZED_DIRECTORY_PATTERN_LOOKUP:
//            return Collections.unmodifiableCollection(SPEntries.entrySet()).iterator();
            return Collections.unmodifiableCollection(storage.entrySet()).iterator();
//        case OPTIMIZED_DIRECTORY_PATTERN_LOOKUP:
//            return Collections.unmodifiableCollection(RTEntries.entrySet()).iterator();
//            return Collections.unmodifiableCollection(storage.entrySet()).iterator();
        default:
            return Collections.unmodifiableCollection(MEntries.entrySet()).iterator();
        }
    }

    public static String replace(String lookup, String... arguments) {
        StringBuilder result = new StringBuilder();
        int last = 0;
        while (true) {
            int pos = lookup.indexOf("$", last);
            if (pos < 0) {
                result.append(lookup.substring(last, lookup.length()));
                break;
            }
            result.append(lookup.substring(last, pos));
            final int arg = lookup.charAt(pos + 1) - '0';
            try {
                result.append(arguments[arg]);
            } catch (Exception e) {
                throw new IllegalArgumentException("Replacing $" + arg + " in <" + lookup
                    + ">, but too few arguments supplied.");
            }
            last = pos + 2;
        }
        return result.toString();
    }

    /**
     * @return the number of entries
     */
    public int size() {
        switch (_lookupType) {
        case STAR_PATTERN_LOOKUP:  // fall through
        case OPTIMIZED_DIRECTORY_PATTERN_LOOKUP:
//            return SPEntries.size();
            return storage.size();
//        case OPTIMIZED_DIRECTORY_PATTERN_LOOKUP:
//            return storage.size();
//            return RTEntries.size();
        default:
            return MEntries.size();
        }
    }
}