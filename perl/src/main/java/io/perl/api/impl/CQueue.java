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
 * Concurrent Queue Implementation using VarHandle References.
 * DON'T USE THIS CLASS.
 * Use Java native 'ConcurrentLinkedQueue', because the ConcurrentLinkedQueue does better Garbage collection.
 */
final public class CQueue<T> implements Queue<T> {

    static final private class Node<T> {
        public volatile T item;
        public volatile Node<T> next;
        Node(T item) {
            this.item = item;
            this.next = null;
        }
    }

    private static final VarHandle HEAD;
    private static final VarHandle TAIL;
    private static final VarHandle ITEM;
    private static final VarHandle NEXT;

    final private Node<T> firstNode;
    private volatile Node<T> head;
    private volatile Node<T> tail;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            HEAD = l.findVarHandle(CQueue.class, "head", CQueue.Node.class);
            TAIL = l.findVarHandle(CQueue.class, "tail", CQueue.Node.class);
            ITEM = l.findVarHandle(Node.class, "item", Object.class);
            NEXT = l.findVarHandle(CQueue.Node.class, "next", CQueue.Node.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public CQueue() {
        this.firstNode = new Node<>(null);
        this.head = firstNode;
        this.tail = firstNode;
    }

    public T pollOnce() {
        final Object cur = NEXT.getAndSet(head, null);
        if (cur == null) {
            return null;
        }
        HEAD.set(this, cur);
        return (T) ITEM.getAndSet(cur, null);
    }

    @Override
    public T poll() {
        Object curHead = HEAD.get(this);
        Object nxt = NEXT.get(curHead);

        while (nxt != null && !HEAD.compareAndSet(this, curHead, nxt)) {
            curHead = HEAD.get(this);
            nxt = NEXT.get(curHead);
        }

        if (nxt == null) {
            return null;
        }
        NEXT.set(curHead, null);
        return (T) ITEM.getAndSet(nxt, null);
    }

    @Override
    public boolean add(T data) {
        final Node<T> node = new Node<>(data);
        final Object cur = TAIL.getAndSet(this, node);
        NEXT.set(cur, node);
        return true;
    }

    @Override
    public void clear() {
        NEXT.set(firstNode, null);
        Object first = HEAD.getAndSet(this, firstNode);
        TAIL.set(this, firstNode);
        /*
           The below code helps JVM garbage collector to recycle;
           without the below code, out of memory issues are observed
        */
        while ( first != null ) {
            first = NEXT.getAndSet(first, null);
        }
    }


}
