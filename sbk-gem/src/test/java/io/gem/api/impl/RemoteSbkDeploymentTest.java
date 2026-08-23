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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests remote SBK deployment decisions.
 */
final class RemoteSbkDeploymentTest {
    @Test
    void quotesEveryRemoteCommandToken() {
        final String command = RemoteSbkDeployment.shellJoin(List.of("/opt/SBK dir/bin/sbk", "-file",
                "/tmp/a file; touch /tmp/not-created", "value's"));

        assertEquals("'/opt/SBK dir/bin/sbk' '-file' '/tmp/a file; touch /tmp/not-created' 'value'\\''s'",
                command);
    }

    @Test
    void parsesOnlyAuthoritativeVersionLine() {
        assertEquals("10.6", RemoteSbkDeployment.parseVersion("SBK Version: 10.6\n"));
        assertNull(RemoteSbkDeployment.parseVersion("SBK-GEM Version: 10.6\n"));
    }

}
