package models;

import com.webauthn4j.credential.CredentialRecord;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.client.CollectedClientData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Credential implements CredentialRecord {
    private final byte[] credentialId;
    private final byte[] credentialPublicKey;
    private final AttestedCredentialData attestedCredentialData;
    private long counter;
    private Boolean uvInitialized;
    private Boolean backupEligible;
    private Boolean backedUp;

    public Credential(byte[] credentialId, byte[] credentialPublicKey, long signCount, AttestedCredentialData attestedCredentialData) {
        this(credentialId, credentialPublicKey, signCount, attestedCredentialData, null, null, null);
    }

    public Credential(byte[] credentialId,
                      byte[] credentialPublicKey,
                      long signCount,
                      AttestedCredentialData attestedCredentialData,
                      Boolean uvInitialized,
                      Boolean backupEligible,
                      Boolean backedUp) {
        this.credentialId = credentialId;
        this.credentialPublicKey = credentialPublicKey;
        this.counter = signCount;
        this.attestedCredentialData = attestedCredentialData;
        this.uvInitialized = uvInitialized;
        this.backupEligible = backupEligible;
        this.backedUp = backedUp;
    }

    public byte[] getCredentialId() {
        return credentialId;
    }

    public byte[] getCredentialPublicKey() {
        return credentialPublicKey;
    }

    @Nullable
    @Override
    public Boolean isUvInitialized() {
        return uvInitialized;
    }

    @Override
    public void setUvInitialized(boolean uvInitialized) {
        this.uvInitialized = uvInitialized;
    }

    @Nullable
    @Override
    public Boolean isBackupEligible() {
        return backupEligible;
    }

    @Override
    public void setBackupEligible(boolean backupEligible) {
        this.backupEligible = backupEligible;
    }

    @Nullable
    @Override
    public Boolean isBackedUp() {
        return backedUp;
    }

    @Override
    public void setBackedUp(boolean backedUp) {
        this.backedUp = backedUp;
    }

    @Override
    public @Nullable CollectedClientData getClientData() {
        return null;
    }

    @NotNull
    @Override
    public AttestedCredentialData getAttestedCredentialData() {
        return attestedCredentialData;
    }

    @Override
    public long getCounter() {
        return counter;
    }

    @Override
    public void setCounter(long counter) {
        this.counter = counter;
    }
}
