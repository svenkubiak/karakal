package utils;

import io.mangoo.utils.Arguments;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public final class AppUtils {

    public static String getDomain(String url) {
        Arguments.requireNonBlank(url, "url can not be null or blank");
        try {
            return URI.create(url).toURL().getHost();
        } catch (MalformedURLException e) {
            //Intentionally left lank
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
}
