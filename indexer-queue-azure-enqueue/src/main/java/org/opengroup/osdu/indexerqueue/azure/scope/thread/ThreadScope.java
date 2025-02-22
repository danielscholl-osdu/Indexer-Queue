package org.opengroup.osdu.indexerqueue.azure.scope.thread;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

/**
 * Thread scope which allows putting data in thread scope and clearing up afterwards.
 */

public class ThreadScope implements Scope, DisposableBean {

    /**
     * Get bean for given name in the "ThreadScope".
     */
    public Object get(String name, ObjectFactory<?> factory) {
        ThreadScopeContext context = ThreadScopeContextHolder.getContext();

        Object result = context.getBean(name);
        if (null == result) {
            result = factory.getObject();
            context.setBean(name, result);
        }
        return result;
    }

    /**
     * Removes bean from scope.
     */
    public Object remove(String name) {
        ThreadScopeContext context = ThreadScopeContextHolder.getContext();
        return context.remove(name);
    }

    public void registerDestructionCallback(String name, Runnable callback) {
        ThreadScopeContextHolder.getContext().registerDestructionCallback(name, callback);
    }

    /**
     * Resolve the contextual object for the given key, if any. E.g. the HttpServletRequest object for key "request".
     */
    public Object resolveContextualObject(String key) {
        return null;
    }

    /**
     * Return the conversation ID for the current underlying scope, if any.
     * <p/>
     * In this case, it returns the thread name.
     */
    public String getConversationId() {
        return Thread.currentThread().getName();
    }

    @Override
    public void destroy() {
        ThreadScopeContextHolder.clearContext();
    }
}


