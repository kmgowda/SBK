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
./build/install/sbk/bin/sbk -class jdbc  -url jdbc:derby://localhost:1527/dbs -table kmg_1 -size 100 -writers 1 -time 60
```

you can specify the derby driver explicitly as follows
```
./build/install/sbk/bin/sbk -class jdbc  -driver org.apache.derby.jdbc.ClientDriver -url jdbc:derby://localhost:1527/dbs -table kmg_1 -size 100 -writers 1 -time 60
```
you can also specify the username and password in the above commands with -user and -password options. The default user name/password are admin/admin.

Below is the exmaple SBK command for the Apache Derby Embedded Driver benchmarking.
```
./build/install/sbk/bin/sbk -class jdbc  -driver org.apache.derby.jdbc.EmbeddedDriver -url jdbc:derby:test.db   -table kmg_1 -size 100 -writers 1 -time 60
```
The example command for read benchmarking is as follows.
```
./build/install/sbk/bin/sbk -class jdbc  -driver org.apache.derby.jdbc.EmbeddedDriver -url jdbc:derby:test.db   -table kmg_1 -size 100 -readers 1 -time 60
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
 ./build/install/sbk/bin/sbk -class jdbc  -driver com.mysql.jdbc.Driver -url jdbc:mysql://localhost:3306/social -user root -password root  -table kmg_2 -size 100 -writers 1 -time 60
```

Note that **"social"** is the name of the data base used while starting the MySQL server and the same name should be used in the **-url** option of SBK jdbc command.

An Example data base read command is as follows:
```
 ./build/install/sbk/bin/sbk -class jdbc  -driver com.mysql.jdbc.Driver -url jdbc:mysql://localhost:3306/social -user root -password root  -table kmg_2 -size 100 -readers 1 -time 60
``` 

The MySQL database write benchmarking example using SBK docker images is
```
docker run  -p 127.0.0.1:8080:8080/tcp  kmgowda/sbk:latest -class jdbc  -driver com.mysql.jdbc.Driver -url jdbc:mysql://192.168.0.192:3306/social -user root -password root  -table kmg_2 -size 100 -writers 1 -time 60
```

## JDBC with PostgreSQL
The SBK with JDBC is tested with PostgreSQL server. Visit this page : https://www.postgresql.org to download and know about the Postgress.
you can run the PostgreSQL docker image as follows.
```
docker run  -p 127.0.0.1:5432:5432/tcp  --name kmg-postgres -e POSTGRES_USER=root -e POSTGRES_PASSWORD=root -d postgres
```
An example command to run the SBK benchmarking is
```
./build/install/sbk/bin/sbk -class jdbc  -driver org.postgresql.Driver -url jdbc:postgresql://localhost:5432/postgres -user root -password root  -table kmg_1 -size 100 -writers 1 -time 60
```
Make sure the user name and passwords are same while running the postgreSQL server and SBK benchmarking.
generally **'postgres'** is the name of the database available by default.

## JDBC PostgreSQL for CockroachDB Performance Benchmarking
The SBK with JDBC using PostgreSQL Jdbc driver can be used to conduct the performance benchmarking of CockroachDB.
you refer this page : https://www.cockroachlabs.com/docs/stable/start-a-local-cluster.html to build simple local cluster.
The default database name to access is **'defaultdb'** and the default port is 26257.

An example command to run the SBK benchmarking is
```
./build/install/sbk/bin/sbk -class jdbc  -driver org.postgresql.Driver -url jdbc:postgresql://localhost:26257/defaultdb  -user root -password root  -table test -size 100 -writers 5 -time 60
```
Make sure that you use root/root as username/password while running above benchmarking command.
 
## JDBC with Microsoft SQL Server
The SBK with JDBC is tested with Microsoft SQL server. Visit this page : https://www.postgresql.org  to download developer or expression editions of MS SQL.
you can run the MS SQL docker image as follows.

```
docker run -e 'ACCEPT_EULA=Y' -e 'SA_PASSWORD=Kmg@1234' -p 1433:1433 -d mcr.microsoft.com/mssql/server:2017-latest
```
In the above example, the password `Kmg@1234` used for default user name `sa`

An example command to run the SBK benchmarking is
```
./build/install/sbk/bin/sbk -class jdbc  -driver com.microsoft.sqlserver.jdbc.SQLServerDriver -url jdbc:sqlserver://localhost:1433 -user sa -password Kmg@1234  -table kmg_1 -size 1000 -writers 1 -time 60
```
Make sure the user name is `sa` and passwords are same while running the MS SQL server and SBK benchmarking.

## JDBC with SQLite
The SBK with JDBC is tested with embedded SQL data base SQLite. Visit this page : https://www.sqlite.org/index.html to know more.

An example command to run the SBK benchmarking is
```
./build/install/sbk/bin/sbk -class jdbc  -driver org.sqlite.JDBC -url jdbc:sqlite:test.db   -table kmg_2 -size 100 -writers 1 -time 60
```
Note that, SQLite is a local on disk database, it does not supports multiple wrtiers and it supports mulitple readers benchmarking. 
