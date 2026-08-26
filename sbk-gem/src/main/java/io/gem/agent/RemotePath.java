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

/** POSIX path operations for supported Linux and macOS remote nodes. */
public final class RemotePath {
    /** POSIX root and path separator. */
    public static final String ROOT = "/";

    private RemotePath() {
    }

    /**
     * Join a remote parent and one or more relative path segments.
     *
     * @param parent absolute or relative remote parent
     * @param children relative child segments
     * @return joined POSIX remote path
     * @throws IllegalArgumentException when no child is supplied or a child is absolute
     */
    public static String join(String parent, String... children) {
        if (children.length == 0) {
            throw new IllegalArgumentException("At least one remote child path is required");
        }
        final StringBuilder path = new StringBuilder(normalize(parent));
        for (String child : children) {
            if (child == null || child.isBlank() || child.startsWith(ROOT)) {
                throw new IllegalArgumentException("Remote child path must be non-empty and relative: " + child);
            }
            if (path.length() == 0 || path.charAt(path.length() - 1) != ROOT.charAt(0)) {
                path.append(ROOT);
            }
            path.append(child);
        }
        return path.toString();
    }

    /**
     * Return the parent of a normalized POSIX remote path.
     *
     * @param path remote path
     * @return parent path, or the POSIX root when no non-root parent exists
     */
    public static String parent(String path) {
        final String normalized = normalize(path);
        final int separator = normalized.lastIndexOf(ROOT);
        return separator <= 0 ? ROOT : normalized.substring(0, separator);
    }

    /**
     * Remove trailing separators from a remote path while preserving the POSIX root.
     *
     * @param path remote path
     * @return normalized path, or {@code null} for a null or blank value
     */
    public static String normalize(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String normalized = path.trim();
        while (normalized.length() > ROOT.length() && normalized.endsWith(ROOT)) {
            normalized = normalized.substring(0, normalized.length() - ROOT.length());
        }
        return normalized;
    }

    /**
     * Test whether a remote path is absolute according to the POSIX remote-node contract.
     *
     * @param path remote path
     * @return true when the path begins at the POSIX root
     */
    public static boolean isAbsolute(String path) {
        return path != null && path.startsWith(ROOT);
    }
}
