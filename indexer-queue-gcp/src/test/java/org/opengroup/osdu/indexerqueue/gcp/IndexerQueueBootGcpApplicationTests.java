package org.opengroup.osdu.indexerqueue.gcp;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@Ignore
@RunWith(SpringRunner.class)
@SpringBootTest
public class IndexerQueueBootGcpApplicationTests {

    private IndexerQueueBootGcpApplication sut;

    @Test
    public void contextLoads() {
        this.sut.main(new String[] {});
    }
}
