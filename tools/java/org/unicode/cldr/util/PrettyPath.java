package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.text.Transliterator;

public class PrettyPath {
	StringBuffer rules;
	Transliterator transliterator;
	
	static class MatchReplace {
		public MatchReplace(String regex, String replacement) {
			this.regex = regex;
			this.replacement = replacement;
		}
		String regex;
		String replacement;
	}
	
	public PrettyPath add(String regex, String replacement) {
		transliterator = null;
		rules.append(regex).append(">").append(replacement).append(";"); 
		return this;
	}

	public String transform(String path) {
		if (transliterator == null) {
			transliterator = Transliterator.createFromRules("ID", rules.toString(), Transliterator.FORWARD);
		}
		return transliterator.transliterate(path);
	}
}