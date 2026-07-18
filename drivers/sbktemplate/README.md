<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# SBK driver template

This project is a development scaffold and is intentionally excluded from the runtime distribution. Do not benchmark with `-class sbktemplate`.

To create a driver:

1. Write or review a [driver specification](../../docs/DRIVER_SPECIFICATION.md).
2. Copy this directory to `drivers/<name>`.
3. Rename the package, classes, resource, and Gradle project references.
4. Replace all placeholder operations with the vendor client's real semantics.
5. Register the driver in both `settings-drivers.gradle` and `build-drivers.gradle`.
6. Follow the implementation and verification steps in the [driver guide](../../docs/DRIVER_GUIDE.md).

The template is not proof that a driver is correct: completion semantics, error propagation, shutdown, concurrency, data lifecycle, and real-backend tests must be designed for each backend.
