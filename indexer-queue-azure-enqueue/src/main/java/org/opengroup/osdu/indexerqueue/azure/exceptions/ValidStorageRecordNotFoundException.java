
package org.opengroup.osdu.indexerqueue.azure.exceptions;

public class ValidStorageRecordNotFoundException extends RuntimeException {
  public ValidStorageRecordNotFoundException(String message){
    super(message);
  }
}
