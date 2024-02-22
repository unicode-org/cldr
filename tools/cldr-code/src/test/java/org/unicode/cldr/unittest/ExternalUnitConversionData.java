package org.unicode.cldr.unittest;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Set;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.UnitConverter.ConversionInfo;
import org.unicode.cldr.util.UnitConverter.UnitSystem;

final class ExternalUnitConversionData implements Comparable<ExternalUnitConversionData> {

    public final String quantity;
    public final String source;
    public final String symbol;
    public final String target;
    public final ConversionInfo info;
    public final String from;
    public final String line;
    public final Set<UnitSystem> systems;

    public ExternalUnitConversionData(
            String quantity,
            String source,
            String symbol,
            String target,
            Rational factor,
            Rational offset,
            Set<UnitSystem> systems,
            String from,
            String line) {
        super();
        this.quantity = quantity;
        this.source = source;
        this.symbol = symbol;
        this.target = target;
        this.info = new ConversionInfo(factor, offset == null ? Rational.ZERO : offset);
        this.systems = systems == null ? ImmutableSet.of() : ImmutableSet.copyOf(systems);
        this.from = from;
        this.line = line;
    }

    @Override
    public String toString() {
        return quantity + "\t" + source + "\t" + symbol + "\t" + target + "\t" + info + "\t"
                + systems + "\t«" + line + "»";
    }

    @Override
    public int hashCode() {
        return Objects.hash(quantity, info, source, target);
    }

    @Override
    public boolean equals(Object obj) {
        return compareTo((ExternalUnitConversionData) obj) == 0;
    }

    @Override
    public int compareTo(ExternalUnitConversionData other) {
        return ComparisonChain.start()
                .compare(quantity, other.quantity)
                .compare(target, other.target)
                .compare(info, other.info)
                .compare(source, other.source)
                .result();
    }
}
