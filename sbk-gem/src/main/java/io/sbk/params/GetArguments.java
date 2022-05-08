/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.params;

/**
 * Interface GetArguments.
 */
public interface GetArguments {

    /**
     * to get options provided as arguments.
     *
     * @return options provided as arguments.
     */
    String[] getOptionsArgs();

    /**
     * to get Parsed options provided as arguments.
     *
     * @return Parsed options provided as arguments.
     */
    String[] getParsedArgs();
}
