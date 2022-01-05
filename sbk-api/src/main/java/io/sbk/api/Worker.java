/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api;

import io.perl.PerlChannel;

/**
 * Abstract class for Writers and Readers.
 */
public abstract class Worker {
    public final int id;
    public final int recordIDMax;
    public final Parameters params;
    public final PerlChannel perlChannel;

    public Worker(int workerID, int idMax, Parameters params, PerlChannel perlChannel) {
        this.id = workerID;
        this.recordIDMax = idMax;
        this.params = params;
        this.perlChannel = perlChannel;
    }
}
