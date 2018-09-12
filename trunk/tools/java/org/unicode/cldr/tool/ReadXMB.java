package org.unicode.cldr.tool;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.XMLFileReader;

public class ReadXMB {

    /*
     * Two cases:
     * <!-- //ldml/characters/ellipsis[@type="final"] -->
     * <msg id='8185172660664561036' desc='Supply the elipsis pattern for when the final part of a string is omitted.
     * Note: before translating, be sure to read http://cldr.org/translation/characters.'
     * ><ph name='FIRST_PART_OF_TEXT'><ex>very long na</ex>{0}</ph>…</msg>
     * <!-- English original: {0}… -->
     * and
     * <!-- //ldml/characters/exemplarCharacters[@type="currencySymbol"] -->
     * <msg id='684343635911473473' desc='Supply the characters used in your language for the "currencySymbol" category.
     * Note: before translating, be sure to read http://cldr.org/translation/exemplars.'
     * >[a b c č d e f g h i j k l ł m n o º p q r s t u v w x y z]</msg>
     */
    public static Map<String, String> load(String directory, String file) {
        final CasingHandler simpleHandler = new CasingHandler();
        XMLFileReader xfr = new XMLFileReader().setHandler(simpleHandler);
        xfr.read(directory + "/" + file, XMLFileReader.CONTENT_HANDLER | XMLFileReader.ERROR_HANDLER
            | XMLFileReader.LEXICAL_HANDLER, true);
        simpleHandler.flush();
        return simpleHandler.info;
    }

    private static class CasingHandler extends XMLFileReader.SimpleHandler {
        public Map<String, String> info = new LinkedHashMap<String, String>();
        String path;
        String id;
        String value;

        @Override
        public void handlePathValue(String pathx, String value) {
            // System.out.println("*PATH:\t" + pathx + "\t\t" + value);
            int pos = pathx.indexOf("[@id=\"");
            int posEnd = pathx.indexOf("\"]", pos + 6);
            id = pathx.substring(pos + 6, posEnd);
            if (id == null) {
                System.out.println("PATH:\t" + pathx + "\t\t" + value);
            }
            this.value = value;
        }

        @Override
        public void handleComment(String pathx, String comment) {
            // System.out.println("*COMMENT:\t" + path + "\t\t" + comment);
            comment = comment.trim();
            if (comment.startsWith("//ldml")) {
                flush();
                path = comment;
            } else if (comment.startsWith("English original:")) {
                value = comment.substring(17).trim();
            } else {
                System.out.println("COMMENT:\t" + pathx + "\t\t" + comment);
            }
        }

        private void flush() {
            // System.out.println(id + "\t" + value + "\t" + path);
            if (path != null) {
                info.put(path, value);
            }
            id = value = path = null;
        }
    }

    public static void main(String[] args) {
        Map<String, String> info = load(CLDRPaths.BASE_DIRECTORY + "tools/java/org/unicode/cldr/unittest/data/xmb/",
            "en.xml");
        System.out.println("============");
        for (Entry<String, String> entry : info.entrySet()) {
            System.out.println(entry.getValue() + "\t" + entry.getKey());
        }
    }
}