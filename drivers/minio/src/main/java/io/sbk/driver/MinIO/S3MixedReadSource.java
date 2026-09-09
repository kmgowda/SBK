/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.driver.MinIO;

import java.util.Locale;

/** Selects the object source used by readers in mixed writer/reader runs. */
enum S3MixedReadSource {
    CATALOG,
    PUBLISHED;

    static S3MixedReadSource parse(String value) {
        if (value == null || value.isBlank()) {
            return CATALOG;
        }
        final String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (PUBLISHED.name().equals(normalized)) {
            throw new IllegalArgumentException("mixed-read-source published is disabled because "
                    + "it cannot currently guarantee balanced completion without adding MinIO "
                    + "writer hot-path overhead; use catalog");
        }
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "mixed-read-source must be catalog", ex);
        }
    }
}
