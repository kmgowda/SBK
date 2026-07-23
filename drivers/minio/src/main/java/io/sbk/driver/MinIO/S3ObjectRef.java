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

/**
 * Immutable reference to an S3 object discovered or created by the benchmark.
 *
 * @param key object key
 * @param versionId optional version identifier
 * @param size object size in bytes
 * @param createdTime benchmark time when a mixed-workload write began, or zero
 */
public record S3ObjectRef(String key, String versionId, long size, long createdTime) {
}
