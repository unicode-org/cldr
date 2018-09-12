package org.unicode.cldr.draft.keyboard.test;

import java.io.PrintWriter;

import org.unicode.cldr.draft.keyboard.test.windows.KlcParserTest;

public final class RunTests {

    public static void main(String[] args) {
        PrintWriter printWriter = new PrintWriter(System.out);
        new CharacterMapTest().run(args, printWriter);
        new IsoLayoutPositionTest().run(args, printWriter);
        new KeyboardIdTest().run(args, printWriter);
        new KeyboardSettingsTest().run(args, printWriter);
        new KeyboardTest().run(args, printWriter);
        new KeycodeMapTest().run(args, printWriter);
        new KeyMapTest().run(args, printWriter);
        new ModifierKeyCombinationSetTest().run(args, printWriter);
        new ModifierKeyCombinationTest().run(args, printWriter);
        new ModifierKeySimplifierTest().run(args, printWriter);
        new ModifierKeyTest().run(args, printWriter);
        new TransformTest().run(args, printWriter);

        // Windows.
        new KlcParserTest().run(args, printWriter);
    }
}
