package controllers;

import com.google.re2j.Pattern;
import filters.PasskeyFilter;
import io.mangoo.annotations.FilterWith;
import io.mangoo.core.Config;
import io.mangoo.filters.CsrfFilter;
import io.mangoo.routing.Response;
import io.mangoo.routing.bindings.Form;
import io.mangoo.routing.bindings.Session;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.CookieImpl;
import jakarta.inject.Inject;
import models.App;
import org.apache.commons.lang3.StringUtils;
import services.DataService;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import constants.Const;
import utils.AppUtils;

public class DashboardController {
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_\\- ]{3,64}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^$|^(@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+)(, @[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+)*$");
    private final DataService dataService;
    private final Config config;

    @Inject
    public DashboardController(DataService dataService, Config config) {
        this.dataService = Objects.requireNonNull(dataService, "dataService must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    public Response login() {
        App dashboard = dataService.findDashboard();
        return Response.ok().render("appId", dashboard.getAppId());
    }

    public Response logout(Session session) {
        session.clear();

        App dashboard = dataService.findDashboard();
        Cookie cookie = new CookieImpl(Const.COOKIE_NAME)
                .setPath("/")
                .setSecure(true)
                .setDomain(AppUtils.getDomain(dashboard.getUrl()))
                .setValue("")
                .setMaxAge(-1)
                .setDiscard(true)
                .setExpires(new Date(1));

        return Response.redirect("/dashboard/login")
                .cookie(cookie)
                .header("Clear-Site-Data", "*");
    }

    @FilterWith(PasskeyFilter.class)
    public Response index() {
        List<App> apps = dataService.findApps();
        return Response.ok().render("apps", apps);
    }

    @FilterWith(PasskeyFilter.class)
    public Response app(String appId) {
        App app = null;
        if (StringUtils.isNotBlank(appId)) {
            app = dataService.findApp(appId);
        }

        return Response.ok().render("app", app);
    }

    @FilterWith(PasskeyFilter.class)
    public Response info(String appId) {
        App app = null;
        if (StringUtils.isNotBlank(appId)) {
            app = dataService.findApp(appId);
        }

        return Response.ok().render("app", app).render("url", config.getString("karakal.url"));
    }


    @FilterWith(PasskeyFilter.class)
    public Response delete(String appId) {
        dataService.removeUsersFromApp(appId);
        dataService.delete(appId);
        return Response.ok();
    }

    @FilterWith({PasskeyFilter.class, CsrfFilter.class})
    public Response save(Form form) {
        String appId = form.get("appId");
        App app = null;
        if (StringUtils.isNotBlank(appId)) {
            app = dataService.findApp(appId);
        }

        form.expectValue("name", "Name must be a valid value containing min. 3 and up to 64 alphanumeric characters.");
        form.expectRegex("name", NAME_PATTERN, "Name must be a valid value containing min. 3 and up to 64 alphanumeric characters.");
        if (app == null || !form.get("name").equalsIgnoreCase(app.getName())) {
            form.expectFalse("name", dataService.appExists(form.get("name")), "An application with the same name already exists.");
        }
        form.expectValue("redirect", "Redirect must be a valid URL.");
        form.expectUrl("redirect", "Redirect must be a valid URL.");
        form.expectValue("url", "URL must be a valid URL.");
        form.expectUrl("url", "URL must be a valid URL.");
        form.expectRegex("email", EMAIL_PATTERN, "E-mails domains must be comma seperated value of domains with user");
        form.expectValue("ttl", "Ttl muss be a valid integer between 60 and 900 seconds.");
        form.expectNumeric("ttl", "Ttl muss be a valid integer between 60 and 900 seconds.");
        form.expectRangeValue("ttl", 60, 900, "Ttl muss be a valid integer between 60 and 900 seconds.");

        if (form.isValid()) {
            if (app == null) {
                app = new App(form.get("name"));
            }

            app.setName(form.get("name"));
            app.setRegistration(form.getBoolean("registration").orElse(false));
            app.setUrl(form.get("url"));
            app.setRedirect(form.get("redirect"));
            app.setAudience(form.get("audience"));
            app.setEmail(form.get("email"));
            app.setTtl(form.getLong("ttl").orElse(Const.COOKIE_MAX_AGE));
            dataService.save(app);
        } else {
            form.keep();
            return Response.redirect("/dashboard/app");
        }

        return Response.redirect("/dashboard");
    }
}