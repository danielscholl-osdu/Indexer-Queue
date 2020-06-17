// Copyright 2017-2019, Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.indexerqueue.gcp.api;

import lombok.extern.java.Log;
import org.opengroup.osdu.core.common.SwaggerDoc;
import org.opengroup.osdu.core.common.model.search.CloudTaskRequest;
import org.opengroup.osdu.indexerqueue.gcp.util.AppEngineTaskBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.IOException;

@Log
@RestController
@RequestMapping("/_dps/task-handlers")
public class TaskApi {

    @Autowired
    private AppEngineTaskBuilder appEngineTaskBuilder;

    // THIS IS AN INTERNAL USE API ONLY
    // THAT MEANS WE DON'T DOCUMENT IT IN SWAGGER, ACCESS IS LIMITED TO INDEXER SERVICE
//    @RolesAllowed({SearchServiceRole.ADMIN})
    @PostMapping("/enqueue")
    public ResponseEntity enqueueTask(@NotNull(message = SwaggerDoc.REQUEST_VALIDATION_NOT_NULL_BODY) @Valid @RequestBody CloudTaskRequest request) throws IOException {

        this.appEngineTaskBuilder.createTask(request);

        return new ResponseEntity(HttpStatus.OK);
    }
}
