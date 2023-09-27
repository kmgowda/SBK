/**
 * Copyright (c) KMG. All Rights Reserved..
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.driver.Dynamodb;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.api.Storage;
import io.sbk.data.DataType;
import io.sbk.data.impl.ByteArray;
import io.sbk.params.InputOptions;
import io.sbk.params.ParameterOptions;
import io.sbk.system.Printer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;
import software.amazon.awssdk.utils.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;

/**
 * Class for Dynamodb storage driver.
 * <p>
 * Incase if your data type in other than byte[] (Byte Array)
 * then change the datatype and getDataType.
 */
public class Dynamodb implements Storage<byte[]> {
    private final static String CONFIGFILE = "Dynamodb.properties";
    private DynamodbConfig config;

    private DynamoDbClient dynamoDbClient;


    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            config = mapper.readValue(
                    Objects.requireNonNull(Dynamodb.class.getClassLoader().getResourceAsStream(CONFIGFILE)),
                    DynamodbConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }
        params.addOption("endpoint", true, "Dynamodb Endpoint, default endpoint: " + config.endpoint);
        params.addOption("table", true, "Dynamodb Table, default table: " + config.table);
        params.addOption("region", true, "Dynamodb Endpoint, default endpoint: " + config.region);
        params.addOption("awsAccessKey", true, "AWS Access Key, default access key: " + config.awsAccessKey);
        params.addOption("awsSecretKey", true, "AWS Secret Key, default secret key: " + config.awsSecretKey);
        params.addOption("readCapacity", true, "Dynamo Read provision, default read provision: " + config.readCapacity);
        params.addOption("writeCapacity", true, "Dynamo Write provision, default write provision: " + config.writeCapacity);
    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        config.endpoint = params.getOptionValue("endpoint", config.endpoint);
        config.table = params.getOptionValue("table", config.table);
        config.region = params.getOptionValue("region", config.region);
        config.awsAccessKey = params.getOptionValue("awsAccessKey", config.awsAccessKey);
        config.awsSecretKey = params.getOptionValue("awsSecretKey", config.awsSecretKey);
        config.readCapacity = params.getOptionValue("readCapacity", config.readCapacity);
        config.writeCapacity = params.getOptionValue("writeCapacity", config.writeCapacity);
    }

    @Override
    public void openStorage(final ParameterOptions params) throws IOException {
        try {
            if (!StringUtils.isEmpty(config.awsAccessKey) && !StringUtils.isEmpty(config.awsSecretKey)) {
                AwsBasicCredentials basicCredentials = AwsBasicCredentials.create(config.awsAccessKey, config.awsSecretKey);
                dynamoDbClient = DynamoDbClient.builder()
                        .credentialsProvider(StaticCredentialsProvider.create(basicCredentials))
                        .region(Region.of(config.region))
                        .build();
                Printer.log.info("AWS DynamoDb client created.");
            } else {
                dynamoDbClient = DynamoDbClient.builder()
                        .region(Region.of(config.region))
                        .endpointOverride(URI.create(config.endpoint))
                        .build();
                Printer.log.info("Local DynamoDb client created.");
            }
            createTable(dynamoDbClient, config.table);
            Printer.log.info("Storage opened successfully.");
        } catch (Exception e) {
            Printer.log.error("Unable to create dynamo client/table." + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static long generateStartKey(int id) {
        return (long) id * (long) Integer.MAX_VALUE;
    }

    private String createTable(DynamoDbClient ddb, String table) {
        DynamoDbWaiter dbWaiter = ddb.waiter();
        CreateTableRequest request = CreateTableRequest.builder()
                .attributeDefinitions(AttributeDefinition.builder()
                        .attributeName("key")
                        .attributeType(ScalarAttributeType.S)
                        .build())
                .keySchema(KeySchemaElement.builder()
                        .attributeName("key")
                        .keyType(KeyType.HASH)
                        .build())
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(Long.valueOf(config.readCapacity))
                        .writeCapacityUnits(Long.valueOf(config.writeCapacity))
                        .build())
                .tableName(table)
                .build();

        String newTable = "";
        try {
            CreateTableResponse response = ddb.createTable(request);
            DescribeTableRequest tableRequest = DescribeTableRequest.builder()
                    .tableName(table)
                    .build();

            // Wait until the Amazon DynamoDB table is created.
            WaiterResponse<DescribeTableResponse> waiterResponse = dbWaiter.waitUntilTableExists(tableRequest);
            waiterResponse.matched().response().ifPresent(System.out::println);
            newTable = response.tableDescription().tableName();
            Printer.log.info("Table created:" + newTable);
            return newTable;

        } catch (DynamoDbException e) {
            Printer.log.error(e.getMessage());
        }
        Printer.log.error("No table created.");
        return "";
    }

    @Override
    public void closeStorage(final ParameterOptions params) throws IOException {
        dynamoDbClient.close();
    }

    @Override
    public DataWriter<byte[]> createWriter(final int id, final ParameterOptions params) {
        return new DynamodbWriter(id, params, config, dynamoDbClient);
    }

    @Override
    public DataReader<byte[]> createReader(final int id, final ParameterOptions params) {
        return new DynamodbReader(id, params, config, dynamoDbClient);
    }

    @Override
    public DataType<byte[]> getDataType() {
        return new ByteArray();
    }
}
