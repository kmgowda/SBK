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
 * A non-blocking multiple-producer, single-consumer queue.
 *
 * <p>Producers publish a new node by atomically linking it to the current last
 * node. The sole consumer advances its thread-confined head and therefore does
 * not perform a compare-and-set operation while dequeuing. Padded cursor
 * holders prevent producer-tail and consumer-head cache-line contention. This
 * queue is intended for SBK's many-producer performance-event paths, where
 * exactly one recorder consumes the events.</p>
 *
 * <p>This class does not permit {@code null} elements. Only one thread may call
 * {@link #poll()} or {@link #clear()}; using multiple consumers violates the
 * queue contract.</p>
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
     * Creates an empty MPSC queue.
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
