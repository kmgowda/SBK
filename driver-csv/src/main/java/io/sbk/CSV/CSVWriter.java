/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.CSV;

import io.sbk.api.Parameters;
import io.sbk.api.Writer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;


/**
 * Class for File Channel Writer.
 */
public class CSVWriter implements Writer<String> {
    private CSVPrinter csvPrinter;
    private long key;

    public CSVWriter(int id, Parameters params, CSVConfig config) throws IOException {
        java.io.File file = new java.io.File(config.fileName);
        file.delete();
        csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(config.fileName)), CSVFormat.DEFAULT
                .withHeader("Key", "text"));
        key = 0;
    }

    @Override
    public CompletableFuture<Void> writeAsync(String data) throws IOException {
        csvPrinter.printRecord(key++, data);
        return null;
    }

    @Override
    public void sync() throws IOException {
        csvPrinter.flush();
    }

    @Override
    public void close() throws  IOException {
        csvPrinter.close();
    }
}