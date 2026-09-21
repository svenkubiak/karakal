package karakal.filters;

import constants.Const;
import filters.PasskeyFilter;
import io.mangoo.routing.Response;
import io.mangoo.routing.bindings.Request;
import io.undertow.server.handlers.CookieImpl;
import models.App;
import models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import services.DataService;
import utils.JwtUtils;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PasskeyFilterTests {
    private static final String URL = "https://karakal.example";

    private DataService dataService;
    private PasskeyFilter filter;
    private App dashboard;
    private KeyPair dashboardKeyPair;
    private User admin;

    @BeforeEach
    void setUp() throws Exception {
        dataService = mock(DataService.class);
        filter = new PasskeyFilter(URL, dataService);

        dashboard = new App("Dashboard");
        dashboard.setDashboard(true);
        dashboardKeyPair = JwtUtils.generateRSAKeyPair();
        dashboard.setPublicKey(JwtUtils.toBase64(dashboardKeyPair.getPublic()));
        dashboard.setPrivateKey(JwtUtils.toBase64(dashboardKeyPair.getPrivate()));

        admin = new User("admin@example.com");
        admin.setAppId(dashboard.getAppId());

        when(dataService.findDashboard()).thenReturn(dashboard);
        when(dataService.findUser("admin@example.com", dashboard.getAppId())).thenReturn(admin);
    }

    private String jwt(KeyPair keyPair, String keyId, long ttlSeconds, String issuer, String audience) throws Exception {
        var jwtData = JwtUtils.jwtData()
                .withIssuer(issuer)
                .withAudience(audience)
                .withSubject("admin@example.com")
                .withTtlSeconds(ttlSeconds)
                .withKeyId(keyId)
                .withSigningKey((RSAPrivateKey) keyPair.getPrivate());

        return JwtUtils.createJwt(jwtData);
    }

    private String validJwt() throws Exception {
        return jwt(dashboardKeyPair, JwtUtils.thumbprint((RSAPublicKey) dashboardKeyPair.getPublic()),
                600, URL, "karakal.example");
    }

    private Response execute(String cookieValue) {
        Request request = mock(Request.class);
        when(request.getCookie(Const.COOKIE_NAME))
                .thenReturn(cookieValue == null ? null : new CookieImpl(Const.COOKIE_NAME, cookieValue));

        return filter.execute(request, Response.ok());
    }

    private static void assertRedirectedToLogin(Response response) {
        assertEquals("/dashboard/login", response.getRedirectTo());
    }

    @Test
    void validTokenPasses() throws Exception {
        Response response = execute(validJwt());

        assertNull(response.getRedirectTo(), "a valid token must not be redirected");
    }

    @Test
    void publicKeyIsReadFromTheDatabaseNotOverHttp() throws Exception {
        execute(validJwt());

        // no network access, the trust anchor is the App document itself
        org.mockito.Mockito.verify(dataService).findDashboard();
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() throws Exception {
        KeyPair attacker = JwtUtils.generateRSAKeyPair();

        assertRedirectedToLogin(execute(jwt(attacker,
                JwtUtils.thumbprint((RSAPublicKey) dashboardKeyPair.getPublic()), 600, URL, "karakal.example")));
    }

    @Test
    void tokenWithForeignKeyIdIsRejected() throws Exception {
        KeyPair other = JwtUtils.generateRSAKeyPair();

        // signed with the correct key but claiming a different key id
        assertRedirectedToLogin(execute(jwt(dashboardKeyPair,
                JwtUtils.thumbprint((RSAPublicKey) other.getPublic()), 600, URL, "karakal.example")));
    }

    @Test
    void tokenWithoutKeyIdIsStillAccepted() throws Exception {
        assertNull(execute(jwt(dashboardKeyPair, null, 600, URL, "karakal.example")).getRedirectTo(),
                "tokens issued before the key id was introduced must keep working");
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        assertRedirectedToLogin(execute(jwt(dashboardKeyPair,
                JwtUtils.thumbprint((RSAPublicKey) dashboardKeyPair.getPublic()), -60, URL, "karakal.example")));
    }

    @Test
    void tokenOfAnotherIssuerOrAudienceIsRejected() throws Exception {
        String kid = JwtUtils.thumbprint((RSAPublicKey) dashboardKeyPair.getPublic());

        assertRedirectedToLogin(execute(jwt(dashboardKeyPair, kid, 600, "https://evil.example", "karakal.example")));
        assertRedirectedToLogin(execute(jwt(dashboardKeyPair, kid, 600, URL, "evil.example")));
    }

    @Test
    void missingOrBlankCookieIsRejected() {
        assertRedirectedToLogin(execute(null));
        assertRedirectedToLogin(execute(""));
        assertRedirectedToLogin(execute("not-a-jwt"));
    }

    @Test
    void missingDashboardApplicationDoesNotBlowUp() throws Exception {
        when(dataService.findDashboard()).thenReturn(null);

        assertRedirectedToLogin(execute(validJwt()));
    }

    // --- H-09 ------------------------------------------------------------------------------

    @Test
    void tokenOfADeletedUserIsRejected() throws Exception {
        String jwt = validJwt();
        when(dataService.findUser("admin@example.com", dashboard.getAppId())).thenReturn(null);

        assertRedirectedToLogin(execute(jwt));
    }

    @Test
    void tokenIssuedBeforeALogoutIsRejected() throws Exception {
        String jwt = validJwt();
        admin.setInvalidBefore(Instant.now().plusSeconds(5));

        assertRedirectedToLogin(execute(jwt));
    }

    @Test
    void tokenIssuedAfterALogoutIsAccepted() throws Exception {
        admin.setInvalidBefore(Instant.now().minusSeconds(60));

        assertNull(execute(validJwt()).getRedirectTo(),
                "a logout must not invalidate tokens that were issued afterwards");
    }

    @Test
    void usersWithoutAnInvalidationTimestampAreNotLockedOut() throws Exception {
        assertNull(admin.getInvalidBefore());

        assertNull(execute(validJwt()).getRedirectTo());
    }

    @Test
    void tokenThatIsNotValidYetIsRejected() throws Exception {
        // nbf is set to iat - 30s by createJwt, so a token of the future is built by hand
        var jwtData = utils.JwtUtils.jwtData()
                .withIssuer(URL)
                .withAudience("karakal.example")
                .withSubject("admin@example.com")
                .withTtlSeconds(600)
                .withSigningKey((RSAPrivateKey) dashboardKeyPair.getPrivate());

        String token = JwtUtils.createJwt(jwtData);
        com.nimbusds.jwt.SignedJWT parsed = com.nimbusds.jwt.SignedJWT.parse(token);
        var claims = new com.nimbusds.jwt.JWTClaimsSet.Builder(parsed.getJWTClaimsSet())
                .notBeforeTime(java.util.Date.from(Instant.now().plusSeconds(300)))
                .build();

        var resigned = new com.nimbusds.jwt.SignedJWT(parsed.getHeader(), claims);
        resigned.sign(new com.nimbusds.jose.crypto.RSASSASigner(dashboardKeyPair.getPrivate()));

        assertRedirectedToLogin(execute(resigned.serialize()));
    }
}
