package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.cldr.util.RbnfData;

public class ListRbnf {
    public static void main(String[] args) {
        listRbnf();
    }

    public static void listRbnf() {
        Joiner joinTab = Joiner.on('\t');
        Joiner joinCommaSpace = Joiner.on(", ");
        Splitter splitDash = Splitter.on('-');
        System.out.println("\nLocale to type to subtype");
        Set<String> keys = RbnfData.INSTANCE.getRbnfTypeToLocales().keySet();
        System.out.println("locale\t" + joinTab.join(keys));

        Multimap<String, String> typeToSubtype = TreeMultimap.create();

        for (Entry<String, Multimap<String, String>> entry :
                RbnfData.INSTANCE.getLocaleToTypesToSubtypes().entrySet()) {
            String locale = entry.getKey();
            List<String> row = new ArrayList<>();
            row.add(locale);
            for (String key : keys) {
                Collection<String> values = entry.getValue().get(key);
                row.add(values == null ? "" : joinCommaSpace.join(values));
                typeToSubtype.putAll(key, values);
            }
            System.out.println(joinTab.join(row));
        }
        System.out.println("\nType to subtype");
        Set<String> allPieces = new TreeSet<>();
        Set<String> allSubtypes = new TreeSet<>();

        for (Entry<String, Collection<String>> entry : typeToSubtype.asMap().entrySet()) {
            Collection<String> values = entry.getValue();
            allSubtypes.addAll(values);
            Set<String> pieces = new TreeSet<>();
            values.stream().forEach(x -> pieces.addAll(splitDash.splitToList(x)));
            System.out.println(
                    joinTab.join(
                            entry.getKey(),
                            joinCommaSpace.join(values),
                            joinCommaSpace.join(pieces)));
            allPieces.addAll(pieces);
        }
        System.out.println("\nAll subtypes");
        System.out.println(joinCommaSpace.join(allSubtypes));

        System.out.println("\nAll pieces");
        System.out.println(joinCommaSpace.join(allPieces));

        System.out.println("\nSubtype to locale");
        for (Entry<String, Collection<String>> entry :
                RbnfData.INSTANCE.getRbnfTypeToLocales().asMap().entrySet()) {
            System.out.println(entry.getKey() + "\t" + joinCommaSpace.join(entry.getValue()));
        }
    }
}
