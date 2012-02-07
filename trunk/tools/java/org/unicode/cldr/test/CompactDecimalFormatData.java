package org.unicode.cldr.test;
public class CompactDecimalFormatData {
	static void load() {
		CompactDecimalFormat.add("en", 
			new long[]{1L, 1L, 1L, 1000L, 1000L, 1000L, 1000000L, 1000000L, 1000000L, 1000000000L, 1000000000L, 1000000000L, 1000000000000L, 1000000000000L, 1000000000000L},
			new String[]{"", "", "", "", "", "", "", "", "", "", "", "", "", "", ""},
			new String[]{"", "", "", "K", "K", "K", "M", "M", "M", "B", "B", "B", "T", "T", "T"});
		CompactDecimalFormat.add("fr", 
			new long[]{1L, 1L, 1L, 1L, 1L, 1L, 1000000L, 1000000L, 1000000L, 1000000000L, 1000000000L, 1000000000L, 1000000000000L, 1000000000000L, 1000000000000L},
			new String[]{"", "", "", "", "", "", "", "", "", "", "", "", "", "", ""},
			new String[]{"", "", "", "", "", "", " M", " M", " M", " Md", " Mds", " Mds", " Bn", " Bn", " Bn"});
}}
