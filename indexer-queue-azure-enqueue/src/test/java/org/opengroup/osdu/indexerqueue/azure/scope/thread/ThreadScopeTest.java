package org.opengroup.osdu.indexerqueue.azure.scope.thread;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThreadScopeTest {

    final static private String name = "name";

    @Mock
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
        }
    };

    @Mock
    ThreadScopeContext threadScopeContext;

    @InjectMocks
    ThreadScope sut = new ThreadScope();

    @Test
    void should_returnObject_whenGetIsCalled_whenBeanNotPresent() {
        assertNotNull(sut.get(name, new ObjectFactory<Object>() {
            @Override
            public Object getObject() throws BeansException {
                return new Object();
            }
        }));
    }

  @Test
  void should_returnSameObject_whenGetIsCalled_whenBeanPresent() {
    Object expectedObject = sut.get(name, new ObjectFactory<Object>() {
      @Override
      public Object getObject() throws BeansException {
        return new Object();
      }
    });

    Object responseObject = sut.get(name, new ObjectFactory<Object>() {
      @Override
      public Object getObject() throws BeansException {
        return new Object();
      }
    });

    assertEquals(expectedObject, responseObject);
  }

  @Test
  void should_invoke_removeMethodOfThreadScopeContext_when_RemoveCalled() {
    try (MockedStatic<ThreadScopeContextHolder> mockedHolder = mockStatic(ThreadScopeContextHolder.class)) {
      mockedHolder.when(() -> ThreadScopeContextHolder.getContext()).thenReturn(threadScopeContext);
      when(threadScopeContext.remove(name)).thenReturn(null);

      sut.remove(name);

      verify(threadScopeContext, times(1)).remove(name);
    }
  }

  @Test
    void should_invoke_RegisterDestructionCallbackMethodOfThreadScopeContext_whenRegisterDestructionCallbackCalled() {
    try (MockedStatic<ThreadScopeContextHolder> mockedHolder = mockStatic(ThreadScopeContextHolder.class)) {
      mockedHolder.when(() -> ThreadScopeContextHolder.getContext()).thenReturn(threadScopeContext);
      doNothing().when(threadScopeContext).registerDestructionCallback(name, runnable);

      sut.registerDestructionCallback(name, runnable);

      verify(threadScopeContext, times(1)).registerDestructionCallback(name, runnable);
    }
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
    void should_invoke_clearContextMethodOfThreadScopeContextHolder_when_destroyCalled() {
      try (MockedStatic<ThreadScopeContextHolder> mockedHolder = mockStatic(ThreadScopeContextHolder.class)) {
        mockedHolder.when(() -> ThreadScopeContextHolder.clearContext()).thenCallRealMethod();

        sut.destroy();

        mockedHolder.verify(
          () -> ThreadScopeContextHolder.clearContext(),
          times(1)
        );
      }
    }
}
