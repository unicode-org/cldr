package org.unicode.cldr.util;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.unicode.cldr.util.GrammarInfo.GrammaticalFeature;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.ibm.icu.util.Freezable;

public class GrammarDerivation implements Freezable<GrammarDerivation>{

    public enum CompoundUnitStructure {per, times, power, prefix}

    public class Values {
        public final String value0;
        public final String value1;

        public Values(String... values) {
            super();
            this.value0 = values[0];
            this.value1 = values.length == 2 ? values[0] : null;
        }
        @Override
        public String toString() {
            final ToStringHelper temp = MoreObjects.toStringHelper(getClass()).add("value0", value0);
            if (value1 != null) {
                temp.add("value1", value1);
            }
            return temp.toString();
        }

    }

    private Map<GrammaticalFeature, Map<CompoundUnitStructure, Values>> data = new TreeMap<>();

    public void add(String featureStr, String structureStr, String... values) {
        GrammaticalFeature feature = GrammaticalFeature.fromName(featureStr);
        CompoundUnitStructure structure = CompoundUnitStructure.valueOf(structureStr);
        Map<CompoundUnitStructure, Values> structureToValues = data.get(feature);
        if (structureToValues == null) {
            data.put(feature, structureToValues = new TreeMap<>());
        }
        structureToValues.put(structure, new Values(values));
    }

    public Values get(GrammaticalFeature feature, CompoundUnitStructure structure) {
        Map<CompoundUnitStructure, Values> structureToValues = data.get(feature);
        if (structureToValues == null) {
            return null;
        }
        return structureToValues.get(structure);
    }


    @Override
    public boolean isFrozen() {
        return data instanceof TreeMap;
    }

    @Override
    public GrammarDerivation freeze() {
        if (!isFrozen()) {
            data = CldrUtility.protectCollection(data);
        }
        return this;
    }

    @Override
    public GrammarDerivation cloneAsThawed() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).add("data", data).toString();
    }
}
