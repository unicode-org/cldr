package org.unicode.cldr.unittest;

import java.util.Date;

import com.ibm.icu.dev.test.TestFmwk;

public class TestTimeEnd extends TestFmwk {
    public static void main(String[] args) {
        new TestTimeEnd().run(args);
    }
    
    public void TestRecordTime() {
       long theTime=System.currentTimeMillis();
       
       System.out.println(" Testing ended: Timestamp: "+new Date()+" Current time millis: " +theTime);
    }
}
