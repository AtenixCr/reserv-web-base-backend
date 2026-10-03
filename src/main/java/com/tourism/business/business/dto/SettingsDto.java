package com.tourism.business.business.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
public final class SettingsDto {
    public record Translation(@Pattern(regexp="es|en|pt") @NotNull String locale,
        @NotBlank @Size(max=120) String displayName, @NotNull @Size(max=4000) String description, @NotNull @Size(max=4000) String services) {}
    public record Hour(@Min(1) @Max(7) int weekday, @NotNull @Pattern(regexp="([01][0-9]|2[0-3]):[0-5][0-9]") String opensAt,
        @NotNull @Pattern(regexp="([01][0-9]|2[0-3]):[0-5][0-9]") String closesAt) {}
    public record Closed(@NotNull LocalDate visitDate, @NotBlank @Size(max=500) String reason) {}
    public record CapacityOverride(@NotNull LocalDate visitDate, @Min(0) @NotNull Integer capacity) {}
    public record Settings(@NotBlank String version, @NotNull @Min(0) Integer defaultCapacity,
        @NotBlank @Size(max=40) String phone, @NotBlank @Email @Size(max=254) String email,
        @NotBlank @Size(max=500) String address, @NotBlank @Size(max=40) String sinpeNumber, boolean enabled,
        @NotNull @Size(min=1,max=3) List<@Valid Translation> translations,
        @NotNull @Size(max=70) List<@Valid Hour> hours, @NotNull @Size(max=1000) List<@Valid Closed> closedDates,
        @NotNull @Size(max=1000) List<@Valid CapacityOverride> capacityOverrides) {}
    public record Prices(@NotBlank String version,
        @NotNull @Pattern(regexp="[0-9]{1,17}(\\.[0-9]{1,2})?") String adultPrice,
        @NotNull @Pattern(regexp="[0-9]{1,17}(\\.[0-9]{1,2})?") String childPrice,
        @NotNull @Pattern(regexp="[0-9]{1,17}(\\.[0-9]{1,2})?") String seniorPrice,
        @NotNull @Pattern(regexp="[0-9]{1,17}(\\.[0-9]{1,2})?") String parkingPrice) {}
    public record Rate(String id, String adultPrice, String childPrice, String seniorPrice, String parkingPrice) {}
    public record Exchange(@NotNull @Pattern(regexp="USD|BRL") String currency,
        @NotNull @Pattern(regexp="[0-9]{1,11}(\\.[0-9]{1,8})?") String rate, @NotNull LocalDate updatedOn) {}
    public record Exchanges(@NotBlank String version, @NotNull @Size(max=2) List<@Valid Exchange> rates) {}
    public record Snapshot(Settings settings, Rate ratePlan, List<Exchange> exchangeRates, LocalDate serverDate) {}
}
