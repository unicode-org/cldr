package org.unicode.cldr.unittest;

import org.unicode.cldr.tool.GenerateTransform;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class GenerateTransformTest extends TestFmwk {

  public static void main(String[] args) {
    new GenerateTransformTest().run(args);
  }

  public void TestBasic() {
    GenerateTransform gen = new GenerateTransform();
    try {
      //gen.addFromCldrFile("Serbian-Latin-BGN", Transliterator.FORWARD);
      gen.addFromCldrFile("Syriac-Latin", Transliterator.FORWARD);
//    georgian = new UnicodeSet("[\u10d0-\u10f0]";
//    Transliterator trans = Transliterator.getInstance("Georgian-Latin");
//    UnicodeSet sourceSet = trans.getSourceSet();
//    for (UnicodeSetIterator it = new UnicodeSetIterator(sourceSet); it.next();) {
//    String source = it.getString();
//    gen.add(source, trans.transform(source));
//    }
//    gen.addFallback("{c}[iey]", "s");
//    gen.addFallback("c", "k");
//    gen.addFallback("f", "v");
//    gen.addFallback("w", "u");
//    gen.addFallback("x", "ks");
//    gen.addFallback("y", "i");
      System.out.println(gen.toRules(new UnicodeSet("[:script=Cyrl:]"), new UnicodeSet("[a-z]")));
    } catch (RuntimeException e) {
      e.printStackTrace();
      throw e;
    }
  }

}
