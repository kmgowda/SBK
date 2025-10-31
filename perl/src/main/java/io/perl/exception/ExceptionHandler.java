/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perl.exception;

/**
 * Simple callback interface for reporting exceptions from PerL internals to
 * caller code. Implementations may rethrow, log, or otherwise handle the
 * provided Throwable.
 */
public interface ExceptionHandler {

    /**
     * Report or propagate an exception occurred inside the PerL runtime.
     * Implementations may choose to rethrow the exception or record it.
     *
     * @param ex Throwable to handle
     */
    void throwException(Throwable ex);
}
