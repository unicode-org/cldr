package org.unicode.cldr.icu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Wrapper class for converted ICU data which RB paths to values.
 */
public class IcuData implements Iterable<String> {
    private boolean hasFallback;
    private String sourceFile;
    private String name;
    private Map<String, List<String[]>> rbPathToValues;
    private String comment;
    private Map<String, String> enumMap;

    /**
     * IcuData constructor.
     *
     * @param sourceFile
     *            the source file of the IcuData object, displayed in
     *            comments in the file
     * @param name
     *            The name of the IcuData object, also used as the name of the
     *            root node in the output file
     * @param hasFallback
     *            true if the output file has another ICU file as a
     *            fallback
     */
    public IcuData(String sourceFile, String name, boolean hasFallback) {
        this(sourceFile, name, hasFallback, new HashMap<String, String>());
    }

    /**
     * IcuData constructor.
     *
     * @param sourceFile
     *            the source file of the IcuData object, displayed in
     *            comments in the file
     * @param name
     *            The name of the IcuData object, also used as the name of the
     *            root node in the output file
     * @param hasFallback
     *            true if the output file has another ICU file as a
     *            fallback
     * @param enumMap
     *            a mapping of CLDR string values to their integer values in
     *            ICU
     */
    public IcuData(String sourceFile, String name, boolean hasFallback, Map<String, String> enumMap) {
        this.hasFallback = hasFallback;
        this.sourceFile = sourceFile;
        this.name = name;
        rbPathToValues = new HashMap<String, List<String[]>>();
        this.enumMap = enumMap;
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
     *
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

    /**
     * Sets a comment to be placed above the data structure.
     * @param comment
     */
    public void setFileComment(String comment) {
        this.comment = comment;
    }

    public String getFileComment() {
        return comment;
    }

    /**
     * The RB path,value pair actually has an array as the value. So when we
     * add to it, add to a list.
     *
     * @param path
     * @param value
     * @return
     */
    public void add(String path, String... values) {
        List<String[]> list = rbPathToValues.get(path);
        if (list == null) {
            rbPathToValues.put(path, list = new ArrayList<String[]>(1));
        }
        list.add(normalizeValues(path, values));
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
        add(path, new String[] { value });
    }

    void addAll(String path, Collection<String[]> valueList) {
        for (String[] values : valueList) {
            add(path, values);
        }
    }

    public void replace(String path, String... values) {
        List<String[]> list = new ArrayList<String[]>(1);
        rbPathToValues.put(path, list);
        list.add(normalizeValues(path, values));
    }

    private String[] normalizeValues(String rbPath, String[] values) {
        if (isIntRbPath(rbPath)) {
            List<String> normalizedValues = new ArrayList<String>();
            for (int i = 0; i < values.length; i++) {
                String curValue = values[i];
                String enumValue = enumMap.get(curValue);
                if (enumValue != null) curValue = enumValue;
                normalizedValues.add(curValue);
            }
            return normalizedValues.toArray(values);
        } else {
            return values;
        }
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
     *
     * @return
     */
    public Set<String> keySet() {
        return rbPathToValues.keySet();
    }

    @Override
    public Iterator<String> iterator() {
        return rbPathToValues.keySet().iterator();
    }

    public int size() {
        return rbPathToValues.size();
    }

    public boolean containsKey(String key) {
        return rbPathToValues.containsKey(key);
    }

    public List<String[]> get(String path) {
        return rbPathToValues.get(path);
    }

    /**
     * @param rbPath
     * @return true if the rbPath is for integer values.
     */
    public static boolean isIntRbPath(String rbPath) {
        return rbPath.endsWith(":int") || rbPath.endsWith(":intvector");
    }
}