/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.driver.Activemq;

import javax.jms.Destination;
import javax.jms.Session;

/**
 * Class for Activemq storage configuration.
 */
public class ActivemqConfig {
    // Add Activemq Storage driver configuration parameters
    public String url;
    public String qName;
    public Session session;
    public Destination dst;
}