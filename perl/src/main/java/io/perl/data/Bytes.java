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
 * Class Bytes.
 */
public final class Bytes {

    /**
     * <code>BYTES_PER_KB = 1024</code>.
     */
    final public static int BYTES_PER_KB = 1024;

    /**
     * <code>BYTES_PER_MB = BYTES_PER_KB * BYTES_PER_KB</code>.
     */
    final public static int BYTES_PER_MB = BYTES_PER_KB * BYTES_PER_KB;

    /**
     * <code>BYTES_PER_GB = ((long) BYTES_PER_MB) * BYTES_PER_MB</code>.
     */
    final public static long BYTES_PER_GB = ((long) BYTES_PER_MB) * BYTES_PER_MB;
}
