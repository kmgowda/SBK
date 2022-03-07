/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.exception;


import lombok.Getter;

/**
 * class HelpException.
 * extends Exception class.
 */
final public class HelpException extends Exception {

    @Getter
    private final String helpText;

    /**
     * <code>this.helpText = helpText</code>.
     * @param helpText
     * helpText is further passed to super class {@link Exception}.
     */
    public HelpException(String helpText) {
        super(helpText);
        this.helpText = helpText;
    }
}
