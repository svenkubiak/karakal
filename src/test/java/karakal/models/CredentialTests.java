package karakal.models;

import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.EC2COSEKey;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import models.Credential;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;

import static org.junit.jupiter.api.Assertions.*;

class CredentialTests {

    private static AttestedCredentialData attestedCredentialData() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = generator.generateKeyPair();

        return new AttestedCredentialData(
                AAGUID.ZERO,
                "credential-id".getBytes(),
                EC2COSEKey.create((ECPublicKey) keyPair.getPublic(), COSEAlgorithmIdentifier.ES256));
    }

    private static Credential credential(long signCount) throws Exception {
        return new Credential("credential-id".getBytes(), new byte[]{1, 2, 3}, signCount, attestedCredentialData());
    }

    @Test
    void getCounter_returnsPersistedSignCount() throws Exception {
        assertEquals(42L, credential(42L).getCounter(),
                "a constant counter of 0 disables the cloned authenticator detection of webauthn4j");
    }

    @Test
    void setCounter_updatesTheCounter() throws Exception {
        Credential credential = credential(1L);

        credential.setCounter(7L);

        assertEquals(7L, credential.getCounter());
    }

    @Test
    void flagsDefaultToNull_forTheLegacyConstructor() throws Exception {
        Credential credential = credential(0L);

        assertNull(credential.isUvInitialized());
        assertNull(credential.isBackupEligible());
        assertNull(credential.isBackedUp(),
                "unknown backup eligibility must stay null so webauthn4j skips the BE check for legacy credentials");
    }

    @Test
    void flagsAreReturnedAsProvided() throws Exception {
        Credential credential = new Credential(
                "credential-id".getBytes(), new byte[]{1}, 3L, attestedCredentialData(), true, false, false);

        assertEquals(Boolean.TRUE, credential.isUvInitialized());
        assertEquals(Boolean.FALSE, credential.isBackupEligible());
        assertEquals(Boolean.FALSE, credential.isBackedUp());
    }

    @Test
    void settersUpdateTheFlags() throws Exception {
        Credential credential = credential(0L);

        credential.setUvInitialized(true);
        credential.setBackupEligible(true);
        credential.setBackedUp(true);

        assertEquals(Boolean.TRUE, credential.isUvInitialized());
        assertEquals(Boolean.TRUE, credential.isBackupEligible());
        assertEquals(Boolean.TRUE, credential.isBackedUp());
    }

    @Test
    void attestedCredentialDataIsReturned() throws Exception {
        AttestedCredentialData data = attestedCredentialData();
        Credential credential = new Credential("id".getBytes(), new byte[]{1}, 0L, data);

        assertSame(data, credential.getAttestedCredentialData());
    }
}
