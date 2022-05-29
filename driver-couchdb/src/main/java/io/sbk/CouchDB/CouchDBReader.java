/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.CouchDB;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sbk.params.ParameterOptions;
import io.sbk.api.Reader;
import org.ektorp.CouchDbConnector;
import org.ektorp.DocumentNotFoundException;

import java.io.EOFException;
import java.io.IOException;
import java.util.Map;


/**
 * Class for Reader.
 */
public class CouchDBReader implements Reader<String> {
    final private CouchDbConnector db;
    final private ParameterOptions params;
    private long key;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CouchDBReader(int id, ParameterOptions params, CouchDBConfig config, CouchDbConnector db) throws IOException {
        this.key = CouchDB.generateStartKey(id);
        this.params = params;
        this.db = db;
    }

    @Override
    public String read() throws EOFException, IOException {
        String k = Long.toString(key);
        try {
            Map<String, Object> map = db.get(Map.class, k);
            if (map != null) {
                key++;
                return (String) map.get("data");
            }
        } catch (DocumentNotFoundException ex) {
            throw new EOFException("Key : " + k + "not found");
        }
        return null;
    }

    @Override
    public void close() throws IOException {
    }
}