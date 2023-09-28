/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.api;

/**
 * Class RemoteResponse.
 */
public final class RemoteResponse {

    /**
     * <code>int returnCode</code>.
     */
    public final int returnCode;

    /**
     * <code>String stdOutput</code>.
     */
    public final String stdOutput;

    /**
     * <code>String errOutput</code>.
     */
    public final String errOutput;

    /**
     * <code>String host</code>.
     */
    public final String host;


    /**
     * The constructor RemoteResponse is responsible for initializing all values.
     *
     * @param returnCode    int
     * @param stdOutput     String
     * @param errOutput     String
     * @param host          String
     */
    public RemoteResponse(int returnCode, String stdOutput, String errOutput, String host) {
        this.returnCode = returnCode;
        this.stdOutput = stdOutput;
        this.errOutput = errOutput;
        this.host = host;
    }

}
