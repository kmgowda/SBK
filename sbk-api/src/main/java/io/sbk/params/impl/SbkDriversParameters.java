/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.params.impl;

import io.sbk.config.Config;
import java.util.Arrays;

/**
 * Parameters implementation that exposes storage driver and logger selection options.
 *
 * <p>Augments {@link SbkParameters} by adding {@code -class} (storage driver) and
 * {@code -logger} (logger driver) options. The available values are provided via the
 * constructor and rendered in help text.
 */
public non-sealed class SbkDriversParameters extends SbkParameters {
    final private String[] drivers;
    final private String[] loggers;

    /**
     * Create parameters with custom help description and available drivers/loggers.
     *
     * @param name    benchmark name (used in help header)
     * @param desc    help description
     * @param drivers allowed storage driver class names to list in help
     * @param loggers allowed logger class names to list in help
     */
    public SbkDriversParameters(String name, String desc, String[] drivers, String[] loggers) {
        super(name, desc);

        if (drivers != null && drivers.length > 0) {
            this.drivers = drivers.clone();
            addOption(Config.CLASS_OPTION, true, "Storage Driver Class,\n Available Drivers "
                    + Arrays.toString(this.drivers));
        } else {
            this.drivers = new String[]{""};
        }

        if (loggers != null && loggers.length > 0) {
            this.loggers = loggers.clone();
        } else {
            this.loggers = new String[]{""};
        }
        addOption(Config.LOGGER_OPTION, true, "Logger Driver Class,\n Available Drivers "
                + Arrays.toString(this.loggers));
    }

    /**
     * Convenience constructor using the default description.
     *
     * @param name    benchmark name
     * @param drivers allowed storage driver class names
     * @param loggers allowed logger class names
     */
    public SbkDriversParameters(String name, String[] drivers, String[] loggers) {
        this(name, Config.DESC, drivers, loggers);
    }

}
