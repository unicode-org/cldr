package org.unicode.cldr.draft.keyboard;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Describes various platform dependent settings that are pertinent to the keyboard use.
 */
public final class KeyboardSettings {
    private final FallbackSetting fallbackSetting;
    private final TransformFailureSetting transformFailureSetting;
    private final TransformPartialSetting transformPartialSetting;

    private KeyboardSettings(FallbackSetting fallbackSetting,
        TransformFailureSetting transformFailureSetting,
        TransformPartialSetting transformPartialSetting) {
        this.fallbackSetting = checkNotNull(fallbackSetting);
        this.transformFailureSetting = checkNotNull(transformFailureSetting);
        this.transformPartialSetting = checkNotNull(transformPartialSetting);
    }

    /** Creates a keyboard settings object from the given options. */
    public static KeyboardSettings of(FallbackSetting fallbackSetting,
        TransformFailureSetting transformFailureSetting,
        TransformPartialSetting transformPartialSetting) {
        return new KeyboardSettings(fallbackSetting, transformFailureSetting, transformPartialSetting);
    }

    public FallbackSetting fallbackSetting() {
        return fallbackSetting;
    }

    public TransformFailureSetting transformFailureSetting() {
        return transformFailureSetting;
    }

    public TransformPartialSetting transformPartialSetting() {
        return transformPartialSetting;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("fallbackSetting", fallbackSetting)
            .add("transformFailureSetting", transformFailureSetting)
            .add("transformPartialSetting", transformPartialSetting)
            .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof KeyboardSettings) {
            KeyboardSettings other = (KeyboardSettings) o;
            return Objects.equal(fallbackSetting, other.fallbackSetting)
                && Objects.equal(transformFailureSetting, other.transformFailureSetting)
                && Objects.equal(transformPartialSetting, other.transformPartialSetting);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fallbackSetting, transformFailureSetting, transformPartialSetting);
    }

    /**
     * Describes the behavior of the system when a key press fails. It specifies what happens if there
     * is no mapping for a particular key for the given set of modifier keys. This setting is
     * completely platform dependent. NONE indicates the setting does not apply to the platform.
     */
    public enum FallbackSetting {
        BASE, OMIT, NONE;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    /**
     * Describes the behavior of the system when a transform fails. For example it specifies what
     * happens if a dead-key is pressed and the following key cannot be combined. This setting is
     * completely platform dependent. NONE indicates the setting does not apply to the platform.
     */
    public enum TransformFailureSetting {
        EMIT, OMIT, NONE;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    /**
     * Describes the behavior of the system while a transform is in progress. It specifies whether the
     * pressed keys are displayed or not. This setting is completely platform dependent. NONE
     * indicates the setting does not apply to the platform.
     */
    public enum TransformPartialSetting {
        HIDE, SHOW, NONE;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }
}
