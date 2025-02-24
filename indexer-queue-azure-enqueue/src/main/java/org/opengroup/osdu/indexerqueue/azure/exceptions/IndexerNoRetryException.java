package org.opengroup.osdu.indexerqueue.azure.exceptions;

public class IndexerNoRetryException extends RuntimeException {

  public IndexerNoRetryException(String message) {
    super(message);
  }
}
