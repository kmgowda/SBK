/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.Artemis;

public class ArtemisClientConfig {
    public String uri;
    public String user;
    public String password;
    public boolean xa;
    public boolean autoCommitSends;
    public boolean autoCommitAcks;
    public boolean preAcknowledge;
    public int ackBatchSize;
}
