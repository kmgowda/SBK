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

/** Storage Benchmark Protocol major and minor version. */
final public class SbpVersion {
    /** Value used when no valid version has been loaded. */
    public final static int INVALID_VERSION = -1;
    /** Protocol major version. */
    public int major;
    /** Protocol minor version. */
    public int minor;

    /** Creates an invalid version that can be populated by property binding. */
    public  SbpVersion() {
        major = INVALID_VERSION;
        minor = INVALID_VERSION;
    }
}
