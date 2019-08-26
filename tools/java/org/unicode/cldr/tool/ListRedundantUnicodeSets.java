package org.unicode.cldr.tool;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.ExemplarType;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.UnicodeSets;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;
import com.ibm.icu.util.ULocale;

public class ListRedundantUnicodeSets {

    public static void main(String[] args) {
        System.out.println("LocaleID"
            +"\tTarget Level"
            +"\tExemplarType"
            +"\tNo. Original"
            +"\tNo. Remaining"
            +"\tNot Redundant (KEEP)"
            +"\tRedundant Exceptions (KEEP)"
            +"\tIndexEx & Redundant"
            +"\tCollation & Redundant"
            +"\tOther Redundant"
            +"\tNo. Collation clusters not in exemplars"
            );

        Factory cldrFactory = CLDRConfig.getInstance().getCldrFactory();
        SupplementalDataInfo sdi = CLDRConfig.getInstance().getSupplementalDataInfo();
        StandardCodes sdc = CLDRConfig.getInstance().getStandardCodes();

        for (String localeID : cldrFactory.getAvailable()) {
            Level localeCoverageLevel = sdc.getLocaleCoverageLevel(Organization.cldr, localeID);
            if (localeCoverageLevel == Level.UNDETERMINED) {
                continue;
            }
            CLDRFile cldrFile = cldrFactory.make(localeID, false);
            for (ExemplarType exemplarType : Arrays.asList(ExemplarType.main)) {

                UnicodeSet exemplarSet = cldrFile.getExemplarSet(exemplarType, WinningChoice.WINNING);
                if (exemplarSet.isEmpty()) {
                    continue;
                }

                Set<String> redundants = UnicodeSets.getRedundantStrings(exemplarSet);
                TreeSet<String> remaining = new TreeSet<>(exemplarSet.strings());
                remaining.removeAll(redundants);

                // now get ones that are equivalent to single string
                // (Keep cases for ar, bs, hr, sr; indic)
                Set<String> forTranslit = new TreeSet<>();
                for (String r : redundants) {
                    if (isForTranslit(localeID, r)) {
                        forTranslit.add(r);
                    }
                }
                redundants.removeAll(forTranslit);

                Set<String> indexSet = new TreeSet<>();
                UnicodeSet indexSetRaw = cldrFile.getExemplarSet(ExemplarType.index, WinningChoice.WINNING);
                if (!indexSetRaw.isEmpty()) {
                    ULocale ulocale = new ULocale(localeID);
                    for (String s : indexSetRaw) {
                        String lowerCase = UCharacter.toLowerCase(ulocale, s);
                        if (UnicodeSet.getSingleCodePoint(lowerCase) == Integer.MAX_VALUE) {
                            indexSet.add(lowerCase);
                        }
                    }
                    if (!indexSet.isEmpty()) {
                        redundants.removeAll(indexSet);
                        indexSet.removeAll(redundants);
                    }
                }

                // now get collation exemplar strings
                Set<String> colExemplars = getCollationExemplars(localeID).addAllTo(new TreeSet<String>());
                Set<String> colAndEx = Collections.emptySet();
                if (!colExemplars.isEmpty()) {
                    colAndEx = new TreeSet<>(colExemplars);
                    colAndEx.retainAll(redundants);
                    redundants.removeAll(colAndEx);
                    colExemplars.removeAll(redundants);
                    colExemplars.removeAll(remaining);
                    colExemplars.removeAll(forTranslit);
                }

                System.out.println(localeID 
                    + "\t" + localeCoverageLevel
                    + "\t" + exemplarType 
                    + "\t" + exemplarSet.strings().size()
                    + "\t" + (remaining.size() + forTranslit.size())
                    + "\t" + remaining
                    + "\t" + forTranslit
                    + "\t" + indexSet
                    + "\t" + colAndEx
                    + "\t" + redundants 
                    + "\t" + colExemplars.size()
                    );
            }
        }
    }

    private static UnicodeSet ROOT_COLLATION_EXEMPLARS = null;

    private static UnicodeSet getCollationExemplars(String localeID) {
        if (ROOT_COLLATION_EXEMPLARS == null) {
            ROOT_COLLATION_EXEMPLARS = getCollationExemplars2("root");
        }
        UnicodeSet result = getCollationExemplars2(localeID).removeAll(ROOT_COLLATION_EXEMPLARS);
        return result;
    }

    private static UnicodeSet getCollationExemplars2(String localeID) {
        try {
            Locale locale = new Locale(localeID);
            ICUServiceBuilder builder = ICUServiceBuilder.forLocale(CLDRLocale.getInstance(localeID));
            RuleBasedCollator col = builder.getRuleBasedCollator();
            UnicodeSet contractions = new UnicodeSet();
            UnicodeSet expansions = new UnicodeSet();
            col.getContractionsAndExpansions(contractions, expansions, true);
            UnicodeSet result = new UnicodeSet();
            for (String s : contractions) {
                result.add(s.toLowerCase(locale));
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    static final Normalizer2 nfkd = Normalizer2.getNFKDInstance();
    static final Set<String> CAN_BE_COMPOSED; 

    static {
        Set<String> _toComposed = new TreeSet<>();
        UnicodeSet options = new UnicodeSet("[[:nfkcqc=n:]-[:dt=final:]-[:dt=medial:]-[:dt=initial:]-[:dt=isolated:]]");
        StringBuffer b = new StringBuffer();
        for (EntryRange range : options.ranges()) {
            for (int cp = range.codepoint; cp <= range.codepointEnd; ++cp) {
                b.setLength(0);
                String result = nfkd.normalize(b.appendCodePoint(cp));
                if (UnicodeSet.getSingleCodePoint(result) == Integer.MAX_VALUE) {
                    _toComposed.add(result);
                }
            }
        }
        CAN_BE_COMPOSED = ImmutableSet.copyOf(_toComposed);
    }

    static final UnicodeSet NUKTA = new UnicodeSet("[:Indic_Syllabic_Category=nukta:]").freeze();
    static final Multimap<String,String> TRANSLIT_CLUSTERS = ImmutableMultimap.<String,String>builder()
        .putAll("bs", "dž", "lj", "nj")
        .putAll("hr", "dž", "lj", "nj")
        .putAll("sr_Latn", "dž", "lj", "nj")
        .build();
    private static boolean isForTranslit(String locale, String s) {
        boolean result = CAN_BE_COMPOSED.contains(nfkd.normalize(s));
        if (result && (NUKTA.containsSome(s) 
            || TRANSLIT_CLUSTERS.containsEntry(locale, s))) {
            return true;
        };
        return false;
    }
}
