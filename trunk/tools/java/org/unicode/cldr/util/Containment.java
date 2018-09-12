package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Multimap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.util.TimeZone;

public class Containment {
    private static final SupplementalDataInfo supplementalData = SupplementalDataInfo.getInstance();
    static final Relation<String, String> containmentCore = supplementalData
        .getContainmentCore();
    static final Set<String> continents = containmentCore.get("001");
    static final Set<String> subcontinents;
    static {
        LinkedHashSet<String> temp = new LinkedHashSet<String>();
        for (String continent : continents) {
            temp.addAll(containmentCore.get(continent));
        }
        subcontinents = Collections.unmodifiableSet(temp);
    }
    static final Relation<String, String> containmentFull = supplementalData
        .getTerritoryToContained();
    static final Relation<String, String> containedToContainer = (Relation<String, String>) Relation
        .of(new HashMap<String, Set<String>>(),
            HashSet.class)
        .addAllInverted(containmentFull)
        .freeze();

    static final Relation<String, String> leavesToContainers;
    static {
        leavesToContainers = Relation
            .of(new HashMap<String, Set<String>>(),
                HashSet.class);
        // for each container, get all of its leaf nodes
        Set<String> containers = supplementalData.getContainers();
        for (String s : containers) {
            HashSet<String> leaves = new HashSet<String>();
            addLeaves(s, leaves, containers);
            leavesToContainers.putAll(leaves, s);
        }
        leavesToContainers.freeze();
//        for (Entry<String, Set<String>> e : leavesToContainers.keyValuesSet()) {
//            System.out.println(e.getKey() + " " + e.getValue());
//        }
    }

    static final Relation<String, String> containedToContainerCore = (Relation<String, String>) Relation
        .of(new HashMap<String, Set<String>>(),
            HashSet.class)
        .addAllInverted(containmentCore)
        .freeze();
    static final Map<String, Integer> toOrder = new LinkedHashMap<String, Integer>();
    static int level = 0;
    static int order;
    static {
        initOrder("001");
        // Special cases. Cyprus is because it is in the wrong location because it gets picked up in the EU.
        resetOrder("003", "021");
        resetOrder("419", "005");
        resetOrder("CY", "BH");
    }

    // static Map<String, String> zone2country = StandardCodes.make().getZoneToCounty();

    public static String getRegionFromZone(String tzid) {
        if ("Etc/Unknown".equals(tzid)) {
            return "001";
        }
        try {
            return TimeZone.getRegion(tzid);
        } catch (IllegalArgumentException e) {
            return "ZZ";
        }
        // return zone2country.get(source0);
    }

    private static void addLeaves(String s, Set<String> target, Set<String> nonLeaf) {
        Set<String> contained = supplementalData.getContained(s);
        if (contained == null) {
            return;
        }
        for (String child : contained) {
            if (!nonLeaf.contains(child)) {
                target.add(child);
            } else {
                addLeaves(child, target, nonLeaf);
            }
        }
    }

    public static String getContainer(String territory) {
        Set<String> containers = containedToContainerCore.get(territory);
        if (containers == null) {
            containers = containedToContainer.get(territory);
        }
        String container = containers != null
            ? containers.iterator().next()
            : territory.equals("001") ? "001" : "ZZ";
        return container;
    }

    /**
     * Return all the containers, including deprecated.
     * @param territory
     * @return
     */
    public static Set<String> getContainers(String territory) {
        return containedToContainer.get(territory);
    }

    /**
     * Return the Continent containing the territory, or 001 if the territory is 001, otherwise ZZ
     *
     * @param territory
     */
    public static String getContinent(String territory) {
        while (true) {
            if (territory == null
                || territory.equals("001")
                || territory.equals("ZZ")
                || continents.contains(territory)) {
                return territory;
            }
            String newTerritory = getContainer(territory);
            if (newTerritory == null) {
                return territory;
            }
            territory = newTerritory;
        }
    }

    /**
     * Return the Subcontinent containing the territory, or the continent if it is a continent, or
     * 001 if it is 001, otherwise ZZ.
     *
     * @param territory
     */
    public static String getSubcontinent(String territory) {
        while (true) {
            if (territory.equals("001")
                || territory.equals("ZZ")
                || continents.contains(territory)
                || subcontinents.contains(territory)) {
                return territory;
            }
            territory = getContainer(territory);
        }
    }

    public static int getOrder(String territory) {
        Integer temp = toOrder.get(territory);
        return temp != null ? temp.intValue() : level;
    }

    private static void initOrder(String territory) {
        if (!toOrder.containsKey(territory)) {
            toOrder.put(territory, ++level);
        }
        Set<String> contained = containmentFull.get(territory);
        if (contained == null) {
            return;
        }
        for (String subitem : contained) {
            if (!toOrder.containsKey(subitem)) {
                toOrder.put(subitem, ++level);
            }
        }
        for (String subitem : contained) {
            initOrder(subitem);
        }
    }

    private static void resetOrder(String newTerritory, String oldTerritory) {
        // final Integer newOrder = toOrder.get(newTerritory);
        // if (newOrder != null) {
        // throw new IllegalArgumentException(newTerritory + " already defined as " + newOrder);
        // }
        final Integer oldOrder = toOrder.get(oldTerritory);
        if (oldOrder == null) {
            throw new IllegalArgumentException(oldTerritory + " not yet defined");
        }
        toOrder.put(newTerritory, oldOrder);
    }

    public Set<String> getContinents() {
        return continents;
    }

    public Set<String> getSubontinents() {
        return subcontinents;
    }

    public static Set<List<String>> getAllDirected(Multimap<String, String> multimap, String lang) {
        LinkedHashSet<List<String>> result = new LinkedHashSet<List<String>>();
        getAllDirected(multimap, lang, new ArrayList<String>(), result);
        return result;
    }

    private static void getAllDirected(Multimap<String, String> multimap, String lang, ArrayList<String> target, Set<List<String>> targets) {
        target.add(lang);
        Collection<String> parents = multimap.get(lang);
        int size = parents.size();
        if (size == 0) {
            targets.add(target);
        } else if (size == 1) {
            for (String parent : parents) {
                getAllDirected(multimap, parent, target, targets);
            }
        } else {
            for (String parent : parents) {
                getAllDirected(multimap, parent, (ArrayList<String>) target.clone(), targets);
            }
        }
    }

    /**
     * For each leaf region (eg "CO"), return all containers [019, 419, 005, 001]
     * @param leaf
     * @return
     */
    public static Set<String> leafToContainer(String leaf) {
        return leavesToContainers.get(leaf);
    }

    public static boolean isLeaf(String region) {
        return leavesToContainers.containsKey(region);
    }
}