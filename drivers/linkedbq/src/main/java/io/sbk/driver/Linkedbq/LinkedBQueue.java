/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.driver.Linkedbq;

import io.perl.api.Queue;

import java.util.concurrent.LinkedBlockingDeque;

public class LinkedBQueue<T> extends LinkedBlockingDeque<T> implements Queue<T> {
}