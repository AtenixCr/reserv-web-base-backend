package com.tourism.business.business.service;

import com.tourism.business.business.dto.ParkServiceDto.*;
import com.tourism.business.common.ApiException;
import java.net.URI;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParkServiceService {

    private final JdbcTemplate jdbc;

    public ParkServiceService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(
        readOnly = true,
        isolation = Isolation.REPEATABLE_READ
    )
    public List<AdminService> list() {
        return read(null, false);
    }

    @Transactional(
        readOnly = true,
        isolation = Isolation.REPEATABLE_READ
    )
    public List<PublicService> publicServices() {
        return read(null, true).stream()
            .map(service -> new PublicService(
                service.id(),
                service.imageUrl(),
                service.iconUrl(),
                service.translations()
            ))
            .toList();
    }

    @Transactional
    public AdminService create(Save body) {
        validate(body);

        List<Long> settings = jdbc.query(
            "SELECT id FROM dbo.BusinessSettings WHERE singleton = 1",
            (row, index) -> row.getLong("id")
        );

        if (settings.isEmpty()) {
            throw new ApiException(409, "SETTINGS_INCOMPLETE");
        }

        // OUTPUT INTO permite utilizar la tabla con triggers.
        Long id = jdbc.queryForObject("""
            SET NOCOUNT ON;

            DECLARE @created TABLE (id bigint);

            INSERT dbo.ParkService (
                settingsId, imageUrl, iconUrl, sortOrder, enabled
            )
            OUTPUT INSERTED.id INTO @created
            VALUES (?, ?, ?, ?, ?);

            SELECT id FROM @created;
            """,
            Long.class,
            settings.getFirst(),
            normalizeUrl(body.imageUrl()),
            normalizeUrl(body.iconUrl()),
            body.sortOrder(),
            body.enabled()
        );

        if (id == null) {
            throw new ApiException(500, "UNEXPECTED_ERROR");
        }

        replaceTranslations(id, body.translations());
        return get(id);
    }

    @Transactional
    public AdminService update(long id, Save body) {
        validate(body);
        lockVersion(id, body.version());

        jdbc.update("""
            UPDATE dbo.ParkService
            SET imageUrl = ?,
                iconUrl = ?,
                sortOrder = ?,
                enabled = ?
            WHERE id = ?
            """,
            normalizeUrl(body.imageUrl()),
            normalizeUrl(body.iconUrl()),
            body.sortOrder(),
            body.enabled(),
            id
        );

        replaceTranslations(id, body.translations());
        return get(id);
    }

    @Transactional
    public void disable(long id, String version) {
        lockVersion(id, version);

        // Desactiva el servicio sin eliminar sus datos.
        jdbc.update(
            "UPDATE dbo.ParkService SET enabled = 0 WHERE id = ?",
            id
        );
    }

    private void lockVersion(long id, String expectedVersion) {
        List<String> versions = jdbc.query("""
            SELECT version
            FROM dbo.ParkService WITH (UPDLOCK, HOLDLOCK)
            WHERE id = ?
            """,
            (row, index) -> Base64.getEncoder()
                .encodeToString(row.getBytes("version")),
            id
        );

        if (versions.isEmpty()) {
            throw new ApiException(404, "SERVICE_NOT_FOUND");
        }

        if (!Objects.equals(versions.getFirst(), expectedVersion)) {
            throw new ApiException(409, "VERSION_CONFLICT");
        }
    }

    private AdminService get(long id) {
        List<AdminService> result = read(id, false);

        if (result.isEmpty()) {
            throw new ApiException(404, "SERVICE_NOT_FOUND");
        }

        return result.getFirst();
    }

    private List<AdminService> read(Long id, boolean publicOnly) {
        String where = " WHERE b.singleton = 1";

        if (publicOnly) {
            where += " AND p.enabled = 1";
        }

        Object[] parameters;

        if (id != null) {
            where += " AND p.id = ?";
            parameters = new Object[] { id };
        } else {
            parameters = new Object[0];
        }

        Map<Long, List<Translation>> translations = new HashMap<>();

        jdbc.query("""
            SELECT t.serviceId, t.locale, t.name,
                   t.description, t.imageAlt
            FROM dbo.ParkServiceTranslation t
            JOIN dbo.ParkService p ON p.id = t.serviceId
            JOIN dbo.BusinessSettings b ON b.id = p.settingsId
            """ + where + " ORDER BY t.serviceId, t.locale",
            (row, index) -> {
                long serviceId = row.getLong("serviceId");

                translations.computeIfAbsent(
                    serviceId,
                    ignored -> new ArrayList<>()
                ).add(new Translation(
                    row.getString("locale"),
                    row.getString("name"),
                    row.getString("description"),
                    row.getString("imageAlt")
                ));

                return serviceId;
            },
            parameters
        );

        return jdbc.query("""
            SELECT p.id, p.version, p.imageUrl,
                   p.iconUrl, p.sortOrder, p.enabled
            FROM dbo.ParkService p
            JOIN dbo.BusinessSettings b ON b.id = p.settingsId
            """ + where + " ORDER BY p.sortOrder, p.id",
            (row, index) -> new AdminService(
                Long.toString(row.getLong("id")),
                Base64.getEncoder().encodeToString(
                    row.getBytes("version")
                ),
                row.getString("imageUrl"),
                row.getString("iconUrl"),
                row.getInt("sortOrder"),
                row.getBoolean("enabled"),
                List.copyOf(translations.getOrDefault(
                    row.getLong("id"),
                    List.of()
                ))
            ),
            parameters
        );
    }

    private void replaceTranslations(
        long id,
        List<Translation> translations
    ) {
        jdbc.update(
            "DELETE FROM dbo.ParkServiceTranslation WHERE serviceId = ?",
            id
        );

        for (Translation translation : translations) {
            jdbc.update("""
                INSERT dbo.ParkServiceTranslation (
                    serviceId, locale, name, description, imageAlt
                )
                VALUES (?, ?, ?, ?, ?)
                """,
                id,
                translation.locale(),
                translation.name().trim(),
                translation.description().trim(),
                translation.imageAlt().trim()
            );
        }
    }

    private void validate(Save body) {
        List<Translation> translations = body.translations();

        boolean repeatedLocale = translations.stream()
            .map(Translation::locale)
            .distinct()
            .count() != translations.size();

        boolean hasSpanish = translations.stream()
            .anyMatch(item -> item.locale().equals("es"));

        if (repeatedLocale || !hasSpanish) {
            throw new ApiException(
                400,
                "INVALID_INPUT",
                Map.of("translations", "INVALID_FIELD")
            );
        }

        validateUrl(body.imageUrl(), "imageUrl");
        validateUrl(body.iconUrl(), "iconUrl");
    }

    private void validateUrl(String value, String field) {
        String normalized = normalizeUrl(value);

        if (normalized == null) {
            return;
        }

        try {
            URI uri = new URI(normalized);

            boolean validScheme =
                "https".equalsIgnoreCase(uri.getScheme()) ||
                "http".equalsIgnoreCase(uri.getScheme());

            if (validScheme &&
                uri.getHost() != null &&
                uri.getUserInfo() == null &&
                uri.getFragment() == null) {
                return;
            }
        } catch (Exception ignored) {
            // Se devuelve el error de validación abajo.
        }

        throw new ApiException(
            400,
            "INVALID_INPUT",
            Map.of(field, "INVALID_FIELD")
        );
    }

    private String normalizeUrl(String value) {
        return value == null || value.isBlank()
            ? null
            : value.trim();
    }
}