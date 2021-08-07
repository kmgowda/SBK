/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.api.impl;

import io.sbk.config.Config;

import java.util.Arrays;

public class SbkDriversParameters extends SbkParameters {
    final private String[] drivers;

    public SbkDriversParameters(String name, String desc, String[] drivers) {
        super(name, desc);
        this.drivers = drivers;

        if (this.drivers != null && this.drivers.length > 0) {
            addOption(Config.CLASS_OPTION, true, "Storage Driver Class,\n Available Drivers "
                    + Arrays.toString(this.drivers));
        }
    }

    public SbkDriversParameters(String name, String[] drivers) {
        this(name, Config.DESC, drivers);
    }

}
