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
1. In the dashboards' menu you choose the dashboard of the storage system on which you want to conduct the performance benchmarking.
1. For example, if you are running the SBK performance benchmarking of file system as follows

   ```
    <SBK dir>% ./build/install/sbk/bin/sbk -class file -writers 1 -size 100 -seconds 60
   ```
1. you can choose the [File system dashboard](https://github.com/kmgowda/SBK/blob/master/grafana/dashboards/sbk-file.json) to see the performance results graphs. 


# Grafana with kubernetes

1. Update the [prometheus mount path](https://github.com/kmgowda/SBK/blob/master/grafana/prometheus-deployment.yaml#L36) to your local <SBK folder>/grafana/prometheus folder. 
1. Update the [Grafana dashboards path](https://github.com/kmgowda/SBK/blob/master/grafana/grafana-deployment.yaml#L41) to your local <SBK folder>/grafana/dashboards folder.    
1. Update the [Grafana provisioning path](https://github.com/kmgowda/SBK/blob/master/grafana/grafana-deployment.yaml#L45) to your local <SBK folder>/grafana/provisioning folder, 
1. Update the [Grafana config file](https://github.com/kmgowda/SBK/blob/master/grafana/grafana-deployment.yaml#L49) 
   to you local <SBK folder>/grafana/config.ini,  
1. Use the below command to run the grafana , prometheus pods and deployments

   ```
   <SBK dir/grafana>% kubectl apply -f grafana-deployment.yaml -f grafana-service.yaml -f prometheus-deployment.yaml -f  prometheus-service.yaml
     
   ```
   
   output is as follows:
   ```
   kmg@kmgs-MBP grafana % kubectl apply -f grafana-deployment.yaml -f grafana-service.yaml -f prometheus-deployment.yaml -f  prometheus-service.yaml 
   deployment.apps/grafana created
   service/grafana configured
   deployment.apps/prometheus created
   service/prometheus configured
   ```
1. you can check the status of the deployments with the below command:

   ```
   kubectl get svc
   ```
   
   output is as follows:

   ```
   kmg@kmgs-MBP grafana % kubectl get svc                                                         
   NAME          TYPE           CLUSTER-IP      EXTERNAL-IP   PORT(S)          AGE
   grafana       ClusterIP      10.105.242.78   <none>        3000/TCP         37s
   kubernetes    ClusterIP      10.96.0.1       <none>        443/TCP          7m47s
   prometheus    ClusterIP      10.108.47.252   <none>        9090/TCP         37s   
   ```
1. Note that,  there is no external IP for grafana, in case if you have the grafana container's ip address to mapped 
   to your localhost then use the below command:
   ```
   kubectl expose deployment grafana --type=LoadBalancer --name=grafana-ext
   ```
   the execution of above command is one time activity; if you delete and recreate the grafana service, you need not 
   execute the above command again.

1. Optionally you can expose the prometheus port too as follows
   ```
   kubectl expose deployment prometheus --type=LoadBalancer --name=prometheus-ext
   ```

1. now, you will get the external IP address for grafana service. check the output as follows.
   ```
   kmg@kmgs-MBP grafana % kubectl get svc
   NAME          TYPE           CLUSTER-IP      EXTERNAL-IP   PORT(S)          AGE
   grafana       ClusterIP      10.105.242.78   <none>        3000/TCP         14m
   grafana-ext   LoadBalancer   10.97.215.174   localhost     3000:31011/TCP   14m
   kubernetes    ClusterIP      10.96.0.1       <none>        443/TCP          21m
   prometheus    ClusterIP      10.108.47.252   <none>        9090/TCP         14m 
   ```
1. now you can log in to the grafana service [grafana local host port 3000](http://localhost:3000)  with username 
   'admin' and password 'admin'.


Helper kubctl commands to clean the pods , deployment and services

```
kubectl delete --all pods 
kubectl delete --all deployments
kubectl delete --all namespaces 
```
   