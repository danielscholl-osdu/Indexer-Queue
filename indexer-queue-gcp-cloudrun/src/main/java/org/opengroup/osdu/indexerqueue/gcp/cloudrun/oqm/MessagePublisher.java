package org.opengroup.osdu.indexerqueue.gcp.cloudrun.oqm;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
public interface MessagePublisher {

    HttpStatus sendMessage(String message) throws IOException;

}
