package org.opengroup.osdu.indexerqueue.azure.scope.thread;

import java.util.HashMap;
import java.util.Map;

public class ThreadScopeContext {

    protected final Map<String, Bean> beans = new HashMap<>();

    /**
     * Get a bean value from the context.
     *
     * @param name bean name
     * @return bean value or null
     */
    public Object getBean(String name) {
        Bean bean = beans.get(name);
        if (null == bean) {
            return null;
        }
        return bean.object;
    }

    /**
     * Set a bean in the context.
     *
     * @param name bean name
     * @param object bean value
     */
    public void setBean(String name, Object object) {

        Bean bean = beans.computeIfAbsent(name,k-> new Bean());
        bean.object = object;
    }

    /**
     * Remove a bean from the context, calling the destruction callback if any.
     *
     * @param name bean name
     * @return previous value
     */
    public Object remove(String name) {
        Bean bean = beans.get(name);
        if (null != bean) {
            beans.remove(name);
            if (null != bean.destructionCallback) {
              bean.destructionCallback.run();
            }
            return bean.object;
        }
        return null;
    }

    /**
     * Register the given callback as to be executed after request completion.
     *
     * @param name The name of the bean.
     * @param callback The callback of the bean to be executed for destruction.
     */
    public void registerDestructionCallback(String name, Runnable callback) {
        Bean bean = beans.computeIfAbsent(name,k->new Bean());
        bean.destructionCallback = callback;
    }

    /** Clear all beans and call the destruction callback. */
    public void clear() {
        for (Bean bean : beans.values()) {
            if (null != bean.destructionCallback) {
                bean.destructionCallback.run();
            }
        }
        beans.clear();
    }

    /** Private class storing bean name and destructor callback. */
    private class Bean {

        private Object object;
        private Runnable destructionCallback;

        public Object getObject() {
            return object;
        }

        public void setObject(Object object) {
            this.object = object;
        }

        public Runnable getDestructionCallback() {
            return destructionCallback;
        }

        public void setDestructionCallback(Runnable destructionCallback) {
            this.destructionCallback = destructionCallback;
        }
    }
}
