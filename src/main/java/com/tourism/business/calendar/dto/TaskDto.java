package com.tourism.business.calendar.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public final class TaskDto {
    private TaskDto() {}
    public record Save(@NotNull LocalDate taskDate,@NotBlank @Size(max=120) String title,@NotBlank @Size(max=2000) String description,@NotNull @Pattern(regexp="PENDING|COMPLETED") String status,String version) {}
    public record Delete(@NotBlank String version) {}
}
