package org.unicode.cldr.tool;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;

/**
 * Class to minimize certain types of regex. It does not work with open-ended quantifiers like * or +.
 * The regex that is produced requires no backup, so should be faster as well as more compact.
 * (But less readable!)
 * @author markdavis
 */
public class MinimizeRegex {
    /**
     * Sort  strings length-first, because (ab|abc) in regex world can stop at the first match, eg "ab".
     */
    private static final Comparator<String> LENGTH_FIRST_COMPARE = Comparator.comparingInt(String::length)
    .reversed()
    .thenComparing(Comparator.naturalOrder());

    public static void main(String[] args) {
        String defaultArg = "zxx|zu|zh|zgh|yue|yo|yi|yav|xog|xh|wo|wae|vun|vo|vi|vai|uz|ur|und|uk|ug|tzm|twq|tt|tr|to|tk|ti|th|tg|teo|te|ta|sw|sv|su|st|sr|sq|so|sn|smn|sm|sl|sk|si|shi|sg|ses|seh|se|sd|sbp|saq|sah|sa|rwk|rw|ru|rof|ro|rn|rm|qu|pt|ps|prg|pl|pa|os|or|om|nyn|ny|nus|nnh|nn|nmg|nl|ne|nds|nd|nb|naq|mzn|my|mul|mua|mt|ms|mr|mn|ml|mk|mi|mgo|mgh|mg|mfe|mer|mas|lv|luy|luo|lu|lt|lrc|lo|ln|lkt|lg|lb|lag|la|ky|kw|ku|ksh|ksf|ksb|ks|kok|ko|kn|km|kln|kl|kkj|kk|ki|khq|kea|kde|kam|kab|ka|jv|jmc|jgo|ja|it|is|ii|ig|id|ia|hy|hu|ht|hsb|hr|hmn|hi|he|haw|ha|gv|guz|gu|gsw|gl|gd|ga|fy|fur|fr|fo|fil|fi|ff|fa|ewo|eu|et|es|eo|en|el|ee|ebu|dz|dyo|dua|dsb|dje|de|dav|da|cy|cu|cs|co|ckb|chr|cgg|ceb|ce|ccp|ca|bs|brx|br|bo|bn|bm|bg|bez|bem|be|bas|az|ast|asa|as|ar|am|ak|agq|af";
        //defaultArg = "aa|ace|ad[ay]|ain|al[et]|anp?|arp|ast|av|awa|ay|ma[dgik]|mdf|men|mh|mi[cn]|mni|mos|mu[ls]|mwl|myv";
        String regexString = args.length < 1 ? defaultArg : args[0];
        UnicodeSet set = new UnicodeSet(args.length < 2 ? "[:ascii:]" : args[1]);
        
        System.out.println(defaultArg + "\n");
        Output<Set<String>> flattenedOut = new Output<>();
        String recompressed = compressWith(regexString, set, flattenedOut);
        System.out.println(CollectionUtilities.join(flattenedOut.value,"|") + "\n");
        System.out.println(recompressed + "\n");
    }

    public static String compressWith(String regexString, UnicodeSet set) {
        return compressWith(regexString, set, null);
    }
    
    public static String simplePattern(Collection<String> strings) {
        TreeSet<String> temp = new TreeSet<>(LENGTH_FIRST_COMPARE);
        temp.addAll(strings);
        return CollectionUtilities.join(temp,"|");
    }
    
    public static String compressWith(String regexString, UnicodeSet set, Output<Set<String>> flattenedOut) {
        Set<String> flattened = flatten(Pattern.compile(regexString), "", set);
        String regexString2 = CollectionUtilities.join(flattened,"|");
        Set<String> flattened2 = flatten(Pattern.compile(regexString2), "", set);
        if (!flattened2.equals(flattened)) {
            throw new IllegalArgumentException("Failed to compress: " + regexString + " using " + set + ", got " + regexString2);
        }

        if (flattenedOut != null) {
            flattenedOut.value = flattened;
        }
        return compressWith(flattened, set);
    }

    /**
     * Does not work with sets of strings containing regex syntax.
     * @param flattened
     * @param set
     * @return
     */
    public static String compressWith(Set<String> flattened, UnicodeSet set) {
        String recompressed = compress(flattened, new Output<Boolean>());
        Set<String> flattened2;
        try {
            flattened2 = flatten(Pattern.compile(recompressed), "", set);
        } catch (PatternSyntaxException e) {
            int loc = e.getIndex();
            if (loc >= 0) {
                recompressed = recompressed.substring(0,loc) + "$$$$$" + recompressed.substring(loc);
            }
            throw new IllegalArgumentException("Failed to parse: " + recompressed, e);
        }
        if (!flattened2.equals(flattened)) {
            throw new IllegalArgumentException("Failed to compress:\n" + flattened + "\n≠ " + flattened2);
        }
        return recompressed;
    }

    private static String compress(Set<String> flattened, Output<Boolean> isSingle) {
        // make a map from first code points to remainder
        Multimap<Integer, String> firstToRemainder = TreeMultimap.create();
        UnicodeSet results = new UnicodeSet();
        boolean hasEmpty = false;
        for (String s : flattened) {
            if (s.isEmpty()) {
                hasEmpty = true;
                continue;
            }
            int first = s.codePointAt(0);
            firstToRemainder.put(first, s.substring(UCharacter.charCount(first)));
        }
        StringBuilder buf = new StringBuilder();
        for (Entry<Integer, Collection<String>> entry : firstToRemainder.asMap().entrySet()) {
            Set<String> items = (Set<String>) entry.getValue();
            buf.setLength(0);
            buf.appendCodePoint(entry.getKey());
            if (items.size() == 1) {
                buf.append(items.iterator().next());
            } else {
                String sub = compress(items, isSingle);
                if (isSingle.value) {
                    buf.append(sub);
                } else {
                    buf.append('(').append(sub).append(')');
                }
            }
            results.add(buf.toString());
        }
        Set<String> strings = new TreeSet<>(results.strings());
        results.removeAll(strings);
        switch(results.size()) {
        case 0:
            break;
        case 1:
            strings.add(results.iterator().next());
            break;
        default:
            strings.add(results.toPattern(false));
            break;
        }
        switch (strings.size()) {
        case 0: throw new IllegalArgumentException();
        case 1: 
            isSingle.value = true;
            return strings.iterator().next() + (hasEmpty ? "?" : "");
        default:
            String result = CollectionUtilities.join(strings, "|");
            if (hasEmpty) {
                isSingle.value = true;
                return '(' + result + ")?";
            }
            isSingle.value = false;
            return result;
        }
    }

    public static TreeSet<String> flatten(Pattern pattern, String prefix, UnicodeSet set) {
        return flatten(pattern.matcher(""), prefix, set, new TreeSet<>(LENGTH_FIRST_COMPARE));
    }
    
    private static TreeSet<String> flatten(Matcher matcher, String prefix, UnicodeSet set, TreeSet<String> results) {
        for (String s : set) {
            String trial = prefix + s;
            matcher.reset(trial);
            boolean matches = matcher.matches();
            if (matches) {
                results.add(trial);
            }
            if (matcher.hitEnd()) {
                flatten(matcher, trial, set, results);
            }
        }
        return results;
    }
}
