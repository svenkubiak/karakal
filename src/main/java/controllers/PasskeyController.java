package controllers;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.AttestedCredentialDataConverter;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.*;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.AuthenticatorData;
import com.webauthn4j.data.attestation.authenticator.EC2COSEKey;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.data.extension.authenticator.RegistrationExtensionAuthenticatorOutput;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import services.DataService;
import utils.AppUtils;
import utils.CacheUtils;
import utils.JwtUtils;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PasskeyController {
    private static final Logger LOG = LogManager.getLogger(PasskeyController.class);
    private static final List<PublicKeyCredentialParameters> PUB_KEY_CRED_PARAMS =
            List.of(new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256));
    private static final AttestedCredentialDataConverter ATTESTED_CREDENTIAL_DATA_CONVERTER =
            new AttestedCredentialDataConverter(new ObjectConverter());
    private final DataService dataService;
    private final Config config;

    @Inject
    public PasskeyController(DataService dataService, Config config) {
        this.dataService = Objects.requireNonNull(dataService, "dataService can not be null");
        this.config = Objects.requireNonNull(config, "config can not be null");
    }

    /**
     * Adds the CORS and caching headers of an application to a given response.
     *
     * <p>{@code Vary: Origin} is required because the value of {@code Access-Control-Allow-Origin}
     * differs per application, {@code Cache-Control: no-store} because these responses carry
     * challenges and tokens.</p>
     */
    private static Response cors(Response response, App app) {
        return response
                .header("Access-Control-Allow-Origin", app.getUrl())
                .header("Vary", "Origin")
                .header("Cache-Control", Const.NO_STORE);
    }

    /**
     * Checks that a request originates from the application it claims to belong to.
     *
     * <p>The application is derived from the request body respectively a request header, both of
     * which are under the control of the caller. A request without an {@code Origin} header is
     * accepted, as it can not be a browser based cross origin request in the first place.</p>
     */
    private static boolean isAllowedOrigin(App app, Request request) {
        String origin = request.getHeader("Origin");
        if (StringUtils.isBlank(origin)) {
            return true;
        }

        String normalized = AppUtils.normalizeOrigin(origin);

        return StringUtils.isNotBlank(normalized) && normalized.equals(AppUtils.normalizeOrigin(app.getUrl()));
    }

    static RegistrationParameters registrationParameters(ServerProperty serverProperty) {
        return new RegistrationParameters(
                serverProperty,
                PUB_KEY_CRED_PARAMS,
                true,   // userVerificationRequired
                true    // userPresenceRequired
        );
    }

    static AttestedCredentialData attestedCredentialData(User user) throws GeneralSecurityException {
        String cbor = user.getAttestedCredentialDataCbor();
        if (StringUtils.isNotBlank(cbor)) {
            return ATTESTED_CREDENTIAL_DATA_CONVERTER.convert(CommonUtils.decodeFromBase64(cbor));
        }

        var publicKey = (ECPublicKey) KeyFactory
                .getInstance("EC")
                .generatePublic(new X509EncodedKeySpec(user.getPublicKeyCose()));

        return new AttestedCredentialData(
                AAGUID.ZERO,
                user.getCredentialId(),
                EC2COSEKey.create(publicKey, COSEAlgorithmIdentifier.ES256));
    }

    private static String toBase64(byte[] bytes) {
        return new String(CommonUtils.encodeToBase64(bytes), StandardCharsets.UTF_8);
    }

    public Response jwks(@NotBlank @Pattern(regexp = Const.APP_ID_REGEX) String appId) {
        var app = dataService.findApp(appId);
        if (app != null) {
            try {
                byte[] keyBytes = CommonUtils.decodeFromBase64(app.getPublicKey());
                RSAPublicKey publicKey = (RSAPublicKey) KeyFactory
                        .getInstance("RSA")
                        .generatePublic(new X509EncodedKeySpec(keyBytes));

                String n = CommonUtils.urlEncodeWithoutPaddingToBase64(publicKey.getModulus().toByteArray());
                String e = CommonUtils.urlEncodeWithoutPaddingToBase64(publicKey.getPublicExponent().toByteArray());


                Map<String, Object> jwk = new HashMap<>();
                jwk.put("kid", JwtUtils.thumbprint(publicKey));
                jwk.put("use", "sig");
                jwk.put("kty", "RSA");
                jwk.put("e", e);
                jwk.put("n", n);
                jwk.put("alg", "RS256");

                List<Map<String, Object>> keysList = List.of(jwk);

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

    /**
     * Restores the attested credential data of a given user.
     *
     * <p>Credentials are stored in the canonical CBOR encoding of webauthn4j. Credentials that were
     * registered before that encoding was introduced are restored from the stored public key, which
     * is an X.509 encoded EC public key. The AAGUID is not used during authentication and is
     * therefore zeroed in that case.</p>
     */

    public Response registerInit(@NotNull @NotEmpty Map<String, String> data, Request request) {
        var app = dataService.findApp(data.get("appId"));
        if (app != null && isAllowedOrigin(app, request) &&
            dataService.isRegistrationAllowed(app) && dataService.isValidNonce(app, request)) {
            String username = data.get("username");
            var user = dataService.findUser(username, app.getAppId());

            if (user == null && AppUtils.isAllowedDomain(app, username))  {
                var challenge = new DefaultChallenge();
                var flowId = CommonUtils.randomString(32);
                CacheUtils.cacheRegisterChallenge(flowId, app.getAppId(), username, challenge.getValue());

                var authenticatorSelectionCriteria =
                        new AuthenticatorSelectionCriteria(
                                AuthenticatorAttachment.CROSS_PLATFORM,
                                Boolean.FALSE,
                                ResidentKeyRequirement.PREFERRED,
                                UserVerificationRequirement.REQUIRED
                        );

                var options = new PublicKeyCredentialCreationOptions(
                        new PublicKeyCredentialRpEntity(AppUtils.getDomain(app.getUrl()), AppUtils.getDomain(app.getUrl())),
                        new PublicKeyCredentialUserEntity(CommonUtils.uuidV6().getBytes(StandardCharsets.UTF_8), username, ""),
                        challenge,
                        PUB_KEY_CRED_PARAMS,
                        60000L,
                        List.of(),
                        authenticatorSelectionCriteria,
                        AttestationConveyancePreference.NONE,
                        null
                );

                return cors(Response.ok(), app)
                        .header("Access-Control-Expose-Headers", Const.FLOW_ID_HEADER)
                        .header(Const.FLOW_ID_HEADER, flowId)
                        .bodyJson(options);
            }
        }

        return Response.badRequest();
    }

    public Response preflight(Request request) {
        String origin = request.getHeader("Origin");
        if (StringUtils.isNotBlank(origin)) {
            var app = dataService.findAppByUrl(origin);
            if (app != null) {
                return cors(Response.ok(), app)
                        .header("Access-Control-Allow-Methods", "POST, OPTIONS")
                        .header("Access-Control-Max-Age", "600")
                        .header("Access-Control-Allow-Headers", "Content-Type, karakal-username, karakal-app-id, karakal-nonce, " + Const.FLOW_ID_HEADER);
            }
        }

        return Response.badRequest();
    }

    public Response registerComplete(Request request) {
        try {
            User user = null;
            String body = request.getBody();
            String username = request.getHeader("karakal-username");
            var app = dataService.findApp(request.getHeader("karakal-app-id"));
            if (app != null) {
                user = dataService.findUser(username, app.getAppId());
            }

            if (app != null &&
                isAllowedOrigin(app, request) &&
                dataService.isRegistrationAllowed(app) &&
                user == null &&
                StringUtils.isNotBlank(body) &&
                dataService.isValidNonce(app, request)) {

                var webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager();

                byte[] challenge = CacheUtils.getAndRemoveRegisterChallenge(
                        request.getHeader(Const.FLOW_ID_HEADER), app.getAppId(), username);
                if (challenge == null) {
                    return Response.badRequest();
                }

                var serverProperty = ServerProperty
                        .builder()
                        .origin(new Origin(app.getUrl()))
                        .rpId(AppUtils.getDomain(app.getUrl()))
                        .challenge(new DefaultChallenge(challenge))
                        .build();

                var registrationParameters = registrationParameters(serverProperty);

                var registrationData = webAuthnManager.parseRegistrationResponseJSON(body);
                webAuthnManager.verify(registrationData, registrationParameters);

                AttestedCredentialData attestedCredentialData = null;
                AuthenticatorData<RegistrationExtensionAuthenticatorOutput> registrationAuthenticatorData = null;
                var attestationObject = registrationData.getAttestationObject();
                if (attestationObject != null) {
                    registrationAuthenticatorData = attestationObject.getAuthenticatorData();
                    attestedCredentialData = registrationAuthenticatorData.getAttestedCredentialData();
                }

                if (attestedCredentialData != null && attestedCredentialData.getCOSEKey().getPublicKey() != null) {
                    byte[] credentialId = attestedCredentialData.getCredentialId();
                    byte[] publicKeyCose = attestedCredentialData.getCOSEKey().getPublicKey().getEncoded();

                    user = new User(username);
                    user.setAppId(app.getAppId());
                    user.setCredentialId(credentialId);
                    user.setPublicKeyCose(publicKeyCose);
                    user.setSignCount(registrationAuthenticatorData.getSignCount());
                    user.setUvInitialized(registrationAuthenticatorData.isFlagUV());
                    user.setBackupEligible(registrationAuthenticatorData.isFlagBE());
                    user.setBackedUp(registrationAuthenticatorData.isFlagBS());
                    user.setAttestedCredentialDataCbor(toBase64(
                            ATTESTED_CREDENTIAL_DATA_CONVERTER.convert(attestedCredentialData)));

                    // Kept for rollback safety only, no longer read on authentication
                    user.setAttestedCredentialData(JsonUtils.toJson(attestedCredentialData));
                    user.setCoseKey(JsonUtils.toJson(attestedCredentialData.getCOSEKey()));

                    dataService.save(user);

                    // The first administrator is registered without any authentication, so the
                    // registration is closed immediately instead of waiting for the first login
                    if (app.isDashboard() && app.isRegistration()) {
                        app.setRegistration(false);
                        dataService.save(app);
                    }

                    return cors(Response.ok(), app);
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

        var app = dataService.findApp(data.get("appId"));
        if (app == null) {
            return Response.notFound();
        }

        var user = dataService.findUser(username, app.getAppId());
        if (user != null && isAllowedOrigin(app, request) &&
            AppUtils.isAllowedDomain(app, username) && dataService.isValidNonce(app, request)) {
            Map<String, Object> allowCredential = new HashMap<>();
            allowCredential.put("type", "public-key");
            allowCredential.put("id", CommonUtils.urlEncodeWithoutPaddingToBase64(user.getCredentialId()));

            byte [] challenge =  new DefaultChallenge().getValue();
            var flowId = CommonUtils.randomString(32);
            CacheUtils.cacheLoginChallenge(flowId, app.getAppId(), username, challenge);

            Map<String, Object> response = new HashMap<>();
            response.put("challenge", CommonUtils.urlEncodeWithoutPaddingToBase64(challenge));
            response.put("timeout", 60000);
            response.put("rpId", AppUtils.getDomain(app.getUrl()));
            response.put("allowCredentials", List.of(allowCredential));
            response.put("userVerification", "required");

            return cors(Response.ok(), app)
                    .header("Access-Control-Expose-Headers", Const.FLOW_ID_HEADER)
                    .header(Const.FLOW_ID_HEADER, flowId)
                    .bodyJson(response);
        }

        return Response.badRequest();
    }

    public Response loginComplete(Request request) throws Exception {
        User user = null;
        var app = dataService.findApp(request.getHeader("karakal-app-id"));
        if (app != null) {
            user = dataService.findUser(request.getHeader("karakal-username"), app.getAppId());
        }

        String body = request.getBody();
        if (user != null && isAllowedOrigin(app, request) &&
            StringUtils.isNotBlank(body) && dataService.isValidNonce(app, request)) {
            var manager = WebAuthnManager.createNonStrictWebAuthnManager();
            AuthenticationData authData = manager.parseAuthenticationResponseJSON(body);

            byte[] challenge = CacheUtils.getAndRemoveLoginChallenge(
                    request.getHeader(Const.FLOW_ID_HEADER), app.getAppId(), user.getUsername());
            if (challenge == null) {
                return Response.badRequest();
            }

            var serverProperty = ServerProperty
                    .builder()
                    .origin(new Origin(app.getUrl()))
                    .rpId(AppUtils.getDomain(app.getUrl()))
                    .challenge(new DefaultChallenge(challenge))
                    .build();

            AttestedCredentialData attestedCredentialData;
            try {
                attestedCredentialData = attestedCredentialData(user);
            } catch (Exception e) {
                LOG.error("Failed to restore attested credential data", e);
                return Response.badRequest();
            }

            var credential = new Credential(
                    user.getCredentialId(),
                    user.getPublicKeyCose(),
                    user.getSignCount(),
                    attestedCredentialData,
                    user.getUvInitialized(),
                    user.getBackupEligible(),
                    user.getBackedUp());
            var params = new AuthenticationParameters(
                    serverProperty,
                    credential,
                    List.of(user.getCredentialId()),
                    true
            );

            var jwtData = JwtUtils.jwtData()
                    .withJwtID(CommonUtils.randomString(32))
                    .withKeyId(JwtUtils.thumbprint(JwtUtils.fromBase64Public(app.getPublicKey())))
                    .withAudience(app.getAudience())
                    .withIssuer(config.getString("karakal.url"))
                    .withSubject(user.getUsername())
                    .withSigningKey(JwtUtils.fromBase64Private(app.getPrivateKey()))
                    .withTtlSeconds(app.getTtl());

            var jwt = JwtUtils.createJwt(jwtData);

            try {
                manager.verify(authData, params);

                if (app.isDashboard() && app.isRegistration()) {
                    app.setRegistration(false);
                    dataService.save(app);
                }

                // The verifier updates the credential record (sign count, UV and backup state)
                // as part of a successful verification, see AuthenticationDataVerifier#updateRecord.
                // Backup eligibility is immutable for the lifetime of a credential and is therefore
                // never overwritten here.
                user.setSignCount(credential.getCounter());
                user.setUvInitialized(credential.isUvInitialized());
                user.setBackedUp(credential.isBackedUp());

                // Lazy migration of credentials that were stored before the attested credential
                // data was persisted in its canonical CBOR encoding
                if (StringUtils.isBlank(user.getAttestedCredentialDataCbor())) {
                    user.setAttestedCredentialDataCbor(toBase64(
                            ATTESTED_CREDENTIAL_DATA_CONVERTER.convert(attestedCredentialData)));
                }

                dataService.save(user);

                return cors(Response.ok(), app)
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