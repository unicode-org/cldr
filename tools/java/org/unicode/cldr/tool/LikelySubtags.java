package org.unicode.cldr.tool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData.Type;
import org.unicode.cldr.util.SupplementalDataInfo.CurrencyDateInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;

import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;

public class LikelySubtags {
    static final boolean DEBUG = true;
    static final String TAG_SEPARATOR = "_";

    private Map<String, String> toMaximized;
    private boolean favorRegion = false;
    private SupplementalDataInfo supplementalDataInfo;
    private Map<String, String> currencyToLikelyTerritory = new HashMap<String, String>();

    /**
     * Create the likely subtags.
     *
     * @param toMaximized
     */
    public LikelySubtags(Map<String, String> toMaximized) {
        this(SupplementalDataInfo.getInstance(), toMaximized);
    }

    /**
     * Create the likely subtags.
     *
     * @param toMaximized
     */
    public LikelySubtags(SupplementalDataInfo supplementalDataInfo) {
        this(supplementalDataInfo, supplementalDataInfo.getLikelySubtags());
    }

    /**
     * Create the likely subtags.
     *
     * @param toMaximized
     */
    public LikelySubtags(SupplementalDataInfo supplementalDataInfo, Map<String, String> toMaximized) {
        this.supplementalDataInfo = supplementalDataInfo;
        this.toMaximized = toMaximized;

        Date now = new Date();
        Set<Row.R2<Double, String>> sorted = new TreeSet<Row.R2<Double, String>>();
        for (String territory : supplementalDataInfo.getTerritoriesWithPopulationData()) {
            PopulationData pop = supplementalDataInfo.getPopulationDataForTerritory(territory);
            double population = pop.getPopulation();
            sorted.add(Row.of(-population, territory));
        }
        for (R2<Double, String> item : sorted) {
            String territory = item.get1();
            Set<CurrencyDateInfo> targetCurrencyInfo = supplementalDataInfo.getCurrencyDateInfo(territory);
            if (targetCurrencyInfo == null) {
                continue;
            }
            for (CurrencyDateInfo cdi : targetCurrencyInfo) {
                String currency = cdi.getCurrency();
                if (!currencyToLikelyTerritory.containsKey(currency) && cdi.getStart().before(now)
                    && cdi.getEnd().after(now) && cdi.isLegalTender()) {
                    currencyToLikelyTerritory.put(currency, territory);
                }
            }
        }
        // System.out.println("Currency to Territory:\n\t" +
        // CollectionUtilities.join(currencyToLikelyTerritory.entrySet(), "\n\t"));
    }

    /**
     * Create the likely subtags.
     *
     * @param toMaximized
     */
    public LikelySubtags() {
        this(SupplementalDataInfo.getInstance());
    }

    public boolean isFavorRegion() {
        return favorRegion;
    }

    public LikelySubtags setFavorRegion(boolean favorRegion) {
        this.favorRegion = favorRegion;
        return this;
    }

    public Map<String, String> getToMaximized() {
        return toMaximized;
    }

    public LikelySubtags setToMaximized(Map<String, String> toMaximized) {
        this.toMaximized = toMaximized;
        return this;
    }

    public static String maximize(String languageTag, Map<String, String> toMaximized) {
        return new LikelySubtags(toMaximized).maximize(languageTag);
    }

    public static String minimize(String input, Map<String, String> toMaximized, boolean favorRegion) {
        return new LikelySubtags(toMaximized).setFavorRegion(favorRegion).minimize(input);
    }

    public synchronized String maximize(String languageTag) {
        if (languageTag == null) {
            return null;
        }
        LanguageTagParser ltp = new LanguageTagParser();
        if (DEBUG && languageTag.equals("es" + TAG_SEPARATOR + "Hans" + TAG_SEPARATOR + "CN")) {
            System.out.print(""); // debug
        }
        // clean up the input by removing Zzzz, ZZ, and changing "" into und.
        ltp.set(languageTag);
        String language = ltp.getLanguage();
        String region = ltp.getRegion();
        String script = ltp.getScript();
        List<String> variants = ltp.getVariants();
        Map<String, String> extensions = ltp.getExtensions();

        if (language.equals("")) {
            ltp.setLanguage(language = "und");
        }
        if (script.equals("Zzzz")) {
            ltp.setScript(script = "");
        }
        if (region.equals("ZZ")) {
            ltp.setRegion(region = "");
        }
        if (variants.size() != 0) {
            variants = new ArrayList<String>(variants); // make copy
            ltp.setVariants(Collections.<String> emptySet());
        }
        if (extensions.size() != 0) {
            extensions = new LinkedHashMap<String, String>(extensions);
            ltp.setExtensions(Collections.<String, String> emptyMap());
        }

        // check whole
        String result = toMaximized.get(ltp.toString());
        if (result != null) {
            return result;
        }

        boolean noLanguage = language.equals("und");
        boolean noScript = script.isEmpty();
        boolean noRegion = region.isEmpty();

        // not efficient, but simple to match spec.
        for (String region2 : noRegion ? Arrays.asList(region) : Arrays.asList(region, "")) {
            ltp.setRegion(region2);
            for (String script2 : noScript ? Arrays.asList(script) : Arrays.asList(script, "")) {
                ltp.setScript(script2);

                result = toMaximized.get(ltp.toString());
                if (result != null) {
                    ltp.set(result);
                    if (!noLanguage) {
                        ltp.setLanguage(language);
                    }
                    if (!noScript) {
                        ltp.setScript(script);
                    }
                    if (!noRegion) {
                        ltp.setRegion(region);
                    }
                    ltp.setVariants(variants).setExtensions(extensions);
                    return ltp.toString();
                }
            }
        }

        // now check und_script
        if (!noScript) {
            ltp.setLanguage("und");
            ltp.setScript(script);
            result = toMaximized.get(ltp.toString());
            if (result != null) {
                ltp.set(result);
                if (!noLanguage) {
                    ltp.setLanguage(language);
                }
                if (!noScript) {
                    ltp.setScript(script);
                }
                if (!noRegion) {
                    ltp.setRegion(region);
                }
                ltp.setVariants(variants).setExtensions(extensions);
                return ltp.toString();
            }
        }

        //        String result = toMaximized.get(ltp.toString());
        //        if (result != null) {
        //            return ltp.set(result).setVariants(variants).setExtensions(extensions).toString();
        //        }
        //        // try empty region
        //        if (region.length() != 0) {
        //            result = toMaximized.get(ltp.setRegion("").toString());
        //            if (result != null) {
        //                return ltp.set(result).setRegion(region).setVariants(variants).setExtensions(extensions).toString();
        //            }
        //            ltp.setRegion(region); // restore
        //        }
        //        // try empty script
        //        if (script.length() != 0) {
        //            result = toMaximized.get(ltp.setScript("").toString());
        //            if (result != null) {
        //                return ltp.set(result).setScript(script).setVariants(variants).setExtensions(extensions).toString();
        //            }
        //            // try empty script and region
        //            if (region.length() != 0) {
        //                result = toMaximized.get(ltp.setRegion("").toString());
        //                if (result != null) {
        //                    return ltp.set(result).setScript(script).setRegion(region).setVariants(variants)
        //                        .setExtensions(extensions).toString();
        //                }
        //            }
        //        }
        // if (!language.equals("und") && script.length() != 0 && region.length() != 0) {
        // return languageTag; // it was ok, and we couldn't do anything with it
        // }
        return null; // couldn't maximize
    }

    // TODO, optimize if needed by adding private routine that maximizes a LanguageTagParser instead of multiple parsings
    public synchronized String minimize(String input) {
        String maximized = maximize(input, toMaximized);
        if (maximized == null) {
            return null;
        }
        if (DEBUG && maximized.equals("sr" + TAG_SEPARATOR + "Latn" + TAG_SEPARATOR + "RS")) {
            System.out.print(""); // debug
        }
        LanguageTagParser ltp = new LanguageTagParser().set(maximized);
        String language = ltp.getLanguage();
        String region = ltp.getRegion();
        String script = ltp.getScript();

        // handle variants
        List<String> var = ltp.getVariants();
        Map<String, String> ext = ltp.getExtensions();
        String maximizedCheck = maximized;
        if (!var.isEmpty() || !ext.isEmpty()) {
            maximizedCheck = ltp.toLSR();
        }
        // try building up from shorter to longer, and find the first that matches
        // could be more optimized, but for this code we want simplest
        String[] trials = { language,
            language + TAG_SEPARATOR + (favorRegion ? region : script),
            language + TAG_SEPARATOR + (!favorRegion ? region : script) };
        for (String trial : trials) {
            String newMaximized = maximize(trial, toMaximized);
            if (maximizedCheck.equals(newMaximized)) {
                if (var.isEmpty() && ext.isEmpty()) {
                    return trial;
                }
                var = new ArrayList(var);
                ext = new LinkedHashMap(ext);
                ltp.set(trial);
                ltp.setVariants(var);
                ltp.setExtensions(ext);
                return ltp.toString();
            }
        }
        return maximized;
    }

    static final Map<String, String> EXTRA_SCRIPTS =
        Builder.with(new HashMap<String, String>())
        .on("crs", "pcm", "tlh").put("Latn")
        .freeze();

    public String getLikelyScript(String code) {
        String max = this.maximize(code);

        String script = null;
        if (max != null) {
            script = new LanguageTagParser().set(max).getScript();
        } else {
            Map<Type, BasicLanguageData> data = supplementalDataInfo.getBasicLanguageDataMap(code);
            if (data != null) {
                for (BasicLanguageData item : data.values()) {
                    Set<String> scripts = item.getScripts();
                    if (scripts == null || scripts.size() == 0) continue;
                    script = scripts.iterator().next();
                    Type type = item.getType();
                    if (type == Type.primary) {
                        break;
                    }
                }
            }
            if (script == null) {
                script = EXTRA_SCRIPTS.get(code);
                if (script == null) {
                    script = "Zzzz";
                }
            }
        }
        return script;
    }

    public String getLikelyTerritoryFromCurrency(String code) {
        return currencyToLikelyTerritory.get(code);
    }
}
