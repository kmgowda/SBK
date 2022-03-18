/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.Couchbase;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;

/**
 * Class for Couchbase storage configuration.
 */
public class CouchbaseConfig {
    // Add Couchbase Storage driver configuration parameters
    String url;
    String bucketName;
    String user;
    String pass;
    Cluster cluster;
    Bucket bucket;
}