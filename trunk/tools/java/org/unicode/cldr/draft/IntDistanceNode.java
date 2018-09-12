package org.unicode.cldr.draft;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.draft.XLocaleDistance.DistanceNode;
import org.unicode.cldr.draft.XLocaleDistance.DistanceTable;
import org.unicode.cldr.draft.XLocaleDistance.IdMakerFull;
import org.unicode.cldr.draft.XLocaleDistance.StringDistanceNode;
import org.unicode.cldr.draft.XLocaleDistance.StringDistanceTable;

import com.google.common.base.Objects;
import com.ibm.icu.util.Output;

final class IntDistanceNode extends DistanceNode {
    final IntDistanceNode.IntDistanceTable distanceTable;

    public IntDistanceNode(int distance, IntDistanceNode.IntDistanceTable distanceTable) {
        super(distance);
        this.distanceTable = distanceTable;
    }

    public IntDistanceNode.IntDistanceTable getDistanceTable() {
        return distanceTable;
    }

    @Override
    public boolean equals(Object obj) {
        IntDistanceNode other = (IntDistanceNode) obj;
        return distance == other.distance && Objects.equal(distanceTable, other.distanceTable);
    }

    @Override
    public int hashCode() {
        return distance ^ Objects.hashCode(distanceTable);
    }

    @Override
    public String toString() {
        return "\ndistance: " + distance + ", " + distanceTable;
    }

    public static DistanceNode from(int distance, IntDistanceNode.IntDistanceTable otherTable) {
        return otherTable == null ? new DistanceNode(distance) : new IntDistanceNode(distance, otherTable);
    }

    static class IntDistanceTable extends DistanceTable {
        private static final IdMakerFull[] ids = { new IdMakerFull<String>("lang", XLocaleDistance.ANY), new IdMakerFull<String>("script", XLocaleDistance.ANY),
            new IdMakerFull<String>("region", XLocaleDistance.ANY) };
        private static final IdMakerFull<IntDistanceNode.IntDistanceTable> cache = new IdMakerFull<>("table");

        private final IdMakerFull<String> id;
        private final DistanceNode[][] distanceNodes; // map from desired, supported => node

        public IntDistanceTable(StringDistanceTable source) {
            this(source, loadIds(source, 0));
        }

        private static int loadIds(StringDistanceTable source, int idNumber) {
            IdMakerFull id = ids[idNumber]; // use different Id for language, script, region
            for (Entry<String, Map<String, DistanceNode>> e1 : source.subtables.entrySet()) {
                int desired = id.add(e1.getKey());
                for (Entry<String, DistanceNode> e2 : e1.getValue().entrySet()) {
                    int supported = id.add(e2.getKey());
                    StringDistanceNode oldNode = (StringDistanceNode) e2.getValue();
                    if (oldNode.distanceTable != null) {
                        loadIds((StringDistanceTable) oldNode.distanceTable, idNumber + 1);
                    }
                }
            }
            return 0;
        }

        private IntDistanceTable(StringDistanceTable source, int idNumber) { // move construction out later
            id = ids[idNumber]; // use different Id for language, script, region
            int size = id.size();
            distanceNodes = new DistanceNode[size][size];

            // fill in the values in the table
            for (Entry<String, Map<String, DistanceNode>> e1 : source.subtables.entrySet()) {
                int desired = id.add(e1.getKey());
                for (Entry<String, DistanceNode> e2 : e1.getValue().entrySet()) {
                    int supported = id.add(e2.getKey());
                    DistanceNode oldNode = e2.getValue();
                    final StringDistanceTable oldDistanceTable = (StringDistanceTable) oldNode.getDistanceTable();
                    IntDistanceNode.IntDistanceTable otherTable = oldDistanceTable == null ? null
                        : cache.intern(new IntDistanceTable(oldDistanceTable, idNumber + 1));
                    DistanceNode node = IntDistanceNode.from(oldNode.distance, otherTable);
                    distanceNodes[desired][supported] = node;
                }
            }
            // now, to make star work, 
            // copy all the zero columns/rows down to any null value
            for (int row = 0; row < size; ++row) {
                for (int column = 0; column < size; ++column) {
                    DistanceNode value = distanceNodes[row][column];
                    if (value != null) {
                        continue;
                    }
                    value = distanceNodes[0][column];
                    if (value == null) {
                        value = distanceNodes[row][0];
                        if (value == null) {
                            value = distanceNodes[0][0];
                        }
                    }
                    distanceNodes[row][column] = value;
                }
            }
        }

        @Override
        public int getDistance(String desired, String supported, Output<DistanceTable> distanceTable, boolean starEquals) {
            final int desiredId = id.toId(desired);
            final int supportedId = id.toId(supported); // can optimize later
            DistanceNode value = distanceNodes[desiredId][supportedId];
            if (distanceTable != null) {
                distanceTable.value = value.getDistanceTable();
            }
            return starEquals && desiredId == supportedId && (desiredId != 0 || desired.equals(supported)) ? 0
                : value.distance;
        }

        @Override
        public boolean equals(Object obj) {
            IntDistanceNode.IntDistanceTable other = (IntDistanceNode.IntDistanceTable) obj;
            if (!id.equals(other.id)) {
                return false;
            }
            ;
            return Arrays.deepEquals(distanceNodes, other.distanceNodes);
        }

        @Override
        public int hashCode() {
            return id.hashCode() ^ Arrays.deepHashCode(distanceNodes);
        }

        @Override
        public String toString() {
            return abbreviate("\t", new HashMap<DistanceNode, Integer>(), new StringBuilder(id.name + ": ")).toString();
        }

        private StringBuilder abbreviate(String indent, Map<DistanceNode, Integer> cache, StringBuilder result) {
            for (int i = 0; i < distanceNodes.length; ++i) {
                DistanceNode[] row = distanceNodes[i];
                for (int j = 0; j < row.length; ++j) {
                    DistanceNode value = row[j];
                    if (value == null) {
                        continue;
                    }
                    result.append(value.distance);
                    IntDistanceNode.IntDistanceTable dt = (IntDistanceNode.IntDistanceTable) value.getDistanceTable();
                    if (dt == null) {
                        result.append(";");
                        continue;
                    }
                    Integer old = cache.get(value);
                    result.append("/");
                    if (old != null) {
                        result.append(old + ";");
                    } else {
                        final int table = cache.size();
                        cache.put(value, table);
                        result.append("\n" + indent + table + "=" + dt.id.name + ": ");
                        dt.abbreviate(indent + "\t", cache, result);
                    }
                }
            }
            return result;
        }

        @Override
        public Set<String> getCloser(int threshold) {
            Set<String> result = new HashSet<>();
            for (int i = 0; i < distanceNodes.length; ++i) {
                DistanceNode[] row = distanceNodes[i];
                for (int j = 0; j < row.length; ++j) {
                    DistanceNode value = row[j];
                    if (value.distance < threshold) {
                        result.add(id.fromId(i));
                        break;
                    }
                }
            }
            return result;
        }

        @Override
        String toString(boolean abbreviate) {
            return toString();
        }
    }
}