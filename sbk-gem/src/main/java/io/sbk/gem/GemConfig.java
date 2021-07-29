/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.gem;

public class GemConfig {
    final public static String SBK_GEM_APP_NAME = "sbk.gem.applicationName";
    final public static String NAME = "sbk-gem";
    final public static String DESC = "Storage Benchmark Kit - Group Execution Monitor";
    final public static String BIN_DIR = "bin";
    final public static String LOCAL_HOST = "localhost";

    //override by props file or command line parameters
    public String nodes;
    public String gemuser;
    public String gempass;
    public int gemport;
    public String sbkdir;
    public String sbkcommand;
    public boolean copy;

    //override by props file
    public long remoteTimeoutSeconds;
    public int timeoutSeconds;
    public int maxIterations;
    public String remoteDir;
    public boolean fork;
}
