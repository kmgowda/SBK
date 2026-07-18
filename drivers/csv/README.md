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

# CSV file driver

`-class csv` reads and writes local CSV-backed string records. The driver-specific `-file <path>` option selects the data file; defaults come from `src/main/resources/csv.properties`.

```bash
./build/install/sbk/bin/sbk -class csv -file /tmp/sbk.csv -writers 1 -size 1024 -seconds 15
./build/install/sbk/bin/sbk -class csv -file /tmp/sbk.csv -readers 1 -size 1024 -seconds 15
```

Use a non-production path and inspect the configured append/recreate behavior before repeating a write run. Run `-class csv -help` for authoritative options. See [the driver guide](../../docs/DRIVER_GUIDE.md).
