#!/usr/bin/env bash
# Ejecuta verify, carga .env y lanza sonar:sonar con -Dsonar.token (el plugin Maven no lee SONAR_TOKEN del entorno).
set -e
cd "$(dirname "$0")"
./mvnw verify -pl products-service,inventory-service -am
set -a
[ -f .env ] && . ./.env
set +a
if [ -z "$SONAR_TOKEN" ]; then
  echo "Error: SONAR_TOKEN no está definido. Ponlo en .env o exporta SONAR_TOKEN antes de ejecutar este script."
  exit 1
fi
./mvnw sonar:sonar \
  -Dsonar.host.url="${SONAR_HOST_URL:-http://localhost:9000}" \
  -Dsonar.token="$SONAR_TOKEN" \
  ${SONAR_ORGANIZATION:+ -Dsonar.organization="$SONAR_ORGANIZATION"} \
  ${SONAR_PROJECT_KEY:+ -Dsonar.projectKey="$SONAR_PROJECT_KEY"}
