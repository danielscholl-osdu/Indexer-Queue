/*
 * Copyright 2020 Google LLC
 * Copyright 2020 EPAM Systems, Inc
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

package org.opengroup.osdu.indexerqueue.reference.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.CloudTaskRequest;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.indexerqueue.reference.messagebus.IMessageFactory;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class TaskBuilderTest extends TestCase {

  @Mock
  private IMessageFactory mq;
  @InjectMocks
  private TaskBuilder taskBuilder;

  private final String requestBodyValid = "{\"message\":{\"data\":\"W3tcImlkXCI6XCJ0ZW5hbnQxOndlbGxkYjp3ZWxsYm9yZS1kOTAzM2FlMS1mYjE1LTQ5NmMtOWJhMC04ODBmZDFkMmIyY2ZcIixcImtpbmRcIjpcInRlbmFudDE6d2VsbGRiOndlbGxib3JlOjEuMC4wXCIsXCJvcFwiOlwiY3JlYXRlXCJ9XQ==\",\"attributes\":{\"account-id\":\"tenant1\",\"slb-correlation-id\":\"b5a281bd-f59d-4db2-9939-b2d85036fc7e\"},\"messageId\":\"75328163778221\", \"publishTime\":\"2018-05-08T21:48:56.131Z\"}}";

  @Before
  public void setup() {
    initMocks(this);
  }

  @Test
  public void testCreateTask() {
    RecordChangedMessages recordChangedMessages = new RecordChangedMessages();
    DpsHeaders headers = new DpsHeaders();
    taskBuilder.createTask(recordChangedMessages, headers);
    verify(mq).sendMessage(any(), any());
  }

  @Test
  public void testTestCreateTask() {
    CloudTaskRequest request = CloudTaskRequest.builder().url("dummy").message(requestBodyValid).build();
    taskBuilder.createTask(request);
    verify(mq).sendMessage(any(), any());
  }
}
