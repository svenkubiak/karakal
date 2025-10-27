package models;

import com.webauthn4j.credential.CredentialRecord;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.client.CollectedClientData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Credential implements CredentialRecord {
    private final byte[] credentialId;
    private final byte[] credentialPublicKey;
    private final long signCount;
    private final AttestedCredentialData attestedCredentialData;

    public Credential(byte[] credentialId, byte[] credentialPublicKey, long signCount, AttestedCredentialData attestedCredentialData) {
        this.credentialId = credentialId;
        this.credentialPublicKey = credentialPublicKey;
        this.signCount = signCount;
        this.attestedCredentialData = attestedCredentialData;
    }

    @Nullable
    @Override
    public Boolean isUvInitialized() {
        return null;
    }

    @Override
    public void setUvInitialized(boolean b) {}

    @Nullable
    @Override
    public Boolean isBackupEligible() {
        return null;
    }

    @Override
    public void setBackupEligible(boolean b) {}

    @Nullable
    @Override
    public Boolean isBackedUp() {
        return null;
    }

    @Override
    public void setBackedUp(boolean b) {    }

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
        return 0;
    }

    @Override
    public void setCounter(long l) {}
}
