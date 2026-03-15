/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.driver.ChromaDB;

import tech.amikos.chromadb.Client;
import tech.amikos.chromadb.Collection;
import tech.amikos.chromadb.embeddings.DefaultEmbeddingFunction;
import tech.amikos.chromadb.embeddings.EmbeddingFunction;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.params.ParameterOptions;
import io.sbk.api.Storage;
import io.sbk.data.DataType;
import io.sbk.data.impl.ByteArray;
import io.sbk.params.InputOptions;

import java.io.IOException;
import java.util.Objects;

/**
 * Class for ChromaDB storage driver.
 *
 * ChromaDB is a vector database for AI applications. This driver allows
 * benchmarking ChromaDB's performance for storing and retrieving byte arrays.
 */
public class ChromaDB implements Storage<byte[]> {
    private final static String CONFIGFILE = "ChromaDB.properties";
    private ChromaDBConfig config;
    private Client client;
    private Collection collection;
    private EmbeddingFunction embeddingFunction;

    public static long generateStartKey(int id) {
        return (long) id * (long) Integer.MAX_VALUE;
    }

    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory());
        try {
            config = mapper.readValue(
                    Objects.requireNonNull(ChromaDB.class.getClassLoader().getResourceAsStream(CONFIGFILE)),
                    ChromaDBConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        params.addOption("host", true, "ChromaDB host, default: " + config.host);
        params.addOption("port", true, "ChromaDB port, default: " + config.port);
        params.addOption("collectionName", true, "ChromaDB collection name, default: " + config.collectionName);
        params.addOption("embeddingDimension", true, "Embedding dimension, default: " + config.embeddingDimension);
        params.addOption("distanceFunction", true, "Distance function, default: " + config.distanceFunction);
        params.addOption("ssl", true, "Use SSL, default: " + config.ssl);
        params.addOption("authToken", true, "Auth token, default: " + config.authToken);
        params.addOption("timeoutSeconds", true, "Timeout in seconds, default: " + config.timeoutSeconds);
        params.addOption("maxRetries", true, "Max retries, default: " + config.maxRetries);
        params.addOption("batchSize", true, "Batch size, default: " + config.batchSize);
    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        config.host = params.getOptionValue("host", config.host);
        config.port = Integer.parseInt(params.getOptionValue("port", String.valueOf(config.port)));
        config.collectionName = params.getOptionValue("collectionName", config.collectionName);
        config.embeddingDimension = Integer.parseInt(params.getOptionValue("embeddingDimension", String.valueOf(config.embeddingDimension)));
        config.distanceFunction = params.getOptionValue("distanceFunction", config.distanceFunction);
        config.ssl = Boolean.parseBoolean(params.getOptionValue("ssl", String.valueOf(config.ssl)));
        config.authToken = params.getOptionValue("authToken", config.authToken);
        config.timeoutSeconds = Integer.parseInt(params.getOptionValue("timeoutSeconds", String.valueOf(config.timeoutSeconds)));
        config.maxRetries = Integer.parseInt(params.getOptionValue("maxRetries", String.valueOf(config.maxRetries)));
        config.batchSize = Integer.parseInt(params.getOptionValue("batchSize", String.valueOf(config.batchSize)));
    }

    @Override
    public void openStorage(final ParameterOptions params) throws IOException {
        try {
            // Initialize ChromaDB client
            String chromaUrl = (config.ssl ? "https://" : "http://") + config.host + ":" + config.port;
            
            client = new Client(chromaUrl);
            
            // Create embedding function
            embeddingFunction = new DefaultEmbeddingFunction();
            
            // Get or create collection
            try {
                collection = client.getCollection(config.collectionName, embeddingFunction);
            } catch (Exception e) {
                // Collection doesn't exist, create it
                collection = client.createCollection(config.collectionName, null, true, embeddingFunction);
            }

        } catch (Exception ex) {
            throw new IOException("Failed to open ChromaDB", ex);
        }
    }

    @Override
    public void closeStorage(final ParameterOptions params) throws IOException {
        if (client != null) {
            try {
                // ChromaDB client doesn't require explicit close in the Java client
                client = null;
                collection = null;
                embeddingFunction = null;
            } catch (Exception ex) {
                throw new IOException("Failed to close ChromaDB", ex);
            }
        }
    }

    @Override
    public DataWriter<byte[]> createWriter(final int id, final ParameterOptions params) {
        try {
            return new ChromaDBWriter(id, params, config, collection);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataReader<byte[]> createReader(final int id, final ParameterOptions params) {
        try {
            return new ChromaDBReader(id, params, config, collection);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataType<byte[]> getDataType() {
        return new ByteArray();
    }
}
