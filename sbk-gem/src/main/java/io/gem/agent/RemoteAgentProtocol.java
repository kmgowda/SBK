/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.agent;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Binary, length-delimited protocol used between SBK-GEM and its remote agent. */
public final class RemoteAgentProtocol {
    /** Protocol magic. */
    public static final String MAGIC = "SBK_GEM_AGENT_V1";
    /** Maximum request values. */
    public static final int MAX_VALUES = 16_384;
    /** Maximum encoded string size. */
    public static final int MAX_STRING_BYTES = 16 * 1024 * 1024;

    private RemoteAgentProtocol() {
    }

    /**
     * Encode an operation and values.
     * @param operation operation name
     * @param values operation values
     * @return encoded request
     * @throws IOException when encoding fails
     */
    public static byte[] encode(String operation, List<String> values) throws IOException {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             DataOutputStream output = new DataOutputStream(bytes)) {
            writeString(output, MAGIC);
            writeString(output, operation);
            output.writeInt(values.size());
            for (String value : values) {
                writeString(output, value);
            }
            output.flush();
            return bytes.toByteArray();
        }
    }

    /**
     * Read a request.
     * @param input request input
     * @return decoded request
     * @throws IOException when invalid
     */
    public static Request read(DataInputStream input) throws IOException {
        if (!MAGIC.equals(readString(input))) {
            throw new IOException("Unsupported SBK-GEM agent protocol");
        }
        final String operation = readString(input);
        final int count = input.readInt();
        if (count < 0 || count > MAX_VALUES) {
            throw new IOException("Invalid SBK-GEM agent value count: " + count);
        }
        final List<String> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            values.add(readString(input));
        }
        return new Request(operation, List.copyOf(values));
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_STRING_BYTES) {
            throw new IOException("SBK-GEM agent string is too large");
        }
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    private static String readString(DataInputStream input) throws IOException {
        final int length = input.readInt();
        if (length < 0 || length > MAX_STRING_BYTES) {
            throw new IOException("Invalid SBK-GEM agent string length: " + length);
        }
        final byte[] bytes = input.readNBytes(length);
        if (bytes.length != length) {
            throw new IOException("Truncated SBK-GEM agent request");
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /** Decoded immutable request. */
    public static final class Request {
        private final String operation;
        private final String[] values;

        private Request(String operation, List<String> values) {
            this.operation = operation;
            this.values = values.toArray(String[]::new);
        }

        /**
         * Return the requested operation name.
         * @return operation name
         */
        public String operation() {
            return operation;
        }

        /**
         * Return immutable operation values.
         * @return immutable operation values
         */
        public List<String> values() {
            return List.of(values);
        }
    }
}
