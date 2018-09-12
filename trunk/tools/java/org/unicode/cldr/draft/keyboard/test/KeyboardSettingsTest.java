package org.unicode.cldr.draft.keyboard.test;

import org.unicode.cldr.draft.keyboard.KeyboardSettings;
import org.unicode.cldr.draft.keyboard.KeyboardSettings.FallbackSetting;
import org.unicode.cldr.draft.keyboard.KeyboardSettings.TransformFailureSetting;
import org.unicode.cldr.draft.keyboard.KeyboardSettings.TransformPartialSetting;

import com.ibm.icu.dev.test.TestFmwk;

public class KeyboardSettingsTest extends TestFmwk {

    public void testKeyboardSettings() {
        KeyboardSettings settings = KeyboardSettings.of(FallbackSetting.BASE,
            TransformFailureSetting.EMIT, TransformPartialSetting.SHOW);
        assertEquals("", FallbackSetting.BASE, settings.fallbackSetting());
        assertEquals("", TransformFailureSetting.EMIT, settings.transformFailureSetting());
        assertEquals("", TransformPartialSetting.SHOW, settings.transformPartialSetting());
    }

    public void testEqualsTrue() {
        KeyboardSettings settings1 = KeyboardSettings.of(FallbackSetting.BASE,
            TransformFailureSetting.EMIT, TransformPartialSetting.SHOW);
        KeyboardSettings settings2 = KeyboardSettings.of(FallbackSetting.BASE,
            TransformFailureSetting.EMIT, TransformPartialSetting.SHOW);
        assertTrue("", settings1.equals(settings2));
        assertTrue("", settings1.hashCode() == settings2.hashCode());
    }

    public void testEqualsFalse() {
        KeyboardSettings settings1 = KeyboardSettings.of(FallbackSetting.NONE,
            TransformFailureSetting.EMIT, TransformPartialSetting.SHOW);
        KeyboardSettings settings2 = KeyboardSettings.of(FallbackSetting.BASE,
            TransformFailureSetting.EMIT, TransformPartialSetting.SHOW);
        assertFalse("", settings1.equals(settings2));
    }
}
