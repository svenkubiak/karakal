package karakal.utils;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.mangoo.cache.CacheImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.CacheUtils;

import java.lang.reflect.Field;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class CacheUtilsTests {
    private static final String APP = "appId-1";
    private static final String OTHER_APP = "appId-2";
    private static final String USER = "victim@example.com";
    private static final byte[] VICTIM_CHALLENGE = "victim-challenge".getBytes();
    private static final byte[] ATTACKER_CHALLENGE = "attacker-challenge".getBytes();

    @BeforeEach
    void setUp() throws Exception {
        Field field = CacheUtils.class.getDeclaredField("cache");
        field.setAccessible(true);
        field.set(null, new CacheImpl(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofSeconds(60))
                .build()));
    }

    @Test
    void concurrentLoginFlowsOfTheSameUserDoNotInterfere() {
        CacheUtils.cacheLoginChallenge("flow-victim", APP, USER, VICTIM_CHALLENGE);
        // an attacker calls login-init for the same user and the same application
        CacheUtils.cacheLoginChallenge("flow-attacker", APP, USER, ATTACKER_CHALLENGE);

        assertArrayEquals(VICTIM_CHALLENGE, CacheUtils.getAndRemoveLoginChallenge("flow-victim", APP, USER),
                "an attacker must not be able to invalidate a pending login of another user");
        assertArrayEquals(ATTACKER_CHALLENGE, CacheUtils.getAndRemoveLoginChallenge("flow-attacker", APP, USER));
    }

    @Test
    void concurrentRegisterFlowsOfTheSameUserDoNotInterfere() {
        CacheUtils.cacheRegisterChallenge("flow-a", APP, USER, VICTIM_CHALLENGE);
        CacheUtils.cacheRegisterChallenge("flow-b", APP, USER, ATTACKER_CHALLENGE);

        assertArrayEquals(VICTIM_CHALLENGE, CacheUtils.getAndRemoveRegisterChallenge("flow-a", APP, USER));
        assertArrayEquals(ATTACKER_CHALLENGE, CacheUtils.getAndRemoveRegisterChallenge("flow-b", APP, USER));
    }

    @Test
    void challengesAreScopedByApplication() {
        CacheUtils.cacheLoginChallenge("flow-a", APP, USER, VICTIM_CHALLENGE);
        CacheUtils.cacheLoginChallenge("flow-b", OTHER_APP, USER, ATTACKER_CHALLENGE);

        // legacy path without a flow id: each application resolves its own flow
        assertArrayEquals(VICTIM_CHALLENGE, CacheUtils.getAndRemoveLoginChallenge(null, APP, USER));
        assertArrayEquals(ATTACKER_CHALLENGE, CacheUtils.getAndRemoveLoginChallenge(null, OTHER_APP, USER));
    }

    @Test
    void loginChallengeIsSingleUse() {
        CacheUtils.cacheLoginChallenge("flow", APP, USER, VICTIM_CHALLENGE);

        assertArrayEquals(VICTIM_CHALLENGE, CacheUtils.getAndRemoveLoginChallenge("flow", APP, USER));
        assertNull(CacheUtils.getAndRemoveLoginChallenge("flow", APP, USER),
                "a challenge must not survive a failed verification");
        assertNull(CacheUtils.getAndRemoveLoginChallenge(null, APP, USER));
    }

    @Test
    void registerChallengeIsSingleUse() {
        CacheUtils.cacheRegisterChallenge("flow", APP, USER, VICTIM_CHALLENGE);

        assertArrayEquals(VICTIM_CHALLENGE, CacheUtils.getAndRemoveRegisterChallenge("flow", APP, USER));
        assertNull(CacheUtils.getAndRemoveRegisterChallenge("flow", APP, USER));
    }

    @Test
    void legacyFallbackResolvesTheFlowAndIsSingleUseAsWell() {
        CacheUtils.cacheLoginChallenge("flow", APP, USER, VICTIM_CHALLENGE);

        assertArrayEquals(VICTIM_CHALLENGE, CacheUtils.getAndRemoveLoginChallenge("  ", APP, USER));
        assertNull(CacheUtils.getAndRemoveLoginChallenge("  ", APP, USER));
        assertNull(CacheUtils.getAndRemoveLoginChallenge("flow", APP, USER),
                "the challenge itself must be gone, not only the pointer");
    }

    @Test
    void unknownFlowOrUserYieldsNoChallenge() {
        CacheUtils.cacheLoginChallenge("flow", APP, USER, VICTIM_CHALLENGE);

        assertNull(CacheUtils.getAndRemoveLoginChallenge("unknown-flow", APP, USER));
        assertNull(CacheUtils.getAndRemoveLoginChallenge(null, APP, "someone@else.com"));
        assertNull(CacheUtils.getAndRemoveLoginChallenge(null, OTHER_APP, USER));
    }

    @Test
    void registerAndLoginChallengesAreIndependent() {
        CacheUtils.cacheRegisterChallenge("flow", APP, USER, VICTIM_CHALLENGE);
        CacheUtils.cacheLoginChallenge("flow", APP, USER, ATTACKER_CHALLENGE);

        assertArrayEquals(VICTIM_CHALLENGE, CacheUtils.getAndRemoveRegisterChallenge("flow", APP, USER));
        assertArrayEquals(ATTACKER_CHALLENGE, CacheUtils.getAndRemoveLoginChallenge("flow", APP, USER));
    }
}
