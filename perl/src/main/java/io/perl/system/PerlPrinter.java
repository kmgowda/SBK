/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perl.system;

import io.perl.config.PerlConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final public class PerlPrinter {
    final private static String LOGGER_NAME = PerlConfig.NAME;
    final public static Logger log = LoggerFactory.getLogger(LOGGER_NAME);
}
