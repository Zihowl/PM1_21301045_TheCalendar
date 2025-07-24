package com.zihowl.thecalendar.data.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Represents a local change that still needs to be synchronized with the server.
 */
public class PendingOperation extends RealmObject {
    @PrimaryKey
    private long id;
    private String entity;
    private String action;
    private String payload;
    private long timestamp;

    public PendingOperation() {}

    public PendingOperation(long id, String entity, String action, String payload, long timestamp) {
        this.id = id;
        this.entity = entity;
        this.action = action;
        this.payload = payload;
        this.timestamp = timestamp;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}