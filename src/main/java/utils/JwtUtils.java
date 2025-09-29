package utils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.mangoo.exceptions.MangooJwtException;
import io.mangoo.utils.CommonUtils;

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.*;

public final class JwtUtils {
    private static final Set<String> RESERVED = Set.of("iss", "aud", "sub", "iat", "nbf", "exp", "jti");

    private JwtUtils() {}

    public static String createJwt(JwtData jwtData) throws MangooJwtException {
        try {
            var now = Instant.now();
            var claimsBuilder = new JWTClaimsSet.Builder()
                    .issuer(jwtData.issuer())
                    .audience(jwtData.audience())
                    .subject(jwtData.subject())
                    .issueTime(Date.from(now))
                    .notBeforeTime(Date.from(now.minusSeconds(30)))
                    .expirationTime(Date.from(now.plusSeconds(jwtData.ttlSeconds())))
                    .jwtID(jwtData.jwtID() != null ? jwtData.jwtID() : CommonUtils.uuidV6());

            if (jwtData.claims() != null && !jwtData.claims().isEmpty()) {
                for (Map.Entry<String, String> entry : jwtData.claims().entrySet()) {
                    String key = Objects.requireNonNull(entry.getKey(), "extra claim key must not be null");
                    if (RESERVED.contains(key)) {
                        throw new MangooJwtException("Extra claim '" + key + "' conflicts with a reserved claim");
                    }
                    claimsBuilder.claim(key, entry.getValue());
                }
            }

            JWTClaimsSet claimsSet = claimsBuilder.build();

            var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .type(JOSEObjectType.JWT)
                    .build();

            var signedJWT = new SignedJWT(jwsHeader, claimsSet);
            var signer = new RSASSASigner(jwtData.signingKey());
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new MangooJwtException(e);
        }
    }

    public static KeyPair generateRSAKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        return gen.generateKeyPair();
    }

    public static String toBase64(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
    public static RSAPrivateKey fromBase64Private(String base64) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(base64);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) kf.generatePrivate(spec);
    }

    public static RSAPublicKey fromBase64Public(String base64) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(base64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(spec);
    }

    public static JwtData jwtData() {
        return new JwtData(null, null, null, null, null, 0L, Map.of(), null);
    }

    public record JwtData(
            RSAPrivateKey signingKey,
            RSAPublicKey verificationKey,
            String issuer,
            String audience,
            String subject,
            long ttlSeconds,
            Map<String, String> claims,
            String jwtID
    ) {
        public static JwtData create() {
            return new JwtData(null, null, null, null, null, 0L, Map.of(), null);
        }

        public JwtData withSigningKey(RSAPrivateKey signingKey) {
            return new JwtData(signingKey, verificationKey, issuer, audience, subject, ttlSeconds, claims, jwtID);
        }

        public JwtData withVerificationKey(RSAPublicKey verificationKey) {
            return new JwtData(signingKey, verificationKey, issuer, audience, subject, ttlSeconds, claims, jwtID);
        }

        public JwtData withIssuer(String issuer) {
            return new JwtData(signingKey, verificationKey, issuer, audience, subject, ttlSeconds, claims, jwtID);
        }

        public JwtData withAudience(String audience) {
            return new JwtData(signingKey, verificationKey, issuer, audience, subject, ttlSeconds, claims, jwtID);
        }

        public JwtData withSubject(String subject) {
            return new JwtData(signingKey, verificationKey, issuer, audience, subject, ttlSeconds, claims, jwtID);
        }

        public JwtData withTtlSeconds(long ttlSeconds) {
            return new JwtData(signingKey, verificationKey, issuer, audience, subject, ttlSeconds, claims, jwtID);
        }

        public JwtData withClaims(Map<String, String> claims) {
            return new JwtData(signingKey, verificationKey, issuer, audience, subject, ttlSeconds, claims, jwtID);
        }

        public JwtData withJwtID(String jwtID) {
            return new JwtData(signingKey, verificationKey, issuer, audience, subject, ttlSeconds, claims, jwtID);
        }
    }
}
