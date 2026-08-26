/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.agent;

/** Shared filesystem and metadata contract between SBK-GEM and its remote agent. */
public final class RemoteDeploymentContract {
    /** Runtime archive root directory. */
    public static final String ARCHIVE_ROOT = "runtime";
    /** SBK installation directory inside the runtime archive. */
    public static final String SBK_DIRECTORY = "sbk";
    /** Runtime deployment descriptor. */
    public static final String DESCRIPTOR_FILE = "deployment.properties";
    /** Runtime file checksum manifest. */
    public static final String CHECKSUM_FILE = "deployment-files.sha256";
    /** Activated runtime content marker. */
    public static final String REMOTE_DIGEST_FILE = ".sbk-runtime.sha256";
    /** Managed runtime directory prefix. */
    public static final String RUNTIME_PREFIX = "sbk-runtime-";
    /** Java executable path relative to a Java home. */
    public static final String JAVA_EXECUTABLE = "bin/java";
    /** Java compiler path relative to a Java home. */
    public static final String JAVA_COMPILER = "bin/javac";
    /** SHA-256 message-digest algorithm name. */
    public static final String SHA_256 = "SHA-256";
    /** Descriptor format-version property. */
    public static final String FORMAT_VERSION_PROPERTY = "format.version";
    /** Descriptor SBK-version property. */
    public static final String SBK_VERSION_PROPERTY = "sbk.version";
    /** Descriptor Java-version property. */
    public static final String JAVA_VERSION_PROPERTY = "java.version";
    /** Descriptor operating-system property. */
    public static final String PLATFORM_OS_PROPERTY = "platform.os";
    /** Descriptor content-digest property. */
    public static final String CONTENT_SHA_256_PROPERTY = "content.sha256";
    /** Descriptor embedded-Java property. */
    public static final String INCLUDES_JAVA_PROPERTY = "includes.java";

    private RemoteDeploymentContract() {
    }
}
