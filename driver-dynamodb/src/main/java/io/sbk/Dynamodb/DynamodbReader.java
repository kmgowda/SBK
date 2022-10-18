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

import io.sbk.api.Reader;
import io.sbk.params.ParameterOptions;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Class for Dynamodb Reader.
 */
public class DynamodbReader implements Reader<byte[]> {

    private DynamoDbClient ddb;
    final private ParameterOptions params;

    private DynamodbConfig config;

    private long key;

    public DynamodbReader(int readerId, ParameterOptions params, DynamodbConfig config, DynamoDbClient ddb) {
        this.key = Dynamodb.generateStartKey(readerId);
        this.params = params;
        this.config = config;
        this.ddb = ddb;
    }

    @Override
    public byte[] read() throws IOException {
        HashMap<String, AttributeValue> keyToGet = new HashMap<>();
        keyToGet.put("key", AttributeValue.builder()
                .s(Long.toString(key))
                .build());

        GetItemRequest request = GetItemRequest.builder()
                .key(keyToGet)
                .tableName(config.table)
                .build();
        try {
            Map<String, AttributeValue> returnedItem = ddb.getItem(request).item();
            if (returnedItem != null) {
                return returnedItem.get("data").b().asByteArray();
            }
        } catch (DynamoDbException e) {
            throw new RuntimeException();
        }
        return null;
    }

    @Override
    public void close() throws IOException {
    }
}