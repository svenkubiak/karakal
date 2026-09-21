package services;

import com.mongodb.client.model.*;
import constants.Const;
import io.mangoo.core.Config;
import io.mangoo.persistence.interfaces.Datastore;
import io.mangoo.routing.bindings.Request;
import io.mangoo.utils.Argument;
import io.mangoo.utils.CommonUtils;
import jakarta.inject.Inject;
import models.App;
import models.User;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.AppUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;

public class DataService {
    private static final Logger LOG = LogManager.getLogger(DataService.class);
    private static volatile Instant applicationStartedAt;
    private final Datastore datastore;
    private final Config config;

    @Inject
    public DataService(Datastore datastore, Config config) {
        this.datastore = Objects.requireNonNull(datastore, "datastore can not be null");
        this.config = Objects.requireNonNull(config, "config can not be null");
    }

    /**
     * Marks the point in time the application was started. The registration of the very first
     * administrator is only possible within {@link Const#SETUP_WINDOW} after that point in time.
     */
    public void markApplicationStarted() {
        applicationStartedAt = Instant.now();

        String karakalUrl = config.getString("karakal.url");
        if (karakalUrl != null && !karakalUrl.toLowerCase(java.util.Locale.ROOT).startsWith("https://")) {
            LOG.warn("karakal.url is not using https ({}). Cookies with the __Host- prefix and " +
                     "Strict-Transport-Security require a secure context, so authentication will " +
                     "not work outside of local development.", karakalUrl);
        }

        App dashboard = findDashboard();
        if (dashboard != null && dashboard.isRegistration() && !hasUsers(dashboard)) {
            LOG.warn("No administrator registered yet. Registration of the first administrator is " +
                     "possible until {} ({} minutes after application start). Restart the application " +
                     "to open a new setup window.",
                    applicationStartedAt.plus(Const.SETUP_WINDOW), Const.SETUP_WINDOW.toMinutes());
        }
    }

    public void init() {
        App dashboard = findDashboard();
        if (dashboard == null) {
            // Migration of existing installations: the dashboard app used to be identified
            // by its name only. Flag the legacy app instead of creating a second one.
            dashboard = datastore.find(App.class, eq("name", Const.DASHBOARD));

            if (dashboard == null) {
                String karakalUrl = config.getString("karakal.url");
                dashboard = new App(Const.DASHBOARD);
                dashboard.setAudience(AppUtils.getDomain(karakalUrl));
                dashboard.setRedirect(karakalUrl + "/dashboard");
                dashboard.setUrl(AppUtils.normalizeOrigin(karakalUrl));
            }

            dashboard.setDashboard(true);
            datastore.save(dashboard);
        }
    }

    public App findDashboard() {
        return datastore.find(App.class, eq("dashboard", true));
    }

    public void indexify() {
        Collation collation = Collation.builder()
                .locale("en")
                .collationStrength(CollationStrength.SECONDARY)
                .build();

        datastore.query("users").createIndex(Indexes.compoundIndex(
                Indexes.ascending("appId"),
                Indexes.ascending("username")
        ), new IndexOptions().unique(true).collation(collation));
    }

    public List<App> findApps() {
        return datastore.findAll(App.class, Sorts.ascending("name"));
    }

    public void save(Object object) {
        Objects.requireNonNull(object, "object can not be null");
        datastore.save(object);
    }

    public App findApp(String appId) {
        return datastore.find(App.class, eq("appId", appId));
    }

    public void deleteApp(String appId) {
        Argument.validate(appId, Const.APP_ID_PATTERN);

        App app = findApp(appId);
        if (app != null) {
            // The dashboard application is the trust anchor of the admin interface and must never
            // be removed, as that would leave the instance unreachable until it is restarted
            if (app.isDashboard()) {
                throw new IllegalArgumentException("The dashboard application can not be deleted");
            }

            datastore.delete(app);
        }
    }

    public User findUser(String username, String appId) {
        Argument.validate(username, Const.USERNAME_PATTERN);
        Argument.validate(appId, Const.APP_ID_PATTERN);

        return datastore.find(User.class,
                and(
                        eq("username", username),
                        eq("appId", appId)));
    }

    public void removeUsersFromApp(String appId) {
        Argument.validate(appId, Const.APP_ID_PATTERN);

        App app = findApp(appId);
        if (app != null) {
            datastore.query("users").deleteMany(eq("appId", appId));
        }
    }

    /**
     * Checks whether an application with the given name already exists. The name is quoted and
     * anchored, so that it is matched literally and completely - a caller must never be able to
     * control the query through regular expression metacharacters.
     */
    public boolean appExists(String name) {
        Argument.requireNonBlank(name, "name can not be null");

        return datastore.find(App.class,
                regex("name", Pattern.compile("^" + Pattern.quote(name) + "$", Pattern.CASE_INSENSITIVE))) != null;
    }

    public App findAppByUrl(String url) {
        Argument.requireNonBlank(url, "url can not be null or blank");

        String origin = AppUtils.normalizeOrigin(url);
        if (origin.isEmpty()) {
            return null;
        }

        return datastore.find(App.class,
                and(
                        eq("url", origin),
                        ne("dashboard", true)));
    }

    /**
     * Normalizes the URL of every application to its serialized origin. Applications stored before
     * the normalization was introduced may carry a path or a trailing slash, which never matches
     * the {@code Origin} header of a browser.
     */
    public void normalizeAppUrls() {
        for (App app : findApps()) {
            String origin = AppUtils.normalizeOrigin(app.getUrl());
            if (!origin.isEmpty() && !origin.equals(app.getUrl())) {
                LOG.info("Normalizing url of application {} from {} to {}", app.getName(), app.getUrl(), origin);
                app.setUrl(origin);
                datastore.save(app);
            }
        }
    }

    /**
     * Makes sure that every application has a nonce. Applications created before the nonce
     * was persisted are migrated on application start.
     */
    public void generateNonce() {
        List<App> apps = findApps();
        for (App app : apps) {
            getNonce(app);
        }
    }

    /**
     * Returns the nonce of a given application, creating and persisting one if required
     */
    public String getNonce(App app) {
        Objects.requireNonNull(app, "app can not be null");

        if (StringUtils.isBlank(app.getNonce())) {
            app.setNonce(CommonUtils.randomString(32));
            datastore.save(app);
        }

        return app.getNonce();
    }

    public boolean hasUsers(App app) {
        Objects.requireNonNull(app, "app can not be null");

        return datastore.countAll(User.class, eq("appId", app.getAppId())) > 0;
    }

    /**
     * Checks whether a user may register for a given application.
     *
     * <p>The registration of the very first administrator happens without any authentication and is
     * therefore limited to a short window after the application was started. Once an administrator
     * exists, the dashboard behaves like any other application and registration is controlled
     * exclusively by its registration flag.</p>
     */
    public boolean isRegistrationAllowed(App app) {
        Objects.requireNonNull(app, "app can not be null");

        if (!app.isRegistration()) {
            return false;
        }

        if (app.isDashboard() && !hasUsers(app)) {
            Instant startedAt = applicationStartedAt;
            return startedAt != null && Instant.now().isBefore(startedAt.plus(Const.SETUP_WINDOW));
        }

        return true;
    }

    public boolean isValidNonce(App app, Request request) {
        Objects.requireNonNull(app, "app can not be null");
        Objects.requireNonNull(request, "request can not be null");

        String nonce = request.getHeader("karakal-nonce");
        return StringUtils.isNotBlank(nonce) &&
               MessageDigest.isEqual(
                       nonce.getBytes(StandardCharsets.UTF_8),
                       getNonce(app).getBytes(StandardCharsets.UTF_8));
    }
}
