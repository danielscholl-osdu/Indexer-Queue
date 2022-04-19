package org.opengroup.osdu.indexerqueue.azure.scope.thread;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ThreadScopeContextTest {

    final static private String name = "name";

    @Mock
    ThreadScopeContext.Bean bean;

    @InjectMocks
    ThreadScopeContext sut = new ThreadScopeContext();

    @Test
    void should_returnNull_whenKeyNotSetInGetBean() {
        assertNull(sut.getBean(name));
    }

    @Test
    void should_returnNotNull_whenSetBean() {
        ThreadScopeContext.Bean bean = new ThreadScopeContext.Bean();
        sut.setBean(name, bean);
        assertNotNull(sut.getBean(name));
    }

    @Test
    void should_registerDestructionCall_whenRegisterDestructionCallback() {
        sut.registerDestructionCallback(name, new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    @Test
    void should_clearBean_whenClearCalled() {
        ThreadScopeContext.Bean bean = new ThreadScopeContext.Bean();
        sut.setBean(name, bean);
        sut.clear();
        assertNull(sut.getBean(name));
    }
}