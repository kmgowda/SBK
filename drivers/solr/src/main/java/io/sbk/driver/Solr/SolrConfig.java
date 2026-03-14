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

/**
 * Class for Solr storage configuration.
 */
public class SolrConfig {
    // Add Solr Storage driver configuration parameters
    public String zookeeperHost;
    public String zookeeperPort;
    public String collection;
    public String solrUrl;
    public String username;
    public String password;
    public int batchSize;
    public int commitWithinMs;
}
