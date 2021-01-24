# Grafana Docker Compose

The Grafana docker compose consists of Grafana and prometheus docker images.
The [grafana docker compose](https://github.com/kmgowda/SBK/blob/master/grafana/docker-compose.yml) contains the [dashboards](https://github.com/kmgowda/SBK/tree/master/grafana/dashboards) which can be directly deployed for the performance analytics.

if you are running SBK as docker image or as SBK as performance benchmarking application,
this grafana docker compose can be used deploy the performance graphs.

As an example, just follow the below steps to see the performance graphs

1. Run the [grafana docker compose](https://github.com/kmgowda/SBK/blob/master/grafana/docker-compose.yml)

   ```
   <SBK dir/grafana>% ./docker-compose up 

   ```

1. login to [grafana local host port 3000](http://localhost:3000) with username **admin** and password **admin**
1. In the dashboards menu you choose the dashboard of the storage system on which you want to conduct the performance benchmarking.
1. For example, if you are running the SBK performance benchmarking of file system as follows

   ```
    <SBK dir>% ./build/install/sbk/bin/sbk -class file -writers 1 -size 100 -time 60
   ```
1. you can choose the [File system dashboard](https://github.com/kmgowda/SBK/blob/master/grafana/dashboards/sbk-file.json) to see the performance results graphs.  