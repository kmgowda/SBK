/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.driver.ChromaDB;

import tech.amikos.chromadb.Collection;
import io.sbk.params.ParameterOptions;
import io.sbk.api.Writer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.Arrays;

/**
 * Class for ChromaDB Writer.
 */
public class ChromaDBWriter implements Writer<byte[]> {
    private final Collection collection;
    private final ChromaDBConfig config;
    private long key;
    private final String writerId;

    public ChromaDBWriter(int writerID, ParameterOptions params, ChromaDBConfig config, Collection collection) throws IOException {
        this.key = io.sbk.driver.ChromaDB.ChromaDB.generateStartKey(writerID);
        this.collection = collection;
        this.config = config;
        this.writerId = "writer_" + writerID;
    }

    @Override
    public CompletableFuture<?> writeAsync(byte[] data) throws IOException {
        try {
            String id = String.valueOf(this.key++);
            
            // Store the data as document
            String document = new String(data);
            
            // Create metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("writer_id", writerId);
            metadata.put("data_size", String.valueOf(data.length));
            metadata.put("timestamp", String.valueOf(System.currentTimeMillis()));
            
            // Add document to ChromaDB
            collection.add(
                null,  // embeddings (null to use default embedding function)
                Arrays.asList(metadata), // metadata
                Arrays.asList(document),  // documents
                Arrays.asList(id)         // ids
            );
            
        } catch (Exception ex) {
            throw new IOException("Write operation failed", ex);
        }
        return null;
    }

    @Override
    public void sync() throws IOException {
        // ChromaDB handles persistence automatically
        // No explicit sync needed
    }

    @Override
    public void close() throws IOException {
        // Collection and database are closed by the main ChromaDB class
        // No need to close individual writers
    }

}
