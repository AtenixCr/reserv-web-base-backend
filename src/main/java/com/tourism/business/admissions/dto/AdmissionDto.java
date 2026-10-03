package com.tourism.business.admissions.dto;
import jakarta.validation.constraints.*;
public final class AdmissionDto {
    private AdmissionDto() {}
    public record Open(@NotNull @Pattern(regexp="[0-9]{1,17}(\\.[0-9]{1,2})?") String openingFloat) {}
    public record Request(@Positive Long reservationId, String reservationVersion, String rateVersion,
        @Size(max=120) String name, @Email @Size(max=254) String email, @Size(max=40) String phone, @Size(max=1000) String note,
        @NotNull @Min(0) Integer adults, @NotNull @Min(0) Integer children, @NotNull @Min(0) Integer seniors, @NotNull @Min(0) Integer vehicles,
        @Pattern(regexp="CASH|SINPE|CARD") String paymentMethod,
        @Pattern(regexp="[0-9]{1,17}(\\.[0-9]{1,2})?") String receivedAmount, @Size(max=120) String reference) {}
    public record Contact(@NotBlank String version,@Size(max=120) String name,@Email @Size(max=254) String email,@Size(max=40) String phone,@Size(max=1000) String note) {}
    public record Correction(@NotBlank String version,@NotNull @Min(0) Integer adults,@NotNull @Min(0) Integer children,@NotNull @Min(0) Integer seniors,@NotNull @Min(0) Integer vehicles,
        @NotBlank @Size(max=1000) String reason,@Pattern(regexp="CASH|SINPE|CARD") String paymentMethod,
        @Pattern(regexp="[0-9]{1,17}(\\.[0-9]{1,2})?") String receivedAmount,@Size(max=120) String reference) {}
}
