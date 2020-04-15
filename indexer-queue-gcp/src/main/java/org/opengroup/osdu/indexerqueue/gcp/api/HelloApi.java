package org.opengroup.osdu.indexerqueue.gcp.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class HelloApi {

    @GetMapping("/")
    public String Hello() {
        return "Hello Indexer Queue";
    }
}
