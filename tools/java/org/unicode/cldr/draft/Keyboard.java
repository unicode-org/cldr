package org.unicode.cldr.draft;

import java.io.File;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.LanguageTagParser.Status;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XMLFileReader.SimpleHandler;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.text.UnicodeSet;

/**
 * A first, very rough cut at reading the keyboard data.
 * Every public structure is immutable, eg all returned maps, sets.
 * 
 * @author markdavis
 */
public class Keyboard {

    private static final boolean DEBUG = false;

    private static final String BASE = CLDRPaths.BASE_DIRECTORY + "keyboards/";

    public enum IsoRow {
        E, D, C, B, A;
    }

    public enum Iso {
        E00, E01, E02, E03, E04, E05, E06, E07, E08, E09, E10, E11, E12, E13, D00, D01, D02, D03, D04, D05, D06, D07, D08, D09, D10, D11, D12, D13, C00, C01, C02, C03, C04, C05, C06, C07, C08, C09, C10, C11, C12, C13, B00, B01, B02, B03, B04, B05, B06, B07, B08, B09, B10, B11, B12, B13, A00, A01, A02, A03, A04, A05, A06, A07, A08, A09, A10, A11, A12, A13;
        public final IsoRow isoRow;

        Iso() {
            isoRow = IsoRow.valueOf(name().substring(0, 1));
        }
    }

    // add whatever is needed

    public enum Modifier {
        cmd, ctrlL, ctrlR, caps, altL, altR, optL, optR, shiftL, shiftR;
    }

    // public static class ModifierSet {
    // private String temp; // later on expand into something we can use.
    // @Override
    // public String toString() {
    // return temp;
    // }
    // @Override
    // public boolean equals(Object obj) {
    // final ModifierSet other = (ModifierSet)obj;
    // return temp.equals(other.temp);
    // }
    // @Override
    // public int hashCode() {
    // return temp.hashCode();
    // };
    //
    // /**
    // * Parses string like "AltCapsCommand? RShiftCtrl" and returns a set of modifier sets, like:
    // * {{RAlt, LAlt, Caps}, {RAlt, LAlt, Caps, Command}, {RShift, LCtrl, RCtrl}}
    // */
    // public static Set<ModifierSet> parseSet(String input) {
    // //ctrl+opt?+caps?+shift? ctrl+cmd?+opt?+shift? ctrl+cmd?+opt?+caps? cmd+ctrl+caps+shift+optL? ...
    // Set<ModifierSet> results = new HashSet<ModifierSet>(); // later, Treeset
    // if (input != null) {
    // for (String ms : input.trim().split(" ")) {
    // ModifierSet temp = new ModifierSet();
    // temp.temp = ms;
    // results.add(temp);
    // }
    // }
    // return results;
    // // Set<ModifierSet> current = new LinkedHashSet();EnumSet.noneOf(Modifier.class);
    // // for (String mod : input.trim().split("\\+")) {
    // // boolean optional = mod.endsWith("?");
    // // if (optional) {
    // // mod = mod.substring(0,mod.length()-1);
    // // }
    // // Modifier m = Modifier.valueOf(mod);
    // // if (optional) {
    // // temp = EnumSet.copyOf(current);
    // // } else {
    // // for (Modifier m2 : current) {
    // // m2.a
    // // }
    // // }
    // // }
    // }
    // /**
    // * Format a set of modifier sets like {{RAlt, LAlt, Caps}, {RAlt, LAlt, Caps, Command}, {RShift, LCtrl, RCtrl}}
    // * and return a string like "AltCapsCommand? RShiftCtrl". The exact compaction may vary.
    // */
    // public static String formatSet(Set<ModifierSet> input) {
    // return input.toString();
    // }
    // }

    public static Set<String> getPlatformIDs() {
        Set<String> results = new LinkedHashSet<String>();
        File file = new File(BASE);
        for (String f : file.list())
            if (!f.equals("dtd") && !f.startsWith(".") && !f.startsWith("_")) {
                results.add(f);
            }
        return results;
    }

    public static Set<String> getKeyboardIDs(String platformId) {
        Set<String> results = new LinkedHashSet<String>();
        File base = new File(BASE + platformId + "/");
        for (String f : base.list())
            if (f.endsWith(".xml") && !f.startsWith(".") && !f.startsWith("_")) {
                results.add(f.substring(0, f.length() - 4));
            }
        return results;
    }

    public static Platform getPlatform(String platformId) {
        final String fileName = BASE + platformId + "/_platform.xml";
        try {
            final PlatformHandler platformHandler = new PlatformHandler();
            new XMLFileReader()
                .setHandler(platformHandler)
                .read(fileName, -1, true);
            return platformHandler.getPlatform();
        } catch (Exception e) {
            throw new KeyboardException(fileName, e);
        }
    }

    public Keyboard(String locale, String version, String platformVersion, Set<String> names,
        Fallback fallback, Set<KeyMap> keyMaps, Map<TransformType, Transforms> transforms) {
        this.locale = locale;
        this.version = version;
        this.platformVersion = platformVersion;
        this.fallback = fallback;
        this.names = Collections.unmodifiableSet(names);
        this.keyMaps = Collections.unmodifiableSet(keyMaps);
        this.transforms = Collections.unmodifiableMap(transforms);
    }

//    public static Keyboard getKeyboard(String keyboardId, Set<Exception> errors) {
//        int pos = keyboardId.indexOf("-t-k0-") + 6;
//        int pos2 = keyboardId.indexOf('-', pos);
//        if (pos2 < 0) {
//            pos2 = keyboardId.length();
//        }
//        return getKeyboard(keyboardId.substring(pos, pos2), keyboardId, errors);
//    }

    public static String getPlatformId(String keyboardId) {
        int pos = keyboardId.indexOf("-t-k0-") + 6;
        int pos2 = keyboardId.indexOf('-', pos);
        if (pos2 < 0) {
            pos2 = keyboardId.length();
        }
        return keyboardId.substring(pos, pos2);
    }

    public static Keyboard getKeyboard(String platformId, String keyboardId, Set<Exception> errors) {
        final String fileName = BASE + platformId + "/" + keyboardId + ".xml";
        try {
            final KeyboardHandler keyboardHandler = new KeyboardHandler(errors);
            new XMLFileReader()
                .setHandler(keyboardHandler)
                .read(fileName, -1, true);
            return keyboardHandler.getKeyboard();
        } catch (Exception e) {
            throw new KeyboardException(fileName + "\n" + CollectionUtilities.join(errors, ", "), e);
        }
    }

    public static Keyboard getKeyboard(String id, Reader r, Set<Exception> errors) {
        //final String fileName = BASE + platformId + "/" + keyboardId + ".xml";
        try {
            final KeyboardHandler keyboardHandler = new KeyboardHandler(errors);
            new XMLFileReader()
                .setHandler(keyboardHandler)
                .read(id, r, -1, true);
            return keyboardHandler.getKeyboard();
        } catch (Exception e) {
            errors.add(e);
            return null;
        }
    }

    public static class Platform {
        final String id;
        final Map<String, Iso> hardwareMap;

        public String getId() {
            return id;
        }

        public Map<String, Iso> getHardwareMap() {
            return hardwareMap;
        }

        public Platform(String id, Map<String, Iso> hardwareMap) {
            super();
            this.id = id;
            this.hardwareMap = Collections.unmodifiableMap(hardwareMap);
        }
    }

    public enum Gesture {
        LONGPRESS;
        public static Gesture fromString(String string) {
            return Gesture.valueOf(string.toUpperCase(Locale.ENGLISH));
        }
    }

    public enum TransformStatus {
        DEFAULT, NO;
        public static TransformStatus fromString(String string) {
            return string == null ? TransformStatus.DEFAULT : TransformStatus.valueOf(string
                .toUpperCase(Locale.ENGLISH));
        }
    }

    public enum TransformType {
        SIMPLE;
        public static TransformType forString(String string) {
            return string == null ? TransformType.SIMPLE : TransformType.valueOf(string.toUpperCase(Locale.ENGLISH));
        }
    }

    public static class Output {
        final String output;
        final TransformStatus transformStatus;
        final Map<Gesture, List<String>> gestures;

        public Output(String output, Map<Gesture, List<String>> gestures, TransformStatus transformStatus) {
            this.output = output;
            this.transformStatus = transformStatus;
            this.gestures = Collections.unmodifiableMap(gestures); // TODO make lists unmodifiable
        }

        public String getOutput() {
            return output;
        }

        public TransformStatus getTransformStatus() {
            return transformStatus;
        }

        public Map<Gesture, List<String>> getGestures() {
            return gestures;
        }

        public String toString() {
            return "{" + output + "," + transformStatus + ", " + gestures + "}";
        }
    }

    public static class KeyMap {
        private final KeyboardModifierSet modifiers;
        final Map<Iso, Output> iso2output;

        public KeyMap(KeyboardModifierSet keyMapModifiers, Map<Iso, Output> data) {
            this.modifiers = keyMapModifiers;
            this.iso2output = Collections.unmodifiableMap(data);
        }

        public KeyboardModifierSet getModifiers() {
            return modifiers;
        }

        public Map<Iso, Output> getIso2Output() {
            return iso2output;
        }

        public String toString() {
            return "{" + modifiers + "," + iso2output + "}";
        }
    }

    public static class Transforms {
        final Map<String, String> string2string;

        public Transforms(Map<String, String> data) {
            this.string2string = data;
        }

        public Map<String, String> getMatch(String prefix) {
            Map<String, String> results = new LinkedHashMap<String, String>();
            for (Entry<String, String> entry : string2string.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith(prefix)) {
                    results.put(key.substring(prefix.length()), entry.getValue());
                }
            }
            return results;
        }
    }

    private final String locale;
    private final String version;
    private final String platformVersion;
    private final Fallback fallback;
    private final Set<String> names;
    private final Set<KeyMap> keyMaps;
    private final Map<TransformType, Transforms> transforms;

    public String getLocaleId() {
        return locale;
    }

    public String getVersion() {
        return version;
    }

    public String getPlatformVersion() {
        return platformVersion;
    }

    public Fallback getFallback() {
        return fallback;
    }

    public Set<String> getNames() {
        return names;
    }

    public Set<KeyMap> getKeyMaps() {
        return keyMaps;
    }

    public Map<TransformType, Transforms> getTransforms() {
        return transforms;
    }

    /**
     * Return all possible results. Could be external utility. WARNING: doesn't account for transform='no' or
     * failure='omit'.
     */
    public UnicodeSet getPossibleResults() {
        UnicodeSet results = new UnicodeSet();
        for (KeyMap keymap : getKeyMaps()) {
            addOutput(keymap.iso2output.values(), results);
        }
        for (Transforms transforms : getTransforms().values()) {
            // loop, to catch empty case
            for (String result : transforms.string2string.values()) {
                if (!result.isEmpty()) {
                    results.add(result);
                }
            }
        }
        return results;
    }

    private void addOutput(Collection<Output> values, UnicodeSet results) {
        for (Output value : values) {
            if (value.output != null && !value.output.isEmpty()) {
                results.add(value.output);
            }
            for (List<String> outputList : value.gestures.values()) {
                results.addAll(outputList);
            }
        }
    }

    private static class PlatformHandler extends SimpleHandler {
        String id;
        Map<String, Iso> hardwareMap = new HashMap<String, Iso>();
        XPathParts parts = new XPathParts();

        public void handlePathValue(String path, String value) {
            parts.set(path);
            // <platform id='android'/>
            id = parts.getAttributeValue(0, "id");
            if (parts.size() > 1) {
                String element1 = parts.getElement(1);
                // <platform> <hardwareMap> <map keycode='0' iso='C01'/>
                if (element1.equals("hardwareMap")) {
                    hardwareMap.put(parts.getAttributeValue(2, "keycode"),
                        Iso.valueOf(parts.getAttributeValue(2, "iso")));
                }
            }
        };

        public Platform getPlatform() {
            return new Platform(id, hardwareMap);
        }
    }

    public enum Fallback {
        BASE, OMIT;
        public static Fallback forString(String string) {
            return string == null ? Fallback.BASE : Fallback.valueOf(string.toUpperCase(Locale.ENGLISH));
        }
    }

    private static class KeyboardHandler extends SimpleHandler {
        Set<Exception> errors; //  = new LinkedHashSet<Exception>();
        Set<String> errors2 = new LinkedHashSet<String>();
        // doesn't do any error checking for collisions, etc. yet.
        String locale; // TODO
        String version; // TODO
        String platformVersion; // TODO

        Set<String> names = new LinkedHashSet<String>();
        Fallback fallback = Fallback.BASE;

        KeyboardModifierSet keyMapModifiers = null;
        Map<Iso, Output> iso2output = new EnumMap<Iso, Output>(Iso.class);
        Set<KeyMap> keyMaps = new LinkedHashSet<KeyMap>();

        TransformType currentType = null;
        Map<String, String> currentTransforms = null;
        Map<TransformType, Transforms> transformMap = new EnumMap<TransformType, Transforms>(TransformType.class);

        XPathParts parts = new XPathParts();
        LanguageTagParser ltp = new LanguageTagParser();

        public KeyboardHandler(Set<Exception> errorsOutput) {
            errors = errorsOutput;
            errors.clear();
        }

        public Keyboard getKeyboard() {
            // finish everything off
            addToKeyMaps();
            if (currentType != null) {
                transformMap.put(currentType, new Transforms(currentTransforms));
            }
//            errors.clear();
//            errors.addAll(this.errors);
            return new Keyboard(locale, version, platformVersion, names, fallback, keyMaps, transformMap);
        }

        public void handlePathValue(String path, String value) {
            // System.out.println(path);
            try {
                parts.set(path);
                if (locale == null) {
                    // <keyboard locale='bg-t-k0-chromeos-phonetic'>
                    locale = parts.getAttributeValue(0, "locale");
                    ltp.set(locale);
                    Map<String, String> extensions = ltp.getExtensions();
                    LanguageTagParser.Status status = ltp.getStatus(errors2);
                    if (errors2.size() != 0 || !ltp.hasT()) {
                        errors.add(new KeyboardException("Bad locale tag: " + locale + ", " + errors2.toString()));
                    } else if (status != Status.MINIMAL) {
                        errors.add(new KeyboardWarningException("Non-minimal locale tag: " + locale));
                    }
                }
                String element1 = parts.getElement(1);
                if (element1.equals("baseMap")) {
                    // <baseMap fallback='true'>/ <map iso="E00" chars="ـ"/> <!-- ` -->
                    Iso iso = Iso.valueOf(parts.getAttributeValue(2, "iso"));
                    if (DEBUG) {
                        System.out.println("baseMap: iso=" + iso + ";");
                    }
                    final Output output = getOutput();
                    if (output != null) {
                        iso2output.put(iso, output);
                    }
                } else if (element1.equals("keyMap")) {
                    // <keyMap modifiers='shift+caps?'><map base="١" chars="!"/> <!-- 1 -->
                    final String modifiers = parts.getAttributeValue(1, "modifiers");
                    KeyboardModifierSet newMods = KeyboardModifierSet.parseSet(modifiers == null ? "" : modifiers);
                    if (!newMods.equals(keyMapModifiers)) {
                        if (keyMapModifiers != null) {
                            addToKeyMaps();
                        }
                        iso2output = new LinkedHashMap<Iso, Output>();
                        keyMapModifiers = newMods;
                    }
                    String isoString = parts.getAttributeValue(2, "iso");
                    if (DEBUG) {
                        System.out.println("keyMap: base=" + isoString + ";");
                    }
                    final Output output = getOutput();
                    if (output != null) {
                        iso2output.put(Iso.valueOf(isoString), output);
                    }
                } else if (element1.equals("transforms")) {
                    // <transforms type='simple'> <transform from="` " to="`"/>
                    TransformType type = TransformType.forString(parts.getAttributeValue(1, "type"));
                    if (type != currentType) {
                        if (currentType != null) {
                            transformMap.put(currentType, new Transforms(currentTransforms));
                        }
                        currentType = type;
                        currentTransforms = new LinkedHashMap<String, String>();
                    }
                    final String from = fixValue(parts.getAttributeValue(2, "from"));
                    final String to = fixValue(parts.getAttributeValue(2, "to"));
                    if (from.equals(to)) {
                        errors.add(new KeyboardException("Illegal transform from:" + from + " to:" + to));
                    }
                    if (DEBUG) {
                        System.out.println("transform: from=" + from + ";\tto=" + to + ";");
                    }
                    // if (result.isEmpty()) {
                    // System.out.println("**Empty result at " + path);
                    // }
                    currentTransforms.put(from, to);
                } else if (element1.equals("version")) {
                    // <version platform='0.17' number='$Revision$'/>
                    platformVersion = parts.getAttributeValue(1, "platform");
                    version = parts.getAttributeValue(1, "number");
                } else if (element1.equals("names")) {
                    // <names> <name value='cs'/>
                    names.add(parts.getAttributeValue(2, "value"));
                } else if (element1.equals("settings")) {
                    // <settings fallback='omit'/>
                    fallback = Fallback.forString(parts.getAttributeValue(1, "fallback"));
                } else {
                    throw new KeyboardException("Unexpected element: " + element1);
                }
            } catch (Exception e) {
                throw new KeyboardException("Unexpected error in: " + path, e);
            }
        }

        public void addToKeyMaps() {
            for (KeyMap item : keyMaps) {
                if (item.modifiers.containsSome(keyMapModifiers)) {
                    errors.add(new KeyboardException("Modifier overlap: " + item.modifiers + " already contains " + keyMapModifiers));
                }
                if (item.iso2output.equals(iso2output)) {
                    errors.add(new KeyboardException("duplicate keyboard: " + item.modifiers + " has same layout as " + keyMapModifiers));
                }
            }
            keyMaps.add(new KeyMap(keyMapModifiers, iso2output));
        }

        private String fixValue(String value) {
            StringBuilder b = new StringBuilder();
            int last = 0;
            while (true) {
                int pos = value.indexOf("\\u{", last);
                if (pos < 0) {
                    break;
                }
                int posEnd = value.indexOf("}", pos + 3);
                if (posEnd < 0) {
                    break;
                }
                b.append(value.substring(last, pos)).appendCodePoint(
                    Integer.parseInt(value.substring(pos + 3, posEnd), 16));
                last = posEnd + 1;
            }
            b.append(value.substring(last));
            return b.toString();
        }

        public Output getOutput() {
            String chars = null;
            TransformStatus transformStatus = TransformStatus.DEFAULT;
            Map<Gesture, List<String>> gestures = new EnumMap<Gesture, List<String>>(Gesture.class);

            for (Entry<String, String> attributeAndValue : parts.getAttributes(-1).entrySet()) {
                String attribute = attributeAndValue.getKey();
                String attributeValue = attributeAndValue.getValue();
                if (attribute.equals("to")) {
                    chars = fixValue(attributeValue);
                    if (DEBUG) {
                        System.out.println("\tchars=" + chars + ";");
                    }
                    if (chars.isEmpty()) {
                        errors.add(new KeyboardException("**Empty result at " + parts.toString()));
                    }
                } else if (attribute.equals("transform")) {
                    transformStatus = TransformStatus.fromString(attributeValue);
                } else if (attribute.equals("iso") || attribute.equals("base")) {
                    // ignore, handled above
                } else {
                    LinkedHashSet<String> list = new LinkedHashSet<String>();
                    for (String item : attributeValue.trim().split(" ")) {
                        final String fixedValue = fixValue(item);
                        if (fixedValue.isEmpty()) {
                            // throw new KeyboardException("Null string in list. " + parts);
                            continue;
                        }
                        list.add(fixedValue);
                    }
                    gestures.put(Gesture.fromString(attribute),
                        Collections.unmodifiableList(new ArrayList<String>(list)));
                    if (DEBUG) {
                        System.out.println("\tgesture=" + attribute + ";\tto=" + list + ";");
                    }
                }
            }
            return new Output(chars, gestures, transformStatus);
        };
    }

    public static class KeyboardException extends RuntimeException {
        private static final long serialVersionUID = 3802627982169201480L;

        public KeyboardException(String string) {
            super(string);
        }

        public KeyboardException(String string, Exception e) {
            super(string, e);
        }
    }

    public static class KeyboardWarningException extends KeyboardException {
        private static final long serialVersionUID = 3802627982169201480L;

        public KeyboardWarningException(String string) {
            super(string);
        }

        public KeyboardWarningException(String string, Exception e) {
            super(string, e);
        }
    }

}
