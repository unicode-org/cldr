package org.unicode.cldr.util;

import java.util.Set;
import java.util.TreeSet;

public class LocaleSet {

    private Set<CLDRLocale> set = new TreeSet<>();
    private boolean isAllLocales = false;

    public LocaleSet() {
    }

    public LocaleSet(boolean isAllLocales) {
        this.isAllLocales = isAllLocales;
    }

    public LocaleSet(Set<String> localeNameSet) {
        for (String s : localeNameSet) {
            set.add(CLDRLocale.getInstance(s));
        }
    }

    public void add(CLDRLocale locale) {
        set.add(locale);
    }

    public void addAll(Set<CLDRLocale> localeListSet) {
        set.addAll(localeListSet);
    }

    public boolean contains(CLDRLocale locale) {
        return isAllLocales || set.contains(locale);
    }

    public boolean containsLocaleOrParent(CLDRLocale locale) {
        if (isAllLocales || set.contains(locale)) {
            return true;
        }
        final CLDRLocale parent = locale.getParent();
        if (parent != null && set.contains(parent)) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        if (isAllLocales) {
            return LocaleNormalizer.ALL_LOCALES;
        }
        final Set<String> strSet = new TreeSet<>();
        for (CLDRLocale loc : set) {
            strSet.add(loc.getBaseName());
        }
        return String.join(" ", strSet);
    }

    public boolean isEmpty() {
        return set.isEmpty();
    }

    public boolean isAllLocales() {
        return isAllLocales;
    }

    public CLDRLocale firstElement() {
        return set.iterator().next();
    }

    public Set<CLDRLocale> getSet() {
        if (isAllLocales) {
            throw new IllegalArgumentException("Do not call getSet if isAllLocales");
        }
        return set;
    }

}
