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
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
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

    final private CQueue<Integer> cqueue;
    final private ConcurrentLinkedQueue<Integer> clinkedQueue;
    final private LinkedBlockingQueue<Integer> linkedbq;
    final private AtomicQueue<Integer> atomicQueue;

    
    public QueueBenchmark() {
        cqueue = new CQueue<>();
        clinkedQueue = new ConcurrentLinkedQueue<>();
        linkedbq = new LinkedBlockingQueue<>();
        atomicQueue = new AtomicQueue<>();
    }
    
    @Benchmark
    @Fork(value = 1, warmups = 0)
    @Timeout(time = 60)
    @Warmup(iterations = 0)
    @Measurement(iterations = 3)
    public void cqueueBenchmark() {
        cqueue.add(1);
        cqueue.poll();
    }

    @Benchmark
    @Fork(value = 1, warmups = 0)
    @Timeout(time = 60)
    @Warmup(iterations = 0)
    @Measurement(iterations = 3)
    public void concurrentQueueBenchmark() {
        clinkedQueue.add(1);
        clinkedQueue.poll();
    }

    @Benchmark
    @Fork(value = 1, warmups = 0)
    @Timeout(time = 60)
    @Warmup(iterations = 0)
    @Measurement(iterations = 3)
    public void linkedBlockingQueueBenchmark() {
        linkedbq.add(1);
        linkedbq.poll();
    }

    @Benchmark
    @Fork(value = 1, warmups = 0)
    @Timeout(time = 60)
    @Warmup(iterations = 0)
    @Measurement(iterations = 3)
    public void atomicQueueBenchmark() {
        atomicQueue.add(1);
        atomicQueue.poll();
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


