package org.unicode.cldr.util;

public interface FormatterParser<T> {
    public String format(T source);

    public T parse(String formattedString);
}
