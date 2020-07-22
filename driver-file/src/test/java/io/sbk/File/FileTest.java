/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.File;

import io.sbk.api.Parameters;
import io.sbk.api.Reader;
import io.sbk.api.Writer;
import io.sbk.api.impl.SbkLogger;
import io.sbk.api.impl.SbkParameters;
import org.apache.commons.cli.ParseException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


/**
 * Class for File System Interface.
 */
public class FileTest {
    final String[] drivers = {"File"};
    final List<String> driversList = Arrays.asList( drivers );
    private File file;
    private Parameters params;

    @Test
    public void testParseArgs() {
        final String[] args = {"-class", "file", "-size", "100", "-writers", "1", "records", "1"};
        params = new SbkParameters("SBK", "File System Benchmarking",
                "File", driversList, System.currentTimeMillis());
        file = new File();
        file.addArgs(params);
        try {
            params.parseArgs(args);
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        file.parseArgs(params);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseArgsWritersCount() {
        final String[] args = {"-class", "file", "-size", "100", "-writers", "2", "records", "1"};
        params = new SbkParameters("SBK", "File System Benchmarking",
                "File", driversList, System.currentTimeMillis());
        file = new File();
        file.addArgs(params);
        try {
            params.parseArgs(args);
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        file.parseArgs(params);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseArgsReadersWritersCount() {
        final String[] args = {"-class", "file", "-size", "100", "-readers", "1", "-writers", "1", "records", "1"};
        params = new SbkParameters("SBK", "File System Benchmarking",
                "File", driversList, System.currentTimeMillis());
        file = new File();
        file.addArgs(params);
        try {
            params.parseArgs(args);
        } catch (ParseException ex) {
            ex.printStackTrace();
            Assert.fail();
        }
        file.parseArgs(params);
    }

    @Test
    public void testOpenAndCloseStorage() {
        final String[] args = {"-class", "file", "-size", "100", "-writers", "1", "records", "1"};
        params = new SbkParameters("SBK", "File System Benchmarking",
                "File", driversList, System.currentTimeMillis());
        file = new File();
        file.addArgs(params);
        try {
            params.parseArgs(args);
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        file.parseArgs(params);
        try {
            file.openStorage(params);
            file.closeStorage(params);
        } catch (IOException ex) {
            ex.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testGetDataType() {
        file = new File();
        file.getDataType();
     }


    @Test
    public void testCreateWriter() {
        final String[] args = {"-class", "file", "-file", "test.txt", "-size", "100", "-writers", "1", "records", "1"};
        params = new SbkParameters("SBK", "File System Benchmarking",
                "File", driversList, System.currentTimeMillis());
        file = new File();
        file.addArgs(params);
        try {
            params.parseArgs(args);
        } catch (ParseException ex) {
            ex.printStackTrace();
            Assert.fail();
        }
        file.parseArgs(params);
        file.createWriter(0, params);
    }

    @Test
    public void testCreateReader() {
        final String[] writeArgs = {"-class", "file", "-file", "test.txt", "-size", "100", "-writers", "1", "records", "1"};
        final String[] readArgs = {"-class", "file", "-file", "test.txt", "-size", "100", "-readers", "1", "records", "1"};
        params = new SbkParameters("SBK", "File System Benchmarking",
                "File", driversList, System.currentTimeMillis());
        file = new File();
        file.addArgs(params);
        try {
            params.parseArgs(writeArgs);
        } catch (ParseException ex) {
            ex.printStackTrace();
            Assert.fail();
        }
        file.parseArgs(params);
        file.createWriter(0, params);
        file = new File();
        file.getDataType();
        file.addArgs(params);
        try {
            params.parseArgs(readArgs);
        } catch (ParseException ex) {
            ex.printStackTrace();
            Assert.fail();
        }
        file.parseArgs(params);
        file.createReader(0, params);
    }

    @Test
    public void testCreateReaderFileNotFound() {
        final String[] args = {"-class", "file", "-file", "NoFile.sbk", "-size", "100", "-readers", "1", "records", "1"};
        params = new SbkParameters("SBK", "File System Benchmarking",
                "File", driversList, System.currentTimeMillis());
        file = new File();
        file.getDataType();
        file.addArgs(params);
        try {
            params.parseArgs(args);
        } catch (ParseException ex) {
            ex.printStackTrace();
            Assert.fail();
        }
        file.parseArgs(params);
        assertNull(file.createReader(0, params));
    }

    @Test
    public void testWriterReaderData() {
        final String data = "KMG-SBK";
        final String[] writeArgs = {"-class", "file", "-file", "unit.test", "-size", Integer.toString(data.length()), "-writers", "1", "records", "1"};
        final String[] readArgs = {"-class", "file", "-file", "unit.test", "-size", Integer.toString(data.length()), "-readers", "1", "records", "1"};
        final Writer<ByteBuffer> writer;
        final Reader<ByteBuffer> reader;
        ByteBuffer writeBuffer = null;
        ByteBuffer readBuffer = null;
        String readData = null;

        writeBuffer = ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8));
        params = new SbkParameters("SBK", "File System Benchmarking",
                "File", driversList, System.currentTimeMillis());
        file = new File();
        file.addArgs(params);
        try {
            params.parseArgs(writeArgs);
        } catch (ParseException ex) {
            ex.printStackTrace();
            Assert.fail("Parse Arg failed");
        }
        file.parseArgs(params);
        try {
            file.openStorage(params);
        } catch (IOException ex) {
            ex.printStackTrace();
            Assert.fail("open storage Failed");
        }
        writer = file.createWriter(0, params);
        try {
            writer.writeAsync(writeBuffer.slice());
            writer.close();
            file.closeStorage(params);
        } catch (IOException ex) {
            ex.printStackTrace();
            Assert.fail("Writer Failed");
        }
        file = new File();
        file.getDataType();
        file.addArgs(params);
        try {
            params.parseArgs(readArgs);
        } catch (ParseException ex) {
            ex.printStackTrace();
            Assert.fail("Parse Args Failed");
        }
        file.parseArgs(params);
        try {
            file.openStorage(params);
        } catch (IOException ex) {
            ex.printStackTrace();
            Assert.fail("open storage Failed");
        }
        reader = file.createReader(0, params);
        try {
            readBuffer = reader.read();
            reader.close();
            file.closeStorage(params);
        } catch (IOException ex) {
            ex.printStackTrace();
            Assert.fail("Reader Failed");
        }

        byte[] arr = new byte[readBuffer.limit()];
        for (int i = 0; i < readBuffer.limit(); i++) {
            arr[i] = readBuffer.get();
        }
        readData = new String(arr, StandardCharsets.UTF_8);
        SbkLogger.log.info("Write Data: " + data);
        SbkLogger.log.info("Reader Data: " + readData);
        assertEquals(0, data.compareTo(readData));
        writeBuffer.flip();
        SbkLogger.log.info("WriteBuffer : " + writeBuffer.toString());
        SbkLogger.log.info("ReaderBuffer: " + readBuffer.toString());
        assertEquals(0, writeBuffer.compareTo(readBuffer));
    }


}
