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

import io.sbk.config.Config;
import io.sbk.parameters.ParameterOptions;
import io.sbk.api.Reader;
import io.sbk.api.Writer;
import io.sbk.system.Printer;
import io.sbk.parameters.impl.SbkDriversParameters;
import org.junit.Assert;
import org.junit.Test;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Class for File System Interface.
 */
public class FileTest {
    final String[] drivers = {"File"};
    final String benchmarkName = Config.NAME + " -class file";
    private File file;
    private ParameterOptions params;

    @Test
    public void testParseArgs() {
        final String[] args = {"-class", "file", "-size", "100", "-writers", "1", "records", "1"};
        params = new SbkDriversParameters(benchmarkName,  drivers);
        file = new File();
        file.addArgs(params);
        try {
            params.parseArgs(args);
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Parse Args Failed!");
        }
        file.parseArgs(params);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseArgsWritersCount() {
        final String[] args = {"-class", "file", "-size", "100", "-writers", "2", "records", "1"};
        params = new SbkDriversParameters(benchmarkName,  drivers);
        file = new File();
        file.addArgs(params);
        try {
            params.parseArgs(args);
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Parse Args Failed!");
        }
        file.parseArgs(params);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseArgsReadersWritersCount() {
        final String[] args = {"-class", "file", "-size", "100", "-readers", "1", "-writers", "1", "records", "1"};
        params = new SbkDriversParameters(benchmarkName,  drivers);
        file = new File();
        file.addArgs(params);
        try {
            params.parseArgs(args);
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Parse Args Failed!");
        }
        file.parseArgs(params);
    }

    @Test
    public void testOpenAndCloseStorage() {
        final String[] args = {"-class", "file", "-size", "100", "-writers", "1", "records", "1"};
        params = new SbkDriversParameters(benchmarkName,  drivers);
        file = new File();
        file.addArgs(params);
        try {
            params.parseArgs(args);
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Parse Args Failed!");
        }
        file.parseArgs(params);
        try {
            file.openStorage(params);
            file.closeStorage(params);
        } catch (IOException ex) {
            ex.printStackTrace();
            Assert.fail("Open/Close Storage Failed!");
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
        params = new SbkDriversParameters(benchmarkName,  drivers);
        file = new File();
        file.addArgs(params);
        try {
            params.parseArgs(args);
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Parse Args Failed!");
        }
        file.parseArgs(params);
        try {
            file.createWriter(0, params);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("create Writer Failed!");
        }
    }

    @Test
    public void testCreateReader() {
        final String[] writeArgs = {"-class", "file", "-file", "test.txt", "-size", "100", "-writers", "1", "records", "1"};
        final String[] readArgs = {"-class", "file", "-file", "test.txt", "-size", "100", "-readers", "1", "records", "1"};
        params = new SbkDriversParameters(benchmarkName,  drivers);
        file = new File();
        file.addArgs(params);
        try {
            params.parseArgs(writeArgs);
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail();
        }
        file.parseArgs(params);
        try {
            file.createWriter(0, params);
        } catch (IOException ex) {
            ex.printStackTrace();
            Assert.fail("CreateWriter failed!");
        }
        file = new File();
        file.getDataType();
        file.addArgs(params);
        try {
            params.parseArgs(readArgs);
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Parse Args Failed!");
        }
        file.parseArgs(params);
        try {
            file.createReader(0, params);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("createReader failed!");
        }
    }

    @Test
    public void testCreateReaderFileNotFound() {
        final String[] args = {"-class", "file", "-file", "NoFile.sbk", "-size", "100", "-readers", "1", "records", "1"};
        Exception retEx = null;
        params = new SbkDriversParameters(benchmarkName,  drivers);
        file = new File();
        file.getDataType();
        file.addArgs(params);
        try {
            params.parseArgs(args);
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Parse Args Failed!");
        }
        file.parseArgs(params);
        try {
            file.createReader(0, params);
        } catch (IOException ex) {
           Printer.log.info(ex.toString());
           retEx = ex;
        }
        assertNotNull(retEx);
    }

    // This test case works only if the append mode is disabled
    @Test
    public void testWriterReaderData() {
        final String data = "KMG-SBK";
        final String[] writeArgs = {"-class", "file", "-file", "unit-1.test", "-size", Integer.toString(data.length()), "-writers", "1", "records", "1"};
        final String[] readArgs = {"-class", "file", "-file", "unit-1.test", "-size", Integer.toString(data.length()), "-readers", "1", "records", "1"};
        Writer<ByteBuffer> writer = null;
        Reader<ByteBuffer> reader = null;
        ByteBuffer writeBuffer = null;
        ByteBuffer readBuffer = null;
        String readData = null;

        params = new SbkDriversParameters(benchmarkName,  drivers);
        file = new File();
        file.addArgs(params);
        try {
            params.parseArgs(writeArgs);
        } catch (Exception ex) {
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
        try {
            writer = (Writer<ByteBuffer>) file.createWriter(0, params);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("createWriter Failed");
        }
        byte[] byteData = data.getBytes();
        writeBuffer = file.getDataType().allocate(byteData.length);
        for (int i = 0; i < byteData.length; i++) {
            writeBuffer.put(byteData[i]);
        }
        writeBuffer.flip();
        try {
            writer.writeAsync(writeBuffer);
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
        } catch (Exception ex) {
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

        try {
            reader = (Reader<ByteBuffer>) file.createReader(0, params);
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
        Printer.log.info("Write Data: " + data);
        Printer.log.info("Reader Data: " + readData);
        assertEquals(0, data.compareTo(readData));
        writeBuffer.flip();
        Printer.log.info("WriteBuffer : " + writeBuffer.toString());
        Printer.log.info("ReaderBuffer: " + readBuffer.toString());
        assertEquals(0, writeBuffer.compareTo(readBuffer));
    }

    // This test case works only if the append mode is disabled
    @Test
    public void testReaderEOF() {
        final String data = "KMG-SBK";
        final String[] writeArgs = {"-class", "file", "-file", "unit-1.test", "-size", Integer.toString(data.length()), "-writers", "1", "records", "1"};
        final String[] readArgs = {"-class", "file", "-file", "unit-1.test", "-size", Integer.toString(data.length()), "-readers", "1", "records", "1"};
        Writer<ByteBuffer> writer = null;
        Reader<ByteBuffer> reader = null;
        ByteBuffer writeBuffer = null;
        ByteBuffer readBuffer = null;
        String readData = null;

        params = new SbkDriversParameters(benchmarkName,  drivers);
        file = new File();
        file.addArgs(params);
        try {
            params.parseArgs(writeArgs);
        } catch (Exception ex) {
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
        try {
            writer = (Writer<ByteBuffer>) file.createWriter(0, params);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("createWriter Failed");
        }
        byte[] byteData = data.getBytes();
        writeBuffer = file.getDataType().allocate(byteData.length);
        for (int i = 0; i < byteData.length; i++) {
            writeBuffer.put(byteData[i]);
        }
        writeBuffer.flip();
        try {
            writer.writeAsync(writeBuffer);
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
        } catch (Exception ex) {
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

        try {
            reader = (Reader<ByteBuffer>) file.createReader(0, params);
            readBuffer = reader.read();
        } catch (IOException ex) {
            ex.printStackTrace();
            Assert.fail("Reader Failed");
        }

        byte[] arr = new byte[readBuffer.limit()];
        for (int i = 0; i < readBuffer.limit(); i++) {
            arr[i] = readBuffer.get();
        }
        readData = new String(arr, StandardCharsets.UTF_8);
        Printer.log.info("Write Data: " + data);
        Printer.log.info("Reader Data: " + readData);
        assertEquals(0, data.compareTo(readData));
        writeBuffer.flip();
        Printer.log.info("WriteBuffer : " + writeBuffer.toString());
        Printer.log.info("ReaderBuffer: " + readBuffer.toString());
        assertEquals(0, writeBuffer.compareTo(readBuffer));
        // read again
        try {
            reader.read();
        } catch (EOFException ex) {
            Printer.log.info("Got EOF Expected");
        } catch (IOException ex) {
            ex.printStackTrace();
            Assert.fail("Reader Failed");
        }
    }
}
