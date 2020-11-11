package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;

import org.unicode.cldr.util.DtdData.Attribute;
import org.unicode.cldr.util.DtdData.AttributeStatus;
import org.unicode.cldr.util.DtdData.Element;
import org.unicode.cldr.util.DtdData.Mode;
import org.unicode.cldr.util.PathHeader.Factory;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;

/**
 * Walk through all the possible paths in a DTDData
 * @author markdavis
 *
 */
public class DtdPathIterator {
    public DtdPathIterator(DtdData dtdData) {
        super();
        this.dtdData = dtdData;
        this.xpathParts = new XPathParts();
    }

    final DtdData dtdData;
    final XPathParts xpathParts;

    /**
     * Visit the generated parts. Warning: the function must not modify the XPathParts.
     * @param function
     */
    public void visit(Consumer<XPathParts> function, Function<Attribute, String> sample) {
        xpathParts.clear();
        visit(dtdData.ROOT, function, sample);
    }

    private void visit(Element parent, Consumer<XPathParts> visitor, Function<Attribute, String> sample) {
        if (parent.isDeprecated()) {
            return;
        }
        // add element and all its possible attributes
        final String parentName = parent.getName();
        if (parentName.contentEquals("exemplarCharacters")) {
            int debug = 0;
        }
        if (parentName.equals("alias") || parentName.equals("identity") || parentName.equals("special")) {
            return;
        }
        Set<Element> children = parent.getChildren().keySet();
        // filter children to non-deprecated
        if (!children.isEmpty()) {
            Set<Element> temp = new LinkedHashSet<>();
            for (Element e : children) {
                if (!e.isDeprecated()) {
                    temp.add(e);
                }
            }
            children = ImmutableSet.copyOf(temp);
        }
        int index = xpathParts.size();
        xpathParts.addElement(parentName);

        // get possible attributes
        List<Attribute> optionalAttributes = new ArrayList<>();
        for (Attribute attribute : parent.getAttributes().keySet()) {
            if (attribute.isDeprecated()
                || attribute.getStatus() != AttributeStatus.distinguished) {
                continue;
            }
            String attributeName = attribute.getName();
            if (!attributeName.equals("alt")) {
                if (attribute.mode == Mode.OPTIONAL) {
                    if (parentName.equals("displayName") && attributeName.equals("count") && !xpathParts.contains("currency")) {
                        // skip
                    } else {
                        optionalAttributes.add(attribute);
                    }
                } else {
                    xpathParts.setAttribute(index, attributeName, sample.apply(attribute));
                }
            }
        }
        if (!optionalAttributes.isEmpty()) {
            int comboMax = (1 << optionalAttributes.size());
            for (int bitmask = comboMax - 1; bitmask >= 0; --bitmask) {
                // for two items, go from 0b11, 0b10, 0b01, 0b00; for 1: 0b1, 0b0
                for (int bit = 0; bit < optionalAttributes.size(); ++bit) {
                    final Attribute attribute = optionalAttributes.get(bit);
                    String attributeName = attribute.getName();
                    if ((bitmask & (1 << bit)) == 0) {
                        xpathParts.setAttribute(index, attributeName, sample.apply(attribute));
                    } else {
                        xpathParts.removeAttribute(index, attributeName);
                    }
                    if (children.isEmpty()) {
                        // check the path.
                        visitor.accept(xpathParts);
                    } else {
                        for (Element child : children) {
                            visit(child, visitor, sample);
                        }
                    }
                }
            }
        } else {
            if (children.isEmpty()) {
                // check the path.
                visitor.accept(xpathParts);
            } else {
                for (Element child : children) {
                    visit(child, visitor, sample);
                }
            }
        }
        // cleanup
        // remove the element we added
        xpathParts.removeElement(index);
    }

    public static void main(String[] args) {
        Set<XPathParts>seen = new HashSet<>();
        Set<PathHeader>seenPh = new HashSet<>();
        DtdPathIterator dtdPathIterator = new DtdPathIterator(DtdData.getInstance(DtdType.ldml));
        Factory phf = PathHeader.getFactory();
        List<String> failures = new ArrayList<>();
        org.unicode.cldr.util.Factory factory = CLDRConfig.getInstance().getCommonAndSeedAndMainAndAnnotationsFactory();

        // get all the actual starred patterns

        PathStarrer ps = new PathStarrer().setSubstitutionPattern("%A");
        Map<String, String> starredToSample = new TreeMap<>();
        for (String locale : Arrays.asList("en", "de", "zh", "ar", "ru")) {
            CLDRFile cfile = factory.make(locale, true);
            for (String path : cfile.fullIterable()) {
                String starred = ps.set(path);
                starred = starred.replace("[@alt=\"%A\"]","");
                if (!starredToSample.containsKey(starred) && !starred.endsWith("/alias") && !starred.startsWith("//ldml/identity/")) {
                    starredToSample.put(starred, path);
                }
            }
        }
        Set<String> starredUnseen = new TreeSet<>(starredToSample.keySet());

        dtdPathIterator.visit(x -> {
            if (seen.contains(x)) {
                int debug = 0;
            } else {
                failures.clear();
                final String xString = x.toString();
//                PathHeader ph = null;
//                try {
//                    ph = phf.fromPath(xString, failures);
//                    if (seenPh.contains(ph)) {
//                        failures.add("NON_UNIQUE");
//                    } else {
//                        seenPh.add(ph);
//                        if (ph.getPageId() == PageId.Deprecated) {
//                            return;
//                        }
//                    }
//                } catch (Exception e) {
//                    failures.add(e.getMessage());
//                }
                final String sample = starredToSample.get(xString);
                starredUnseen.remove(xString);
                System.out.println(seen.size() + "\t" + x + "\t" + failures + "\t" + sample);
                seen.add(x.cloneAsThawed().freeze());
                if ((seen.size() % 25) == 0) {
                    int debug = 0;
                }
            }
        },
//            y -> y.getSampleValue()
            y -> "%A"
            );
        if (!starredUnseen.isEmpty()) {
            System.out.println("ERROR: In files, not dtd");
            System.out.println(Joiner.on("\n\t").join(starredUnseen));
        }
    }
}
