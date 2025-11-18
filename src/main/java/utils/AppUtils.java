package utils;

import constants.Const;
import io.mangoo.utils.Argument;
import models.App;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
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

    public static boolean isValidUrl(String url) {
        try {
            URI.create(url).toURL();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean validateCommaSeparatedDomains(String input) {
        String[] domains = input.split("\\s*,\\s*"); // Split on commas and trim whitespace
        for (String domain : domains) {
            if (!Const.DOMAIN_PATTERN.matcher(domain).matches()) {
                return false; // Invalid domain found
            }
        }
        return true; // All domains valid
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

    public static boolean matchesDomain(String email, List<String> domains) {
        Argument.requireNonBlank(email, "email can not be null or blank");
        Objects.requireNonNull(domains, "domains can not be null");

        for (String domain : domains) {
            String trimmedDomain = domain == null ? "" : domain.trim();
            if (!trimmedDomain.isEmpty() && email.trim().endsWith(trimmedDomain)) {
                return true;
            }
        }
        return false;
    }
}
