/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perl.data;

/**
 * Utility constants for byte size conversions used across the PerL module.
 *
 * Note: {@link #BYTES_PER_GB} uses {@code long} to avoid overflow for large
 * multiplications.
 */
public final class Bytes {

    /** Number of bytes per kilobyte. */
    final public static int BYTES_PER_KB = 1024;

    /** Number of bytes per megabyte. */
    final public static int BYTES_PER_MB = BYTES_PER_KB * BYTES_PER_KB;

    /** Number of bytes per gigabyte. */
    final public static long BYTES_PER_GB = ((long) BYTES_PER_MB) * BYTES_PER_MB;
}
