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
import io.sbk.api.Reader;

import java.io.IOException;
import java.io.EOFException;
import java.util.Arrays;
import java.util.List;

/**
 * Class for ChromaDB Reader.
 */
public class ChromaDBReader implements Reader<byte[]> {
    private final Collection collection;
    private final ChromaDBConfig config;
    private long key;
    private final String readerId;

    public ChromaDBReader(int readerId, ParameterOptions params, ChromaDBConfig config, Collection collection) throws IOException {
        this.key = io.sbk.driver.ChromaDB.ChromaDB.generateStartKey(readerId);
        this.collection = collection;
        this.config = config;
        this.readerId = "reader_" + readerId;
    }

    @Override
    public byte[] read() throws EOFException, IOException {
        try {
            String id = String.valueOf(this.key);
            
            // Get document by ID
            Collection.GetResult result = collection.get(
                Arrays.asList(id),  // ids
                null,  // include
                null   // where
            );
            
            if (result != null && result.getIds() != null && !result.getIds().isEmpty()) {
                List<String> documents = result.getDocuments();
                if (documents != null && !documents.isEmpty()) {
                    key++;
                    return documents.get(0).getBytes();
                }
            }
            
            // If no result found, we've reached the end
            throw new EOFException("No more data to read from ChromaDB");
            
        } catch (EOFException ex) {
            throw ex;  // Re-throw EOFException
        } catch (Exception ex) {
            throw new IOException("Read operation failed", ex);
        }
    }

    @Override
    public void close() throws IOException {
        // Collection and database are closed by the main ChromaDB class
        // No need to close individual readers
    }
}
