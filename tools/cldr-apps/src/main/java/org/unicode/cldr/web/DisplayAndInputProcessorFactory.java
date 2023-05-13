package org.unicode.cldr.web;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.concurrent.ExecutionException;
import org.unicode.cldr.test.DisplayAndInputProcessor;
import org.unicode.cldr.util.CLDRLocale;

public class DisplayAndInputProcessorFactory {
    private static final LoadingCache<CLDRLocale, DisplayAndInputProcessor> daipCache =
            CacheBuilder.newBuilder()
                    .softValues()
                    .build(
                            new CacheLoader<>() {

                                @Override
                                public DisplayAndInputProcessor load(CLDRLocale key)
                                        throws Exception {
                                    return new DisplayAndInputProcessor(key);
                                }
                            });

    static DisplayAndInputProcessor make(CLDRLocale locale) {
        try {
            return daipCache.get(locale);
        } catch (ExecutionException e) {
            SurveyLog.logException(e);
            throw new RuntimeException("Could not create DAIP for " + locale, e);
        }
    }

    static DisplayAndInputProcessor make(String locale) {
        return make(CLDRLocale.getInstance(locale));
    }
}
