package utils;

import com.webauthn4j.data.client.challenge.DefaultChallenge;
import constants.Const;
import io.mangoo.cache.Cache;
import io.mangoo.cache.CacheProvider;
import io.mangoo.core.Application;

public final class CacheUtils {
    private static final Cache CACHE;
    static {
        CACHE = Application.getInstance(CacheProvider.class).getCache(Const.KARAKAL_CACHE_NAME);
    }

    public static void cacheRegisterChallenge(String username, byte [] challenge) {
        CACHE.put(username + "-register-challenge", challenge);
    }

    public static void removeRegisterChallenge(String username) {
        CACHE.remove(username + "-register-challenge");
    }

    public static byte[] getRegisterChallenge(String username) {
        return CACHE.get(username + "-register-challenge");
    }

    public static void cacheLoginChallenge(String username, byte [] challenge) {
        CACHE.put(username + "-login-challenge", challenge);
    }

    public static void removeLoginChallenge(String username) {
        CACHE.remove(username + "-login-challenge");
    }

    public static byte[] getLoginChallenge(String username) {
        return CACHE.get(username + "-login-challenge");
    }
}
