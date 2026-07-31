/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.webconsole;

/**
 * Describes one SBK benchmark displayed by the Local Web Console.
 *
 * @param runId       unique run identifier
 * @param name        optional user-provided run name
 * @param source      source application, such as SBK or SBM
 * @param storage     storage driver name
 * @param action      benchmark action
 * @param timeUnit    latency time unit
 * @param sbkVersion  SBK implementation version
 * @param javaVersion Java runtime version
 * @param startedAt   run start time in epoch milliseconds
 */
public record WebConsoleRun(String runId, String name, String source, String storage, String action,
                           String timeUnit, String sbkVersion, String javaVersion, long startedAt) {
}
