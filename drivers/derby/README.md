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

# Apache Derby driver

`-class derby` specializes SBK's JDBC driver for Apache Derby. Connection and table behavior are configured through `src/main/resources/derby.properties` and the common JDBC options.

```bash
./build/install/sbk/bin/sbk -class derby -help
```

Start with help, then supply the required connection/table options for a disposable database. Document transaction, sync, and durability settings with results. See the [JDBC driver documentation](../jdbc/README.md) and [driver guide](../../docs/DRIVER_GUIDE.md).
