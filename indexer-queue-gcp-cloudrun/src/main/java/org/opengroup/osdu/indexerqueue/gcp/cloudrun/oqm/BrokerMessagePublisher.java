package org.opengroup.osdu.indexerqueue.gcp.cloudrun.oqm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.gcp.oqm.driver.OqmDriver;
import org.opengroup.osdu.core.gcp.oqm.model.OqmDestination;
import org.opengroup.osdu.core.gcp.oqm.model.OqmMessage;
import org.opengroup.osdu.core.gcp.oqm.model.OqmTopic;
import org.opengroup.osdu.core.gcp.util.HeadersInfo;
import org.opengroup.osdu.indexerqueue.gcp.cloudrun.config.PropertiesConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(value = "indexer-queue.task.enable", havingValue = "false", matchIfMissing = true)
@Slf4j
@RequiredArgsConstructor
@Component
public class BrokerMessagePublisher implements MessagePublisher {

    private final OqmDriver driver;

    private final HeadersInfo headersInfo;

    private final PropertiesConfiguration config;

    public HttpStatus sendMessage(String message){
        OqmMessage oqmMessage = OqmMessage.builder()
                .attributes(headersInfo.getHeaders().getHeaders())
                .data(message)
                .build();

        try {
            driver.publish(oqmMessage, getTopic(), getDestination());
        } catch (Exception e) {
            log.info(e.getMessage());
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.OK;
    }

    private OqmTopic getTopic() {
        return OqmTopic.builder()
                .name(config.getDefaultQueueName())
                .build();
    }

    private OqmDestination getDestination() {
        return OqmDestination.builder()
                .partitionId(headersInfo.getPartitionId())
                .build();
    }
}
