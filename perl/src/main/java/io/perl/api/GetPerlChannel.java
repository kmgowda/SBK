/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perl.api;

/**
 * Capability interface for obtaining a dedicated {@link PerlChannel}. This is
 * used by PerL implementations to hand out per-thread channels to producers.
 *
 * <p>Callers should obtain one channel per thread and must not share a
 * {@code PerlChannel} instance between multiple threads.
 */
public sealed interface GetPerlChannel permits Channel, Perl {

    /**
     * Returns a dedicated {@link PerlChannel} instance for the caller to submit
     * benchmarking events.
     *
     * @return a thread-dedicated {@link PerlChannel}
     */
    PerlChannel getPerlChannel();

}
