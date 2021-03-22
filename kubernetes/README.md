<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->

# SBK Kubernetes Deployments
The SBK with preconfigured storage driver can be deployed as Kubernetes pod.
As an example, to deploy rabbitmq SBK pod, you can refer to the file: https://github.com/kmgowda/SBK/blob/kmg-kubernetes-2/config/kubernetes/sbk-rabbitmq-k8-sample.sh
and configuration file : https://github.com/kmgowda/SBK/blob/kmg-kubernetes-2/config/kubernetes/sbk-rabbitmq-k8-sample.yaml. 
Make sure that you use the appropriate IP address of RabbitMQ server.


## Kubernetes setup with Dockers Desktop and Web Dashboard
1. If you already have the Oracle Virtual box and minikube installed in your system, then I suggest uninstall them to avoid compatibility issues with docker desktop.
2. Install [Docker Desktop](https://www.docker.com/products/docker-desktop)
3. Enable the Kubernetes in Docker desktop . Path: Docker Dashboard -> settings -> Kubernetes
4. Set up the kubernetes Dashboard, using below command: 
```
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v1.10.1/src/deploy/recommended/kubernetes-dashboard.yaml
```
  * For reference and for some more details, refer to this page: https://collabnix.com/kubernetes-dashboard-on-docker-desktop-for-windows-2-0-0-3-in-2-minutes
  
5.  Start the kubectl Proxy using below command:
```
kubectl proxy
```
6. Now try to log-in to local host dashboard using browser, copy & paste the link: 
   http://localhost:8001/api/v1/namespaces/kube-system/services/https:kubernetes-dashboard:/proxy/#!/login

7. The browser asks the authentication login, to get the authentication login token using the below command:
```
kubectl -n kube-system describe secret default
```
This command displays the token which is generally a larger size string, copy it and use it for authentication of step 6. link: http://localhost:8001/api/v1/namespaces/kube-system/services/https:kubernetes-dashboard:/proxy/#!/login


## SBK docker image with Kubernetes and command line arguments
you can directly run the SBK image with command line arguments , below is an example
```
kubectl run sbk --restart=Never --expose=true  --port=9718 --hostport=9718  --image=kmgowda/sbk:latest -- -class  rabbitmq  -broker 192.168.0.192 -topic kmg-topic-11  -writers 5  -readers 1 -size 100 -seconds 60
```

to pass the command line arguments to SBK image , **"--"** prefix is used before **-class** argument.


Example benchmarking command for a file write with busy box is as follows:
```
kubectl run sbk --restart=Never --expose=true  --port=9718 --hostport=9718  --image=kmgowda/sbk:latest -- -class  file  -writers 1  -size 100 -seconds 60
```