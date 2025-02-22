package org.opengroup.osdu.indexerqueue.aws.api;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;


@Component
@ConditionalOnProperty(value = "indexer-queue.process-v2-queue", havingValue = "true")
public class IndexerQueueV2 extends AbstractIndexerQueue {

    public IndexerQueueV2() {
        super(new EnvironmentVariables().getQueueUrlV2());
    }

}
