package com.tourism.business;

import com.tourism.business.business.service.RemoteDatabase;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FoundationTest {
    @Test
    void databaseToolOnlyAcceptsExplicitRemoteDemo() {
        var url = "jdbc:sqlserver://example.invalid:1433;databaseName=TourismDemo;encrypt=true";
        assertDoesNotThrow(() -> RemoteDatabase.requireTarget("remote", url));
        assertThrows(IllegalArgumentException.class, () -> RemoteDatabase.requireTarget("production", url));
        assertThrows(IllegalArgumentException.class, () -> RemoteDatabase.requireTarget(null, url));
        assertThrows(IllegalArgumentException.class, () -> RemoteDatabase.requireTarget("remote", null));
        assertThrows(IllegalArgumentException.class, () -> RemoteDatabase.requireTarget("remote", url.replace("TourismDemo", "master")));
        assertThrows(IllegalArgumentException.class, () -> RemoteDatabase.requireTarget("remote", url + ";databaseName=master;"));
    }

    @Test
    void openApiContractIsValid() {
        var result = new OpenAPIV3Parser().readLocation("docs/openapi.yaml", null, null);
        assertNotNull(result.getOpenAPI());
        assertTrue(result.getMessages().isEmpty(), result.getMessages().toString());
        assertNotNull(result.getOpenAPI().getPaths().get("/health"));
    }
}
