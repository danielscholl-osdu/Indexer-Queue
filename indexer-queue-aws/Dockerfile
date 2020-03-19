FROM amazoncorretto:8

ARG JAR_VERSION
ENV JAR_FILE=indexer-queue-aws-${JAR_VERSION}.jar

WORKDIR /
COPY indexer-queue-aws/target/$JAR_FILE $JAR_FILE

CMD java -jar $JAR_FILE
