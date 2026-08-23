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

import java.util.regex.Pattern;

/** Builds POSIX-shell commands for remote managed-runtime ownership and cleanup. */
final class RemoteRuntimeLifecycle {
    private static final String RUNTIME_PREFIX = "sbk-runtime-";
    private static final String CURRENT_FILE = ".sbk-runtime-current";
    private static final String LEASE_DIRECTORY = ".sbk-runtime-leases";
    private static final String LOCK_DIRECTORY = ".sbk-runtime-management.lock";
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z0-9._-]+");

    private RemoteRuntimeLifecycle() {
    }

    /**
     * Return the lease file reserved for one remote benchmark command.
     *
     * @param parentDirectory managed-runtime parent directory
     * @param deploymentName immutable runtime directory name
     * @param leaseId unique benchmark-command lease identifier
     * @return absolute remote lease path
     */
    static String leasePath(String parentDirectory, String deploymentName, String leaseId) {
        validateIdentifier(deploymentName, "deployment name");
        validateIdentifier(leaseId, "lease identifier");
        return parentDirectory + "/" + LEASE_DIRECTORY + "/" + deploymentName + "/" + leaseId;
    }

    /**
     * Reserve an exact verified runtime and optionally remove inactive older managed runtimes.
     *
     * @param parentDirectory managed-runtime parent directory
     * @param deploymentName immutable runtime directory name
     * @param contentDigest expected content digest
     * @param leaseId unique benchmark-command lease identifier
     * @param cleanupEnabled whether inactive older runtimes should be removed
     * @param lockTimeoutSeconds maximum lock wait
     * @param lockStaleSeconds abandoned-lock reclaim age
     * @param reservationSeconds unlaunched reservation reclaim age
     * @return remote lifecycle command
     */
    static String acquireCommand(String parentDirectory, String deploymentName, String contentDigest,
                                 String leaseId, boolean cleanupEnabled, long lockTimeoutSeconds,
                                 long lockStaleSeconds, long reservationSeconds) {
        validateArguments(deploymentName, contentDigest, leaseId, lockTimeoutSeconds,
                lockStaleSeconds, reservationSeconds);
        final String lease = leasePath(parentDirectory, deploymentName, leaseId);
        final String deploymentDirectory = parentDirectory + "/" + deploymentName;
        return lifecyclePrefix(parentDirectory, leaseId, lockTimeoutSeconds, lockStaleSeconds)
                + "deployment=" + quote(deploymentName) + "; runtime=" + quote(deploymentDirectory)
                + "; lease=" + quote(lease) + "; expected=" + quote(contentDigest) + "; "
                + "test -f \"$runtime/" + SbkRuntimeBundle.REMOTE_DIGEST_FILE + "\"; "
                + "test \"$(cat \"$runtime/" + SbkRuntimeBundle.REMOTE_DIGEST_FILE
                + "\")\" = \"$expected\"; "
                + "mkdir -p \"${lease%/*}\"; now=$(date +%s); "
                + "printf 'reserved:%s\\n' \"$now\" > \"$lease\"; "
                + "current_tmp=\"$parent/" + CURRENT_FILE + ".$lease_id.tmp\"; "
                + "printf '%s\\n' \"$deployment\" > \"$current_tmp\"; "
                + "mv \"$current_tmp\" \"$parent/" + CURRENT_FILE + "\"; "
                + (cleanupEnabled ? cleanupBody(reservationSeconds) : "")
                + "printf 'SBK_RUNTIME_LEASE=%s\\n' \"$lease\"";
    }

    /**
     * Release a command lease and optionally clean inactive non-current runtimes.
     *
     * @param parentDirectory managed-runtime parent directory
     * @param deploymentName immutable runtime directory name
     * @param leaseId unique benchmark-command lease identifier
     * @param cleanupEnabled whether inactive older runtimes should be removed
     * @param lockTimeoutSeconds maximum lock wait
     * @param lockStaleSeconds abandoned-lock reclaim age
     * @param reservationSeconds unlaunched reservation reclaim age
     * @return remote lifecycle command
     */
    static String releaseCommand(String parentDirectory, String deploymentName, String leaseId,
                                 boolean cleanupEnabled, long lockTimeoutSeconds,
                                 long lockStaleSeconds, long reservationSeconds) {
        validateArguments(deploymentName, "0", leaseId, lockTimeoutSeconds,
                lockStaleSeconds, reservationSeconds);
        final String lease = leasePath(parentDirectory, deploymentName, leaseId);
        if (!cleanupEnabled) {
            return "rm -f " + quote(lease);
        }
        return lifecyclePrefix(parentDirectory, leaseId, lockTimeoutSeconds, lockStaleSeconds)
                + "lease=" + quote(lease) + "; rm -f \"$lease\"; "
                + cleanupBody(reservationSeconds)
                + "true";
    }

    /**
     * Wrap a remote SBK command so its reservation becomes a live PID lease and is released on exit.
     *
     * @param leasePath remote lease path
     * @param releaseCommand command which releases the lease and performs bounded cleanup
     * @param sbkCommand actual remote SBK command
     * @return wrapped command preserving the SBK exit code
     * @throws IllegalArgumentException when the command is empty
     */
    static String launchCommand(String leasePath, String releaseCommand, String sbkCommand) {
        if (sbkCommand == null || sbkCommand.isBlank()) {
            throw new IllegalArgumentException("Remote SBK command must not be empty");
        }
        return "set -u; lease=" + quote(leasePath) + "; "
                + "printf 'pid:%s\\n' \"$$\" > \"$lease\"; "
                + "cleanup_runtime() { sh -c " + quote(releaseCommand)
                + " || printf 'SBK-GEM: managed runtime lease cleanup failed\\n' >&2; }; "
                + "trap 'exit 129' HUP; trap 'exit 130' INT; trap 'exit 143' TERM; "
                + "trap 'status=$?; trap - EXIT; cleanup_runtime; exit $status' EXIT; "
                + sbkCommand;
    }

    private static String lifecyclePrefix(String parentDirectory, String leaseId, long lockTimeoutSeconds,
                                          long lockStaleSeconds) {
        return "set -eu; parent=" + quote(parentDirectory) + "; lease_id=" + quote(leaseId) + "; "
                + "mkdir -p \"$parent\" \"$parent/" + LEASE_DIRECTORY + "\"; "
                + "lock=\"$parent/" + LOCK_DIRECTORY + "\"; owner=\"$lease_id.$$\"; "
                + "deadline=$(($(date +%s)+"
                + lockTimeoutSeconds + ")); "
                + "while ! mkdir \"$lock\" 2>/dev/null; do now=$(date +%s); "
                + "if test ! -f \"$lock/created\"; then sleep 1; "
                + "if test ! -f \"$lock/created\"; then stale=\"$lock.stale.$owner\"; "
                + "if mv \"$lock\" \"$stale\" 2>/dev/null; then rm -rf \"$stale\"; fi; fi; "
                + "continue; fi; created=$(cat \"$lock/created\" 2>/dev/null || printf '0'); "
                + "case \"$created\" in ''|*[!0-9]*) created=0 ;; esac; "
                + "if test $((now-created)) -ge " + lockStaleSeconds
                + "; then stale=\"$lock.stale.$owner\"; "
                + "if mv \"$lock\" \"$stale\" 2>/dev/null; then rm -rf \"$stale\"; fi; continue; fi; "
                + "test \"$now\" -lt \"$deadline\" || { printf 'timed out waiting for runtime lifecycle "
                + "lock: %s\\n' \"$lock\" >&2; exit 75; }; sleep 1; done; "
                + "printf '%s\\n' \"$owner\" > \"$lock/owner\"; "
                + "printf '%s\\n' \"$(date +%s)\" > \"$lock/created\"; "
                + "release_lock() { current_owner=$(cat \"$lock/owner\" 2>/dev/null || true); "
                + "test \"$current_owner\" != \"$owner\" || rm -rf \"$lock\"; }; "
                + "trap release_lock EXIT HUP INT TERM; ";
    }

    private static String cleanupBody(long reservationSeconds) {
        return "current=''; test ! -f \"$parent/" + CURRENT_FILE + "\" "
                + "|| current=$(cat \"$parent/" + CURRENT_FILE + "\"); now=$(date +%s); "
                + "for candidate in \"$parent\"/" + RUNTIME_PREFIX + "*; do "
                + "test -d \"$candidate\" || continue; test ! -L \"$candidate\" || continue; "
                + "candidate_name=${candidate##*/}; test \"$candidate_name\" != \"$current\" || continue; "
                + "test -f \"$candidate/" + SbkRuntimeBundle.DESCRIPTOR_FILE + "\" || continue; "
                + "test -f \"$candidate/" + SbkRuntimeBundle.REMOTE_DIGEST_FILE + "\" || continue; "
                + "candidate_leases=\"$parent/" + LEASE_DIRECTORY + "/$candidate_name\"; active=0; "
                + "for lease_entry in \"$candidate_leases\"/*; do test -f \"$lease_entry\" || continue; "
                + "lease_value=$(cat \"$lease_entry\" 2>/dev/null || true); "
                + "case \"$lease_value\" in pid:*) lease_pid=${lease_value#pid:}; "
                + "case \"$lease_pid\" in ''|*[!0-9]*) rm -f \"$lease_entry\" ;; "
                + "*) if kill -0 \"$lease_pid\" 2>/dev/null; then active=1; "
                + "else rm -f \"$lease_entry\"; fi ;; esac ;; "
                + "reserved:*) reserved=${lease_value#reserved:}; "
                + "case \"$reserved\" in ''|*[!0-9]*) rm -f \"$lease_entry\" ;; "
                + "*) if test $((now-reserved)) -le " + reservationSeconds
                + "; then active=1; else rm -f \"$lease_entry\"; fi ;; esac ;; "
                + "*) rm -f \"$lease_entry\" ;; esac; done; "
                + "if test \"$active\" -eq 0; then rm -rf \"$candidate\" \"$candidate_leases\"; fi; done; ";
    }

    private static void validateArguments(String deploymentName, String contentDigest, String leaseId,
                                          long lockTimeoutSeconds, long lockStaleSeconds,
                                          long reservationSeconds) {
        validateIdentifier(deploymentName, "deployment name");
        validateIdentifier(contentDigest, "content digest");
        validateIdentifier(leaseId, "lease identifier");
        if (lockTimeoutSeconds < 1 || lockStaleSeconds < 1 || reservationSeconds < 1) {
            throw new IllegalArgumentException("Remote runtime lifecycle timeouts must be positive");
        }
    }

    private static void validateIdentifier(String value, String description) {
        if (value == null || !SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid " + description + ": " + value);
        }
    }

    private static String quote(String value) {
        return RemoteSbkDeployment.shellQuote(value);
    }
}
