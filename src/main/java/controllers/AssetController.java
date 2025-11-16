package controllers;

import constants.Const;
import io.mangoo.cache.Cache;
import io.mangoo.core.Config;
import io.mangoo.routing.Response;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import models.App;
import services.DataService;

import java.util.Objects;

public class AssetController {
    private final DataService dataService;
    private final Config config;
    private final Cache cache;

    @Inject
    public AssetController(DataService dataService, Config config, Cache cache) {
        this.dataService = Objects.requireNonNull(dataService, "dataService can not be null");
        this.config = Objects.requireNonNull(config, "config can not be null");
        this.cache = Objects.requireNonNull(cache, "cache can not be null");
    }

    public Response script(@NotBlank @Pattern(regexp = Const.APP_ID_REGEX) String appId) {
        App app = dataService.findApp(appId);
        if (app != null) {
            return Response.ok()
                    .contentType("text/javascript")
                    .header("Cache-Control", "no-cache")
                    .render("nonce", cache.get("nonce-" + app.getAppId()))
                    .render("appId", appId)
                    .render("api", config.getString("karakal.url"))
                    .render("registration", app.isRegistration());
        }

        return Response.notFound()
                .contentType("text/javascript")
                .header("Cache-Control", "no-cache");
    }
}
