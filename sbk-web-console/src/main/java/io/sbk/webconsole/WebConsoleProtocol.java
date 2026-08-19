/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.webconsole;

/** Local Web Console HTTP paths, methods, headers, and media types. */
final class WebConsoleProtocol {
    static final String API_PREFIX = "/api/v1/";
    static final String RUNS_PATH = API_PREFIX + "runs";
    static final String RUN_PATH_PREFIX = RUNS_PATH + "/";
    static final String HEALTH_PATH = API_PREFIX + "health";
    static final String CONFIG_PATH = API_PREFIX + "config";
    static final String BROWSER_CONNECT_PATH = API_PREFIX + "browser/connect";
    static final String BROWSER_DISCONNECT_PATH = API_PREFIX + "browser/disconnect";
    static final String METHOD_GET = "GET";
    static final String METHOD_POST = "POST";
    static final String CONTENT_TYPE_HEADER = "Content-Type";
    static final String CACHE_CONTROL_HEADER = "Cache-Control";
    static final String JSON = "application/json";
    static final String JSON_UTF_8 = "application/json; charset=utf-8";
    static final String TEXT_UTF_8 = "text/plain; charset=utf-8";
    static final String HTML_UTF_8 = "text/html; charset=utf-8";
    static final String JAVASCRIPT_UTF_8 = "text/javascript; charset=utf-8";
    static final String CSS_UTF_8 = "text/css; charset=utf-8";
    static final String EVENT_STREAM_UTF_8 = "text/event-stream; charset=utf-8";
    static final String NO_CACHE = "no-cache";
    static final String NO_STORE = "no-store";
    static final String RUNS_RESOURCE = "runs";
    static final String CONNECT_SUFFIX = "/connect";

    private WebConsoleProtocol() {
    }
}
