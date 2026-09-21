package controllers;

import com.webauthn4j.converter.AttestedCredentialDataConverter;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.EC2COSEKey;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import models.User;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PasskeyControllerTests {
    private static final byte[] CREDENTIAL_ID = "the-real-credential-id".getBytes(StandardCharsets.UTF_8);
    private static final AttestedCredentialDataConverter CONVERTER =
            new AttestedCredentialDataConverter(new ObjectConverter());

    private static ECPublicKey publicKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = generator.generateKeyPair();
        return (ECPublicKey) keyPair.getPublic();
    }

    // --- C-02 -------------------------------------------------------------------------------

    @Test
    void registrationParameters_enforceUserVerification() {
        RegistrationParameters parameters = PasskeyController.registrationParameters(serverProperty());

        assertTrue(parameters.isUserVerificationRequired(),
                "without user verification a passkey degrades to a pure possession factor");
        assertTrue(parameters.isUserPresenceRequired());
    }

    @Test
    void registrationParameters_restrictTheSignatureAlgorithm() {
        RegistrationParameters parameters = PasskeyController.registrationParameters(serverProperty());

        List<PublicKeyCredentialParameters> pubKeyCredParams = parameters.getPubKeyCredParams();
        assertNotNull(pubKeyCredParams, "a null value disables the algorithm check in webauthn4j");
        assertEquals(1, pubKeyCredParams.size());
        assertEquals(COSEAlgorithmIdentifier.ES256, pubKeyCredParams.getFirst().getAlg());
        assertEquals(PublicKeyCredentialType.PUBLIC_KEY, pubKeyCredParams.getFirst().getType());
    }

    private static ServerProperty serverProperty() {
        return ServerProperty.builder()
                .origin(new Origin("https://app.example"))
                .rpId("app.example")
                .challenge(new DefaultChallenge("challenge".getBytes(StandardCharsets.UTF_8)))
                .build();
    }

    // --- C-04 -------------------------------------------------------------------------------

    @Test
    void attestedCredentialData_isRestoredFromCanonicalCbor() throws Exception {
        ECPublicKey publicKey = publicKey();
        EC2COSEKey coseKey = EC2COSEKey.create(publicKey, COSEAlgorithmIdentifier.ES256);
        AttestedCredentialData original = new AttestedCredentialData(AAGUID.ZERO, CREDENTIAL_ID, coseKey);

        User user = new User("user@example.com");
        user.setCredentialId(CREDENTIAL_ID);
        user.setAttestedCredentialDataCbor(
                new String(Base64.getEncoder().encode(CONVERTER.convert(original)), StandardCharsets.UTF_8));

        AttestedCredentialData restored = PasskeyController.attestedCredentialData(user);

        assertArrayEquals(CREDENTIAL_ID, restored.getCredentialId());
        assertEquals(publicKey, restored.getCOSEKey().getPublicKey());
    }

    @Test
    void attestedCredentialData_legacyFallbackUsesTheRealCredentialId() throws Exception {
        ECPublicKey publicKey = publicKey();

        User user = new User("user@example.com");
        user.setCredentialId(CREDENTIAL_ID);
        // legacy state: no CBOR, only the X.509 encoded public key written at registration time
        user.setPublicKeyCose(EC2COSEKey.create(publicKey, COSEAlgorithmIdentifier.ES256)
                .getPublicKey().getEncoded());

        AttestedCredentialData restored = PasskeyController.attestedCredentialData(user);

        assertArrayEquals(CREDENTIAL_ID, restored.getCredentialId(),
                "the credential id must never be derived from the AAGUID");
        assertEquals(publicKey, restored.getCOSEKey().getPublicKey());
        assertEquals(AAGUID.ZERO, restored.getAaguid());
    }

    @Test
    void attestedCredentialData_cborAndLegacyFallbackYieldTheSameKey() throws Exception {
        ECPublicKey publicKey = publicKey();
        EC2COSEKey coseKey = EC2COSEKey.create(publicKey, COSEAlgorithmIdentifier.ES256);

        User cborUser = new User("user@example.com");
        cborUser.setCredentialId(CREDENTIAL_ID);
        cborUser.setAttestedCredentialDataCbor(new String(Base64.getEncoder().encode(
                CONVERTER.convert(new AttestedCredentialData(AAGUID.ZERO, CREDENTIAL_ID, coseKey))),
                StandardCharsets.UTF_8));

        User legacyUser = new User("user@example.com");
        legacyUser.setCredentialId(CREDENTIAL_ID);
        legacyUser.setPublicKeyCose(coseKey.getPublicKey().getEncoded());

        EC2COSEKey fromCbor = (EC2COSEKey) PasskeyController.attestedCredentialData(cborUser).getCOSEKey();
        EC2COSEKey fromLegacy = (EC2COSEKey) PasskeyController.attestedCredentialData(legacyUser).getCOSEKey();

        assertArrayEquals(fromCbor.getX(), fromLegacy.getX());
        assertArrayEquals(fromCbor.getY(), fromLegacy.getY());
        assertEquals(fromCbor.getAlgorithm(), fromLegacy.getAlgorithm());
        assertEquals(fromCbor.getCurve(), fromLegacy.getCurve());
    }

    @Test
    void attestedCredentialData_failsLoudlyOnCorruptedData() {
        User user = new User("user@example.com");
        user.setCredentialId(CREDENTIAL_ID);
        user.setAttestedCredentialDataCbor("this-is-not-valid-base64-cbor!!");

        assertThrows(Exception.class, () -> PasskeyController.attestedCredentialData(user),
                "corrupted data must surface as an exception so the caller can answer with 400");
    }
}
