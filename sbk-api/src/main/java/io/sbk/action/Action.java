/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.action;

/**
 * enum Action { Writing, Reading, Write_Reading, Write_OnlyReading, Read_Writing, Read_OnlyWriting}.
 */
public enum Action {
    /**
     * <code>Writing = 0</code>.
     */
    Writing,

    /**
     * <code>Reading = 1</code>.
     */
    Reading,

    /**
     * <code>Write_Reading = 2</code>.
     */
    Write_Reading,

    /**
     * <code>Write_OnlyReading = 3</code>.
     */
    Write_OnlyReading,

    /**
     * <code>Read_Writing = 4</code>.
     */
    Read_Writing,

    /**
     * <code>Read_OnlyWriting = 5</code>.
     */
    Read_OnlyWriting,
}
