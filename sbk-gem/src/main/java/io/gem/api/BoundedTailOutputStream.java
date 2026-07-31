/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.api;

import org.jetbrains.annotations.NotNull;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Fixed-capacity output stream that retains only the most recently written bytes.
 *
 * <p>Remote commands can produce unbounded diagnostic output. Retaining a tail keeps
 * failure diagnostics useful without allowing one remote process to exhaust the
 * SBK-GEM host heap.
 */
final class BoundedTailOutputStream extends OutputStream {
    private static final byte[] EMPTY = new byte[0];
    private final int capacity;
    private byte[] buffer;
    private int start;
    private int size;

    /**
     * Create a bounded diagnostic stream.
     *
     * @param capacity maximum retained byte count
     * @throws IllegalArgumentException if capacity is not positive
     */
    BoundedTailOutputStream(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Diagnostic stream capacity must be positive");
        }
        this.capacity = capacity;
        buffer = EMPTY;
    }

    @Override
    public synchronized void write(int value) {
        ensureCapacity(Math.min(capacity, size + 1));
        if (size < capacity) {
            buffer[(start + size) % buffer.length] = (byte) value;
            size++;
        } else {
            buffer[start] = (byte) value;
            start = (start + 1) % buffer.length;
        }
    }

    @Override
    public synchronized void write(byte @NotNull [] values, int offset, int length) {
        if (offset < 0 || length < 0 || offset > values.length - length) {
            throw new IndexOutOfBoundsException();
        }
        if (length == 0) {
            return;
        }
        if (length >= capacity) {
            ensureCapacity(capacity);
            System.arraycopy(values, offset + length - capacity, buffer, 0, capacity);
            start = 0;
            size = capacity;
            return;
        }

        ensureCapacity(Math.min(capacity, size + length));
        final int overflow = Math.max(0, size + length - capacity);
        start = (start + overflow) % capacity;
        size -= overflow;
        final int end = (start + size) % capacity;
        final int firstLength = Math.min(length, capacity - end);
        System.arraycopy(values, offset, buffer, end, firstLength);
        System.arraycopy(values, offset + firstLength, buffer, 0, length - firstLength);
        size += length;
    }

    /**
     * Return the retained bytes in their original order.
     *
     * @return copy of the retained output tail
     */
    synchronized byte[] toByteArray() {
        if (size == 0) {
            return new byte[0];
        }
        if (start + size <= buffer.length) {
            return Arrays.copyOfRange(buffer, start, start + size);
        }
        final byte[] result = new byte[size];
        final int firstLength = buffer.length - start;
        System.arraycopy(buffer, start, result, 0, firstLength);
        System.arraycopy(buffer, 0, result, firstLength, size - firstLength);
        return result;
    }

    private void ensureCapacity(int required) {
        if (buffer.length >= required) {
            return;
        }
        int expanded = Math.min(capacity, Math.max(32, buffer.length));
        while (expanded < required && expanded < capacity) {
            expanded = Math.min(capacity, expanded << 1);
        }
        final byte[] replacement = new byte[expanded];
        if (size > 0) {
            if (start + size <= buffer.length) {
                System.arraycopy(buffer, start, replacement, 0, size);
            } else {
                final int firstLength = buffer.length - start;
                System.arraycopy(buffer, start, replacement, 0, firstLength);
                System.arraycopy(buffer, 0, replacement, firstLength, size - firstLength);
            }
        }
        buffer = replacement;
        start = 0;
    }

    @Override
    public String toString() {
        return new String(toByteArray(), StandardCharsets.UTF_8);
    }
}
