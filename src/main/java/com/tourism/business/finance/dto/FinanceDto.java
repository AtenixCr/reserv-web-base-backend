package com.tourism.business.finance.dto;
import jakarta.validation.constraints.*;
public final class FinanceDto {
    private FinanceDto() {}
    public record Expense(@NotNull @Pattern(regexp="[0-9]{1,17}(\\.[0-9]{1,2})?") String amount,@NotNull @Pattern(regexp="CASH|SINPE|CARD") String method,@NotBlank @Size(max=120) String category,@NotBlank @Size(max=1000) String description) {}
    public record Refund(@NotNull @Positive Long admissionId,@NotNull @Pattern(regexp="[0-9]{1,17}(\\.[0-9]{1,2})?") String amount,@NotNull @Pattern(regexp="CASH|SINPE|CARD") String method,@NotBlank @Size(max=1000) String reason) {}
    public record Compensation(@NotNull @Pattern(regexp="EXPENSE|REFUND|ADJUSTMENT") String sourceType,@NotNull @Positive Long sourceId,@NotBlank @Size(max=1000) String reason) {}
    public record Close(@NotBlank String version,@NotNull @Pattern(regexp="[0-9]{1,17}(\\.[0-9]{1,2})?") String countedCash,@Size(max=1000) String note,@NotNull @Pattern(regexp="es|en|pt") String locale) {}
}
