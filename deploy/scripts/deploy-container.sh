#!/bin/bash
# deploy/scripts/deploy-container.sh
# Usage: ./deploy-container.sh <service_name> <host_port> <env_file>

set -e

SERVICE_NAME=$1
HOST_PORT=$2
ENV_FILE=$3

if [ -z "$SERVICE_NAME" ] || [ -z "$HOST_PORT" ] || [ -z "$ENV_FILE" ]; then
  echo "Usage: $0 <service_name> <host_port> <env_file>"
  exit 1
fi

docker stop "$SERVICE_NAME" || true
docker rm "$SERVICE_NAME" || true

docker run -d \
  --name "$SERVICE_NAME" \
  --restart unless-stopped \
  -p "$HOST_PORT":8080 \
  --env-file "$ENV_FILE" \
  "$SERVICE_NAME":latest