package com.tourism.business.reservations.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
import com.tourism.business.business.dto.SettingsDto.Hour;
public final class ReservationDto {
    private ReservationDto() {}
    public record Request(@NotNull LocalDate visitDate, @NotBlank @Size(min=2,max=120) String name,
        @NotBlank @Email @Size(max=254) String email, @NotBlank @Size(max=40) String phone,
        @NotNull @Min(0) Integer adults, @NotNull @Min(0) Integer children, @NotNull @Min(0) Integer seniors,
        @NotNull @Min(0) Integer vehicles, @Size(max=1000) String note, @Pattern(regexp="es|en|pt") String locale,
        @Size(max=40) String rateVersion) {
        public Request { name=name==null?null:name.trim(); email=email==null?null:email.trim(); phone=phone==null?null:phone.trim(); note=note==null?null:note.trim(); locale=locale==null?"es":locale; }
        public long people() { return (long)adults+children+seniors; }
    }
    public record Day(LocalDate visitDate, List<Hour> hours, long available, String reason) {}
    public record Month(LocalDate serverDate, LocalDate minDate, LocalDate maxDate, List<Day> days) {}
    public record Line(String category, int quantity, String unitPrice, String subtotal) {}
    public record Quote(LocalDate visitDate, String rateVersion, String currency, List<Line> lines, String total, List<Hour> hours) {}
    public record Confirmation(String id, String code, String status, String version, String name, String locale,
        Quote quote, String sinpeNumber, String businessName, String phone, String email, String emailStatus) {}
    public record Transition(@NotBlank String version, @NotBlank @Size(max=1000) String reason) {}
}
