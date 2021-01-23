/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Template;
import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.api.Storage;
import io.sbk.api.Parameters;

import java.io.IOException;

/**
 * Class for Storage driver.
 */
public class Template implements Storage<byte[]> {

    @Override
    public void addArgs(final Parameters params) throws IllegalArgumentException {

    }

    @Override
    public void parseArgs(final Parameters params) throws IllegalArgumentException {
    }

    @Override
    public void openStorage(final Parameters params) throws  IOException {
    }

    @Override
    public void closeStorage(final Parameters params) throws IOException {
    }

    @Override
    public DataWriter<byte[]> createWriter(final int id, final Parameters params) {
        return new TemplateWriter(id, params);
    }

    @Override
    public DataReader<byte[]> createReader(final int id, final Parameters params) {
        return new TemplateReader(id, params);
    }
}
