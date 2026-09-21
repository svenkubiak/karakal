package constants;

import java.time.Duration;
import java.util.regex.Pattern;

public final class Const {
    public static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_\\- ]{3,64}$");
    public static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    public static final String APP_ID_REGEX = "^[a-zA-Z0-9-_]{1,100}$";
    public static final Pattern APP_ID_PATTERN = Pattern.compile(APP_ID_REGEX);
    public static final String COOKIE_NAME = "__Host-karakal-auth";
    public static final String FLOW_ID_HEADER = "karakal-flow-id";
    public static final String KARAKAL_CACHE_NAME = "karakal-auth-cache";
    public static final String DASHBOARD = "Dashboard";
    public static final long COOKIE_MAX_AGE = 600L;
    public static final Duration SETUP_WINDOW = Duration.ofMinutes(15);
    public static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self'",
            "img-src 'self'",
            "font-src 'self'",
            "connect-src 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'",
            "object-src 'none'",
            "base-uri 'none'");
    public static final String PERMISSIONS_POLICY = String.join(", ",
            "accelerometer=()",
            "camera=()",
            "geolocation=()",
            "gyroscope=()",
            "magnetometer=()",
            "microphone=()",
            "payment=()",
            "usb=()",
            "publickey-credentials-get=(self)",
            "publickey-credentials-create=(self)");
    public static final String STRICT_TRANSPORT_SECURITY = "max-age=31536000; includeSubDomains";
    public static final String NO_STORE = "no-store";
    // Optional leading @, at least one label plus a TLD, sub domains allowed, no leading/trailing hyphen
    // A single host, with or without sub domains, e.g. localhost or api.myapp.com
    public static final Pattern AUDIENCE_PATTERN =
            Pattern.compile("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*$");
    public static final Pattern DOMAIN_PATTERN =
            Pattern.compile("^@?(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}$");
    private Const() {}
}
