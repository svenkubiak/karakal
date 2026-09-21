package utils;

import constants.Const;
import io.mangoo.cache.Cache;
import io.mangoo.cache.CacheProvider;
import io.mangoo.core.Application;
import org.apache.commons.lang3.StringUtils;

/**
 * Holds the WebAuthn challenges of pending registration and authentication flows.
 *
 * <p>A challenge is stored under an opaque flow id, so that concurrent flows of the same user can
 * not overwrite each other. Additionally a pointer from application and username to that flow id is
 * kept, which allows clients that do not send the flow id back to complete their flow. Challenges
 * are single use: they are removed as soon as they are read.</p>
 */
public final class CacheUtils {
    private static final String REGISTER = "-register-challenge";
    private static final String LOGIN = "-login-challenge";
    private static Cache cache;

    private CacheUtils() {}

    private static Cache cache() {
        if (cache == null) {
            cache = Application.getInstance(CacheProvider.class).getCache(Const.KARAKAL_CACHE_NAME);
        }

        return cache;
    }

    public static void cacheRegisterChallenge(String flowId, String appId, String username, byte[] challenge) {
        cache().put("flow:" + flowId + REGISTER, challenge);
        cache().put(appId + ":" + username + REGISTER, flowId);
    }

    public static byte[] getAndRemoveRegisterChallenge(String flowId, String appId, String username) {
        return getAndRemoveChallenge(flowId, appId + ":" + username + REGISTER, REGISTER);
    }

    public static void cacheLoginChallenge(String flowId, String appId, String username, byte[] challenge) {
        cache().put("flow:" + flowId + LOGIN, challenge);
        cache().put(appId + ":" + username + LOGIN, flowId);
    }

    public static byte[] getAndRemoveLoginChallenge(String flowId, String appId, String username) {
        return getAndRemoveChallenge(flowId, appId + ":" + username + LOGIN, LOGIN);
    }

    private static byte[] getAndRemoveChallenge(String flowId, String pointerKey, String suffix) {
        String flow = flowId;

        if (StringUtils.isBlank(flow)) {
            // Fallback for clients that do not send the flow id back yet
            flow = cache().get(pointerKey);
            cache().remove(pointerKey);
        }

        if (StringUtils.isBlank(flow)) {
            return null;
        }

        String challengeKey = "flow:" + flow + suffix;

        // Challenges are single use, a failed attempt must not leave a reusable challenge behind.
        // The mangoo cache offers no atomic get and remove, which is acceptable here as a
        // concurrent reader would need to present the very same valid assertion.
        byte[] challenge = cache().get(challengeKey);
        cache().remove(challengeKey);

        return challenge;
    }
}
