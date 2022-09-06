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
import java.util.concurrent.atomic.AtomicReference;

final public class CQueue<T> implements Queue<T> {
    final private Node<T> firstNode;
    final private  AtomicReference<Node<T>> head;
    final private  AtomicReference<Node<T>> tail;

    public CQueue() {
        this.firstNode = new Node<>(null);
        this.head = new AtomicReference<>(null);
        this.tail = new AtomicReference<>(null);
    }

    @Override
    public T poll() {
        final Node<T> first = head.getAndSet(null);
        if (first == null) {
            return null;
        }
        if (first.next == null) {
            head.set(first);
            return null;
        }
        head.set(first.next);
        final Node<T> cur = first.next;
        first.next = null;
        return cur.item;
    }

    @Override
    public boolean add(T data) {
        final Node<T> node = new Node<>(data);
        final Node<T> cur = tail.getAndSet(node);
        if (cur == null) {
            firstNode.next = node;
            head.set(firstNode);
        } else {
            cur.next = node;
        }
        return true;
    }

    @Override
    public void clear() {
        Node<T> first = head.getAndSet(null);
        tail.set(null);
        Node<T> cur;
        while ( first != null ) {
            cur = first;
            first = first.next;
            cur.next = null;
        }
    }

    static final class Node<T> {
        final public T item;
        public volatile Node<T> next;
        Node(T item) {
            this.item = item;
            this.next = null;
        }
    }
}
