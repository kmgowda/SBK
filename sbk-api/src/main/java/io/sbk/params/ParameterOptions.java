/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.params;

/**
 * Composite interface combining parsed CLI access with typed benchmark parameters.
 *
 * <p>Extends {@link ParsedOptions} for raw option/value retrieval and {@link Parameters}
 * for strongly-typed accessors (writers, readers, sizes, steps, etc.).
 */
public sealed interface ParameterOptions extends ParsedOptions, Parameters permits InputParameterOptions {

}
