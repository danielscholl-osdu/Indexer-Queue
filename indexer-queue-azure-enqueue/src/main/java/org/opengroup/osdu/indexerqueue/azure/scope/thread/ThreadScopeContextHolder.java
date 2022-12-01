package org.opengroup.osdu.indexerqueue.azure.scope.thread;

public final class ThreadScopeContextHolder {

    private static final ThreadLocal<ThreadScopeContext> CONTEXT_HOLDER = ThreadLocal
            .withInitial(ThreadScopeContext::new);

    private ThreadScopeContextHolder() {
        // utility object, not allowed to create instances
    }

    /**
     * Get the thread specific context.
     *
     * @return thread scoped context
     */
    public static ThreadScopeContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * Set the thread specific context.
     *
     * @param context thread scoped context
     */
    public static void setContext(ThreadScopeContext context) {
        CONTEXT_HOLDER.set(context);
    }

    public static void clearContext() {
        CONTEXT_HOLDER.remove();
    }
}
