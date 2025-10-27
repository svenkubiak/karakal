package services;

import com.google.common.base.Preconditions;
import com.mongodb.client.model.*;
import constants.Const;
import io.mangoo.core.Config;
import io.mangoo.persistence.interfaces.Datastore;
import io.mangoo.utils.Argument;
import jakarta.inject.Inject;
import models.App;
import models.User;
import utils.AppUtils;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;

public class DataService {
    private final Datastore datastore;
    private final Config config;

    @Inject
    public DataService(Datastore datastore, Config config) {
        this.datastore = Objects.requireNonNull(datastore, "datastore can not be null");
        this.config = Objects.requireNonNull(config, "config can not be null");
    }

    public void init() {
        App dashboard = findDashboard();
        if (dashboard == null) {
            String karakalUrl = config.getString("karakal.url");
            dashboard = new App(Const.DASHBOARD);
            dashboard.setAudience(AppUtils.getDomain(karakalUrl));
            dashboard.setRedirect(karakalUrl + "/dashboard");
            dashboard.setUrl(karakalUrl);
            datastore.save(dashboard);
        }
    }

    public App findDashboard() {
        return datastore.find(App.class, eq("name", Const.DASHBOARD));
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
        return datastore.findAll(App.class,
                not(eq("name", Const.DASHBOARD)), Sorts.ascending("name"));
    }

    public void save(Object object) {
        Objects.requireNonNull(object, "object can not be null");
        datastore.save(object);
    }

    public App findApp(String appId) {
        Argument.validate(appId, Const.APP_ID_PATTERN);

        return datastore.find(App.class,
                and(
                        eq("appId", appId),
                        not(eq("name", Const.DASHBOARD))));
    }

    public void deleteApp(String appId) {
        Argument.validate(appId, Const.APP_ID_PATTERN);

        App app = findApp(appId);
        if (app != null) {
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

    public boolean appExists(String name) {
        Argument.requireNonBlank(name, "name can not be null");

        return datastore.find(App.class,
                regex("name", Pattern.compile(name, Pattern.CASE_INSENSITIVE))) != null;
    }

    public App findAppByUrl(String url) {
        Argument.requireNonBlank(url, "url can not be null or blank");
        Preconditions.checkArgument(AppUtils.isValidUrl(url), "url is not a valid URL");

        return datastore.find(App.class,
                and(
                        eq("url", url),
                        not(eq("name", Const.DASHBOARD))));
    }
}
