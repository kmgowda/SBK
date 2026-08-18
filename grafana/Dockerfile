ARG GRAFANA_IMAGE=grafana/grafana:12.4.1
FROM ${GRAFANA_IMAGE}
ADD ./provisioning /etc/grafana/provisioning
ADD ./config.ini /etc/grafana/config.ini

# for dashboards, let the dashboard directory is supplied as a mounted volume.
#ADD ./dashboards /var/lib/grafana/dashboards
