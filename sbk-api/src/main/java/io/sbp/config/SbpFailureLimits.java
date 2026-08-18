/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbp.config;

/** Shared SBP failure-diagnostic wire limits used by clients and servers. */
public final class SbpFailureLimits {
    /** Maximum component-name characters accepted by SBM. */
    public static final int COMPONENT_CHARACTERS = 64;
    /** Maximum failure-message characters accepted by SBM. */
    public static final int MESSAGE_CHARACTERS = 4096;
    /** Prefix retained when an oversized diagnostic is truncated. */
    public static final int MESSAGE_PREFIX_CHARACTERS = 3072;
    /** Marker inserted between retained diagnostic prefix and suffix. */
    public static final String TRUNCATION_MARKER = " ... [truncated] ... ";

    private SbpFailureLimits() {
    }
}
