//package org.opengroup.osdu.indexerqueue.azure.scope.thread;
//
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Spy;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//class ThreadScopeContextTest {
//
//    final static private String name = "name";
//
//    @Mock
//    Object object;
//
//    @Mock
//    ThreadScopeContext.Bean bean;
//
//    @Mock
//    Runnable runnable;
//
//    @InjectMocks
//    ThreadScopeContext sut = new ThreadScopeContext();
//
//    @Test
//    void should_returnNull_whenKeyNotSetInGetBean() {
//        assertNull(sut.getBean(name));
//    }
//
//    @Test
//    void should_returnSameObjectAddedInSetBean_whenGetBeanCalled() {
//        sut.setBean(name, object);
//        Object response = sut.getBean(name);
//        assertEquals(object, response);
//    }
//
//  @Test
//  void should_returnNull_whenRemoveCalledOnAbsentBean() {
//    assertNull(sut.remove(name));
//  }
//
//  @Test
//  void should_returnBeanObjectAndDeleteBean_whenRemoveCalledOnBean() {
//      sut.setBean(name, object);
//      Object response = sut.remove(name);
//      assertEquals(object, response);
//      assertNull(sut.getBean(name));
//  }
//
//    @Test
//    void should_invoke_setDestructionCallBack_OfBeanClass_whenRegisterDestructionCallback_isCalled() {
//        sut.registerDestructionCallback(name, runnable);
//        verify(bean,times(1)).setDestructionCallback(runnable);
//    }
//
//    @Test
//    void should_clearBean_whenClearCalled() {
//        sut.setBean(name, object);
//        sut.clear();
//        assertNull(sut.getBean(name));
//    }
//
//    @Test
//    void should_callDestructionCallBack_whenClearCalled() {
//      doNothing().when(runnable).run();
//      sut.setBean(name, object);
//      sut.registerDestructionCallback(name,runnable);
//      sut.clear();
//      verify(runnable,times(1)).run();
//    }
//}
