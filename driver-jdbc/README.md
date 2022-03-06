<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# JDBC Driver for SBK
The JDBC driver for SBK supports multi writers,readers performance benchmarking. But, the End to End Latency benchmarking is not supported.
Using this JDBC support of SBK, you can conduct the performance benchmarking of any data base which provides JDBC driver.

## JDBC with Apache Derby 
Refer to this page : https://db.apache.org/derby/papers/DerbyTut/index.html to get the Derby Installations steps.
By default, the Derby Network client is tested, you can refer to this page : https://db.apache.org/derby/papers/DerbyTut/ns_intro.html for Derby network Server installations.

An example, SBK benchmarking command is
```
./build/install/sbk/bin/sbk -class jdbc  -url jdbc:derby://localhost:1527/dbs -table kmg_1 -size 100 -writers 1 -seconds 60
```

you can specify the derby driver explicitly as follows
```
./build/install/sbk/bin/sbk -class jdbc  -driver org.apache.derby.jdbc.ClientDriver -url jdbc:derby://localhost:1527/dbs -table kmg_1 -size 100 -writers 1 -seconds 60
```
you can also specify the username and password in the above commands with -user and -password options. The default user name/password are admin/admin.

Below is the exmaple SBK command for the Apache Derby Embedded Driver benchmarking.
```
./build/install/sbk/bin/sbk -class jdbc  -driver org.apache.derby.jdbc.EmbeddedDriver -url jdbc:derby:test.db   -table kmg_1 -size 100 -writers 1 -seconds 60
```
The example command for read benchmarking is as follows.
```
./build/install/sbk/bin/sbk -class jdbc  -driver org.apache.derby.jdbc.EmbeddedDriver -url jdbc:derby:test.db   -table kmg_1 -size 100 -readers 1 -seconds 60
```


## JDBC with MySQL
The SBK is tested with MySQL benchmarking too. you can find the community edition of Mysql to download here : https://dev.mysql.com/downloads/mysql
or you can use the docker deployment of MySql too. you can find the mysql docker images here: https://hub.docker.com/r/mysql/mysql-server/

command to pull the mysql server 8.0 docker image
```
docker pull mysql/mysql-server:8.0
```

Here is an example, to run with custom user name, password and with the write access is as follows.

```
docker run -p 3306:3306 -v /tmp:/tmp --name db --detach -e MYSQL_ROOT_PASSWORD="root" -e MYSQL_ROOT_HOST=% -e MYSQL_DATABASE=social -d mysql/mysql-server:8.0 --lower_case_table_names=1 --init-connect='GRANT CREATE USER ON *.* TO 'root'@'%';FLUSH PRIVILEGES;'
```

An example SBK command is as follows
```
 ./build/install/sbk/bin/sbk -class jdbc  -driver com.mysql.jdbc.Driver -url jdbc:mysql://localhost:3306/social -user root -password root  -table kmg_2 -size 100 -writers 1 -seconds 60
```

Note that **"social"** is the name of the data base used while starting the MySQL server and the same name should be used in the **-url** option of SBK jdbc command.

An Example database read command is as follows:
```
 ./build/install/sbk/bin/sbk -class jdbc  -driver com.mysql.jdbc.Driver -url jdbc:mysql://localhost:3306/social -user root -password root  -table kmg_2 -size 100 -readers 1 -seconds 60
``` 

The MySQL database write benchmarking example using SBK docker images is
```
docker run  -p 127.0.0.1:8080:8080/tcp  kmgowda/sbk:latest -class jdbc  -driver com.mysql.jdbc.Driver -url jdbc:mysql://192.168.0.192:3306/social -user root -password root  -table kmg_2 -size 100 -writers 1 -seconds 60
```

## JDBC with PostgreSQL
The SBK with JDBC is tested with PostgreSQL server. Visit this page : https://www.postgresql.org to download and know about the Postgress.
you can run the PostgreSQL docker image as follows.
```
docker run  -p 127.0.0.1:5432:5432/tcp  --name kmg-postgres -e POSTGRES_USER=root -e POSTGRES_PASSWORD=root -d postgres
```
An example command to run the SBK benchmarking is
```
./build/install/sbk/bin/sbk -class jdbc  -driver org.postgresql.Driver -url jdbc:postgresql://localhost:5432/postgres -user root -password root  -table kmg_1 -size 100 -writers 1 -seconds 60
```
Make sure the username and passwords are same while running the postgreSQL server and SBK benchmarking.
generally **'postgres'** is the name of the database available by default.

## JDBC PostgreSQL for CockroachDB Performance Benchmarking
The SBK with JDBC using PostgreSQL Jdbc driver can be used to conduct the performance benchmarking of CockroachDB.
you refer this page : https://www.cockroachlabs.com/docs/stable/start-a-local-cluster.html to build simple local cluster.
The default database name to access is **'defaultdb'** and the default port is 26257.

An example command to run the SBK benchmarking is
```
./build/install/sbk/bin/sbk -class jdbc  -driver org.postgresql.Driver -url jdbc:postgresql://localhost:26257/defaultdb  -user root -password root  -table test -size 100 -writers 5 -seconds 60
```
Make sure that you use root/root as username/password while running above benchmarking command.
 
## JDBC with Microsoft SQL Server
The SBK with JDBC is tested with Microsoft SQL server. Visit this page : https://hub.docker.com/_/microsoft-mssql-server  to download developer or expression editions of MS SQL.
you can run the MS SQL docker image as follows.

```
docker run -e 'ACCEPT_EULA=Y' -e 'SA_PASSWORD=Kmg@1234' -p 1433:1433 -d mcr.microsoft.com/mssql/server:2017-latest
```
In the above example, the password `Kmg@1234` used for default user name `sa`

An example command to run the SBK benchmarking is
```
./build/install/sbk/bin/sbk -class jdbc  -driver com.microsoft.sqlserver.jdbc.SQLServerDriver -url jdbc:sqlserver://localhost:1433 -user sa -password Kmg@1234  -table kmg_1 -size 1000 -writers 1 -seconds 60
```
Make sure the user name is `sa` and passwords are same while running the MS SQL server and SBK benchmarking.

## JDBC with SQLite
The SBK with JDBC is tested with embedded SQL data base SQLite. Visit this page : https://www.sqlite.org/index.html to know more.

An example command to run the SBK benchmarking is
```
./build/install/sbk/bin/sbk -class jdbc  -driver org.sqlite.JDBC -url jdbc:sqlite:test.db   -table kmg_2 -size 100 -writers 1 -seconds 60
```
Note that, SQLite is a local on disk database, it does not supports multiple wrtiers and it supports mulitple readers benchmarking. 


## JDBC with MariaDB
The SBK is tested with MariaDB benchmarking too. you can find the mariadb docker images here: https://hub.docker.com/_/mariadb

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
./build/install/sbk/bin/sbk -class jdbc -driver org.mariadb.jdbc.Driver -url jdbc:mariadb://localhost/mysql  -table kmg -user root -password root -writers 1 -size 100 -seconds 60
```

Note that **"mysql"** is the name of the data base used while starting the MariaDB server and the same name should be used in the **-url** option of SBK jdbc command.

An Example database read command is as follows:
```
./build/install/sbk/bin/sbk -class jdbc -driver org.mariadb.jdbc.Driver -url jdbc:mariadb://localhost/mysql  -table kmg -user root -password root -readers 1 -size 100 -seconds 60
``` 

## JDBC 'Class not found issue' Work around

Sometimes you will get the following class not found error when running the sbk jdbc benchmarking with mysql

```
kmg@kmgs-MBP SBK % ./build/install/sbk/bin/sbk -class jdbc  -driver com.mysql.cj.jdbc.Driver -url jdbc:mysql://localhost:3306/social -user root -password root  -table kmg_2 -size 100 -writers 1 -seconds 60
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-simple-1.7.32.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/logback-classic-1.0.13.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/Users/kmg/projects/SBK/build/install/sbk/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]
2022-03-06 12:15:52 INFO Reflections took 82 ms to scan 46 urls, producing 98 keys and 218 values 
2022-03-06 12:15:52 INFO 
       _____   ____    _   __
      / ____| |  _ \  | | / /
     | (___   | |_) | | |/ /
      \___ \  |  _ <  |   <
      ____) | | |_) | | |\ \
     |_____/  |____/  |_| \_\

2022-03-06 12:15:52 INFO Storage Benchmark Kit
2022-03-06 12:15:52 INFO SBK Version: 0.98
2022-03-06 12:15:52 INFO SBK Website: https://github.com/kmgowda/SBK
2022-03-06 12:15:52 INFO Arguments List: [-class, jdbc, -driver, com.mysql.cj.jdbc.Driver, -url, jdbc:mysql://localhost:3306/social, -user, root, -password, root, -table, kmg_2, -size, 100, -writers, 1, -seconds, 60]
2022-03-06 12:15:52 INFO Java Runtime Version: 17+35-LTS-2724
2022-03-06 12:15:52 INFO Storage Drivers Package: io.sbk
2022-03-06 12:15:52 INFO sbk.applicationName: sbk
2022-03-06 12:15:52 INFO sbk.appHome: /Users/kmg/projects/SBK/build/install/sbk
2022-03-06 12:15:52 INFO sbk.className: 
2022-03-06 12:15:52 INFO '-class': jdbc
2022-03-06 12:15:52 INFO Available Storage Drivers in package 'io.sbk': 40 [Artemis, 
AsyncFile, BookKeeper, Cassandra, CephS3, ConcurrentQ, CouchDB, CSV, Db2, Derby, 
FdbRecord, File, FileStream, FoundationDB, HDFS, Hive, Ignite, Jdbc, Kafka, LevelDB, 
MariaDB, MinIO, MongoDB, MsSql, MySQL, Nats, NatsStream, Nsq, Null, OpenIO, PostgreSQL, 
Pravega, Pulsar, RabbitMQ, Redis, RedPanda, RocketMQ, RocksDB, SeaweedS3, SQLite]
2022-03-06 12:15:52 INFO Arguments to Driver 'Jdbc' : [-driver, com.mysql.cj.jdbc.Driver, -url, jdbc:mysql://localhost:3306/social, -user, root, -password, root, -table, kmg_2, -size, 100, -writers, 1, -seconds, 60]
2022-03-06 12:15:52 INFO Time Unit: MILLISECONDS
2022-03-06 12:15:52 INFO Minimum Latency: 0 ms
2022-03-06 12:15:52 INFO Maximum Latency: 180000 ms
2022-03-06 12:15:52 INFO Window Latency Store: Array, Size: 1 MB
2022-03-06 12:15:52 INFO Total Window Latency Store: HashMap, Size: 256 MB
2022-03-06 12:15:52 INFO Total Window Extension: None, Size: 0 MB
2022-03-06 12:15:52 INFO SBK Benchmark Started
2022-03-06 12:15:52 INFO SBK PrometheusLogger Started
Exception in thread "main" java.lang.NoClassDefFoundError: Could not initialize class org.apache.ignite.IgniteJdbcThinDriver
	at java.base/java.lang.Class.forName0(Native Method)
	at java.base/java.lang.Class.forName(Class.java:467)
	at java.sql/java.sql.DriverManager.isDriverAllowed(DriverManager.java:558)
	at java.sql/java.sql.DriverManager.getConnection(DriverManager.java:678)
	at java.sql/java.sql.DriverManager.getConnection(DriverManager.java:190)
	at io.sbk.Jdbc.Jdbc.openStorage(Jdbc.java:208)
	at io.sbk.api.impl.SbkBenchmark.start(SbkBenchmark.java:140)
	at io.sbk.api.impl.Sbk.run(Sbk.java:80)
	at io.sbk.main.SbkMain.main(SbkMain.java:29)

```
 to fix the issue , make sure that you already have SQL Server running ; As a example, make sure that the you start the mysql docker before running the benchmark. Here is an example, to run with custom user name, password and with the write access is as follows.

```
docker run -p 3306:3306 -v /tmp:/tmp --name db --detach -e MYSQL_ROOT_PASSWORD="root" -e MYSQL_ROOT_HOST=% -e MYSQL_DATABASE=social -d mysql/mysql-server:8.0 --lower_case_table_names=1 --init-connect='GRANT CREATE USER ON *.* TO 'root'@'%';FLUSH PRIVILEGES;'
```

To fix the class no found issue, make sure that you are already running the database server.
 