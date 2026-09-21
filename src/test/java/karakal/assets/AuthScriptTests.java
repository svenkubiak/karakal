package karakal.assets;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The delivered auth script exists twice: as readable source (script.js) and as the minified
 * artifact that is actually served (script.ftl). There is no build step keeping them in sync,
 * so these tests guard the defects of C-07 in both files.
 */
class AuthScriptTests {
    private static final Path TEMPLATES = Path.of("src/main/resources/templates/AssetController");

    private static String read(String file) throws Exception {
        return Files.readString(TEMPLATES.resolve(file), StandardCharsets.UTF_8);
    }

    @Test
    void source_passesTheOptionsObjectToCredentialsCreate() throws Exception {
        String source = read("script.js");

        assertTrue(source.contains("navigator.credentials.create({publicKey: options})"), source);
        assertFalse(source.contains("{publicKey: t}"),
                "'t' is not declared in that scope and the resulting ReferenceError is swallowed by the catch");
    }

    @Test
    void minified_passesTheOptionsObjectToCredentialsCreate() throws Exception {
        String minified = read("script.ftl");

        assertTrue(minified.contains("navigator.credentials.create({publicKey:a})"), minified);
        assertFalse(minified.contains("navigator.credentials.create({publicKey:t})"), minified);
    }

    @Test
    void minified_keepsTheWorkingLoginPathUntouched() throws Exception {
        String minified = read("script.ftl");

        assertTrue(minified.contains("navigator.credentials.get({publicKey:t})"),
                "in the login path 't' legitimately is the options object and must not be renamed");
    }

    @Test
    void initialisationErrorPathDoesNotCallRenderFragment() throws Exception {
        assertFalse(read("script.js").contains("if (container) renderFragment(error)"),
                "renderFragment is block scoped and not yet initialised at that point");
        assertFalse(read("script.ftl").contains("container&&renderFragment(error)"),
                "renderFragment is block scoped and not yet initialised at that point");
    }

    @Test
    void bothFilesRenderTheNonceAndAppId() throws Exception {
        for (String file : new String[]{"script.js", "script.ftl"}) {
            String content = read(file);
            assertTrue(content.contains("${nonce}"), file);
            assertTrue(content.contains("${appId}"), file);
            assertTrue(content.contains("karakal-nonce"), file);
        }
    }
}
