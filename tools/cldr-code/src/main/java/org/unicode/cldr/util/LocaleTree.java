package org.unicode.cldr.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class LocaleTree {

    public static LocaleNode getTree() {
        return LocaleTreeHelper.ROOT;
    }

    public static LocaleNode getTree(String localeId) {
        return LocaleTreeHelper.MAP.get(localeId);
    }

    public static class LocaleNode implements Comparable<LocaleNode> {
        public final int level;
        public final String localeId;
        public final String parentId;
        public final ImmutableSet<LocaleNode> immediateChildren;

        private LocaleNode(
                String localeId, int level, String parentId, Set<LocaleNode> immediateChildren) {
            this.localeId = localeId;
            this.level = level;
            this.parentId = parentId;
            this.immediateChildren = ImmutableSet.copyOf(immediateChildren);
        }

        @Override
        public String toString() {
            return toString(new StringBuilder(), "").toString();
        }

        private StringBuilder toString(StringBuilder target, String indent) {
            target.append(indent)
                    .append("localeId=")
                    .append(localeId)
                    .append(" level=")
                    .append(level);
            if (parentId != null) {
                target.append(" parentId=").append(parentId);
            }
            //            if (!immediateChildren.isEmpty()) {
            //                target.append(" children=");
            //                boolean first = true;
            //                for (LocaleNode child : immediateChildren) {
            //                    if (first) {
            //                        first = false;
            //                    } else {
            //                        target.append(", ");
            //                    }
            //                    target.append(child.localeId);
            //                }
            //            }
            return target;
        }

        public StringBuilder toStringRecursive() {
            return toStringRecursive(new StringBuilder(), "\n");
        }

        private StringBuilder toStringRecursive(StringBuilder target, String indent) {
            target.append(indent)
                    .append("localeId=")
                    .append(localeId)
                    .append(" level=")
                    .append(level);
            indent += " ";
            for (LocaleNode child : immediateChildren) {
                child.toStringRecursive(target, indent);
            }
            return target;
        }

        @Override
        public int compareTo(LocaleNode o) {
            return localeId.compareTo(o.localeId);
        }

        @Override
        public boolean equals(Object obj) {
            return localeId.equals(((LocaleNode) obj).localeId);
        }

        @Override
        public int hashCode() {
            return localeId.hashCode();
        }

        public void forEach(int levelStart, int levelLimit, Consumer<LocaleNode> action) {
            if (level < levelLimit) {
                if (level >= levelStart) {
                    action.accept(this);
                }
                for (LocaleNode child : immediateChildren) {
                    child.forEach(levelStart, levelLimit, action);
                }
            }
        }
    }

    static final class LocaleTreeHelper {
        private static final LocaleNode ROOT = load();
        private static final Map<String, LocaleNode> MAP =
                ImmutableMap.copyOf(makeMap(ROOT, new HashMap<String, LocaleNode>()));

        private static Map<String, LocaleNode> makeMap(
                LocaleNode node, Map<String, LocaleNode> map) {
            map.put(node.localeId, node);
            for (LocaleNode child : node.immediateChildren) {
                makeMap(child, map);
            }
            return map;
        }

        private static LocaleNode load() {
            Set<String> fileNames = new HashSet<>();
            // get all the files
            for (String folder : DtdType.ldml.directories) {
                final File file = new File(CLDRPaths.COMMON_DIRECTORY + folder);
                fileNames.addAll(Arrays.asList(file.list()));
            }
            // add to the multimap
            Multimap<String, String> parentToImmediateChildren = TreeMultimap.create();
            for (String fileName : fileNames) {
                if (!fileName.endsWith(".xml") || fileName.equals("root.xml")) {
                    continue;
                }
                String localeId = fileName.substring(0, fileName.length() - 4);
                final CLDRLocale parentLoc = CLDRLocale.getInstance(localeId).getParent();
                String parentId = parentLoc.toString();
                parentToImmediateChildren.put(parentId, localeId);
            }
            // set up the nodes from root
            return addNode("root", 0, null, parentToImmediateChildren);
        }

        private static LocaleNode addNode(
                String localeId,
                int level,
                String parentId,
                Multimap<String, String> parentToImmediateChildren) {
            Collection<String> children = parentToImmediateChildren.get(localeId);
            int level2 = level + 1;
            return new LocaleNode(
                    localeId,
                    level,
                    parentId,
                    children.isEmpty()
                            ? Collections.emptySet()
                            : children.stream()
                                    .map(
                                            x ->
                                                    addNode(
                                                            x,
                                                            level2,
                                                            localeId,
                                                            parentToImmediateChildren))
                                    .collect(Collectors.toCollection(TreeSet::new)));
        }
    }

    // Temp test
    public static void main(String[] args) {
        System.out.println(LocaleTree.getTree());
        System.out.println(LocaleTree.getTree("en"));
        System.out.println(LocaleTree.getTree("en_001"));
        System.out.println(LocaleTree.getTree().toStringRecursive());
        final Consumer<LocaleNode> action = x -> System.out.println(x.localeId);
        new TreeSet().spliterator();
        LocaleTree.getTree("en").forEach(1, 99, action);
    }
}
