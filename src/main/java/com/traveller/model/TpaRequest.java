package com.traveller.model;

import java.util.UUID;

public final class TpaRequest {

    public enum Type {
        TPA,
        TPAHERE
    }

    private final UUID requester;
    private final UUID target;
    private final Type type;
    private final long createdAt;

    public TpaRequest(UUID requester, UUID target, Type type) {
        this.requester = requester;
        this.target = target;
        this.type = type;
        this.createdAt = System.currentTimeMillis();
    }

    public UUID getRequester() {
        return requester;
    }

    public UUID getTarget() {
        return target;
    }

    public Type getType() {
        return type;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired(long timeoutMillis) {
        return System.currentTimeMillis() - createdAt > timeoutMillis;
    }
}
