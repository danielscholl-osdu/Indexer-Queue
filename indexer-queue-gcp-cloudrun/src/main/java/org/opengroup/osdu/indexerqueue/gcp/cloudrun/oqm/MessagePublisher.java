package org.opengroup.osdu.indexerqueue.gcp.cloudrun.oqm;

import org.opengroup.osdu.core.common.model.search.CloudTaskRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
public interface MessagePublisher {

    HttpStatus sendMessage(CloudTaskRequest request) throws IOException;

}
