package org.unicode.cldr.unittest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import org.unicode.cldr.unittest.CheckResult.ResultStatus;
import org.unicode.cldr.unittest.ObjectMatcherFactory;
import org.unicode.cldr.unittest.ObjectMatcherFactory.MatcherPattern;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.dev.util.CollectionUtilities.ObjectMatcher;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.UnicodeSet;


public class TestAttributeValues extends TestFmwk {

	private final static Joiner SEMICOLON_JOINER = Joiner.on(";").skipNulls();

	/**
	 * Source directories
	 */
	private String[] sourceDirs = new String[] { 
			"/Users/ribnitz/Documents/workspace/cldr/common",
			"/Users/ribnitz/Documents/workspace/cldr/exemplars", 
	//	"/Users/ribnitz/Documents/workspace/cldr/keyboards",
		"/Users/ribnitz/Documents/workspace/cldr/seed"
			};

	/**
	 * Output to CSV
	 */
	private boolean csvOutput = false;

	/**
	 * location of the CSV file
	 */
	private static final String CSV_FILE = "/Users/ribnitz/Documents/data_errors.csv";

	private final Set<String> elementOrder = new LinkedHashSet<String>();
	private final Set<String> attributeOrder = new LinkedHashSet<String>();

	private final Map<String, Map<String, MatcherPattern>> element_attribute_validity = new HashMap<String, Map<String, MatcherPattern>>();
	private final Map<String, MatcherPattern> common_attribute_validity = new HashMap<String, MatcherPattern>();
	final static Map<String, MatcherPattern> variables = new HashMap<String, MatcherPattern>();
	// static VariableReplacer variableReplacer = new VariableReplacer(); //
	// note: this can be coalesced with the above
	// -- to do later.
	private  boolean initialized = false;
	private LocaleMatcher localeMatcher;
	private final Map<String, Map<String, String>> code_type_replacement = new TreeMap<String, Map<String, String>>();
	private SupplementalDataInfo supplementalData;

	private boolean isEnglish;
	PluralInfo pluralInfo;

	XPathParts parts = new XPathParts(null, null);
	static final UnicodeSet DIGITS = new UnicodeSet("[0-9]").freeze();

	/**
	 * Callable returning the value of csvOutput, used to call the
	 * factory method on CheckResult. 
	 */
	private class CheckCSVValuePred implements Callable<Boolean> {
		@Override
		public Boolean call() {
			return csvOutput;
		}
	}

	/**
	 * Predicate to filter the results, will return only those where the ResultStatus matches
	 * @author ribnitz
	 *
	 */
	private static final class CheckResultPredicate implements
			Predicate<CheckResult> {
		private final ResultStatus success;
		
		public CheckResultPredicate(ResultStatus success) {
			this.success=success;
		}
		@Override
		public boolean apply(CheckResult input) {
			return input.getStatus()==success;
		}
	}

	/**
	 * Tightly coupled matcher for Locales
	 * @author ribnitz
	 *
	 */
	private static class LocaleMatcher implements ObjectMatcher<String> {
		ObjectMatcher<String> grandfathered = variables.get("$grandfathered").matcher;
		ObjectMatcher<String> language = variables.get("$language").matcher;
		ObjectMatcher<String> script = variables.get("$script").matcher;
		ObjectMatcher<String> territory = variables.get("$territory").matcher;
		
		ObjectMatcher<String> variant = variables.get("$variant").matcher;
		LocaleIDParser lip = new LocaleIDParser();
		static LocaleMatcher singleton = null;
		static Object sync = new Object();

		private LocaleMatcher(boolean b) {
		}

		public static LocaleMatcher make() {
			synchronized (sync) {
				if (singleton == null) {
					singleton = new LocaleMatcher(true);
				}
			}
			return singleton;
		}

		public boolean matches(String value) {
			if (grandfathered.matches(value)) return true;
			lip.set((String) value);
			String field = lip.getLanguage();
			if (!language.matches(field)) return false;
			field = lip.getScript();
			if (field.length() != 0 && !script.matches(field)) return false;
			field = lip.getRegion();
			if (field.length() != 0 && !territory.matches(field)) return false;
			String[] fields = lip.getVariants();
			for (int i = 0; i < fields.length; ++i) {
				if (!variant.matches(fields[i])) return false;
			}
			return true;
		}
	}

	private static class FileAcceptPredicate implements Predicate<Path> {

		private  String[] fnameExclusions=new String[] {
			//	"Latn","FONIPA", "Arab","Ethi"
		};
		//private final static Pattern FNAME_PATTERN=PatternCache.get("[a-z]{2,3}");
		private final static String[] EXCLUDED_PATHS = new String[] { /*"bcp47" */ };
		private final Collection<String> excludedFiles;
		public FileAcceptPredicate(Collection<String> excluded) {
			excludedFiles=excluded;
		}
		@Override
		public boolean apply(Path input) {
			
			String fname = input.getFileName()
					.toString();
//			int underscorePos=fname.indexOf("_");
//			int dotPos=fname.indexOf(".");
//			int matchPos;
//			if (underscorePos>-1) {
//				matchPos=underscorePos;
//			} else {
//				matchPos=dotPos;
//			}
//			if (!FNAME_PATTERN.matcher(fname.substring(0,matchPos)).matches()) {
//				return false;
//			}
			if (!fname.endsWith(".xml")) {
				return false;
			}
			if (excludedFiles.contains(fname)) {
				return false;
			}
			for (String cur:fnameExclusions) {
				if (fname.contains(cur)) {
					return false;
				}
			}
			boolean add=true;
			for (int i = 0; i < input.getNameCount(); i++) {
				if (!add) {
					break;
				}
				String curPath = input.getName(i)
						.toString();
				for (String exc : EXCLUDED_PATHS) {
					if (curPath.contains(exc)) {
						add = false;
						break;
					}
				}
			}
			return add;
		}
		
	}
	private static class LocaleSetGenerator {
		
		public static Set<Path> generateLocaleSet(Iterable<File> sourceFiles,
				 final Collection<String> excludedFiles) {
			final Set<Path> fileSet = new HashSet<>();
			final Predicate<Path> acceptPredicate=new FileAcceptPredicate(excludedFiles);
			for (File sourceFile : sourceFiles) {
				try {
					Files.walkFileTree(sourceFile.toPath(),
							new SimpleFileVisitor<Path>() {

								public FileVisitResult visitFile(
										Path file,
										BasicFileAttributes attrs)
										throws IOException {
									 
									if (acceptPredicate.apply(file)) {
										fileSet.add(file);
									}
									return FileVisitResult.CONTINUE;
								}

							});
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return fileSet;
		}
	}
	

	private static final Relation<PluralInfo.Count, String> PLURAL_EXCEPTIONS = Relation.of(
			new EnumMap<PluralInfo.Count, Set<String>>(PluralInfo.Count.class), HashSet.class);
	static {
		PLURAL_EXCEPTIONS.put(PluralInfo.Count.many, "hr");
		PLURAL_EXCEPTIONS.put(PluralInfo.Count.many, "sr");
		PLURAL_EXCEPTIONS.put(PluralInfo.Count.many, "sh");
		PLURAL_EXCEPTIONS.put(PluralInfo.Count.many, "bs");
		PLURAL_EXCEPTIONS.put(PluralInfo.Count.few, "ru");
	}

	private static boolean isPluralException(Count countValue, String locale) {
		Set<String> exceptions = PLURAL_EXCEPTIONS.get(countValue);
		if (exceptions == null) {
			return false;
		}
		if (exceptions.contains(locale)) {
			return true;
		}
		int bar = locale.indexOf('_'); // catch bs_Cyrl, etc.
		if (bar > 0) {
			String base = locale.substring(0, bar);
			if (exceptions.contains(base)) {
				return true;
			}
		}
		return false;
	}


	/**
	 * Returns replacement, or null if there is none. "" if the code is deprecated, but without a replacement.
	 * Input is of the form $language
	 * 
	 * @return
	 */
	private String getReplacement(String value, String attributeValue) {
		Map<String, String> type_replacement = code_type_replacement.get(value);
		if (type_replacement == null) {
			return null;
		}
		return type_replacement.get(attributeValue);
	}

	LocaleIDParser localeIDParser = new LocaleIDParser();

	private void initialize(CLDRFile cldrFileToCheck, Collection<CheckResult> possibleErrors,Factory fact) {

		if (cldrFileToCheck==null) {
			return;
		}
		

		supplementalData = SupplementalDataInfo.getInstance(cldrFileToCheck.getSupplementalDirectory());
		pluralInfo = supplementalData.getPlurals(PluralType.cardinal, cldrFileToCheck.getLocaleID());
		//	        	super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
		isEnglish = "en".equals(localeIDParser.set(cldrFileToCheck.getLocaleID()).getLanguage());
		synchronized (elementOrder) {
			if (!initialized) {
				//    CLDRFile metadata = getFactory().getSupplementalMetadata();
				CLDRFile metadata = fact.getSupplementalMetadata();
				getMetadata(metadata, supplementalData, fact);
				initialized = true;
				localeMatcher = LocaleMatcher.make();
				// cleaup localeList
				Iterator<String>  lIter=localeSet.iterator();
				while (lIter.hasNext()) {
					String loc=lIter.next();
					if (!localeMatcher.matches(loc)) {
						lIter.remove();
					}
				}
			}
		}
//		String localeId= cldrFileToCheck.getLocaleID();
//		if (!localeMatcher.matches(localeId)) {
//				possibleErrors.add(
//					new CheckResult().setStatus(ResultStatus.error).setLocale(localeId)
//					.setMessage("Invalid Locale {0}", new Object[] { localeId,cldrFileToCheck}));
//
//		}
	}

	private Set<String> unknownFinalElements=new HashSet<>();
	private Set<String> unhandledPaths=new HashSet<>();
	private final static Splitter WHITESPACE_SPLTTER=Splitter.on(PatternCache.get("\\s+"));

	private Set<String> localeSet = new HashSet<>();
	
	private void getMetadata(CLDRFile metadata, SupplementalDataInfo sdi, Factory fact) {
		// sorting is expensive, but we need it here.

		Comparator<String> ldmlComparator = metadata.getComparator();
		String path2 = metadata.iterator().next();
		if (!path2.startsWith("//ldml")) {
			ldmlComparator = null;
		}
		String locale=metadata.getLocaleID();
		for (String p: new ForwardingIterable<String>(metadata.iterator(null, ldmlComparator))) {
			String value = metadata.getStringValue(p);
			String path = metadata.getFullXPath(p);
			parts.set(path);
			String lastElement = parts.getElement(-1);
			if (lastElement.equals("elementOrder")) {
				elementOrder.addAll(WHITESPACE_SPLTTER.splitToList(value.trim()));
			} else if (lastElement.equals("attributeOrder")) {
				attributeOrder.addAll(WHITESPACE_SPLTTER.splitToList(value.trim()));
			} else if (lastElement.equals("suppress")) {
				// skip for now
				unhandledPaths.add("Unhandled path (suppress):"+path);
			} else if (lastElement.equals("serialElements")) {
				// skip for now
			//	unhandledPaths.add("Unhandled path (serialElement):"+path);
			} else if (lastElement.equals("attributes")) {
				// skip for now
			} else if (lastElement.equals("variable")) {
				// String oldValue = value;
				// value = variableReplacer.replace(value);
				// if (!value.equals(oldValue)) System.out.println("\t" + oldValue + " => " + value);
				Map<String, String> attributes = parts.getAttributes(-1);
				MatcherPattern mp = getMatcherPattern(value, attributes, path, sdi);
				if (mp != null) {
					String id = attributes.get("id");
					variables.put(id, mp);
					// variableReplacer.add(id, value);
				}
			} else if (lastElement.equals("attributeValues")) {
				try {
					Map<String, String> attributes = parts.getAttributes(-1);

					MatcherPattern mp = getMatcherPattern(value, attributes, path, sdi);
					if (mp == null) {
						// System.out.println("Failed to make matcher for: " + value + "\t" + path);
						continue;
					}
					Iterable<String> attributeList =WHITESPACE_SPLTTER.split(attributes.get("attributes").trim());
					String elementsString = (String) attributes.get("elements");
					if (elementsString == null) {
						addAttributes(attributeList, common_attribute_validity, mp);
					} else {
						Iterable<String> elementList=WHITESPACE_SPLTTER.split(elementsString.trim());
//						String[] elementList = elementsString.trim().split("\\s+");
						for (String element: elementList) {
							// System.out.println("\t" + element);
							Map<String, MatcherPattern> attribute_validity = element_attribute_validity.get(element);
							if (attribute_validity == null)
								element_attribute_validity.put(element,
										attribute_validity = new TreeMap<String, MatcherPattern>());
							addAttributes(attributeList, attribute_validity, mp);
						}
					}

				} catch (RuntimeException e) {
					System.err
					.println("Problem with: " + path + ", \t" + value);
					e.printStackTrace();
				}
			} else if (lastElement.equals("version")) {
				// skip for now
				// skip for now
				//unhandledPaths.add("Unhandled path (version):"+path);
			} else if (lastElement.equals("generation")) {
				// skip for now
			} else if (lastElement.endsWith("Alias")) {
				String code = "$" + lastElement.substring(0, lastElement.length() - 5);
				Map<String, String> type_replacement = code_type_replacement.get(code);
				if (type_replacement == null) {
					code_type_replacement.put(code, type_replacement = new TreeMap<String, String>());
				}
				Map<String, String> attributes = parts.getAttributes(-1);
				String type = attributes.get("type");
				String replacement = attributes.get("replacement");
				if (replacement == null) {
					replacement = "";
				}
				type_replacement.put(type, replacement);
			} else if (lastElement.equals("territoryAlias")) {
				// skip for now
				unhandledPaths.add("Unhandled path (territoryAlaias):"+path);
			} else if (lastElement.equals("deprecatedItems")) {
				// skip for now
				//unhandledPaths.add("Unhandled path (deprecatedItems):"+path);
			} else if (lastElement.endsWith("Coverage")) {
				// skip for now
				// skip for now
				unhandledPaths.add("Unhandled path (Coverage):"+path);
			} else if (lastElement.endsWith("skipDefaultLocale")) {
				// skip for now
				unhandledPaths.add("Unhandled path (SkipdefaultLocale):"+path);
			} else if (lastElement.endsWith("defaultContent")) {
				String locPath = extractValueList(path,"locales");
				Iterable<String> locales=WHITESPACE_SPLTTER.split(locPath);
				Set<String> invalidLocales=new HashSet<>();
				for (String loc: locales) {
					if (!localeSet.contains(loc)) {
						invalidLocales.add(loc);
					}
				}
				if (!invalidLocales.isEmpty()) {
					unhandledPaths.add(invalidLocales.size()+" invalid locales: "+Joiner.on(", ").skipNulls().join(invalidLocales)+" Path:"+path);
				}
			//	unhandledPaths.add("Unhandled path (defaultContent):"+path);
			} else if (lastElement.endsWith("distinguishingItems")) {
				// skip for now
				//unhandledPaths.add("Unhandled path (distinguishingItems):"+path);
			} else if (lastElement.endsWith("blockingItems")) {
				// skip for now
				//.add("Unhandled path (blockingItems):"+path);
			} else {
				System.out.println("Unknown final element: " + path);
				unknownFinalElements.add(path);
			}
		}
	}


	private String extractValueList(String path,String pattern) {
		//filter the path
		int pathStart=path.indexOf(pattern);
		String locPath=null;
		if (pathStart>-1) {
			int strStart=pathStart+pattern.length();
			int strEnd=path.indexOf("\"",strStart+3);
		 locPath=path.substring(strStart+2,strEnd);
		}
		return locPath;
	}

	private MatcherPattern getBcp47MatcherPattern(SupplementalDataInfo sdi, String key) {
		MatcherPattern m = new MatcherPattern();
		Relation<R2<String, String>, String> bcp47Aliases = sdi.getBcp47Aliases();
		Set<String> values = new TreeSet<String>();
		for (String value : sdi.getBcp47Keys().getAll(key)) {
			if (key.equals("cu")) { // Currency codes are in upper case.
				values.add(value.toUpperCase());
			} else {
				values.add(value);
			}
			R2<String, String> keyValue = R2.of(key, value);
			Set<String> aliases = bcp47Aliases.getAll(keyValue);
			if (aliases != null) {
				values.addAll(aliases);
			}
		}

		// Special case exception for generic calendar, since we don't want to expose it in bcp47
		if (key.equals("ca")) {
			values.add("generic");
		}

		m.value = key;
		m.pattern = values.toString();
		m.matcher =  ObjectMatcherFactory.createCollectionMatcher(values);
		return m;

	}

	private MatcherPattern getMatcherPattern(String value, Map<String, String> attributes, String path,
			SupplementalDataInfo sdi) {
		String typeAttribute = attributes.get("type");
		MatcherPattern result = variables.get(value);
		if (result != null) {
			MatcherPattern temp = new MatcherPattern();
			temp.pattern = result.pattern;
			temp.matcher = result.matcher;
			temp.value = value;
			result = temp;
			if ("list".equals(typeAttribute)) {
				temp.matcher = ObjectMatcherFactory.createListMatcher(result.matcher);
			}
			return result;
		}

		result = new MatcherPattern();
		result.pattern = value;
		result.value = value;
		if (typeAttribute==null) {
			if (value.startsWith("$")) {
				typeAttribute="choice";
			}
		}
		if ("choice".equals(typeAttribute)
				|| "given".equals(attributes.get("order"))) {
			List<String> valueList=WHITESPACE_SPLTTER.splitToList(value.trim());
			result.matcher =ObjectMatcherFactory.createCollectionMatcher(valueList);
		} else if ("bcp47".equals(typeAttribute)) {
			result = getBcp47MatcherPattern(sdi, value);
		} else if ("regex".equals(typeAttribute)) {
			result.matcher = ObjectMatcherFactory.createRegexMatcher(value, Pattern.COMMENTS); // Pattern.COMMENTS to get whitespace
		} else if ("list".equals(typeAttribute)) {
			result.matcher = ObjectMatcherFactory.createRegexMatcher(value, Pattern.COMMENTS); // Pattern.COMMENTS to get whitespace
		} else if ("locale".equals(typeAttribute)) {
			// locale: localeMatcher will test for language values, and fail with a NPE.
			if (variables.containsKey("$language")) {
				result.matcher = LocaleMatcher.make();
			} else {
				// no language in the variables
				System.out.println("Empty locale type element at Path: "+path);
				result.matcher=ObjectMatcherFactory.createNullHandlingMatcher(variables, "$language",true);
			}
		} else if ("notDoneYet".equals(typeAttribute) || "notDoneYet".equals(value)) {
			result.matcher = ObjectMatcherFactory.createRegexMatcher(".*", Pattern.COMMENTS);
		} else {
			System.out.println("unknown type; value: <" + value + ">,\t" + typeAttribute + ",\t" + attributes + ",\t"
					+ path);
			return null;
		}
		return result;
	}

	private void addAttributes(Iterable<String> attributes, Map<String, MatcherPattern> attribute_validity, MatcherPattern mp) {
		for (String attribute : attributes) {
			MatcherPattern old = attribute_validity.get(attribute);
			if (old != null) {
				mp.matcher = ObjectMatcherFactory.createOrMatcher(old.matcher, mp.matcher);
				mp.pattern = old.pattern + " OR " + mp.pattern;
			}
			attribute_validity.put(attribute, mp);
		}
	}

	public void TestNoUnhandledFinalElements() {
		assertTrue("The following final elements are unknown: "+unknownFinalElements, unknownFinalElements.isEmpty());
	}
	
	public void TestNoUnhandledPaths() {
		if (!unhandledPaths.isEmpty()) {
			StringBuilder sb=new StringBuilder();
			sb.append(unhandledPaths.size()+" unhandled paths:\r\n");
			for (String p: unhandledPaths) {
				sb.append(p);
				sb.append("\r\n");
			}
			errln(sb.toString());
		}
	}
	private final static Collection<String> FILE_EXCLUSIONS=Arrays.asList(new String[] {"coverageLevels.xml","pluralRanges.xml"});
	public void TestAttributes() { 
		CLDRConfig cldrConf=CLDRConfig.getInstance();
		File[] sourceFiles=new File[sourceDirs.length];
		for (int i=0;i<sourceDirs.length;i++) {
			sourceFiles[i]=new File(sourceDirs[i]);
		}
	//	File[] sourceDirectories = cldrConf.getMainDataDirectories(sourceFiles);
		final Iterable<Path> fileSet = LocaleSetGenerator.generateLocaleSet(Arrays.asList(sourceFiles),FILE_EXCLUSIONS);
		final Set<File> dirs=new HashSet<>();
		for (Path cur: fileSet) {
			String curName=cur.getFileName().toString();
			localeSet.add(curName.substring(0, curName.lastIndexOf(".")));
			dirs.add(cur.getParent().toFile());
		}
		
		
		String factoryFilter="("+Joiner.on("|").join(localeSet)+")";
		// get the factory
		// set up the test
		Factory cldrFactory = SimpleFactory.make(dirs.toArray(new File[0]), factoryFilter);
		//		.setSupplementalDirectory(new File(CLDRPaths.SUPPLEMENTAL_DIRECTORY));
		supplementalData=cldrConf.getSupplementalDataInfo();
		Set<String> availableLocales=cldrFactory.getAvailable();
	    System.out.println("Testing: en");
	    CLDRFile rootLocale=CLDRConfig.getInstance().getEnglish();
	    List<CheckResult> results=new ArrayList<>();
		initialize(rootLocale, results,cldrFactory);
		for (String curPath: rootLocale) {
			handleCheck(curPath, curPath, "", results,rootLocale);
		}
//		List<CheckResult> results=new ArrayList<>();
		Set<String> localesToTest=new HashSet<>(availableLocales);
		localesToTest.remove(rootLocale.getLocaleID());
		int numTested=1;
		for (String c:localesToTest) {
			System.out.println("Testing: "+c);
			CLDRFile curCldr= cldrFactory.make(c, false);
			initialize(curCldr, results,cldrFactory);
			for (String curPath: curCldr) {
				handleCheck(curPath, curPath, "", results,curCldr);
			}
			numTested++;
		}
		List<CheckResult> warnings=Collections.emptyList();
		List<CheckResult> errors=Collections.emptyList();
		// did we get some errors or warnings?
		if (!results.isEmpty()) {
			warnings=FluentIterable.from(results).filter(new CheckResultPredicate(ResultStatus.warning)).toList();
			errors=FluentIterable.from(results).filter(new CheckResultPredicate(ResultStatus.error)).toList();

			System.out.println(getResultMessages(warnings,"warnings"));
			System.out.println(getResultMessages(errors, "errors"));
		}
		boolean successful=warnings.isEmpty() && errors.isEmpty();
		if (!successful) {
			if (csvOutput) {
				StringBuilder sb = new StringBuilder();
				compileProblemList(warnings, sb);
				compileProblemList(errors, sb);
				File errFile=new File(CSV_FILE);
				if (errFile.exists()) {
					errFile.delete();
				}
				try {
					if (errFile.createNewFile()) {
						try (Writer wr=new BufferedWriter(new FileWriter(errFile))) {
							wr.write(sb.toString());
						}
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			StringBuilder sb=new StringBuilder();
			if (errors.size()>0) {
				sb.append(errors.size());
				sb.append(" errors");
			}
			if (errors.size()>0 && warnings.size()>0) {
				sb.append(" and ");
			}
			if (warnings.size()>0) {
				sb.append(warnings.size());
				sb.append(" warnings");
			}
			sb.append("\r\n");
			if (!unhandledPaths.isEmpty()) {
				sb.append(unhandledPaths.size()+" unhandled paths:\r\n");
				for (String p: unhandledPaths) {
					sb.append(p);
					sb.append("\r\n");
				}
			}
			logln(sb.toString());
			System.out.println(numTested+" CLDRFiles tested");
		}

	}



	public void TestStaticInterface() {
		CLDRConfig config=CLDRConfig.getInstance();
		TestAttributeValues.performCheck(config.getEnglish(),config.getCldrFactory(), supplementalData);
	}
	
	
	private void compileProblemList(List<CheckResult> problems, StringBuilder sb) {
		if (csvOutput) {
			for (CheckResult problem : problems) {
				sb.append(SEMICOLON_JOINER.join(new Object[] { 
						problem.getStatus().name(),
						problem.getLocale(),
						problem.getMessage(), 
						problem.getPath()}));
				sb.append("\r\n");
			}
		} else {
			for (CheckResult problem : problems) { 
				sb.append(problem.getStatus()+" "+problem.getLocale()+" "+problem.getMessage());
				sb.append("\r\n");
			}
		}
	}

	/**
	 * Perform a check of fileToCheck, providing the factory, and SupplementalDataInfo
	 * @param fileToCheck
	 * @param fact
	 * @param sdi
	 * @return an iterable over the results.
	 */
	public static Iterable<CheckResult> performCheck(CLDRFile fileToCheck,Factory fact,SupplementalDataInfo sdi) {
		List<CheckResult> results=new ArrayList<>();
		TestAttributeValues tav=new TestAttributeValues();
		tav.supplementalData=sdi;
			tav.initialize(fileToCheck, results,fact);
			for (String curPath: fileToCheck) {
				tav.handleCheck(curPath, curPath, "", results,fileToCheck);
			}
		if (results.isEmpty()) {
			return Collections.emptyList();
		}
		return results;
	}

	private String getResultMessages(Collection<CheckResult> result,String resultType) {
		StringBuilder sb=new StringBuilder();
		if (result.size()>0) {
			sb.append("The following "+resultType+" occurred:");
			sb.append("\r\n");
			for (CheckResult cur: result) {
				sb.append(cur.getMessage());
				sb.append("\r\n");
			}
		}
		return sb.toString();
	}

	private void check(Map<String, MatcherPattern> attribute_validity,
			String attribute, String attributeValue, List<CheckResult> result,
			String path, String locale) {
		if (attribute_validity == null)
			return; // no test
		MatcherPattern matcherPattern = attribute_validity.get(attribute);
		if (matcherPattern == null)
			return; // no test
		if (matcherPattern.matcher.matches(attributeValue))
			return;
		// special check for deprecated codes
		String replacement = getReplacement(matcherPattern.value,
				attributeValue);
		if (replacement != null) {
			// if (isEnglish) return; // don't flag English
			if (replacement.length() == 0) {
				result.add(new CheckResult()
						.setStatus(ResultStatus.warning)
						.setLocale(locale)
						.setPath(path)
						.setMessage(
								"Locale {0}: Deprecated Attribute Value {1}={2}. Consider removing. Path:{3}",
								new Object[] { locale, attribute,
										attributeValue, path }));
			} else {
				CheckResult cr=CheckResult.create(ResultStatus.warning, locale, path, new CheckCSVValuePred(), 
						"Locale {0}: Deprecated Attribute Value {1}={2}. Consider removing, and possibly modifying the related "
						+ "value for {3}.", 
						"Locale {0}: Deprecated Attribute Value {1}={2}. Consider removing, and possibly modifying the related value for {3}.Path: {4}", 
						new Object[] { locale, attribute,attributeValue, replacement}, 
						new Object[] { locale, attribute,attributeValue, replacement, path });
				result.add(cr);
			}
		} else {
			String pattern = matcherPattern.pattern;
			// for now: disregard missing variable expansions

			if (pattern != null && !pattern.trim().startsWith("$")) {
				// root locale?
				if (locale.equals(CLDRConfig.getInstance().getEnglish()
						.getLocaleID())) {
					// root locale
					result.add(new CheckResult()
							.setStatus(ResultStatus.warning)
							.setLocale(locale)
							.setPath(path)
							.setMessage(
									"Locale {0}: Unexpected Attribute Value {1}={2}: expected: {3}; please add to supplemental data. Path: {4}",
									new Object[] { locale, attribute,
											attributeValue,
											matcherPattern.pattern, path }));
					matcherPattern.matcher = ObjectMatcherFactory
							.createOrMatcher(
									matcherPattern.matcher,
									ObjectMatcherFactory
											.createStringMatcher(attributeValue));
				} else {
					CheckResult cr=CheckResult.create(ResultStatus.error, locale, path, 
							new CheckCSVValuePred(), 
							"Locale {0}: Unexpected Attribute Value {1}={2}: expected: {3}", 
							"Locale {0}: Unexpected Attribute Value {1}={2}: expected: {3}  Path: {4}", 
							new Object[] { locale, attribute,attributeValue, matcherPattern.pattern}, 
							new Object[] { locale, attribute,attributeValue, matcherPattern.pattern, path });
					result.add(cr);
				}
			}
		}
	}
	
	
	private void handleCheck(String path, String fullPath, String value,
			List<CheckResult> result,CLDRFile fileToCheck) {

		if (fullPath == null) return; // skip paths that we don't have
		if (fullPath.indexOf('[') < 0) return; // skip paths with no attributes
		String locale = fileToCheck.getSourceLocaleID(path, null);

		pluralInfo = supplementalData.getPlurals(PluralType.cardinal, fileToCheck.getLocaleID());
		// skip paths that are not in the immediate locale
		if (!fileToCheck.getLocaleID().equals(locale)) {
			return;
		}
		parts.set(fullPath);
		for (int i = 0; i < parts.size(); ++i) {
			if (parts.getAttributeCount(i) == 0) continue;
			Map<String, String> attributes = parts.getAttributes(i);
			String element = parts.getElement(i);

			Map<String, MatcherPattern> attribute_validity = element_attribute_validity.get(element);
			for (Map.Entry<String, String> entry: attributes.entrySet()) {
				String attribute = entry.getKey();
				String attributeValue =entry.getValue();
				// check the common attributes first
				check(common_attribute_validity, attribute, attributeValue, result, fullPath,locale);
				// then for the specific element
				check(attribute_validity, attribute, attributeValue, result, fullPath,locale);

				// now for plurals

				if (attribute.equals("count")) {
					if (DIGITS.containsAll(attributeValue)) {
						// ok, keep going
					} else {
						final Count countValue = PluralInfo.Count.valueOf(attributeValue);
						if (!pluralInfo.getCounts().contains(countValue)
								&& !isPluralException(countValue, locale)) {
							result.add(
									new CheckResult()
										.setStatus(ResultStatus.error)
										.setPath(path)
										.setMessage("Locale {0}: Illegal plural value {1}; must be one of: {2} Path: {3}", 
												new Object[] { locale, countValue, pluralInfo.getCounts(), path }));
						}
					}
				}
			}
		}
	}
	
	public static void main(String[] args) {
		TestAttributeValues tav=new TestAttributeValues();
		tav.run(args);
	}
}
