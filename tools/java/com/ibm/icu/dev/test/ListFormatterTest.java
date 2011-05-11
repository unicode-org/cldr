package com.ibm.icu.dev.test;

import com.ibm.icu.text.ListFormatter;

public class ListFormatterTest extends TestFmwk {
    public static void main(String[] args) {
        new ListFormatterTest().run(args);
    }
    
    public void TestBasic() {
        ListFormatter formatter = new ListFormatter("{0} and {1}", "{0}; {1}", "{0}, {1}", "{0}, and {1}");
        assertEquals("2", "A and B", formatter.format("A", "B"));
        assertEquals("3", "A; B, and C", formatter.format("A", "B", "C"));
        assertEquals("4", "A; B, C, and D", formatter.format("A", "B", "C", "D"));
        assertEquals("5", "A; B, C, D, and E", formatter.format("A", "B", "C", "D", "E"));
    }
}
