/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.driver.Solr;

import io.sbk.params.ParameterOptions;
import io.sbk.api.Writer;
import io.sbk.system.Printer;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Class for Solr Writer.
 */
public class SolrWriter implements Writer<String> {
    private final SolrConfig config;
    private final SolrClient client;
    private long id;

    public SolrWriter(int writerID, ParameterOptions params, SolrConfig config, SolrClient client) {
        this.id = Solr.generateStartKey(writerID);
        this.config = config;
        this.client = client;
    }

    @Override
    public CompletableFuture<?> writeAsync(String data) throws IOException {
        try {
            writeData(data);
        } catch (SolrServerException ex) {
            Printer.log.error("Solr: recordWrite failed !");
            throw new IOException(ex);
        }
        return null;
    }

    @Override
    public void sync() throws IOException {
        try {
            client.commit(config.collection);
        } catch (SolrServerException e) {
            Printer.log.error("Solr: sync failed !");
            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        // SolrClient is managed by the main Solr class
    }

    private void writeData(String data) throws SolrServerException, IOException {
        SolrInputDocument document = new SolrInputDocument();
        document.addField("id", String.valueOf(id++));
        document.addField("data", data);
        document.addField("timestamp", System.currentTimeMillis());
        
        try {
            client.add(config.collection, document, config.commitWithinMs);
        } catch (SolrServerException | IOException ex) {
            Printer.log.error("Solr: recordWrite failed !");
            throw new RuntimeException(ex);
        }
    }
}
