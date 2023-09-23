/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.logger.impl;

import io.sbk.logger.GemLogger;
import io.sbm.logger.impl.SbmPrometheusLogger;

import java.io.IOException;

public abstract class AbstractGemLogger extends SbmPrometheusLogger implements GemLogger {
    @Override
    public String[] getOptionsArgs() {
        try {
            throw new IOException("The getOptionsArgs method is not overridden/implemented\n");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public String[] getParsedArgs() {
        try {
            throw new IOException("The getParsedArgs method is not overridden/implemented\n");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
