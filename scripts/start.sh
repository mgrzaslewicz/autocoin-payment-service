#!/usr/bin/env bash

preconditions() {
    if [[ -f "env.properties" ]]
    then
        . "env.properties"
    else
        echo "Can't find env.properties. Maybe forgot to create one in scripts dir?"
        exit 100
    fi

  declare -a requiredVariables=(
    "APP_OAUTH_CLIENT_ID"
    "APP_OAUTH_CLIENT_SECRET"
    "APP_PORT_ON_HOST"
    "DB_PASSWORD"
    "DB_USERNAME"
    "JDBC_URL"
    "LOG_PATH"
    "SERVICE_NAME"
    "VERSION"
  )

  for requiredVariable in "${requiredVariables[@]}"; do
    if [ -z "${!requiredVariable}" ]; then
      echo "$requiredVariable not set. Please edit env.properties file in deployment directory."
      exit 1
    fi
  done


}

preconditions

# Run new container
echo "Starting new version of container. Using version: ${VERSION}";
echo "Using port: ${APP_PORT_ON_HOST}";

# Use JAVA_OPTS="-XX:+ExitOnOutOfMemoryError" to prevent from running when any of threads runs of out memory and dies

docker run --name ${SERVICE_NAME} -d \
    -p ${APP_PORT_ON_HOST}:10023 \
    -e DOCKER_TAG=${VERSION} \
    -e JAVA_OPTS="-XX:+ExitOnOutOfMemoryError" \
    -e APP_OAUTH_CLIENT_ID=${APP_OAUTH_CLIENT_ID} \
    -e APP_OAUTH_CLIENT_SECRET=${APP_OAUTH_CLIENT_SECRET} \
    -e JDBC_URL=${JDBC_URL} \
    -e DB_PASSWORD=${DB_PASSWORD} \
    -e DB_USERNAME=${DB_USERNAME} \
    -e TELEGRAM_BOT_USERNAME=${TELEGRAM_BOT_USERNAME} \
    -e TELEGRAM_BOT_TOKEN=${TELEGRAM_BOT_TOKEN} \
    -e TELEGRAM_PAYMENT_NOTIFICATION_CHAT_ID=${TELEGRAM_PAYMENT_NOTIFICATION_CHAT_ID} \
    -v ${LOG_PATH}:/app/log \
    --memory=200m \
    --restart=no \
    --network autocoin-services-admin \
    localhost:5000/${SERVICE_NAME}:${VERSION}
