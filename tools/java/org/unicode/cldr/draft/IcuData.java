package org.unicode.cldr.draft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Wrapper class for converted ICU data.
 */
class IcuData {
    private boolean hasFallback;
    private String sourceFile;
    private String name;
    private Map<String, List<String[]>> rbPathToValues;
    private boolean hasSpecial;

    public IcuData(String sourceFile, String name) {
        this(sourceFile, name, true);
    }

    public IcuData(String sourceFile, String name, boolean hasFallback) {
        this.hasFallback = hasFallback;
        this.sourceFile = sourceFile;
        this.name = name;
        rbPathToValues = new HashMap<String,List<String[]>>();
    }

    /**
     * @return true if data should fallback on data in other files, true by default
     */
    public boolean hasFallback() {
        return hasFallback;
    }

    /**
     * Returns the the relative path of the source file used to generate the
     * ICU data. Used when writing the data to file.
     * @return 
     */
    public String getSourceFile() {
        return sourceFile;
    }

    /**
     * @return the name to be used for the data.
     */
    public String getName() {
        return name;
    }
    
    public void setHasSpecial(boolean hasSpecial) {
        this.hasSpecial = hasSpecial;
    }
    
    /**
     * @return true if special data is included in this IcuData, false by default
     */
    public boolean hasSpecial() {
        return hasSpecial;
    }
    /**
     * The RB path,value pair actually has an array as the value. So when we
     * add to it, add to a list.
     * 
     * @param path
     * @param value
     * @return
     */
    public void add(String path, String[] value) {
        List<String[]> list = rbPathToValues.get(path);
        if (list == null) {
            rbPathToValues.put(path, list = new ArrayList<String[]>(1));
        }
        list.add(value);
    }

    /**
     * The RB path,value pair actually has an array as the value. So when we
     * add to it, add to a list.
     * 
     * @param path
     * @param value
     * @return
     */
    void add(String path, String value) {
        add(path, new String[]{value});
    }
    
    void addAll(String path, List<String[]> values) {
        rbPathToValues.put(path, values);
    }

    /**
     * Get items
     * 
     * @return
     */
    public Set<Entry<String, List<String[]>>> entrySet() {
        return rbPathToValues.entrySet();
    }

    /**
     * Get items
     * @return
     */
    public Set<String> keySet() {
        return rbPathToValues.keySet();
    }
    
    public boolean containsKey(String key) {
        return rbPathToValues.containsKey(key);
    }
    
    public List<String[]> get(String path) {
        return rbPathToValues.get(path);
    }
}