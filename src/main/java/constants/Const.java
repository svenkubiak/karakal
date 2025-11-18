package constants;

import java.util.regex.Pattern;

public final class Const {
    public static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_\\- ]{3,64}$");
    public static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    public static final String APP_ID_REGEX = "^[a-zA-Z0-9-_]{1,100}$";
    public static final Pattern APP_ID_PATTERN = Pattern.compile(APP_ID_REGEX);
    public static final String COOKIE_NAME = "__Host-karakal-auth";
    public static final String KARAKAL_CACHE_NAME = "karakal-auth-cache";
    public static final String DASHBOARD = "Dashboard";
    public static final long COOKIE_MAX_AGE = 600L;
    public static final Pattern DOMAIN_PATTERN = Pattern.compile("^(?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.[A-Za-z]{2,}$");
}
