/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.Dynamodb;

/**
 * Class for Dynamodb storage configuration.
 */
public class DynamodbConfig {
    public String endpoint;
    public String table;
    public String region;
    public String awsAccessKey;
    public String awsSecretKey;

    public String readCapacity;

    public String writeCapacity;
}