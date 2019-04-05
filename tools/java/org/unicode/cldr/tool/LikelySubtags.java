package org.unicode.cldr.tool;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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

    // TODO Old, crufty code, needs reworking.
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
        return maximize(ltp);
    }

    private String maximize(LanguageTagParser ltp) {
        String language = ltp.getLanguage();
        String region = ltp.getRegion();
        String script = ltp.getScript();
        List<String> variants = ltp.getVariants();
        Map<String, String> extensions = ltp.getExtensions();
        Map<String, String> localeExtensions = ltp.getLocaleExtensions();

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
            ltp.setVariants(Collections.<String> emptySet());
        }
        if (extensions.size() != 0) {
            ltp.setExtensions(Collections.<String, String> emptyMap());
        }
        if (localeExtensions.size() != 0) {
            ltp.setExtensions(Collections.<String, String> emptyMap());
        }

        // check whole
        String result = toMaximized.get(ltp.toString());
        if (result != null) {
            return ltp.set(result)
                .setVariants(variants)
                .setExtensions(extensions)
                .setLocaleExtensions(localeExtensions)
                .toString();
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
                    return ltp.setVariants(variants)
                        .setExtensions(extensions)
                        .setLocaleExtensions(localeExtensions)
                        .toString();
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
                return ltp.setVariants(variants)
                    .setExtensions(extensions)
                    .setLocaleExtensions(localeExtensions)
                    .toString();
            }
        }

        return null; // couldn't maximize
    }

    // TODO, optimize if needed by adding private routine that maximizes a LanguageTagParser instead of multiple parsings
    // TODO Old, crufty code, needs reworking.
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
        List<String> variants = ltp.getVariants();
        Map<String, String> extensions = ltp.getExtensions();
        Map<String, String> localeExtensions = ltp.getLocaleExtensions();

        String maximizedCheck = maximized;
        if (!variants.isEmpty() || !extensions.isEmpty() || !localeExtensions.isEmpty()) {
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
                if (variants.isEmpty() && extensions.isEmpty() && localeExtensions.isEmpty()) {
                    return trial;
                }
                return ltp.set(trial)
                    .setVariants(variants)
                    .setExtensions(extensions)
                    .setLocaleExtensions(extensions)
                    .toString();
            }
        }
        return maximized;
    }

    static final Map<String, String> EXTRA_SCRIPTS = Builder.with(new HashMap<String, String>())
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
