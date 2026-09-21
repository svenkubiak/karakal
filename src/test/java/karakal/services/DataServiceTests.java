package karakal.services;

import io.mangoo.core.Config;
import io.mangoo.persistence.interfaces.Datastore;
import io.mangoo.routing.bindings.Request;
import models.App;
import models.User;
import org.bson.BsonDocument;
import org.bson.codecs.BsonValueCodecProvider;
import org.bson.codecs.DocumentCodecProvider;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import services.DataService;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DataServiceTests {
    private static final CodecRegistry CODEC_REGISTRY = CodecRegistries.fromProviders(
            new ValueCodecProvider(), new BsonValueCodecProvider(), new DocumentCodecProvider());

    private Datastore datastore;
    private Config config;
    private DataService dataService;

    @BeforeEach
    void setUp() throws Exception {
        datastore = mock(Datastore.class);
        config = mock(Config.class);
        dataService = new DataService(datastore, config);
        setApplicationStartedAt(Instant.now());
    }

    private static String json(Bson filter) {
        return filter.toBsonDocument(BsonDocument.class, CODEC_REGISTRY).toJson();
    }

    private static void setApplicationStartedAt(Instant instant) throws Exception {
        Field field = DataService.class.getDeclaredField("applicationStartedAt");
        field.setAccessible(true);
        field.set(null, instant);
    }

    private static App app(String name, boolean dashboard) {
        App app = new App(name);
        app.setDashboard(dashboard);
        app.setUrl("https://app.example");
        return app;
    }

    // --- C-01 -------------------------------------------------------------------------------

    @Test
    void findDashboard_usesExactFlagEquality_neverARegex() {
        dataService.findDashboard();

        ArgumentCaptor<Bson> filter = ArgumentCaptor.forClass(Bson.class);
        verify(datastore).find(eq(App.class), filter.capture());

        String query = json(filter.getValue());
        assertEquals("{\"dashboard\": true}", query);
        assertFalse(query.contains("$regularExpression"),
                "the dashboard must never be resolved through an unanchored regex");
    }

    @Test
    void findAppByUrl_excludesDashboardByFlag_notByName() {
        dataService.findAppByUrl("https://app.example");

        ArgumentCaptor<Bson> filter = ArgumentCaptor.forClass(Bson.class);
        verify(datastore).find(eq(App.class), filter.capture());

        String query = json(filter.getValue());
        assertTrue(query.contains("\"url\": \"https://app.example\""), query);
        assertTrue(query.contains("\"dashboard\": {\"$ne\": true}"), query);
    }

    @Test
    void init_migratesLegacyDashboard_byExactNameMatch() {
        App legacy = new App("Dashboard");
        assertFalse(legacy.isDashboard());

        when(datastore.find(eq(App.class), any(Bson.class))).thenAnswer(invocation -> {
            String query = json(invocation.getArgument(1));
            assertFalse(query.contains("$regularExpression"),
                    "the legacy lookup must be an exact match, not a regex: " + query);
            return query.contains("\"name\"") ? legacy : null;
        });

        dataService.init();

        ArgumentCaptor<Object> saved = ArgumentCaptor.forClass(Object.class);
        verify(datastore).save(saved.capture());
        App app = (App) saved.getValue();
        assertSame(legacy, app, "the existing dashboard app must be reused, not replaced");
        assertTrue(app.isDashboard());
    }

    @Test
    void init_createsFlaggedDashboard_whenNoneExists() {
        when(datastore.find(eq(App.class), any(Bson.class))).thenReturn(null);
        when(config.getString("karakal.url")).thenReturn("https://karakal.example");

        dataService.init();

        ArgumentCaptor<Object> saved = ArgumentCaptor.forClass(Object.class);
        verify(datastore).save(saved.capture());
        App app = (App) saved.getValue();
        assertEquals("Dashboard", app.getName());
        assertTrue(app.isDashboard());
        assertEquals("https://karakal.example", app.getUrl());
        assertEquals("https://karakal.example/dashboard", app.getRedirect());
    }

    @Test
    void init_doesNothing_whenFlaggedDashboardExists() {
        when(datastore.find(eq(App.class), any(Bson.class))).thenReturn(app("Dashboard", true));

        dataService.init();

        verify(datastore, never()).save(any());
    }

    // --- C-06 -------------------------------------------------------------------------------

    @Test
    void newApp_hasNonce() {
        assertEquals(32, new App("Some App").getNonce().length());
    }

    @Test
    void getNonce_returnsPersistedValue_withoutWriting() {
        App app = app("Some App", false);
        String nonce = app.getNonce();

        assertEquals(nonce, dataService.getNonce(app));
        verify(datastore, never()).save(any());
    }

    @Test
    void getNonce_generatesAndPersists_forLegacyAppWithoutNonce() {
        App app = app("Legacy App", false);
        app.setNonce(null);

        String nonce = dataService.getNonce(app);

        assertNotNull(nonce);
        assertEquals(32, nonce.length());
        assertEquals(nonce, app.getNonce());
        verify(datastore).save(app);
        assertEquals(nonce, dataService.getNonce(app), "the nonce must be stable across calls");
    }

    @Test
    void generateNonce_onlyMigratesAppsWithoutNonce() {
        App withNonce = app("With", false);
        App withoutNonce = app("Without", false);
        withoutNonce.setNonce("");
        when(datastore.findAll(eq(App.class), any(Bson.class))).thenReturn(List.of(withNonce, withoutNonce));

        String unchanged = withNonce.getNonce();
        dataService.generateNonce();

        assertEquals(unchanged, withNonce.getNonce(), "an existing nonce must not be rotated");
        assertNotNull(withoutNonce.getNonce());
        verify(datastore, times(1)).save(withoutNonce);
        verify(datastore, never()).save(withNonce);
    }

    @Test
    void isValidNonce_acceptsMatchingHeaderOnly() {
        App app = app("Some App", false);
        Request request = mock(Request.class);

        when(request.getHeader("karakal-nonce")).thenReturn(app.getNonce());
        assertTrue(dataService.isValidNonce(app, request));

        when(request.getHeader("karakal-nonce")).thenReturn("wrong");
        assertFalse(dataService.isValidNonce(app, request));

        when(request.getHeader("karakal-nonce")).thenReturn("  ");
        assertFalse(dataService.isValidNonce(app, request));

        when(request.getHeader("karakal-nonce")).thenReturn(null);
        assertFalse(dataService.isValidNonce(app, request));
    }

    // --- C-08 -------------------------------------------------------------------------------

    private void withUsers(long count) {
        when(datastore.countAll(eq(User.class), any(Bson.class))).thenReturn(count);
    }

    @Test
    void isRegistrationAllowed_isFalse_whenRegistrationDisabled() {
        App app = app("Some App", false);
        app.setRegistration(false);

        assertFalse(dataService.isRegistrationAllowed(app));
    }

    @Test
    void isRegistrationAllowed_isTrue_forRegularApps_regardlessOfSetupWindow() throws Exception {
        setApplicationStartedAt(Instant.now().minus(1, ChronoUnit.DAYS));

        assertTrue(dataService.isRegistrationAllowed(app("Some App", false)));
        verify(datastore, never()).countAll(any(), any());
    }

    @Test
    void isRegistrationAllowed_isTrue_forFirstAdministratorInsideSetupWindow() {
        withUsers(0);

        assertTrue(dataService.isRegistrationAllowed(app("Dashboard", true)));
    }

    @Test
    void isRegistrationAllowed_isFalse_forFirstAdministratorAfterSetupWindow() throws Exception {
        withUsers(0);
        setApplicationStartedAt(Instant.now().minus(16, ChronoUnit.MINUTES));

        assertFalse(dataService.isRegistrationAllowed(app("Dashboard", true)));
    }

    @Test
    void isRegistrationAllowed_isFalse_whenApplicationStartWasNeverMarked() throws Exception {
        withUsers(0);
        setApplicationStartedAt(null);

        assertFalse(dataService.isRegistrationAllowed(app("Dashboard", true)));
    }

    @Test
    void isRegistrationAllowed_isTrue_forDashboardWithUsers_evenAfterSetupWindow() throws Exception {
        withUsers(1);
        setApplicationStartedAt(Instant.now().minus(1, ChronoUnit.DAYS));

        assertTrue(dataService.isRegistrationAllowed(app("Dashboard", true)),
                "an administrator must still be able to open registration for additional administrators");
    }

    @Test
    void hasUsers_countsUsersOfTheGivenApp() {
        App app = app("Some App", false);
        when(datastore.countAll(eq(User.class), any(Bson.class))).thenReturn(0L, 3L);

        assertFalse(dataService.hasUsers(app));
        assertTrue(dataService.hasUsers(app));

        ArgumentCaptor<Bson> filter = ArgumentCaptor.forClass(Bson.class);
        verify(datastore, times(2)).countAll(eq(User.class), filter.capture());
        assertEquals("{\"appId\": \"" + app.getAppId() + "\"}", json(filter.getValue()));
    }

    @Test
    void markApplicationStarted_opensSetupWindow() throws Exception {
        setApplicationStartedAt(null);
        when(datastore.find(eq(App.class), any(Bson.class))).thenReturn(app("Dashboard", true));
        withUsers(0);

        dataService.markApplicationStarted();

        assertTrue(dataService.isRegistrationAllowed(app("Dashboard", true)));
    }

    @Test
    void findUser_rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> dataService.findUser("not-an-email", "appId"));
        assertThrows(IllegalArgumentException.class, () -> dataService.findUser("user@example.com", "invalid id!"));
        verify(datastore, never()).find(any(), any());
    }

    // --- H-10 ------------------------------------------------------------------------------

    @Test
    void findAppByUrl_normalizesTheGivenOrigin() {
        dataService.findAppByUrl("https://app.example.com/some/path");

        ArgumentCaptor<Bson> filter = ArgumentCaptor.forClass(Bson.class);
        verify(datastore).find(eq(App.class), filter.capture());

        assertTrue(json(filter.getValue()).contains("\"url\": \"https://app.example.com\""), json(filter.getValue()));
    }

    @Test
    void findAppByUrl_returnsNothingForAnInvalidOrigin() {
        assertNull(dataService.findAppByUrl("null"));
        verify(datastore, never()).find(any(), any());
    }

    @Test
    void normalizeAppUrls_migratesOnlyWhatIsNotNormalized() {
        App normalized = app("Normalized", false);
        normalized.setUrl("https://a.example.com");
        App legacy = app("Legacy", false);
        legacy.setUrl("https://b.example.com/callback/");
        when(datastore.findAll(eq(App.class), any(Bson.class))).thenReturn(List.of(normalized, legacy));

        dataService.normalizeAppUrls();

        assertEquals("https://b.example.com", legacy.getUrl());
        verify(datastore).save(legacy);
        verify(datastore, never()).save(normalized);
    }

    // --- H-04 ------------------------------------------------------------------------------

    @Test
    void appExists_rejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> dataService.appExists(" "));
        verify(datastore, never()).find(any(), any());
    }

    @Test
    void appExists_quotesAndAnchorsTheName() {
        dataService.appExists("My App");

        ArgumentCaptor<Bson> filter = ArgumentCaptor.forClass(Bson.class);
        verify(datastore).find(eq(App.class), filter.capture());

        assertEquals("{\"name\": {\"$regularExpression\": {\"pattern\": \"^\\\\QMy App\\\\E$\", \"options\": \"i\"}}}",
                json(filter.getValue()));
    }

    @Test
    void appExists_neverLetsTheCallerControlTheQuery() {
        for (String metacharacters : new String[]{".*", "^Dash", "(a+)+$", "["}) {
            reset(datastore);

            assertDoesNotThrow(() -> dataService.appExists(metacharacters),
                    "an invalid pattern must not escape as an exception: " + metacharacters);

            ArgumentCaptor<Bson> filter = ArgumentCaptor.forClass(Bson.class);
            verify(datastore).find(eq(App.class), filter.capture());

            String query = json(filter.getValue());
            assertTrue(query.contains(Pattern.quote(metacharacters).replace("\\", "\\\\")),
                    "the name must be quoted literally: " + query);
        }
    }
}
