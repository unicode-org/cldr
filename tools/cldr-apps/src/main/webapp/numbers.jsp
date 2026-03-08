<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%@ page
 import="java.util.*,java.util.regex.*,java.io.IOException,java.text.ParsePosition,com.ibm.icu.impl.ICUResourceBundle,com.ibm.icu.math.BigDecimal,com.ibm.icu.text.*,com.ibm.icu.util.*,com.ibm.icu.impl.ICUData"
 contentType="text/html;charset=UTF-8"
 pageEncoding="UTF-8"%>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
<head>
  <meta content="text/html; charset=UTF-8" http-equiv="Content-Type" />
  <meta content="George Rhoten" name="AUTHOR" />
  <title>Number Format Tester</title>
  <link rel="icon" type="image/png" href="/favicon.png"/>
  <link rel='stylesheet' type='text/css' href='./surveytool.css' />
<style type="text/css">
  .rtl {text-align:right}
  .nonsense {color:#C3C3C3; background-color:lightgray}
  .thead {text-align:center; font-weight:bold;}
/*  .results {font-family:Sans-serif;}*/
div.expander {
    cursor: pointer;
}
div.expander > span {
    border-width: 1px;
    border-style: solid;
    background-color: #EEEEEE;
    color: #111111;
    text-decoration: none;
    padding-left: 0.25em;
    padding-right: 0.25em;
    padding-top: 0;
    padding-bottom: 0;
    margin-right: 0.5em;
    line-height: 1.6em;
    vertical-align: top;
    font-size: 75%;
    font-weight: bold;
}
h2 {
    margin: 0;
    padding: 0;
    border: 0;
}
</style>
<%!
static final String ORDINAL_RULES = "OrdinalRules";
static final String SPELLOUT_RULES = "SpelloutRules";
static final String NUMBERING_SYSTEM_RULES = "NumberingSystemRules";
static final List<ULocale> LOCALES = getAvailableLocales();
static final boolean PARSE_CHECK = true;
static final int MAX_LINE_COUNT = 1000;
static final int MAX_FRACTION_DIGITS = 9;

/**
 * A range of numbers to format. The long constructor creates an exact integer range; the double
 * constructor handles fractions and special values (Inf, NaN). When {@code start} is null the
 * range is a single special double value stored in {@code complexVal}.
 */
private static final class NumberRange {
    final BigDecimal start;
    final BigDecimal end;
    final double complexVal;

    NumberRange(long start, long end) {
        this.start = new BigDecimal(start);
        this.end = new BigDecimal(end);
        this.complexVal = Double.NaN;
    }

    NumberRange(double start, double end) {
        if (Double.isInfinite(start) || Double.isNaN(start)) {
            this.start = null;
            this.end = null;
            this.complexVal = start;
        } else {
            this.start = new BigDecimal(start).setScale(MAX_FRACTION_DIGITS, BigDecimal.ROUND_HALF_UP);
            this.end = new BigDecimal(end).setScale(MAX_FRACTION_DIGITS, BigDecimal.ROUND_HALF_UP);
            this.complexVal = Double.NaN;
        }
    }

    boolean isRational() {
        return start != null;
    }
}

static final NumberRange[] DEFAULT_RANGES = new NumberRange[] {
        new NumberRange(-1, 0),
        new NumberRange(0.2, 0.2),
        new NumberRange(1, 31),
        new NumberRange(98, 102),
        new NumberRange(998, 1002),
        new NumberRange(1998, 2002),
        new NumberRange(9998, 10002),
        new NumberRange(100000, 100001),
        new NumberRange(1000000, 1000001),
        new NumberRange(10000000, 10000001),
        new NumberRange(100000000, 100000001),
        new NumberRange(1000000000, 1000000001),
    };

static List<ULocale> getAvailableLocales() {
    ULocale[] locales = NumberFormat.getAvailableULocales();
    List<ULocale> result = new ArrayList<ULocale>(locales.length/2);
    for (ULocale loc : locales) {
        RuleBasedNumberFormat rbnf = new RuleBasedNumberFormat(loc, RuleBasedNumberFormat.SPELLOUT);
        if (!rbnf.getLocale(ULocale.ACTUAL_LOCALE).equals(loc)) {
            // Uninteresting duplicate data. Show only the minimal set of information.
            continue;
        }
        result.add(loc);
    }
    return result;
}
static String escapeString(String arg) {
    return arg.replaceAll("&", "&amp;").replaceAll("\"", "&quot;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
}

/**
 * If we get something like 3.8.1.0_11, it becomes just "3.8.1".
 * If we get something like 4.0.0.0, it becomes just "4.0".
 */
static String trimVersion(String ver) {
    int endIdx = ver.lastIndexOf('_') - 1;
    if (endIdx < 0) {
        endIdx = ver.length() - 1;
    }
    for (; endIdx >= 0; endIdx--) {
        char currChar = ver.charAt(endIdx);
        if (currChar != '0' && currChar != '.') {
            endIdx++;
            break;
        }
    }
    if (endIdx == 1) {
        int minorEndIdx = ver.indexOf('.', endIdx + 1);
        if (minorEndIdx > endIdx) {
            endIdx = minorEndIdx;
        }
    }
    return ver.substring(0, endIdx);
}

private static String getRulePrefix(String currRuleName) {
    try {
        return currRuleName.substring(1, currRuleName.indexOf('-'));
    }
    catch (StringIndexOutOfBoundsException e) {
        return currRuleName;
    }
}

private static String getDisplayName(String currRuleName) {
    try {
        String prefix = currRuleName.substring(1, currRuleName.indexOf('-'));
        if (prefix.equals("spellout") || prefix.equals("digits") || prefix.equals("duration")) {
            currRuleName = currRuleName.substring(currRuleName.indexOf('-') + 1);
        }
    }
    catch (StringIndexOutOfBoundsException e) {
        // Meh, use the default.
    }
    return escapeString(currRuleName);
}

private static void printSkipLine(JspWriter out, RuleBasedNumberFormat rbnf) throws IOException {
    //System.out.println("<tr><td colspan=\"" + tableColumns + "\">...</td></tr>");
    out.print("<tr><td>...</td>");
    for (String name : rbnf.getRuleSetNames()) {
        out.print("<th class=\"thead\"><b>" + getDisplayName(name) + "</b></th>");
    }
    out.println("</tr>");
}

static final NumberFormat FRACTION_FORMATTER = NumberFormat.getInstance(ULocale.US);
static {
    FRACTION_FORMATTER.setMaximumFractionDigits(MAX_FRACTION_DIGITS);
    FRACTION_FORMATTER.setMinimumFractionDigits(1);
    FRACTION_FORMATTER.setGroupingUsed(false);
}

private static String formatNumber(double num) {
    long longVal = (long)num;
    String numStr;
    if (Double.isInfinite(num)) {
        if (num < 0) {
            numStr = "-Inf";
        }
        else {
            numStr = "Inf";
        }
    }
    else if (longVal != num) {
        numStr = FRACTION_FORMATTER.format(num);
    }
    else {
        numStr = Long.toString(longVal);
    }
    return numStr;
}

private static String formatNumber(BigDecimal num) {
    if (num.scale() == 0) {
        return Long.toString(num.longValue());
    }
    return FRACTION_FORMATTER.format(num);
}

private static void printUnrationalLine(JspWriter out, RuleBasedNumberFormat rbnf, double num, boolean isRTL) throws IOException {
    String numStr = formatNumber(num);

    out.print("<tr><td>" + numStr + "</td>");
    for (String name : rbnf.getRuleSetNames()) {
        String result = rbnf.format(num, name);
        String errorMsg = "";
        if (PARSE_CHECK && !Double.isNaN(num)) {
            // Even when it's an irrelevant value, we want to parse to make
            // sure that there is no exception being thrown.
            try {
                Number parseResult = rbnf.parse(result, new ParsePosition(0));
                if (parseResult.doubleValue() != num && !isIrrelevantValue(name, num)) {
                    errorMsg = "<br/><span style=\"color:red\">Error parsed as " + escapeString(parseResult.toString()) + "</span>";
                }
            }
            catch (Throwable e) {
                errorMsg = "<br/><span style=\"color:red\">Error parsing " + escapeString(e.getMessage()) + "</span>";
            }
        }
        out.print("<td" + getNumberStyle(name, num, isRTL) + ">" + escapeString(result) + errorMsg+ "</td>");
    }
    out.println("</tr>");
}

private static void printLine(JspWriter out, RuleBasedNumberFormat rbnf, BigDecimal num, boolean isRTL) throws IOException {
    String numStr;
    boolean isInteger = num.scale() == 0;
    if (isInteger) {
        numStr = num.toString();
    }
    else {
        numStr = FRACTION_FORMATTER.format(num);
    }

    out.print("<tr><td>" + numStr + "</td>");
    for (String name : rbnf.getRuleSetNames()) {
        String result;
        if (isInteger) {
            result = rbnf.format(num.longValue(), name);
        }
        else {
            result = rbnf.format(num.doubleValue(), name);
        }
        String errorMsg = "";
        if (PARSE_CHECK) {
            try {
                Number parseResult = rbnf.parse(result, new ParsePosition(0));
                boolean roundtrips = (isInteger && parseResult.longValue() == num.longValue())
                        || (!isInteger && parseResult.doubleValue() == num.doubleValue());
                if (!roundtrips && !isIrrelevantValue(name, num)) {
                    errorMsg = "<br/><span style=\"color:red\">Error parsed as " + escapeString(parseResult.toString()) + "</span>";
                }
            }
            catch (Throwable e) {
                errorMsg = "<br/><span style=\"color:red\">Error parsing " + escapeString(e.getMessage()) + "</span>";
            }
        }
        out.print("<td" + getNumberStyle(name, num.doubleValue(), isRTL) + ">" + escapeString(result) + errorMsg + "</td>");
    }
    out.println("</tr>");
}

private static boolean isIrrelevantValue(String ruleName, double val) {
    return (val < 1 || val != (long)val) && (ruleName.contains("ordinal") || ruleName.contains("year"));
}

private static boolean isIrrelevantValue(String ruleName, BigDecimal val) {
    return (val.compareTo(BigDecimal.ONE) < 0 || val.scale() != 0)
            && (ruleName.contains("ordinal") || ruleName.contains("year"));
}

private static String getNumberStyle(String ruleName, double val, boolean isRTL) {
    if (isIrrelevantValue(ruleName, val)) {
        if (isRTL) {
            return " class=\"nonsense rtl\"";
        }
        return " class=\"nonsense\"";
    }
    if (isRTL) {
        return " class=\"rtl\"";
    }
    return "";
}

private static String getRules(ULocale selectedLocale, String ruleType) {
    // We are doing this silliness because the Java compiler likes to hard code the string during initial compilation (javac).
    // This is a problem when the ICU4J jar is upgraded, and this JSP is not recompiled.
    ICUResourceBundle rbnfBundle = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUData.ICU_RBNF_BASE_NAME.replaceFirst("[0-9]+", Integer.toString(VersionInfo.ICU_VERSION.getMajor())), selectedLocale);
    UResourceBundle ruleTypeBundle;
    try {
        ruleTypeBundle = rbnfBundle.getWithFallback("RBNFRules/" + ruleType);
    }
    catch (MissingResourceException e) {
        throw new MissingResourceException("Rule type " + ruleType + " does not exist.", e.getClassName(), e.getKey());
    }
    StringBuilder sb = new StringBuilder();
    for (String ruleStr : ruleTypeBundle.getStringArray()) {
        if (sb.length() > 0) {
            sb.append("\n");
        }
        sb.append(ruleStr);
    }
    return sb.toString();
}

private static double parseNumber(String str) {
    if ("Inf".equals(str)) {
        return Double.POSITIVE_INFINITY;
    }
    if ("-Inf".equals(str)) {
        return Double.NEGATIVE_INFINITY;
    }
    if ("NaN".equals(str)) {
        return Double.NaN;
    }
    return Double.parseDouble(str);
}

private static final Pattern NUMBER_PAIR = Pattern.compile("(-?[0-9,.]+|-?Inf|NaN)(?:-(-?[0-9,.]+))?");

private static NumberRange[] getNumberRanges(String numbers) {
    String []numberLines = numbers.split("[;\\r\\n]");
    NumberRange []result = new NumberRange[numberLines.length];
    int count = 0;
    for (String line : numberLines) {
        if (line.length() == 0) {
            continue;
        }
        try {
            Matcher numberPair = NUMBER_PAIR.matcher(line);
            if (numberPair.find()) {
                String startStr = numberPair.group(1).replace(",", "");
                String endStr = numberPair.group(2) != null
                        ? numberPair.group(2).replace(",", "")
                        : startStr;
                try {
                    result[count++] = new NumberRange(Long.parseLong(startStr), Long.parseLong(endStr));
                }
                catch (NumberFormatException e) {
                    result[count++] = new NumberRange(parseNumber(startStr), parseNumber(endStr));
                }
            }
        }
        catch (Exception pe) {
            // skip
        }
    }
    if (count == 0) {
        return null;
    }
    return result;
}

%><%

request.setCharacterEncoding("UTF-8");

String selectedLocaleStr = request.getParameter("locale");
ULocale selectedLocale;
if (selectedLocaleStr == null) {
    selectedLocale = ULocale.forLocale(request.getLocale());
}
else {
    selectedLocale = new ULocale(selectedLocaleStr);
}
if (!LOCALES.contains(selectedLocale)) {
    selectedLocale = new ULocale(selectedLocale.getLanguage());
}
if (!LOCALES.contains(selectedLocale)) {
    selectedLocale = ULocale.ENGLISH;
}

boolean isEdit = request.getParameter("edit") != null;

String type = request.getParameter("type");
if (type == null) {
    type = SPELLOUT_RULES;
}
String errorMsg = null;
String rulesStr = request.getParameter("rules");
if (!isEdit || rulesStr == null || rulesStr.isEmpty()) {
    try {
        rulesStr = getRules(selectedLocale, type);
    }
    catch (Exception e) {
        if (errorMsg == null) {
            // The standard default rules can not be loaded? That is bad. Is ICU okay?
            errorMsg = e.getMessage();
        }
        rulesStr = "";
    }
}
RuleBasedNumberFormat rbnf = null;
try {
    rbnf = new RuleBasedNumberFormat(rulesStr, selectedLocale);
}
catch (Exception e) {
    if (errorMsg == null) {
        errorMsg = e.getMessage();
    }
}
String numbers = request.getParameter("numbers");
if (numbers == null) {
    numbers = "";
}
NumberRange []ranges = getNumberRanges(numbers);
if (ranges == null || ranges.length == 0) {
    ranges = DEFAULT_RANGES;
}


%>
<script type="text/javascript">
//<![CDATA[
/*
Calling this function toggles the display a set of subitems.
@param middleElem The node that is before the node to toggle.
*/
function toggleView(middleElem) {
    let groupElem = middleElem;
    do {
        groupElem = groupElem.nextSibling;
    }
    while (groupElem.nodeType !== 1); // Get to the next real node.
    let selectorElem = middleElem.firstChild;
    if (groupElem.style.display === 'block') {
        groupElem.style.display = 'none';
        selectorElem.innerHTML = '+';
    }
    else {
        groupElem.style.display = 'block';
        selectorElem.innerHTML = '\u2212';
    }
    selectorElem.blur();
}
//]]>
</script>
</head>
<body style="padding-left: 1em; padding-right: 1em;">
<h2>Number Format Tester</h2>

<form action='<%= escapeString(request.getRequestURI()) %>' method="post">
<table style="border: solid black 1px; width: 100%;">
<tr><th style="text-align: right; width: 1%; white-space:nowrap; border-width: 0;"><label for="locale">Locale</label></th>
    <td style="text-align: left; width: 1%; white-space:nowrap"><select id="locale" name="locale"><%
for (ULocale currULoc : LOCALES) {
    String currLocStr = currULoc.toString();
    out.println("<option"+(currULoc.equals(selectedLocale) ?" selected=\"selected\"":"")+" value=\""+currLocStr+"\">" +currLocStr+" ["+currULoc.getDisplayName()+"]</option>");
}
%></select></td>
<td style="padding-left: 1em;"><input type="submit" name="load" value="Load Rules"/></td></tr>

<tr><th style="text-align: right; width: 1%; white-space:nowrap; border-width: 0;"><label for="type">Type</label></th>
<td style="text-align: left; width: 1%; white-space:nowrap;"><select id="type" name="type">
<option<%=(type.equals(NUMBERING_SYSTEM_RULES) ?" selected=\"selected\"":"")%> value="<%=NUMBERING_SYSTEM_RULES%>">Numbering System</option>
<option<%=(type.equals(ORDINAL_RULES) ?" selected=\"selected\"":"")%> value="<%=ORDINAL_RULES%>">Ordinal</option>
<option<%=(type.equals(SPELLOUT_RULES) ?" selected=\"selected\"":"")%> value="<%=SPELLOUT_RULES%>">Spellout</option>
</select></td>
<td></td></tr>
<tr><td colspan="3">
<div onclick="toggleView(this)" class="expander"><span><%= !isEdit ? "+" : "\u2212" %></span><strong>Edit</strong></div>
<table style="width: 100%; display: <%= !isEdit ? "none" : "block" %>">

<tr>
<th style="text-align: left; white-space:nowrap; border-width: 0;"><label for="numbers">Numbers</label></th>
<th style="text-align: left; width: 100%; white-space:nowrap; border-width: 0;"><label for="rules">Rules</label></th>
</tr>
<tr>
    <td style="padding-right: 1em;"><textarea id="numbers" name="numbers" cols="20" rows="40"><%
StringBuilder numberLine = new StringBuilder();
for (NumberRange range: ranges) {
    if (range == null) {
        continue;
    }
    if (numberLine.length() > 0) {
        numberLine.append("\n");
    }
    if (range.isRational()) {
        numberLine.append(formatNumber(range.start));
        if (range.start.compareTo(range.end) < 0) {
            numberLine.append("-").append(formatNumber(range.end));
        }
    } else {
        numberLine.append(formatNumber(range.complexVal));
    }
}
out.println(numberLine.toString());
%></textarea></td>
    <td style="padding-right: 1em;"><textarea id="rules" name="rules" cols="20" rows="40" style="width: 100%"><%= escapeString(rulesStr) %></textarea></td></tr>

<tr>
    <td colspan="2" style="padding-right: 1em; padding-left: 1em"></td></tr>

<tr><td colspan="2" style="padding-right: 1em; padding-left: 1em"><input type="submit" name="edit" value="Format" /></td></tr>
</table>
</td></tr>
</table>
</form>

<h3>Result</h3>

<%
if (errorMsg != null) {
    out.print("<p style=\"color:red;\">"+escapeString(errorMsg)+"</p>");
}
else {
    int tableColumns = 0;
    try {
        int numColumns = rbnf.getRuleSetNames().length;
        tableColumns += numColumns;
        out.print("<table border=\"1\" cellspacing=\"0\" cellpadding=\"2\" class=\"results\">");
        out.print("<tr><th rowspan=\"2\" class=\"thead\"><b>Number</b></th>");
        out.print("<th colspan=\"" + numColumns + "\" style=\"text-align:center;\">"+escapeString(getRulePrefix(rbnf.getDefaultRuleSetName())));
        out.print("<br/><span style=\"color: gray\">Default = " + getDisplayName(rbnf.getDefaultRuleSetName()));
        out.println("</span></th>");
        out.println("</tr>");
        out.print("<tr>");
        for (String name : rbnf.getRuleSetNames()) {
            out.print("<th class=\"thead\"><b>" + getDisplayName(name) + "</b></th>");
        }
        out.println("</tr>");
        byte direction = Character.getDirectionality(rbnf.format(1).charAt(0));
        boolean isRTL = (direction == Character.DIRECTIONALITY_RIGHT_TO_LEFT || direction == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC);
        int lineCount = 0;
        for (int rangeIdx = 0; rangeIdx < ranges.length; rangeIdx++) {
            if (ranges[rangeIdx] == null) {
                continue;
            }
            NumberRange range = ranges[rangeIdx];
            if (range.isRational() && rangeIdx != 0) {
                printSkipLine(out, rbnf);
            }
            if (!range.isRational()) {
                printUnrationalLine(out, rbnf, range.complexVal, isRTL);
            }
            else {
                BigDecimal num = range.start;
                BigDecimal end = range.end;
                while (num.compareTo(end) <= 0) {
                    if (lineCount++ >= MAX_LINE_COUNT) {
                        throw new Exception("Too many numbers to format.");
                    }
                    printLine(out, rbnf, num, isRTL);
                    num = num.add(BigDecimal.ONE);
                }
            }
        }
    }
    catch (Exception e) {
        out.print("<tr><td colspan=\"" + (tableColumns + 1) + "\" style=\"color:red; text-align: center\">"+escapeString(e.getMessage())+"</td></tr>");
    }
    out.println("</table>");
}
%>
<hr />
        <div style="float: left;padding-bottom: 1em"><a href="https://unicode.org/">Unicode</a> | <a href="https://cldr.unicode.org/">CLDR</a>
        | <a href="https://unicode.org/reports/tr35/tr35-numbers.html#RBNF_Syntax">Rule Based Number Format Syntax Documentation</a>
        | <a href="https://cldr.unicode.org/requesting_changes#how-to-file-a-ticket">Feedback or corrections to the displayed rules</a></div>
<div style="float: right; font-size: 60%;"><span class="notselected">Powered by
<a href="https://icu.unicode.org/">ICU</a> <%= trimVersion(VersionInfo.ICU_VERSION.toString()) %></span></div>
</body>
</html>
