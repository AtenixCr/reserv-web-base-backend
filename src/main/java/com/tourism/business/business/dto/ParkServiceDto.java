package com.tourism.business.business.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public final class ParkServiceDto {

    private ParkServiceDto() {}

    public record Translation(
        @NotNull
        @Pattern(regexp = "es|en|pt")
        String locale,

        @NotBlank
        @Size(max = 120)
        String name,

        @NotNull
        @Size(max = 4000)
        String description,

        @NotNull
        @Size(max = 250)
        String imageAlt
    ) {}

    public record Save(
        @Size(max = 64)
        String version,

        @Size(max = 2048)
        String imageUrl,

        @Size(max = 2048)
        String iconUrl,

        @NotNull
        @Min(0)
        Integer sortOrder,

        @NotNull
        Boolean enabled,

        @NotNull
        @Size(min = 1, max = 3)
        List<@NotNull @Valid Translation> translations
    ) {}

    public record AdminService(
        String id,
        String version,
        String imageUrl,
        String iconUrl,
        int sortOrder,
        boolean enabled,
        List<Translation> translations
    ) {}

    public record PublicService(
        String id,
        String imageUrl,
        String iconUrl,
        List<Translation> translations
    ) {}
}