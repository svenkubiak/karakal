package utils;

import constants.Const;
import io.mangoo.utils.Argument;
import models.App;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class AppUtils {
    public static String getDomain(String url) {
        Argument.requireNonBlank(url, "url can not be null or blank");
        try {
            return URI.create(url).toURL().getHost();
        } catch (MalformedURLException e) {
            //Intentionally left lank
        } catch (IllegalArgumentException e) {
            // Invalid URI syntax
        }

        return "";
    }

    /**
     * Normalizes a URL to its serialized origin, i.e. scheme, host and a non default port.
     *
     * <p>The {@code Origin} header of a browser never carries a path or a trailing slash, and the
     * value of {@code Access-Control-Allow-Origin} must be a serialized origin as well. Stored
     * application URLs are therefore normalized, otherwise a stored
     * {@code https://app.example.com/} would never match the origin {@code https://app.example.com}
     * and would additionally produce an invalid CORS header.</p>
     *
     * @return the normalized origin or an empty string if the given value is not a valid URL
     */
    public static String normalizeOrigin(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }

        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();

            if (scheme == null || host == null) {
                return "";
            }

            scheme = scheme.toLowerCase(Locale.ROOT);
            var origin = new StringBuilder(scheme).append("://").append(host.toLowerCase(Locale.ROOT));

            int port = uri.getPort();
            if (port != -1 && !isDefaultPort(scheme, port)) {
                origin.append(':').append(port);
            }

            return origin.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static boolean isDefaultPort(String scheme, int port) {
        return ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
    }

    public static boolean isValidUrl(String url) {
        try {
            URI.create(url).toURL();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean validateCommaSeparatedDomains(String input) {
        String[] domains = input.split("\\s*,\\s*");
        for (String domain : domains) {
            if (!Const.DOMAIN_PATTERN.matcher(domain).matches()) {
                return false;
            }
        }
        return true;
    }

    public static boolean isValidAppId(String appId) {
        return Const.APP_ID_PATTERN.matcher(appId).matches();
    }

    public static boolean isAllowedDomain(App app, String username) {
        Objects.requireNonNull(app, "app can not be null");
        Argument.requireNonBlank(username, "username can not be null or blank");

        var allowedDomain = true;
        String allowedDomains = app.getEmail();
        if (allowedDomains != null && !allowedDomains.isEmpty()) {
            List<String> list = Arrays.asList(allowedDomains.split(","));
            allowedDomain = AppUtils.matchesDomain(username, list);
        }
        return allowedDomain;
    }

    /**
     * Checks whether the host of a given e-mail address is covered by one of the given domains.
     *
     * <p>The comparison is anchored at the domain boundary: a domain matches the host itself or any
     * of its sub domains. A plain suffix comparison would accept {@code user@evil-example.com} for
     * the allowed domain {@code example.com}. A leading {@code @} in a configured domain is
     * optional, as both notations are documented.</p>
     */
    public static boolean matchesDomain(String email, List<String> domains) {
        Argument.requireNonBlank(email, "email can not be null or blank");
        Objects.requireNonNull(domains, "domains can not be null");

        int index = email.trim().lastIndexOf('@');
        if (index < 0) {
            return false;
        }

        String host = email.trim().substring(index + 1).toLowerCase(Locale.ROOT);
        if (host.isEmpty()) {
            return false;
        }

        for (String domain : domains) {
            String allowedDomain = domain == null ? "" : domain.trim().toLowerCase(Locale.ROOT);
            if (allowedDomain.startsWith("@")) {
                allowedDomain = allowedDomain.substring(1);
            }

            if (!allowedDomain.isEmpty() &&
                (host.equals(allowedDomain) || host.endsWith("." + allowedDomain))) {
                return true;
            }
        }

        return false;
    }
}
