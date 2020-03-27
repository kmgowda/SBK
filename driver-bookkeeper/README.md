<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# Apache Bookkeeper Benchmarking

Make sure that you do the binding of distributedlog to Ledgers of Bookkeeper

Example command:
```
<bookkeeper folder>/bin/dlog admin bind -l /ledgers -s localhost:2181 -c distributedlog://localhost:2181/streams
```

After binding you can issue the SBK command for benchmarking
```
./build/distributions/sbk/bin/sbk -class bookkeeper -uri distributedlog://localhost:2181/streams -log kmg-test2 -writers 1 -size 1000 -time 60 -recreate true
```
