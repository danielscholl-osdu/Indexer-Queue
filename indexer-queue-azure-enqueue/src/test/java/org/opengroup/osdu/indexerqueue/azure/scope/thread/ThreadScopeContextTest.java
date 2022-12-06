package org.opengroup.osdu.indexerqueue.azure.scope.thread;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThreadScopeContextTest {

    final static private String name = "name";

    @Mock
    Object object;

    @Mock
    ThreadScopeContext.Bean bean;

    @Mock
    Runnable runnable;

    @InjectMocks
    ThreadScopeContext sut = new ThreadScopeContext();

    @Test
    void should_returnNull_whenKeyNotSetInGetBean() {
        assertNull(sut.getBean(name));
    }

    @Test
    void should_returnSameObjectAddedInSetBean_whenGetBeanCalled() {
        sut.setBean(name, object);
        Object response = sut.getBean(name);

        assertEquals(object, response);
    }

  @Test
  void should_returnNull_whenRemoveCalledOnAbsentBean() {
    assertNull(sut.remove(name));
  }

  @Test
  void should_returnBeanObjectAndDeleteBean_whenRemoveCalledOnBean() {
      sut.setBean(name, object);
      Object response = sut.remove(name);

      assertEquals(object, response);
      assertNull(sut.getBean(name));
  }

    @Test
    void should_setDestructionCallBack_whenRegisterDestructionCallback_isCalled() {
      doNothing().when(runnable).run();
      sut.registerDestructionCallback(name, runnable);
      sut.remove(name);

      verify(runnable,times(1)).run();
    }

    @Test
    void should_clearBean_whenClearCalled() {
        sut.setBean(name, object);
        sut.clear();

        assertNull(sut.getBean(name));
    }

    @Test
    void should_callDestructionCallBack_whenClearCalled() {
      doNothing().when(runnable).run();
      sut.setBean(name, object);
      sut.registerDestructionCallback(name,runnable);
      sut.clear();

      verify(runnable,times(1)).run();
    }
}
