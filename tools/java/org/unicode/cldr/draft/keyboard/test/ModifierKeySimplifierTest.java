package org.unicode.cldr.draft.keyboard.test;

import java.util.EnumSet;

import org.unicode.cldr.draft.keyboard.ModifierKey;
import org.unicode.cldr.draft.keyboard.ModifierKeyCombination;
import org.unicode.cldr.draft.keyboard.ModifierKeySimplifier;

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.test.TestFmwk;

public class ModifierKeySimplifierTest extends TestFmwk {
    private static ImmutableSet<ModifierKey> EMPTY_SET = ImmutableSet.<ModifierKey> of();

    public void testSimplifyInputWithOverlappingSets() {
        try {
            ModifierKeySimplifier.simplifyInput(
                ImmutableSet.of(ModifierKey.ALT_LEFT, ModifierKey.CAPSLOCK),
                ImmutableSet.of(ModifierKey.ALT_LEFT));
            fail();
        } catch (IllegalArgumentException e) {
            // Expected.
        }
    }

    public void testSimplifyInputWithBase() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnKeys(EMPTY_SET);
        assertTrue("", combination.onKeys().isEmpty());
        ImmutableSet<ModifierKey> offKeys = ImmutableSet.of(ModifierKey.CONTROL, ModifierKey.ALT,
            ModifierKey.OPTION, ModifierKey.SHIFT, ModifierKey.COMMAND, ModifierKey.CAPSLOCK);
        assertEquals("", offKeys, combination.offKeys());
    }

    public void testSimplifyInputWithParentOn() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(
            ModifierKey.ALT, ModifierKey.CAPSLOCK));
        ImmutableSet<ModifierKey> onKeys = ImmutableSet.of(ModifierKey.ALT, ModifierKey.CAPSLOCK);
        assertEquals("", onKeys, combination.onKeys());
        ImmutableSet<ModifierKey> offKeys = ImmutableSet.of(ModifierKey.CONTROL, ModifierKey.OPTION,
            ModifierKey.SHIFT, ModifierKey.COMMAND);
        assertEquals("", offKeys, combination.offKeys());
    }

    public void testSimplifyInputWithChildOn() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(
            ModifierKey.OPTION_LEFT, ModifierKey.COMMAND));
        ImmutableSet<ModifierKey> onKeys = ImmutableSet.of(ModifierKey.OPTION_LEFT, ModifierKey.COMMAND);
        assertEquals("", onKeys, combination.onKeys());
        ImmutableSet<ModifierKey> offKeys = ImmutableSet.of(ModifierKey.ALT, ModifierKey.CONTROL,
            ModifierKey.OPTION_RIGHT, ModifierKey.SHIFT, ModifierKey.CAPSLOCK);
        assertEquals("", offKeys, combination.offKeys());
    }

    public void testSimplifyInputWithBothChildrenOn() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(
            ModifierKey.SHIFT_LEFT, ModifierKey.SHIFT_RIGHT));
        ImmutableSet<ModifierKey> onKeys = ImmutableSet.of(ModifierKey.SHIFT_LEFT,
            ModifierKey.SHIFT_RIGHT);
        assertEquals("", onKeys, combination.onKeys());
        ImmutableSet<ModifierKey> offKeys = ImmutableSet.of(ModifierKey.ALT, ModifierKey.CONTROL,
            ModifierKey.OPTION, ModifierKey.CAPSLOCK, ModifierKey.COMMAND);
        assertEquals("", offKeys, combination.offKeys());
    }

    public void testSimplifyInputWithParentDontCare() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnAndDontCareKeys(EMPTY_SET,
            ImmutableSet.of(ModifierKey.SHIFT));
        assertEquals("", EMPTY_SET, combination.onKeys());
        ImmutableSet<ModifierKey> offKeys = ImmutableSet.of(ModifierKey.ALT, ModifierKey.CONTROL,
            ModifierKey.OPTION, ModifierKey.CAPSLOCK, ModifierKey.COMMAND);
        assertEquals("", offKeys, combination.offKeys());
    }

    public void testSimplifyInputWithParentDontCareAndChildOn() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.ALT_LEFT), ImmutableSet.of(ModifierKey.ALT));
        ImmutableSet<ModifierKey> onKeys = ImmutableSet.of(ModifierKey.ALT_LEFT);
        assertEquals("", onKeys, combination.onKeys());
        ImmutableSet<ModifierKey> offKeys = ImmutableSet.of(ModifierKey.SHIFT, ModifierKey.CONTROL,
            ModifierKey.OPTION, ModifierKey.CAPSLOCK, ModifierKey.COMMAND);
        assertEquals("", offKeys, combination.offKeys());
    }

    public void testSimplifyInputWithChildDontCare() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnAndDontCareKeys(EMPTY_SET,
            ImmutableSet.of(ModifierKey.CONTROL_RIGHT));
        assertEquals("", EMPTY_SET, combination.onKeys());
        ImmutableSet<ModifierKey> offKeys = ImmutableSet.of(ModifierKey.SHIFT,
            ModifierKey.CONTROL_LEFT, ModifierKey.ALT, ModifierKey.OPTION, ModifierKey.CAPSLOCK,
            ModifierKey.COMMAND);
        assertEquals("", offKeys, combination.offKeys());
    }

    public void testSimplifyInputWithAllThreeKeysPresent() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.CONTROL, ModifierKey.CONTROL_LEFT),
            ImmutableSet.of(ModifierKey.CONTROL_RIGHT));
        ImmutableSet<ModifierKey> onKeys = ImmutableSet.of(ModifierKey.CONTROL_LEFT);
        assertEquals("", onKeys, combination.onKeys());
        ImmutableSet<ModifierKey> offKeys = ImmutableSet.of(ModifierKey.SHIFT, ModifierKey.ALT,
            ModifierKey.OPTION, ModifierKey.CAPSLOCK, ModifierKey.COMMAND);
        assertEquals("", offKeys, combination.offKeys());
    }

    public void testSimplifyToStringWithBase() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnKeys(EMPTY_SET);
        assertEquals("", "", ModifierKeySimplifier.simplifyToString(combination));
    }

    public void testSimplifyToStringWithParentOn() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(
            ModifierKey.ALT, ModifierKey.CAPSLOCK));
        assertEquals("", "alt+caps", ModifierKeySimplifier.simplifyToString(combination));
    }

    public void testSimplifyToStringWithChildOn() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(
            ModifierKey.OPTION_LEFT, ModifierKey.COMMAND));
        assertEquals("", "cmd+optL", ModifierKeySimplifier.simplifyToString(combination));
    }

    public void testSimplifyToStringWithBothChildrenOn() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(
            ModifierKey.SHIFT_LEFT, ModifierKey.SHIFT_RIGHT));
        assertEquals("", "shiftL+shiftR", ModifierKeySimplifier.simplifyToString(combination));
    }

    public void testSimplifyToStringWithParentDontCare() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnAndDontCareKeys(EMPTY_SET,
            ImmutableSet.of(ModifierKey.SHIFT));
        assertEquals("", "shift?", ModifierKeySimplifier.simplifyToString(combination));
    }

    public void testSimplifyToStringWithChildDontCareAndOn() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.SHIFT_LEFT), ImmutableSet.of(ModifierKey.SHIFT_RIGHT));
        assertEquals("", "shiftL+shiftR?", ModifierKeySimplifier.simplifyToString(combination));
    }

    public void testSimplifyToStringWithChildDontCare() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnAndDontCareKeys(EMPTY_SET,
            ImmutableSet.of(ModifierKey.OPTION_RIGHT));
        assertEquals("", "optR?", ModifierKeySimplifier.simplifyToString(combination));
    }

    public void testSimplifyToStringWithComplexCombination() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.CONTROL, ModifierKey.ALT_RIGHT, ModifierKey.COMMAND),
            ImmutableSet.of(ModifierKey.OPTION, ModifierKey.CAPSLOCK, ModifierKey.SHIFT_LEFT));
        assertEquals("", "cmd+ctrl+altR+opt?+caps?+shiftL?",
            ModifierKeySimplifier.simplifyToString(combination));
    }

    public void testSimplifySetWithIdentical() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.CONTROL, ModifierKey.CAPSLOCK),
            ImmutableSet.of(ModifierKey.SHIFT_RIGHT));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.CONTROL, ModifierKey.CAPSLOCK),
            ImmutableSet.of(ModifierKey.SHIFT_RIGHT));
        ImmutableSet<ModifierKeyCombination> result = ModifierKeySimplifier.simplifySet(ImmutableSet.of(
            combination1, combination2));
        assertEquals("", 1, result.size());
        assertEquals("", ImmutableSet.of(combination1), result);
    }

    public void testSimplifySetWithSingleOnKey() {
        // altR+shift+caps
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(
            ModifierKey.SHIFT, ModifierKey.ALT_RIGHT, ModifierKey.CAPSLOCK));
        // altR+shift
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(
            ModifierKey.SHIFT, ModifierKey.ALT_RIGHT));
        // 1 + 0 = ?
        ImmutableSet<ModifierKeyCombination> result = ModifierKeySimplifier.simplifySet(ImmutableSet.of(
            combination1, combination2));
        // altR+shift+caps?
        ModifierKeyCombination resultCombination = ModifierKeyCombination.ofOnAndDontCareKeys(
            EnumSet.of(ModifierKey.SHIFT, ModifierKey.ALT_RIGHT), EnumSet.of(ModifierKey.CAPSLOCK));
        assertEquals("", ImmutableSet.of(resultCombination), result);
    }

    public void testSimplifySetWithParentOnKey() {
        // ctrl+altR+shift
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnKeys(EnumSet.of(
            ModifierKey.SHIFT, ModifierKey.ALT_RIGHT, ModifierKey.CONTROL));
        // altR+shift
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnKeys(EnumSet.of(
            ModifierKey.SHIFT, ModifierKey.ALT_RIGHT));
        // 1?? + 0?? = ???
        ImmutableSet<ModifierKeyCombination> result = ModifierKeySimplifier.simplifySet(ImmutableSet.of(
            combination1, combination2));
        // altR+shift+ctrl?
        ModifierKeyCombination resultCombination = ModifierKeyCombination.ofOnAndDontCareKeys(
            EnumSet.of(ModifierKey.SHIFT, ModifierKey.ALT_RIGHT), EnumSet.of(ModifierKey.CONTROL));
        assertEquals("", ImmutableSet.of(resultCombination), result);
    }

    public void testSimplifySetWithSingleOffKey() {
        // altR+shift
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnKeys(EnumSet.of(
            ModifierKey.SHIFT, ModifierKey.ALT_RIGHT));
        // altR+shift+cmd?
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnAndDontCareKeys(
            EnumSet.of(ModifierKey.SHIFT, ModifierKey.ALT_RIGHT), EnumSet.of(ModifierKey.COMMAND));
        // 0 + ? = ?
        ImmutableSet<ModifierKeyCombination> result = ModifierKeySimplifier.simplifySet(ImmutableSet.of(
            combination1, combination2));
        // altR+shift+cmd?
        ModifierKeyCombination resultCombination = ModifierKeyCombination.ofOnAndDontCareKeys(
            EnumSet.of(ModifierKey.SHIFT, ModifierKey.ALT_RIGHT), EnumSet.of(ModifierKey.COMMAND));
        assertEquals("", ImmutableSet.of(resultCombination), result);
    }

    public void testSimplifySetWithParentOffKey() {
        // altR+shift
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnKeys(EnumSet.of(
            ModifierKey.SHIFT, ModifierKey.ALT_RIGHT));
        // altR+shift+opt?
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnAndDontCareKeys(
            EnumSet.of(ModifierKey.SHIFT, ModifierKey.ALT_RIGHT), EnumSet.of(ModifierKey.OPTION));
        // 0?? + ??? = ???
        ImmutableSet<ModifierKeyCombination> result = ModifierKeySimplifier.simplifySet(ImmutableSet.of(
            combination1, combination2));
        // altR+shift+opt?
        ModifierKeyCombination resultCombination = ModifierKeyCombination.ofOnAndDontCareKeys(
            EnumSet.of(ModifierKey.SHIFT, ModifierKey.ALT_RIGHT), EnumSet.of(ModifierKey.OPTION));
        assertEquals("", ImmutableSet.of(resultCombination), result);
    }

    public void testSimplifySetWithChildOnKey() {
        // altR+shift+ctrlL
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnKeys(EnumSet.of(
            ModifierKey.SHIFT, ModifierKey.ALT_RIGHT, ModifierKey.CONTROL_LEFT));
        // altR+shift
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnKeys(EnumSet.of(
            ModifierKey.SHIFT, ModifierKey.ALT_RIGHT));
        // ?10 + 0?? = ??0
        ImmutableSet<ModifierKeyCombination> result = ModifierKeySimplifier.simplifySet(ImmutableSet.of(
            combination1, combination2));
        // altR+shift+ctrlL?
        ModifierKeyCombination resultCombination = ModifierKeyCombination.ofOnAndDontCareKeys(
            EnumSet.of(ModifierKey.SHIFT, ModifierKey.ALT_RIGHT), EnumSet.of(ModifierKey.CONTROL_LEFT));
        assertEquals("", ImmutableSet.of(resultCombination), result);
    }

    public void testSimplifySetWithChildOffKey() {
        // altR+shift
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnKeys(EnumSet.of(
            ModifierKey.SHIFT, ModifierKey.ALT_RIGHT));
        // altR+shift+ctrlL?
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnAndDontCareKeys(
            EnumSet.of(ModifierKey.SHIFT, ModifierKey.ALT_RIGHT), EnumSet.of(ModifierKey.CONTROL_LEFT));
        // 0?? + ??0 = ??0
        ImmutableSet<ModifierKeyCombination> result = ModifierKeySimplifier.simplifySet(ImmutableSet.of(
            combination1, combination2));
        // altR+shift+ctrlL?
        ModifierKeyCombination resultCombination = ModifierKeyCombination.ofOnAndDontCareKeys(
            EnumSet.of(ModifierKey.SHIFT, ModifierKey.ALT_RIGHT), EnumSet.of(ModifierKey.CONTROL_LEFT));
        assertEquals("", ImmutableSet.of(resultCombination), result);
    }

    public void testSimplifySetWithTwoDifferentChildrenDontCare() {
        // altR+shift+ctrlR?
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnAndDontCareKeys(
            EnumSet.of(ModifierKey.SHIFT, ModifierKey.ALT_RIGHT), EnumSet.of(ModifierKey.CONTROL_RIGHT));
        // altR+shift+ctrlL?
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnAndDontCareKeys(
            EnumSet.of(ModifierKey.SHIFT, ModifierKey.ALT_RIGHT), EnumSet.of(ModifierKey.CONTROL_LEFT));
        // ??0 + ?0? = ??0 + ?0?
        ImmutableSet<ModifierKeyCombination> result = ModifierKeySimplifier.simplifySet(ImmutableSet.of(
            combination1, combination2));
        // altR+shift+ctrlL? altR+shift+ctrlR
        assertEquals("", ImmutableSet.of(combination1, combination2), result);
    }

    public void testSimplifySetWithTwoDifferentChildrenOn() {
        // altR+shift+ctrlR?
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnKeys(EnumSet.of(
            ModifierKey.SHIFT, ModifierKey.ALT_RIGHT, ModifierKey.CONTROL_RIGHT));
        // altR+shift+ctrlL?
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnKeys(EnumSet.of(
            ModifierKey.SHIFT, ModifierKey.ALT_RIGHT, ModifierKey.CONTROL_LEFT));
        // ?01 + ?10 = ?01 + ?10
        ImmutableSet<ModifierKeyCombination> result = ModifierKeySimplifier.simplifySet(ImmutableSet.of(
            combination1, combination2));
        // ctrlL+altR+shift ctrlR+altR+shift
        assertEquals("", ImmutableSet.of(combination1, combination2), result);
    }

    public void testSimplifySetWithParentDontCareAndChildOn() {
        // altR+shift+opt?
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnAndDontCareKeys(
            EnumSet.of(ModifierKey.SHIFT, ModifierKey.ALT_RIGHT), EnumSet.of(ModifierKey.OPTION));
        // altR+shift+optR
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnKeys(EnumSet.of(
            ModifierKey.SHIFT, ModifierKey.ALT_RIGHT, ModifierKey.OPTION_RIGHT));
        // ??? + ?01 = ???
        ImmutableSet<ModifierKeyCombination> result = ModifierKeySimplifier.simplifySet(ImmutableSet.of(
            combination1, combination2));
        // altR+shift+opt?
        ModifierKeyCombination resultCombination = ModifierKeyCombination.ofOnAndDontCareKeys(
            EnumSet.of(ModifierKey.SHIFT, ModifierKey.ALT_RIGHT), EnumSet.of(ModifierKey.OPTION));
        assertEquals("", ImmutableSet.of(resultCombination), result);
    }

    public void testSimplifySetWithParentDontCareAndChildDontCare() {
        // altR+shift+opt?
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnAndDontCareKeys(
            EnumSet.of(ModifierKey.SHIFT, ModifierKey.ALT_RIGHT), EnumSet.of(ModifierKey.OPTION));
        // altR+shift+optR?
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnAndDontCareKeys(
            EnumSet.of(ModifierKey.SHIFT, ModifierKey.ALT_RIGHT), EnumSet.of(ModifierKey.OPTION_RIGHT));
        // ??? + ?0? = ???
        ImmutableSet<ModifierKeyCombination> result = ModifierKeySimplifier.simplifySet(ImmutableSet.of(
            combination1, combination2));
        // altR+shift+opt?
        ModifierKeyCombination resultCombination = ModifierKeyCombination.ofOnAndDontCareKeys(
            EnumSet.of(ModifierKey.SHIFT, ModifierKey.ALT_RIGHT), EnumSet.of(ModifierKey.OPTION));
        assertTrue("", result.contains(resultCombination));
    }

    public void testSimplifySetWithNoSimplification() {
        // ctrl+shift+caps+opt?
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnAndDontCareKeys(
            EnumSet.of(ModifierKey.CONTROL, ModifierKey.SHIFT, ModifierKey.CAPSLOCK),
            EnumSet.of(ModifierKey.OPTION));
        // altR+shift+cmd?
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnAndDontCareKeys(
            EnumSet.of(ModifierKey.SHIFT, ModifierKey.ALT_RIGHT), EnumSet.of(ModifierKey.COMMAND));
        ImmutableSet<ModifierKeyCombination> result = ModifierKeySimplifier.simplifySet(ImmutableSet.of(
            combination1, combination2));
        assertEquals("", ImmutableSet.of(combination1, combination2), result);
    }
}
