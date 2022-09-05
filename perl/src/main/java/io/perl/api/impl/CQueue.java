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

import io.perl.api.Queue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class CQueue<T> implements Queue<T> {
    final private  AtomicReference<Node<T>> head;
    final private  AtomicReference<Node<T>> tail;

    private CQueue(AtomicReference<Node<T>> head, AtomicReference<Node<T>> tail) {
        this.head = head;
        this.tail = tail;
    }

    public CQueue() {
        this(new AtomicReference<>(null), new AtomicReference<>(null));
    }

    @Override
    public T poll() {
        while (true) {
            Node<T> first = head.get();
            if (first == null) {
                return null;
            } else {
                head.set(first.next);
                if (first.ins.compareAndSet(true, false)) {
                    first.next = null;
                    return first.item;
                }
            }
        }
    }

    @Override
    public boolean add(T data) {
        Node<T> node = new Node<>(data);
        Node<T> prev = tail.getAndSet(node);
        if (prev != null) {
            prev.next = node;
        }
        head.compareAndSet(null, node);
        return true;
    }

    @Override
    public void clear() {
        tail.getAndSet(null);
        Node<T> first = head.getAndSet(null);
        Node<T> cur;
        while ( first != null ) {
            cur = first;
            first = first.next;
            cur.next = null;
        }
    }

    static final class Node<T> {
        final public T item;
        final public AtomicBoolean ins;
        public volatile Node<T> next;

        Node(T item) {
            this.item = item;
            this.ins = new AtomicBoolean(true);
            this.next = null;
        }
    }
}
