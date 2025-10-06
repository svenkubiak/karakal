package filters;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import constants.Const;
import io.mangoo.cache.Cache;
import io.mangoo.interfaces.filters.PerRequestFilter;
import io.mangoo.routing.Response;
import io.mangoo.routing.bindings.Request;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import models.App;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import services.DataService;
import utils.AppUtils;

import java.net.URI;
import java.util.Date;
import java.util.Objects;

public class PasskeyFilter implements PerRequestFilter {
    private static final Logger LOG = LogManager.getLogger(PasskeyFilter.class);
    private static final String PUBLIC_KEY = "karakal-dashboard-public-key";
    private final Cache cache;
    private final String url;
    private final String jwksUrl;

    @Inject
    public PasskeyFilter(Cache cache, @Named("karakal.url") String url, DataService dataService) {
        this.cache = Objects.requireNonNull(cache, "cache can not be null");
        this.url = Objects.requireNonNull(url, "url can not be null");
        Objects.requireNonNull(dataService, "dataService can not be null");

        App dashboard = dataService.findDashboard();
        this.jwksUrl = url + "/api/v1/app/" + dashboard.getAppId() + "/jwks.json";
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
                    LOG.error("Failed to verify JWT", e);
                }
            }
        }

        return Response.redirect("/dashboard/login");
    }

    private void validateJwt(String jwt) throws Exception {
        Objects.requireNonNull(jwt, "JWT can not be null");

        SignedJWT signedJWT = SignedJWT.parse(jwt);
        RSASSAVerifier verifier = new RSASSAVerifier(getPublicKey());

        boolean isValid = signedJWT.verify(verifier);
        if (isValid) {
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            Date expirationTime = claims.getExpirationTime();

            if (expirationTime == null) {
                throw new Exception("Token does not have expiration time!");
            }
            if (new Date().after(expirationTime)) {
                throw new Exception("Token has expired!");
            }

            if (!claims.getIssuer().equals(url)) {
                throw new Exception("Invalid issuer");
            }
            if (!claims.getAudience().contains(AppUtils.getDomain(url))) {
                throw new Exception("Invalid audience");
            }
        } else {
            throw new Exception("Invalid JWT");
        }
    }

    private RSAKey getPublicKey() {
        return cache.get(PUBLIC_KEY, v -> {
            try {
                JWKSet jwkSet = JWKSet.load(URI.create(jwksUrl).toURL());
                JWK jwk = jwkSet.getKeys().getFirst();

                if (jwk instanceof RSAKey rsaJwk) {
                    return rsaJwk;
                }
            } catch (Exception e) {
                LOG.error("Failed to get public key", e);
            }

            return null;
        });
    }
}