/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.exception;

/**
 * Signals that SBK-GEM already reported a command-line validation failure and printed its help text.
 */
public final class SbkGemParameterException extends IllegalArgumentException {

    /**
     * Create a reported parameter exception.
     *
     * @param cause original command-line validation failure
     */
    public SbkGemParameterException(IllegalArgumentException cause) {
        super(cause.getMessage(), cause);
    }
}
