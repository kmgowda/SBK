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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/*
 * Concurrent Queue Implementation using Atomic References.
 * DON'T USE THIS CLASS.
 * Use Java native 'ConcurrentLinkedQueue', because the ConcurrentLinkedQueue does better Garbage collection.
 */
final public class CQueue<T> implements Queue<T> {
    final private Node<T> firstNode;
    private volatile Node<T> head;
    private volatile Node<T> tail;

    public CQueue() {
        this.firstNode = new Node<>(null);
        this.head = firstNode;
        this.tail = firstNode;
    }

    @Override
    public T poll() {
        final Node<T> first = (Node<T>) HEAD.get(this);
        final Node<T> cur = (Node<T>) NEXT.get(first);
        if (cur == null) {
            return null;
        }
        HEAD.set(this, cur);
        first.next = null;
        return cur.item;
    }

    @Override
    public boolean add(T data) {
        final Node<T> node = new Node<>(data);
        final Node<T> cur = (Node<T>) TAIL.getAndSet(this, node);
        cur.next = node;
        return true;
    }

    @Override
    public void clear() {
        Object first = HEAD.getAndSet(this, firstNode);
        TAIL.set(this,firstNode);
        /*
           The below code helps JVM garbage collector to recycle;
           without the below code, out of memory issues are observed
        */
        while ( first != null ) {
            first = NEXT.getAndSet(first, null);
        }
    }

    static final private class Node<T> {
        final public T item;
        public Node<T> next;
        Node(T item) {
            this.item = item;
            this.next = null;
        }

    }

    private static final VarHandle HEAD;
    private static final VarHandle TAIL;
    static final VarHandle ITEM;
    static final VarHandle NEXT;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            HEAD = l.findVarHandle(CQueue.class, "head", CQueue.Node.class);
            TAIL = l.findVarHandle(CQueue.class, "tail", CQueue.Node.class);
            ITEM = l.findVarHandle(CQueue.Node.class, "item", Object.class);
            NEXT = l.findVarHandle(CQueue.Node.class, "next", CQueue.Node.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }


}
