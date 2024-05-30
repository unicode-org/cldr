package org.unicode.cldr.web;

import static org.unicode.cldr.web.FixedCandidateProvider.forEnumWithFixedXpath;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.GrammarInfo;
import org.unicode.cldr.util.GrammarInfo.GrammaticalFeature;
import org.unicode.cldr.util.GrammarInfo.GrammaticalScope;
import org.unicode.cldr.util.GrammarInfo.GrammaticalTarget;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.personname.PersonNameFormatter;
import org.unicode.cldr.web.FixedCandidateProvider.PatternCacheCandidateProvider;

/** Cache for on-disk immutable data. */
public class DiskDataCache {
    static final Logger logger = Logger.getLogger(DiskDataCache.class.getSimpleName());

    private final Factory factory;
    private final CLDRFile english; // TODO: Unused?
    private final PathHeader.Factory phf;
    final SupplementalDataInfo sdi;

    final List<FixedCandidateProvider> personNameProviders =
            ImmutableList.of(
                    forEnumWithFixedXpath(
                            "//ldml/personNames/parameterDefault[@parameter=\"formality\"]",
                            PersonNameFormatter.Formality.values()),
                    forEnumWithFixedXpath(
                            "//ldml/personNames/parameterDefault[@parameter=\"length\"]",
                            PersonNameFormatter.Length.values()));

    /** this is the immutable cousin of STFactory.PerLocaleData, for the on-disk data */
    class DiskDataEntry {
        final CLDRLocale locale;
        final XMLSource diskData;
        final CLDRFile diskFile;
        final Set<String> pathsForFile;

        private final List<FixedCandidateProvider> fixedCandidateProviders = new LinkedList<>();

        public DiskDataEntry(CLDRLocale locale) {
            this.locale = locale;
            diskData = factory.makeSource(locale.getBaseName()).freeze();
            diskFile = factory.make(locale.getBaseName(), true).freeze();
            pathsForFile = getPathHeaderFactory().pathsForFile(diskFile);

            addFixedCandidateProviders();
        }

        private void addFixedCandidateProviders() {
            // Add all Candidate Providers here
            fixedCandidateProviders.add(new GrammarCandidateProvider());
            fixedCandidateProviders.addAll(personNameProviders);
        }

        /** Candidate provider for a regex */
        class GrammarCandidateProvider extends PatternCacheCandidateProvider {
            final GrammarInfo grammarInfo = sdi.getGrammarInfo(locale.getBaseName());

            public GrammarCandidateProvider() {
                super("^//ldml/units/unitLength.*/unit.*gender");
            }

            @Override
            protected Collection<String> getCandidates() {
                return grammarInfo.get(
                        GrammaticalTarget.nominal,
                        GrammaticalFeature.grammaticalGender,
                        GrammaticalScope.units);
            }
        }

        /**
         * @returns a list of values (or empty list) of any 'fixed' candidates for this xpath
         */
        Collection<String> getFixedCandidates(final String xpath) {
            for (final FixedCandidateProvider fcp : fixedCandidateProviders) {
                final Collection<String> r = fcp.apply(xpath);
                if (r != null) return r;
            }
            return Collections.emptySet();
        }
    }

    public DiskDataCache(Factory f, CLDRFile english, SupplementalDataInfo sdi) {
        this.factory = f;
        this.english = english;
        this.phf = PathHeader.getFactory(english);
        this.sdi = sdi;
    }

    public PathHeader.Factory getPathHeaderFactory() {
        return phf;
    }

    private LoadingCache<CLDRLocale, DiskDataEntry> cache =
            CacheBuilder.newBuilder()
                    .build(
                            new CacheLoader<CLDRLocale, DiskDataEntry>() {

                                @Override
                                public DiskDataEntry load(@Nonnull CLDRLocale l) throws Exception {
                                    return new DiskDataEntry(l);
                                }
                            });

    public DiskDataEntry get(CLDRLocale locale) {
        logger.fine(() -> "Loading " + locale);
        try {
            return cache.get(locale);
        } catch (ExecutionException e) {
            SurveyLog.logException(logger, e, "Trying to construct " + locale);
            SurveyMain.busted("Loading " + locale, e);
            return null; /* notreached */
        }
    }
}
