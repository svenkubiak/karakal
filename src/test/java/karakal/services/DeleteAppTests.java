package karakal.services;

import io.mangoo.core.Config;
import io.mangoo.persistence.interfaces.Datastore;
import models.App;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import services.DataService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeleteAppTests {
    private Datastore datastore;
    private DataService dataService;

    @BeforeEach
    void setUp() {
        datastore = mock(Datastore.class);
        dataService = new DataService(datastore, mock(Config.class));
    }

    @Test
    void deleteApp_refusesToDeleteTheDashboardApplication() {
        App dashboard = new App("Dashboard");
        dashboard.setDashboard(true);
        when(datastore.find(eq(App.class), any(Bson.class))).thenReturn(dashboard);

        assertThrows(IllegalArgumentException.class, () -> dataService.deleteApp("some-app-id"),
                "deleting the dashboard would remove the trust anchor of the admin interface");
        verify(datastore, never()).delete(any());
    }

    @Test
    void deleteApp_deletesRegularApplications() {
        App app = new App("Some App");
        when(datastore.find(eq(App.class), any(Bson.class))).thenReturn(app);

        dataService.deleteApp("some-app-id");

        verify(datastore).delete(app);
    }

    @Test
    void deleteApp_ignoresUnknownApplications() {
        when(datastore.find(eq(App.class), any(Bson.class))).thenReturn(null);

        assertDoesNotThrow(() -> dataService.deleteApp("some-app-id"));
        verify(datastore, never()).delete(any());
    }
}
