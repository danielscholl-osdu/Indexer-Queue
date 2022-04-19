package org.opengroup.osdu.indexerqueue.azure.scope.thread;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doAnswer;

@RunWith(PowerMockRunner.class)
class ThreadScopeTest {

    final static private String name = "name";

    @Mock
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
        }
    };

    @InjectMocks
    ThreadScope sut = new ThreadScope();

    @Test
    void should_returnObject_whenNameIsGet() {
        assertNotNull(sut.get(name, new ObjectFactory<Object>() {
            @Override
            public Object getObject() throws BeansException {
                return new Object();
            }
        }));
    }

    @Test
    void should_removeObjectWithName_whenRemoveCalled() {
        assertNull(sut.remove(name));
    }

    @Test
    void should_returnNotNull_whenRegisterDestructionCallback() {
        sut.registerDestructionCallback(name, runnable);
        assertNotNull(ThreadScopeContextHolder.getContext());
    }

    @Test
    void should_returnNull_whenResolveContextualObject() {
        assertNull(sut.resolveContextualObject(name));
    }

    @Test
    void should_returnGetConversationId_whenCalled() {
        assertNotNull(sut.getConversationId());
    }

    @Test
    void should_destroyThreadScopeContextHolder() {
        sut.destroy();
        assertNotNull(ThreadScopeContextHolder.getContext());
    }
}