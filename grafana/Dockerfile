ARG GRAFANA_IMAGE=grafana/grafana:12.4.1
FROM ${GRAFANA_IMAGE}
COPY ./provisioning /etc/grafana/provisioning
COPY ./config.ini /etc/grafana/grafana.ini

# for dashboards, let the dashboard directory is supplied as a mounted volume.
#ADD ./dashboards /var/lib/grafana/dashboards
