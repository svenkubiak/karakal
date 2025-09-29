package services;

import com.mongodb.client.model.*;
import io.mangoo.core.Config;
import io.mangoo.persistence.interfaces.Datastore;
import jakarta.inject.Inject;
import models.App;
import models.User;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;
import static org.apache.fury.codegen.ExpressionUtils.neq;

public class DataService {
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
            dashboard = new App("dashboard");
            dashboard.setAudience(config.getString("karakal.url"));
            dashboard.setRedirect(config.getString("karakal.url") + "/dashboard");
            dashboard.setDomain(config.getString("karakal.domain"));
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
        datastore.save(object);
    }

    public App findApp(String appId) {
        return datastore.find(App.class, eq("appId", appId));
    }

    public void delete(String appId) {
        App app = findApp(appId);
        datastore.delete(app);
    }

    public User findUser(String username) {
        return datastore.find(User.class,
                eq("username", username));
    }

    public User findUser(String username, String appId) {
        return datastore.find(User.class,
                and(eq("username", username), eq("appId", appId)));
    }

    public void removeUsersFromApp(String appId) {
        App app = findApp(appId);
        if (app != null) {
            datastore.query("users").deleteMany(eq("appId", appId));
        }
    }

    public boolean appExists(String name) {
        return datastore.find(App.class,
                regex("name", Pattern.compile(name, Pattern.CASE_INSENSITIVE))) != null;
    }
}
