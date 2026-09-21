package filters;

import com.nimbusds.jwt.JWTClaimsSet;
import constants.Const;
import io.mangoo.interfaces.filters.PerRequestFilter;
import io.mangoo.routing.Response;
import io.mangoo.routing.bindings.Request;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import models.App;
import models.User;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import services.DataService;
import utils.AppUtils;
import utils.JwtUtils;

import java.time.Instant;
import java.util.Objects;

public class PasskeyFilter implements PerRequestFilter {
    private static final Logger LOG = LogManager.getLogger(PasskeyFilter.class);
    private final String url;
    private final DataService dataService;

    @Inject
    public PasskeyFilter(@Named("karakal.url") String url, DataService dataService) {
        this.url = Objects.requireNonNull(url, "url can not be null");
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
                    LOG.error("Failed to verify JWT", e);
                }
            }
        }

        return Response.redirect("/dashboard/login");
    }

    private void validateJwt(String jwt) throws Exception {
        Objects.requireNonNull(jwt, "JWT can not be null");

        App dashboard = dataService.findDashboard();
        if (dashboard == null) {
            throw new IllegalStateException("No dashboard application found");
        }

        // The public key is read directly from the database. It used to be fetched over HTTP from
        // the own public JWKS endpoint, which made the reverse proxy and the name resolution part
        // of the trust anchor of the admin interface.
        JWTClaimsSet claims = JwtUtils.verify(
                jwt,
                JwtUtils.fromBase64Public(dashboard.getPublicKey()),
                url,
                AppUtils.getDomain(url));

        // A valid signature is not sufficient, the subject must still be a user of the dashboard
        // application. Otherwise a deleted administrator would keep full access until the token
        // expires.
        User user = dataService.findUser(claims.getSubject(), dashboard.getAppId());
        if (user == null) {
            throw new Exception("Subject is not a user of the dashboard application");
        }

        if (isInvalidated(user, claims)) {
            throw new Exception("Token was invalidated by a logout");
        }
    }

    private boolean isInvalidated(User user, JWTClaimsSet claims) {
        Instant invalidBefore = user.getInvalidBefore();
        if (invalidBefore == null) {
            return false;
        }

        var issuedAt = claims.getIssueTime();
        return issuedAt == null || issuedAt.toInstant().isBefore(invalidBefore);
    }
}
