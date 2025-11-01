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
 * Composite interface bundling option registration, parsing, and typed parameter access.
 *
 * <p>Implementations both build the CLI schema (via {@link InputOptions}), parse arguments
 * ({@link ParseInputOptions}), and expose strongly-typed parameter getters ({@link Parameters})
 * transitively through {@link ParameterOptions}.
 */
public non-sealed interface InputParameterOptions extends ParseInputOptions, ParameterOptions {
}
