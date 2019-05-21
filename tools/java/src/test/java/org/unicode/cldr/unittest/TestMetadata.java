package org.unicode.cldr.unittest;

import java.util.Collection;
import java.util.Iterator;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Differ;

import com.ibm.icu.dev.test.TestFmwk;

public class TestMetadata extends TestFmwk {
    static CLDRConfig testInfo = CLDRConfig.getInstance();

    public static void main(String[] args) {
        new TestMetadata().run(args);
    }

    // disable, since we no longer have 3 different sources.
//    public void oldTestOrdering() {
//        logln("Make sure that all and only blocking elements are serialElements.");
//
//        // Then Serial Order
//        Set<String> cldrFileSerialElements = new TreeSet<String>(
//            TestDtdData.getSerialElements());
//        Set<String> metadataSerialElements = new TreeSet<String>(testInfo
//            .getSupplementalDataInfo().getSerialElements());
//        checkEquals("Serial Order", "CLDRFile.orderedElements",
//            metadataSerialElements, "cldrFile", cldrFileSerialElements);
//    }

    private void checkEquals(String title, String firstTitle,
        Collection<String> cldrFileOrder, String secondTitle,
        Collection<String> dtdAttributeOrder) {
        if (!cldrFileOrder.equals(dtdAttributeOrder)) {
            errln(title + " differ:" + CldrUtility.LINE_SEPARATOR + firstTitle
                + ":" + CldrUtility.LINE_SEPARATOR + "\t" + cldrFileOrder
                + CldrUtility.LINE_SEPARATOR + secondTitle + ":"
                + CldrUtility.LINE_SEPARATOR + "\t" + dtdAttributeOrder
                + CldrUtility.LINE_SEPARATOR
                + "To fix, replace contents of " + firstTitle + " with"
                + CldrUtility.LINE_SEPARATOR + "\t"
                + CldrUtility.join(dtdAttributeOrder, " ")
                + CldrUtility.LINE_SEPARATOR + "Differences:");
            Differ<String> differ = new Differ<String>(200, 1);
            Iterator<String> oldIt = cldrFileOrder.iterator();
            Iterator<String> newIt = dtdAttributeOrder.iterator();
            while (oldIt.hasNext() || newIt.hasNext()) {
                if (oldIt.hasNext())
                    differ.addA(oldIt.next());
                if (newIt.hasNext())
                    differ.addB(newIt.next());
                differ.checkMatch(!oldIt.hasNext() && !newIt.hasNext());

                if (differ.getACount() != 0 || differ.getBCount() != 0) {
                    final Object start = differ.getA(-1);
                    if (start.toString().length() != 0) {
                        errln("..." + CldrUtility.LINE_SEPARATOR + "\tSame: "
                            + start);
                    }
                    for (int i = 0; i < differ.getACount(); ++i) {
                        errln("\t" + firstTitle + ": " + differ.getA(i));
                    }
                    for (int i = 0; i < differ.getBCount(); ++i) {
                        errln("\t" + secondTitle + ": " + differ.getB(i));
                    }
                    final Object end = differ.getA(differ.getACount());
                    if (end.toString().length() != 0) {
                        errln("Same: " + end + CldrUtility.LINE_SEPARATOR
                            + "\t...");
                    }
                }
            }
            errln("Done with differences");

        }
    }
}
