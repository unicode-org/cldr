/*
 ******************************************************************************
 * Copyright (C) 2004-2005, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 *
 * in shell:  (such as .cldrrc)
 *   export CWDEBUG="-DCLDR_DTD_CACHE=/tmp/cldrdtd/"
 *   export CWDEFS="-DCLDR_DTD_CACHE_DEBUG=y ${CWDEBUG}"
 *
 *
 * in code:
 *   docBuilder.setEntityResolver(new CachingEntityResolver());
 *
 */
package org.unicode.cldr.util;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class ExtractCollationRules {
    Map<String, String> type_rules = new TreeMap<String, String>();
    XPathParts parts = new XPathParts();
    StringBuffer rules = new StringBuffer();

    public ExtractCollationRules set(CLDRFile file) {
        type_rules.clear();
        String lastType = "";
        rules.setLength(0);

        String context = null;

        for (Iterator it = file.iterator("//ldml/collations", file.getComparator()); it.hasNext();) {

            // System.out.print(rules.substring(lastLen, rules.length()));
            // lastLen = rules.length();

            String path = (String) it.next();
            String value = file.getStringValue(path);
            parts.set(path);
            String type = parts.findAttributeValue("collation", "type");
            if (!type.equals(lastType)) {
                lastType = type;
                type_rules.put(lastType, rules.toString());
                rules.setLength(0);
            }
            String mainType = parts.getElement(3);
            // base?, settings?, suppress_contractions?, optimize?
            // x: context?, ( p | pc | s | sc | t | tc | i | ic )*, extend?
            if (mainType.equals("settings")) {
                writeSettings(parts.getAttributes(3), rules);
                continue;
            } else if (mainType.equals("rules")) {
                String ruleType = parts.getElement(4);
                char c = ruleType.charAt(0);
                if (c == 'x') {
                    ruleType = parts.getElement(5);
                    c = ruleType.charAt(0);
                }
                boolean isMultiple = ruleType.length() > 1 && ruleType.charAt(1) == 'c';
                String lastContext = context;
                context = null;
                switch (c) {
                case 'r':
                    appendOrdering("&", null, value, false, true);
                    break;
                case 'p':
                    appendOrdering("<", lastContext, value, isMultiple, true);
                    break;
                case 's':
                    appendOrdering("<<", lastContext, value, isMultiple, true);
                    break;
                case 't':
                    appendOrdering("<<<", lastContext, value, isMultiple, false);
                    break;
                case 'i':
                    appendOrdering("=", lastContext, value, isMultiple, false);
                    break;
                case 'c':
                    context = value;
                    break;
                case 'e':
                    appendOrdering("/", null, value, false, false);
                    break;
                default:
                    System.out.println("Couldn't handle: " + path + "\t" + value);
                }
                continue;
            } else {

            }
            System.out.println("Couldn't handle: " + path + "\t" + value);
        }
        type_rules.put(lastType, rules.toString());
        return this;
    }

    private void appendOrdering(String relation, String context, String valueAfter, boolean isMultiple,
        boolean lineBreakBefore) {
        if (isMultiple) {
            int cp;
            for (int i = 0; i < valueAfter.length(); i += UTF16.getCharCount(cp)) {
                cp = UTF16.charAt(valueAfter, i);
                if (lineBreakBefore)
                    rules.append(CldrUtility.LINE_SEPARATOR);
                else
                    rules.append(' ');
                rules.append(relation);
                if (context != null) rules.append(' ').append(quote(context));
                rules.append(' ').append(quote(UTF16.valueOf(cp)));
            }
        } else {
            if (lineBreakBefore)
                rules.append(CldrUtility.LINE_SEPARATOR);
            else
                rules.append(' ');
            rules.append(relation);
            if (context != null) rules.append(' ').append(quote(context));
            rules.append(' ').append(quote(valueAfter));
        }
    }

    private void writeSettings(Map<String, String> attributes, StringBuffer results) {
        for (Iterator<String> it = attributes.keySet().iterator(); it.hasNext();) {
            String attribute = it.next();
            String value = attributes.get(attribute);
            // TODO fix different cases
            results.append("[" + attribute + " " + value + "]" + CldrUtility.LINE_SEPARATOR);
            // if (attribute.equals("normalization")) {
            //
            // }
        }
    }

    public Iterator<String> iterator() {
        return type_rules.keySet().iterator();
    }

    public String getRules(Object key) {
        return (String) type_rules.get(key);
    }

    static StringBuffer quoteOperandBuffer = new StringBuffer(); // faster

    static UnicodeSet needsQuoting = null;
    static UnicodeSet needsUnicodeForm = null;

    static final String quote(String s) {
        if (needsQuoting == null) {
            /*
             * c >= 'a' && c <= 'z'
             * || c >= 'A' && c <= 'Z'
             * || c >= '0' && c <= '9'
             * || (c >= 0xA0 && !UCharacterProperty.isRuleWhiteSpace(c))
             */
            needsQuoting = new UnicodeSet(
                "[[:whitespace:][:c:][:z:][:ascii:]-[a-zA-Z0-9]]"); //
            // "[[:ascii:]-[a-zA-Z0-9]-[:c:]-[:z:]]"); // [:whitespace:][:c:][:z:]
            // for (int i = 0; i <= 0x10FFFF; ++i) {
            // if (UCharacterProperty.isRuleWhiteSpace(i)) needsQuoting.add(i);
            // }
            // needsQuoting.remove();
            needsUnicodeForm = new UnicodeSet("[\\u000d\\u000a[:zl:][:zp:]]");
        }
        s = Normalizer.compose(s, false);
        quoteOperandBuffer.setLength(0);
        boolean noQuotes = true;
        boolean inQuote = false;
        int cp;
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(s, i);
            if (!needsQuoting.contains(cp)) {
                if (inQuote) {
                    quoteOperandBuffer.append('\'');
                    inQuote = false;
                }
                quoteOperandBuffer.append(UTF16.valueOf(cp));
            } else {
                noQuotes = false;
                if (cp == '\'') {
                    quoteOperandBuffer.append("''");
                } else {
                    if (!inQuote) {
                        quoteOperandBuffer.append('\'');
                        inQuote = true;
                    }
                    if (!needsUnicodeForm.contains(cp))
                        quoteOperandBuffer.append(UTF16.valueOf(cp)); // cp != 0x2028
                    else if (cp > 0xFFFF) {
                        quoteOperandBuffer.append("\\U").append(hex(cp, 8));
                    } else if (cp <= 0x20 || cp > 0x7E) {
                        quoteOperandBuffer.append("\\u").append(hex(cp, 4));
                    } else {
                        quoteOperandBuffer.append(UTF16.valueOf(cp));
                    }
                }
            }
            /*
             * switch (c) {
             * case '<': case '>': case '#': case '=': case '&': case '/':
             * quoteOperandBuffer.append('\'').append(c).append('\'');
             * break;
             * case '\'':
             * quoteOperandBuffer.append("''");
             * break;
             * default:
             * if (0 <= c && c < 0x20 || 0x7F <= c && c < 0xA0) {
             * quoteOperandBuffer.append("\\u").append(Utility.hex(c));
             * break;
             * }
             * quoteOperandBuffer.append(c);
             * break;
             * }
             */
        }
        if (inQuote) {
            quoteOperandBuffer.append('\'');
        }
        if (noQuotes) return s; // faster
        return quoteOperandBuffer.toString();
    }

    static public String hex(long i, int places) {
        if (i == Long.MIN_VALUE) return "-8000000000000000";
        boolean negative = i < 0;
        if (negative) {
            i = -i;
        }
        String result = Long.toString(i, 16).toUpperCase();
        if (result.length() < places) {
            result = "0000000000000000".substring(result.length(), places) + result;
        }
        if (negative) {
            return '-' + result;
        }
        return result;
    }

}
