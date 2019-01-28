// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package org.unicode.cldr.icu;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Collection;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.ant.CLDRConverterTool.Alias;

class DependencyGraphData {
    SupplementalDataInfo supplementalDataInfo;
    Collection<Alias> aliasList;

    DependencyGraphData(SupplementalDataInfo supplementalDataInfo, Collection<Alias> aliasList) {
        this.supplementalDataInfo = supplementalDataInfo;
        this.aliasList = aliasList;
    }

    public void print(String outputDir, String filename) throws IOException {
        PrintWriter out = FileUtilities.openUTF8Writer(outputDir, filename);
        out.append("# -*- coding: utf-8 -*-\n");
        out.append("# © 2019 and later: Unicode, Inc. and others.\n");
        out.append("# License & terms of use: http://www.unicode.org/copyright.html#License\n");
        out.println();

        out.append("data = {");
        boolean firstOuter = true;
        if (aliasList != null) {
            if (!firstOuter) {
                out.append(',');
            }
            firstOuter = false;
            out.append("\n    \"aliases\": {");
            boolean firstInner = true;
            for (Alias alias : aliasList) {
                if (alias.rbPath != null) {
                    continue;
                }
                if (!firstInner) {
                    out.append(',');
                }
                firstInner = false;
                out.append("\n        \"");
                out.append(alias.from);
                out.append("\": \"");
                out.append(alias.to);
                out.append('"');
            }
            out.append("\n    }");
        }
        Collection<String> explicitChildren = supplementalDataInfo.getExplicitChildren();
        if (!explicitChildren.isEmpty()) {
            if (!firstOuter) {
                out.append(',');
            }
            firstOuter = false;
            out.append("\n    \"parents\": {");
            boolean firstInner = true;
            for (String child : explicitChildren) {
                if (!firstInner) {
                    out.append(',');
                }
                firstInner = false;
                String parent = supplementalDataInfo.getExplicitParentLocale(child);
                out.append("\n        \"");
                out.append(child);
                out.append("\": \"");
                out.append(parent);
                out.append('"');
            }
            out.append("\n    }");
        }
        out.append("\n}\n");
        out.close();
    }
}
