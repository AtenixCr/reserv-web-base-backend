package com.tourism.business.reservations.controller;
import com.tourism.business.reservations.dto.ReservationDto.*;
import com.tourism.business.reservations.service.ReservationService;
import com.tourism.business.availability.service.AvailabilityService;
import com.tourism.business.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Clock;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1")
public class ReservationController {
    public record Edit(@jakarta.validation.constraints.NotBlank String version,@Valid @jakarta.validation.constraints.NotNull Request request) {}
    private final ReservationService reservations;
    private final AvailabilityService availability;
    private final Clock clock;
    private final int limit;
    private final long window;
    private final Map<String,List<Long>> attempts=new HashMap<>();
    public ReservationController(ReservationService reservations,AvailabilityService availability,Clock clock,@Value("${SECURITY_RESERVATION_MAX_ATTEMPTS:10}") int limit,@Value("${SECURITY_RESERVATION_WINDOW_SECONDS:600}") long window) { this.reservations=reservations;this.availability=availability;this.clock=clock;this.limit=limit;this.window=window*1000; }
    private synchronized void rateLimit(String address) {
        long now=clock.millis();attempts.values().forEach(v->v.removeIf(t->t<=now-window));attempts.entrySet().removeIf(e->e.getValue().isEmpty());
        var values=attempts.get(address);
        if(values!=null && values.size()>=limit) throw new ApiException(429,"RATE_LIMITED",Map.of("retryAfter",Long.toString(Math.max(1,(values.getFirst()+window-now+999)/1000))));
        if(values==null) { if(attempts.size()>=20000) throw new ApiException(429,"RATE_LIMITED",Map.of("retryAfter","600")); values=new ArrayList<>(); attempts.put(address,values); }
        values.add(now);
    }
    @GetMapping("/public/availability") public Month month(@RequestParam String month) { return availability.month(month); }
    @GetMapping("/reservations") public Object list(@RequestParam java.time.LocalDate date,@RequestParam(defaultValue="") String search,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return reservations.list(date,search,page,size);}
    @GetMapping("/reservations/{id}") public Object detail(@PathVariable long id){return reservations.detail(id);}
    @PostMapping("/reservations") public Object internalCreate(@Valid @RequestBody Request body,@RequestHeader(value="Idempotency-Key",required=false) String key){return ResponseEntity.status(201).body(reservations.create(body,key));}
    @PostMapping("/reservations/{id}/quotes") public Object editQuote(@PathVariable long id,@Valid @RequestBody Edit body){return reservations.editQuote(id,body.version(),body.request());}
    @PatchMapping("/reservations/{id}") public Object edit(@PathVariable long id,@Valid @RequestBody Edit body){return reservations.edit(id,body.version(),body.request());}
    @PostMapping("/public/reservation-quotes") public Quote quote(@Valid @RequestBody Request request) { return reservations.quote(request); }
    @PostMapping("/public/reservations") public ResponseEntity<Confirmation> create(@RequestBody Request body,@RequestHeader(value="Idempotency-Key",required=false) String key,HttpServletRequest request) { rateLimit(request.getRemoteAddr());return ResponseEntity.status(201).body(reservations.create(body,key)); }
    @PostMapping("/reservations/{id}/cancel") public Object cancel(@PathVariable long id,@Valid @RequestBody Transition body) { return reservations.transition(id,body,false); }
    @PostMapping("/reservations/{id}/no-show") public Object noShow(@PathVariable long id,@Valid @RequestBody Transition body) { return reservations.transition(id,body,true); }
}
