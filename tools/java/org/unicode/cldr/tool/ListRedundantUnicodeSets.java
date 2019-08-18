package org.unicode.cldr.tool;

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

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;

public class ListRedundantUnicodeSets {
    public static void main(String[] args) {
        System.out.println("LocaleID"
            +"\tTarget Level"
            +"\tExemplarType"
            +"\tNo. Original"
            +"\tNo. Remaining"
            +"\tRemaining (Keep)"
            +"\tEquiv to Code Point & Redundant (maybe edit down & keep)"
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
            for (ExemplarType exemplarType : ExemplarType.values()) {
                if (exemplarType != ExemplarType.main) {
                    continue;
                }
                UnicodeSet exemplarSet = cldrFile.getExemplarSet(exemplarType, WinningChoice.WINNING);
                if (exemplarSet.isEmpty()) {
                    continue;
                }

                Set<String> redundants = UnicodeSets.getRedundantStrings(exemplarSet);
                TreeSet<String> remaining = new TreeSet<>(exemplarSet.strings());
                remaining.removeAll(redundants);

                // now get ones that are canonically equivalent to single string
                Set<String> canonicals = new TreeSet<>();
                for (String r : redundants) {
                    if (hasPrecomposed(r)) {
                        canonicals.add(r);
                    }
                }
                redundants.removeAll(canonicals);

                // now get collation exemplar strings
                Set<String> colExemplars = getCollationExemplars(localeID).addAllTo(new TreeSet<String>());
                Set<String> colAndEx = Collections.emptySet();
                if (!colExemplars.isEmpty()) {
                    colAndEx = new TreeSet<>(colExemplars);
                    colAndEx.retainAll(redundants);
                    redundants.removeAll(colAndEx);
                    colExemplars.removeAll(redundants);
                    colExemplars.removeAll(remaining);
                    colExemplars.removeAll(canonicals);
                }

                System.out.println(localeID 
                    + "\t" + localeCoverageLevel
                    + "\t" + exemplarType 
                    + "\t" + exemplarSet.strings().size()
                    + "\t" + remaining.size()
                    + "\t" + remaining
                    + "\t" + canonicals
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
    
    private static boolean hasPrecomposed(String s) {
        return CAN_BE_COMPOSED.contains(nfkd.normalize(s));
    }
}
