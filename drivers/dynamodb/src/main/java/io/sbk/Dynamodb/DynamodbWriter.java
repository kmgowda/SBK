/**
 * Copyright (c) KMG. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Dynamodb;

import io.sbk.api.Writer;
import io.sbk.params.ParameterOptions;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;


/**
 * Class for Dynamodb Writer.
 */
public class DynamodbWriter implements Writer<byte[]> {

    private DynamoDbClient ddb;
    final private ParameterOptions params;

    private DynamodbConfig config;

    private long key;

    public DynamodbWriter(int writerID, ParameterOptions params, DynamodbConfig config, DynamoDbClient ddb) {
        this.key = Dynamodb.generateStartKey(writerID);
        this.params = params;
        this.config = config;
        this.ddb = ddb;
    }

    @Override
    public CompletableFuture writeAsync(byte[] data) throws IOException {
        HashMap<String, AttributeValue> itemValues = new HashMap<>();
        itemValues.put("key", AttributeValue.builder().s(Long.toString(key++)).build());
        itemValues.put("data", AttributeValue.builder().b(SdkBytes.fromByteArray(data)).build());
        PutItemRequest request = PutItemRequest.builder().tableName(config.table).item(itemValues).build();
        ddb.putItem(request);
        return null;
    }

    @Override
    public void sync() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }
}