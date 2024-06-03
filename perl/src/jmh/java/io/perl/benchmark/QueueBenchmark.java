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

import io.perl.api.impl.AtomicQueue;
import io.perl.api.impl.CQueue;
import io.perl.api.impl.SyncQueue;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Timeout;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;


@State(Scope.Thread)
//@Fork(value = 2, jvmArgs = {"-Djmh.blackhole.autoDetect=false"})
public class QueueBenchmark {

    final protected CQueue<Integer> cqueue;
    final protected ConcurrentLinkedQueue<Integer> clinkedQueue;
    final protected LinkedBlockingQueue<Integer> linkedbq;
    final protected AtomicQueue<Integer> atomicQueue;
    final protected SyncQueue<Integer> syncQueue;

    
    public QueueBenchmark() {
        cqueue = new CQueue<>();
        clinkedQueue = new ConcurrentLinkedQueue<>();
        linkedbq = new LinkedBlockingQueue<>();
        atomicQueue = new AtomicQueue<>();
        syncQueue = new SyncQueue<>();
    }
    
    @Benchmark
    @Fork(value = 1, warmups = 0)
    @Timeout(time = 60)
    @Warmup(iterations = 0)
    @Measurement(iterations = 3)
    @BenchmarkMode(Mode.Throughput)
    public void cqueueBenchmark() {
        cqueue.add(1);
        cqueue.poll();
    }

    @Benchmark
    @Fork(value = 1, warmups = 0)
    @Timeout(time = 60)
    @Warmup(iterations = 0)
    @Measurement(iterations = 3)
    @BenchmarkMode(Mode.Throughput)
    public void concurrentQueueBenchmark() {
        clinkedQueue.add(1);
        clinkedQueue.poll();
    }

    @Benchmark
    @Fork(value = 1, warmups = 0)
    @Timeout(time = 60)
    @Warmup(iterations = 0)
    @Measurement(iterations = 3)
    @BenchmarkMode(Mode.Throughput)
    public void linkedBlockingQueueBenchmark() {
        linkedbq.add(1);
        linkedbq.poll();
    }

    @Benchmark
    @Fork(value = 1, warmups = 0)
    @Timeout(time = 60)
    @Warmup(iterations = 0)
    @Measurement(iterations = 3)
    @BenchmarkMode(Mode.Throughput)
    public void atomicQueueBenchmark() {
        atomicQueue.add(1);
        atomicQueue.poll();
    }


    @Benchmark
    @Fork(value = 1, warmups = 0)
    @Timeout(time = 60)
    @Warmup(iterations = 0)
    @Measurement(iterations = 3)
    @BenchmarkMode(Mode.Throughput)
    public void syncQueueBenchmark() {
        syncQueue.add(1);
        syncQueue.poll();
    }


    @Benchmark
    @Fork(value = 1, warmups = 0)
    @Timeout(time = 60)
    @Warmup(iterations = 0)
    @Measurement(iterations = 3)
    @BenchmarkMode(Mode.Throughput)
    @Threads(10)
    public void cqueueMultiThreadBenchmark() {
        cqueue.add(10);
        cqueue.poll();
    }

    @Benchmark
    @Fork(value = 1, warmups = 0)
    @Timeout(time = 60)
    @Warmup(iterations = 0)
    @Measurement(iterations = 3)
    @BenchmarkMode(Mode.Throughput)
    @Threads(10)
    public void concurrentQueueMultiThreadBenchmark() {
        clinkedQueue.add(10);
        clinkedQueue.poll();
    }

    @Benchmark
    @Fork(value = 1, warmups = 0)
    @Timeout(time = 60)
    @Warmup(iterations = 0)
    @Measurement(iterations = 3)
    @BenchmarkMode(Mode.Throughput)
    @Threads(10)
    public void linkedBlockingQueueMultiThreadBenchmark() {
        linkedbq.add(10);
        linkedbq.poll();
    }

    @Benchmark
    @Fork(value = 1, warmups = 0)
    @Timeout(time = 60)
    @Warmup(iterations = 0)
    @Measurement(iterations = 3)
    @BenchmarkMode(Mode.Throughput)
    @Threads(10)
    public void atomicQueueMultiThreadBenchmark() {
        atomicQueue.add(10);
        atomicQueue.poll();
    }

    @Benchmark
    @Fork(value = 1, warmups = 0)
    @Timeout(time = 60)
    @Warmup(iterations = 0)
    @Measurement(iterations = 3)
    @BenchmarkMode(Mode.Throughput)
    @Threads(10)
    public void syncQueueMultiThreadBenchmark() {
        syncQueue.add(10);
        syncQueue.poll();
    }


    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .exclude("org.openjdk.jmh.benchmarks.*")
                .include("io.perl.benchmark.QueueBenchmark.*")
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}


