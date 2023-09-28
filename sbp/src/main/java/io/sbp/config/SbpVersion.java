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

final public class SbpVersion {
    public final static int INVALID_VERSION = -1;
    public int major;
    public int minor;

    public  SbpVersion() {
        major = INVALID_VERSION;
        minor = INVALID_VERSION;
    }
}
