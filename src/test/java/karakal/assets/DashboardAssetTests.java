package karakal.assets;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The dashboard assets exist as source and as a hand maintained minified artifact. These tests
 * guard that the CSRF token of H-11 is sent by both of them.
 */
class DashboardAssetTests {
    private static final Path RESOURCES = Path.of("src/main/resources");

    private static String read(String file) throws Exception {
        return Files.readString(RESOURCES.resolve(file), StandardCharsets.UTF_8);
    }

    @Test
    void layoutProvidesTheCsrfToken() throws Exception {
        String layout = read("templates/layout.ftl");

        assertTrue(layout.contains("name=\"csrf-token\""), layout);
        assertTrue(layout.contains("<@csrftoken/>"), layout);
    }

    @Test
    void deleteRequestSendsTheCsrfToken() throws Exception {
        for (String file : new String[]{"files/assets/js/dashboard.js", "files/assets/js/dashboard.min.js"}) {
            String content = read(file);

            assertTrue(content.contains("x-csrf-token"), file);
            assertTrue(content.contains("csrf-token"), file);
        }
    }

    @Test
    void dashboardFieldsAreReadOnlyInTheForm() throws Exception {
        String form = read("templates/DashboardController/app.ftl");

        for (String field : new String[]{"name=\"name\"", "name=\"url\"", "name=\"redirect\"", "name=\"audience\""}) {
            int index = form.indexOf(field);
            assertTrue(index > -1, field);
            assertTrue(form.startsWith(field + " <#if (app.dashboard)?? && app.dashboard>readonly</#if>", index),
                    "immutable for the dashboard application: " + field);
        }
    }
}
