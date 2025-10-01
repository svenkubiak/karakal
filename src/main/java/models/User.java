package models;

import io.mangoo.annotations.Collection;
import io.mangoo.persistence.Entity;
import io.mangoo.utils.Arguments;

import java.time.LocalDateTime;

@Collection(name = "users")
public class User extends Entity {
    private String appId;
    private String username;
    private String attestedCredentialData;
    private String coseKey;
    private byte[] credentialId;
    private byte[] publicKeyCose;
    private long signCount;
    private LocalDateTime createdAt;

    public User() {}

    public User(String username) {
        this.username = Arguments.requireNonBlank(username, "username can not be null or empty");
        this.createdAt = LocalDateTime.now();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getSignCount() {
        return signCount;
    }

    public void setSignCount(long signCount) {
        this.signCount = signCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public byte[] getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(byte[] credentialId) {
        this.credentialId = credentialId;
    }

    public byte[] getPublicKeyCose() {
        return publicKeyCose;
    }

    public void setPublicKeyCose(byte[] publicKeyCose) {
        this.publicKeyCose = publicKeyCose;
    }

    public String getAttestedCredentialData() {
        return attestedCredentialData;
    }

    public void setAttestedCredentialData(String attestedCredentialData) {
        this.attestedCredentialData = attestedCredentialData;
    }

    public String getCoseKey() {
        return coseKey;
    }

    public void setCoseKey(String coseKey) {
        this.coseKey = coseKey;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }
}
