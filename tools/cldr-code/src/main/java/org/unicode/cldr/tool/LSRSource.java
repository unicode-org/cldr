package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Comparators;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;

public class LSRSource implements Comparable<LSRSource> {
    private static final Joiner JOIN_SPACE = Joiner.on(' ');
    private static final Splitter SPLIT_SPACE = Splitter.on(' ').omitEmptyStrings();
    private final CLDRLocale cldrLocale;
    private final Set<String> sources;

    LSRSource(String lang, String script, String region, String sources) {
        cldrLocale = CLDRLocale.getInstance(lang, script, region);
        this.sources = ImmutableSortedSet.copyOf(SPLIT_SPACE.splitToList(sources));
    }

    public String getLanguage() {
        return cldrLocale.getLanguage();
    }

    public String getScript() {
        return cldrLocale.getScript();
    }

    public String getRegion() {
        return cldrLocale.getRegion();
    }

    public Set<String> getSources() {
        return sources;
    }

    public String getLsrString() {
        return cldrLocale.toString();
    }

    @Override
    public int compareTo(LSRSource other) {
        return ComparisonChain.start()
                .compare(cldrLocale, other.cldrLocale)
                .compare(
                        sources,
                        other.sources,
                        Comparators.lexicographical(Comparator.<String>naturalOrder()))
                .result();
    }

    @Override
    public int hashCode() {
        return Objects.hash(cldrLocale, sources);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof LSRSource)) return false;

        LSRSource other = (LSRSource) obj;
        return Objects.equals(cldrLocale, other.cldrLocale)
                && Objects.equals(sources, other.sources);
    }

    @Override
    public String toString() {
        return cldrLocale.toString() + " // " + getSources();
    }

    public String line(String source) {
        final CLDRFile english = CLDRConfig.getInstance().getEnglish();

        //      <likelySubtag from="aa" to="aa_Latn_ET"/>
        // <!--{ Afar; ?; ? } => { Afar; Latin; Ethiopia }-->
        final String target = cldrLocale.toString();
        final String result =
                "<likelySubtag from=\""
                        + source
                        + "\" to=\""
                        + target
                        + (getSources().isEmpty() ? "" : "\" origin=\"" + getSourceString())
                        + "\"/>"
                        + "\t<!-- "
                        + english.nameGetter().getNameFromIdentifier(source)
                        + " ➡︎ "
                        + english.nameGetter().getNameFromIdentifier(target)
                        + " -->";
        return result;
    }

    public String getSourceString() {
        return JOIN_SPACE.join(getSources());
    }

    public CLDRLocale getCldrLocale() {
        return cldrLocale;
    }

    //    public static String combineLSR(String lang, String script, String region) {
    //        return (lang.isEmpty() ? "und" : lang)
    //                + (script.isEmpty() ? "" : "_" + script)
    //                + (region.isEmpty() ? "" : "_" + region);
    //    }
}
