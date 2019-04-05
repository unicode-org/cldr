/*
 **********************************************************************
 * Copyright (c) 2002-2011, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.tool.LikelySubtags;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.UnicodeSet;

public class LanguageTagParser {

    private static final Joiner HYPHEN_JOINER = Joiner.on('-');

    private static final Comparator<? super String> EXTENSION_ORDER = new Comparator<String>() {

        @Override
        public int compare(String o1, String o2) {
            int diff = getBucket(o1) - getBucket(o2);
            if (diff != 0) {
                return diff;
            }
            return o1.compareTo(o2);
        }

        private int getBucket(String o1) {
            switch (o1.length()) {
            case 1:
                return o1.charAt(0) == 't' ? 0 : 2;
            case 2:
                return o1.charAt(1) <= '9' ? 1 : 3;
            default: 
                throw new IllegalArgumentException();
            }
        }
    };

    /**
     * @return Returns the language, or "" if none.
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @return Returns the script, or "" if none.
     */
    public String getScript() {
        return script;
    }

    /**
     * @return Returns the region, or "" if none.
     */
    public String getRegion() {
        return region;
    }

    /**
     * @return Returns the variants.
     */
    public List<String> getVariants() {
        return ImmutableList.copyOf(variants);
    }

    /**
     * @return Returns the grandfathered flag
     */
    public boolean isGrandfathered() {
        return grandfathered;
    }

    /**
     * @return Returns the extensions.
     */
    @Deprecated
    public Map<String, String> getExtensions() {
        return OutputOption.ICU.convert(extensions);
    }

    /**
     * @return Returns the localeExtensions.
     */
    @Deprecated
    public Map<String, String> getLocaleExtensions() {
        return OutputOption.ICU.convert(localeExtensions);
    }

    /**
     * @return Returns the extensions.
     */
    public Map<String, List<String>> getExtensionsDetailed() {
        return ImmutableMap.copyOf(extensions);
    }

    /**
     * @return Returns the localeExtensions.
     */
    public Map<String, List<String>> getLocaleExtensionsDetailed() {
        return ImmutableMap.copyOf(localeExtensions);
    }

    /**
     * @return Returns the original, preparsed language tag
     */
    public String getOriginal() {
        return original;
    }

    /**
     * @return Returns the language-script (or language) part of a tag.
     */
    public String getLanguageScript() {
        if (script.length() != 0) return language + "_" + script;
        return language;
    }

    /**
     * @param in
     *            Collection of language tag strings
     * @return Returns each of the language-script tags in the collection.
     */
    public static Set<String> getLanguageScript(Collection<String> in) {
        return getLanguageAndScript(in, null);
    }

    /**
     * @param in
     *            Collection of language tag strings
     * @return Returns each of the language-script tags in the collection.
     */
    public static Set<String> getLanguageAndScript(Collection<String> in, Set<String> output) {
        if (output == null) output = new TreeSet<String>();
        LanguageTagParser lparser = new LanguageTagParser();
        for (Iterator<String> it = in.iterator(); it.hasNext();) {
            output.add(lparser.set(it.next()).getLanguageScript());
        }
        return output;
    }

    // private fields

    private String original;
    private boolean grandfathered = false;
    private String language;
    private String script;
    private String region;
    private Set<String> variants = new TreeSet<String>();
    private Map<String, List<String>> extensions = new TreeMap<String, List<String>>(); // use tree map
    private Map<String, List<String>> localeExtensions = new TreeMap<String, List<String>>(EXTENSION_ORDER);

    private static final UnicodeSet ALPHA = new UnicodeSet("[a-zA-Z]").freeze();
    private static final UnicodeSet DIGIT = new UnicodeSet("[0-9]").freeze();
    private static final UnicodeSet ALPHANUM = new UnicodeSet("[0-9a-zA-Z]").freeze();
    private static final UnicodeSet EXTENSION_VALUE = new UnicodeSet("[0-9a-zA-Z/_]").freeze();
    private static final UnicodeSet X = new UnicodeSet("[xX]").freeze();
    private static final UnicodeSet ALPHA_MINUS_X = new UnicodeSet(ALPHA).removeAll(X).freeze();
    private static StandardCodes standardCodes = StandardCodes.make();
    private static final Set<String> grandfatheredCodes = standardCodes.getAvailableCodes("grandfathered");
    private static final String separator = "-_"; // '-' alone for 3066bis language tags
    private static final UnicodeSet SEPARATORS = new UnicodeSet().addAll(separator).freeze();
    private static final Splitter SPLIT_BAR = Splitter.on(CharMatcher.anyOf(separator));
    private static final Splitter SPLIT_COLON = Splitter.on(';');
    private static final Splitter SPLIT_EQUAL = Splitter.on('=');
    private static final SupplementalDataInfo SDI = SupplementalDataInfo.getInstance();
    private static final Relation<R2<String, String>, String> BCP47_ALIASES = SDI.getBcp47Aliases();

    /**
     * Parses out a language tag, setting a number of fields that can subsequently be retrieved.
     * If a private-use field is found, it is returned as the last extension.<br>
     * This only checks for well-formedness (syntax), not for validity (subtags in registry). For the latter, see
     * isValid.
     *
     * @param languageTag
     * @return
     */
    public LanguageTagParser set(String languageTag) {
        if (languageTag.length() == 0 || languageTag.equals("root")) {
            // throw new IllegalArgumentException("Language tag cannot be empty");
            //
            // With ICU 64 the language tag for root is normalized to empty string so we
            // cannot throw for empty string as above. However, code here and in clients
            // assumes a non-empty language tag, so for now just map "" or "root" to "und".
            languageTag = "und";
        } else if (languageTag.startsWith("_") || languageTag.startsWith("-")) {
            languageTag = "und" + languageTag;
        }
        languageTag = languageTag.toLowerCase(Locale.ROOT);

        // clear everything out
        language = region = script = "";
        grandfathered = false;
        variants.clear();
        extensions.clear();
        localeExtensions.clear();
        original = languageTag;
        int atPosition = languageTag.indexOf('@');
        if (atPosition >= 0) {
            final String extensionsString = languageTag.substring(atPosition + 1).toLowerCase(Locale.ROOT);
            for (String keyValue : SPLIT_COLON.split(extensionsString)) {
                final Iterator<String> keyValuePair = SPLIT_EQUAL.split(keyValue).iterator();
                final String key = keyValuePair.next();
                final String value = keyValuePair.next();
                if (keyValuePair.hasNext() || !ALPHANUM.containsAll(key) || !EXTENSION_VALUE.containsAll(value)) {
                    throwError(keyValue, "Invalid key/value pair");
                }
                List<String> valueList = SPLIT_BAR.splitToList(value);
                switch(key.length()) {
                case 1: 
                    extensions.put(key, valueList);
                    break;
                case 2:
                    localeExtensions.put(key, valueList);
                    break;
                default:
                    throwError(keyValue, "Invalid key/value pair");
                    break;
                }
            }
            languageTag = languageTag.substring(0, atPosition);
        }

        // first test for grandfathered
        if (grandfatheredCodes.contains(languageTag)) {
            language = languageTag;
            grandfathered = true;
            return this;
        }

        // each time we fetch a token, we check for length from 1..8, and all alphanum
        StringTokenizer st = new StringTokenizer(languageTag, separator);
        String subtag;
        try {
            subtag = getSubtag(st);
        } catch (Exception e1) {
            throw new IllegalArgumentException("Illegal language tag: " + languageTag, e1);
        }

        // check for private use (x-...) and return if so
        if (subtag.equalsIgnoreCase("x")) {
            getExtension(subtag, st, 1);
            return this;
        }

        // check that language subtag is valid
        if (!ALPHA.containsAll(subtag) || subtag.length() < 2) {
            throwError(subtag, "Invalid language subtag");
        }
        try { // The try block is to catch the out-of-tokens case. Easier than checking each time.
            language = subtag;
            subtag = getSubtag(st); // prepare for next

            // check for script, 4 letters
            if (subtag.length() == 4 && ALPHA.containsAll(subtag)) {
                script = subtag;
                script = script.substring(0, 1).toUpperCase(Locale.ROOT)
                    + script.substring(1);
                subtag = getSubtag(st); // prepare for next
            }

            // check for region, 2 letters or 3 digits
            if (subtag.length() == 2 && ALPHA.containsAll(subtag)
                || subtag.length() == 3 && DIGIT.containsAll(subtag)) {
                region = subtag.toUpperCase(Locale.ENGLISH);
                subtag = getSubtag(st); // prepare for next
            }

            // get variants: length > 4 or len=4 & starts with digit
            while (isValidVariant(subtag)) {
                variants.add(subtag);
                subtag = getSubtag(st); // prepare for next
            }

            // get extensions: singleton '-' subtag (2-8 long)
            while (subtag.length() == 1 && ALPHA_MINUS_X.contains(subtag)) {
                subtag = getExtension(subtag, st, 2);
                if (subtag == null) return this; // done
            }

            if (subtag.equalsIgnoreCase("x")) {
                getExtension(subtag, st, 1);
                return this;
            }

            // if we make it to this point, then we have an error
            throwError(subtag, "Illegal subtag");

        } catch (NoSuchElementException e) {
            // this exception just means we ran out of tokens. That's ok, so we just return.
        }
        return this;
    }

    private boolean isValidVariant(String subtag) {
        return subtag != null && ALPHANUM.containsAll(subtag)
            && (subtag.length() > 4 || subtag.length() == 4 && DIGIT.contains(subtag.charAt(0)));
    }

    /**
     *
     * @return true iff the language tag validates
     */
    public boolean isValid() {
        if (grandfathered) return true; // don't need further checking, since we already did so when parsing
        if (!validates(language, "language")) return false;
        if (!validates(script, "script")) return false;
        if (!validates(region, "territory")) return false;
        for (Iterator<String> it = variants.iterator(); it.hasNext();) {
            if (!validates(it.next(), "variant")) return false;
        }
        return true; // passed the gauntlet
    }

    public enum Status {
        WELL_FORMED, VALID, CANONICAL, MINIMAL
    }

    public Status getStatus(Set<String> errors) {
        errors.clear();
        if (!isValid()) {
            return Status.WELL_FORMED;
            // TODO, check the bcp47 extension codes also
        }
        Map<String, Map<String, R2<List<String>, String>>> aliasInfo = SDI.getLocaleAliasInfo();
        Map<String, Map<String, String>> languageInfo = StandardCodes.getLStreg().get("language");

        if (aliasInfo.get("language").containsKey(language)) {
            errors.add("Non-canonical language: " + language);
        }
        Map<String, String> lstrInfo = languageInfo.get(language);
        if (lstrInfo != null) {
            String scope = lstrInfo.get("Scope");
            if ("collection".equals(scope)) {
                errors.add("Collection language: " + language);
            }
        }
        if (aliasInfo.get("script").containsKey(script)) {
            errors.add("Non-canonical script: " + script);
        }
        if (aliasInfo.get("territory").containsKey(region)) {
            errors.add("Non-canonical region: " + region);
        }
        if (!errors.isEmpty()) {
            return Status.VALID;
        }
        String tag = language + (script.isEmpty() ? "" : "_" + script) + (region.isEmpty() ? "" : "_" + region);
        String minimized = LikelySubtags.minimize(tag, SDI.getLikelySubtags(), false);
        if (minimized == null) {
            errors.add("No minimal data for:" + tag);
            if (script.isEmpty() && region.isEmpty()) {
                return Status.MINIMAL;
            } else {
                return Status.CANONICAL;
            }
        }
        if (!tag.equals(minimized)) {
            errors.add("Not minimal:" + tag + "-->" + minimized);
            return Status.CANONICAL;
        }
        return Status.MINIMAL;
    }

    /**
     * @param subtag
     * @param type
     * @return true if the subtag is empty, or if it is in the registry
     */
    private boolean validates(String subtag, String type) {
        return subtag.length() == 0 || standardCodes.getAvailableCodes(type).contains(subtag);
    }

    /**
     * Internal method
     *
     * @param minLength
     *            TODO
     */
    private String getExtension(String subtag, StringTokenizer st, int minLength) {
        String base = subtag;
        final char extension = subtag.charAt(0);
        if (extensions.containsKey(subtag)) {
            throwError(subtag, "Can't have two extensions with the same key");
        }
        if (!st.hasMoreElements()) {
            throwError(subtag, "Private Use / Extension requires subsequent subtag");
        }
        boolean takesSubkeys = extension == 'u' || extension == 't';
        boolean firstT = extension == 't';
        boolean haveContents = false;
        List<String> result = new ArrayList<>();
        try {
            while (st.hasMoreElements()) {
                subtag = getSubtag(st);
                if (subtag.length() < minLength) {
                    return subtag;
                }
                if (takesSubkeys 
                    && subtag.length() == 2 
                    && (!firstT || isTKey(subtag))) { // start new key-value pair
                    if (!result.isEmpty() || base.length() != 1) { // don't add empty t- or u-
                        localeExtensions.put(base, ImmutableList.copyOf(result));
                        haveContents = true;
                        result.clear();
                    }
                    base = subtag;
                    continue;
                }
                firstT = false;
                result.add(subtag);
            }
            return null;
        } finally {
            if (takesSubkeys) {
                if (!result.isEmpty() || base.length() != 1) { // don't add empty t- or u-
                    localeExtensions.put(base, ImmutableList.copyOf(result));
                    haveContents = true;
                }
                if (!haveContents) {
                    throw new IllegalArgumentException("extension must not be empty: " + base);
                }
            } else {
                if (result.isEmpty()) {
                    throw new IllegalArgumentException("extension must not be empty: " + base);
                }
                extensions.put(base, ImmutableList.copyOf(result));
            }
        }
    }

    /**
     * Internal method
     */
    private String getSubtag(StringTokenizer st) {
        String result = st.nextToken();
        if (result.length() < 1 || result.length() > 8) {
            throwError(result, "Illegal length (must be 1..8)");
        }
        if (!ALPHANUM.containsAll(result)) {
            throwError(result, "Illegal characters (" + new UnicodeSet().addAll(result).removeAll(ALPHANUM) + ")");
        }
        return result;
    }

    /**
     * Internal method
     */
    private void throwError(String subtag, String errorText) {
        throw new IllegalArgumentException(errorText + ": " + subtag + " in " + original);
    }

    public LanguageTagParser setRegion(String region) {
        this.region = region;
        return this;
    }

    public LanguageTagParser setScript(String script) {
        this.script = script;
        return this;
    }

    public enum OutputOption {
        ICU('_'), BCP47('-');
        final char separator;
        final Joiner joiner;

        private OutputOption(char separator) {
            this.separator = separator;
            joiner = Joiner.on(separator);
        }

        public Map<String, String> convert(Map<String, List<String>> mapToList) {
            if (mapToList.isEmpty()) {
                return Collections.emptyMap();
            }
            ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
            for (Entry<String, List<String>> entry : mapToList.entrySet()) {
                builder.put(entry.getKey(), joiner.join(entry.getValue()));
            }
            return builder.build();
        }
    }

    public String toString() {
        return toString(OutputOption.ICU);
    }

    public String toString(OutputOption oo) {
        StringBuilder result = new StringBuilder(language); // optimize for the simple cases
        if (this.script.length() != 0) result.append(oo.separator).append(script);
        if (this.region.length() != 0) result.append(oo.separator).append(region);
        if (this.variants.size() != 0) {
            for (String variant : variants) {
                result.append(oo.separator).append(oo != OutputOption.ICU ? variant : variant.toUpperCase(Locale.ROOT));
            }
        }
        boolean haveAt = false;
        boolean needSep = false;

        StringBuilder extensionsAfterU = null;
        StringBuilder extensionX = null;
        if (this.extensions.size() != 0) {
            StringBuilder target = result;
            for (Entry<String, List<String>> extension : extensions.entrySet()) {
                String key = extension.getKey();
                String value = oo.joiner.join(extension.getValue());
                switch (key) {
                case "v":
                case "w":
                case "y":
                case "z":
                    if (extensionsAfterU == null) {
                        extensionsAfterU = new StringBuilder();
                    }
                    target = extensionsAfterU;
                    break;
                case "x":
                    if (extensionX == null) {
                        extensionX = new StringBuilder();
                    }
                    target = extensionX;
                    break;
                default:
                    // no action; we already have target set right for earlier items.
                }
                if (oo == OutputOption.BCP47) {
                    target.append(oo.separator).append(key)
                    .append(oo.separator).append(value);
                } else {
                    if (!haveAt) {
                        target.append('@');
                        haveAt = true;
                    }
                    if (needSep) {
                        target.append(";");
                    } else {
                        needSep = true;
                    }
                    target.append(key)
                    .append('=').append(value);
                }
            }
        }
        if (this.localeExtensions.size() != 0) {
            if (oo == OutputOption.BCP47) {
                List<String> tValue = localeExtensions.get("t");
                if (tValue != null) {
                    result.append(oo.separator).append('t')
                    .append(oo.separator).append(oo.joiner.join(tValue));
                    for (Entry<String, List<String>> extension : localeExtensions.entrySet()) {
                        String key = extension.getKey();
                        if (isTKey(key)) {
                            String value = oo.joiner.join(extension.getValue());
                            result.append(oo.separator).append(key).append(oo.separator).append(value);
                        }
                    }
                }
                boolean haveU = false;
                for (Entry<String, List<String>> extension : localeExtensions.entrySet()) {
                    if (!haveU) {
                        List<String> uValue = localeExtensions.get("u");
                        result.append(oo.separator).append('u');
                        if (uValue != null) {
                            result.append(oo.separator).append(oo.joiner.join(tValue));
                        }
                        haveU = true;
                    }
                    String key = extension.getKey();
                    if (key.length() == 2 && key.charAt(1) >= 'a') {
                        String value = oo.joiner.join(extension.getValue());
                        result.append(oo.separator).append(key).append(oo.separator).append(value);
                    }
                }
            } else {
                if (!haveAt) {
                    result.append('@');
                }
                for (Entry<String, List<String>> extension : localeExtensions.entrySet()) {
                    if (needSep) {
                        result.append(";");
                    } else {
                        needSep = true;
                    }
                    String key = extension.getKey();
                    String value = oo.joiner.join(extension.getValue());
                    result.append(key.toUpperCase(Locale.ROOT))
                    .append('=').append(value.toUpperCase(Locale.ROOT));
                }
            }
        }
        // do extensions after u, with x last
        if (extensionsAfterU != null) {
            result.append(extensionsAfterU);
        }
        if (extensionX != null) {
            result.append(extensionX);
        }
        return result.toString();
    }

    public static boolean isTKey(String key) {
        return key.length() == 2 && key.charAt(1) < 'a';
    }

    /**
     * Return just the language, script, and region (no variants or extensions)
     * @return
     */
    public String toLSR() {
        String result = language; // optimize for the simple cases
        if (this.script.length() != 0) result += "_" + script;
        if (this.region.length() != 0) result += "_" + region;
        return result;
    }

    public enum Fields {
        LANGUAGE, SCRIPT, REGION, VARIANTS
    };

    public static Set<Fields> LANGUAGE_SCRIPT = Collections.unmodifiableSet(EnumSet.of(Fields.LANGUAGE, Fields.SCRIPT));
    public static Set<Fields> LANGUAGE_REGION = Collections.unmodifiableSet(EnumSet.of(Fields.LANGUAGE, Fields.REGION));
    public static Set<Fields> LANGUAGE_SCRIPT_REGION = Collections.unmodifiableSet(EnumSet.of(Fields.LANGUAGE,
        Fields.SCRIPT, Fields.REGION));

    public String toString(Set<Fields> selection) {
        String result = language;
        if (selection.contains(Fields.SCRIPT) && script.length() != 0) result += "_" + script;
        if (selection.contains(Fields.REGION) && region.length() != 0) result += "_" + region;
        if (selection.contains(Fields.VARIANTS) && variants.size() != 0) {
            for (String variant : (Collection<String>) variants) {
                result += "_" + variant;
            }
        }
        return result;
    }

    public LanguageTagParser setLanguage(String language) {
        if (SEPARATORS.containsSome(language)) {
            String oldScript = script;
            String oldRegion = region;
            Set<String> oldVariants = variants;
            set(language);
            if (script.length() == 0) {
                script = oldScript;
            }
            if (region.length() == 0) {
                region = oldRegion;
            }
            if (oldVariants.size() != 0) {
                variants = oldVariants;
            }
        } else {
            this.language = language;
        }
        return this;
    }

    public LanguageTagParser setLocaleExtensions(Map<String, String> localeExtensions) {
        this.localeExtensions = expandMap(localeExtensions, 1, Integer.MAX_VALUE);
        return this;
    }

    public LanguageTagParser setVariants(Collection<String> newVariants) {
        for (String variant : newVariants) {
            if (!isValidVariant(variant)) {
                throw new IllegalArgumentException("Illegal variant: " + variant);
            }
        }
        variants.clear();
        variants.addAll(newVariants);
        return this;
    }

    static final Pattern EXTENSION_PATTERN = PatternCache.get("([0-9a-zA-Z]{2,8}(-[0-9a-zA-Z]{2,8})*)?");

    public LanguageTagParser setExtensions(Map<String, String> newExtensions) {
        this.extensions = expandMap(newExtensions, 2, 8);
        return this;
    }

    public static String getSimpleParent(String s) {
        int lastBar = s.lastIndexOf('_');
        return lastBar >= 0 ? s.substring(0, lastBar) : "";
    }

    private Map<String, List<String>> expandMap(Map<String, String> newLocaleExtensions, int minLength, int maxLength) {
        if (newLocaleExtensions.isEmpty()) {
            return Collections.emptyMap();
        }
        ImmutableMap.Builder<String, List<String>> result = ImmutableMap.builder();
        for (Entry<String, String> entry : newLocaleExtensions.entrySet()) {
            result.put(entry.getKey(), split(entry.getValue(), minLength, maxLength));
        }
        return result.build();
    }

    private List<String> split(String value, int minLength, int maxLength) {
        List<String> values = SPLIT_BAR.splitToList(value);
        for (String s : values) {
            if (s.length() < minLength || s.length() > maxLength) {
                throw new IllegalArgumentException("Illegal subtag length for: " + s);
            }
            if (!ALPHANUM.contains(s)) {
                throw new IllegalArgumentException("Illegal locale character in: " + s);
            }
        }
        return values;
    }

    public enum Format {icu("_","_"), bcp47("-","-"), structure("; ", "=");
        public final String separator;
        public final String separator2;
        private Format(String separator, String separator2) {
            this.separator = separator;
            this.separator2 = separator2;
        }
    };

    public String toString(Format format) {
        StringBuilder result = new StringBuilder();
        if (format == Format.structure) {
            result.append("[");
        }
        appendField(format, result, "language", language);
        appendField(format, result, "script", script);
        appendField(format, result, "region", region);
        appendField(format, result, "variants", variants);
        appendField(format, result, "extensions", extensions, new UnicodeSet('a','s'));
        appendField(format, result, "localeX", localeExtensions, null);
        appendField(format, result, "extensions", extensions,  new UnicodeSet('v','w', 'y','z'));
        appendField(format, result, "extensions", extensions, new UnicodeSet('x','x'));
        if (format == Format.structure) {
            result.append("]");
        }
//            if (script.length() != 0) {
//                result. += "_" + script;
//            }
//            if (selection.contains(Fields.REGION) && region.length() != 0) result += "_" + region;
//            if (selection.contains(Fields.VARIANTS) && variants.size() != 0) {
//                for (String variant : (Collection<String>) variants) {
//                    result += "_" + variant;
//                }
//            }
        return result.toString();
    }

    private void appendField(Format format, StringBuilder result, String fieldName, String fieldValue) {
        if (!fieldValue.isEmpty()) {
            if (result.length() > 1) {
                result.append(format.separator);
            }
            if (format == Format.structure) {
                result.append(fieldName).append("=");
            }
            result.append(fieldValue);
        }
    }

    private void appendFieldKey(Format format, StringBuilder result, String fieldName, String fieldValue) {
        result.append(format.separator).append(fieldName).append(format.separator2).append(fieldValue);
    }

    private void appendField(Format format, StringBuilder result, String fieldName, Collection<String> fieldValues) {
        if (!fieldValues.isEmpty()) {
            appendField(format, result, fieldName, CollectionUtilities.join(fieldValues, ","));
        }
    }

    /**
     * null match means it is -t- or -u-
     */
    private void appendField(Format format, StringBuilder result, String fieldName, Map<String, List<String>> fieldValues, UnicodeSet match) {
        if (match == null && format != Format.structure) {
            List<String> tLang = fieldValues.get("t");
            List<String> uSpecial = fieldValues.get("u");
            boolean haveTLang = tLang != null;
            boolean haveUSpecial = uSpecial != null;

            // do all the keys ending with digits first
            boolean haveT = false;
            boolean haveU = false;
            StringBuilder result2 = new StringBuilder(); // put -u- at end
            for (Entry<String, List<String>> entry : fieldValues.entrySet()) {
                String key = entry.getKey();
                if (key.length() < 2) {
                    continue;
                }
                int lastChar = key.codePointBefore(key.length());
                if (lastChar < 'a') {
                    if (!haveT) {
                        result.append(format.separator).append('t');
                        if (haveTLang) { // empty is illegal, but just in case
                            result.append(format.separator).append(CollectionUtilities.join(tLang, format.separator));
                            haveTLang = false;
                        }
                        haveT = true;
                    }
                    appendFieldKey(format, result, entry.getKey(), CollectionUtilities.join(entry.getValue(), format.separator));
                } else {
                    if (!haveU) {
                        result2.append(format.separator).append('u');
                        if (haveUSpecial) { // not yet valid, but just in case
                            result2.append(format.separator).append(CollectionUtilities.join(uSpecial, format.separator));
                            haveUSpecial = false;
                        }
                        haveU = true;
                    }
                    appendFieldKey(format, result2, entry.getKey(), CollectionUtilities.join(entry.getValue(), format.separator));
                }
            }
            if (haveTLang) {
                result.append(format.separator).append('t').append(format.separator).append(CollectionUtilities.join(tLang, format.separator));
            }
            if (haveUSpecial) {
                result2.append(format.separator).append('u').append(format.separator).append(CollectionUtilities.join(uSpecial, format.separator));
            }
            result.append(result2); // put in right order
        } else {
            for (Entry<String, List<String>> entry : fieldValues.entrySet()) {
                if (match == null || match.contains(entry.getKey())) {
                    appendFieldKey(format, result, entry.getKey(), CollectionUtilities.join(entry.getValue(), format.separator));
                }
            }
        }
    }
}