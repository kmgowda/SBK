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
import io.sbk.api.Reader;
import io.sbk.system.Printer;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;

import java.io.EOFException;
import java.io.IOException;

/**
 * Class for Solr Reader.
 */
public class SolrReader implements Reader<String> {
    private final SolrConfig config;
    private final SolrClient client;
    private long id;

    public SolrReader(int readerId, ParameterOptions params, SolrConfig config, SolrClient client) {
        this.id = Solr.generateStartKey(readerId);
        this.config = config;
        this.client = client;
    }

    @Override
    public String read() throws EOFException, IOException {
        try {
            SolrQuery query = new SolrQuery();
            query.setQuery("id:" + id);
            query.setRows(1);
            
            QueryResponse response = client.query(config.collection, query);
            
            if (response.getResults().getNumFound() == 0) {
                throw new EOFException("No more data to read from Solr");
            }
            
            SolrDocument document = response.getResults().get(0);
            id++;
            return (String) document.getFieldValue("data");
            
        } catch (SolrServerException e) {
            Printer.log.error("Solr: recordRead failed !");
            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        // SolrClient is managed by the main Solr class
    }
}
