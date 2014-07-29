package org.unicode.cldr.tool;


import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.tool.CheckHtmlFiles.MyOptions;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTool;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.XPathParts.Comments.CommentType;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.dev.util.TransliteratorUtilities;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

/**
 * By Steven R. Loomis (srl) thx Markus Scherer
 */
@CLDRTool(alias="generatedecompcollrules",description="based on decomposition, generate identical collation rules")
public class GenerateDecompCollationRules {
    
    private final static String RESET = "\u200E&";
    private final static String IDENTICAL = "\u200E=";
    private final static String COMMENT = "# ";
    private final static String NL = "\n";
    
    private static final Options myOptions = new Options(GenerateDecompCollationRules.class);
    
    enum MyOptions {
        unicodeset(".*", "[[:dt=init:][:dt=med:][:dt=fin:][:dt=iso:]]", "UnicodeSet of input chars"),
        verbose(null, null, "verbose debugging messages");

        // boilerplate
        final Option option;

        MyOptions(String argumentPattern, String defaultArgument, String helpText) {
            option = myOptions.add(this, argumentPattern, defaultArgument, helpText);
        }
    }

            
    public static void main(String[] args) throws IOException {
        final Transliterator hex = Transliterator.getInstance("any-hex");
        final Transliterator name = Transliterator.getInstance("any-name");
        myOptions.parse(MyOptions.verbose, args, true);
        final boolean verbose = myOptions.get(MyOptions.verbose).doesOccur();
        final CLDRConfig cldrConfig = CLDRConfig.getInstance();
        final Normalizer2 n = Normalizer2.getNFKDInstance();        
        UnicodeSet uSet;
        Option uSetOption = myOptions.get(MyOptions.unicodeset);
        final String uSetRules = uSetOption.doesOccur() ? uSetOption.getValue() : uSetOption.getDefaultArgument();
        System.out.println("UnicodeSet rules: " + uSetRules);
        try {
            uSet = new UnicodeSet(uSetRules);
        } catch(Throwable t) {
            t.printStackTrace();
            System.err.println("Failed to construct UnicodeSet from \""+uSetRules+"\" - see http://unicode.org/cldr/utility/list-unicodeset.jsp");
            return;
        }
        System.out.println("UnicodeSet size: " + uSet.size());
        
        final Relation<String,String> reg2pres = new Relation(new TreeMap<String, Set<String>>(), TreeSet.class);
        
        for(final String presForm : uSet) {
            final String regForm = n.normalize(presForm); 
            if(verbose) System.out.println("# " + presForm + " = "+hex.transliterate(presForm)+"... -> "+
                regForm +" = "+hex.transliterate(regForm));
            reg2pres.put(regForm, presForm);
        }
        System.out.println("Relation size: " + reg2pres.size());
        
        StringBuilder rules = new StringBuilder();
        
        for(final String regForm : reg2pres.keySet()) {
            final Set<String> presForms = reg2pres.get(regForm);
            
            
            rules.append(COMMENT)
                 .append(RESET)
                 .append(hex.transliterate(regForm));

            for(final String presForm : presForms) {
                rules.append(IDENTICAL)
                     .append(hex.transliterate(presForm));
            }
            rules.append(NL);

            rules.append(RESET)
                 .append(regForm);
            
            for(final String presForm : presForms) {
                rules.append(IDENTICAL)
                     .append(presForm);
            }
            rules.append(NL);
        }
        
        if(verbose) {
            System.out.println(rules);
        }
        
        // now, generate the output file
        XPathParts xpp = new XPathParts(null,null)
        .addElements(LDMLConstants.LDML,
                     LDMLConstants.COLLATIONS,
                     LDMLConstants.COLLATION,
                     "cr");
        // The following crashes. Bug #XXXX
        //xpp.setAttribute(-1, LDMLConstants.COLLATION, LDMLConstants.STANDARD);
        SimpleXMLSource xmlSource = new SimpleXMLSource("ar");
        CLDRFile newFile = new CLDRFile(xmlSource);
        newFile.add(xpp.toString(), "xyzzy");
        newFile.addComment(xpp.toString(), "Generated by " + GenerateDecompCollationRules.class.getSimpleName() +  " " + new java.util.Date() + "\n" +
            "from rules " + uSetRules + "\n", CommentType.PREBLOCK);
        final String filename = newFile.getLocaleID()+".xml";
        StringWriter sw = new StringWriter();
        newFile.write(new PrintWriter(sw));
        sw.close();
        try (PrintWriter w = 
                BagFormatter.openUTF8Writer(CLDRPaths.GEN_DIRECTORY, filename)) {
            w.print(sw.toString().replace("xyzzy", 
                    "<![CDATA[\n" + 
                    rules.toString() +
                    "\n" + "]]>"));
            //newFile.write(w);
            System.out.println("Wrote to " + CLDRPaths.GEN_DIRECTORY +"/"+filename );
        }

    }
}
