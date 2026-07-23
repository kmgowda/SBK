/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perl.api.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.perl.api.Queue;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;

/**
 * An unbounded, non-blocking multiple-producer, single-consumer (MPSC) queue.
 *
 * <h2>Concurrency model</h2>
 * <p>Any number of producer threads may call {@link #add(Object)}. Exactly one
 * consumer thread owns {@link #poll()} and {@link #clear()}. A producer
 * allocates one node, walks forward from the producer tail when that hint
 * lags, and publishes the node by comparing and setting the last node's
 * {@code next} reference. That successful compare-and-set is the enqueue
 * linearization point. A best-effort tail update reduces later traversal but
 * is not required for correctness.</p>
 *
 * <p>The consumer reads the successor of its thread-confined head with acquire
 * semantics, clears the successor's item, and advances the head without a
 * compare-and-set. Publication through the producer's compare-and-set and the
 * consumer's acquire read establishes visibility of the queued element.
 * Per-producer order and the global order of successful link operations are
 * preserved. Empty {@code poll()} calls return {@code null}; consequently
 * {@code null} elements are rejected.</p>
 *
 * <h2>Why it can be faster than the JDK queue</h2>
 * <p>This class deliberately implements only the operations required by
 * PerL. Unlike {@link java.util.concurrent.ConcurrentLinkedQueue}, it does not
 * support multiple consumers, iterators, interior removal, size traversal, or
 * bulk collection operations. The sole consumer therefore avoids the item
 * compare-and-set and concurrent-head compare-and-set required by a
 * multi-consumer queue. Separately padded head and tail holders reduce false
 * sharing between consumer and producer cache lines. Use
 * {@code ./gradlew :perl:cqueuePerformanceTest} to run the JDK 25 JMH
 * comparison; results are host-specific.</p>
 *
 * <h2>Allocation and garbage collection</h2>
 * <p>Every successful enqueue allocates one {@code Node}, just as JDK 25
 * {@code ConcurrentLinkedQueue} does. The consumer clears the item reference
 * before advancing the head, so a consumed node does not retain the user's
 * payload. Under normal producer progress, retired predecessor nodes become
 * unreachable as the consumer advances. The padded head and tail objects add
 * constant per-queue memory overhead, not per-element overhead.</p>
 *
 * <p>This specialization does <strong>not</strong> provide better reclamation
 * in every workload. JDK 25 {@code ConcurrentLinkedQueue} self-links retired
 * heads and unlinks dead nodes to prevent a stalled traversal or iterator
 * from retaining a long linked chain and to reduce cross-generational links.
 * {@code CQueue} has no equivalent self-link recovery: a producer suspended
 * while holding a stale node can keep consumed successor nodes reachable
 * until that producer resumes or exits. The JDK queue is therefore the safer
 * choice when threads may stall indefinitely or general-purpose collection
 * operations are required. PerL's production queue array currently uses the
 * JDK queue for this stronger GC-retention behavior; {@code CQueue} is intended
 * for controlled MPSC deployments where lower coordination cost is the
 * priority.</p>
 *
 * <h2>Usage constraints</h2>
 * <ul>
 *     <li>Do not call {@link #poll()} or {@link #clear()} from more than one
 *     thread, concurrently or sequentially without external ownership
 *     transfer.</li>
 *     <li>Do not enqueue {@code null}.</li>
 *     <li>Apply external backpressure if an unbounded producer backlog is not
 *     acceptable.</li>
 * </ul>
 *
 * @param <T> queued element type
 */
final public class CQueue<T> implements Queue<T> {

    static final private class Node<T> {
        @SuppressWarnings("unused")
        private T item;
        @SuppressWarnings("unused")
        private Node<T> next;

        Node(T item) {
            ITEM.set(this, item);
        }
    }

    @SuppressFBWarnings(value = "UUF_UNUSED_FIELD",
            justification = "Fields isolate consumer state from producer cache lines")
    static final private class HeadRef<T> {
        @SuppressWarnings("unused")
        private long pad00;
        @SuppressWarnings("unused")
        private long pad01;
        @SuppressWarnings("unused")
        private long pad02;
        @SuppressWarnings("unused")
        private long pad03;
        @SuppressWarnings("unused")
        private long pad04;
        @SuppressWarnings("unused")
        private long pad05;
        @SuppressWarnings("unused")
        private long pad06;
        private Node<T> head;
        @SuppressWarnings("unused")
        private long pad10;
        @SuppressWarnings("unused")
        private long pad11;
        @SuppressWarnings("unused")
        private long pad12;
        @SuppressWarnings("unused")
        private long pad13;
        @SuppressWarnings("unused")
        private long pad14;
        @SuppressWarnings("unused")
        private long pad15;
        @SuppressWarnings("unused")
        private long pad16;
    }

    @SuppressFBWarnings(value = "UUF_UNUSED_FIELD",
            justification = "Fields isolate producer state from consumer cache lines")
    static final private class TailRef<T> {
        @SuppressWarnings("unused")
        private long pad00;
        @SuppressWarnings("unused")
        private long pad01;
        @SuppressWarnings("unused")
        private long pad02;
        @SuppressWarnings("unused")
        private long pad03;
        @SuppressWarnings("unused")
        private long pad04;
        @SuppressWarnings("unused")
        private long pad05;
        @SuppressWarnings("unused")
        private long pad06;
        @SuppressWarnings("unused")
        private volatile Node<T> tail;
        @SuppressWarnings("unused")
        private long pad10;
        @SuppressWarnings("unused")
        private long pad11;
        @SuppressWarnings("unused")
        private long pad12;
        @SuppressWarnings("unused")
        private long pad13;
        @SuppressWarnings("unused")
        private long pad14;
        @SuppressWarnings("unused")
        private long pad15;
        @SuppressWarnings("unused")
        private long pad16;
    }

    private static final VarHandle TAIL;
    private static final VarHandle ITEM;
    private static final VarHandle NEXT;

    private final HeadRef<T> headRef;
    private final TailRef<T> tailRef;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            TAIL = l.findVarHandle(TailRef.class, "tail", CQueue.Node.class);
            ITEM = l.findVarHandle(Node.class, "item", Object.class);
            NEXT = l.findVarHandle(CQueue.Node.class, "next", CQueue.Node.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Creates an empty MPSC queue with one sentinel node.
     */
    public CQueue() {
        final Node<T> sentinel = new Node<>(null);
        this.headRef = new HeadRef<>();
        this.tailRef = new TailRef<>();
        this.headRef.head = sentinel;
        this.tailRef.tail = sentinel;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T poll() {
        final Node<T> currentHead = headRef.head;
        final Node<T> next = (Node<T>) NEXT.getAcquire(currentHead);
        if (next == null) {
            return null;
        }

        final T item = (T) ITEM.get(next);
        ITEM.set(next, null);
        headRef.head = next;
        return item;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean add(T data) {
        final Node<T> newNode = new Node<>(Objects.requireNonNull(data, "data"));
        Node<T> tailNode = (Node<T>) TAIL.getAcquire(tailRef);
        Node<T> current = tailNode;

        while (true) {
            final Node<T> next = (Node<T>) NEXT.getAcquire(current);
            if (next == null) {
                if (NEXT.compareAndSet(current, null, newNode)) {
                    if (current != tailNode) {
                        TAIL.weakCompareAndSetRelease(tailRef, tailNode, newNode);
                    }
                    return true;
                }
            } else {
                final Node<T> latestTail = (Node<T>) TAIL.getAcquire(tailRef);
                if (current != tailNode && tailNode != latestTail) {
                    tailNode = latestTail;
                    current = latestTail;
                } else {
                    current = next;
                }
            }
        }
    }

    @Override
    public void clear() {
        while (poll() != null) {
            // Drain through the normal single-consumer path so the queue remains reusable.
        }
    }
}
