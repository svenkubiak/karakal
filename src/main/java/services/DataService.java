package services;

import com.google.common.base.Preconditions;
import com.mongodb.client.model.*;
import io.mangoo.core.Config;
import io.mangoo.persistence.interfaces.Datastore;
import io.mangoo.utils.Arguments;
import jakarta.inject.Inject;
import models.App;
import models.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.AppUtils;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;

public class DataService {
    private static final Logger LOG = LogManager.getLogger(DataService.class);
    private static final Pattern APP_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9-_]{1,100}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private final Datastore datastore;
    private final Config config;

    @Inject
    public DataService(Datastore datastore, Config config) {
        this.datastore = Objects.requireNonNull(datastore, "datastore can not be null");
        this.config = Objects.requireNonNull(config, "config can not be null");
    }

    public void init() {
        App dashboard = datastore.find(App.class, eq("name", "dashboard"));
        if (dashboard == null) {
            String karakalUrl = config.getString("karakal.url");
            dashboard = new App("dashboard");
            dashboard.setAudience(AppUtils.getDomain(karakalUrl));
            dashboard.setRedirect(karakalUrl + "/dashboard");
            dashboard.setUrl(karakalUrl);
            datastore.save(dashboard);
        }
    }

    public App findDashboard() {
        return datastore.find(App.class, eq("name", "dashboard"));
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
        return datastore.findAll(App.class, not(eq("name", "dashboard")), Sorts.ascending("name"));
    }

    public void save(Object object) {
        Objects.requireNonNull(object, "object can not be null");
        datastore.save(object);
    }

    public App findApp(String appId) {
        Arguments.requireNonBlank(appId, "appId can not be null");

        if (APP_ID_PATTERN.matcher(appId).matches()) {
            return datastore.find(App.class, eq("appId", appId));
        }

        return null;
    }

    public void delete(String appId) {
        Arguments.requireNonBlank(appId, "appId can not be null");

        if (APP_ID_PATTERN.matcher(appId).matches()) {
            App app = findApp(appId);
            datastore.delete(app);
        }
    }

    public User findUser(String username) {
        Arguments.requireNonBlank(username, "username can not be null");

        if (USERNAME_PATTERN.matcher(username).matches()) {
            return datastore.find(User.class, eq("username", username));
        }

        return null;
    }

    public User findUser(String username, String appId) {
        Arguments.requireNonBlank(username, "username can not be null");
        Arguments.requireNonBlank(appId, "appId can not be null");

        if (USERNAME_PATTERN.matcher(username).matches() && APP_ID_PATTERN.matcher(appId).matches()) {
            return datastore.find(User.class,
                    and(eq("username", username), eq("appId", appId)));
        }

        return null;
    }

    public void removeUsersFromApp(String appId) {
        Arguments.requireNonBlank(appId, "appId can not be null");

        App app = findApp(appId);
        if (app != null) {
            datastore.query("users").deleteMany(eq("appId", appId));
        }
    }

    public boolean appExists(String name) {
        return datastore.find(App.class,
                regex("name", Pattern.compile(name, Pattern.CASE_INSENSITIVE))) != null;
    }

    public App findAppByUrl(String url) {
        Arguments.requireNonBlank(url, "url can not be null or blank");
        Preconditions.checkArgument(AppUtils.isValidUrl(url), "url is not a valid URL");

        return datastore.find(App.class, eq("url", url));
    }
}
