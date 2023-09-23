<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# HDFS Benchmarking

Make sure the you have JAVA_HOME set at your .bashrc file

Example:

```
export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk
export CLASSPATH=.:$JAVA_HOME/jre/lib/rt.jar:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
export PATH=$PATH:$JAVA_HOME/bin
```

you have set the following envirnoment variables too.
Examples:
```
export HDFS_NAMENODE_USER="root"
export HDFS_DATANODE_USER="root"
export HDFS_SECONDARYNAMENODE_USER="root"
export YARN_RESOURCEMANAGER_USER="root"
export YARN_NODEMANAGER_USER="root"
```

The example setting of core-site.xml is as follows

```
<configuration>
    <property>
        <name>fs.defaultFS</name>
        <value>hdfs://localhost:9000</value>
    </property>
    <property>
        <name>io.file.buffer.size</name>
        <value>1048576</value>
    </property>
</configuration>
```

The Example hdfs-site.xml as follows:

```
<configuration>
    <property>
        <name>dfs.replication</name>
        <value>2</value>
    </property>
    <property>
        <name>dfs.http.address</name>
         <value>localhost:50070</value>
    </property>
    <property>
        <name>dfs.datanode.data.dir</name>
        <value>/data/kmg/hadoop-data/data</value>
        <final>true</final>
    </property>
    <property>
        <name>dfs.namenode.name.dir</name>
        <value>/data/kmg/hadoop-data/name</value>
        <final>true</final>
    </property>
    <property>
        <name>dfs.permissions</name>
        <value>false</value>
    </property>
    <property>
        <name>dfs.datanode.use.datanode.hostname</name>
        <value>false</value>
    </property>
</configuration>
```

Format the name node
Example command:
```
/data/kmg/hadoop-3.2.0/bin/hdfs namenode -format
```

To start the DFS

```
/data/kmg/hadoop-3.2.0/sbin/start-dfs.sh
```

To stop the DFS
```
/data/kmg/hadoop-3.2.0/sbin/stop-dfs.sh
```


SBK Example invocations:

```
#Write

./build/distributions/sbk/bin/sbk -class hdfs -uri hdfs://localhost:9000 -file kmg-tmp-10.txt -writers 1 -size 1000000 -records 100000


#Read
./build/distributions/sbk/bin/sbk -class hdfs -uri hdfs://localhost:9000 -file kmg-tmp-10.txt -readers 10 -size 1000000 -records 100000

```

