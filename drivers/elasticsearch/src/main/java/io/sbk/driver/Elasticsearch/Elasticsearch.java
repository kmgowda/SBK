/**
 * Copyright (c) KMG. All Rights Reserved..
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
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.data.impl.SbkString;
import io.sbk.params.ParameterOptions;
import io.sbk.api.Storage;
import io.sbk.data.DataType;
import io.sbk.params.InputOptions;
import io.sbk.system.Printer;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import java.io.IOException;
import java.util.Objects;

/**
 * Class for Elasticsearch storage driver.
 *
 * Incase if your data type in other than byte[] (Byte Array)
 * then change the datatype and getDataType.
 */
public class Elasticsearch implements Storage<String> {
    private final static String CONFIGFILE = "Elasticsearch.properties";
    private ElasticsearchConfig config;
    private ElasticsearchClient elasticsearchClient;

    public static long generateStartKey(int id) {
        return (long) id * (long) Integer.MAX_VALUE;
    }

    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            config = mapper.readValue(
                    Objects.requireNonNull(Elasticsearch.class.getClassLoader().getResourceAsStream(CONFIGFILE)),
                    ElasticsearchConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        // change and uncomment the below code as per your driver specific parameters
        // params.addOption("param", true, "Elasticsearch parameter, default param: " + config.param);
        params.addOption("user", true, "ElasticSearch user : " + config.user);
        params.addOption("password", true, "ElasticSearch Password " + config.password);
        params.addOption("url", true, "ElasticSearch URL:" + config.url);
        params.addOption("index", true, "ElasticSearch Index: " + config.index);
    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        // change and uncommnet the below code as per your driver specific parameters
        // config.param = params.getOptionValue("param", config.param);
        config.user = params.getOptionValue("user", config.user);
        config.password = params.getOptionValue("password", config.password);
        config.url = params.getOptionValue("url", config.url);
        config.index = params.getOptionValue("index", config.index);
    }

    @Override
    public void openStorage(final ParameterOptions params) throws IOException {
        try {
            elasticsearchClient = connect();
            Printer.log.info("ElasticSearch Client Connected.....");
            String index1 = config.index.trim();
            if (!indexExists(index1)) {
                createIndex(elasticsearchClient, index1);
            }
        } catch (ElasticsearchException e ) {
            Printer.log.error(e.getMessage());
            throw  new RuntimeException(e);
        }
    }

    private ElasticsearchClient connect() {
        Printer.log.info("Attempting to connect to Elasticsearch...");
        try {
            final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(config.user, config.password));

            RestClientBuilder builder = RestClient.builder(HttpHost.create(config.url))
                    .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));

            RestClient restClient = builder.build();
            ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
            return new ElasticsearchClient(transport);
        } catch (ElasticsearchException e) {
            Printer.log.error("Error connecting to Elasticsearch: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private  boolean indexExists(String indexName) {
        try {
            BooleanResponse response = elasticsearchClient.
                    indices().exists(e -> e.index(indexName));
            return response.value();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private  void createIndex(ElasticsearchClient client, String indexName) {
        try {
            CreateIndexRequest createIndexRequest = CreateIndexRequest.of(c -> c.index(indexName));
            CreateIndexResponse createIndexResponse = client.indices().create(createIndexRequest);

            if (createIndexResponse.acknowledged()) {
                Printer.log.info(indexName + "Created Successfully");
            } else {
                Printer.log.info("Index creation was not acknowledged.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void shutDownElasticsearch() {
        //shut down for linux system (optional)
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

    @Override
    public void closeStorage(final ParameterOptions params) throws IOException {
        shutDownElasticsearch();
    }

    @Override
    public DataWriter<String> createWriter(final int id, final ParameterOptions params) {
        return new ElasticsearchWriter(id, params, config, elasticsearchClient);
    }

    @Override
    public DataReader<String> createReader(final int id, final ParameterOptions params) {
        return new ElasticsearchReader(id, params, config, elasticsearchClient);
    }

    @Override
    public DataType<String> getDataType() {
        return new SbkString();
    }
}