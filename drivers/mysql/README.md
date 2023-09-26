<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->

# MySQL Performance Benchmarking using SBK
The MySQL driver for SBK supports multiple writers and multiple readers performance benchmarking. But, the End to End Latency benchmarking is not supported.
The MySQL driver uses the auto incrementing index are primary key to support multiple writers.
The MYSQL driver uses the JDBC for IO operations.

As an example, to start the mysql storage server as a container is as follows:

```
docker run -p 3306:3306 -v /tmp:/tmp --name db --detach -e MYSQL_ROOT_PASSWORD="root" -e MYSQL_ROOT_HOST=% -e MYSQL_DATABASE=social -d mysql/mysql-server:8.0 --lower_case_table_names=1 --init-connect='GRANT CREATE USER ON *.* TO 'root'@'%';FLUSH PRIVILEGES;'
```

An example, SBK benchmarking command is
```
./build/install/sbk/bin/sbk -class mysql  -size 100 -writers 1 -seconds 60 
```

by default, the SBK uses the url: jdbc:mysql://localhost:3306/social, and default table name is 'test'
here **social** is the database name , the default username is 'root' and the default password is 'root'

Sample SBK MySQL write output is follows

```
kmg@kmgs-MacBook-Pro SBK % ./build/install/sbk/bin/sbk -class mysql  -size 100 -writers 1 -seconds 60 
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-01-17 17:36:13 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-01-17 17:36:13 INFO SBK version: 0.841
2021-01-17 17:36:13 INFO Argument List: [-class, mysql, -size, 100, -writers, 1, -seconds, 60]
2021-01-17 17:36:13 INFO sbk.applicationName: sbk
2021-01-17 17:36:13 INFO sbk.className: 
2021-01-17 17:36:14 INFO Reflections took 62 ms to scan 28 urls, producing 40 keys and 145 values 
2021-01-17 17:36:14 INFO Available Drivers : 27
2021-01-17 17:36:14 INFO Time Unit: MILLISECONDS
Loading class `com.mysql.jdbc.Driver'. This is deprecated. The new driver class is `com.mysql.cj.jdbc.Driver'. The driver is automatically registered via the SPI and manual loading of the driver class is generally unnecessary.
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by org.apache.ignite.internal.util.GridUnsafe$2 (file:/Users/kmg/projects/SBK/build/install/sbk/lib/ignite-core-2.8.1.jar) to field java.nio.Buffer.address
WARNING: Please consider reporting this to the maintainers of org.apache.ignite.internal.util.GridUnsafe$2
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
Sun Jan 17 17:36:14 IST 2021 WARN: Establishing SSL connection without server's identity verification is not recommended. According to MySQL 5.5.45+, 5.6.26+ and 5.7.6+ requirements SSL connection must be established by default if explicit option isn't set. For compliance with existing applications not using SSL the verifyServerCertificate property is set to 'false'. You need either to explicitly disable SSL by setting useSSL=false, or set useSSL=true and provide truststore for server certificate verification.
2021-01-17 17:36:14 INFO JDBC Driver Type: mysql
2021-01-17 17:36:14 INFO JDBC Driver Name: MySQL Connector/J
2021-01-17 17:36:14 INFO JDBC Driver Version: mysql-connector-java-8.0.11 (Revision: 6d4eaa273bc181b4cf1c8ad0821a2227f116fedf)
2021-01-17 17:36:14 INFO Deleting the Table: test
2021-01-17 17:36:14 INFO Unknown table 'social.test'
2021-01-17 17:36:14 INFO Creating the Table: test

Sun Jan 17 17:36:14 IST 2021 WARN: Establishing SSL connection without server's identity verification is not recommended. According to MySQL 5.5.45+, 5.6.26+ and 5.7.6+ requirements SSL connection must be established by default if explicit option isn't set. For compliance with existing applications not using SSL the verifyServerCertificate property is set to 'false'. You need either to explicitly disable SSL by setting useSSL=false, or set useSSL=true and provide truststore for server certificate verification.
MySQL Writing        4620 records,     923.8 records/sec,     0.09 MB/sec,      1.1 ms avg latency,      23 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 99th,       5 ms 99.9th,      23 ms 99.99th. 
MySQL Writing        4037 records,     807.2 records/sec,     0.08 MB/sec,      1.2 ms avg latency,     119 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       4 ms 99th,       5 ms 99.9th,     119 ms 99.99th. 
MySQL Writing        4538 records,     907.2 records/sec,     0.09 MB/sec,      1.1 ms avg latency,       6 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       4 ms 99th,       6 ms 99.9th,       6 ms 99.99th. 
MySQL Writing        4644 records,     928.6 records/sec,     0.09 MB/sec,      1.1 ms avg latency,       6 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       3 ms 99th,       5 ms 99.9th,       6 ms 99.99th. 
MySQL Writing        4516 records,     903.0 records/sec,     0.09 MB/sec,      1.1 ms avg latency,       7 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       3 ms 99th,       5 ms 99.9th,       7 ms 99.99th. 
MySQL Writing        4673 records,     934.4 records/sec,     0.09 MB/sec,      1.1 ms avg latency,       5 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 99th,       4 ms 99.9th,       5 ms 99.99th. 
MySQL Writing        5062 records,    1011.0 records/sec,     0.10 MB/sec,      1.0 ms avg latency,       6 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 99th,       4 ms 99.9th,       6 ms 99.99th. 
MySQL Writing        4687 records,     937.2 records/sec,     0.09 MB/sec,      1.1 ms avg latency,       5 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 99th,       5 ms 99.9th,       5 ms 99.99th. 
MySQL Writing        4619 records,     923.4 records/sec,     0.09 MB/sec,      1.1 ms avg latency,       6 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       4 ms 99th,       5 ms 99.9th,       6 ms 99.99th. 
MySQL Writing        3709 records,     741.7 records/sec,     0.07 MB/sec,      1.3 ms avg latency,       8 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       5 ms 99th,       6 ms 99.9th,       8 ms 99.99th. 
MySQL Writing        4128 records,     825.3 records/sec,     0.08 MB/sec,      1.2 ms avg latency,       7 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       4 ms 99th,       6 ms 99.9th,       7 ms 99.99th. 
MySQL Writing        4617 records,     927.1 records/sec,     0.09 MB/sec,      1.1 ms avg latency,       7 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       3 ms 99th,       5 ms 99.9th,       7 ms 99.99th. 
MySQL Writing(Total)        53850 records,     897.5 records/sec,     0.09 MB/sec,      1.1 ms avg latency,     119 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       3 ms 99th,       5 ms 99.9th,       7 ms 99.99th. 

```
The sample SBK MySQL read output is below
```
kmg@kmgs-MacBook-Pro SBK % ./build/install/sbk/bin/sbk -class mysql  -size 100 -readers 1 -seconds 60
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-01-17 17:46:23 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-01-17 17:46:23 INFO SBK version: 0.841
2021-01-17 17:46:23 INFO Argument List: [-class, mysql, -size, 100, -readers, 1, -seconds, 60]
2021-01-17 17:46:23 INFO sbk.applicationName: sbk
2021-01-17 17:46:23 INFO sbk.className: 
2021-01-17 17:46:23 INFO Reflections took 58 ms to scan 28 urls, producing 40 keys and 145 values 
2021-01-17 17:46:23 INFO Available Drivers : 27
2021-01-17 17:46:23 INFO Time Unit: MILLISECONDS
Loading class `com.mysql.jdbc.Driver'. This is deprecated. The new driver class is `com.mysql.cj.jdbc.Driver'. The driver is automatically registered via the SPI and manual loading of the driver class is generally unnecessary.
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by org.apache.ignite.internal.util.GridUnsafe$2 (file:/Users/kmg/projects/SBK/build/install/sbk/lib/ignite-core-2.8.1.jar) to field java.nio.Buffer.address
WARNING: Please consider reporting this to the maintainers of org.apache.ignite.internal.util.GridUnsafe$2
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
Sun Jan 17 17:46:24 IST 2021 WARN: Establishing SSL connection without server's identity verification is not recommended. According to MySQL 5.5.45+, 5.6.26+ and 5.7.6+ requirements SSL connection must be established by default if explicit option isn't set. For compliance with existing applications not using SSL the verifyServerCertificate property is set to 'false'. You need either to explicitly disable SSL by setting useSSL=false, or set useSSL=true and provide truststore for server certificate verification.
2021-01-17 17:46:24 INFO JDBC Driver Type: mysql
2021-01-17 17:46:24 INFO JDBC Driver Name: MySQL Connector/J
2021-01-17 17:46:24 INFO JDBC Driver Version: mysql-connector-java-8.0.11 (Revision: 6d4eaa273bc181b4cf1c8ad0821a2227f116fedf)
Sun Jan 17 17:46:24 IST 2021 WARN: Establishing SSL connection without server's identity verification is not recommended. According to MySQL 5.5.45+, 5.6.26+ and 5.7.6+ requirements SSL connection must be established by default if explicit option isn't set. For compliance with existing applications not using SSL the verifyServerCertificate property is set to 'false'. You need either to explicitly disable SSL by setting useSSL=false, or set useSSL=true and provide truststore for server certificate verification.
2021-01-17 17:46:24 INFO Reader 0 exited with EOF
MySQL Reading       53854 records,   53854.0 records/sec,     5.14 MB/sec,      0.0 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 99th,       0 ms 99.9th,       1 ms 99.99th. 
MySQL Reading(Total)        53854 records,   53854.0 records/sec,     5.14 MB/sec,      0.0 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 99th,       0 ms 99.9th,       1 ms 99.99th. 

```


## MariaDB performance Benchmarking using MySQL driver of SBK
The MariaDB is MySQL complaint. you can use the mariadb jdbc driver to conduct the performance benchmarking.
you can find the mysql docker images here: https://hub.docker.com/_/mariadb

command to pull the latest MariaDB docker image
```
docker pull mariadb:latest
```

Here is an example, to run with custom root password with the write access is as follows.

```
docker run -p 3306:3306 --name mariadb -e MYSQL_ROOT_PASSWORD=root -d mariadb:latest
```

An example SBK command is as follows
```
./build/install/sbk/bin/sbk -class mysql -driver org.mariadb.jdbc.Driver -url jdbc:mariadb://localhost/mysql  -table kmg -user root -password root -writers 1 -size 100 -seconds 60
```

Note that **"mysql"** is the name of the data base used while starting the MariaDB server and the same name should be used in the **-url** option of SBK jdbc command.

Sample SBK MySQL write output for MariaDB is as follows:

```
kmg@kmgs-MacBook-Pro SBK % ./build/install/sbk/bin/sbk -class mysql -driver org.mariadb.jdbc.Driver -url jdbc:mariadb://localhost/mysql  -table kmg -user root -password root -writers 1 -size 100 -seconds 60
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-01-17 17:52:20 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-01-17 17:52:20 INFO SBK version: 0.841
2021-01-17 17:52:20 INFO Argument List: [-class, mysql, -driver, org.mariadb.jdbc.Driver, -url, jdbc:mariadb://localhost/mysql, -table, kmg, -user, root, -password, root, -writers, 1, -size, 100, -seconds, 60]
2021-01-17 17:52:20 INFO sbk.applicationName: sbk
2021-01-17 17:52:20 INFO sbk.className: 
2021-01-17 17:52:21 INFO Reflections took 52 ms to scan 28 urls, producing 40 keys and 145 values 
2021-01-17 17:52:21 INFO Available Drivers : 27
2021-01-17 17:52:21 INFO Time Unit: MILLISECONDS
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by org.apache.ignite.internal.util.GridUnsafe$2 (file:/Users/kmg/projects/SBK/build/install/sbk/lib/ignite-core-2.8.1.jar) to field java.nio.Buffer.address
WARNING: Please consider reporting this to the maintainers of org.apache.ignite.internal.util.GridUnsafe$2
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
2021-01-17 17:52:21 INFO JDBC Driver Type: mariadb
2021-01-17 17:52:21 INFO JDBC Driver Name: MariaDB Connector/J
2021-01-17 17:52:21 INFO JDBC Driver Version: 2.7.1
2021-01-17 17:52:21 INFO The Table: kmg already exists
2021-01-17 17:52:21 INFO Deleting the Table: kmg
2021-01-17 17:52:21 INFO Creating the Table: kmg
MySQL Writing        5143 records,    1028.4 records/sec,     0.10 MB/sec,      1.0 ms avg latency,       8 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 99th,       3 ms 99.9th,       8 ms 99.99th. 
MySQL Writing        5183 records,    1035.8 records/sec,     0.10 MB/sec,      1.0 ms avg latency,       3 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 99th,       2 ms 99.9th,       3 ms 99.99th. 
MySQL Writing        5257 records,    1051.2 records/sec,     0.10 MB/sec,      0.9 ms avg latency,      33 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 99th,       2 ms 99.9th,      33 ms 99.99th. 
MySQL Writing        5122 records,    1024.2 records/sec,     0.10 MB/sec,      1.0 ms avg latency,       3 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 99th,       2 ms 99.9th,       3 ms 99.99th. 
MySQL Writing        5252 records,    1050.2 records/sec,     0.10 MB/sec,      1.0 ms avg latency,       6 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 99th,       2 ms 99.9th,       6 ms 99.99th. 
MySQL Writing        5146 records,    1029.0 records/sec,     0.10 MB/sec,      1.0 ms avg latency,       3 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 99th,       2 ms 99.9th,       3 ms 99.99th. 
MySQL Writing        5243 records,    1048.0 records/sec,     0.10 MB/sec,      1.0 ms avg latency,      42 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 99th,       2 ms 99.9th,      42 ms 99.99th. 
MySQL Writing        5317 records,    1063.2 records/sec,     0.10 MB/sec,      0.9 ms avg latency,      10 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 99th,       2 ms 99.9th,      10 ms 99.99th. 
MySQL Writing        5532 records,    1106.0 records/sec,     0.11 MB/sec,      0.9 ms avg latency,       3 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       1 ms 99th,       2 ms 99.9th,       3 ms 99.99th. 
MySQL Writing        5162 records,    1032.2 records/sec,     0.10 MB/sec,      1.0 ms avg latency,      11 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 99th,       3 ms 99.9th,      11 ms 99.99th. 
MySQL Writing        5265 records,    1052.8 records/sec,     0.10 MB/sec,      0.9 ms avg latency,       3 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 99th,       2 ms 99.9th,       3 ms 99.99th. 
MySQL Writing        5277 records,    1059.0 records/sec,     0.10 MB/sec,      0.9 ms avg latency,       4 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 99th,       2 ms 99.9th,       4 ms 99.99th. 
MySQL Writing(Total)        62899 records,    1048.3 records/sec,     0.10 MB/sec,      1.0 ms avg latency,      42 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       1 ms 10th,       1 ms 25th,       1 ms 50th,       1 ms 75th,       2 ms 99th,       2 ms 99.9th,       4 ms 99.99th. 

```

An Example database read command is as follows:
```
 ./build/install/sbk/bin/sbk -class mysql -driver org.mariadb.jdbc.Driver -url jdbc:mariadb://localhost/mysql  -table kmg -user root -password root -readers 1 -size 100 -seconds 60
``` 

Sample SBK MySQL read output for MariaDB is as follows:

```
kmg@kmgs-MacBook-Pro SBK % ./build/install/sbk/bin/sbk -class mysql -driver org.mariadb.jdbc.Driver -url jdbc:mariadb://localhost/mysql  -table kmg -user root -password root -readers 1 -size 100 -seconds 60
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.14.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2021-01-17 17:53:32 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2021-01-17 17:53:32 INFO SBK version: 0.841
2021-01-17 17:53:32 INFO Argument List: [-class, mysql, -driver, org.mariadb.jdbc.Driver, -url, jdbc:mariadb://localhost/mysql, -table, kmg, -user, root, -password, root, -readers, 1, -size, 100, -seconds, 60]
2021-01-17 17:53:32 INFO sbk.applicationName: sbk
2021-01-17 17:53:32 INFO sbk.className: 
2021-01-17 17:53:32 INFO Reflections took 55 ms to scan 28 urls, producing 40 keys and 145 values 
2021-01-17 17:53:32 INFO Available Drivers : 27
2021-01-17 17:53:32 INFO Time Unit: MILLISECONDS
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by org.apache.ignite.internal.util.GridUnsafe$2 (file:/Users/kmg/projects/SBK/build/install/sbk/lib/ignite-core-2.8.1.jar) to field java.nio.Buffer.address
WARNING: Please consider reporting this to the maintainers of org.apache.ignite.internal.util.GridUnsafe$2
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
2021-01-17 17:53:32 INFO JDBC Driver Type: mariadb
2021-01-17 17:53:32 INFO JDBC Driver Name: MariaDB Connector/J
2021-01-17 17:53:32 INFO JDBC Driver Version: 2.7.1
2021-01-17 17:53:33 INFO Reader 0 exited with EOF
MySQL Reading       62905 records,   62905.0 records/sec,     6.00 MB/sec,      0.0 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 99th,       0 ms 99.9th,       1 ms 99.99th. 
MySQL Reading(Total)        62905 records,   62905.0 records/sec,     6.00 MB/sec,      0.0 ms avg latency,       1 ms max latency; Discarded Latencies:       0 lower,        0 higher;  Latency Percentiles:       0 ms 10th,       0 ms 25th,       0 ms 50th,       0 ms 75th,       0 ms 99th,       0 ms 99.9th,       1 ms 99.99th. 

```
