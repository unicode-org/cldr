package org.unicode.cldr.util;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.ibm.icu.util.Output;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.unicode.cldr.util.SupplementalDataInfo.UnitIdComponentType;

public class UnitParser {
    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    private static final CLDRConfig info = CLDR_CONFIG;
    private static final SupplementalDataInfo SDI = info.getSupplementalDataInfo();
    public static final Splitter DASH_SPLITTER = Splitter.on('-');
    public static final Joiner DASH_JOIN = Joiner.on('-');

    private String bufferedItem = null;
    private UnitIdComponentType bufferedType = null;
    private Iterator<String> source;

    public UnitParser set(Iterator<String> source) {
        bufferedItem = null;
        this.source = source;
        return this;
    }

    public UnitParser set(Iterable<String> source) {
        return set(source.iterator());
    }

    public UnitParser set(String source) {
        return set(UnitParser.DASH_SPLITTER.split(source).iterator());
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
    public String nextParse(Output<UnitIdComponentType> type) {
        String output = null;
        State state = State.start;
        UnitIdComponentType outputType = null;

        while (true) {
            if (bufferedItem == null) {
                if (!source.hasNext()) {
                    break;
                }
                bufferedItem = source.next();
                bufferedType = SDI.getUnitIdComponentType(bufferedItem);
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
                            type.value =
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
                            type.value =
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
                            type.value = bufferedType;
                            return output;
                        case havePrefix:
                            throw new IllegalArgumentException(
                                    "Unit prefix must be followed with base: "
                                            + output
                                            + " ❌ "
                                            + bufferedItem);
                        case haveBaseOrSuffix: // have stuff to return
                            type.value =
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
                        "Unit prefix must be followed with base: " + output + " ❌ " + bufferedItem);
            case haveBaseOrSuffix: // have stuff to return
                type.value =
                        outputType == UnitIdComponentType.suffix
                                ? UnitIdComponentType.base
                                : outputType;
                return output;
        }
    }

    // TODO create from custom map
    public UnitIdComponentType getUnitIdComponentType(String part) {
        return SDI.getUnitIdComponentType(part);
    }
}
