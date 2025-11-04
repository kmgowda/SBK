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

import io.sbk.action.Action;


/**
 * Supplies the benchmark {@link io.sbk.action.Action} (READ/WRITE variants) selected via CLI.
 *
 * <p>Implementations typically parse the action from command-line options and expose it
 * through {@link #getAction()} so downstream components can adjust behavior accordingly.
 */
public interface ActionParameter {

    /**
     * Get the selected benchmark action.
     *
     * @return the {@link io.sbk.action.Action} to execute
     */
    Action getAction();
}
