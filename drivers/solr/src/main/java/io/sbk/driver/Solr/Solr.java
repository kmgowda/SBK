/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.driver.Solr;

import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.data.impl.SbkString;
import io.sbk.params.ParameterOptions;
import io.sbk.api.Storage;
import io.sbk.data.DataType;
import io.sbk.params.InputOptions;
import io.sbk.system.Printer;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.apache.solr.client.solrj.SolrQuery;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.javaprop.JavaPropsFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Class for Solr storage driver.
 *
 * In case if your data type in other than String
 * then change the datatype and getDataType.
 */
public class Solr implements Storage<String> {
    private final static String CONFIGFILE = "Solr.properties";
    private SolrConfig config;
    private SolrClient solrClient;

    public static long generateStartKey(int id) {
        return (long) id * (long) Integer.MAX_VALUE;
    }

    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory());
        try {
            config = mapper.readValue(
                    Objects.requireNonNull(Solr.class.getClassLoader().getResourceAsStream(CONFIGFILE)),
                    SolrConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        params.addOption("zookeeperHost", true, "Solr ZooKeeper host, default: " + config.zookeeperHost);
        params.addOption("zookeeperPort", true, "Solr ZooKeeper port, default: " + config.zookeeperPort);
        params.addOption("collection", true, "Solr collection name, default: " + config.collection);
        params.addOption("solrUrl", true, "Solr URL, default: " + config.solrUrl);
        params.addOption("username", true, "Solr username, default: " + config.username);
        params.addOption("password", true, "Solr password, default: " + config.password);
        params.addOption("batchSize", true, "Batch size for writes, default: " + config.batchSize);
        params.addOption("commitWithinMs", true, "Commit within time in ms, default: " + config.commitWithinMs);
    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        config.zookeeperHost = params.getOptionValue("zookeeperHost", config.zookeeperHost);
        config.zookeeperPort = params.getOptionValue("zookeeperPort", config.zookeeperPort);
        config.collection = params.getOptionValue("collection", config.collection);
        config.solrUrl = params.getOptionValue("solrUrl", config.solrUrl);
        config.username = params.getOptionValue("username", config.username);
        config.password = params.getOptionValue("password", config.password);
        config.batchSize = Integer.parseInt(params.getOptionValue("batchSize", String.valueOf(config.batchSize)));
        config.commitWithinMs = Integer.parseInt(params.getOptionValue("commitWithinMs", String.valueOf(config.commitWithinMs)));
    }

    @Override
    public void openStorage(final ParameterOptions params) throws IOException {
        try {
            solrClient = connect();
            Printer.log.info("Solr Client Connected.....");
            
            // Check if collection exists, if not create it
            if (!collectionExists(config.collection)) {
                createCollection(config.collection);
            }
        } catch (Exception e) {
            Printer.log.error(e.getMessage());
            throw new IOException(e);
        }
    }

    private SolrClient connect() {
        Printer.log.info("Attempting to connect to Solr...");
        try {
            // Use CloudSolrClient for SolrCloud mode with ZooKeeper
            String zkHost = config.zookeeperHost + ":" + config.zookeeperPort;
            CloudSolrClient cloudSolrClient = new CloudSolrClient.Builder(Arrays.asList(zkHost)).build();
            cloudSolrClient.setDefaultCollection(config.collection);
            
            // If authentication is provided, configure it
            if (config.username != null && !config.username.isEmpty()) {
                // Note: Solr authentication setup would go here
                // For simplicity, we're not implementing full auth in this example
            }
            
            return cloudSolrClient;
        } catch (Exception e) {
            Printer.log.error("Error connecting to Solr: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private boolean collectionExists(String collectionName) {
        try {
            // Simple approach: try to query the collection to see if it exists
            SolrQuery query = new SolrQuery("*:*");
            query.setRows(0); // Just check if collection exists, don't get data
            solrClient.query(collectionName, query);
            return true; // If query succeeds, collection exists
        } catch (Exception e) {
            // If query fails, collection likely doesn't exist
            return false;
        }
    }

    private void createCollection(String collectionName) {
        try {
            CollectionAdminRequest.Create createRequest = CollectionAdminRequest.createCollection(collectionName, "sbk_config", 1, 1);
            CollectionAdminResponse response = createRequest.process(solrClient);
            
            if (response.isSuccess()) {
                Printer.log.info("Collection " + collectionName + " created successfully");
            } else {
                Printer.log.error("Failed to create collection: " + response.getStatus());
            }
        } catch (Exception e) {
            Printer.log.error("Error creating collection: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void closeStorage(final ParameterOptions params) throws IOException {
        try {
            if (solrClient != null) {
                solrClient.close();
            }
        } catch (Exception e) {
            Printer.log.error("Failed to close Solr connection: " + e.getMessage());
        }
    }

    @Override
    public DataWriter<String> createWriter(final int id, final ParameterOptions params) {
        return new SolrWriter(id, params, config, solrClient);
    }

    @Override
    public DataReader<String> createReader(final int id, final ParameterOptions params) {
        return new SolrReader(id, params, config, solrClient);
    }

    @Override
    public DataType<String> getDataType() {
        return new SbkString();
    }
}
