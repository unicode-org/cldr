package org.unicode.cldr.unittest;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Containment;
import org.unicode.cldr.util.LanguageGroup;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.ULocale;

public class TestLanguageGroup extends TestFmwk {
    static CLDRConfig CONF = CLDRConfig.getInstance();
    static CLDRFile ENGLISH = CONF.getEnglish();
    static SupplementalDataInfo SDI = CONF.getSupplementalDataInfo();
    static Multimap<String, String> PARENT_TO_CHILDREN = SDI.getLanguageGroups();
    static Multimap<String, String> CHILDREN_TO_PARENT = ImmutableMultimap.copyOf(Multimaps.invertFrom(PARENT_TO_CHILDREN, TreeMultimap.create()));

    public static void main(String[] args) {
        System.out.println("See the class description for GenerateLanguageContainment.java about fixing problems, or the javadoc for the test code");
        new TestLanguageGroup().run(args);
    }

    static final Set<String> ISOLATES = ImmutableSet.of("ko", "qu", "root", "ce", "kgp", "und");

    /**
     * Checks that items with no parent are not in that category by mistake.
     */
    public void TestCodes() {
        LanguageTagParser ltp = new LanguageTagParser();
        Set<String> seen = new HashSet<>();
        for (String locale : CONF.getCldrFactory().getAvailableLanguages()) {
            String lang = ltp.set(locale).getLanguage();
            if (lang.equals("root")) {
                continue;
            }
            if (seen.contains(lang)) {
                continue;
            }
            seen.add(lang);
            Set<List<String>> targets = getAllAncestors(lang);
            assertEquals(getName(lang) + " has multiple ancestors: " + targets, 1, targets.size());
            List<String> target = targets.iterator().next();
            if ((target.size() == 1) && !ISOLATES.contains(lang)) {
                errln("Single ancestor but not in ISOLATES: " + getName(lang) + "\t" + target);
                /** If this fails, check the ancestors on wikipedia, and if none of them have */
            } else if (!(target.size() == 1) && ISOLATES.contains(lang)) {
                errln("Multiple ancestors but in ISOLATES: " + getName(lang) + "\t" + target);
            } else {
                logln(getName(lang) + "\t" + target);
            }
        }
    }

    static final Set<String> MIXED_LANGUAGES = ImmutableSet.of("sth");

    public void TestSingleParent() {
        for (Entry<String, Collection<String>> entry : CHILDREN_TO_PARENT.asMap().entrySet()) {
            String child = entry.getKey();
            Collection<String> parents = entry.getValue();
            if (parents.size() != 1) {
                StringBuilder parentsString = new StringBuilder();
                parents.forEach(code -> parentsString.append(getName(code) + "; "));
                msg("\tThere are multiple parents\t" + parentsString + "\tfor\t" + getName(child), MIXED_LANGUAGES.contains(child) ? LOG : ERR, true, true);
            }
        }
    }

    public static Set<LanguageGroup> OLD_SPECIALS = ImmutableSet.of(LanguageGroup.root, LanguageGroup.cjk, LanguageGroup.other, LanguageGroup.american, LanguageGroup.caucasian);

    public void TestOldLangaugeGroup() {
        for (LanguageGroup item : LanguageGroup.values()) {
            if (OLD_SPECIALS.contains(item)) { // special cases
                continue;
            }
            Set<ULocale> locales = LanguageGroup.getLocales(item);
            String parent = item.iso;
            parent = parent.replace("_001", "");
            logln(parent + ": " + new TreeSet<>(locales));
            logln(parent + ": " + getAllChildren(parent));
            for (ULocale child : locales) {
                String childString = child.toLanguageTag();
                if (!assertTrue(getName(parent) + " contains " + getName(childString), isAncestorOf(parent, childString))) {
                    System.out.println("superclasses of " + childString + ": " + getAllAncestors(childString));
                    System.out.println("subclasses of " + parent + ": " + getAllChildren(childString));
                }
            }
        }
    }

    String getName(String code) {
        return code + " [" + fixedName(code) + "]";
    }

    private String fixedName(String code) {
        switch (code) {
        case "grk":
            return "Hellenic";
        default:
            return ENGLISH.getName(code).replace(" [Other]", "");
        }
    }

    boolean isAncestorOf(String ancestor, String lang) {
        while (true) {
            Collection<String> parents = CHILDREN_TO_PARENT.get(lang);
            if (parents == null || parents.isEmpty()) {
                return false;
            }
            if (parents.size() != 1) { // recurse with 2 since is simpler
                for (String parent : parents) {
                    if (parent.equals(ancestor) || isAncestorOf(ancestor, parent)) {
                        return true;
                    }
                }
                return false;
            }
            lang = parents.iterator().next();
            if (lang.equals(ancestor)) {
                return true;
            }
        }
    }

//    <T extends Collection<String>> T getAllDirected(Multimap<String, String> multimap, String lang, T target) {
//        Collection<String> parents = multimap.get(lang);
//        if (!parents.isEmpty()) {
//            target.addAll(parents);
//            for (String parent : parents) {
//                getAllDirected(multimap, parent, target);
//            }
//        }
//        return target;
//    }

    <T extends Collection<String>> Set<List<String>> getAllAncestors(String lang) {
        return Containment.getAllDirected(CHILDREN_TO_PARENT, lang);
    }

    <T extends Collection<String>> Set<List<String>> getAllChildren(String lang) {
        return Containment.getAllDirected(PARENT_TO_CHILDREN, lang);
    }

}
