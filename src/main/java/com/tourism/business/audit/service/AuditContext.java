package com.tourism.business.audit.service;

import java.util.UUID;

public final class AuditContext implements AutoCloseable {
    public record Identity(Long actorId, UUID requestId) {}
    private static final ThreadLocal<Identity> CURRENT = new ThreadLocal<>();
    private final Identity previous;

    private AuditContext(Long actorId, UUID requestId) {
        previous = CURRENT.get();
        CURRENT.set(new Identity(actorId, requestId));
    }

    public static AuditContext open(Long actorId, UUID requestId) {
        return new AuditContext(actorId, java.util.Objects.requireNonNull(requestId));
    }

    public static Identity current() {
        return CURRENT.get();
    }

    @Override
    public void close() {
        if (previous == null) CURRENT.remove();
        else CURRENT.set(previous);
    }
}
