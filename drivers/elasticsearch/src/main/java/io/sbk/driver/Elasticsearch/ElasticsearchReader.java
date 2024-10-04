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
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import io.sbk.params.ParameterOptions;
import io.sbk.api.Reader;
import io.sbk.system.Printer;

import java.io.IOException;
import java.util.Map;

/**
 * Class for Elasticsearch Reader.
 */
public class ElasticsearchReader implements Reader<String> {
    private final ElasticsearchConfig config;
    private final ElasticsearchClient client;
    private long id;

    public ElasticsearchReader(int readerId, ParameterOptions params, ElasticsearchConfig config, ElasticsearchClient client) {
        this.id = Elasticsearch.generateStartKey(readerId);
        this.config = config;
        this.client = client;
    }

    @Override
    public String read() throws IOException {
        try {
            GetRequest request = GetRequest.of(g -> g
                    .index(config.index.trim())
                    .id(String.valueOf(id++))
            );
            GetResponse<Map> response = client.get(request, Map.class);
            return response.fields().toString();
        } catch (ElasticsearchException e) {
            Printer.log.error("Elastic Search: recordRead failed !");
            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        shutDownElasticsearch();
    }

    private void shutDownElasticsearch() {
        try {
            String[] command = {"sh", "-c", "sudo systemctl stop elasticsearch"};
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                Printer.log.info("Elasticsearch shut down successfully.");
            } else {
                Printer.log.info("Failed to shut down Elasticsearch.");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}