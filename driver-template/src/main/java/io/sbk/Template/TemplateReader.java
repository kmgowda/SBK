/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Template;

import io.sbk.api.Reader;
import io.sbk.parameters.ParameterOptions;

import java.io.IOException;

/**
 * Class for Reader.
 */
public class TemplateReader implements Reader<byte[]> {

    public TemplateReader(int readerId, ParameterOptions params) {
    }

    @Override
    public byte[] read() throws IOException {
         throw new IOException("Its Template Reader Driver");
    }

    @Override
    public void close() throws IOException {
         throw new IOException("Its Template Reader Driver");
    }
}