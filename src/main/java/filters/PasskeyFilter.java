package filters;

import constants.Const;
import io.mangoo.cache.Cache;
import io.mangoo.interfaces.filters.PerRequestFilter;
import io.mangoo.routing.Response;
import io.mangoo.routing.bindings.Request;
import io.mangoo.utils.CommonUtils;
import jakarta.inject.Inject;
import models.App;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import services.DataService;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Objects;

public class PasskeyFilter implements PerRequestFilter {
    private static final Logger LOG = LogManager.getLogger(PasskeyFilter.class);
    private static final String PUBLIC_KEY_PASSKEY = "karakal-app-public-key";
    private final Cache cache;
    private final DataService dataService;

    @Inject
    public PasskeyFilter(Cache cache, DataService dataService) {
        this.cache = Objects.requireNonNull(cache, "cache can not be null");
        this.dataService = Objects.requireNonNull(dataService, "dataService can not be null");
    }

    @Override
    public Response execute(Request request, Response response) {
        var cookie = request.getCookie(Const.COOKIE_NAME);
        if (cookie != null) {
            var cookieValue = cookie.getValue();
            if (StringUtils.isNotBlank(cookieValue)) {
                try {
                    validateJwt(cookieValue);
                    return response;
                } catch (Exception e) {
                    //Intentionally left blank
                }
            }
        }

        return Response.redirect("/dashboard/login");
    }

    private void validateJwt(String jwt) throws Exception {
        Objects.requireNonNull(jwt, "JWT can not be null");
        App dashboard = dataService.findDashboard();

        var jwtConsumer = new JwtConsumerBuilder()
                .setExpectedAudience(dashboard.getAudience())
                .setRequireExpirationTime()
                .setExpectedIssuer(dashboard.getUrl())
                .setEnableRequireIntegrity()
                .setVerificationKey(getPublicKey())
                .build();

        jwtConsumer.process(jwt);
    }

    private PublicKey getPublicKey() {
        return cache.get(PUBLIC_KEY_PASSKEY, v -> {
            App dashboard = dataService.findDashboard();
            if (dashboard != null) {
                try {
                    return KeyFactory
                            .getInstance("RSA")
                            .generatePublic(new X509EncodedKeySpec(CommonUtils.decodeFromBase64(dashboard.getPublicKey())));
                } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
                    LOG.error("Failed to parse app public key", e);
                }
            }

            return null;
        });
    }
}