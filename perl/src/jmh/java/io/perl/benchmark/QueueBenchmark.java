/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perl.benchmark;

import io.perl.api.impl.CQueue;
import org.openjdk.jmh.annotations.AuxCounters;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Timeout;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * Compares PerL's MPSC queue with the JDK 25
 * {@link ConcurrentLinkedQueue}.
 *
 * <p>The round-trip benchmarks report the enqueue/dequeue cost in nanoseconds.
 * The grouped benchmarks use genuinely shared queue instances with four
 * producers and one consumer, the topology for which {@link CQueue} is
 * specialized. The {@code records} auxiliary counter reports successful
 * dequeues separately from empty poll attempts.</p>
 */
@Fork(value = 3)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Timeout(time = 30, timeUnit = TimeUnit.SECONDS)
public class QueueBenchmark {

    private static final Integer VALUE = 1;
    private static final int MPSC_DRAIN_BATCH = 8;

    /**
     * Thread-private state used to measure one enqueue/dequeue round trip.
     */
    @State(Scope.Thread)
    public static class RoundTripState {
        private CQueue<Integer> cqueue;
        private ConcurrentLinkedQueue<Integer> concurrentQueue;

        /**
         * Creates empty queues before each measurement iteration.
         */
        @Setup(Level.Iteration)
        public void setUp() {
            cqueue = new CQueue<>();
            concurrentQueue = new ConcurrentLinkedQueue<>();
        }
    }

    /**
     * Shared state for the four-producer, one-consumer CQueue benchmark.
     */
    @State(Scope.Group)
    public static class CQueueMpscState {
        private final CQueue<Integer> queue = new CQueue<>();

        /**
         * Releases any queued nodes after an iteration.
         */
        @TearDown(Level.Iteration)
        public void tearDown() {
            queue.clear();
        }
    }

    /**
     * Shared state for the four-producer, one-consumer JDK queue benchmark.
     */
    @State(Scope.Group)
    public static class JdkMpscState {
        private final ConcurrentLinkedQueue<Integer> queue = new ConcurrentLinkedQueue<>();

        /**
         * Releases any queued nodes after an iteration.
         */
        @TearDown(Level.Iteration)
        public void tearDown() {
            queue.clear();
        }
    }

    /**
     * Counts successful records and unsuccessful poll attempts.
     */
    @AuxCounters(AuxCounters.Type.EVENTS)
    @State(Scope.Thread)
    public static class PollCounters {
        /**
         * Number of records dequeued.
         */
        public long records;

        /**
         * Number of polls that found no record.
         */
        public long emptyPolls;

        /**
         * Resets the counters before each iteration.
         */
        @Setup(Level.Iteration)
        public void setUp() {
            records = 0;
            emptyPolls = 0;
        }
    }

    /**
     * Measures one CQueue enqueue/dequeue round trip.
     *
     * @param state thread-private queue state
     * @return dequeued value
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Integer cqueueRoundTrip(final RoundTripState state) {
        state.cqueue.add(VALUE);
        return state.cqueue.poll();
    }

    /**
     * Measures one JDK queue enqueue/dequeue round trip.
     *
     * @param state thread-private queue state
     * @return dequeued value
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Integer jdkQueueRoundTrip(final RoundTripState state) {
        state.concurrentQueue.add(VALUE);
        return state.concurrentQueue.poll();
    }

    /**
     * Enqueues from each producer in the CQueue 4P/1C group.
     *
     * @param state shared queue state
     * @return {@code true}
     */
    @Benchmark
    @Group("cqueueFourProducersOneConsumer")
    @GroupThreads(4)
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public boolean cqueueMpscProducer(final CQueueMpscState state) {
        return state.queue.add(VALUE);
    }

    /**
     * Drains a small batch in the CQueue 4P/1C group.
     *
     * @param state shared queue state
     * @param counters successful and empty poll counters
     * @return number of dequeued values in this batch
     */
    @Benchmark
    @Group("cqueueFourProducersOneConsumer")
    @GroupThreads(1)
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public int cqueueMpscConsumer(final CQueueMpscState state,
                                  final PollCounters counters) {
        int drained = 0;
        for (int index = 0; index < MPSC_DRAIN_BATCH; index++) {
            if (state.queue.poll() == null) {
                counters.emptyPolls++;
            } else {
                counters.records++;
                drained++;
            }
        }
        return drained;
    }

    /**
     * Enqueues from each producer in the JDK queue 4P/1C group.
     *
     * @param state shared queue state
     * @return {@code true}
     */
    @Benchmark
    @Group("jdkFourProducersOneConsumer")
    @GroupThreads(4)
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public boolean jdkMpscProducer(final JdkMpscState state) {
        return state.queue.add(VALUE);
    }

    /**
     * Drains a small batch in the JDK queue 4P/1C group.
     *
     * @param state shared queue state
     * @param counters successful and empty poll counters
     * @return number of dequeued values in this batch
     */
    @Benchmark
    @Group("jdkFourProducersOneConsumer")
    @GroupThreads(1)
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public int jdkMpscConsumer(final JdkMpscState state,
                               final PollCounters counters) {
        int drained = 0;
        for (int index = 0; index < MPSC_DRAIN_BATCH; index++) {
            if (state.queue.poll() == null) {
                counters.emptyPolls++;
            } else {
                counters.records++;
                drained++;
            }
        }
        return drained;
    }

    /**
     * Runs only the queue benchmarks from an IDE or command line.
     *
     * @param args ignored command-line arguments
     * @throws Exception if JMH cannot run the benchmark
     */
    public static void main(final String[] args) throws Exception {
        final Options options = new OptionsBuilder()
                .exclude("org.openjdk.jmh.benchmarks.*")
                .include("io.perl.benchmark.QueueBenchmark.*")
                .build();

        new Runner(options).run();
    }
}
