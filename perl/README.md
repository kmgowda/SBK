<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->

<p align="center">
    <a href="https://kmgowda.github.io/SBK/perl?from=SBK">
        <img src="images/perl-no-background.png" alt="Storage Benchmark Kit" width="650" height="100">
    </a>
</p>

# PerL - Performance Logger

[![Api](https://img.shields.io/badge/PerL-API-brightgreen)](https://kmgowda.github.io/SBK/perl/javadoc/index.html)

The PerL is the core of SBK framework. The PerL provides the foundation APIs for performance benchmarking, storing 
latency values and calculating percentiles. The APIs of PerL are used by SBK-API module to define the readers and writers 
interfaces. The Latency store methods/classes are used by SBK-API and SBK-RAM. The PerL module can be used by any 
application for performance benchmarking.

If you want to conduct the performance benchmarking without read and write interfaces/APIs, then PerL can be used.
The PerL provides the APIs for performance benchmarking which can used for other than storage systems. PerL can be used 
for performance benchmarking of any software system.

# How to use PerL

## Get the Perl Package

### PerL Maven Central

   ```
    repositories {
        mavenCentral()
    }

    dependencies {
        implementation 'io.github.kmgowda:perl:0.96'
    }
   ```
  Note that 'mavenCentral()' repository is required to fetch the SBK APIs package and its dependencies.
 
### PerL Git hub package

  ```
    repositories {
        mavenCentral()

        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/kmgowda/SBK")
            credentials {
                username = "sbk-public"
                password = "\u0067hp_FBqmGRV6KLTcFjwnDTvozvlhs3VNja4F67B5"
            }   
   
       }
    }

    dependencies {
        implementation 'sbk:perl:0.96'
    }

   ```

### PerL Jit package

  ```
    repositories {
        mavenCentral()
        maven {
            url 'https://jitpack.io'
        }
    }

    dependencies {
        implementation 'com.github.kmgowda.SBK:perl:0.96'
    }
   
   ```

Note that 'mavenCentral()' repository is required to fetch the SBK APIs package and its dependencies.


## Use PerL APIs in your application
1. Use [PerlBuilder.build API](https://kmgowda.github.io/SBK/perl/javadoc/io/perl/api/impl/PerlBuilder.html) to create and get the Concurrent queue based Perl interface.
   1. see the example : https://github.com/kmgowda/SBK/blob/master/sbk-api/src/main/java/io/sbk/api/impl/SbkBenchmark.java#L93   
   2. see the Junit test example : https://github.com/kmgowda/SBK/blob/master/perl/src/test/java/io/perl/test/PerlTest.java#L80
   3. The created Perl interface object can be distributed among several threads.  

2. Use [getPerlChannel API](https://kmgowda.github.io/SBK/perl/javadoc/io/perl/api/GetPerlChannel.html#getPerlChannel()) 
   to get the perl channel.
   1. Multiple threads can invoke this API to get the dedicated PerlChannel object.
   2. This dedicated PerlChannel is not thread safe and it should not used distributed among multiple threads
   3. See the example : https://github.com/kmgowda/SBK/blob/master/sbk-api/src/main/java/io/sbk/api/impl/SbkBenchmark.java#L177
   4. See the Junit test example : https://github. com/kmgowda/SBK/blob/master/perl/src/test/java/io/perl/test/PerlTest.java#L83 

3. start the benchmarking using [Run API](https://kmgowda.github.io/SBK/perl/javadoc/io/perl/api/RunBenchmark.html) 
   1. see the example : https://github.com/kmgowda/SBK/blob/master/sbk-api/src/main/java/io/sbk/api/impl/SbkBenchmark.java#L203
   2. see the Junit test example : https://github.com/kmgowda/SBK/blob/master/perl/src/test/java/io/perl/test/PerlTest.java#L85
   
4. you send the performance data to Perl channel using [send API](https://kmgowda.github.io/SBK/perl/javadoc/io/perl/api/PerlChannel.html)
   1. see the example : https://github.com/kmgowda/SBK/blob/master/sbk-api/src/main/java/io/sbk/api/Writer.java#L97
   2. See the Junit example to send the performance data with multiple threads : https://github.com/kmgowda/SBK/blob/master/perl/src/test/java/io/perl/test/PerlTest.java#L95
   
5. in case of any exception, you can send the [exception](https://kmgowda.github.io/SBK/perl/javadoc/io/perl/exception/ExceptionHandler.html)

6. The Perl will periodically sends/prints the performance results to logger/printer which is supplied with 
   [PerlBuilder.build API](https://kmgowda.github.io/SBK/perl/javadoc/io/perl/api/impl/PerlBuilder.html#build(io.perl.logger.PerformanceLogger,io.perl.api.ReportLatency,io.time.Time,io.perl.config.PerlConfig,java.util.concurrent.ExecutorService)) in step 1.

7. stop the benchmarking using [Stop API](https://kmgowda.github.io/SBK/perl/javadoc/io/perl/api/Perl.html)
   1. see the example: https://github.com/kmgowda/SBK/blob/master/sbk-api/src/main/java/io/sbk/api/impl/SbkBenchmark.java#L364
   
