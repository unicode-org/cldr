package org.unicode.cldr.unittest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.LanguageGroup;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.ULocale;

public class TestLanguageGroup extends TestFmwk {
    static CLDRConfig CONF = CLDRConfig.getInstance();
    static CLDRFile ENGLISH = CONF.getEnglish();
    static SupplementalDataInfo  SDI = CONF.getSupplementalDataInfo();
    static Multimap<String, String> PARENT_TO_CHILDREN = SDI.getLanguageGroups();
    static Multimap<String, String> CHILDREN_TO_PARENT = ImmutableMultimap.copyOf(Multimaps.invertFrom(PARENT_TO_CHILDREN, TreeMultimap.create()));



    public static void main(String[] args) {
        new TestLanguageGroup().run(args);
    }

    public void TestOldLangaugeGroup() {
        for (LanguageGroup item : LanguageGroup.values()) {
            Set<ULocale> locales = LanguageGroup.getLocales(item);
            String parent = item.iso;
            parent = parent.replace("_001","");
            System.out.println(parent + ": " + new TreeSet<>(locales));
            System.out.println(parent + ": " + getAllChildren(parent, new TreeSet<>()));
            for (ULocale child : locales) {
                String childString = child.toLanguageTag();
                if (!assertTrue(getName(parent) + " is parent of " + getName(childString), isAncestorOf(parent, childString))) {
                    System.out.println("ancestors of " + childString + ": " + getAllAncestors(childString, new ArrayList<>()));
                }
            }
        }
    }

    String getName(String code) {
        return code + " [" + ENGLISH.getName(code) + "]";
    }

    boolean isAncestorOf(String ancestor, String lang) {
        while (true) {
            Collection<String> parents = CHILDREN_TO_PARENT.get(lang);
            if (parents == null || parents.isEmpty()) {
                return false;
            }
            if (parents.size() != 1) {
                throw new IllegalArgumentException();
            }
            lang = parents.iterator().next();
            if (lang.equals(ancestor)) {
                return true;
            }
        }
    }

    <T extends Collection<String>> T getAllDirected(Multimap<String, String> multimap, String lang, T target) {
        Collection<String> parents = multimap.get(lang);
        if (!parents.isEmpty()) {
            target.addAll(parents);
            for (String parent : parents) {
                getAllDirected(multimap, parent, target);
            }
        }
        return target;
    }
    
    <T extends Collection<String>> T getAllAncestors(String lang, T target) {
        return getAllDirected(CHILDREN_TO_PARENT, lang, target);
    }


    <T extends Collection<String>> T getAllChildren(String lang, T target) {
        return getAllDirected(PARENT_TO_CHILDREN, lang, target);
    }

}
