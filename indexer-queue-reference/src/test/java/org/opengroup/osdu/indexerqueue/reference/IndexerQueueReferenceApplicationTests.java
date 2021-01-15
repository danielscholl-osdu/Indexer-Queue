package org.opengroup.osdu.indexerqueue.reference;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.indexerqueue.reference.IndexerQueueReferenceApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@Ignore
@RunWith(SpringRunner.class)
@SpringBootTest
public class IndexerQueueReferenceApplicationTests {

    private IndexerQueueReferenceApplication sut;

    @Test
    public void contextLoads() {
        this.sut.main(new String[] {});
    }
}
