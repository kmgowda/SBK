FROM grafana/grafana:8.5.1
ADD ./provisioning /etc/grafana/provisioning
ADD ./config.ini /etc/grafana/config.ini

# for dashboards, let the dashboard directory is supplied as a mounted volume.
#ADD ./dashboards /var/lib/grafana/dashboards