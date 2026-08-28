/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.api.impl;

import io.gem.api.RemoteExecutionStatus;
import io.gem.api.RemoteResponse;
import io.sbk.system.Printer;

import java.util.List;
import java.util.Locale;

/** Formats and logs the final host-tagged distributed benchmark result. */
final class DistributedResultPrinter {
    private static final int MINIMUM_SEPARATOR_WIDTH = 80;
    private static final String STATUS_LABEL = "Distributed Benchmark Status";
    private static final String EXPECTED_LABEL = "Expected Nodes";
    private static final String SUCCESSFUL_LABEL = "Successful Nodes";
    private static final String FAILED_LABEL = "Failed Nodes";
    private static final String REGISTRATIONS_LABEL = "Maximum SBM Registrations";
    private static final int LABEL_WIDTH = STATUS_LABEL.length();

    private DistributedResultPrinter() {
    }

    static void print(RemoteResponse[] results, boolean all, int maximumRegisteredClients) {
        int successful = 0;
        int failed = 0;
        for (RemoteResponse result : results) {
            if (result != null && result.status == RemoteExecutionStatus.SUCCESS) {
                successful++;
            } else {
                failed++;
            }
        }
        final String runStatus = runStatus(results, maximumRegisteredClients);
        final List<String> summaryLines = summary(runStatus, results.length, successful, failed,
                maximumRegisteredClients);
        final String title = "SBK-GEM Distributed Benchmark Final Results";
        int separatorWidth = Math.max(MINIMUM_SEPARATOR_WIDTH, title.length());
        for (String line : summaryLines) {
            separatorWidth = Math.max(separatorWidth, line.length());
        }
        for (int i = 0; i < results.length; i++) {
            separatorWidth = Math.max(separatorWidth, hostSummary(results[i], i).length());
        }
        final String separator = "-".repeat(separatorWidth);

        Printer.log.info(separator);
        Printer.log.info(title);
        Printer.log.info(separator);
        if ("SUCCESS".equals(runStatus)) {
            summaryLines.forEach(Printer.log::info);
        } else {
            summaryLines.forEach(Printer.log::error);
            Printer.log.error("SBK-GEM: Distributed results are incomplete and must not be used as a valid "
                    + "performance comparison");
        }
        for (int i = 0; i < results.length; i++) {
            logHost(results[i], i, all);
        }
        Printer.log.info(separator);
    }

    static List<String> summary(String runStatus, int expected, int successful, int failed,
                                int maximumRegisteredClients) {
        final String registrationText = maximumRegisteredClients < 0 ? "unavailable"
                : maximumRegisteredClients + "/" + expected;
        return List.of(line(STATUS_LABEL, runStatus), line(EXPECTED_LABEL, expected),
                line(SUCCESSFUL_LABEL, successful), line(FAILED_LABEL, failed),
                line(REGISTRATIONS_LABEL, registrationText));
    }

    static String runStatus(RemoteResponse[] results, int maximumRegisteredClients) {
        int successful = 0;
        for (RemoteResponse result : results) {
            if (result != null && result.status == RemoteExecutionStatus.SUCCESS) {
                successful++;
            }
        }
        final boolean registrationIncomplete = maximumRegisteredClients >= 0
                && maximumRegisteredClients < results.length;
        if (successful == results.length && !registrationIncomplete) {
            return "SUCCESS";
        }
        return successful > 0 || maximumRegisteredClients > 0 ? "INCOMPLETE" : "FAILED";
    }

    private static void logHost(RemoteResponse result, int index, boolean all) {
        if (result == null) {
            Printer.log.error(hostSummary(null, index));
            return;
        }
        final String summary = hostSummary(result, index);
        if (result.status == RemoteExecutionStatus.SUCCESS) {
            Printer.log.info(summary);
        } else {
            Printer.log.error(summary);
            Printer.log.error(result.failureMessage);
        }
        if (all || result.status != RemoteExecutionStatus.SUCCESS) {
            if (!result.stdOutput.isBlank()) {
                Printer.log.error("Host '{}' bounded stdout tail:\n{}", result.host, result.stdOutput);
            }
            if (!result.errOutput.isBlank()) {
                Printer.log.error("Host '{}' bounded stderr tail:\n{}", result.host, result.errOutput);
            }
        }
    }

    private static String line(String label, Object value) {
        return String.format(Locale.ROOT, "SBK-GEM %-" + LABEL_WIDTH + "s : %s", label, value);
    }

    private static String hostSummary(RemoteResponse result, int index) {
        if (result == null) {
            return "Host " + (index + 1) + ": unknown, status: NOT_COMPLETED, return code: unavailable";
        }
        final String returnCode = result.returnCode == RemoteResponse.UNKNOWN_RETURN_CODE
                ? "unavailable" : Integer.toString(result.returnCode);
        return "Host " + (index + 1) + ": " + result.host + ", status: " + result.status
                + ", return code: " + returnCode;
    }
}
