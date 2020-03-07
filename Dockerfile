FROM adoptopenjdk/openjdk13:alpine

ADD target/autocoin-payment-service*.jar /app/autocoin-payment-service.jar

WORKDIR /app
EXPOSE 10023
ENTRYPOINT [ "java", "-XX:+ExitOnOutOfMemoryError", "-Djava.security.egd=file:/dev/./urandom", "-jar", "autocoin-payment-service.jar" ]
