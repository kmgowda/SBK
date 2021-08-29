/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.CSV;

import io.sbk.api.ParameterOptions;
import io.sbk.api.Reader;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;

/**
 * Class for File Reader.
 */
public class CSVReader implements Reader<String> {
    final private CSVParser csvParser;
    final private Iterator<CSVRecord> csvIterator;

    public CSVReader(int id, ParameterOptions params, CSVConfig config) throws IOException {
        csvParser = new CSVParser(Files.newBufferedReader(Paths.get(config.fileName)), CSVFormat.DEFAULT
                .withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());
        csvIterator = csvParser.iterator();

    }

    @Override
    public String read() throws IOException {
        if (csvIterator.hasNext()) {
            return csvIterator.next().get(1);
        }
        throw new EOFException();
    }

    @Override
    public void close() throws IOException {
        csvParser.close();
    }
}