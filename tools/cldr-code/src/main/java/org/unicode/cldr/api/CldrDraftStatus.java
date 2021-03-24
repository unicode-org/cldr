package org.unicode.cldr.api;

import java.util.Optional;

import org.unicode.cldr.util.CLDRFile.DraftStatus;

import com.google.common.base.Ascii;

/**
 * Draft status for controlling which values to visit. Draft statuses are ordered by ordinal value,
 * with {@code UNCONFIRMED} being the most lenient status (include everything) and {@code APPROVED}
 * being the strictest.
 *
 * <p>This enum is largely a wrapper for functionality found in the underlying CLDR classes, but
 * repackaged for convenience and to minimize surface area (and to avoid anyone needing to import
 * classes from outside the "api" package).
 */
public enum CldrDraftStatus {
    /** Include all values during visitation, performing no filtering. */
    UNCONFIRMED(DraftStatus.unconfirmed),
    /** Include only values with "provisional" status or higher during visitation. */
    PROVISIONAL(DraftStatus.provisional),
    /** Include only values with "contributed" status or higher during visitation. */
    CONTRIBUTED(DraftStatus.contributed),
    /** Include only "approved" values. */
    APPROVED(DraftStatus.approved);

    private final DraftStatus rawStatus;
    private final Optional<CldrDraftStatus> optStatus;

    CldrDraftStatus(DraftStatus rawType) {
        this.rawStatus = rawType;
        this.optStatus = Optional.of(this);
    }

    DraftStatus getRawStatus() {
        return rawStatus;
    }

    Optional<CldrDraftStatus> asOptional() {
        return optStatus;
    }

    static /* @Nullable */ CldrDraftStatus forString(/* @Nullable */ String name) {
        return name != null ? CldrDraftStatus.valueOf(Ascii.toUpperCase(name)) : null;
    }
}
