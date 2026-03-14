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

public class ChromaDBConfig {
    public String host = "localhost";
    public int port = 8000;
    public String collectionName = "sbk_benchmark";
    public int embeddingDimension = 384;
    public String distanceFunction = "cosine";
    public boolean ssl = false;
    public String authToken = "";
    public int timeoutSeconds = 30;
    public int maxRetries = 3;
    public int batchSize = 100;
}
