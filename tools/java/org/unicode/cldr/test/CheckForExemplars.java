/*
 ******************************************************************************
 * Copyright (C) 2005, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package org.unicode.cldr.test;

import java.util.List;
import java.util.Map;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.XMLSource;

import com.ibm.icu.impl.CollectionUtilities;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class CheckForExemplars extends CheckCLDR {
  //private final UnicodeSet commonAndInherited = new UnicodeSet(CheckExemplars.Allowed).complement(); 
  // "[[:script=common:][:script=inherited:][:alphabetic=false:]]");
  static String[] EXEMPLAR_SKIPS = {"/currencySpacing", "/hourFormat", "/exemplarCharacters", "/pattern",
    "/localizedPatternChars", "/segmentations", "/dateFormatItem", "/references"};
  
  UnicodeSet exemplars;
  UnicodeSet scriptRegionExemplars;
  UnicodeSet currencySymbolExemplars;
  boolean skip;
  Collator col;
  Collator spaceCol;
  String informationMessage;
  
  public CheckCLDR setCldrFileToCheck(CLDRFile cldrFile, Map options, List possibleErrors) {
    if (cldrFile == null) return this;
    skip = true;
    super.setCldrFileToCheck(cldrFile, options, possibleErrors);
    if (cldrFile.getLocaleID().equals("root")) {
      return this;
    }
    String locale = cldrFile.getLocaleID();
    informationMessage = "<a href='http://unicode.org/cldr/apps/survey?_=" + locale + "&x=characters'>characters</a>";
    col = Collator.getInstance(new ULocale(locale));
    spaceCol = Collator.getInstance(new ULocale(locale));
    spaceCol.setStrength(col.PRIMARY);
    
    CLDRFile resolvedFile = cldrFile.getResolved();
    boolean[] ok = new boolean[1];
    exemplars = safeGetExemplars("", possibleErrors, resolvedFile, ok);
    if (!ok[0]) exemplars = new UnicodeSet();
    
    if (exemplars == null) {
      CheckStatus item = new CheckStatus().setCause(this).setType(CheckStatus.errorType)
      .setMessage("No Exemplar Characters: {0}", new Object[]{this.getClass().getName()});
      possibleErrors.add(item);
      return this;
    }
    //UnicodeSet temp = resolvedFile.getExemplarSet("standard");
    //if (temp != null) exemplars.addAll(temp);
    UnicodeSet auxiliary = safeGetExemplars("auxiliary", possibleErrors, resolvedFile, ok); // resolvedFile.getExemplarSet("auxiliary", CLDRFile.WinningChoice.WINNING);
    if (auxiliary != null) exemplars.addAll(auxiliary);
    exemplars.addAll(CheckExemplars.AlwaysOK).freeze();
    
    scriptRegionExemplars = (UnicodeSet) new UnicodeSet(exemplars).removeAll("();,").freeze();
    
    currencySymbolExemplars = safeGetExemplars("currencySymbol", possibleErrors, resolvedFile, ok); // resolvedFile.getExemplarSet("currencySymbol", CLDRFile.WinningChoice.WINNING);
    if (currencySymbolExemplars == null) {
      currencySymbolExemplars = new UnicodeSet(exemplars);
    } else {
      currencySymbolExemplars.addAll(exemplars);
    }
    skip = false;
    return this;
  }

  private UnicodeSet safeGetExemplars(String type, List possibleErrors, CLDRFile resolvedFile, boolean[] ok) {
    UnicodeSet result = null;
    try {
        result = resolvedFile.getExemplarSet(type, CLDRFile.WinningChoice.WINNING);
        ok[0] = true;
    } catch(IllegalArgumentException iae) {
      possibleErrors.add(new CheckStatus()
          .setCause(this).setType(CheckStatus.errorType)
          .setMessage("Could not get exemplar set: " + iae.toString()));
      ok[0] = false;
    }
    return result;
  }
  
  public CheckCLDR handleCheck(String path, String fullPath, String value,
      Map<String, String> options, List<CheckStatus> result) {
    if (fullPath == null) return this; // skip paths that we don't have
    if (skip) return this;
    /*srl*/ if(path == null) { 
      throw new InternalError("Empty path!");
    } else if(getCldrFileToCheck() == null) {
      throw new InternalError("no file to check!");
    }
    String sourceLocale = getResolvedCldrFileToCheck().getSourceLocaleID(path, null);
    if (XMLSource.CODE_FALLBACK_ID.equals(sourceLocale)) {
      return this;
    } else if ("root".equals(sourceLocale)) {
      // skip eras for non-gregorian
      if (true) return this;
      if (path.indexOf("/calendar") >= 0 && path.indexOf("gregorian") <= 0) return this;
    }
    for (int i = 0; i < EXEMPLAR_SKIPS.length; ++i) {
      if (path.indexOf(EXEMPLAR_SKIPS[i]) > 0 ) return this; // skip some items.
    }
    if (path.startsWith("//ldml/posix/messages")) return this;
    
    if (path.contains("/currency") && path.endsWith("/symbol")) {
      if (!currencySymbolExemplars.containsAll(value)) {
        UnicodeSet missing = new UnicodeSet().addAll(value).removeAll(currencySymbolExemplars);
        String fixedMissing = CollectionUtilities.prettyPrint(missing, true, null, null, col, col);
        result.add(new CheckStatus().setCause(this).setType(CheckStatus.warningType)
        .setMessage("The characters \u200E{0}\u200E are not used in currency symbols in this language, according to " + informationMessage + ".", new Object[]{fixedMissing}));
      }
    } else if (!exemplars.containsAll(value)) {
      UnicodeSet missing = new UnicodeSet().addAll(value).removeAll(exemplars);
      String fixedMissing = CollectionUtilities.prettyPrint(missing, true, null, null, col, col);
      result.add(new CheckStatus().setCause(this).setType(CheckStatus.warningType)
      .setMessage("The characters \u200E{0}\u200E are not used in this language, according to " + informationMessage + ".", new Object[]{fixedMissing}));
    } else if (path.contains("/localeDisplayNames") && !scriptRegionExemplars.containsAll(value)) {
      UnicodeSet missing = new UnicodeSet().addAll(value).removeAll(scriptRegionExemplars);
      String fixedMissing = CollectionUtilities.prettyPrint(missing, true, null, null, col, col);
      result.add(new CheckStatus().setCause(this).setType(CheckStatus.warningType)
      .setMessage("The characters \u200E{0}\u200E are discouraged in display names. Please choose the best single name.", new Object[]{fixedMissing}));
    }
    // check for spaces 
    if (!path.startsWith("//ldml/references/reference") && !path.endsWith("/insertBetween")) {
      if (!value.equals(value.trim())) {
        result.add(new CheckStatus().setCause(this).setType(CheckStatus.errorType)
        .setMessage("This item must not start or end with whitespace."));
      }
    }
    return this;
  }
}