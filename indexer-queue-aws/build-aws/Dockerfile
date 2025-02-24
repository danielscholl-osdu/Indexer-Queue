# Copyright © 2020 Amazon Web Services
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html
FROM registry.repo.osdu.aws/amazoncorretto:17-alpine

RUN apk update && \
    apk add --no-cache \
    bash \
    curl \
    shadow \
    && \
    /usr/sbin/groupadd -g 10001 -r appgroup && \
    /usr/sbin/useradd -M -g 10001 -r -u 10001 appuser

ARG JAR_FILE=indexer-queue-aws/target/indexer-queue*-spring-boot.jar
WORKDIR /
COPY ${JAR_FILE} app.jar
COPY /indexer-queue-aws/build-aws/entrypoint.sh /entrypoint.sh
EXPOSE 8080

USER 10001:10001
ENTRYPOINT ["/bin/sh", "-c", ". /entrypoint.sh"]
