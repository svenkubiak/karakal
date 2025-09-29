package controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.data.*;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.Curve;
import com.webauthn4j.data.attestation.authenticator.EC2COSEKey;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import constants.Const;
import io.mangoo.cache.Cache;
import io.mangoo.cache.CacheProvider;
import io.mangoo.core.Config;
import io.mangoo.routing.Response;
import io.mangoo.routing.bindings.Request;
import io.mangoo.utils.CommonUtils;
import io.mangoo.utils.JsonUtils;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.CookieImpl;
import jakarta.inject.Inject;
import models.App;
import models.Credential;
import models.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import services.DataService;
import utils.JwtUtils;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class PasskeyController {
    private static final Logger LOG = LogManager.getLogger(PasskeyController.class);
    private final DataService dataService;
    private final Cache cache;
    private final Config config;
    
    @Inject
    public PasskeyController(DataService dataService, CacheProvider cacheProvider, Config config) {
        this.dataService = Objects.requireNonNull(dataService, "dataService can not be null");
        this.cache = Objects.requireNonNull(cacheProvider.getCache("karakal"), "cache can not be null");
        this.config = Objects.requireNonNull(config, "config can not be null");
    }

    public Response wellKnown(String appId) {
        App app = dataService.findApp(appId);

        List<Map<String, String>> keys = new ArrayList<>();
        if (app != null) {
            try {
                byte[] keyBytes = Base64.getDecoder().decode(app.getPublicKey());
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(spec);

                String n = Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.getModulus().toByteArray());
                String e = Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.getPublicExponent().toByteArray());

                keys.add(Map.of("kry", "RSA",  "use", "sig", "alg", "RS256", "n", n, "e", e));
                return Response.ok().bodyJson(keys);
            } catch (Exception e) {
                LOG.error("Failed to retrieve jwks data", e);
                return Response.internalServerError();
            }
        }

        return Response.notFound();
    }

    public Response registerInit(Map<String, String> data) {
        if (data != null && !data.isEmpty()) {
            String applicationId = data.get("applicationId");
            App app = dataService.findApp(applicationId);

            if (app != null && app.isRegistration()) {
                String username = data.get("username");
                User user = dataService.findUser(data.get("username"), app.getAppId());

                if (user == null) {
                    DefaultChallenge challenge = new DefaultChallenge();
                    cacheRegisterChallenge(username, challenge);

                    PublicKeyCredentialCreationOptions options = new PublicKeyCredentialCreationOptions(
                            new PublicKeyCredentialRpEntity(app.getDomain(), app.getDomain()),
                            new PublicKeyCredentialUserEntity(CommonUtils.uuidV6().getBytes(), username, ""),
                            challenge,
                            Arrays.asList(
                                    new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256),
                                    new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.RS256)
                            ),
                            60000L,
                            Collections.emptyList(),
                            null,
                            AttestationConveyancePreference.DIRECT,
                            null
                    );

                    return Response.ok().bodyJson(options);
                }
            }
        }

        return Response.badRequest();
    }

    private void cacheRegisterChallenge(String username, DefaultChallenge challenge) {
        cache.put(username + "-register-challenge", challenge.getValue());
    }

    public Response registerComplete(Request request) {
        try {
            String username = request.getHeader("x-username");
            User user = dataService.findUser(username);
            App app = dataService.findApp(request.getHeader("x-application-id"));

            if (user == null) {
                WebAuthnManager webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager();
                RegistrationData registrationData = webAuthnManager.parseRegistrationResponseJSON(request.getBody());

                byte[] challenge = getRegisterChallenge(username);

                ServerProperty serverProperty = new ServerProperty(
                        new Origin("https://" + app.getDomain()),
                        app.getDomain(),
                        new DefaultChallenge(challenge)
                );

                RegistrationParameters registrationParameters = new RegistrationParameters(
                        serverProperty,
                        null,
                        false,
                        true
                );

                webAuthnManager.verify(registrationData, registrationParameters);

                AttestedCredentialData attestedCredentialData = registrationData
                        .getAttestationObject()
                        .getAuthenticatorData()
                        .getAttestedCredentialData();

                byte[] credentialId = attestedCredentialData.getCredentialId();
                byte[] publicKeyCose = attestedCredentialData.getCOSEKey().getPublicKey().getEncoded();
                long signCount = registrationData
                        .getAttestationObject()
                        .getAuthenticatorData()
                        .getSignCount();

                user = new User(app.getAppId(), username, credentialId, publicKeyCose, signCount, JsonUtils.toJson(attestedCredentialData), JsonUtils.toJson(attestedCredentialData.getCOSEKey()));
                dataService.save(user);
                removeRegisterChallenge(username);

                return Response.ok();
            }
        } catch(Exception e) {
            LOG.error("Failed to complete registration", e);

            return Response.badRequest();
        }

        return Response.badRequest();
    }

    private void removeRegisterChallenge(String username) {
        cache.remove(username + "-register-challenge");
    }

    private byte[] getRegisterChallenge(String username) {
        return cache.get(username + "-register-challenge");
    }

    public Response loginInit(Map<String, String> data) {
        String username = data.get("username");

        App app = dataService.findApp(data.get("applicationId"));
        User user = dataService.findUser(username, app.getAppId());

        if (user != null) {
            Map<String, Object> allowCredential = new HashMap<>();
            allowCredential.put("type", "public-key");
            allowCredential.put("id", Base64.getUrlEncoder().withoutPadding().encodeToString(user.getCredentialId()));

            DefaultChallenge challenge = new DefaultChallenge();
            cacheLoginChallenge(username, challenge);

            Map<String, Object> response = new HashMap<>();
            response.put("challenge", Base64.getUrlEncoder().withoutPadding().encodeToString(challenge.getValue()));
            response.put("timeout", 60000);
            response.put("rpId", app.getDomain());
            response.put("allowCredentials", List.of(allowCredential));
            response.put("userVerification", "preferred");

            return Response.ok().bodyJson(response);
        }

        return Response.badRequest();
    }

    private void cacheLoginChallenge(String username, DefaultChallenge challenge) {
        cache.put(username + "-login-challenge", challenge.getValue());
    }

    public Response loginComplete(Request request) throws Exception {
        App app = dataService.findApp(request.getHeader("x-application-id"));
        User user = dataService.findUser(request.getHeader("x-username"), request.getHeader("x-application-id"));
        if (user != null && app != null) {
            WebAuthnManager manager = WebAuthnManager.createNonStrictWebAuthnManager();
            AuthenticationData authData = manager.parseAuthenticationResponseJSON(request.getBody());
            byte[] challenge = getLoginChallenge(user.getUsername());
            ServerProperty serverProperty = new ServerProperty(
                    new Origin("https://" + app.getDomain()),
                    app.getDomain(),
                    new DefaultChallenge(challenge)
            );

            Map<String, Object> coseMap = null;
            try {
                coseMap = JsonUtils.getMapper().readValue(user.getCoseKey(), Map.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            int keyType = Integer.parseInt(coseMap.get("1").toString());
            int algorithm = Integer.parseInt(coseMap.get("3").toString());
            int crv = Integer.parseInt(coseMap.get("-1").toString());
            byte[] x = Base64.getDecoder().decode((String) coseMap.get("-2"));
            byte[] y = Base64.getDecoder().decode((String) coseMap.get("-3"));

            EC2COSEKey coseKey = new EC2COSEKey(
                    null,
                    COSEAlgorithmIdentifier.ES256,
                    null,
                    Curve.SECP256R1,
                    x,
                    y
            );

            Map<String, String> flatMap = JsonUtils.toFlatMap(user.getAttestedCredentialData());
            AttestedCredentialData attestedCredentialData = new AttestedCredentialData(
                    new AAGUID(Base64.getDecoder().decode(flatMap.get("aaguid.bytes"))),
                    flatMap.get("aaguid.value").getBytes(),
                    coseKey
            );

            Credential c = new Credential(user.getCredentialId(), user.getPublicKeyCose(), user.getSignCount(), attestedCredentialData);
            AuthenticationParameters params = new AuthenticationParameters(
                    serverProperty,
                    c,
                    false,
                    true
            );

            var jwtData = JwtUtils.jwtData()
                    .withJwtID(CommonUtils.randomString(32))
                    .withAudience(app.getAudience())
                    .withIssuer(app.getDomain())
                    .withSubject(user.getUsername())
                    .withSigningKey(JwtUtils.fromBase64Private(app.getPrivateKey()))
                    .withTtlSeconds(Const.COOKIE_MAX_AGE);

            var jwt = JwtUtils.createJwt(jwtData);

            Cookie cookie = new CookieImpl(Const.COOKIE_NAME)
                    .setDomain(app.getDomain())
                    .setExpires(new Date((new Date().getTime() + Const.COOKIE_MAX_AGE * 1000)))
                    .setSecure(true)
                    .setHttpOnly(true)
                    .setValue(jwt)
                    .setPath("/");

            try {
                manager.verify(authData, params);
                removeLoginChallenge(user.getUsername());
                return Response.ok().header("x-login-redirect", app.getRedirect()).cookie(cookie);
            } catch (Exception e) {
                LOG.error("Failed to complete authentication", e);
                return Response.badRequest();
            }
        }

        return Response.badRequest();
    }

    private void removeLoginChallenge(String username) {
        cache.remove(username + "-login-challenge");
    }

    private byte[] getLoginChallenge(String username) {
        return cache.get(username + "-login-challenge");
    }
}