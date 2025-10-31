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
 * Core PerL public interface representing a running performance benchmark
 * instance. Implementations provide mechanisms to obtain channels for
 * submitting measurements and to control the benchmark lifecycle.
 *
 * <p>Responsibilities:
 * <ul>
 *     <li>Provide per-thread {@link PerlChannel} instances via
 *     {@link GetPerlChannel#getPerlChannel()}</li>
 *     <li>Start and manage internal worker threads that aggregate latency and
 *     throughput metrics</li>
 *     <li>Expose a {@link #stop()} method to gracefully terminate the benchmark
 *     and flush any pending metrics.</li>
 * </ul>
 *
 * <p>Thread-safety:
 * <ul>
 *     <li>The {@link Perl} implementation itself is thread-safe for lifecycle
 *     operations (start/stop), but individual {@link PerlChannel} instances
 *     returned by {@link GetPerlChannel#getPerlChannel()} are not thread-safe
 *     and must only be used by a single thread.</li>
 * </ul>
 *
 * <p>Typical usage:
 * <pre>
 * {@code
 * Perl perl = PerlBuilder.build(logger, time, config, executor);
 * PerlChannel channel = perl.getPerlChannel();
 * perl.run(totalSeconds, totalRecords);
 * // use channel.send(...) from multiple threads (each thread uses its own channel)
 * perl.stop();
 * }
 * </pre>
 */
public non-sealed interface Perl extends RunBenchmark, GetPerlChannel {

    /**
     * Stop and shutdown the benchmark. Implementations should ensure all
     * pending metrics are flushed and any background threads are terminated.
     */
    void stop();
}
