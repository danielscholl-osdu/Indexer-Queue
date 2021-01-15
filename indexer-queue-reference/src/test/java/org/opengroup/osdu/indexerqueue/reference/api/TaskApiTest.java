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

package org.opengroup.osdu.indexerqueue.reference.api;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.opengroup.osdu.indexerqueue.reference.util.TaskBuilder;
import org.springframework.http.HttpStatus;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.model.search.CloudTaskRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class TaskApiTest {

  @Mock
  private TaskBuilder taskBuilder;
  @InjectMocks
  private TaskApi sut;

  @Before
  public void setup() {
    initMocks(this);
    when(taskBuilder.createTask(any())).thenReturn(HttpStatus.OK);
  }

  @Test
  public void should_enqueueTask() {
    CloudTaskRequest request = CloudTaskRequest.builder().url("dummy").message("message").build();

    ResponseEntity response = sut.enqueueTask(request);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(taskBuilder).createTask(request);
  }
}
