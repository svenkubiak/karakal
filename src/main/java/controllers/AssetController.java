package controllers;

import constants.Const;
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

    @Inject
    public AssetController(DataService dataService, Config config) {
        this.dataService = Objects.requireNonNull(dataService, "dataService can not be null");
        this.config = Objects.requireNonNull(config, "config can not be null");
    }

    public Response styles(@NotBlank @Pattern(regexp = Const.APP_ID_PATTERN) String appId) {
        return Response.ok().contentType("text/css").render();
    }

    public Response scripts(@NotBlank @Pattern(regexp = Const.APP_ID_PATTERN) String appId) {
        App app = dataService.findApp(appId);
        if (app != null) {
            return Response.ok()
                    .contentType("text/javascript")
                    .header("Cache-Control", "no-cache")
                    .render("appId", appId)
                    .render("api", config.getString("karakal.url"))
                    .render("registration", app.isRegistration());
        }

        return Response.notFound();
    }
}
