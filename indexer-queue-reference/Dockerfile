FROM openjdk:8-slim
WORKDIR /app
COPY target/indexer-queue-reference-*-spring-boot.jar indexer-queue-reference.jar
# Run the web service on container startup.
CMD java -Djava.security.egd=file:/dev/./urandom -Dserver.port=8080 -jar /app/indexer-queue-reference.jar
