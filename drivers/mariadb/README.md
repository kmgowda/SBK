# MariaDB Performance benchmarking with SBK
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
./build/install/sbk/bin/sbk -class mariadb  -writers 1 -size 100 -seconds 60
```

with default driver is 'org.mariadb.jdbc.Driver' and url 'jdbc:mariadb://localhost/mysql'

Note that **"mysql"** is the name of the data base used while starting the MariaDB server and the same name is used in default url.

the default table name is 'test' and username is 'root' and password is 'root'


