<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# RabbitMQ Benchmarking

For local system RabbitMQ benchmarking, install the RabbitMQ on your system.

make sure that you have set the right set of admin previlages

```
# for admin user
sudo rabbitmqctl add_user admin admin

rabbitmqctl change_password admin admin

rabbitmqctl set_user_tags admin administrator

rabbitmqctl set_permissions -p / admin ".*" ".*" ".*"
```

You can set the previlages for guest user too

```
sudo rabbitmqctl add_user guest guest

rabbitmqctl change_password guest guest

rabbitmqctl set_user_tags guest administrator

rabbitmqctl set_permissions -p / guest ".*" ".*" ".*"
```

To allow remote users to access your server,  add the following content to in the file /etc/rabbitmq/rabbitmq.config

In case , if you have installed the Rabbit MQ in windows system, then to allow remote users to access your server,  add the following content to in the file c:\Users\[your user name]\AppData\Roaming\RabbitMQ\rabbitmq.config or :\Users\[your user name]\AppData\Roaming\RabbitMQ\advanced.config (%APPDATA%\RabbitMQ\rabbit.config or advanced.conifg).

```
[{rabbit, [{loopback_users, []}]}].

```

you can enable the web access 

```
sudo rabbitmq-plugins enable rabbitmq_management
```


After completeting the above steps you can start your local rabbitmq server

```
rabbitmq-server
```

Now, you can access your local RabbitMQ server : http://localhost:15672/#/
