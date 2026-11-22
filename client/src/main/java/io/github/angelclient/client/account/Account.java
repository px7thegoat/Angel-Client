package io.github.angelclient.client.account;

public class Account {
    public enum Type { OFFLINE, MICROSOFT }

    private Type type;
    private String username;
    private String uuid;

    public Account(Type type, String username, String uuid) {
        this.type = type;
        this.username = username;
        this.uuid = uuid;
    }

    public Type getType() { return type; }
    public String getUsername() { return username; }
    public String getUuid() { return uuid; }

    public void setType(Type type) { this.type = type; }
    public void setUsername(String username) { this.username = username; }
    public void setUuid(String uuid) { this.uuid = uuid; }
}
