package controllers;

import constants.Const;
import filters.PasskeyFilter;
import io.mangoo.annotations.FilterWith;
import io.mangoo.core.Config;
import io.mangoo.filters.CsrfFilter;
import io.mangoo.routing.Response;
import io.mangoo.routing.bindings.Form;
import io.mangoo.routing.bindings.Request;
import io.mangoo.routing.bindings.Session;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.CookieImpl;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import models.App;
import models.User;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import services.DataService;
import utils.AppUtils;
import utils.JwtUtils;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class DashboardController {
    private static final Logger LOG = LogManager.getLogger(DashboardController.class);
    private final DataService dataService;
    private final Config config;

    @Inject
    public DashboardController(DataService dataService, Config config) {
        this.dataService = Objects.requireNonNull(dataService, "dataService must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    public Response login() {
        App dashboard = dataService.findDashboard();
        return Response.ok()
                .header("Cache-Control", Const.NO_STORE)
                .render("appId", dashboard.getAppId());
    }

    public Response logout(Session session, Request request) {
        session.clear();
        invalidateToken(request);

        Cookie cookie = new CookieImpl(Const.COOKIE_NAME)
                .setPath("/")
                .setSecure(true)
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
        return Response.ok()
                .header("Cache-Control", Const.NO_STORE)
                .render("apps", apps);
    }

    @FilterWith(PasskeyFilter.class)
    public Response app(String appId) {
        App app = null;
        if (StringUtils.isNotBlank(appId) && AppUtils.isValidAppId(appId)) {
            app = dataService.findApp(appId);
        }

        return Response.ok()
                .header("Cache-Control", Const.NO_STORE)
                .render("app", app);
    }

    @FilterWith(PasskeyFilter.class)
    public Response info(@NotBlank @Pattern(regexp = Const.APP_ID_REGEX) String appId) {
        var app = dataService.findApp(appId);
        if (app != null) {
            return Response.ok()
                    .header("Cache-Control", Const.NO_STORE)
                    .render("app", app)
                    .render("url", config.getString("karakal.url"));
        }
        return Response.notFound().bodyDefault();
    }


    @FilterWith({PasskeyFilter.class, CsrfFilter.class})
    public Response delete(@NotBlank @Pattern(regexp = Const.APP_ID_REGEX) String appId) {
        App app = dataService.findApp(appId);
        if (app == null) {
            return Response.notFound();
        }

        // Deleting the dashboard application would remove all administrators and the trust anchor
        // of the admin interface, leaving the instance unreachable until it is restarted
        if (app.isDashboard()) {
            LOG.warn("Rejected an attempt to delete the dashboard application");
            return Response.badRequest();
        }

        LOG.info("Deleting application {} including all of its users", app.getName());
        dataService.removeUsersFromApp(appId);
        dataService.deleteApp(appId);

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
        form.expectRegex("name", Const.NAME_PATTERN, "Name must be a valid value containing min. 3 and up to 64 alphanumeric characters.");
        // Only a syntactically valid name is passed on, as the checks below hit the database
        if (!form.hasError("name")) {
            String name = form.get("name");

            if (app == null || !app.isDashboard()) {
                form.expectFalse("name", Const.DASHBOARD.equalsIgnoreCase(name), "This application name is reserved.");
            }
            if (app == null || !name.equalsIgnoreCase(app.getName())) {
                form.expectFalse("name", dataService.appExists(name), "An application with the same name already exists.");
            }
        }
        boolean isDashboard = app != null && app.isDashboard();

        // URL, redirect and audience of the dashboard application are derived from karakal.url.
        // Changing them would either lock out every administrator (rpId/origin of the passkeys,
        // audience of the token) or turn the login page into an open redirect.
        if (!isDashboard) {
            form.expectValue("redirect", "Redirect must be a valid URL.");
            form.expectUrl("redirect", "Redirect must be a valid URL.");
            form.expectValue("url", "URL must be a valid URL.");
            form.expectUrl("url", "URL must be a valid URL.");
            form.expectValue("audience", "Audience must be a valid host.");
            form.expectRegex("audience", Const.AUDIENCE_PATTERN, "Audience must be a valid host.");
        }
        if (StringUtils.isNotBlank(form.get("email"))) {
            form.expectTrue("email", AppUtils.validateCommaSeparatedDomains(form.get("email")), "E-mails domains must be comma separated value of domains");
        }
        form.expectValue("ttl", "Ttl muss be a valid integer between 60 and 900 seconds.");
        form.expectNumeric("ttl", "Ttl muss be a valid integer between 60 and 900 seconds.");
        form.expectRangeValue("ttl", 60, 900, "Ttl muss be a valid integer between 60 and 900 seconds.");

        if (form.isValid()) {
            if (app == null) {
                app = new App(form.get("name"));
            }

            boolean registration = form.getBoolean("registration").orElse(Boolean.FALSE);
            if (isDashboard) {
                String karakalUrl = config.getString("karakal.url");
                app.setName(Const.DASHBOARD);
                app.setUrl(AppUtils.normalizeOrigin(karakalUrl));
                app.setRedirect(karakalUrl + "/dashboard");
                app.setAudience(AppUtils.getDomain(karakalUrl));

                if (registration && !app.isRegistration()) {
                    LOG.warn("Registration for the dashboard application was enabled. Anyone matching " +
                             "the configured e-mail domains can now register as an administrator.");
                }
            } else {
                app.setName(form.get("name"));
                // stored as a serialized origin, so it can be compared to the Origin header
                app.setUrl(AppUtils.normalizeOrigin(form.get("url")));
                app.setRedirect(form.get("redirect"));
                app.setAudience(form.get("audience"));
            }

            app.setRegistration(registration);
            app.setEmail(form.get("email"));
            app.setTtl(form.getLong("ttl").orElse(Const.COOKIE_MAX_AGE));
            dataService.save(app);
        } else {
            form.keep();
            return Response.redirect("/dashboard/app");
        }

        return Response.redirect("/dashboard");
    }


    private void invalidateToken(Request request) {
        var cookie = request.getCookie(Const.COOKIE_NAME);
        if (cookie == null || StringUtils.isBlank(cookie.getValue())) {
            return;
        }

        try {
            App dashboard = dataService.findDashboard();
            String url = config.getString("karakal.url");

            var claims = JwtUtils.verify(
                    cookie.getValue(),
                    JwtUtils.fromBase64Public(dashboard.getPublicKey()),
                    url,
                    AppUtils.getDomain(url));

            User user = dataService.findUser(claims.getSubject(), dashboard.getAppId());
            if (user != null) {
                user.setInvalidBefore(Instant.now());
                dataService.save(user);
            }
        } catch (Exception e) {
            // An invalid or expired token can not be invalidated, the cookie is dropped either way
            LOG.info("Could not invalidate token on logout", e);
        }
    }
}