package utils;

import models.App;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AppUtilsTests {
    @Test
    void getDomain_returnsHost_forValidUrl() {
        String host = AppUtils.getDomain("https://example.org:8443/path?q=1");
        assertEquals("example.org", host);
    }

    @Test
    void getDomain_returnsEmpty_forInvalidUrl() {
        String host = AppUtils.getDomain("not a url");
        assertEquals("", host);
    }

    @Test
    void isValidUrl_detectsValidAndInvalid() {
        assertTrue(AppUtils.isValidUrl("https://example.com"));
        assertTrue(AppUtils.isValidUrl("http://localhost:8080/health"));
        assertFalse(AppUtils.isValidUrl("ht!tp:/broken"));
        assertFalse(AppUtils.isValidUrl(""));
    }

    @Test
    void isValidAppId_matchesPattern() {
        assertTrue(AppUtils.isValidAppId("abc-123_ABC"));
        assertTrue(AppUtils.isValidAppId("a"));
        assertFalse(AppUtils.isValidAppId("has space"));
        assertFalse(AppUtils.isValidAppId("too*special"));
    }

    @Test
    void isAllowedDomain_trueWhenNoRestriction() {
        App app = new App();
        app.setEmail(""); // no domain restrictions configured
        assertTrue(AppUtils.isAllowedDomain(app, "user@example.com"));
    }

    @Test
    void isAllowedDomain_respectsConfiguredDomains() {
        App app = new App();
        app.setEmail("@example.com, @sub.domain.io");

        assertTrue(AppUtils.isAllowedDomain(app, "user@example.com"));
        assertTrue(AppUtils.isAllowedDomain(app, "a@sub.domain.io"));
        assertFalse(AppUtils.isAllowedDomain(app, "b@other.org"));
    }

    @Test
    void matchesDomain_checksSuffixes() {
        assertTrue(AppUtils.matchesDomain(" user@foo.bar ", java.util.List.of("@foo.bar")));
        assertFalse(AppUtils.matchesDomain("user@foo.bar", java.util.List.of("@bar.foo")));
    }
}
