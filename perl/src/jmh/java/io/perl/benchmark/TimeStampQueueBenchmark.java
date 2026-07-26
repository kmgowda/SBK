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

import io.perl.api.TimeStamp;
import io.perl.api.impl.ElasticWait;
import io.perl.api.impl.TimeStampMpscQueue;
import io.perl.api.TimeStampNode;
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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Compares PerL's complete timestamp publication paths.
 *
 * <p>The JDK path allocates a {@link TimeStamp} and then lets
 * {@link ConcurrentLinkedQueue} allocate its private linked node. The
 * intrusive path allocates one {@link TimeStampNode}, which is both payload
 * and link. The round-trip benchmark exposes latency and normalized allocation
 * per measurement. The grouped benchmarks reproduce PerL's four-producer,
 * one-consumer topology and expose producer throughput under contention. An
 * empty consumer parks briefly, as PerL's {@link ElasticWait} does, so empty
 * polling speed cannot inflate group throughput or starve producers.</p>
 */
@Fork(value = 3)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Timeout(time = 30, timeUnit = TimeUnit.SECONDS)
public class TimeStampQueueBenchmark {
    private static final int MPSC_DRAIN_BATCH = 8;
    /*
     * PerL parks after an empty channel sweep. A short equal delay keeps this
     * queue microbenchmark productive without rewarding empty-poll spinning.
     */
    private static final long MPSC_IDLE_NANOS = 1_000L;

    /**
     * Thread-private queues for end-to-end allocation and round-trip latency.
     */
    @State(Scope.Thread)
    public static class RoundTripState {
        private TimeStampMpscQueue intrusiveQueue;
        private ConcurrentLinkedQueue<TimeStamp> jdkQueue;
        private long sequence;

        /**
         * Creates empty queues before each measurement iteration.
         */
        @Setup(Level.Iteration)
        public void setUp() {
            intrusiveQueue = new TimeStampMpscQueue();
            jdkQueue = new ConcurrentLinkedQueue<>();
            sequence = 0;
        }
    }

    /**
     * Shared state for the intrusive four-producer, one-consumer benchmark.
     */
    @State(Scope.Group)
    public static class IntrusiveMpscState {
        private final TimeStampMpscQueue queue = new TimeStampMpscQueue();

        /**
         * Drains records remaining after an iteration.
         */
        @TearDown(Level.Iteration)
        public void tearDown() {
            queue.clear();
        }
    }

    /**
     * Shared state for the JDK four-producer, one-consumer benchmark.
     */
    @State(Scope.Group)
    public static class JdkMpscState {
        private final ConcurrentLinkedQueue<TimeStamp> queue =
                new ConcurrentLinkedQueue<>();

        /**
         * Drains records remaining after an iteration.
         */
        @TearDown(Level.Iteration)
        public void tearDown() {
            queue.clear();
        }
    }

    /**
     * Counts successful timestamp dequeues and empty polls.
     */
    @AuxCounters(AuxCounters.Type.EVENTS)
    @State(Scope.Thread)
    public static class PollCounters {
        /**
         * Successfully dequeued timestamps.
         */
        public long records;

        /**
         * Polls that observed an empty queue.
         */
        public long emptyPolls;

        /**
         * Resets counters before each iteration.
         */
        @Setup(Level.Iteration)
        public void setUp() {
            records = 0;
            emptyPolls = 0;
        }
    }

    /**
     * Measures allocation, enqueue, and dequeue on the intrusive path.
     *
     * @param state thread-private benchmark state
     * @return the dequeued node
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public TimeStampNode intrusiveRoundTrip(final RoundTripState state) {
        final long sequence = ++state.sequence;
        state.intrusiveQueue.add(
                new TimeStampNode(sequence, sequence + 1, 1, 100));
        return state.intrusiveQueue.poll();
    }

    /**
     * Measures allocation, enqueue, and dequeue on the JDK path.
     *
     * @param state thread-private benchmark state
     * @return the dequeued timestamp
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public TimeStamp jdkRoundTrip(final RoundTripState state) {
        final long sequence = ++state.sequence;
        state.jdkQueue.add(new TimeStamp(sequence, sequence + 1, 1, 100));
        return state.jdkQueue.poll();
    }

    /**
     * Publishes an intrusive timestamp from each MPSC producer.
     *
     * @param state shared intrusive queue state
     * @return {@code true}
     */
    @Benchmark
    @Group("intrusiveFourProducersOneConsumer")
    @GroupThreads(4)
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public boolean intrusiveProducer(final IntrusiveMpscState state) {
        return state.queue.add(new TimeStampNode(1, 2, 1, 100));
    }

    /**
     * Drains a small timestamp batch from the intrusive queue.
     *
     * @param state shared intrusive queue state
     * @param counters dequeue counters
     * @return number of timestamps drained
     */
    @Benchmark
    @Group("intrusiveFourProducersOneConsumer")
    @GroupThreads(1)
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public int intrusiveConsumer(final IntrusiveMpscState state,
                                 final PollCounters counters) {
        return drain(state.queue, counters);
    }

    /**
     * Publishes a timestamp from each JDK queue producer.
     *
     * @param state shared JDK queue state
     * @return {@code true}
     */
    @Benchmark
    @Group("jdkFourProducersOneConsumer")
    @GroupThreads(4)
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public boolean jdkProducer(final JdkMpscState state) {
        return state.queue.add(new TimeStamp(1, 2, 1, 100));
    }

    /**
     * Drains a small timestamp batch from the JDK queue.
     *
     * @param state shared JDK queue state
     * @param counters dequeue counters
     * @return number of timestamps drained
     */
    @Benchmark
    @Group("jdkFourProducersOneConsumer")
    @GroupThreads(1)
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public int jdkConsumer(final JdkMpscState state,
                           final PollCounters counters) {
        int drained = 0;
        for (int index = 0; index < MPSC_DRAIN_BATCH; index++) {
            if (state.queue.poll() == null) {
                counters.emptyPolls++;
                LockSupport.parkNanos(MPSC_IDLE_NANOS);
                break;
            } else {
                counters.records++;
                drained++;
            }
        }
        return drained;
    }

    private static int drain(TimeStampMpscQueue queue,
                             PollCounters counters) {
        int drained = 0;
        for (int index = 0; index < MPSC_DRAIN_BATCH; index++) {
            if (queue.poll() == null) {
                counters.emptyPolls++;
                LockSupport.parkNanos(MPSC_IDLE_NANOS);
                break;
            } else {
                counters.records++;
                drained++;
            }
        }
        return drained;
    }
}
