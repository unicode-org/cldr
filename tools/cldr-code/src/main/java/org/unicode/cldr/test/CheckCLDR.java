/*
 ******************************************************************************
 * Copyright (C) 2005-2014, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */

package org.unicode.cldr.test;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.text.ListFormatter;
import com.ibm.icu.text.MessageFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.icu.dev.util.ElapsedTimer;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRInfo.CandidateInfo;
import org.unicode.cldr.util.CLDRInfo.PathValueInfo;
import org.unicode.cldr.util.CLDRInfo.UserInfo;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.InternalCldrException;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.SurveyToolStatus;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.RegexFileParser;
import org.unicode.cldr.util.RegexFileParser.RegexLineParser;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.util.VoteResolver.Status;

/**
 * This class provides a foundation for both console-driven CLDR tests, and Survey Tool Tests.
 *
 * <p>To add a test, subclass CLDRFile and override handleCheck and possibly setCldrFileToCheck.
 * Then put the test into getCheckAll.
 *
 * <p>To use the test, take a look at the main in ConsoleCheckCLDR. Note that you need to call
 * setDisplayInformation with the CLDRFile for the locale that you want the display information (eg
 * names for codes) to be in.<br>
 * Some options are passed in the Map options. Examples: boolean SHOW_TIMES =
 * options.containsKey("SHOW_TIMES"); // for printing times for doing setCldrFileToCheck.
 *
 * <p>Some errors/warnings will be explicitly filtered out when calling CheckCLDR's check() method.
 * The full list of filters can be found in org/unicode/cldr/util/data/CheckCLDR-exceptions.txt.
 *
 * @author davis
 */
public abstract class CheckCLDR implements CheckAccessor {

    /** protected so subclasses can use it */
    protected static Logger logger = Logger.getLogger(CheckCLDR.class.getSimpleName());

    /**
     * set the internal logger level. For ConsoleCheck.
     *
     * @returns the previous level
     */
    public static java.util.logging.Level setLoggerLevel(java.util.logging.Level newLevel) {
        // NB: we use the full package name here, to avoid conflict with other CLDR classes named
        // Level
        java.util.logging.Level oldLevel = logger.getLevel();
        logger.setLevel(newLevel);
        return oldLevel;
    }

    /** serialize CheckCLDR as just its class name */
    public String toString() {
        return getClass().getSimpleName();
    }

    public static final boolean LIMITED_SUBMISSION =
            false; // TODO: CLDR-13337: represent differently

    private static CLDRFile displayInformation;

    private CLDRFile cldrFileToCheck;
    private CLDRFile englishFile = null;

    private boolean skipTest = false;
    private Phase phase;
    private Map<Subtype, List<Pattern>> filtersForLocale = new HashMap<>();

    @Override
    public String getStringValue(String path) {
        return getCldrFileToCheck().getStringValue(path);
    }

    @Override
    public String getUnresolvedStringValue(String path) {
        return getCldrFileToCheck().getUnresolved().getStringValue(path);
    }

    @Override
    public String getLocaleID() {
        return getCldrFileToCheck().getLocaleID();
    }

    @Override
    public CheckCLDR getCause() {
        return this;
    }

    public enum InputMethod {
        DIRECT,
        BULK
    }

    public enum StatusAction {
        /** Allow voting and add new values (in Change column). */
        ALLOW,
        /** Allow voting and ticket (in Change column). */
        ALLOW_VOTING_AND_TICKET,
        /** Allow voting but no add new values (in Change column). */
        ALLOW_VOTING_BUT_NO_ADD,
        /** Only allow filing a ticket. */
        ALLOW_TICKET_ONLY,
        /** Disallow (for various reasons) */
        FORBID_ERRORS(true),
        FORBID_READONLY(true),
        FORBID_UNLESS_DATA_SUBMISSION(true),
        FORBID_NULL(true),
        FORBID_ROOT(true),
        FORBID_CODE(true),
        FORBID_PERMANENT_WITHOUT_FORUM(true);

        private final boolean isForbidden;

        private StatusAction() {
            isForbidden = false;
        }

        private StatusAction(boolean isForbidden) {
            this.isForbidden = isForbidden;
        }

        public boolean isForbidden() {
            return isForbidden;
        }

        public boolean canVote() {
            // the one non-voting case
            if (this == ALLOW_TICKET_ONLY) return false;
            return !isForbidden();
        }
    }

    private static final HashMap<String, Phase> PHASE_NAMES = new HashMap<>();

    public enum Phase {
        BUILD,
        SUBMISSION,
        VETTING,
        FINAL_TESTING("RESOLUTION");

        Phase(String... alternateName) {
            for (String name : alternateName) {
                PHASE_NAMES.put(name.toUpperCase(Locale.ENGLISH), this);
            }
        }

        public static Phase forString(String value) {
            if (value == null) {
                return org.unicode.cldr.util.CLDRConfig.getInstance().getPhase();
            }
            value = value.toUpperCase(Locale.ENGLISH);
            Phase result = PHASE_NAMES.get(value);
            return result != null ? result : Phase.valueOf(value);
        }

        /** true if it's a 'unit test' phase. */
        public boolean isUnitTest() {
            return this == BUILD || this == FINAL_TESTING;
        }

        /**
         * Return whether or not to show a row, and if so, how.
         *
         * @param pathValueInfo - may be null for a non-path entry.
         * @param inputMethod
         * @param ph the path header - may be null if it is a non-path entry
         * @param userInfo null if there is no userInfo (nobody logged in).
         * @return
         */
        public StatusAction getShowRowAction(
                PathValueInfo pathValueInfo,
                InputMethod inputMethod,
                PathHeader ph,
                UserInfo userInfo // can get voterInfo from this.
                ) {

            // default to read/write
            PathHeader.SurveyToolStatus status = PathHeader.SurveyToolStatus.READ_WRITE;
            boolean canReadAndWrite = true;

            if (ph != null) {
                status = ph.getSurveyToolStatus();
                canReadAndWrite = ph.canReadAndWrite();
            }
            /*
             * Always forbid DEPRECATED items - don't show.
             *
             * Currently, bulk submission and TC voting are allowed even for SurveyToolStatus.HIDE,
             * but not for SurveyToolStatus.DEPRECATED. If we ever want to treat HIDE and DEPRECATED
             * the same here, then it would be simpler to call ph.shouldHide which is true for both.
             */
            if (status == SurveyToolStatus.DEPRECATED) {
                return StatusAction.FORBID_READONLY;
            }

            if (status == SurveyToolStatus.READ_ONLY) {
                return StatusAction.ALLOW_TICKET_ONLY;
            }

            // if TC+, allow anything else, even suppressed items and errors
            if (userInfo != null
                    && userInfo.getVoterInfo().getLevel().compareTo(VoteResolver.Level.tc) >= 0) {
                return StatusAction.ALLOW;
            }

            // always forbid bulk import except in data submission.
            if (inputMethod == InputMethod.BULK && (this != Phase.SUBMISSION && isUnitTest())) {
                return StatusAction.FORBID_UNLESS_DATA_SUBMISSION;
            }

            if (status == SurveyToolStatus.HIDE) {
                return StatusAction.FORBID_READONLY;
            }

            ValueStatus valueStatus = ValueStatus.NONE;
            if (pathValueInfo != null) {
                CandidateInfo winner = pathValueInfo.getCurrentItem();
                valueStatus = getValueStatus(winner, ValueStatus.NONE, null);

                // if limited submission, and winner doesn't have an error, limit the values

                if (LIMITED_SUBMISSION) {
                    if (!SubmissionLocales.allowEvenIfLimited(
                            pathValueInfo.getLocale().toString(),
                            pathValueInfo.getXpath(),
                            valueStatus == ValueStatus.ERROR,
                            pathValueInfo.getBaselineStatus() == Status.missing)) {
                        return StatusAction.FORBID_READONLY;
                    }
                }
            }

            if (this == Phase.SUBMISSION || isUnitTest()) {
                return (canReadAndWrite)
                        ? StatusAction.ALLOW
                        : StatusAction.ALLOW_VOTING_AND_TICKET;
            }

            // We are in vetting, not in submission

            // Only allow ADD if we have an error or warning
            // Only check winning value for errors/warnings per ticket #8677
            if (valueStatus != ValueStatus.NONE) {
                return (canReadAndWrite)
                        ? StatusAction.ALLOW
                        : StatusAction.ALLOW_VOTING_AND_TICKET;
            }

            // No warnings, so allow just voting.
            return StatusAction.ALLOW_VOTING_BUT_NO_ADD;
        }

        /**
         * getAcceptNewItemAction. MUST only be called if getShowRowAction(...).canShow() TODO
         * Consider moving Phase, StatusAction, etc into CLDRInfo.
         *
         * @param enteredValue If null, means an abstention. If voting for an existing value,
         *     pathValueInfo.getValues().contains(enteredValue) MUST be true
         * @param pathValueInfo
         * @param userInfo
         * @return
         */
        public StatusAction getAcceptNewItemAction(
                CandidateInfo enteredValue,
                PathValueInfo pathValueInfo,
                PathHeader ph,
                UserInfo userInfo // can get voterInfo from this.
                ) {
            if (!ph.canReadAndWrite()) {
                return StatusAction.FORBID_READONLY;
            }

            // only logged in users can add items.
            if (userInfo == null) {
                return StatusAction.FORBID_ERRORS;
            }

            // we can always abstain
            if (enteredValue == null) {
                return StatusAction.ALLOW;
            }

            // if TC+, allow anything else, even suppressed items and errors
            if (userInfo.getVoterInfo().getLevel().compareTo(VoteResolver.Level.tc) >= 0) {
                return StatusAction.ALLOW;
            }

            // Disallow errors.
            ValueStatus valueStatus =
                    getValueStatus(enteredValue, ValueStatus.NONE, CheckStatus.crossCheckSubtypes);
            if (valueStatus == ValueStatus.ERROR) {
                return StatusAction.FORBID_ERRORS;
            }

            // Allow items if submission
            if (this == Phase.SUBMISSION || isUnitTest()) {
                return StatusAction.ALLOW;
            }

            // Voting for an existing value is ok
            valueStatus = ValueStatus.NONE;
            for (CandidateInfo value : pathValueInfo.getValues()) {
                if (value == enteredValue) {
                    return StatusAction.ALLOW;
                }
                valueStatus = getValueStatus(value, valueStatus, CheckStatus.crossCheckSubtypes);
            }

            // If there were any errors/warnings on other values, allow
            if (valueStatus != ValueStatus.NONE) {
                return StatusAction.ALLOW;
            }

            // Otherwise (we are vetting, but with no errors or warnings)
            // DISALLOW NEW STUFF

            return StatusAction.FORBID_UNLESS_DATA_SUBMISSION;
        }

        public enum ValueStatus {
            ERROR,
            WARNING,
            NONE
        }

        public ValueStatus getValueStatus(
                CandidateInfo value, ValueStatus previous, Set<Subtype> changeErrorToWarning) {
            if (previous == ValueStatus.ERROR || value == null) {
                return previous;
            }

            for (CheckStatus item : value.getCheckStatusList()) {
                CheckStatus.Type type = item.getType();
                if (type.equals(CheckStatus.Type.Error)) {
                    if (changeErrorToWarning != null
                            && changeErrorToWarning.contains(item.getSubtype())) {
                        return ValueStatus.WARNING;
                    } else {
                        return ValueStatus.ERROR;
                    }
                } else if (type.equals(CheckStatus.Type.Warning)) {
                    previous = ValueStatus.WARNING;
                }
            }
            return previous;
        }
    }

    public static final class Options implements Comparable<Options> {

        public enum Option {
            locale,
            CoverageLevel_requiredLevel("CoverageLevel.requiredLevel"),
            CoverageLevel_localeType("CoverageLevel.localeType"),
            SHOW_TIMES,
            phase,
            lgWarningCheck,
            CheckCoverage_skip("CheckCoverage.skip"),
            exemplarErrors;

            private String key;

            public String getKey() {
                return key;
            }

            Option(String key) {
                this.key = key;
            }

            Option() {
                this.key = name();
            }
        }

        private static StandardCodes sc = StandardCodes.make();

        private final boolean DEBUG_OPTS = false;

        String options[] = new String[Option.values().length];
        CLDRLocale locale = null;

        private final String key; // for fast compare

        /**
         * Adopt some other map
         *
         * @param fromOptions
         */
        public Options(Map<String, String> fromOptions) {
            clear();
            setAll(fromOptions);
            key = null; // no key = slow compare
        }

        private void setAll(Map<String, String> fromOptions) {
            for (Map.Entry<String, String> e : fromOptions.entrySet()) {
                set(e.getKey(), e.getValue());
            }
        }

        /**
         * @param key
         * @param value
         */
        public void set(String key, String value) {
            // TODO- cache the map
            for (Option o : Option.values()) {
                if (o.getKey().equals(key)) {
                    set(o, value);
                    return;
                }
            }
            throw new IllegalArgumentException(
                    "Unknown CLDR option: '"
                            + key
                            + "' - valid keys are: "
                            + Options.getValidKeys());
        }

        private static String getValidKeys() {
            Set<String> allkeys = new TreeSet<>();
            for (Option o : Option.values()) {
                allkeys.add(o.getKey());
            }
            return ListFormatter.getInstance().format(allkeys);
        }

        public Options() {
            clear();
            key = "".intern(); // null Options.
        }

        /**
         * Deep clone
         *
         * @param options2
         */
        public Options(Options options2) {
            this.options = Arrays.copyOf(options2.options, options2.options.length);
            this.key = options2.key;
            this.locale = options2.locale;
        }

        public Options(CLDRLocale locale) {
            this.locale = locale;
            options = new String[Option.values().length];
            set(Option.locale, locale.getBaseName());
            StringBuilder sb = new StringBuilder();
            sb.append(locale.getBaseName()).append('/');
            key = sb.toString().intern();
        }

        public Options(
                CLDRLocale locale,
                CheckCLDR.Phase testPhase,
                String requiredLevel,
                String localeType) {
            this.locale = locale;
            options = new String[Option.values().length];
            StringBuilder sb = new StringBuilder();
            set(Option.locale, locale.getBaseName());
            sb.append(locale.getBaseName()).append('/');
            set(Option.CoverageLevel_requiredLevel, requiredLevel);
            sb.append(requiredLevel).append('/');
            set(Option.CoverageLevel_localeType, localeType);
            sb.append(localeType).append('/');
            set(Option.phase, testPhase.name().toLowerCase());
            sb.append(localeType).append('/');
            key = sb.toString().intern();
        }

        @Override
        public Options clone() {
            return new Options(this);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Options)) return false;
            if (this.key != null && ((Options) other).key != null) {
                return (this.key == ((Options) other).key);
            } else {
                return this.compareTo((Options) other) == 0;
            }
        }

        private Options clear() {
            for (int i = 0; i < options.length; i++) {
                options[i] = null;
            }
            return this;
        }

        private Options set(Option o, String v) {
            options[o.ordinal()] = v;
            if (DEBUG_OPTS) System.err.println("Setting " + o + " = " + v);
            return this;
        }

        public String get(Option o) {
            final String v = options[o.ordinal()];
            if (DEBUG_OPTS) System.err.println("Getting " + o + " = " + v);
            return v;
        }

        public CLDRLocale getLocale() {
            if (locale != null) return locale;
            return CLDRLocale.getInstance(get(Option.locale));
        }

        /**
         * Get the required coverage level for the specified locale, for this CheckCLDR object.
         *
         * @param localeID
         * @return the Level
         *     <p>Called by CheckCoverage.setCldrFileToCheck and CheckDates.setCldrFileToCheck
         */
        public Level getRequiredLevel(String localeID) {
            Level result;
            // see if there is an explicit level
            String localeType = get(Option.CoverageLevel_requiredLevel);
            if (localeType != null) {
                result = Level.get(localeType);
                if (result != Level.UNDETERMINED) {
                    return result;
                }
            }
            // otherwise, see if there is an organization level for the "Cldr" organization.
            // This is not user-specific.
            return sc.getLocaleCoverageLevel("Cldr", localeID);
        }

        public boolean contains(Option o) {
            String s = get(o);
            return (s != null && !s.isEmpty());
        }

        @Override
        public int compareTo(Options other) {
            if (other == this) return 0;
            if (key != null && other.key != null) {
                if (key == other.key) return 0;
                return key.compareTo(other.key);
            }
            for (int i = 0; i < options.length; i++) {
                final String s1 = options[i];
                final String s2 = other.options[i];
                if (s1 == null && s2 == null) {
                    // no difference
                } else if (s1 == null) {
                    return -1;
                } else if (s2 == null) {
                    return 1;
                } else {
                    int rv = s1.compareTo(s2);
                    if (rv != 0) {
                        return rv;
                    }
                }
            }
            return 0;
        }

        @Override
        public int hashCode() {
            if (key != null) return key.hashCode();

            int h = 1;
            for (int i = 0; i < options.length; i++) {
                if (options[i] == null) {
                    h *= 11;
                } else {
                    h = (h * 11) + options[i].hashCode();
                }
            }
            return h;
        }

        @Override
        public String toString() {
            if (key != null) return "Options:" + key;
            StringBuilder sb = new StringBuilder();
            for (Option o : Option.values()) {
                if (options[o.ordinal()] != null) {
                    sb.append(o).append('=').append(options[o.ordinal()]).append(' ');
                }
            }
            return sb.toString();
        }
    }

    public boolean isSkipTest() {
        return skipTest;
    }

    // this should only be set for the test in setCldrFileToCheck
    public void setSkipTest(boolean skipTest) {
        this.skipTest = skipTest;
    }

    /**
     * Here is where the list of all checks is found.
     *
     * @param nameMatcher Regex pattern that determines which checks are run, based on their class
     *     name (such as .* for all checks, .*Collisions.* for CheckDisplayCollisions, etc.)
     * @return
     */
    public static CompoundCheckCLDR getCheckAll(Factory factory, String nameMatcher) {
        return new CompoundCheckCLDR()
                .setFilter(Pattern.compile(nameMatcher, Pattern.CASE_INSENSITIVE).matcher(""))
                .add(new CheckAnnotations())
                // .add(new CheckAttributeValues(factory))
                .add(new CheckChildren(factory))
                .add(new CheckCoverage(factory))
                .add(new CheckDates(factory))
                .add(new CheckForCopy(factory))
                .add(new CheckDisplayCollisions(factory))
                .add(new CheckExemplars(factory))
                .add(new CheckForExemplars(factory))
                .add(new CheckForInheritanceMarkers())
                .add(new CheckNames())
                .add(new CheckNumbers(factory))
                // .add(new CheckZones()) // this doesn't work; many spurious errors that user can't
                // correct
                .add(new CheckMetazones())
                .add(new CheckLogicalGroupings(factory))
                .add(new CheckAlt())
                .add(new CheckAltOnly(factory))
                .add(new CheckCurrencies())
                .add(new CheckCasing())
                .add(
                        new CheckConsistentCasing(
                                factory)) // this doesn't work; many spurious errors that user can't
                // correct
                .add(new CheckQuotes())
                .add(new CheckUnits())
                .add(new CheckWidths())
                .add(new CheckPlaceHolders())
                .add(new CheckPersonNames())
                .add(new CheckNew(factory)) // this is at the end; it will check for other certain
        // other errors and warnings and
        // not add a message if there are any.
        ;
    }

    /** These determine what language is used to display information. Must be set before use. */
    public static synchronized CLDRFile getDisplayInformation() {
        return displayInformation;
    }

    public static synchronized void setDisplayInformation(CLDRFile inputDisplayInformation) {
        displayInformation = inputDisplayInformation;
    }

    /** Get the CLDRFile. */
    public final CLDRFile getCldrFileToCheck() {
        return cldrFileToCheck;
    }

    /**
     * Often subclassed for initializing. If so, make the first 2 lines: if (cldrFileToCheck ==
     * null) return this; super.handleSetCldrFileToCheck(cldrFileToCheck); do stuff
     *
     * <p>Called late via accept().
     *
     * @param cldrFileToCheck
     * @param options
     * @param possibleErrors any deferred possibleErrors can be set here. They will be appended to
     *     every handleCheck() call.
     * @return
     */
    public CheckCLDR handleSetCldrFileToCheck(
            CLDRFile cldrFileToCheck, Options options, List<CheckStatus> possibleErrors) {

        // nothing by default
        return this;
    }

    /**
     * Set the CLDRFile. Must be done before calling check.
     *
     * @param cldrFileToCheck
     * @param options (not currently used)
     * @param possibleErrors
     */
    public CheckCLDR setCldrFileToCheck(
            CLDRFile cldrFileToCheck, Options options, List<CheckStatus> possibleErrors) {
        this.cldrFileToCheck = cldrFileToCheck;
        reset();
        // clear the *cached* possible Errors. Not counting any set immediately by subclasses.
        cachedPossibleErrors.clear();
        cachedOptions = new Options(options);
        // we must load filters here, as they are used by check()

        // Shortlist error filters for this locale.
        String locale = cldrFileToCheck.getLocaleID();
        filtersForLocale.clear();
        for (R3<Pattern, Subtype, Pattern> filter : getAllFilters()) {
            if (filter.get0() == null || !filter.get0().matcher(locale).matches()) continue;
            Subtype subtype = filter.get1();
            List<Pattern> xpaths = filtersForLocale.get(subtype);
            if (xpaths == null) {
                filtersForLocale.put(subtype, xpaths = new ArrayList<>());
            }
            xpaths.add(filter.get2());
        }

        // hook for checks that want to set possibleErrors early
        handleCheckPossibleErrors(cldrFileToCheck, options, possibleErrors);

        return this;
    }

    /** override this if you want to return errors immediately when setCldrFileToCheck is called */
    protected void handleCheckPossibleErrors(
            CLDRFile cldrFileToCheck, Options options, List<CheckStatus> possibleErrors) {
        // nothing by default.
    }

    /** override this if you want to reset state immediately when setCldrFileToCheck is called */
    protected void reset() {
        initted = false;
    }

    /**
     * Subclasses must call this, after any skip calculation to indicate that an xpath is relevant
     * to them.
     *
     * @param result out-parameter to contain any deferred errors
     * @return false if test is skipped and should exit
     */
    protected boolean accept(List<CheckStatus> result) {
        if (!initted) {
            if (this.cldrFileToCheck == null) {
                throw new NullPointerException("accept() was called before setCldrFileToCheck()");
            }
            // clear this again.
            cachedPossibleErrors.clear();
            // call into the subclass
            handleSetCldrFileToCheck(this.cldrFileToCheck, cachedOptions, cachedPossibleErrors);
            // all of these are entireLocale
            cachedPossibleErrors.forEach(e -> e.setEntireLocale());
            initted = true;
        }
        // unconditionally append all cached possible errors
        result.addAll(cachedPossibleErrors);
        if (isSkipTest()) {
            return false;
        }
        return true;
    }

    /** has accept() been called since setCldrFileToCheck() was called? */
    boolean initted = false;

    /** cache of possible errors, for handleSetCldrFileToCheck */
    List<CheckStatus> cachedPossibleErrors = new ArrayList<>();

    Options cachedOptions = null;

    /**
     * abstract interface for mapping from a Subtype to a "more details" URL. see
     * org.unicode.cldr.web.SubtypeToURLMap
     */
    public interface SubtypeToURLProvider extends Function<Subtype, String> {}

    /** Status value returned from check */
    public static class CheckStatus implements Comparable<CheckStatus> {
        public static final Type alertType = Type.Comment,
                warningType = Type.Warning,
                errorType = Type.Error,
                exampleType = Type.Example,
                demoType = Type.Demo;

        public enum Type {
            Comment,
            Warning,
            Error,
            Example,
            Demo
        }

        public enum Subtype {
            none,
            noUnproposedVariant,
            deprecatedAttribute,
            illegalPlural,
            invalidLocale,
            incorrectCasing,
            valueMustBeOverridden,
            valueAlwaysOverridden,
            nullChildFile,
            internalError,
            coverageLevel,
            missingPluralInfo,
            currencySymbolTooWide,
            incorrectDatePattern,
            abbreviatedDateFieldTooWide,
            displayCollision,
            illegalExemplarSet,
            missingAuxiliaryExemplars,
            extraPlaceholders,
            missingPlaceholders,
            shouldntHavePlaceholders,
            couldNotAccessExemplars,
            noExemplarCharacters,
            modifiedEnglishValue,
            invalidCurrencyMatchSet,
            multipleMetazoneMappings,
            noMetazoneMapping,
            noMetazoneMappingAfter1970,
            noMetazoneMappingBeforeNow,
            cannotCreateZoneFormatter,
            insufficientCoverage,
            missingLanguageTerritoryInfo,
            missingEuroCountryInfo,
            deprecatedAttributeWithReplacement,
            missingOrExtraDateField,
            internalUnicodeSetFormattingError,
            auxiliaryExemplarsOverlap,
            missingPunctuationCharacters,

            charactersNotInCurrencyExemplars,
            asciiCharactersNotInCurrencyExemplars,
            charactersNotInMainOrAuxiliaryExemplars,
            asciiCharactersNotInMainOrAuxiliaryExemplars,

            narrowDateFieldTooWide,
            illegalCharactersInExemplars,
            orientationDisagreesWithExemplars,
            inconsistentDatePattern,
            inconsistentTimePattern,
            missingDatePattern,
            illegalDatePattern,
            missingMainExemplars,
            mustNotStartOrEndWithSpace,
            illegalCharactersInNumberPattern,
            numberPatternNotCanonical,
            currencyPatternMissingCurrencySymbol,
            currencyPatternUnexpectedCurrencySymbol,
            missingMinusSign,
            badNumericType,
            percentPatternMissingPercentSymbol,
            illegalNumberFormat,
            unexpectedAttributeValue,
            metazoneContainsDigit,
            tooManyGroupingSeparators,
            inconsistentPluralFormat,
            missingZeros,
            sameAsEnglish,
            sameAsCode,
            dateSymbolCollision,
            incompleteLogicalGroup,
            extraMetazoneString,
            inconsistentDraftStatus,
            errorOrWarningInLogicalGroup,
            valueTooWide,
            valueTooNarrow,
            nameContainsYear,
            patternCannotContainDigits,
            patternContainsInvalidCharacters,
            parenthesesNotAllowed,
            illegalNumberingSystem,
            unexpectedOrderOfEraYear,
            invalidPlaceHolder,
            asciiQuotesNotAllowed,
            badMinimumGroupingDigits,
            inconsistentPeriods,
            inheritanceMarkerNotAllowed,
            invalidDurationUnitPattern,
            invalidDelimiter,
            illegalCharactersInPattern,
            badParseLenient,
            tooManyValues,
            invalidSymbol,
            invalidGenderCode,
            mismatchedUnitComponent,
            longPowerWithSubscripts,
            gapsInPlaceholderNumbers,
            duplicatePlaceholders,
            largerDifferences,
            missingNonAltPath,
            badSamplePersonName,
            missingLanguage,
            namePlaceholderProblem,
            missingSpaceBetweenNameFields,
            shortDateFieldInconsistentLength,
            illegalParameterValue,
            illegalAnnotationCode,
            nullOrEmptyValue,
            ttsAnnotationMissing,
            illegalCharacter,
            missingNumberingSystem,
            forbiddenValue,
            inconsistentCoreDatePattern,
            inconsistentCurrencyPattern,
            inconsistentCompactPattern,
            inconsistentPositiveAndNegativePatterns;

            @Override
            public String toString() {
                // converts "thisThisThis" to "this this this"
                return TO_STRING.matcher(name()).replaceAll(" $1").toLowerCase();
            }

            static Pattern TO_STRING = PatternCache.get("([A-Z])");
        }

        /**
         * These error don't prevent entry during submission, since they become valid if a different
         * row is changed.
         */
        public static Set<Subtype> crossCheckSubtypes =
                ImmutableSet.of(
                        Subtype.dateSymbolCollision,
                        Subtype.displayCollision,
                        Subtype.inconsistentDraftStatus,
                        Subtype.incompleteLogicalGroup,
                        Subtype.inconsistentPeriods,
                        Subtype.abbreviatedDateFieldTooWide,
                        Subtype.narrowDateFieldTooWide,
                        Subtype.shortDateFieldInconsistentLength,
                        Subtype.coverageLevel);

        public static Set<Subtype> errorCodesPath =
                ImmutableSet.of(
                        Subtype.duplicatePlaceholders,
                        Subtype.extraPlaceholders,
                        Subtype.gapsInPlaceholderNumbers,
                        Subtype.invalidPlaceHolder,
                        Subtype.missingPlaceholders,
                        Subtype.shouldntHavePlaceholders);

        private Type type;
        private Subtype subtype = Subtype.none;
        private String messageFormat;
        private Object[] parameters;
        private CheckAccessor cause;
        private boolean checkOnSubmit = true;

        public CheckStatus() {}

        public boolean isCheckOnSubmit() {
            return checkOnSubmit;
        }

        public CheckStatus setCheckOnSubmit(boolean dependent) {
            this.checkOnSubmit = dependent;
            return this;
        }

        public Type getType() {
            return type;
        }

        public CheckStatus setMainType(CheckStatus.Type type) {
            this.type = type;
            return this;
        }

        public String getMessage() {
            String message = messageFormat;
            if (messageFormat != null && parameters != null) {
                try {
                    String fixedApos = MessageFormat.autoQuoteApostrophe(messageFormat);
                    MessageFormat format = new MessageFormat(fixedApos);
                    message = format.format(parameters);
                    if (errorCodesPath.contains(subtype)) {
                        message +=
                                "; see <a href='http://cldr.unicode.org/translation/error-codes#"
                                        + subtype.name()
                                        + "'  target='cldr_error_codes'>"
                                        + subtype
                                        + "</a>.";
                    }
                } catch (Exception e) {
                    message = messageFormat;
                    final String failMsg =
                            "MessageFormat Failure: "
                                    + subtype
                                    + "; "
                                    + messageFormat
                                    + "; "
                                    + (parameters == null ? null : Arrays.asList(parameters));
                    logger.log(java.util.logging.Level.SEVERE, e, () -> failMsg);
                    System.err.println(failMsg);
                    // throw new IllegalArgumentException(subtype + "; " + messageFormat + "; "
                    // + (parameters == null ? null : Arrays.asList(parameters)), e);
                }
            }
            Exception[] exceptionParameters = getExceptionParameters();
            if (exceptionParameters != null) {
                for (Exception exception : exceptionParameters) {
                    message += "; " + exception.getMessage(); // + " \t(" +
                    // exception.getClass().getName() + ")";
                    // for (StackTraceElement item : exception.getStackTrace()) {
                    // message += "\n\t" + item;
                    // }
                }
            }
            return message.replace('\t', ' ');
        }

        public CheckStatus setMessage(String message) {
            if (cause == null) {
                throw new IllegalArgumentException("Must have cause set.");
            }
            if (message == null) {
                throw new IllegalArgumentException("Message cannot be null.");
            }
            this.messageFormat = message;
            this.parameters = null;
            return this;
        }

        public CheckStatus setMessage(String message, Object... messageArguments) {
            if (cause == null) {
                throw new IllegalArgumentException("Must have cause set.");
            }
            this.messageFormat = message;
            this.parameters = messageArguments;
            return this;
        }

        @Override
        public String toString() {
            return getType() + ": " + getMessage();
        }

        /** Warning: don't change the contents of the parameters after retrieving. */
        public Object[] getParameters() {
            return parameters;
        }

        /**
         * Returns any Exception parameters in the status, or null if there are none.
         *
         * @return
         */
        public Exception[] getExceptionParameters() {
            if (parameters == null) {
                return null;
            }

            List<Exception> errors = new ArrayList<>();
            for (Object o : parameters) {
                if (o instanceof Exception) {
                    errors.add((Exception) o);
                }
            }
            if (errors.size() == 0) {
                return null;
            }
            return errors.toArray(new Exception[errors.size()]);
        }

        /** Warning: don't change the contents of the parameters after passing in. */
        public CheckStatus setParameters(Object[] parameters) {
            if (cause == null) {
                throw new IllegalArgumentException("Must have cause set.");
            }
            this.parameters = parameters;
            return this;
        }

        public SimpleDemo getDemo() {
            return null;
        }

        public CheckCLDR getCause() {
            return cause instanceof CheckCLDR ? (CheckCLDR) cause : null;
        }

        public CheckStatus setCause(CheckAccessor cause) {
            this.cause = cause;
            return this;
        }

        public Subtype getSubtype() {
            return subtype;
        }

        public CheckStatus setSubtype(Subtype subtype) {
            this.subtype = subtype;
            return this;
        }

        /**
         * Convenience function: return true if any items in this list are of errorType
         *
         * @param result the list to check (could be null for empty)
         * @return true if any items in result are of errorType
         */
        public static final boolean hasError(List<CheckStatus> result) {
            return hasType(result, errorType);
        }

        /**
         * Convenience function: return true if any items in this list are of errorType
         *
         * @param result the list to check (could be null for empty)
         * @return true if any items in result are of errorType
         */
        public static boolean hasType(List<CheckStatus> result, Type type) {
            if (result == null) return false;
            for (CheckStatus s : result) {
                if (s.getType().equals(type)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * @returns true if this status applies to the entire locale, not a single path
         */
        public boolean getEntireLocale() {
            return entireLocale;
        }

        /** Mark this CheckStatus as isEntireLocale */
        CheckStatus setEntireLocale() {
            entireLocale = true;
            return this;
        }

        private boolean entireLocale = false;

        @Override
        public int compareTo(CheckStatus o) {
            if (this == o) return 0;
            return ComparisonChain.start()
                    .compare(getType(), o.getType())
                    .compare(getSubtype(), o.getSubtype())
                    .compare(getMessage(), o.getMessage())
                    .result();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof CheckStatus) return false;
            return compareTo((CheckStatus) o) == 0;
        }
    }

    public abstract static class SimpleDemo {
        Map<String, String> internalPostArguments = new HashMap<>();

        /**
         * @param postArguments A read-write map containing post-style arguments. eg TEXTBOX=abcd,
         *     etc. <br>
         *     The first time this is called, the Map should be empty.
         * @return true if the map has been changed
         */
        public abstract String getHTML(Map<String, String> postArguments) throws Exception;

        /** Only here for compatibility. Use the other getHTML instead */
        public final String getHTML(String path, String fullPath, String value) throws Exception {
            return getHTML(internalPostArguments);
        }

        /**
         * THIS IS ONLY FOR COMPATIBILITY: you can call this, then the non-postArguments form of
         * getHTML; or better, call getHTML with the postArguments.
         *
         * @param postArguments A read-write map containing post-style arguments. eg TEXTBOX=abcd,
         *     etc.
         * @return true if the map has been changed
         */
        public final boolean processPost(Map<String, String> postArguments) {
            internalPostArguments.clear();
            internalPostArguments.putAll(postArguments);
            return true;
        }
    }

    public abstract static class FormatDemo extends SimpleDemo {
        protected String currentPattern, currentInput, currentFormatted, currentReparsed;
        protected ParsePosition parsePosition = new ParsePosition(0);

        protected abstract String getPattern();

        protected abstract String getSampleInput();

        protected abstract void getArguments(Map<String, String> postArguments);

        @Override
        public String getHTML(Map<String, String> postArguments) throws Exception {
            getArguments(postArguments);
            StringBuffer htmlMessage = new StringBuffer();
            FormatDemo.appendTitle(htmlMessage);
            FormatDemo.appendLine(
                    htmlMessage, currentPattern, currentInput, currentFormatted, currentReparsed);
            htmlMessage.append("</table>");
            return htmlMessage.toString();
        }

        public String getPlainText(Map<String, String> postArguments) {
            getArguments(postArguments);
            return MessageFormat.format(
                    "<\"\u200E{0}\u200E\", \"{1}\"> \u2192 \"\u200E{2}\u200E\" \u2192 \"{3}\"",
                    (Object[])
                            new String[] {
                                currentPattern, currentInput, currentFormatted, currentReparsed
                            });
        }

        /**
         * @param htmlMessage
         * @param pattern
         * @param input
         * @param formatted
         * @param reparsed
         */
        public static void appendLine(
                StringBuffer htmlMessage,
                String pattern,
                String input,
                String formatted,
                String reparsed) {
            htmlMessage
                    .append("<tr><td><input type='text' name='pattern' value='")
                    .append(TransliteratorUtilities.toXML.transliterate(pattern))
                    .append("'></td><td><input type='text' name='input' value='")
                    .append(TransliteratorUtilities.toXML.transliterate(input))
                    .append("'></td><td>")
                    .append("<input type='submit' value='Test' name='Test'>")
                    .append("</td><td>" + "<input type='text' name='formatted' value='")
                    .append(TransliteratorUtilities.toXML.transliterate(formatted))
                    .append("'></td><td>" + "<input type='text' name='reparsed' value='")
                    .append(TransliteratorUtilities.toXML.transliterate(reparsed))
                    .append("'></td></tr>");
        }

        /**
         * @param htmlMessage
         */
        public static void appendTitle(StringBuffer htmlMessage) {
            htmlMessage.append(
                    "<table border='1' cellspacing='0' cellpadding='2'"
                            +
                            // " style='border-collapse: collapse' style='width: 100%'" +
                            "><tr>"
                            + "<th>Pattern</th>"
                            + "<th>Unlocalized Input</th>"
                            + "<th></th>"
                            + "<th>Localized Format</th>"
                            + "<th>Re-Parsed</th>"
                            + "</tr>");
        }
    }

    /**
     * Checks the path/value in the cldrFileToCheck for correctness, according to some criterion. If
     * the path is relevant to the check, there is an alert or warning, then a CheckStatus is added
     * to List.
     *
     * @param path Must be a distinguished path, such as what comes out of CLDRFile.iterator()
     * @param fullPath Must be the full path
     * @param value the value associated with the path
     * @param result
     */
    public final CheckCLDR check(
            String path, String fullPath, String value, Options options, List<CheckStatus> result) {
        if (cldrFileToCheck == null) {
            throw new InternalCldrException("CheckCLDR problem: cldrFileToCheck must not be null");
        }
        if (path == null) {
            throw new InternalCldrException("CheckCLDR problem: path must not be null");
        }
        // if (fullPath == null) {
        // throw new InternalError("CheckCLDR problem: fullPath must not be null");
        // }
        // if (value == null) {
        // throw new InternalError("CheckCLDR problem: value must not be null");
        // }
        result.clear();

        /*
         * If the item is non-winning, and either inherited or it is code-fallback, then don't run
         * any tests on this item.  See http://unicode.org/cldr/trac/ticket/7574
         *
         * The following conditional formerly used "value == ..." and "value != ...", which in Java doesn't
         * mean what it does in some other languages. The condition has been changed to use the equals() method.
         * Since value can be null, check for that first.
         */
        // if (value == cldrFileToCheck.getBaileyValue(path, null, null) && value !=
        // cldrFileToCheck.getWinningValue(path)) {
        if (value != null
                && !value.equals(cldrFileToCheck.getWinningValue(path))
                && cldrFileToCheck.getUnresolved().getStringValue(path) == null) {
            return this;
        }

        // If we're being asked to run tests for an inheritance marker, then we need to change it
        // to the "real" value first before running tests. Testing the value
        // CldrUtility.INHERITANCE_MARKER ("↑↑↑") doesn't make sense.
        if (CldrUtility.INHERITANCE_MARKER.equals(value)) {
            value = cldrFileToCheck.getBaileyValue(path, null, null);
            // If it hasn't changed, then don't run any tests.
            if (CldrUtility.INHERITANCE_MARKER.equals(value)) {
                return this;
            }
        }
        CheckCLDR instance = handleCheck(path, fullPath, value, options, result);
        Iterator<CheckStatus> iterator = result.iterator();
        // Filter out any errors/warnings that match the filter list in CheckCLDR-exceptions.txt.
        while (iterator.hasNext()) {
            CheckStatus status = iterator.next();
            if (shouldExcludeStatus(fullPath, status)) {
                iterator.remove();
            }
        }
        return instance;
    }

    /**
     * Returns any examples in the result parameter. Both examples and demos can be returned. A demo
     * will have getType() == CheckStatus.demoType. In that case, there will be no getMessage
     * available; instead, call getDemo() to get the demo, then call getHTML() to get the initial
     * HTML.
     */
    public final CheckCLDR getExamples(
            String path, String fullPath, String value, Options options, List<CheckStatus> result) {
        result.clear();
        return handleGetExamples(path, fullPath, value, options, result);
    }

    @SuppressWarnings("unused")
    protected CheckCLDR handleGetExamples(
            String path,
            String fullPath,
            String value,
            Options options2,
            List<CheckStatus> result) {
        return this; // NOOP unless overridden
    }

    /**
     * This is what the subclasses override.
     *
     * <p>If a path is not applicable, exit early with <code>return this;</code> Once a path is
     * applicable, call <code>accept(result);</code> to add deferred possible problems.
     *
     * <p>If something is found, a CheckStatus is added to result. This can be done multiple times
     * in one call, if multiple errors or warnings are found. The CheckStatus may return warnings,
     * errors, examples, or demos. We may expand that in the future.
     *
     * <p>The code to add the CheckStatus will look something like::
     *
     * <pre>
     * result.add(new CheckStatus()
     *     .setType(CheckStatus.errorType)
     *     .setMessage(&quot;Value should be {0}&quot;, new Object[] { pattern }));
     * </pre>
     */
    public abstract CheckCLDR handleCheck(
            String path, String fullPath, String value, Options options, List<CheckStatus> result);

    /** Only for use in ConsoleCheck, for debugging */
    public void handleFinish() {}

    /**
     * Internal class used to bundle up a number of Checks.
     *
     * @author davis
     */
    static class CompoundCheckCLDR extends CheckCLDR {
        private Matcher filter;
        private List<CheckCLDR> checkList = new ArrayList<>();
        private List<CheckCLDR> filteredCheckList = new ArrayList<>();

        public CompoundCheckCLDR add(CheckCLDR item) {
            checkList.add(item);
            if (filter == null) {
                filteredCheckList.add(item);
            } else {
                final String className = item.getClass().getName();
                if (filter.reset(className).find()) {
                    filteredCheckList.add(item);
                }
            }
            return this;
        }

        @Override
        public CheckCLDR handleCheck(
                String path,
                String fullPath,
                String value,
                Options options,
                List<CheckStatus> result) {
            result.clear();

            if (!accept(result)) return this;

            // If we're being asked to run tests for an inheritance marker, then we need to change
            // it
            // to the "real" value first before running tests. Testing the value
            // CldrUtility.INHERITANCE_MARKER ("↑↑↑") doesn't make sense.
            if (CldrUtility.INHERITANCE_MARKER.equals(value)) {
                value = getCldrFileToCheck().getBaileyValue(path, null, null);
            }
            for (Iterator<CheckCLDR> it = filteredCheckList.iterator(); it.hasNext(); ) {
                CheckCLDR item = it.next();
                // skip proposed items in final testing.
                if (Phase.FINAL_TESTING == item.getPhase()) {
                    if (path.contains("proposed") && path.contains("[@alt=")) {
                        continue;
                    }
                }
                try {
                    if (!item.isSkipTest()) {
                        item.handleCheck(path, fullPath, value, options, result);
                    }
                } catch (Exception e) {
                    addError(result, item, e);
                    return this;
                }
            }
            return this;
        }

        @Override
        public void handleFinish() {
            for (Iterator<CheckCLDR> it = filteredCheckList.iterator(); it.hasNext(); ) {
                CheckCLDR item = it.next();
                item.handleFinish();
            }
        }

        @Override
        protected CheckCLDR handleGetExamples(
                String path,
                String fullPath,
                String value,
                Options options,
                List<CheckStatus> result) {
            result.clear();
            for (Iterator<CheckCLDR> it = filteredCheckList.iterator(); it.hasNext(); ) {
                CheckCLDR item = it.next();
                try {
                    item.handleGetExamples(path, fullPath, value, options, result);
                } catch (Exception e) {
                    addError(result, item, e);
                    return this;
                }
            }
            return this;
        }

        private void addError(List<CheckStatus> result, CheckCLDR item, Exception e) {
            // send to java.util.logging, useful for servers
            logger.log(
                    java.util.logging.Level.SEVERE,
                    e,
                    () -> {
                        String locale = "(unknown)";
                        if (item.cldrFileToCheck != null) {
                            locale = item.cldrFileToCheck.getLocaleID();
                        }
                        return String.format(
                                "Internal error: %s in %s", item.getClass().getName(), locale);
                    });
            // also add as a check
            result.add(
                    new CheckStatus()
                            .setCause(this)
                            .setMainType(CheckStatus.errorType)
                            .setSubtype(Subtype.internalError)
                            .setMessage(
                                    "Internal error in {0}. Exception: {1}, Message: {2}, Trace: {3}",
                                    new Object[] {
                                        item.getClass().getName(),
                                        e.getClass().getName(),
                                        e,
                                        Arrays.asList(e.getStackTrace())
                                    }));
        }

        @Override
        public void handleCheckPossibleErrors(
                CLDRFile cldrFileToCheck, Options options, List<CheckStatus> possibleErrors) {
            ElapsedTimer testTime = null, testOverallTime = null;
            if (cldrFileToCheck == null) return;
            boolean SHOW_TIMES = options.contains(Options.Option.SHOW_TIMES);
            setPhase(Phase.forString(options.get(Options.Option.phase)));
            if (SHOW_TIMES)
                testOverallTime = new ElapsedTimer("Test setup time for setCldrFileToCheck: {0}");
            super.handleCheckPossibleErrors(cldrFileToCheck, options, possibleErrors);
            possibleErrors.clear();

            for (Iterator<CheckCLDR> it = filteredCheckList.iterator(); it.hasNext(); ) {
                CheckCLDR item = it.next();
                if (SHOW_TIMES)
                    testTime =
                            new ElapsedTimer(
                                    "Test setup time for " + item.getClass().toString() + ": {0}");
                try {
                    item.setPhase(getPhase());
                    item.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
                    if (SHOW_TIMES) {
                        if (item.isSkipTest()) {
                            System.out.println("Disabled : " + testTime);
                        } else {
                            System.out.println("OK : " + testTime);
                        }
                    }
                } catch (RuntimeException e) {
                    addError(possibleErrors, item, e);
                    if (SHOW_TIMES) System.out.println("ERR: " + testTime + " - " + e.toString());
                }
            }
            if (SHOW_TIMES) System.out.println("Overall: " + testOverallTime + ": {0}");
            // all of these are entire locale
            possibleErrors.forEach(e -> e.setEntireLocale());
        }

        public Matcher getFilter() {
            return filter;
        }

        public CompoundCheckCLDR setFilter(Matcher filter) {
            this.filter = filter;
            filteredCheckList.clear();
            for (Iterator<CheckCLDR> it = checkList.iterator(); it.hasNext(); ) {
                CheckCLDR item = it.next();
                if (filter == null || filter.reset(item.getClass().getName()).matches()) {
                    filteredCheckList.add(item);
                    item.handleSetCldrFileToCheck(getCldrFileToCheck(), (Options) null, null);
                }
            }
            return this;
        }

        public String getFilteredTests() {
            return filteredCheckList.toString();
        }

        public List<CheckCLDR> getFilteredTestList() {
            return filteredCheckList;
        }
    }

    @Override
    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    /** A map of error/warning types to their filters. */
    private static Supplier<List<R3<Pattern, Subtype, Pattern>>> filterSupplier =
            Suppliers.memoize(
                    () -> {
                        final List<R3<Pattern, Subtype, Pattern>> newFilters = new ArrayList<>();
                        RegexFileParser fileParser = new RegexFileParser();
                        fileParser.setLineParser(
                                new RegexLineParser() {
                                    @Override
                                    public void parse(String line) {
                                        String[] fields = line.split("\\s*;\\s*");
                                        Subtype subtype = Subtype.valueOf(fields[0]);
                                        Pattern locale = PatternCache.get(fields[1]);
                                        Pattern xpathRegex =
                                                PatternCache.get(
                                                        fields[2].replaceAll("\\[@", "\\\\[@"));
                                        newFilters.add(new R3<>(locale, subtype, xpathRegex));
                                    }
                                });
                        fileParser.parse(
                                CheckCLDR.class,
                                "/org/unicode/cldr/util/data/CheckCLDR-exceptions.txt");
                        return ImmutableList.copyOf(newFilters);
                    });

    private static final List<R3<Pattern, Subtype, Pattern>> getAllFilters() {
        return filterSupplier.get();
    }

    /**
     * Checks if a status should be excluded from the list of results returned from CheckCLDR.
     *
     * @param xpath the xpath that the status belongs to
     * @param status the status
     * @return true if the status should be included
     */
    private boolean shouldExcludeStatus(String xpath, CheckStatus status) {
        List<Pattern> xpathPatterns = filtersForLocale.get(status.getSubtype());
        if (xpathPatterns == null) {
            return false;
        }
        for (Pattern xpathPattern : xpathPatterns) {
            if (xpathPattern.matcher(xpath).matches()) {
                return true;
            }
        }
        return false;
    }

    public CLDRFile getEnglishFile() {
        return englishFile;
    }

    public void setEnglishFile(CLDRFile englishFile) {
        this.englishFile = englishFile;
    }

    public CharSequence fixedValueIfInherited(String value, String path) {
        return !CldrUtility.INHERITANCE_MARKER.equals(value)
                ? value
                : getCldrFileToCheck().getStringValueWithBailey(path);
    }
}
