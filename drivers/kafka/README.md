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

# Apache Kafka driver

`-class kafka` benchmarks Kafka producer and consumer operations with byte-array payloads. Driver options include broker URI, topic, partition count, replication factor, minimum in-sync replicas, and topic-creation behavior. Defaults are in `src/main/resources/kafka.properties`.

```bash
./build/install/sbk/bin/sbk -class kafka -broker localhost:9092 -topic sbk -writers 1 -size 1024 -seconds 15
./build/install/sbk/bin/sbk -class kafka -broker localhost:9092 -topic sbk -readers 1 -size 1024 -seconds 15
```

Record producer acknowledgement, replication, in-sync-replica, compression, batching, and consumer-group settings with results. Use a disposable topic unless retention and reuse are intentional. Run `-class kafka -help` for current options and see [the driver guide](../../docs/DRIVER_GUIDE.md).
