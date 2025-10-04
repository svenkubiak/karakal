package app;

import com.github.benmanes.caffeine.cache.Caffeine;
import constants.Const;
import controllers.ApplicationController;
import controllers.AssetController;
import controllers.DashboardController;
import controllers.PasskeyController;
import io.mangoo.cache.Cache;
import io.mangoo.cache.CacheImpl;
import io.mangoo.cache.CacheProvider;
import io.mangoo.core.Application;
import io.mangoo.interfaces.MangooBootstrap;
import io.mangoo.routing.Bind;
import io.mangoo.routing.On;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import services.DataService;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Singleton
public class Bootstrap implements MangooBootstrap {
    private final DataService dataService;

    @Inject
    public Bootstrap(DataService dataService) {
        this.dataService = Objects.requireNonNull(dataService, "dataService can not be null");
    }

    @Override
    public void initializeRoutes() {
        Bind.controller(ApplicationController.class).withRoutes(
                On.get().to("/").respondeWith("index"),
                On.get().to("/health").respondeWith("health")
        );

        Bind.controller(DashboardController.class).withRoutes(
                On.get().to("/dashboard").respondeWith("index"),
                On.get().to("/dashboard/login").respondeWith("login"),
                On.get().to("/dashboard/logout").respondeWith("logout"),
                On.get().to("/dashboard/app").respondeWith("app"),
                On.get().to("/dashboard/app/{appId}").respondeWith("app"),
                On.get().to("/dashboard/app/{appId}/info").respondeWith("info"),
                On.post().to("/dashboard/app").respondeWith("save"),
                On.delete().to("/dashboard/app/{appId}").respondeWith("delete")
        );

        Bind.controller(AssetController.class).withRoutes(
                On.get().to("/api/v1/assets/{appId}/karakal.min.js").respondeWith("scripts"),
                On.get().to("/api/v1/assets/{appId}/karakal.min.css").respondeWith("styles")
        );

        Bind.controller(PasskeyController.class).withRoutes(
                On.get().to("/api/v1/app/{appId}/jwks.json").respondeWith("jwks"),
                On.post().to("/api/v1/register-init").respondeWith("registerInit"),
                On.post().to("/api/v1/register-complete").respondeWith("registerComplete"),
                On.post().to("/api/v1/login-init").respondeWith("loginInit"),
                On.post().to("/api/v1/login-complete").respondeWith("loginComplete"),
                On.options().to("/api/v1/register-init").respondeWith("preflight"),
                On.options().to("/api/v1/register-complete").respondeWith("preflight"),
                On.options().to("/api/v1/login-init").respondeWith("preflight"),
                On.options().to("/api/v1/login-complete").respondeWith("preflight")
        );
        
        Bind.pathResource().to("/assets/");
        Bind.fileResource().to("/robots.txt");
        Bind.fileResource().to("/favicon.ico");
    }
    
    @Override
    public void applicationInitialized() {
        Cache karakalCache = new CacheImpl(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.of(60, ChronoUnit.SECONDS))
                .build());

        Application.getInstance(CacheProvider.class).addCache(Const.KARAKAL_CACHE_NAME, karakalCache);
    }

    @Override
    public void applicationStarted() {
        dataService.init();
        dataService.indexify();
    }

    @Override
    public void applicationStopped() {
        // TODO Auto-generated method stub
    }
}