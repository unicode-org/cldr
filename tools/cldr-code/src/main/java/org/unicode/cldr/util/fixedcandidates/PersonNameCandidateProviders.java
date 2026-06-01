package org.unicode.cldr.util.fixedcandidates;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.unicode.cldr.util.personname.PersonNameFormatter;

class PersonNameCandidateProviders {
    public static final List<FixedCandidateProvider> personNameProviders =
            ImmutableList.of(
                    FixedCandidateProvider.forXPathAndEnum(
                            "//ldml/personNames/parameterDefault[@parameter=\"formality\"]",
                            PersonNameFormatter.Formality.values()),
                    FixedCandidateProvider.forXPathAndEnum(
                            "//ldml/personNames/parameterDefault[@parameter=\"length\"]",
                            PersonNameFormatter.Length.values()));
}
