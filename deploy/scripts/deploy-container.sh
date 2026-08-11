#!/bin/bash
# Usage: ./deploy-container.sh <service_name> <host_port> <container_port> <env_file>

set -e

SERVICE_NAME=$1
HOST_PORT=$2
CONTAINER_PORT=$3
ENV_FILE=$4

if [ -z "$SERVICE_NAME" ] || [ -z "$HOST_PORT" ] || [ -z "$CONTAINER_PORT" ] || [ -z "$ENV_FILE" ]; then
  echo "Usage: $0 <service_name> <host_port> <container_port> <env_file>"
  exit 1
fi

mkdir -p /var/log/apps/"$SERVICE_NAME"

docker stop "$SERVICE_NAME" || true
docker rm "$SERVICE_NAME" || true

docker run -d \
  --name "$SERVICE_NAME" \
  --network dear-net \
  --restart unless-stopped \
  -p "$HOST_PORT":"$CONTAINER_PORT" \
  --env-file "$ENV_FILE" \
  -v /var/log/apps/"$SERVICE_NAME":/logs \
  "$SERVICE_NAME":latest