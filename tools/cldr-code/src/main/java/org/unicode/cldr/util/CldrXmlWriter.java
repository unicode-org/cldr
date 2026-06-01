package org.unicode.cldr.util;

import com.google.common.base.Splitter;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Export CLDRFile objects as XML files */
public class CldrXmlWriter {

    private static final boolean WRITE_COMMENTS_THAT_NO_LONGER_HAVE_BASE = false;

    private final Set<String> orderedSet;
    private final Map<String, ?> options;
    private final PrintWriter pw;
    private final CLDRFile cldrFile;
    private final Set<String> identitySet;
    private final java.util.function.Predicate<String> skipTest;
    private final boolean isResolved;
    private final XPathParts.Comments tempComments;
    private final XMLSource xmlSource;

    private String firstFullPath = null;
    private XPathParts firstFullPathParts = null;
    private DtdType dtdType = DtdType.ldml; // default
    private boolean suppressInheritanceMarkers = false;
    private XPathParts last = null;

    public CldrXmlWriter(CLDRFile cldrFile, PrintWriter pw, Map<String, ?> options) {
        this.options = options;
        this.pw = pw;
        this.cldrFile = cldrFile;

        xmlSource = cldrFile.dataSource;
        orderedSet = new TreeSet<>(cldrFile.getComparator());
        cldrFile.fullIterable().forEach(orderedSet::add);
        if (orderedSet.size() > 0) { // May not have any elements.
            final String firstPath = orderedSet.iterator().next();
            firstFullPath = cldrFile.getFullXPath(firstPath);
            firstFullPathParts = XPathParts.getFrozenInstance(firstFullPath);
            dtdType = DtdType.valueOf(firstFullPathParts.getElement(0));
        }
        identitySet = new TreeSet<>(cldrFile.getComparator());
        isResolved = xmlSource.isResolving();
        tempComments = (XPathParts.Comments) xmlSource.getXpathComments().clone();
        skipTest = (java.util.function.Predicate<String>) options.get("SKIP_PATH");
    }

    public void write() {
        start();
        firstLoop();
        secondLoop();
        finish();
    }

    private void start() {
        pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        if (!options.containsKey("DTD_OMIT")) {
            // <!DOCTYPE ldml SYSTEM "../../common/dtd/ldml.dtd">
            // <!DOCTYPE supplementalData SYSTEM '../../common/dtd/ldmlSupplemental.dtd'>
            String fixedPath = "../../" + dtdType.dtdPath;
            if (options.containsKey("DTD_DIR")) {
                String dtdDir = options.get("DTD_DIR").toString();
                fixedPath = dtdDir + dtdType + ".dtd";
            }
            pw.println("<!DOCTYPE " + dtdType + " SYSTEM \"" + fixedPath + "\">");
        }
        if (options.containsKey("COMMENT")) {
            pw.println("<!-- " + options.get("COMMENT") + " -->");
        }
        if (options.containsKey("SUPPRESS_IM")) {
            suppressInheritanceMarkers = true;
        }
        if (!cldrFile.isNonInheriting()) {
            initializeIdentity();
        }
        final String initialComment =
                fixInitialComment(xmlSource.getXpathComments().getInitialComment());
        XPathParts.writeComment(pw, 0, initialComment, true);
    }

    private void initializeIdentity() {
        /*
         * <identity>
         * <version number="1.2"/>
         * <language type="en"/>
         */
        // if ldml has any attributes, get them.
        String ldml_identity = "//ldml/identity";
        if (firstFullPath != null) { // if we had a path
            if (firstFullPath.contains("/identity")) {
                ldml_identity = firstFullPathParts.toString(2);
            } else {
                ldml_identity = firstFullPathParts.toString(1) + "/identity";
            }
        }

        identitySet.add(ldml_identity + "/version[@number=\"$" + "Revision" + "$\"]");
        LocaleIDParser lip = new LocaleIDParser();
        lip.set(xmlSource.getLocaleID());
        identitySet.add(ldml_identity + "/language[@type=\"" + lip.getLanguage() + "\"]");
        if (lip.getScript().length() != 0) {
            identitySet.add(ldml_identity + "/script[@type=\"" + lip.getScript() + "\"]");
        }
        if (lip.getRegion().length() != 0) {
            identitySet.add(ldml_identity + "/territory[@type=\"" + lip.getRegion() + "\"]");
        }
        String[] variants = lip.getVariants();
        for (String variant : variants) {
            identitySet.add(ldml_identity + "/variant[@type=\"" + variant + "\"]");
        }
    }

    private void firstLoop() {
        /*
         * First loop: call writeDifference for each xpath in identitySet, with empty string "" for value.
         * There is no difference between "filtered" and "not filtered" in this loop.
         */
        for (String xpath : identitySet) {
            if (isResolved && xpath.contains("/alias")) {
                continue;
            }
            XPathParts current = XPathParts.getFrozenInstance(xpath).cloneAsThawed();
            current.writeDifference(pw, current, last, "", tempComments);
            last = current;
        }
    }

    private void secondLoop() {
        /*
         * call writeDifference for each xpath in orderedSet, with v = getStringValue(xpath).
         */
        for (String xpath : orderedSet) {
            if (skipTest != null && skipTest.test(xpath)) {
                continue;
            }
            if (isResolved && xpath.contains("/alias")) {
                continue;
            }
            String v = cldrFile.getStringValue(xpath);
            if (v == null) {
                continue;
            }
            if (suppressInheritanceMarkers && CldrUtility.INHERITANCE_MARKER.equals(v)) {
                continue;
            }
            /*
             * The difference between "filtered" (currentFiltered) and "not filtered" (current) is that
             * current uses getFullXPath(xpath), while currentFiltered uses xpath.
             */
            XPathParts currentFiltered = XPathParts.getFrozenInstance(xpath).cloneAsThawed();
            if (currentFiltered.size() >= 2 && currentFiltered.getElement(1).equals("identity")) {
                continue;
            }
            XPathParts current =
                    XPathParts.getFrozenInstance(cldrFile.getFullXPath(xpath)).cloneAsThawed();
            current.writeDifference(pw, currentFiltered, last, v, tempComments);
            last = current;
        }
        last.writeLast(pw);
    }

    private void finish() {
        String finalComment = xmlSource.getXpathComments().getFinalComment();

        if (WRITE_COMMENTS_THAT_NO_LONGER_HAVE_BASE) {
            // write comments that no longer have a base
            List<String> x = tempComments.extractCommentsWithoutBase();
            if (x.size() != 0) {
                String extras = "Comments without bases" + XPathParts.NEWLINE;
                for (String key : x) {
                    extras += XPathParts.NEWLINE + key;
                }
                finalComment += XPathParts.NEWLINE + extras;
            }
        }
        XPathParts.writeComment(pw, 0, finalComment, true);
    }

    private static final Splitter LINE_SPLITTER = Splitter.on('\n');

    private String fixInitialComment(String initialComment) {
        if (initialComment == null || initialComment.isEmpty()) {
            return CldrUtility.getCopyrightString();
        } else {
            boolean fe0fNote = false;
            StringBuilder sb =
                    new StringBuilder(CldrUtility.getCopyrightString()).append(XPathParts.NEWLINE);
            for (String line : LINE_SPLITTER.split(initialComment)) {
                if (line.startsWith("Warnings: All cp values have U+FE0F characters removed.")) {
                    fe0fNote = true;
                    continue;
                }
                if (line.contains("Copyright")
                        || line.contains("©")
                        || line.contains("trademark")
                        || line.startsWith("CLDR data files are interpreted")
                        || line.startsWith("SPDX-License-Identifier")
                        || line.startsWith("For terms of use")
                        || line.startsWith("according to the LDML specification")
                        || line.startsWith(
                                "terms of use, see http://www.unicode.org/copyright.html")) {
                    continue;
                }
                sb.append(XPathParts.NEWLINE).append(line);
            }
            if (fe0fNote) {
                // Keep this on a separate line.
                sb.append(XPathParts.NEWLINE);
                sb.append(
                        "Warnings: All cp values have U+FE0F characters removed. See /annotationsDerived/ for derived annotations.");
                sb.append(XPathParts.NEWLINE);
            }
            return sb.toString();
        }
    }
}
