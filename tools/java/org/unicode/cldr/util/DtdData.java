package org.unicode.cldr.util;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRFile.DtdType;

import com.ibm.icu.dev.util.Relation;

public class DtdData extends XMLFileReader.SimpleHandler  {
    static final boolean SHOW_PROGRESS = CldrUtility.getProperty("verbose", false);
    static final boolean SHOW_ALL = CldrUtility.getProperty("show_all", false);
    static final boolean DEBUG = true;
    static final Pattern FILLER = Pattern.compile("[^-a-zA-Z0-1]");

    final Map<String, Element> nameToElement = new HashMap<String, Element>();
    final Relation<String,Attribute> nameToAttributes = Relation.of(new TreeMap<String,Set<Attribute>>(), LinkedHashSet.class);

    private Element ROOT;
    private Element PCDATA;
    private Element ANY;
    private DtdType dtdType;

    enum Mode {
        required("#REQUIRED"), 
        optional("#IMPLIED"),
        fixed("#FIXED"),
        nil("null")
        ;
        final String source;
        Mode(String s) {
            source = s;
        }
        public static Mode forString(String mode) {
            for (Mode value : Mode.values()) {
                if (value.source.equals(mode)) {
                    return value;
                }
            }
            if (mode == null) {
                return nil;
            }
            throw new IllegalArgumentException(mode);
        }
    }

    public static class Attribute {
        public final String name;
        public final Element element;
        public final Mode mode;
        public final String value;
        public final Set<String> values;

        private Attribute(Element element2, String aName, Mode mode2, String[] split, String value2) {
            element = element2;
            name = aName;
            mode = mode2;
            value = value2;
            LinkedHashSet<String> temp = new LinkedHashSet<String>();
            for (String part : split) {
                if (part.length() != 0) {
                    temp.add(part);
                }
            }
            values = Collections.unmodifiableSet(temp);
        }
        @Override
        public String toString() {
            return element.name + ":" + name;
        }
        public String features() {
            return values + (mode == Mode.nil ? "" : ", mode=" + mode) + (value == null ? "" : ", default=" + value);
        }
    }

    private DtdData(DtdType type) {
        this.dtdType = type;
    }

    private void addAttribute(String eName, String aName, String type, String mode, String value) {
        Attribute a = new Attribute(nameToElement.get(eName), aName, Mode.forString(mode), FILLER.split(type), value);
        nameToAttributes.put(aName, a);
        a.element.attributes.add(a);
    }

    public class Element {
        public final String name;
        final Set<Element> children = new LinkedHashSet<Element>();
        final Set<Attribute> attributes = new LinkedHashSet<Attribute>();

        private Element(String name2) {
            name = name2;
        }
        private void setChildren(String model) {
            if (model.equals("EMPTY")) {
                return;
            }
            for (String part : FILLER.split(model)) {
                if (part.length() != 0) {
                    children.add(elementFrom(part));
                }
            }
        }
        public boolean containsAttribute(String string) {
            for (Attribute a : attributes) {
                if (a.name.equals(string)) {
                    return true;
                }
            }
            return false;
        }
        @Override
        public String toString() {
            return name;
        }
    }

    private Element elementFrom(String name) {
        Element result = nameToElement.get(name);
        if (result == null) {
            nameToElement.put(name, result = new Element(name));
            if (ROOT == null) {
                ROOT = result;
            }
        }
        return result;
    }

    private void addElement(String name2, String model) {
        Element element = elementFrom(name2);
        element.setChildren(model);
    }

    // TODO hide this
    @Override
    public void handleElementDecl(String name, String model) {
        if (SHOW_ALL) {
            System.out.println("element: " + name + ", model: " + model);
        }
        addElement(name, model);
    }

    // TODO hide this
    public void handleAttributeDecl(String eName, String aName, String type, String mode, String value) {
        if (SHOW_ALL) {System.out.println("eName: " + eName
                + ", attribute: " + aName
                + ", type: " + type
                + ", mode: " + mode
                + ", value: " + value
                );
        }
        addAttribute(eName, aName, type, mode, value);
    }

    // TODO hide this
    @Override
    public void handleEndDtd() {
        throw new XMLFileReader.AbortException();
    }

    private static class Walker {
        HashSet<Element> seen = new HashSet<Element>();
        Set<Element> elementsMissingDraft = new LinkedHashSet<Element>();
        Set<Element> elementsMissingAlt = new LinkedHashSet<Element>();
        static final Set<String> SKIP_ATTRIBUTES = new HashSet<String>(Arrays.asList(
                "draft", "alt", "standard", "references"));
        Set<Attribute> attributesWithValues = new LinkedHashSet<Attribute>();

        private DtdData dtdData;

        public Walker(DtdData dtdData) {
            this.dtdData = dtdData;
        }
        private void show(Element element) {
            show(element, "");
            System.out.println();
            if (dtdData.dtdType == DtdType.ldml && elementsMissingDraft.size() != 0) {
                System.out.println("*Elements missing draft:\t" + elementsMissingDraft);
                System.out.println();
            }
            if (dtdData.dtdType == DtdType.ldml && elementsMissingAlt.size() != 0) {
                System.out.println("*Elements missing alt:\t" + elementsMissingAlt);
                System.out.println();
            }
            if (attributesWithValues.size() != 0) {
                System.out.println("*Attributes with default values:");
                for (Attribute a : attributesWithValues) {
                    System.out.println("\t" + a + "\t" + a.features());
                }
                System.out.println();
            }
            StringBuilder diff = new StringBuilder();
            for (Entry<String, Set<Attribute>> entry : dtdData.nameToAttributes.keyValuesSet()) {
                Relation<String,String> featuresToElements = Relation.of(new TreeMap<String,Set<String>>(), LinkedHashSet.class);
                for (Attribute a : entry.getValue()) {
                    featuresToElements.put(a.features(), a.element.name);
                }
                if (featuresToElements.size() != 1) {
                    diff.append("\t" + entry.getKey() + "\n");
                    for (Entry<String, Set<String>> entry2 : featuresToElements.keyValuesSet()) {
                        diff.append("\t\t" + entry2.getKey() + "\n");
                        diff.append("\t\t\t on " + entry2.getValue() + "\n");
                    }
                }
            }
            if (diff.length() != 0) {
                System.out.println("*Attributes with different features by element:");
                System.out.println(diff);
                System.out.println();
            }
        }
        private void show(Element element, String indent) {
            if (seen.contains(element)) {
                System.out.println(indent + element.name + "*");
            } else {
                seen.add(element);
                if (!element.containsAttribute("draft")) {
                    elementsMissingDraft.add(element);
                }
                if (!element.containsAttribute("alt")) {
                    elementsMissingAlt.add(element);
                }
                if (element.children.size() == 0) {
                    System.out.println(indent + element.name + "\tNO-KIDS");
                } else if (element.children.contains(dtdData.PCDATA) || element.children.contains(dtdData.ANY)) {
                    System.out.println(indent + element.name + "\t" + element.children);
                } else {
                    System.out.println(indent + element.name);
                }

                indent += "\t";
                for (Attribute a : element.attributes) {
                    if (a.value != null) {
                        attributesWithValues.add(a);
                    }
                    if (SKIP_ATTRIBUTES.contains(a.name)) {
                        continue;
                    }
                    System.out.println(indent + "@" + a.name + "\t" + a.features());
                }
                for (Element e : element.children) {
                    if (e != dtdData.PCDATA && e != dtdData.ANY) {
                        show(e, indent);
                    }
                }
            }
        }
    }

    private void finish() {
        PCDATA = elementFrom("PCDATA");
        ANY = elementFrom("ANY");
    }

    static final Map<CLDRFile.DtdType, String> DTD_TYPE_TO_FILE;
    static {
        EnumMap<CLDRFile.DtdType, String> temp = new EnumMap<CLDRFile.DtdType, String>(CLDRFile.DtdType.class);
        temp.put(CLDRFile.DtdType.ldml, CldrUtility.COMMON_DIRECTORY + "dtd/ldml.dtd");
        temp.put(CLDRFile.DtdType.supplementalData, CldrUtility.COMMON_DIRECTORY + "dtd/ldmlSupplemental.dtd");
        temp.put(CLDRFile.DtdType.ldmlBCP47, CldrUtility.COMMON_DIRECTORY + "dtd/ldmlSupplemental.dtd");
        temp.put(CLDRFile.DtdType.keyboard, CldrUtility.BASE_DIRECTORY + "keyboards/dtd/ldmlKeyboard.dtd");
        temp.put(CLDRFile.DtdType.platform, CldrUtility.BASE_DIRECTORY + "keyboards/dtd/ldmlPlatform.dtd");
        DTD_TYPE_TO_FILE = Collections.unmodifiableMap(temp);
    }

    public static DtdData make(CLDRFile.DtdType type) {
        DtdData simpleHandler = new DtdData(type);
        XMLFileReader xfr = new XMLFileReader().setHandler(simpleHandler);
        StringReader s = new StringReader("<?xml version='1.0' encoding='UTF-8' ?>"
                + "<!DOCTYPE ldml SYSTEM '" + DTD_TYPE_TO_FILE.get(type) + "'>");
        xfr.read(type.toString(), s, -1, true); //  DTD_TYPE_TO_FILE.get(type)
        simpleHandler.finish();
        return simpleHandler;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            Set<DtdType> args2 = DTD_TYPE_TO_FILE.keySet();
            args = new String[args2.size()];
            int i = 0;
            for (DtdType arg : args2) {
                args[i++] = arg.name();
            }
        }
        long start = System.currentTimeMillis();
        for (String arg : args) {
            DtdData dtdData = make(CLDRFile.DtdType.valueOf(arg));
            new Walker(dtdData).show(dtdData.ROOT);
        }
        long end = System.currentTimeMillis();
        System.out.println("Millis: " + (end - start));
    }
}
