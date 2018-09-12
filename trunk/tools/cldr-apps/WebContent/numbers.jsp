<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%@ page language="java"
 import="java.util.*,java.util.regex.*,java.io.IOException,java.text.ParsePosition,com.ibm.icu.impl.ICUResourceBundle,com.ibm.icu.text.*,com.ibm.icu.util.*,com.ibm.icu.impl.ICUData"
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
  .noparse {background-color:lightgray}
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
    padding-top: 0em;
    padding-bottom: 0em;
    margin-right: 0.5em;
    line-height: 1.6em;
    vertical-align: top;
    font-size: 75%;
    font-weight: bold;
}

</style>
<%!
static final String ORDINAL_RULES = "OrdinalRules";
static final String SPELLOUT_RULES = "SpelloutRules";
static final String NUMBERING_SYSTEM_RULES = "NumberingSystemRules";
static final List<ULocale> LOCALES = getAvailableLocales();
static final boolean PARSE_CHECK = true;
static final int MAX_LINE_COUNT = 1000;
static final double[][] DEFAULT_RANGES = new double[][] {
        new double[]{-1, 0},
        new double[]{0.2, 0.2},
        new double[]{1, 31},
        new double[]{98, 102},
        new double[]{998, 1002},
        new double[]{1998, 2002},
        new double[]{9998, 10002},
        new double[]{100000, 100001},
        new double[]{1000000, 1000001},
        new double[]{10000000, 10000001},
        new double[]{100000000, 100000001},
        new double[]{1000000000, 1000000001},
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
static final String escapeString(String arg) {
    return arg.replaceAll("&", "&amp;").replaceAll("\"", "&quot;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
}

static final String unescapeString(String arg) {
    return arg.replaceAll("&amp;", "&").replaceAll("&quot;", "\"").replaceAll("&lt;", "<").replaceAll("&gt;", ">");
}

/**
 * If we get something like 3.8.1.0_11, it becomes just "3.8.1".
 * If we get something like 4.0.0.0, it becomes just "4.0".
 */
static final String trimVersion(String ver) {
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

private static String getDisplayName(RuleBasedNumberFormat spellout, String currRuleName) {
    try {
        String prefix = currRuleName.substring(1, currRuleName.indexOf('-'));
        if (prefix.equals("spellout") || prefix.equals("digits") || prefix.equals("duration")) {
            String suffix = currRuleName.substring(currRuleName.indexOf('-') + 1);
            //return suffix + (spellout.getDefaultRuleSetName().equals(currRuleName) ? "<br/><em>Default</em>" : "");
            return suffix;
        }
    }
    catch (StringIndexOutOfBoundsException e) {
    }
    return currRuleName;
}

private static void printSkipLine(JspWriter out, RuleBasedNumberFormat rbnf) throws IOException {
    //System.out.println("<tr><td colspan=\"" + tableColumns + "\">...</td></tr>");
    out.print("<tr><td>...</td>");
    for (String name : rbnf.getRuleSetNames()) {
        out.print("<th class=\"thead\"><b>" + getDisplayName(rbnf, name) + "</b></th>");
    }
    out.println("</tr>");
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
        NumberFormat fmt = NumberFormat.getInstance(ULocale.US);
        fmt.setMaximumFractionDigits(9);
        fmt.setMinimumFractionDigits(1);
        fmt.setGroupingUsed(false);
        numStr = fmt.format(num);
    }
    else {
        numStr = new Long(longVal).toString();
    }
    return numStr;
}

private static void printLine(JspWriter out, RuleBasedNumberFormat rbnf, double num, boolean isRTL) throws IOException {
    String numStr = formatNumber(num);

    out.print("<tr><td>" + numStr + "</td>");
    for (String name : rbnf.getRuleSetNames()) {
        String result = rbnf.format(num, name);
        String errorMsg = "";
        if (PARSE_CHECK && !isUnparseable(name) && !Double.isNaN(num)) {
            // Even when it's an irrelevant value, we want to parse to make
            // sure that there is no exception being thrown.
            try {
                Number parseResult = rbnf.parse(result, new ParsePosition(0));
                if (parseResult.doubleValue() != num && !isIrrelevantValue(name, num)) {
                    errorMsg = "<br/><span style=\"color:red\">Error parsed as " + parseResult + "</span>";
                }
            }
            catch (Throwable e) {
                errorMsg = "<br/><span style=\"color:red\">Error parsing " + e + "</span>";
            }
        }
        out.print("<td" + getNumberStyle(name, num, isRTL) + ">" + result + errorMsg+ "</td>");
    }
    out.println("</tr>");
}

private static boolean isIrrelevantValue(String ruleName, double val) {
    return (val < 1 || val != (long)val) && (ruleName.contains("ordinal") || ruleName.contains("year"));
}

private static boolean isUnparseable(String ruleName) {
    return ruleName.contains("@noparse");
}

private static String getNumberStyle(String ruleName, double val, boolean isRTL) {
    String bidiStyle = "";
    if (isRTL) {
        bidiStyle = " rtl";
    }
    if (isIrrelevantValue(ruleName, val)) {
        return " class=\"nonsense"+bidiStyle+"\"";
    }
    if (ruleName.contains("@noparse")) {
        return " class=\"noparse"+bidiStyle+"\"";
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
    try {
        return Double.valueOf(str).doubleValue();
    }
    catch (NumberFormatException nfe) {
        if ("Inf".equals(str)) {
            return Double.POSITIVE_INFINITY;
        }
        else if ("-Inf".equals(str)) {
            return Double.NEGATIVE_INFINITY;
        }
        else if ("NaN".equals(str)) {
            return Double.NaN;
        }
        throw nfe;
    }
}

private static final Pattern NUMBER_PAIR = Pattern.compile("(-?[0-9,.]+|-?Inf|NaN)(?:-(-?[0-9,.]+))?");

private static double[][] getNumberRanges(String numbers) {
    String []numberLines = numbers.split("[;\\r\\n]");
    double [][]result = new double[numberLines.length][];
    int count = 0;
    for (String line : numberLines) {
        if (line.length() == 0) {
            continue;
        }
        double []range = new double[2];
        try {
            Matcher numberPair = NUMBER_PAIR.matcher(line);
            numberPair.find();
            range[0] = parseNumber(numberPair.group(1));
            if (numberPair.group(2) != null) {
                range[1] = parseNumber(numberPair.group(2));
            }
            else {
                range[1] = range[0];
            }
            result[count++] = range;
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
if (!isEdit || rulesStr == null || rulesStr.length() <= 0) {
    try {
        rulesStr = getRules(selectedLocale, type);
    }
    catch (Exception e) {
        if (errorMsg == null) {
            // The standard default rules can not be loaded? That is bad. Is ICU okay?
            errorMsg = e.getMessage();
            org.unicode.cldr.web.SurveyLog.logException(e);
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
double [][]ranges = getNumberRanges(numbers);
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
    var groupElem = middleElem;
    do {
        groupElem = groupElem.nextSibling;
    }
    while (groupElem.nodeType != 1); // Get to the next real node.
    var selectorElem = middleElem.firstChild;
    if (groupElem.style.display == 'block') {
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
<body style="padding: 1em;">
<h2>Number Format Tester</h2>

<form action='<%= request.getRequestURI() %>' method="post">
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
for (double[] pair: ranges) {
    if (pair == null) {
        continue;
    }
    if (numberLine.length() > 0) {
        numberLine.append("\n");
    }
    numberLine.append(formatNumber(pair[0]));
    if (pair[0] < pair[1]) {
        numberLine.append("-").append(formatNumber(pair[1]));
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
    out.print("<p style=\"color:red;\">"+errorMsg+"</p>");
}
else {
    int tableColumns = 0;
    try {
        int numColumns = rbnf.getRuleSetNames().length;
        tableColumns += numColumns;
        out.print("<table border=\"1\" cellspacing=\"0\" cellpadding=\"2\" class=\"results\">");
        out.print("<tr><th rowspan=\"2\" class=\"thead\"><b>Number</b></th>");
        out.print("<th colspan=\"" + numColumns + "\" style=\"text-align:center;\">"+getRulePrefix(rbnf.getDefaultRuleSetName()));
        out.print("<br/><span style=\"color: gray\">Default = " + getDisplayName(rbnf, rbnf.getDefaultRuleSetName()));
        out.println("</span></th>");
        out.println("</tr>");
        out.print("<tr>");
        for (String name : rbnf.getRuleSetNames()) {
            out.print("<th class=\"thead\"><b>" + getDisplayName(rbnf, name) + "</b></th>");
        }
        out.println("</tr>");
        byte direction = Character.getDirectionality(rbnf.format(1).charAt(0));
        boolean isRTL = (direction == Character.DIRECTIONALITY_RIGHT_TO_LEFT || direction == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC);
        int lineCount = 0;
        for (int rangeIdx = 0; rangeIdx < ranges.length; rangeIdx++) {
            if (ranges[rangeIdx] == null) {
                continue;
            }
            double end = ranges[rangeIdx][1];
            if (end == (long)end && rangeIdx != 0) {
                printSkipLine(out, rbnf);
            }
            double num = ranges[rangeIdx][0];
            if (Double.isInfinite(num) || Double.isNaN(num)) {
                printLine(out, rbnf, num, isRTL);
            }
            else {
                for (; num <= end; num++) {
                    if (lineCount++ >= MAX_LINE_COUNT) {
                        throw new Exception("Too many numbers to format.");
                    }
                    printLine(out, rbnf, num, isRTL);
                }
            }
        }
    }
    catch (Exception e) {
        out.print("<tr><td colspan=\"" + (tableColumns + 1) + "\" style=\"color:red; text-align: center\">"+e.getMessage()+"</td></tr>");
    }
    out.println("</table>");
}
%>
<hr />
        <div style="float: left;"><a href="http://www.unicode.org/">Unicode</a> | <a href="http://cldr.unicode.org/">CLDR</a>
        | <a href="http://unicode.org/cldr/trac/newticket?component=survey&amp;summary=Feedback+for+Number+Format+Tester+<%=type%>&amp;locale=<%=selectedLocale.toString()%>">Feedback or corrections to the displayed rules</a></div>
<div style="float: right; font-size: 60%;"><span class="notselected">Powered by
<a href="http://www.icu-project.org/">ICU</a> <%= trimVersion(VersionInfo.ICU_VERSION.toString()) %></span></div>
</body>
</html>
