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
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(
                    "mixed-read-source must be catalog or published", ex);
        }
    }
}
