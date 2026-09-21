package karakal.utils;

import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import utils.JwtUtils;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static org.junit.jupiter.api.Assertions.*;

class JwtKeyIdTests {

    @Test
    void thumbprintIsDeterministicAndKeySpecific() throws Exception {
        KeyPair one = JwtUtils.generateRSAKeyPair();
        KeyPair two = JwtUtils.generateRSAKeyPair();

        String first = JwtUtils.thumbprint((RSAPublicKey) one.getPublic());

        assertEquals(first, JwtUtils.thumbprint((RSAPublicKey) one.getPublic()),
                "the key id must be stable, it is derived from the key itself");
        assertNotEquals(first, JwtUtils.thumbprint((RSAPublicKey) two.getPublic()));
    }

    @Test
    void thumbprintSurvivesTheBase64RoundTrip() throws Exception {
        KeyPair keyPair = JwtUtils.generateRSAKeyPair();
        RSAPublicKey restored = JwtUtils.fromBase64Public(JwtUtils.toBase64(keyPair.getPublic()));

        assertEquals(JwtUtils.thumbprint((RSAPublicKey) keyPair.getPublic()), JwtUtils.thumbprint(restored));
    }

    @Test
    void createJwtWritesTheKeyIdIntoTheHeader() throws Exception {
        KeyPair keyPair = JwtUtils.generateRSAKeyPair();
        String kid = JwtUtils.thumbprint((RSAPublicKey) keyPair.getPublic());

        var jwtData = JwtUtils.jwtData()
                .withIssuer("https://karakal.example")
                .withAudience("karakal.example")
                .withSubject("user@example.com")
                .withTtlSeconds(600)
                .withKeyId(kid)
                .withSigningKey((RSAPrivateKey) keyPair.getPrivate());

        SignedJWT jwt = SignedJWT.parse(JwtUtils.createJwt(jwtData));

        assertEquals(kid, jwt.getHeader().getKeyID());
    }

    @Test
    void createJwtOmitsTheKeyIdWhenNotSet() throws Exception {
        KeyPair keyPair = JwtUtils.generateRSAKeyPair();

        var jwtData = JwtUtils.jwtData()
                .withIssuer("https://karakal.example")
                .withAudience("karakal.example")
                .withSubject("user@example.com")
                .withTtlSeconds(600)
                .withSigningKey((RSAPrivateKey) keyPair.getPrivate());

        SignedJWT jwt = SignedJWT.parse(JwtUtils.createJwt(jwtData));

        assertNull(jwt.getHeader().getKeyID(), "tokens issued before the change must stay parseable");
    }
}
