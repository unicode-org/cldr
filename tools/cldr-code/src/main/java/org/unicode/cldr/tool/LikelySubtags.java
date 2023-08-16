package org.unicode.cldr.tool;

import com.google.common.collect.ImmutableMap;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.LanguageTagParser.OutputOption;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData.Type;
import org.unicode.cldr.util.SupplementalDataInfo.CurrencyDateInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;

public class LikelySubtags {
    static final boolean DEBUG = true;
    static final String TAG_SEPARATOR = "_";
    private static final boolean SKIP_UND = true;

    private boolean favorRegion = false;
    private final Map<String, String> toMaximized;

    /**
     * Create the likely subtags.
     *
     * @param toMaximized
     */
    public LikelySubtags(Map<String, String> toMaximized) {
        this.toMaximized =
                toMaximized == null
                        ? LikelySubtagsData.getInstance().defaultToMaximized
                        : ImmutableMap.copyOf(toMaximized);
    }

    /** thread-safe data loading. Retooled so that the constant data is shared across instances. */
    private static class LikelySubtagsData {
        private static final LikelySubtagsData SINGLETON = new LikelySubtagsData();

        private static LikelySubtagsData getInstance() {
            return SINGLETON;
        }

        private final SupplementalDataInfo supplementalDataInfo =
                CLDRConfig.getInstance().getSupplementalDataInfo();
        private final Map<String, String> defaultToMaximized =
                supplementalDataInfo.getLikelySubtags();
        private final Map<String, String> currencyToLikelyTerritory;

        private LikelySubtagsData() {
            Map<String, String> _currencyToLikelyTerritory = new HashMap<>();
            Date now = new Date();
            Set<Row.R2<Double, String>> sorted = new TreeSet<>();
            for (String territory : supplementalDataInfo.getTerritoriesWithPopulationData()) {
                PopulationData pop = supplementalDataInfo.getPopulationDataForTerritory(territory);
                double population = pop.getPopulation();
                sorted.add(Row.of(-population, territory));
            }
            for (R2<Double, String> item : sorted) {
                String territory = item.get1();
                Set<CurrencyDateInfo> targetCurrencyInfo =
                        supplementalDataInfo.getCurrencyDateInfo(territory);
                if (targetCurrencyInfo == null) {
                    continue;
                }
                for (CurrencyDateInfo cdi : targetCurrencyInfo) {
                    String currency = cdi.getCurrency();
                    if (!_currencyToLikelyTerritory.containsKey(currency)
                            && cdi.getStart().before(now)
                            && cdi.getEnd().after(now)
                            && cdi.isLegalTender()) {
                        _currencyToLikelyTerritory.put(currency, territory);
                    }
                }
            }
            currencyToLikelyTerritory = ImmutableMap.copyOf(_currencyToLikelyTerritory);
        }
    }

    /**
     * Create the likely subtags.
     *
     * @param toMaximized
     */
    public LikelySubtags() {
        this(null);
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

    public static String maximize(String languageTag, Map<String, String> toMaximized) {
        return new LikelySubtags(toMaximized).maximize(languageTag);
    }

    public static String minimize(
            String input, Map<String, String> toMaximized, boolean favorRegion) {
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

    /** Maximize to a string (modifying the LanguageTagParser in so doing) */
    public String maximize(LanguageTagParser ltp) {
        if (maximizeInPlace(ltp)) {
            return ltp.toString();
        } else {
            return null;
        }
    }

    /**
     * Maximize in place, for use when the modified LanguageTagParser is the desired return value
     */
    public boolean maximizeInPlace(LanguageTagParser ltp) {
        String language = ltp.getLanguage();
        String region = ltp.getRegion();
        String script = ltp.getScript();
        List<String> variants = ltp.getVariants();
        Map<String, String> extensions = ltp.getExtensions();
        Map<String, String> localeExtensions = ltp.getLocaleExtensions();

        String sourceLanguage = language;
        String sourceScript = script;
        String sourceRegion = region;

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
            ltp.setVariants(Collections.<String>emptySet());
        }
        if (extensions.size() != 0) {
            ltp.setExtensions(Collections.<String, String>emptyMap());
        }
        if (localeExtensions.size() != 0) {
            ltp.setExtensions(Collections.<String, String>emptyMap());
        }

        if (!ltp.getLanguage().equals("und")
                && !ltp.getScript().isEmpty()
                && !ltp.getRegion().isEmpty()) {
            return true;
        }

        // check whole
        String result = toMaximized.get(ltp.toString());
        if (result != null) {
            ltp.set(result)
                    .setVariants(variants)
                    .setExtensions(extensions)
                    .setLocaleExtensions(localeExtensions);
            return true;
        }

        boolean noLanguage = language.equals("und");
        boolean noScript = script.isEmpty();
        boolean noRegion = region.isEmpty();

        // not efficient, but simple to match spec.
        for (int count = 0; ; ++count) { // breaks down below
            for (String script2 : noScript ? Arrays.asList(script) : Arrays.asList(script, "")) {
                ltp.setScript(script2);

                for (String region2 :
                        noRegion ? Arrays.asList(region) : Arrays.asList(region, "")) {
                    ltp.setRegion(region2);
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
                        ltp.setVariants(variants)
                                .setExtensions(extensions)
                                .setLocaleExtensions(localeExtensions);
                        if (count == 1) {
                            System.out.println(
                                    "2nd pass, "
                                            + new LanguageTagParser()
                                                    .setLanguage(sourceLanguage)
                                                    .setScript(sourceScript)
                                                    .setRegion(sourceRegion)
                                            + " ==> "
                                            + ltp);
                        }
                        return true;
                    }
                }
            }

            if (SKIP_UND || ltp.getLanguage().equals("und")) {
                break;
            } else {
                // Otherwise repeat the loop, trying for und matches
                ltp.setLanguage("und");
            }
        }
        return false; // couldn't maximize
    }

    // TODO, optimize if needed by adding private routine that maximizes a LanguageTagParser instead
    // of multiple parsings
    // TODO Old, crufty code, needs reworking.
    public String minimize(String input) {
        return minimize(input, OutputOption.ICU_LCVARIANT);
    }

    public synchronized String minimize(String input, OutputOption oo) {
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
        String[] trials = {
            language,
            language + TAG_SEPARATOR + (favorRegion ? region : script),
            language + TAG_SEPARATOR + (!favorRegion ? region : script)
        };
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
                        .toString(oo);
            }
        }
        return maximized;
    }

    static final Map<String, String> EXTRA_SCRIPTS =
            Builder.with(new HashMap<String, String>())
                    .on("crs", "pcm", "tlh")
                    .put("Latn")
                    .freeze();

    public String getLikelyScript(String code) {
        String max = this.maximize(code);

        String script = null;
        if (max != null) {
            script = new LanguageTagParser().set(max).getScript();
        } else {
            Map<Type, BasicLanguageData> data =
                    LikelySubtagsData.getInstance()
                            .supplementalDataInfo
                            .getBasicLanguageDataMap(code);
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
        return LikelySubtagsData.getInstance().currencyToLikelyTerritory.get(code);
    }
}
