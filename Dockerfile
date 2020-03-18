FROM adoptopenjdk/openjdk13:alpine

ADD target/autocoin-payment-service*.jar /app/autocoin-payment-service.jar

WORKDIR /app
RUN mkdir -p /app/data
EXPOSE 10023
ENTRYPOINT [ "java", "-XX:+ExitOnOutOfMemoryError", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/app/data", "-XX:+PrintFlagsFinal", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "autocoin-payment-service.jar" ]
