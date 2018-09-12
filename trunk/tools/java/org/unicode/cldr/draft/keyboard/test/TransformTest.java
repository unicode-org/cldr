package org.unicode.cldr.draft.keyboard.test;

import org.unicode.cldr.draft.keyboard.Transform;

import com.ibm.icu.dev.test.TestFmwk;

public class TransformTest extends TestFmwk {

    public void testTransform() {
        Transform transform = Transform.of("^e", "ê");
        assertEquals("", "^e", transform.sequence());
        assertEquals("", "ê", transform.output());
    }

    public void testEqualsTrue() {
        Transform transform1 = Transform.of("^e", "ê");
        Transform transform2 = Transform.of("^e", "ê");

        assertTrue("", transform1.equals(transform2));
        assertTrue("", transform1.hashCode() == transform2.hashCode());
    }

    public void testEqualsFalse() {
        Transform transform1 = Transform.of("^e", "ê");
        Transform transform2 = Transform.of("e^", "ê");

        assertFalse("", transform1.equals(transform2));
        assertFalse("", transform1.hashCode() == transform2.hashCode());
    }
}
