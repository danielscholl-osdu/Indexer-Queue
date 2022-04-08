/*
 * Copyright 2022 Google LLC
 * Copyright 2022 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexerqueue.gcp.cloudrun.oqm.subscribe;

import java.io.IOException;
import java.util.Optional;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.opengroup.osdu.core.auth.TokenProvider;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.CloudTaskRequest;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.gcp.oqm.driver.OqmDriver;
import org.opengroup.osdu.core.gcp.oqm.model.OqmAckReplier;
import org.opengroup.osdu.core.gcp.oqm.model.OqmDestination;
import org.opengroup.osdu.core.gcp.oqm.model.OqmMessage;
import org.opengroup.osdu.core.gcp.oqm.model.OqmMessageReceiver;
import org.opengroup.osdu.core.gcp.oqm.model.OqmSubscriber;
import org.opengroup.osdu.core.gcp.oqm.model.OqmSubscription;
import org.opengroup.osdu.core.gcp.oqm.model.OqmSubscriptionQuery;
import org.opengroup.osdu.core.gcp.oqm.model.OqmTopic;
import org.opengroup.osdu.indexerqueue.gcp.cloudrun.config.IndexerQueueConfigProperties;
import org.opengroup.osdu.indexerqueue.gcp.cloudrun.oqm.publish.MessagePublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Runs once on the service start when PULL subscription type is activated.
 * Fetches all tenants' oqm destinations for TOPIC existence.
 * If exists - searches for pull SUBSCRIPTION existence.
 * Creates SUBSCRIPTION if it doesn't exist. Then subscribe itself on SUBSCRIPTION.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnExpression("'${subscription-type}' == 'pull' and '${oqmDriver}' != null")
public class OqmSubscriberManager {

  private static final String SUBSCRIPTION_PREFIX = "indexer-queue-oqm-";
  private static final String ACKNOWLEDGE = "message acknowledged by client";
  private static final String NOT_ACKNOWLEDGE = "message not acknowledged by client";

  private final OqmDriver driver;
  private final IndexerQueueConfigProperties properties;
  private final ITenantFactory tenantInfoFactory;
  private final MessagePublisher messagePublisher;
  private final TokenProvider tokenProvider;

  private String subscriptionName;

  @PostConstruct
  void postConstruct() {
    subscriptionName = getSubscriptionName();
    log.info("OqmSubscriberManager provisioning STARTED");
    tenantInfoFactory.listTenantInfo().forEach(this::createSubscriptionForTenant);
    log.info("OqmSubscriberManager provisioning COMPLETED");
  }

  private void createSubscriptionForTenant(TenantInfo tenantInfo) {
    String topicName = properties.getRecordsChangedTopicName();
    log.info("OQM: provisioning tenant {}:", tenantInfo.getDataPartitionId());
    log.info("OQM: check for topic {} existence:", topicName);
    OqmTopic topic = driver.getTopic(topicName, getDestination(tenantInfo))
        .orElse(null);
    if (topic == null) {
      log.info("OQM: check for topic {} existence: ABSENT. Skipped", topicName);
      return;
    }

    log.info("OQM: check for topic {} existence: PRESENT", topicName);
    OqmSubscription subscription = getSubscription(tenantInfo, topic);

    if (subscription == null) {
      subscription = createSubscription(tenantInfo, topic);
    } else {
      log.info("OQM: check for subscription {} existence: PRESENT", subscriptionName);
    }

    registerSubscriber(tenantInfo, subscription);
    log.info("OQM: provisioning tenant {}: COMPLETED.", tenantInfo.getDataPartitionId());
  }

  @Nullable
  private OqmSubscription getSubscription(TenantInfo tenantInfo, OqmTopic topic) {
    log.info("OQM: check for subscription {} existence:", subscriptionName);
    OqmSubscriptionQuery query = OqmSubscriptionQuery.builder()
        .namePrefix(subscriptionName)
        .subscriberable(true)
        .build();
    return driver
        .listSubscriptions(topic, query, getDestination(tenantInfo)).stream()
        .findAny()
        .orElse(null);
  }

  private OqmSubscription createSubscription(TenantInfo tenantInfo, OqmTopic topic) {
    log.info("OQM: check for subscription {} existence: ABSENT. Will create.", subscriptionName);
    OqmSubscription request = OqmSubscription.builder()
        .topic(topic)
        .name(subscriptionName)
        .build();
    return driver.createAndGetSubscription(request, getDestination(tenantInfo));
  }

  private void registerSubscriber(TenantInfo tenantInfo, OqmSubscription subscription) {
    log.info("OQM: registering Subscriber for subscription {}", subscription.getName());
    OqmDestination destination = getDestination(tenantInfo);

    OqmMessageReceiver receiver = (oqmMessage, oqmAckReplier) -> {
      log.info("OQM message: {} - {} - {}", oqmMessage.getId(), oqmMessage.getData(),
          oqmMessage.getAttributes());
      try {
        ackSendingMessage(oqmMessage, oqmAckReplier);
      } catch (Exception e) {
        log.debug(NOT_ACKNOWLEDGE, e.getMessage());
        oqmAckReplier.nack();
      }
    };

    OqmSubscriber subscriber = OqmSubscriber.builder()
        .subscription(subscription)
        .messageReceiver(receiver)
        .build();
    driver.subscribe(subscriber, destination);
    log.info("OQM: provisioning subscription {}: Subscriber REGISTERED.", subscription.getName());
  }

  private void ackSendingMessage(OqmMessage oqmMessage, OqmAckReplier oqmAckReplier) throws IOException {
    CloudTaskRequest request = CloudTaskRequest.builder()
        .url(getRelativeIndexerWorkerUrl(oqmMessage))
        .message(oqmMessage.getData())
        .build();

    DpsHeaders headers = getHeaders(oqmMessage);
    if (headers.getPartitionId() == null) {
      log.error("Partition id is not set for message: {}", oqmMessage.getId());
      oqmAckReplier.ack();
    }

    HttpStatus response = messagePublisher.sendMessage(request, headers);
    log.debug("OQM: Send request to Indexer. Response: {}", response);
    // ack message always due to retries in publisher side
    oqmAckReplier.ack();
  }

  private String getRelativeIndexerWorkerUrl(OqmMessage oqmMessage) {
    return Optional.ofNullable(oqmMessage.getAttributes().get("relative-indexer-worker-url"))
        .orElse(properties.getDefaultRelativeIndexerWorkerUrl());
  }

  @NotNull
  private DpsHeaders getHeaders(OqmMessage oqmMessage) {
    DpsHeaders headers = new DpsHeaders();
    headers.getHeaders().put("data-partition-id", oqmMessage.getAttributes().get("data-partition-id"));
    headers.getHeaders().put("user", oqmMessage.getAttributes().get("user"));
    headers.getHeaders().put("correlation-id", oqmMessage.getAttributes().get("correlation-id"));
    headers.getHeaders().put("account-id", oqmMessage.getAttributes().get("account-id"));
    headers.getHeaders().put("authorization", tokenProvider.getIdToken());
    return headers;
  }

  private OqmDestination getDestination(TenantInfo tenantInfo) {
    return OqmDestination.builder()
        .partitionId(tenantInfo.getDataPartitionId())
        .build();
  }

  private String getSubscriptionName() {
    return SUBSCRIPTION_PREFIX + properties.getRecordsChangedTopicName();
  }
}
