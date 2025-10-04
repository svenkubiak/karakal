package controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.data.*;
import com.webauthn4j.data.attestation.AttestationObject;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.Curve;
import com.webauthn4j.data.attestation.authenticator.EC2COSEKey;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import constants.Const;
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
import org.apache.commons.compress.utils.Lists;
import org.apache.fury.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import services.DataService;
import utils.AppUtils;
import utils.CacheUtils;
import utils.JwtUtils;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

public class PasskeyController {
    private static final Logger LOG = LogManager.getLogger(PasskeyController.class);
    private final DataService dataService;
    private final Config config;

    @Inject
    public PasskeyController(DataService dataService, Config config) {
        this.dataService = Objects.requireNonNull(dataService, "dataService can not be null");
        this.config = Objects.requireNonNull(config, "config can not be null");
    }

    public Response jwks(String appId) {
        App app = dataService.findApp(appId);
        if (app != null) {
            try {
                byte[] keyBytes = CommonUtils.decodeFromBase64(app.getPublicKey());
                RSAPublicKey publicKey = (RSAPublicKey) KeyFactory
                        .getInstance("RSA")
                        .generatePublic(new X509EncodedKeySpec(keyBytes));

                String n = CommonUtils.urlEncodeWithoutPaddingToBase64(publicKey.getModulus().toByteArray());
                String e = CommonUtils.urlEncodeWithoutPaddingToBase64(publicKey.getPublicExponent().toByteArray());


                Map<String, Object> jwk = new HashMap<>();
                jwk.put("use", "sig");
                jwk.put("kty", "RSA");
                jwk.put("e", e);
                jwk.put("n", n);
                jwk.put("alg", "RS256");

                List<Map<String, Object>> keysList = Collections.singletonList(jwk);

                Map<String, Object> jwks = new HashMap<>();
                jwks.put("keys", keysList);

                return Response.ok().bodyJson(jwks);
            } catch (Exception e) {
                LOG.error("Failed to retrieve Jwks data", e);
                return Response.internalServerError();
            }
        }

        return Response.notFound();
    }

    public Response registerInit(Map<String, String> data) {
        if (data != null && !data.isEmpty()) {
            App app = dataService.findApp(data.get("appId"));

            if (app != null && app.isRegistration()) {
                String username = data.get("username");
                User user = dataService.findUser(username, app.getAppId());

                if (user == null && app.isRegistration()) {
                    DefaultChallenge challenge = new DefaultChallenge();
                    CacheUtils.cacheRegisterChallenge(username, challenge.getValue());

                    PublicKeyCredentialCreationOptions options = new PublicKeyCredentialCreationOptions(
                            new PublicKeyCredentialRpEntity(AppUtils.getDomain(app.getUrl()), AppUtils.getDomain(app.getUrl())),
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

                    return Response.ok()
                            .header("Access-Control-Allow-Origin", app.getUrl())
                            .bodyJson(options);
                }
            }
        }

        return Response.badRequest();
    }

    public Response preflight(Request request) {
        String origin = request.getHeader("Origin");
        if (StringUtils.isNotBlank(origin)) {
            App app = dataService.findAppByUrl(origin);
            if (app != null) {
                return Response.ok()
                        .header("Access-Control-Allow-Origin", app.getUrl())
                        .header("Access-Control-Allow-Methods", "POST, OPTIONS")
                        .header("Access-Control-Allow-Headers", "Content-Type, karakal-username, karakal-app-id");
            }
        }

        return Response.badRequest();
    }

    public Response registerComplete(Request request) {
        try {
            User user = null;
            String body = request.getBody();
            String username = request.getHeader("karakal-username");
            App app = dataService.findApp(request.getHeader("karakal-app-id"));
            if (app != null) {
                user = dataService.findUser(username, app.getAppId());
            }

            if (app != null && app.isRegistration() && user == null && StringUtils.isNotBlank(body)) {
                WebAuthnManager webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager();

                byte[] challenge = CacheUtils.getRegisterChallenge(username);
                ServerProperty serverProperty = new ServerProperty(
                        new Origin(app.getUrl()), AppUtils.getDomain(app.getUrl()),
                        new DefaultChallenge(challenge)
                );

                RegistrationParameters registrationParameters = new RegistrationParameters(
                        serverProperty,
                        null,
                        false,
                        true
                );

                RegistrationData registrationData = webAuthnManager.parseRegistrationResponseJSON(body);
                webAuthnManager.verify(registrationData, registrationParameters);

                AttestedCredentialData attestedCredentialData = null;
                AttestationObject attestationObject = registrationData.getAttestationObject();
                if (attestationObject != null) {
                    attestedCredentialData = attestationObject.getAuthenticatorData().getAttestedCredentialData();
                }

                if (attestedCredentialData != null && attestedCredentialData.getCOSEKey().getPublicKey() != null) {
                    byte[] credentialId = attestedCredentialData.getCredentialId();
                    byte[] publicKeyCose = attestedCredentialData.getCOSEKey().getPublicKey().getEncoded();

                    long signCount = registrationData
                            .getAttestationObject()
                            .getAuthenticatorData()
                            .getSignCount();

                    //CHECK THIS!
                    user = new User(username);
                    user.setAppId(app.getAppId());
                    user.setCredentialId(credentialId);
                    user.setPublicKeyCose(publicKeyCose);
                    user.setSignCount(signCount);
                    user.setAttestedCredentialData(JsonUtils.toJson(attestedCredentialData));
                    user.setCoseKey(JsonUtils.toJson(attestedCredentialData.getCOSEKey()));

                    dataService.save(user);
                    CacheUtils.removeRegisterChallenge(username);

                    return Response.ok().header("Access-Control-Allow-Origin", app.getUrl());
                }
            }
        } catch(Exception e) {
            LOG.error("Failed to complete registration", e);
            return Response.badRequest();
        }

        return Response.badRequest();
    }

    public Response loginInit(Map<String, String> data) {
        if (data != null && !data.isEmpty()) {
            String username = data.get("username");

            App app = dataService.findApp(data.get("appId"));
            User user = null;
            if (app != null) {
                user = dataService.findUser(username, app.getAppId());
            }

            if (user != null) {
                Map<String, Object> allowCredential = new HashMap<>();
                allowCredential.put("type", "public-key");
                allowCredential.put("id", CommonUtils.urlEncodeWithoutPaddingToBase64(user.getCredentialId()));

                byte [] challenge =  new DefaultChallenge().getValue();
                CacheUtils.cacheLoginChallenge(username, challenge);

                Map<String, Object> response = new HashMap<>();
                response.put("challenge", CommonUtils.urlEncodeWithoutPaddingToBase64(challenge));
                response.put("timeout", 60000);
                response.put("rpId", AppUtils.getDomain(app.getUrl()));
                response.put("allowCredentials", List.of(allowCredential));
                response.put("userVerification", "preferred");

                return Response.ok().header("Access-Control-Allow-Origin", app.getUrl()).bodyJson(response);
            }
        }

        return Response.badRequest();
    }

    @SuppressWarnings("rawtypes")
    public Response loginComplete(Request request) throws Exception {
        User user = null;
        App app = dataService.findApp(request.getHeader("karakal-app-id"));
        if (app != null) {
            user = dataService.findUser(request.getHeader("karakal-username"), app.getAppId());
        }

        String body = request.getBody();
        if (user != null && StringUtils.isNotBlank(body)) {
            WebAuthnManager manager = WebAuthnManager.createNonStrictWebAuthnManager();
            AuthenticationData authData = manager.parseAuthenticationResponseJSON(body);

            byte[] challenge = CacheUtils.getLoginChallenge(user.getUsername());
            ServerProperty serverProperty = new ServerProperty(
                    new Origin(app.getUrl()), AppUtils.getDomain(app.getUrl()),
                    new DefaultChallenge(challenge)
            );

            Map coseMap = JsonUtils.getMapper().readValue(user.getCoseKey(), Map.class);

            //int keyType = Integer.parseInt(coseMap.get("1").toString());
            //int algorithm = Integer.parseInt(coseMap.get("3").toString());
            //int crv = Integer.parseInt(coseMap.get("-1").toString());
            byte[] x = CommonUtils.decodeFromBase64((String) coseMap.get("-2"));
            byte[] y = CommonUtils.decodeFromBase64((String) coseMap.get("-3"));

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
                    new AAGUID(CommonUtils.decodeFromBase64(flatMap.get("aaguid.bytes"))),
                    flatMap.get("aaguid.value").getBytes(),
                    coseKey
            );

            Credential credential = new Credential(user.getCredentialId(), user.getPublicKeyCose(), user.getSignCount(), attestedCredentialData);
            AuthenticationParameters params = new AuthenticationParameters(
                    serverProperty,
                    credential,
                    List.of(user.getCredentialId()),
                    false
            );

            var jwtData = JwtUtils.jwtData()
                    .withJwtID(CommonUtils.randomString(32))
                    .withAudience(app.getAudience())
                    .withIssuer(config.getString("karakal.url"))
                    .withSubject(user.getUsername())
                    .withSigningKey(JwtUtils.fromBase64Private(app.getPrivateKey()))
                    .withTtlSeconds(Const.COOKIE_MAX_AGE);

            var jwt = JwtUtils.createJwt(jwtData);

            try {
                manager.verify(authData, params);
                CacheUtils.removeLoginChallenge(user.getUsername());

                if (app.getName().equalsIgnoreCase("dashboard") && app.isRegistration()) {
                    app.setRegistration(false);
                    dataService.save(app);
                }

                return Response.ok()
                        .header("Access-Control-Allow-Origin", app.getUrl())
                        .bodyJson(Map.of("jwt", jwt,
                                "name", Const.COOKIE_NAME,
                                "maxAge",
                                Const.COOKIE_MAX_AGE * 1000,
                                "redirect", app.getRedirect()));

            } catch (Exception e) {
                LOG.error("Failed to complete authentication", e);
                return Response.badRequest();
            }
        }

        return Response.badRequest();
    }
}