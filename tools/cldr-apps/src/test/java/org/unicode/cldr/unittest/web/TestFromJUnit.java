package org.unicode.cldr.unittest.web;

import org.junit.Test;
import org.junit.Assert;
import org.unicode.cldr.util.CLDRConfig;

public class TestFromJUnit {
    @Test
    public void testAll() {
        String args[] = System.getProperty("rununittest.arg", "-n -q").split(" ");
        Assert.assertNotNull("args from rununuttest.arg", args);
        CLDRConfig cc = CLDRConfig.getInstance();
        Assert.assertNotNull("CLDRConfig", cc);
        TestAll ta = new TestAll();
        Assert.assertNotNull("setTestLog", cc.setTestLog(ta));
        Assert.assertNotNull("System.out", System.out);
        java.io.PrintWriter pw = new java.io.PrintWriter(System.out);
        Assert.assertNotNull("pw", pw);
        int errCount = ta.run(args, pw);

        Assert.assertEquals("Error Count", 0, errCount);
    }
}
