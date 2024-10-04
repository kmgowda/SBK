/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.driver.Elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import io.sbk.params.ParameterOptions;
import io.sbk.api.Writer;
import io.sbk.system.Printer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


/**
 * Class for Elasticsearch Writer.
 */
public class ElasticsearchWriter implements Writer<String> {
    private final ElasticsearchConfig config;
    private final ElasticsearchClient client;
    private long id;

    public ElasticsearchWriter(int writerID, ParameterOptions params, ElasticsearchConfig config, ElasticsearchClient client) {
        this.id = Elasticsearch.generateStartKey(writerID);
        this.config = config;
        this.client = client;
    }

    @Override
    public CompletableFuture writeAsync(String data) throws IOException {
        try {
            writeData(data);
        } catch (ElasticsearchException ex ) {
            Printer.log.error("Elastic Search: recordWrite failed !");
            throw new IOException(ex);
        }
        return null;
    }

    @Override
    public void sync() throws IOException {
    }

    @Override
    public void close() throws IOException {
        shutDownElasticsearch();
    }


    private void writeData(String data) {
        Map<String, String> document = new HashMap<>();
        document.put("data", data);
        try {
            IndexRequest<Map<String, String>> request = IndexRequest.of(i -> i
                    .index(config.index.trim())
                    .id(String.valueOf(id++))
                    .document(document)
            );
            client.index(request);
        } catch (ElasticsearchException | IOException ex ) {
            Printer.log.error("Elastic Search: recordWrite failed !");
            throw new RuntimeException(ex);
        }
    }

    private void shutDownElasticsearch() {
        try {
            String[] command = {"sh", "-c", "sudo systemctl stop elasticsearch"};
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("Elasticsearch shut down successfully.");
            } else {
                System.out.println("Failed to shut down Elasticsearch.");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}