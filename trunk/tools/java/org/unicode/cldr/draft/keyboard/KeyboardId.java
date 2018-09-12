package org.unicode.cldr.draft.keyboard;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.unicode.cldr.draft.keyboard.KeyboardSettings.FallbackSetting;
import org.unicode.cldr.draft.keyboard.KeyboardSettings.TransformFailureSetting;
import org.unicode.cldr.draft.keyboard.KeyboardSettings.TransformPartialSetting;

import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.ibm.icu.util.ULocale;

/**
 * An object that is used to uniquely identify a particular keyboard. This object can be serialized
 * as a string. The string has the following format:
 * {@code <locale>-t-k0-<platform>-<attribute0>-<attribute1>-<attributeN>}
 * 
 * <p>
 * The locale and platform tags are mandatory, the attributes are not.
 * 
 * <p>
 * The following are all valid keyboard locale strings:
 * <ul>
 * <li>bn-t-k0-windows.xml</li>
 * <li>de-BE-t-k0-windows-var.xml</li>
 * <li>fi-t-k0-osx-extended-var.xml</li>
 * <li>es-US-t-k0-android-768dpi.xml</li>
 * </ul>
 */
public final class KeyboardId {
    private final ULocale locale;
    private final Platform platform;
    private final ImmutableList<String> attributes;

    private KeyboardId(ULocale locale, Platform platform, ImmutableList<String> attributes) {
        this.locale = checkNotNull(locale);
        this.platform = checkNotNull(platform);
        this.attributes = checkNotNull(attributes);
    }

    /** Creates a keyboard id from the given locale, platform and attributes. */
    public static KeyboardId of(ULocale locale, Platform platform, ImmutableList<String> attributes) {
        return new KeyboardId(locale, platform, attributes);
    }

    /**
     * Creates a keyboard id from the given string. See class documentation for information on the
     * required format of the string.
     */
    public static KeyboardId fromString(String keyboardLocale) {
        int tExtensionLocation = keyboardLocale.indexOf("-t-k0-");
        checkArgument(tExtensionLocation != -1, keyboardLocale);
        String localeString = keyboardLocale.substring(0, tExtensionLocation);
        ULocale locale = ULocale.forLanguageTag(localeString);
        String[] attributeStrings = keyboardLocale.substring(tExtensionLocation + 6).split("-");
        checkArgument(attributeStrings.length > 0, keyboardLocale);
        Platform platform = Platform.fromString(attributeStrings[0]);
        ImmutableList<String> attributes = attributeStrings.length > 1
            ? ImmutableList.copyOf(attributeStrings).subList(1, attributeStrings.length)
            : ImmutableList.<String> of();
        return new KeyboardId(locale, platform, attributes);
    }

    /** Returns the keyboard's locale. */
    public ULocale locale() {
        return locale;
    }

    /** Returns the keyboard's platform. */
    public Platform platform() {
        return platform;
    }

    /** Returns the list of additional attributes associated with the keyboard (if any). */
    public ImmutableList<String> attributes() {
        return attributes;
    }

    private static final Joiner DASH_JOINER = Joiner.on("-");

    @Override
    public String toString() {
        ImmutableList.Builder<String> components = ImmutableList.builder();
        // We want to use dashes within the locale as opposed to underscores.
        components.add(locale.toString().replace("_", "-"));
        components.add("t-k0");
        components.add(platform.toString());
        components.addAll(FluentIterable.from(attributes).transform(Functions.toStringFunction()));
        return DASH_JOINER.join(components.build());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof KeyboardId) {
            KeyboardId other = (KeyboardId) o;
            return Objects.equal(locale, other.locale) && Objects.equal(platform, other.platform)
                && Objects.equal(attributes, other.attributes);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(locale, platform, attributes);
    }

    /** The current set of platforms supported. */
    public enum Platform {
        ANDROID(4.4f, KeyboardSettings.of(FallbackSetting.NONE, TransformFailureSetting.NONE,
            TransformPartialSetting.NONE)), CHROMEOS(33f, KeyboardSettings.of(FallbackSetting.BASE, TransformFailureSetting.OMIT,
                TransformPartialSetting.HIDE)), OSX(10.9f, KeyboardSettings.of(FallbackSetting.BASE, TransformFailureSetting.EMIT,
                    TransformPartialSetting.SHOW)), WINDOWS(10f, KeyboardSettings.of(FallbackSetting.OMIT, TransformFailureSetting.EMIT,
                        TransformPartialSetting.HIDE));

        private final float version;
        private final KeyboardSettings settings;

        private Platform(float version, KeyboardSettings settings) {
            this.version = version;
            this.settings = checkNotNull(settings);
            checkArgument(version >= 0);
        }

        public double version() {
            return version;
        }

        public KeyboardSettings settings() {
            return settings;
        }

        @Override
        public String toString() {
            return name().toLowerCase();
        }

        /**
         * Retrieves the enum value for the given string. Throws an illegal argument exception if the
         * given string does not correspond to an enum value.
         */
        private static Platform fromString(String platform) {
            Platform value = Platform.valueOf(platform.toUpperCase());
            checkArgument(platform != null, platform);
            return value;
        }
    }
}
