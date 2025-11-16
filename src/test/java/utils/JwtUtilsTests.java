package utils;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.mangoo.exceptions.MangooJwtException;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTests {
    @Test
    void generateRSAKeyPair_producesUsableKeys() throws Exception {
        KeyPair pair = JwtUtils.generateRSAKeyPair();
        assertNotNull(pair.getPrivate());
        assertNotNull(pair.getPublic());
        assertTrue(pair.getPrivate() instanceof RSAPrivateKey);
        assertTrue(pair.getPublic() instanceof RSAPublicKey);
        assertEquals(2048, ((RSAPublicKey) pair.getPublic()).getModulus().bitLength());
    }

    @Test
    void base64_roundTrip_privateAndPublic() throws Exception {
        KeyPair pair = JwtUtils.generateRSAKeyPair();
        String privB64 = JwtUtils.toBase64(pair.getPrivate());
        String pubB64 = JwtUtils.toBase64(pair.getPublic());

        RSAPrivateKey priv = JwtUtils.fromBase64Private(privB64);
        RSAPublicKey pub = JwtUtils.fromBase64Public(pubB64);

        assertEquals(((RSAPrivateKey) pair.getPrivate()).getModulus(), priv.getModulus());
        assertEquals(((RSAPublicKey) pair.getPublic()).getModulus(), pub.getModulus());
    }

    @Test
    void createJwt_containsExpectedClaimsAndHeader() throws Exception {
        KeyPair pair = JwtUtils.generateRSAKeyPair();
        var jwtData = JwtUtils.JwtData.create()
                .withSigningKey((RSAPrivateKey) pair.getPrivate())
                .withIssuer("issuer-1")
                .withAudience("aud-1")
                .withSubject("sub-1")
                .withTtlSeconds(120)
                .withClaims(Map.of("role", "admin", "env", "test"));

        String jwt = JwtUtils.createJwt(jwtData);
        SignedJWT signedJWT = SignedJWT.parse(jwt);

        JWSHeader header = signedJWT.getHeader();
        assertEquals(JWSAlgorithm.RS256, header.getAlgorithm());
        assertEquals("JWT", header.getType().getType());

        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        assertEquals("issuer-1", claims.getIssuer());
        assertEquals("sub-1", claims.getSubject());
        assertTrue(claims.getAudience().contains("aud-1"));
        assertEquals("admin", claims.getStringClaim("role"));
        assertEquals("test", claims.getStringClaim("env"));

        Date now = Date.from(Instant.now());
        assertTrue(claims.getIssueTime().before(now) || claims.getIssueTime().equals(now));
        assertTrue(claims.getExpirationTime().after(now));
        assertNotNull(claims.getJWTID());
    }

    @Test
    void createJwt_throwsOnReservedExtraClaim() throws Exception {
        KeyPair pair = JwtUtils.generateRSAKeyPair();
        var jwtData = JwtUtils.JwtData.create()
                .withSigningKey((RSAPrivateKey) pair.getPrivate())
                .withIssuer("iss")
                .withAudience("aud")
                .withSubject("sub")
                .withTtlSeconds(60)
                .withClaims(Map.of("exp", "should-not-be-allowed"));

        assertThrows(MangooJwtException.class, () -> JwtUtils.createJwt(jwtData));
    }

    @Test
    void jwtData_builderMethods_setAllFields() throws Exception {
        KeyPair pair = JwtUtils.generateRSAKeyPair();
        var base = JwtUtils.jwtData();
        var data = base
                .withSigningKey((RSAPrivateKey) pair.getPrivate())
                .withVerificationKey((RSAPublicKey) pair.getPublic())
                .withIssuer("i")
                .withAudience("a")
                .withSubject("s")
                .withTtlSeconds(10)
                .withJwtID("id-1")
                .withClaims(Map.of("k", "v"));

        assertNotNull(data.signingKey());
        assertNotNull(data.verificationKey());
        assertEquals("i", data.issuer());
        assertEquals("a", data.audience());
        assertEquals("s", data.subject());
        assertEquals(10, data.ttlSeconds());
        assertEquals("id-1", data.jwtID());
        assertEquals("v", data.claims().get("k"));
    }
}
