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
    /** Maximum aggregate encoded request size. */
    public static final int MAX_REQUEST_BYTES = 64 * 1024 * 1024;
    /** Stable diagnostic emitted when a transferred runtime archive fails integrity verification. */
    public static final String ARCHIVE_DIGEST_MISMATCH = "SBK archive SHA-256 mismatch";
    /** Probe operation. */
    public static final String PROBE = "probe";
    /** Runtime activation operation. */
    public static final String ACTIVATE = "activate";
    /** Runtime verification operation. */
    public static final String VERIFY = "verify";
    /** Retired-runtime cleanup operation. */
    public static final String CLEANUP = "cleanup";
    /** Runtime reservation operation. */
    public static final String RUNTIME_RESERVE = "runtime-reserve";
    /** Runtime lease acquisition operation. */
    public static final String RUNTIME_ACQUIRE = "runtime-acquire";
    /** Runtime lease heartbeat operation. */
    public static final String RUNTIME_HEARTBEAT = "runtime-heartbeat";
    /** Runtime lease release operation. */
    public static final String RUNTIME_RELEASE = "runtime-release";
    /** Remote benchmark execution operation. */
    public static final String RUN = "run";

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
        if (values.size() > MAX_VALUES) {
            throw new IOException("Invalid SBK-GEM agent value count: " + values.size());
        }
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             DataOutputStream output = new DataOutputStream(bytes)) {
            writeString(output, MAGIC);
            writeString(output, operation);
            output.writeInt(values.size());
            for (String value : values) {
                writeString(output, value);
                if (bytes.size() > MAX_REQUEST_BYTES) {
                    throw new IOException("SBK-GEM agent request is too large");
                }
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
        final int[] remainingBytes = {MAX_REQUEST_BYTES};
        if (!MAGIC.equals(readString(input, remainingBytes))) {
            throw new IOException("Unsupported SBK-GEM agent protocol");
        }
        final String operation = readString(input, remainingBytes);
        remainingBytes[0] -= Integer.BYTES;
        if (remainingBytes[0] < 0) {
            throw new IOException("SBK-GEM agent request is too large");
        }
        final int count = input.readInt();
        if (count < 0 || count > MAX_VALUES) {
            throw new IOException("Invalid SBK-GEM agent value count: " + count);
        }
        final List<String> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            values.add(readString(input, remainingBytes));
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

    private static String readString(DataInputStream input, int[] remainingBytes) throws IOException {
        remainingBytes[0] -= Integer.BYTES;
        if (remainingBytes[0] < 0) {
            throw new IOException("SBK-GEM agent request is too large");
        }
        final int length = input.readInt();
        if (length < 0 || length > MAX_STRING_BYTES) {
            throw new IOException("Invalid SBK-GEM agent string length: " + length);
        }
        remainingBytes[0] -= length;
        if (remainingBytes[0] < 0) {
            throw new IOException("SBK-GEM agent request is too large");
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
