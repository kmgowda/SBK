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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds the remote SBK version probe and interprets its response.
 */
final class RemoteSbkDeployment {
    private static final Pattern VERSION_PATTERN = Pattern.compile("(?m)^SBK Version:\\s*(\\S+)\\s*$");

    private RemoteSbkDeployment() {
    }

    /**
     * Extract the SBK version from launcher output.
     *
     * @param output output produced by {@code sbk -version}
     * @return parsed version, or {@code null} when the output is invalid
     */
    static String parseVersion(String output) {
        if (output == null) {
            return null;
        }
        final Matcher matcher = VERSION_PATTERN.matcher(output);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * Quote a value for use as one POSIX-shell argument.
     *
     * @param value unquoted value
     * @return safely single-quoted value
     */
    static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    /**
     * Serialize argument tokens for a POSIX shell without allowing a token to be reinterpreted.
     *
     * @param tokens command and argument tokens
     * @return safely quoted shell command
     */
    static String shellJoin(List<String> tokens) {
        return tokens.stream().map(RemoteSbkDeployment::shellQuote).reduce((left, right) -> left + " " + right)
                .orElseThrow(() -> new IllegalArgumentException("Command tokens must not be empty"));
    }
}
