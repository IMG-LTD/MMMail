package com.mmmail.foundation.tenant;

public final class TenantScopeContextHolder {

    private static final ThreadLocal<TenantScopeContext> HOLDER = new ThreadLocal<>();

    private TenantScopeContextHolder() {
    }

    public static TenantScopeContext get() {
        return HOLDER.get();
    }

    public static void set(TenantScopeContext context) {
        HOLDER.set(context);
    }

    public static void clear() {
        HOLDER.remove();
    }
}
