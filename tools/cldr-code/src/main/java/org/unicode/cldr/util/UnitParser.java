package org.unicode.cldr.util;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.ibm.icu.util.Output;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import org.unicode.cldr.util.SupplementalDataInfo.UnitIdComponentType;
import org.unicode.cldr.util.With.SimpleIterator;

public class UnitParser implements SimpleIterator<String> {
    public static final Splitter DASH_SPLITTER = Splitter.on('-');
    public static final Joiner DASH_JOIN = Joiner.on('-');

    private String bufferedItem = null;
    private UnitIdComponentType bufferedType = null;
    private Iterator<String> source;
    private UnitIdComponentType type;
    Function<String, UnitIdComponentType> componentTypeSupplier;
    private String original;

    // Provide this api for use inside of SupplementalDataInfo, to avoid circularity
    public UnitParser(Function<String, UnitIdComponentType> componentTypeSupplier) {
        this.componentTypeSupplier = componentTypeSupplier;
    }

    public UnitParser() {
        this(CLDRConfig.getInstance().getSupplementalDataInfo()::getUnitIdComponentType);
    }

    //    public UnitParser set(Iterator<String> source) {
    //        bufferedItem = null;
    //        this.source = source;
    //        return this;
    //    }
    //
    //    public UnitParser set(Iterable<String> source) {
    //        return set(source.iterator());
    //    }
    //
    public UnitParser set(String source) {
        if (source == null) {
            throw new IllegalArgumentException("Unit Parser doesn't handle null");
        }
        bufferedItem = null;
        this.original = source;
        this.source = UnitParser.DASH_SPLITTER.split(source).iterator();
        return this;
    }

    private enum State {
        start,
        havePrefix,
        haveBaseOrSuffix
    }

    public List<Pair<UnitIdComponentType, String>> getRemaining() {
        List<Pair<UnitIdComponentType, String>> result = new ArrayList<>();
        Output<UnitIdComponentType> type = new Output<>();
        while (true) {
            String item = nextParse(type);
            if (item == null) {
                return result;
            }
            result.add(Pair.of(type.value, item));
        }
    }

    /**
     * Parses the next segment in the source from set.
     *
     * @param output returns type type of the item
     * @return a unit segment of the form: prefix* base suffix*, and, per, or power; or null if no
     *     more remaining
     */
    public String nextParse(Output<UnitIdComponentType> unitIdComponentType) {
        String result = next();
        unitIdComponentType.value = type;
        return result;
    }

    /**
     * Return the last UnitIdComponentType from a next() call.
     *
     * @return
     */
    public UnitIdComponentType getLastUnitIdComponentType() {
        return type;
    }

    /**
     * Parses the next segment in the source from set. The UnitIdComponentType can be retrieved
     * after calling, from getLastUnitIdComponentType()
     *
     * @return a unit segment of the form: prefix* base suffix*, and, per, or power; or null if no
     *     more remaining
     */
    @Override
    public String next() {
        String output = null;
        State state = State.start;
        UnitIdComponentType outputType = null;

        while (true) {
            if (bufferedItem == null) {
                if (!source.hasNext()) {
                    break;
                }
                bufferedItem = source.next();
                bufferedType = componentTypeSupplier.apply(bufferedItem);
            }
            switch (bufferedType) {
                case prefix:
                    switch (state) {
                        case start:
                            state = State.havePrefix;
                            break;
                        case havePrefix: // ok, continue
                            break;
                        case haveBaseOrSuffix:
                            type =
                                    outputType == UnitIdComponentType.suffix
                                            ? UnitIdComponentType.base
                                            : outputType;
                            return output;
                    }
                    break;
                case base:
                    switch (state) {
                        case start:
                        case havePrefix:
                            state = State.haveBaseOrSuffix;
                            break;
                        case haveBaseOrSuffix: // have stuff to return
                            type =
                                    outputType == UnitIdComponentType.suffix
                                            ? UnitIdComponentType.base
                                            : outputType;
                            return output;
                    }
                    break;
                case suffix:
                    switch (state) {
                        case start:
                        case havePrefix:
                            throw new IllegalArgumentException(
                                    "Unit suffix must follow base: "
                                            + original
                                            + " → "
                                            + output
                                            + " ❌ "
                                            + bufferedItem);
                        case haveBaseOrSuffix: // ok, continue
                            break;
                    }
                    break;
                case and:
                case per:
                case power:
                    switch (state) {
                        case start: // return this item
                            output = bufferedItem;
                            bufferedItem = null;
                            type = bufferedType;
                            return output;
                        case havePrefix:
                            throw new IllegalArgumentException(
                                    "Unit prefix must be followed with base: "
                                            + original
                                            + " → "
                                            + output
                                            + " ❌ "
                                            + bufferedItem);
                        case haveBaseOrSuffix: // have stuff to return
                            type =
                                    outputType == UnitIdComponentType.suffix
                                            ? UnitIdComponentType.base
                                            : outputType;
                            return output;
                    }
                    break;
            }
            output = output == null ? bufferedItem : output + "-" + bufferedItem;
            bufferedItem = null;
            outputType = bufferedType;
        }
        switch (state) {
            default:
            case start:
                return null;
            case havePrefix:
                throw new IllegalArgumentException(
                        "Unit prefix must be followed with base: "
                                + original
                                + " → "
                                + output
                                + " ❌ "
                                + bufferedItem);
            case haveBaseOrSuffix: // have stuff to return
                type =
                        outputType == UnitIdComponentType.suffix
                                ? UnitIdComponentType.base
                                : outputType;
                return output;
        }
    }

    // TODO create from custom map
    public UnitIdComponentType getUnitIdComponentType(String part) {
        return componentTypeSupplier.apply(part);
    }
}
