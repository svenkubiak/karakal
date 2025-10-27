package models;

import constants.Const;
import io.mangoo.annotations.Collection;
import io.mangoo.annotations.Indexed;
import io.mangoo.persistence.Entity;
import io.mangoo.utils.Argument;
import io.mangoo.utils.CommonUtils;
import utils.JwtUtils;

import java.security.KeyPair;

@Collection(name = "apps")
public class App extends Entity {
    @Indexed(unique = true)
    private String appId;

    @Indexed(unique = true)
    private String name;

    private String redirect;
    private String url;
    private boolean registration;
    private String publicKey;
    private String privateKey;
    private String audience;
    private String email;
    private long ttl;

    public App() {}

    public App(String name) {
        Argument.requireNonBlank(name, "name can not be null or empty");

        this.appId = CommonUtils.randomString(32);
        this.name = name;
        this.registration = true;
        this.ttl = Const.COOKIE_MAX_AGE;

        try {
            KeyPair keyPair = JwtUtils.generateRSAKeyPair();

            this.privateKey = JwtUtils.toBase64(keyPair.getPrivate());
            this.publicKey = JwtUtils.toBase64(keyPair.getPublic());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRedirect() {
        return redirect;
    }

    public void setRedirect(String redirect) {
        this.redirect = redirect;
    }

    public boolean isRegistration() {
        return registration;
    }

    public void setRegistration(boolean registration) {
        this.registration = registration;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
