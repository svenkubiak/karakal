package controllers;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.data.*;
import com.webauthn4j.data.attestation.AttestationObject;
import com.webauthn4j.data.attestation.authenticator.*;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.data.extension.authenticator.AuthenticationExtensionAuthenticatorOutput;
import com.webauthn4j.server.ServerProperty;
import constants.Const;
import io.mangoo.core.Config;
import io.mangoo.routing.Response;
import io.mangoo.routing.bindings.Request;
import io.mangoo.utils.CommonUtils;
import io.mangoo.utils.JsonUtils;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import models.App;
import models.Credential;
import models.User;
import org.apache.fury.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import services.DataService;
import utils.AppUtils;
import utils.CacheUtils;
import utils.JwtUtils;

import java.nio.charset.StandardCharsets;
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

    public Response jwks(@NotBlank @Pattern(regexp = Const.APP_ID_REGEX) String appId) {
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

    public Response registerInit(@NotNull @NotEmpty Map<String, String> data, Request request) {
        App app = dataService.findApp(data.get("appId"));

        if (app != null && app.isRegistration() && dataService.isValidNonce(app, request)) {
            String username = data.get("username");
            User user = dataService.findUser(username, app.getAppId());

            if (user == null && AppUtils.isAllowedDomain(app, username) && app.isRegistration())  {
                DefaultChallenge challenge = new DefaultChallenge();
                CacheUtils.cacheRegisterChallenge(username, challenge.getValue());

                AuthenticatorSelectionCriteria authenticatorSelectionCriteria =
                        new AuthenticatorSelectionCriteria(
                                AuthenticatorAttachment.CROSS_PLATFORM,
                                Boolean.FALSE,
                                ResidentKeyRequirement.PREFERRED,
                                UserVerificationRequirement.REQUIRED
                        );

                PublicKeyCredentialCreationOptions options = new PublicKeyCredentialCreationOptions(
                        new PublicKeyCredentialRpEntity(AppUtils.getDomain(app.getUrl()), AppUtils.getDomain(app.getUrl())),
                        new PublicKeyCredentialUserEntity(CommonUtils.uuidV6().getBytes(StandardCharsets.UTF_8), username, ""),
                        challenge,
                        List.of(new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256)),
                        60000L,
                        Collections.emptyList(),
                        authenticatorSelectionCriteria,
                        AttestationConveyancePreference.NONE,
                        null
                );

                return Response.ok()
                        .header("Access-Control-Allow-Origin", app.getUrl())
                        .bodyJson(options);
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
                        .header("Vary", "Origin")
                        .header("Access-Control-Allow-Origin", app.getUrl())
                        .header("Access-Control-Allow-Methods", "POST, OPTIONS")
                        .header("Access-Control-Allow-Headers", "Content-Type, karakal-username, karakal-app-id, karakal-nonce");
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

            if (app != null &&
                app.isRegistration() &&
                user == null &&
                StringUtils.isNotBlank(body) &&
                dataService.isValidNonce(app, request)) {

                WebAuthnManager webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager();

                byte[] challenge = CacheUtils.getRegisterChallenge(username);
                if (challenge == null) {
                    return Response.badRequest();
                }

                ServerProperty serverProperty = ServerProperty
                        .builder()
                        .origin(new Origin(app.getUrl()))
                        .rpId(AppUtils.getDomain(app.getUrl()))
                        .challenge(new DefaultChallenge(challenge))
                        .build();

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

                    user = new User(username);
                    user.setAppId(app.getAppId());
                    user.setCredentialId(credentialId);
                    user.setPublicKeyCose(publicKeyCose);
                    user.setSignCount(signCount);
                    user.setAttestedCredentialData(JsonUtils.toJson(attestedCredentialData));
                    user.setCoseKey(JsonUtils.toJson(attestedCredentialData.getCOSEKey()));

                    dataService.save(user);
                    CacheUtils.removeRegisterChallenge(username);

                    return Response.ok()
                            .header("Access-Control-Allow-Origin", app.getUrl());
                }
            } else {
                return Response.notFound();
            }
        } catch (Exception e) {
            LOG.error("Failed to complete registration", e);
            return Response.badRequest();
        }

        return Response.badRequest();
    }

    public Response loginInit(@NotNull @NotEmpty Map<String, String> data, Request request) {
        String username = data.get("username");

        App app = dataService.findApp(data.get("appId"));
        if (app == null) {
            return Response.notFound();
        }

        User user = dataService.findUser(username, app.getAppId());
        if (user != null && AppUtils.isAllowedDomain(app, username) && dataService.isValidNonce(app, request)) {
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
            response.put("userVerification", "required");

            return Response.ok()
                    .header("Access-Control-Allow-Origin", app.getUrl())
                    .bodyJson(response);
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
        if (user != null && StringUtils.isNotBlank(body) && dataService.isValidNonce(app, request)) {
            WebAuthnManager manager = WebAuthnManager.createNonStrictWebAuthnManager();
            AuthenticationData authData = manager.parseAuthenticationResponseJSON(body);

            byte[] challenge = CacheUtils.getLoginChallenge(user.getUsername());
            if (challenge == null) {
                return Response.badRequest();
            }

            ServerProperty serverProperty = ServerProperty
                    .builder()
                    .origin(new Origin(app.getUrl()))
                    .rpId(AppUtils.getDomain(app.getUrl()))
                    .challenge(new DefaultChallenge(challenge))
                    .build();

            Map coseMap = JsonUtils.getMapper().readValue(user.getCoseKey(), Map.class);
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
                    flatMap.get("aaguid.value").getBytes(StandardCharsets.UTF_8),
                    coseKey
            );

            Credential credential = new Credential(user.getCredentialId(), user.getPublicKeyCose(), user.getSignCount(), attestedCredentialData);
            AuthenticationParameters params = new AuthenticationParameters(
                    serverProperty,
                    credential,
                    List.of(user.getCredentialId()),
                    true
            );

            var jwtData = JwtUtils.jwtData()
                    .withJwtID(CommonUtils.randomString(32))
                    .withAudience(app.getAudience())
                    .withIssuer(config.getString("karakal.url"))
                    .withSubject(user.getUsername())
                    .withSigningKey(JwtUtils.fromBase64Private(app.getPrivateKey()))
                    .withTtlSeconds(app.getTtl());

            var jwt = JwtUtils.createJwt(jwtData);

            try {
                manager.verify(authData, params);
                CacheUtils.removeLoginChallenge(user.getUsername());

                if (app.getName().equalsIgnoreCase(Const.DASHBOARD) && app.isRegistration()) {
                    app.setRegistration(false);
                    dataService.save(app);
                }

                AuthenticatorData<AuthenticationExtensionAuthenticatorOutput> authenticatorData = authData.getAuthenticatorData();
                if (authenticatorData != null) {
                    user.setSignCount(authenticatorData.getSignCount());
                    dataService.save(user);
                }

                return Response.ok()
                        .header("Access-Control-Allow-Origin", app.getUrl())
                        .bodyJson(Map.of(
                                "jwt", jwt,
                                "name", Const.COOKIE_NAME,
                                "maxAge", app.getTtl(),
                                "redirect", app.getRedirect()));

            } catch (Exception e) {
                LOG.error("Failed to complete authentication", e);
                return Response.badRequest();
            }
        }

        return Response.badRequest();
    }
}