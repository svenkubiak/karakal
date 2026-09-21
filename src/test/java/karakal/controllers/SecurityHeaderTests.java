package karakal.controllers;

import constants.Const;
import io.mangoo.test.http.TestRequest;
import io.mangoo.test.http.TestResponse;
import karakal.TestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({TestExtension.class})
public class SecurityHeaderTests {

    @Test
    public void securityHeadersArePresentOnEveryResponse() {
        TestResponse response = TestRequest.get("/health").execute();

        assertEquals(Const.CONTENT_SECURITY_POLICY, response.getHeader("Content-Security-Policy"));
        assertEquals(Const.PERMISSIONS_POLICY, response.getHeader("Permissions-Policy"));
        assertEquals(Const.STRICT_TRANSPORT_SECURITY, response.getHeader("Strict-Transport-Security"));
    }

    @Test
    public void defaultsOfTheFrameworkAreKept() {
        TestResponse response = TestRequest.get("/health").execute();

        assertEquals("DENY", response.getHeader("X-Frame-Options"));
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        assertEquals("no-referrer", response.getHeader("Referrer-Policy"));
    }

    @Test
    public void legacyXssAuditorIsDisabled() {
        assertEquals("0", TestRequest.get("/health").execute().getHeader("X-XSS-Protection"),
                "the legacy XSS auditor can introduce vulnerabilities of its own");
    }

    @Test
    public void serverHeaderIsNotDisclosed() {
        // TestResponse returns an empty string for a header that is not present
        assertEquals("", TestRequest.get("/health").execute().getHeader("Server"));
    }

    @Test
    public void contentSecurityPolicyForbidsFramingAndInlineCode() {
        String csp = TestRequest.get("/health").execute().getHeader("Content-Security-Policy");

        org.junit.jupiter.api.Assertions.assertTrue(csp.contains("frame-ancestors 'none'"), csp);
        org.junit.jupiter.api.Assertions.assertTrue(csp.contains("object-src 'none'"), csp);
        org.junit.jupiter.api.Assertions.assertTrue(csp.contains("base-uri 'none'"), csp);
        org.junit.jupiter.api.Assertions.assertFalse(csp.contains("unsafe-inline"), csp);
        org.junit.jupiter.api.Assertions.assertFalse(csp.contains("unsafe-eval"), csp);
    }

    @Test
    public void dashboardPagesAreNotCached() {
        assertEquals("no-store", TestRequest.get("/dashboard/login").execute().getHeader("Cache-Control"));
    }
}
