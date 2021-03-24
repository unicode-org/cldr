package org.unicode.cldr.util;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.unicode.cldr.util.DtdData.ValueStatus;

import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R3;

/**
 * Checks that an {element,attribute,attributeValue} tuple is valid, caching the results.
 */
public class PathChecker {
    // caches for speed
    private static final Map<XPathParts, Boolean> seen = new ConcurrentHashMap<>();
    private static Map<DtdType, Map<String,Map<String,Map<String,ValueStatus>>>> seenEAV = new ConcurrentHashMap<>();

    /**
     * Returns true if the path is ok. The detailed errors (if any) are set only the first time the path is seen!
     */
    public boolean checkPath(String path) {
        XPathParts parts = XPathParts.getFrozenInstance(path);
        return checkPath(parts, null);
    }

    /**
     * Returns true if the path is ok. The detailed errors (if any) are set only the first time the path is seen!
     */

    public boolean checkPath(String path, Map<Row.R3<String,String,String>, ValueStatus> errors) {
        XPathParts parts = XPathParts.getFrozenInstance(path);
        return checkPath(parts, errors);
    }

    /**
     * Returns true if the path is ok. The detailed errors (if any) are set only the first time the path is seen!
     */
    public boolean checkPath(XPathParts parts, Map<Row.R3<String, String, String>, ValueStatus> errors) {
        Boolean seenAlready = seen.get(parts);
        if (seenAlready != null) {
            return seenAlready;
        }
        DtdData dtdData = parts.getDtdData();
        boolean ok = true;
        if (errors != null) {
            errors.clear();
        }

        for (int elementIndex = 0; elementIndex < parts.size(); ++elementIndex) {
            String element = parts.getElement(elementIndex);
            for (Entry<String, String> entry : parts.getAttributes(elementIndex).entrySet()) {
                String attribute = entry.getKey();
                String attrValue = entry.getValue();
                ok &= checkAttribute(dtdData, element, attribute, attrValue, errors);
            }
        }
        seen.put(parts, ok);
        return ok;
    }

    private boolean checkAttribute(DtdData dtdData, String element, String attribute, String attrValue, Map<R3<String, String, String>, ValueStatus> errors) {
        // check if we've seen the EAV yet
        // we don't need to synchronize because a miss isn't serious
        Map<String, Map<String, Map<String, ValueStatus>>> elementToAttrToAttrValueToStatus = seenEAV.get(dtdData.dtdType);
        if (elementToAttrToAttrValueToStatus == null) {
            Map<String, Map<String, Map<String, ValueStatus>>> subAlready = seenEAV.putIfAbsent(dtdData.dtdType, elementToAttrToAttrValueToStatus = new ConcurrentHashMap<>());
            if (subAlready != null) {
                elementToAttrToAttrValueToStatus = subAlready; // discards empty map
            }
        }
        Map<String, Map<String, ValueStatus>> attrToAttrValueToStatus = elementToAttrToAttrValueToStatus.get(element);
        if (attrToAttrValueToStatus == null) {
            Map<String, Map<String, ValueStatus>> subAlready = elementToAttrToAttrValueToStatus.putIfAbsent(element, attrToAttrValueToStatus = new ConcurrentHashMap<>());
            if (subAlready != null) {
                attrToAttrValueToStatus = subAlready; // discards empty map
            }
        }
        Map<String, ValueStatus> attrValueToStatus = attrToAttrValueToStatus.get(attribute);
        if (attrValueToStatus == null) {
            Map<String, ValueStatus> setAlready = attrToAttrValueToStatus.putIfAbsent(attribute, attrValueToStatus = new ConcurrentHashMap<>());
            if (setAlready != null) {
                attrValueToStatus = setAlready; // discards empty map
            }
        }
        ValueStatus valueStatus = attrValueToStatus.get(attrValue);
        if (valueStatus == null) {
            valueStatus = dtdData.getValueStatus(element, attribute, attrValue);
            if (valueStatus != ValueStatus.valid) {
                // Set breakpoint here for debugging (referenced from http://cldr.unicode.org/development/testattributevalues)
                dtdData.getValueStatus(element, attribute, attrValue);
            }
            attrValueToStatus.putIfAbsent(attrValue, valueStatus);
        }

        if (errors != null && valueStatus != ValueStatus.valid) {
            errors.put(Row.of(element, attribute, attrValue), valueStatus);
        }
        return valueStatus != null;
    }
}