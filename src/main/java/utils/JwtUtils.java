package utils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
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

            var jwsHeaderBuilder = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .type(JOSEObjectType.JWT);

            if (jwtData.keyId() != null) {
                jwsHeaderBuilder.keyID(jwtData.keyId());
            }

            var jwsHeader = jwsHeaderBuilder.build();

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

    /**
     * Verifies the signature and the standard claims of a given JWT and returns its claims.
     *
     * <p>Checks the signature against the given public key, the key id (if the token carries one),
     * {@code exp}, {@code nbf}, {@code iss} and {@code aud}.</p>
     *
     * @throws MangooJwtException if the token is invalid in any way
     */
    public static JWTClaimsSet verify(String jwt, RSAPublicKey publicKey, String issuer, String audience)
            throws MangooJwtException {
        Objects.requireNonNull(jwt, "jwt can not be null");
        Objects.requireNonNull(publicKey, "publicKey can not be null");

        try {
            SignedJWT signedJWT = SignedJWT.parse(jwt);

            String keyId = signedJWT.getHeader().getKeyID();
            if (keyId != null && !keyId.isBlank() && !keyId.equals(thumbprint(publicKey))) {
                throw new MangooJwtException("Token was signed with an unknown key");
            }

            if (!signedJWT.verify(new RSASSAVerifier(publicKey))) {
                throw new MangooJwtException("Invalid signature");
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            var now = Instant.now();

            Date expirationTime = claims.getExpirationTime();
            if (expirationTime == null) {
                throw new MangooJwtException("Token does not have an expiration time");
            }
            if (now.isAfter(expirationTime.toInstant())) {
                throw new MangooJwtException("Token has expired");
            }

            Date notBefore = claims.getNotBeforeTime();
            if (notBefore != null && now.isBefore(notBefore.toInstant())) {
                throw new MangooJwtException("Token is not valid yet");
            }

            if (!Objects.equals(claims.getIssuer(), issuer)) {
                throw new MangooJwtException("Invalid issuer");
            }
            if (claims.getAudience() == null || !claims.getAudience().contains(audience)) {
                throw new MangooJwtException("Invalid audience");
            }

            return claims;
        } catch (MangooJwtException e) {
            throw e;
        } catch (Exception e) {
            throw new MangooJwtException(e);
        }
    }

    /**
     * Returns the RFC 7638 thumbprint of a public key, used as the key id of the corresponding
     * application. It is derived from the key itself and therefore needs no separate storage.
     */
    public static String thumbprint(RSAPublicKey publicKey) throws JOSEException {
        return new RSAKey.Builder(publicKey).build().computeThumbprint().toString();
    }

    public static JwtData jwtData() {
        return new JwtData(null, null, null, null, null, 0L, Map.of(), null, null);
    }

    public record JwtData(
            RSAPrivateKey signingKey,
            RSAPublicKey verificationKey,
            String issuer,
            String audience,
            String subject,
            long ttlSeconds,
            Map<String, String> claims,
            String jwtID,
            String keyId
    ) {
        public static JwtData create() {
            return new JwtData(null, null, null, null, null, 0L, Map.of(), null, null);
        }

        public JwtData withSigningKey(RSAPrivateKey signingKey) {
            return new JwtData(signingKey, verificationKey, issuer, audience, subject, ttlSeconds, claims, jwtID, keyId);
        }

        public JwtData withVerificationKey(RSAPublicKey verificationKey) {
            return new JwtData(signingKey, verificationKey, issuer, audience, subject, ttlSeconds, claims, jwtID, keyId);
        }

        public JwtData withIssuer(String issuer) {
            return new JwtData(signingKey, verificationKey, issuer, audience, subject, ttlSeconds, claims, jwtID, keyId);
        }

        public JwtData withAudience(String audience) {
            return new JwtData(signingKey, verificationKey, issuer, audience, subject, ttlSeconds, claims, jwtID, keyId);
        }

        public JwtData withSubject(String subject) {
            return new JwtData(signingKey, verificationKey, issuer, audience, subject, ttlSeconds, claims, jwtID, keyId);
        }

        public JwtData withTtlSeconds(long ttlSeconds) {
            return new JwtData(signingKey, verificationKey, issuer, audience, subject, ttlSeconds, claims, jwtID, keyId);
        }

        public JwtData withClaims(Map<String, String> claims) {
            return new JwtData(signingKey, verificationKey, issuer, audience, subject, ttlSeconds, claims, jwtID, keyId);
        }

        public JwtData withJwtID(String jwtID) {
            return new JwtData(signingKey, verificationKey, issuer, audience, subject, ttlSeconds, claims, jwtID, keyId);
        }

        public JwtData withKeyId(String keyId) {
            return new JwtData(signingKey, verificationKey, issuer, audience, subject, ttlSeconds, claims, jwtID, keyId);
        }
    }
}
