/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.driver.MinIO;

import java.util.Locale;

/**
 * S3 workload operations exposed by the MinIO driver.
 *
 * <p>Every operation is implemented through the corresponding MinIO Java SDK
 * method. The enum only classifies operations for SBK's writer and reader
 * worker paths; it does not implement the S3 protocol.
 */
public enum S3Operation {
    PUT(true, false),
    UPDATE(true, true),
    COPY(true, true),
    DELETE(true, true),
    TAG_SET(true, true),
    TAG_DELETE(true, true),
    GET(false, true),
    RANGE_GET(false, true),
    STAT(false, true),
    TAG_GET(false, true),
    LIST(false, false),
    BUCKET_CREATE(true, false),
    BUCKET_DELETE(true, false),
    BUCKET_STAT(false, false),
    BUCKET_LIST(false, false);

    private final boolean writerOperation;
    private final boolean objectCatalogRequired;

    S3Operation(boolean writerOperation, boolean objectCatalogRequired) {
        this.writerOperation = writerOperation;
        this.objectCatalogRequired = objectCatalogRequired;
    }

    /**
     * Resolve a command-line operation name.
     *
     * @param value operation name, case-insensitive; hyphens and underscores are equivalent
     * @return resolved operation
     * @throws IllegalArgumentException when the name is unsupported
     */
    public static S3Operation fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("S3 operation must not be empty");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if ("CREATE".equals(normalized)) {
            normalized = "PUT";
        } else if ("OVERWRITE".equals(normalized)) {
            normalized = "UPDATE";
        } else if ("HEAD".equals(normalized)) {
            normalized = "STAT";
        } else if ("RANGE_READ".equals(normalized)) {
            normalized = "RANGE_GET";
        }
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported S3 operation '" + value
                    + "'. Supported operations: " + supportedValues(), ex);
        }
    }

    /**
     * Return whether this operation runs on SBK's writer path.
     *
     * @return true for mutating operations
     */
    public boolean isWriterOperation() {
        return writerOperation;
    }

    /**
     * Return whether this operation needs a prepared object catalog.
     *
     * @return true when existing object keys are required
     */
    public boolean requiresObjectCatalog() {
        return objectCatalogRequired;
    }

    /**
     * Return the accepted values for CLI help.
     *
     * @return pipe-separated lower-case values
     */
    public static String supportedValues() {
        return "put|update|copy|delete|tag-set|tag-delete|get|range-get|stat|tag-get|list"
                + "|bucket-create|bucket-delete|bucket-stat|bucket-list";
    }
}
