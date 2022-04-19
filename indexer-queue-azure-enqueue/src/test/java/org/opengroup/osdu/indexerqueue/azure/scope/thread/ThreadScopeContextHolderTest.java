package org.opengroup.osdu.indexerqueue.azure.scope.thread;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import static org.junit.jupiter.api.Assertions.*;

class ThreadScopeContextHolderTest {

    private static final ThreadLocal<ThreadScopeContext> CONTEXT_HOLDER = ThreadLocal
            .withInitial(ThreadScopeContext::new);

    @InjectMocks
    ThreadScopeContextHolder sut = new ThreadScopeContextHolder();

    @Test
    void should_returnNotNull_whenSetContext() {
        sut.setContext(new ThreadScopeContext());
        assertNotNull(CONTEXT_HOLDER.get());
    }
}