package karakal.utils;

import models.App;
import org.junit.jupiter.api.Test;
import utils.AppUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    // --- H-01 ------------------------------------------------------------------------------

    @Test
    void matchesDomain_isAnchoredAtTheDomainBoundary() {
        java.util.List<String> allowed = java.util.List.of("example.com");

        assertTrue(AppUtils.matchesDomain("user@example.com", allowed));
        assertFalse(AppUtils.matchesDomain("attacker@evil-example.com", allowed),
                "a plain suffix comparison would accept an attacker controlled domain");
        assertFalse(AppUtils.matchesDomain("attacker@notexample.com", allowed));
        assertFalse(AppUtils.matchesDomain("attacker@xexample.com", allowed));
    }

    @Test
    void matchesDomain_acceptsSubDomains() {
        assertTrue(AppUtils.matchesDomain("user@sub.example.com", java.util.List.of("example.com")));
        assertTrue(AppUtils.matchesDomain("user@a.b.example.com", java.util.List.of("example.com")));
        assertFalse(AppUtils.matchesDomain("user@example.com.evil.io", java.util.List.of("example.com")));
    }

    @Test
    void matchesDomain_treatsBothNotationsAlike() {
        assertTrue(AppUtils.matchesDomain("user@corp.de", java.util.List.of("corp.de")));
        assertTrue(AppUtils.matchesDomain("user@corp.de", java.util.List.of("@corp.de")));
        assertFalse(AppUtils.matchesDomain("attacker@mycorp.de", java.util.List.of("corp.de")));
        assertFalse(AppUtils.matchesDomain("attacker@mycorp.de", java.util.List.of("@corp.de")));
    }

    @Test
    void matchesDomain_isCaseInsensitive() {
        assertTrue(AppUtils.matchesDomain("User@EXAMPLE.CoM", java.util.List.of("example.com")));
        assertTrue(AppUtils.matchesDomain("user@example.com", java.util.List.of("ExAmple.COM")));
    }

    @Test
    void matchesDomain_requiresAnAtSign() {
        assertFalse(AppUtils.matchesDomain("example.com", java.util.List.of("example.com")),
                "a value without a local part is not an e-mail address");
        assertFalse(AppUtils.matchesDomain("user@", java.util.List.of("example.com")));
    }

    @Test
    void matchesDomain_usesTheLastAtSign() {
        assertFalse(AppUtils.matchesDomain("\"user@example.com\"@evil.io", java.util.List.of("example.com")),
                "the host is everything after the last @, not a substring of the local part");
        assertTrue(AppUtils.matchesDomain("\"a@b\"@example.com", java.util.List.of("example.com")));
    }

    @Test
    void matchesDomain_ignoresBlankEntries() {
        assertFalse(AppUtils.matchesDomain("user@example.com", java.util.Arrays.asList("", "  ", null)));
    }

    @Test
    void domainPattern_acceptsTheDocumentedNotations() {
        assertTrue(AppUtils.validateCommaSeparatedDomains("example.com"));
        assertTrue(AppUtils.validateCommaSeparatedDomains("@example.com"),
                "the notation suggested by the dashboard must be saveable");
        assertTrue(AppUtils.validateCommaSeparatedDomains("sub.corp.de"),
                "sub domains must be saveable, they are accepted by the matching");
        assertTrue(AppUtils.validateCommaSeparatedDomains("@foo.de, bar.de, sub.example.com"));
    }

    @Test
    void domainPattern_rejectsInvalidDomains() {
        assertFalse(AppUtils.validateCommaSeparatedDomains("@mydomain"));
        assertFalse(AppUtils.validateCommaSeparatedDomains("-bad.de"));
        assertFalse(AppUtils.validateCommaSeparatedDomains("bad-.de"));
        assertFalse(AppUtils.validateCommaSeparatedDomains("example.com, not valid"));
        assertFalse(AppUtils.validateCommaSeparatedDomains("example.c"));
    }

    // --- H-10 ------------------------------------------------------------------------------

    @Test
    void normalizeOrigin_stripsPathAndTrailingSlash() {
        assertEquals("https://app.example.com", AppUtils.normalizeOrigin("https://app.example.com"));
        assertEquals("https://app.example.com", AppUtils.normalizeOrigin("https://app.example.com/"));
        assertEquals("https://app.example.com", AppUtils.normalizeOrigin("https://app.example.com/login?a=1"));
    }

    @Test
    void normalizeOrigin_stripsDefaultPortsButKeepsOthers() {
        assertEquals("https://app.example.com", AppUtils.normalizeOrigin("https://app.example.com:443/"));
        assertEquals("http://app.example.com", AppUtils.normalizeOrigin("http://app.example.com:80"));
        assertEquals("https://app.example.com:8443", AppUtils.normalizeOrigin("https://app.example.com:8443/x"));
        assertEquals("http://localhost:9090", AppUtils.normalizeOrigin("http://localhost:9090/"));
    }

    @Test
    void normalizeOrigin_lowercasesSchemeAndHost() {
        assertEquals("https://app.example.com", AppUtils.normalizeOrigin("HTTPS://APP.Example.COM"));
    }

    @Test
    void normalizeOrigin_returnsEmptyForAnythingThatIsNotAnOrigin() {
        assertEquals("", AppUtils.normalizeOrigin("null"));
        assertEquals("", AppUtils.normalizeOrigin(""));
        assertEquals("", AppUtils.normalizeOrigin(null));
        assertEquals("", AppUtils.normalizeOrigin("not a url"));
        assertEquals("", AppUtils.normalizeOrigin("/relative/path"));
    }

    @Test
    void normalizeOrigin_isStableWhenAppliedTwice() {
        String once = AppUtils.normalizeOrigin("https://app.example.com/login");
        assertEquals(once, AppUtils.normalizeOrigin(once));
    }

    // --- H-11 ------------------------------------------------------------------------------

    @Test
    void audiencePattern_acceptsHosts() {
        assertTrue(constants.Const.AUDIENCE_PATTERN.matcher("localhost").matches());
        assertTrue(constants.Const.AUDIENCE_PATTERN.matcher("api.myapp.com").matches());
        assertTrue(constants.Const.AUDIENCE_PATTERN.matcher("my-app.example.co.uk").matches());
    }

    @Test
    void audiencePattern_rejectsAnythingElse() {
        assertFalse(constants.Const.AUDIENCE_PATTERN.matcher("https://api.myapp.com").matches());
        assertFalse(constants.Const.AUDIENCE_PATTERN.matcher("a.de, b.de").matches(),
                "the backend writes a single audience into the token, a list would be meaningless");
        assertFalse(constants.Const.AUDIENCE_PATTERN.matcher("-bad.de").matches());
        assertFalse(constants.Const.AUDIENCE_PATTERN.matcher("bad-.de").matches());
        assertFalse(constants.Const.AUDIENCE_PATTERN.matcher("").matches());
        assertFalse(constants.Const.AUDIENCE_PATTERN.matcher("api.myapp.com:8080").matches());
    }

    @Test
    void everySaveableDomainBehavesAsExpectedWhenMatching() {
        // the notation offered by the UI and the matching logic must agree
        for (String domain : new String[]{"@foo.de", "bar.de", "sub.example.com"}) {
            assertTrue(AppUtils.validateCommaSeparatedDomains(domain), domain);

            String host = domain.startsWith("@") ? domain.substring(1) : domain;
            assertTrue(AppUtils.matchesDomain("user@" + host, java.util.List.of(domain)), domain);
            assertFalse(AppUtils.matchesDomain("user@evil-" + host, java.util.List.of(domain)), domain);
        }
    }
}
