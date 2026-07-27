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
import io.perl.api.TimeStampNode;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;

/**
 * Lock-free intrusive multiple-producer, single-consumer timestamp queue.
 *
 * <h2>Allocation model</h2>
 * <p>Producers supply a single-use {@link TimeStampNode}; enqueue does not
 * allocate. The node contains both the immutable timestamp payload and the
 * linked-queue {@code next} reference. This removes the second allocation made
 * by {@link java.util.concurrent.ConcurrentLinkedQueue} for its private wrapper
 * node.</p>
 *
 * <h2>Concurrency and memory ordering</h2>
 * <p>Any number of producers may call {@link #add(TimeStampNode)}. Exactly one
 * consumer owns {@link #poll()} and {@link #clear()}. Producers locate a
 * trailing node and publish with a compare-and-set from a {@code null}
 * successor to the new node. That successful compare-and-set is the enqueue
 * linearization point. The consumer reads {@code next} with acquire semantics,
 * which observes the immutable timestamp fields initialized before producer
 * publication. Per-producer FIFO order and the global order of successful link
 * operations are preserved.</p>
 *
 * <p>The consumer owns the head and therefore advances it without a
 * compare-and-set or an item-field compare-and-set. A lagging producer tail is
 * only a traversal hint; failure to update it cannot lose an element. Under
 * contention, a failed producer compare-and-set means another producer made
 * progress, so the enqueue algorithm is lock-free.</p>
 *
 * <h2>Garbage collection</h2>
 * <p>Consumed predecessor chains are detached in batches of
 * {@value #RETIRE_BATCH_SIZE}. The consumer release-publishes a recovery head
 * before self-linking every retired predecessor in the completed batch. A
 * producer suspended with any stale pointer detects that self-link and resumes
 * from the recovery head. Unlike JDK
 * {@link java.util.concurrent.ConcurrentLinkedQueue}, an intrusive node cannot
 * null a separate item reference while retaining its structural wrapper.
 * Self-linking removes each retired timestamp from the live queue chain and
 * enables its payload and node to be reclaimed after stale producer,
 * tail-hint, consumer, and caller references have also disappeared. It cannot
 * make an object collectible while any such strong reference remains. Nodes
 * are deliberately not pooled: pooling would retain heap, complicate
 * ownership, and introduce ABA risks.</p>
 *
 * <p>Like JDK {@link java.util.concurrent.ConcurrentLinkedQueue}, the producer
 * tail is allowed to lag by one node. Updating the shared tail only after a
 * producer has traversed away from its initial tail candidate avoids a tail
 * compare-and-set on every enqueue while preserving the linked-node
 * compare-and-set as the linearization point.</p>
 *
 * <p>Producer and consumer state live in separate, manually padded holder
 * objects to reduce false sharing. This is a best-effort layout optimization:
 * the Java language and HotSpot do not guarantee field order or cache-line
 * placement. The implementation deliberately avoids the internal
 * {@code @Contended} annotation because using it would require internal-module
 * access and JVM flags in every embedding application. Padding is not part of
 * the correctness argument.</p>
 *
 * <h2>Usage constraints</h2>
 * <ul>
 *     <li>Only one thread may invoke {@code poll} or {@code clear}.</li>
 *     <li>Each node may be enqueued exactly once.</li>
 *     <li>The queue is unbounded; callers provide overload control.</li>
 * </ul>
 */
public final class TimeStampMpscQueue implements Queue<TimeStampNode> {
    static final int RETIRE_BATCH_SIZE = 16;

    @SuppressFBWarnings(value = "UUF_UNUSED_FIELD",
            justification = "Fields isolate consumer state from producer cache lines")
    private static final class HeadRef {
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
        private TimeStampNode head;
        private TimeStampNode recoveryHead;
        private final TimeStampNode[] retiredNodes;
        private int retiredNodeCount;
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

        private HeadRef(int retireBatchSize) {
            retiredNodes = new TimeStampNode[retireBatchSize];
        }
    }

    @SuppressFBWarnings(value = "UUF_UNUSED_FIELD",
            justification = "Fields isolate producer state from consumer cache lines")
    private static final class TailRef {
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
        private volatile TimeStampNode tail;
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
    private static final VarHandle NEXT;
    private static final VarHandle RECOVERY_HEAD;

    private final HeadRef headRef;
    private final TailRef tailRef;
    private final int retireBatchSize;

    static {
        try {
            final MethodHandles.Lookup lookup = MethodHandles.lookup();
            TAIL = lookup.findVarHandle(TailRef.class, "tail", TimeStampNode.class);
            NEXT = MethodHandles.privateLookupIn(TimeStampNode.class, lookup)
                    .findVarHandle(TimeStampNode.class, "next", TimeStampNode.class);
            RECOVERY_HEAD = lookup.findVarHandle(
                    HeadRef.class, "recoveryHead", TimeStampNode.class);
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    /**
     * Creates an empty queue with one constant-cost sentinel node.
     */
    public TimeStampMpscQueue() {
        this(RETIRE_BATCH_SIZE);
    }

    /**
     * Creates an empty queue with an injectable retirement batch for
     * concurrency-model tests.
     *
     * @param retireBatchSize number of consumed predecessors retired together
     * @throws IllegalArgumentException when {@code retireBatchSize} is less
     *                                  than one
     */
    TimeStampMpscQueue(int retireBatchSize) {
        if (retireBatchSize < 1) {
            throw new IllegalArgumentException(
                    "Retirement batch size must be positive");
        }
        final TimeStampNode sentinel = new TimeStampNode(0, 0, 0, 0);
        this.headRef = new HeadRef(retireBatchSize);
        this.tailRef = new TailRef();
        this.retireBatchSize = retireBatchSize;
        this.headRef.head = sentinel;
        this.headRef.recoveryHead = sentinel;
        this.tailRef.tail = sentinel;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method must be called by only one consumer thread.</p>
     *
     * @return the next producer-supplied node, or {@code null} when empty
     */
    @Override
    public TimeStampNode poll() {
        final TimeStampNode currentHead = headRef.head;
        final TimeStampNode next = (TimeStampNode) NEXT.getAcquire(currentHead);
        if (next == null) {
            return null;
        }

        headRef.head = next;
        final int retiredNodeCount = headRef.retiredNodeCount;
        headRef.retiredNodes[retiredNodeCount] = currentHead;
        if (retiredNodeCount + 1 == retireBatchSize) {
            retireBatch(next, retireBatchSize);
        } else {
            headRef.retiredNodeCount = retiredNodeCount + 1;
        }
        return next;
    }

    /**
     * Adds a single-use timestamp node without allocating a queue wrapper.
     *
     * @param node producer-owned node to enqueue
     * @return {@code true} after the node is linked
     * @throws NullPointerException if {@code node} is {@code null}
     */
    @Override
    public boolean add(TimeStampNode node) {
        return add(node, null);
    }

    /**
     * Enqueues a node and optionally pauses immediately after reading the tail.
     * The callback exists only for deterministic stale-producer testing.
     *
     * @param node single-use timestamp node
     * @param afterTailRead callback invoked after the initial tail read
     * @return {@code true}
     */
    boolean add(TimeStampNode node, Runnable afterTailRead) {
        final TimeStampNode newNode = Objects.requireNonNull(node, "node");
        TimeStampNode tailNode = (TimeStampNode) TAIL.getAcquire(tailRef);
        TimeStampNode current = tailNode;
        if (afterTailRead != null) {
            afterTailRead.run();
        }

        while (true) {
            final TimeStampNode next = (TimeStampNode) NEXT.getAcquire(current);
            if (next == null) {
                if (NEXT.compareAndSet(current, null, newNode)) {
                    // JDK-style slack: avoid touching the shared tail when the
                    // initial tail candidate was already the trailing node.
                    if (current != tailNode) {
                        TAIL.weakCompareAndSetRelease(
                                tailRef, tailNode, newNode);
                    }
                    return true;
                }
            } else if (next == current) {
                final TimeStampNode latestTail =
                        (TimeStampNode) TAIL.getAcquire(tailRef);
                if (tailNode != latestTail) {
                    tailNode = latestTail;
                    current = latestTail;
                } else {
                    final TimeStampNode recoveryHead =
                            (TimeStampNode) RECOVERY_HEAD.getAcquire(headRef);
                    TAIL.weakCompareAndSetRelease(
                            tailRef, tailNode, recoveryHead);
                    tailNode = recoveryHead;
                    current = recoveryHead;
                }
            } else {
                final TimeStampNode latestTail =
                        (TimeStampNode) TAIL.getAcquire(tailRef);
                if (current != tailNode && tailNode != latestTail) {
                    tailNode = latestTail;
                    current = latestTail;
                } else {
                    current = next;
                }
            }
        }
    }

    /**
     * Drains all queued nodes and completes retirement of the remaining
     * predecessor batch so the queue can be reused.
     *
     * <p>This method must be called by only one consumer thread.</p>
     */
    @Override
    public void clear() {
        while (poll() != null) {
            // Drain through the normal single-consumer path so the queue remains reusable.
        }
        if (headRef.retiredNodeCount != 0) {
            retireBatch(headRef.head, headRef.retiredNodeCount);
        }
    }

    /**
     * Counts retired nodes still linked before the consumer head.
     *
     * @return bounded retired-node count used by reclamation tests
     */
    int retainedRetiredNodeCount() {
        return headRef.retiredNodeCount;
    }

    private void retireBatch(
            TimeStampNode recoveryHead, int retiredNodeCount) {
        RECOVERY_HEAD.setRelease(headRef, recoveryHead);
        final TimeStampNode[] retiredNodes = headRef.retiredNodes;
        headRef.retiredNodeCount = 0;
        for (int index = 0; index < retiredNodeCount; index++) {
            final TimeStampNode retiredNode = retiredNodes[index];
            NEXT.setRelease(retiredNode, retiredNode);
            retiredNodes[index] = null;
        }
    }
}
