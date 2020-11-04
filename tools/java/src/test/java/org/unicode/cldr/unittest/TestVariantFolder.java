package org.unicode.cldr.unittest;

import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.VariantFolder;

import com.ibm.icu.text.CanonicalIterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class TestVariantFolder {
    public static void main(String[] args) {
        VariantFolder variantFolder = new VariantFolder(
            new VariantFolder.CaseVariantFolder());
        String[] tests = { "abc", "aß", "\uFB01sh", "Åbë" };
        for (String test : tests) {
            Set<String> set = variantFolder.getClosure(test);
            System.out.println(test + "\t" + set.size() + "\t"
                + new TreeSet<Object>(set));
            final Set<String> closed = closeUnderCanonicalization(set,
                new TreeSet<String>());
            System.out.println(test + "\t" + closed.size() + "\t" + closed);
        }

        variantFolder = new VariantFolder(
            new VariantFolder.CompatibilityFolder());
        String[] testSets = { "[:Word_Break=ExtendNumLet:]",
            "[:Word_Break=Format:]", "[:Word_Break=Katakana:]",
            "[[:Word_Break=MidLetter:]\u2018]", "[:Word_Break=MidNum:]",
            "[[:Word_Break=MidNum:]-[\\uFE13]]", "[:Word_Break=Numeric:]",
            "[\\u0027\\u2018\\u2019\\u002e]", };
        for (String testSet : testSets) {
            UnicodeSet source = new UnicodeSet(testSet);
            Set<String> target = new TreeSet<String>();
            for (UnicodeSetIterator it = new UnicodeSetIterator(source); it
                .next();) {
                Set<String> closure = variantFolder.getClosure(it.getString());
                target.addAll(closure);
            }
            UnicodeSet utarget = new UnicodeSet();
            utarget.addAll(target);
            System.out.println(testSet + " => "
                + new UnicodeSet(utarget).removeAll(source));
        }
    }

    static CanonicalIterator canonicalterator = new CanonicalIterator("");

    static Set<String> closeUnderCanonicalization(Set<String> source,
        Set<String> output) {
        for (String item : source) {
            canonicalterator.setSource(item);
            for (String equiv = canonicalterator.next(); equiv != null; equiv = canonicalterator
                .next()) {
                output.add(equiv);
            }
        }
        return output;
    }
}