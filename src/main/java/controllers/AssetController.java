package controllers;

import constants.Const;
import io.mangoo.core.Config;
import io.mangoo.routing.Response;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    public Response script(@NotBlank @Pattern(regexp = Const.APP_ID_REGEX) String appId) {
        var app = dataService.findApp(appId);
        if (app != null) {
            return Response.ok()
                    .contentType("text/javascript")
                    .header("Cache-Control", "no-cache")
                    .render("nonce", dataService.getNonce(app))
                    .render("appId", appId)
                    .render("api", config.getString("karakal.url"))
                    .render("registration", dataService.isRegistrationAllowed(app));
        }

        return Response.notFound()
                .contentType("text/javascript")
                .header("Cache-Control", "no-cache");
    }
}
