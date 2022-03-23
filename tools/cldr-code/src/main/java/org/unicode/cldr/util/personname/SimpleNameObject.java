package org.unicode.cldr.util.personname;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.util.ULocale;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.unicode.cldr.util.personname.PersonNameFormatter.Field;
import org.unicode.cldr.util.personname.PersonNameFormatter.ModifiedField;
import org.unicode.cldr.util.personname.PersonNameFormatter.Modifier;
import org.unicode.cldr.util.personname.PersonNameFormatter.NameObject;

/**
 * Simple implementation for testing and using in CLDR examples.
 * Immutable
 */
public class SimpleNameObject implements NameObject {

  private final ULocale nameLocale;
  private final ImmutableMap<ModifiedField, String> patternData;

  @Override
  public Set<Field> getAvailableFields() {
    Set<Field> result = EnumSet.noneOf(Field.class);
    for (Entry<ModifiedField, String> entry : patternData.entrySet()) {
      result.add(entry.getKey().getField());
    }
    return ImmutableSet.copyOf(result);
  }

  @Override
  public String getBestValue(
    ModifiedField modifiedField,
    Set<Modifier> remainingModifers
  ) {
    // TODO Alex flesh out the SimpleNameObject to handle modifiers
    // and return the ones that need handling by the formatter

    // For now just return the item.
    return patternData.get(modifiedField);
  }

  @Override
  public ULocale getNameLocale() {
    return nameLocale;
  }

  public SimpleNameObject(
    ULocale nameLocale,
    ImmutableMap<ModifiedField, String> patternData
  ) {
    this.nameLocale = nameLocale == null ? ULocale.ROOT : nameLocale;
    this.patternData = patternData;
  }

  /*
   * Takes string in form locale=fr, given=John Bob, given2=Edwin ...
   */
  public SimpleNameObject(String namePattern) {
    Map<ModifiedField, String> patternData = new LinkedHashMap<>();
    ULocale nameLocale = ULocale.ROOT;
    for (String setting : PersonNameFormatter.SPLIT_COMMA.split(namePattern)) {
      List<String> parts = PersonNameFormatter.SPLIT_EQUALS.splitToList(
        setting
      );
      if (parts.size() != 2) {
        throw new IllegalArgumentException(
          "Bad format, should be like: given=John Bob, given2=Edwin, …: " +
          namePattern
        );
      }
      final String key = parts.get(0);
      final String value = parts.get(1);
      switch (key) {
        case "locale":
          nameLocale = new ULocale(value);
          break;
        default:
          patternData.put(ModifiedField.from(key), value);
          break;
      }
    }
    this.nameLocale = nameLocale;
    this.patternData = ImmutableMap.copyOf(patternData);
  }

  @Override
  public String toString() {
    return "{locale=" + nameLocale + " " + "patternData=" + patternData + "}";
  }
}
