#!/usr/bin/env bash
NAMESPACE="${NAMESPACE:-sbk-benchmark}"
SBK_RABBITMQ_STARTUP_WAIT_SECONDS="${SBK_RABBITMQ_STARTUP_WAIT_SECONDS:-5}"
SCRIPT_DIRECTORY=$(dirname "$0")
kubectl delete -f "${SCRIPT_DIRECTORY}/sbk-rabbitmq-k8-sample.yaml" -n "${NAMESPACE}"
kubectl apply -f "${SCRIPT_DIRECTORY}/sbk-rabbitmq-k8-sample.yaml" -n "${NAMESPACE}"
sleep "${SBK_RABBITMQ_STARTUP_WAIT_SECONDS}s"
kubectl logs -f jobs/sbk-rabbitmq-k8 -n "${NAMESPACE}"
