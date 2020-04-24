#!/usr/bin/env bash
NAMESPACE="default"
DIR=$(dirname $0)
kubectl delete -f ${DIR}/sbk-rabbitmq-k8-sample.yaml -n ${NAMESPACE}
kubectl apply -f ${DIR}/sbk-rabbitmq-k8-sample.yaml -n ${NAMESPACE}
sleep 5s
kubectl logs -f jobs/sbk-rabbitmq-k8 -n ${NAMESPACE}



