package constants;

import com.google.re2j.Pattern;

public final class Const {
    public static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_\\- ]{3,64}$");
    public static final Pattern EMAIL_PATTERN = Pattern.compile("^$|^(@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+)(, @[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+)*$");
    public static final java.util.regex.Pattern USERNAME_PATTERN = java.util.regex.Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    public static final String APP_ID_PATTERN = "^[a-zA-Z0-9-_]{1,100}$";
    public static final String COOKIE_NAME = "karakal-auth";
    public static final long COOKIE_MAX_AGE = 600L;
    public static final String KARAKAL_CACHE_NAME = "karakal-auth-cache";
    public static final String DASHBOARD = "dashboard";
}
